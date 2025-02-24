package com.azamovhudstc.scarpingtutorial.itv

import com.azamovhudstc.scarpingtutorial.utils.Utils
import com.azamovhudstc.scarpingtutorial.utils.parser
import com.lagradost.nicehttp.Requests
import kotlinx.coroutines.runBlocking

val mainUrl = "https://api.itv.uz"
fun main(args: Array<String>) {

    runBlocking {
        val responseList = getFreeTvChannels()
        val list = responseList.data.filter { it.params.isFree }
        val freeChannel = list.get(2)
        getChannelDataByID(freeChannel)
    }

}



suspend fun getFreeTvChannels(): TvResponse {
    val niceHttp = Requests(baseClient = Utils.httpClient, responseParser = parser)
    val response =
        niceHttp.get("$mainUrl/v2/cards/channels/list?categoryId=8&itemsPerPage=0&moduleId=1")
            .parsed<TvResponse>()

    return response
}


suspend fun getChannelDataByID(tvResponse: Data) {
    val niceHttp = Requests(baseClient = Utils.httpClient, responseParser = parser)
    val currentTimeMillis = System.currentTimeMillis()

    val request = niceHttp.get(
        "$mainUrl/v2/cards/channels/show?channelId=${tvResponse.channelId}",
        headers =
        mapOf(
            "Itoken" to "6d7e4735846e243ab46a794e332f9519",
            "Referer" to "https://itv.uz/",
            "Ilogin" to "itv65869f5e66f28",
            "Idevice" to "K60-NEu4XqqucXbyV6aIb",
            "lauth" to "true",
            "lplatform" to "WebSite",
            "Itime" to currentTimeMillis.toString()
        ),
    )
    println(request.body!!.string())
    ///

}