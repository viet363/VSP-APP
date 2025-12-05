package com.example.app.Model

data class UserAddressModel(
    val id: Long,
    val userId: Long,
    val receiverName: String?,
    val phone: String?,
    val addressDetail: String,
    val isDefault: Boolean
)

data class AddressResponse(
    val success: Boolean,
    val data: List<UserAddressModel>
)