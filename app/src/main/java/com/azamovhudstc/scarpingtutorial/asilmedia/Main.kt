package com.azamovhudstc.scarpingtutorial.asilmedia

import com.azamovhudstc.scarpingtutorial.asilmedia.model.FullMovieData
import com.azamovhudstc.scarpingtutorial.asilmedia.model.Media
import com.azamovhudstc.scarpingtutorial.asilmedia.model.MovieInfo
import com.azamovhudstc.scarpingtutorial.utils.Utils
import com.azamovhudstc.scarpingtutorial.utils.parser
import com.lagradost.nicehttp.Requests
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

private const val mainUrl = "http://asilmedia.org"
private const val host = "asilmedia.org"


fun main(args: Array<String>) {
    val movieList = ArrayList<MovieInfo>()


    runBlocking {
        val searchResponse =
            Jsoup.connect("$mainUrl/popular.html")
                .followRedirects(true)
                .headers(
                    mapOf(
                        "Content-Type" to "application/x-www-form-urlencoded",
                        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
                        "Host" to "asilmedia.org",
                        "Cache-Control" to "no-cache",
                        "Pragma" to "no-cache",
                        "Connection" to "keep-alive",
                        "Upgrade-Insecure-Requests" to "1",
                        "X-Requested-With" to "XMLHttpRequest"
                    )
                ).get()

        println(searchResponse)

        val base = AsilMediaBase()
        base.getLastNews()
    }
}

class AsilMediaBase {


    suspend fun searchMovieByName(query: String): ArrayList<MovieInfo> {
        val movieList = ArrayList<MovieInfo>()
        val searchRequest = Utils.getJsoupAsilMedia(
            params = mapOf(
                "story" to query,
                "do" to "search",
                "subaction" to "search"
            ), host = host, mapOfHeaders =
            mapOf(
                "Accept" to "/*",
                "Host" to "asilmedia.org",
                "Cache-Control" to "no-cache",
                "Pragma" to "no-cache",

                "Connection" to "keep-alive",
                "Upgrade-Insecure-Requests" to "1",

                )
        )

        val document = searchRequest
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
        var pathSegments = arrayListOf("films", "tarjima_kinolar")
        val doc = Utils.getJsoup(
            url = mainUrl + "/films/tarjima_kinolar", mapOfHeaders = mapOf(
                "Accept" to "/*",
                "Host" to "asilmedia.org",
                "Cache-Control" to "no-cache",
                "Pragma" to "no-cache",

                "Connection" to "keep-alive",
                "Upgrade-Insecure-Requests" to "1",

                )
        )
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

    suspend fun getMovieDetails(href: String) {
        val document = Jsoup.connect(href)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3")
            .followRedirects(true)
            .headers(
                mapOf(
                    "Content-Type" to "application/x-www-form-urlencoded",
                    "Accept" to "/*",
                    "Host" to "asilmedia.org",
                    "Cache-Control" to "no-cache",
                    "Pragma" to "no-cache",
                    "Connection" to "keep-alive",
                    "Upgrade-Insecure-Requests" to "1",

                    )
            ).method(Connection.Method.GET).execute().parse()
        println(href)
        println(document)
        val year: String =
            document.select("div.fullmeta-item span.fullmeta-label:contains(Год) + span.fullmeta-seclabel a")
                .text()
        val country: String =
            document.select("div.fullmeta-item span.fullmeta-label:contains(Страна) + span.fullmeta-seclabel a")
                .text()
        val durationElement = document.selectFirst(".fullmeta-item .fullmeta-seclabel a")?.text()
        val duration = durationElement?.replace(" мин", "")

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
        val nonRussianDescription = descriptionElements.text()


        val rating = document.select(".r-im.txt-bold500.pfrate-count").text()


        // Extract the video URL from the iframe inside the "cn-content" div
        val videoDiv = document.selectFirst("#cn-content")
        val iframeElement = videoDiv?.selectFirst("iframe")
        val videoUrl = iframeElement?.attr("src")
        val parsedUrl = parseUrl(videoUrl!!)
        //Buni Sindirish imkonsiz

        val data = FullMovieData(
            year = year,
            country = country,
            duration = duration ?: "",
            posterImageSrc = posterImageSrc,
            genres = genres,
            directors = directors,
            actors = actors,
            options = options.distinct().filterNot { it.second.toIntOrNull() != null },
            imageUrls = imageUrls,
            description = nonRussianDescription!!,
            videoUrl = parsedUrl!!,
            IMDB_rating = rating
        )
        println(options.get(0).second)
        println(options.get(0).first)

        val realLink = reGenerateMp4(parsedUrl)
        println(data.toString())
        println(realLink)


    }

    private suspend fun reGenerateMp4(link: String) = withContext(Dispatchers.IO) {
        val requests = Requests(baseClient = Utils.httpClient, responseParser = parser)
        val response = requests.get(
            link, headers = mapOf(
                "Accept" to "/*",
                "Host" to "asilmedia.org",
                "Cache-Control" to "no-cache",
                "Pragma" to "no-cache",

                "Connection" to "keep-alive",
                "Upgrade-Insecure-Requests" to "1",

                )
        )
        return@withContext response.url.toString()

    }

    fun extractMovieList(document: Document): List<MovieInfo> {
        val movieElements = document.select("article.shortstory-item")

        return movieElements.map { element ->
            extractMovieInfo(element)
        }
    }

    fun extractMovieInfo(element: org.jsoup.nodes.Element): MovieInfo {
        val title = element.select("h2.title.is-6.txt-ellipsis.mb-2").text()
        val year = element.select("div.year a").text()
        val genre = element.select("div.genre").text()
        val rating = element.select("span.ratingtypeplusminus.ratingplus").text()
        val image = element.select("img.img-fit").attr("data-src")
        val href = element.select("a[href]").attr("href")
        val quality = element.select("div.badge-tleft span.is-first").eachText()

        return MovieInfo(genre, rating, title, image, href, quality, year)
    }

    suspend fun getNeedWatch() {
        val document =
            Jsoup.connect(mainUrl + "/whatchnow.html")
                .followRedirects(true)
                .headers(
                    mapOf(
                        "Content-Type" to "application/x-www-form-urlencoded",
                        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
                        "Host" to "asilmedia.org",
                        "Cache-Control" to "no-cache",
                        "Pragma" to "no-cache",
                        "Connection" to "keep-alive",
                        "Upgrade-Insecure-Requests" to "1",
                        "X-Requested-With" to "XMLHttpRequest"
                    )
                ).get()
        val movieList = extractMovieList(document)

        movieList.forEach { movieInfo ->
            println(movieInfo)
            println("-------------")
        }


    }

//    suspend fun

    suspend fun getPopularData() {
        val document =
            Jsoup.connect(mainUrl)
                .followRedirects(true)
                .headers(
                    mapOf(
                        "Content-Type" to "application/x-www-form-urlencoded",
                        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
                        "Host" to "asilmedia.org",
                        "Cache-Control" to "no-cache",
                        "Pragma" to "no-cache",
                        "Connection" to "keep-alive",
                        "Upgrade-Insecure-Requests" to "1",
                        "X-Requested-With" to "XMLHttpRequest"
                    )
                ).get()
        val movieItems: List<Element> = document.select(".slider-item.moviebox")

        val movieList = movieItems.map { movieItem ->
            val title: String = movieItem.select(".title").text()
            val year: String = movieItem.select(".year a").text()
            val genre: String = movieItem.select(".genre").text()
            val rating: String = movieItem.select(".ratingtypeplusminus").text()
            val image: String = movieItem.select(".img-fit").attr("data-src")
            val href: String = movieItem.select("a").attr("href")
            val quality: List<String> = movieItem.select(".badge-tleft span").eachText()
            MovieInfo(
                genre = genre,
                rating = rating,
                title = title,
                image = image,
                href = href,
                quality = quality,
                year = year
            )
        }

        for (movieInfo in movieList) {
            println(movieInfo)
        }


    }

    fun getLastPagination(page: Int) {
        val document =
            Jsoup.connect("$mainUrl/lastnews/page/$page")
                .followRedirects(true)
                .headers(
                    mapOf(
                        "Content-Type" to "application/x-www-form-urlencoded",
                        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
                        "Host" to "asilmedia.org",
                        "Cache-Control" to "no-cache",
                        "Pragma" to "no-cache",
                        "Connection" to "keep-alive",
                        "Upgrade-Insecure-Requests" to "1",
                        "X-Requested-With" to "XMLHttpRequest"
                    )
                ).get()
        val movieList = extractMovieList(document)

        movieList.forEach { movieInfo ->
            println(movieInfo)
            println("-------------")
        }
    }


    fun getLastNews() {
        val document =
            Jsoup.connect(mainUrl + "/lastnews/")
                .followRedirects(true)
                .headers(
                    mapOf(
                        "Content-Type" to "application/x-www-form-urlencoded",
                        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
                        "Host" to "asilmedia.org",
                        "Cache-Control" to "no-cache",
                        "Pragma" to "no-cache",
                        "Connection" to "keep-alive",
                        "Upgrade-Insecure-Requests" to "1",
                        "X-Requested-With" to "XMLHttpRequest"
                    )
                ).get()
        val movieList = extractMovieList(document)

        movieList.forEach { movieInfo ->
            println(movieInfo)
            println("-------------")
        }
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


