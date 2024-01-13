package com.azamovhudstc.scarpingtutorial.uzmovi

import com.azamovhudstc.scarpingtutorial.utils.Utils
import com.azamovhudstc.scarpingtutorial.utils.Utils.getJsoup
import com.azamovhudstc.scarpingtutorial.uzmovi.movie.ParsedMovie

private val mainUrl = "http://uzmovi.com/"

 fun main() {
    val list = searchMovie("Yigit So`zi")
    animeDetails(list.get(0)) /// Add Trailer Scarping
}

 fun animeDetails(parsedMovie: ParsedMovie) {
    println(parsedMovie.href)
    val doc = getJsoup(parsedMovie.href)
    val tabPaneElement = doc.select(".tab-pane.fade.in.active").first()

    if (tabPaneElement!!.id().equals("online1")) {
        val totalEpisodeList = doc.getElementById("online1")!!.select("a.BatcoH.BatcoH-5")
        for (episode in totalEpisodeList) {
            val episodeLinks = episode.select("a.BatcoH.BatcoH-5")
            println(episode.text())
            println(episodeLinks.attr("href"))
        }
    } else {
        val totalEpisodeList =
            doc.getElementById("full-video")!!.getElementsByTag("script").select("script").get(0)
        val regex = """file:"([^"]+)",\s*poster:"([^"]+)".*""".toRegex()
        val matchResult = regex.find(totalEpisodeList.data())
        val (file, poster) = matchResult!!.destructured
        println(file)
        println(poster)

    }


}



 fun searchMovie(query: String): ArrayList<ParsedMovie> {
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