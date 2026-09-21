package com.example.sahamatik_lig.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sahamatik_lig.adapter.LigAdapter
import com.example.sahamatik_lig.databinding.ActivityMainBinding
import com.example.sahamatik_lig.databinding.BottomSheetLigSecimBinding
import com.example.sahamatik_lig.databinding.DialogAddLeagueBinding
import com.example.sahamatik_lig.databinding.DialogGrupTurnuvaEkleBinding
import com.example.sahamatik_lig.databinding.DialogHizliMacOlusturBinding
import com.example.sahamatik_lig.model.KadroRepository
import com.example.sahamatik_lig.model.Lig
import com.example.sahamatik_lig.model.Oyuncu
import com.example.sahamatik_lig.databinding.DialogSilmeOnayiBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.ListenerRegistration

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var ligList: ArrayList<Lig>
    private lateinit var adapter: LigAdapter
    private var ligListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ligList = ArrayList()

        adapter = LigAdapter(ligList,
            onItemClick = { secilenLig ->
                if (secilenLig.formatTipi == "TEKIL_MAC") {
                    val evTakim = secilenLig.takimlar.getOrNull(0) ?: "Ev Sahibi"
                    val depTakim = secilenLig.takimlar.getOrNull(1) ?: "Deplasman"
                    val intent = Intent(this@MainActivity, MacDetailActivity::class.java).apply {
                        putExtra("EV_TAKIM", evTakim)
                        putExtra("DEP_TAKIM", depTakim)
                        putExtra("LIG_ADI", secilenLig.name)
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

        // KRİTİK NOKTA: Firestore'daki ligleri çek ve ekrana bas
        ligleriYukle()

        // Bir kez eski ust duzey "oyuncular" koleksiyonunu temizle
        val prefs = getSharedPreferences("SahamatikGorseller", MODE_PRIVATE)
        if (!prefs.getBoolean("oyuncularTemizlendi", false)) {
            KadroRepository.eskiOyuncuKoleksiyonunuSil(
                onSuccess = { prefs.edit().putBoolean("oyuncularTemizlendi", true).apply() }
            )
        }
    }

    private fun ligleriYukle() {
        ligListener = KadroRepository.ligleriCanliDinle(
            onUpdate = { gelenLigler ->
                ligList.clear()
                ligList.addAll(gelenLigler)
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
                    dialogBinding.tvTakim1KadroBilgi.text = "$takim1Adi için 7 oyuncu girin:"
                    dialogBinding.btnHizliMacDevam.text = "2. Takıma Geç"
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
                    inputs.forEachIndexed { index, (isim, mevki) ->
                        val finalIsim = if (isim.isNotEmpty()) isim else "$mevki ${index + 1}"
                        takim1Oyuncular.add(Pair(finalIsim, mevki))
                    }

                    // 3. Adıma Geç (Takım 2 Kadrosu)
                    currentStep = 3
                    dialogBinding.layoutMacAdim2.visibility = View.GONE
                    dialogBinding.layoutMacAdim3.visibility = View.VISIBLE
                    dialogBinding.tvHizliMacBaslik.text = "⚪ $takim2Adi Kadrosu (3/3)"
                    dialogBinding.tvTakim2KadroBilgi.text = "$takim2Adi için 7 oyuncu girin:"
                    dialogBinding.btnHizliMacDevam.text = "Maçı Başlat ⚽"
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

                    takim2Oyuncular.clear()
                    inputs.forEachIndexed { index, (isim, mevki) ->
                        val finalIsim = if (isim.isNotEmpty()) isim else "$mevki ${index + 1}"
                        takim2Oyuncular.add(Pair(finalIsim, mevki))
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
            olusturulmaTarihi = System.currentTimeMillis()
        )

        KadroRepository.ligKaydet(
            lig = tekilLig,
            onSuccess = {
                KadroRepository.takimOlustur(macAdi, takim1, onSuccess = {
                    t1Oyuncular.forEach { (isim, mevki) ->
                        KadroRepository.oyuncuEkle(
                            Oyuncu(isim = isim, mevki = mevki, takimAdi = takim1, ligAdi = macAdi)
                        )
                    }

                    KadroRepository.takimOlustur(macAdi, takim2, onSuccess = {
                        var t2Eklenen = 0
                        val toplamT2 = t2Oyuncular.size
                        t2Oyuncular.forEach { (isim, mevki) ->
                            KadroRepository.oyuncuEkle(
                                Oyuncu(isim = isim, mevki = mevki, takimAdi = takim2, ligAdi = macAdi),
                                onSuccess = {
                                    t2Eklenen++
                                    if (t2Eklenen == toplamT2) {
                                        val intent = Intent(this@MainActivity, MacDetailActivity::class.java).apply {
                                            putExtra("EV_TAKIM", takim1)
                                            putExtra("DEP_TAKIM", takim2)
                                            putExtra("LIG_ADI", macAdi)
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
                gruplarMap = gruplar
            )

            KadroRepository.ligKaydet(yeniLig)
            for (takimAdi in takimListesi) {
                KadroRepository.takimOlustur(turnuvaAdi, takimAdi)
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
                        formatTipi = "KLASIK"
                    )

                    // Firestore'a kaydet
                    KadroRepository.ligKaydet(yeniLig)
                    for (takimAdi in teamNames) {
                        KadroRepository.takimOlustur(leagueName, takimAdi)
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

    // Ligi silme dialogu - Binding ile
    private fun showLigSilDialog(lig: Lig) {
        val dialogBinding = DialogSilmeOnayiBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.tvSilmeBaslik.text = "Ligi Sil"
        dialogBinding.tvSilmeAciklama.text =
            "${lig.name} ligini ve tum takimlarini, oyuncularini silmek istediginize emin misiniz?\n\nBu islem geri alinamaz!"

        dialogBinding.btnSilOnay.setOnClickListener {
            dialog.dismiss()
            KadroRepository.ligSil(
                lig.name,
                onSuccess = {
                    if (!isFinishing && !isDestroyed) {
                        Toast.makeText(this, "${lig.name} silindi", Toast.LENGTH_SHORT).show()
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

    override fun onDestroy() {
        super.onDestroy()
        ligListener?.remove()
    }
}