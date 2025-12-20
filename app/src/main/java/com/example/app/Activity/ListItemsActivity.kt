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

        getBundle()
        setupFilterButton()

        // Ưu tiên hiển thị sản phẩm đã filter từ MainActivity
        if (filteredProducts != null && filteredProducts!!.isNotEmpty()) {
            showFilteredProducts(filteredProducts!!)
        } else if (filterRequest != null) {
            // Nếu có filterRequest nhưng chưa có sản phẩm, gọi API filter
            applyFilter(filterRequest!!)
        } else if (searchQuery.isNotEmpty()) {
            setupSearchObservers()
            callSearchApi(searchQuery)
        } else {
            setupRecommendedObserver()
            initList()
        }
    }

    private fun setupFilterButton() {
        binding.filterBtn.setOnClickListener {
            openFilterActivity()
        }
    }

    private fun showFilteredProducts(products: List<SerializableItemsModel>) {
        Log.d(TAG, "Showing ${products.size} filtered products")

        // Convert SerializableItemsModel back to ItemsModel
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

        // Hiển thị danh sách
        binding.viewList.layoutManager = GridLayoutManager(this, 2)
        binding.viewList.adapter = RecommendedAdapter(itemsList.toMutableList())
        binding.categoryTxt.text = "Kết quả lọc (${itemsList.size} sản phẩm)"
        binding.progressBarList.visibility = View.GONE
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
                } else {
                    binding.categoryTxt.text = "Không tìm thấy sản phẩm cho '$searchQuery'"
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
                    binding.viewList.adapter = RecommendedAdapter(items.toMutableList())

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
        binding.categoryTxt.text = "Đang tìm kiếm: '$query'..."

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
        filterRequest = intent.getSerializableExtra("filterRequest") as? FilterRequest
        filteredProducts = intent.getSerializableExtra("filteredProducts") as? ArrayList<SerializableItemsModel>

        Log.d(TAG, "Received: id='$id', title='$title', searchQuery='$searchQuery'")
        Log.d(TAG, "Filter request: $filterRequest")
        Log.d(TAG, "Filtered products: ${filteredProducts?.size ?: 0} items")
    }

    private fun openFilterActivity() {
        val intent = Intent(this, FilterActivity::class.java)
        // Nếu đang filter, truyền filterRequest hiện tại để giữ các giá trị
        filterRequest?.let {
            intent.putExtra("currentFilter", it)
        }
        filterLauncher.launch(intent)
    }

    private fun applyFilter(filterRequest: FilterRequest) {
        Log.d(TAG, "=== APPLYING FILTER IN LISTITEMSACTIVITY ===")
        Log.d(TAG, "Filter request: $filterRequest")

        this.filterRequest = filterRequest
        binding.progressBarList.visibility = View.VISIBLE
        binding.categoryTxt.text = "Đang lọc sản phẩm..."

        // Dừng tất cả observer cũ
        viewModel.recommended.removeObservers(this)
        viewModel.searchResults.removeObservers(this)

        // Gọi ViewModel để filter (thay vì gọi Retrofit trực tiếp)
        viewModel.filterProducts(filterRequest)

        // Observer kết quả filter
        viewModel.recommended.observe(this, Observer { items ->
            binding.progressBarList.visibility = View.GONE

            if (!items.isNullOrEmpty()) {
                Log.d(TAG, "Filter successful: ${items.size} products found")
                binding.viewList.layoutManager = GridLayoutManager(this, 2)
                binding.viewList.adapter = RecommendedAdapter(items.toMutableList())
                binding.categoryTxt.text = "Kết quả lọc (${items.size} sản phẩm)"
            } else {
                binding.categoryTxt.text = "Không tìm thấy sản phẩm phù hợp"
                binding.viewList.adapter = null
                Toast.makeText(this, "Không có sản phẩm phù hợp với bộ lọc", Toast.LENGTH_SHORT).show()
            }
        })

        // Observer lỗi
        viewModel.errorMessage.observe(this, Observer { error ->
            error?.let {
                binding.progressBarList.visibility = View.GONE
                Toast.makeText(this, "Lỗi lọc: $it", Toast.LENGTH_SHORT).show()
                binding.categoryTxt.text = "Lỗi lọc sản phẩm"
                Log.e(TAG, "Filter error: $it")
            }
        })
    }
}