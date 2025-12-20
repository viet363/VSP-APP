package com.example.app.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.app.Model.FilterRequest
import com.example.app.R
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
        loadCurrentFilter()
    }

    private fun loadCurrentFilter() {
        // Nhận filter hiện tại nếu có
        val currentFilter = intent.getSerializableExtra("currentFilter") as? FilterRequest
        currentFilter?.let {
            // Hiển thị giá trị filter hiện tại
            it.minPrice?.let { min -> binding.minPriceEditText.setText(min.toString()) }
            it.maxPrice?.let { max -> binding.maxPriceEditText.setText(max.toString()) }
            it.minRating?.let { rating -> binding.ratingBar.rating = rating.toFloat() }
            it.inStock?.let { inStock -> binding.inStockCheckbox.isChecked = inStock }

            // Set sort spinner
            when (it.sortBy) {
                "price_asc" -> binding.sortSpinner.setSelection(1)
                "price_desc" -> binding.sortSpinner.setSelection(2)
                "rating" -> binding.sortSpinner.setSelection(3)
                "newest" -> binding.sortSpinner.setSelection(4)
                else -> binding.sortSpinner.setSelection(0)
            }
        }
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
        val minPriceText = binding.minPriceEditText.text.toString()
        val maxPriceText = binding.maxPriceEditText.text.toString()

        Log.d("FilterActivity", "Min price text: '$minPriceText', Max price text: '$maxPriceText'")
        Log.d("FilterActivity", "Rating: ${binding.ratingBar.rating.toInt()}")
        Log.d("FilterActivity", "In stock: ${binding.inStockCheckbox.isChecked}")
        Log.d("FilterActivity", "Sort by position: ${binding.sortSpinner.selectedItemPosition}")

        val filterRequest = FilterRequest(
            minPrice = getPriceValue(minPriceText),
            maxPrice = getPriceValue(maxPriceText),
            minRating = binding.ratingBar.rating.toInt(),
            sortBy = getSortByValue(),
            inStock = binding.inStockCheckbox.isChecked
        )

        Log.d("FilterActivity", "Created filter request: $filterRequest")

        // Validate price range
        if (filterRequest.minPrice != null && filterRequest.maxPrice != null) {
            if (filterRequest.minPrice!! > filterRequest.maxPrice!!) {
                Toast.makeText(this, "Giá tối thiểu phải nhỏ hơn giá tối đa", Toast.LENGTH_SHORT).show()
                binding.minPriceEditText.startAnimation(
                    AnimationUtils.loadAnimation(this, R.anim.shake)
                )
                return
            }
        }

        Toast.makeText(this, "Áp dụng bộ lọc thành công!", Toast.LENGTH_SHORT).show()

        val resultIntent = Intent().apply {
            putExtra(EXTRA_FILTER_RESULT, filterRequest)
        }
        setResult(RESULT_FILTER_APPLIED, resultIntent)
        finish()

        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
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