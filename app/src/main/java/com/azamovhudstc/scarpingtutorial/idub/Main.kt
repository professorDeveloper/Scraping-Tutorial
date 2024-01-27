package com.azamovhudstc.scarpingtutorial.idub

import com.azamovhudstc.scarpingtutorial.utils.Utils.getJsoup
import com.azamovhudstc.scarpingtutorial.utils.Utils.httpClient
import com.azamovhudstc.scarpingtutorial.utils.parser
import com.lagradost.nicehttp.Requests
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

private const val mainUrl = "https://idub.tv/"

class IdubBase{

    fun main(args: Array<String>) {
        runBlocking {
            val list = searchKdramma("Baxt")
            val selectedData = list.get(0)
            movieDetailByHref(href = selectedData.href)

        }
    }
    fun movieDetailByHref(href: String) {
        println("Parsing Detail....")
        println("====================================")
        val episodeList = parseEpisodesByHref(href)///
        println("========== Selected 1-Episode ==========")

        println(parseVideoTrailer(episodeList.get(0)))


    }

    fun parseEpisodesByHref(href: String): ArrayList<String> {
        val document = getJsoup(href)


        // Extracting episode URLs
        val episodeUrls: List<String> =
            document.select("section.buttttons div.kontaiher a.BatcoH").map { element ->
                element.attr("href")
            }

        // Print the list of episode URLs
        episodeUrls.forEach { println(it) }

        return episodeUrls as ArrayList<String>
    }


    fun parseVideoTrailer(href: String): VideoInfo {

        val document = getJsoup(href)
        val title: String = document.selectFirst("h2.fs__stitle")!!.text()

        val playerUrl: String =
            document.selectFirst("div.tabs__content.tabs__content_1.active iframe")!!.attr("src")

        val trailerUrl: String =
            document.selectFirst("div.tabs__content.tabs__content_2 iframe")?.attr("data-src")?:"empty :)"

        return VideoInfo(title, playerUrl, trailerUrl)

    }

    suspend fun searchKdramma(query: String): List<TVShow> = withContext(Dispatchers.IO) {
        println("Searching ... $query ")
        val requests = Requests(httpClient, responseParser = parser)
        val response = requests.get(
            mainUrl,
            params = mapOf("story" to query, "do" to "search", "subaction" to "search")
        )

        val document = Jsoup.parse(response.body!!.string())


        // Extracting information from the parsed HTML
        val tvShows: List<TVShow> = document.select("article.new-short").map { element ->
            val title: String = element.select("h3.new-short__title").text()
            val date: String = element.select("span.info__date").text()
            val year: String = element.select("span.info__xf--text:contains(год) + b a").text()
            val country: String =
                element.select("span.info__xf--text:contains(страна) + b a").text()
            val age: String = element.select("span.info__xf--text:contains(возраст) + b a").text()
            val description: String = element.select("div.info__descr").text()
            val ratingKP: String = element.select("div.kp__value").text()
            val ratingIMDb: String = element.select("div.imdb__value").text()
            val episodeCount: String =
                element.select("div.new-short__episode span.episode__value").first()?.text() ?: ""
            val seasonCount: String =
                element.select("div.new-short__episode span.episode__value + span.episode__text")
                    .text()
            val href: String = element.select("a.new-short__title--link").attr("href")
            val imageLink: String = element.select("img.new-short__poster--img").attr("data-src")

            TVShow(
                title = title,
                date = date,
                year = year,
                country = country,
                age = age,
                description = description,
                ratingKP = ratingKP,
                ratingIMDb = ratingIMDb,
                episodeCount = episodeCount,
                seasonCount = seasonCount,
                href = href,
                imageLink = imageLink
            )
        }

        // Print the list of TVShow instances
        tvShows.forEach {
            println("====================================")
            println(it)
            println("====================================")
        }

        return@withContext tvShows
    }
}
