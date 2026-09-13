package com.example.sahamatik_lig.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sahamatik_lig.databinding.ItemGrupTabloBinding
import com.example.sahamatik_lig.databinding.ItemGrupTakimSatirBinding
import com.example.sahamatik_lig.model.Takim

class GrupSiralamaAdapter(
    private val grupMap: Map<String, List<Takim>>,
    private val onTakimClick: (String) -> Unit
) : RecyclerView.Adapter<GrupSiralamaAdapter.GrupViewHolder>() {

    private val grupIsimleri = grupMap.keys.toList()

    inner class GrupViewHolder(val binding: ItemGrupTabloBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GrupViewHolder {
        val binding = ItemGrupTabloBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GrupViewHolder(binding)
    }

    override fun getItemCount(): Int = grupIsimleri.size

    override fun onBindViewHolder(holder: GrupViewHolder, position: Int) {
        val grupAdi = grupIsimleri[position]
        val takimlar = grupMap[grupAdi] ?: emptyList()

        // 1. Koyu gri başlığa grup adını yaz (Örn: A Grubu, B Grubu)
        holder.binding.tvGrupBaslik.text = grupAdi

        // 2. Önceki görünümleri temizle (Hücre tekrar kullanıldığında üst üste binmesin)
        holder.binding.containerTakimSatirlari.removeAllViews()

        // 3. Grubun takımlarını puan/averaj durumuna göre sırala
        val siraliTakimlar = takimlar.sortedWith(
            compareByDescending<Takim> { it.puan }
                .thenByDescending { it.averaj }
        )

        // 4. Her takımı dinamik olarak tablonun içine ekle
        val inflater = LayoutInflater.from(holder.itemView.context)
        siraliTakimlar.forEachIndexed { index, takim ->
            val satirBinding = ItemGrupTakimSatirBinding.inflate(inflater, holder.binding.containerTakimSatirlari, false)

            satirBinding.tvSiraNo.text = "${index + 1}"
            satirBinding.tvTakimAdi.text = takim.name
            satirBinding.tvOynanan.text = "${takim.oynananMac}"
            satirBinding.tvGalibiyet.text = "${takim.galibiyet}"
            satirBinding.tvBeraberlik.text = "${takim.beraberlik}"
            satirBinding.tvMaglubiyet.text = "${takim.maglubiyet}"
            satirBinding.tvAveraj.text = "${takim.averaj}"
            satirBinding.tvPuan.text = "${takim.puan}"

            // Satıra tıklanınca takım detayını aç
            satirBinding.root.setOnClickListener {
                onTakimClick(takim.name)
            }

            holder.binding.containerTakimSatirlari.addView(satirBinding.root)
        }
    }
}