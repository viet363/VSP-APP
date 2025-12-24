package com.example.app.Network

import com.example.app.Model.AddressResponse
import com.example.app.Model.UserAddressModel
import retrofit2.Call
import retrofit2.http.*

interface AddressApi {

    @GET("mobile/address")
    fun getAddresses(): Call<AddressResponse>

    @POST("mobile/address")
    fun addAddress(@Body body: HashMap<String, String>): Call<UserAddressModel>

    @PUT("mobile/address/{id}")
    fun updateAddress(
        @Path("id") id: Long,
        @Body body: HashMap<String, String>
    ): Call<UserAddressModel>

    @DELETE("mobile/address/{id}")
    fun deleteAddress(@Path("id") id: Long): Call<UserAddressModel>
}