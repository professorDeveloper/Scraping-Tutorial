package com.azamovhudstc.scarpingtutorial.streamflix

import android.util.Base64
import com.azamovhudstc.scarpingtutorial.utils.Utils
import com.google.gson.Gson
import com.lagradost.cloudstream3.USER_AGENT
import kotlinx.coroutines.delay
import java.net.URLDecoder
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class VidsrcToExtractor : Extractor() {

    override val name = "Vidsrc.to"
    override val mainUrl = "https://vidsrc.to"
    private val keyUrl = "https://raw.githubusercontent.com/Ciarands/vidsrc-keys/main/keys.json"
    private val gson = Gson()

    fun server(videoType: Video.Type): Video.Server {
        return Video.Server(
            id = name,
            name = name,
            src = when (videoType) {
                is Video.Type.Episode -> "$mainUrl/embed/tv/${videoType.tvShow.id}/${videoType.season.number}/${videoType.number}"
                is Video.Type.Movie -> "$mainUrl/embed/movie/${videoType.id}"
                else -> throw IllegalArgumentException("Unknown video type")
            },
        )
    }

    override suspend fun extract(link: String): Video {
        val headers = mapOf(
            "User-Agent" to USER_AGENT,
            "Referer" to mainUrl
        )

        val doc = Utils.getJsoup(link, headers)
        println(link)

        val mediaId = doc.selectFirst("ul.episodes li a")
            ?.attr("data-id")
            ?: throw Exception("Can't retrieve media ID")

        val keysJson = Utils.get(keyUrl)
        val keys = gson.fromJson(keysJson, Keys::class.java)
        val sourcesUrl = "$mainUrl/ajax/embed/episode/$mediaId/sources"
        val token = encode(keys.encrypt[0], mediaId)
        val sourcesResponse = Utils.get("$sourcesUrl?token=$token", headers)
        val sources = gson.fromJson(sourcesResponse, EpisodeSources::class.java).result
            ?: throw Exception("Can't retrieve sources")

        var video: Video? = null
        var attempts = 0
        val maxAttempts = sources.size

        while (video == null && attempts < maxAttempts) {
            try {
                val source = sources[attempts]

                val embedUrl = "$mainUrl/ajax/embed/source/${source.id}"
                val embedToken = encode(keys.encrypt[0], source.id)
                val embedResponse = Utils.get("$embedUrl?token=$embedToken", headers)
                val embedRes = gson.fromJson(embedResponse, EmbedSource::class.java)

                val finalUrl = decryptUrl(keys.decrypt[0], embedRes.result.url)

                if (finalUrl == embedRes.result.url) {
                    throw Exception("finalUrl == embedUrl")
                }

                video = when (source.title) {
                    "F2Cloud", "Vidplay" -> {
                        val baseUrl = finalUrl.substringBefore("/e/")
                        if (baseUrl.contains("mcloud")) {
                            VidplayExtractor.MyCloud().extract(finalUrl)
                        } else {
                            VidplayExtractor.Any(baseUrl).extract(finalUrl)
                        }
                    }

                    "Filemoon" -> {
                        val baseUrl = finalUrl.substringBefore("/e/")
                        FilemoonExtractor.Any(baseUrl).extract(finalUrl)
                    }

                    else -> {
                        // Boshqa extractorlar uchun
                        Extractor.extract(finalUrl)
                    }
                }
            } catch (e: Exception) {
                attempts++
                if (attempts >= maxAttempts) {
                    throw Exception("Failed to extract video after $maxAttempts attempts", e)
                }
                // Delay between retries
                delay(1000)
            }
        }

        video ?: throw Exception("Failed to extract video")

        // Get subtitles
        val subtitlesUrl = "$mainUrl/ajax/embed/episode/$mediaId/subtitles"
        val subtitlesResponse = Utils.get(subtitlesUrl, headers)
        val subtitles = gson.fromJson(subtitlesResponse, Array<Subtitles>::class.java).toList()

        return video.copy(
            subtitles = subtitles.map {
                Video.Subtitle(
                    it.label,
                    it.file,
                )
            }
        )
    }

    private fun decryptUrl(key: String, encUrl: String): String {
        var data = Base64.decode(encUrl.toByteArray(), Base64.URL_SAFE)
        val rc4Key = SecretKeySpec(key.toByteArray(), "RC4")
        val cipher = Cipher.getInstance("RC4")
        cipher.init(Cipher.DECRYPT_MODE, rc4Key, cipher.parameters)
        data = cipher.doFinal(data)
        return URLDecoder.decode(data.toString(Charsets.UTF_8), "utf-8")
    }

    private fun encode(key: String, vId: String): String {
        val decodedId = decodeData(key, vId)
        val encodedBase64 = Base64.encode(decodedId, Base64.NO_WRAP).toString(Charsets.UTF_8)
        return encodedBase64
            .replace("/", "_")
            .replace("+", "-")
    }

    private fun decodeData(key: String, data: String): ByteArray {
        val keyBytes = key.toByteArray(Charsets.UTF_8)
        val s = ByteArray(256) { it.toByte() }
        var j = 0

        for (i in 0 until 256) {
            j = (j + s[i].toInt() + keyBytes[i % keyBytes.size].toInt()) and 0xff
            s[i] = s[j].also { s[j] = s[i] }
        }

        val decoded = ByteArray(data.length)
        var i = 0
        var k = 0

        for (index in decoded.indices) {
            i = (i + 1) and 0xff
            k = (k + s[i].toInt()) and 0xff
            s[i] = s[k].also { s[k] = s[i] }
            val t = (s[i].toInt() + s[k].toInt()) and 0xff
            decoded[index] = (data[index].code xor s[t].toInt()).toByte()
        }

        return decoded
    }

    data class EpisodeSources(
        val status: Int,
        val result: List<Result>?
    ) {
        data class Result(
            val id: String,
            val title: String
        )
    }

    data class EmbedSource(
        val status: Int,
        val result: Result
    ) {
        data class Result(
            val url: String
        )
    }

    data class Subtitles(
        val label: String,
        val file: String,
    )

    data class Keys(
        val encrypt: List<String>,
        val decrypt: List<String>,
    )
}