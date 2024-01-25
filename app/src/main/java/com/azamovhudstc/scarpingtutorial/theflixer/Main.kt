package com.azamovhudstc.scarpingtutorial.theflixer

import com.azamovhudstc.scarpingtutorial.utils.Utils.getJsoup
import kotlinx.coroutines.runBlocking

private const val mainUrl = "https://theflixertv.to"

fun main(args: Array<String>) {
    runBlocking {
        val list = searchMovieByQuery("Wednesday")
        val tvShow = getDetailFullMovie(list.get(0).watchUrl)
        println(tvShow.dataId)
        println(tvShow.posterUrl)
        println(tvShow.bannerUrl)
        val map = getSeasonList(tvShow.dataId)
        val season1Episodes = getEpisodeBySeason(map.get(map.keys.first())!!)
        println(season1Episodes.get(0).title)
        println(season1Episodes.get(0).id)

    }
}


fun parseRatingInfo(id: String): RatingInfo? {
    val document = getJsoup("$mainUrl/ajax/vote_info/$id")

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

fun getEpisodeBySeason(seasonId: String): ArrayList<Episode> {
    val document = getJsoup("$mainUrl/ajax/season/episodes/${seasonId}")
    val episodesList = arrayListOf<Episode>()

    // Select all the elements with class "flw-item film_single-item episode-item eps-item"
    val episodeItems = document.select(".flw-item.film_single-item.episode-item.eps-item")

    // Iterate through the items and extract episode information
    for (episodeItem in episodeItems) {
        val id = episodeItem.attr("id")
        val dataId = episodeItem.attr("data-id")
        val episodeNumber = episodeItem.select(".episode-number").text()
        val title = episodeItem.select(".film-name a").attr("title")
        val imageUrl = episodeItem.select(".film-poster-img").attr("src")

        // Create an Episode object and add it to the list
        episodesList.add(Episode(id, dataId, episodeNumber, title, imageUrl))
    }

    return episodesList
}


fun getSeasonList(id: String): Map<String, String> {
    val document = getJsoup("$mainUrl/ajax/season/list/$id")
    val seasonsMap = mutableMapOf<String, String>()
    val seasonItems = document.select(".dropdown-item.ss-item")
    for (seasonItem in seasonItems) {
        val dataId = seasonItem.attr("data-id")
        val seasonName = seasonItem.text()
        seasonsMap[seasonName] = dataId
    }
    return seasonsMap
}

suspend fun getDetailFullMovie(href: String): TVShow {
    val document = getJsoup("$mainUrl$href")
    val dataId = document.select("div.detail_page-watch").attr("data-id")
    val title = document.select("h2.heading-name a").text()
    val year = document.select("div.row-line span.type:contains(Released) + span").text()
    val type = "TV" // Assuming it's always a TV show based on the provided HTML
    val posterUrl = document.select("div.film-poster img.film-poster-img").attr("src")
    val overview = document.select("div.description").text()
    val released = document.select("div.row-line span.type:contains(Released) + span").text()
    val genres = document.select("div.row-line span.type:contains(Genre) + a").eachText()
    val casts = document.select("div.row-line span.type:contains(Casts) + a").eachText()
    val duration = document.select("div.row-line span.type:contains(Duration) + span").text()
    val country = document.select("div.row-line span.type:contains(Country) + a").text()
    val production = document.select("div.row-line span.type:contains(Production) + a").text()
    val banner = document.select("meta[property=og:image]").attr("content")
    return TVShow(
        dataId = dataId,
        title = title,
        year = year,
        type = type,
        posterUrl = posterUrl,
        bannerUrl = banner,
        overview = overview,
        released = released,
        genres = genres,
        casts = casts,
        duration = duration,
        country = country,
        production = production,
        ratingInfo = parseRatingInfo(dataId)!!
    )

}


private fun searchMovieByQuery(query: String): ArrayList<Film> {
    val list = ArrayList<Film>()
    println(
        "------------------------------ ${
            addLineBetweenWords(
                query,
                "-"
            )
        }------------------------------\n"
    )

    println("Finding Movie......")
    val document =
        getJsoup(
            "$mainUrl/search/${
                addLineBetweenWords(
                    query,
                    "-"
                )
            }"
        ).getElementsByClass("block_area-content block_area-list film_list film_list-grid")
    for (filmItem in document.select("div.film_list-wrap > div.flw-item")) {
        val filmData = mapOf(
            "title" to filmItem.select("h2.film-name a").attr("title"),
            "year" to filmItem.select("span.fdi-item:eq(0)").text(),
            "type" to filmItem.select("span.fdi-item strong").text(),
            "posterUrl" to filmItem.select("img.film-poster-img").attr("data-src"),
            "watchUrl" to filmItem.select("a.film-poster-ahref").attr("href")
        )
        val filmDataClass = mapToFilm(filmData)
        list.add(filmDataClass)

    }
    return list
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