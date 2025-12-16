package com.example.app.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app.Adapter.CategoryAdapter
import com.example.app.Adapter.RecommendedAdapter
import com.example.app.Adapter.SliderAdapter
import com.example.app.Helper.TinyDB
import com.example.app.Model.*
import com.example.app.ViewModel.MainViewModel
import com.example.app.databinding.ActivityMainBinding

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var tinyDB: TinyDB
    private val viewModel: MainViewModel by viewModels()

    private val TAG = "MainActivity"

    private val filterLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == FilterActivity.RESULT_FILTER_APPLIED) {
            val data = result.data
            val filterRequest = data?.getSerializableExtra(FilterActivity.EXTRA_FILTER_RESULT) as? FilterRequest
            filterRequest?.let {
                applyFilterOnMain(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "MainActivity started")

        tinyDB = TinyDB(this)

        val userId = tinyDB.getLong("userId", 0L)
        Log.d(TAG, "Retrieved user ID (Long): $userId")

        if (userId == 0L) {
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

        initWishlistAndFilter()

        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Log.e(TAG, "ViewModel error: $it")
            }
        }
    }

    private fun initWishlistAndFilter() {
        binding.wishlistBtn.setOnClickListener {
            openWishlistActivity()
        }

        binding.filterBtn.setOnClickListener {
            openFilterActivity()
        }

        binding.wishlistBottomBtn.setOnClickListener {
            openWishlistActivity()
        }
    }

    private fun openWishlistActivity() {
        val token = tinyDB.getString("token", "")

        if (token.isEmpty()) {
            Toast.makeText(this, "Vui lòng đăng nhập để xem sản phẩm yêu thích", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        } else {
            val intent = Intent(this, WishlistActivity::class.java)
            startActivity(intent)
        }
    }

    private fun openFilterActivity() {
        val intent = Intent(this, FilterActivity::class.java)
        filterLauncher.launch(intent)
    }

    private fun applyFilterOnMain(filterRequest: FilterRequest) {
        Toast.makeText(this, "Đang áp dụng bộ lọc...", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, ListItemsActivity::class.java)
        intent.putExtra("filterRequest", filterRequest)
        intent.putExtra("title", "Kết quả lọc")
        startActivity(intent)
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
                val intent = Intent(this, SearchActivity::class.java)
                intent.putExtra("searchQuery", query)
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }

            binding.searchView.setQuery("", false)
            binding.searchView.visibility = View.GONE
            binding.btnSearchSubmit.visibility = View.GONE
            binding.btnSearch.visibility = View.VISIBLE
        }
    }

    private fun initRecommended(userId: Long) {
        binding.progressBarRecommend.visibility = View.VISIBLE
        binding.viewRecommendation.layoutManager = GridLayoutManager(this, 2)

        binding.viewRecommendation.adapter = RecommendedAdapter(mutableListOf())

        viewModel.isShowingFallback.observe(this) { isFallback ->
            if (isFallback) {
                binding.recommendedTitle.text = "Sản phẩm từ các danh mục"
            } else {
                binding.recommendedTitle.text = "Sản phẩm đề xuất cho bạn"
            }
        }

        viewModel.recommended.observe(this) { items ->
            binding.progressBarRecommend.visibility = View.GONE

            if (items.isNotEmpty()) {
                Log.d(TAG, "Đang hiển thị ${items.size} sản phẩm")

                binding.viewRecommendation.adapter = RecommendedAdapter(items.toMutableList())
                binding.recommendedTitle.visibility = View.VISIBLE
                binding.viewRecommendation.visibility = View.VISIBLE
                binding.emptyRecommended.visibility = View.GONE
            } else {
                Log.d(TAG, "Không có sản phẩm nào để hiển thị")
                binding.recommendedTitle.visibility = View.GONE
                binding.viewRecommendation.visibility = View.GONE
                binding.emptyRecommended.visibility = View.VISIBLE
                binding.emptyRecommended.text = "Không có sản phẩm nào"
            }
        }

        if (userId > 0L) {
            viewModel.loadRecommended(userId)
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
                binding.viewCategory.adapter = CategoryAdapter(mutableListOf())
            } else {
                Log.d(TAG, "Categories loaded: ${list.size} items")
                binding.viewCategory.adapter = CategoryAdapter(list.toMutableList())
            }
        }

        viewModel.loadCategories()
    }

    private fun initBanner() {
        binding.progressBarSlider.visibility = View.VISIBLE

        viewModel.banners.observe(this) { banners ->
            binding.progressBarSlider.visibility = View.GONE

            if (banners.isNotEmpty()) {
                showBanner(banners)
            }
        }
    }

    private fun showBanner(images: List<SliderModel>) {
        if (images.isEmpty()) {
            Log.w(TAG, "No banner images to show")
            binding.viewPager2.visibility = View.GONE
            return
        }

        binding.viewPager2.visibility = View.VISIBLE
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
        binding.homeBtn.setOnClickListener {
            val userId = tinyDB.getLong("userId", 0L)
            if (userId > 0L) {
                viewModel.loadRecommended(userId)
            }
        }

        binding.cartBtn.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        binding.chatBtn.setOnClickListener {
            startActivity(Intent(this, MyChatActivity::class.java))
        }

        binding.profileBtn.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        binding.orderBtn.setOnClickListener {
            startActivity(Intent(this, MyOrderActivity::class.java))
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