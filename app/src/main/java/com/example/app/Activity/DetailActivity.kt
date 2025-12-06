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

        // Load ảnh chính (từ intent nếu có)
        val firstImage = item?.picUrl?.firstOrNull()
        Log.d("DetailActivity", "First image URL: $firstImage")

        if (!firstImage.isNullOrEmpty()) {
            Glide.with(this)
                .load(firstImage)
                .into(binding.img)
        } else {
            Log.d("DetailActivity", "No image available from intent")
        }

        // Setup danh sách ảnh phụ (nếu có trong intent)
        val imageUrls = item?.picUrl ?: emptyList()
        Log.d("DetailActivity", "Total images from intent: ${imageUrls.size}")

        if (imageUrls.isNotEmpty()) {
            binding.picList.layoutManager =
                LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            binding.picList.adapter = PicAdapter(imageUrls.toMutableList()) { url ->
                Log.d("DetailActivity", "Image selected: $url")
                Glide.with(this)
                    .load(url)
                    .into(binding.img)
            }
        }

        binding.titleTxt.text = item?.title ?: "Không có tên"
        binding.priceTxt.text = "${item?.price ?: 0}₫"
        binding.raitingTxt.text = item?.rating?.toString() ?: "0.0"

        // Hiển thị mô tả tạm thời từ item (sẽ override nếu API trả về mô tả chi tiết)
        binding.derscriptionTxt.text = item?.description ?: "Không có mô tả"
    }

    private fun initObservers() {
        // Chi tiết sản phẩm (từ API)
        viewModel.productDetail.observe(this) { detail ->
            Log.d("DetailActivity", "Product detail received:")
            Log.d("DetailActivity", "  Product: ${detail?.productName}")
            Log.d("DetailActivity", "  Description: ${detail?.description}")

            if (detail?.description != null) {
                binding.derscriptionTxt.text = detail.description
            }

            // Nếu API có picUrl, dùng ảnh từ server (override ảnh intent)
            detail?.picUrl?.let { url ->
                if (url.isNotBlank()) {
                    Glide.with(this)
                        .load(url)
                        .into(binding.img)

                    // update pic list single image
                    binding.picList.layoutManager =
                        LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
                    binding.picList.adapter = PicAdapter(mutableListOf(url)) { selected ->
                        Glide.with(this).load(selected).into(binding.img)
                    }
                }
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
                // Nếu muốn ẩn recycler khi không có specs:
                // binding.modelList.visibility = View.GONE
            }
        }

        // Danh sách ảnh phụ từ productImages LiveData (nếu server trả nhiều ảnh)
        viewModel.productImages.observe(this) { images ->
            Log.d("DetailActivity", "Product images received: ${images.size}")
            if (images.isNotEmpty()) {
                binding.picList.layoutManager =
                    LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
                binding.picList.adapter = PicAdapter(images.toMutableList()) { url ->
                    Glide.with(this).load(url).into(binding.img)
                }
            }
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
    }
}
