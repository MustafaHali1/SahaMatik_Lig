package com.example.sahamatik_lig.view

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.Toast
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sahamatik_lig.adapter.OyuncuAramaAdapter
import com.example.sahamatik_lig.databinding.ActivityTakimDetailBinding
import com.example.sahamatik_lig.databinding.DialogOyuncuAraSecBinding
import com.example.sahamatik_lig.databinding.DialogSilmeOnayiBinding
import com.example.sahamatik_lig.databinding.ItemOyuncuRosterBinding
import com.example.sahamatik_lig.model.KadroRepository
import com.example.sahamatik_lig.model.Oyuncu
import com.example.sahamatik_lig.util.ImagePickerHelper
import com.google.firebase.firestore.ListenerRegistration
import java.io.ByteArrayOutputStream
import android.util.Base64

class TakimDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTakimDetailBinding
    private var takimAdi: String = ""
    private var ligAdi: String = ""
    private var firestoreListener: ListenerRegistration? = null
    private lateinit var imagePicker: ImagePickerHelper
    private var mevcutOyuncular: List<Oyuncu> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTakimDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        takimAdi = intent.getStringExtra("TAKIM_ADI") ?: ""
        ligAdi = intent.getStringExtra("LIG_ADI") ?: ""

        imagePicker = ImagePickerHelper(this) { uri ->
            binding.ivTakimLogo.setImageURI(uri)
            ImagePickerHelper.uriKaydet(this, "logo_${ligAdi}_$takimAdi", uri.toString())
            logoFirestoreKaydet(uri)
        }

        binding.tvTakimAdi.text = takimAdi
        binding.tvLigAdi.text = ligAdi

        ImagePickerHelper.uriGetir(this, "logo_${ligAdi}_$takimAdi")?.let { uri ->
            binding.ivTakimLogo.setImageURI(uri)
        }

        binding.ivTakimLogo.setOnClickListener { imagePicker.pickImage() }

        binding.btnGeri.setOnClickListener { finish() }

        canliKadroTakibiBaslat()

        binding.btnOyuncuEkle.setOnClickListener {
            val haricTutulanlar = mevcutOyuncular.map {
                it.username.removePrefix("@").lowercase().trim()
            }.toSet()
            showOyuncuAraSecDialog(haricTutulanUsernameler = haricTutulanlar) { secilenOyuncu ->
                KadroRepository.takimaOyuncuEkleKontrollu(
                    ligAdi = ligAdi,
                    takimAdi = takimAdi,
                    oyuncu = secilenOyuncu,
                    maxKontenjan = 10,
                    onSuccess = {
                        if (!isFinishing && !isDestroyed) {
                            Toast.makeText(this, "${secilenOyuncu.isim} (${secilenOyuncu.username}) kadroya eklendi! ⚽", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onLimitDolu = {
                        if (!isFinishing && !isDestroyed) {
                            Toast.makeText(this, "⚠️ $takimAdi takımı 10 kişilik maksimum kontenjana ulaştı!", Toast.LENGTH_LONG).show()
                        }
                    },
                    onError = { e ->
                        if (!isFinishing && !isDestroyed) {
                            Toast.makeText(this, e.message ?: "Oyuncu eklenemedi", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }

        binding.btnTakimDavetLinki.setOnClickListener {
            KadroRepository.davetLinkiPaylas(this, ligAdi, takimAdi)
        }
    }

    private fun logoFirestoreKaydet(uri: Uri) {
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos)
            val base64String = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
            KadroRepository.takimLogosuGuncelle(
                ligAdi, takimAdi, "logoBase64",
                mapOf("logoBase64" to base64String)
            )
            Toast.makeText(this, "Logo guncellendi!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Logo kaydedilemedi", Toast.LENGTH_SHORT).show()
        }
    }

    private fun canliKadroTakibiBaslat() {
        firestoreListener = KadroRepository.takimOyunculariniCanliDinle(
            ligAdi = ligAdi,
            takimAdi = takimAdi,
            onUpdate = { oyuncular ->
                mevcutOyuncular = oyuncular
                kadroListesiniCiz(oyuncular)
            },
            onError = { e -> Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_SHORT).show() }
        )
    }

    private fun kadroListesiniCiz(oyuncular: List<Oyuncu>) {
        binding.containerKaleciler.removeAllViews()
        binding.containerDefanslar.removeAllViews()
        binding.containerOrtaSaha.removeAllViews()
        binding.containerForvet.removeAllViews()

        for (oyuncu in oyuncular) {
            val hedefContainer = when (oyuncu.mevki.trim()) {
                "Kaleci" -> binding.containerKaleciler
                "Defans" -> binding.containerDefanslar
                "Orta Saha" -> binding.containerOrtaSaha
                "Forvet" -> binding.containerForvet
                else -> binding.containerOrtaSaha
            }

            val itemBinding = ItemOyuncuRosterBinding.inflate(layoutInflater, hedefContainer, false)
            itemBinding.tvOyuncuIsim.text = oyuncu.isim
            itemBinding.tvOyuncuHarf.text = oyuncu.isim.firstOrNull()?.uppercase() ?: "?"

            // Kisa tiklama - OyuncuDetailActivity'ye git
            itemBinding.root.setOnClickListener {
                val intent = Intent(this, OyuncuDetailActivity::class.java).apply {
                    putExtra("OYUNCU", oyuncu)
                    putExtra("USERNAME", oyuncu.username)
                    putExtra("ISIM", oyuncu.isim)
                    putExtra("MEVKI", oyuncu.mevki)
                    putExtra("TAKIM_ADI", takimAdi)
                    putExtra("LIG_ADI", ligAdi)
                }
                startActivity(intent)
            }

            // Sil butonu tiklama
            itemBinding.ivOyuncuSil.setOnClickListener {
                showOyuncuSilDialog(oyuncu)
            }

            hedefContainer.addView(itemBinding.root)
        }
    }

    // Oyuncu bilgi dialogu — Ortak Tasarımlı
    private fun showOyuncuBilgisiDialog(oyuncu: Oyuncu) {
        val dialogBinding = com.example.sahamatik_lig.databinding.DialogOyuncuBilgiBinding.inflate(layoutInflater)

        dialogBinding.tvOyuncuIsim.text = oyuncu.isim
        dialogBinding.tvOyuncuMevki.text = "${oyuncu.mevki} • $takimAdi"
        dialogBinding.tvOyuncuHarf.text = oyuncu.isim.firstOrNull()?.uppercase() ?: "?"
        dialogBinding.tvGolSayisi.text = oyuncu.gol.toString()
        dialogBinding.tvSariSayisi.text = oyuncu.sari.toString()
        dialogBinding.tvKirmiziSayisi.text = oyuncu.kirmizi.toString()

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnKapat.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    // Oyuncu silme dialogu - Binding ile
    private fun showOyuncuSilDialog(oyuncu: Oyuncu) {
        val dialogBinding = DialogSilmeOnayiBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.tvSilmeBaslik.text = "Oyuncu Sil"
        dialogBinding.tvSilmeAciklama.text =
            "${oyuncu.isim} isimli oyuncuyu silmek istediginize emin misiniz?\n\nBu islem geri alinamaz!"

        dialogBinding.btnSilOnay.setOnClickListener {
            dialog.dismiss()
            KadroRepository.oyuncuSil(
                ligAdi, takimAdi, oyuncu.id,
                onSuccess = {
                    if (!isFinishing && !isDestroyed) {
                        Toast.makeText(this, "${oyuncu.isim} silindi", Toast.LENGTH_SHORT).show()
                    }
                },
                onError = { e ->
                    if (!isFinishing && !isDestroyed) {
                        Toast.makeText(this, "Silme basarisiz: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        dialogBinding.btnSilIptal.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
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

        val aramaAdapter = OyuncuAramaAdapter(emptyList()) { secilenOyuncu ->
            onSecildi(secilenOyuncu)
            dialog.dismiss()
        }
        dialogBinding.rvAramaSonuclari.layoutManager = LinearLayoutManager(this)
        dialogBinding.rvAramaSonuclari.adapter = aramaAdapter

        val listeyiFiltreleVeGoster = { liste: List<Oyuncu> ->
            val temiz = if (haricTutulanUsernameler.isNotEmpty()) {
                liste.filter {
                    val clean = it.username.removePrefix("@").lowercase().trim()
                    !haricTutulanUsernameler.contains(clean)
                }
            } else liste
            runOnUiThread {
                aramaAdapter.listeyiGuncelle(temiz)
                dialogBinding.tvAramaBosSonuc.visibility = if (temiz.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        // İlk açılışta tüm hazır profilleri listele
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

    override fun onDestroy() {
        super.onDestroy()
        firestoreListener?.remove()
    }
}
