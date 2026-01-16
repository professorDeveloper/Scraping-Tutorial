package com.azamovhudstc.scarpingtutorial.elevenMovies

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.Gson
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.extractorApis
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.schemaStripRegex
import com.lagradost.cloudstream3.utils.unshortenLinkSafe
import kotlinx.coroutines.runBlocking

data class PrimeSrcServerList(
    val servers: List<PrimeSrcServer>,
)

data class PrimeSrc(
    val link: String
)

data class PrimeSrcServer(
    val name: String,
    val key: String,
    @JsonProperty("file_size")
    val fileSize: String?,
    @JsonProperty("file_name")
    val fileName: String?,
)

const val PrimeSrcApi = "https://primesrc.me"
suspend fun invokePrimeSrc(
    imdbId: String? = null,
    season: Int? = null,
    episode: Int? = null,
    subtitleCallback: (SubtitleFile) -> Unit,
    callback: (ExtractorLink) -> Unit
) {

    val headers = mapOf(
        "accept" to "*/*",
        "referer" to if (season == null) "$PrimeSrcApi/embed/movie?imdb=$imdbId" else "$PrimeSrcApi/embed/tv?imdb=$imdbId&season=$season&episode=$episode",
        "sec-ch-ua" to "\"Chromium\";v=\"140\", \"Not=A?Brand\";v=\"24\", \"Google Chrome\";v=\"140\"",
        "sec-ch-ua-mobile" to "?0",
        "sec-ch-ua-platform" to "\"Windows\"",
        "sec-fetch-dest" to "empty",
        "sec-fetch-mode" to "cors",
        "sec-fetch-site" to "same-origin",
        "user-agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36"
    )
    val url = if (season == null) {
        "$PrimeSrcApi/api/v1/s?imdb=$imdbId&type=movie"
    } else {
        "$PrimeSrcApi/api/v1/s?imdb=$imdbId&season=$season&episode=$episode&type=tv"
    }

    val serverList =
        app.get(url, timeout = 30, headers = headers).parsedSafe<PrimeSrcServerList>()
    val it = serverList?.servers?.firstOrNull() ?: return
    val rawServerJson =
        app.get("$PrimeSrcApi/api/v1/l?key=${it.key}", timeout = 30, headers = headers).text
    println(
        rawServerJson
    )
    println("$PrimeSrcApi/api/v1/l?key=${it.key}")
    val jsonObject = Gson().fromJson(rawServerJson, PrimeSrc::class.java)
    loadSourceNameExtractor(
        "PrimeWire${if (it.fileName.isNullOrEmpty()) "" else " (${it.fileName}) "}",
        jsonObject.link,
        PrimeSrcApi,
        subtitleCallback,
        callback,
        null,
        it.fileSize ?: ""
    )


}

suspend fun loadSourceNameExtractor(
    source: String,
    url: String,
    referer: String? = null,
    subtitleCallback: (SubtitleFile) -> Unit,
    callback: (ExtractorLink) -> Unit,
    quality: Int? = null,
    size: String = ""
) {
    println("GGG")
    println(loadExtractor(url, referer, subtitleCallback) {

    })
}

fun main() {
    runBlocking {
        val imdbId = "tt1375666"
        invokePrimeSrc(
            imdbId = imdbId,
            season = null,
            episode = null,
            subtitleCallback = {},
            callback = {})
    }
}
