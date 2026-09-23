package com.example.sahamatik_lig.view

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sahamatik_lig.R
import com.example.sahamatik_lig.databinding.ActivityOyuncuDetailBinding
import com.example.sahamatik_lig.model.KadroRepository
import com.example.sahamatik_lig.model.Oyuncu
import com.example.sahamatik_lig.util.ImagePickerHelper
import com.example.sahamatik_lig.util.ProfilFotoHelper

class OyuncuDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOyuncuDetailBinding
    private lateinit var imagePickerHelper: ImagePickerHelper
    private var aktifProfilFotoUri: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOyuncuDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val username = intent.getStringExtra("USERNAME") ?: ""
        val isim = intent.getStringExtra("ISIM") ?: ""
        val mevki = intent.getStringExtra("MEVKI") ?: "Forvet"
        val takimAdi = intent.getStringExtra("TAKIM_ADI") ?: ""
        val ligAdi = intent.getStringExtra("LIG_ADI") ?: ""
        val gelenOyuncu = intent.getSerializableExtra("OYUNCU") as? Oyuncu

        val aktifUsername = when {
            username.isNotEmpty() -> username
            gelenOyuncu?.username?.isNotEmpty() == true -> gelenOyuncu.username
            isim.isNotEmpty() -> isim
            gelenOyuncu?.isim?.isNotEmpty() == true -> gelenOyuncu.isim
            else -> "oyuncu"
        }.removePrefix("@")

        val aktifIsim = when {
            isim.isNotEmpty() -> isim
            gelenOyuncu?.isim?.isNotEmpty() == true -> gelenOyuncu.isim
            else -> aktifUsername
        }

        val aktifMevki = when {
            mevki.isNotEmpty() -> mevki
            gelenOyuncu?.mevki?.isNotEmpty() == true -> gelenOyuncu.mevki
            else -> "Forvet"
        }

        val aktifTakim = when {
            takimAdi.isNotEmpty() -> takimAdi
            gelenOyuncu?.takimAdi?.isNotEmpty() == true -> gelenOyuncu.takimAdi
            else -> ""
        }

        val aktifLig = when {
            ligAdi.isNotEmpty() -> ligAdi
            gelenOyuncu?.ligAdi?.isNotEmpty() == true -> gelenOyuncu.ligAdi
            else -> ""
        }

        aktifProfilFotoUri = gelenOyuncu?.profilFotoUri ?: ""
        val yerel = KadroRepository.yerelProfilGetir(this)
        if (yerel != null && yerel.username.removePrefix("@").equals(aktifUsername, ignoreCase = true) && yerel.profilFotoUri.isNotEmpty()) {
            aktifProfilFotoUri = yerel.profilFotoUri
        }

        // Yalnızca kendi profilini düzenleyebilir; başkasının (örn: Mahmud) profili salt okunurdur!
        val yerelKullaniciMi = yerel != null && (
            yerel.username.removePrefix("@").equals(aktifUsername, ignoreCase = true) ||
            yerel.isim.equals(aktifIsim, ignoreCase = true)
        )

        if (yerelKullaniciMi) {
            // Yüz Doğrulamalı Fotoğraf Seçici Kurulumu (Yalnızca kendi profili için)
            imagePickerHelper = ImagePickerHelper(this) { selectedUri ->
                val base64 = ProfilFotoHelper.uriToBase64(this, selectedUri) ?: selectedUri.toString()
                aktifProfilFotoUri = base64

                val bmp = ProfilFotoHelper.gorselYukle(this, base64)
                if (bmp != null) {
                    binding.ivProfilFoto.visibility = View.VISIBLE
                    binding.ivProfilFoto.setImageBitmap(bmp)
                    binding.tvProfilHarf.visibility = View.GONE
                }

                // Cihazdaki yerel profil kullanıcısı ise yerel hafızaya kaydet
                val mevcutYerel = KadroRepository.yerelProfilGetir(this)
                if (mevcutYerel != null) {
                    KadroRepository.yerelProfilKaydet(
                        this,
                        mevcutYerel.username,
                        mevcutYerel.isim,
                        mevcutYerel.mevki,
                        mevcutYerel.formaNo,
                        profilFotoUri = base64
                    )
                }

                // Firestore oyuncu profillerine kaydet (Bulut üzerinden tüm telefonlara anında akar!)
                KadroRepository.oyuncuProfilFotoGuncelle(aktifUsername, base64)
                Toast.makeText(this, "✅ Profil fotoğrafınız buluta kaydedildi!", Toast.LENGTH_SHORT).show()
            }

            binding.containerProfilFoto.setOnClickListener {
                imagePickerHelper.pickFaceImage()
            }
        } else {
            binding.containerProfilFoto.isClickable = false
            binding.containerProfilFoto.isFocusable = false
        }

        // İlk Arayüzü Hızlıca Doldur
        arayuzuGuncelle(
            isim = aktifIsim,
            username = aktifUsername,
            mevki = aktifMevki,
            takim = aktifTakim,
            lig = aktifLig,
            toplamMac = gelenOyuncu?.toplamMac ?: 0,
            toplamGol = gelenOyuncu?.let { if (it.toplamGol > 0) it.toplamGol else it.gol } ?: 0,
            toplamSari = gelenOyuncu?.let { if (it.toplamSari > 0) it.toplamSari else it.sari } ?: 0,
            toplamKirmizi = gelenOyuncu?.let { if (it.toplamKirmizi > 0) it.toplamKirmizi else it.kirmizi } ?: 0,
            gecmisMaclar = gelenOyuncu?.katildigiMaclar ?: emptyList(),
            gecmisLigler = gelenOyuncu?.katildigiLigler ?: emptyList(),
            profilFotoUri = aktifProfilFotoUri
        )

        binding.btnGeri.setOnClickListener { finish() }

        // Firestore'dan Tam Kariyer Verilerini Çek
        KadroRepository.oyuncuProfiliGetir(aktifUsername) { kayitliOyuncu ->
            runOnUiThread {
                if (kayitliOyuncu != null) {
                    if (kayitliOyuncu.profilFotoUri.isNotEmpty()) {
                        aktifProfilFotoUri = kayitliOyuncu.profilFotoUri
                    }
                    arayuzuGuncelle(
                        isim = if (kayitliOyuncu.isim.isNotEmpty()) kayitliOyuncu.isim else aktifIsim,
                        username = kayitliOyuncu.username,
                        mevki = if (kayitliOyuncu.mevki.isNotEmpty()) kayitliOyuncu.mevki else aktifMevki,
                        takim = if (kayitliOyuncu.takimAdi.isNotEmpty()) kayitliOyuncu.takimAdi else aktifTakim,
                        lig = aktifLig,
                        toplamMac = kayitliOyuncu.toplamMac,
                        toplamGol = kayitliOyuncu.toplamGol,
                        toplamSari = kayitliOyuncu.toplamSari,
                        toplamKirmizi = kayitliOyuncu.toplamKirmizi,
                        gecmisMaclar = kayitliOyuncu.katildigiMaclar,
                        gecmisLigler = kayitliOyuncu.katildigiLigler,
                        profilFotoUri = aktifProfilFotoUri
                    )
                } else {
                    // Henüz profili yoksa bu oyuncu için başlangıç profilini kaydet
                    val yeniProfil = Oyuncu(
                        username = aktifUsername,
                        isim = aktifIsim,
                        mevki = aktifMevki,
                        takimAdi = aktifTakim,
                        ligAdi = aktifLig,
                        formaNo = 10,
                        profilFotoUri = aktifProfilFotoUri,
                        toplamMac = if (gelenOyuncu != null && (gelenOyuncu.gol > 0 || gelenOyuncu.toplamMac > 0)) 1 else 0,
                        toplamGol = gelenOyuncu?.gol ?: 0,
                        toplamSari = gelenOyuncu?.sari ?: 0,
                        toplamKirmizi = gelenOyuncu?.kirmizi ?: 0,
                        katildigiLigler = if (aktifLig.isNotEmpty()) listOf(aktifLig) else emptyList()
                    )
                    KadroRepository.oyuncuProfiliKaydet(yeniProfil)
                }
            }
        }
    }

    private fun arayuzuGuncelle(
        isim: String,
        username: String,
        mevki: String,
        takim: String,
        lig: String,
        toplamMac: Int,
        toplamGol: Int,
        toplamSari: Int,
        toplamKirmizi: Int,
        gecmisMaclar: List<String>,
        gecmisLigler: List<String>,
        profilFotoUri: String = ""
    ) {
        binding.tvProfilIsim.text = isim
        binding.tvProfilUsername.text = "@${username.removePrefix("@")}"
        binding.tvProfilHarf.text = isim.firstOrNull()?.uppercase() ?: username.firstOrNull()?.uppercase() ?: "?"

        val bmp = ProfilFotoHelper.gorselYukle(this, profilFotoUri)
        if (bmp != null) {
            binding.ivProfilFoto.visibility = View.VISIBLE
            binding.ivProfilFoto.setImageBitmap(bmp)
            binding.tvProfilHarf.visibility = View.GONE
        } else {
            binding.ivProfilFoto.visibility = View.GONE
            binding.tvProfilHarf.visibility = View.VISIBLE
        }

        val mevkiEmoji = when (mevki.trim().lowercase()) {
            "kaleci" -> "🧤 Kaleci"
            "defans" -> "🛡️ Defans"
            "orta saha" -> "⚡ Orta Saha"
            "forvet" -> "⚽ Forvet"
            else -> "⚽ $mevki"
        }
        binding.tvProfilMevki.text = mevkiEmoji

        val takimBilgi = when {
            takim.isNotEmpty() && lig.isNotEmpty() -> "• $takim ($lig)"
            takim.isNotEmpty() -> "• $takim"
            lig.isNotEmpty() -> "• $lig"
            else -> ""
        }
        binding.tvProfilTakim.text = takimBilgi
        binding.tvProfilTakim.visibility = if (takimBilgi.isNotEmpty()) View.VISIBLE else View.GONE

        // İstatistikler
        binding.tvToplamMac.text = toplamMac.toString()
        binding.tvToplamGol.text = toplamGol.toString()
        binding.tvToplamSari.text = toplamSari.toString()
        binding.tvToplamKirmizi.text = toplamKirmizi.toString()

        // 1. LİGLER & TURNUVALAR (Sadece gerçek ligler, tekil maçlar filtrelenir)
        binding.containerLigler.removeAllViews()
        val gecerliLigler = gecmisLigler.filter { ligAdi ->
            ligAdi.isNotBlank() &&
            !ligAdi.matches(Regex(".*\\d+\\s*-\\s*\\d+.*")) &&
            !ligAdi.contains("halısaha", ignoreCase = true) &&
            !ligAdi.contains("halisaha", ignoreCase = true)
        }.distinct()

        if (gecerliLigler.isEmpty()) {
            binding.tvLiglerBos.visibility = View.VISIBLE
            binding.containerLigler.addView(binding.tvLiglerBos)
        } else {
            binding.tvLiglerBos.visibility = View.GONE
            gecerliLigler.forEach { ligAdi ->
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setBackgroundResource(R.drawable.bg_lig_kart)
                    setPadding(32, 24, 32, 24)
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    lp.bottomMargin = 14
                    layoutParams = lp
                    gravity = android.view.Gravity.CENTER_VERTICAL
                }

                val badge = TextView(this).apply {
                    text = "🏆 LİG"
                    setTextColor(0xFF0F3D2E.toInt())
                    textSize = 12f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                }

                val title = TextView(this).apply {
                    text = ligAdi
                    setTextColor(0xFF16201C.toInt())
                    textSize = 13.5f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setPadding(24, 0, 0, 0)
                }

                card.addView(badge)
                card.addView(title)
                binding.containerLigler.addView(card)
            }
        }

        // 2. NORMAL / TEKİL MAÇLAR
        binding.containerMaclar.removeAllViews()

        // Akıllı Birleştirme: "Sınır", "0 - 0", "0 - 1" gibi parçalı kayıtları tek bir güncel "Sınır (Bilgisayar 0 - 1 Endüstri)" haline getirir
        val hamMaclar = gecmisMaclar.filter { it.isNotBlank() }.distinct()
        val birlesikFormatlilar = hamMaclar.filter { it.contains("(") && it.contains(")") }
        val ciplakSkorlular = hamMaclar.filter { it.matches(Regex(".*\\d+\\s*-\\s*\\d+.*")) && !it.contains("(") }
        val sadeceBasliklar = hamMaclar.filter { !it.matches(Regex(".*\\d+\\s*-\\s*\\d+.*")) && !it.contains("(") }

        val eklenenler = mutableListOf<String>()
        eklenenler.addAll(birlesikFormatlilar)

        for (skor in ciplakSkorlular) {
            val baslik = sadeceBasliklar.firstOrNull()
            val formatli = if (baslik != null) "$baslik ($skor)" else skor
            val prefix = if (formatli.contains("(")) formatli.substringBefore("(") else formatli.substringBeforeLast("-").trim()
            val mevcutIndex = eklenenler.indexOfFirst { it.startsWith(prefix) }
            if (mevcutIndex != -1) {
                eklenenler[mevcutIndex] = formatli
            } else {
                eklenenler.add(formatli)
            }
        }

        for (b in sadeceBasliklar) {
            if (!eklenenler.any { it.startsWith("$b (") || it == b }) {
                eklenenler.add(b)
            }
        }

        val gecerliMaclar = eklenenler.distinct()

        if (gecerliMaclar.isEmpty()) {
            binding.tvMaclarBos.visibility = View.VISIBLE
            binding.containerMaclar.addView(binding.tvMaclarBos)
        } else {
            binding.tvMaclarBos.visibility = View.GONE
            gecerliMaclar.forEach { macAdi ->
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setBackgroundResource(R.drawable.bg_lig_kart)
                    setPadding(32, 24, 32, 24)
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    lp.bottomMargin = 14
                    layoutParams = lp
                    gravity = android.view.Gravity.CENTER_VERTICAL
                }

                val badge = TextView(this).apply {
                    text = "⚽ MAÇ"
                    setTextColor(0xFF1E7A4D.toInt())
                    textSize = 12f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                }

                val title = TextView(this).apply {
                    text = macAdi
                    setTextColor(0xFF16201C.toInt())
                    textSize = 13.5f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setPadding(24, 0, 0, 0)
                }

                card.addView(badge)
                card.addView(title)
                binding.containerMaclar.addView(card)
            }
        }
    }
}
