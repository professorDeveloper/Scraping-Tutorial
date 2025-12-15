package com.azamovhudstc.scarpingtutorial.vidsrc

data class Vidsrcccservers(
    val data: List<VidsrcccDaum>,
    val success: Boolean,
)

data class VidsrcccDaum(
    val name: String,
    val hash: String,
)

data class Vidsrcccm3u8(
    val data: VidsrcccData,
    val success: Boolean,
)

data class VidsrcccData(
    val type: String,
    val source: String,
)

data class VidsrcServersResponse(
    val data: List<VidsrcServer>
)

data class VidsrcServer(
    val name: String,
    val hash: String
)


data class VidsrcSourceResponse(
    val data: VidsrcccData
)
