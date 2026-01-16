package com.azamovhudstc.scarpingtutorial.streamflix

import com.azamovhudstc.scarpingtutorial.utils.Utils
import com.lagradost.cloudstream3.USER_AGENT
import java.net.URL

class VoeExtractor : Extractor() {

    override val name = "VOE"
    override val mainUrl = "https://voe.sx"
    override val aliasUrls = listOf(
        "https://jilliandescribecompany.com",
        "https://mikaylaarealike.com",
        "https://christopheruntilpoint.com",
        "https://walterprettytheir.com",
        "https://crystaltreatmenteast.com"
    )

    override suspend fun extract(link: String): Video {
        // URL ni tahlil qilish
        val parsedUrl = URL(link)
        val originalPath =
            parsedUrl.path + if (parsedUrl.query != null) "?${parsedUrl.query}" else ""

        // Asosiy URL ni aniqlash (mainUrl yoki alias)
        val baseUrl = if (link.startsWith(mainUrl)) {
            mainUrl
        } else {
            aliasUrls.firstOrNull { link.startsWith(it) } ?: mainUrl
        }

        // Birinchi sahifani olish uchun headers
        val headers = mapOf(
            "User-Agent" to USER_AGENT,
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "Accept-Language" to "en-US,en;q=0.5",
            "Referer" to baseUrl
        )

        // Birinchi sahifani olish (redirect domain aniqlash uchun)
        val firstPageUrl = baseUrl + originalPath
        val firstPageHtml = Utils.get(firstPageUrl, headers)

        // Redirect domain ni aniqlash
        val regex = Regex("""https://([a-zA-Z0-9.-]+)(?:/[^'"]*)?""")
        val match = regex.find(firstPageHtml)
        val redirectBaseUrl = if (match != null) {
            "https://${match.groupValues[1]}/"
        } else {
            throw Exception("Base url not found for VOE")
        }
        val finalUrl = redirectBaseUrl + originalPath
        val finalPageHtml = Utils.get(finalUrl, headers)
        println(finalUrl)
        val encodedString = DecryptHelper.findEncodedRegex(finalPageHtml)

        // Script tag dan ma'lumot olish
        val scriptTagRegex = Regex(
            """<script\s+type="application/json">(.*?)</script>""",
            RegexOption.DOT_MATCHES_ALL
        )
        val scriptTagContent =
            scriptTagRegex.find(finalPageHtml)?.groupValues?.get(1)?.trim().orEmpty()

        // Decrypt qilish
        println(encodedString)
        val decryptedContent = when {
            encodedString != null -> DecryptHelper.decrypt(encodedString)
            scriptTagContent.isNotEmpty() -> DecryptHelper.decrypt(scriptTagContent)
            else -> throw Exception("No encrypted data found")
        }

        // Source ni olish
        val m3u8 = decryptedContent.get("source")?.asString
            ?: throw Exception("No source found in decrypted data")

        return Video(
            source = m3u8,
            headers = mapOf(
                "User-Agent" to USER_AGENT,
                "Referer" to redirectBaseUrl
            ),
            subtitles = emptyList()
        )
    }
}