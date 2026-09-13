package com.example.sahamatik_lig.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.sahamatik_lig.databinding.FragmentIstatistiklerBinding
import com.example.sahamatik_lig.databinding.ItemIstatistikSatirBinding
import com.example.sahamatik_lig.util.ImagePickerHelper

class IstatistiklerFragment : Fragment() {

    private var _binding: FragmentIstatistiklerBinding? = null
    private val binding get() = _binding!!

    // Ortak ViewModel üzerinden lig puan durumunu dinliyoruz
    private val viewModel: LigViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIstatistiklerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.puanDurumu.observe(viewLifecycleOwner) { liste ->
            if (liste.isNullOrEmpty()) return@observe

            // 1. En çok gol atanlar (atilanGol azalan sırada)
            val enCokGolAtanlar = liste.sortedByDescending { it.atilanGol }
            binding.containerEnCokGolAtan.removeAllViews()

            enCokGolAtanlar.take(5).forEachIndexed { index, takim ->
                val satir = ItemIstatistikSatirBinding.inflate(layoutInflater, binding.containerEnCokGolAtan, false)
                satir.tvSiraNo.text = "${index + 1}"
                satir.tvTakimAdi.text = takim.takimAdi
                satir.tvDeger.text = takim.atilanGol.toString()

                // Takımın logosu kayıtlıysa çek
                ImagePickerHelper.uriGetir(requireContext(), "logo_${takim.takimAdi}")?.let { uri ->
                    satir.ivTakimLogo.setImageURI(uri)
                }

                binding.containerEnCokGolAtan.addView(satir.root)
            }

            // 2. En çok gol yiyenler (yenilenGol azalan sırada)
            val enCokGolYiyenler = liste.sortedByDescending { it.yenilenGol }
            binding.containerEnCokGolYiyen.removeAllViews()

            enCokGolYiyenler.take(5).forEachIndexed { index, takim ->
                val satir = ItemIstatistikSatirBinding.inflate(layoutInflater, binding.containerEnCokGolYiyen, false)
                satir.tvSiraNo.text = "${index + 1}"
                satir.tvTakimAdi.text = takim.takimAdi
                satir.tvDeger.text = takim.yenilenGol.toString()

                // Takımın logosu kayıtlıysa çek
                ImagePickerHelper.uriGetir(requireContext(), "logo_${takim.takimAdi}")?.let { uri ->
                    satir.ivTakimLogo.setImageURI(uri)
                }

                binding.containerEnCokGolYiyen.addView(satir.root)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = IstatistiklerFragment()
    }
}