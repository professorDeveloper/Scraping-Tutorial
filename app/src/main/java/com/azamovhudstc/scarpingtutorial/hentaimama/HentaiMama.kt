package com.azamovhudstc.scarpingtutorial.hentaimama

import android.annotation.SuppressLint
import com.azamovhudstc.scarpingtutorial.anime_pahe.ShowResponse
import com.azamovhudstc.scarpingtutorial.utils.Utils
import com.azamovhudstc.scarpingtutorial.utils.parser
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lagradost.nicehttp.Requests
import kotlinx.coroutines.runBlocking
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.TlsVersion
import org.slf4j.helpers.Util
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSession

data class VideoServer(
    val url: String,
    val index: String,
)

data class Episode(val link: String, val thumb: String, val num: String)
class HentaiMama {
    val name = "Hentaimama"
    val saveName = "hentai_mama"
    val hostUrl = "https://hentaimama.io"
    val isDubAvailableSeparately = false
    val isNSFW = true

    val okClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header(
                    "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) " +
                            "Chrome/124.0 Safari/537.36"
                )
                .build()
            chain.proceed(request)
        }
        .apply {
            // Create a trust manager that accepts all certificates
            val trustAllCerts = arrayOf<TrustManager>(@SuppressLint("CustomX509TrustManager")
            object : X509TrustManager {
                @SuppressLint("TrustAllX509TrustManager")
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                @SuppressLint("TrustAllX509TrustManager")
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            // Install the all-trusting trust manager
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())

            sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            hostnameVerifier { _, _ -> true }

            // Add connection specs for better compatibility
            connectionSpecs(listOf(
                ConnectionSpec.MODERN_TLS,
                ConnectionSpec.COMPATIBLE_TLS,
                ConnectionSpec.CLEARTEXT
            ))

            connectTimeout(30, TimeUnit.SECONDS)
            readTimeout(30, TimeUnit.SECONDS)
            writeTimeout(30, TimeUnit.SECONDS)
        }
        .build()

    val client = Requests(okClient, responseParser = parser)
    suspend fun loadEpisodes(
        animeLink: String,
        extra: Map<String, String>?
    ): List<com.azamovhudstc.scarpingtutorial.hentaimama.Episode> {
        val pageBody = Utils.getJsoup(animeLink)

        val episodes =
            pageBody.select("div#episodes.sbox.fixidtab div.module.series div.content.series div.items article")
                .reversed()
                .map { article ->
                    // Extract episode number from the h3 text
                    val epNum = article.select("div.data h3").text().replace("Episode", "").trim()

                    // Extract episode URL from the season_m div (remove .animation-3 class)
                    val url = article.select("div.poster div.season_m a").attr("href")

                    // Extract thumbnail from img data-src attribute
                    val thumb = article.select("div.poster img").attr("data-src")

                    com.azamovhudstc.scarpingtutorial.hentaimama.Episode(link = url, thumb, epNum)
                }

        return episodes
    }

    suspend fun loadVideoServers(
        episodeLink: String,
        extra: Map<String, String>?
    ): List<VideoServer> {
        val animeId = client.get(episodeLink).document
            .select("#post_report > input:nth-child(5)")
            .attr("value")

        // Make POST request to get player contents
        val response = client.post(
            "https://hentaimama.io/wp-admin/admin-ajax.php",
            data = mapOf(
                "action" to "get_player_contents",
                "a" to animeId
            )
        ).text

        // Parse JSON response
        val gson = Gson()
        val listType = object : TypeToken<List<String>>() {}.type
        val videoUrls: List<String> = gson.fromJson(response, listType)

        // Convert to VideoServer objects
        val videoServers = videoUrls.mapIndexed { index, url ->
            println(url.extractIframeSrc())
            VideoServer(url.extractIframeSrc() ?: "", "Mirror $index")
        }

        return videoServers
    }

    //     suspend fun getVideoExtractor(server: VideoServer): VideoExtractor = HentaiMamaExtractor(server)
    fun String.extractIframeSrc(): String? {
        val srcPattern = """src="([^"]+)"""".toRegex()
        return srcPattern.find(this)?.groupValues?.get(1)
    }

    class HentaiMamaExtractor() {
        val hostUrl = "https://hentaimama.io"

        val client = Requests(Utils.httpClient, responseParser = parser)

        suspend fun search(query: String): List<ShowResponse> {
            val url = "$hostUrl/?s=${query.replace(" ", "+")}"
            val document = client.get(url).document

            return document.select("div.result-item article").map {
                val link = it.select("div.details div.title a").attr("href")
                val title = it.select("div.details div.title a").text()
                val cover = it.select("div.image div a img").attr("src")
                ShowResponse(title, link, cover)
            }
        }

        suspend fun extract(server: VideoServer): VideoContainer {
            val doc = client.get(server.url)

            doc.document.selectFirst("video>source")?.attr("src")?.let { directSrc ->
                return VideoContainer(
                    listOf(
                        Video(null, VideoType.CONTAINER, directSrc, getSize(directSrc))
                    )
                )
            }

            // Extract sources from JavaScript
            val unSanitized =
                doc.text.findBetween("sources: [", "],") ?: return VideoContainer(listOf())

            // Sanitize the JSON string
            val sanitizedJson = "[${
                unSanitized
                    .replace("type:", "\"type\":")
                    .replace("file:", "\"file\":")
            }]"

            // Parse JSON using Gson
            val gson = Gson()
            val listType = object : TypeToken<List<ResponseElement>>() {}.type
            val json: List<ResponseElement> = gson.fromJson(sanitizedJson, listType)

            // Convert to Video objects
            val videos = json.map { element ->
                if (element.type == "hls")
                    Video(null, VideoType.M3U8, element.file, null)
                else
                    Video(null, VideoType.CONTAINER, element.file, getSize(element.file))
            }

            return VideoContainer(videos)
        }

        data class ResponseElement(
            val type: String,
            val file: String
        )

        // Additional data classes you'll need:
        data class VideoContainer(
            val videos: List<Video>
        )

        data class Video(
            val id: String?,
            val type: VideoType,
            val url: String,
            val size: Long?
        )

        enum class VideoType {
            M3U8,
            CONTAINER
        }

        // Extension function for string manipulation
        fun String.findBetween(start: String, end: String): String? {
            val startIndex = this.indexOf(start)
            if (startIndex == -1) return null

            val actualStart = startIndex + start.length
            val endIndex = this.indexOf(end, actualStart)
            if (endIndex == -1) return null

            return this.substring(actualStart, endIndex)
        }

        // Placeholder for getSize function
        fun getSize(url: String): Long? {
            // Implement your logic to get video file size
            return null
        }
    }

}

fun main(args: Array<String>) {
    runBlocking {
        val extractor = HentaiMama.HentaiMamaExtractor()
        val parser = HentaiMama()
        val item = extractor.search("Night").first()
        println(item.link)
        val episodes = parser.loadEpisodes(item.link ?: "", item.extra)
        val videoServers = parser.loadVideoServers(episodes.get(0).link, null)
        extractor.extract(
            videoServers.get(0)
        ).videos.onEach {
            println(it.url)
            println(it.size)
            println(it.type)
        }
    }
}
