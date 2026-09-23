package com.example.sahamatik_lig.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sahamatik_lig.databinding.ItemOyuncuAramaSonucBinding
import com.example.sahamatik_lig.model.Oyuncu

class OyuncuAramaAdapter(
    private var oyuncuListesi: List<Oyuncu>,
    private val onOyuncuSecildi: (Oyuncu) -> Unit
) : RecyclerView.Adapter<OyuncuAramaAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemOyuncuAramaSonucBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOyuncuAramaSonucBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val oyuncu = oyuncuListesi[position]
        val harf = oyuncu.isim.trim().take(1).uppercase()
        holder.binding.tvAramaHarf.text = if (harf.isNotEmpty()) harf else "O"

        val formattedUsername = if (oyuncu.username.startsWith("@")) oyuncu.username else "@${oyuncu.username}"
        holder.binding.tvAramaUsername.text = formattedUsername

        val mevkiIcon = when (oyuncu.mevki.trim()) {
            "Kaleci" -> "🧤 Kaleci"
            "Defans" -> "🛡️ Defans"
            "Orta Saha" -> "⚡ Orta Saha"
            "Forvet" -> "⚽ Forvet"
            else -> oyuncu.mevki
        }

        val macSayisi = if (oyuncu.toplamMac > 0) oyuncu.toplamMac else 0
        val golSayisi = if (oyuncu.gol > 0) oyuncu.gol else oyuncu.toplamGol
        holder.binding.tvAramaDetay.text = "${oyuncu.isim} • $mevkiIcon • $macSayisi Maç, $golSayisi Gol"

        holder.binding.root.setOnClickListener {
            onOyuncuSecildi(oyuncu)
        }
        holder.binding.btnAramaEkle.setOnClickListener {
            onOyuncuSecildi(oyuncu)
        }
    }

    override fun getItemCount(): Int = oyuncuListesi.size

    fun listeyiGuncelle(yeniListe: List<Oyuncu>) {
        oyuncuListesi = yeniListe
        notifyDataSetChanged()
    }
}
