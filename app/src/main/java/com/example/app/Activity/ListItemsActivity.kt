package com.example.app.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import com.example.app.Adapter.RecommendedAdapter
import com.example.app.Helper.TinyDB
import com.example.app.Model.FilterRequest
import com.example.app.Model.SerializableItemsModel
import com.example.app.ViewModel.MainViewModel
import com.example.app.databinding.ActivityListItemsBinding

class ListItemsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListItemsBinding
    private val viewModel: MainViewModel by viewModels()

    private var id: String = ""
    private var title: String = ""
    private var searchQuery: String = ""
    private var filterRequest: FilterRequest? = null
    private var filteredProducts: ArrayList<SerializableItemsModel>? = null
    private var isFilterApplied: Boolean = false

    private val TAG = "ListItemsActivity"

    private val filterLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == FilterActivity.RESULT_FILTER_APPLIED) {
            val data = result.data
            val newFilterRequest = data?.getSerializableExtra(FilterActivity.EXTRA_FILTER_RESULT) as? FilterRequest
            newFilterRequest?.let {
                applyFilter(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityListItemsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSystemBack()
        setupBackButton()
        getBundle()
        setupFilterButton()
        setupClearFilterButton()

        // Cập nhật trạng thái lọc ngay khi nhận bundle
        isFilterApplied = filterRequest != null || (filteredProducts != null && filteredProducts!!.isNotEmpty())
        updateClearFilterButtonVisibility()

        if (filteredProducts != null && filteredProducts!!.isNotEmpty()) {
            showFilteredProducts(filteredProducts!!)
        } else if (filterRequest != null) {
            applyFilter(filterRequest!!)
        } else if (searchQuery.isNotEmpty()) {
            setupSearchObservers()
            callSearchApi(searchQuery)
        } else {
            setupRecommendedObserver()
            initList()
        }
    }

    private fun setupSystemBack() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d(TAG, "System back pressed")
                finish()
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun setupBackButton() {
        binding.backBtn.setOnClickListener {
            Log.d(TAG, "Back button clicked")
            onBackPressed()
        }
    }

    // THÊM: Hàm setup nút xóa lọc
    private fun setupClearFilterButton() {
        binding.clearFilterBtn.setOnClickListener {
            Log.d(TAG, "Clear filter button clicked - Returning to MainActivity")
            returnToMainActivity()
        }
    }

    private fun setupFilterButton() {
        binding.filterBtn.setOnClickListener {
            openFilterActivity()
        }
    }

    // THÊM: Hàm cập nhật hiển thị nút xóa lọc
    private fun updateClearFilterButtonVisibility() {
        binding.clearFilterBtn.visibility = if (isFilterApplied) {
            View.VISIBLE
        } else {
            View.GONE
        }
        Log.d(TAG, "Clear filter button visibility: ${if (isFilterApplied) "VISIBLE" else "GONE"}")
    }

    // THÊM: Hàm quay về MainActivity
    private fun returnToMainActivity() {
        Log.d(TAG, "Returning to MainActivity - Clearing all filters")

        // Tạo intent quay về MainActivity
        val intent = Intent(this, MainActivity::class.java)

        // Clear tất cả extras để đảm bảo về trang chủ sạch
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

        // Có thể thêm extra để MainActivity biết là từ xóa lọc quay về
        intent.putExtra("clearFilter", true)

        // Start activity
        startActivity(intent)

        // Animation
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

        // Kết thúc activity hiện tại
        finish()

        // Hiển thị thông báo
        Toast.makeText(this, "Đã xóa bộ lọc và quay về trang chủ", Toast.LENGTH_SHORT).show()
    }

    private fun showFilteredProducts(products: List<SerializableItemsModel>) {
        Log.d(TAG, "Showing ${products.size} filtered products")

        val itemsList = products.map { serializableItem ->
            com.example.app.Model.ItemsModel(
                id = serializableItem.id.toInt(),
                title = serializableItem.name,
                description = null,
                price = serializableItem.price,
                picUrl = serializableItem.picUrl.toMutableList(),
                rating = serializableItem.rating.toDouble(),
                isRecommended = serializableItem.isRecommended
            )
        }

        binding.viewList.layoutManager = GridLayoutManager(this, 2)
        binding.viewList.adapter = RecommendedAdapter(itemsList.toMutableList())
        binding.categoryTxt.text = "Kết quả lọc (${itemsList.size} sản phẩm)"
        binding.progressBarList.visibility = View.GONE

        // Cập nhật trạng thái lọc
        isFilterApplied = true
        updateClearFilterButtonVisibility()
    }

    private fun setupSearchObservers() {
        viewModel.searchResults.observe(this, Observer { results ->
            Log.d(TAG, "Search results received: ${results?.size ?: 0} items")

            if (viewModel.isLoading.value == false) {
                binding.progressBarList.visibility = View.GONE

                if (!results.isNullOrEmpty()) {
                    Log.d(TAG, "Search API success: ${results.size} items")
                    binding.categoryTxt.text = "Kết quả cho: '$searchQuery' (${results.size} sản phẩm)"

                    binding.viewList.layoutManager = GridLayoutManager(this, 2)
                    binding.viewList.adapter = RecommendedAdapter(results.toMutableList())

                    // Tắt trạng thái lọc khi hiển thị kết quả tìm kiếm
                    isFilterApplied = false
                    updateClearFilterButtonVisibility()
                } else {
                    binding.categoryTxt.text = "Không tìm thấy sản phẩm cho '$searchQuery'"
                    binding.viewList.adapter = null
                    isFilterApplied = false
                    updateClearFilterButtonVisibility()
                }
            }
        })

        viewModel.isLoading.observe(this, Observer { isLoading ->
            if (!isLoading) {
                val results = viewModel.searchResults.value
                if (results.isNullOrEmpty()) {
                    binding.progressBarList.visibility = View.GONE
                    binding.categoryTxt.text = "Không tìm thấy sản phẩm"
                }
            }
        })

        viewModel.errorMessage.observe(this, Observer { error ->
            error?.let {
                Log.e(TAG, "Search error: $it")
                binding.progressBarList.visibility = View.GONE
                binding.categoryTxt.text = "Lỗi tìm kiếm: $it"
                viewModel.clearError()
                isFilterApplied = false
                updateClearFilterButtonVisibility()
            }
        })
    }

    private fun setupRecommendedObserver() {
        viewModel.recommended.observe(this, Observer { items ->
            Log.d(TAG, "Products received: ${items?.size ?: 0} items")

            if (viewModel.isLoading.value == false) {
                binding.progressBarList.visibility = View.GONE

                if (!items.isNullOrEmpty()) {
                    viewModel.isShowingFallback.value?.let { isFallback ->
                        if (isFallback) {
                            if (title.isNotEmpty()) {
                                binding.categoryTxt.text = "$title (${items.size} sản phẩm)"
                            } else {
                                val recommendedCount = items.count { it.isRecommended }
                                val additionalCount = items.size - recommendedCount

                                if (recommendedCount > 0 && additionalCount > 0) {
                                    binding.categoryTxt.text = "Sản phẩm đề xuất ($recommendedCount) + bổ sung ($additionalCount)"
                                } else if (recommendedCount > 0) {
                                    binding.categoryTxt.text = "Sản phẩm đề xuất ($recommendedCount sản phẩm)"
                                } else {
                                    binding.categoryTxt.text = "Sản phẩm nổi bật (${items.size} sản phẩm)"
                                }
                            }
                        } else {
                            if (title.isNotEmpty()) {
                                binding.categoryTxt.text = "$title (${items.size} sản phẩm)"
                            } else {
                                binding.categoryTxt.text = "Sản phẩm đề xuất cho bạn (${items.size} sản phẩm)"
                            }
                        }
                    }

                    binding.viewList.layoutManager = GridLayoutManager(this, 2)
                    binding.viewList.adapter = RecommendedAdapter(items.toMutableList())

                    val recommendedCount = items.count { it.isRecommended }
                    Log.d(TAG, "Hiển thị: ${items.size} sản phẩm (${recommendedCount} đề xuất, ${items.size - recommendedCount} bổ sung)")

                    // Tắt trạng thái lọc khi hiển thị sản phẩm ban đầu
                    isFilterApplied = false
                    updateClearFilterButtonVisibility()
                } else {
                    binding.categoryTxt.text = "Không có sản phẩm"
                    binding.viewList.adapter = null
                    isFilterApplied = false
                    updateClearFilterButtonVisibility()
                }
            }
        })

        viewModel.isLoading.observe(this, Observer { isLoading ->
            if (isLoading) {
                binding.progressBarList.visibility = View.VISIBLE
                if (title.isNotEmpty()) {
                    binding.categoryTxt.text = "Đang tải sản phẩm..."
                } else {
                    binding.categoryTxt.text = "Đang tải sản phẩm đề xuất..."
                }
            } else {
                val items = viewModel.recommended.value
                if (items.isNullOrEmpty()) {
                    binding.progressBarList.visibility = View.GONE
                    if (title.isNotEmpty()) {
                        binding.categoryTxt.text = "$title (Không có sản phẩm)"
                    } else {
                        binding.categoryTxt.text = "Không có sản phẩm"
                    }
                    isFilterApplied = false
                    updateClearFilterButtonVisibility()
                }
            }
        })

        viewModel.errorMessage.observe(this, Observer { error ->
            error?.let {
                Log.e(TAG, "Load products error: $it")
                binding.progressBarList.visibility = View.GONE

                if (title.isNotEmpty()) {
                    binding.categoryTxt.text = "Lỗi tải $title: $it"
                } else {
                    binding.categoryTxt.text = "Lỗi tải sản phẩm: $it"
                }

                viewModel.clearError()
                isFilterApplied = false
                updateClearFilterButtonVisibility()
            }
        })
    }

    private fun callSearchApi(query: String) {
        Log.d(TAG, "Calling search API for query: $query")
        binding.progressBarList.visibility = View.VISIBLE
        binding.categoryTxt.text = "Đang tìm kiếm: '$query'..."

        viewModel.searchProducts(query)
    }

    private fun initList() {
        binding.apply {
            progressBarList.visibility = View.VISIBLE

            if (title.isNotEmpty()) {
                categoryTxt.text = title
            } else {
                categoryTxt.text = "Đang tải sản phẩm đề xuất..."
            }

            if (id.isNotEmpty()) {
                Log.d(TAG, "Load items for category ID = $id")
                viewModel.loadFiltered(id)
            } else {
                Log.d(TAG, "Loading recommended items with fallback...")
                val tinyDB = TinyDB(this@ListItemsActivity)
                val userId = tinyDB.getLong("userId")

                Log.d(TAG, "User ID from TinyDB: $userId")

                if (userId > 0) {
                    viewModel.loadRecommended(userId)
                } else {
                    Log.d(TAG, "No user ID found, loading popular products")
                    binding.categoryTxt.text = "Sản phẩm phổ biến"
                    viewModel.loadFallbackProducts(15)
                }
            }
        }
    }

    private fun getBundle() {
        id = intent.getStringExtra("id") ?: ""
        title = intent.getStringExtra("title") ?: ""
        searchQuery = intent.getStringExtra("searchQuery") ?: ""
        filterRequest = intent.getSerializableExtra("filterRequest") as? FilterRequest
        filteredProducts = intent.getSerializableExtra("filteredProducts") as? ArrayList<SerializableItemsModel>

        Log.d(TAG, "Received: id='$id', title='$title', searchQuery='$searchQuery'")
        Log.d(TAG, "Filter request: $filterRequest")
        Log.d(TAG, "Filtered products: ${filteredProducts?.size ?: 0} items")
    }

    private fun openFilterActivity() {
        val intent = Intent(this, FilterActivity::class.java)
        filterRequest?.let {
            intent.putExtra("currentFilter", it)
        }
        filterLauncher.launch(intent)
    }

    private fun applyFilter(filterRequest: FilterRequest) {
        Log.d(TAG, "APPLYING FILTER IN LISTITEMSACTIVITY")
        Log.d(TAG, "Filter request: $filterRequest")

        this.filterRequest = filterRequest
        binding.progressBarList.visibility = View.VISIBLE
        binding.categoryTxt.text = "Đang lọc sản phẩm..."

        viewModel.recommended.removeObservers(this)
        viewModel.searchResults.removeObservers(this)
        viewModel.errorMessage.removeObservers(this)

        viewModel.filterProducts(filterRequest)

        viewModel.recommended.observe(this, Observer { items ->
            binding.progressBarList.visibility = View.GONE

            if (!items.isNullOrEmpty()) {
                Log.d(TAG, "Filter successful: ${items.size} products found")
                binding.viewList.layoutManager = GridLayoutManager(this, 2)
                binding.viewList.adapter = RecommendedAdapter(items.toMutableList())
                binding.categoryTxt.text = "Kết quả lọc (${items.size} sản phẩm)"

                // Cập nhật trạng thái lọc
                isFilterApplied = true
                updateClearFilterButtonVisibility()
            } else {
                binding.categoryTxt.text = "Không tìm thấy sản phẩm phù hợp"
                binding.viewList.adapter = null
                Toast.makeText(this, "Không có sản phẩm phù hợp với bộ lọc", Toast.LENGTH_SHORT).show()

                // Vẫn đánh dấu là đã áp dụng lọc (dù không có kết quả)
                isFilterApplied = true
                updateClearFilterButtonVisibility()
            }
        })

        viewModel.errorMessage.observe(this, Observer { error ->
            error?.let {
                binding.progressBarList.visibility = View.GONE
                Toast.makeText(this, "Lỗi lọc: $it", Toast.LENGTH_SHORT).show()
                binding.categoryTxt.text = "Lỗi lọc sản phẩm"
                Log.e(TAG, "Filter error: $it")

                // Vẫn đánh dấu là đã áp dụng lọc (dù có lỗi)
                isFilterApplied = true
                updateClearFilterButtonVisibility()
            }
        })
    }

    override fun onBackPressed() {
        Log.d(TAG, "onBackPressed() called")
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "LIST ITEMS ACTIVITY DESTROYED")
    }
}