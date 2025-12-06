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
import com.example.app.Model.ItemsModel
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
            // Không phải search, load bình thường
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
            Log.d(TAG, "Recommended products received: ${items?.size ?: 0} items")

            if (viewModel.isLoading.value == false) {
                binding.progressBarList.visibility = View.GONE

                if (!items.isNullOrEmpty()) {
                    viewModel.isShowingFallback.value?.let { isFallback ->
                        if (isFallback && title.isEmpty()) {
                            binding.categoryTxt.text = "Sản phẩm nổi bật"
                        }
                    }

                    binding.viewList.layoutManager = GridLayoutManager(this, 2)
                    binding.viewList.adapter = ListItemsAdapter(items.toMutableList())
                } else {
                    binding.categoryTxt.text = "Không có sản phẩm"
                    binding.viewList.adapter = null
                }
            }
        })

        // Quan sát trạng thái loading
        viewModel.isLoading.observe(this, Observer { isLoading ->
            if (!isLoading) {
                val items = viewModel.recommended.value
                if (items.isNullOrEmpty()) {
                    binding.progressBarList.visibility = View.GONE
                    binding.categoryTxt.text = "Không có sản phẩm"
                }
            }
        })

        // Quan sát lỗi
        viewModel.errorMessage.observe(this, Observer { error ->
            error?.let {
                Log.e(TAG, "Load products error: $it")
                binding.progressBarList.visibility = View.GONE
                binding.categoryTxt.text = "Lỗi tải sản phẩm: $it"
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
            categoryTxt.text = title

            if (id.isNotEmpty()) {
                Log.d(TAG, "Load items for category ID = $id")
                viewModel.loadFiltered(id)
            } else {
                Log.d(TAG, "Loading recommended items...")
                val tinyDB = TinyDB(this@ListItemsActivity)
                val userId = tinyDB.getLong("userId")
                viewModel.loadRecommended(userId)
            }
        }
    }

    private fun getBundle() {
        id = intent.getStringExtra("id") ?: ""
        title = intent.getStringExtra("title") ?: ""
        searchQuery = intent.getStringExtra("searchQuery") ?: ""

        Log.d(TAG, "Received: id=$id, title=$title, searchQuery=$searchQuery")
    }
}