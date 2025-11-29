package com.azamovhudstc.scarpingtutorial.hianime

import com.azamovhudstc.scarpingtutorial.utils.Utils
import com.google.gson.Gson
import org.jsoup.Jsoup
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

data class EpisodeResponse(
    val html: String
)

class HiAnimeSource {

    private val BASE_URL = "https://hianime.bz"

    /**
     * SEARCH ANIME
     */
    fun searchAnime(keyword: String): List<HiAnime> {
        val result = mutableListOf<HiAnime>()

        return try {
            val doc = Utils.getJsoup("$BASE_URL/search?keyword=$keyword")

            val items = doc.select("div.flw-item")

            for (item in items) {
                val poster = item.selectFirst(".film-poster")
                val detail = item.selectFirst(".film-detail")

                val title = detail?.selectFirst(".film-name a")?.attr("title") ?: continue
                val link = BASE_URL + (detail.selectFirst(".film-name a")?.attr("href") ?: "")
                val imageUrl = poster?.selectFirst("img")?.attr("data-src") ?: ""
                val type = detail?.selectFirst(".fd-infor .fdi-item")?.text() ?: ""
                val duration = detail?.selectFirst(".fdi-duration")?.text() ?: ""

                val subCount = poster?.selectFirst(".tick-item.tick-sub")?.ownText()?.toIntOrNull()
                val dubCount = poster?.selectFirst(".tick-item.tick-dub")?.ownText()?.toIntOrNull()

                result.add(
                    HiAnime(
                        id = link.extractAnimeId() ?: 0,
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

            result
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * GET EPISODE LIST BY ANIME ID
     */
    suspend fun getEpisodeListById(id: Int): List<Episode> {
        val episodes = mutableListOf<Episode>()

        return try {
            val json = Utils.get("$BASE_URL/ajax/v2/episode/list/$id")
            val episodeResponse = Gson().fromJson(json, EpisodeResponse::class.java)

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

            episodes
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }


    /**
     * Extract anime ID from URLs like
     * https://hianime.bz/anime/one-piece-quest-17184
     */
    private fun String.extractAnimeId(): Int? {
        return Regex("-(\\d+)").find(this)?.groupValues?.get(1)?.toIntOrNull()
    }

    /**
     * Extract episode ID from URL params like ?ep=134
     */
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
}
