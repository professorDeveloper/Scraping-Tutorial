package com.azamovhudstc.scarpingtutorial.playimdb

import com.azamovhudstc.scarpingtutorial.helper.M3u8Helper
import com.azamovhudstc.scarpingtutorial.vidsrc.decryptMethods
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.httpsify
import kotlinx.coroutines.runBlocking
import org.jsoup.Jsoup
import java.net.URI


private suspend fun extractIframeUrl(url: String): String? {
    return httpsify(
        app.get(url).document.select("iframe").attr("src")
    ).takeIf { it.isNotEmpty() }
}

fun getBaseUrl(url: String): String {
    return URI(url).let { "${it.scheme}://${it.host}" }
}

private suspend fun extractProrcpUrl(iframeUrl: String): String? {
    val doc = app.get(iframeUrl, referer = iframeUrl).document
    val regex = Regex("src:\\s+'(.*?)'")
    val matchedSrc = regex.find(doc.html())?.groupValues?.get(1) ?: return null
    val host = getBaseUrl(iframeUrl)
    return host + matchedSrc
}

private suspend fun extractAndDecryptSource(
    prorcpUrl: String,
    referer: String
): List<Pair<String, String>>? {
    println(prorcpUrl)
    println(referer)
    val responseText = app.get(prorcpUrl, referer = referer).text
    val playerJsRegex = Regex("""Playerjs\(\{.*?file:"(.*?)".*?\}\)""")
    val temp = playerJsRegex.find(responseText)?.groupValues?.get(1)

    val encryptedURLNode = if (!temp.isNullOrEmpty()) {
        mapOf("id" to "playerjs", "content" to temp)
    } else {
        val document = Jsoup.parse(responseText)
        val reporting = document.selectFirst("#reporting_content") ?: return null
        val node = reporting.nextElementSibling() ?: return null
        mapOf("id" to node.attr("id"), "content" to node.text())
    }

    val id = encryptedURLNode["id"] ?: return null
    val content = encryptedURLNode["content"] ?: return null

    val decrypted = decryptMethods[id]?.invoke(content) ?: return null

    // Domain mapping
    val vSubs = mapOf(
        "v1" to "shadowlandschronicles.com",
        "v2" to "cloudnestra.com",
        "v3" to "thepixelpioneer.com",
        "v4" to "putgate.org",
    )
    val placeholderRegex = "\\{(v\\d+)\\}".toRegex()
    val mirrors: List<Pair<String, String>> = decrypted
        .split(" or ")
        .map { it.trim() }
        .filter { it.startsWith("http") }
        .map { rawUrl ->
            val match = placeholderRegex.find(rawUrl)
            val version = match?.groupValues?.get(1) ?: ""
            val domain = vSubs[version] ?: ""
            val finalUrl = if (domain.isNotEmpty()) {
                placeholderRegex.replace(rawUrl) { domain }
            } else {
                rawUrl
            }

            version to finalUrl
        }

    return mirrors.ifEmpty { null }
}

const val Vidsrcxyz = "https://vidsrc-embed.su"
suspend fun invokeVidSrcXyz(
    id: String? = null,
    season: Int? = null,
    episode: Int? = null,
): String {
    val url = if (season == null) {
        "$Vidsrcxyz/embed/movie?imdb=$id"
    } else {
        "$Vidsrcxyz/embed/tv?imdb=$id&season=$season&episode=$episode"
    }
    val iframeUrl = extractIframeUrl(url) ?: return ""
    val prorcpUrl = extractProrcpUrl(iframeUrl) ?: "Not Found 2"
    val decryptedSource = extractAndDecryptSource(prorcpUrl, iframeUrl) ?: return ""
    decryptedSource.forEach {
        println("Link:${it.second}")
    }
    return decryptedSource.get(0).second

}

fun main(args: Array<String>) {
    runBlocking {
        invokeVidSrcXyz(id = "tt26443597").let {
            println(it)
        }
    }
}