package com.azamovhudstc.scarpingtutorial.playimdb

import android.annotation.SuppressLint
import android.util.Log
import com.azamovhudstc.scarpingtutorial.themoviedb.Backdrop
import com.azamovhudstc.scarpingtutorial.utils.Utils.getJsoup
import com.azamovhudstc.scarpingtutorial.utils.Utils.httpClient
import com.azamovhudstc.scarpingtutorial.utils.parser
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.base64DecodeArray
import com.lagradost.nicehttp.Requests
import com.saikou.sozo_tv.data.model.SeasonResponse
import com.saikou.sozo_tv.data.model.SubtitleItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.SocketTimeoutException
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

data class Episode(
    val season: Int, val episode: Int, val title: String, val iframeUrl: String
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
        val iframe = doc.selectFirst("iframe") ?: throw Exception("Iframe not found")

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
                season = 0, episode = 1, title = pageTitle, iframeUrl = fixedUrl
            )
        )
    }

    episodes.sortedWith(compareBy({ it.season }, { it.episode }))
}

fun extractSeriesIframe(link: String): String? {
    val doc: Document = getJsoup(link)
//    println(doc)
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

    val response = Jsoup.connect(iframeUrl).method(Connection.Method.GET).header(
        "accept",
        "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"
    ).header("accept-language", "en-US,en;q=0.9,uz-UZ;q=0.8,uz;q=0.7")
        .header("cache-control", "max-age=0").header("dnt", "1").header("priority", "u=0, i")
        .header(
            "sec-ch-ua",
            "\"Google Chrome\";v=\"141\", \"Not?A_Brand\";v=\"8\", \"Chromium\";v=\"141\""
        ).header("sec-ch-ua-mobile", "?0").header("sec-ch-ua-platform", "\"Windows\"")
        .header("sec-fetch-dest", "document").header("sec-fetch-mode", "navigate")
        .header("sec-fetch-site", "none").header("sec-fetch-user", "?1")
        .header("upgrade-insecure-requests", "1").header(
            "User-agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36"
        ).ignoreHttpErrors(true).ignoreContentType(true).execute()
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

suspend fun extractDirectM3u8(iframeUrl: String, fckURl: String): String {
    try {
        val response = Jsoup.connect(iframeUrl).method(Connection.Method.GET).header(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:101.0) Gecko/20100101 Firefox/101.0"
        ).header(
            "Accept",
            "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"
        ).header("Accept-Language", "en-US,en;q=0.5")
            .header("Referer", fckURl) // bu muhim — iframe parent
            .header("Host", "cloudnestra.com").header("Alt-Used", "cloudnestra.com")
            .header("Connection", "keep-alive").header("Upgrade-Insecure-Requests", "1")
            .header("Sec-Fetch-Dest", "iframe").header("Sec-Fetch-Mode", "navigate")
            .header("Sec-Fetch-Site", "same-origin").header("TE", "trailers")
            .ignoreContentType(true).timeout(15000).execute()

        val html = response.body()
        println(html)
        val regex = Regex("""https?://[^\s'"]+\.m3u8""")
        val match = regex.find(html)

        return if (match != null) {
            val m3u8Url = match.value
            m3u8Url
        } else {
            "empty"
        }

    } catch (e: Exception) {
        println(e)
        return ""
    }
}


suspend fun getDetails(season: Int, tmdbId: Int): ArrayList<Backdrop> {
    val niceHttp = Requests(baseClient = httpClient, responseParser = parser)
    val request = niceHttp.get(
        "https://jumpfreedom.com/3/tv/${tmdbId}/season/${season}?language=en-US", headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:101.0) Gecko/20100101 Firefox/101.0",
        )
    )
    if (request.isSuccessful) {

        val body = request.body.string()
        val data = Gson().fromJson(body, SeasonResponse::class.java)
        val stillPaths = data.episodes.mapNotNull { it.stillPath }


        return stillPaths.map { Backdrop("https://image.tmdb.org/t/p/w500/${it.toString()}") } as ArrayList<Backdrop>
    } else {
        println(request.body.string())
        Log.d("GGG", "getDetails:fuck  life ")
    }
    return ArrayList()
}

suspend fun getSubTitleList(tmdbId: Int, season: Int, episode: Int) {
    val niceHttp = Requests(baseClient = httpClient, responseParser = parser)

    val request = niceHttp.get(
        "https://sub.wyzie.ru/search?id=${tmdbId}&season=${season}&episode=${episode}",
        headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:101.0) Gecko/20100101 Firefox/101.0",
        )
    )

    if (request.isSuccessful) {
        val body = request.body.string()
        try {
            val listType = object : TypeToken<List<SubtitleItem>>() {}.type
            val data: List<SubtitleItem> = Gson().fromJson(body, listType)

            if (data.isNotEmpty()) {
                data.forEach {
                    println("${it.lang} - ${it.url}")
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@SuppressLint("NewApi")
fun base64Decode(string: String): String {
    val clean = string.trim().replace("\n", "").replace("\r", "")
    val padded = clean.padEnd((clean.length + 3) / 4 * 4, '=')
    return try {
        val decodedBytes = Base64.getDecoder().decode(padded)
        String(decodedBytes, Charsets.UTF_8)
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}

private val keyHex =
    base64Decode("Njk2ZDM3MzI2MzY4NjE3MjUwNjE3MzczNzc2ZjcyNjQ2ZjY2NjQ0OTZlNjk3NDU2NjU2Mzc0NmY3MjUzNzQ2ZA==")
private val ivHex = base64Decode("Njk2ZDM3MzI2MzY4NjE3MjUwNjE3MzczNzc2ZjcyNjQ=")
private val key = keyHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
private val iv = ivHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

fun normalizeCustomAlphabet(s: String): String {
    return s.replace("-_.", "/")
        .replace("@", "+")
        .replace("\\s+".toRegex(), "")
}

fun base64ToBytes(b64: String): ByteArray {
    var base64Str = b64
    val pad = base64Str.length % 4
    if (pad != 0) base64Str += "=".repeat(4 - pad)
    return base64DecodeArray(base64Str)
}

fun decryptAes256Cbc(cipherBytes: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
    if (cipherBytes.size % 16 != 0) {
        throw IllegalArgumentException("Ciphertext length (${cipherBytes.size}) not multiple of 16.")
    }
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    val secretKey = SecretKeySpec(key, "AES")
    val ivSpec = IvParameterSpec(iv)
    cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
    return cipher.doFinal(cipherBytes)
}

fun decryptString(input: String): String {
    val normalized = normalizeCustomAlphabet(input)
    val cipherBytes = base64ToBytes(normalized)
    val plaintextBytes = decryptAes256Cbc(cipherBytes, key, iv)
    val text = String(plaintextBytes, Charsets.UTF_8)
    println(text)
    return text
}

fun main(args: Array<String>) {

    println(base64Decode("aHR0cHM6Ly9hcGkzLmRldmNvcnAubWU="))
    println(
        decryptString(
            "EhrwXvTumlibv-_.f1o@oMsYEmHYMzVYRSYPvoTpNjh6@VMsU5GBP5wcAtyc9eYrv77NXRulSl0XeP3YuBvzFjXzvfoNMRCp6QLJRg6q58CaNif@VSOnuTqZyWsgNxZsr09d-_.FmQ8uQI3MWESGh-_.8g@jMtO0zb4yNaRtzvNvyCpyrrdrhlHZ5po@677D-_.XrGasjL7GeO53Ib8fWlUDHDLiRg41oGlOMENRUuRKI1Q18hZ1UopkB2nHa0wf8xjULV@syo77hYbmeYTSRw7veBik5T91ro7QeH4scrHM2HEghU7a1L0xNb533RNLByqcrNF2M4J2L@Bn0F5V@bWM0kIuJe9zihn3Idb2f0qgUyBv6F7zGgK3aCb5WNBIELcmzVUVvZX@DeTcbnMOq1s3ef7io8oDiD@lFay-_.wpZ95mzU8vgFYBFEUZ9HQcBw4c6lBXkGlr6PF7CGEuQuWxsElVJZngd56h77RWp2tTmxjxaaFpakORF5c7SXztPOZoyasztKqOodgmCJvr9mwM3qM04HLOgESK41PnqW0r9oV60r@KgNgJEVH75FLtYxqiPx9lpRRYDmWPveiODW2erI997MSK47OZ38AJob8HpfnBkhzfyqyBhhC0ChQOtC8rPMIg6XB@4c0a2nMUGKgRHPAN-_.5opOYnjSXJokwd9ZJmp9ew9Yo8B4Sb4v9-_.r4peIiiIOCquVjsivg0i0MGhF7Rn2rnCJ-_.fxtjoBM4A13YSNOGDIcIIMl4BfI-_.Wn@hmMpIYtzZ2S-_.-_.hpbzXmwmaCU-_.7t3bly6ad6KvabdoF29hmg1O-_.@zJptMjTZwlE2XYWD9t4kqHoJsyFOCfaIY3caIKw@Jiz0h4K8lEhqOjijxCRL6gPXw1E7W-_.dv1FgiC9TRtNvmyqQUTxGskh2NrQwLWkGv1J8ElP9H8ccu2DWD6wsZ8PdgCpTNO8gb@NQrp6dmnSYm-_.GEqtIda0mQ7ECgmEZ2ZV0O4FUUoLeqf5XMiOs2kc8hjisfbxRpunPw48Lh0ZFTeS4p82Z55uLlCwRCUCDq2KWNpOVaRQWkfWXnteZlmSqSzTlOtNfUSlKyqMOeEPQJBDP3@yBDza9mFRtAKdOg97ydGpUw@afssYCwHlJZzZ@IN5YBu8Uuk7EkzpMWf@c0j8vo4rcWTuyrZKT5R-_.E2O-_.Pb0rE5fymQT9-_.UhuX67uq5dP0QCqvNub@Zv8C7Sj8H27365F7EbP4yRiC-_.ilK4LMXIgvDAFB4PgVQkSzaJksmb5bL5MuxPgZ8Pa@MbKB27GuliYd1VnumVT1EZvCs-_.LE378vmquvJOwqxJ93wyW7r-_.hBHtJnJsmtWPtXPE2fcAJuvLHFR6reypEwMO0yCDPLK3igTn2klcdw9bLfFU0qTRPL8pxteNDdJn1Gb42THF7IWgi4AnvjC4KL42EZQXnYQOcK64jQTAkhGfA1nK-_.tM7CpNPKb1bKB@ORTYbZdI-_.loDhj-_.ARZMzQIGmHkbq8Rjoa6hU9pUglNPGpA-_.ggyhcEIkCQhas5SGd0RYsMiU@uyGrt-_.pP7WZl1mEc5jPTmyMidmnAbCqXTvyFK8ybooT94tJsr2reWmUV-_.7Ti50Nc-_.vL9eOj4ws7st7RqkGq@s8hfgpWZh4tUbAV-_.RvNRHTYG3i1w0yUrHVT1KtdT7Brrfa4pJCAmm2qC1dsciYZ85oPUSKfof3LEE@pVSLZwrJJmei4BivY22ii03NjvQqGJ-_.iGdvUrthGMcr5YetoAPHLC2NUwaf3EXZNxhbJVRlQ2SFTA7kSGF@B7KmnNx@amcAMyzEQvEqlRner6O@BDgJUDayptOhRW-_.SQCZiNjWYcBxQQHD9P-_.1VfS08d6Gx@8Bh28ppDNgZ08mT4o9pDatmOavU0P@QMwven7iPH3CZccMh-_.dZpl2DYZIgisORk6BdeaNcZPq1SvLZAk5qh5sFdQtRx8jJHq68frI3-_.zEhYtm7Z61QfBOdoQbuM-_.8DIpLs4WjeW4K32ja2EA-_.-_.8@ZI1zirTLIJq1GjqPt4SFnoC5nwp4PDAUdWf-_.HVAN8mIeyahiX1h75nfPEckQ-_.Llw08BCAAjb4iVh3@0BN1LhIHPmu32dbGk-_.qebuXyBaNsi-_.fGFwni4gX-_.Qp5OHO@mJaYK1W7u5VypPYoTz9Zo-_.J90@T1Fr2Fqp@ptLMNB5EKlQ@AV8S4kx2OqsHNxyhtPhFXQWs14rwhZxEEDrd0p68ixzfrJJA6XNrCTTeDNapZRpJdoKxB6OG-_.vw4jj7NnGSyU5I-_.GyQTMZdKW5Mc9OX4u2AGmo-_.X4Qh@13urZ7yRggYOoMwKaE22ueWU3jNLwWIvFSbUdBhV1cARtfM7h4umUZn6@8OJLWESR@U4DuYkpG91LYarV-_.UIGYM14VjqHmaMhuQdeLcBu7GsVWckcRmZYesgNzg3kBNyYqHkpjTqJoZqM5rV74h017kXqYei@eTB90sQbUDRedsT@FxTepgc0QBu4wrKYovdqQU2ArwT6TNl6YAf74@XRXvKSJKQnc04FhDM8mHwzG0yKKhcjmo3eyRC@DnJx-_.ikdpOin7lObj5A-_.gW3MZi0m5o4Qj@wN1VDeTwQMSHY5pKDVDNg1-_.zy3W@FQMGKXH@GOItSF1E@7KQ4kmkbAyWL4FXKWmqd8wy52tCbIc@FaQ-_.TfNJGkbNAkDOkxx5QUhj5-_.WLBUUNa2mIjys2QBUrFETBUt91gPKm7rkyysB4eiZPU4xm3fAT2MO02e6akCgVJ4teRPOoB3h@idTqQnlbFNLJqfwiVXF-_.j3HJit7XMqOk1YQB2oabyBgHMMI1S2AKSjEaxbj0s0q@fAsX8fMunJ1tMboaOzHnHuDHD9k4h@4NNzXJ7FrpnoKH@4N9tOl3K3I4qOFLJpzh6s1DBDs8zhnVpRr6f7pBoo7SX42HXos9rHN1pZmyYXwiEs27IWinz1AXv@w1p8EWPiyFI2Vo4GIzsvSsF96yEc-_.d9aORXetL5QFdBFbvbYLOPTqJZLZoXVaor15174klyx3atrKBBMsxDnLVQvL-_.fCuuW6ctkxExXkCUnSbZEba1i1YMnIG1kaqCVVdfaLBjniLENTs0kyq4BQa6TPRo2mRzQrxs@9Pg5vH-_.6ivIsJfcW@i9abw656FI5jpmhkSaIlnD@Sx920VnQfqbIr6WJZ9ykd@13p5KvunWC7N0J81gdVqI1R4a9A12t-_.y7-_.G8VUVkjxXa30sJczYiiO3i2OzfuqYgs82KFpMcYqxB1RJ7smrkEBFBEq5LxJpQ0ZsVTYCgRm-_.W@ttTXK3N675j1Klstv7KDTxKuMCuHzP2RXWnUn-_.BV-_.1-_.cGXs7MH0xm9EpTGAjL@KD@oDaYm5Dkn9BR3mAZ9Tu1o3JIBC8fdTqcHVg@mvy4J4JdE6bdd2itR@l2GoxPj0xEDJlfrgoaJUy6@EhToMMkydaS6xmGaH1pvyLNbjcTFucHXfdDJuhvZQ5MPBbVioJ@0v28lkrVE@PWHPaDFndyyFOvyubkQu7SCw7O259CupVNRQPRGyReQg616iUf1FpE91tpFs06wwCWFH28ve-_.sdxEsJQc8iLjXE1amYbaORlMc9ItmvuLOgONTt8i3UEasRkTTwy2JvEuRUcUHgtTi2jVMMitSZyAJQ8A4SMQfLpq79XNvsr3EqjPHZgu74NoU@g5vxGqq62NcAP@97kTuruPuT1tXcVbuembNPZ9pL2VITbU83z5pzie3DVpIUVGXZrcC9yId9lokov2dU0-_.EcqmVo-_.2@pplKrUI5oeYrMCtqS2Y@yDChCGN6wb5VCDWPsihgW-_.KZLLzffaV2HfJW6oA9IXvUQt44bySGpu5u6YKx0NstL8-_.W8hRdUlG4Vxw"
        )
    )
    //Series example using
//    runBlocking {
//        val id = "tt33511103"
//        getEpisodes(id).onSuccess {
//            val list = it
//            println(list[0].iframeUrl)
//            convertRcptProctor(list[0].iframeUrl).let {
//                extractDirectM3u8(it, list[0].iframeUrl).let {
//                    println(it)
//                }
//            }
//
//        }
//    }

    val id = "119051"
    getEpisodes(id).onSuccess {
        val list = it
        list.forEach {
            println(it)
        }
    }
}

