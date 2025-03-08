package com.azamovhudstc.scarpingtutorial.moviefone

import com.azamovhudstc.scarpingtutorial.utils.Utils
import com.azamovhudstc.scarpingtutorial.utils.Utils.getJsoup
import org.jsoup.nodes.Document

fun main() {
    val selectedTrailer = searchMovieByVideoFon("absolution")[0]
    println(getMovieVideoLink(selectedTrailer))
}

fun getMovieVideoLink(data: SearchItem): String {
    var m3u8Link = "https://cdn.jwplayer.com/manifests/"
    val doc = getJsoup(
        data.url, mapOf(
            "authority" to "www.moviefone.com",
            "accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9",
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
        )
    )
    val mediaVideoLink = doc.select("#mf-trailer-jwplayer")
        .firstOrNull()?.attr("data-media-id")?.let {
            "https://cdn.jwplayer.com/v2/media/$it"
        }

    if (mediaVideoLink != null) {
        val videoData = VideoData(mediaVideoLink = mediaVideoLink)
        println("1-ko'rinishdan topildi: $videoData")
        m3u8Link += mediaVideoLink.substringAfterLast("/") + ".m3u8"
        return m3u8Link
    }

        return ""
}

fun searchMovieByVideoFon(query: String): List<SearchItem> {
    val doc: Document = Utils.getJsoup(
        "https://www.moviefone.com/search/?q=${query}", mapOfHeaders =
        mapOf(
            "authority" to "www.moviefone.com",
            "accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
//            "Cookie" to "_ga=GA1.1.1163975711.1740377515; cto_bundle=nZXLDV9CUG0zRnBwUHBjcHhCd0EzQ1pPVDNzVXV0QWRnMWpmNlpGQ0dxN1NCcjk5YkdUYXlBdE4lMkI4UFRTTjEyODE1Z3dDbFY3WFY2c3ZnUFhSaXJNdG1DQzM4blVhJTJGdmxidEhzQm9NdGtEWXVsckdEY3g2TmZDeExGQTZUZHFqWXFGWiUyQmI1YlhjM3ZJR25KajcyQkxUUDV6cXclM0QlM0Q; __gads=ID=879fbd3ff115d0cf:T=1740377538:RT=1740377858:S=ALNI_MaYaqhjNZB37hhT-sOPZK4eZ7rkLw; __eoi=ID=9e2019d17d122a1e:T=1740377538:RT=1740377858:S=AA-AfjbGjWmuJ0Jj5BSTkQafTmmR; XSRF-TOKEN=eyJpdiI6IkhuUFZoa2JCbDBQSDRGYmFweWhTa2c9PSIsInZhbHVlIjoiWFJCdStOVTZRakpMcWZZV1JRVENudExVNlRmVVF4Rk9BVzBuQnVCNForeHI1Wk92ckxjdjJWalArZG9jVkttdSIsIm1hYyI6Ijg3ZWRjMzA4YzcyZDQzYzJkNDEzOTRmMThmYmQyMTM4Y2ZjYWM3YjQzZDg2NTc3M2M3MjE5N2Y4NzZlMTIwMjkifQ%3D%3D; moviefone_session=eyJpdiI6IkhoNE9MNFB5UzdlOXdzc0pqdXJ4ekE9PSIsInZhbHVlIjoiZVFFZ285WlNKYmJiVlhBaTFIblJFcUhBYXJOWDBRTHh5Z2lIQmdmNU5vYW5WTGdweGFcL3J5aWhNeFZwWWR3YksiLCJtYWMiOiJjNjZiYzYzYzE2NTE2NWNiYjcwNTE0MWU1MGZiY2NmNTcyNmNiMTQ0Y2RkYjkxM2UzMDA2OTRkZGE3MzNhYTRjIn0%3D; _ga_7V3J010SY0=GS1.1.1740377515.1.1.1740377932.28.0.0"
        )
    )
    val searchItems = doc.select(".search-item").map { element ->
        val title = element.select(".search-asset-title a").text()
        val url = element.select(".search-asset-title a").attr("href")
        val imageUrl = element.select(".search-image").attr("data-src")
        val description = element.select(".search-desc").text()
        val type = element.select(".search-type").text()
        SearchItem(
            title = title,
            url = url,
            imageUrl = imageUrl,
            description = description,
            type = type
        )
    }

    // Natijani chiqaramiz
    searchItems.forEach {
        println(it)
    }

    return searchItems

}

data class SearchItem(
    val title: String,
    val url: String,
    val imageUrl: String,
    val description: String,
    val type: String
)

data class VideoData(
    val mediaVideoLink: String? = null,
    val playlistId: String? = null
)
