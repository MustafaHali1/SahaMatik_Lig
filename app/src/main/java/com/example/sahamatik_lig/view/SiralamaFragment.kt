package com.example.sahamatik_lig.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sahamatik_lig.adapter.PuanDurumuAdapter
import com.example.sahamatik_lig.databinding.FragmentSiralamaBinding
import com.example.sahamatik_lig.model.TakimPuan



class SiralamaFragment : Fragment() {
   private var _binding: FragmentSiralamaBinding?=null
    private val binding get() = _binding!!
    private var takimIsimleri: ArrayList<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
             takimIsimleri = it.getStringArrayList(ARG_TAKIMLAR)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSiralamaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Gelen takım isimlerini TakimPuan objelerine dönüştürüyoruz (Varsayılan 0 puanlı)
        val puanListesi = ArrayList<TakimPuan>()
        takimIsimleri?.forEach { takimAdi ->
            puanListesi.add(TakimPuan(takimAdi = takimAdi))
        }

        // Adapter Kurulumu
        val adapter = PuanDurumuAdapter(puanListesi)
        binding.rvPuanDurumu.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPuanDurumu.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TAKIMLAR = "takimlar"

        // LigDetailActivity'de hata veren newInstance metodunu burada tanımladık!
        @JvmStatic
        fun newInstance(takimlar: ArrayList<String>) =
            SiralamaFragment().apply {
                arguments = Bundle().apply {
                    putStringArrayList(ARG_TAKIMLAR, takimlar)
                }
            }
    }
}