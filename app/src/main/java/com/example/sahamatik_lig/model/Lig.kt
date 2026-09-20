package com.example.sahamatik_lig.model

import java.io.Serializable

data class Lig(
    val id: Int = 0,
    val name: String = "",
    val takimsayisi: Int = 0,
    val takimlar: List<String> = emptyList(),
    val formatTipi: String = "KLASIK",
    val gruplarMap: Map<String, List<String>>? = null,
    val olusturulmaTarihi: Long = System.currentTimeMillis()
) : Serializable
