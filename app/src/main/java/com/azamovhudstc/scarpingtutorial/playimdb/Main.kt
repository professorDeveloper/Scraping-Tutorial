package com.azamovhudstc.scarpingtutorial.playimdb

import com.azamovhudstc.scarpingtutorial.utils.Utils
import com.azamovhudstc.scarpingtutorial.utils.Utils.getJsoup
import com.azamovhudstc.scarpingtutorial.utils.Utils.httpClient
import com.lagradost.nicehttp.Requests
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.regex.Pattern

data class Episode(
    val season: Int,
    val episode: Int,
    val title: String,
    val iframeUrl: String
)

fun getEpisodes(imdbId: String): Result<List<Episode>> = runCatching {
    val doc: Document = getJsoup("https://streamimdb.me/embed/$imdbId")
    val episodes = mutableListOf<Episode>()

    val epsDiv = doc.selectFirst("#eps")
    if (epsDiv != null && epsDiv.select(".ep").isNotEmpty()) {
        epsDiv.select(".ep").forEach { el ->
            val url = el.attr("data-iframe").trim()
            val titleText = el.text().ifBlank {
                val s = el.attr("data-s")
                val e = el.attr("data-e")
                "S${s.padStart(2, '0')}E${e.padStart(2, '0')}"
            }

            episodes.add(
                Episode(
                    season = el.attr("data-s").toIntOrNull() ?: 0,
                    episode = el.attr("data-e").toIntOrNull() ?: 0,
                    title = titleText,
                    iframeUrl = if (url.startsWith("http")) url else "https://streamimdb.me$url"
                )
            )
        }
    } else {
        val iframe = doc.selectFirst("iframe")
            ?: throw Exception("Iframe not found")

        val iframeSrc = iframe.attr("src").trim()
        val fixedUrl = when {
            iframeSrc.startsWith("http") -> iframeSrc
            iframeSrc.startsWith("//") -> "https:$iframeSrc"
            iframeSrc.startsWith("/") -> "https://streamimdb.me$iframeSrc"
            else -> "https://streamimdb.me/$iframeSrc"
        }

        val pageTitle = doc.selectFirst("title")?.text()?.trim() ?: "Movie"

        episodes.add(
            Episode(
                season = 0,
                episode = 0,
                title = pageTitle,
                iframeUrl = fixedUrl
            )
        )
    }

    episodes.sortedWith(compareBy({ it.season }, { it.episode }))
}

fun extractSeriesIframe(link: String): String? {
    val doc: Document = getJsoup(link)
    println(doc)
    val iframeSrc = doc.selectFirst("iframe#player_iframe")?.attr("src")

    if (iframeSrc != null) {
        val finalUrl = if (iframeSrc.startsWith("//")) {
            "https:$iframeSrc"
        } else iframeSrc

        return finalUrl
    }

    println("⚠️ iframe#player_iframe not found")
    return null
}

val headers = mapOf(
    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36",
    "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
    "Accept-Language" to "en-US,en;q=0.5",
    "Accept-Encoding" to "gzip, deflate, br",
    "Alt-Used" to "cloudnestra.com",
    "Connection" to "keep-alive",
    "Upgrade-Insecure-Requests" to "1",
    "Sec-Fetch-Dest" to "document",
    "Sec-Fetch-Mode" to "navigate",
    "Sec-Fetch-Site" to "none",
    "Sec-Fetch-User" to "?1",
    "TE" to "trailers"
)

suspend fun convertRcptProctor(iframeUrl: String): String = withContext(Dispatchers.IO) {

    val response = Jsoup.connect(iframeUrl)
        .method(Connection.Method.GET)
        .header(
            "accept",
            "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"
        )
        .header("accept-language", "en-US,en;q=0.9,uz-UZ;q=0.8,uz;q=0.7")
        .header("cache-control", "max-age=0")
        .header("dnt", "1")
        .header("priority", "u=0, i")
        .header(
            "sec-ch-ua",
            "\"Google Chrome\";v=\"141\", \"Not?A_Brand\";v=\"8\", \"Chromium\";v=\"141\""
        )
        .header("sec-ch-ua-mobile", "?0")
        .header("sec-ch-ua-platform", "\"Windows\"")
        .header("sec-fetch-dest", "document")
        .header("sec-fetch-mode", "navigate")
        .header("sec-fetch-site", "none")
        .header("sec-fetch-user", "?1")
        .header("upgrade-insecure-requests", "1")
        .header(
            "user-agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36"
        )
        .ignoreHttpErrors(true)
        .ignoreContentType(true)
        .execute()
    val document = Jsoup.parse(response.body())
    val scripts = document.select("script")

    var prorcpUrl: String? = null
    for (script in scripts) {
        val content = script.data()
        val regex = Regex("""src:\s*['"](/prorcp/[^'"]+)['"]""")
        val match = regex.find(content)
        if (match != null) {
            prorcpUrl = match.groupValues[1]
            break
        }
    }
    return@withContext "https://cloudnestra.com/$prorcpUrl" ?: ""

}


suspend fun extractDirectM3u8(iframeUrl: String): String {
    val response = Jsoup.connect(iframeUrl)
        .method(Connection.Method.GET)
        .header(
            "accept",
            "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"
        )
        .header("accept-language", "en-US,en;q=0.9,uz-UZ;q=0.8,uz;q=0.7")
        .header("cache-control", "max-age=0")
        .header("dnt", "1")
        .header(
            "user-agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome Safari"
        )
        .ignoreContentType(true)
        .timeout(15000)
        .execute()

    val html = response.body()

    // Regex orqali .m3u8 linkni topamiz
    val regex = Regex("""https?://[^\s'"]+\.m3u8""")
    val match = regex.find(html)

    return if (match != null) {
        val m3u8Url = match.value
        m3u8Url
    } else {
        "empty"
    }

}

fun main(args: Array<String>) {
    runBlocking {
        val id = "tt21909764"
        getEpisodes(id).onSuccess {
            val list = it
            convertRcptProctor(list[0].iframeUrl).let {
                extractDirectM3u8(it).let {
                    println(it)
                }
            }
//            extractSeriesIframe(list[0].iframeUrl)?.let {
//                convertRcptProctor(it).let {
//                }
//            }
        }
    }
}


