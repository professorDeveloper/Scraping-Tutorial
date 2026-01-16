package com.azamovhudstc.scarpingtutorial.streamflix

import com.azamovhudstc.scarpingtutorial.utils.Utils
import com.lagradost.cloudstream3.USER_AGENT
import java.net.URI

open class DoodLaExtractor : Extractor() {

    override val name = "DoodStream"
    override val mainUrl = "https://dood.la"
    override val aliasUrls = listOf(
        "https://dsvplay.com",
        "https://mikaylaarealike.com",
        "https://myvidplay.com"
    )

    private val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

    override suspend fun extract(link: String): Video {
        // Embed URL ga o'tkazish
        val embedUrl = link.replace("/d/", "/e/")

        // Headers ni tayyorlash
        val headers = mapOf(
            "User-Agent" to USER_AGENT,
            "Referer" to link,
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "Accept-Language" to "en-US,en;q=0.5"
        )

        // Birinchi sahifani olish
        val documentHtml = Utils.get(embedUrl, headers)

        // MD5 URL ni topish
        val md5Regex = Regex("/pass_md5/[^']*")
        val md5Path = md5Regex.find(documentHtml)?.value
            ?: throw Exception("Can't find md5")

        val baseUrl = getBaseUrl(embedUrl)
        val md5Url = baseUrl + md5Path

        // MD5 URL dan javob olish
        val response = Utils.get(md5Url, headers)

        // Video URL ni yaratish
        val hashTable = createHashTable()
        val videoUrl = response.trim() + hashTable + "?token=${md5Path.substringAfterLast("/")}"

        return Video(
            source = videoUrl,
            headers = mapOf(
                "User-Agent" to USER_AGENT,
                "Referer" to mainUrl
            )
        )
    }

    private fun createHashTable(): String {
        return buildString {
            repeat(10) {
                append(alphabet.random())
            }
        }
    }

    private fun getBaseUrl(url: String): String {
        return try {
            val uri = URI(url)
            "${uri.scheme}://${uri.host}"
        } catch (e: Exception) {
            // Agar URI parse qilishda muammo bo'lsa, oddiy string usulida
            if (url.startsWith("http://")) {
                url.substringBefore("/", "")
            } else if (url.startsWith("https://")) {
                val afterProtocol = url.substringAfter("https://")
                "https://" + afterProtocol.substringBefore("/")
            } else {
                "https://" + url.substringBefore("/")
            }
        }
    }

    class DoodLiExtractor : DoodLaExtractor() {
        override var mainUrl = "https://dood.li"
    }

    class DoodExtractor : DoodLaExtractor() {
        override val mainUrl = "https://vide0.net"
    }
}