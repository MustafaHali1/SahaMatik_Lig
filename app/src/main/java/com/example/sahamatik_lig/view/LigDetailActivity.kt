package com.example.sahamatik_lig.view

import android.graphics.Color
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.sahamatik_lig.databinding.ActivityLigDetailBinding
import androidx.fragment.app.Fragment

class LigDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLigDetailBinding
    private lateinit var siralamaFragment: SiralamaFragment
    private lateinit var maclarFragment: MaclarFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLigDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Intent ile gelen Lig Adı ve Takımları alıyoruz
        val ligAdi = intent.getStringExtra("LIG_ADI")?:"LIG DETAY"
        val takimlar = intent.getStringArrayListExtra("TAKIMLAR")?:arrayListOf()

        binding.tvLigTitle.text=ligAdi

        // Geri Butonu
        binding.tvBack.setOnClickListener { finish() }


        // Fragment instancelarını oluşturuyoruz
        siralamaFragment = SiralamaFragment.newInstance(takimlar)
        maclarFragment = MaclarFragment.newInstance(takimlar)

        // İlk açılışta Sıralama Fragment'ını basıyoruz
        replaceFragment(siralamaFragment)
        setButtonSelected(isSiralamaSelected = true)

        // Buton Tıklamaları - Tamamen Binding
        binding.btnSiralama.setOnClickListener {
            replaceFragment(siralamaFragment)
            setButtonSelected(isSiralamaSelected = true)
        }

        binding.btnMaclar.setOnClickListener {
            replaceFragment(maclarFragment)
            setButtonSelected(isSiralamaSelected = false)
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()
    }

    // Renk değişimlerini renk koduyla View Binding üzerinden yapıyoruz
    private fun setButtonSelected(isSiralamaSelected: Boolean) {
        if (isSiralamaSelected) {
            binding.btnSiralama.setBackgroundColor(Color.parseColor("#00C853")) // Yeşil
            binding.btnSiralama.setTextColor(Color.WHITE)

            binding.btnMaclar.setBackgroundColor(Color.parseColor("#D9D9D9")) // Gri
            binding.btnMaclar.setTextColor(Color.BLACK)
        } else {
            binding.btnMaclar.setBackgroundColor(Color.parseColor("#00C853")) // Yeşil
            binding.btnMaclar.setTextColor(Color.WHITE)

            binding.btnSiralama.setBackgroundColor(Color.parseColor("#D9D9D9")) // Gri
            binding.btnSiralama.setTextColor(Color.BLACK)
        }

        }
    }
