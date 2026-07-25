package com.anicli.tv.data.remote.dto

import com.google.gson.JsonElement

// --- Requests ---
data class GraphQLRequest(
    val variables: Map<String, Any>,
    val query: String
)

// --- Responses ---
data class GraphQLResponse<T>(
    val data: T?,
    val errors: List<GraphQLError>? = null
)

data class GraphQLError(
    val message: String
)

// 1. Search Query Data
data class SearchData(
    val shows: ShowsNode?
)

data class ShowsNode(
    val edges: List<ShowEdge>?
)

data class ShowEdge(
    val _id: String,
    val name: String,
    val englishName: String? = null,
    val thumbnail: String? = null,
    val availableEpisodes: Map<String, Any>? = null,
    val airedStart: Map<String, Any>? = null
)

// 2. Show Episodes List Data
data class ShowData(
    val show: ShowDetailNode?
)

data class ShowDetailNode(
    val _id: String,
    val name: String? = null,
    val englishName: String? = null,
    val thumbnail: String? = null,
    val availableEpisodesDetail: Map<String, List<String>>? = null
)

// 3. Episode Stream Data
data class EpisodeData(
    val episode: EpisodeNode?,
    val tobeparsed: String? = null
)

data class EpisodeNode(
    val episodeString: String,
    val sourceUrls: JsonElement? // Can be an encrypted string containing "tobeparsed" or a JsonArray
)

// Stream Decrypted format
data class DecryptedStream(
    val url: String,
    val hls: Boolean = false,
    val mp4: Boolean = false,
    val resolutionStr: String = ""
)
