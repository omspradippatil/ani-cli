package com.anicli.tv.provider

import com.anicli.tv.data.remote.AnimeApiService
import com.anicli.tv.data.remote.CryptoUtils
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

    private val SEARCH_GQL = """query( ${'$'}search: SearchInput ${'$'}limit: Int ${'$'}page: Int ${'$'}translationType: VaildTranslationTypeEnumType ${'$'}countryOrigin: VaildCountryOriginEnumType ) { shows( search: ${'$'}search limit: ${'$'}limit page: ${'$'}page translationType: ${'$'}translationType countryOrigin: ${'$'}countryOrigin ) { edges { _id name thumbnail availableEpisodes airedStart __typename } }}"""
    private val DETAILS_GQL = """query (${'$'}showId: String!) { show( _id: ${'$'}showId ) { _id thumbnail availableEpisodesDetail }}"""
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
                                title = edge.name,
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
                    title = "Anime", // We don't have the title in this specific query, the UI usually holds state from the item click
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
                        title = edge.name,
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
            
            val dynamicKey = CryptoUtils.fetchDynamicKey(okHttpClient)
            
            val queryHash = "f4662f4b7510b26795dd53ef824a0bf1740fbbc5d1273fab18222ac831bca8d0"
            val aaReq = CryptoUtils.generateAaReq(dynamicKey.key, dynamicKey.epoch, queryHash)

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
            val sourceUrlsElem = response.data?.episode?.sourceUrls ?: run {
                val errorMsg = response.errors?.firstOrNull()?.message ?: "No source URLs found. Data: ${response.data}"
                throw Exception("API Error: $errorMsg")
            }
            
            val decryptedStreams = mutableListOf<String>()
            
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
            
            if (decryptedStreams.isEmpty()) throw Exception("Decryption failed or stream not available")
            
            val parsedSources = mutableListOf<StreamSource>()
            
            for (streamRaw in decryptedStreams) {
                try {
                    // it should be an array of objects like [{"url": "...", "height": 1080}]
                    // occasionally it's an array of {"sourceUrl": "..."}
                    val array = gson.fromJson(streamRaw, JsonArray::class.java)
                    for (i in 0 until array.size()) {
                        val obj = array.get(i).asJsonObject
                        val url = if (obj.has("link")) obj.get("link").asString else if (obj.has("url")) obj.get("url").asString else if (obj.has("sourceUrl")) obj.get("sourceUrl").asString else null
                        val quality = if (obj.has("resolutionStr")) obj.get("resolutionStr").asString else if (obj.has("height")) obj.get("height").asString else "Auto"
                        
                        if (url != null) {
                            parsedSources.add(
                                StreamSource(
                                    url = url,
                                    title = "Quality: ${quality}p",
                                    quality = quality,
                                    subtitles = emptyList(),
                                    headers = mapOf(
                                        "Referer" to "https://mkissa.to",
                                        "Origin" to "https://mkissa.to",
                                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:150.0) Gecko/20100101 Firefox/150.0"
                                    )
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            Result.success(parsedSources)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
