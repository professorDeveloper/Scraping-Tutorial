package com.azamovhudstc.scarpingtutorial.imdb.kt

import com.azamovhudstc.scarpingtutorial.utils.Utils
import org.json.JSONObject
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.slf4j.helpers.Util
import kotlin.time.DurationUnit
import kotlin.time.toDuration

const val BASE_URL = "https://www.imdb.com/"

fun main(args: Array<String>) {
    val url = "never let go"
    val response = searchMovie(url)

    println(System.currentTimeMillis().toDuration(DurationUnit.SECONDS))
    println(getDetails(response))
    println(System.currentTimeMillis().toDuration(DurationUnit.SECONDS))
}

fun String.extractViId(): String? {
    // Regex: URL ichida '/' bilan boshlangan "vi" va undan keyin raqamlar, keyin yana '/'
    val regex = Regex("""/(vi\d+)/""")
    val match = regex.find(this)
    return match?.groupValues?.get(1)
}

fun getTrailerLink(trailerUrl: String): String {

    val document = Utils.getJsoup(
        BASE_URL + "video/$trailerUrl",
        mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8"
        )
    )

    // __NEXT_DATA__ IDli script tagidagi matnni olamiz
    val scriptData =
        document.select("script#__NEXT_DATA__").first()?.html() ?: return "master.m3u8 URL notfound"

    // Regex: "url": " ... hls-...-master.m3u8 ..." formatidagi URLni izlaydi
    val regex = Regex(""""url"\s*:\s*"([^"]*hls-[^"]*?-master\.m3u8[^"]*)"""")
    val matchResult = regex.find(scriptData)

    return if (matchResult != null) {
        val masterUrl = matchResult.groupValues[1]
        println("Found master.m3u8 URL: $masterUrl")
        masterUrl
    } else {
        println("master.m3u8 URL notfound")
        "master.m3u8 URL notfound"
    }
}

fun getTrailer(item: SearchItem): String {
    val document = Utils.getJsoup(
        item.detailsUrl,
        mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8"
        )
    )

    // __NEXT_DATA__ identifikatorli script tagini tanlaymiz va uning ichidagi matnni olamiz
    val scriptData =
        document.select("script#__NEXT_DATA__").first()?.html() ?: return "m3u8 URL notfound"

    // Regex: "url": "https://... .m3u8 ... " formatidagi linklarni izlaymiz
    val regex = Regex(""""url"\s*:\s*"([^"]+\.m3u8[^"]*)"""")

    // Barcha mos keladigan natijalarni ro'yxatga to'playmiz
    val matches = regex.findAll(scriptData).toList()

    return if (matches.isNotEmpty()) {
        // Oxirgi mos kelgan elementni qaytaramiz
        val lastUrl = matches.last().groupValues[1]
        println(lastUrl)
        println("Last m3u8 URL: ${lastUrl.extractViId()}")
        return lastUrl.extractViId().toString()
    } else {
        println("m3u8 URL notfound")
        "m3u8 URL notfound"
    }
}
fun extractImdbId(url: String): String? {
    val regex = """title/(tt\d+)/""".toRegex()
    return regex.find(url)?.groupValues?.get(1)
}

fun getDetails(item: SearchItem): DetailResponse {
    println("Details url:${item.detailsUrl}")
    val doc = Utils.getJsoup(
        item.detailsUrl,
        mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9",
        )
    )

    println(extractImdbId(item.detailsUrl))

    val photoData = getPhotos(doc)
    val castData = getCasts(doc)
    val quoteItem = getSomeDetailData(doc)
    val trailer = getTrailerLink(getTrailer(item))
    val getPrincipalCredits = getPrincipalCast(doc)
    return DetailResponse(
        photoData.photos,
        castData.cast,
        quoteItem,
        getPrincipalCredits
    )
}

fun getPhotos(item: Document): PhotosResponse {
    val doc = item
    val photos = mutableListOf<PhotoItem>()

    // Rasm ma'lumotlarini yig'ish
    val elements: Elements = doc.select("a.sc-180dfae3-0.hDscSA")
    for (element in elements) {
        val imageUrl = element.select("img.ipc-image").attr("src")
        val altText = element.select("img.ipc-image").attr("alt")
        val detailsUrl = "https://www.imdb.com" + element.attr("href")

        photos.add(PhotoItem(imageUrl.replace(Regex("UX\\d+"), "UX920"), altText, detailsUrl))
    }
    println("Pthotosssss $photos")

    return PhotosResponse(photos)
}

fun getCasts(item: Document): CastResponse {
    val doc = item
    val castList = mutableListOf<CastItem>()

    val elements: Elements = doc.select("div[data-testid='title-cast-item']")
    for (element in elements) {
        val name = element.select("a[data-testid='title-cast-item__actor']").text()

        val character = element.select("span.sc-cd7dc4b7-4.zVTic").text()

        var imageUrl = element.select("img.ipc-image").attr("src")
        imageUrl = imageUrl.ifEmpty {
            "https://example.com/default_avatar.jpg"
        }

        val detailsUrl =
            "https://www.imdb.com" + element.select("a.ipc-lockup-overlay").attr("href")

        castList.add(
            CastItem(
                name, character, ImageUrlFormatter.formatImageUrl(imageUrl), detailsUrl
            )
        )
    }

    return CastResponse(castList)
}

fun getSomeDetailData(document: Document): QuoteItem {
    val doc = document
    val elements: Elements = doc.select("li[data-testid='didyouknow-quote']")
    for (element in elements) {
        val character = element.select("a.ipc-link").text()
        val quote = element.select("div.ipc-html-content-inner-div p").text()
        val detailsUrl =
            "https://www.imdb.com" + element.select("a.ipc-metadata-list-item__label").attr("href")
        return QuoteItem(character.toString(), quote.toString(), detailsUrl.toString())
    }
    return QuoteItem("", "", "")
}

fun getPrincipalCast(document: Document): PrincipalCredits {
    val doc = document
    // Directors
    val directorElements: Elements =
        doc.select("li[data-testid='title-pc-principal-credit']:has(span:contains(Director)) a")
    val directors = directorElements.map {
        DirectorItem(
            name = it.text(),
            profileUrl = "https://www.imdb.com" + it.attr("href")
        )
    }

    // Writers
    val writerElements: Elements =
        doc.select("li[data-testid='title-pc-principal-credit']:has(span:contains(Writers)) a")
    val writers = writerElements.map {
        WriterItem(
            name = it.text(),
            profileUrl = "https://www.imdb.com" + it.attr("href")
        )
    }

    // Stars
    val starElements: Elements =
        doc.select("li[data-testid='title-pc-principal-credit']:has(a:contains(Stars)) a.ipc-metadata-list-item__list-content-item")
    val stars = starElements.map {
        StarItem(
            name = it.text(),
            profileUrl = "https://www.imdb.com" + it.attr("href")
        )
    }

    return PrincipalCredits(directors, writers, stars)
}

fun searchMovie(query: String): SearchItem {
    val request = Utils.getJsoup(
        "$BASE_URL/find/?q=$query", mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9",
        )
    )

    val doc = request.body()

    val elements: Elements = doc.select("li.find-title-result")
    for (element in elements) {
        val title = element.select("a.ipc-metadata-list-summary-item__t").text()
        val year = element.select("ul.ipc-metadata-list-summary-item__tl li").text()
        val cast = element.select("ul.ipc-metadata-list-summary-item__stl li").text()
        val imageUrl = element.select("img.ipc-image").attr("src")
        val detailsUrl =
            "https://www.imdb.com" + element.select("a.ipc-metadata-list-summary-item__t")
                .attr("href")
        println(detailsUrl)
        return SearchItem(title, year, cast, imageUrl, detailsUrl)
    }
    return SearchItem("", "", "", "", "")
}

data class CastResponse(
    val cast: List<CastItem>
)

data class DetailResponse(
    val photos: List<PhotoItem>,
    val cast: List<CastItem>,
    val quotes: QuoteItem,
    val principalCast: PrincipalCredits
)

data class SearchResponse(
    val items: List<SearchItem>
)

data class PhotosResponse(
    val photos: List<PhotoItem>
)

data class PrincipalCredits(
    val directors: List<DirectorItem>,
    val writers: List<WriterItem>,
    val stars: List<StarItem>
)

data class SearchItem(
    val title: String,
    val year: String,
    val cast: String,
    val imageUrl: String,
    val detailsUrl: String
)

data class DirectorItem(
    val name: String,
    val profileUrl: String
)

data class WriterItem(
    val name: String,
    val profileUrl: String
)

data class StarItem(
    val name: String,
    val profileUrl: String
)

data class QuoteItem(
    val character: String, val quote: String, val detailsUrl: String
)

data class HeroMediaItem(
    val imageUrl: String,
    val altText: String,
    val videoUrl: String?,
    val videoTitle: String?,
    val videoDuration: String?
)

data class CastItem(
    val name: String, val character: String, val imageUrl: String, val detailsUrl: String
)

data class PhotoItem(
    val imageUrl: String, val altText: String, val detailsUrl: String
)

object ImageUrlFormatter {
    fun formatImageUrl(imageUrl: String): String {
        val updatedUrl = imageUrl.replace(Regex("UX\\d+"), "UX400")

        // CR orqasidagi oxirgi ikki raqamni 400 ga o'zgartirish
        return updatedUrl.replace(Regex("CR\\d+,\\d+,\\d+,\\d+"), "CR0,0,400,400")
    }
}
