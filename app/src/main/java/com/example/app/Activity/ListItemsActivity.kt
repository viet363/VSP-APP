package com.example.app.Activity

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.lifecycle.Observer // Thêm import này
import com.example.app.Adapter.ListItemsAdapter
import com.example.app.Model.ItemsModel
import com.example.app.ViewModel.MainViewModel
import com.example.app.databinding.ActivityListItemsBinding

class ListItemsActivity : BaseActivity() {
    private lateinit var binding: ActivityListItemsBinding
    private val viewModel = MainViewModel()
    private var id: String = ""
    private var title: String = ""
    private var searchQuery: String = ""
    private val TAG = "ListItemsActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListItemsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getBundle()
        initList()
    }

    private fun initList() {
        binding.apply {
            backBtn.setOnClickListener {
                finish()
            }
            progressBarList.visibility = View.VISIBLE

            // Kiểm tra xem có tìm kiếm hay không
            if (searchQuery.isNotEmpty()) {
                // Hiển thị kết quả tìm kiếm từ Intent mới
                val searchResults = intent.getParcelableArrayListExtra<ItemsModel>("searchResults")
                Log.d(TAG, "Received searchResults for query '$searchQuery': size=${searchResults?.size ?: 0}, items=${searchResults?.joinToString { it.title }}")
                if (!searchResults.isNullOrEmpty()) {
                    viewList.layoutManager = GridLayoutManager(this@ListItemsActivity, 2)
                    viewList.adapter = ListItemsAdapter(searchResults.toMutableList())
                    categoryTxt.text = "Kết quả tìm kiếm: $searchQuery"
                } else {
                    categoryTxt.text = "Không tìm thấy sản phẩm"
                }
                progressBarList.visibility = View.GONE
            } else {
                // Hiển thị danh sách theo danh mục
                viewModel.recommended.observe(this@ListItemsActivity, Observer { items ->
                    viewList.layoutManager = GridLayoutManager(this@ListItemsActivity, 2)
                    viewList.adapter = ListItemsAdapter(items.toMutableList())
                    progressBarList.visibility = View.GONE
                })
                viewModel.loadFiltered(id)
            }
        }
    }

    private fun getBundle() {
        // Lấy dữ liệu từ Intent mới
        id = intent.getStringExtra("id") ?: ""
        title = intent.getStringExtra("title") ?: ""
        searchQuery = intent.getStringExtra("searchQuery") ?: ""

        if (searchQuery.isEmpty()) {
            binding.categoryTxt.text = title
        }
    }
}