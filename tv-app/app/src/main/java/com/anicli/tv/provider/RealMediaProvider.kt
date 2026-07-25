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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealMediaProvider @Inject constructor(
    private val apiService: AnimeApiService,
    private val okHttpClient: okhttp3.OkHttpClient
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

            val variablesObj = mapOf(
                "showId" to mediaId,
                "translationType" to "sub",
                "episodeString" to epNumber
            )
            val extensionsObj = mapOf(
                "persistedQuery" to mapOf(
                    "version" to 1,
                    "sha256Hash" to queryHash
                ),
                "aaReq" to aaReq
            )
            
            val variablesStr = gson.toJson(variablesObj)
            val extensionsStr = gson.toJson(extensionsObj)
            
            val response = apiService.getWatchInfo(variablesStr, extensionsStr)
            val decryptedStreams = mutableListOf<String>()
            val data = response.data
            
            if (data?.tobeparsed != null) {
                // The API returned the encrypted stream directly at the root of data
                val decrypted = CryptoUtils.decryptAllAnimeStream(data.tobeparsed, dynamicKey.key)
                if (decrypted.isNotEmpty()) decryptedStreams.add(decrypted)
            } else {
                val sourceUrlsElem = data?.episode?.sourceUrls ?: run {
                    val errorMsg = response.errors?.firstOrNull()?.message ?: "No source URLs found. Data: $data"
                    throw Exception("API Error: $errorMsg")
                }
                
                // sourceUrls can be a string representing encrypted JSON or directly an array
                if (sourceUrlsElem.isJsonPrimitive && sourceUrlsElem.asJsonPrimitive.isString) {
                    val strValue = sourceUrlsElem.asString
                    // if it's not starting with "[", it might be the tobeparsed JSON wrapper
                    if (strValue.contains("tobeparsed")) {
                        val jsonObj = gson.fromJson(strValue, JsonObject::class.java)
                        val toBeParsed = jsonObj.get("tobeparsed")?.asString
                        if (toBeParsed != null) {
                            val decrypted = CryptoUtils.decryptAllAnimeStream(toBeParsed, dynamicKey.key)
                            decryptedStreams.add(decrypted)
                        }
                    } else {
                        // Try to decrypt just in case the string itself is the base64 payload
                        val decrypted = CryptoUtils.decryptAllAnimeStream(strValue, dynamicKey.key)
                        if (decrypted.isNotEmpty()) decryptedStreams.add(decrypted)
                    }
                } else if (sourceUrlsElem.isJsonArray) {
                    decryptedStreams.add(sourceUrlsElem.toString())
                }
            }
            
            if (decryptedStreams.isEmpty()) throw Exception("Decryption failed or stream not available")
            
            val parsedSources = mutableListOf<StreamSource>()
            
            for (streamRaw in decryptedStreams) {
                try {
                    var array: JsonArray? = null
                    val elem = gson.fromJson(streamRaw, com.google.gson.JsonElement::class.java)
                    
                    if (elem.isJsonArray) {
                        array = elem.asJsonArray
                    } else if (elem.isJsonObject) {
                        val obj = elem.asJsonObject
                        if (obj.has("episode")) {
                            val epObj = obj.getAsJsonObject("episode")
                            if (epObj.has("sourceUrls")) {
                                array = epObj.getAsJsonArray("sourceUrls")
                            }
                        } else if (obj.has("sourceUrls")) {
                            array = obj.getAsJsonArray("sourceUrls")
                        } else if (obj.has("links")) {
                            array = obj.getAsJsonArray("links")
                        }
                    }

                    if (array != null) {
                        for (i in 0 until array.size()) {
                            val obj = array.get(i).asJsonObject
                            val url = if (obj.has("link")) obj.get("link").asString else if (obj.has("url")) obj.get("url").asString else if (obj.has("sourceUrl")) obj.get("sourceUrl").asString else null
                            val quality = if (obj.has("resolutionStr")) obj.get("resolutionStr").asString else if (obj.has("height")) obj.get("height").asString else "Auto"
                            val sourceName = if (obj.has("sourceName")) obj.get("sourceName").asString else "Quality: ${quality}p"
                            
                            if (url != null) {
                                var finalUrl: String = url
                                if (url.contains("mp4upload.com")) {
                                    val extracted = com.anicli.tv.extractor.Mp4UploadExtractor.extract(url)
                                    if (extracted != null) {
                                        finalUrl = extracted
                                    }
                                } else if (url.startsWith("/apivtwo/")) {
                                    try {
                                        val clockUrl = "https://mkissa.net$url"
                                        val req = okhttp3.Request.Builder()
                                            .url(clockUrl)
                                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                                            .header("Referer", "https://mkissa.to/")
                                            .build()
                                        val resp = okhttp3.OkHttpClient().newCall(req).execute()
                                        val clockJson = resp.body?.string()
                                        if (clockJson != null) {
                                            val cObj = gson.fromJson(clockJson, com.google.gson.JsonObject::class.java)
                                            if (cObj.has("links")) {
                                                val linksArr = cObj.getAsJsonArray("links")
                                                if (linksArr != null && linksArr.size() > 0) {
                                                    val extractedLink = linksArr.get(0).asJsonObject.get("link")?.asString
                                                    if (extractedLink != null) {
                                                        finalUrl = extractedLink
                                                    }
                                                }
                                            }
                                        }
                                    } catch(e: Exception) { e.printStackTrace() }
                                } else if (url.contains("ok.ru")) {
                                    val extractedList = com.anicli.tv.extractor.OkRuExtractor.extract(url)
                                    for (ext in extractedList) {
                                        parsedSources.add(
                                            StreamSource(
                                                url = ext.second,
                                                title = "Ok.ru ${ext.first}",
                                                quality = ext.first,
                                                subtitles = emptyList(),
                                                headers = emptyMap() // ok.ru direct links don't require referer
                                            )
                                        )
                                    }
                                }
                                
                                val isIframe = finalUrl.contains("ok.ru") || 
                                               (finalUrl.contains("bysekoze.com") && finalUrl.contains("/e/")) ||
                                               finalUrl.contains("watchanime.uns.bio") ||
                                               (finalUrl.contains("mp4upload.com") && finalUrl.contains("embed-"))
                                               
                                val referer = if (finalUrl.contains("mp4upload.com")) {
                                    "https://www.mp4upload.com/"
                                } else {
                                    "https://mkissa.to"
                                }
                                
                                if (!isIframe) {
                                    parsedSources.add(
                                        StreamSource(
                                            url = finalUrl,
                                            title = sourceName,
                                            quality = quality,
                                            subtitles = emptyList(),
                                            headers = mapOf(
                                                "Referer" to referer,
                                                "Origin" to referer,
                                                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:150.0) Gecko/20100101 Firefox/150.0"
                                            )
                                        )
                                    )
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            if (parsedSources.isEmpty()) throw Exception("No compatible streams found. Try a different episode or server.")
            
            Result.success(parsedSources)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
