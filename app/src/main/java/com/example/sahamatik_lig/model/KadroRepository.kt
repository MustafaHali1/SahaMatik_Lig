package com.example.sahamatik_lig.model

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

object KadroRepository {

    private val db by lazy { FirebaseFirestore.getInstance() }

    // ─── TAKIM ────────────────────────────────────────────────────────────────

    fun takimOlustur(ligAdi: String, takimAdi: String, onSuccess: () -> Unit = {}, onError: (Exception) -> Unit = {}) {
        val takimRef = db.collection("ligler")
            .document(ligAdi.trim())
            .collection("takimlar")
            .document(takimAdi.trim())

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
        db.collection("ligler").document(ligAdi.trim())
            .collection("takimlar").document(takimAdi.trim())
            .collection("oyuncular").get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    doc.reference.delete()
                }
                db.collection("ligler").document(ligAdi.trim())
                    .collection("takimlar").document(takimAdi.trim())
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
            .document(ligAdi.trim())
            .collection("takimlar")
            .document(takimAdi.trim())

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
            .document(oyuncu.ligAdi.trim())
            .collection("takimlar")
            .document(oyuncu.takimAdi.trim())
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
            .document(ligAdi.trim())
            .collection("takimlar")
            .document(takimAdi.trim())
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
            .document(ligAdi.trim())
            .collection("takimlar")
            .document(takimAdi.trim())
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
            .document(ligAdi.trim())
            .collection("takimlar")
            .document(takimAdi.trim())
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
            .document(ligAdi.trim())
            .collection("takimlar")
            .document(takimAdi.trim())
            .collection("oyuncular")
            .get()
            .addOnSuccessListener { snapshot ->
                val liste = snapshot.documents.mapNotNull { it.toObject(Oyuncu::class.java) }
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
            "olusturulmaTarihi" to lig.olusturulmaTarihi
        )
        db.collection("ligler").document(lig.name.trim()).set(ligData)
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
                                olusturulmaTarihi = olusturulmaTarihi
                            )
                        } catch (e: Exception) {
                            null // parse hatası varsa bu ligi atla
                        }
                    }
                    onUpdate(liste)
                }
            }
    }

    fun ligSil(
        ligAdi: String,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        val trimmedLigAdi = ligAdi.trim()
        val baseRef = db.collection("ligler").document(trimmedLigAdi)

        baseRef.collection("takimlar").get()
            .addOnSuccessListener { takimlarSnapshot ->
                val tumTakimlar = takimlarSnapshot.documents

                if (tumTakimlar.isEmpty()) {
                    ligSilDevam(trimmedLigAdi, baseRef, onSuccess, onError)
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
                                    ligSilDevam(trimmedLigAdi, baseRef, onSuccess, onError)
                                }
                            }
                        }
                        .addOnFailureListener {
                            takimDoc.reference.delete().addOnSuccessListener {
                                tamamlananTakim++
                                if (tamamlananTakim == tumTakimlar.size) {
                                    ligSilDevam(trimmedLigAdi, baseRef, onSuccess, onError)
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
            "olaylar" to olaylar
        )

        db.collection("ligler")
            .document(ligAdi.trim())
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
            .document(ligAdi.trim())
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

                        MacSonucu(
                            macId = macId,
                            takim1 = takim1,
                            takim2 = takim2,
                            skor1 = skor1,
                            skor2 = skor2,
                            hafta = hafta,
                            isOynandi = isOynandi,
                            olaylar = olaylar
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
}