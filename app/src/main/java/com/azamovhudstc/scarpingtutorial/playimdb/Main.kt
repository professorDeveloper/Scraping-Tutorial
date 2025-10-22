package com.azamovhudstc.scarpingtutorial.playimdb

import com.azamovhudstc.scarpingtutorial.utils.Utils
import com.azamovhudstc.scarpingtutorial.utils.Utils.getJsoup
import com.azamovhudstc.scarpingtutorial.utils.Utils.httpClient
import com.lagradost.nicehttp.Requests
import kotlinx.coroutines.runBlocking
import org.jsoup.nodes.Document

data class Episode(
    val season: Int,
    val episode: Int,
    val title: String,
    val iframeUrl: String
)

fun getEpisodes(imdbId: String): Result<List<Episode>> = runCatching {
    val doc: Document = Utils.getJsoup("https://streamimdb.me/embed/$imdbId")
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
    val doc: Document = Utils.getJsoup(link)
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

suspend fun convertRcptProctor(iframeUrl: String) {
    val request = Requests(
        baseClient = httpClient, defaultHeaders = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:101.0) Gecko/20100101 Firefox/101.0",

            )
    )
    val scripts = request.get(iframeUrl).document.getElementsByTag("script")

    val regex = Regex("""src:\s*'(/prorcp/[^']+)'""")

    var srcValue: String? = null

    for (script in scripts) {
        val match = regex.find(script.data())
        if (match != null) {
            srcValue = match.groups[1]?.value
            break
        }
    }

    if (srcValue != null) {
        val fullUrl =
            if (srcValue.startsWith("http")) srcValue else "https://streamimdb.me$srcValue"

        println("✅ Extracted iframe source URL:")
        println(fullUrl)
    } else {
        println("⚠️ src not found in scripts!")
    }
}

fun main() = runBlocking {
    val id = "tt0903747"
    var count = 0
    getEpisodes(id).onSuccess { eps ->
        eps.forEach {
            println("${it.title} -> ${it.iframeUrl}")
            count += 1
        }
        val iframeUrl = extractSeriesIframe(
            eps.find { it.title == "S03E11: Breaking Bad" }?.iframeUrl ?: ""

        )
        println(iframeUrl)
        convertRcptProctor(iframeUrl ?: "")

    }.onFailure {
        println("Error: ${it.message}")
    }
    println(count)
}


