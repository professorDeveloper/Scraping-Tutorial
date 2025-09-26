import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Log
import androidx.core.graphics.scale
import com.azamovhudstc.scarpingtutorial.Coroutines.ioSafe
import com.azamovhudstc.scarpingtutorial.Coroutines.main
import com.azamovhudstc.scarpingtutorial.channels_ontv.File
import com.azamovhudstc.scarpingtutorial.helper.M3u8Helper
import com.azamovhudstc.scarpingtutorial.helper.M3u8Helper2
import com.azamovhudstc.scarpingtutorial.launchSafe
import com.azamovhudstc.scarpingtutorial.utils.Utils.httpClient
import com.azamovhudstc.scarpingtutorial.utils.parser
import com.lagradost.nicehttp.Requests
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.util.Base64
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.log2



fun main(args: Array<String>): Unit = runBlocking {
    launchSafe(Dispatchers.Default) {
        runBlocking(Dispatchers.IO) {
            val url =
                "https://9dd.avalanche-rush.site/pq5z/c4/bJzO9NZz0d6kvjsNmMUTMzhiou_t-RrWoQhJDr7gUNwFCvNg-2eals7FpqsSEco00n5SzH0mM5sEUfjFrL7WlZDx6MYLjeg/list,Z3r-aM6peKE-ic4lJkPfnljqs9Q0UQ.m3u8"
            val headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:101.0) Gecko/20100101 Firefox/101.0",
                "Accept" to "*/*",
                "Accept-Language" to "en-US,en;q=0.5",
                "Accept-Encoding" to "gzip, deflate, br",
                "Origin" to "https://rapidshare.cc",
                "Connection" to "keep-alive",
                "Referer" to "https://rapidshare.cc/",
                "Sec-Fetch-Dest" to "empty",
                "Sec-Fetch-Mode" to "cors",
                "Sec-Fetch-Site" to "cross-site"
            )

            val request = Requests(baseClient = httpClient, responseParser = parser)
            val response = request.get(url, headers)
            println(response.document)
        }

    }
}