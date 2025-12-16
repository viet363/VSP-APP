package com.example.app.Activity

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.app.Model.FilterRequest
import com.example.app.databinding.ActivityFilterBinding

class FilterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFilterBinding

    companion object {
        const val EXTRA_FILTER_RESULT = "filter_result"
        const val RESULT_FILTER_APPLIED = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFilterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initUI()
        setupListeners()
    }

    private fun initUI() {
        binding.ratingBar.rating = 0f

        val sortOptions = arrayOf(
            "Mặc định",
            "Giá thấp đến cao",
            "Giá cao đến thấp",
            "Đánh giá cao nhất",
            "Mới nhất"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sortOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.sortSpinner.adapter = adapter
    }

    private fun setupListeners() {
        binding.backBtn.setOnClickListener { finish() }

        binding.applyFilterBtn.setOnClickListener {
            applyFilter()
        }

        binding.resetFilterBtn.setOnClickListener {
            resetFilter()
        }
    }

    private fun applyFilter() {
        val filterRequest = FilterRequest(
            minPrice = getPriceValue(binding.minPriceEditText.text.toString()),
            maxPrice = getPriceValue(binding.maxPriceEditText.text.toString()),
            minRating = binding.ratingBar.rating.toInt(),
            sortBy = getSortByValue(),
            inStock = binding.inStockCheckbox.isChecked
        )

        if (filterRequest.minPrice != null && filterRequest.maxPrice != null) {
            if (filterRequest.minPrice!! > filterRequest.maxPrice!!) {
                Toast.makeText(this, "Giá tối thiểu phải nhỏ hơn giá tối đa", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val resultIntent = Intent().apply {
            putExtra(EXTRA_FILTER_RESULT, filterRequest)
        }
        setResult(RESULT_FILTER_APPLIED, resultIntent)
        finish()
    }

    private fun resetFilter() {
        binding.minPriceEditText.text.clear()
        binding.maxPriceEditText.text.clear()
        binding.ratingBar.rating = 0f
        binding.inStockCheckbox.isChecked = false
        binding.sortSpinner.setSelection(0)
    }

    private fun getPriceValue(text: String): Double? {
        return if (text.isNotBlank()) {
            try {
                text.toDouble()
            } catch (e: NumberFormatException) {
                null
            }
        } else {
            null
        }
    }

    private fun getSortByValue(): String? {
        return when (binding.sortSpinner.selectedItemPosition) {
            1 -> "price_asc"
            2 -> "price_desc"
            3 -> "rating"
            4 -> "newest"
            else -> null
        }
    }
}