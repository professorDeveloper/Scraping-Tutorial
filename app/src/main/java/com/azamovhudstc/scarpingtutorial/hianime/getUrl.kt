package com.azamovhudstc.scarpingtutorial.hianime

import android.annotation.SuppressLint
import android.util.Log
import com.azamovhudstc.scarpingtutorial.utils.Utils
import com.azamovhudstc.scarpingtutorial.utils.parser
import com.google.gson.Gson
import com.lagradost.nicehttp.Requests
import java.net.URLEncoder

@SuppressLint("NewApi")
suspend fun getUrl(
    url: String,
    referer: String? = null,
    subtitleCallback: (SubtitleFile) -> Unit,
    callback: (ExtractorLink) -> Unit
) {
    val gson = Gson()

    // umumiy headerlar
    val mainHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0",
        "Accept" to "*/*",
        "Referer" to "https://megacloud.blog/"
    )

    try {
        // id ni olish
        val id = url.substringAfterLast("/").substringBefore("?")

        // nonce olish
        val responseNonce = Requests(baseClient = Utils.httpClient, responseParser = parser).get(url, headers = mainHeaders).text
        val match1 = Regex("""\b[a-zA-Z0-9]{48}\b""").find(responseNonce)
        val match2 = Regex("""\b([a-zA-Z0-9]{16})\b.*?\b([a-zA-Z0-9]{16})\b.*?\b([a-zA-Z0-9]{16})\b""").find(responseNonce)

        val nonce = match1?.value ?: match2?.let {
            it.groupValues[1] + it.groupValues[2] + it.groupValues[3]
        } ?: throw Exception("Nonce topilmadi")

        // API chaqirish
        val apiUrl = "https://megacloud.blog/embed-2/v3/e-1/getSources?id=$id&_k=$nonce"
        val responseJson = Requests(baseClient = Utils.httpClient, responseParser = parser).get(apiUrl, headers = mainHeaders).text
        val response = gson.fromJson(responseJson, MegacloudResponse::class.java)

        // source olish
        val encoded = response.sources.firstOrNull()?.file ?: throw Exception("No sources found")

        // key olish
        val keyJson = Requests(baseClient = Utils.httpClient, responseParser = parser).get("https://raw.githubusercontent.com/yogesh-hacker/MegacloudKeys/refs/heads/main/keys.json").text
        val key = gson.fromJson(keyJson, Megakey::class.java)?.mega

        // m3u8 linkni yechish
        val m3u8 = if (encoded.contains(".m3u8")) {
            encoded
        } else {
            val decodeUrl = "https://script.google.com/macros/s/AKfycbxHbYHbrGMXYD2-bC-C43D3njIbU-wGiYQuJL61H4vyy6YVXkybMNNEPJNPPuZrD1gRVA/exec"
            val fullUrl = "$decodeUrl?encrypted_data=${URLEncoder.encode(encoded, "UTF-8")}" +
                    "&nonce=${URLEncoder.encode(nonce, "UTF-8")}" +
                    "&secret=${URLEncoder.encode(key, "UTF-8")}"

            val decrypted = Requests(baseClient = Utils.httpClient, responseParser = parser).get(fullUrl).text
            Regex("\"file\":\"(.*?)\"").find(decrypted)?.groupValues?.get(1)
                ?: throw Exception("Video URL not found")
        }

        // m3u8 linkni qaytarish
        callback(
            ExtractorLink(
                name = "Megacloud",
                url = m3u8,
                referer = "https://megacloud.blog/",
                quality = "HD",
                isM3u8 = true,
                headers = mainHeaders
            )
        )

        // subtitles
        response.tracks.forEach { track ->
            if (track.kind == "captions" || track.kind == "subtitles") {
                subtitleCallback(SubtitleFile(track.label, track.file))
            }
        }

    } catch (e: Exception) {
        Log.e("Megacloud", "Xatolik: ${e.message}")
    }
}

// --- Data classlar ---

data class MegacloudResponse(
    val sources: List<Source>,
    val tracks: List<Track>,
    val encrypted: Boolean,
    val intro: Intro,
    val outro: Outro,
    val server: Long
)

data class Source(val file: String, val type: String)

data class Track(
    val file: String,
    val label: String,
    val kind: String,
    val default: Boolean? = null
)

data class Intro(val start: Long, val end: Long)
data class Outro(val start: Long, val end: Long)
data class Megakey(val rabbit: String, val mega: String)

// --- Placeholder classlar ---
// Bularni project’ingda o‘zing moslab yozasan
data class ExtractorLink(
    val name: String,
    val url: String,
    val referer: String,
    val quality: String,
    val isM3u8: Boolean,
    val headers: Map<String, String>
)

data class SubtitleFile(val label: String, val file: String)
