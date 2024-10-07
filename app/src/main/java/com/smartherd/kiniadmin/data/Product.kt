package com.smartherd.kiniadmin.data

data class Product(
    val productId: String = "",  // Ensure default values are provided in case of null checks
    val name: String = "",
    val price: String = "",
    val category: String = "",
    val imageUrl: String = ""
)
