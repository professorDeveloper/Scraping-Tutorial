package com.azamovhudstc.scarpingtutorial.utils

import com.azamovhudstc.scarpingtutorial.anibla.AmediaSearchData

fun String.removeEmTagsWithRegex(): String {
    val regex = Regex("<em>(.*?)</em>")
    return regex.replace(this, "$1")
}
fun showData(data: AmediaSearchData) {
    printlnColored("=================================", Color.GREEN)
    data.data.forEach {
        printlnColored(it.name.uz, Color.YELLOW)
    }
    printlnColored("=================================", Color.GREEN)

}