package com.azamovhudstc.scarpingtutorial.asilmedia

import com.azamovhudstc.scarpingtutorial.asilmedia.model.FullMovieData
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
        val base = AsilMediaBase()
        val list = base.searchMovieByName("Ajobtovur Ishlar")
        var selectedMovie = base.getMovieDetails(list.get(0).href)
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
            println("Image: ${mainUrl + movieInfo.image}")
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
        val year: String =
            document.select("div.fullmeta-item span.fullmeta-label:contains(Год) + span.fullmeta-seclabel a")
                .text()
        val country: String =
            document.select("div.fullmeta-item span.fullmeta-label:contains(Страна) + span.fullmeta-seclabel a")
                .text()
        val duration: String =
            document.select("div.fullmeta-item span.fullmeta-label:contains(Продолжительность) + span.fullmeta-seclabel a")
                .text()

        val posterImageSrc: String =
            document.select("div.poster picture.poster-img img.lazyload").attr("data-src")

        // Extracting information from the parsed HTML
        val genres: List<Pair<String, String>> =
            document.select("div.fullinfo-list span.list-label:contains(Жанры) + span a")
                .map { Pair(it.text(), it.attr("href")) }

        val directors: List<Pair<String, String>> =
            document.select("div.fullinfo-list span.list-label:contains(Режиссер) + span a")
                .map { Pair(it.text(), it.attr("href")) }

        val actors: List<Pair<String, String>> =
            document.select("div.fullinfo-list span.list-label:contains(Актеры) + span a")
                .map { Pair(it.text(), it.attr("href")) }

        val pattern = Regex("""<option value="([^"]+)">(.*?)</option>""")

        // Find all matches in the HTML content
        val matches = pattern.findAll(document.html())

        // Process the matches
        val options = matches.map {
            val value = it.groupValues[1]
            val text = it.groupValues[2]
            Pair(text, value)
        }.toList()

        val imageUrls = document.select(".xfieldimagegallery img.lazyload[data-src]")
            .map { it.attr("data-src") }

        val descriptionElements = document.select("span[itemprop=description]")
        val regex = Regex("\\b[а-яА-Я]+\\b")
        val text = descriptionElements.select("div > p").first()?.text() ?: ""
        val nonRussianDescription =regex.replace(text, "").trim()

// Filter out paragraphs with Russian characters

        println("Description: $nonRussianDescription")

        // Extension function to check if a character is Russian


        // Extract the video URL from the iframe inside the "cn-content" div
        val videoDiv = document.selectFirst("#cn-content")
        val iframeElement = videoDiv?.selectFirst("iframe")
        val videoUrl = iframeElement?.attr("src")
        val parsedUrl = parseUrl(videoUrl!!)
        val data = FullMovieData(
            year = year,
            country = country,
            duration = duration,
            posterImageSrc = posterImageSrc,
            genres = genres,
            directors = directors,
            actors = actors,
            options = options.distinct(),
            imageUrls = imageUrls,
            description = nonRussianDescription!!,
            videoUrl = parsedUrl!!
        )
        println(data.toString())

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

fun Char.isRussian(): Boolean {
    return this in '\u0400'..'я' || this == 'ё' || this == 'Ё'
}