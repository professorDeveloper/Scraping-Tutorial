package com.azamovhudstc.scarpingtutorial.uzmovi

import com.azamovhudstc.scarpingtutorial.utils.Utils
import com.azamovhudstc.scarpingtutorial.uzmovi.movie.ParsedMovie

private val mainUrl = "http://uzmovi.com/"

suspend fun main() {
    val list = searchMovie("Sening Isming")
    println(list.toString())
}

suspend fun searchMovie(query: String): ArrayList<ParsedMovie> {
    val list = arrayListOf<ParsedMovie>()
    val searchUrl = "$mainUrl/search?q=$query"
    val doc = Utils.getJsoup(searchUrl) //REQUEST SEARCH
    val movieContent = doc.getElementsByClass("shortstory-in categ")//GET ANIME LIST

    for (movie in movieContent) {
        val movieName = movie.getElementsByClass("short-link").text() //GET ANIME NAME
        val movieCover = movie.getElementsByTag("img").attr("data-src") // GET ANIME COVER
        val movieHref =
            movie.getElementsByClass("short-link").select("h4.short-link a").attr("href")

        list.add(ParsedMovie(movieName, movieHref, movieCover))
    }

    return list

}