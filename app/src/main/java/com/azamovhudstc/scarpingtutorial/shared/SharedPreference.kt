package com.azamovhudstc.scarpingtutorial.shared

import android.content.Context
import com.azamovhudstc.scarpingtutorial.utils.SharedPreference


class LocalStorage(context: Context) : SharedPreference(context) {
    companion object {
        @Volatile
        lateinit var instance: LocalStorage
        fun getInstance(context: Context): LocalStorage {
            if (!::instance.isInitialized) {
                instance = LocalStorage(context)
            }
            return instance
        }
    }

    var token: String by Strings()
    var json by Strings()
}

fun String.saveToken(context: Context) {
    val sharedPreference = context.getSharedPreferences("shared", Context.MODE_PRIVATE)
    val editor = sharedPreference.edit()

    editor.putString("token", this.toString())
    editor.apply()
    println("Token Saqlandi: $this")
}

fun getToken(context: Context): String {
    val sharedPreference = context.getSharedPreferences("shared", Context.MODE_PRIVATE)
    return sharedPreference.getString("token", "") ?: "Token Not Found"
}