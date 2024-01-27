package com.azamovhudstc.scarpingtutorial.tv_online

import com.azamovhudstc.scarpingtutorial.main.Color
import com.azamovhudstc.scarpingtutorial.main.displayLoadingAnimation
import com.azamovhudstc.scarpingtutorial.main.printlnColored
import com.azamovhudstc.scarpingtutorial.tv_online.parsed.Movie
import com.azamovhudstc.scarpingtutorial.utils.Utils.getJsoup
import com.azamovhudstc.scarpingtutorial.utils.Utils.httpClient
import com.azamovhudstc.scarpingtutorial.utils.parser
import com.lagradost.nicehttp.Requests
import kotlinx.coroutines.runBlocking
import org.jsoup.nodes.Element


private const val mainURL = "https://tas-ix.tv"


class TasixBase {

    fun main() {
        val list = getListTv()

        getTvFullDataByHref(list.get(list.lastIndex).href)
    }

    fun getTvFullDataByHref(href: String) {
        try {
            val doc = getJsoup(mainURL + href)
            println(mainURL + href)
            val iframeElement = doc.select("iframe").first()
            val srcAttributeValue = iframeElement?.attr("src")
            val pattern = Regex("""file=([^&]+)""")

            if (iframeElement != null) {
                val matchResult = pattern.find(srcAttributeValue.toString())

                // Extract the value of the file parameter if a match is found
                val fileParameterValue = matchResult?.groups?.get(1)?.value

                if (fileParameterValue != null) {
                    println(fileParameterValue)
                    val requests = Requests(baseClient = httpClient, responseParser = parser)
                    runBlocking {
                        val doc = requests.get(fileParameterValue)
                        println(doc.body.string())
                        //I think this Little bit hard code
                    }
                } else {
                    println("File parameter not found.")
                }
            }

        } catch (e: Exception) {
            printlnColored("SSL ERROR (Do`nt Worry)", Color.DARK_ORANGE)
        }
    }

    fun getListTv(): List<Movie> {
        displayLoadingAnimation("Loading Tv list", Color.YELLOW)
        val doc = getJsoup(mainURL)
        val uzbekChannels: Element? = doc.select("li:has(a:contains(Узбекский каналы))").first()
        val movieList = arrayListOf<Movie>()

        if (uzbekChannels != null) {
            // Select all links within the hidden-menu
            val links: List<Element> = uzbekChannels.select(".hidden-menu a")

            // Print the links
            for ((count, link) in links.withIndex()) {
                val href = link.attr("href")
                val text = link.select("i").text()
                val randomColors = Color.values().toList()
                printlnColored(" $count Text: $text", color = randomColors.random())
                movieList.add(Movie(href, text))
            }

        } else {
            printlnColored("Uzbek channels not found.",Color.YELLOW)
        }
        return movieList

    }
}