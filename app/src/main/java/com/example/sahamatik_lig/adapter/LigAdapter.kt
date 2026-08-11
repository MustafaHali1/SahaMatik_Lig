package com.example.sahamatik_lig.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sahamatik_lig.databinding.ItemLigBinding
import com.example.sahamatik_lig.model.Lig

class LigAdapter(
    private val ligList: List<Lig>,
    private val onItemClick: (Lig) -> Unit // Tıklama olayını MainActivity'ye ileten lambda
) : RecyclerView.Adapter<LigAdapter.LigViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LigViewHolder {
        val binding = ItemLigBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LigViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LigViewHolder, position: Int) {
        holder.bind(ligList[position], onItemClick)
    }

    override fun getItemCount(): Int = ligList.size

    class LigViewHolder(private val binding: ItemLigBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(lig: Lig, onItemClick: (Lig) -> Unit) {
            binding.recyclerViewTextView.text = lig.name

            // Karta tıklandığında seçilen ligi dışarı gönderiyoruz
            binding.root.setOnClickListener {
                onItemClick(lig)
            }
        }
    }
}