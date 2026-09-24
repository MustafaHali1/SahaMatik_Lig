package com.example.sahamatik_lig.view

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import android.widget.TextView
import com.example.sahamatik_lig.adapter.LigAdapter
import com.example.sahamatik_lig.adapter.OyuncuAramaAdapter
import com.example.sahamatik_lig.databinding.ActivityMainBinding
import com.example.sahamatik_lig.databinding.BottomSheetLigSecimBinding
import com.example.sahamatik_lig.databinding.DialogAddLeagueBinding
import com.example.sahamatik_lig.databinding.DialogGrupTurnuvaEkleBinding
import com.example.sahamatik_lig.databinding.DialogHizliMacOlusturBinding
import com.example.sahamatik_lig.databinding.DialogOyuncuAraSecBinding
import com.example.sahamatik_lig.databinding.DialogProfilOlusturBinding
import com.example.sahamatik_lig.databinding.DialogSilmeOnayiBinding
import com.example.sahamatik_lig.model.KadroRepository
import com.example.sahamatik_lig.model.Lig
import com.example.sahamatik_lig.model.Oyuncu
import com.example.sahamatik_lig.util.ImagePickerHelper
import com.example.sahamatik_lig.util.ProfilFotoHelper
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.ListenerRegistration

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var ligList: ArrayList<Lig>
    private lateinit var adapter: LigAdapter
    private var ligListener: ListenerRegistration? = null
    private lateinit var profilFacePickerHelper: ImagePickerHelper
    private var onFaceFotoSecildiCallback: ((Uri) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ligList = ArrayList()

        val cihazId = KadroRepository.cihazIdAl(this)
        adapter = LigAdapter(
            ligList = ligList,
            mevcutCihazId = cihazId,
            onItemClick = { secilenLig ->
                if (secilenLig.formatTipi == "TEKIL_MAC") {
                    val evTakim = secilenLig.takimlar.getOrNull(0) ?: "Ev Sahibi"
                    val depTakim = secilenLig.takimlar.getOrNull(1) ?: "Deplasman"
                    val intent = Intent(this@MainActivity, MacDetailActivity::class.java).apply {
                        putExtra("EV_TAKIM", evTakim)
                        putExtra("DEP_TAKIM", depTakim)
                        putExtra("LIG_ADI", secilenLig.name)
                        putExtra("FORMAT_TIPI", "TEKIL_MAC")
                        putExtra("MAC_ID", 1)
                        putExtra("HAFTA", 1)
                    }
                    startActivity(intent)
                } else {
                    val intent = Intent(this@MainActivity, LigDetailActivity::class.java).apply {
                        putExtra("LIG_ADI", secilenLig.name)
                        putExtra("FORMAT_TIPI", secilenLig.formatTipi)
                        putStringArrayListExtra("TAKIMLAR", ArrayList(secilenLig.takimlar))

                        if (secilenLig.formatTipi == "GRUP" && secilenLig.gruplarMap != null) {
                            val gruplarHashMap = HashMap<String, ArrayList<String>>()
                            for ((grup, takimlar) in secilenLig.gruplarMap) {
                                gruplarHashMap[grup] = ArrayList(takimlar)
                            }
                            putExtra("GRUPLAR_MAP", gruplarHashMap)
                        }
                    }
                    startActivity(intent)
                }
            },
            onItemLongClick = { secilenLig -> showLigSilDialog(secilenLig) },
            onItemSilClick = { secilenLig -> showLigSilDialog(secilenLig) }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.fabAddLeague.setOnClickListener {
            showLigTuruSecimDialog()
        }

        // Yüz Doğrulamalı Profil Fotoğraf Seçici
        profilFacePickerHelper = ImagePickerHelper(this) { uri ->
            onFaceFotoSecildiCallback?.invoke(uri)
        }

        // Profil Butonu
        binding.cardUserProfile.setOnClickListener {
            profilSayfasinaGit()
        }
        val yerel = KadroRepository.yerelProfilGetir(this)
        if (yerel != null && yerel.profilFotoUri.isNotBlank() && !yerel.profilFotoUri.startsWith("data:image")) {
            try {
                val base64 = ProfilFotoHelper.uriToBase64(this, Uri.parse(yerel.profilFotoUri))
                if (base64 != null) {
                    KadroRepository.yerelProfilKaydet(this, yerel.username, yerel.isim, yerel.mevki, yerel.formaNo, profilFotoUri = base64)
                    KadroRepository.oyuncuProfilFotoGuncelle(yerel.username, base64)
                }
            } catch (_: Exception) {}
        }
        profilButonunuGuncelle()

        // İlk açılış kontrolü: Oyuncu profili yoksa oluşturma dialogunu aç
        if (!KadroRepository.yerelProfilVarMi(this)) {
            showProfilOlusturDialog()
        }

        // KRİTİK NOKTA: Firestore'daki ligleri çek ve ekrana bas
        ligleriYukle()

        // 20 Mock Kullanıcıyı Tek Seferlik Yükle (Tam 2 Kaleci Kuralı)
        val userPrefs = getSharedPreferences("SahamatikUserPrefs", MODE_PRIVATE)
        if (!userPrefs.getBoolean("mock_kullanicilar_yuklendi_v2", false)) {
            com.example.sahamatik_lig.util.MockVeriYukleyici.sahteKullanicilariYukle {
                userPrefs.edit().putBoolean("mock_kullanicilar_yuklendi_v2", true).apply()
                runOnUiThread {
                    Toast.makeText(this, "20 Test Oyuncusu Yüklendi! ⚽", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Eski Düz ve Sahte İsimleri Temizle
        if (!userPrefs.getBoolean("eski_duz_isimler_temizlendi", false)) {
            KadroRepository.eskiDuzIsimleriTemizle {
                userPrefs.edit().putBoolean("eski_duz_isimler_temizlendi", true).apply()
            }
        }

        // =========================================================================================
        // 🚨 [TEK SEFERLİK VERİTABANI VE TEST VERİLERİNİ TEMİZLEME KODU - İSTEDİĞİNİZ ZAMAN SİLEBİLİRSİNİZ]
        // ℹ️ Bu blok, veritabanındaki önceki testlerden kalan bozuk ligleri ve maçları sıfırlar.
        // 🛡️ 20 adet sahte (mock) oyuncu KORUNUR ve sıfırdan temiz yüklenir.
        // 🗑️ DİLEDİĞİNİZ ZAMAN BU İF BLOĞUNU BURADAN TAMAMEN SİLEBİLİR VEYA KALDIRABİLİRSİNİZ.
        // =========================================================================================
        if (!userPrefs.getBoolean("veritabani_temiz_sifirlandi_v1", false)) {
            KadroRepository.veritabaniniTemizleVeSifirla(this) {
                userPrefs.edit().putBoolean("veritabani_temiz_sifirlandi_v1", true).apply()
                runOnUiThread {
                    Toast.makeText(this, "Veritabanı sıfırlandı, temiz başlangıç yapıldı! ⚽", Toast.LENGTH_LONG).show()
                    ligleriYukle()
                }
            }
        }
        // =========================================================================================

        // Deep Link Kontrolü (WhatsApp / Paylaşım linkinden gelenler)
        deepLinkKontrolEt(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        deepLinkKontrolEt(intent)
    }

    override fun onResume() {
        super.onResume()
        profilButonunuGuncelle()
    }

    private fun ligleriYukle() {
        ligListener = KadroRepository.ligleriCanliDinle(
            onUpdate = { gelenLigler ->
                val aktifLigler = gelenLigler.filter { lig ->
                    !KadroRepository.ayrilinanLigMi(this@MainActivity, lig.name)
                }
                ligList.clear()
                ligList.addAll(aktifLigler)
                adapter.notifyDataSetChanged()
            },
            onError = { e ->
                Toast.makeText(this, "Ligler yüklenemedi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun showLigTuruSecimDialog() {
        val dialog = BottomSheetDialog(this)
        val sheetBinding = BottomSheetLigSecimBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        sheetBinding.cardKlasikLig.setOnClickListener {
            dialog.dismiss()
            showAddLeagueDialog()
        }

        sheetBinding.cardGrupTurnuva.setOnClickListener {
            dialog.dismiss()
            showAddGroupTournamentDialog()
        }

        sheetBinding.cardTekilMac.setOnClickListener {
            dialog.dismiss()
            showHizliMacDialog()
        }

        dialog.show()
    }

    private fun showHizliMacDialog() {
        val dialogBinding = DialogHizliMacOlusturBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        var currentStep = 1
        var macAdi = ""
        var takim1Adi = ""
        var takim2Adi = ""
        val takim1Oyuncular = mutableListOf<Pair<String, String>>()
        val takim2Oyuncular = mutableListOf<Pair<String, String>>()

        dialogBinding.btnHizliMacIptal.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnHizliMacDevam.setOnClickListener {
            when (currentStep) {
                1 -> {
                    macAdi = dialogBinding.etMacAdi.text.toString().trim().replace("/", "-")
                    takim1Adi = dialogBinding.etTakim1Adi.text.toString().trim().replace("/", "-")
                    takim2Adi = dialogBinding.etTakim2Adi.text.toString().trim().replace("/", "-")

                    if (macAdi.isEmpty()) {
                        Toast.makeText(this, "Lütfen maç adını girin", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    if (takim1Adi.isEmpty() || takim2Adi.isEmpty()) {
                        Toast.makeText(this, "Lütfen her iki takım adını da girin", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    if (takim1Adi.equals(takim2Adi, ignoreCase = true)) {
                        Toast.makeText(this, "Takım adları farklı olmalıdır", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    // 2. Adıma Geç (Takım 1 Kadrosu)
                    currentStep = 2
                    dialogBinding.layoutMacAdim1.visibility = View.GONE
                    dialogBinding.layoutMacAdim2.visibility = View.VISIBLE
                    dialogBinding.tvHizliMacBaslik.text = "🟢 $takim1Adi Kadrosu (2/3)"
                    dialogBinding.tvTakim1KadroBilgi.text = "$takim1Adi için oyuncu seçin (Tıklayarak arayın):"
                    dialogBinding.btnHizliMacDevam.text = "2. Takıma Geç"

                    val t1List = listOf(
                        dialogBinding.etT1Oyuncu1,
                        dialogBinding.etT1Oyuncu2,
                        dialogBinding.etT1Oyuncu3,
                        dialogBinding.etT1Oyuncu4,
                        dialogBinding.etT1Oyuncu5,
                        dialogBinding.etT1Oyuncu6,
                        dialogBinding.etT1Oyuncu7
                    )
                    t1List.forEach { et ->
                        et.isFocusable = true
                        et.setOnClickListener {
                            val digerSecilenler = t1List.filter { it != et }
                                .map { it.text.toString().trim().removePrefix("@").lowercase() }
                                .filter { it.isNotEmpty() }
                                .toSet()
                            showOyuncuAraSecDialog(haricTutulanUsernameler = digerSecilenler) { secilen ->
                                et.setText(secilen.username)
                            }
                        }
                    }
                }

                2 -> {
                    val inputs = listOf(
                        Pair(dialogBinding.etT1Oyuncu1.text.toString().trim(), "Kaleci"),
                        Pair(dialogBinding.etT1Oyuncu2.text.toString().trim(), "Defans"),
                        Pair(dialogBinding.etT1Oyuncu3.text.toString().trim(), "Defans"),
                        Pair(dialogBinding.etT1Oyuncu4.text.toString().trim(), "Orta Saha"),
                        Pair(dialogBinding.etT1Oyuncu5.text.toString().trim(), "Orta Saha"),
                        Pair(dialogBinding.etT1Oyuncu6.text.toString().trim(), "Forvet"),
                        Pair(dialogBinding.etT1Oyuncu7.text.toString().trim(), "Forvet")
                    )

                    takim1Oyuncular.clear()
                    // Yalnızca gerçekten girilen veya seçilen oyuncular eklenir; sahte isim uydurulmaz!
                    inputs.forEach { (isim, mevki) ->
                        if (isim.isNotBlank()) {
                            takim1Oyuncular.add(Pair(isim, mevki))
                        }
                    }

                    // 3. Adıma Geç (Takım 2 Kadrosu)
                    currentStep = 3
                    dialogBinding.layoutMacAdim2.visibility = View.GONE
                    dialogBinding.layoutMacAdim3.visibility = View.VISIBLE
                    dialogBinding.tvHizliMacBaslik.text = "⚪ $takim2Adi Kadrosu (3/3)"
                    dialogBinding.tvTakim2KadroBilgi.text = "$takim2Adi için oyuncu seçin (Tıklayarak arayın):"
                    dialogBinding.btnHizliMacDevam.text = "Maçı Başlat ⚽"

                    val t2List = listOf(
                        dialogBinding.etT2Oyuncu1,
                        dialogBinding.etT2Oyuncu2,
                        dialogBinding.etT2Oyuncu3,
                        dialogBinding.etT2Oyuncu4,
                        dialogBinding.etT2Oyuncu5,
                        dialogBinding.etT2Oyuncu6,
                        dialogBinding.etT2Oyuncu7
                    )
                    t2List.forEach { et ->
                        et.isFocusable = true
                        et.setOnClickListener {
                            // Takım 1'deki TÜM oyuncuları ve Takım 2'de halihazırda seçilenleri hariç tut!
                            val t1Usernames = takim1Oyuncular.map { it.first.removePrefix("@").lowercase().trim() }
                            val digerT2 = t2List.filter { it != et }
                                .map { it.text.toString().trim().removePrefix("@").lowercase() }
                                .filter { it.isNotEmpty() }
                            val tumEngellenenler = (t1Usernames + digerT2).toSet()

                            showOyuncuAraSecDialog(haricTutulanUsernameler = tumEngellenenler) { secilen ->
                                et.setText(secilen.username)
                            }
                        }
                    }
                }

                3 -> {
                    val inputs = listOf(
                        Pair(dialogBinding.etT2Oyuncu1.text.toString().trim(), "Kaleci"),
                        Pair(dialogBinding.etT2Oyuncu2.text.toString().trim(), "Defans"),
                        Pair(dialogBinding.etT2Oyuncu3.text.toString().trim(), "Defans"),
                        Pair(dialogBinding.etT2Oyuncu4.text.toString().trim(), "Orta Saha"),
                        Pair(dialogBinding.etT2Oyuncu5.text.toString().trim(), "Orta Saha"),
                        Pair(dialogBinding.etT2Oyuncu6.text.toString().trim(), "Forvet"),
                        Pair(dialogBinding.etT2Oyuncu7.text.toString().trim(), "Forvet")
                    )

                    // KONTROL: Takım 1 ile çakışan oyuncu var mı? (Aynı oyuncu iki takıma birden yazılamaz!)
                    val t1Usernames = takim1Oyuncular.map { it.first.removePrefix("@").lowercase().trim() }.toSet()
                    val cakisamOyuncu = inputs.find { pair ->
                        pair.first.isNotBlank() && t1Usernames.contains(pair.first.removePrefix("@").lowercase().trim())
                    }
                    if (cakisamOyuncu != null) {
                        Toast.makeText(this, "⚠️ '${cakisamOyuncu.first}' zaten $takim1Adi takımında yer alıyor! Aynı maçta iki takımda birden oynayamaz.", Toast.LENGTH_LONG).show()
                        return@setOnClickListener
                    }

                    takim2Oyuncular.clear()
                    // Yalnızca gerçekten girilen veya seçilen oyuncular eklenir; sahte isim uydurulmaz!
                    inputs.forEach { (isim, mevki) ->
                        if (isim.isNotBlank()) {
                            takim2Oyuncular.add(Pair(isim, mevki))
                        }
                    }

                    dialog.dismiss()
                    hizliMacOlusturVeBaslat(macAdi, takim1Adi, takim2Adi, takim1Oyuncular, takim2Oyuncular)
                }
            }
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun hizliMacOlusturVeBaslat(
        macAdi: String,
        takim1: String,
        takim2: String,
        t1Oyuncular: List<Pair<String, String>>,
        t2Oyuncular: List<Pair<String, String>>
    ) {
        Toast.makeText(this, "⚽ Maç ve kadrolar hazırlanıyor...", Toast.LENGTH_SHORT).show()

        val tekilLig = Lig(
            name = macAdi,
            takimsayisi = 2,
            takimlar = listOf(takim1, takim2),
            formatTipi = "TEKIL_MAC",
            olusturulmaTarihi = System.currentTimeMillis(),
            olusturanId = KadroRepository.cihazIdAl(this)
        )

        KadroRepository.ligKaydet(
            lig = tekilLig,
            onSuccess = {
                val tekilMacBaslik = "$macAdi ($takim1 vs $takim2)"
                KadroRepository.takimOlustur(macAdi, takim1, onSuccess = {
                    t1Oyuncular.forEach { (rawIsim, mevki) ->
                        val cleanUsername = if (rawIsim.startsWith("@")) rawIsim else "@${rawIsim.lowercase().replace(" ", "")}"
                        val displayName = if (rawIsim.startsWith("@")) rawIsim.removePrefix("@") else rawIsim
                        val o = Oyuncu(
                            isim = displayName,
                            username = cleanUsername,
                            mevki = mevki,
                            takimAdi = takim1,
                            ligAdi = macAdi
                        )
                        KadroRepository.oyuncuEkle(o)
                        KadroRepository.oyuncuProfiliKaydet(o)
                        KadroRepository.oyuncuyaOrganizasyonEkle(cleanUsername, tekilMacBaslik, "TEKIL_MAC")
                    }

                    KadroRepository.takimOlustur(macAdi, takim2, onSuccess = {
                        var t2Eklenen = 0
                        val toplamT2 = t2Oyuncular.size
                        t2Oyuncular.forEach { (rawIsim, mevki) ->
                            val cleanUsername = if (rawIsim.startsWith("@")) rawIsim else "@${rawIsim.lowercase().replace(" ", "")}"
                            val displayName = if (rawIsim.startsWith("@")) rawIsim.removePrefix("@") else rawIsim
                            val o = Oyuncu(
                                isim = displayName,
                                username = cleanUsername,
                                mevki = mevki,
                                takimAdi = takim2,
                                ligAdi = macAdi
                            )
                            KadroRepository.oyuncuProfiliKaydet(o)
                            KadroRepository.oyuncuyaOrganizasyonEkle(cleanUsername, tekilMacBaslik, "TEKIL_MAC")
                            KadroRepository.oyuncuEkle(
                                o,
                                onSuccess = {
                                    t2Eklenen++
                                    if (t2Eklenen == toplamT2) {
                                        val yerel = KadroRepository.yerelProfilGetir(this@MainActivity)
                                        if (yerel != null && yerel.username.isNotEmpty()) {
                                            KadroRepository.oyuncuyaOrganizasyonEkle(yerel.username, tekilMacBaslik, "TEKIL_MAC")
                                        }
                                        val intent = Intent(this@MainActivity, MacDetailActivity::class.java).apply {
                                            putExtra("EV_TAKIM", takim1)
                                            putExtra("DEP_TAKIM", takim2)
                                            putExtra("LIG_ADI", macAdi)
                                            putExtra("FORMAT_TIPI", "TEKIL_MAC")
                                            putExtra("MAC_ID", 1)
                                            putExtra("HAFTA", 1)
                                        }
                                        startActivity(intent)
                                    }
                                }
                            )
                        }
                    }, onError = { e ->
                        Toast.makeText(this, "2. Takım eklenemedi: ${e.message}", Toast.LENGTH_SHORT).show()
                    })
                }, onError = { e ->
                    Toast.makeText(this, "1. Takım eklenemedi: ${e.message}", Toast.LENGTH_SHORT).show()
                })
            },
            onError = { e ->
                Toast.makeText(this, "Maç oluşturulamadı: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun showAddGroupTournamentDialog() {
        val dialogBinding = DialogGrupTurnuvaEkleBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnOlustur.setOnClickListener {
            val turnuvaAdi = dialogBinding.etTurnuvaAdi.text.toString().trim()
            val grupSayisiStr = dialogBinding.etGrupSayisi.text.toString().trim()
            val takimlarGirdi = dialogBinding.etTakimlar.text.toString().trim()

            val grupSayisi = grupSayisiStr.toIntOrNull() ?: 0

            val takimListesi = takimlarGirdi.split("\n", ",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            if (turnuvaAdi.isEmpty()) {
                Toast.makeText(this, "Lütfen turnuva adını girin!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (grupSayisi < 2) {
                Toast.makeText(this, "Grup sayısı en az 2 olmalıdır!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val gerekenMinimumTakim = grupSayisi * 2
            if (takimListesi.size < gerekenMinimumTakim) {
                Toast.makeText(this, "$grupSayisi grup için en az $gerekenMinimumTakim takım girmelisiniz!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val gruplar = com.example.sahamatik_lig.util.GrupTurnuvaHelper.gruplaraDagit(takimListesi, grupSayisi)

            val yeniLig = Lig(
                id = ligList.size + 1,
                name = turnuvaAdi,
                takimsayisi = takimListesi.size,
                takimlar = takimListesi,
                formatTipi = "GRUP",
                gruplarMap = gruplar,
                olusturanId = KadroRepository.cihazIdAl(this)
            )

            KadroRepository.ligKaydet(yeniLig)
            for (takimAdi in takimListesi) {
                KadroRepository.takimOlustur(turnuvaAdi, takimAdi)
            }

            val yerel = KadroRepository.yerelProfilGetir(this)
            if (yerel != null && yerel.username.isNotEmpty()) {
                KadroRepository.oyuncuyaOrganizasyonEkle(yerel.username, turnuvaAdi, "GRUP")
            }

            Toast.makeText(this, "$turnuvaAdi oluşturuldu!", Toast.LENGTH_SHORT).show()

            val gruplarHashMap = HashMap<String, ArrayList<String>>()
            for ((grup, takimlar) in gruplar) {
                gruplarHashMap[grup] = ArrayList(takimlar)
            }

            val intent = Intent(this@MainActivity, LigDetailActivity::class.java).apply {
                putExtra("LIG_ADI", turnuvaAdi)
                putExtra("FORMAT_TIPI", "GRUP")
                putExtra("GRUPLAR_MAP", gruplarHashMap)
            }
            startActivity(intent)
            dialog.dismiss()
        }

        dialogBinding.btnIptal.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun showAddLeagueDialog() {
        val dialogBinding = DialogAddLeagueBinding.inflate(layoutInflater)

        val teamEditTextList = mutableListOf<EditText>()
        var currentStep = 1
        var leagueName = ""

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnDevam.setOnClickListener {
            if (currentStep == 1) {
                leagueName = dialogBinding.etLeagueName.text.toString().trim()
                val teamCountStr = dialogBinding.etTeamCount.text.toString().trim()

                if (leagueName.isNotEmpty() && teamCountStr.isNotEmpty()) {
                    val teamCount = teamCountStr.toIntOrNull()

                    if (teamCount == null || teamCount < 2) {
                        Toast.makeText(this, "En az 2 takım olmalı!", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    dialogBinding.containerTeams.removeAllViews()
                    teamEditTextList.clear()

                    for (i in 1..teamCount) {
                        val editText = EditText(this).apply {
                            hint = "$i. Takım Adı"
                            setSingleLine()
                            setBackgroundResource(com.example.sahamatik_lig.R.drawable.bg_dialog_input)
                            setPadding(32, 24, 32, 24)
                            val lp = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            lp.bottomMargin = 16
                            layoutParams = lp
                            setTextColor(getColor(com.example.sahamatik_lig.R.color.textDark))
                            setHintTextColor(getColor(com.example.sahamatik_lig.R.color.textSecondary))
                            textSize = 14f
                        }

                        dialogBinding.containerTeams.addView(editText)
                        teamEditTextList.add(editText)
                    }

                    dialogBinding.layoutStep1.visibility = View.GONE
                    dialogBinding.layoutStep2.visibility = View.VISIBLE
                    dialogBinding.tvDialogTitle.text = "⚽ Takım İsimlerini Gir (2/2)"
                    dialogBinding.btnDevam.text = "Kura Çek & Başlat"
                    currentStep = 2

                } else {
                    Toast.makeText(this, "Lütfen tüm alanları doldurun!", Toast.LENGTH_SHORT).show()
                }

            } else if (currentStep == 2) {
                val teamNames = mutableListOf<String>()
                var allFilled = true

                for (et in teamEditTextList) {
                    val name = et.text.toString().trim()
                    if (name.isEmpty()) {
                        allFilled = false
                        break
                    }
                    teamNames.add(name)
                }

                if (allFilled) {
                    val yeniLig = Lig(
                        id = ligList.size + 1,
                        name = leagueName,
                        takimsayisi = teamNames.size,
                        takimlar = teamNames,
                        formatTipi = "KLASIK",
                        olusturanId = KadroRepository.cihazIdAl(this)
                    )

                    // Firestore'a kaydet
                    KadroRepository.ligKaydet(yeniLig)
                    for (takimAdi in teamNames) {
                        KadroRepository.takimOlustur(leagueName, takimAdi)
                    }

                    val yerel = KadroRepository.yerelProfilGetir(this)
                    if (yerel != null && yerel.username.isNotEmpty()) {
                        KadroRepository.oyuncuyaOrganizasyonEkle(yerel.username, leagueName, "KLASIK")
                    }

                    Toast.makeText(this, "$leagueName başarıyla kuruldu!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()

                    val intent = Intent(this@MainActivity, LigDetailActivity::class.java).apply {
                        putExtra("LIG_ADI", leagueName)
                        putExtra("FORMAT_TIPI", "KLASIK")
                        putStringArrayListExtra("TAKIMLAR", ArrayList(teamNames))
                    }
                    startActivity(intent)

                } else {
                    Toast.makeText(this, "Lütfen tüm takım isimlerini girin!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialogBinding.btnIptal.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    // Ligi silme / ayrılma dialogu - Kurucu vs Katılımcı Ayrımı
    private fun showLigSilDialog(lig: Lig) {
        val currentCihazId = KadroRepository.cihazIdAl(this)
        val isKurucu = lig.olusturanId.isEmpty() || lig.olusturanId == currentCihazId

        if (isKurucu) {
            val dialogBinding = DialogSilmeOnayiBinding.inflate(layoutInflater)
            val dialog = AlertDialog.Builder(this)
                .setView(dialogBinding.root)
                .create()

            dialogBinding.tvSilmeBaslik.text = "🗑️ Ligi / Maçı Sil (Kurucu)"
            dialogBinding.tvSilmeAciklama.text =
                "Bu organizasyonu kuran sizsiniz.\n'${lig.name}' silindiğinde tüm takımlar ve oyuncular için Firebase'den tamamen kaldırılacaktır.\n\nBu işlem geri alınamaz!"

            dialogBinding.btnSilOnay.text = "Tamamen Sil"
            dialogBinding.btnSilOnay.setOnClickListener {
                dialog.dismiss()
                KadroRepository.ligSil(
                    lig.name,
                    onSuccess = {
                        if (!isFinishing && !isDestroyed) {
                            Toast.makeText(this, "${lig.name} Firebase'den silindi 🗑️", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onError = { e ->
                        if (!isFinishing && !isDestroyed) {
                            Toast.makeText(this, "Silme başarısız: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            dialogBinding.btnSilIptal.setOnClickListener {
                dialog.dismiss()
            }

            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.show()
        } else {
            showLigdenAyrilDialog(lig)
        }
    }

    private fun showLigdenAyrilDialog(lig: Lig) {
        val dialogBinding = DialogSilmeOnayiBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.tvSilmeBaslik.text = "🚪 Kadrodan / Maçtan Ayrıl"
        dialogBinding.tvSilmeAciklama.text =
            "Bu organizasyonu siz kurmadınız.\n\nAyrıldığınızda maç Firebase'den SİLİNMEZ. Sadece kadrodaki yeriniz boşalır ve maç sizin ana ekranınızdan kaldırılır.\n\n(Kariyer istatistikleriniz profilinizde kalmaya devam eder)"

        dialogBinding.btnSilOnay.text = "Ayrıl"
        dialogBinding.btnSilOnay.setOnClickListener {
            dialog.dismiss()
            val yerel = KadroRepository.yerelProfilGetir(this)
            val username = yerel?.username ?: ""

            KadroRepository.ligdenAyril(
                context = this,
                ligAdi = lig.name,
                username = username,
                onSuccess = {
                    if (!isFinishing && !isDestroyed) {
                        Toast.makeText(this, "${lig.name} kadrosundan ayrıldınız 🚪", Toast.LENGTH_SHORT).show()
                        ligleriYukle()
                    }
                },
                onError = { e ->
                    if (!isFinishing && !isDestroyed) {
                        Toast.makeText(this, "Ayrılma işlemi başarısız: ${e.message}", Toast.LENGTH_SHORT).show()
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

    // ─── KULLANICI PROFİLİ VE ONBOARDING ──────────────────────────────────────

    private fun profilButonunuGuncelle() {
        val yerel = KadroRepository.yerelProfilGetir(this)
        if (yerel != null && yerel.isim.isNotEmpty()) {
            val bmp = ProfilFotoHelper.gorselYukle(this, yerel.profilFotoUri)
            if (bmp != null) {
                binding.ivHeaderUserFoto.visibility = View.VISIBLE
                binding.ivHeaderUserFoto.setImageBitmap(bmp)
                binding.tvHeaderUserHarf.visibility = View.GONE
            } else {
                binding.ivHeaderUserFoto.visibility = View.GONE
                binding.tvHeaderUserHarf.visibility = View.VISIBLE
                val harf = yerel.isim.trim().take(1).uppercase()
                binding.tvHeaderUserHarf.text = harf
            }
        } else {
            binding.ivHeaderUserFoto.visibility = View.GONE
            binding.tvHeaderUserHarf.visibility = View.VISIBLE
            binding.tvHeaderUserHarf.text = "👤"
        }
    }

    private fun profilSayfasinaGit() {
        val yerel = KadroRepository.yerelProfilGetir(this)
        if (yerel != null && yerel.username.isNotEmpty()) {
            KadroRepository.oyuncuProfiliGetir(yerel.username) { profil ->
                val hedefOyuncu = profil ?: yerel
                val intent = Intent(this, OyuncuDetailActivity::class.java).apply {
                    putExtra("OYUNCU", hedefOyuncu)
                }
                startActivity(intent)
            }
        } else {
            showProfilOlusturDialog()
        }
    }

    private fun showProfilOlusturDialog() {
        val dialogBinding = DialogProfilOlusturBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        var secilenMevki = "Forvet"

        // Mevki seçim fonksiyonu
        fun mevkiSec(mevki: String) {
            secilenMevki = mevki
            val seciliBg = com.example.sahamatik_lig.R.drawable.bg_format_secim_kart
            val pasifBg = com.example.sahamatik_lig.R.drawable.bg_dialog_input

            dialogBinding.btnMevkiKaleci.setBackgroundResource(if (mevki == "Kaleci") seciliBg else pasifBg)
            dialogBinding.btnMevkiKaleci.setTextColor(Color.parseColor(if (mevki == "Kaleci") "#0F3D2E" else "#5B6660"))

            dialogBinding.btnMevkiDefans.setBackgroundResource(if (mevki == "Defans") seciliBg else pasifBg)
            dialogBinding.btnMevkiDefans.setTextColor(Color.parseColor(if (mevki == "Defans") "#0F3D2E" else "#5B6660"))

            dialogBinding.btnMevkiOrtaSaha.setBackgroundResource(if (mevki == "Orta Saha") seciliBg else pasifBg)
            dialogBinding.btnMevkiOrtaSaha.setTextColor(Color.parseColor(if (mevki == "Orta Saha") "#0F3D2E" else "#5B6660"))

            dialogBinding.btnMevkiForvet.setBackgroundResource(if (mevki == "Forvet") seciliBg else pasifBg)
            dialogBinding.btnMevkiForvet.setTextColor(Color.parseColor(if (mevki == "Forvet") "#0F3D2E" else "#5B6660"))
        }

        dialogBinding.btnMevkiKaleci.setOnClickListener { mevkiSec("Kaleci") }
        dialogBinding.btnMevkiDefans.setOnClickListener { mevkiSec("Defans") }
        dialogBinding.btnMevkiOrtaSaha.setOnClickListener { mevkiSec("Orta Saha") }
        dialogBinding.btnMevkiForvet.setOnClickListener { mevkiSec("Forvet") }

        // Mevcut profil varsa doldur
        val mevcut = KadroRepository.yerelProfilGetir(this)
        var secilenProfilFotoUri = mevcut?.profilFotoUri ?: ""

        if (secilenProfilFotoUri.isNotBlank()) {
            val bmp = ProfilFotoHelper.gorselYukle(this, secilenProfilFotoUri)
            if (bmp != null) {
                dialogBinding.ivProfilFoto.visibility = View.VISIBLE
                dialogBinding.ivProfilFoto.setImageBitmap(bmp)
                dialogBinding.tvProfilHarf.visibility = View.GONE
            }
        }

        onFaceFotoSecildiCallback = { uri ->
            val base64 = ProfilFotoHelper.uriToBase64(this, uri) ?: uri.toString()
            secilenProfilFotoUri = base64
            val bmp = ProfilFotoHelper.gorselYukle(this, base64)
            if (bmp != null) {
                dialogBinding.ivProfilFoto.visibility = View.VISIBLE
                dialogBinding.ivProfilFoto.setImageBitmap(bmp)
                dialogBinding.tvProfilHarf.visibility = View.GONE
            }
        }

        dialogBinding.containerProfilFotoSec.setOnClickListener {
            profilFacePickerHelper.pickFaceImage()
        }

        if (mevcut != null) {
            dialogBinding.etProfilIsim.setText(mevcut.isim)
            dialogBinding.etProfilUsername.setText(mevcut.username)
            dialogBinding.etProfilFormaNo.setText(mevcut.formaNo.toString())
            mevkiSec(mevcut.mevki)
            if (mevcut.isim.isNotEmpty() && secilenProfilFotoUri.isEmpty()) {
                dialogBinding.tvProfilHarf.text = mevcut.isim.take(1).uppercase()
            }
        } else {
            mevkiSec("Forvet")
        }

        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        var checkRunnable: Runnable? = null

        fun usernameKontrolEt(raw: String) {
            val clean = raw.removePrefix("@").lowercase().trim()
            if (clean.isEmpty()) {
                dialogBinding.tvUsernameDurumUyari.visibility = View.GONE
                dialogBinding.layoutUsernameOneriler.visibility = View.GONE
                dialogBinding.containerOneriChipleri.removeAllViews()
                dialogBinding.btnProfilKaydet.alpha = 0.5f
                return
            }

            // Mevcut kullanıcının kendi kullanıcı adıysa geçerli
            if (mevcut != null && clean == mevcut.username.removePrefix("@").lowercase().trim()) {
                dialogBinding.tvUsernameDurumUyari.visibility = View.GONE
                dialogBinding.layoutUsernameOneriler.visibility = View.GONE
                dialogBinding.containerOneriChipleri.removeAllViews()
                dialogBinding.btnProfilKaydet.alpha = 1.0f
                return
            }

            KadroRepository.usernameMusaitMi(clean) { musait ->
                runOnUiThread {
                    if (musait) {
                        dialogBinding.tvUsernameDurumUyari.visibility = View.GONE
                        dialogBinding.layoutUsernameOneriler.visibility = View.GONE
                        dialogBinding.containerOneriChipleri.removeAllViews()
                        dialogBinding.btnProfilKaydet.alpha = 1.0f
                    } else {
                        dialogBinding.tvUsernameDurumUyari.visibility = View.VISIBLE
                        dialogBinding.tvUsernameDurumUyari.text = "⚠️ @$clean kullanıcı adı alınmış!"
                        dialogBinding.layoutUsernameOneriler.visibility = View.VISIBLE
                        dialogBinding.btnProfilKaydet.alpha = 0.5f

                        KadroRepository.usernameOnerileriUret(clean) { oneriler ->
                            runOnUiThread {
                                dialogBinding.containerOneriChipleri.removeAllViews()
                                for (oneri in oneriler) {
                                    val chip = TextView(this).apply {
                                        text = oneri
                                        setTextColor(Color.parseColor("#0F3D2E"))
                                        setBackgroundResource(com.example.sahamatik_lig.R.drawable.bg_dialog_input)
                                        setPadding(24, 12, 24, 12)
                                        val lp = LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.WRAP_CONTENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT
                                        ).apply {
                                            marginEnd = 12
                                        }
                                        layoutParams = lp
                                        textSize = 12f
                                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                                        setOnClickListener {
                                            dialogBinding.etProfilUsername.setText(oneri)
                                            dialogBinding.etProfilUsername.setSelection(oneri.length)
                                        }
                                    }
                                    dialogBinding.containerOneriChipleri.addView(chip)
                                }
                            }
                        }
                    }
                }
            }
        }

        dialogBinding.etProfilUsername.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                checkRunnable?.let { handler.removeCallbacks(it) }
                checkRunnable = Runnable {
                    usernameKontrolEt(s?.toString() ?: "")
                }
                handler.postDelayed(checkRunnable!!, 350)
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        dialogBinding.etProfilIsim.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                val isim = s?.toString()?.trim() ?: ""
                if (isim.isNotEmpty()) {
                    dialogBinding.tvProfilHarf.text = isim.take(1).uppercase()
                } else {
                    dialogBinding.tvProfilHarf.text = "⚽"
                }
                if (dialogBinding.etProfilUsername.text.isNullOrEmpty() && isim.isNotEmpty()) {
                    val otoUsername = "@${isim.lowercase().replace(" ", "")}"
                    dialogBinding.etProfilUsername.setText(otoUsername)
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        dialogBinding.btnProfilKaydet.setOnClickListener {
            val isim = dialogBinding.etProfilIsim.text.toString().trim()
            var rawUsername = dialogBinding.etProfilUsername.text.toString().trim().lowercase().replace(" ", "")
            val formaNo = dialogBinding.etProfilFormaNo.text.toString().trim().toIntOrNull() ?: 10

            if (isim.isEmpty()) {
                Toast.makeText(this, "Lütfen adınızı ve soyadınızı girin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (rawUsername.isEmpty()) {
                rawUsername = "@${isim.lowercase().replace(" ", "")}"
            } else if (!rawUsername.startsWith("@")) {
                rawUsername = "@$rawUsername"
            }

            val clean = rawUsername.removePrefix("@")
            val kendiKullaniciAdi = mevcut != null && clean == mevcut.username.removePrefix("@").lowercase().trim()

            fun kaydetVeKapat() {
                val yeniOyuncu = Oyuncu(
                    isim = isim,
                    username = rawUsername,
                    mevki = secilenMevki,
                    formaNo = formaNo,
                    profilFotoUri = secilenProfilFotoUri
                )

                // 1. Cihaza (SharedPreferences) kaydet
                KadroRepository.yerelProfilKaydet(this, rawUsername, isim, secilenMevki, formaNo, profilFotoUri = secilenProfilFotoUri)

                // 2. Firestore oyuncuProfilleri koleksiyonuna kaydet
                KadroRepository.oyuncuProfiliKaydet(yeniOyuncu)

                // 3. UI güncelle
                profilButonunuGuncelle()
                dialog.dismiss()

                Toast.makeText(this, "Hoş geldin $isim! Profilin oluşturuldu ⚽", Toast.LENGTH_SHORT).show()
            }

            if (kendiKullaniciAdi) {
                kaydetVeKapat()
            } else {
                KadroRepository.usernameMusaitMi(clean) { musait ->
                    runOnUiThread {
                        if (musait) {
                            kaydetVeKapat()
                        } else {
                            Toast.makeText(this, "Bu kullanıcı adı alınmış! Lütfen listeden önerilen bir adı seçin.", Toast.LENGTH_SHORT).show()
                            usernameKontrolEt(rawUsername)
                        }
                    }
                }
            }
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

    // ─── DEEP LINK İLE KATILMA KONTROLÜ ───────────────────────────────────────

    private fun deepLinkKontrolEt(intent: Intent?) {
        val data = intent?.data ?: return
        val ligAdi = data.getQueryParameter("lig")
        val takimAdi = data.getQueryParameter("takim")

        if (!ligAdi.isNullOrBlank() && !takimAdi.isNullOrBlank()) {
            katilmaOnayiGoster(ligAdi, takimAdi)
        }
    }

    private fun katilmaOnayiGoster(ligAdi: String, takimAdi: String) {
        val yerel = KadroRepository.yerelProfilGetir(this)
        if (yerel == null || yerel.username.isBlank()) {
            Toast.makeText(this, "Kadroya katılabilmek için önce profilinizi oluşturmalısınız!", Toast.LENGTH_LONG).show()
            showProfilOlusturDialog()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("⚽ Kadroya Katıl")
            .setMessage("'$ligAdi' ligi kapsamında '$takimAdi' takımına katılmak istiyor musun?\n\nOyuncu: ${yerel.isim} (${yerel.username})\nMevki: ${yerel.mevki}")
            .setPositiveButton("Katıl") { _, _ ->
                KadroRepository.takimaOyuncuEkleKontrollu(
                    ligAdi = ligAdi,
                    takimAdi = takimAdi,
                    oyuncu = yerel,
                    maxKontenjan = 10,
                    onSuccess = {
                        Toast.makeText(this, "Tebrikler! '$takimAdi' takımının kadrosuna katıldın! ⚽", Toast.LENGTH_LONG).show()
                    },
                    onLimitDolu = {
                        Toast.makeText(this, "⚠️ '$takimAdi' takımı 10 kişilik maksimum kontenjana ulaşmış!", Toast.LENGTH_LONG).show()
                    },
                    onError = { e ->
                        Toast.makeText(this, e.message ?: "Katılma başarısız oldu", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Vazgeç", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        ligListener?.remove()
    }
}