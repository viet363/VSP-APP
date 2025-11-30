package com.example.app.Network

import com.example.app.Model.UserAddressModel
import retrofit2.Call
import retrofit2.http.*

interface AddressApi {

    @GET("address/{userId}")
    fun getAddresses(@Path("userId") userId: Long): Call<List<UserAddressModel>>

    @POST("address/add")
    fun addAddress(@Body body: HashMap<String, String>): Call<UserAddressModel>

    @PUT("address/update")
    fun updateAddress(@Body body: HashMap<String, String>): Call<UserAddressModel>

    @DELETE("address/delete/{id}")
    fun deleteAddress(@Path("id") id: Long): Call<UserAddressModel>
}
