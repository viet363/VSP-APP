package com.example.app.Model

import com.google.gson.annotations.SerializedName

data class UserAddressModel(
    @SerializedName("Id")
    val id: Long? = null,

    @SerializedName("UserId")
    val userId: Long? = null,

    @SerializedName("Receiver_name")
    val receiverName: String? = null,

    @SerializedName("Phone")
    val phone: String? = null,

    @SerializedName("Address_detail")
    val addressDetail: String = "",

    @SerializedName("Is_default")
    val isDefault: Int = 0
) : java.io.Serializable {

    val isDefaultBoolean: Boolean
        get() = isDefault == 1
}

data class AddressResponse(
    val success: Boolean,
    val count: Int,
    val data: List<UserAddressModel>
)