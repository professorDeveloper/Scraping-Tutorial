import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import org.jsoup.Jsoup
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.regex.Pattern
import java.util.zip.GZIPInputStream
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

object M3U8FromHiddenDiv {

    private val m3u8Regex: Pattern = Pattern.compile("""(?i)(https?:)?//[^\s'"]+?\.m3u8(?:\?[^\s'"<>#]*)?""")
    private val h4sInUrlRegex: Pattern = Pattern.compile("""/pl/([A-Za-z0-9+/_-]+)/master\.m3u8""")
    private val jsonFileRegex: Pattern = Pattern.compile(""""file"\s*:\s*"([^"]+\.m3u8[^"]*)"""")

    fun extract(pageUrl: String, ua: String = defaultUA()): String? {
        val doc = Jsoup.parse("")
        val html = doc.outerHtml()
        // 0) Playerjs ichidan to'g'ridan-to'g'ri file? (kamdan-kam)
        findM3U8(html)?.let { return absolutize(pageUrl, it) }

        val payload = doc.selectFirst("#xTyBxQyGTA")?.text()?.trim()
        if (!payload.isNullOrBlank()) {
            // ba'zan boshida “Y=” kabi marker bo‘ladi — olib tashlaymiz
            val core = payload.removePrefix("Y=").trim()

            // A) toza Base64 deb urinamiz
            decodeBase64ThenInflate(core)?.let { decoded ->
                findM3U8(decoded)?.let { return absolutize(pageUrl, it) }
                findJsonFile(decoded)?.let { return absolutize(pageUrl, it) }
            }

            // B) Base64 bo'lmasa, URL-decode / ikki marta base64 / inflateni ham sinab ko‘ramiz
            decodeLadder(core)?.let { decoded ->
                findM3U8(decoded)?.let { return absolutize(pageUrl, it) }
                findJsonFile(decoded)?.let { return absolutize(pageUrl, it) }
            }
        }

        // 2) HTML/JS ichida H4sIA… (Base64+GZIP) bo'lsa, ochib ko'ramiz
        decodeH4sCandidateFromText(html)?.let { d ->
            findM3U8(d)?.let { return absolutize(pageUrl, it) }
            findJsonFile(d)?.let { return absolutize(pageUrl, it) }
        }

        // 3) atributlarda ham sinab ko‘ramiz
        doc.select("[src],[href],script").forEach { el ->
            val cand = el.attr("src").ifEmpty { el.attr("href") }
            if (cand.isNotBlank()) {
                decodeH4sCandidateFromText(cand)?.let { d ->
                    findM3U8(d)?.let { return absolutize(pageUrl, it) }
                    findJsonFile(d)?.let { return absolutize(pageUrl, it) }
                }
            }
            if (el.tagName().equals("script", true)) {
                val js = el.data()
                decodeH4sCandidateFromText(js)?.let { d ->
                    findM3U8(d)?.let { return absolutize(pageUrl, it) }
                    findJsonFile(d)?.let { return absolutize(pageUrl, it) }
                }
            }
        }

        return null
    }

    // ---- helpers ----
    private fun defaultUA() =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Safari/537.36"

    private fun findM3U8(text: String): String? {
        val m = m3u8Regex.matcher(text)
        if (m.find()) {
            var u = m.group().trim()
            if (u.startsWith("//")) u = "https:$u"
            return u
        }
        val p = h4sInUrlRegex.matcher(text)
        if (p.find()) {
            val b64 = p.group(1)
            decodeH4s(b64)?.let { inner ->
                findM3U8(inner)?.let { return it }
            }
        }
        return null
    }

    private fun findJsonFile(text: String): String? {
        val m = jsonFileRegex.matcher(text)
        return if (m.find()) m.group(1) else null
    }

    private fun decodeBase64ThenInflate(b64: String): String? {
        val cleaned = b64.filter { it.isLetterOrDigit() || it in "+/_-=" } // chala alfavitlarga chidamli
        return (base64Decode(cleaned)
            ?.let { tryGzip(it) ?: tryZlib(it) }
            ?: base64Decode(cleaned) // ba’zi holatda faqat base64 bo'ladi
                )?.toString(StandardCharsets.UTF_8)
    }

    private fun decodeLadder(s: String): String? {
        // 1) base64
        base64Decode(s)?.let { bytes ->
            // 1a) gzip or zlib?
            tryGzip(bytes)?.let { return it.toString(StandardCharsets.UTF_8) }
            tryZlib(bytes)?.let { return it.toString(StandardCharsets.UTF_8) }
            return bytes.toString(StandardCharsets.UTF_8)
        }
        return null
    }

    @SuppressLint("NewApi")
    private fun base64Decode(s: String): ByteArray? = try {
        val norm = s.replace('-', '+').replace('_', '/')
        val pad = when (norm.length % 4) { 2 -> norm + "=="; 3 -> norm + "="; else -> norm }
        Base64.getDecoder().decode(pad)
    } catch (_: Exception) { null }

    private fun tryGzip(data: ByteArray): ByteArray? = try {
        GZIPInputStream(ByteArrayInputStream(data)).use { gz ->
            val out = ByteArrayOutputStream()
            val buf = ByteArray(8192)
            while (true) {
                val n = gz.read(buf); if (n <= 0) break
                out.write(buf, 0, n)
            }
            out.toByteArray()
        }
    } catch (_: Exception) { null }

    private fun tryZlib(data: ByteArray): ByteArray? = try {
        InflaterInputStream(ByteArrayInputStream(data), Inflater(true)).use { inf ->
            val out = ByteArrayOutputStream()
            val buf = ByteArray(8192)
            while (true) {
                val n = inf.read(buf); if (n <= 0) break
                out.write(buf, 0, n)
            }
            out.toByteArray()
        }
    } catch (_: Exception) { null }

    private fun decodeH4sCandidateFromText(text: String): String? {
        val idx = text.indexOf("H4sIA")
        if (idx < 0) return null
        val end = text.indexOfAny(charArrayOf('"','\'',')','(',' ','\n','\r','<','>'), idx + 5)
        val token = if (end > idx) text.substring(idx, end) else text.substring(idx)
        return decodeH4s(token)
    }

    private fun decodeH4s(b64: String): String? {
        val bytes = base64Decode(b64) ?: return null
        val ungz = tryGzip(bytes) ?: return null
        return ungz.toString(StandardCharsets.UTF_8)
    }

    private fun absolutize(baseUrl: String, found: String): String = try {
        if (found.startsWith("http://") || found.startsWith("https://")) found
        else URL(URL(baseUrl), found).toString()
    } catch (_: Exception) { found }
}

// ---- exam