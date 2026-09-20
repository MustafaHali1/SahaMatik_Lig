package com.example.sahamatik_lig.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sahamatik_lig.databinding.ItemMacBinding
import com.example.sahamatik_lig.model.Mac
import com.example.sahamatik_lig.util.ImagePickerHelper

class MacAdapter(
    private val macListesi: List<Mac>,
    private val ligAdi: String = "",
    private val onMacClick: (Mac) -> Unit
) : RecyclerView.Adapter<MacAdapter.MacViewHolder>() {

    class MacViewHolder(val binding: ItemMacBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MacViewHolder {
        val binding = ItemMacBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MacViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MacViewHolder, position: Int) {
        val mac = macListesi[position]
        val context = holder.itemView.context

        holder.binding.tvHafta.text = "${mac.hafta}. Hafta"
        holder.binding.tvTakim1.text = mac.takim1
        holder.binding.tvTakim2.text = mac.takim2

        // Skor gösterimi
        if (mac.isOynandi && mac.skor1 != null && mac.skor2 != null) {
            holder.binding.tvMacSkor.text = "${mac.skor1} - ${mac.skor2}"
        } else {
            holder.binding.tvMacSkor.text = "VS"
        }

        // Logo yükleme (varsa)
        val evLogoUri = ImagePickerHelper.uriGetir(context, "logo_${ligAdi}_${mac.takim1}")
        if (evLogoUri != null) {
            holder.binding.ivEvLogo.setImageURI(evLogoUri)
        }
        val depLogoUri = ImagePickerHelper.uriGetir(context, "logo_${ligAdi}_${mac.takim2}")
        if (depLogoUri != null) {
            holder.binding.ivDepLogo.setImageURI(depLogoUri)
        }

        // Tıklama — MacDetailActivity'yi Fragment üzerinden açacak
        holder.binding.root.setOnClickListener {
            onMacClick(mac)
        }
    }

    override fun getItemCount(): Int = macListesi.size
}
