package com.example.sahamatik_lig.view

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sahamatik_lig.adapter.GrupSiralamaAdapter
import com.example.sahamatik_lig.adapter.PuanDurumuAdapter
import com.example.sahamatik_lig.databinding.FragmentSiralamaBinding
import com.example.sahamatik_lig.model.Takim
import com.example.sahamatik_lig.model.TakimPuan

class SiralamaFragment : Fragment() {
    private var _binding: FragmentSiralamaBinding? = null
    private val binding get() = _binding!!

    private var formatTipi: String = "KLASIK"
    private var takimIsimleri: ArrayList<String>? = null
    private var gruplarMap: HashMap<String, ArrayList<String>>? = null

    // Activity seviyesinde tek bir ViewModel kullanıyoruz
    private val viewModel: LigViewModel by activityViewModels()

    private lateinit var puanAdapter: PuanDurumuAdapter
    private val puanListesi = ArrayList<TakimPuan>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            formatTipi = it.getString(ARG_FORMAT, "KLASIK")
            takimIsimleri = it.getStringArrayList(ARG_TAKIMLAR)
            @Suppress("UNCHECKED_CAST")
            gruplarMap = it.getSerializable(ARG_GRUPLAR) as? HashMap<String, ArrayList<String>>
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
        binding.rvPuanDurumu.layoutManager = LinearLayoutManager(requireContext())

        if (formatTipi == "GRUP" && gruplarMap != null) {
            // Grupları ViewModel'a tanıt (eğer maçlar henüz başlamadıysa)
            viewModel.grupTurnuvasiBaslat(gruplarMap!!)

            // Puan durumu değiştikçe grup tablolarını baştan oluştur ve bas
            viewModel.puanDurumu.observe(viewLifecycleOwner) { siraliListe ->
                val grupTakimlariMap = LinkedHashMap<String, List<Takim>>()

                for ((grupAdi, takimAdlari) in gruplarMap!!) {
                    val guncelTakimlar = ArrayList<Takim>()
                    for (takimAdi in takimAdlari) {
                        // Boşlukları temizleyerek birebir eşleştir
                        val p = siraliListe.find { it.takimAdi.trim() == takimAdi.trim() }
                        guncelTakimlar.add(
                            Takim(
                                name = takimAdi,
                                oynananMac = p?.oynanan ?: 0,
                                galibiyet = p?.galibiyet ?: 0,
                                beraberlik = p?.beraberlik ?: 0,
                                maglubiyet = p?.maglubiyet ?: 0,
                                atilanGol = p?.atilanGol ?: 0,
                                yenilenGol = p?.yenilenGol ?: 0,
                                averaj = p?.averaj ?: 0,
                                puan = p?.puan ?: 0
                            )
                        )
                    }
                    grupTakimlariMap[grupAdi] = guncelTakimlar
                }

                // Adapter'ı tıklama desteğiyle bağla
                binding.rvPuanDurumu.adapter = GrupSiralamaAdapter(grupTakimlariMap) { tiklananTakimAdi ->
                    takimDetayinaGit(tiklananTakimAdi)
                }
            }

        } else {
            // Klasik Lig Düzeni - Tıklama desteğiyle başlat
            puanAdapter = PuanDurumuAdapter(puanListesi) { tiklananTakimAdi ->
                takimDetayinaGit(tiklananTakimAdi)
            }
            binding.rvPuanDurumu.adapter = puanAdapter

            takimIsimleri?.let {
                viewModel.ligiBaslat(it)
            }

            viewModel.puanDurumu.observe(viewLifecycleOwner) { siraliListe ->
                if (!siraliListe.isNullOrEmpty()) {
                    puanListesi.clear()
                    puanListesi.addAll(siraliListe)
                    puanAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun takimDetayinaGit(takimAdi: String) {
        val intent = Intent(requireContext(), TakimDetailActivity::class.java).apply {
            putExtra("TAKIM_ADI", takimAdi)
            putExtra("LIG_ADI", "Sultanbeyli Ligi")
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_FORMAT = "format_tipi"
        private const val ARG_TAKIMLAR = "takimlar"
        private const val ARG_GRUPLAR = "gruplar_map"

        @JvmStatic
        fun newInstance(takimlar: ArrayList<String>) =
            SiralamaFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_FORMAT, "KLASIK")
                    putStringArrayList(ARG_TAKIMLAR, takimlar)
                }
            }

        @JvmStatic
        fun newInstanceGrup(gruplar: HashMap<String, ArrayList<String>>) =
            SiralamaFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_FORMAT, "GRUP")
                    putSerializable(ARG_GRUPLAR, gruplar)
                }
            }
    }
}