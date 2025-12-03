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

        getBundle()
        initList()
    }

    private fun initList() {
        binding.apply {

            backBtn.setOnClickListener { finish() }
            progressBarList.visibility = View.VISIBLE

            if (searchQuery.isNotEmpty()) {
                val searchResults = intent.getSerializableExtra("searchResults") as? ArrayList<ItemsModel>

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

            categoryTxt.text = title

            viewModel.recommended.observe(this@ListItemsActivity, Observer { items ->
                viewList.layoutManager = GridLayoutManager(this@ListItemsActivity, 2)
                viewList.adapter = ListItemsAdapter(items.toMutableList())

                progressBarList.visibility = View.GONE
            })

            if (id.isNotEmpty()) {
                Log.d(TAG, "Load items for category ID = $id")
                // Sửa lỗi 2: Truyền trực tiếp id (đã là String)
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

        if (searchQuery.isEmpty()) {
            binding.categoryTxt.text = title
        }
    }
}