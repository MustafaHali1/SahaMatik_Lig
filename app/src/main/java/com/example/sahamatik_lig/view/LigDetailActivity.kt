package com.example.sahamatik_lig.view

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.sahamatik_lig.databinding.ActivityLigDetailBinding

class LigDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLigDetailBinding
    private lateinit var siralamaFragment: SiralamaFragment
    private lateinit var maclarFragment: MaclarFragment

    private val istatistiklerFragment = IstatistiklerFragment.newInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLigDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val ligAdi = intent.getStringExtra("LIG_ADI") ?: "LİG DETAY"
        val formatTipi = intent.getStringExtra("FORMAT_TIPI") ?: "KLASIK"
        val takimlar = intent.getStringArrayListExtra("TAKIMLAR") ?: arrayListOf()

        @Suppress("UNCHECKED_CAST")
        val gruplarMap = intent.getSerializableExtra("GRUPLAR_MAP") as? HashMap<String, ArrayList<String>>

        binding.tvLigTitle.text = ligAdi

        binding.tvBack.setOnClickListener { finish() }

        if (formatTipi == "GRUP" && gruplarMap != null) {
            siralamaFragment = SiralamaFragment.newInstanceGrup(gruplarMap)
            maclarFragment = MaclarFragment.newInstanceGrup(gruplarMap)
        } else {
            siralamaFragment = SiralamaFragment.newInstance(takimlar)
            maclarFragment = MaclarFragment.newInstance(takimlar)
        }

        replaceFragment(siralamaFragment)
        setButtonSelected(0)

        binding.btnSiralama.setOnClickListener {
            replaceFragment(siralamaFragment)
            setButtonSelected(0)
        }

        binding.btnMaclar.setOnClickListener {
            replaceFragment(maclarFragment)
            setButtonSelected(1)
        }

        binding.btnIstatistik.setOnClickListener {
            replaceFragment(istatistiklerFragment)
            setButtonSelected(2)
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()
    }

    private fun setButtonSelected(sekmeIndex: Int) {
        val aktifBeyaz = Color.parseColor("#F5F3EC")
        val pasifGri = Color.parseColor("#8C9E95")

        // Sıralama (0)
        binding.btnSiralama.setTextColor(if (sekmeIndex == 0) aktifBeyaz else pasifGri)
        binding.indicatorSiralama.visibility = if (sekmeIndex == 0) View.VISIBLE else View.INVISIBLE

        // Maçlar (1)
        binding.btnMaclar.setTextColor(if (sekmeIndex == 1) aktifBeyaz else pasifGri)
        binding.indicatorMaclar.visibility = if (sekmeIndex == 1) View.VISIBLE else View.INVISIBLE

        // İstatistikler (2)
        binding.btnIstatistik.setTextColor(if (sekmeIndex == 2) aktifBeyaz else pasifGri)
        binding.indicatorIstatistik.visibility = if (sekmeIndex == 2) View.VISIBLE else View.INVISIBLE
    }
}