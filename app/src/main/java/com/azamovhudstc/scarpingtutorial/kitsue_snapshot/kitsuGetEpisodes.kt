package com.azamovhudstc.scarpingtutorial.kitsue_snapshot

import com.azamovhudstc.scarpingtutorial.utils.Utils
import com.google.gson.Gson
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    val kitsuApi = KitsuApi()
    runBlocking {
        val animeId = kitsuApi.searchId("one piece")
        println("Anime ID = $animeId")

        val episodes = kitsuApi.getEpisodes(animeId!!, 0)
        episodes.forEach {
            println("EP ${it.number}: ${it.title}")
            println("Thumbnail: ${it.thumbnail}")
            println("Desc: ${it.description}")
            println("--------------------------")
        }


    }

}