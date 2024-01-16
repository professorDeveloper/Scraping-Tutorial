package com.azamovhudstc.scarpingtutorial.shared

import com.google.gson.Gson

fun String.parseJson(): ArrayList<String> {
    val gson = Gson()
    var list = ArrayList<String>()
    list = gson.fromJson(this, ArrayList::class.java) as ArrayList<String>

    return list
}

fun ArrayList<String>.saveGson(): String {
    val gson = Gson()
    return gson.toJson(this)
}