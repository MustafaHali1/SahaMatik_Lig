package com.example.sahamatik_lig.view

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sahamatik_lig.R
import com.example.sahamatik_lig.adapter.OyuncuAramaAdapter
import com.example.sahamatik_lig.databinding.ActivityMacDetailBinding
import com.example.sahamatik_lig.databinding.BottomSheetOyuncuOlayBinding
import com.example.sahamatik_lig.databinding.DialogOyuncuAraSecBinding
import com.example.sahamatik_lig.databinding.DialogOyuncuBulutBinding
import com.example.sahamatik_lig.databinding.ItemSahaOyuncuBinding
import com.example.sahamatik_lig.databinding.ItemYedekOyuncuBinding
import com.example.sahamatik_lig.model.KadroRepository
import com.example.sahamatik_lig.model.Oyuncu
import com.example.sahamatik_lig.util.ImagePickerHelper
import com.example.sahamatik_lig.util.ProfilFotoHelper
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * MacDetailActivity — Sofascore Tarzı Sade ve Hızlı Halı Saha Maç Yönetimi.
 *
 * Özellikler:
 *  - 7 Oyuncu Sahada (Mevkilerine göre dinamik diziliş)
 *  - Diğer tüm oyuncular "Y. Kulübesi" (Yedekler) bölümünde
 *  - Oyuncuya dokunulduğunda "Bulut Mini Kartı" açılır (Ekran kapanmaz/kesilmez)
 *  - Gol, kart veya oyuncu değişikliğinde dakika SORULMAZ, anında uygulanır!
 *  - Kurucu/Yönetici Kilidi: Sadece maçı oluşturan cihaz olay ekleyebilir/değişiklik yapabilir
 *  - Maçtan çıkışta skor Firestore'a kaydedilir ve oyuncu kariyer istatistikleri güncellenir
 */
class MacDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMacDetailBinding

    // Maç Bilgileri
    private var evTakim: String = ""
    private var depTakim: String = ""
    private var ligAdi: String = ""
    private var formatTipi: String = ""
    private var macId: Int = -1
    private var hafta: Int = 1
    private var ligOlusturanId: String = ""
    private var macDahaOnceKaydedildiMi: Boolean = false
    private val oncekiOyuncuStats = mutableMapOf<String, Map<String, Int>>()

    // Skorlar
    private var evSkor: Int = 0
    private var depSkor: Int = 0
    private val MAX_SKOR_LIMIT = 30

    // Kadrolar (Sahadaki 7 kişi + Kulübedeki yedekler)
    private val evSahadakiler = mutableListOf<Oyuncu>()
    private val evYedekler = mutableListOf<Oyuncu>()

    private val depSahadakiler = mutableListOf<Oyuncu>()
    private val depYedekler = mutableListOf<Oyuncu>()

    // Hangi takımın kadrosu görüntüleniyor (true: Ev Sahibi, false: Deplasman)
    private var isEvSecili: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMacDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Intent verilerini al
        evTakim    = intent.getStringExtra("EV_TAKIM")    ?: ""
        depTakim   = intent.getStringExtra("DEP_TAKIM")   ?: ""
        ligAdi     = intent.getStringExtra("LIG_ADI")     ?: ""
        formatTipi = intent.getStringExtra("FORMAT_TIPI") ?: ""
        macId      = intent.getIntExtra("MAC_ID", -1)
        hafta      = intent.getIntExtra("HAFTA", 1)
        evSkor     = intent.getIntExtra("EV_SKOR", 0)
        depSkor    = intent.getIntExtra("DEP_SKOR", 0)

        // Başlıklar ve Logolar
        binding.tvEvAdi.text = evTakim
        binding.tvDepAdi.text = depTakim
        binding.tvHaftaLabel.text = if (hafta > 1) "$hafta. Hafta • Maç Detayı" else "Maç Detayı"
        binding.btnTabEv.text = "🏠 $evTakim"
        binding.btnTabDep.text = "⚽ $depTakim"

        ImagePickerHelper.uriGetir(this, "logo_${ligAdi}_$evTakim")?.let { binding.ivEvLogo.setImageURI(it) }
        ImagePickerHelper.uriGetir(this, "logo_${ligAdi}_$depTakim")?.let { binding.ivDepLogo.setImageURI(it) }

        // Takım isimlerine veya logolarına tıklandığında TakimDetailActivity'ye git
        val evDetayAc = {
            if (evTakim.isNotEmpty()) {
                val intent = Intent(this, TakimDetailActivity::class.java).apply {
                    putExtra("TAKIM_ADI", evTakim)
                    putExtra("LIG_ADI", ligAdi)
                }
                startActivity(intent)
            }
        }
        binding.ivEvLogo.setOnClickListener { evDetayAc() }
        binding.tvEvAdi.setOnClickListener { evDetayAc() }

        val depDetayAc = {
            if (depTakim.isNotEmpty()) {
                val intent = Intent(this, TakimDetailActivity::class.java).apply {
                    putExtra("TAKIM_ADI", depTakim)
                    putExtra("LIG_ADI", ligAdi)
                }
                startActivity(intent)
            }
        }
        binding.ivDepLogo.setOnClickListener { depDetayAc() }
        binding.tvDepAdi.setOnClickListener { depDetayAc() }

        skorGuncelle()

        // Kaydedilmiş maç skoru ve oyuncu istatistikleri varsa Firestore'dan al ve güncelle
        if (ligAdi.isNotEmpty() && macId != -1) {
            KadroRepository.macSonuclariYukle(ligAdi, onResult = { sonuclar ->
                val kayitli = sonuclar.find { it.macId == macId }
                if (kayitli != null && kayitli.isOynandi) {
                    macDahaOnceKaydedildiMi = true
                    oncekiOyuncuStats.clear()
                    kayitli.oyuncuIstatistikleri.forEach { (u, stats) ->
                        val clean = u.removePrefix("@").lowercase().trim()
                        oncekiOyuncuStats[clean] = mapOf(
                            "gol" to (stats["gol"] ?: 0L).toInt(),
                            "sari" to (stats["sari"] ?: 0L).toInt(),
                            "kirmizi" to (stats["kirmizi"] ?: 0L).toInt()
                        )
                    }
                    runOnUiThread {
                        evSkor = kayitli.skor1
                        depSkor = kayitli.skor2
                        skorGuncelle()
                    }
                }
            })
        }

        // Kurucu / Yetkili ve Format kontrolü için lig bilgisini yükle
        if (ligAdi.isNotEmpty()) {
            KadroRepository.ligGetir(ligAdi) { lig ->
                if (lig != null) {
                    ligOlusturanId = lig.olusturanId
                    if (formatTipi.isEmpty()) {
                        formatTipi = lig.formatTipi
                    }
                }
            }
        }

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

        // Kaptan / Yönetici Butonları (Maç Kurulduktan Sonra da Kadro Ekleme & Link Paylaşma)
        binding.btnMacOyuncuEkle.setOnClickListener {
            val hedefTakim = if (isEvSecili) evTakim else depTakim
            val macMevcutUsernameler = (evSahadakiler + evYedekler + depSahadakiler + depYedekler).map {
                (if (it.username.isNotEmpty()) it.username else it.isim).removePrefix("@").lowercase().trim()
            }.toSet()

            val acVeEkle = { engellenenler: Set<String> ->
                showOyuncuAraSecDialog(haricTutulanUsernameler = engellenenler) { secilenOyuncu ->
                    KadroRepository.takimaOyuncuEkleKontrollu(
                        ligAdi = ligAdi,
                        takimAdi = hedefTakim,
                        oyuncu = secilenOyuncu,
                        maxKontenjan = 10,
                        onSuccess = {
                            Toast.makeText(this, "${secilenOyuncu.isim} kadroya eklendi! ⚽", Toast.LENGTH_SHORT).show()
                            kadrolariYukle()
                        },
                        onLimitDolu = {
                            Toast.makeText(this, "⚠️ Takım kontenjanı dolu (Maksimum 10 oyuncu)!", Toast.LENGTH_LONG).show()
                        },
                        onError = { e ->
                            Toast.makeText(this, e.message ?: "Ekleme başarısız", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }

            if (formatTipi != "TEKIL_MAC") {
                KadroRepository.ligdekiTumOyuncular(ligAdi) { tumTakimlarOyuncular ->
                    val ligdekiTumUsernameler = tumTakimlarOyuncular.values.flatten().map {
                        (if (it.username.isNotEmpty()) it.username else it.isim).removePrefix("@").lowercase().trim()
                    }.toSet()
                    runOnUiThread {
                        acVeEkle(macMevcutUsernameler + ligdekiTumUsernameler)
                    }
                }
            } else {
                acVeEkle(macMevcutUsernameler)
            }
        }

        binding.btnMacDavetLinki.setOnClickListener {
            val hedefTakim = if (isEvSecili) evTakim else depTakim
            KadroRepository.davetLinkiPaylas(this, ligAdi, hedefTakim)
        }

        // Geri Tuşları: Çıkış alert mesajı göster
        binding.tvBack.setOnClickListener { showCikisKayitDialog() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showCikisKayitDialog()
            }
        })

        // Kadroları Firestore'dan Çek
        kadrolariYukle()
    }

    override fun onResume() {
        super.onResume()
        ImagePickerHelper.uriGetir(this, "logo_${ligAdi}_$evTakim")?.let { binding.ivEvLogo.setImageURI(it) }
        ImagePickerHelper.uriGetir(this, "logo_${ligAdi}_$depTakim")?.let { binding.ivDepLogo.setImageURI(it) }
        kadrolariYukle()
    }

    // ─── SKOR ─────────────────────────────────────────────────────────────────

    private fun skorGuncelle() {
        binding.tvEvSkor.text = evSkor.toString()
        binding.tvDepSkor.text = depSkor.toString()
    }

    // ─── KADROLAR VE 7 KİŞİLİK HALI SAHA AYRIMI ───────────────────────────────

    private var ilkKadroYuklendiMi = false

    private fun kadrolariYukle() {
        KadroRepository.takimOyunculariniGetir(ligAdi, evTakim) { evTum ->
            if (!ilkKadroYuklendiMi) {
                ayristirKadro(evTum, evSahadakiler, evYedekler)
            } else {
                kadroyuKoruVeGuncelle(evTum, evSahadakiler, evYedekler)
            }

            KadroRepository.takimOyunculariniGetir(ligAdi, depTakim) { depTum ->
                if (!ilkKadroYuklendiMi) {
                    ayristirKadro(depTum, depSahadakiler, depYedekler)
                    ilkKadroYuklendiMi = true
                } else {
                    kadroyuKoruVeGuncelle(depTum, depSahadakiler, depYedekler)
                }

                runOnUiThread {
                    takimSekmesiGuncelle()
                }
            }
        }
    }

    private fun oyuncuEslesiyorMu(o1: Oyuncu, o2: Oyuncu): Boolean {
        val u1 = (if (o1.username.isNotEmpty()) o1.username else o1.isim).removePrefix("@").lowercase().trim()
        val u2 = (if (o2.username.isNotEmpty()) o2.username else o2.isim).removePrefix("@").lowercase().trim()
        if (u1.isNotEmpty() && u1 == u2) return true
        if (o1.id.isNotEmpty() && o1.id == o2.id) return true
        return o1.isim.trim().equals(o2.isim.trim(), ignoreCase = true)
    }

    private fun kadroyuKoruVeGuncelle(
        firestoreOyuncular: List<Oyuncu>,
        sahadakiler: MutableList<Oyuncu>,
        yedekler: MutableList<Oyuncu>
    ) {
        val yerel = KadroRepository.yerelProfilGetir(this)

        // 1. Sahadaki oyuncuların mevkisini/fotoğrafını güncelle, AMA sahadaki yerini ve maç istatistiklerini (gol, kart) koru!
        for (i in sahadakiler.indices) {
            val mevcut = sahadakiler[i]
            val fs = firestoreOyuncular.find { oyuncuEslesiyorMu(it, mevcut) }
            var foto = fs?.profilFotoUri ?: mevcut.profilFotoUri
            if (foto.isBlank() && yerel != null && yerel.profilFotoUri.isNotBlank()) {
                val cleanMevcut = (if (mevcut.username.isNotEmpty()) mevcut.username else mevcut.isim).removePrefix("@").lowercase().trim()
                val cleanYerel = yerel.username.removePrefix("@").lowercase().trim()
                if (cleanMevcut == cleanYerel || mevcut.isim.equals(yerel.isim, ignoreCase = true)) {
                    foto = yerel.profilFotoUri
                }
            }
            sahadakiler[i] = mevcut.copy(
                profilFotoUri = foto,
                isim = fs?.isim ?: mevcut.isim,
                mevki = fs?.mevki ?: mevcut.mevki
            )
        }

        // 2. Kulübedeki oyuncuların mevkisini/fotoğrafını güncelle, kulübedeki yerini koru!
        for (i in yedekler.indices) {
            val mevcut = yedekler[i]
            val fs = firestoreOyuncular.find { oyuncuEslesiyorMu(it, mevcut) }
            var foto = fs?.profilFotoUri ?: mevcut.profilFotoUri
            if (foto.isBlank() && yerel != null && yerel.profilFotoUri.isNotBlank()) {
                val cleanMevcut = (if (mevcut.username.isNotEmpty()) mevcut.username else mevcut.isim).removePrefix("@").lowercase().trim()
                val cleanYerel = yerel.username.removePrefix("@").lowercase().trim()
                if (cleanMevcut == cleanYerel || mevcut.isim.equals(yerel.isim, ignoreCase = true)) {
                    foto = yerel.profilFotoUri
                }
            }
            yedekler[i] = mevcut.copy(
                profilFotoUri = foto,
                isim = fs?.isim ?: mevcut.isim,
                mevki = fs?.mevki ?: mevcut.mevki
            )
        }

        // 3. Yeni eklenen oyuncu varsa (ne sahada ne kulübede)
        for (fs in firestoreOyuncular) {
            val sahadaVar = sahadakiler.any { oyuncuEslesiyorMu(it, fs) }
            val yedekteVar = yedekler.any { oyuncuEslesiyorMu(it, fs) }
            if (!sahadaVar && !yedekteVar) {
                var foto = fs.profilFotoUri
                if (foto.isBlank() && yerel != null && yerel.profilFotoUri.isNotBlank()) {
                    val cleanFs = (if (fs.username.isNotEmpty()) fs.username else fs.isim).removePrefix("@").lowercase().trim()
                    val cleanYerel = yerel.username.removePrefix("@").lowercase().trim()
                    if (cleanFs == cleanYerel || fs.isim.equals(yerel.isim, ignoreCase = true)) {
                        foto = yerel.profilFotoUri
                    }
                }
                val yeni = fs.copy(profilFotoUri = foto, gol = 0, sari = 0, kirmizi = 0)
                if (sahadakiler.size < 7) {
                    sahadakiler.add(yeni)
                } else {
                    yedekler.add(yeni)
                }
            }
        }

        // 4. Firestore'dan tamamen silinen oyuncu varsa temizle
        sahadakiler.removeAll { s -> !firestoreOyuncular.any { oyuncuEslesiyorMu(it, s) } }
        yedekler.removeAll { y -> !firestoreOyuncular.any { oyuncuEslesiyorMu(it, y) } }

        // Fotoğrafı eksik olanlar için asenkron profil sorgusu
        for (oyuncu in (sahadakiler + yedekler)) {
            if (oyuncu.profilFotoUri.isBlank()) {
                val rawUsername = if (oyuncu.username.isNotEmpty()) oyuncu.username else oyuncu.isim
                KadroRepository.oyuncuProfiliGetir(rawUsername) { profil ->
                    if (profil != null && profil.profilFotoUri.isNotBlank()) {
                        val sIndex = sahadakiler.indexOfFirst { oyuncuEslesiyorMu(it, oyuncu) }
                        if (sIndex != -1) {
                            sahadakiler[sIndex] = sahadakiler[sIndex].copy(profilFotoUri = profil.profilFotoUri)
                            runOnUiThread { takimSekmesiGuncelle() }
                        } else {
                            val yIndex = yedekler.indexOfFirst { oyuncuEslesiyorMu(it, oyuncu) }
                            if (yIndex != -1) {
                                yedekler[yIndex] = yedekler[yIndex].copy(profilFotoUri = profil.profilFotoUri)
                                runOnUiThread { takimSekmesiGuncelle() }
                            }
                        }
                    }
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

        val yerel = KadroRepository.yerelProfilGetir(this)

        // Maç içi istatistikleri ve profil fotoğraflarını ayarla
        val hazirlananOyuncular = tumOyuncular.map { oyuncu ->
            val clean = (if (oyuncu.username.isNotEmpty()) oyuncu.username else oyuncu.isim).removePrefix("@").lowercase().trim()
            val macStat = oncekiOyuncuStats[clean]

            // Eğer fotoğrafı boşsa ve yerel kullanıcı ile eşleşiyorsa yerel profilin fotoğrafını eşle
            var guncelFoto = oyuncu.profilFotoUri
            if (guncelFoto.isBlank() && yerel != null && yerel.profilFotoUri.isNotBlank()) {
                val cleanYerel = yerel.username.removePrefix("@").lowercase().trim()
                if (clean == cleanYerel || oyuncu.isim.equals(yerel.isim, ignoreCase = true)) {
                    guncelFoto = yerel.profilFotoUri
                }
            }

            val bazOyuncu = if (guncelFoto != oyuncu.profilFotoUri) oyuncu.copy(profilFotoUri = guncelFoto) else oyuncu

            if (macStat != null) {
                bazOyuncu.copy(
                    gol = macStat["gol"] ?: 0,
                    sari = macStat["sari"] ?: 0,
                    kirmizi = macStat["kirmizi"] ?: 0
                )
            } else if (!macDahaOnceKaydedildiMi) {
                bazOyuncu.copy(gol = 0, sari = 0, kirmizi = 0)
            } else {
                bazOyuncu
            }
        }

        // 1 Kaleci (varsa ilk kaleciyi sahaya al)
        val kaleciler = hazirlananOyuncular.filter { it.mevki.trim().equals("Kaleci", ignoreCase = true) }.toMutableList()
        val digerOyuncular = hazirlananOyuncular.filter { !it.mevki.trim().equals("Kaleci", ignoreCase = true) }.toMutableList()

        if (kaleciler.isNotEmpty()) {
            sahadakiler.add(kaleciler.removeAt(0))
        }

        // Kalan oyuncuları (mevkileri korunarak) ilk 7 kişi sahada olacak şekilde ekle (sabit 2-2-2 yok, neyse o)
        val havuz = (digerOyuncular + kaleciler).toMutableList()
        while (sahadakiler.size < 7 && havuz.isNotEmpty()) {
            sahadakiler.add(havuz.removeAt(0))
        }

        // 7'den sonrakiler kulübeye
        yedekler.addAll(havuz)

        // Fotoğrafı eksik olan kayıtlı oyuncular için Firestore'daki ana profil koleksiyonundan fotoğrafı sorgula
        for (oyuncu in (sahadakiler + yedekler)) {
            if (oyuncu.profilFotoUri.isBlank()) {
                val rawUsername = if (oyuncu.username.isNotEmpty()) oyuncu.username else oyuncu.isim
                KadroRepository.oyuncuProfiliGetir(rawUsername) { profil ->
                    if (profil != null && profil.profilFotoUri.isNotBlank()) {
                        val sIndex = sahadakiler.indexOfFirst { oyuncuEslesiyorMu(it, oyuncu) }
                        if (sIndex != -1) {
                            sahadakiler[sIndex] = sahadakiler[sIndex].copy(profilFotoUri = profil.profilFotoUri)
                            runOnUiThread { takimSekmesiGuncelle() }
                        } else {
                            val yIndex = yedekler.indexOfFirst { oyuncuEslesiyorMu(it, oyuncu) }
                            if (yIndex != -1) {
                                yedekler[yIndex] = yedekler[yIndex].copy(profilFotoUri = profil.profilFotoUri)
                                runOnUiThread { takimSekmesiGuncelle() }
                            }
                        }
                    }
                }
            }
        }
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

        val currentCihazId = KadroRepository.cihazIdAl(this)
        val isYonetici = ligOlusturanId.isEmpty() || ligOlusturanId == currentCihazId
        binding.layoutKadroEklemeButonlari.visibility = if (isYonetici) View.VISIBLE else View.GONE

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

    private fun fotoYukleVeyaEskiYontem(
        profilFotoUri: String,
        cardInner: androidx.cardview.widget.CardView,
        ivFoto: ImageView,
        tvNo: TextView,
        formaNo: String,
        formaRenk: String
    ) {
        val bitmap = ProfilFotoHelper.gorselYukle(this, profilFotoUri)
        if (bitmap != null) {
            cardInner.setCardBackgroundColor(Color.TRANSPARENT)
            ivFoto.setImageBitmap(bitmap)
            ivFoto.visibility = View.VISIBLE
            tvNo.visibility = View.GONE
        } else {
            // ESKİ YÖNTEM: Fotoğrafı olmayanlar için forma renginde yuvarlak içinde numara
            cardInner.setCardBackgroundColor(Color.parseColor(formaRenk))
            ivFoto.visibility = View.GONE
            tvNo.visibility = View.VISIBLE
            tvNo.text = formaNo
            tvNo.setTextColor(Color.WHITE)
            tvNo.background = null
        }
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
        val formaRenk = if (oyuncu.kirmizi > 0) {
            "#4A4A4A"
        } else if (isEvSahibi) {
            "#2E7D4F"
        } else {
            "#C0392B"
        }

        // Dış border rengi (Takım rengi)
        itemBinding.cardAvatarBorder.setCardBackgroundColor(Color.parseColor(formaRenk))

        // Yüz fotoğrafı veya forma numarası (eski yöntem)
        fotoYukleVeyaEskiYontem(
            profilFotoUri = oyuncu.profilFotoUri,
            cardInner = itemBinding.cardInnerAvatar,
            ivFoto = itemBinding.ivOyuncuFoto,
            tvNo = itemBinding.tvFormaNo,
            formaNo = formaNo,
            formaRenk = formaRenk
        )

        // Sahada sade ve kalabalık olmayan isim: Sadece numara ve ilk isim (Örn: "10. Yiğit")
        val ilkIsim = oyuncu.isim.trim().split(Regex("\\s+")).firstOrNull { it.isNotBlank() } ?: oyuncu.isim
        itemBinding.tvOyuncuAdi.text = "$formaNo. $ilkIsim"
        itemBinding.tvOyuncuMevki.text = oyuncu.mevki.take(3).uppercase()

        // Gol rozeti (Gol sayısına göre top sayısı artar: ⚽, ⚽⚽, ⚽⚽⚽)
        if (oyuncu.gol > 0) {
            itemBinding.tvRozetGol.visibility = View.VISIBLE
            itemBinding.tvRozetGol.text = if (oyuncu.gol in 1..3) {
                "⚽".repeat(oyuncu.gol)
            } else {
                "⚽ x${oyuncu.gol}"
            }
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
            showOyuncuBulutDialog(oyuncu, isEvSahibi, isSahada = isSahada)
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
            val avatarRenk = if (oyuncu.kirmizi > 0) "#4A4A4A" else if (isEvSahibi) "#2E7D4F" else "#C0392B"
            itemBinding.cardYedekBorder.setCardBackgroundColor(Color.parseColor(avatarRenk))

            fotoYukleVeyaEskiYontem(
                profilFotoUri = oyuncu.profilFotoUri,
                cardInner = itemBinding.cardInnerYedek,
                ivFoto = itemBinding.ivYedekFoto,
                tvNo = itemBinding.tvYedekNo,
                formaNo = no,
                formaRenk = avatarRenk
            )

            val ilkIsim = oyuncu.isim.trim().split(Regex("\\s+")).firstOrNull { it.isNotBlank() } ?: oyuncu.isim
            itemBinding.tvYedekIsim.text = "$no. $ilkIsim"
            itemBinding.tvYedekMevki.text = oyuncu.mevki.take(3).uppercase()

            if (oyuncu.gol > 0) {
                itemBinding.tvYedekRozetGol.visibility = View.VISIBLE
                itemBinding.tvYedekRozetGol.text = if (oyuncu.gol in 1..3) "⚽".repeat(oyuncu.gol) else "⚽ x${oyuncu.gol}"
            } else {
                itemBinding.tvYedekRozetGol.visibility = View.GONE
            }

            itemBinding.root.setOnClickListener {
                showOyuncuBulutDialog(oyuncu, isEvSahibi, isSahada = false)
            }

            binding.containerYedekler.addView(itemBinding.root)
        }
    }

    // ─── BULUT MİNİ KART: OYUNCU İŞLEM & DETAY PENCERESİ ────────────────────────

    private fun showOyuncuBulutDialog(oyuncu: Oyuncu, isEvSahibi: Boolean, isSahada: Boolean) {
        val dialogBinding = DialogOyuncuBulutBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        // Mevki rozeti
        val mevkiIcon = when (oyuncu.mevki.trim()) {
            "Kaleci" -> "🧤 Kaleci"
            "Defans" -> "🛡️ Defans"
            "Orta Saha" -> "⚡ Orta Saha"
            "Forvet" -> "⚽ Forvet"
            else -> oyuncu.mevki
        }
        dialogBinding.tvBulutMevki.text = mevkiIcon

        // Avatar baş harfi / Yüz fotoğrafı
        val ilkHarf = oyuncu.isim.trim().take(1).uppercase()
        dialogBinding.tvBulutHarf.text = if (ilkHarf.isNotEmpty()) ilkHarf else "O"

        val bulutBitmap = ProfilFotoHelper.gorselYukle(this, oyuncu.profilFotoUri)
        if (bulutBitmap != null) {
            dialogBinding.ivBulutFoto.setImageBitmap(bulutBitmap)
            dialogBinding.ivBulutFoto.visibility = View.VISIBLE
            dialogBinding.tvBulutHarf.visibility = View.GONE
        } else {
            dialogBinding.ivBulutFoto.visibility = View.GONE
            dialogBinding.tvBulutHarf.visibility = View.VISIBLE
        }

        // İsim ve Username
        dialogBinding.tvBulutIsim.text = oyuncu.isim
        val rawUsername = if (oyuncu.username.isNotEmpty()) oyuncu.username else "@${oyuncu.isim.lowercase().replace(" ", "")}"
        val formattedUsername = if (rawUsername.startsWith("@")) rawUsername else "@$rawUsername"
        dialogBinding.tvBulutUsername.text = formattedUsername

        val takimAdi = if (isEvSahibi) evTakim else depTakim
        val durumStr = if (isSahada) "Sahada" else "Yedek Kulübesinde"
        dialogBinding.tvBulutTakim.text = "$takimAdi • $durumStr"

        // Maç içi istatistikler
        dialogBinding.tvBulutMacGol.text = oyuncu.gol.toString()
        dialogBinding.tvBulutMacSari.text = oyuncu.sari.toString()
        dialogBinding.tvBulutMacKirmizi.text = oyuncu.kirmizi.toString()

        // Kariyer istatistiklerini getir
        dialogBinding.tvBulutKariyerOzet.text = "⭐ Kariyer yükleniyor..."
        KadroRepository.oyuncuProfiliGetir(formattedUsername) { profil ->
            runOnUiThread {
                if (profil != null) {
                    dialogBinding.tvBulutKariyerOzet.text = "⭐ Kariyer: ${profil.toplamMac} Maç • ${profil.toplamGol} Gol • ${profil.toplamSari} Sarı • ${profil.toplamKirmizi} Kırmızı"
                } else {
                    dialogBinding.tvBulutKariyerOzet.text = "⭐ Kariyer: 0 Maç • 0 Gol"
                }
            }
        }

        // Yönetici / Kurucu Yetki Kontrolü (İzleyiciler sadece kartı ve profili görür, olay ekleyemez)
        val currentCihazId = KadroRepository.cihazIdAl(this)
        val isYonetici = ligOlusturanId.isEmpty() || ligOlusturanId == currentCihazId
        dialogBinding.layoutBulutYoneticiButonlari.visibility = if (isYonetici) View.VISIBLE else View.GONE

        // İhraç edilmişse uyarı verip butonları pasifleştir
        if (oyuncu.kirmizi > 0) {
            dialogBinding.btnBulutGolEkle.visibility = View.GONE
            dialogBinding.btnBulutSariKart.isEnabled = false
            dialogBinding.btnBulutKirmiziKart.isEnabled = false
            dialogBinding.btnBulutDegistir.isEnabled = false
            dialogBinding.btnBulutDegistir.alpha = 0.5f
            dialogBinding.btnBulutSariKart.alpha = 0.5f
            dialogBinding.btnBulutKirmiziKart.alpha = 0.5f
        } else {
            if (isSahada) {
                dialogBinding.btnBulutGolEkle.visibility = View.VISIBLE
                dialogBinding.btnBulutDegistir.text = "🔄 Değiştir"
            } else {
                dialogBinding.btnBulutGolEkle.visibility = View.GONE
                dialogBinding.btnBulutDegistir.text = "🔄 Oyuna Gir"
            }

            // Buton Aksiyonları (Anında uygulanır, dakika sormaz!)
            dialogBinding.btnBulutGolEkle.setOnClickListener {
                val anlikSkor = if (isEvSahibi) evSkor else depSkor
                if (anlikSkor >= MAX_SKOR_LIMIT) {
                    Toast.makeText(this, "Maksimum skor sınırına ($MAX_SKOR_LIMIT) ulaşıldı!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    return@setOnClickListener
                }
                dialog.dismiss()
                golEkle(oyuncu, isEvSahibi)
            }

            dialogBinding.btnBulutSariKart.setOnClickListener {
                dialog.dismiss()
                sariKartEkle(oyuncu, isEvSahibi)
            }

            dialogBinding.btnBulutKirmiziKart.setOnClickListener {
                dialog.dismiss()
                kirmiziKartEkle(oyuncu, isEvSahibi)
            }

            dialogBinding.btnBulutDegistir.setOnClickListener {
                dialog.dismiss()
                showOyuncuDegisiklikDialog(oyuncu, isEvSahibi, isSahada)
            }
        }

        // Tam Profili Gör (OyuncuDetailActivity açılır)
        dialogBinding.btnBulutTamProfil.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, OyuncuDetailActivity::class.java).apply {
                val aktarilacakOyuncu = oyuncu.copy(
                    username = formattedUsername,
                    takimAdi = takimAdi,
                    ligAdi = ligAdi
                )
                putExtra("OYUNCU", aktarilacakOyuncu)
            }
            startActivity(intent)
        }

        dialogBinding.btnBulutKapat.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    // ─── DOĞRUDAN OLAY UYGULAMALARI (DAKİKASIZ, ANINDA) ────────────────────────

    private fun golEkle(oyuncu: Oyuncu, isEvSahibi: Boolean) {
        val takimAdi = if (isEvSahibi) evTakim else depTakim

        if (isEvSahibi) evSkor++ else depSkor++
        skorGuncelle()

        oyuncu.gol++
        KadroRepository.oyuncuGuncelle(
            ligAdi = oyuncu.ligAdi,
            takimAdi = takimAdi,
            oyuncuId = oyuncu.id,
            guncellenenVeri = mapOf("gol" to oyuncu.gol)
        )

        Toast.makeText(this, "⚽ GOL! ${oyuncu.isim}", Toast.LENGTH_SHORT).show()
        takimSekmesiGuncelle()
    }

    private fun sariKartEkle(oyuncu: Oyuncu, isEvSahibi: Boolean) {
        val takimAdi = if (isEvSahibi) evTakim else depTakim

        // Zaten 2 sarı veya 1 kırmızı kartı varsa tekrar verilemez!
        if (oyuncu.kirmizi >= 1 || oyuncu.sari >= 2) {
            Toast.makeText(this, "⚠️ ${oyuncu.isim} zaten ihraç edilmiş durumda!", Toast.LENGTH_SHORT).show()
            return
        }

        if (oyuncu.sari == 1) {
            // İkinci sarı kart -> Tam olarak 2 Sarı ve Otomatik 1 Kırmızı Kart!
            oyuncu.sari = 2
            oyuncu.kirmizi = 1

            KadroRepository.oyuncuGuncelle(
                ligAdi = oyuncu.ligAdi,
                takimAdi = takimAdi,
                oyuncuId = oyuncu.id,
                guncellenenVeri = mapOf("sari" to oyuncu.sari, "kirmizi" to oyuncu.kirmizi)
            )

            Toast.makeText(this, "🟨🟥 İkinci sarı kart! ${oyuncu.isim} ihraç edildi!", Toast.LENGTH_LONG).show()
        } else {
            oyuncu.sari = 1
            KadroRepository.oyuncuGuncelle(
                ligAdi = oyuncu.ligAdi,
                takimAdi = takimAdi,
                oyuncuId = oyuncu.id,
                guncellenenVeri = mapOf("sari" to oyuncu.sari)
            )
            Toast.makeText(this, "🟨 Sarı kart: ${oyuncu.isim}", Toast.LENGTH_SHORT).show()
        }

        takimSekmesiGuncelle()
    }

    private fun kirmiziKartEkle(oyuncu: Oyuncu, isEvSahibi: Boolean) {
        val takimAdi = if (isEvSahibi) evTakim else depTakim

        if (oyuncu.kirmizi >= 1) {
            Toast.makeText(this, "⚠️ ${oyuncu.isim} zaten kırmızı kart görmüş!", Toast.LENGTH_SHORT).show()
            return
        }

        oyuncu.kirmizi = 1
        KadroRepository.oyuncuGuncelle(
            ligAdi = oyuncu.ligAdi,
            takimAdi = takimAdi,
            oyuncuId = oyuncu.id,
            guncellenenVeri = mapOf("kirmizi" to oyuncu.kirmizi)
        )

        Toast.makeText(this, "🟥 Doğrudan kırmızı kart! ${oyuncu.isim} ihraç edildi.", Toast.LENGTH_LONG).show()
        takimSekmesiGuncelle()
    }

    // ─── OYUNCU DEĞİŞİKLİĞİ (DAKİKA SORMAZ, SEÇİLDİĞİ AN YER DEĞİŞİR) ──────────

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
                // Kaleci yerine öncelikle yedek kaleciler aranır
                val yedekKaleciler = yedekler.filter { it.mevki.trim().equals("Kaleci", ignoreCase = true) }

                if (yedekKaleciler.isNotEmpty()) {
                    val kaleciIsimleri = yedekKaleciler.map { "🧤 ${it.isim} (Kaleci)" }.toTypedArray()

                    AlertDialog.Builder(this)
                        .setTitle("🧤 Kaleci Değişikliği")
                        .setItems(kaleciIsimleri) { _, index ->
                            degisikligiGerceklestir(cikan = secilenOyuncu, giren = yedekKaleciler[index], isEvSahibi = isEvSahibi)
                        }
                        .setNegativeButton("İptal", null)
                        .show()
                    return
                } else {
                    AlertDialog.Builder(this)
                        .setTitle("⚠️ Yedek Kaleci Yok")
                        .setMessage("Kulübede başka Kaleci bulunmuyor. Sahadaki başka bir mevki oyuncusu kaleye geçsin mi?")
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

        sahadakiler.remove(cikan)
        yedekler.remove(giren)

        sahadakiler.add(giren)
        yedekler.add(cikan)

        Toast.makeText(this, "🔄 ${cikan.isim} çıktı, ${giren.isim} girdi!", Toast.LENGTH_SHORT).show()
        takimSekmesiGuncelle()
    }

    // ─── KAYDETME VE ÇIKIŞ ─────────────────────────────────────────────────────

    private fun showCikisKayitDialog() {
        val currentCihazId = KadroRepository.cihazIdAl(this)
        val isYonetici = ligOlusturanId.isEmpty() || ligOlusturanId == currentCihazId
        if (!isYonetici) {
            // Yönetici olmayan kullanıcılar için maç kaydetme seçeneği gerekmez, doğrudan çıkabilir
            finish()
            return
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("⚽ Maç Kaydı")
            .setMessage("Maçta yapılan değişiklikler kaydedilsin mi?\n\n(Kaydettiğinizde oyuncuların istatistikleri kariyerlerine işlenecektir)")
            .setPositiveButton("💾 Kaydet ve Çık") { _, _ ->
                maciBitirVeKaydet()
            }
            .setNegativeButton("❌ Kaydetmeden Çık") { _, _ ->
                finish()
            }
            .setNeutralButton("İptal", null)
            .show()
    }

    private fun maciBitirVeKaydet() {
        if (macId != -1 && ligAdi.isNotEmpty()) {
            val tumOyuncular = (evSahadakiler + evYedekler + depSahadakiler + depYedekler).distinctBy {
                if (it.username.isNotEmpty()) it.username else it.isim
            }

            val guncelOyuncuStats = mutableMapOf<String, Map<String, Int>>()
            val macBilgisi = "$evTakim $evSkor - $depSkor $depTakim"
            val yeniMacMi = !macDahaOnceKaydedildiMi

            for (oyuncu in tumOyuncular) {
                val u = if (oyuncu.username.isNotEmpty()) oyuncu.username else oyuncu.isim
                val cleanU = u.removePrefix("@").lowercase().trim()

                // Bu maçtaki güncel oyuncu istatistikleri
                guncelOyuncuStats[cleanU] = mapOf(
                    "gol" to oyuncu.gol,
                    "sari" to oyuncu.sari,
                    "kirmizi" to oyuncu.kirmizi
                )

                // Önceki oturumdaki değerleri al (yoksa 0)
                val onceki = oncekiOyuncuStats[cleanU] ?: emptyMap()
                val eskiGol = onceki["gol"] ?: 0
                val eskiSari = onceki["sari"] ?: 0
                val eskiKirmizi = onceki["kirmizi"] ?: 0

                val golFarki = oyuncu.gol - eskiGol
                val sariFarki = oyuncu.sari - eskiSari
                val kirmiziFarki = oyuncu.kirmizi - eskiKirmizi

                // Kariyer istatistiklerini DELTA ile güncelle
                // yeniMacMi == false ise toplamMac ASLA artmaz!
                // formatTipi == "TEKIL_MAC" ise ligler listesine ASLA ekleme yapılmaz!
                KadroRepository.oyuncuKariyerDeltaGuncelle(
                    username = cleanU,
                    yeniMacMi = yeniMacMi,
                    golFarki = golFarki,
                    sariFarki = sariFarki,
                    kirmiziFarki = kirmiziFarki,
                    macBilgisi = macBilgisi,
                    organizasyonAdi = ligAdi,
                    formatTipi = formatTipi
                )
            }

            // Maç sonucunu ve oyuncu bazlı maç istatistiklerini kaydet
            KadroRepository.macKaydet(
                ligAdi  = ligAdi,
                macId   = macId,
                takim1  = evTakim,
                takim2  = depTakim,
                hafta   = hafta,
                skor1   = evSkor,
                skor2   = depSkor,
                olaylar = emptyList(),
                oyuncuIstatistikleri = guncelOyuncuStats
            )

            macDahaOnceKaydedildiMi = true
        }

        val donenIntent = Intent().apply {
            putExtra("MAC_ID",   macId)
            putExtra("EV_SKOR",  evSkor)
            putExtra("DEP_SKOR", depSkor)
        }
        setResult(Activity.RESULT_OK, donenIntent)
        finish()
    }

    // ─── INSTAGRAM TARZI OYUNCU ARA VE SEÇ DİYALOĞU ───────────────────────────

    private fun showOyuncuAraSecDialog(
        haricTutulanUsernameler: Set<String> = emptySet(),
        onSecildi: (Oyuncu) -> Unit
    ) {
        val dialogBinding = DialogOyuncuAraSecBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        val adapter = OyuncuAramaAdapter(emptyList()) { secilenOyuncu ->
            onSecildi(secilenOyuncu)
            dialog.dismiss()
        }
        dialogBinding.rvAramaSonuclari.layoutManager = LinearLayoutManager(this)
        dialogBinding.rvAramaSonuclari.adapter = adapter

        val listeyiFiltreleVeGoster = { liste: List<Oyuncu> ->
            val temiz = if (haricTutulanUsernameler.isNotEmpty()) {
                liste.filter {
                    val clean = (if (it.username.isNotEmpty()) it.username else it.isim).removePrefix("@").lowercase().trim()
                    !haricTutulanUsernameler.contains(clean)
                }
            } else liste
            runOnUiThread {
                adapter.listeyiGuncelle(temiz)
                dialogBinding.tvAramaBosSonuc.visibility = if (temiz.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        // İlk açılışta tüm oyuncuları listele
        KadroRepository.oyuncuAra("") { liste ->
            listeyiFiltreleVeGoster(liste)
        }

        dialogBinding.etOyuncuAramaInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                val q = s?.toString()?.trim() ?: ""
                dialogBinding.btnAramaTemizle.visibility = if (q.isNotEmpty()) View.VISIBLE else View.GONE
                KadroRepository.oyuncuAra(q) { filtreli ->
                    listeyiFiltreleVeGoster(filtreli)
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        dialogBinding.btnAramaTemizle.setOnClickListener {
            dialogBinding.etOyuncuAramaInput.setText("")
        }

        dialogBinding.btnAramaKapat.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }
}
