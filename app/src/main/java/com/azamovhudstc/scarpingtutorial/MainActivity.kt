package com.azamovhudstc.scarpingtutorial

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.azamovhudstc.scarpingtutorial.utils.Utils.getJsoup
import com.azamovhudstc.scarpingtutorial.utils.Utils.postJson
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //Enjoy My Code
    }
}


private val mainUrl = "https://yugenanime.tv" //GET MAIN URL
private lateinit var epList: MutableList<String>
suspend fun main() {
    coroutineScope {
        val parseData = searchAnime("One Piece").get(0)
        val animeEpisodesMap = returnEpMap(parseData)
        val epType = "SUB"
        epList = animeEpisodesMap[epType]!!.keys.toMutableList()
        val animeUrl = parseData.href
        val epIndex = epList[0]!!.toString()
        val animeEpisodeMap2 = animeEpisodesMap["SUB"]!!
        val animeTotalEpisode = animeEpisodesMap[epType]!!.size.toString()

        streamLink(animeUrl, epIndex.toString(), listOf("SUB"))

    }
}

suspend fun streamLink(
    animeUrl: String,
    animeEpCode: String,
    extras: List<String>?
) {
    val watchLink = animeUrl.replace("anime", "watch")
    val animeEpUrl = "$mainUrl$watchLink$animeEpCode"
    var yugenEmbedLink = getJsoup(animeEpUrl).getElementById("main-embed")!!.attr("src")
    if (!yugenEmbedLink.contains("https:")) yugenEmbedLink = "https:$yugenEmbedLink"
    val mapOfHeaders = mutableMapOf(
        "X-Requested-With" to "XMLHttpRequest",
        "content-type" to "application/x-www-form-urlencoded; charset=UTF-8"
    )
    val apiRequest = "$mainUrl/api/embed/"
    val id = yugenEmbedLink.split("/")
    val dataMap = mapOf("id" to id[id.size - 2], "ac" to "0")
    println(dataMap)
    val linkDetails = postJson(apiRequest, mapOfHeaders, dataMap)!!.asJsonObject
    val link = linkDetails["hls"].asJsonArray.first().asString

    println(link)
}

suspend fun returnEpMap(parseData: AnimeParseData): Map<String, Map<String, String>> {
    val url = "$mainUrl${parseData.href}watch/?sort=episode" //GET ANIME DETAIL URL
    val doc = getJsoup(url) //REQUEST ANIME DETAIL
    println(doc.getElementsByClass("p-10-t").first()!!.text()) // get anime name
    val subsEpCount = doc.getElementsByClass("box p-10 p-15 m-15-b anime-metadetails")
        .select("div:nth-child(6)").select("span").text() /// get episode count
    val epMapSub =
        (1..subsEpCount.toInt()).associate { it.toString() to it.toString() } /// generate map from 1 to epCount

    val epMap = mutableMapOf("SUB" to epMapSub)

    return epMap
}


suspend fun searchAnime(query: String): ArrayList<AnimeParseData> {
    val animeList = arrayListOf<AnimeParseData>()
    val url = "$mainUrl/discover/?q=$query" //GET SEARCH URL
    val doc = getJsoup(url) //REQUEST SEARCH
    val animeContent = doc.getElementsByClass("anime-meta") //GET ANIME LIST
    for (item in animeContent) {
        val animeName = item.getElementsByClass("anime-name").text() //GET ANIME NAME
        val animeCover = item.getElementsByTag("img").attr("data-src") // GET ANIME COVER
        val href = item.attr("href") //GET DETAIL URL
        val animeParseData = AnimeParseData(animeName, href, animeCover) //PARSE DATA
        animeList.add(animeParseData) //ADD DATA
    }

    return animeList //RETURN DATA
}


fun <A, B> List<A>.asyncMap(f: suspend (A) -> B): List<B> = runBlocking {
    map { async { f(it) } }.map { it.await() }
}