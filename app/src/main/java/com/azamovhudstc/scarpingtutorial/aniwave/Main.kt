package com.azamovhudstc.scarpingtutorial.aniwave

import com.azamovhudstc.scarpingtutorial.utils.Utils.getJson
import com.azamovhudstc.scarpingtutorial.utils.Utils.getJsoup
import com.talent.animescrap_common.model.AnimeDetails
import org.jsoup.Jsoup

private val mainUrl = "https://aniwave.to"
private val url = "https://vidplay.site/"
private val apiUrl = "https://9anime.eltik.net/"

suspend fun main(args: Array<String>) {
    val simpleAnime = searchAnime("One Piece").get(0)
    println(simpleAnime.animeName)
    println(simpleAnime.animeLink)
    println(animeDetails(simpleAnime.animeLink).animeCover)

    animeDetails(simpleAnime.animeLink).animeEpisodes["Sub"]!!.toList().onEach {
        println(it.first)
        println(it.second)
    }
}

suspend fun searchAnime(query: String): ArrayList<SimpleAnime> {
    val animeList = getAnimeList("$mainUrl/filter?keyword=$query")

    return animeList

}

suspend fun animeDetails(contentLink: String): AnimeDetails {
    val doc = getJsoup("$mainUrl$contentLink")
    val cover = doc.select("#w-info").first()!!.getElementsByTag("img").attr("src")
    val desc = doc.select("#w-info .info .content").text()
    val title = doc.select("#w-info .info .title").attr("data-jp")

    val dataId = doc.getElementsByAttribute("data-id").first()!!.attr("data-id")
    println(dataId)
    val vrf = getVrf(dataId)
    val eps =
        Jsoup.parseBodyFragment(getJson("$mainUrl/ajax/episode/list/$dataId?vrf=$vrf")!!.asJsonObject["result"].asString)
            .select("li a")
    val subMap = mutableMapOf<String, String>()
    val dubMap = mutableMapOf<String, String>()
    eps.forEach {
        val epNum = it.attr("data-num")
        val epIds = it.attr("data-ids")
        val isSub = it.attr("data-sub").toInt() == 1
        val isDub = it.attr("data-dub").toInt() == 1
        if (isSub) subMap[epNum] = epIds
        if (isDub) dubMap[epNum] = epIds
    }

    return AnimeDetails(title, desc, cover, mapOf("Sub" to subMap, "Dub" to dubMap))
}

private fun getVrf(dataId: String): String {
    val json = getJson("$apiUrl/vrf?query=${dataId}&apikey=chayce")
    return json!!.asJsonObject["url"].asString
}

private fun decodeVrf(dataId: String): String {
    val json = getJson("$apiUrl/decrypt?query=${dataId}&apikey=chayce")
    return json!!.asJsonObject["url"].asString
}


private fun getAnimeList(url: String): ArrayList<SimpleAnime> {
    val animeList = arrayListOf<SimpleAnime>()
    val doc = getJsoup(url)
    doc.select("#list-items .item").forEach { item ->
        animeList.add(
            SimpleAnime(
                item.select(".info a").attr("data-jp"),
                item.getElementsByTag("img").attr("src"),
                item.getElementsByTag("a").attr("href")
            )
        )
    }
    return animeList
}
