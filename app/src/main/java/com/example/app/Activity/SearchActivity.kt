package com.example.app.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.ViewModelProvider
import com.example.app.Adapter.SearchAdapter
import com.example.app.ViewModel.MainViewModel
import com.example.app.databinding.ActivitySearchBinding

class SearchActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySearchBinding
    private lateinit var adapter: SearchAdapter
    private lateinit var viewModel: MainViewModel
    private val TAG = "SearchActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "=== SEARCH ACTIVITY STARTED ===")

        // Lấy query từ intent
        val searchQuery = intent.getStringExtra("searchQuery") ?: ""
        Log.d(TAG, "Intent query: '$searchQuery'")

        // Khởi tạo ViewModel
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        Log.d(TAG, "ViewModel created")

        setupRecyclerView()
        setupBackButton()
        setupSystemBack()
        setupSearchView(searchQuery)
        observeViewModel()

        // Nếu có query từ intent, thực hiện search ngay
        if (searchQuery.isNotEmpty()) {
            Log.d(TAG, "Searching immediately for: '$searchQuery'")
            binding.searchView.setQuery(searchQuery, false)
            viewModel.searchProducts(searchQuery)
        }
    }

    private fun setupRecyclerView() {
        Log.d(TAG, "Setting up RecyclerView")
        binding.recyclerViewSearch.layoutManager = LinearLayoutManager(this)

        adapter = SearchAdapter(mutableListOf()) { product ->
            Log.d(TAG, "Product clicked: ${product.title}")
            openProductDetail(product.id)
        }

        binding.recyclerViewSearch.adapter = adapter
    }

    private fun openProductDetail(productId: Int) {
        Log.d(TAG, "Opening product detail: $productId")
        val intent = Intent(this, DetailActivity::class.java)
        intent.putExtra("productId", productId)
        startActivity(intent)
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            Log.d(TAG, "Back button clicked")
            onBackPressed()
        }
    }

    private fun setupSystemBack() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d(TAG, "System back pressed")
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun setupSearchView(initialQuery: String) {
        Log.d(TAG, "Setting up SearchView with query: '$initialQuery'")

        binding.searchView.setQuery(initialQuery, false)

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                Log.d(TAG, "Search submitted: '$query'")
                query?.let {
                    viewModel.searchProducts(it)
                    binding.searchView.clearFocus()
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                Log.d(TAG, "Search text changed: '$newText'")
                newText?.let {
                    if (it.length >= 2) {
                        Log.d(TAG, "Searching for: '$it'")
                        viewModel.searchProducts(it)
                    } else {
                        Log.d(TAG, "Text too short, clearing results")
                        adapter.updateList(emptyList())
                        binding.emptyState.visibility = View.GONE
                    }
                }
                return true
            }
        })

        if (initialQuery.isEmpty()) {
            binding.searchView.requestFocus()
            Log.d(TAG, "SearchView focused")
        }
    }

    private fun observeViewModel() {
        Log.d(TAG, "Setting up ViewModel observers")

        viewModel.searchResults.observe(this) { results ->
            Log.d(TAG, "Search results: ${results.size} items")

            // Debug: hiển thị thông tin chi tiết
            if (results.isNotEmpty()) {
                results.take(3).forEachIndexed { index, item ->
                    Log.d(TAG, "Item $index: ${item.title}, price: ${item.price}")
                }
            }

            adapter.updateList(results)

            if (results.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.recyclerViewSearch.visibility = View.GONE
                Log.d(TAG, "No results found")
            } else {
                binding.emptyState.visibility = View.GONE
                binding.recyclerViewSearch.visibility = View.VISIBLE
                Log.d(TAG, "Showing ${results.size} results")
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            Log.d(TAG, "Loading: $isLoading")
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Log.e(TAG, "Error: $it")
            }
        }
    }

    override fun onBackPressed() {
        Log.d(TAG, "onBackPressed() called")
        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "=== SEARCH ACTIVITY DESTROYED ===")
    }
}