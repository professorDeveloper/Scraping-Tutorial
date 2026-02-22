package com.azamovhudstc.scarpingtutorial.streamflix.anime

import com.azamovhudstc.scarpingtutorial.hianime.EpisodeServers
import com.azamovhudstc.scarpingtutorial.hianime.HiServer
import com.azamovhudstc.scarpingtutorial.utils.Utils
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import org.jsoup.Jsoup

fun main(args: Array<String>) {
    runBlocking {
        val servers = extractServers(episodeId = 12345)
        println("Servers:")
        servers.forEach {
            println(" - ${it.label} | (ID: ${it.id})")
        }
        val megacloudExtractor = MegacloudExtractor()
        megacloudExtractor.extractVideoUrl("https://megacloud.blog/embed-2/v3/e-1/TetF6fylRPHy?k=1")
            .let {
                println("Video URL: ${it.first}")
                println("Tracks:")
                it.second.forEach { track ->
                    println(" - ${track.file} (${track.label})")
                }
            }


    }
}

private val gson = Gson()

fun extractServers(episodeId: Int): List<HiServer> {
    val json = Utils.get("https://hianime.bz/ajax/v2/episode/servers?episodeId=$episodeId")
    val resp = gson.fromJson(json, EpisodeServers::class.java)
    val doc = Jsoup.parse(resp.html)

    return doc.select(".server-item[data-id]").mapNotNull { el ->
        val id = el.attr("data-id")
        val label = el.selectFirst("a.btn")?.text().orEmpty()
        val type = el.attr("data-type")

        if (id.isBlank() || type.isBlank()) {
            null
        } else {
            HiServer(
                id = id,
                label = label,
//                type = type
            )
        }
    }
}