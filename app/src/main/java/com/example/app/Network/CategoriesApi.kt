package com.example.app.Network

import com.example.app.Model.CategoryModel
import retrofit2.Call
import retrofit2.http.GET

interface CategoriesApi {

    @GET("categories")
    fun getAllCategories(): Call<List<CategoryModel>>

}
