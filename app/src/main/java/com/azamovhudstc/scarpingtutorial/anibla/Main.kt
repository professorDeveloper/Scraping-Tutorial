package com.azamovhudstc.scarpingtutorial.anibla

import com.azamovhudstc.scarpingtutorial.anibla.detail.DetailData
import com.azamovhudstc.scarpingtutorial.utils.*
import com.lagradost.nicehttp.Requests
import kotlinx.coroutines.runBlocking

private const val hostUrl = "https://amediatv.uz/"
private const val imageHostUrl = "https://cdn.amediatv.uz/"

fun main(args: Array<String>) {
    val amediaTvBase = AmediaTvBase()

    runBlocking {
        val data = amediaTvBase.searchByQuery("Naruto")
        showData(data)

        printlnColored("Selected Anime ${data.data.get(0).name.uz}", Color.GREEN)
        val detailData = amediaTvBase.getFullDataById(data.data.get(0))
        println("Anime Name : ${detailData.data.name.uz}")
        println("Anime Created : ${detailData.data.createdAt}")
        println("Anime Video Stream Url : ${detailData.data.video}")
        println("Anime Download Url : ${detailData.data.url}")
        println("Anime Studios : ${detailData.data.studia}")
        printlnColored("================= Episodes ================", Color.GREEN)

        detailData.seria.reversed().forEach {
            printlnColored("Episode Name : ${it.name.uz}", Color.YELLOW)
            printlnColored("Episode Length : ${it.length}", Color.YELLOW)
        }

        printlnColored("Selected 1-Episode", Color.GREEN)

        val episode = detailData.seria.get(0)
        amediaTvBase.getM3u8File(episode.video,episode._id)
    }
}


class AmediaTvBase {

    private val requests = Requests(baseClient = Utils.httpClient, responseParser = parser)

    suspend fun getFullDataById(data: Data): DetailData {
        displayLoadingAnimation("Paring Detail...",Color.GREEN)
        val responseData =
            requests.get("https://cdn.amediatv.uz/api/season/v2/${data._id}").parsed<DetailData>()

        return responseData
    }


    suspend fun getM3u8File(href: String, dataId: String) {
        displayLoadingAnimation("Parsing File....",Color.GREEN)
        val document = Utils.getJsoup(href)

        val fileAttributeValue = extractFileAttributeValue(document!!.html())
        println("File Attribute Value: $fileAttributeValue")

    }

 private   fun extractFileAttributeValue(scriptContent: String): String {
     val regex = """file\s*:\s*['"]([^'"]+)['"]""".toRegex()
        val matchResult = regex.find(scriptContent)
        return matchResult?.groupValues?.get(1) !!
    }

    suspend fun searchByQuery(query: String): AmediaSearchData {
        displayLoadingAnimation("Searching Anime", Color.GREEN)

        val data = requests.get("https://cdn.amediatv.uz/api/season/search?text=$query")
            .parsed<AmediaSearchData>()

        return data
    }
}
