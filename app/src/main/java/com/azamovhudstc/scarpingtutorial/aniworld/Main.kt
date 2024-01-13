package com.azamovhudstc.scarpingtutorial.aniworld

import com.azamovhudstc.scarpingtutorial.utils.Utils.getJsoup
import com.azamovhudstc.scarpingtutorial.utils.Utils.httpClient
import com.azamovhudstc.scarpingtutorial.utils.parser
import com.lagradost.nicehttp.Requests
import okhttp3.MultipartBody
import okhttp3.RequestBody


private val mainUrl = "https://aniworld.to"

suspend fun main(args: Array<String>) {

    val data = searchAnimeInAniWord("One Piece")
    val epData = animeDetails(data.get(0)).get(0)
    setLink(epData.link)
}


suspend fun animeDetails(parsedData: AniworldSearchDataItem): ArrayList<EpisodeData> {
    val epList = arrayListOf<EpisodeData>()
    val doc = getJsoup("$mainUrl/${parsedData.link}")
    val animeList = doc.getElementsByClass("hosterSiteDirectNav")
    val episodeElements = animeList.select("ul:has(li a[data-episode-id]) li a[data-episode-id]")


    for (episodeElement in episodeElements) {
        val number = episodeElement.text()
        val url = episodeElement.attr("href")
        epList.add(EpisodeData(number, url))
    }
    return epList
}

suspend fun setLink(url: String) {
    val document = getJsoup("$mainUrl/$url")
    val hosterElements = document.getElementsByClass("watchEpisode")
    for (element in hosterElements) {
        val hoster = element.select("i").attr("title")
        val hosterUrl = element.attr("href")
        val hosterName = element.select("h4").text()
        val videoButton = element.select(".hosterSiteVideoButton").text()
        println("Hoster: $hoster")
        println("Hoster URL: $hosterUrl")
        println("Hoster Name: $hosterName")
        println("Video Button: $videoButton")
        println("--------------")
    }

}

suspend fun searchAnimeInAniWord(keyWord: String): AniworldSearchData {
    val request = Requests(baseClient = httpClient)
    val requestBody: RequestBody = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart(
            "keyword",
            keyWord,
        )
        .build()

    val data = request.post(
        "https://aniworld.to/ajax/search",
        requestBody = requestBody,
        responseParser = parser
    )
    val parsedData = data.parsed<AniworldSearchData>()
    return parsedData
}