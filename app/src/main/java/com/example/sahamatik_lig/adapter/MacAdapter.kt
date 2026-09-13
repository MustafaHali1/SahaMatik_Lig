package com.example.sahamatik_lig.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sahamatik_lig.databinding.ItemMacBinding
import com.example.sahamatik_lig.model.Mac
import com.example.sahamatik_lig.view.MacDetailActivity

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
            holder.binding.tvMacSkor.text = "${mac.skor1} - ${mac.skor2}"
        } else {
            holder.binding.tvMacSkor.text = "VS"
        }

        // Tıklanınca doğrudan MacDetailActivity'yi açıyoruz:
        holder.binding.root.setOnClickListener {
            val context = holder.itemView.context
            val intent = android.content.Intent(context, MacDetailActivity::class.java).apply {
                putExtra("EV_TAKIM", mac.takim1)
                putExtra("DEP_TAKIM", mac.takim2)
                putExtra("SKOR", if (mac.isOynandi) "${mac.skor1} - ${mac.skor2}" else "0 - 0")
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = macListesi.size
}