package com.azamovhudstc.scarpingtutorial.asilmedia

import com.azamovhudstc.scarpingtutorial.asilmedia.model.Media
import com.azamovhudstc.scarpingtutorial.asilmedia.model.MovieInfo
import com.azamovhudstc.scarpingtutorial.utils.Utils
import com.azamovhudstc.scarpingtutorial.utils.parser
import com.lagradost.nicehttp.Requests
import kotlinx.coroutines.runBlocking
import org.jsoup.select.Elements

private const val mainUrl = "http://asilmedia.org"


fun main(args: Array<String>) {

    runBlocking {
        val base =AsilMediaBase()
        base.searchMovieByName("")
    }
}
class AsilMediaBase {


    suspend fun searchMovieByName(query: String): ArrayList<MovieInfo> {
        val movieList = ArrayList<MovieInfo>()
        val searchRequest = Requests(baseClient = Utils.httpClient, responseParser = parser).post(
            mainUrl,
            data = mapOf(
                "story" to query,
                "do" to "search",
                "subaction" to "search"
            )
        )

        val document = searchRequest.document
        val articles = document.select("article.shortstory-item")
        for (article in articles) {
            val genre = article.select("div.genre").text()
            val rating = article.select("span.ratingplus").text()
            val title = article.select("header.moviebox-meta h2.title").text()
            val image = article.select("picture.poster-img img").attr("data-src")
            val href = article.select("a.flx-column-reverse").attr("href")
            val quality = article.select("div.badge-tleft span.is-first").eachText()
            val year = article.select("div.year a").text()

            val movieInfo = MovieInfo(genre, rating, title, image, href, quality, year)
            movieList.add(movieInfo)
        }

        // You can now work with the list of MovieInfo objects
        for (movieInfo in movieList) {
            println("Genre: ${movieInfo.genre}")
            println("Rating: ${movieInfo.rating}")
            println("Title: ${movieInfo.title}")
            println("Image: ${mainUrl+movieInfo.image}")
            println("Href: ${movieInfo.href}")
            println("Quality: ${movieInfo.quality}")
            println("Year: ${movieInfo.year}")
            println("---------------")
        }
        return movieList

    }

    fun getMovieList(): ArrayList<Media> {
        val list = arrayListOf<Media>()
        val doc = Utils.getJsoup("$mainUrl/films/tarjima_kinolar/")
        val elements = doc.getElementById("dle-content")

        val articleElements = elements!!.select("article")
        for (document in articleElements) {
            val titleElement = document.select("h2.is-6.txt-ellipsis.mb-2").first()
            val linkElement: Elements = document.select("a.flx.flx-column.flx-column-reverse")
            val titleText = titleElement?.text()
            val linkHref: String? = linkElement.attr("href")
            val imageLink = document.select("div.poster picture.poster-img img").attr("data-src")
            list.add(Media(titleText!!, linkHref!!, imageLink))
            println("---------------------------------------")
            println("Title -> $titleText")
            println("Link -> $linkHref")
            println("Image Link -> $mainUrl$imageLink")
            println("---------------------------------------")
        }
        return list
    }

    fun getMovieDetails(href: String) {
        val document = Utils.getJsoup(href)
        val downloadList: Elements = document.select("#download1 .downlist-inner a[href]")

        // Iterate through download links and print the URLs
        downloadList.forEachIndexed { index, element ->
            val fileUrl = element.attr("href")
            val parsedUrl = parseUrl(fileUrl)
            println("Download URL $index: $parsedUrl")
        }


        // Extract the video URL from the iframe inside the "cn-content" div
        val videoDiv = document.selectFirst("#cn-content")
        val iframeElement = videoDiv?.selectFirst("iframe")
        val videoUrl = iframeElement?.attr("src")
        val parsedUrl = parseUrl(videoUrl!!)

        // Print the extracted video URL
        println("Video URL: $parsedUrl")
    }

    fun parseUrl(url: String): String? {
        // Split the URL using "?" as the delimiter
        val parts = url.split("?")

        // Check if there are two parts (base URL and parameters)
        if (parts.size == 2) {
            // Split the parameters using "&" as the delimiter
            val parameters = parts[1].split("&")

            // Find the parameter with "file=" prefix
            val fileParameter = parameters.find { it.startsWith("file=") }

            // Extract the value after "file=" prefix
            return fileParameter?.substringAfter("file=")
        }

        return null
    }

}
