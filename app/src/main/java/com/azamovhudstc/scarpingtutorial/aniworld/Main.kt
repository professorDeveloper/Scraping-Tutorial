package com.azamovhudstc.scarpingtutorial.aniworld

import com.azamovhudstc.scarpingtutorial.utils.Utils.getJsoup
import com.azamovhudstc.scarpingtutorial.utils.Utils.httpClient
import com.azamovhudstc.scarpingtutorial.utils.parser
import com.lagradost.nicehttp.Requests
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.jsoup.Jsoup


private const val mainUrl = "https://aniworld.to"

suspend fun main(args: Array<String>) {
    val data = searchAnimeInAniWord("One Piece")
    val epData = animeDetails(data.get(0)).get(0)
    println(
        epData.link
    )
    setLink(epData.link)
    val epFullData = setLink(epData.link)

    println("$mainUrl${epFullData.hostUrl}")
    val redirectLink = getAnimeRedirectLink(epFullData.hostUrl)

    val regex = Regex("/([^/]+)\$") // Matches the last part after the slash
//    val matchResult = regex.find(redirectLink)
//    val extractedPart = matchResult?.groups?.get(1)?.value
//    val videoLink = "https://bradleyviewdoctor.com/e/${extractedPart}"


}

suspend fun getAnimeRedirectLink(redirectLink: String) {
    val document = getJsoup("$mainUrl/$redirectLink")
    val scriptTags = document.select("script")

    for (scriptTag in scriptTags) {
        val scriptCode = scriptTag.html()
        if (scriptCode.contains("var sources")) {
            // sources obyektini topsangiz, uni Kotlin obyektiga o'tkazish mumkin
            val hlsUrl = extractValue(scriptCode, "'hls': '(.*?)'")
            val videoHeight = extractValue(scriptCode, "'video_height': (\\d+)")

            // Natijalarni chiqaring
            println("HLS URL: $hlsUrl")
            println("Video Height: $videoHeight")
        }
    }


}

fun extractValue(input: String, regex: String): String {
    val matchResult = Regex(regex).find(input)
    return matchResult?.groupValues?.get(1) ?: ""
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




suspend fun setLink(url: String): EpisodeFullData {
    var episodeFullData = EpisodeFullData("", "", "")
    val document = getJsoup("$mainUrl/$url")
    val hosterElements = document.getElementsByClass("watchEpisode")
    for (element in hosterElements) {
        val hoster = element.select("i").attr("title")
        val hosterUrl = element.attr("href")
        val hosterName = element.select("h4").text()
        if (hosterName == "VOE") {
            episodeFullData = EpisodeFullData(hosterName, hosterUrl, hoster)
        }
    }

    return episodeFullData
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