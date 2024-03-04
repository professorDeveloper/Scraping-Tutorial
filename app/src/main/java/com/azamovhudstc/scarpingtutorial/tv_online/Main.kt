package com.azamovhudstc.scarpingtutorial.tv_online

import com.azamovhudstc.scarpingtutorial.tv_online.parsed.Movie
import com.azamovhudstc.scarpingtutorial.utils.Color
import com.azamovhudstc.scarpingtutorial.utils.Utils.getJsoup
import com.azamovhudstc.scarpingtutorial.utils.Utils.httpClient
import com.azamovhudstc.scarpingtutorial.utils.displayLoadingAnimation
import com.azamovhudstc.scarpingtutorial.utils.parser
import com.azamovhudstc.scarpingtutorial.utils.printlnColored
import com.lagradost.nicehttp.Requests
import kotlinx.coroutines.runBlocking
import org.jsoup.nodes.Element
import org.jsoup.select.Elements


private const val mainURL = "https://tas-ix.tv"
fun main(args: Array<String>) {

    var tasixBase = TasixBase()
    val list = tasixBase.getListTv()
    tasixBase.getTvFullDataByHref(list.get(list.lastIndex).href)

}

class TasixBase {

    fun main() {
        val list = getListTv()

        getTvFullDataByHref(list.get(list.lastIndex).href)
    }



    fun getTvFullDataByHref(href: String) {
        try {
            val doc = getJsoup(href)
            val iframeElement = doc.select("iframe").first()
            val srcAttributeValue = iframeElement?.attr("src")
            val pattern = Regex("""file=([^&]+)""")

            if (iframeElement != null) {
                val matchResult = pattern.find(srcAttributeValue.toString())

                // Extract the value of the file parameter if a match is found
                val fileParameterValue = matchResult?.groups?.get(1)?.value

                if (fileParameterValue != null) {
                    println("Link ::"+fileParameterValue)
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
        val movieElements: Elements = doc.select(".tcarusel-item.main-news")
        if (movieElements != null) {
            // Select all links within the hidden-menu
            movieElements
                .map {
                    val href = it.select("a[href]").attr("href")
                    val title = it.select("a[href]").text()
                    val image = it.select("img.xfieldimage").attr("src")
                    val rating = it.select(".current-rating").text().toInt()
                    printlnColored("  Text: ${removeNumbers(title)}", Color.YELLOW)
                    printlnColored("  Image: $image", Color.DARK_ORANGE)
                    printlnColored("  Href: $href", Color.CYAN)
                    printlnColored("  Rating: $rating", Color.GREEN)
                    printlnColored("-----------------------------", Color.YELLOW)

                }
        } else {
            printlnColored("Uzbek channels not found.", Color.YELLOW)
        }
        return movieElements.map {
            val href = it.select("a[href]").attr("href")
            val title = it.select("a[href]").text()
            val image = it.select("img.xfieldimage").attr("src")
            val rating = it.select(".current-rating").text().toInt()

            Movie(href, title, image, rating)
        }

    }

    fun removeNumbers(input: String): String {
        return input.replace(Regex("\\d+"), "").trim()
    }

}