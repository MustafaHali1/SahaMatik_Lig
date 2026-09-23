package com.example.sahamatik_lig.model

/**
 * Firestore'dan yüklenen maç sonucunu temsil eder.
 * Mac modeli ile farkı: bu Firestore'dan gelen persist edilmiş veridir.
 */
data class MacSonucu(
    val macId: Int = 0,
    val takim1: String = "",
    val takim2: String = "",
    val skor1: Int = 0,
    val skor2: Int = 0,
    val hafta: Int = 0,
    val isOynandi: Boolean = false,
    val olaylar: List<Map<String, Any>> = emptyList(),
    val oyuncuIstatistikleri: Map<String, Map<String, Long>> = emptyMap()
)
