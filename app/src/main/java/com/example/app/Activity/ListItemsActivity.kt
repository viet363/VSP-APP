package com.example.app.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import com.example.app.Adapter.ListItemsAdapter
import com.example.app.Helper.TinyDB
import com.example.app.Model.FilterRequest
import com.example.app.Model.FilterResponse
import com.example.app.Network.RetrofitClient
import com.example.app.ViewModel.MainViewModel
import com.example.app.databinding.ActivityListItemsBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ListItemsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListItemsBinding
    private val viewModel: MainViewModel by viewModels()

    private var id: String = ""
    private var title: String = ""
    private var searchQuery: String = ""

    private val TAG = "ListItemsActivity"

    private val filterLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == FilterActivity.RESULT_FILTER_APPLIED) {
            val data = result.data
            val filterRequest = data?.getSerializableExtra(FilterActivity.EXTRA_FILTER_RESULT) as? FilterRequest
            filterRequest?.let {
                applyFilter(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityListItemsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getBundle()

        if (searchQuery.isNotEmpty()) {
            setupSearchObservers()
            callSearchApi(searchQuery)
        } else {
            setupRecommendedObserver()
            initList()
        }
    }

    private fun setupSearchObservers() {
        viewModel.searchResults.observe(this, Observer { results ->
            Log.d(TAG, "Search results received: ${results?.size ?: 0} items")

            if (viewModel.isLoading.value == false) {
                binding.progressBarList.visibility = View.GONE

                if (!results.isNullOrEmpty()) {
                    Log.d(TAG, "Search API success: ${results.size} items")
                    binding.categoryTxt.text = "Kết quả cho: $searchQuery"

                    binding.viewList.layoutManager = GridLayoutManager(this, 2)
                    binding.viewList.adapter = ListItemsAdapter(results.toMutableList())
                } else {
                    binding.categoryTxt.text = "Không tìm thấy sản phẩm"
                    binding.viewList.adapter = null
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
                    binding.viewList.adapter = ListItemsAdapter(items.toMutableList())

                    val recommendedCount = items.count { it.isRecommended }
                    Log.d(TAG, "Hiển thị: ${items.size} sản phẩm (${recommendedCount} đề xuất, ${items.size - recommendedCount} bổ sung)")
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

        Log.d(TAG, "Received: id=$id, title='$title', searchQuery='$searchQuery'")
    }

    private fun openFilterActivity() {
        val intent = Intent(this, FilterActivity::class.java)
        filterLauncher.launch(intent)
    }

    private fun applyFilter(filterRequest: FilterRequest) {
        binding.progressBarList.visibility = View.VISIBLE
        binding.categoryTxt.text = "Đang lọc sản phẩm..."

        RetrofitClient.productsApi().filterProducts(filterRequest)
            .enqueue(object : Callback<FilterResponse> {
                override fun onResponse(call: Call<FilterResponse>, response: Response<FilterResponse>) {
                    binding.progressBarList.visibility = View.GONE

                    if (response.isSuccessful) {
                        val result = response.body()
                        if (result?.success == true && !result.data.isNullOrEmpty()) {
                            binding.viewList.layoutManager = GridLayoutManager(this@ListItemsActivity, 2)
                            binding.viewList.adapter = ListItemsAdapter(result.data.toMutableList())
                            binding.categoryTxt.text = "Kết quả lọc (${result.data.size} sản phẩm)"
                            Log.d(TAG, "Filter applied: ${result.data.size} products found")
                        } else {
                            binding.categoryTxt.text = "Không tìm thấy sản phẩm phù hợp"
                            binding.viewList.adapter = null
                            Toast.makeText(this@ListItemsActivity, result?.message ?: "Không có sản phẩm phù hợp", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@ListItemsActivity, "Lỗi lọc sản phẩm", Toast.LENGTH_SHORT).show()
                        binding.categoryTxt.text = "Lỗi lọc sản phẩm"
                    }
                }

                override fun onFailure(call: Call<FilterResponse>, t: Throwable) {
                    binding.progressBarList.visibility = View.GONE
                    Toast.makeText(this@ListItemsActivity, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                    binding.categoryTxt.text = "Lỗi kết nối"
                }
            })
    }
}