package com.example.sahamatik_lig.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sahamatik_lig.adapter.MacAdapter
import com.example.sahamatik_lig.databinding.DialogSkorBinding
import com.example.sahamatik_lig.databinding.FragmentMaclarBinding
import com.example.sahamatik_lig.model.Mac
import com.example.sahamatik_lig.util.FiksturHelper
import java.util.ArrayList

class MaclarFragment : Fragment() {

    private var _binding: FragmentMaclarBinding? = null
    private val binding get() = _binding!!

    private var takimIsimleri: ArrayList<String>? = null
    private var macListesi = ArrayList<Mac>()

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
        _binding = FragmentMaclarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        takimIsimleri?.let { takimlar ->
            if (macListesi.isEmpty()) {
                macListesi = FiksturHelper.fiksturOlustur(takimlar)
            }
        }

        val adapter = MacAdapter(macListesi) { secilenMac ->
            showScoreDialog(secilenMac)
        }

        binding.rvMaclar.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMaclar.adapter = adapter
    }

    private fun showScoreDialog(mac: Mac) {
        val dialogInflater = LayoutInflater.from(requireContext())
        // XML adın dialog_skor.xml olduğu için DialogSkorBinding kullanıyoruz
        val dialogBinding = DialogSkorBinding.inflate(dialogInflater)

        dialogBinding.tvTakim1.text = mac.takim1
        dialogBinding.tvTakim2.text = mac.takim2

        mac.skor1?.let { dialogBinding.Skor1.setText(it.toString()) }
        mac.skor2?.let { dialogBinding.Skor2.setText(it.toString()) }

        AlertDialog.Builder(requireContext())
            .setTitle("Maç Skoru Gir")
            .setView(dialogBinding.root)
            .setPositiveButton("Kaydet") { _, _ ->
                val s1 = dialogBinding.Skor1.text.toString().toIntOrNull()
                val s2 = dialogBinding.Skor2.text.toString().toIntOrNull()

                if (s1 != null && s2 != null) {
                    mac.skor1 = s1
                    mac.skor2 = s2
                    mac.isOynadi = true

                    binding.rvMaclar.adapter?.notifyDataSetChanged()
                    Toast.makeText(requireContext(), "Skor kaydedildi!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Lütfen tüm skorları girin!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TAKIMLAR = "takimlar"

        @JvmStatic
        fun newInstance(takimlar: ArrayList<String>) =
            MaclarFragment().apply {
                arguments = Bundle().apply {
                    putStringArrayList(ARG_TAKIMLAR, takimlar)
                }
            }
    }
}