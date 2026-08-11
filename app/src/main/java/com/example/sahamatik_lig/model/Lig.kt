package com.example.sahamatik_lig.model

data class Lig(
    val id: Int,
    val name: String,
    val takimsayisi:Int=0,
    val takimlar: List<String> = emptyList()

)
