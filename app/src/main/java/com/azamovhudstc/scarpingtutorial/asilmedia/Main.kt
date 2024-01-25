package com.azamovhudstc.scarpingtutorial.asilmedia

import com.azamovhudstc.scarpingtutorial.asilmedia.model.Media
import com.azamovhudstc.scarpingtutorial.utils.Utils
import org.jsoup.select.Elements

private const val mainUrl = "http://asilmedia.org"

fun main(args: Array<String>) {
    val homeList = getMovieList()

    val data = homeList.get(homeList.lastIndex - 1)
    getMovieDetails(data.url)
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
        val parsedUrl  = parseUrl(fileUrl)
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
