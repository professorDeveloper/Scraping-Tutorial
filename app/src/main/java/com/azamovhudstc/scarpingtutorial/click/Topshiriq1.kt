package com.azamovhudstc.scarpingtutorial.click

fun main(args: Array<String>) {
    val list1 = listOf(1, 2, 3, 4, 5)
    val list2 = listOf(2, 4, 6)
    val list3 = emptyList<Int>()
    println("Natija 1: ${sumOfToqNumbers(list1)}")
    println("Natija 2: ${sumOfToqNumbers(list2)}")
    println("Natija 3: ${sumOfToqNumbers(list3)}")
}

fun sumOfToqNumbers(numbers: List<Int>): Int {
    var sum = 0
    numbers.forEach {
        if (it % 2 != 0) {
            sum += it
        }
    }
    return sum
}