package com.azamovhudstc.scarpingtutorial.hianime

import com.azamovhudstc.scarpingtutorial.utils.Utils
import com.azamovhudstc.scarpingtutorial.utils.parser
import com.google.gson.Gson
import com.lagradost.nicehttp.Requests
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import org.slf4j.helpers.Util
import java.net.URI

data class HiAnime(
    val id: Int,
    val title: String,
    val imageUrl: String,
    val type: String,
    val duration: String,
    val link: String,
    val subCount: Int?,
    val dubCount: Int?
)

data class Episode(
    val number: Int,
    val id: Int,
    val title: String,
    val link: String
)

fun main(args: Array<String>) {
    runBlocking {
        val hiAnime = HiAnimeSource()
        val item = hiAnime.searchAnime("My Dress-Up Darling Season 2").forEach {
            println(it)
        }
        hiAnime.getEpisodeListById(19794).forEach {
            println(it.link)
        }
    }
}

data class EpisodeResponse(
    val html: String
)

class HiAnimeSource {
    private val BASE_URL = "https://hianime.bz/"
    fun searchAnime(keyword: String): List<HiAnime> {
        val result = mutableListOf<HiAnime>()

        try {
            val doc = Utils.getJsoup("$BASE_URL/search?keyword=$keyword")

            val items = doc.select("div.flw-item")

            for (item in items) {
                val poster = item.selectFirst(".film-poster")
                val detail = item.selectFirst(".film-detail")

                val title = detail?.selectFirst(".film-name a")?.attr("title") ?: continue
                val link = BASE_URL + (detail?.selectFirst(".film-name a")?.attr("href") ?: "")
                val imageUrl = poster?.selectFirst("img")?.attr("data-src") ?: ""
                val type = detail?.selectFirst(".fd-infor .fdi-item")?.text() ?: ""
                val duration = detail?.selectFirst(".fdi-duration")?.text() ?: ""

                val subCountText = poster?.selectFirst(".tick-item.tick-sub")?.ownText()
                val dubCountText = poster?.selectFirst(".tick-item.tick-dub")?.ownText()

                val subCount = subCountText?.toIntOrNull()
                val dubCount = dubCountText?.toIntOrNull()

                result.add(
                    HiAnime(
                        id = link.extractId() ?: 0,
                        title = title,
                        imageUrl = imageUrl,
                        type = type,
                        duration = duration,
                        link = link,
                        subCount = subCount,
                        dubCount = dubCount
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return result
    }


    suspend fun getEpisodeListById(id: Int): List<Episode> {
        val episodes = mutableListOf<Episode>()

        try {
            val client = Requests(
                baseClient = Utils.httpClient,
                responseParser = parser
            )
            val response = client.get("$BASE_URL/ajax/v2/episode/list/$id")
            val body = response.body.string()

            val episodeResponse = Gson().fromJson(body, EpisodeResponse::class.java)

            val doc = Jsoup.parse(episodeResponse.html)
            val items = doc.select("a.ssl-item.ep-item")

            for (item in items) {
                val number = item.attr("data-number").toIntOrNull() ?: continue
                val epId = item.attr("data-id").toIntOrNull() ?: continue
                val title = item.attr("title")
                val link = BASE_URL + item.attr("href")

                episodes.add(
                    Episode(
                        number = number,
                        id = epId,
                        title = title,
                        link = link
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return episodes
    }
//    //https://megaplay.buzz/stream/s-2/136197/dub
//    fun getM3u8DubLink(url: String): String {
//        val id = url.extractEpId() ?: -1
//        val request =Utils.getJsoup("https://megaplay.buzz/stream/s-2/$id/dub")
//
////    }

    fun String.extractEpId(): Int? {
        return try {
            val uri = URI(this)
            val query = uri.query ?: return null
            query.split("&")
                .map { it.split("=") }
                .firstOrNull { it.first() == "ep" }
                ?.getOrNull(1)
                ?.toIntOrNull()
        } catch (e: Exception) {
            null
        }
    }

    fun String.extractId(): Int? {
        val regex = Regex("-(\\d+)")
        return regex.find(this)?.groupValues?.get(1)?.toIntOrNull()
    }

}