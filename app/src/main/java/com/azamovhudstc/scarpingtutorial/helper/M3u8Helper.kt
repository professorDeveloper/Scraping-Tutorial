package com.azamovhudstc.scarpingtutorial.helper

import com.azamovhudstc.scarpingtutorial.utils.Utils.httpClient
import com.azamovhudstc.scarpingtutorial.utils.parser
import com.lagradost.nicehttp.Requests
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

/** backwards api surface */
class M3u8Helper {
    companion object {
    }


    data class M3u8Stream(
        val streamUrl: String,
        val quality: Int? = null,
        val headers: Map<String, String> = mapOf()
    )

}


object M3u8Helper2 {
    private val TAG = "M3u8Helper"


    private val ENCRYPTION_DETECTION_REGEX = Regex("#EXT-X-KEY:METHOD=([^,]+),")
    private val ENCRYPTION_URL_IV_REGEX =
        Regex("#EXT-X-KEY:METHOD=([^,]+),URI=\"([^\"]+)\"(?:,IV=(.*))?")
    private val QUALITY_REGEX =
        Regex("""#EXT-X-STREAM-INF:(?:(?:.*?(?:RESOLUTION=\d+x(\d+)).*?\s+(.*))|(?:.*?\s+(.*)))""")
    private val TS_EXTENSION_REGEX =
        Regex("""#EXTINF:(([0-9]*[.])?[0-9]+|).*\n(.+?\n)""") // fuck it we ball, who cares about the type anyways
    //Regex("""(.*\.(ts|jpg|html).*)""") //.jpg here 'case vizcloud uses .jpg instead of .ts

    private fun absoluteExtensionDetermination(url: String): String? {
        val split = url.split("/")
        val gg: String = split[split.size - 1].split("?")[0]
        return if (gg.contains(".")) {
            gg.split(".").ifEmpty { null }?.last()
        } else null
    }

    private fun toBytes16Big(n: Int): ByteArray {
        return ByteArray(16) {
            val fixed = n / 256.0.pow((15 - it))
            (maxOf(0, fixed.toInt()) % 256).toByte()
        }
    }

    private fun defaultIv(index: Int): ByteArray {
        return toBytes16Big(index + 1)
    }

    fun getDecrypted(
        secretKey: ByteArray,
        data: ByteArray,
        iv: ByteArray = byteArrayOf(),
        index: Int,
    ): ByteArray {
        val ivKey = if (iv.isEmpty()) defaultIv(index) else iv
        val c = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val skSpec = SecretKeySpec(secretKey, "AES")
        val ivSpec = IvParameterSpec(ivKey)
        c.init(Cipher.DECRYPT_MODE, skSpec, ivSpec)
        return c.doFinal(data)
    }

    private fun getParentLink(uri: String): String {
        val split = uri.split("/").toMutableList()
        split.removeAt(split.lastIndex)
        return split.joinToString("/")
    }

    private fun isNotCompleteUrl(url: String): Boolean {
        return !url.startsWith("https://") && !url.startsWith("http://")
    }


    data class TsLink(
        val url: String,
        val time: Double?,
    )

    data class LazyHlsDownloadData(
        private val encryptionData: ByteArray,
        private val encryptionIv: ByteArray,
        val isEncrypted: Boolean,
        val allTsLinks: List<TsLink>,
        val relativeUrl: String,
        val headers: Map<String, String>,
    ) {

        val size get() = allTsLinks.size


    }

    @Throws
    suspend fun hslLazy(
        playlistStream: M3u8Helper.M3u8Stream,
        selectBest: Boolean = true,
        requireAudio: Boolean,
        depth: Int = 3,
    ): LazyHlsDownloadData {
        // Allow nesting, but not too much:
        // Master Playlist (different videos)
        // -> Media Playlist (different qualities of the same video)
        // -> Media Segments (ts files of a single video)
        if (depth < 0) {
            throw IllegalArgumentException()
        }

        val playlistResponse = Requests(baseClient = httpClient, responseParser = parser).get(
            playlistStream.streamUrl,
            headers = playlistStream.headers,
            verify = false
        ).text

        val parsed = HlsPlaylistParser.parse(playlistStream.streamUrl, playlistResponse)
        if (parsed != null) {
            // find first with no audio group if audio is required, as otherwise muxing is required
            // as m3u8 files can include separate tracks for dubs/subs
            val variants = if (requireAudio) {
                parsed.variants.filter { it.isPlayableStandalone(parsed) }
            } else {
                parsed.variants.filter { !it.isTrickPlay() }
            }

            if (variants.isEmpty()) {
                throw IllegalStateException(
                    if (requireAudio) {
                        "M3u8 contains no video with audio"
                    } else {
                        "M3u8 contains no video"
                    }
                )
            }

            // M3u8 can also include different camera angles (parsed.videos) for the same quality
            // but here the default is used
            val bestVideo = if (selectBest) {
                variants.maxBy { (it.format.width * it.format.height).toLong() * 1000L + it.format.averageBitrate.toLong() }
            } else {
                variants.minBy { (it.format.width * it.format.height).toLong() * 1000L + it.format.averageBitrate.toLong() }
            }

            val quality = bestVideo.format.height
            return hslLazy(
                playlistStream = M3u8Helper.M3u8Stream(
                    bestVideo.url.toString(),
                    if (quality > 0) quality else null,
                    playlistStream.headers
                ),
                selectBest = selectBest,
                requireAudio = requireAudio,
                depth = depth - 1
            )
        }

        var encryptionIv = byteArrayOf()
        var encryptionData = byteArrayOf()

        val match = ENCRYPTION_URL_IV_REGEX.find(playlistResponse)?.groupValues
        val encryptionState: Boolean

        if (!match.isNullOrEmpty()) {
            encryptionState = true
            var encryptionUri = match[2]

            if (isNotCompleteUrl(encryptionUri)) {
                encryptionUri = "${getParentLink(playlistStream.streamUrl)}/$encryptionUri"
            }

            encryptionIv = match[3].toByteArray()
            val encryptionKeyResponse =
                Requests(baseClient = httpClient, responseParser = parser).get(
                    encryptionUri,
                    headers = playlistStream.headers,
                    verify = false
                )
            encryptionData = encryptionKeyResponse.body.bytes()
        } else {
            encryptionState = false
        }

        val relativeUrl = getParentLink(playlistStream.streamUrl)
        val allTsList = TS_EXTENSION_REGEX.findAll(playlistResponse + "\n").map { ts ->
            val time = ts.groupValues[1]
            val value = ts.groupValues[3]
            val url = if (isNotCompleteUrl(value)) {
                "$relativeUrl/${value}"
            } else {
                value
            }
            TsLink(url = url, time = time.toDoubleOrNull())
        }.toList()

        if (allTsList.isEmpty()) throw IllegalStateException("M3u8 must contains TS files")

        return LazyHlsDownloadData(
            encryptionData = encryptionData,
            encryptionIv = encryptionIv,
            isEncrypted = encryptionState,
            allTsLinks = allTsList,
            relativeUrl = relativeUrl,
            headers = playlistStream.headers
        )
    }
}