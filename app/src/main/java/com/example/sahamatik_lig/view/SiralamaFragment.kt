package com.example.sahamatik_lig.view

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sahamatik_lig.adapter.GrupSiralamaAdapter
import com.example.sahamatik_lig.adapter.PuanDurumuAdapter
import com.example.sahamatik_lig.databinding.FragmentSiralamaBinding
import com.example.sahamatik_lig.databinding.DialogSilmeOnayiBinding
import com.example.sahamatik_lig.model.KadroRepository
import com.example.sahamatik_lig.model.Takim
import com.example.sahamatik_lig.model.TakimPuan

class SiralamaFragment : Fragment() {
    private var _binding: FragmentSiralamaBinding? = null
    private val binding get() = _binding!!

    private var formatTipi: String = "KLASIK"
    private var takimIsimleri: ArrayList<String>? = null
    private var gruplarMap: HashMap<String, ArrayList<String>>? = null

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
            viewModel.grupTurnuvasiBaslat(gruplarMap!!)

            viewModel.puanDurumu.observe(viewLifecycleOwner) { siraliListe ->
                val grupTakimlariMap = LinkedHashMap<String, List<Takim>>()

                for ((grupAdi, takimAdlari) in gruplarMap!!) {
                    val guncelTakimlar = ArrayList<Takim>()
                    for (takimAdi in takimAdlari) {
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

                val ligAdi = arguments?.getString(ARG_LIG_ADI) ?: ""
                binding.rvPuanDurumu.adapter = GrupSiralamaAdapter(
                    grupMap = grupTakimlariMap,
                    ligAdi = ligAdi,
                    onTakimClick = { tiklananTakimAdi -> takimDetayinaGit(tiklananTakimAdi) },
                    onTakimSilClick = { tiklananTakimAdi -> showTakimSilDialog(tiklananTakimAdi) }
                )
            }

        } else {
            val ligAdi = arguments?.getString(ARG_LIG_ADI) ?: ""
            puanAdapter = PuanDurumuAdapter(
                takimListesi = puanListesi,
                ligAdi = ligAdi,
                onTakimClick = { tiklananTakimAdi -> takimDetayinaGit(tiklananTakimAdi) },
                onTakimSilClick = { tiklananTakimAdi -> showTakimSilDialog(tiklananTakimAdi) }
            )
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
            putExtra("LIG_ADI", arguments?.getString(ARG_LIG_ADI) ?: "")
        }
        startActivity(intent)
    }

    // Takim silme dialogu - Binding ile
    private fun showTakimSilDialog(takimAdi: String) {
        val ligAdi = arguments?.getString(ARG_LIG_ADI) ?: ""

        val dialogBinding = DialogSilmeOnayiBinding.inflate(LayoutInflater.from(requireContext()))
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.tvSilmeBaslik.text = "Takimi Sil"
        dialogBinding.tvSilmeAciklama.text =
            "$takimAdi takimini ve tum oyuncularini silmek istediginize emin misiniz?\n\nBu islem geri alinamaz!"

        dialogBinding.btnSilOnay.setOnClickListener {
            dialog.dismiss()
            KadroRepository.takimSil(
                ligAdi, takimAdi,
                onSuccess = {
                    viewModel.takimSil(takimAdi)
                    if (isAdded) {
                        Toast.makeText(requireContext(), "$takimAdi silindi", Toast.LENGTH_SHORT).show()
                    }
                },
                onError = { e ->
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Silme basarisiz: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        dialogBinding.btnSilIptal.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_FORMAT = "format_tipi"
        private const val ARG_TAKIMLAR = "takimlar"
        private const val ARG_GRUPLAR = "gruplar_map"
        private const val ARG_LIG_ADI = "lig_adi"

        @JvmStatic
        fun newInstance(takimlar: ArrayList<String>, ligAdi: String = "") =
            SiralamaFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_FORMAT, "KLASIK")
                    putStringArrayList(ARG_TAKIMLAR, takimlar)
                    putString(ARG_LIG_ADI, ligAdi)
                }
            }

        @JvmStatic
        fun newInstanceGrup(gruplar: HashMap<String, ArrayList<String>>, ligAdi: String = "") =
            SiralamaFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_FORMAT, "GRUP")
                    putSerializable(ARG_GRUPLAR, gruplar)
                    putString(ARG_LIG_ADI, ligAdi)
                }
            }
    }
}
