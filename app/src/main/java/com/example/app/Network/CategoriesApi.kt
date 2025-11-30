package com.example.app.Network

import com.example.app.Model.CategoryResponse
import retrofit2.Call
import retrofit2.http.GET

interface CategoriesApi {
    @GET("categories")
    fun getCategories(): Call<List<CategoryResponse>>
}
