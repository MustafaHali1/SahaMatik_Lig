package com.example.sahamatik_lig.util

import android.util.Log
import com.example.sahamatik_lig.model.Oyuncu
import com.google.firebase.firestore.FirebaseFirestore

object MockVeriYukleyici {

    /**
     * 20 Sahte oyuncunun kullanıcı adları (Temizleme yapılırken bunlar silinmez)
     */
    val sahteUsernameler: Set<String> = setOf(
        "panter_mert", "baris_eldiven", "duvar_burak", "stoper_semih", "hakan_tank",
        "alperen_def", "serdar_kaya", "solbek_yasin", "maestro_kerem", "on_numara_emre",
        "pasor_selim", "cengiz_ciger", "tolga_cm", "ferdi_box2box", "golcu_kral",
        "fisek_sinan", "roket_yigit", "sniper_can", "avci_burak", "torpido_emre"
    )

    /**
     * 2 Kaleci + 6 Defans + 6 Orta Saha + 6 Forvet = 20 Oyuncu
     * Tam 2 halı saha takımını (10 vs 10) çıkaracak dengeli mock veri havuzu.
     * Hem 'users' hem de 'oyuncuProfilleri' koleksiyonuna çift yönlü eşitlenerek yazılır.
     */
    fun sahteKullanicilariYukle(onTamamlandi: () -> Unit = {}) {
        val db = FirebaseFirestore.getInstance()
        val usersCollection = db.collection("users")
        val profillerCollection = db.collection("oyuncuProfilleri")

        val sahteKullanicilar = listOf(
            // 🧤 Kaleciler (Tam 2 Kişi — 2 Takım için 1'er Kaleci)
            Oyuncu(username = "@panter_mert", isim = "Mert Kaya", mevki = "Kaleci", formaNo = 1, toplamMac = 12, toplamGol = 0, gol = 0, toplamSari = 1, toplamKirmizi = 0),
            Oyuncu(username = "@baris_eldiven", isim = "Barış Çelik", mevki = "Kaleci", formaNo = 12, toplamMac = 15, toplamGol = 0, gol = 0, toplamSari = 2, toplamKirmizi = 0),

            // 🛡️ Defanslar (6 Kişi)
            Oyuncu(username = "@duvar_burak", isim = "Burak Şahin", mevki = "Defans", formaNo = 4, toplamMac = 20, toplamGol = 2, gol = 0, toplamSari = 4, toplamKirmizi = 1),
            Oyuncu(username = "@stoper_semih", isim = "Semih Aydın", mevki = "Defans", formaNo = 5, toplamMac = 14, toplamGol = 1, gol = 0, toplamSari = 3, toplamKirmizi = 0),
            Oyuncu(username = "@hakan_tank", isim = "Hakan Koç", mevki = "Defans", formaNo = 2, toplamMac = 18, toplamGol = 3, gol = 0, toplamSari = 5, toplamKirmizi = 0),
            Oyuncu(username = "@alperen_def", isim = "Alperen Yıldız", mevki = "Defans", formaNo = 3, toplamMac = 10, toplamGol = 0, gol = 0, toplamSari = 1, toplamKirmizi = 0),
            Oyuncu(username = "@serdar_kaya", isim = "Serdar Arslan", mevki = "Defans", formaNo = 22, toplamMac = 9, toplamGol = 1, gol = 0, toplamSari = 2, toplamKirmizi = 0),
            Oyuncu(username = "@solbek_yasin", isim = "Yasin Polat", mevki = "Defans", formaNo = 77, toplamMac = 16, toplamGol = 2, gol = 0, toplamSari = 2, toplamKirmizi = 0),

            // ⚡ Orta Sahalar (6 Kişi)
            Oyuncu(username = "@maestro_kerem", isim = "Kerem Öztürk", mevki = "Orta Saha", formaNo = 8, toplamMac = 22, toplamGol = 9, gol = 0, toplamSari = 2, toplamKirmizi = 0),
            Oyuncu(username = "@on_numara_emre", isim = "Emre Kılıç", mevki = "Orta Saha", formaNo = 10, toplamMac = 19, toplamGol = 11, gol = 0, toplamSari = 1, toplamKirmizi = 0),
            Oyuncu(username = "@pasor_selim", isim = "Selim Doğan", mevki = "Orta Saha", formaNo = 6, toplamMac = 13, toplamGol = 4, gol = 0, toplamSari = 0, toplamKirmizi = 0),
            Oyuncu(username = "@cengiz_ciger", isim = "Cengiz Kurt", mevki = "Orta Saha", formaNo = 14, toplamMac = 17, toplamGol = 5, gol = 0, toplamSari = 3, toplamKirmizi = 0),
            Oyuncu(username = "@tolga_cm", isim = "Tolga Acar", mevki = "Orta Saha", formaNo = 18, toplamMac = 11, toplamGol = 3, gol = 0, toplamSari = 1, toplamKirmizi = 0),
            Oyuncu(username = "@ferdi_box2box", isim = "Ferdi Aslan", mevki = "Orta Saha", formaNo = 20, toplamMac = 21, toplamGol = 7, gol = 0, toplamSari = 2, toplamKirmizi = 0),

            // ⚽ Forvetler (6 Kişi)
            Oyuncu(username = "@golcu_kral", isim = "Batuhan Er", mevki = "Forvet", formaNo = 9, toplamMac = 25, toplamGol = 34, gol = 0, toplamSari = 1, toplamKirmizi = 0),
            Oyuncu(username = "@fisek_sinan", isim = "Sinan Güler", mevki = "Forvet", formaNo = 7, toplamMac = 16, toplamGol = 18, gol = 0, toplamSari = 0, toplamKirmizi = 0),
            Oyuncu(username = "@roket_yigit", isim = "Yiğit Korkmaz", mevki = "Forvet", formaNo = 11, toplamMac = 14, toplamGol = 15, gol = 0, toplamSari = 2, toplamKirmizi = 0),
            Oyuncu(username = "@sniper_can", isim = "Can Yaman", mevki = "Forvet", formaNo = 19, toplamMac = 18, toplamGol = 21, gol = 0, toplamSari = 1, toplamKirmizi = 0),
            Oyuncu(username = "@avci_burak", isim = "Burak Yılmaz", mevki = "Forvet", formaNo = 17, toplamMac = 15, toplamGol = 17, gol = 0, toplamSari = 2, toplamKirmizi = 0),
            Oyuncu(username = "@torpido_emre", isim = "Emre Akman", mevki = "Forvet", formaNo = 99, toplamMac = 13, toplamGol = 12, gol = 0, toplamSari = 0, toplamKirmizi = 0)
        )

        val batch = db.batch()

        for (oyuncu in sahteKullanicilar) {
            val cleanUsername = oyuncu.username.removePrefix("@").trim()
            val data = mapOf(
                "username" to oyuncu.username,
                "isim" to oyuncu.isim,
                "adSoyad" to oyuncu.isim,
                "mevki" to oyuncu.mevki,
                "formaNo" to oyuncu.formaNo,
                "profilFotoUri" to oyuncu.profilFotoUri,
                "toplamMac" to oyuncu.toplamMac,
                "toplamGol" to oyuncu.toplamGol,
                "gol" to 0,
                "toplamSari" to oyuncu.toplamSari,
                "sari" to 0,
                "toplamKirmizi" to oyuncu.toplamKirmizi,
                "kirmizi" to 0,
                "katildigiLigler" to emptyList<String>(),
                "katildigiMaclar" to emptyList<String>()
            )

            // users koleksiyonuna yaz
            batch.set(usersCollection.document(cleanUsername), data)
            // oyuncuProfilleri koleksiyonuna yaz
            batch.set(profillerCollection.document(cleanUsername), data)
        }

        batch.commit()
            .addOnSuccessListener {
                Log.d("SahaMatik", "20 dengeli sahte kullanıcı başarıyla Firestore'a yazıldı!")
                onTamamlandi()
            }
            .addOnFailureListener { e ->
                Log.e("SahaMatik", "Mock kullanıcılar yüklenirken hata: ${e.message}")
            }
    }
}
