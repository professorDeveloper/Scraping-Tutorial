package com.azamovhudstc.scarpingtutorial.main

import com.azamovhudstc.scarpingtutorial.asilmedia.AsilMediaBase
import com.azamovhudstc.scarpingtutorial.idub.IdubBase
import com.azamovhudstc.scarpingtutorial.theflixer.TheFlixerBase
import com.azamovhudstc.scarpingtutorial.tv_online.TasixBase
import com.azamovhudstc.scarpingtutorial.uzmovi.UzmoviBase
import kotlinx.coroutines.runBlocking
import java.util.*

fun main(args: Array<String>) {
    val uzmoviBase = UzmoviBase()
    val asilMediaBase = AsilMediaBase()
    val tasixBase = TasixBase()
    val idubBase = IdubBase()
    val theFlixerBase = TheFlixerBase()
    val scanner = Scanner(System.`in`)
    val banner =
        """"+---------------------------------------------------------------------------------+
|                                                                                 |
|  _  _  _         _         ______                              _                |
| (_)(_)(_)       | |       / _____)                            (_)               |
|  _  _  _  _____ | |__    ( (____    ____   ____  _____  ____   _  ____    ____  |
| | || || || ___ ||  _ \    \____ \  / ___) / ___)(____ ||  _ \ | ||  _ \  / _  | |
| | || || || ____|| |_) )   _____) )( (___ | |    / ___ || |_| || || | | |( (_| | |
|  \_____/ |_____)|____/   (______/  \____)|_|    \_____||  __/ |_||_| |_| \___ | |
|                                                        |_|              (_____| |
| Telegram : https://t.me/native_applications                                     |
| Github   :  https://github.com/professorDeveloper                               |                                                                               
+---------------------------------------------------------------------------------+""".trimMargin()
    printlnColored(banner, Color.DARK_ORANGE)
    while (true) {
        printlnColored("1 -> Uzmovi", Color.GREEN)
        printlnColored("2 -> The Flixer", Color.BLUE)
        printlnColored("3 -> Idub", Color.YELLOW)
        printlnColored("4 -> Online Tv", Color.CYAN)
        printlnColored("5 -> Random Movies List ", Color.MAGENTA)
        printlnColored("Select Platform: ", Color.WHITE)

        val selectType = scanner.nextInt()

        when (selectType) {
            1 -> {

                println("Enter Movie Name :")
                val movieName = scanner.next()
                displayLoadingAnimation("Searching for movies", Color.GREEN)
                val list = uzmoviBase.searchMovie(movieName)
                printlnColored(" Selected Movie: ${list[0].title}", Color.GREEN)
                displayLoadingAnimation("Loading Episodes", Color.GREEN)

                uzmoviBase.movieDetails(list[0]) // Get Movie Details  Scraping by href
            }
            2 -> {

                runBlocking {
                    println("Enter Movie Name :")
                    val movieName = scanner.next()
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


                    printlnColored("Server Name : ${sourceList.get(0).serverName}",Color.BLUE)
                    printlnColored("Server Id : ${sourceList.get(0).dataId}",Color.BLUE)

                    displayLoadingAnimation("Get Subtitle", Color.BLUE)
                    val pairData = theFlixerBase.checkM3u8FileByLink(sourceList.get(0))
                    theFlixerBase.getSources(pairData)

                    // ... (print other details with colors)
                }
            }
            3 -> {

                println("Enter Movie Name :")
                val movieName = scanner.next()
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

// Function to display a simple loading animation with color
fun displayLoadingAnimation(message: String, color: Color) {
    val loadingChars = listOf("⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏")
    var counter = 0

    val loadingAnimation = Thread {
        try {
            while (true) {
                printColored("\r${loadingChars[counter % loadingChars.size]} $message", color)
                counter++
                Thread.sleep(100) // Adjust the delay based on your preference
            }
        } catch (e: InterruptedException) {
            // Restore interrupted status
            Thread.currentThread().interrupt()
        }
    }

    loadingAnimation.start()

    // Run the loading animation for a few seconds (you can adjust the duration)
    Thread.sleep(3000)

    loadingAnimation.interrupt()

    // Clear the loading line
    printColored("\r${" ".repeat(message.length + 3)}\r", color)
}

// Function to print colored text
fun printlnColored(text: String, color: Color) {
    val colorCode = when (color) {
        Color.RED -> "\u001B[31m"
        Color.GREEN -> "\u001B[32m"
        Color.YELLOW -> "\u001B[33m"
        Color.BLUE -> "\u001B[34m"
        Color.MAGENTA -> "\u001B[35m"
        Color.CYAN -> "\u001B[36m"
        Color.WHITE -> "\u001B[37m"
        Color.DARK_ORANGE -> "\u001B[38;2;170;85;0m"

    }
    val resetColor = "\u001B[0m"

    println("$colorCode$text$resetColor")
}

fun printColored(text: String, color: Color) {
    val colorCode = when (color) {
        Color.RED -> "\u001B[31m"
        Color.GREEN -> "\u001B[32m"
        Color.YELLOW -> "\u001B[33m"
        Color.BLUE -> "\u001B[34m"
        Color.MAGENTA -> "\u001B[35m"
        Color.CYAN -> "\u001B[36m"
        Color.WHITE -> "\u001B[37m"
        Color.DARK_ORANGE -> "\u001B[38;2;170;85;0m"
    }
    val resetColor = "\u001B[0m"

    print("$colorCode$text$resetColor")
}

// Enum to represent ANSI color codes
enum class Color {
    RED, GREEN, YELLOW, BLUE, MAGENTA, CYAN, WHITE, DARK_ORANGE
}
