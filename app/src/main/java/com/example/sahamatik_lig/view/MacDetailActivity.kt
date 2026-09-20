package com.example.sahamatik_lig.view

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sahamatik_lig.R
import com.example.sahamatik_lig.databinding.ActivityMacDetailBinding
import com.example.sahamatik_lig.databinding.BottomSheetOyuncuOlayBinding
import com.example.sahamatik_lig.databinding.ItemMacOlayiBinding
import com.example.sahamatik_lig.databinding.ItemSahaOyuncuBinding
import com.example.sahamatik_lig.databinding.ItemYedekOyuncuBinding
import com.example.sahamatik_lig.model.KadroRepository
import com.example.sahamatik_lig.model.Oyuncu
import com.example.sahamatik_lig.util.ImagePickerHelper
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * MacDetailActivity — Sofascore / Halı Saha Profesyonel Maç Yönetim Ekranı.
 *
 * Kurallar:
 *  1. Yedek kulübesindeki oyuncu gol atamaz (Gol butonu sadece sahadakilerde görünür).
 *  2. Kaleci değişikliği kuralı: Kaleci yerine öncelikle yedek kaleci girer.
 *  3. 2. Sarı Kart = Kırmızı Kart kuralı (Otomatik ihraç).
 *  4. Kırmızı kartlı oyuncuya tekrar gol veya kart eklenemez.
 *  5. Skor sınırı: Hatalı girişleri önleyen tavan skor (30 gol).
 *  6. VAR / Geri Alma: Olaylar şeridindeki bir olaya tıklanarak gol veya kart iptal edilebilir, skor düşer.
 */
class MacDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMacDetailBinding

    // Maç Bilgileri
    private var evTakim: String = ""
    private var depTakim: String = ""
    private var ligAdi: String = ""
    private var macId: Int = -1
    private var hafta: Int = 1

    // Skorlar (Max 30 gol tavan sınırı)
    private var evSkor: Int = 0
    private var depSkor: Int = 0
    private val MAX_SKOR_LIMIT = 30

    // Kadrolar (Sahadaki 7 kişi + Kulübedeki yedekler)
    private val evSahadakiler = mutableListOf<Oyuncu>()
    private val evYedekler = mutableListOf<Oyuncu>()

    private val depSahadakiler = mutableListOf<Oyuncu>()
    private val depYedekler = mutableListOf<Oyuncu>()

    // Hangi takımın sahası görüntüleniyor (true: Ev Sahibi, false: Deplasman)
    private var isEvSecili: Boolean = true

    // Maç Olayları
    private val macOlaylari = mutableListOf<MacOlayi>()

    data class MacOlayi(
        val dakika: Int,
        val tur: String,          // "GOL", "SARI", "KIRMIZI", "DEGISIKLIK"
        val takimAdi: String,
        val oyuncuAdi: String,
        val oyuncuId: String = ""
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMacDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Intent verilerini al
        evTakim  = intent.getStringExtra("EV_TAKIM")  ?: ""
        depTakim = intent.getStringExtra("DEP_TAKIM") ?: ""
        ligAdi   = intent.getStringExtra("LIG_ADI")   ?: ""
        macId    = intent.getIntExtra("MAC_ID", -1)
        hafta    = intent.getIntExtra("HAFTA", 1)
        evSkor   = intent.getIntExtra("EV_SKOR", 0)
        depSkor  = intent.getIntExtra("DEP_SKOR", 0)

        // Başlıklar ve Logolar
        binding.tvEvAdi.text = evTakim
        binding.tvDepAdi.text = depTakim
        binding.tvHaftaLabel.text = "$hafta. Hafta • Maç Detayı"
        binding.btnTabEv.text = "🏠 $evTakim"
        binding.btnTabDep.text = "⚽ $depTakim"

        ImagePickerHelper.uriGetir(this, "logo_${ligAdi}_$evTakim")?.let { binding.ivEvLogo.setImageURI(it) }
        ImagePickerHelper.uriGetir(this, "logo_${ligAdi}_$depTakim")?.let { binding.ivDepLogo.setImageURI(it) }

        skorGuncelle()

        // Olaylar RecyclerView
        binding.rvOlaylar.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Takım Sekme Butonları
        binding.btnTabEv.setOnClickListener {
            if (!isEvSecili) {
                isEvSecili = true
                takimSekmesiGuncelle()
            }
        }

        binding.btnTabDep.setOnClickListener {
            if (isEvSecili) {
                isEvSecili = false
                takimSekmesiGuncelle()
            }
        }

        // Geri Tuşları
        binding.tvBack.setOnClickListener { maciBitirVeKaydet() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                maciBitirVeKaydet()
            }
        })

        // Kadroları Firestore'dan Çek
        kadrolariYukle()
    }

    // ─── SKOR ─────────────────────────────────────────────────────────────────

    private fun skorGuncelle() {
        binding.tvEvSkor.text = evSkor.toString()
        binding.tvDepSkor.text = depSkor.toString()
    }

    // ─── KADROLAR VE 7 KİŞİLİK HALI SAHA AYRIMI ───────────────────────────────

    private fun kadrolariYukle() {
        KadroRepository.takimOyunculariniGetir(ligAdi, evTakim) { evTum ->
            ayristirKadro(evTum, evSahadakiler, evYedekler)

            KadroRepository.takimOyunculariniGetir(ligAdi, depTakim) { depTum ->
                ayristirKadro(depTum, depSahadakiler, depYedekler)

                runOnUiThread {
                    takimSekmesiGuncelle()
                }
            }
        }
    }

    private fun ayristirKadro(
        tumOyuncular: List<Oyuncu>,
        sahadakiler: MutableList<Oyuncu>,
        yedekler: MutableList<Oyuncu>
    ) {
        sahadakiler.clear()
        yedekler.clear()

        if (tumOyuncular.isEmpty()) return

        val kaleciler = tumOyuncular.filter { it.mevki.trim().equals("Kaleci", ignoreCase = true) }.toMutableList()
        val defanslar = tumOyuncular.filter { it.mevki.trim().equals("Defans", ignoreCase = true) }.toMutableList()
        val ortasahalar = tumOyuncular.filter { it.mevki.trim().equals("Orta Saha", ignoreCase = true) }.toMutableList()
        val forvetler = tumOyuncular.filter { it.mevki.trim().equals("Forvet", ignoreCase = true) }.toMutableList()

        // 1 Kaleci
        if (kaleciler.isNotEmpty()) sahadakiler.add(kaleciler.removeAt(0))

        // 2 Defans
        repeat(2) { if (defanslar.isNotEmpty()) sahadakiler.add(defanslar.removeAt(0)) }

        // 2 Orta Saha
        repeat(2) { if (ortasahalar.isNotEmpty()) sahadakiler.add(ortasahalar.removeAt(0)) }

        // 2 Forvet
        repeat(2) { if (forvetler.isNotEmpty()) sahadakiler.add(forvetler.removeAt(0)) }

        // 7 kişi dolana kadar kalanları ekle
        val kalanHavuz = (kaleciler + defanslar + ortasahalar + forvetler).toMutableList()
        while (sahadakiler.size < 7 && kalanHavuz.isNotEmpty()) {
            sahadakiler.add(kalanHavuz.removeAt(0))
        }

        // 7'den sonrakiler kulübeye
        yedekler.addAll(kalanHavuz)
    }

    // ─── SEKME VE SAHA ÇİZİMİ ─────────────────────────────────────────────────

    private fun takimSekmesiGuncelle() {
        val aktifTakimAdi = if (isEvSecili) evTakim else depTakim
        val sahadakiler = if (isEvSecili) evSahadakiler else depSahadakiler
        val yedekler = if (isEvSecili) evYedekler else depYedekler

        if (isEvSecili) {
            binding.btnTabEv.setBackgroundResource(R.drawable.bg_lig_kart)
            binding.btnTabEv.setTextColor(Color.parseColor("#0F3D2E"))
            binding.btnTabDep.setBackgroundColor(Color.TRANSPARENT)
            binding.btnTabDep.setTextColor(Color.parseColor("#8CAA9A"))
        } else {
            binding.btnTabDep.setBackgroundResource(R.drawable.bg_lig_kart)
            binding.btnTabDep.setTextColor(Color.parseColor("#0F3D2E"))
            binding.btnTabEv.setBackgroundColor(Color.TRANSPARENT)
            binding.btnTabEv.setTextColor(Color.parseColor("#8CAA9A"))
        }

        binding.tvSeciliTakimBaslik.text = "$aktifTakimAdi • 7 Kişilik Kadro"

        sahayiCiz(sahadakiler, isEvSahibi = isEvSecili)
        kulubeyiCiz(yedekler, isEvSahibi = isEvSecili)
    }

    private fun sahayiCiz(oyuncular: List<Oyuncu>, isEvSahibi: Boolean) {
        binding.rowForvet.removeAllViews()
        binding.rowOrtaSaha.removeAllViews()
        binding.rowDefans.removeAllViews()
        binding.rowKaleci.removeAllViews()

        if (oyuncular.isEmpty()) {
            binding.tvSahaBosUyari.visibility = View.VISIBLE
            return
        }
        binding.tvSahaBosUyari.visibility = View.GONE

        val kaleciler = mutableListOf<Oyuncu>()
        val defanslar = mutableListOf<Oyuncu>()
        val ortasahalar = mutableListOf<Oyuncu>()
        val forvetler = mutableListOf<Oyuncu>()

        for (oyuncu in oyuncular) {
            when (oyuncu.mevki.trim()) {
                "Kaleci" -> kaleciler.add(oyuncu)
                "Defans" -> defanslar.add(oyuncu)
                "Orta Saha" -> ortasahalar.add(oyuncu)
                "Forvet" -> forvetler.add(oyuncu)
                else -> ortasahalar.add(oyuncu)
            }
        }

        var formaSayac = 1
        for (o in forvetler) ekleSahaOyuncuView(binding.rowForvet, o, formaSayac++, isEvSahibi, isSahada = true)
        for (o in ortasahalar) ekleSahaOyuncuView(binding.rowOrtaSaha, o, formaSayac++, isEvSahibi, isSahada = true)
        for (o in defanslar) ekleSahaOyuncuView(binding.rowDefans, o, formaSayac++, isEvSahibi, isSahada = true)
        for (o in kaleciler) ekleSahaOyuncuView(binding.rowKaleci, o, 1, isEvSahibi, isSahada = true)

        binding.tvDizilisBilgi.text = "${kaleciler.size}-${defanslar.size}-${ortasahalar.size}-${forvetler.size}"
    }

    private fun ekleSahaOyuncuView(
        parentRow: LinearLayout,
        oyuncu: Oyuncu,
        varsayilanNo: Int,
        isEvSahibi: Boolean,
        isSahada: Boolean
    ) {
        val itemBinding = ItemSahaOyuncuBinding.inflate(layoutInflater, parentRow, false)

        val formaNo = if (oyuncu.formaNo > 0) oyuncu.formaNo.toString() else varsayilanNo.toString()
        itemBinding.tvFormaNo.text = formaNo

        // Kırmızı kartlıysa koyu gri forma rengi
        val formaRenk = if (oyuncu.kirmizi > 0) {
            "#4A4A4A"
        } else if (isEvSahibi) {
            "#2E7D4F"
        } else {
            "#C0392B"
        }
        itemBinding.tvFormaNo.setBackgroundColor(Color.parseColor(formaRenk))

        itemBinding.tvOyuncuAdi.text = oyuncu.isim
        itemBinding.tvOyuncuMevki.text = oyuncu.mevki.take(3).uppercase()

        // Gol rozeti
        if (oyuncu.gol > 0) {
            itemBinding.tvRozetGol.visibility = View.VISIBLE
            itemBinding.tvRozetGol.text = "⚽"
        } else {
            itemBinding.tvRozetGol.visibility = View.GONE
        }

        // Kart rozeti
        if (oyuncu.kirmizi > 0) {
            itemBinding.tvRozetKart.visibility = View.VISIBLE
            itemBinding.tvRozetKart.text = "🟥"
        } else if (oyuncu.sari > 0) {
            itemBinding.tvRozetKart.visibility = View.VISIBLE
            itemBinding.tvRozetKart.text = "🟨"
        } else {
            itemBinding.tvRozetKart.visibility = View.GONE
        }

        itemBinding.root.setOnClickListener {
            showOyuncuOlayBottomSheet(oyuncu, isEvSahibi, isSahada = isSahada)
        }

        parentRow.addView(itemBinding.root)
    }

    private fun kulubeyiCiz(yedekler: List<Oyuncu>, isEvSahibi: Boolean) {
        binding.containerYedekler.removeAllViews()
        binding.tvYedekSayisi.text = "${yedekler.size} Oyuncu"

        if (yedekler.isEmpty()) {
            binding.tvYedekYok.visibility = View.VISIBLE
            return
        }
        binding.tvYedekYok.visibility = View.GONE

        var sayac = 12
        for (oyuncu in yedekler) {
            val itemBinding = ItemYedekOyuncuBinding.inflate(layoutInflater, binding.containerYedekler, false)

            val no = if (oyuncu.formaNo > 0) oyuncu.formaNo.toString() else (sayac++).toString()
            itemBinding.tvYedekNo.text = no

            val avatarRenk = if (oyuncu.kirmizi > 0) "#4A4A4A" else if (isEvSahibi) "#2E7D4F" else "#C0392B"
            itemBinding.tvYedekNo.setBackgroundColor(Color.parseColor(avatarRenk))

            itemBinding.tvYedekIsim.text = oyuncu.isim
            itemBinding.tvYedekMevki.text = oyuncu.mevki.take(3).uppercase()

            if (oyuncu.gol > 0) {
                itemBinding.tvYedekRozetGol.visibility = View.VISIBLE
            } else {
                itemBinding.tvYedekRozetGol.visibility = View.GONE
            }

            itemBinding.root.setOnClickListener {
                showOyuncuOlayBottomSheet(oyuncu, isEvSahibi, isSahada = false)
            }

            binding.containerYedekler.addView(itemBinding.root)
        }
    }

    // ─── BOTTOM SHEET: KURAL ENTEGRASYONLU OYUNCU MENÜSÜ ──────────────────────

    private fun showOyuncuOlayBottomSheet(oyuncu: Oyuncu, isEvSahibi: Boolean, isSahada: Boolean) {
        // KURAL 4: Kırmızı kartlı oyuncu hiçbir işlem yapamaz!
        if (oyuncu.kirmizi > 0) {
            Toast.makeText(this, "⛔ ${oyuncu.isim} kırmızı kart gördüğü için oyundan ihraç edilmiştir!", Toast.LENGTH_LONG).show()
            return
        }

        val takimAdi = if (isEvSahibi) evTakim else depTakim
        val sheetDialog = BottomSheetDialog(this)
        val sheetBinding = BottomSheetOyuncuOlayBinding.inflate(layoutInflater)
        sheetDialog.setContentView(sheetBinding.root)

        val durumMetni = if (isSahada) "Sahada" else "Yedek Kulübesinde"
        sheetBinding.tvOlayOyuncuAdi.text = "${oyuncu.isim} ($takimAdi • $durumMetni)"

        // KURAL 1: Yedek kulübesindeki oyuncu gol atamaz!
        if (isSahada) {
            sheetBinding.btnGolEkle.visibility = View.VISIBLE
            sheetBinding.tvDegistirBaslik.text = "Oyuncu Değiştir (Yedekle)"
        } else {
            // YEDEKTE: Gol seçeneği tamamen kapatılır
            sheetBinding.btnGolEkle.visibility = View.GONE
            sheetBinding.tvDegistirBaslik.text = "Oyuna Gir (Sahadakiyle Değiş)"
        }

        // 1. GOL EKLE
        sheetBinding.btnGolEkle.setOnClickListener {
            val anlikSkor = if (isEvSahibi) evSkor else depSkor
            // KURAL 5: Tavan skor sınırı koruması
            if (anlikSkor >= MAX_SKOR_LIMIT) {
                Toast.makeText(this, "Maksimum skor sınırına ($MAX_SKOR_LIMIT) ulaşıldı!", Toast.LENGTH_SHORT).show()
                sheetDialog.dismiss()
                return@setOnClickListener
            }

            sheetDialog.dismiss()
            showDakikaDialog("GOL", oyuncu, isEvSahibi)
        }

        // 2. SARI KART
        sheetBinding.btnSariKart.setOnClickListener {
            sheetDialog.dismiss()
            showDakikaDialog("SARI", oyuncu, isEvSahibi)
        }

        // 3. KIRMIZI KART
        sheetBinding.btnKirmiziKart.setOnClickListener {
            sheetDialog.dismiss()
            showDakikaDialog("KIRMIZI", oyuncu, isEvSahibi)
        }

        // 4. OYUNCU DEĞİŞTİR / OYUNA GİR
        sheetBinding.btnOyuncuDegistir.setOnClickListener {
            sheetDialog.dismiss()
            showOyuncuDegisiklikDialog(oyuncu, isEvSahibi, isSahada)
        }

        sheetDialog.show()
    }

    private fun showDakikaDialog(olayTuru: String, oyuncu: Oyuncu, isEvSahibi: Boolean) {
        val etDakika = EditText(this).apply {
            hint = "Dakika (örn: 35)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("Dakikayı Girin")
            .setView(etDakika)
            .setPositiveButton("Onayla") { _, _ ->
                val dakika = etDakika.text.toString().trim().toIntOrNull()
                if (dakika == null || dakika < 1 || dakika > 120) {
                    Toast.makeText(this, "Geçerli bir dakika girin (1-120)", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                olayiUygula(olayTuru, dakika, oyuncu, isEvSahibi)
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun olayiUygula(olayTuru: String, dakika: Int, oyuncu: Oyuncu, isEvSahibi: Boolean) {
        val takimAdi = if (isEvSahibi) evTakim else depTakim

        when (olayTuru) {
            "GOL" -> {
                if (isEvSahibi) evSkor++ else depSkor++
                skorGuncelle()

                KadroRepository.oyuncuGuncelle(
                    ligAdi = oyuncu.ligAdi,
                    takimAdi = takimAdi,
                    oyuncuId = oyuncu.id,
                    guncellenenVeri = mapOf("gol" to oyuncu.gol + 1)
                )
                oyuncu.gol++
                olayEkle(dakika, "GOL", takimAdi, oyuncu.isim, oyuncu.id)
                Toast.makeText(this, "⚽ GOL! ${oyuncu.isim}", Toast.LENGTH_SHORT).show()
            }
            "SARI" -> {
                // KURAL 3: 2. Sarı Kart = Kırmızı Kart!
                if (oyuncu.sari >= 1) {
                    // İkinci sarı kart -> Kırmızıya dönüştür
                    oyuncu.sari++
                    oyuncu.kirmizi++

                    KadroRepository.oyuncuGuncelle(
                        ligAdi = oyuncu.ligAdi,
                        takimAdi = takimAdi,
                        oyuncuId = oyuncu.id,
                        guncellenenVeri = mapOf("sari" to oyuncu.sari, "kirmizi" to oyuncu.kirmizi)
                    )

                    olayEkle(dakika, "KIRMIZI", takimAdi, "${oyuncu.isim} (2.Sarıdan)", oyuncu.id)
                    Toast.makeText(this, "🟨🟥 İkinci sarı kart! ${oyuncu.isim} oyundan ihraç edildi!", Toast.LENGTH_LONG).show()
                } else {
                    oyuncu.sari++
                    KadroRepository.oyuncuGuncelle(
                        ligAdi = oyuncu.ligAdi,
                        takimAdi = takimAdi,
                        oyuncuId = oyuncu.id,
                        guncellenenVeri = mapOf("sari" to oyuncu.sari)
                    )
                    olayEkle(dakika, "SARI", takimAdi, oyuncu.isim, oyuncu.id)
                    Toast.makeText(this, "🟨 Sarı kart: ${oyuncu.isim}", Toast.LENGTH_SHORT).show()
                }
            }
            "KIRMIZI" -> {
                oyuncu.kirmizi++
                KadroRepository.oyuncuGuncelle(
                    ligAdi = oyuncu.ligAdi,
                    takimAdi = takimAdi,
                    oyuncuId = oyuncu.id,
                    guncellenenVeri = mapOf("kirmizi" to oyuncu.kirmizi)
                )
                olayEkle(dakika, "KIRMIZI", takimAdi, oyuncu.isim, oyuncu.id)
                Toast.makeText(this, "🟥 Direkt Kırmızı Kart! ${oyuncu.isim} ihraç edildi.", Toast.LENGTH_LONG).show()
            }
        }

        takimSekmesiGuncelle()
    }

    // ─── KURAL 2: KALECİ - KALECİ İLE DEĞİŞİR KURALI ──────────────────────────

    private fun showOyuncuDegisiklikDialog(secilenOyuncu: Oyuncu, isEvSahibi: Boolean, isSahada: Boolean) {
        val sahadakiler = if (isEvSahibi) evSahadakiler else depSahadakiler
        val yedekler = if (isEvSahibi) evYedekler else depYedekler

        val isKaleci = secilenOyuncu.mevki.trim().equals("Kaleci", ignoreCase = true)

        if (isSahada) {
            // SAHADAKİ OYUNCU ÇIKMAK İSTİYOR
            if (yedekler.isEmpty()) {
                Toast.makeText(this, "Kulübede yedek oyuncu bulunmuyor!", Toast.LENGTH_SHORT).show()
                return
            }

            if (isKaleci) {
                // KURAL 2: Sahadan çıkan kaleciyse öncelikle yedek kaleciler aranır!
                val yedekKaleciler = yedekler.filter { it.mevki.trim().equals("Kaleci", ignoreCase = true) }

                if (yedekKaleciler.isNotEmpty()) {
                    val kaleciIsimleri = yedekKaleciler.map { "🧤 ${it.isim} (Kaleci)" }.toTypedArray()

                    AlertDialog.Builder(this)
                        .setTitle("🧤 Kaleci Değişikliği (Yedek Kaleciler)")
                        .setItems(kaleciIsimleri) { _, index ->
                            val girenKaleci = yedekKaleciler[index]
                            degisikligiGerceklestir(cikan = secilenOyuncu, giren = girenKaleci, isEvSahibi = isEvSahibi)
                        }
                        .setNegativeButton("İptal", null)
                        .show()
                    return
                } else {
                    // Kulübede kaleci yoksa kullanıcıya onay sorulur
                    AlertDialog.Builder(this)
                        .setTitle("⚠️ Yedek Kaleci Yok!")
                        .setMessage("Kulübede başka Kaleci bulunmuyor. Sahadaki başka bir mevki oyuncusunu kaleye geçirmek istiyor musunuz?")
                        .setPositiveButton("Evet, Yedek Seç") { _, _ ->
                            val yedekIsimleri = yedekler.map { "${it.isim} (${it.mevki})" }.toTypedArray()
                            AlertDialog.Builder(this)
                                .setTitle("Kaleye Geçecek Oyuncuyu Seçin")
                                .setItems(yedekIsimleri) { _, index ->
                                    degisikligiGerceklestir(cikan = secilenOyuncu, giren = yedekler[index], isEvSahibi = isEvSahibi)
                                }
                                .setNegativeButton("İptal", null)
                                .show()
                        }
                        .setNegativeButton("İptal", null)
                        .show()
                    return
                }
            }

            // Normal saha oyuncusu değişikliği
            val yedekIsimleri = yedekler.map { "${it.isim} (${it.mevki})" }.toTypedArray()
            AlertDialog.Builder(this)
                .setTitle("Oyuna Girecek Yedeği Seçin")
                .setItems(yedekIsimleri) { _, index ->
                    degisikligiGerceklestir(cikan = secilenOyuncu, giren = yedekler[index], isEvSahibi = isEvSahibi)
                }
                .setNegativeButton("İptal", null)
                .show()

        } else {
            // KULÜBEDEKİ OYUNCU OYUNA GİRMEK İSTİYOR
            if (sahadakiler.isEmpty()) {
                Toast.makeText(this, "Sahada oyuncu bulunmuyor!", Toast.LENGTH_SHORT).show()
                return
            }

            if (isKaleci) {
                // KURAL 2: Giren yedek kaleciyse sahadaki kaleci yerine geçer
                val sahadakiKaleci = sahadakiler.find { it.mevki.trim().equals("Kaleci", ignoreCase = true) }
                if (sahadakiKaleci != null) {
                    AlertDialog.Builder(this)
                        .setTitle("🧤 Kaleci Değişikliği")
                        .setMessage("Yedek Kaleci ${secilenOyuncu.isim}, sahadaki Kaleci ${sahadakiKaleci.isim} yerine oyuna girecek. Onaylıyor musunuz?")
                        .setPositiveButton("Onayla") { _, _ ->
                            degisikligiGerceklestir(cikan = sahadakiKaleci, giren = secilenOyuncu, isEvSahibi = isEvSahibi)
                        }
                        .setNegativeButton("İptal", null)
                        .show()
                    return
                }
            }

            val sahaIsimleri = sahadakiler.map { "${it.isim} (${it.mevki})" }.toTypedArray()
            AlertDialog.Builder(this)
                .setTitle("Oyundan Çıkacak Oyuncuyu Seçin")
                .setItems(sahaIsimleri) { _, index ->
                    degisikligiGerceklestir(cikan = sahadakiler[index], giren = secilenOyuncu, isEvSahibi = isEvSahibi)
                }
                .setNegativeButton("İptal", null)
                .show()
        }
    }

    private fun degisikligiGerceklestir(cikan: Oyuncu, giren: Oyuncu, isEvSahibi: Boolean) {
        val sahadakiler = if (isEvSahibi) evSahadakiler else depSahadakiler
        val yedekler = if (isEvSahibi) evYedekler else depYedekler
        val takimAdi = if (isEvSahibi) evTakim else depTakim

        val etDakika = EditText(this).apply {
            hint = "Kaçıncı dakika? (örn: 55)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("Değişiklik Dakikası")
            .setView(etDakika)
            .setPositiveButton("Onayla") { _, _ ->
                val dakika = etDakika.text.toString().trim().toIntOrNull() ?: 50

                sahadakiler.remove(cikan)
                yedekler.remove(giren)

                sahadakiler.add(giren)
                yedekler.add(cikan)

                olayEkle(dakika, "DEGISIKLIK", takimAdi, "${cikan.isim} ➔ ${giren.isim}")
                Toast.makeText(this, "🔄 ${cikan.isim} çıktı, ${giren.isim} girdi!", Toast.LENGTH_SHORT).show()

                takimSekmesiGuncelle()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    // ─── MAÇ OLAYLARI (TIMELINE & VAR İPTAL SİSTEMİ) ──────────────────────────

    private fun olayEkle(dakika: Int, tur: String, takimAdi: String, oyuncuAdi: String, oyuncuId: String = "") {
        val yeniOlay = MacOlayi(dakika, tur, takimAdi, oyuncuAdi, oyuncuId)
        val index = macOlaylari.indexOfFirst { it.dakika > dakika }.takeIf { it >= 0 } ?: macOlaylari.size
        macOlaylari.add(index, yeniOlay)
        olaylariGuncelle()
    }

    private fun olaylariGuncelle() {
        if (macOlaylari.isEmpty()) {
            binding.tvOlayYok.visibility = View.VISIBLE
            binding.rvOlaylar.visibility = View.GONE
            return
        }

        binding.tvOlayYok.visibility = View.GONE
        binding.rvOlaylar.visibility = View.VISIBLE
        binding.rvOlaylar.adapter = MacOlayiAdapter()
    }

    /**
     * PRO ÖZELLİK: VAR / Hatalı Olayı Geri Alma
     * Olaylar listesindeki bir olaya tıklayınca o golü/kartı iptal eder ve skoru geri çeker.
     */
    private fun showOlayIptalDialog(olay: MacOlayi) {
        val turAciklama = when (olay.tur) {
            "GOL" -> "⚽ Gol (+1 Skor)"
            "SARI" -> "🟨 Sarı Kart"
            "KIRMIZI" -> "🟥 Kırmızı Kart"
            "DEGISIKLIK" -> "🔄 Değişiklik"
            else -> olay.tur
        }

        AlertDialog.Builder(this)
            .setTitle("Olayı İptal Et / Sil")
            .setMessage("${olay.dakika}'. dakikadaki $turAciklama ($olay.oyuncuAdi) kaydını silmek istiyor musunuz?\n\n(Gol ise skor 1 düşecektir)")
            .setPositiveButton("Evet, Sil") { _, _ ->
                macOlaylari.remove(olay)

                // Eğer silinen bir GOL ise skoru düşür
                if (olay.tur == "GOL") {
                    if (olay.takimAdi == evTakim && evSkor > 0) {
                        evSkor--
                    } else if (olay.takimAdi == depTakim && depSkor > 0) {
                        depSkor--
                    }
                    skorGuncelle()

                    // Oyuncunun gol sayısını düşür
                    val tum = if (olay.takimAdi == evTakim) (evSahadakiler + evYedekler) else (depSahadakiler + depYedekler)
                    val o = tum.find { it.id == olay.oyuncuId || it.isim == olay.oyuncuAdi }
                    if (o != null && o.gol > 0) {
                        o.gol--
                        KadroRepository.oyuncuGuncelle(ligAdi, olay.takimAdi, o.id, mapOf("gol" to o.gol))
                    }
                    Toast.makeText(this, "Gol iptal edildi, skor güncellendi.", Toast.LENGTH_SHORT).show()
                } else if (olay.tur == "SARI") {
                    val tum = if (olay.takimAdi == evTakim) (evSahadakiler + evYedekler) else (depSahadakiler + depYedekler)
                    val o = tum.find { it.id == olay.oyuncuId || it.isim == olay.oyuncuAdi }
                    if (o != null && o.sari > 0) {
                        o.sari--
                        KadroRepository.oyuncuGuncelle(ligAdi, olay.takimAdi, o.id, mapOf("sari" to o.sari))
                    }
                } else if (olay.tur == "KIRMIZI") {
                    val tum = if (olay.takimAdi == evTakim) (evSahadakiler + evYedekler) else (depSahadakiler + depYedekler)
                    val oyuncu = tum.find { it.id == olay.oyuncuId || olay.oyuncuAdi.contains(it.isim) }
                    if (oyuncu != null && oyuncu.kirmizi > 0) {
                        oyuncu.kirmizi--
                        KadroRepository.oyuncuGuncelle(ligAdi, olay.takimAdi, oyuncu.id, mapOf("kirmizi" to oyuncu.kirmizi))
                    }
                }

                olaylariGuncelle()
                takimSekmesiGuncelle()
            }
            .setNegativeButton("Vazgeç", null)
            .show()
    }

    // ─── KAYDETME VE ÇIKIŞ ─────────────────────────────────────────────────────

    private fun maciBitirVeKaydet() {
        if (macId != -1 && ligAdi.isNotEmpty()) {
            val olaylarFirestore = macOlaylari.map { olay ->
                mapOf(
                    "dakika"    to olay.dakika,
                    "tur"       to olay.tur,
                    "takimAdi"  to olay.takimAdi,
                    "oyuncuAdi" to olay.oyuncuAdi,
                    "oyuncuId"  to olay.oyuncuId
                )
            }

            KadroRepository.macKaydet(
                ligAdi  = ligAdi,
                macId   = macId,
                takim1  = evTakim,
                takim2  = depTakim,
                hafta   = hafta,
                skor1   = evSkor,
                skor2   = depSkor,
                olaylar = olaylarFirestore
            )
        }

        val donenIntent = Intent().apply {
            putExtra("MAC_ID",   macId)
            putExtra("EV_SKOR",  evSkor)
            putExtra("DEP_SKOR", depSkor)
        }
        setResult(Activity.RESULT_OK, donenIntent)
        finish()
    }

    // ─── OLAY ADAPTER (TIKLAYINCA İPTAL ETME ÖZELLİKLİ) ────────────────────────

    private inner class MacOlayiAdapter : RecyclerView.Adapter<MacOlayiAdapter.OlayVH>() {

        inner class OlayVH(val binding: ItemMacOlayiBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): OlayVH {
            val itemBinding = ItemMacOlayiBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return OlayVH(itemBinding)
        }

        override fun onBindViewHolder(holder: OlayVH, position: Int) {
            val olay = macOlaylari[position]
            holder.binding.tvOlayIkon.text = when (olay.tur) {
                "GOL"        -> "⚽"
                "SARI"       -> "🟨"
                "KIRMIZI"    -> "🟥"
                "DEGISIKLIK" -> "🔄"
                else         -> "•"
            }
            holder.binding.tvOlayOyuncu.text = olay.oyuncuAdi.take(12)
            holder.binding.tvOlayDakika.text = "${olay.dakika}'"

            // Tıklayınca VAR / İptal menüsü açılır
            holder.binding.root.setOnClickListener {
                showOlayIptalDialog(olay)
            }
        }

        override fun getItemCount(): Int = macOlaylari.size
    }
}
