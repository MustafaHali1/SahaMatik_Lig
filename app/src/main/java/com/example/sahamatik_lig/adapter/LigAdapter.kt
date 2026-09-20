package com.example.sahamatik_lig.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sahamatik_lig.databinding.ItemLigBinding
import com.example.sahamatik_lig.model.Lig

class LigAdapter(
    private val ligList: List<Lig>,
    private val onItemClick: (Lig) -> Unit,
    private val onItemLongClick: (Lig) -> Unit,
    private val onItemSilClick: (Lig) -> Unit
) : RecyclerView.Adapter<LigAdapter.LigViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LigViewHolder {
        val binding = ItemLigBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LigViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LigViewHolder, position: Int) {
        holder.bind(ligList[position], onItemClick, onItemLongClick, onItemSilClick)
    }

    override fun getItemCount(): Int = ligList.size

    class LigViewHolder(private val binding: ItemLigBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(lig: Lig, onItemClick: (Lig) -> Unit, onItemLongClick: (Lig) -> Unit, onItemSilClick: (Lig) -> Unit) {
            binding.tvLigAdi.text = lig.name
            binding.tvLigDetay.text = "${lig.takimsayisi} Takim - ${lig.formatTipi}"

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
