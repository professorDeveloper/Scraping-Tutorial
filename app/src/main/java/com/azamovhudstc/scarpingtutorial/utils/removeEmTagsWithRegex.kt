package com.azamovhudstc.scarpingtutorial.utils

fun String.removeEmTagsWithRegex(): String {
    val regex = Regex("<em>(.*?)</em>")
    return regex.replace(this, "$1")
}
