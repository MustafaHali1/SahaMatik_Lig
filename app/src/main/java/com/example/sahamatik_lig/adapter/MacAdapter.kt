package com.example.sahamatik_lig.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sahamatik_lig.databinding.ItemMacBinding
import com.example.sahamatik_lig.model.Mac

class MacAdapter(
    private val macListesi: List<Mac>,
    private val onMacClick: (Mac) -> Unit
) : RecyclerView.Adapter<MacAdapter.MacViewHolder>() {

    class MacViewHolder(val binding: ItemMacBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MacViewHolder {
        val binding = ItemMacBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MacViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MacViewHolder, position: Int) {
        val mac = macListesi[position]

        holder.binding.tvHafta.text = "${mac.hafta}. Hafta"
        holder.binding.tvTakim1.text = mac.takim1
        holder.binding.tvTakim2.text = mac.takim2

        if (mac.isOynandi) {
            holder.binding.tvSkor.text = "${mac.skor1} - ${mac.skor2}"
        } else {
            holder.binding.tvSkor.text = "VS"
        }

        // Tıklamayı direkt root View (CardView) üzerine yazıyoruz
        holder.binding.root.setOnClickListener {
            onMacClick(mac)
        }
    }

    override fun getItemCount(): Int = macListesi.size
}