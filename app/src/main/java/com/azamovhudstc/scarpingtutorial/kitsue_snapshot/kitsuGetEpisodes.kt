package com.azamovhudstc.scarpingtutorial.kitsue_snapshot

import com.azamovhudstc.scarpingtutorial.utils.Utils
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking

fun kitsuSearchId(query: String): String? {
    val url = "https://kitsu.io/api/edge/anime?filter[text]=$query"
    val json = Utils.get(url)

    val result = Gson().fromJson(json, KitsuSearchResponse::class.java)

    return result.data?.firstOrNull()?.id
}

fun getKitsuEpisodesPaged(animeId: String, page: Int): List<KitsuEpisode> {
    val limit = 20
    val offset = page * limit

    val url =
        "https://kitsu.io/api/edge/anime/$animeId/episodes?page[limit]=$limit&page[offset]=$offset"

    val json = Utils.get(url)
    val result = Gson().fromJson(json, KitsuEpisodeResponse::class.java)
    val list = result.data ?: return emptyList()

    return list.mapNotNull { ep ->
        val attr = ep.attributes ?: return@mapNotNull null

        KitsuEpisode(
            id = ep.id ?: "",
            number = attr.number ?: 0,
            title = attr.canonicalTitle
                ?: attr.titles?.en_us
                ?: attr.titles?.en_jp
                ?: attr.titles?.ja_jp
                ?: "Episode ${attr.number}",
            description = attr.description ?: "",
            thumbnail = attr.thumbnail?.original ?: ""
        )
    }
}

fun main(args: Array<String>) {
    runBlocking {
        val animeId = kitsuSearchId("one piece")
        println("Anime ID = $animeId")

        val episodes = getKitsuEpisodesPaged(animeId!!, 0)
        episodes.forEach {
            println("EP ${it.number}: ${it.title}")
            println("Thumbnail: ${it.thumbnail}")
            println("Desc: ${it.description}")
            println("--------------------------")
        }
    }

}