package com.gps.warehouse.data.remote.assets_dto

// ====================== DTO для авторизации IT Assets ======================

data class RegisterTokenRequest(
    val token: String
)

data class RegisterTokenResponse(
    val status: String,
    val msg: String,
    val data: TokenData? = null
)

data class TokenData(
    val token: String? = null
)