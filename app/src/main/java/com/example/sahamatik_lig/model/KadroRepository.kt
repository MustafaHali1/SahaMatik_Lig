package com.example.sahamatik_lig.model

object KadroRepository {
    val tumOyuncular = mutableListOf<Oyuncu>()

    fun takimOyunculariniGetir(takimAdi: String): List<Oyuncu> {
        return tumOyuncular.filter { it.takimAdi.trim().equals(takimAdi.trim(), ignoreCase = true) }
    }

    fun oyuncuEkle(oyuncu: Oyuncu) {
        tumOyuncular.add(oyuncu)
    }
}