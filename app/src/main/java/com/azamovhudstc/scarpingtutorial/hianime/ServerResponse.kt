package com.azamovhudstc.scarpingtutorial.hianime

import com.azamovhudstc.scarpingtutorial.utils.Utils
import com.google.gson.Gson
import org.jsoup.Jsoup

data class ServerResponse(val link: String)
data class EpisodeServers(val html: String)
data class HiServer(val id: String, val label: String)

class HiAnimeVideoExtractor {

    private val gson = Gson()
    private val base = "https://hianime.bz"

    fun extractServers(episodeId: Int): List<HiServer> {
        val json = Utils.get("$base/ajax/v2/episode/servers?episodeId=$episodeId")
        val resp = gson.fromJson(json, EpisodeServers::class.java)

        val doc = Jsoup.parse(resp.html)

        return doc.select(".server-item[data-id]").map {
            HiServer(
                id = it.attr("data-id"),
                label = it.select("a.btn").text()
            )
        }
    }

    fun extractVideoFromServer(serverId: String): String {
        val json = Utils.get("$base/ajax/v2/episode/sources?id=$serverId")
        val source = gson.fromJson(json, ServerResponse::class.java).link
        return source
    }

    fun extractMegacloudVideo(url: String): String {
        val extractor = MegacloudExtractor()
        val (m3u8, tracks) = extractor.extractVideoUrl(url)
        println("Subtitles: $tracks")
        return m3u8
    }
}
