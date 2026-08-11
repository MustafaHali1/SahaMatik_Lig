package com.example.sahamatik_lig.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sahamatik_lig.databinding.ItemPuanDurumuBinding
import com.example.sahamatik_lig.model.TakimPuan

class PuanDurumuAdapter(private val takimListesi: List<TakimPuan>) :
    RecyclerView.Adapter<PuanDurumuAdapter.PuanViewHolder>() {

    class PuanViewHolder(val binding: ItemPuanDurumuBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PuanViewHolder {
        val binding = ItemPuanDurumuBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PuanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PuanViewHolder, position: Int) {
        val takim = takimListesi[position]

        with(holder.binding) {
            // SIRA NO (1, 2, 3...)
            tvSira.text = (position + 1).toString()

            // TAKIM BİLGİLERİ
            tvTakimAdi.text = takim.takimAdi
            tvOynanan.text = takim.oynanan.toString()
            tvGalibiyet.text = takim.galibiyet.toString()
            tvBeraberlik.text = takim.beraberlik.toString()
            tvMaglubiyet.text = takim.maglubiyet.toString()
            tvAveraj.text = takim.averaj.toString()
            tvPuan.text = takim.puan.toString()
        }
    }

    override fun getItemCount(): Int = takimListesi.size
}