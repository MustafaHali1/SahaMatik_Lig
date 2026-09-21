package com.example.sahamatik_lig.view

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sahamatik_lig.databinding.ActivityTakimDetailBinding
import com.example.sahamatik_lig.databinding.DialogOyuncuEkleBinding
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

        binding.btnOyuncuEkle.setOnClickListener { showOyuncuEkleDialog() }
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
            onUpdate = { oyuncular -> kadroListesiniCiz(oyuncular) },
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

            // Kisa tiklama - bilgi
            itemBinding.root.setOnClickListener {
                showOyuncuBilgisiDialog(oyuncu)
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

    /**
     * Oyuncu ekleme dialog'u.
     * Custom layout (dialog_oyuncu_ekle.xml) kullanır.
     * Kaydet / İptal butonları layout içindedir — getButton() ile DEĞİL,
     * binding üzerinden wiring yapılır.
     */
    private fun showOyuncuEkleDialog() {
        val dialogBinding = DialogOyuncuEkleBinding.inflate(layoutInflater)

        // Mevki spinner'ını doldur
        val mevkiler = arrayOf("Kaleci", "Defans", "Orta Saha", "Forvet")
        dialogBinding.spMevki.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, mevkiler
        )

        // Dialog oluştur — setPositiveButton kullanmıyoruz,
        // butonlar zaten custom layout içinde (dialog_oyuncu_ekle.xml)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        // Kaydet butonu
        dialogBinding.btnKaydet.setOnClickListener {
            val isim  = dialogBinding.etOyuncuIsmi.text.toString().trim()
            val mevki = dialogBinding.spMevki.selectedItem.toString()

            // İsim boş bırakılamaz
            if (isim.isEmpty()) {
                Toast.makeText(this, "İsim boş olamaz!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Yeni oyuncu oluştur ve Firestore'a kaydet
            val yeniOyuncu = Oyuncu(
                isim     = isim,
                mevki    = mevki,
                takimAdi = takimAdi,
                ligAdi   = ligAdi
            )
            KadroRepository.oyuncuEkle(
                oyuncu    = yeniOyuncu,
                onSuccess = {
                    if (!isFinishing && !isDestroyed) {
                        Toast.makeText(this, "$isim eklendi!", Toast.LENGTH_SHORT).show()
                    }
                },
                onError = { e ->
                    if (!isFinishing && !isDestroyed) {
                        Toast.makeText(this, "Kayıt başarısız: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            )
            dialog.dismiss()
        }

        // İptal butonu
        dialogBinding.btnIptal.setOnClickListener {
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
