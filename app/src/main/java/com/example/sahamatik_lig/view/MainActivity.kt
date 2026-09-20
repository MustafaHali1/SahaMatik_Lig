package com.example.sahamatik_lig.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sahamatik_lig.adapter.LigAdapter
import com.example.sahamatik_lig.databinding.ActivityMainBinding
import com.example.sahamatik_lig.databinding.BottomSheetLigSecimBinding
import com.example.sahamatik_lig.databinding.DialogAddLeagueBinding
import com.example.sahamatik_lig.databinding.DialogGrupTurnuvaEkleBinding
import com.example.sahamatik_lig.model.KadroRepository
import com.example.sahamatik_lig.model.Lig
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

        dialog.show()
    }

    private fun showAddGroupTournamentDialog() {
        val dialogBinding = DialogGrupTurnuvaEkleBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
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

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun showAddLeagueDialog() {
        val dialogBinding = DialogAddLeagueBinding.inflate(layoutInflater)

        val teamEditTextList = mutableListOf<EditText>()
        var currentStep = 1
        var leagueName = ""

        val builder = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Devam Et", null)
            .setNegativeButton("İptal") { dialog, _ -> dialog.dismiss() }

        val alertDialog = builder.create()
        alertDialog.show()

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
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
                        val editText = EditText(this)
                        editText.hint = "$i. Takım Adı"
                        editText.setSingleLine()

                        dialogBinding.containerTeams.addView(editText)
                        teamEditTextList.add(editText)
                    }

                    dialogBinding.layoutStep1.visibility = View.GONE
                    dialogBinding.layoutStep2.visibility = View.VISIBLE
                    dialogBinding.tvDialogTitle.text = "Takım isimlerini Gir (2/2)"

                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).text = "Kura Cek & Ligi Baslat"
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

                    // Firestore'a kaydet (Uygulama kapansa da kalıcı olacak)
                    KadroRepository.ligKaydet(yeniLig)
                    for (takimAdi in teamNames) {
                        KadroRepository.takimOlustur(leagueName, takimAdi)
                    }

                    Toast.makeText(this, "$leagueName başarıyla kuruldu!", Toast.LENGTH_SHORT).show()
                    alertDialog.dismiss()

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