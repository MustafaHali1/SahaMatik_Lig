package com.example.sahamatik_lig.model

data class Mac (
    val id: Int,
    val hafta: Int,
    val takim1: String,
    val takim2: String,
    var skor1: Int? = null,
    var skor2: Int? = null,

    ) {
    var isOynadi: Boolean= false
    var isOynandi: Boolean=false
}
