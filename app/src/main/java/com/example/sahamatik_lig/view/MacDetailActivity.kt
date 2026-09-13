package com.example.sahamatik_lig.view

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.sahamatik_lig.databinding.ActivityMacDetailBinding
import com.example.sahamatik_lig.databinding.ItemSahaOyuncuBinding
import com.example.sahamatik_lig.databinding.ItemYedekOyuncuBinding
import com.example.sahamatik_lig.model.KadroRepository
import com.example.sahamatik_lig.model.Oyuncu
import com.example.sahamatik_lig.util.ImagePickerHelper

class MacDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMacDetailBinding

    private var evTakim: String = "Pendik Spor"
    private var depTakim: String = "Sahil FC"

    private var evSkor: Int = 0
    private var depSkor: Int = 0

    // Sahadaki ve kulübedeki gerçek oyuncu listeleri (boşsa boş kalır)
    private val evSahadakiOyuncular = mutableListOf<Oyuncu>()
    private val evYedekOyuncular = mutableListOf<Oyuncu>()

    private val depSahadakiOyuncular = mutableListOf<Oyuncu>()
    private val depYedekOyuncular = mutableListOf<Oyuncu>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMacDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        evTakim = intent.getStringExtra("EV_TAKIM") ?: "Pendik Spor"
        depTakim = intent.getStringExtra("DEP_TAKIM") ?: "Sahil FC"

        skorGuncelle()

        binding.tvEvAdi.text = evTakim
        binding.tvDepAdi.text = depTakim

        binding.tvYedekEvBaslik.text = "$evTakim Yedekler"
        binding.tvYedekDepBaslik.text = "$depTakim Yedekler"

        // Takım logolarını getir
        ImagePickerHelper.uriGetir(this, "logo_$evTakim")?.let { binding.ivEvLogo.setImageURI(it) }
        ImagePickerHelper.uriGetir(this, "logo_$depTakim")?.let { binding.ivDepLogo.setImageURI(it) }

        binding.tvBack.setOnClickListener { finish() }

        // Sadece gerçek oyuncuları yükle
        kadrolariHazirla()
        ekraniCiz()
    }

    private fun skorGuncelle() {
        binding.tvSkor.text = "$evSkor - $depSkor"
    }

    private fun kadrolariHazirla() {
        // ARTIK SAHTE OYUNCU ÜRETİLMİYOR: Ne kayıtlıysa sadece o gelir, boşsa liste boştur.
        val evTum = KadroRepository.takimOyunculariniGetir(evTakim)
        val depTum = KadroRepository.takimOyunculariniGetir(depTakim)

        // Dışarıdan format geldiyse o alınır, gelmediyse mevcut kadro kadar alınır
        val sahaKapasitesi = intent.getIntExtra("KISI_SAYISI", if (evTum.size >= 11) 11 else evTum.size)

        evSahadakiOyuncular.clear()
        evSahadakiOyuncular.addAll(evTum.take(sahaKapasitesi))
        evYedekOyuncular.clear()
        evYedekOyuncular.addAll(evTum.drop(sahaKapasitesi))

        depSahadakiOyuncular.clear()
        depSahadakiOyuncular.addAll(depTum.take(sahaKapasitesi))
        depYedekOyuncular.clear()
        depYedekOyuncular.addAll(depTum.drop(sahaKapasitesi))
    }

    private fun ekraniCiz() {
        cizEvSahibiSahasi()
        cizDeplasmanSahasi()
        cizYedekKulubesi(binding.containerEvYedekler, evYedekOyuncular, isEvSahibi = true)
        cizYedekKulubesi(binding.containerDepYedekler, depYedekOyuncular, isEvSahibi = false)
    }

    private fun cizEvSahibiSahasi() {
        binding.evKaleciRow.removeAllViews()
        binding.evDefansRow.removeAllViews()
        binding.evOrtaRow.removeAllViews()
        binding.evForvetRow.removeAllViews()

        if (evSahadakiOyuncular.isEmpty()) return

        val kaleci = evSahadakiOyuncular.filter { it.mevki.equals("Kaleci", ignoreCase = true) }
        val digerleri = evSahadakiOyuncular.filterNot { it.mevki.equals("Kaleci", ignoreCase = true) }

        val hatPlani = when (evSahadakiOyuncular.size) {
            in 1..5 -> Triple(1, 2, 1)
            in 6..8 -> Triple(2, 3, 1)
            else -> Triple(4, 4, 2)
        }

        val defanslar = digerleri.take(hatPlani.first)
        val ortalar = digerleri.drop(hatPlani.first).take(hatPlani.second)
        val forvetler = digerleri.drop(hatPlani.first + hatPlani.second)

        satiraOyuncuYerlestir(binding.evKaleciRow, if (kaleci.isNotEmpty()) kaleci else digerleri.take(1), isEvSahibi = true)
        satiraOyuncuYerlestir(binding.evDefansRow, defanslar, isEvSahibi = true)
        satiraOyuncuYerlestir(binding.evOrtaRow, ortalar, isEvSahibi = true)
        satiraOyuncuYerlestir(binding.evForvetRow, forvetler, isEvSahibi = true)
    }

    private fun cizDeplasmanSahasi() {
        binding.depForvetRow.removeAllViews()
        binding.depOrtaRow.removeAllViews()
        binding.depDefansRow.removeAllViews()
        binding.depKaleciRow.removeAllViews()

        if (depSahadakiOyuncular.isEmpty()) return

        val kaleci = depSahadakiOyuncular.filter { it.mevki.equals("Kaleci", ignoreCase = true) }
        val digerleri = depSahadakiOyuncular.filterNot { it.mevki.equals("Kaleci", ignoreCase = true) }

        val hatPlani = when (depSahadakiOyuncular.size) {
            in 1..5 -> Triple(1, 2, 1)
            in 6..8 -> Triple(2, 3, 1)
            else -> Triple(4, 4, 2)
        }

        val defanslar = digerleri.take(hatPlani.first)
        val ortalar = digerleri.drop(hatPlani.first).take(hatPlani.second)
        val forvetler = digerleri.drop(hatPlani.first + hatPlani.second)

        satiraOyuncuYerlestir(binding.depForvetRow, forvetler, isEvSahibi = false)
        satiraOyuncuYerlestir(binding.depOrtaRow, ortalar, isEvSahibi = false)
        satiraOyuncuYerlestir(binding.depDefansRow, defanslar, isEvSahibi = false)
        satiraOyuncuYerlestir(binding.depKaleciRow, if (kaleci.isNotEmpty()) kaleci else digerleri.take(1), isEvSahibi = false)
    }

    private fun satiraOyuncuYerlestir(row: LinearLayout, oyuncular: List<Oyuncu>, isEvSahibi: Boolean) {
        row.removeAllViews()
        for ((index, oyuncu) in oyuncular.withIndex()) {
            val itemBinding = ItemSahaOyuncuBinding.inflate(layoutInflater, row, false)
            itemBinding.tvSirtNo.text = "${index + 1}"
            itemBinding.tvOyuncuAdi.text = oyuncu.isim

            // Tıklama çökmesini önleyen stabil menü çağrısı
            itemBinding.root.setOnClickListener {
                showOlaySecimMenusu(oyuncu, isEvSahibi, isYedek = false)
            }
            row.addView(itemBinding.root)
        }
    }

    private fun cizYedekKulubesi(container: LinearLayout, yedekler: List<Oyuncu>, isEvSahibi: Boolean) {
        container.removeAllViews()
        for ((index, oyuncu) in yedekler.withIndex()) {
            val itemBinding = ItemYedekOyuncuBinding.inflate(layoutInflater, container, false)
            itemBinding.tvYedekNo.text = "${index + 12}"
            itemBinding.tvYedekIsim.text = oyuncu.isim
            itemBinding.tvYedekMevki.text = oyuncu.mevki.take(3).uppercase()

            itemBinding.root.setOnClickListener {
                showOlaySecimMenusu(oyuncu, isEvSahibi, isYedek = true)
            }
            container.addView(itemBinding.root)
        }
    }

    // ÇÖKMEYİ ENGELLEYEN VE ASLA PATLAMAYAN OLAY SEÇİM PENCERESİ
    private fun showOlaySecimMenusu(oyuncu: Oyuncu, isEvSahibi: Boolean, isYedek: Boolean) {
        val takimAdi = if (isEvSahibi) evTakim else depTakim
        val secenekler = arrayOf("⚽ Gol Ekle (+1)", "🟨 Sarı Kart", "🟥 Kırmızı Kart", "🔄 Oyuncu Değiştir")

        AlertDialog.Builder(this)
            .setTitle("${oyuncu.isim} ($takimAdi)")
            .setItems(secenekler) { _, index ->
                when (index) {
                    0 -> { // Gol
                        if (isEvSahibi) evSkor++ else depSkor++
                        skorGuncelle()
                        Toast.makeText(this, "⚽ GOL! ${oyuncu.isim}", Toast.LENGTH_SHORT).show()
                    }
                    1 -> Toast.makeText(this, "🟨 Sarı kart verildi: ${oyuncu.isim}", Toast.LENGTH_SHORT).show()
                    2 -> Toast.makeText(this, "🟥 Kırmızı kart verildi: ${oyuncu.isim}", Toast.LENGTH_SHORT).show()
                    3 -> showOyuncuDegistirDialog(oyuncu, isEvSahibi, isYedek)
                }
            }
            .setNegativeButton("Kapat", null)
            .show()
    }

    private fun showOyuncuDegistirDialog(secilenOyuncu: Oyuncu, isEvSahibi: Boolean, isYedek: Boolean) {
        val sahadakiListe = if (isEvSahibi) evSahadakiOyuncular else depSahadakiOyuncular
        val yedekListe = if (isEvSahibi) evYedekOyuncular else depYedekOyuncular
        val hedefListe = if (isYedek) sahadakiListe else yedekListe

        if (hedefListe.isEmpty()) {
            Toast.makeText(this, "Değiştirilecek başka oyuncu yok!", Toast.LENGTH_SHORT).show()
            return
        }

        val oyuncuIsimleri = hedefListe.map { "${it.isim} (${it.mevki})" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("${secilenOyuncu.isim} yerine kimi alacaksın?")
            .setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, oyuncuIsimleri)) { _, which ->
                val takasOyuncusu = hedefListe[which]

                if (isYedek) {
                    yedekListe.remove(secilenOyuncu)
                    sahadakiListe.remove(takasOyuncusu)
                    sahadakiListe.add(secilenOyuncu)
                    yedekListe.add(takasOyuncusu)
                } else {
                    sahadakiListe.remove(secilenOyuncu)
                    yedekListe.remove(takasOyuncusu)
                    yedekListe.add(secilenOyuncu)
                    sahadakiListe.add(takasOyuncusu)
                }

                ekraniCiz()
                Toast.makeText(this, "🔄 Oyuncu değişikliği yapıldı!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("İptal", null)
            .show()
    }
}