package com.example.sahamatik_lig.util

import com.example.sahamatik_lig.model.Mac

object GrupTurnuvaHelper {

    // Grup harfleri: A, B, C, D...
    private val grupHarfleri = listOf("A", "B", "C", "D", "E", "F", "G", "H")
    /**
     * Takımları rastgele karıştırıp belirtilen grup sayısına eşit şekilde dağıtır.
     * Dönen sonuç: Map<"A Grubu", List<Takım İsimleri>>
     */
    fun gruplaraDagit(takimlar: List<String>, grupSayisi: Int): Map<String, ArrayList<String>> {
        val karisikTakimlar = takimlar.shuffled()
        val gruplar = LinkedHashMap<String, ArrayList<String>>()

        // Grupları oluştur (A Grubu, B Grubu...)
        for (i in 0 until grupSayisi) {
            val grupAdi = "${grupHarfleri.getOrElse(i) { "${i + 1}" }} Grubu"
            gruplar[grupAdi] = ArrayList()
        }

        // Takımları sırayla gruplara dağıt
        val grupAnahtarlari = gruplar.keys.toList()
        for (i in karisikTakimlar.indices) {
            val grupIndex = i % grupSayisi
            val hedefGrup = grupAnahtarlari[grupIndex]
            gruplar[hedefGrup]?.add(karisikTakimlar[i])
        }

        return gruplar
    }

    /**
     * Her grup için kendi içinde lig usulü fikstür oluşturur.
     */
    fun grupFiksturleriniOlustur(gruplar: Map<String, ArrayList<String>>): Map<String, ArrayList<Mac>> {
        val grupMaclari = LinkedHashMap<String, ArrayList<Mac>>()
        for ((grupAdi, takimListesi) in gruplar) {
            grupMaclari[grupAdi] = FiksturHelper.fiksturOlustur(takimListesi)
        }
        return grupMaclari
    }
}