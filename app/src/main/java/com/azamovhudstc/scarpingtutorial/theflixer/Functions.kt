package com.azamovhudstc.scarpingtutorial.theflixer

fun updateEndpoint(url: String): String {
    val baseUrl = "https://theflixertv.to"
    val path = url.removePrefix(baseUrl)

    return when {
        path.startsWith("/movie/") -> baseUrl + path.replace("/movie/", "/watch-movie/")
        path.startsWith("/tv/") -> baseUrl + path.replace("/tv/", "/watch-tv/")
        else -> url
    }
}
