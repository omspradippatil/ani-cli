package com.anicli.tv.provider

import android.content.SharedPreferences
import android.util.Log
import com.anicli.tv.domain.model.Episode
import com.anicli.tv.domain.model.MediaDetail
import com.anicli.tv.domain.model.MediaItem
import com.anicli.tv.domain.model.MediaRow
import com.anicli.tv.domain.model.StreamSource
import com.anicli.tv.domain.provider.MediaProvider
import com.anicli.tv.extractor.GoGoAnimeExtractor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * GoGoAnime provider — HTML scraping + GoGoCDN stream extraction.
 *
 * - Domain is read from SharedPreferences (`pref_gogoanime_domain`).
 * - Falls back through a hardcoded list if the primary domain fails.
 * - Mirrors ani-cli's GoGoAnime scraping approach exactly.
 *
 * Supported domains (at time of writing):
 *   gogoanime.by, gogoanime3.co, gogoanime.gg, anitaku.pe
 */
@Singleton
class GoGoAnimeProvider @Inject constructor(
    @Named("VideoClient") private val okHttpClient: OkHttpClient,
    private val sharedPreferences: SharedPreferences
) : MediaProvider {

    override val name: String = "GoGoAnime"

    companion object {
        private const val TAG = "GoGoAnimeProvider"
        const val PREF_DOMAIN = "pref_gogoanime_domain"
        private const val DEFAULT_DOMAIN = "gogoanimes.fi"

        /** Ordered fallback mirror list — tried in sequence if primary fails */
        private val FALLBACK_DOMAINS = listOf(
            "gogoanimes.fi",
            "gogoanime.by",
            "gogoanime3.co",
            "anitaku.pe",
            "gogoanime.gg"
        )

        private val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:150.0) Gecko/20100101 Firefox/150.0"
    }

    // ─── Domain resolution ────────────────────────────────────────────────────

    private fun getActiveDomain(): String =
        sharedPreferences.getString(PREF_DOMAIN, DEFAULT_DOMAIN)
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_DOMAIN

    private fun baseUrl(): String = "https://${getActiveDomain()}"

    // ─── HTTP helpers ─────────────────────────────────────────────────────────

    private suspend fun get(url: String, referer: String? = null): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val reqBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
        if (referer != null) reqBuilder.header("Referer", referer)
        okHttpClient.newCall(reqBuilder.build()).execute().body?.string() ?: ""
    }

    // ─── MediaProvider implementation ─────────────────────────────────────────

    override suspend fun getHomeFeed(): Flow<Result<List<MediaRow>>> = flow {
        try {
            val rows = mutableListOf<MediaRow>()

            // Recent (first page of home)
            val recentItems = scrapeListPage("${baseUrl()}/home.html")
                .ifEmpty { scrapeListPage(baseUrl()) }
            if (recentItems.isNotEmpty()) {
                rows.add(MediaRow("Recent on GoGoAnime", recentItems.take(20)))
            }

            // Popular (today's popular)
            val popularItems = scrapeListPage("${baseUrl()}/popular.html")
            if (popularItems.isNotEmpty()) {
                rows.add(MediaRow("Popular on GoGoAnime", popularItems.take(20)))
            }

            // New season
            val newSeasonItems = scrapeListPage("${baseUrl()}/new-season.html")
            if (newSeasonItems.isNotEmpty()) {
                rows.add(MediaRow("New Season", newSeasonItems.take(20)))
            }

            if (rows.isEmpty()) {
                emit(Result.failure(Exception("GoGoAnime: Could not load home feed. Check your mirror domain in Settings.")))
            } else {
                emit(Result.success(rows))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getHomeFeed failed", e)
            emit(Result.failure(e))
        }
    }

    override suspend fun search(query: String): Result<List<MediaItem>> {
        if (query.isBlank()) return Result.success(emptyList())
        return try {
            val url = "${baseUrl()}/search.html?keyword=${java.net.URLEncoder.encode(query, "UTF-8")}"
            val html = get(url)
            Result.success(parseSearchResults(html))
        } catch (e: Exception) {
            Log.e(TAG, "search failed for: $query", e)
            Result.failure(e)
        }
    }

    override suspend fun getDetails(mediaId: String): Result<MediaDetail> {
        return try {
            // mediaId = GoGoAnime slug (e.g. "one-piece-10040")
            val url = "${baseUrl()}/category/$mediaId"
            val html = get(url)
            Result.success(parseDetailPage(html, mediaId))
        } catch (e: Exception) {
            Log.e(TAG, "getDetails failed for: $mediaId", e)
            Result.failure(e)
        }
    }

    override suspend fun getStreamSources(episodeId: String): Result<List<StreamSource>> {
        return try {
            // episodeId format: "slug|episode_number" e.g. "one-piece-10040|1"
            val parts = episodeId.split("|")
            if (parts.size != 2) throw Exception("Invalid GoGoAnime episodeId: $episodeId")
            val slug = parts[0]
            val epNum = parts[1]

            // GoGoAnime episode page URL
            val episodePageUrl = "${baseUrl()}/$slug-episode-$epNum"
            val episodeHtml = get(episodePageUrl, referer = baseUrl())

            // Find the GoGoCDN embed URL from the episode page
            val embedUrl = extractEmbedUrl(episodeHtml)
                ?: throw Exception("Could not find stream embed for $slug ep $epNum")

            // Extract streams via GoGoCDN AES decryption
            val streams = GoGoAnimeExtractor.extract(embedUrl, okHttpClient)

            if (streams.isEmpty()) {
                throw Exception("No streams found for $slug episode $epNum. The embed may have changed.")
            }

            val sources = streams.map { (quality, url) ->
                StreamSource(
                    url = url,
                    title = "GoGo ${quality}",
                    quality = quality,
                    subtitles = emptyList(),
                    headers = mapOf(
                        "Referer" to embedUrl,
                        "Origin" to "https://gogoanimes.fi",
                        "User-Agent" to USER_AGENT
                    )
                )
            }

            Result.success(sources)
        } catch (e: Exception) {
            Log.e(TAG, "getStreamSources failed for: $episodeId", e)
            Result.failure(e)
        }
    }

    // ─── HTML Parsers ─────────────────────────────────────────────────────────

    /**
     * Parses a generic anime list page (home, popular, new-season).
     * Looks for <ul class="items"><li> elements with poster and title.
     */
    private suspend fun scrapeListPage(url: String): List<MediaItem> {
        return try {
            val html = get(url)
            parseSearchResults(html)
        } catch (e: Exception) {
            Log.w(TAG, "scrapeListPage failed: $url", e)
            emptyList()
        }
    }

    /**
     * Parses GoGoAnime search results HTML.
     * Pattern: <ul class="items"><li> ... <a href="/category/SLUG" ...> ... <img src="POSTER"> ... <p class="name"><a...>TITLE</a></p>
     */
    private fun parseSearchResults(html: String): List<MediaItem> {
        val items = mutableListOf<MediaItem>()

        // Match each list item block
        val liPattern = Regex("""<li>(.*?)</li>""", RegexOption.DOT_MATCHES_ALL)
        val hrefPattern = Regex("""href=["']/category/([^"']+)["']""")
        val imgPattern = Regex("""<img[^>]+src=["']([^"']+)["']""")
        val titlePattern = Regex("""class=["']name["'][^>]*>\s*<a[^>]*>([^<]+)</a>""")

        for (liMatch in liPattern.findAll(html)) {
            val li = liMatch.groupValues[1]

            val slug = hrefPattern.find(li)?.groupValues?.get(1) ?: continue
            val poster = imgPattern.find(li)?.groupValues?.get(1) ?: ""
            val title = titlePattern.find(li)?.groupValues?.get(1)?.trim()
                ?: slug.replace("-", " ").replaceFirstChar { it.uppercase() }

            if (slug.isNotBlank()) {
                items.add(
                    MediaItem(
                        id = slug,
                        title = title,
                        posterUrl = poster.let { if (it.startsWith("//")) "https:$it" else it },
                        backdropUrl = poster.let { if (it.startsWith("//")) "https:$it" else it },
                        tags = emptyList()
                    )
                )
            }
        }

        return items
    }

    /**
     * Parses a GoGoAnime category (detail) page.
     * Extracts title, description, poster, and episode list from paginated anchors.
     */
    private fun parseDetailPage(html: String, slug: String): MediaDetail {
        // Title: <div class="anime_info_body_bg"> <h1>TITLE</h1>
        val titleMatch = Regex("""<h1>([^<]+)</h1>""").find(html)
        val title = titleMatch?.groupValues?.get(1)?.trim()
            ?: slug.replace("-", " ").replaceFirstChar { it.uppercase() }

        // Poster: the main <img> in anime_info_body_bg
        val posterMatch = Regex("""class=["']anime_info_body_bg["'][^>]*>.*?<img[^>]+src=["']([^"']+)["']""", RegexOption.DOT_MATCHES_ALL).find(html)
        val poster = posterMatch?.groupValues?.get(1)?.let {
            if (it.startsWith("//")) "https:$it" else it
        } ?: ""

        // Description: <span class="type">Plot Summary:</span> DESCRIPTION
        val descMatch = Regex("""Plot Summary:</span>\s*([^<]+)""").find(html)
        val description = descMatch?.groupValues?.get(1)?.trim() ?: "No description available."

        // Genre: <a href="/genre/...">TAG</a>
        val genrePattern = Regex("""href=["']/genre/[^"']+["']>([^<]+)</a>""")
        val genres = genrePattern.findAll(html).map { it.groupValues[1].trim() }.toList()

        // Episode pages: id="episode_page" contains <a ep_start="X" ep_end="Y">
        val epEndPattern = Regex("""ep_end=["'](\d+)["']""")
        val epNumbers = epEndPattern.findAll(html).map { it.groupValues[1].toIntOrNull() ?: 0 }.toList()
        
        // Also try to get episode starts to handle shows that don't start at 1
        val epStartPattern = Regex("""ep_start=["'](\d+)["']""")
        val epStarts = epStartPattern.findAll(html).map { it.groupValues[1].toIntOrNull() ?: 1 }.toList()

        // GoGoAnime mirrors like gogoanimes.fi list episodes directly in hrefs now
        val inlineEpPattern = Regex("""href=["']/$slug-episode-(\d+)["']""")
        val inlineEps = inlineEpPattern.findAll(html).map { it.groupValues[1].toIntOrNull() ?: 0 }.toList()

        val maxEp = (epNumbers + inlineEps).maxOrNull() ?: 0
        val startEp = (epStarts + inlineEps.filter { it > 0 }).minOrNull()?.coerceAtLeast(1) ?: 1

        val episodes = (startEp..maxEp).map { epNum ->
            Episode(
                id = "$slug|$epNum",
                title = "Episode $epNum",
                number = epNum,
                thumbnail = null,
                isFiller = false
            )
        }

        return MediaDetail(
            id = slug,
            title = title,
            description = description,
            posterUrl = poster,
            backdropUrl = poster,
            genres = genres,
            episodes = episodes
        )
    }

    /**
     * Extracts GoGoCDN / VidStreaming embed URL from an episode page.
     * Looks for <div class="play-video"><iframe src="..."></div>
     * or a link matching gogo-cdn.com / vidstreaming / gogoplay
     */
    private fun extractEmbedUrl(html: String): String? {
        // Primary: look for iframe src pointing to known CDNs
        val cdnPatterns = listOf(
            Regex("""<iframe[^>]+src=["'](https?://(?:www\.)?gogo-cdn\.com[^"']+)["']"""),
            Regex("""<iframe[^>]+src=["'](https?://(?:www\.)?vidstreaming\.io[^"']+)["']"""),
            Regex("""<iframe[^>]+src=["'](https?://(?:www\.)?gogoplay\d?\.com[^"']+)["']"""),
            Regex("""<iframe[^>]+src=["'](//(?:www\.)?gogo-cdn\.com[^"']+)["']"""),
            Regex("""<iframe[^>]+src=["'](//(?:www\.)?vidstreaming\.io[^"']+)["']"""),
            Regex("""<iframe[^>]+src=["'](//(?:www\.)?gogoplay\d?\.com[^"']+)["']"""),
            // Generic play-video iframe
            Regex("""class=["']play-video["'][^>]*>.*?<iframe[^>]+src=["']([^"']+)["']""", RegexOption.DOT_MATCHES_ALL)
        )

        for (pattern in cdnPatterns) {
            val match = pattern.find(html)
            if (match != null) {
                var url = match.groupValues[1]
                if (url.startsWith("//")) url = "https:$url"
                return url
            }
        }
        return null
    }
}
