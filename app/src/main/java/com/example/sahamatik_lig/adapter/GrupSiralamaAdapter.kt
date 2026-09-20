package com.example.sahamatik_lig.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sahamatik_lig.databinding.ItemGrupTabloBinding
import com.example.sahamatik_lig.databinding.ItemGrupTakimSatirBinding
import com.example.sahamatik_lig.model.Takim

class GrupSiralamaAdapter(
    private val grupMap: Map<String, List<Takim>>,
    private val ligAdi: String = "",
    private val onTakimClick: (String) -> Unit,
    private val onTakimSilClick: (String) -> Unit = {}
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

        holder.binding.tvGrupBaslik.text = grupAdi
        holder.binding.containerTakimSatirlari.removeAllViews()

        val siraliTakimlar = takimlar.sortedWith(
            compareByDescending<Takim> { it.puan }
                .thenByDescending { it.averaj }
        )

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

            // Takım logosunu yükle
            val logoUri = com.example.sahamatik_lig.util.ImagePickerHelper.uriGetir(holder.itemView.context, "logo_${ligAdi}_${takim.name}")
            if (logoUri != null) {
                satirBinding.ivTakimLogo.setImageURI(logoUri)
            } else {
                satirBinding.ivTakimLogo.setImageResource(com.example.sahamatik_lig.R.drawable.bg_harf_avatar)
            }

            // Tiklama - takim detayina git
            satirBinding.root.setOnClickListener { onTakimClick(takim.name) }

            // Uzun tiklama - takim sil
            satirBinding.root.setOnLongClickListener {
                onTakimSilClick(takim.name)
                true
            }

            holder.binding.containerTakimSatirlari.addView(satirBinding.root)
        }
    }
}
