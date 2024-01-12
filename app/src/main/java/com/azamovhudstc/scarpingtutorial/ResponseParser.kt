package com.azamovhudstc.scarpingtutorial

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lagradost.nicehttp.ResponseParser
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import kotlin.reflect.KClass

data class Examples (
    @JsonProperty("cat") val cat: String,
    @JsonProperty("dog") val dog: String?,
    @JsonProperty("fish") val fish: String?
)

// Just pass in a
val cache = Cache(
    File("cacheDir", "http_cache"),
    50L * 1024L * 1024L // 50 MiB
)

val okHttpClient = OkHttpClient.Builder()
    .cache(cache)
    .build()
val parser = object : ResponseParser {
    val mapper: ObjectMapper = jacksonObjectMapper().configure(
        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
        false
    )

    override fun <T : Any> parse(text: String, kClass: KClass<T>): T {
        return mapper.readValue(text, kClass.java)
    }

    override fun <T : Any> parseSafe(text: String, kClass: KClass<T>): T? {
        return try {
            mapper.readValue(text, kClass.java)
        } catch (e: Exception) {
            null
        }
    }

    override fun writeValueAsString(obj: Any): String {
        return mapper.writeValueAsString(obj)
    }
}
