package com.azamovhudstc.scarpingtutorial.vidrock

import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.base64Encode
import com.lagradost.cloudstream3.utils.ExtractorLink
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder

class VidRockSource {
    val vidrock = "https://vidrock.net"
    suspend fun invokevidrock(
        tmdbId: Int? = null,
        season: Int? = null,
        episode: Int? = null,
    ) {
        val type = if (season == null) "movie" else "tv"
        val encoded = vidrockEncode(tmdbId.toString(), type, season, episode)
        val response = app.get("$vidrock/api/$type/$encoded").text
        println("$vidrock/api/$type/$encoded")
        println(response)
        val sourcesJson = JSONObject(response)

        val vidrockHeaders = mapOf(
            "Origin" to vidrock
        )

        sourcesJson.keys().forEach { key ->
            val sourceObj = sourcesJson.optJSONObject(key) ?: return@forEach
            val rawUrl = sourceObj.optString("url", null)
            val lang = sourceObj.optString("language", "Unknown")
            if (rawUrl.isNullOrBlank() || rawUrl == "null") return@forEach

            // Decode only if encoded
            val safeUrl = if (rawUrl.contains("%")) {
                URLDecoder.decode(rawUrl, "UTF-8")
            } else rawUrl

            when {
                safeUrl.contains("/playlist/") -> {
                    val playlistResponse = app.get(safeUrl, headers = vidrockHeaders).text
                    val playlistArray = JSONArray(playlistResponse)
                    for (j in 0 until playlistArray.length()) {
                        val item = playlistArray.optJSONObject(j) ?: continue
                        val itemUrl = item.optString("url", null) ?: continue
                        val res = item.optInt("resolution", 0)
                        println(itemUrl)
                        println(item)
                        println(res)
                    }
                }

                safeUrl.contains(".mp4", ignoreCase = true) -> {
                    println("mp4:" + safeUrl)
                }

                // Handle HLS/m3u8
                safeUrl.contains(".m3u8", ignoreCase = true) -> {
                    println("m3u8:" + safeUrl)
                }

                else -> {
                    println("else:" + safeUrl)

                }
            }
        }
    }

    fun vidrockEncode(
        tmdb: String,
        type: String,
        season: Int? = null,
        episode: Int? = null
    ): String {
        val base = if (type == "tv" && season != null && episode != null) {
            "$tmdb-$season-$episode"
        } else {
            val map = mapOf(
                '0' to 'a', '1' to 'b', '2' to 'c', '3' to 'd', '4' to 'e',
                '5' to 'f', '6' to 'g', '7' to 'h', '8' to 'i', '9' to 'j'
            )
            tmdb.map { map[it] ?: it }.joinToString("")
        }
        val reversed = base.reversed()
        val firstEncode = base64Encode(reversed.toByteArray())
        val doubleEncode = base64Encode(firstEncode.toByteArray())

        return doubleEncode
    }

}