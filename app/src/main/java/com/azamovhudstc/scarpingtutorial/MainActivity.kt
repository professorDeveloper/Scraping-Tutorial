package com.azamovhudstc.scarpingtutorial

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.nicehttp.Requests
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

    }
}


suspend fun main() {
    coroutineScope {
        val requests = Requests(okHttpClient, responseParser = parser)
        val jsonString = requests.get("https://swapi.dev/api/planets/1/").parsed<ParsedData>()
        println(jsonString.created)

    }
}


fun <A, B> List<A>.asyncMap(f: suspend (A) -> B): List<B> = runBlocking {
    map { async { f(it) } }.map { it.await() }
}