package com.example.sahamatik_lig.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sahamatik_lig.databinding.ItemLigBinding
import com.example.sahamatik_lig.model.Lig

class LigAdapter(
    private val ligList: List<Lig>,
    private val mevcutCihazId: String,
    private val onItemClick: (Lig) -> Unit,
    private val onItemLongClick: (Lig) -> Unit,
    private val onItemSilClick: (Lig) -> Unit
) : RecyclerView.Adapter<LigAdapter.LigViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LigViewHolder {
        val binding = ItemLigBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LigViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LigViewHolder, position: Int) {
        holder.bind(ligList[position], mevcutCihazId, onItemClick, onItemLongClick, onItemSilClick)
    }

    override fun getItemCount(): Int = ligList.size

    class LigViewHolder(private val binding: ItemLigBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            lig: Lig,
            mevcutCihazId: String,
            onItemClick: (Lig) -> Unit,
            onItemLongClick: (Lig) -> Unit,
            onItemSilClick: (Lig) -> Unit
        ) {
            binding.tvLigAdi.text = lig.name
            if (lig.formatTipi == "TEKIL_MAC") {
                val ev = lig.takimlar.getOrNull(0) ?: "Ev Sahibi"
                val dep = lig.takimlar.getOrNull(1) ?: "Deplasman"
                binding.tvLigDetay.text = "⚽ Tekil Maç • $ev vs $dep"
            } else {
                val formatAdi = if (lig.formatTipi == "GRUP") "Grup Turnuvası" else "Klasik Lig"
                binding.tvLigDetay.text = "${lig.takimsayisi} Takım • $formatAdi"
            }

            // Kurucu kontrolü: Kuran kaptan mı yoksa davetli oyuncu mu?
            val isKurucu = lig.olusturanId.isEmpty() || lig.olusturanId == mevcutCihazId
            if (isKurucu) {
                binding.ivSil.setImageResource(android.R.drawable.ic_menu_delete)
                binding.ivSil.imageTintList = android.content.res.ColorStateList.valueOf(0xFFC0392B.toInt())
                binding.ivSil.contentDescription = "Ligi Sil (Kurucu)"
            } else {
                binding.ivSil.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                binding.ivSil.imageTintList = android.content.res.ColorStateList.valueOf(0xFFE67E22.toInt())
                binding.ivSil.contentDescription = "Kadrodan Ayrıl"
            }

            // Kisa tiklama - lige git
            binding.root.setOnClickListener { onItemClick(lig) }

            // Uzun tiklama
            binding.root.setOnLongClickListener {
                onItemLongClick(lig)
                true
            }

            // Sil butonu tiklama
            binding.ivSil.setOnClickListener { onItemSilClick(lig) }
        }
    }
}
