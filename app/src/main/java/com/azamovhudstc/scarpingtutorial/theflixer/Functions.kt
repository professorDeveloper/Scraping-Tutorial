package com.azamovhudstc.scarpingtutorial.theflixer

import com.azamovhudstc.scarpingtutorial.utils.Utils

fun updateEndpoint(url: String): String {
    val baseUrl = "https://theflixertv.to"
    val path = url.removePrefix(baseUrl)

    return when {
        path.startsWith("/movie/") -> baseUrl + path.replace("/movie/", "/watch-movie/")
        path.startsWith("/tv/") -> baseUrl + path.replace("/tv/", "/watch-tv/")
        else -> url
    }
}

fun mapToFilm(map: Map<String, String>): Film {
    return Film(
        title = map["title"] ?: "",
        year = map["year"] ?: "",
        type = map["type"] ?: "",
        posterUrl = map["posterUrl"] ?: "",
        watchUrl = map["watchUrl"] ?: ""
    )
}


fun addLineBetweenWords(text: String, line: String): String {
    val words = text.split(" ")
    val newText = words.joinToString(line)
    return newText
}

fun parseRatingInfo(id: String,mainUrl:String): RatingInfo? {
    val document = Utils.getJsoup("$mainUrl/ajax/vote_info/$id")

    return try {
        val title = document.select(".rs-title").text()
        val totalVotes = document.select(".rr-mark").text().split(" ")[0].toInt()
        val ratingPercentage = document.select(".progress-bar").attr("style")
            .split("width: ")[1]
            .split("%;")[0]
            .toDouble()

        RatingInfo(title, totalVotes, ratingPercentage)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
