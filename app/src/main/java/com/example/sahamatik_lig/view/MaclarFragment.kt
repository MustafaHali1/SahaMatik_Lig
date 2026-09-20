package com.example.sahamatik_lig.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sahamatik_lig.adapter.MacAdapter
import com.example.sahamatik_lig.databinding.FragmentMaclarBinding

class MaclarFragment : Fragment() {

    private var _binding: FragmentMaclarBinding? = null
    private val binding get() = _binding!!

    private var formatTipi: String = "KLASIK"
    private var takimIsimleri: ArrayList<String>? = null
    private var gruplarMap: HashMap<String, ArrayList<String>>? = null

    // Tek ViewModel referansı — activityViewModels ile
    private val viewModel: LigViewModel by activityViewModels()

    private lateinit var macAdapter: MacAdapter

    companion object {
        private const val ARG_FORMAT = "format_tipi"
        private const val ARG_TAKIMLAR = "takimlar"
        private const val ARG_GRUPLAR = "gruplar_map"
        private const val ARG_LIG_ADI = "lig_adi"
        const val REQUEST_CODE_MAC = 1001

        @JvmStatic
        fun newInstance(takimlar: ArrayList<String>, ligAdi: String = "") =
            MaclarFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_FORMAT, "KLASIK")
                    putStringArrayList(ARG_TAKIMLAR, takimlar)
                    putString(ARG_LIG_ADI, ligAdi)
                }
            }

        @JvmStatic
        fun newInstanceGrup(gruplar: HashMap<String, ArrayList<String>>, ligAdi: String = "") =
            MaclarFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_FORMAT, "GRUP")
                    putSerializable(ARG_GRUPLAR, gruplar)
                    putString(ARG_LIG_ADI, ligAdi)
                }
            }
    }

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
        _binding = FragmentMaclarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Lig başlatma — tek ViewModel referansı kullanılıyor
        if (formatTipi == "GRUP" && gruplarMap != null) {
            viewModel.grupTurnuvasiBaslat(gruplarMap!!)
        } else {
            takimIsimleri?.let { takimlar ->
                viewModel.ligiBaslat(takimlar)
            }
        }

        // Adapter kur
        macAdapter = MacAdapter(
            macListesi = viewModel.macListesi,
            ligAdi = arguments?.getString(ARG_LIG_ADI) ?: "",
            onMacClick = { secilenMac ->
                val ligAdi = arguments?.getString(ARG_LIG_ADI) ?: ""
                val intent = Intent(requireContext(), MacDetailActivity::class.java).apply {
                    putExtra("EV_TAKIM", secilenMac.takim1)
                    putExtra("DEP_TAKIM", secilenMac.takim2)
                    putExtra("LIG_ADI", ligAdi)
                    putExtra("MAC_ID", secilenMac.id)
                    putExtra("HAFTA", secilenMac.hafta)
                    if (secilenMac.isOynandi) {
                        putExtra("EV_SKOR", secilenMac.skor1 ?: 0)
                        putExtra("DEP_SKOR", secilenMac.skor2 ?: 0)
                    }
                }
                startActivityForResult(intent, REQUEST_CODE_MAC)
            }
        )

        binding.rvMaclar.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMaclar.adapter = macAdapter

        // Firestore'dan skorları yükle (uygulama kapanıp açılsa bile çalışır)
        val ligAdi = arguments?.getString(ARG_LIG_ADI) ?: ""
        if (ligAdi.isNotEmpty()) {
            viewModel.skorlariFirestoredenYukle(ligAdi)
        }

        // Skor yükleme tamamlandığında adapter güncelle
        viewModel.skorYuklendi.observe(viewLifecycleOwner) { yuklendi ->
            if (yuklendi) {
                macAdapter.notifyDataSetChanged()
            }
        }
    }

    // MacDetailActivity'den döndüğünde skoru güncelle
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_MAC && resultCode == Activity.RESULT_OK && data != null) {
            val macId = data.getIntExtra("MAC_ID", -1)
            val evSkor = data.getIntExtra("EV_SKOR", 0)
            val depSkor = data.getIntExtra("DEP_SKOR", 0)

            if (macId != -1) {
                val mac = viewModel.macListesi.find { it.id == macId }
                if (mac != null) {
                    viewModel.skorGuncelle(mac, evSkor, depSkor)
                    macAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}