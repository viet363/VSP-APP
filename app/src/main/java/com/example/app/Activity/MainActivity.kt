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
import com.example.app.Model.ItemsModel
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

        tinyDB = TinyDB(this)

        // Không dùng Firebase nữa → userId lấy từ local
        val userId = tinyDB.getString("userId")
        if (userId.isNullOrEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding.nametitle.text = tinyDB.getString("profile_name") ?: "Khách hàng thân mến"

        initBanner()
        initCategory()
        initRecommended()
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
            if (query.isNotEmpty()) {
                viewModel.searchProductsByName(query)

                viewModel.searchResults.observe(this) { searchResults ->
                    if (searchResults.isNotEmpty()) {
                        val intent = Intent(this, ListItemsActivity::class.java).apply {
                            putExtra("searchQuery", query)
                            putParcelableArrayListExtra(
                                "searchResults",
                                ArrayList(searchResults)
                            )
                        }
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "Không tìm thấy sản phẩm", Toast.LENGTH_SHORT).show()
                    }
                }

            } else {
                Toast.makeText(this, "Vui lòng nhập tên sản phẩm", Toast.LENGTH_SHORT).show()
            }

            binding.searchView.setQuery("", false)
            binding.searchView.visibility = View.GONE
            binding.btnSearchSubmit.visibility = View.GONE
            binding.btnSearch.visibility = View.VISIBLE
        }
    }

    private fun initRecommended() {
        binding.progressBarRecommend.visibility = View.VISIBLE
        binding.viewRecommendation.layoutManager = GridLayoutManager(this, 2)

        viewModel.recommended.observe(this) { items ->
            binding.progressBarRecommend.visibility = View.GONE

            if (items.isNullOrEmpty()) {
                binding.viewRecommendation.adapter = RecommendedAdapter(mutableListOf())
            } else {
                binding.viewRecommendation.adapter = RecommendedAdapter(items.toMutableList())
            }
        }

        viewModel.loadRecommended()
    }

    private fun initCategory() {
        binding.progressBarCategory.visibility = View.VISIBLE
        binding.viewCategory.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        viewModel.categories.observe(this) { categories ->
            binding.progressBarCategory.visibility = View.GONE

            if (categories.isNullOrEmpty()) {
                val dummy = mutableListOf(
                    CategoryModel(1, "Electronic", "cat1.png", null, null),
                    CategoryModel(2, "Fashion", "cat2.png", null, null)
                )
                binding.viewCategory.adapter = CategoryAdapter(dummy)
            } else {
                binding.viewCategory.adapter = CategoryAdapter(categories.toMutableList())
            }
        }

        viewModel.loadCategory()
    }

    private fun initBanner() {
        binding.progressBarSlider.visibility = View.VISIBLE

        viewModel.banners.observe(this) { bannerList ->
            if (bannerList.isNullOrEmpty()) {
                val dummy = listOf(
                    SliderModel("banner1.png"),
                    SliderModel("banner2.png")
                )
                showBanner(dummy)
            } else {
                showBanner(bannerList)
            }
        }

        viewModel.loadBanners()
    }

    private fun showBanner(images: List<SliderModel>) {
        binding.progressBarSlider.visibility = View.GONE

        binding.viewPager2.adapter = SliderAdapter(images.toMutableList(), binding.viewPager2)
        binding.viewPager2.clipToPadding = false
        binding.viewPager2.clipChildren = false
        binding.viewPager2.offscreenPageLimit = 3
        binding.viewPager2.getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
    }

    private fun initBottomMenu() {
        binding.cartBtn.setOnClickListener { startActivity(Intent(this, CartActivity::class.java)) }
        binding.profileBtn.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }
        binding.orderBtn.setOnClickListener { startActivity(Intent(this, MyOrderActivity::class.java)) }
        binding.chatBtn.setOnClickListener { startActivity(Intent(this, MyChatActivity::class.java)) }
    }
}
