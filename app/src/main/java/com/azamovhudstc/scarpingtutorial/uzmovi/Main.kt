package com.azamovhudstc.scarpingtutorial.uzmovi

import com.azamovhudstc.scarpingtutorial.utils.Utils
import com.azamovhudstc.scarpingtutorial.utils.Utils.getJsoup
import com.azamovhudstc.scarpingtutorial.utils.parser
import com.azamovhudstc.scarpingtutorial.uzmovi.movie.ParsedMovie
import com.lagradost.nicehttp.Requests
import kotlinx.coroutines.runBlocking

private val mainUrl = "http://uzmovi.com/"

//sorry my english is not good
//:joy
fun main() {
    val list = searchMovie("Wenzdey")

    for (movie in list) {
        //this  loop is for testing
        println("-------------------------------")
        println(movie.title)
        println(movie.href)
        println("-------------------------------")
    }
    println("----------------SELECTED MOVIE ${list.get(0).title}---------------")
    movieDetails(list.get(0)) /// Get Movie Details  Scarping by href

//    runBlocking {
//        getM3u8LocationFile("")
//    }
}


suspend fun getM3u8LocationFile(mainUrl: String) {
    val requests = Requests(Utils.httpClient, responseParser = parser)

    val data = requests.get(
        mainUrl,

        headers = mapOf(
            "Cookie" to "_ym_uid=1664171290829008916; \"_pubcid\"=439b1e7c-eab3-4392-a9a7-19b1e53fe9f3; _ym_d=1696009917; __gads=ID=47342de96a689496-224c06c4fbdd00d6:T=1685651803:RT=1699104092:S=ALNI_Mb2ZhtSMyfS5P7PZrwc7eQv5t2WRg; __gpi=UID=00000c2ace922f58:T=1685651803:RT=1699104092:S=ALNI_MZzapclV2KKmb9oTHGcM6MVmi-EBg; comment_name=Foydalanuvchi; _pbjs_userid_consent_data=3524755945110770; _gid=GA1.2.704416453.1705347575; adrcid=ACr-r0sIPrgrh7iAg-Dg5rQ; adrcid_cd=1705407018194; _ym_isad=1; ci_session=rku1vq97bd4cdr8e1piekobjspkeuedl; _ga_XVBVMVW651=GS1.1.1705431478.202.1.1705433781.0.0.0; _ga=GA1.2.504275464.1685651802",
            "Connection" to "keep-alive",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/237.84.2.178 Safari/537.36",
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
        ), referer = "http://uzmovi.com/",
        allowRedirects = true,
        verify = true

    )


    println(data.url+"index.m3u8")

    checkM3u8Link(data.url+"playlist.m3u8")

}

fun checkM3u8Link(url: String) {
    runBlocking {
        val requests = Requests(Utils.httpClient, responseParser = parser)
        val data = requests.get(
            mainUrl,

            headers = mapOf(
                "Cookie" to "_ym_uid=1664171290829008916; \"_pubcid\"=439b1e7c-eab3-4392-a9a7-19b1e53fe9f3; _ym_d=1696009917; __gads=ID=47342de96a689496-224c06c4fbdd00d6:T=1685651803:RT=1699104092:S=ALNI_Mb2ZhtSMyfS5P7PZrwc7eQv5t2WRg; __gpi=UID=00000c2ace922f58:T=1685651803:RT=1699104092:S=ALNI_MZzapclV2KKmb9oTHGcM6MVmi-EBg; comment_name=Foydalanuvchi; _pbjs_userid_consent_data=3524755945110770; _gid=GA1.2.704416453.1705347575; adrcid=ACr-r0sIPrgrh7iAg-Dg5rQ; adrcid_cd=1705407018194; _ym_isad=1; ci_session=rku1vq97bd4cdr8e1piekobjspkeuedl; _ga_XVBVMVW651=GS1.1.1705431478.202.1.1705433781.0.0.0; _ga=GA1.2.504275464.1685651802",
                "Connection" to "keep-alive",
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/237.84.2.178 Safari/537.36",
                "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
            ), referer = "http://uzmovi.com/",
            allowRedirects = true,
            verify = true

        )
        println(data.url)
    }
}

fun movieDetails(parsedMovie: ParsedMovie) {
    println(parsedMovie.href)
    //Scraping from uzmovi details
    val doc = getJsoup(parsedMovie.href)
    val tabPaneElement = doc.select(".tab-pane.fade.in.active").first()// This Code Supported CSS
    if (tabPaneElement?.getElementById("online9") != null) {
        println(tabPaneElement?.getElementById("online9"))
        val scriptContent = tabPaneElement?.select("script")!!.first()!!.html()
        val playerConfigStart = scriptContent.indexOf("var playerjsfilm_config = {")
        val playerConfigEnd = scriptContent.indexOf("};", playerConfigStart) + 1
        val playerConfig = scriptContent.substring(playerConfigStart, playerConfigEnd)

        val fileMatch = Regex("""file:\s*"([^"]+)"""").find(playerConfig)
        val titleMatch = Regex("""title:\s*"([^"]+)"""").find(playerConfig)

        val file = fileMatch?.groupValues?.get(1)
        val title = titleMatch?.groupValues?.get(1)


        println("File: $file" + "")
        println("Title: $title")


        runBlocking {
            getM3u8LocationFile(file!!)
        }
    } else {
        val episodeLinks = tabPaneElement!!.select("div#online1 center a")
        val epList = arrayListOf<String>()
        // Displaying the parsed episode links
        episodeLinks.forEachIndexed { index, link ->
            val href = link.attr("href")
            val text = link.text()
            epList.add(href)
            println("- $text: $href")
        }

        setLink(epList.get(0))

    }
}

//http://46.148.234.182/atsspxds/dy8yclFDMVpPMXFWcTJycVVZZlplM3Nkb0YzQUExMUdiRVFTcUkwMnprL28vbFlRWGVRM1I3T0tHZGhwVE0wcA_9pn_9pn/ZVpJNm5kcVRMMjIvUHpsbGtSVzU0YWx6Yi96VGN6bWg3WjF1dndwenhHSVVBRjNnKzNLVnQxb2hnSFB0RzVGQg_5uq_5uq/RGpoRys2NGNVTElzTlBYSVBjK2tTdz09/
//So this url for Exoplayer if you do`nt know exo Player Just Search hls-player android :D
fun setLink(episodeLink: String) {
    val doc = getJsoup(episodeLink)
    val tabPaneElement = doc.select(".tab-pane.fade.in.active").first()// This Code Supported CSS
    val scriptContent = tabPaneElement!!.select("div#online1 script").html()

    // Extracting the content of the playerjsserial_config variable
    val playerConfigStart = scriptContent.indexOf("var playerjsserial_config = {")
    val playerConfigEnd = scriptContent.indexOf("};", playerConfigStart) + 1
    val playerConfig = scriptContent.substring(playerConfigStart, playerConfigEnd)

    // Extracting file, title, and poster using regular expressions
    val fileMatch = Regex("""file:\s*"([^"]+)""").find(playerConfig)
    val titleMatch = Regex("""title:\s*"([^"]+)""").find(playerConfig)
    val posterMatch = Regex("""poster:\s*"([^"]+)""").find(playerConfig)

    val file = fileMatch?.groupValues?.get(1)
    val title = titleMatch?.groupValues?.get(1)
    val poster = posterMatch?.groupValues?.get(1)

    println("File: $file")
    println("Title: $title")
    println("Poster: $poster")

    runBlocking {
        getM3u8LocationFile(convertToHttps(file!!))

    }
}

fun convertToHttps(httpString: String): String {
    // Check if the string starts with "http://"
    if (httpString.startsWith("http://")) {
        // Replace "http://" with "https://"
        return "https://" + httpString.substring(7)
    } else {
        // If the string doesn't start with "http://", return it unchanged
        return httpString
    }
}


fun searchMovie(query: String): ArrayList<ParsedMovie> {
    val list = arrayListOf<ParsedMovie>()
    //this searchUrl is for search in uzmovi
    //sample url : http://uzmovi.com/search?q=Sening+Isming
    val searchUrl = "$mainUrl/search?q=$query"
    val doc = getJsoup(searchUrl) //REQUEST SEARCH
    val movieContent = doc.getElementsByClass("shortstory-in categ")//GET Movie LIST

    for (movie in movieContent) {
        val movieName = movie.getElementsByClass("short-link").text() //GET Movie NAME
        val movieCover = movie.getElementsByTag("img").attr("data-src") // GET Movie COVER
        val movieHref =
            movie.getElementsByClass("short-link").select("h4.short-link a")
                .attr("href") //GET MOVIE DETAIL LINK
        list.add(ParsedMovie(movieName, movieHref, movieCover))
    }

    return list

}