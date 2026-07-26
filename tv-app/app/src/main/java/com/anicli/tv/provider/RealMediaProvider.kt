package com.anicli.tv.provider

import com.anicli.tv.data.remote.AnimeApiService
import com.anicli.tv.data.remote.CryptoUtils
import com.anicli.tv.data.remote.DynamicKey
import com.anicli.tv.data.remote.dto.GraphQLRequest
import com.anicli.tv.domain.model.Episode
import com.anicli.tv.domain.model.MediaDetail
import com.anicli.tv.domain.model.MediaItem
import com.anicli.tv.domain.model.MediaRow
import com.anicli.tv.domain.model.StreamSource
import com.anicli.tv.domain.provider.MediaProvider
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealMediaProvider @Inject constructor(
    private val apiService: AnimeApiService,
    @javax.inject.Named("VideoClient") private val okHttpClient: okhttp3.OkHttpClient
) : MediaProvider {

    override val name: String = "AllAnime (ani-cli native)"
    private val gson = Gson()
    private var cachedDynamicKey: DynamicKey? = null
    private var cachedDynamicKeyTime: Long = 0

    private val SEARCH_GQL = """query( ${'$'}search: SearchInput ${'$'}limit: Int ${'$'}page: Int ${'$'}translationType: VaildTranslationTypeEnumType ${'$'}countryOrigin: VaildCountryOriginEnumType ) { shows( search: ${'$'}search limit: ${'$'}limit page: ${'$'}page translationType: ${'$'}translationType countryOrigin: ${'$'}countryOrigin ) { edges { _id name englishName thumbnail availableEpisodes airedStart __typename } }}"""
    private val DETAILS_GQL = """query (${'$'}showId: String!) { show( _id: ${'$'}showId ) { _id name englishName thumbnail availableEpisodesDetail }}"""
    private val EPISODE_GQL = """query (${'$'}showId: String!, ${'$'}translationType: VaildTranslationTypeEnumType!, ${'$'}episodeString: String!) { episode( showId: ${'$'}showId translationType: ${'$'}translationType episodeString: ${'$'}episodeString ) { episodeString sourceUrls }}"""

    override suspend fun getHomeFeed(): Flow<Result<List<MediaRow>>> = flow {
        try {
            // Fetch recent or popular by sending an empty search query
            val request = GraphQLRequest(
                variables = mapOf(
                    "search" to mapOf(
                        "allowAdult" to false,
                        "allowUnknown" to false,
                        "query" to ""
                    ),
                    "limit" to 40,
                    "page" to 1,
                    "translationType" to "sub",
                    "countryOrigin" to "ALL"
                ),
                query = SEARCH_GQL
            )
            
            val response = apiService.searchAnime(request)
            val rows = mutableListOf<MediaRow>()
            
            val edges = response.data?.shows?.edges
            if (!edges.isNullOrEmpty()) {
                rows.add(
                    MediaRow(
                        title = "Recent Releases",
                        items = edges.map { edge ->
                            MediaItem(
                                id = edge._id,
                                title = edge.englishName?.takeIf { it.isNotBlank() } ?: edge.name,
                                posterUrl = edge.thumbnail ?: "https://wp.youtube-anime.com/aln.youtube-anime.com/images/covers/${edge._id}.jpg", 
                                backdropUrl = edge.thumbnail ?: "https://wp.youtube-anime.com/aln.youtube-anime.com/images/covers/${edge._id}.jpg",
                                tags = emptyList() // could extract from airedStart year if needed
                            )
                        }
                    )
                )
            }
            emit(Result.success(rows))
        } catch (e: Exception) {
            e.printStackTrace()
            emit(Result.failure(e))
        }
    }

    override suspend fun getDetails(mediaId: String): Result<MediaDetail> {
        return try {
            val request = GraphQLRequest(
                variables = mapOf("showId" to mediaId),
                query = DETAILS_GQL
            )
            
            val response = apiService.getAnimeInfo(request)
            val show = response.data?.show ?: throw Exception("Show details not found")
            
            // AllAnime returns a map of translation type (e.g. "sub", "dub", "raw") to a list of episodes
            val subEpisodes = show.availableEpisodesDetail?.get("sub") ?: emptyList()
            
            Result.success(
                MediaDetail(
                    id = show._id,
                    title = show.englishName?.takeIf { it.isNotBlank() } ?: show.name ?: "Anime",
                    description = "Description loaded.",
                    posterUrl = show.thumbnail ?: "https://wp.youtube-anime.com/aln.youtube-anime.com/images/covers/${show._id}.jpg",
                    backdropUrl = show.thumbnail ?: "https://wp.youtube-anime.com/aln.youtube-anime.com/images/covers/${show._id}.jpg",
                    genres = emptyList(),
                    episodes = subEpisodes.map { epNum ->
                        Episode(
                            id = "${show._id}|$epNum",
                            title = "Episode $epNum",
                            number = epNum.toFloatOrNull()?.toInt() ?: 0,
                            thumbnail = null,
                            isFiller = false
                        )
                    }
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun search(query: String): Result<List<MediaItem>> {
        if (query.isBlank()) {
            return Result.success(emptyList())
        }
        return try {
            val request = GraphQLRequest(
                variables = mapOf(
                    "search" to mapOf(
                        "allowAdult" to false,
                        "allowUnknown" to false,
                        "query" to query
                    ),
                    "limit" to 40,
                    "page" to 1,
                    "translationType" to "sub",
                    "countryOrigin" to "ALL"
                ),
                query = SEARCH_GQL
            )
            val searchResponse = apiService.searchAnime(request)
            
            val edges = searchResponse.data?.shows?.edges ?: emptyList()
            
            Result.success(
                edges.map { edge ->
                    MediaItem(
                        id = edge._id,
                        title = edge.englishName?.takeIf { it.isNotBlank() } ?: edge.name,
                        posterUrl = edge.thumbnail ?: "https://wp.youtube-anime.com/aln.youtube-anime.com/images/covers/${edge._id}.jpg",
                        backdropUrl = edge.thumbnail ?: "https://wp.youtube-anime.com/aln.youtube-anime.com/images/covers/${edge._id}.jpg",
                        tags = emptyList()
                    )
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

        override suspend fun getStreamSources(episodeId: String): Result<List<StreamSource>> {
        return try {
            val parts = episodeId.split("|")
            if (parts.size != 2) throw Exception("Invalid Episode ID format")
            val mediaId = parts[0]
            val epNumber = parts[1]
            
            val currentTime = System.currentTimeMillis()
            var dynamicKey = cachedDynamicKey
            if (dynamicKey == null || currentTime - cachedDynamicKeyTime > 5 * 60 * 1000) {
                dynamicKey = CryptoUtils.fetchDynamicKey(okHttpClient)
                cachedDynamicKey = dynamicKey
                cachedDynamicKeyTime = currentTime
            }
            
            val queryHash = "f4662f4b7510b26795dd53ef824a0bf1740fbbc5d1273fab18222ac831bca8d0"
            val aaReq = CryptoUtils.generateAaReq(dynamicKey.key, dynamicKey.epoch, queryHash, dynamicKey.serverTimeOffset)

            val parsedSources = java.util.Collections.synchronizedList(mutableListOf<StreamSource>())
            
            val types = listOf("sub", "dub")
            kotlinx.coroutines.withTimeoutOrNull(8000) {
                kotlinx.coroutines.coroutineScope {
                    val requests = types.map { translationType ->
                        async(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                val variablesObj = mapOf(
                                    "showId" to mediaId,
                                    "translationType" to translationType,
                                    "episodeString" to epNumber
                                )
                                val extensionsObj = mapOf(
                                    "persistedQuery" to mapOf(
                                        "version" to 1,
                                        "sha256Hash" to queryHash
                                    ),
                                    "aaReq" to aaReq
                                )
                                
                                val response = apiService.getWatchInfo(gson.toJson(variablesObj), gson.toJson(extensionsObj))
                                val data = response.data
                                val decryptedStreams = mutableListOf<String>()
                                
                                if (data?.tobeparsed != null) {
                                    val decrypted = CryptoUtils.decryptAllAnimeStream(data.tobeparsed, dynamicKey.key)
                                    if (decrypted.isNotEmpty()) decryptedStreams.add(decrypted)
                                } else {
                                    val sourceUrlsElem = data?.episode?.sourceUrls
                                    if (sourceUrlsElem != null && sourceUrlsElem.isJsonPrimitive && sourceUrlsElem.asJsonPrimitive.isString) {
                                        val strValue = sourceUrlsElem.asString
                                        if (strValue.contains("tobeparsed")) {
                                            val jsonObj = gson.fromJson(strValue, com.google.gson.JsonObject::class.java)
                                            val toBeParsed = jsonObj.get("tobeparsed")?.asString
                                            if (toBeParsed != null) {
                                                val decrypted = CryptoUtils.decryptAllAnimeStream(toBeParsed, dynamicKey.key)
                                                decryptedStreams.add(decrypted)
                                            }
                                        } else {
                                            val decrypted = CryptoUtils.decryptAllAnimeStream(strValue, dynamicKey.key)
                                            if (decrypted.isNotEmpty()) decryptedStreams.add(decrypted)
                                        }
                                    } else if (sourceUrlsElem != null && sourceUrlsElem.isJsonArray) {
                                        decryptedStreams.add(sourceUrlsElem.toString())
                                    }
                                }
                                
                                for (streamRaw in decryptedStreams) {
                                    var array: com.google.gson.JsonArray? = null
                                    val elem = gson.fromJson(streamRaw, com.google.gson.JsonElement::class.java)
                                    
                                    if (elem.isJsonArray) {
                                        array = elem.asJsonArray
                                    } else if (elem.isJsonObject) {
                                        val obj = elem.asJsonObject
                                        if (obj.has("episode") && !obj.get("episode").isJsonNull) {
                                            val epObj = obj.getAsJsonObject("episode")
                                            if (epObj.has("sourceUrls") && !epObj.get("sourceUrls").isJsonNull) array = epObj.getAsJsonArray("sourceUrls")
                                        } else if (obj.has("sourceUrls") && !obj.get("sourceUrls").isJsonNull) {
                                            array = obj.getAsJsonArray("sourceUrls")
                                        } else if (obj.has("links") && !obj.get("links").isJsonNull) {
                                            array = obj.getAsJsonArray("links")
                                        }
                                    }
    
                                    if (array != null) {
                                        for (i in 0 until array.size()) {
                                            val obj = array.get(i).asJsonObject
                                            val url = if (obj.has("link")) obj.get("link").asString else if (obj.has("url")) obj.get("url").asString else if (obj.has("sourceUrl")) obj.get("sourceUrl").asString else null
                                            val quality = if (obj.has("resolutionStr")) obj.get("resolutionStr").asString else if (obj.has("height")) obj.get("height").asString else "Auto"
                                            
                                            val typeLabel = if (translationType == "dub") "[Dub]" else "[Sub]"
                                            val sourceName = if (obj.has("sourceName")) "$typeLabel ${obj.get("sourceName").asString}" else "$typeLabel Quality: ${quality}p"
                                            
                                            if (url != null) {
                                                var finalUrl: String = url
                                                if (url.contains("mp4upload.com")) {
                                                    val extracted = com.anicli.tv.extractor.Mp4UploadExtractor.extract(url, okHttpClient)
                                                    if (extracted != null) finalUrl = extracted
                                                } else if (url.startsWith("/apivtwo/")) {
                                                    try {
                                                        val clockUrl = "https://mkissa.net$url"
                                                        val req = okhttp3.Request.Builder().url(clockUrl).header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:150.0) Gecko/20100101 Firefox/150.0").header("Referer", "https://mkissa.to/").build()
                                                        val resp = okHttpClient.newCall(req).execute()
                                                        val clockJson = resp.body?.string()
                                                        if (clockJson != null) {
                                                            val cObj = gson.fromJson(clockJson, com.google.gson.JsonObject::class.java)
                                                            val linksArr = cObj.getAsJsonArray("links")
                                                            if (linksArr != null && linksArr.size() > 0) {
                                                                finalUrl = linksArr.get(0).asJsonObject.get("link")?.asString ?: url
                                                            }
                                                        }
                                                    } catch(e: Exception) {}
                                                } else if (url.contains("ok.ru")) {
                                                    val extractedList = com.anicli.tv.extractor.OkRuExtractor.extract(url, okHttpClient)
                                                    for (ext in extractedList) {
                                                        parsedSources.add(StreamSource(ext.second, "$typeLabel Ok.ru ${ext.first}", ext.first, emptyList(), emptyMap()))
                                                    }
                                                }
                                                
                                                val isIframe = finalUrl.contains("ok.ru") || finalUrl.contains("/e/") || finalUrl.contains("embed-") || finalUrl.contains("watchanime")
                                                val referer = if (finalUrl.contains("mp4upload")) "https://www.mp4upload.com/" else "https://mkissa.to"
                                                
                                                if (!isIframe) {
                                                    parsedSources.add(StreamSource(finalUrl, sourceName, quality, emptyList(), mapOf("Referer" to referer, "Origin" to referer, "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:150.0) Gecko/20100101 Firefox/150.0")))
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    requests.awaitAll()
                }
            }
            
            if (parsedSources.isEmpty()) throw Exception("No compatible streams found. Try a different episode or server.")
            
            // Sort to put highest quality first, then Dub/Sub based on something if needed
            val sorted = parsedSources.toList().sortedByDescending { 
                var score = 0
                if (it.quality.contains("1080")) score += 1000
                else if (it.quality.contains("720")) score += 700
                else if (it.quality.contains("480")) score += 400
                if (it.title.contains("[Sub]")) score += 100
                score
            }
            Result.success(sorted)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}

