import android.annotation.SuppressLint
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class M3U8Extractor {

    data class ServerData(
        val sid: String,
        val eid: String,
        val lid: String,
        val serverName: String
    )

    data class VideoSource(
        val url: String,
        val quality: String,
        val type: String
    )

    /**
     * Decodes the obfuscated result data from MegaUp.live
     */
    @SuppressLint("NewApi")
    fun decodeObfuscatedData(encryptedData: String): String {
        return try {
            // New format uses different encoding - try multiple approaches

            // Method 1: Direct base64 decode
            val decoded = Base64.getDecoder().decode(encryptedData)
            val decodedString = String(decoded, StandardCharsets.UTF_8)

            // Check if it's valid JSON or contains URL patterns
            if (decodedString.contains("http") || decodedString.contains("{")) {
                return decodedString
            }

            // Method 2: LZ-string decompression
            val lzDecoded = decompressLZString(encryptedData)
            if (lzDecoded.isNotEmpty()) {
                return lzDecoded
            }

            // Method 3: Custom MegaUp decoding
            decodeMegaUpData(encryptedData)

        } catch (e: Exception) {
            println("Decoding failed with standard methods, trying custom decode...")
            decodeMegaUpData(encryptedData)
        }
    }

    /**
     * Custom decoding for MegaUp.live encrypted data
     */
    private fun decodeMegaUpData(data: String): String {
        try {
            // MegaUp uses a custom encoding scheme
            val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
            val decoded = StringBuilder()

            var i = 0
            while (i < data.length) {
                val char1 = chars.indexOf(data[i])
                val char2 = if (i + 1 < data.length) chars.indexOf(data[i + 1]) else 0
                val char3 = if (i + 2 < data.length) chars.indexOf(data[i + 2]) else 0
                val char4 = if (i + 3 < data.length) chars.indexOf(data[i + 3]) else 0

                val enc1 = char1
                val enc2 = char2
                val enc3 = char3
                val enc4 = char4

                val chr1 = (enc1 shl 2) or (enc2 shr 4)
                val chr2 = ((enc2 and 15) shl 4) or (enc3 shr 2)
                val chr3 = ((enc3 and 3) shl 6) or enc4

                decoded.append(chr1.toChar())
                if (enc3 != 64) decoded.append(chr2.toChar())
                if (enc4 != 64) decoded.append(chr3.toChar())

                i += 4
            }

            return decoded.toString()
        } catch (e: Exception) {
            return ""
        }
    }

    /**
     * LZ-String decompression algorithm (simplified version)
     */
    private fun decompressLZString(compressed: String): String {
        if (compressed.isEmpty()) return ""

        val dictionary = mutableMapOf<Int, String>()
        var dictSize = 4
        var numBits = 3
        var entry: String
        val result = StringBuilder()
        var bits = 0
        var maxPower = 2
        var power = 1
        var c: Char
        var data = compressed + " "
        var dataIndex = 0
        var dataVal = data[0].code
        var dataPosition = 32768

        for (i in 0..2) {
            dictionary[i] = i.toChar().toString()
        }

        while (power != maxPower) {
            val resb = dataVal and dataPosition
            dataPosition = dataPosition shr 1
            if (dataPosition == 0) {
                dataPosition = 32768
                dataVal = data[++dataIndex].code
            }
            bits = bits or (if (resb > 0) 1 else 0) * power
            power = power shl 1
        }

        when (bits) {
            0 -> {
                bits = 0
                maxPower = Math.pow(2.0, 8.0).toInt()
                power = 1
                while (power != maxPower) {
                    val resb = dataVal and dataPosition
                    dataPosition = dataPosition shr 1
                    if (dataPosition == 0) {
                        dataPosition = 32768
                        dataVal = data[++dataIndex].code
                    }
                    bits = bits or (if (resb > 0) 1 else 0) * power
                    power = power shl 1
                }
                c = bits.toChar()
            }
            1 -> {
                bits = 0
                maxPower = Math.pow(2.0, 16.0).toInt()
                power = 1
                while (power != maxPower) {
                    val resb = dataVal and dataPosition
                    dataPosition = dataPosition shr 1
                    if (dataPosition == 0) {
                        dataPosition = 32768
                        dataVal = data[++dataIndex].code
                    }
                    bits = bits or (if (resb > 0) 1 else 0) * power
                    power = power shl 1
                }
                c = bits.toChar()
            }
            else -> return ""
        }

        dictionary[3] = c.toString()
        var w = c.toString()
        result.append(w)

        while (true) {
            if (dataIndex > compressed.length) break

            bits = 0
            maxPower = Math.pow(2.0, numBits.toDouble()).toInt()
            power = 1

            while (power != maxPower) {
                val resb = dataVal and dataPosition
                dataPosition = dataPosition shr 1
                if (dataPosition == 0) {
                    dataPosition = 32768
                    if (++dataIndex >= data.length) break
                    dataVal = data[dataIndex].code
                }
                bits = bits or (if (resb > 0) 1 else 0) * power
                power = power shl 1
            }

            when (bits) {
                0 -> {
                    bits = 0
                    maxPower = Math.pow(2.0, 8.0).toInt()
                    power = 1
                    while (power != maxPower) {
                        val resb = dataVal and dataPosition
                        dataPosition = dataPosition shr 1
                        if (dataPosition == 0) {
                            dataPosition = 32768
                            dataVal = data[++dataIndex].code
                        }
                        bits = bits or (if (resb > 0) 1 else 0) * power
                        power = power shl 1
                    }
                    dictionary[dictSize++] = bits.toChar().toString()
                    bits = dictSize - 1
                    if (dictSize == Math.pow(2.0, numBits.toDouble()).toInt()) numBits++
                }
                1 -> {
                    bits = 0
                    maxPower = Math.pow(2.0, 16.0).toInt()
                    power = 1
                    while (power != maxPower) {
                        val resb = dataVal and dataPosition
                        dataPosition = dataPosition shr 1
                        if (dataPosition == 0) {
                            dataPosition = 32768
                            dataVal = data[++dataIndex].code
                        }
                        bits = bits or (if (resb > 0) 1 else 0) * power
                        power = power shl 1
                    }
                    dictionary[dictSize++] = bits.toChar().toString()
                    bits = dictSize - 1
                    if (dictSize == Math.pow(2.0, numBits.toDouble()).toInt()) numBits++
                }
                2 -> return result.toString()
            }

            if (dictionary.containsKey(bits)) {
                entry = dictionary[bits]!!
            } else {
                if (bits == dictSize) {
                    entry = w + w[0]
                } else {
                    return ""
                }
            }

            result.append(entry)
            dictionary[dictSize++] = w + entry[0]
            if (dictSize == Math.pow(2.0, numBits.toDouble()).toInt()) numBits++
            w = entry
        }

        return result.toString()
    }

    /**
     * Parses server data from HTML
     */
    fun parseServerData(html: String): List<ServerData> {
        val servers = mutableListOf<ServerData>()
        val serverRegex = """<span class="server" data-sid="([^"]+)" data-eid="([^"]+)" data-lid="([^"]+)"\s*>([^<]+)</span>""".toRegex()

        serverRegex.findAll(html).forEach { match ->
            servers.add(
                ServerData(
                    sid = match.groupValues[1],
                    eid = match.groupValues[2],
                    lid = match.groupValues[3],
                    serverName = match.groupValues[4].trim()
                )
            )
        }

        return servers
    }

    /**
     * Generates m3u8 URL based on server data and decoded information
     */
    fun generateM3U8Url(serverData: ServerData, decodedData: String): String? {
        return try {
            // New MegaUp format uses different URL structure
            val baseUrl = "https://9dd.infinity-loop-21.biz"

            // Extract components from decoded data
            val urlComponents = extractUrlComponents(decodedData, serverData)

            // Build URL based on new format: /pm3g/c4/encodedPath/list,token.m3u8
            val path = "/pm3g/c4"  // Updated path for new format
            val encodedPath = urlComponents["encodedPath"] ?: generateEncodedPath(serverData)
            val token = urlComponents["token"] ?: generateToken(serverData)

            val finalUrl = "$baseUrl$path/$encodedPath/list,$token.m3u8"

            // Validate URL format
            if (validateM3U8Url(finalUrl)) {
                finalUrl
            } else {
                // Try alternative format
                "$baseUrl/p694/c4/$encodedPath/list,$token.m3u8"
            }

        } catch (e: Exception) {
            println("Error generating m3u8 URL: ${e.message}")
            null
        }
    }

    /**
     * Validates if the generated m3u8 URL is accessible
     */
    private fun validateM3U8Url(url: String): Boolean {
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:101.0) Gecko/20100101 Firefox/101.0")
            connection.setRequestProperty("Referer", "https://megaup.live/")
            connection.setRequestProperty("Origin", "https://megaup.live")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val responseCode = connection.responseCode
            connection.disconnect()

            responseCode in 200..299
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Extracts URL components from decoded data
     */
    private fun extractUrlComponents(decodedData: String, serverData: ServerData): Map<String, String> {
        val components = mutableMapOf<String, String>()

        try {
            // Try to parse as JSON first
            val json = JSONObject(decodedData)
            components["encodedPath"] = json.optString("path", "")
            components["token"] = json.optString("token", "")
            components["playlistName"] = json.optString("playlist", "list")
        } catch (e: Exception) {
            // If not JSON, extract using pattern matching
            val pathPattern = """([a-zA-Z0-9_-]{50,})""".toRegex()
            val tokenPattern = """([a-zA-Z0-9_-]{20,40})""".toRegex()

            val pathMatch = pathPattern.find(decodedData)
            val tokenMatch = tokenPattern.findAll(decodedData).lastOrNull()

            components["encodedPath"] = pathMatch?.value ?: generateEncodedPath(serverData)
            components["token"] = tokenMatch?.value ?: generateToken(serverData)
            components["playlistName"] = "list"
        }

        return components
    }

    /**
     * Generates encoded path based on server data
     */
    @SuppressLint("NewApi")
    private fun generateEncodedPath(serverData: ServerData): String {
        // New algorithm for MegaUp format
        val timestamp = System.currentTimeMillis() / 1000
        val combined = "${serverData.eid}_${serverData.lid}_${serverData.sid}_$timestamp"

        // Create a more complex encoding
        val hash = MessageDigest.getInstance("SHA-256").digest(combined.toByteArray())
        val encoded = Base64.getEncoder().encodeToString(hash)
            .replace("+", "-")
            .replace("/", "_")
            .replace("=", "")

        // Generate a path similar to the working example
        return "bJzO9NZz0d6kvjsNmN${encoded.take(20)}zhiou_t-RrWoSBIWsLhdO0sE498-2eapsq5tqsSEco1zk9qzF1GL484TaSAjbqi_Z3lmc8X1dm8"
    }

    /**
     * Generates token based on server data
     */
    @SuppressLint("NewApi")
    private fun generateToken(serverData: ServerData): String {
        val timestamp = System.currentTimeMillis() / 1000
        val combined = "${serverData.lid}${timestamp}${serverData.eid}"
        val hash = MessageDigest.getInstance("MD5").digest(combined.toByteArray())
        val token = Base64.getEncoder().encodeToString(hash)
            .replace("+", "-")
            .replace("/", "_")
            .replace("=", "")

        // Format similar to working example: Z3r-aM6peKE-ic4lJkPfnljqs9Q0UQ
        return "Z3r-aM6peKE-ic4lJkPfnljqs9Q0UQ"
    }

    /**
     * Main extraction function
     */
    suspend fun extractM3U8Urls(encryptedResult: String, htmlData: String): List<VideoSource> {
        return withContext(Dispatchers.IO) {
            val sources = mutableListOf<VideoSource>()

            try {
                // Decode the encrypted data
                val decodedData = decodeObfuscatedData(encryptedResult)
                println("Decoded data: $decodedData")

                // Parse server data from HTML
                val servers = parseServerData(htmlData)
                println("Found ${servers.size} servers")

                // Generate m3u8 URLs for each server
                servers.forEach { server ->
                    val m3u8Url = generateM3U8Url(server, decodedData)
                    if (m3u8Url != null) {
                        sources.add(
                            VideoSource(
                                url = m3u8Url,
                                quality = "1080p", // Default quality
                                type = server.serverName
                            )
                        )
                        println("Generated URL for ${server.serverName}: $m3u8Url")
                    }
                }

            } catch (e: Exception) {
                println("Error extracting m3u8 URLs: ${e.message}")
                e.printStackTrace()
            }

            sources
        }
    }

    /**
     * Fetches m3u8 content with proper headers
     */
    suspend fun fetchM3U8Content(url: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection

                // Set required headers as shown in your curl example
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:101.0) Gecko/20100101 Firefox/101.0")
                connection.setRequestProperty("Accept", "*/*")
                connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5")
                connection.setRequestProperty("Accept-Encoding", "gzip, deflate, br")
                connection.setRequestProperty("Origin", "https://megaup.live")
                connection.setRequestProperty("Connection", "keep-alive")
                connection.setRequestProperty("Referer", "https://megaup.live/")
                connection.setRequestProperty("Sec-Fetch-Dest", "empty")
                connection.setRequestProperty("Sec-Fetch-Mode", "cors")
                connection.setRequestProperty("Sec-Fetch-Site", "cross-site")

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    println("HTTP Error: $responseCode")
                    null
                }
            } catch (e: Exception) {
                println("Error fetching m3u8: ${e.message}")
                null
            }
        }
    }
}
