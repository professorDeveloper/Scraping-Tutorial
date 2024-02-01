package com.azamovhudstc.scarpingtutorial.main

import com.azamovhudstc.scarpingtutorial.anibla.AmediaTvBase
import com.azamovhudstc.scarpingtutorial.asilmedia.AsilMediaBase
import com.azamovhudstc.scarpingtutorial.idub.IdubBase
import com.azamovhudstc.scarpingtutorial.theflixer.TheFlixerBase
import com.azamovhudstc.scarpingtutorial.tv_online.TasixBase
import com.azamovhudstc.scarpingtutorial.utils.Color
import com.azamovhudstc.scarpingtutorial.utils.banner
import com.azamovhudstc.scarpingtutorial.utils.displayLoadingAnimation
import com.azamovhudstc.scarpingtutorial.utils.printlnColored
import com.azamovhudstc.scarpingtutorial.uzmovi.UzmoviBase
import kotlinx.coroutines.runBlocking
import java.util.*

fun main(args: Array<String>) {
    val uzmoviBase = UzmoviBase()
    val asilMediaBase = AsilMediaBase()
    val amediaTvBase = AmediaTvBase()
    val tasixBase = TasixBase()
    val idubBase = IdubBase()
    val theFlixerBase = TheFlixerBase()
    val scanner = Scanner(System.`in`)
    printlnColored(banner, Color.DARK_ORANGE)
    while (true) {
        printlnColored("1 -> Uzmovi", Color.GREEN)
        printlnColored("2 -> The Flixer", Color.BLUE)
        printlnColored("3 -> Idub", Color.YELLOW)
        printlnColored("4 -> Online Tv", Color.CYAN)
        printlnColored("5 -> Random Movies List ", Color.MAGENTA)
        printlnColored("Select Platform: ", Color.DARK_ORANGE)
        val scannerForNext = Scanner(System.`in`)

        val selectType = scannerForNext.nextInt()
        scannerForNext.nextLine() // Consume the newline character

        when (selectType) {
            1 -> {

                print("Enter Movie Name :")
                val movieName = scanner.nextLine()
                displayLoadingAnimation("Searching for movies", Color.GREEN)
                val list = uzmoviBase.searchMovie(movieName)
                printlnColored(" Selected Movie: ${list[0].title}", Color.GREEN)
                displayLoadingAnimation("Loading Episodes", Color.GREEN)

                uzmoviBase.movieDetails(list[0]) // Get Movie Details  Scraping by href
            }
            2 -> {

                runBlocking {
                    println("Enter Movie Name :")
                    val movieName = scanner.nextLine()
                    val list = theFlixerBase.searchMovieByQuery(movieName)
                    displayLoadingAnimation("Searching for movies", Color.BLUE)
                    val searchedMovie = list[0]
                    val tvShow = theFlixerBase.getDetailFullMovie(searchedMovie.watchUrl)

                    printlnColored("  Data ID: ${tvShow.dataId}", Color.BLUE)
                    printlnColored("  BannerUrl: ${tvShow.bannerUrl}", Color.BLUE)
                    printlnColored("  Movie Title: ${tvShow.title}", Color.BLUE)
                    printlnColored("  Movie Duration: ${tvShow.duration}", Color.BLUE)
                    printlnColored("  Movie Country: ${tvShow.country}", Color.BLUE)
                    printlnColored("  Movie Year: ${tvShow.year}", Color.BLUE)

                    displayLoadingAnimation("Loading Episodes", Color.BLUE)
                    val map = theFlixerBase.getSeasonList(tvShow.dataId)
                    val season1Episodes =
                        theFlixerBase.getEpisodeBySeason(map.get(map.keys.first())!!)

                    val episode = season1Episodes.get(0)
                    displayLoadingAnimation("Loading Sources", Color.BLUE)
                    val sourceList = theFlixerBase.getEpisodeVideoByLink(
                        episode.dataId,
                        theFlixerBase.mainUrl + searchedMovie.watchUrl
                    )


                    printlnColored("Server Name : ${sourceList.get(0).serverName}", Color.BLUE)
                    printlnColored("Server Id : ${sourceList.get(0).dataId}", Color.BLUE)

                    displayLoadingAnimation("Get Subtitle", Color.BLUE)
                    val pairData = theFlixerBase.checkM3u8FileByLink(sourceList.get(0))
                    theFlixerBase.getSources(pairData)

                    // ... (print other details with colors)
                }
            }
            3 -> {

                print("Enter Movie Name :")
                val movieName = scanner.nextLine()
                displayLoadingAnimation("Searching for movies", Color.YELLOW)
                runBlocking {
                    val list = idubBase.searchKdramma(movieName)
                    val selectedData = list[0]
                    idubBase.movieDetailByHref(href = selectedData.href)

                    // ... (print details with colors)
                }
            }
            4 -> {

                val list = tasixBase.getListTv()
                println("Choose TV by Number :")
                val selectedNumber = scanner.nextInt()

                if (selectedNumber in list.indices) {
                    displayLoadingAnimation("Searching for TV", Color.YELLOW)

                    tasixBase.getTvFullDataByHref(href = list[selectedNumber].href)
                } else {
                    printlnColored("Invalid selection!!!", Color.RED)
                }
            }
            5 -> {
                displayLoadingAnimation("Fetching random movies", Color.MAGENTA)
                asilMediaBase.getMovieList()
            }
            else -> {
                printlnColored("Invalid selection!!!", Color.RED)
            }
        }
    }
}

