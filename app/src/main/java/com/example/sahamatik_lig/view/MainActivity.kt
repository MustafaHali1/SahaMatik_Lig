package com.example.sahamatik_lig.view

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sahamatik_lig.adapter.LigAdapter
import com.example.sahamatik_lig.databinding.ActivityMainBinding
import com.example.sahamatik_lig.databinding.DialogAddLeagueBinding
import com.example.sahamatik_lig.model.Lig
import com.example.sahamatik_lig.view.LigDetailActivity
import android.content.Intent


// Activity'nin olduğu pakete göre değişebilir

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var ligList: ArrayList<Lig>
    private lateinit var adapter: LigAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Güncellenmiş modele uygun varsayılan ligler
        ligList = arrayListOf(
            Lig(1, "Sultanbeyli Ligi", 4, listOf("Takım A", "Takım B", "Takım C", "Takım D")),
            Lig(2, "Pendik Ligi", 2, listOf("Pendik Spor", "Sahil FC")),
            Lig(3, "Kartal Ligi", 2, listOf("Kartal SK", "Atalar FC"))
        )

        // RecyclerView ve Adapter kurulumu
         adapter = LigAdapter(ligList) { secilenLig ->
            val intent = Intent(this@MainActivity, LigDetailActivity::class.java).apply {
                putExtra("LIG_ADI", secilenLig.name)

                // Takım listesi null değilse gönder, null ise boş liste yolla ki patlamasın
                val takimArrayList = ArrayList(secilenLig.takimlar ?: emptyList())
                putStringArrayListExtra("TAKIMLAR", takimArrayList)
            }
            startActivity(intent)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        // Artı butonuna basınca pop-up açılır
        binding.fabAddLeague.setOnClickListener {
            showAddLeagueDialog()
        }
    }
      //lig ekleme
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
                // ADIM 1: LİG ADI VE TAKIM SAYISI ALMA
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
                // ADIM 2: TAKIM İSİMLERİNİ TOPLAMA VE LİSTEYE EKLEME
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
                    // YENİ LİGİ MODELİMİZE UYGUN OLUŞTURUYORUZ
                    val yeniLig = Lig(
                        id = ligList.size + 1,
                        name = leagueName,
                        takimsayisi = teamNames.size,
                        takimlar = teamNames
                    )

                    // LİSTEYE EKLE VE ADAPTER'A HABER VER
                    ligList.add(yeniLig)
                    adapter.notifyItemInserted(ligList.size - 1)

                    Toast.makeText(this, "$leagueName başarıyla kuruldu!", Toast.LENGTH_SHORT).show()
                    alertDialog.dismiss()

                    // 🚀İŞTE EKSİK OLAN KISIM BURASIYDI: DETAY EKRANINA GEÇİŞ
                    val intent = Intent(this@MainActivity, LigDetailActivity::class.java).apply {
                        putExtra("LIG_ADI", leagueName)
                        putStringArrayListExtra("TAKIMLAR", ArrayList(teamNames))
                    }
                    startActivity(intent)

                } else {
                    Toast.makeText(this, "Lütfen tüm takım isimlerini girin!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}