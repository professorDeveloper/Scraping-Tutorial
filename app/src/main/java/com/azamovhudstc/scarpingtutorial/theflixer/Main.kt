package com.azamovhudstc.scarpingtutorial.theflixer

import com.azamovhudstc.scarpingtutorial.utils.Color
import com.azamovhudstc.scarpingtutorial.utils.Utils
import com.azamovhudstc.scarpingtutorial.utils.Utils.getJsoup
import com.azamovhudstc.scarpingtutorial.utils.Utils.httpClient
import com.azamovhudstc.scarpingtutorial.utils.parser
import com.azamovhudstc.scarpingtutorial.utils.printlnColored
import com.lagradost.nicehttp.Requests
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jsoup.nodes.Element



class TheFlixerBase() {
     val mainUrl = "https://theflixertv.to"
    suspend fun getSources(pairData: Pair<String, String>) {

        val requests = Requests(baseClient = httpClient, responseParser = parser)
        val response = requests.get(
            url = "https://rabbitstream.net/ajax/embed-4/getSources?id=${pairData.second}",
            headers = mapOf(
                "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
                "Sec-Fetch-Dest" to "iframe",
                "Sec-GPC" to "1",
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:101.0) Gecko/20100101 Firefox/101.0",
                "Sec-Fetch-Mode" to "navigate",
                "Sec-Fetch-Site" to "cross-site",
                "Upgrade-Insecure-Requests" to "1",
                "Referer" to "https://rabbitstream.net/embed-4/BI6Zv2yJxZBq?z=",
                "X-Requested-With" to "XMLHttpRequest"
            )
        )

        println(response.body.string())

    }


    fun main(args: Array<String>) {
        runBlocking {
            val list = searchMovieByQuery("Stranger things")
            val searchedMovie = list.get(0)
            val tvShow = getDetailFullMovie(searchedMovie.watchUrl)
            println("  Data ID: ${tvShow.dataId}")
            println("  Title: ${tvShow.title}")
            println("  Year: ${tvShow.year}")
            println("  Type: ${tvShow.type}")
            println("  Banner URL: ${tvShow.bannerUrl}")
            println("  Rating Info: ${tvShow.ratingInfo}")
            println("  Poster URL: ${tvShow.posterUrl}")
            println("  Released: ${tvShow.released}")
            println("  Genres: ${tvShow.genres.joinToString(", ")}")
            println("  Casts: ${tvShow.casts.joinToString(", ")}")
            println("  Duration: ${tvShow.duration}")
            println("  Country: ${tvShow.country}")
            println("  Production: ${tvShow.production}")

            val map = getSeasonList(tvShow.dataId)
            val season1Episodes = getEpisodeBySeason(map.get(map.keys.first())!!)

            val episode = season1Episodes.get(0)
            val sourceList = getEpisodeVideoByLink(episode.dataId, mainUrl + searchedMovie.watchUrl)


            println("Server Name : ${sourceList.get(0).serverName}")
            println("Server Id : ${sourceList.get(0).dataId}")

            val pairData = checkM3u8FileByLink(sourceList.get(0))
            getSources(pairData)
        }
    }
    suspend fun checkM3u8FileByLink(episodeData: EpisodeData): Pair<String, String> =
        withContext(Dispatchers.IO) {
            val document = getJsoup("$mainUrl/ajax/episode/sources/${episodeData.dataId}")
            val json = document.body().select("body").text()
            val regex = """"link"\s*:\s*"([^"]+)"""".toRegex()
            val matchResult = regex.find(json)

            val link = matchResult?.groupValues?.get(1) ?: ""
            val request = Requests(baseClient = httpClient, responseParser = parser)

            val response = request.get(
                link,
                headers = mapOf(
                    "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
                    "Sec-Fetch-Dest" to "iframe",
                    "Sec-GPC" to "1",
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:101.0) Gecko/20100101 Firefox/101.0",
                    "Sec-Fetch-Mode" to "navigate",
                    "Sec-Fetch-Site" to "cross-site",
                    "Upgrade-Insecure-Requests" to "1",
                    "Referer" to "https://theflixertv.to/",
                )
            )

            val html = response.body.string()

            val realIdRegex = """data-realid="([^"]+)"""".toRegex()
            val dataIdRegex = """data-id="([^"]+)"""".toRegex()

            val realIdMatchResult = realIdRegex.find(html)
            val dataIdMatchResult = dataIdRegex.find(html)

            val realId = realIdMatchResult?.groupValues?.get(1) ?: ""
            val dataId = dataIdMatchResult?.groupValues?.get(1) ?: ""

            println("Real ID: $realId")
            println("Data ID: $dataId")

            return@withContext Pair(realId, dataId)

        }


    fun getEpisodeVideoByLink(id: String, detailFullLink: String): ArrayList<EpisodeData> {
        val sourceList = ArrayList<EpisodeData>()


        runBlocking {
            val document = getJsoup("https://theflixertv.to/ajax/episode/servers/$id")
            val episodeElements: List<Element> = document.select(".ulclear .link-item")
            for (episodeElement in episodeElements) {
                val dataId = episodeElement.attr("data-id")
                val serverName = episodeElement.select("span").text()
                val updateUrl = updateEndpoint(detailFullLink) + ".$dataId"
                val episodeData = EpisodeData(dataId, serverName, updateUrl)
                sourceList.add(episodeData)
            }

        }
        return sourceList
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
            episodesList.add(Episode(id, dataId, episodeNumber, title))
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
            ratingInfo = parseRatingInfo(dataId, mainUrl)!!
        )

    }


     fun searchMovieByQuery(query: String): ArrayList<Film> {
        val list = ArrayList<Film>()
        printlnColored(
            "------------------------------ ${
                addLineBetweenWords(
                    query,
                    "-"
                )
            }------------------------------\n"
       , Color.BLUE )

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

}



fun main() {
    runBlocking {

        val requests = Requests(baseClient = Utils.httpClient)
        for (i in 1..10000) {
            val url = "https://count.getloli.com/@professorDeveloper?name=Sozo-tv&theme=rule34&padding=8&offset=0&align=top&scale=1&pixelated=1&darkmode=auto"

            val headers = mapOf(
                "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
                "Accept-Language" to "en-US,en;q=0.9,uz-UZ;q=0.8,uz;q=0.7",
                "Cache-Control" to "max-age=0",
                "DNT" to "1",
                "Priority" to "u=0, i",
                "Sec-Ch-Ua" to "\"Chromium\";v=\"142\", \"Google Chrome\";v=\"142\", \"Not_A Brand\";v=\"99\"",
                "Sec-Ch-Ua-Mobile" to "?0",
                "Sec-Ch-Ua-Platform" to "\"Windows\"",
                "Sec-Fetch-Dest" to "document",
                "Sec-Fetch-Mode" to "navigate",
                "Sec-Fetch-Site" to "none",
                "Sec-Fetch-User" to "?1",
                "Upgrade-Insecure-Requests" to "1",
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36"
            )

            val response = requests.get(
                url = url,
                headers = headers
            )
            println("$i ${response.code}")
        }
    }
}