package com.example.sahamatik_lig.model

data class Oyuncu(
    val id: String = "",          // Firestore document ID için String en güvenlisidir
    val isim: String = "",
    val mevki: String = "",       // "Kaleci", "Defans", "Orta Saha", "Forvet"
    val takimAdi: String = "",
    val ligAdi: String = "",      // Hangi lige ait olduğunu bilmesi için
    var gol: Int = 0,
    var sari: Int = 0,
    var kirmizi: Int = 0,
    var formaNo: Int = 0,         // Forma numarası (opsiyonel)
    var isYedek: Boolean = false  // Yedek durumu (opsiyonel)
)