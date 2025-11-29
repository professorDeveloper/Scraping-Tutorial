package com.azamovhudstc.scarpingtutorial.xprime

import com.google.gson.Gson
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.*
import kotlin.math.abs

enum class MediaType {
    movies, tvShows
}

enum class StreamType {
    hls
}

data class MediaStream(
    val type: StreamType,
    val url: String,
    val headers: Map<String, String>? = null,
    val quality: String = "Auto",
    // subtitles and audios omitted for simplicity, add if needed
    val hasDefaultAudio: Boolean = true
)

data class StreamExtractorOptions(
    val tmdbId: Int,
    val season: Int? = null,
    val episode: Int? = null,
    val title: String,
    val releaseYear: String? = null,
    val imdbId: String? = null
) {
    init {
        require((season == null && episode == null) || (season != null && episode != null)) {
            "If one of season or episode is provided, both must be provided."
        }
    }
}

interface BaseStreamExtractor {
    val acceptedMediaTypes: List<MediaType>
    val needsExternalLink: Boolean
    suspend fun getExternalLink(options: StreamExtractorOptions): Map<String, Any?>?
    suspend fun getStreams(
        options: StreamExtractorOptions,
        externalLink: String? = null,
        externalLinkHeaders: Map<String, String>? = null
    ): List<MediaStream>
}

data class Episode(
    val season: Int,
    val episode: Int,
    val title: String,
    val pageUrl: String
)

data class Backdrop(val url: String)

data class SubtitleItem(val lang: String, val url: String)  // assuming

val commonHeaders = mapOf(
    "sec-ch-ua" to "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Microsoft Edge\";v=\"120\"",
    "sec-ch-ua-mobile" to "?0",
    "sec-ch-ua-platform" to "\"Windows\"",
    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0",
    "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
    "Accept-Language" to "en-US,en;q=0.5",
//    "Accept-Encoding" to "gzip, deflate, br",
    "Connection" to "keep-alive",
    "Upgrade-Insecure-Requests" to "1",
    "Sec-Fetch-Dest" to "document",
    "Sec-Fetch-Mode" to "navigate",
    "Sec-Fetch-Site" to "none",
    "Sec-Fetch-User" to "?1"
)

fun String.removeSpecialChars(): String {
    return this.replace(Regex("[^a-zA-Z0-9 -]"), "")
}

fun String.normalize(): String {
    return this.lowercase(Locale.getDefault())
}

fun getClosestResolutionFromDimensions(width: Int, height: Int): String {
    val resolutions = mapOf(
        "4K" to listOf(3840, 2160),
        "2K" to listOf(2560, 1440),
        "1080p" to listOf(1920, 1080),
        "720p" to listOf(1280, 720),
        "480p" to listOf(854, 480),
        "360p" to listOf(640, 360),
        "240p" to listOf(426, 240)
    )
    var closestName = ""
    var closestDistance = Double.MAX_VALUE
    for ((name, dims) in resolutions) {
        val w = dims[0]
        val h = dims[1]
        val distance = ((width - w) * (width - w) + (height - h) * (height - h)).toDouble()
        if (distance < closestDistance) {
            closestDistance = distance
            closestName = name
        }
    }
    return closestName
}

fun getClosestResolutionFromFileName(fileName: String): String {
    val normalized = fileName.normalize()
    return when {
        normalized.contains("4k") || normalized.contains("2160") -> "4K"
        normalized.contains("2k") || normalized.contains("1440") -> "2K"
        normalized.contains("1080") -> "1080p"
        normalized.contains("720") -> "720p"
        normalized.contains("480") -> "480p"
        normalized.contains("360") -> "360p"
        normalized.contains("240") -> "240p"
        else -> "Auto"
    }
}

fun getClosestResolutionFromBandwidth(bandwidth: Int): String {
    val resolutionBandwidths = mapOf(
        "4K" to 15000000,
        "2K" to 8000000,
        "1080p" to 5000000,
        "720p" to 2500000,
        "480p" to 1000000,
        "360p" to 750000,
        "240p" to 400000
    )
    var closestName = ""
    var closestDiff = Long.MAX_VALUE
    for ((name, bw) in resolutionBandwidths) {
        val diff = abs(bandwidth - bw).toLong()
        if (diff < closestDiff) {
            closestDiff = diff
            closestName = name
        }
    }
    return closestName
}

fun isAlreadyResolution(value: String): Boolean {
    return when (value) {
        "4K", "2160p", "2K", "1440p", "1080p", "720p", "480p", "360p", "240p" -> true
        else -> false
    }
}

suspend fun getTmdbData(
    type: String,
    tmdbId: Int? = null,
    title: String? = null,
    year: Int? = null,
    season: Int? = null
): Map<String, Any>? {
    var url = "https://jumpfreedom.com/3/"
    if (tmdbId != null) {
        url += "$type/$tmdbId?language=en-US"
        if (season != null && type == "tv") {
            url += "/season/$season"
        }
    } else if (title != null) {
        val query = title.replace(" ", "%20")
        url += "search/$type?query=$query&language=en-US"
        if (year != null) {
            url += "&year=$year"
        }
    } else {
        return null
    }
    try {
        val response = Jsoup.connect(url)
            .headers(commonHeaders)
            .ignoreContentType(true)
            .execute().body()
        return Gson().fromJson(response, HashMap::class.java) as Map<String, Any>?
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

suspend fun getTmdbId(title: String, isTv: Boolean, year: Int? = null): Int? {
    val type = if (isTv) "tv" else "movie"
    val data = getTmdbData(type, title = title, year = year) ?: return null
    val results = data["results"] as? List<Map<String, Any>> ?: return null
    for (result in results) {
        val rTitle = result["title"] as? String ?: result["name"] as? String ?: continue
        val rDate =
            result["release_date"] as? String ?: result["first_air_date"] as? String ?: continue
        val rYear = rDate.take(4).toIntOrNull() ?: continue
        if (rTitle.normalize() == title.normalize() && (year == null || rYear == year)) {
            return (result["id"] as? Double)?.toInt()
        }
    }
    return results.firstOrNull()?.let { (it["id"] as? Double)?.toInt() }
}

suspend fun getReleaseYear(tmdbId: Int, isTv: Boolean = false): String? {
    val type = if (isTv) "tv" else "movie"
    val data = getTmdbData(type, tmdbId) ?: return null
    val date = data["release_date"] as? String ?: data["first_air_date"] as? String ?: return null
    return date.take(4)
}

class HollyMovieExtractor : BaseStreamExtractor {

    private val providerKey = "semo_hollymovie"
    private val baseUrl = "https://hollymoviehd.cc"
    private val TAG = "HollyMovieExtractor"

    override val acceptedMediaTypes: List<MediaType> = listOf(MediaType.movies, MediaType.tvShows)

    override val needsExternalLink: Boolean = true

    override suspend fun getExternalLink(options: StreamExtractorOptions): Map<String, Any?>? {
        try {
            val isTv = options.season != null && options.episode != null
            var formattedTitle = options.title.removeSpecialChars().replace(" ", "-").normalize()
            var year = options.releaseYear
            if (!isTv && year == null) {
                year = getReleaseYear(options.tmdbId)
            }
            var path = "/$formattedTitle"
            if (isTv) {
                path = "/episode$path-season-${options.season}-episode-${options.episode}/"
            } else {
                if (year == null) throw Exception("Release year required for movies")
                path += "-$year/"
            }
            val pageUrl = baseUrl + path
            val doc: Document = Jsoup.connect(pageUrl)
                .headers(commonHeaders)
                .get()
            println(doc)

            val embedUrl =
                doc.selectFirst("iframe[src^=\"https://flashstream.cc/embed\"]")?.attr("src")
                    ?: throw Exception("No external link found for: $providerKey")
            return mapOf(
                "url" to embedUrl,
                "headers" to emptyMap<String, String>()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    override suspend fun getStreams(
        options: StreamExtractorOptions,
        externalLink: String?,
        externalLinkHeaders: Map<String, String>?
    ): List<MediaStream> {
        try {
            if (externalLink.isNullOrEmpty()) {
                throw Exception("External link is required for $providerKey")
            }
            val doc: Document = Jsoup.connect(externalLink)
                .headers(commonHeaders)
                .get()
            val scripts = doc.select("script")
            var streamsvrPath: String? = null
            for (script in scripts) {
                val content = script.data()
                val regex = Regex("""src:\s*['"](/streamsvr/[^'"]+)['"]""")
                val match = regex.find(content)
                if (match != null) {
                    streamsvrPath = match.groupValues[1]
                    break
                }
            }
            val streamsvrUrl = "https://flashstream.cc$streamsvrPath"
                ?: throw Exception("No streamsvr found for: $providerKey")
            val response = Jsoup.connect(streamsvrUrl)
                .method(Connection.Method.GET)
                .headers(commonHeaders)
                .header("Referer", externalLink)
                .ignoreContentType(true)
                .timeout(15000)
                .execute()
            val html = response.body()
            val regex = Regex("""https?://[^\s'"]+\.m3u8""")
            val match = regex.find(html)
            val m3u8Url = match?.value ?: throw Exception("No m3u8 URL found for: $providerKey")
            return listOf(
                MediaStream(
                    type = StreamType.hls,
                    url = m3u8Url,
                    headers = mapOf("Referer" to externalLink)
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }
}

suspend fun getDetails(season: Int, tmdbId: Int): ArrayList<Backdrop> {
    val data = getTmdbData("tv", tmdbId, season = season) ?: return ArrayList()
    val episodes = data["episodes"] as? List<Map<String, Any>> ?: return ArrayList()
    val stillPaths = episodes.mapNotNull { it["still_path"] as? String }
    return ArrayList(stillPaths.map { Backdrop("https://image.tmdb.org/t/p/w500/$it") })
}

suspend fun getSubTitleList(tmdbId: Int, season: Int, episode: Int): List<SubtitleItem> {
    val url = "https://sub.wyzie.ru/search?id=$tmdbId&season=$season&episode=$episode"
    try {
        val response = Jsoup.connect(url)
            .headers(commonHeaders)
            .ignoreContentType(true)
            .execute().body()
        val listType = object : com.google.gson.reflect.TypeToken<List<SubtitleItem>>() {}.type
        return Gson().fromJson(response, listType)
    } catch (e: Exception) {
        e.printStackTrace()
        return emptyList()
    }
}

fun main(args: Array<String>) {
    kotlinx.coroutines.runBlocking {
        // Movie example
        val tmdbId = getTmdbId("Oppenheimer", false, 2023) ?: 872585 // fallback
        val movieOptions = StreamExtractorOptions(tmdbId = tmdbId, title = "Oppenheimer")
        val extractor = HollyMovieExtractor()
        val external = extractor.getExternalLink(movieOptions)
        val externalLink = external?.get("url") as? String
        if (externalLink != null) {
            val streams = extractor.getStreams(movieOptions, externalLink)
            if (streams.isNotEmpty()) {
                println(streams[0].url)
            }
        }

        // Series example
        val seriesTmdbId = getTmdbId("The Boys", true) ?: 76479
        val seriesOptions = StreamExtractorOptions(
            tmdbId = seriesTmdbId,
            season = 1,
            episode = 1,
            title = "The Boys"
        )
        val seriesExternal = extractor.getExternalLink(seriesOptions)
        val seriesExternalLink = seriesExternal?.get("url") as? String
        if (seriesExternalLink != null) {
            val seriesStreams = extractor.getStreams(seriesOptions, seriesExternalLink)
            if (seriesStreams.isNotEmpty()) {
                println(seriesStreams[0].url)
            }
        }
    }
}