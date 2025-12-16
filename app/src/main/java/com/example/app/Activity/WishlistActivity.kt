package com.example.app.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.app.Adapter.WishlistAdapter
import com.example.app.Helper.TinyDB
import com.example.app.Model.CommonResponse
import com.example.app.Model.ItemsModel
import com.example.app.Model.WishlistResponse
import com.example.app.Network.RetrofitClient
import com.example.app.databinding.ActivityWishlistBinding
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class WishlistActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWishlistBinding
    private lateinit var adapter: WishlistAdapter
    private val TAG = "WishlistActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWishlistBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initUI()
        setupAdapter()
        loadWishlist()
    }

    private fun initUI() {
        binding.backBtn.setOnClickListener { finish() }
        binding.titleTxt.text = "Sản phẩm yêu thích"

        val tinyDB = TinyDB(this)
        val token = tinyDB.getString("token", "")
        if (token.isEmpty()) {
            Toast.makeText(this, "Vui lòng đăng nhập để xem sản phẩm yêu thích", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupAdapter() {
        adapter = WishlistAdapter(mutableListOf()) { wishlistItem ->
            Log.d(TAG, "Click on wishlist item: ${wishlistItem.title}, ID: ${wishlistItem.productId}")

            val imageUrls = when (wishlistItem.picUrl) {
                is List<*> -> (wishlistItem.picUrl as List<*>).mapNotNull { it?.toString() }
                is String -> {
                    try {
                        if ((wishlistItem.picUrl as String).startsWith("[")) {
                            val jsonArray = org.json.JSONArray(wishlistItem.picUrl as String)
                            (0 until jsonArray.length()).map { jsonArray.getString(it) }
                        } else {
                            listOf(wishlistItem.picUrl as String)
                        }
                    } catch (e: Exception) {
                        listOf(wishlistItem.picUrl as String)
                    }
                }
                else -> emptyList()
            }

            val item = ItemsModel(
                id = wishlistItem.productId?.toInt() ?: 0,
                title = wishlistItem.title ?: "Không có tên",
                description = wishlistItem.description ?: "",
                price = wishlistItem.price?.toDouble() ?: 0.0,
                picUrl = imageUrls,
                rating = wishlistItem.rating?.toDouble() ?: 0.0
            )

            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("object", item)
            startActivity(intent)
        }

        binding.wishlistRecyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.wishlistRecyclerView.adapter = adapter

        adapter.onRemoveClickListener = { productId, position ->
            Log.d(TAG, "Remove item: productId=$productId, position=$position")
            removeFromWishlist(productId, position)
        }
    }

    private fun loadWishlist() {
        Log.d(TAG, "=== LOADING WISHLIST ===")

        val tinyDB = TinyDB(this)
        val token = tinyDB.getString("token", "")
        val userId = tinyDB.getLong("userId", -1L)

        Log.d(TAG, "Token available: ${token.isNotEmpty()}")
        Log.d(TAG, "User ID from storage: $userId")

        showLoading(true)

        RetrofitClient.wishlistApi().getMyWishlist()
            .enqueue(object : Callback<WishlistResponse> {
                override fun onResponse(
                    call: Call<WishlistResponse>,
                    response: Response<WishlistResponse>
                ) {
                    showLoading(false)

                    Log.d(TAG, "Wishlist response code: ${response.code()}")

                    if (response.isSuccessful) {
                        val result = response.body()
                        Log.d(TAG, "Wishlist result success: ${result?.success}")

                        if (result?.success == true) {
                            val items = result.data ?: emptyList()
                            Log.d(TAG, "Wishlist items count: ${items.size}")

                            items.forEachIndexed { index, item ->
                                Log.d(TAG, "Item $index:")
                                Log.d(TAG, "  - ID: ${item.id}")
                                Log.d(TAG, "  - ProductId: ${item.productId}")
                                Log.d(TAG, "  - Title: ${item.title}")
                                Log.d(TAG, "  - Price: ${item.price}")
                                Log.d(TAG, "  - picUrl: ${item.picUrl}")
                                Log.d(TAG, "  - picUrl type: ${item.picUrl?.javaClass?.simpleName}")
                            }

                            if (items.isNotEmpty()) {
                                adapter.updateData(items)
                                binding.emptyState.visibility = View.GONE
                                binding.wishlistRecyclerView.visibility = View.VISIBLE
                                Log.d(TAG, "Loaded ${items.size} wishlist items")
                            } else {
                                showEmptyState("Danh sách yêu thích trống")
                                Log.d(TAG, "Wishlist is empty")
                            }
                        } else {
                            showEmptyState(result?.message ?: "Không có sản phẩm yêu thích")
                            Toast.makeText(this@WishlistActivity, result?.message ?: "Lỗi tải danh sách", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        handleError(response)
                    }
                }

                override fun onFailure(call: Call<WishlistResponse>, t: Throwable) {
                    showLoading(false)
                    Log.e(TAG, "Load wishlist error: ${t.message}", t)
                    Toast.makeText(this@WishlistActivity, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                    showEmptyState("Lỗi kết nối")
                }
            })
    }

    private fun removeFromWishlist(productId: Long, position: Int) {
        Log.d(TAG, "Removing product $productId from wishlist")
        Toast.makeText(this, "Đang xóa...", Toast.LENGTH_SHORT).show()

        RetrofitClient.wishlistApi().removeFromWishlist(productId)
            .enqueue(object : Callback<CommonResponse> {
                override fun onResponse(
                    call: Call<CommonResponse>,
                    response: Response<CommonResponse>
                ) {
                    Log.d(TAG, "Remove response code: ${response.code()}")

                    if (response.isSuccessful) {
                        val result = response.body()
                        Log.d(TAG, "Remove result success: ${result?.success}")

                        if (result?.success == true) {
                            adapter.removeItem(position)
                            Toast.makeText(this@WishlistActivity, "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show()

                            if (adapter.itemCount == 0) {
                                showEmptyState("Danh sách yêu thích trống")
                            }
                        } else {
                            val errorMsg = result?.message ?: "Xóa thất bại"
                            Toast.makeText(this@WishlistActivity, errorMsg, Toast.LENGTH_SHORT).show()
                            Log.e(TAG, "Remove failed: $errorMsg")
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e(TAG, "Remove failed with code ${response.code()}: $errorBody")
                        Toast.makeText(this@WishlistActivity, "Lỗi server", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<CommonResponse>, t: Throwable) {
                    Log.e(TAG, "Remove network error: ${t.message}")
                    Toast.makeText(this@WishlistActivity, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun handleError(response: Response<WishlistResponse>) {
        try {
            val errorBody = response.errorBody()?.string()
            Log.e(TAG, "Wishlist error response: $errorBody")

            val errorJson = JSONObject(errorBody ?: "{}")
            val errorMsg = errorJson.optString("message", "Lỗi không xác định")

            if (response.code() == 401) {
                Toast.makeText(this, "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing error body: ${e.message}")
            Toast.makeText(this, "Lỗi server: ${response.code()}", Toast.LENGTH_SHORT).show()
        }
        showEmptyState("Lỗi tải danh sách")
    }

    private fun showEmptyState(message: String? = null) {
        binding.emptyState.visibility = View.VISIBLE
        binding.wishlistRecyclerView.visibility = View.GONE
        binding.progressBar.visibility = View.GONE

        try {
            val emptyStateLayout = binding.emptyState as? android.widget.LinearLayout
            if (emptyStateLayout != null) {
                for (i in 0 until emptyStateLayout.childCount) {
                    val child = emptyStateLayout.getChildAt(i)
                    if (child is android.widget.TextView) {
                        message?.let { child.text = it }
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating empty state text: ${e.message}")
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            binding.emptyState.visibility = View.GONE
            binding.wishlistRecyclerView.visibility = View.GONE
        }
    }
}