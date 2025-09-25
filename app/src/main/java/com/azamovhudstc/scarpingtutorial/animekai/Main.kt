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


const val MAX_LOD = 6
const val MIN_LOD = 3

data class ImageParams(
    val width: Int,
    val height: Int,
) {
    companion object {
        val DEFAULT = ImageParams(200, 320)
        fun new16by9(width: Int): ImageParams {
            if (width < 100) {
                return DEFAULT
            }
            return ImageParams(
                width / 4,
                (width * 9) / (4 * 16)
            )
        }
    }

    init {
        assert(width > 0 && height > 0)
    }
}


@Suppress("UNUSED_PARAMETER")
private class NoPreviewGenerator : IPreviewGenerator {
    override fun hasPreview(): Boolean = false
    override fun getPreviewImage(fraction: Float): Bitmap? = null
    override fun release() = Unit
    override var params: ImageParams
        get() = ImageParams(0, 0)
        set(value) {}
    override var durationMs: Long = 0L
    override var loadedImages: Int = 0
}

interface IPreviewGenerator {
    fun hasPreview(): Boolean
    fun getPreviewImage(fraction: Float): Bitmap?
    fun release()

    var params: ImageParams

    var durationMs: Long
    var loadedImages: Int

    companion object {


        fun empty(): IPreviewGenerator {
            return NoPreviewGenerator()
        }
    }
}

private fun rescale(image: Bitmap, params: ImageParams): Bitmap {
    if (image.width <= params.width && image.height <= params.height) return image
    val new = image.scale(params.width, params.height)
    // throw away the old image
    if (new != image) {
        image.recycle()
    }
    return new
}

/** rescale to not take up as much memory */
private fun MediaMetadataRetriever.image(timeUs: Long, params: ImageParams): Bitmap? {
    /*if (timeUs <= 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        try {
            val primary = this.primaryImage
            if (primary != null) {
                return rescale(primary, params)
            }
        } catch (t: Throwable) {
            logError(t)
        }
    }*/

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        this.getScaledFrameAtTime(
            timeUs,
            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
            params.width,
            params.height
        )
    } else {
        return rescale(this.getFrameAtTime(timeUs) ?: return null, params)
    }
}

private class M3u8PreviewGenerator(override var params: ImageParams) : IPreviewGenerator {
    // generated images 1:1 to idx of hsl
    private var images: Array<Bitmap?> = arrayOf()

    companion object {
        private const val TAG = "PreviewImgM3u8"
    }


    // prefixSum[i] = sum(hsl.ts[0..i].time)
    // where [0] = 0, [1] = hsl.ts[0].time aka time at start of segment, do [b] - [a] for range a,b
    private var prefixSum: Array<Double> = arrayOf()

    // how many images has been generated
    override var loadedImages: Int = 0

    // how many images we can generate in total, == hsl.size ?: 0
    private var totalImages: Int = 0

    override fun hasPreview(): Boolean {
        return totalImages > 0 && loadedImages >= minOf(totalImages, 4)
    }

    override fun getPreviewImage(fraction: Float): Bitmap? {
        var bestIdx = -1
        var bestDiff = Double.MAX_VALUE
        synchronized(images) {
            // just find the best one in a for loop, we don't care about bin searching rn
            for (i in images.indices) {
                val diff = prefixSum[i].minus(fraction).absoluteValue
                if (diff > bestDiff) {
                    break
                }
                if (images[i] != null) {
                    bestIdx = i
                    bestDiff = diff
                }
            }
            return images.getOrNull(bestIdx)
        }
        /*
        val targetIndex = prefixSum.binarySearch(target)
        var ret = images[targetIndex]
        if (ret != null) {
            return ret
        }
        for (i in 0..images.size) {
            ret = images.getOrNull(i+targetIndex) ?:
        }*/
    }

    private fun clear() {
        synchronized(images) {
            currentJob?.cancel()
            // for (i in images.indices) {
            //     images[i]?.recycle()
            // }
            images = arrayOf()
            prefixSum = arrayOf()
            loadedImages = 0
            totalImages = 0
        }
    }

    override fun release() {
        clear()
        images = arrayOf()
    }

    override var durationMs: Long = 0L

    private var currentJob: Job? = null
    fun load(url: String, headers: Map<String, String>) {
        clear()
        currentJob?.cancel()
        currentJob = ioSafe {
            withContext(Dispatchers.IO) {
                Log.i(TAG, "Loading with url = $url headers = $headers")
                //tmpFile =
                //    File.createTempFile("video", ".ts", context.cacheDir).apply {
                //        deleteOnExit()
                //    }
                val retriever = MediaMetadataRetriever()
                val hsl = M3u8Helper2.hslLazy(
                    M3u8Helper.M3u8Stream(
                        streamUrl = url,
                        headers = headers
                    ),
                    selectBest = false,
                    requireAudio = false,
                )

                // no support for encryption atm
                if (hsl.isEncrypted) {
                    Log.i(TAG, "m3u8 is encrypted")
                    totalImages = 0
                    return@withContext
                }

                // total duration of the entire m3u8 in seconds
                val duration = hsl.allTsLinks.sumOf { it.time ?: 0.0 }
                durationMs = (duration * 1000.0).toLong()
                val durationInv = 1.0 / duration

                // if the total duration is less then 10s then something is very wrong or
                // too short playback to matter
                if (duration <= 10.0) {
                    totalImages = 0
                    return@withContext
                }

                totalImages = hsl.allTsLinks.size

                // we cant init directly as it is no guarantee of in order
                prefixSum = Array(hsl.allTsLinks.size + 1) { 0.0 }
                var runningSum = 0.0
                for (i in hsl.allTsLinks.indices) {
                    runningSum += (hsl.allTsLinks[i].time ?: 0.0)
                    prefixSum[i + 1] = runningSum * durationInv
                }
                synchronized(images) {
                    images = Array(hsl.size) { null }
                    loadedImages = 0
                }

                val maxLod = ceil(log2(duration)).toInt().coerceIn(MIN_LOD, MAX_LOD)
                val count = hsl.allTsLinks.size
                for (l in 1..maxLod) {
                    val items = (1 shl (l - 1))
                    for (i in 0 until items) {
                        val index = (count.div(1 shl l) + (i * count) / items).coerceIn(0, hsl.size)
                        if (synchronized(images) { images[index] } != null) {
                            continue
                        }
                        Log.i(TAG, "Generating preview for $index")

                        val ts = hsl.allTsLinks[index]
                        try {
                            retriever.setDataSource(ts.url, hsl.headers)
                            if (!isActive) {
                                return@withContext
                            }
                            val img = retriever.image(0, params)
                            if (!isActive) {
                                return@withContext
                            }
                            if (img == null || img.width <= 1 || img.height <= 1) continue
                            synchronized(images) {
                                images[index] = img
                                loadedImages += 1
                            }
                        } catch (t: Throwable) {
                            print(t)
                            continue
                        }
                    }
                }

            }
        }
    }
}


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