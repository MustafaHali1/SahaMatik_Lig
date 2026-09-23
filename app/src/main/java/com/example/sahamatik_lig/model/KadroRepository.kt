package com.example.sahamatik_lig.model

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

object KadroRepository {

    private val db by lazy { FirebaseFirestore.getInstance() }

    private fun safeDoc(name: String): String = name.trim().replace("/", "-")

    // ─── TAKIM ────────────────────────────────────────────────────────────────

    fun takimOlustur(ligAdi: String, takimAdi: String, onSuccess: () -> Unit = {}, onError: (Exception) -> Unit = {}) {
        val takimRef = db.collection("ligler")
            .document(safeDoc(ligAdi))
            .collection("takimlar")
            .document(safeDoc(takimAdi))

        val takimData = mapOf(
            "takimAdi" to takimAdi.trim(),
            "ligAdi" to ligAdi.trim()
        )
        takimRef.set(takimData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e) }
    }

    fun takimSil(
        ligAdi: String,
        takimAdi: String,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        db.collection("ligler").document(safeDoc(ligAdi))
            .collection("takimlar").document(safeDoc(takimAdi))
            .collection("oyuncular").get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    doc.reference.delete()
                }
                db.collection("ligler").document(safeDoc(ligAdi))
                    .collection("takimlar").document(safeDoc(takimAdi))
                    .delete()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e -> onError(e) }
            }
            .addOnFailureListener { e -> onError(e) }
    }

    fun takimLogosuGuncelle(
        ligAdi: String,
        takimAdi: String,
        logoKey: String,
        guncellenenVeri: Map<String, Any>,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        val takimRef = db.collection("ligler")
            .document(safeDoc(ligAdi))
            .collection("takimlar")
            .document(safeDoc(takimAdi))

        takimRef.update(guncellenenVeri)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e) }
    }

    // ─── OYUNCU ───────────────────────────────────────────────────────────────

    fun oyuncuEkle(
        oyuncu: Oyuncu,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        val oyuncularRef = db.collection("ligler")
            .document(safeDoc(oyuncu.ligAdi))
            .collection("takimlar")
            .document(safeDoc(oyuncu.takimAdi))
            .collection("oyuncular")

        val yeniDoc = oyuncularRef.document()
        val eklenecekOyuncu = oyuncu.copy(id = yeniDoc.id)

        yeniDoc.set(eklenecekOyuncu)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e) }
    }

    fun oyuncuGuncelle(
        ligAdi: String,
        takimAdi: String,
        oyuncuId: String,
        guncellenenVeri: Map<String, Any>,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        val oyuncuRef = db.collection("ligler")
            .document(safeDoc(ligAdi))
            .collection("takimlar")
            .document(safeDoc(takimAdi))
            .collection("oyuncular")
            .document(oyuncuId)

        oyuncuRef.update(guncellenenVeri)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e) }
    }

    fun oyuncuSil(
        ligAdi: String,
        takimAdi: String,
        oyuncuId: String,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        val oyuncuRef = db.collection("ligler")
            .document(safeDoc(ligAdi))
            .collection("takimlar")
            .document(safeDoc(takimAdi))
            .collection("oyuncular")
            .document(oyuncuId)

        oyuncuRef.delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e) }
    }

    fun takimOyunculariniCanliDinle(
        ligAdi: String,
        takimAdi: String,
        onUpdate: (List<Oyuncu>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return db.collection("ligler")
            .document(safeDoc(ligAdi))
            .collection("takimlar")
            .document(safeDoc(takimAdi))
            .collection("oyuncular")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val liste = snapshot.documents.mapNotNull { it.toObject(Oyuncu::class.java) }
                    onUpdate(liste)
                }
            }
    }

    fun takimOyunculariniGetir(
        ligAdi: String,
        takimAdi: String,
        onResult: (List<Oyuncu>) -> Unit
    ) {
        db.collection("ligler")
            .document(safeDoc(ligAdi))
            .collection("takimlar")
            .document(safeDoc(takimAdi))
            .collection("oyuncular")
            .get()
            .addOnSuccessListener { snapshot ->
                val liste = snapshot.documents.mapNotNull { doc ->
                    val o = doc.toObject(Oyuncu::class.java)
                    o?.copy(id = doc.id)
                }
                onResult(liste)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    // ─── LİG ─────────────────────────────────────────────────────────────────

    fun ligKaydet(lig: Lig, onSuccess: () -> Unit = {}, onError: (Exception) -> Unit = {}) {
        // takimlar listesi de kaydedilmeli — yoksa uygulama yeniden açıldığında boş gelir!
        val ligData = mapOf(
            "name"              to lig.name,
            "takimsayisi"       to lig.takimsayisi,
            "formatTipi"        to lig.formatTipi,
            "takimlar"          to lig.takimlar,          // ← KRİTİK: eksikti!
            "gruplarMap"        to (lig.gruplarMap ?: emptyMap<String, List<String>>()),
            "olusturulmaTarihi" to lig.olusturulmaTarihi,
            "olusturanId"       to lig.olusturanId
        )
        db.collection("ligler").document(safeDoc(lig.name)).set(ligData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e) }
    }

    fun ligleriCanliDinle(
        onUpdate: (List<Lig>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return db.collection("ligler")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val liste = snapshot.documents.mapNotNull { doc ->
                        try {
                            // toObject() yerine manuel parse — List<String> güvenli gelsin
                            val name        = doc.getString("name") ?: return@mapNotNull null
                            val takimsayisi = (doc.getLong("takimsayisi") ?: 0L).toInt()
                            val formatTipi  = doc.getString("formatTipi") ?: "KLASIK"
                            val olusturulmaTarihi = doc.getLong("olusturulmaTarihi") ?: 0L
                            val olusturanId = doc.getString("olusturanId") ?: ""

                            // takimlar: Firestore'da List<String> olarak saklanır
                            @Suppress("UNCHECKED_CAST")
                            val takimlar = (doc.get("takimlar") as? List<String>) ?: emptyList()

                            // gruplarMap: Map<String, List<String>> olarak saklanır
                            @Suppress("UNCHECKED_CAST")
                            val gruplarRaw = doc.get("gruplarMap") as? Map<String, List<String>>
                            val gruplarMap: Map<String, List<String>>? =
                                if (gruplarRaw.isNullOrEmpty()) null else gruplarRaw

                            Lig(
                                name              = name,
                                takimsayisi       = takimsayisi,
                                formatTipi        = formatTipi,
                                takimlar          = takimlar,
                                gruplarMap        = gruplarMap,
                                olusturulmaTarihi = olusturulmaTarihi,
                                olusturanId       = olusturanId
                            )
                        } catch (e: Exception) {
                            null // parse hatası varsa bu ligi atla
                        }
                    }
                    onUpdate(liste)
                }
            }
    }

    fun ligGetir(ligAdi: String, onResult: (Lig?) -> Unit) {
        db.collection("ligler").document(safeDoc(ligAdi)).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val name = doc.getString("name") ?: ligAdi
                    val takimsayisi = (doc.getLong("takimsayisi") ?: 0L).toInt()
                    val formatTipi = doc.getString("formatTipi") ?: "KLASIK"
                    val olusturulmaTarihi = doc.getLong("olusturulmaTarihi") ?: 0L
                    val olusturanId = doc.getString("olusturanId") ?: ""
                    @Suppress("UNCHECKED_CAST")
                    val takimlar = (doc.get("takimlar") as? List<String>) ?: emptyList()
                    @Suppress("UNCHECKED_CAST")
                    val gruplarRaw = doc.get("gruplarMap") as? Map<String, List<String>>

                    val lig = Lig(
                        name = name,
                        takimsayisi = takimsayisi,
                        formatTipi = formatTipi,
                        takimlar = takimlar,
                        gruplarMap = if (gruplarRaw.isNullOrEmpty()) null else gruplarRaw,
                        olusturulmaTarihi = olusturulmaTarihi,
                        olusturanId = olusturanId
                    )
                    onResult(lig)
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    fun ligSil(
        ligAdi: String,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        val safeLigAdi = safeDoc(ligAdi)
        val baseRef = db.collection("ligler").document(safeLigAdi)

        baseRef.collection("takimlar").get()
            .addOnSuccessListener { takimlarSnapshot ->
                val tumTakimlar = takimlarSnapshot.documents

                if (tumTakimlar.isEmpty()) {
                    ligSilDevam(safeLigAdi, baseRef, onSuccess, onError)
                    return@addOnSuccessListener
                }

                var tamamlananTakim = 0
                for (takimDoc in tumTakimlar) {
                    takimDoc.reference.collection("oyuncular").get()
                        .addOnSuccessListener { oyuncuSnapshot ->
                            for (oyuncuDoc in oyuncuSnapshot.documents) {
                                oyuncuDoc.reference.delete()
                            }
                            takimDoc.reference.delete().addOnSuccessListener {
                                tamamlananTakim++
                                if (tamamlananTakim == tumTakimlar.size) {
                                    ligSilDevam(safeLigAdi, baseRef, onSuccess, onError)
                                }
                            }
                        }
                        .addOnFailureListener {
                            takimDoc.reference.delete().addOnSuccessListener {
                                tamamlananTakim++
                                if (tamamlananTakim == tumTakimlar.size) {
                                    ligSilDevam(safeLigAdi, baseRef, onSuccess, onError)
                                }
                            }
                        }
                }
            }
            .addOnFailureListener { e -> onError(e) }
    }

    private fun ligSilDevam(
        trimmedLigAdi: String,
        baseRef: com.google.firebase.firestore.DocumentReference,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        baseRef.collection("macSonuclari").get()
            .addOnSuccessListener { macSnapshot ->
                for (macDoc in macSnapshot.documents) {
                    macDoc.reference.delete()
                }
                baseRef.delete()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e -> onError(e) }
            }
            .addOnFailureListener {
                baseRef.delete()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e -> onError(e) }
            }
    }

    // ─── MAÇ SONUÇLARI ────────────────────────────────────────────────────────

    /**
     * Maç sonucunu Firestore'a kaydeder. macId kullanarak üzerine yazar (set).
     * Eski "add" yerine "set" kullandığı için aynı maç birden fazla kez kaydedilmez.
     */
    fun macKaydet(
        ligAdi: String,
        macId: Int,
        takim1: String,
        takim2: String,
        hafta: Int,
        skor1: Int,
        skor2: Int,
        olaylar: List<Map<String, Any>> = emptyList(),
        oyuncuIstatistikleri: Map<String, Map<String, Int>> = emptyMap(),
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        val macData = mapOf(
            "macId" to macId,
            "takim1" to takim1,
            "takim2" to takim2,
            "skor1" to skor1,
            "skor2" to skor2,
            "hafta" to hafta,
            "isOynandi" to true,
            "olaylar" to olaylar,
            "oyuncuIstatistikleri" to oyuncuIstatistikleri
        )

        db.collection("ligler")
            .document(safeDoc(ligAdi))
            .collection("macSonuclari")
            .document("mac_$macId")
            .set(macData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e) }
    }

    /**
     * Ligin tüm maç sonuçlarını Firestore'dan çeker.
     * Uygulama yeniden açıldığında skorları geri yüklemek için kullanılır.
     */
    fun macSonuclariYukle(
        ligAdi: String,
        onResult: (List<MacSonucu>) -> Unit,
        onError: (Exception) -> Unit = {}
    ) {
        db.collection("ligler")
            .document(safeDoc(ligAdi))
            .collection("macSonuclari")
            .get()
            .addOnSuccessListener { snapshot ->
                val sonuclar = snapshot.documents.mapNotNull { doc ->
                    try {
                        val macId = (doc.getLong("macId") ?: 0L).toInt()
                        val takim1 = doc.getString("takim1") ?: return@mapNotNull null
                        val takim2 = doc.getString("takim2") ?: return@mapNotNull null
                        val skor1 = (doc.getLong("skor1") ?: 0L).toInt()
                        val skor2 = (doc.getLong("skor2") ?: 0L).toInt()
                        val hafta = (doc.getLong("hafta") ?: 0L).toInt()
                        val isOynandi = doc.getBoolean("isOynandi") ?: false

                        @Suppress("UNCHECKED_CAST")
                        val olaylar = (doc.get("olaylar") as? List<Map<String, Any>>) ?: emptyList()

                        @Suppress("UNCHECKED_CAST")
                        val oyuncuStatsRaw = (doc.get("oyuncuIstatistikleri") as? Map<String, Map<String, Long>>) ?: emptyMap()

                        MacSonucu(
                            macId = macId,
                            takim1 = takim1,
                            takim2 = takim2,
                            skor1 = skor1,
                            skor2 = skor2,
                            hafta = hafta,
                            isOynandi = isOynandi,
                            olaylar = olaylar,
                            oyuncuIstatistikleri = oyuncuStatsRaw
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                onResult(sonuclar)
            }
            .addOnFailureListener { e -> onError(e) }
    }

    // ─── TEMİZLİK ─────────────────────────────────────────────────────────────

    fun eskiOyuncuKoleksiyonunuSil(
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        db.collection("oyuncular").get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    onSuccess()
                    return@addOnSuccessListener
                }
                var tamamlanan = 0
                val toplam = snapshot.size()
                for (doc in snapshot.documents) {
                    doc.reference.delete().addOnSuccessListener {
                        tamamlanan++
                        if (tamamlanan == toplam) onSuccess()
                    }.addOnFailureListener { e -> onError(e) }
                }
            }
            .addOnFailureListener { e -> onError(e) }
    }

    // ─── OYUNCU PROFİLİ (KİMLİK & KARİYER) ───────────────────────────────────

    fun cihazIdAl(context: android.content.Context): String {
        val prefs = context.getSharedPreferences("SahamatikUserPrefs", android.content.Context.MODE_PRIVATE)
        var id = prefs.getString("cihaz_id", null)
        if (id == null) {
            id = java.util.UUID.randomUUID().toString()
            prefs.edit().putString("cihaz_id", id).apply()
        }
        return id
    }

    fun oyuncuProfiliKaydet(
        oyuncu: Oyuncu,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        val rawUsername = if (oyuncu.username.isNotEmpty()) oyuncu.username else oyuncu.isim
        val cleanUsername = safeDoc(rawUsername.removePrefix("@"))
        if (cleanUsername.isEmpty()) return

        val data = mapOf(
            "username" to cleanUsername,
            "isim" to oyuncu.isim,
            "mevki" to oyuncu.mevki,
            "takimAdi" to oyuncu.takimAdi,
            "formaNo" to oyuncu.formaNo,
            "profilFotoUri" to oyuncu.profilFotoUri,
            "toplamMac" to oyuncu.toplamMac,
            "toplamGol" to oyuncu.toplamGol,
            "toplamSari" to oyuncu.toplamSari,
            "toplamKirmizi" to oyuncu.toplamKirmizi,
            "katildigiLigler" to oyuncu.katildigiLigler,
            "katildigiMaclar" to oyuncu.katildigiMaclar
        )

        db.collection("oyuncuProfilleri")
            .document(cleanUsername)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e) }
    }

    fun oyuncuProfiliGetir(
        username: String,
        onResult: (Oyuncu?) -> Unit
    ) {
        val raw = username.removePrefix("@").trim()
        val cleanUsername = safeDoc(raw.lowercase())
        if (cleanUsername.isEmpty()) {
            onResult(null)
            return
        }

        fun mapDocToOyuncu(doc: com.google.firebase.firestore.DocumentSnapshot): Oyuncu {
            @Suppress("UNCHECKED_CAST")
            return Oyuncu(
                id = doc.id,
                username = doc.getString("username") ?: ("@" + doc.id),
                isim = doc.getString("isim") ?: doc.getString("adSoyad") ?: doc.id,
                mevki = doc.getString("mevki") ?: "Forvet",
                takimAdi = doc.getString("takimAdi") ?: "",
                formaNo = (doc.getLong("formaNo") ?: 0L).toInt(),
                profilFotoUri = doc.getString("profilFotoUri") ?: "",
                toplamMac = (doc.getLong("toplamMac") ?: 0L).toInt(),
                toplamGol = (doc.getLong("toplamGol") ?: doc.getLong("gol") ?: 0L).toInt(),
                toplamSari = (doc.getLong("toplamSari") ?: 0L).toInt(),
                toplamKirmizi = (doc.getLong("toplamKirmizi") ?: 0L).toInt(),
                katildigiLigler = (doc.get("katildigiLigler") as? List<String>) ?: emptyList(),
                katildigiMaclar = (doc.get("katildigiMaclar") as? List<String>) ?: emptyList()
            )
        }

        db.collection("oyuncuProfilleri")
            .document(cleanUsername)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    onResult(mapDocToOyuncu(doc))
                } else {
                    // Belge doğrudan lowercase ile bulunamadıysa isim veya username alanı ile tara
                    db.collection("oyuncuProfilleri")
                        .whereEqualTo("isim", raw)
                        .get()
                        .addOnSuccessListener { qSnap ->
                            val matchDoc = qSnap.documents.firstOrNull()
                            if (matchDoc != null) {
                                onResult(mapDocToOyuncu(matchDoc))
                            } else {
                                db.collection("oyuncuProfilleri")
                                    .whereEqualTo("username", "@$cleanUsername")
                                    .get()
                                    .addOnSuccessListener { qSnap2 ->
                                        val matchDoc2 = qSnap2.documents.firstOrNull()
                                        if (matchDoc2 != null) {
                                            onResult(mapDocToOyuncu(matchDoc2))
                                        } else {
                                            onResult(null)
                                        }
                                    }
                                    .addOnFailureListener { onResult(null) }
                            }
                        }
                        .addOnFailureListener { onResult(null) }
                }
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    fun oyuncuProfilFotoGuncelle(
        username: String,
        fotoUri: String,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        val cleanUsername = safeDoc(username.removePrefix("@").lowercase().trim())
        if (cleanUsername.isEmpty()) return

        db.collection("oyuncuProfilleri")
            .document(cleanUsername)
            .update("profilFotoUri", fotoUri)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener {
                db.collection("oyuncuProfilleri")
                    .document(cleanUsername)
                    .set(mapOf("profilFotoUri" to fotoUri), com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e -> onError(e) }
            }
    }

    fun oyuncuKariyerGuncelle(
        username: String,
        golArtis: Int,
        sariArtis: Int,
        kirmiziArtis: Int,
        macBilgisi: String = "",
        ligAdi: String = ""
    ) {
        val cleanUsername = safeDoc(username.removePrefix("@"))
        if (cleanUsername.isEmpty()) return

        val ref = db.collection("oyuncuProfilleri").document(cleanUsername)
        ref.get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val updates = mutableMapOf<String, Any>(
                    "toplamMac" to com.google.firebase.firestore.FieldValue.increment(1),
                    "toplamGol" to com.google.firebase.firestore.FieldValue.increment(golArtis.toLong()),
                    "toplamSari" to com.google.firebase.firestore.FieldValue.increment(sariArtis.toLong()),
                    "toplamKirmizi" to com.google.firebase.firestore.FieldValue.increment(kirmiziArtis.toLong())
                )
                if (macBilgisi.isNotEmpty()) {
                    updates["katildigiMaclar"] = com.google.firebase.firestore.FieldValue.arrayUnion(macBilgisi)
                }
                if (ligAdi.isNotEmpty()) {
                    updates["katildigiLigler"] = com.google.firebase.firestore.FieldValue.arrayUnion(ligAdi)
                }
                ref.update(updates)
            }
        }
    }

    /**
     * Mükerrer artışları ve katlanmayı önleyen DELTA kariyer güncellemesi.
     * SADECE yeni maç ise toplamMac artırılır; maça sonradan girip değişiklik yapıldığında toplamMac asla artmaz!
     * Gol ve kartlar da sadece fark (delta = yeni - eski) kadar yansıtılır.
     * formatTipi == "TEKIL_MAC" ise ASLA ligler listesine ekleme yapmaz, sadece maç skorunu maçlar listesine ekler.
     */
    fun oyuncuKariyerDeltaGuncelle(
        username: String,
        yeniMacMi: Boolean,
        golFarki: Int,
        sariFarki: Int,
        kirmiziFarki: Int,
        macBilgisi: String = "",
        organizasyonAdi: String = "",
        formatTipi: String = ""
    ) {
        val cleanUsername = safeDoc(username.removePrefix("@").lowercase().trim())
        if (cleanUsername.isEmpty()) return

        val ref = db.collection("oyuncuProfilleri").document(cleanUsername)
        ref.get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val updates = mutableMapOf<String, Any>()
                if (yeniMacMi) {
                    updates["toplamMac"] = com.google.firebase.firestore.FieldValue.increment(1)
                }
                if (golFarki != 0) {
                    updates["toplamGol"] = com.google.firebase.firestore.FieldValue.increment(golFarki.toLong())
                }
                if (sariFarki != 0) {
                    updates["toplamSari"] = com.google.firebase.firestore.FieldValue.increment(sariFarki.toLong())
                }
                if (kirmiziFarki != 0) {
                    updates["toplamKirmizi"] = com.google.firebase.firestore.FieldValue.increment(kirmiziFarki.toLong())
                }

                // Lig vs Tekil Maç Ayrımı
                if (formatTipi == "TEKIL_MAC") {
                    // TEKİL MAÇ: Asla katildigiLigler'e eklenmez!
                    if (organizasyonAdi.isNotEmpty()) {
                        updates["katildigiLigler"] = com.google.firebase.firestore.FieldValue.arrayRemove(organizasyonAdi)
                    }

                    // Birleşik tek satır formatı: "Sınır (Bilgisayar 0 - 1 Endüstri)"
                    val birlesikFormat = if (organizasyonAdi.isNotEmpty() && macBilgisi.isNotEmpty()) {
                        "$organizasyonAdi ($macBilgisi)"
                    } else if (macBilgisi.isNotEmpty()) {
                        macBilgisi
                    } else {
                        organizasyonAdi
                    }

                    @Suppress("UNCHECKED_CAST")
                    val mevcutMaclar = (doc.get("katildigiMaclar") as? List<String>)?.toMutableList() ?: mutableListOf()

                    // Bu maça ait eski parçalı veya eski skorlu kayıtları temizle (örn: "Sınır", "0 - 0", vb.)
                    val temizListe = mevcutMaclar.filter { m ->
                        val ayniMacMi = (organizasyonAdi.isNotEmpty() && (m == organizasyonAdi || m.startsWith("$organizasyonAdi ("))) ||
                                        (macBilgisi.isNotEmpty() && (m == macBilgisi || (m.contains("-") && organizasyonAdi.isNotEmpty() && m.startsWith("$organizasyonAdi"))))
                        !ayniMacMi
                    }.toMutableList()

                    if (birlesikFormat.isNotEmpty()) {
                        temizListe.add(birlesikFormat)
                    }

                    updates["katildigiMaclar"] = temizListe.distinct()
                } else {
                    // KLASİK LİG VEYA GRUP TURNUVASI: katildigiLigler'e eklenir
                    if (organizasyonAdi.isNotEmpty()) {
                        updates["katildigiLigler"] = com.google.firebase.firestore.FieldValue.arrayUnion(organizasyonAdi)
                    }
                }

                if (updates.isNotEmpty()) {
                    ref.update(updates)
                }
            }
        }
    }

    /**
     * Hatalı veya katlanmış oyuncu profil istatistiklerini doğrudan net değerlere eşitleme/düzeltme fonksiyonu.
     */
    fun oyuncuProfiliniDuzelt(
        username: String,
        netMac: Int? = null,
        netGol: Int? = null,
        netSari: Int? = null,
        netKirmizi: Int? = null,
        onSuccess: () -> Unit = {}
    ) {
        val cleanUsername = safeDoc(username.removePrefix("@").lowercase().trim())
        if (cleanUsername.isEmpty()) return
        val updates = mutableMapOf<String, Any>()
        netMac?.let { updates["toplamMac"] = it }
        netGol?.let { updates["toplamGol"] = it }
        netSari?.let { updates["toplamSari"] = it }
        netKirmizi?.let { updates["toplamKirmizi"] = it }
        if (updates.isNotEmpty()) {
            db.collection("oyuncuProfilleri").document(cleanUsername).update(updates).addOnSuccessListener {
                onSuccess()
            }
        }
    }

    // =========================================================================================
    // 🚨 [TEK SEFERLİK VERİTABANI TEMİZLEME KODU - İSTEDİĞİNİZ ZAMAN SİLEBİLİRSİNİZ]
    // ℹ️ Bu fonksiyon, önceki testlerden kalan tüm ligleri, maç sonuçlarını ve test kullanıcılarını siler.
    // 🛡️ 20 adet sahte (mock) oyuncu KORUNUR ve sıfırdan yeniden yüklenir.
    // 🗑️ DİLEDİĞİNİZ ZAMAN BU FONKSİYONU VE MAINACTIVITY'DEKİ ÇAĞRISINI TAMAMEN SİLEBİLİRSİNİZ.
    fun veritabaniniTemizleVeSifirla(context: android.content.Context, onCompleted: () -> Unit = {}) {

        db.collection("ligler").get().addOnSuccessListener { ligSnapshot ->
            val ligCount = ligSnapshot.size()
            var silinenLigler = 0

            val bitirVeOyuncularaGec = {
                // 1. Oyuncu profillerinden sahte (mock) olmayanları sil
                db.collection("oyuncuProfilleri").get().addOnSuccessListener { profSnapshot ->
                    val batch = db.batch()
                    for (doc in profSnapshot.documents) {
                        val username = doc.id.removePrefix("@").lowercase().trim()
                        if (!com.example.sahamatik_lig.util.MockVeriYukleyici.sahteUsernameler.contains(
                                username
                            )
                        ) {
                            batch.delete(doc.reference)
                        }
                    }
                    batch.commit().addOnCompleteListener {
                        // 2. Users koleksiyonundan sahte olmayanları sil
                        db.collection("users").get().addOnSuccessListener { usersSnapshot ->
                            val userBatch = db.batch()
                            for (doc in usersSnapshot.documents) {
                                val username = doc.id.removePrefix("@").lowercase().trim()
                                if (!com.example.sahamatik_lig.util.MockVeriYukleyici.sahteUsernameler.contains(
                                        username
                                    )
                                ) {
                                    userBatch.delete(doc.reference)
                                }
                            }
                            userBatch.commit().addOnCompleteListener {
                                // 3. 20 Sahte oyuncuyu temiz ve taze olarak yeniden yükle
                                com.example.sahamatik_lig.util.MockVeriYukleyici.sahteKullanicilariYukle {
                                    // 4. Cihazdaki yerel ayrılınan ligler cache'ini sıfırla
                                    val prefs = context.getSharedPreferences(
                                        "SahaMatikPrefs",
                                        android.content.Context.MODE_PRIVATE
                                    )
                                    prefs.edit().remove("ayrilinan_ligler").apply()
                                    onCompleted()
                                }
                            }
                        }.addOnFailureListener { onCompleted() }
                    }
                }.addOnFailureListener { onCompleted() }
            }

            if (ligCount == 0) {
                bitirVeOyuncularaGec()
                return@addOnSuccessListener
            }

            for (ligDoc in ligSnapshot.documents) {
                // Takımlar ve maç sonuçları alt koleksiyonlarını sil
                ligDoc.reference.collection("takimlar").get()
                    .addOnSuccessListener { takimSnapshot ->
                        for (takimDoc in takimSnapshot.documents) {
                            takimDoc.reference.collection("oyuncular").get()
                                .addOnSuccessListener { oSnapshot ->
                                    for (oDoc in oSnapshot.documents) oDoc.reference.delete()
                                    takimDoc.reference.delete()
                                }
                        }
                    }
                ligDoc.reference.collection("macSonuclari").get()
                    .addOnSuccessListener { macSonucSnapshot ->
                        for (mDoc in macSonucSnapshot.documents) mDoc.reference.delete()
                    }
                ligDoc.reference.delete().addOnCompleteListener {
                    silinenLigler++
                    if (silinenLigler >= ligCount) {
                        bitirVeOyuncularaGec()
                    }
                }
            }
        }.addOnFailureListener {
            onCompleted()
        }
    }
        // =========================================================================================

        // ─── YEREL KULLANICI PROFİLİ (CİHAZ HAFIZASI) ───────────────────────────

        fun yerelProfilVarMi(context: android.content.Context): Boolean {
            val prefs = context.getSharedPreferences(
                "SahamatikUserPrefs",
                android.content.Context.MODE_PRIVATE
            )
            return prefs.getBoolean("kullanici_profil_var_mi", false)
        }

        fun yerelProfilKaydet(
            context: android.content.Context,
            username: String,
            isim: String,
            mevki: String,
            formaNo: Int,
            profilFotoUri: String = ""
        ) {
            val prefs = context.getSharedPreferences(
                "SahamatikUserPrefs",
                android.content.Context.MODE_PRIVATE
            )
            prefs.edit()
                .putBoolean("kullanici_profil_var_mi", true)
                .putString("profil_username", username)
                .putString("profil_isim", isim)
                .putString("profil_mevki", mevki)
                .putInt("profil_forma_no", formaNo)
                .putString("profil_foto_uri", profilFotoUri)
                .apply()
        }

        fun yerelProfilGetir(context: android.content.Context): Oyuncu? {
            val prefs = context.getSharedPreferences(
                "SahamatikUserPrefs",
                android.content.Context.MODE_PRIVATE
            )
            if (!prefs.getBoolean("kullanici_profil_var_mi", false)) return null
            return Oyuncu(
                username = prefs.getString("profil_username", "") ?: "",
                isim = prefs.getString("profil_isim", "") ?: "",
                mevki = prefs.getString("profil_mevki", "Forvet") ?: "Forvet",
                formaNo = prefs.getInt("profil_forma_no", 10),
                profilFotoUri = prefs.getString("profil_foto_uri", "") ?: ""
            )
        }

        // ─── BENZERSİZ USERNAME KONTROLÜ & ÖNERİ ÜRETİCİ ─────────────────────────

        fun usernameMusaitMi(username: String, onResult: (Boolean) -> Unit) {
            val clean = safeDoc(username.removePrefix("@").lowercase().trim())
            if (clean.isEmpty()) {
                onResult(false)
                return
            }

            db.collection("oyuncuProfilleri").document(clean).get()
                .addOnSuccessListener { doc ->
                    onResult(!doc.exists())
                }
                .addOnFailureListener {
                    onResult(true)
                }
        }

        fun usernameOnerileriUret(baseUsername: String, onResult: (List<String>) -> Unit) {
            val clean = safeDoc(baseUsername.removePrefix("@").lowercase().trim())
            if (clean.isEmpty()) {
                onResult(emptyList())
                return
            }

            val adaylar = listOf(
                "@${clean}_10",
                "@${clean}_fc",
                "@${clean}_7",
                "@${clean}_9",
                "@${clean}_halisaha"
            )

            val uygunOneriler = mutableListOf<String>()
            var kontrolSayisi = 0

            for (aday in adaylar) {
                val adayClean = safeDoc(aday.removePrefix("@"))
                db.collection("oyuncuProfilleri").document(adayClean).get()
                    .addOnSuccessListener { doc ->
                        if (!doc.exists() && uygunOneriler.size < 3) {
                            uygunOneriler.add(aday)
                        }
                        kontrolSayisi++
                        if (kontrolSayisi == adaylar.size) {
                            onResult(uygunOneriler.take(3))
                        }
                    }
                    .addOnFailureListener {
                        kontrolSayisi++
                        if (kontrolSayisi == adaylar.size) {
                            onResult(uygunOneriler.take(3))
                        }
                    }
            }
        }

        // ─── INSTAGRAM TARZI CANLI OYUNCU ARAMA ──────────────────────────────────

        fun oyuncuAra(query: String, onResult: (List<Oyuncu>) -> Unit) {
            val q = query.removePrefix("@").lowercase().trim()

            db.collection("oyuncuProfilleri").get()
                .addOnSuccessListener { snapshot ->
                    val sonuclar = snapshot.documents.mapNotNull { doc ->
                        try {
                            val username = doc.getString("username") ?: ("@" + doc.id)
                            val isim = doc.getString("isim") ?: doc.getString("adSoyad") ?: doc.id
                            val mevki = doc.getString("mevki") ?: "Forvet"
                            val formaNo = (doc.getLong("formaNo") ?: 10L).toInt()
                            val profilFotoUri = doc.getString("profilFotoUri") ?: ""
                            val toplamMac = (doc.getLong("toplamMac") ?: 0L).toInt()
                            val gol = (doc.getLong("gol") ?: doc.getLong("toplamGol") ?: 0L).toInt()
                            val toplamSari = (doc.getLong("toplamSari") ?: 0L).toInt()
                            val toplamKirmizi = (doc.getLong("toplamKirmizi") ?: 0L).toInt()

                            Oyuncu(
                                id = doc.id,
                                username = if (username.startsWith("@")) username else "@$username",
                                isim = isim,
                                mevki = mevki,
                                formaNo = formaNo,
                                profilFotoUri = profilFotoUri,
                                toplamMac = toplamMac,
                                gol = gol,
                                toplamGol = gol,
                                toplamSari = toplamSari,
                                toplamKirmizi = toplamKirmizi
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }.filter { oyuncu ->
                        if (q.isEmpty()) true
                        else oyuncu.username.lowercase().contains(q) || oyuncu.isim.lowercase()
                            .contains(q)
                    }
                    onResult(sonuclar)
                }
                .addOnFailureListener {
                    onResult(emptyList())
                }
        }

        // ─── ESKİ DÜZ VE SAHTE İSİMLERİ TEMİZLEME ────────────────────────────────

        fun eskiDuzIsimleriTemizle(onTamamlandi: () -> Unit = {}) {
            db.collection("ligler").get().addOnSuccessListener { liglerSnapshot ->
                for (ligDoc in liglerSnapshot.documents) {
                    ligDoc.reference.collection("takimlar").get()
                        .addOnSuccessListener { takimlarSnapshot ->
                            for (takimDoc in takimlarSnapshot.documents) {
                                takimDoc.reference.collection("oyuncular").get()
                                    .addOnSuccessListener { oyuncularSnapshot ->
                                        for (oyuncuDoc in oyuncularSnapshot.documents) {
                                            val username = oyuncuDoc.getString("username") ?: ""
                                            val isim = oyuncuDoc.getString("isim") ?: ""
                                            if (username.isEmpty() || !username.startsWith("@") ||
                                                username.contains("ortasaha", ignoreCase = true) ||
                                                username.contains("defans", ignoreCase = true) ||
                                                username.contains("forvet", ignoreCase = true) ||
                                                (username.contains(
                                                    "kaleci",
                                                    ignoreCase = true
                                                ) && isim.matches(Regex(".*\\d+.*")))
                                            ) {
                                                oyuncuDoc.reference.delete()
                                            }
                                        }
                                    }
                            }
                        }
                }
                onTamamlandi()
            }.addOnFailureListener { onTamamlandi() }
        }

        // ─── KONTENJAN KONTROLLÜ TAKIMA OYUNCU EKLEME ─────────────────────────────

        fun takimaOyuncuEkleKontrollu(
            ligAdi: String,
            takimAdi: String,
            oyuncu: Oyuncu,
            maxKontenjan: Int = 10,
            onSuccess: () -> Unit,
            onLimitDolu: () -> Unit,
            onError: (Exception) -> Unit
        ) {
            val safeLig = safeDoc(ligAdi)
            val safeTakim = safeDoc(takimAdi)

            val oyuncularRef = db.collection("ligler")
                .document(safeLig)
                .collection("takimlar")
                .document(safeTakim)
                .collection("oyuncular")

            oyuncularRef.get().addOnSuccessListener { snapshot ->
                if (snapshot.size() >= maxKontenjan) {
                    onLimitDolu()
                    return@addOnSuccessListener
                }

                val zatenVar = snapshot.documents.any {
                    val u = it.getString("username") ?: ""
                    u.equals(oyuncu.username, ignoreCase = true)
                }
                if (zatenVar) {
                    onError(Exception("Bu oyuncu zaten takım kadrosunda mevcut!"))
                    return@addOnSuccessListener
                }

                val yeniDoc = oyuncularRef.document()
                val eklenecek = oyuncu.copy(
                    id = yeniDoc.id,
                    ligAdi = ligAdi,
                    takimAdi = takimAdi,
                    gol = 0,
                    sari = 0,
                    kirmizi = 0
                )
                yeniDoc.set(eklenecek)
                    .addOnSuccessListener {
                        // Oyuncunun kalıcı kariyer profiline de ekle (Lig veya Tekil Maç)
                        db.collection("ligler").document(safeLig).get()
                            .addOnSuccessListener { lDoc ->
                                val format = lDoc.getString("formatTipi") ?: "KLASIK"
                                oyuncuyaOrganizasyonEkle(oyuncu.username, ligAdi, format)
                            }
                        onSuccess()
                    }
                    .addOnFailureListener { e -> onError(e) }
            }.addOnFailureListener { e -> onError(e) }
        }

        // ─── DAVET LİNKİ PAYLAŞMA YARDIMCISI ─────────────────────────────────────

        fun davetLinkiPaylas(context: android.content.Context, ligAdi: String, takimAdi: String) {
            val link = "https://sahamatik.app/katil?lig=${
                java.net.URLEncoder.encode(
                    ligAdi,
                    "UTF-8"
                )
            }&takim=${java.net.URLEncoder.encode(takimAdi, "UTF-8")}"
            val shareMetin =
                "⚽ SahaMatik Halı Saha Daveti!\n'$takimAdi' takımında yerin hazır. Kadroya katılmak için hemen tıkla:\n$link"

            val sendIntent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                putExtra(android.content.Intent.EXTRA_TEXT, shareMetin)
                type = "text/plain"
            }
            val shareIntent =
                android.content.Intent.createChooser(sendIntent, "Davet Linkini Paylaş")
            context.startActivity(shareIntent)
        }

        // ─── OYUNCUYA KALICI LİG VEYA TEKİL MAÇ GEÇMİŞİ EKLEME ──────────────────

        fun oyuncuyaOrganizasyonEkle(
            username: String,
            organizasyonAdi: String,
            formatTipi: String
        ) {
            val clean = safeDoc(username.removePrefix("@").lowercase().trim())
            if (clean.isEmpty() || organizasyonAdi.isEmpty()) return

            val docRef = db.collection("oyuncuProfilleri").document(clean)
            val alanAdi = if (formatTipi == "TEKIL_MAC") "katildigiMaclar" else "katildigiLigler"

            docRef.update(
                alanAdi,
                com.google.firebase.firestore.FieldValue.arrayUnion(organizasyonAdi)
            )
                .addOnFailureListener {
                    docRef.set(
                        mapOf(alanAdi to listOf(organizasyonAdi)),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
                }
        }

        // ─── LİGDEN / MAÇTAN AYRILMA (KATILIMCI OYUNCU İÇİN - MAÇI SİLMEZ) ───────

        fun ayrilinanLigiKaydet(context: android.content.Context, ligAdi: String) {
            val prefs =
                context.getSharedPreferences("SahaMatikPrefs", android.content.Context.MODE_PRIVATE)
            val set = prefs.getStringSet("ayrilinan_ligler", mutableSetOf())?.toMutableSet()
                ?: mutableSetOf()
            set.add(ligAdi)
            prefs.edit().putStringSet("ayrilinan_ligler", set).apply()
        }

        fun ayrilinanLigMi(context: android.content.Context, ligAdi: String): Boolean {
            val prefs =
                context.getSharedPreferences("SahaMatikPrefs", android.content.Context.MODE_PRIVATE)
            val set = prefs.getStringSet("ayrilinan_ligler", emptySet()) ?: emptySet()
            return set.contains(ligAdi)
        }

        fun ligdenAyril(
            context: android.content.Context,
            ligAdi: String,
            username: String,
            onSuccess: () -> Unit,
            onError: (Exception) -> Unit
        ) {
            ayrilinanLigiKaydet(context, ligAdi)
            val cleanUsername = if (username.startsWith("@")) username else "@$username"
            val safeLig = safeDoc(ligAdi)

            // İlgili ligin altındaki tüm takımlarda bu oyuncuyu bulup sil (kontenjanı boşaltır)
            db.collection("ligler").document(safeLig).collection("takimlar").get()
                .addOnSuccessListener { takimlarSnapshot ->
                    for (takimDoc in takimlarSnapshot.documents) {
                        takimDoc.reference.collection("oyuncular").get()
                            .addOnSuccessListener { oyuncularSnapshot ->
                                for (oDoc in oyuncularSnapshot.documents) {
                                    val u = oDoc.getString("username") ?: ""
                                    if (u.equals(cleanUsername, ignoreCase = true) || u.equals(
                                            username,
                                            ignoreCase = true
                                        )
                                    ) {
                                        oDoc.reference.delete()
                                    }
                                }
                            }
                    }
                    onSuccess()
                }
                .addOnFailureListener { e -> onError(e) }
        }
    }

