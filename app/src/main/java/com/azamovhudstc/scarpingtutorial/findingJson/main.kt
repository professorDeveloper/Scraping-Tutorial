package com.azamovhudstc.scarpingtutorial.findingJson

import com.azamovhudstc.scarpingtutorial.ParsedData
import com.azamovhudstc.scarpingtutorial.utils.Utils
import com.azamovhudstc.scarpingtutorial.utils.parser
import com.lagradost.nicehttp.Requests
import kotlinx.coroutines.coroutineScope

suspend fun main() {
    coroutineScope {
        //So this sample Json is from https://swapi.dev/api/planets/1/
        //I am using this sample Json for testing
        // You can use Kotlin json converter and parse it to ParsedData
        val requests = Requests(baseClient = Utils.httpClient, responseParser = parser)
        val jsonString = requests.get("https://swapi.dev/api/planets/1/").parsed<ParsedData>() //Like this
        //so okay i will run
        println(jsonString.films)

    }
}