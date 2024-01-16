package com.azamovhudstc.scarpingtutorial.findingJson

import com.azamovhudstc.scarpingtutorial.ParsedData
import com.azamovhudstc.scarpingtutorial.utils.Utils
import com.azamovhudstc.scarpingtutorial.utils.parser
import com.lagradost.nicehttp.Requests
import kotlinx.coroutines.coroutineScope

suspend fun main() {
    coroutineScope {
        val requests = Requests(baseClient = Utils.httpClient, responseParser = parser)
        val jsonString = requests.get("https://swapi.dev/api/planets/1/").parsed<ParsedData>()
        println(jsonString.created)

    }
}