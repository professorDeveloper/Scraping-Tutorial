package com.azamovhudstc.scarpingtutorial.vidsrc

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import com.lagradost.cloudstream3.base64Encode
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

@SuppressLint("NewApi")
fun base64Decode(input: String): ByteArray {
    return Base64.getDecoder().decode(input)
}

@SuppressLint("NewApi")
fun base64Encode(input: String): String {
    return Base64.getEncoder().encodeToString(input.toByteArray())
}

fun main(args: Array<String>) {
    println(base64Decode("aGU1aWRnb2JJUV8=").decodeToString())
}
 fun generateVidsrcVrf(movieId: String, userId: String): String {
    val keyBytes = MessageDigest.getInstance("SHA-256").digest(userId.toByteArray())
    val key = SecretKeySpec(keyBytes, "AES")
    val iv = IvParameterSpec(ByteArray(16))
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, key, iv)
    val plaintext = movieId.toByteArray()
    val ciphertext = cipher.doFinal(plaintext)
    val encoded = base64Encode(ciphertext)
    val urlSafe = encoded.replace('+', '-').replace('/', '_').replace("=", "")
    return urlSafe
}

@RequiresApi(Build.VERSION_CODES.O)
fun generateVrfAES(movieId: String, userId: String): String {
    val driveKey = base64Decode("aGU1aWRnb2JJUV8=")
    val keyData = "$driveKey$userId".toByteArray(Charsets.UTF_8)
    val keyBytes = MessageDigest.getInstance("SHA-256").digest(keyData)
    val keySpec = SecretKeySpec(keyBytes, "AES")
    val ivSpec = IvParameterSpec(ByteArray(16))

    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)

    val encrypted = cipher.doFinal(movieId.toByteArray(Charsets.UTF_8))

    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(encrypted)
}

val decryptMethods: Map<String, (String) -> String> = mapOf(

    "TsA2KGDGux" to { input ->
        val decoded = String(
            base64Decode(
                input.reversed()
                    .replace("-", "+")
                    .replace("_", "/")
            )
        )
        decoded.map { (it.code - 7).toChar() }.joinToString("")
    },

    "ux8qjPHC66" to { input ->
        val reversed = input.reversed()
        val hex = reversed.chunked(2).map { it.toInt(16).toChar() }.joinToString("")
        val key = "X9a(O;FMV2-7VO5x;Ao\u0005:dN1NoFs?j,"
        hex.mapIndexed { i, ch ->
            (ch.code xor key[i % key.length].code).toChar()
        }.joinToString("")
    },

    "xTyBxQyGTA" to { input ->
        val filtered = input.reversed().filterIndexed { i, _ -> i % 2 == 0 }
        String(base64Decode(filtered))
    },

    "IhWrImMIGL" to { input ->
        val rot13 = input.reversed().map { ch ->
            when {
                ch in 'a'..'m' || ch in 'A'..'M' -> (ch.code + 13).toChar()
                ch in 'n'..'z' || ch in 'N'..'Z' -> (ch.code - 13).toChar()
                else -> ch
            }
        }.joinToString("")
        String(base64Decode(rot13.reversed()))
    },

    "o2VSUnjnZl" to { input ->
        val map =
            ("xyzabcdefghijklmnopqrstuvwXYZABCDEFGHIJKLMNOPQRSTUVW" zip
                    "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ").toMap()
        input.map { map[it] ?: it }.joinToString("")
    },

    "eSfH1IRMyL" to { input ->
        val shifted = input.reversed().map { (it.code - 1).toChar() }.joinToString("")
        shifted.chunked(2).map { it.toInt(16).toChar() }.joinToString("")
    },

    "Oi3v1dAlaM" to { input ->
        val decoded = String(
            base64Decode(
                input.reversed()
                    .replace("-", "+")
                    .replace("_", "/")
            )
        )
        decoded.map { (it.code - 5).toChar() }.joinToString("")
    },

    "sXnL9MQIry" to { input ->
        val xorKey = "pWB9V)[*4I`nJpp?ozyB~dbr9yt!_n4u"
        val hex = input.chunked(2).map { it.toInt(16).toChar() }.joinToString("")
        val xored = hex.mapIndexed { i, ch ->
            (ch.code xor xorKey[i % xorKey.length].code).toChar()
        }.joinToString("")
        val shifted = xored.map { (it.code - 3).toChar() }.joinToString("")
        String(base64Decode(shifted))
    },

    "JoAHUMCLXV" to { input ->
        val decoded = String(
            base64Decode(
                input.reversed()
                    .replace("-", "+")
                    .replace("_", "/")
            )
        )
        decoded.map { (it.code - 3).toChar() }.joinToString("")
    },

    "KJHidj7det" to { input ->
        val decoded = String(
            base64Decode(input.drop(10).dropLast(16))
        )
        val key = """3SAY~#%Y(V%>5d/Yg${'$'}G[Lh1rK4a;7ok"""
        decoded.mapIndexed { i, ch ->
            (ch.code xor key[i % key.length].code).toChar()
        }.joinToString("")
    },

    "playerjs" to { x ->
        try {
            var a = x.drop(2)

            val b1: (String) -> String = { base64Encode(it) }
            val b2: (String) -> String = { String(base64Decode(it)) }

            val patterns = listOf(
                "*,4).(_)()", "33-*.4/9[6", ":]&*1@@1=&", "=(=:19705/", "%?6497.[:4"
            )

            patterns.forEach { k ->
                a = a.replace("/@#@/" + b1(k), "")
            }

            b2(a)
        } catch (e: Exception) {
            "Failed to decode: ${e.message}"
        }
    }
)

