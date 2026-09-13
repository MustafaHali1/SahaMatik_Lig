package com.example.sahamatik_lig.model

data class Oyuncu(
    val id: Int,
    val isim: String,
    val mevki: String,       // "Kaleci", "Defans", "Orta Saha", "Forvet"
    val takimAdi: String,
    var gol: Int = 0,
    var sari: Int = 0,
    var kirmizi: Int = 0
)