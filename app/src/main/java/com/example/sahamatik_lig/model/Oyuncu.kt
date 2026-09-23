package com.example.sahamatik_lig.model

import java.io.Serializable

data class Oyuncu(
    val id: String = "",          // Firestore document ID için String en güvenlisidir
    val username: String = "",    // Benzersiz kullanıcı adı (@ahmet10)
    val isim: String = "",
    val mevki: String = "",       // "Kaleci", "Defans", "Orta Saha", "Forvet"
    val takimAdi: String = "",
    val ligAdi: String = "",      // Hangi lige/maça ait olduğunu bilmesi için
    var gol: Int = 0,             // Bu maçtaki/ligdeki gol
    var sari: Int = 0,            // Bu maçtaki sarı kart
    var kirmizi: Int = 0,         // Bu maçtaki kırmızı kart
    var formaNo: Int = 0,         // Forma numarası (opsiyonel)
    var isYedek: Boolean = false, // Yedek durumu (opsiyonel)
    val profilFotoUri: String = "", // Profil fotoğrafı
    var toplamMac: Int = 0,       // Kariyer toplam maç
    var toplamGol: Int = 0,       // Kariyer toplam gol
    var toplamSari: Int = 0,      // Kariyer toplam sarı kart
    var toplamKirmizi: Int = 0,   // Kariyer toplam kırmızı kart
    val katildigiLigler: List<String> = emptyList(),
    val katildigiMaclar: List<String> = emptyList()
) : Serializable