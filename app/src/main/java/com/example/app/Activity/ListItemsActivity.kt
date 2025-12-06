package com.example.app.Activity

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import com.example.app.Adapter.ListItemsAdapter
import com.example.app.Helper.TinyDB
import com.example.app.ViewModel.MainViewModel
import com.example.app.databinding.ActivityListItemsBinding

class ListItemsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListItemsBinding
    private val viewModel: MainViewModel by viewModels()

    private var id: String = ""
    private var title: String = ""
    private var searchQuery: String = ""

    private val TAG = "ListItemsActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityListItemsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Lấy dữ liệu từ intent
        getBundle()

        // Nếu có searchQuery, gọi API search
        if (searchQuery.isNotEmpty()) {
            setupSearchObservers()
            callSearchApi(searchQuery)
        } else {
            setupRecommendedObserver()
            initList()
        }
    }

    private fun setupSearchObservers() {
        // Quan sát kết quả tìm kiếm
        viewModel.searchResults.observe(this, Observer { results ->
            Log.d(TAG, "Search results received: ${results?.size ?: 0} items")

            // Kiểm tra xem đang ở trạng thái loading hay không
            if (viewModel.isLoading.value == false) {
                binding.progressBarList.visibility = View.GONE

                if (!results.isNullOrEmpty()) {
                    Log.d(TAG, "Search API success: ${results.size} items")
                    binding.categoryTxt.text = "Kết quả cho: $searchQuery"

                    // Hiển thị kết quả
                    binding.viewList.layoutManager = GridLayoutManager(this, 2)
                    binding.viewList.adapter = ListItemsAdapter(results.toMutableList())
                } else {
                    binding.categoryTxt.text = "Không tìm thấy sản phẩm"
                    // Xóa adapter nếu không có kết quả
                    binding.viewList.adapter = null
                }
            }
        })

        // Quan sát trạng thái loading
        viewModel.isLoading.observe(this, Observer { isLoading ->
            if (!isLoading) {
                // Nếu không còn loading, kiểm tra kết quả
                val results = viewModel.searchResults.value
                if (results.isNullOrEmpty()) {
                    binding.progressBarList.visibility = View.GONE
                    binding.categoryTxt.text = "Không tìm thấy sản phẩm"
                }
            }
        })

        // Quan sát lỗi
        viewModel.errorMessage.observe(this, Observer { error ->
            error?.let {
                Log.e(TAG, "Search error: $it")
                binding.progressBarList.visibility = View.GONE
                binding.categoryTxt.text = "Lỗi tìm kiếm: $it"
                viewModel.clearError() // Xóa lỗi sau khi hiển thị
            }
        })
    }

    private fun setupRecommendedObserver() {
        // Quan sát recommended products
        viewModel.recommended.observe(this, Observer { items ->
            Log.d(TAG, "Products received: ${items?.size ?: 0} items")

            if (viewModel.isLoading.value == false) {
                binding.progressBarList.visibility = View.GONE

                if (!items.isNullOrEmpty()) {
                    // Hiển thị thông tin chi tiết về nguồn sản phẩm
                    viewModel.isShowingFallback.value?.let { isFallback ->
                        if (isFallback) {
                            // Có sử dụng sản phẩm bổ sung
                            if (title.isNotEmpty()) {
                                // Nếu có title từ danh mục
                                binding.categoryTxt.text = "$title (${items.size} sản phẩm)"
                            } else {
                                // Sản phẩm đề xuất + bổ sung
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
                            // Chỉ có sản phẩm đề xuất (không cần bổ sung)
                            if (title.isNotEmpty()) {
                                binding.categoryTxt.text = "$title (${items.size} sản phẩm)"
                            } else {
                                binding.categoryTxt.text = "Sản phẩm đề xuất cho bạn (${items.size} sản phẩm)"
                            }
                        }
                    }

                    // Hiển thị danh sách sản phẩm
                    binding.viewList.layoutManager = GridLayoutManager(this, 2)
                    binding.viewList.adapter = ListItemsAdapter(items.toMutableList())

                    // Log thông tin chi tiết
                    val recommendedCount = items.count { it.isRecommended }
                    Log.d(TAG, "✅ Hiển thị: ${items.size} sản phẩm (${recommendedCount} đề xuất, ${items.size - recommendedCount} bổ sung)")
                } else {
                    binding.categoryTxt.text = "Không có sản phẩm"
                    binding.viewList.adapter = null
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
                }
            }
        })

        // Quan sát lỗi
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
            }
        })
    }

    private fun callSearchApi(query: String) {
        Log.d(TAG, "Calling search API for query: $query")
        binding.progressBarList.visibility = View.VISIBLE
        binding.categoryTxt.text = "Đang tìm kiếm: $query"

        // Gọi API search
        viewModel.searchProducts(query)
    }

    private fun initList() {
        binding.apply {
            backBtn.setOnClickListener { finish() }

            progressBarList.visibility = View.VISIBLE

            if (title.isNotEmpty()) {
                categoryTxt.text = title
            } else {
                categoryTxt.text = "Đang tải sản phẩm đề xuất..."
            }

            if (id.isNotEmpty()) {
                // Load theo danh mục
                Log.d(TAG, "Load items for category ID = $id")
                viewModel.loadFiltered(id)
            } else {
                // Load sản phẩm đề xuất + bổ sung
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

        Log.d(TAG, "Received: id=$id, title='$title', searchQuery='$searchQuery'")
    }
}