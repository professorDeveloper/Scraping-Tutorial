package com.azamovhudstc.scarpingtutorial.kitsue_snapshot

import com.azamovhudstc.scarpingtutorial.utils.Utils
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class KitsuApi {
    private val gson = Gson()
    private val base = "https://kitsu.io/api/edge"

    suspend fun searchId(query: String): String? = withContext(Dispatchers.IO) {
        val encoded = query.replace(" ", "%20")
        val url = "$base/anime?filter[text]=$encoded"

        val json = Utils.get(url)  // blocking → moves to IO thread
        val result = gson.fromJson(json, KitsuSearchResponse::class.java)

        result.data?.firstOrNull()?.id
    }


    suspend fun getEpisodes(animeId: String, page: Int): List<KitsuEpisode> =
        withContext(Dispatchers.IO) {

            val limit = 20
            val offset = page * limit

            val url = "$base/anime/$animeId/episodes?page[limit]=$limit&page[offset]=$offset"

            val json = Utils.get(url)
            val result = gson.fromJson(json, KitsuEpisodeResponse::class.java)
            val list = result.data ?: return@withContext emptyList()

            list.mapNotNull { ep ->
                val attr = ep.attributes ?: return@mapNotNull null

                KitsuEpisode(
                    id = ep.id ?: "",
                    number = attr.number ?: 0,
                    title = attr.canonicalTitle ?: attr.titles?.en_us ?: attr.titles?.en_jp
                    ?: attr.titles?.ja_jp ?: "Episode ${attr.number}",
                    description = attr.description ?: "",
                    thumbnail = attr.thumbnail?.original ?: ""
                )
            }
        }
}