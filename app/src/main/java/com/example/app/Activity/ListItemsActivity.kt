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
            callSearchApi(searchQuery)
        } else {
            // Không phải search, load bình thường
            initList()
        }
    }

    private fun callSearchApi(query: String) {
        Log.d(TAG, "Calling search API for query: $query")
        binding.progressBarList.visibility = View.VISIBLE
        binding.categoryTxt.text = "Đang tìm kiếm: $query"

        viewModel.searchItems(query).observe(this, Observer<MainViewModel.ApiResponse<List<ItemsModel>>> { result ->
            binding.progressBarList.visibility = View.GONE

            if (result.success) {
                Log.d(TAG, "Search API success: ${result.data?.size ?: 0} items")

                if (!result.data.isNullOrEmpty()) {
                    binding.categoryTxt.text = "Kết quả cho: $query"

                    // Hiển thị kết quả
                    binding.viewList.layoutManager = GridLayoutManager(this, 2)
                    binding.viewList.adapter = ListItemsAdapter(result.data.toMutableList())
                } else {
                    binding.categoryTxt.text = "Không tìm thấy sản phẩm"
                }
            } else {
                Log.e(TAG, "Search API failed: ${result.message}")
                binding.categoryTxt.text = "Lỗi tìm kiếm: ${result.message}"
            }
        })
    }

    private fun initList() {
        binding.apply {
            backBtn.setOnClickListener { finish() }

            progressBarList.visibility = View.VISIBLE
            categoryTxt.text = title

            viewModel.recommended.observe(this@ListItemsActivity, Observer { items ->
                viewList.layoutManager = GridLayoutManager(this@ListItemsActivity, 2)
                viewList.adapter = ListItemsAdapter(items.toMutableList())
                progressBarList.visibility = View.GONE
            })

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