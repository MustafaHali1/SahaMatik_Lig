package com.example.sahamatik_lig.model

import java.io.Serializable

data class Takim(
    val id: Int = 0,
    val name: String,
    var oynananMac: Int = 0,
    var galibiyet: Int = 0,
    var beraberlik: Int = 0,
    var maglubiyet: Int = 0,
    var atilanGol: Int = 0,
    var yenilenGol: Int = 0,
    var averaj: Int = 0,
    var puan: Int = 0
) : Serializable