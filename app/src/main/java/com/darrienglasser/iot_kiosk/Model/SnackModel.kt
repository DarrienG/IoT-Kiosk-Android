package com.darrienglasser.iot_kiosk.Model

import retrofit2.Call
import retrofit2.http.GET

interface SnackModel {
    @GET("/getProducts")
    fun getSnacks(): Call<List<Map<String, RShopItem>>>
}

data class RShopItem(val id: String, val price: Double, val image: String)