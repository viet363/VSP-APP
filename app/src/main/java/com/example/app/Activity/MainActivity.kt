package com.example.app.Activity

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app.Adapter.CategoryAdapter
import com.example.app.Adapter.RecommendedAdapter
import com.example.app.Adapter.SliderAdapter
import com.example.app.Helper.TinyDB
import com.example.app.Model.*
import com.example.app.Network.RetrofitClient
import com.example.app.ViewModel.MainViewModel
import com.example.app.databinding.ActivityMainBinding
import com.example.app.service.NotificationPolling
import android.Manifest

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var tinyDB: TinyDB
    private val viewModel: MainViewModel by viewModels()

    private var notificationPolling: NotificationPolling? = null

    private val TAG = "MainActivity"

    private val filterLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == FilterActivity.RESULT_FILTER_APPLIED) {
            val data = result.data
            val filterRequest = data?.getSerializableExtra(FilterActivity.EXTRA_FILTER_RESULT) as? FilterRequest
            filterRequest?.let {
                Log.d(TAG, "Filter request received from FilterActivity: $filterRequest")
                applyFilterOnMain(it)
            }
        }
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "MainActivity started")

        tinyDB = TinyDB(this)

        binding.filterBtn.isClickable = true
        binding.filterBtn.isFocusable = true
        binding.filterBtn.isEnabled = true
        checkNotificationPermission()
        binding.filterBtn.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    Log.d(TAG, "Filter button touched")
                    v.performClick()
                }
            }
            false
        }

        val userId = tinyDB.getLong("userId", 0L)
        Log.d(TAG, "Retrieved user ID (Long): $userId")

        if (userId == 0L) {
            Log.w(TAG, "User ID is empty or not found, redirecting to LoginActivity")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        Log.d(TAG, "User ID found: $userId")

        notificationPolling = NotificationPolling(
            this,
            RetrofitClient.notificationApi(),
            userId
        )
        notificationPolling?.start()

        val profileName = tinyDB.getString("profile_name", "Khách hàng thân mến")
        binding.nametitle.text = profileName

        initBanner()
        initCategory()
        initBottomMenu()
        initSearch()

        initWishlistAndFilter()

        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Log.e(TAG, "ViewModel error: $it")
                Toast.makeText(this, "Lỗi: $it", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_DENIED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            }
        }
    }


    override fun onResume() {
        super.onResume()
        Log.d(TAG, "MainActivity resumed - Loading recommended products")

        // Luôn load sản phẩm đề xuất khi resume
        val userId = tinyDB.getLong("userId", 0L)
        if (userId > 0L) {
            initRecommended(userId)
        }
    }

    private fun initWishlistAndFilter() {
        Log.d(TAG, "Initializing filter button click listener")

        binding.wishlistBtn.setOnClickListener {
            openWishlistActivity()
        }

        binding.filterBtn.setOnClickListener {
            Log.d(TAG, "Filter button clicked")

            binding.filterBtn.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100)
                .withEndAction {
                    binding.filterBtn.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                }.start()

            Toast.makeText(this, "Đang mở bộ lọc...", Toast.LENGTH_SHORT).show()

            openFilterActivity()
        }

        binding.filterBtn.setOnLongClickListener {
            Toast.makeText(this, "Lọc sản phẩm theo giá, đánh giá, kho hàng", Toast.LENGTH_LONG).show()
            true
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
        try {
            val intent = Intent(this, FilterActivity::class.java)
            filterLauncher.launch(intent)

            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening FilterActivity: ${e.message}")
            Toast.makeText(this, "Không thể mở bộ lọc. Vui lòng thử lại!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyFilterOnMain(filterRequest: FilterRequest) {
        Log.d(TAG, "APPLYING FILTER ON MAIN")
        Log.d(TAG, "Filter request: $filterRequest")

        Toast.makeText(this, "Đang áp dụng bộ lọc...", Toast.LENGTH_SHORT).show()

        // Clear sản phẩm hiện tại trước khi filter
        binding.viewRecommendation.adapter = RecommendedAdapter(mutableListOf())
        binding.progressBarRecommend.visibility = View.VISIBLE
        binding.recommendedTitle.text = "Đang lọc sản phẩm..."

        viewModel.filterProducts(filterRequest)

        // Sử dụng observer thay vì Handler
        viewModel.recommended.observe(this, Observer { items ->
            if (viewModel.isLoading.value == false) {
                if (!items.isNullOrEmpty()) {
                    Log.d(TAG, "Filter successful, opening ListItemsActivity with ${items.size} products")

                    val intent = Intent(this, ListItemsActivity::class.java)
                    intent.putExtra("filterRequest", filterRequest)
                    intent.putExtra("title", "Kết quả lọc (${items.size} sản phẩm)")

                    val serializableList = ArrayList(items.map { it.toSerializableItem() })
                    intent.putExtra("filteredProducts", serializableList)

                    startActivity(intent)
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

                    // Reset lại sản phẩm trong MainActivity
                    resetMainToRecommended()
                } else {
                    Toast.makeText(this, "Không tìm thấy sản phẩm phù hợp", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "No products found for filter")
                    resetMainToRecommended()
                }
            }
        })
    }

    private fun resetMainToRecommended() {
        Log.d(TAG, "Resetting MainActivity to show recommended products")

        viewModel.recommended.removeObservers(this)

        val userId = tinyDB.getLong("userId", 0L)
        if (userId > 0L) {
            initRecommended(userId)
        }
    }

    private fun ItemsModel.toSerializableItem(): SerializableItemsModel {
        return SerializableItemsModel(
            id = this.id.toLong(),
            name = this.title ?: "",
            price = this.price,
            picUrl = this.picUrl,
            rating = (this.rating ?: 0.0).toFloat(),
            isRecommended = this.isRecommended
        )
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

        // Observer cho loading
        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                binding.progressBarRecommend.visibility = View.VISIBLE
            }
        }

        if (userId > 0L) {
            Log.d(TAG, "Loading recommended products for user: $userId")
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
            Log.d(TAG, "Home button clicked - Refreshing recommended products")
            val userId = tinyDB.getLong("userId", 0L)
            if (userId > 0L) {
                // Remove observer cũ
                viewModel.recommended.removeObservers(this)
                // Load lại
                initRecommended(userId)
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

    override fun onDestroy() {
        notificationPolling?.stop()
        super.onDestroy()
        Log.d(TAG, "MainActivity destroyed")
    }
}