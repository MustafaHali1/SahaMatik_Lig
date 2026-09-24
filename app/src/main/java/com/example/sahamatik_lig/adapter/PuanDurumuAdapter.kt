package com.example.sahamatik_lig.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sahamatik_lig.R
import com.example.sahamatik_lig.databinding.ItemPuanDurumuBinding
import com.example.sahamatik_lig.model.TakimPuan
import com.example.sahamatik_lig.util.ImagePickerHelper

class PuanDurumuAdapter(
    private val takimListesi: List<TakimPuan>,
    private val ligAdi: String = "",
    private val onTakimClick: (String) -> Unit,
    private val onTakimSilClick: (String) -> Unit
) : RecyclerView.Adapter<PuanDurumuAdapter.PuanViewHolder>() {

    class PuanViewHolder(val binding: ItemPuanDurumuBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PuanViewHolder {
        val binding = ItemPuanDurumuBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PuanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PuanViewHolder, position: Int) {
        val takim = takimListesi[position]
        val context = holder.itemView.context

        with(holder.binding) {
            tvSira.text = (position + 1).toString()
            tvTakimAdi.text = takim.takimAdi
            tvOynanan.text = takim.oynanan.toString()
            tvGalibiyet.text = takim.galibiyet.toString()
            tvBeraberlik.text = takim.beraberlik.toString()
            tvMaglubiyet.text = takim.maglubiyet.toString()
            tvAveraj.text = takim.averaj.toString()
            tvPuan.text = takim.puan.toString()

            // Takım logosunu yükle
            val logoUri = ImagePickerHelper.uriGetir(context, "logo_${ligAdi}_${takim.takimAdi}")
            if (logoUri != null) {
                ivTakimLogo.setImageURI(logoUri)
            } else {
                ivTakimLogo.setImageResource(R.drawable.bg_harf_avatar)
            }

            // Kisa tiklama - takim detayina git
            root.setOnClickListener { onTakimClick(takim.takimAdi) }

            // Uzun tiklama - takim sil
            root.setOnLongClickListener {
                onTakimSilClick(takim.takimAdi)
                true
            }
        }
    }

    override fun getItemCount(): Int = takimListesi.size
}
