package com.example.app.Activity

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.app.Adapter.CommentAdapter
import com.example.app.Adapter.PicAdapter
import com.example.app.Adapter.SpecificationAdapter
import com.example.app.Model.ItemsModel
import com.example.app.databinding.ActivityDetailBinding
import com.example.app.ViewModel.MainViewModel

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private val viewModel: MainViewModel by viewModels()

    private var productId: Int = -1
    private var item: ItemsModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d("DetailActivity", "=== DETAIL ACTIVITY STARTED ===")

        getIntentData()
        initUI()
        initObservers()
        loadData()
    }

    private fun getIntentData() {
        item = intent.getSerializableExtra("object") as? ItemsModel
        productId = item?.id ?: -1

        Log.d("DetailActivity", "Product from intent:")
        Log.d("DetailActivity", "  ID: $productId")
        Log.d("DetailActivity", "  Title: ${item?.title}")
        Log.d("DetailActivity", "  Price: ${item?.price}")
        Log.d("DetailActivity", "  Images: ${item?.picUrl?.size}")
    }

    private fun initUI() {
        binding.backBtn.setOnClickListener { finish() }

        // Load ảnh chính
        val firstImage = item?.picUrl?.firstOrNull()
        Log.d("DetailActivity", "First image URL: $firstImage")

        if (!firstImage.isNullOrEmpty()) {
            Glide.with(this)
                .load(firstImage)
                .into(binding.img)
        } else {
            Log.d("DetailActivity", "No image available")
        }

        // Setup danh sách ảnh phụ
        val imageUrls = item?.picUrl ?: emptyList()
        Log.d("DetailActivity", "Total images: ${imageUrls.size}")

        if (imageUrls.isNotEmpty()) {
            binding.picList.layoutManager =
                LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            binding.picList.adapter = PicAdapter(imageUrls.toMutableList()) { url ->  // Thêm .toMutableList()
                Log.d("DetailActivity", "Image selected: $url")
                Glide.with(this)
                    .load(url)
                    .into(binding.img)
            }
        }

        binding.titleTxt.text = item?.title ?: "Không có tên"
        binding.priceTxt.text = "${item?.price ?: 0}₫"
        binding.raitingTxt.text = item?.rating?.toString() ?: "0.0"

        // Hiển thị mô tả tạm thời từ item
        binding.derscriptionTxt.text = item?.description ?: "Không có mô tả"
    }

    private fun initObservers() {
        // Chi tiết sản phẩm
        viewModel.productDetail.observe(this) { detail ->
            Log.d("DetailActivity", "Product detail received:")
            Log.d("DetailActivity", "  Product: ${detail?.productName}")
            Log.d("DetailActivity", "  Description: ${detail?.description}")

            if (detail?.description != null) {
                binding.derscriptionTxt.text = detail.description
            }
        }

        // Thông số kỹ thuật
        viewModel.productSpecifications.observe(this) { specs ->
            Log.d("DetailActivity", "Specifications received: ${specs.size} items")
            if (specs.isNotEmpty()) {
                specs.forEachIndexed { index, spec ->
                    Log.d("DetailActivity", "  Spec[$index]: ${spec.specKey} = ${spec.specValue}")
                }

                binding.modelList.apply {
                    layoutManager = LinearLayoutManager(this@DetailActivity)
                    adapter = SpecificationAdapter(specs)
                }
            } else {
                Log.d("DetailActivity", "No specifications available")
            }
        }

        // Danh sách ảnh phụ
        viewModel.productImages.observe(this) { images ->
            Log.d("DetailActivity", "Product images received: ${images.size}")
            // Có thể update image list nếu cần
        }

        // Error handling
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Log.e("DetailActivity", "Error: $it")
                android.widget.Toast.makeText(this, it, android.widget.Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        // Loading state
        viewModel.isLoading.observe(this) { loading ->
            Log.d("DetailActivity", "Loading: $loading")
        }
    }

    private fun loadData() {
        if (productId == -1) {
            Log.e("DetailActivity", "Invalid product ID")
            android.widget.Toast.makeText(this, "Không tìm thấy sản phẩm", android.widget.Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.d("DetailActivity", "=== LOADING API DATA ===")
        Log.d("DetailActivity", "Product ID: $productId")

        viewModel.loadProductDetail(productId)
        viewModel.loadSpecifications(productId)
        viewModel.loadReviews(productId)
        viewModel.loadProductImages(productId)
        }
}