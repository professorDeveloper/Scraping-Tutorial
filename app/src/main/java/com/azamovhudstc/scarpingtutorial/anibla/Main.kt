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
        val data = amediaTvBase.searchByQuery("Bir")
        showData(data)

        printlnColored("Selected Anime ${data.data.get(0).name.uz}", Color.GREEN)
        val detailData = amediaTvBase.getFullDataById(data.data.get(0))
        println(detailData.seria.toString())
    }
}


class AmediaTvBase {

    private val requests = Requests(baseClient = Utils.httpClient, responseParser = parser)

    suspend fun getFullDataById(data: Data): DetailData {
        val responseData = requests.get("https://cdn.amediatv.uz/api/season/v2/${data._id}").parsed<DetailData>()

        return responseData
    }

    suspend fun searchByQuery(query: String): AmediaSearchData {
        displayLoadingAnimation("Searching Anime", Color.GREEN)

        val data = requests.get("https://cdn.amediatv.uz/api/season/search?text=$query")
            .parsed<AmediaSearchData>()

        return data
    }
}
