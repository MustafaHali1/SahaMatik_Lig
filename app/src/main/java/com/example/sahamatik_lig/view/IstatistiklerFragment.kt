package com.example.sahamatik_lig.view

import android.content.Intent
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

        val ligAdi = arguments?.getString(ARG_LIG_ADI) ?: ""

        viewModel.puanDurumu.observe(viewLifecycleOwner) { liste ->
            if (liste.isNullOrEmpty()) return@observe

            // En cok gol atanlar
            val enCokGolAtanlar = liste.sortedByDescending { it.atilanGol }
            binding.containerEnCokGolAtan.removeAllViews()

            enCokGolAtanlar.take(5).forEachIndexed { index, takim ->
                val satir = ItemIstatistikSatirBinding.inflate(layoutInflater, binding.containerEnCokGolAtan, false)
                satir.tvSiraNo.text = "${index + 1}"
                satir.tvTakimAdi.text = takim.takimAdi
                satir.tvDeger.text = takim.atilanGol.toString()

                // Logo'yu ligAdi ile dene, yoksa sadece takimAdi ile dene
                val logoUri = if (ligAdi.isNotEmpty()) {
                    ImagePickerHelper.uriGetir(requireContext(), "logo_${ligAdi}_${takim.takimAdi}")
                        ?: ImagePickerHelper.uriGetir(requireContext(), "logo_${takim.takimAdi}")
                } else {
                    ImagePickerHelper.uriGetir(requireContext(), "logo_${takim.takimAdi}")
                }
                if (logoUri != null) {
                    satir.ivTakimLogo.setImageURI(logoUri)
                } else {
                    satir.ivTakimLogo.setImageResource(com.example.sahamatik_lig.R.drawable.bg_harf_avatar)
                }

                // Takım detayına git
                satir.root.setOnClickListener {
                    val intent = Intent(requireContext(), TakimDetailActivity::class.java).apply {
                        putExtra("TAKIM_ADI", takim.takimAdi)
                        putExtra("LIG_ADI", ligAdi)
                    }
                    startActivity(intent)
                }

                binding.containerEnCokGolAtan.addView(satir.root)
            }

            // En cok gol yiyenler
            val enCokGolYiyenler = liste.sortedByDescending { it.yenilenGol }
            binding.containerEnCokGolYiyen.removeAllViews()

            enCokGolYiyenler.take(5).forEachIndexed { index, takim ->
                val satir = ItemIstatistikSatirBinding.inflate(layoutInflater, binding.containerEnCokGolYiyen, false)
                satir.tvSiraNo.text = "${index + 1}"
                satir.tvTakimAdi.text = takim.takimAdi
                satir.tvDeger.text = takim.yenilenGol.toString()

                val logoUri = if (ligAdi.isNotEmpty()) {
                    ImagePickerHelper.uriGetir(requireContext(), "logo_${ligAdi}_${takim.takimAdi}")
                        ?: ImagePickerHelper.uriGetir(requireContext(), "logo_${takim.takimAdi}")
                } else {
                    ImagePickerHelper.uriGetir(requireContext(), "logo_${takim.takimAdi}")
                }
                if (logoUri != null) {
                    satir.ivTakimLogo.setImageURI(logoUri)
                } else {
                    satir.ivTakimLogo.setImageResource(com.example.sahamatik_lig.R.drawable.bg_harf_avatar)
                }

                // Takım detayına git
                satir.root.setOnClickListener {
                    val intent = Intent(requireContext(), TakimDetailActivity::class.java).apply {
                        putExtra("TAKIM_ADI", takim.takimAdi)
                        putExtra("LIG_ADI", ligAdi)
                    }
                    startActivity(intent)
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
        private const val ARG_LIG_ADI = "lig_adi"

        fun newInstance(ligAdi: String = "") = IstatistiklerFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_LIG_ADI, ligAdi)
            }
        }
    }
}
