package com.example.app.Network

import android.content.Context
import android.util.Log
import com.example.app.Helper.TinyDB
import com.example.app.R
import okhttp3.Interceptor
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

    private val retrofit: Retrofit by lazy {
        createRetrofitWithAuth()
    }

    private val publicRetrofit: Retrofit by lazy {
        createPublicRetrofit()
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        baseUrl = context.getString(R.string.base_url)
        Log.d(TAG, "RetrofitClient initialized with BASE_URL: $baseUrl")
    }

    fun refreshToken() {

        Log.d(TAG, "Token refreshed (retrofit will use latest token from TinyDB)")
    }

    private fun createPublicRetrofit(): Retrofit {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun createRetrofitWithAuth(): Retrofit {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = Interceptor { chain ->
            val tinyDB = TinyDB(appContext)
            val token = tinyDB.getString("token", "")

            Log.d(TAG, "Using token for request: ${if (token.isNotEmpty()) "YES (${token.take(10)}...)" else "NO"}")

            val requestBuilder = chain.request().newBuilder()
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")

            if (token.isNotEmpty()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }

            chain.proceed(requestBuilder.build())
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun authApi(): AuthApi = publicRetrofit.create(AuthApi::class.java)
    fun categoriesApi(): CategoriesApi = publicRetrofit.create(CategoriesApi::class.java)
    fun productsApi(): ProductsApi = publicRetrofit.create(ProductsApi::class.java)
    fun recommendApi(): RecommendApiService = publicRetrofit.create(RecommendApiService::class.java)

    fun userApi(): UserApi = retrofit.create(UserApi::class.java)
    fun ordersApi(): OrdersApi = retrofit.create(OrdersApi::class.java)
    fun paymentApi(): PaymentApi = retrofit.create(PaymentApi::class.java)
    fun cartApi(): CartApi = retrofit.create(CartApi::class.java)
    fun chatApi(): ChatApi = retrofit.create(ChatApi::class.java)
    fun wishlistApi(): WishlistApi = retrofit.create(WishlistApi::class.java)
    fun reviewApi(): ReviewApiService = retrofit.create(ReviewApiService::class.java)
}