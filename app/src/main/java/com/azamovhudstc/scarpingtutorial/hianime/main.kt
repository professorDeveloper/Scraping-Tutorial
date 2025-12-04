package com.azamovhudstc.scarpingtutorial.hianime

import kotlinx.coroutines.runBlocking

fun main() {
    val hi = HiAnimeSource()

    println("Searching...")
    val list = hi.searchAnime("One Piece")
    list.forEach { println(it) }

    val first = list.get(1).id
    val episodes = runBlocking { hi.getEpisodeListById(first) }
    println(episodes)

    val videoExtractor = HiAnimeVideoExtractor()
//    /*
    val servers = videoExtractor.extractServers(episodes.first().id)
    println("Servers: $servers")

    val sourceUrl = videoExtractor.extractVideoFromServer(servers.first().id)
    println("Source: $sourceUrl")

    val finalM3u8 = videoExtractor.extractMegacloudVideo(sourceUrl)
    println("FINAL M3U8 → $finalM3u8")
}
