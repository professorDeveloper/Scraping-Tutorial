package com.azamovhudstc.scarpingtutorial.tv_online

import com.azamovhudstc.scarpingtutorial.tv_online.parsed.Movie
import com.azamovhudstc.scarpingtutorial.utils.Utils.getJsoup
import org.jsoup.nodes.Element


private const val mainURL = "https://tas-ix.tv"

fun main() {
    val list = getListTv()

    getTvFullDataByHref(list.get(list.lastIndex).href)
}

fun getTvFullDataByHref(href: String) {
    val doc = getJsoup(mainURL + href)
    val iframeElement = doc.select("iframe").first()
    val srcAttributeValue = iframeElement?.attr("src")
    val pattern = Regex("""file=([^&]+)""")

    if (iframeElement != null) {
        val matchResult = pattern.find(srcAttributeValue.toString())

        // Extract the value of the file parameter if a match is found
        val fileParameterValue = matchResult?.groups?.get(1)?.value

        if (fileParameterValue != null) {
            println("File parameter value: $fileParameterValue")
        } else {
            println("File parameter not found.")
        }
    }

}

fun getListTv(): List<Movie> {
    val doc = getJsoup(mainURL)
    val uzbekChannels: Element? = doc.select("li:has(a:contains(Узбекский каналы))").first()
    val movieList = arrayListOf<Movie>()

    if (uzbekChannels != null) {
        // Select all links within the hidden-menu
        val links: List<Element> = uzbekChannels.select(".hidden-menu a")

        // Print the links
        for (link in links) {
            val href = link.attr("href")
            val text = link.select("i").text()
            println("Link: $href, Text: $text")
            movieList.add(Movie(href, text))
        }

    } else {
        println("Uzbek channels not found.")
    }
    return movieList

}