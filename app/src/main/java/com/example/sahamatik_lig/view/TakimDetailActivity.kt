package com.example.sahamatik_lig.view

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.sahamatik_lig.databinding.ActivityTakimDetailBinding
import com.example.sahamatik_lig.databinding.DialogOyuncuEkleBinding
import com.example.sahamatik_lig.databinding.ItemOyuncuRosterBinding
import com.example.sahamatik_lig.model.KadroRepository
import com.example.sahamatik_lig.model.Oyuncu
import com.example.sahamatik_lig.util.ImagePickerHelper

class TakimDetailActivity : AppCompatActivity() {

    // ViewBinding referansı: activity_takim_detail.xml içindeki tüm bileşenlere erişir
    private lateinit var binding: ActivityTakimDetailBinding

    // Sayfaya dışarıdan gelen veya varsayılan takım ve lig isimleri
    private var takimAdi: String = "Alshabab"
    private var ligAdi: String = "Sultanbeyli Ligi"

    // Galeri üzerinden takım logosu seçip SharedPreferences'a kaydeden yardımcı sınıf
    private val imagePicker = ImagePickerHelper(this) { uri ->
        binding.ivTakimLogo.setImageURI(uri)
        ImagePickerHelper.uriKaydet(this, "logo_$takimAdi", uri.toString())
        Toast.makeText(this, "Logo güncellendi!", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // XML layout ViewBinding ile bağlanıyor
        binding = ActivityTakimDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Bir önceki sayfadan gelen takım ve lig isimlerini yakalıyoruz
        takimAdi = intent.getStringExtra("TAKIM_ADI") ?: "Alshabab"
        ligAdi = intent.getStringExtra("LIG_ADI") ?: "Sultanbeyli Ligi"

        binding.tvTakimAdi.text = takimAdi
        binding.tvLigAdi.text = ligAdi

        // Cihazda daha önce kaydedilmiş logo varsa ImagePickerHelper ile getiriyoruz
        ImagePickerHelper.uriGetir(this, "logo_$takimAdi")?.let { uri ->
            binding.ivTakimLogo.setImageURI(uri)
        }

        // Sol üstteki geri butonuna basılınca sayfayı kapatır
        binding.btnGeri.setOnClickListener { finish() }

        // Takım amblemine dokunulduğunda telefonun galerisini açar
        binding.ivTakimLogo.setOnClickListener {
            imagePicker.pickImage()
        }

        // DOĞRUDAN VE NET TIKLAMA DİNLEYİCİSİ:
        // Butona basıldığı an showOyuncuEkleDialog fonksiyonunu tetikler
        binding.btnOyuncuEkle.setOnClickListener {
            showOyuncuEkleDialog()
        }

        // Sayfa ilk açıldığında depoda kayıtlı olan oyuncuları ekrana basar
        listeyiYenile()
    }

    // Seçilen takıma ait oyuncuları mevkilerine göre ayıran ve ekrana ekleyen fonksiyon
    private fun listeyiYenile() {
        // Yenilemeden önce eski görünümleri temizliyoruz ki üst üste binmesin
        binding.containerKaleciler.removeAllViews()
        binding.containerDefanslar.removeAllViews()
        binding.containerOrtaSaha.removeAllViews()
        binding.containerForvet.removeAllViews()

        // Sadece bu takıma ait oyuncuları çekiyoruz
        val oyuncular = KadroRepository.takimOyunculariniGetir(takimAdi)

        for (oyuncu in oyuncular) {
            // Oyuncunun mevkisine göre yerleşeceği LinearLayout kapsayıcısını belirliyoruz
            val hedefContainer = when (oyuncu.mevki.trim()) {
                "Kaleci" -> binding.containerKaleciler
                "Defans" -> binding.containerDefanslar
                "Orta Saha" -> binding.containerOrtaSaha
                "Forvet" -> binding.containerForvet
                else -> binding.containerOrtaSaha
            }

            // ÖNEMLİ: LayoutParams null kalıp çökmesin diye parent parametresine hedefContainer veriyoruz
            val itemBinding = ItemOyuncuRosterBinding.inflate(layoutInflater, hedefContainer, false)

            itemBinding.tvOyuncuIsim.text = oyuncu.isim
            itemBinding.tvOyuncuHarf.text = oyuncu.isim.firstOrNull()?.uppercase() ?: "?"

            // Oyuncu satırına basıldığında isim ve mevkiyi tost mesajı olarak gösterir
            itemBinding.root.setOnClickListener {
                Toast.makeText(this, "${oyuncu.isim} (${oyuncu.mevki})", Toast.LENGTH_SHORT).show()
            }

            // Kartı ilgili mevki kutusuna ekliyoruz
            hedefContainer.addView(itemBinding.root)
        }
    }

    // ESKİDEN SORUNSUZ ÇALIŞAN KLASİK DİALOG YAPISININ HATASIZ VE EN GÜVENLİ HALİ
    // %100 SAF VIEWBINDING İLE ÇALIŞAN ALT AÇILIR PANEL
    private fun showOyuncuEkleDialog() {
        // 1. ViewBinding ile XML dosyamızı şişiriyoruz
        val dialogBinding = DialogOyuncuEkleBinding.inflate(layoutInflater)

        // 2. Google Material BottomSheet penceresini oluşturuyoruz
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)

        // 3. ViewBinding'in root View'ını doğrudan pencereye veriyoruz
        dialog.setContentView(dialogBinding.root)

        // 4. Başlığı takıma göre dinamik yapıyoruz
        dialogBinding.tvDialogBaslik.text = "$takimAdi - Oyuncu Ekle"

        // 5. Mevki Spinner'ını bağlıyoruz
        val mevkiler = arrayOf("Kaleci", "Defans", "Orta Saha", "Forvet")
        dialogBinding.spMevki.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            mevkiler
        )

        // 6. İptal butonuna basılırsa pencereyi kapat
        dialogBinding.btnDialogIptal.setOnClickListener {
            dialog.dismiss()
        }

        // 7. Kaydet butonuna basıldığında oyuncuyu listeye ekle
        dialogBinding.btnDialogKaydet.setOnClickListener {
            val isim = dialogBinding.etOyuncuIsmi.text.toString().trim()
            val mevki = dialogBinding.spMevki.selectedItem.toString()

            if (isim.isNotEmpty()) {
                val yeni = Oyuncu(
                    id = KadroRepository.tumOyuncular.size + 1,
                    isim = isim,
                    mevki = mevki,
                    takimAdi = takimAdi
                )
                // Depoya ekle
                KadroRepository.oyuncuEkle(yeni)

                // Ekrandaki kadroyu anında güncelle
                listeyiYenile()

                Toast.makeText(this, "$isim kadroya eklendi!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Lütfen oyuncu ismi girin!", Toast.LENGTH_SHORT).show()
            }
        }

        // Pencereyi ekranda aç
        dialog.show()
    }
}