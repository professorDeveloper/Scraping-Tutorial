import org.jsoup.Jsoup
import java.net.URLEncoder

fun getDynamicM3U8(cx: Int = 0, cy: Int = 0): String? {
    val baseUrl = "https://z6bha.com" // real site base URL
    val deviceType = "Desktop/Windows"
    val browserName = "Chrome"

    val url = "$baseUrl/dl?b=view&file_code=mldhaxh6aeq5" +
            "&hash=49004318-54-86-1761160466-f8ee2c5662fbf958d8a2df58e3597227" +
            "&embed=1&referer=noxx.to" +
            "&cx=$cx&cy=$cy" +
            "&device=${URLEncoder.encode(deviceType, "UTF-8")}" +
            "&browser=${URLEncoder.encode(browserName, "UTF-8")}" +
            "&ww=1920&wh=1080"

    println(url)
    return try {
        val doc = Jsoup.connect(url)
            .ignoreContentType(true)
            .header("Content-Cache", "no-cache")
            .get()
        println(doc)
        val m3u8Url = doc.select("body").text()  // the returned URL from server
        m3u8Url
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Example usage of the `getDynamicM3U8` function.
 *
 * Fetches a dynamic M3U8 URL using the provided coordinates (100, 200) and prints the result.
 */
fun main() {
    val m3u8 = getDynamicM3U8(100, 200)
    println("Dynamic M3U8 URL: $m3u8")
}
