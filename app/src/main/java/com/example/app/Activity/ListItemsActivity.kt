package com.example.app.Activity

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import com.example.app.Adapter.ListItemsAdapter
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

        getBundle()
        initList()
    }

    /**
     * Init UI and load list
     */
    private fun initList() {
        binding.apply {

            backBtn.setOnClickListener { finish() }
            progressBarList.visibility = View.VISIBLE

            // ===== 1. Nếu là kết quả tìm kiếm =====
            if (searchQuery.isNotEmpty()) {

                val searchResults =
                    intent.getParcelableArrayListExtra<ItemsModel>("searchResults")

                Log.d(TAG, "searchResults('$searchQuery'): ${searchResults?.size ?: 0}")

                viewList.layoutManager = GridLayoutManager(this@ListItemsActivity, 2)

                if (!searchResults.isNullOrEmpty()) {
                    viewList.adapter = ListItemsAdapter(searchResults.toMutableList())
                    categoryTxt.text = "Kết quả cho: $searchQuery"
                } else {
                    categoryTxt.text = "Không tìm thấy sản phẩm"
                }

                progressBarList.visibility = View.GONE
                return
            }

            // ===== 2. Nếu vào từ Category =====
            categoryTxt.text = title

            // Observe LiveData chỉ 1 lần
            viewModel.recommended.observe(this@ListItemsActivity, Observer { items ->
                viewList.layoutManager = GridLayoutManager(this@ListItemsActivity, 2)
                viewList.adapter = ListItemsAdapter(items.toMutableList())

                progressBarList.visibility = View.GONE
            })

            // Gọi API theo ID category
            if (id.isNotEmpty()) {
                Log.d(TAG, "Load items for category ID = $id")
                viewModel.loadFiltered(id)
            } else {
                Log.d(TAG, "Loading recommended items...")
                viewModel.loadRecommended()
            }
        }
    }

    /**
     * Lấy bundle được gửi từ Adapter
     */
    private fun getBundle() {
        id = intent.getStringExtra("id") ?: ""
        title = intent.getStringExtra("title") ?: ""
        searchQuery = intent.getStringExtra("searchQuery") ?: ""

        Log.d(TAG, "Received: id=$id, title=$title, searchQuery=$searchQuery")

        if (searchQuery.isEmpty()) {
            binding.categoryTxt.text = title
        }
    }
}
