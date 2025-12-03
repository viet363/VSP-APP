package com.example.app.Network

import android.content.Context
import android.util.Log
import com.example.app.Helper.TinyDB
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val TAG = "RetrofitClient"
    private const val BASE_URL = "http://192.168.1.100:4000/api/mobile/"
    //private const val BASE_URL = "http://192.168.3.84:4000/api/mobile/"

    private const val CACHE_SIZE = 10 * 1024 * 1024L // 10 MB cache

    // Cache for Retrofit instances to avoid recreation
    private val retrofitCache = mutableMapOf<String, Retrofit>()

    // Public APIs without auth - lazy loaded
    private val publicRetrofit: Retrofit by lazy {
        createRetrofitClient(null)
    }

    private fun createRetrofitClient(context: Context?): Retrofit {
        Log.d(TAG, "Creating Retrofit client with BASE_URL: $BASE_URL")

        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        // Add cache for better performance
        context?.let {
            val cacheDir = File(it.cacheDir, "http_cache")
            val cache = Cache(cacheDir, CACHE_SIZE)
            clientBuilder.cache(cache)
        }

        // Add logging interceptor for debug builds
        val loggingInterceptor = HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
            override fun log(message: String) {
                Log.d("HTTP", message)
            }
        }).apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Always add logging interceptor but control the level
        clientBuilder.addInterceptor(loggingInterceptor)

        // Add auth interceptor if context is provided
        context?.let { ctx ->
            clientBuilder.addInterceptor { chain ->
                val tinyDB = TinyDB(ctx)
                val token = tinyDB.getString("token")

                Log.d(TAG, "Token present: ${!token.isNullOrEmpty()}")

                val requestBuilder = chain.request().newBuilder()
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")

                if (!token.isNullOrEmpty()) {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }

                val request = requestBuilder.build()

                // Log request details
                Log.d(TAG, "Request URL: ${request.url}")
                Log.d(TAG, "Request Headers: ${request.headers}")

                chain.proceed(request)
            }
        }

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(clientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun getAuthRetrofit(context: Context): Retrofit {
        val key = "auth_${context.hashCode()}"
        return retrofitCache.getOrPut(key) {
            createRetrofitClient(context)
        }
    }

    // XÓA HOÀN TOÀN FUNCTION NÀY - dòng 95 đến 97
    // private fun getPublicRetrofit(): Retrofit = publicRetrofit

    // Auth APIs
    fun authApi(context: Context): AuthApi {
        return try {
            val retrofit = getAuthRetrofit(context)
            retrofit.create(AuthApi::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating AuthApi: ${e.message}", e)
            throw e
        }
    }

    fun userApi(context: Context): UserApi {
        return getAuthRetrofit(context).create(UserApi::class.java)
    }

    // Public APIs (no authentication required)
    // SỬA: thay thế getPublicRetrofit() bằng publicRetrofit
    val categoriesApi: CategoriesApi by lazy {
        publicRetrofit.create(CategoriesApi::class.java)
    }

    val productsApi: ProductsApi by lazy {
        publicRetrofit.create(ProductsApi::class.java)
    }

    val recommendApi: RecommendApiService by lazy {
        publicRetrofit.create(RecommendApiService::class.java)
    }

    val ordersApi: OrdersApi by lazy {
        publicRetrofit.create(OrdersApi::class.java)
    }

    val paymentApi: PaymentApi by lazy {
        publicRetrofit.create(PaymentApi::class.java)
    }

    val cartApi: CartApi by lazy {
        publicRetrofit.create(CartApi::class.java)
    }

    fun clearCache() {
        retrofitCache.clear()
        Log.d(TAG, "Retrofit cache cleared")
    }

    fun updateBaseUrl(newBaseUrl: String) {
        Log.d(TAG, "Base URL updated to: $newBaseUrl")
    }
}