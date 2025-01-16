package com.sundalink.mapstracker

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T
)