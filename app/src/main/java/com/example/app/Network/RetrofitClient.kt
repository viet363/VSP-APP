// RetrofitClient.kt
package com.example.app.Network

import android.content.Context
import android.util.Log
import com.example.app.Helper.TinyDB
import com.example.app.R
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val TAG = "RetrofitClient"
    private const val CONNECT_TIMEOUT = 90L
    private const val READ_TIMEOUT = 90L
    private const val WRITE_TIMEOUT = 180L

    private lateinit var appContext: Context
    private lateinit var baseUrl: String

    // Thêm property retrofit
    private val retrofit: Retrofit by lazy {
        getPublicClient()
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        baseUrl = context.getString(R.string.base_url)
        Log.d(TAG, "RetrofitClient initialized with BASE_URL: $baseUrl")
    }

    private fun getAuthClient(): Retrofit {
        val tinyDB = TinyDB(appContext)

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val token = tinyDB.getString("token", "")
                Log.d(TAG, "Token present: ${token.isNotEmpty()}")

                val original = chain.request()
                val builder = original.newBuilder()

                if (token.isNotEmpty()) {
                    builder.addHeader("Authorization", "Bearer $token")
                }

                builder.addHeader("Content-Type", "application/json")
                builder.addHeader("Accept", "application/json")

                chain.proceed(builder.build())
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun getPublicClient(): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun create(): ApiInterface {
        return retrofit.create(ApiInterface::class.java)
    }

    fun authApi(): AuthApi = getPublicClient().create(AuthApi::class.java)
    fun userApi(): UserApi = getAuthClient().create(UserApi::class.java)
    fun categoriesApi(): CategoriesApi = getPublicClient().create(CategoriesApi::class.java)
    fun productsApi(): ProductsApi = getPublicClient().create(ProductsApi::class.java)
    fun recommendApi(): RecommendApiService = getPublicClient().create(RecommendApiService::class.java)
    fun ordersApi(): OrdersApi = getAuthClient().create(OrdersApi::class.java)
    fun paymentApi(): PaymentApi = getAuthClient().create(PaymentApi::class.java)
    fun cartApi(): CartApi = getAuthClient().create(CartApi::class.java)
    fun chatApi(): ChatApi = getAuthClient().create(ChatApi::class.java)

}