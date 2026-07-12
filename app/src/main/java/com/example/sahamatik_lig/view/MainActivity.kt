package com.example.sahamatik_lig.view

import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sahamatik_lig.adapter.LigAdapter
import com.example.sahamatik_lig.databinding.ActivityMainBinding
import com.example.sahamatik_lig.model.Lig

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    // NEDEN DEĞİŞTİRDİK: Kullanıcı dinamik olarak lig ekleyebilsin diye ArrayList yaptık amk.
    private lateinit var ligList: ArrayList<Lig>
    private lateinit var adapter: LigAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // İlk açılışta senin o uydurma ligleri listeye dolduruyoruz amk
        ligList = arrayListOf(
            Lig(1, "Sultanbeyli Ligi"),
            Lig(2, "Pendik Ligi"),
            Lig(3, "Kartal Ligi")
        )

        // Motoru (Adapter) oluşturup RecyclerView'a bağlıyoruz
        adapter = LigAdapter(ligList)
        binding.recyclerViewLeagues.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewLeagues.adapter = adapter

        // NEDEN EKLEDİK: Figma'dan attığın o yeşil artı butonuna basınca pop-up açılsın amk!
        binding.fabAddLeague.setOnClickListener {
            showAddLeagueDialog()
        }
    }

    // NEDEN YAZDIK: Kullanıcıdan yeni lig ismi alıp listeye zınk diye ekleyen pop-up fonksiyonu
    private fun showAddLeagueDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Yeni Turnuva Oluştur")
        builder.setMessage("Lütfen oluşturmak istediğiniz ligin adını giriniz:")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.hint = "Örn: Gedik Üniversitesi Kupası"
        builder.setView(input)

        builder.setPositiveButton("Oluştur") { dialog, which ->
            val ligName = input.text.toString().trim()

            if (ligName.isNotEmpty()) {
                // Yeni lig için benzersiz bir ID üretiyoruz (listenin boyutu + 1)
                val newId = ligList.size + 1

                // Senin o Lig(id, name) modeline uygun yeni nesneyi oluşturup listeye ekliyoruz amk
                val newLig = Lig(newId, ligName)
                ligList.add(newLig)

                // Motoru uyarıyoruz: "Kral ekrana yeni lig geldi, hemen tazele ortalığı!"
                adapter.notifyDataSetChanged()

                Toast.makeText(this, "$ligName başarıyla oluşturuldu!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Lig adı boş bırakılamaz amk!", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("İptal") { dialog, which ->
            dialog.cancel()
        }

        builder.show()
    }
}