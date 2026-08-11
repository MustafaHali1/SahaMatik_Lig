package com.example.sahamatik_lig.util

import com.example.sahamatik_lig.model.Mac

object FiksturHelper {

    fun fiksturOlustur(takimlar: List<String>): ArrayList<Mac> {
        val macListesi = ArrayList<Mac>()
        val liste = takimlar.toMutableList()

        // Takım sayısı tek ise BAY (maç yapmayan) ekle
        if (liste.size % 2 != 0) {
            liste.add("BAY")
        }

        val toplamTakim = liste.size
        val toplamHafta = toplamTakim - 1
        val macSayisiHaftalik = toplamTakim / 2
        var macId = 1

        for (hafta in 1..toplamHafta) {
            for (i in 0 until macSayisiHaftalik) {
                val t1 = liste[i]
                val t2 = liste[toplamTakim - 1 - i]

                if (t1 != "BAY" && t2 != "BAY") {
                    macListesi.add(
                        Mac(
                            id = macId++,
                            hafta = hafta,
                            takim1 = t1,
                            takim2 = t2,

                        )
                    )
                }
            }
            // Takımları döndürerek fikstür oluşturma
            val sonTakim = liste.removeAt(liste.size - 1)
            liste.add(1, sonTakim)
        }

        return macListesi
    }



}