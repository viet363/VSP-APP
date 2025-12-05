package com.example.app.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app.Adapter.CategoryAdapter
import com.example.app.Adapter.RecommendedAdapter
import com.example.app.Adapter.SliderAdapter
import com.example.app.Helper.TinyDB
import com.example.app.Model.CategoryModel
import com.example.app.Model.SliderModel
import com.example.app.ViewModel.MainViewModel
import com.example.app.databinding.ActivityMainBinding

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var tinyDB: TinyDB
    private val viewModel: MainViewModel by viewModels()

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "MainActivity started")

        tinyDB = TinyDB(this)

        val userId = tinyDB.getString("userId", "")

        if (userId.isEmpty()) {
            Log.w(TAG, "User ID is empty or not found, redirecting to LoginActivity")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        Log.d(TAG, "User ID found: $userId")

        val profileName = tinyDB.getString("profile_name", "Khách hàng thân mến")
        binding.nametitle.text = profileName

        initBanner()
        initCategory()
        initRecommended(userId)
        initBottomMenu()
        initSearch()
    }

    private fun initSearch() {
        binding.btnSearch.setOnClickListener {
            binding.btnSearch.visibility = View.GONE
            binding.searchView.visibility = View.VISIBLE
            binding.btnSearchSubmit.visibility = View.VISIBLE
            binding.searchView.requestFocus()
        }

        binding.btnSearchSubmit.setOnClickListener {
            val query = binding.searchView.query.toString().trim()

            if (query.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên sản phẩm", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.searchProducts(query)

                viewModel.searchResults.observe(this) { results ->
                    Log.d(TAG, "Search results: ${results.size} items")
                }

                val intent = Intent(this, ListItemsActivity::class.java)
                intent.putExtra("searchQuery", query)
                startActivity(intent)
            }

            binding.searchView.setQuery("", false)
            binding.searchView.visibility = View.GONE
            binding.btnSearchSubmit.visibility = View.GONE
            binding.btnSearch.visibility = View.VISIBLE
        }
    }

    private fun initRecommended(userId: String) {
        binding.progressBarRecommend.visibility = View.VISIBLE
        binding.viewRecommendation.layoutManager = GridLayoutManager(this, 2)

        viewModel.recommended.observe(this) { items ->
            binding.progressBarRecommend.visibility = View.GONE
            if (items.isNotEmpty()) {
                Log.d(TAG, "Recommended items loaded: ${items.size} items")
                binding.viewRecommendation.adapter = RecommendedAdapter(items.toMutableList())
            } else {
                Log.d(TAG, "No recommended items found")
                Toast.makeText(this, "Không có sản phẩm đề xuất", Toast.LENGTH_SHORT).show()
            }
        }

        val userIdLong = try {
            userId.toLong()
        } catch (e: NumberFormatException) {
            Log.e(TAG, "Invalid user ID format: $userId", e)
            0L
        }

        if (userIdLong > 0L) {
            viewModel.loadRecommended(userIdLong)
        } else {
            binding.progressBarRecommend.visibility = View.GONE
            Toast.makeText(this, "Lỗi: ID người dùng không hợp lệ", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initCategory() {
        binding.progressBarCategory.visibility = View.VISIBLE

        binding.viewCategory.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        viewModel.categories.observe(this) { list ->
            binding.progressBarCategory.visibility = View.GONE

            if (list.isEmpty()) {
                Log.w(TAG, "No categories found, using default categories")
                binding.viewCategory.adapter = CategoryAdapter(
                    mutableListOf(
                        CategoryModel(1, "Electronic", "cat1.png"),
                        CategoryModel(2, "Fashion", "cat2.png")
                    )
                )
            } else {
                Log.d(TAG, "Categories loaded: ${list.size} items")
                binding.viewCategory.adapter = CategoryAdapter(list.toMutableList())
            }
        }

        viewModel.loadCategories()
    }

    private fun initBanner() {
        binding.progressBarSlider.visibility = View.VISIBLE

        val fakeBanners = listOf(
            SliderModel("banner1.png"),
            SliderModel("banner2.png")
        )
        showBanner(fakeBanners)
    }

    private fun showBanner(images: List<SliderModel>) {
        binding.progressBarSlider.visibility = View.GONE

        if (images.isEmpty()) {
            Log.w(TAG, "No banner images to show")
            return
        }

        val adapter = SliderAdapter(images, binding.viewPager2)
        binding.viewPager2.adapter = adapter
        binding.viewPager2.clipToPadding = false
        binding.viewPager2.clipChildren = false
        binding.viewPager2.offscreenPageLimit = 3

        val child = binding.viewPager2.getChildAt(0)
        if (child != null) {
            child.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        }

        Log.d(TAG, "Banner loaded with ${images.size} images")
    }

    private fun initBottomMenu() {
        binding.cartBtn.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }
        binding.profileBtn.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        binding.orderBtn.setOnClickListener {
            startActivity(Intent(this, MyOrderActivity::class.java))
        }
        binding.chatBtn.setOnClickListener {
            startActivity(Intent(this, MyChatActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "MainActivity resumed")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MainActivity destroyed")
    }
}