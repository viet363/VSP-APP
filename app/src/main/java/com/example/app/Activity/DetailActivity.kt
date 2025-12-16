package com.example.app.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.app.Adapter.CommentAdapter
import com.example.app.Adapter.PicAdapter
import com.example.app.Adapter.SpecificationAdapter
import com.example.app.Helper.TinyDB
import com.example.app.Model.CommonResponse
import com.example.app.Model.ItemsModel
import com.example.app.Network.RetrofitClient
import com.example.app.R
import com.example.app.databinding.ActivityDetailBinding
import com.example.app.ViewModel.MainViewModel
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private val viewModel: MainViewModel by viewModels()

    private var productId: Int = -1
    private var item: ItemsModel? = null
    private var isInWishlist = false

    private lateinit var commentAdapter: CommentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d("DetailActivity", "=== DETAIL ACTIVITY STARTED ===")

        getIntentData()
        initUI()
        initObservers()
        initClickListeners()
        loadData()
        setupCommentList()
        loadReviews()
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

        binding.cartBtn.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        binding.favBtn.setOnClickListener {
            toggleWishlist()
        }

        val firstImage = item?.picUrl?.firstOrNull()
        Log.d("DetailActivity", "First image URL: $firstImage")

        if (!firstImage.isNullOrEmpty()) {
            Glide.with(this)
                .load(firstImage)
                .into(binding.img)
        } else {
            Log.d("DetailActivity", "No image available from intent")
        }

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

        val initialRating = item?.rating?.toFloat() ?: 0f
        binding.raitingTxt.text = String.format("%.1f", initialRating)
        binding.productRatingBar.rating = initialRating

        binding.derscriptionTxt.text = item?.description ?: "Không có mô tả"
    }

    private fun initClickListeners() {
        binding.addToCartBtn.setOnClickListener {
            addToCart()
        }

        binding.submitCommentBtn.setOnClickListener {
            submitReview()
        }

        binding.mediaPickBtn.setOnClickListener {
            Toast.makeText(this, "Chức năng chọn ảnh/video sẽ được phát triển sau", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initObservers() {
        viewModel.productDetail.observe(this) { detail ->
            Log.d("DetailActivity", "Product detail received:")
            Log.d("DetailActivity", "  Product: ${detail?.productName}")
            Log.d("DetailActivity", "  Description: ${detail?.description}")

            if (detail?.description != null) {
                binding.derscriptionTxt.text = detail.description
            }

            detail?.picUrl?.let { url ->
                if (url.isNotBlank()) {
                    Glide.with(this)
                        .load(url)
                        .into(binding.img)

                    binding.picList.layoutManager =
                        LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
                    binding.picList.adapter = PicAdapter(mutableListOf(url)) { selected ->
                        Glide.with(this).load(selected).into(binding.img)
                    }
                }
            }
        }

        viewModel.productSpecifications.observe(this) { specs ->
            Log.d("DetailActivity", "Specifications received: ${specs.size} items")
            if (specs.isNotEmpty()) {
                binding.modelList.apply {
                    layoutManager = LinearLayoutManager(this@DetailActivity)
                    adapter = SpecificationAdapter(specs)
                }
            } else {
                Log.d("DetailActivity", "No specifications available")
            }
        }

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

        viewModel.productReviews.observe(this) { reviews ->
            Log.d("DetailActivity", "Reviews received from ViewModel: ${reviews.size}")
            commentAdapter.updateData(reviews)
            updateReviewCount(reviews.size)
        }

        viewModel.productRating.observe(this) { rating ->
            Log.d("DetailActivity", "Product rating updated: $rating")

            binding.raitingTxt.text = String.format("%.1f", rating)
            binding.productRatingBar.rating = rating

            item?.rating = rating.toDouble()
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Log.e("DetailActivity", "Error: $it")
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        viewModel.isLoading.observe(this) { loading ->
            Log.d("DetailActivity", "Loading: $loading")
            if (loading) {
                binding.commentInputLayout.visibility = View.GONE
            } else {
                binding.commentInputLayout.visibility = View.VISIBLE
            }
        }
    }

    private fun loadData() {
        if (productId == -1) {
            Log.e("DetailActivity", "Invalid product ID")
            Toast.makeText(this, "Không tìm thấy sản phẩm", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.d("DetailActivity", "=== LOADING API DATA ===")
        Log.d("DetailActivity", "Product ID: $productId")

        viewModel.loadProductDetail(productId)
        viewModel.loadSpecifications(productId)

        checkWishlistStatus()
    }

    private fun setupCommentList() {
        commentAdapter = CommentAdapter()
        binding.modelComment.apply {
            layoutManager = LinearLayoutManager(this@DetailActivity)
            adapter = commentAdapter
        }
    }

    private fun loadReviews() {
        if (productId == -1) return

        Log.d("DetailActivity", "Loading reviews for product: $productId")

        viewModel.loadReviews(productId)
    }

    private fun toggleWishlist() {
        if (productId == -1) return

        val tinyDB = TinyDB(this)
        val token = tinyDB.getString("token", "")

        if (token.isEmpty()) {
            Toast.makeText(this, "Vui lòng đăng nhập để thêm vào yêu thích", Toast.LENGTH_SHORT).show()
            return
        }

        if (isInWishlist) {
            removeFromWishlist()
        } else {
            addToWishlist()
        }
    }

    private fun addToWishlist() {
        showLoading(true, "Đang thêm vào yêu thích...")

        RetrofitClient.wishlistApi().addToWishlist(productId.toLong())
            .enqueue(object : Callback<CommonResponse> {
                override fun onResponse(call: Call<CommonResponse>, response: Response<CommonResponse>) {
                    showLoading(false)

                    if (response.isSuccessful && response.body()?.success == true) {
                        isInWishlist = true
                        updateWishlistIcon()
                        Toast.makeText(this@DetailActivity, "Đã thêm vào yêu thích", Toast.LENGTH_SHORT).show()
                    } else {
                        val errorMsg = response.body()?.message ?: "Thêm thất bại"
                        Toast.makeText(this@DetailActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<CommonResponse>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(this@DetailActivity, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun removeFromWishlist() {
        showLoading(true, "Đang xóa khỏi yêu thích...")

        RetrofitClient.wishlistApi().removeFromWishlist(productId.toLong())
            .enqueue(object : Callback<CommonResponse> {
                override fun onResponse(call: Call<CommonResponse>, response: Response<CommonResponse>) {
                    showLoading(false)

                    if (response.isSuccessful && response.body()?.success == true) {
                        isInWishlist = false
                        updateWishlistIcon()
                        Toast.makeText(this@DetailActivity, "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show()
                    } else {
                        val errorMsg = response.body()?.message ?: "Xóa thất bại"
                        Toast.makeText(this@DetailActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<CommonResponse>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(this@DetailActivity, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun checkWishlistStatus() {
        if (productId == -1) return

        val tinyDB = TinyDB(this)
        val token = tinyDB.getString("token", "")

        if (token.isEmpty()) {
            isInWishlist = false
            updateWishlistIcon()
            return
        }

        RetrofitClient.wishlistApi().checkWishlist(productId.toLong())
            .enqueue(object : Callback<CommonResponse> {
                override fun onResponse(call: Call<CommonResponse>, response: Response<CommonResponse>) {
                    Log.d("DetailActivity", "Check wishlist response code: ${response.code()}")

                    if (response.isSuccessful) {
                        val result = response.body()
                        Log.d("DetailActivity", "Check wishlist result: success=${result?.success}, data=${result?.data}")

                        if (result?.success == true) {
                            if (result.data != null && result.data is Map<*, *>) {
                                val dataMap = result.data as Map<*, *>
                                isInWishlist = dataMap["inWishlist"] == true
                                Log.d("DetailActivity", "inWishlist from data: $isInWishlist")
                            } else {
                                isInWishlist = false
                            }
                        } else {
                            isInWishlist = false
                        }
                    } else {
                        Log.e("DetailActivity", "Check wishlist failed with code: ${response.code()}")
                        isInWishlist = false
                    }
                    updateWishlistIcon()
                }

                override fun onFailure(call: Call<CommonResponse>, t: Throwable) {
                    Log.e("DetailActivity", "Check wishlist error: ${t.message}")
                    isInWishlist = false
                    updateWishlistIcon()
                }
            })
    }

    private fun updateWishlistIcon() {
        try {
            if (isInWishlist) {
                binding.favBtn.setImageResource(R.drawable.fav_icon)
                binding.favBtn.setColorFilter(resources.getColor(R.color.red, null))
                Log.d("DetailActivity", "Updated icon to: filled (in wishlist)")
            } else {
                binding.favBtn.setImageResource(R.drawable.fav_icon)
                binding.favBtn.clearColorFilter()
                Log.d("DetailActivity", "Updated icon to: empty (not in wishlist)")
            }
        } catch (e: Exception) {
            Log.e("DetailActivity", "Error updating wishlist icon: ${e.message}")
            binding.favBtn.setImageResource(R.drawable.fav_icon)
        }
    }

    private fun addToCart() {
        if (productId == -1) {
            Toast.makeText(this, "Không thể thêm sản phẩm này vào giỏ hàng", Toast.LENGTH_SHORT).show()
            return
        }

        val tinyDB = TinyDB(this)
        val token = tinyDB.getString("token", "")

        if (token.isEmpty()) {
            Toast.makeText(this, "Vui lòng đăng nhập để thêm vào giỏ hàng", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true, "Đang thêm vào giỏ hàng...")

        val body = hashMapOf<String, Any>(
            "productId" to productId,
            "quantity" to 1
        )

        RetrofitClient.cartApi().addToCart(body)
            .enqueue(object : Callback<CommonResponse> {
                override fun onResponse(call: Call<CommonResponse>, response: Response<CommonResponse>) {
                    showLoading(false)

                    if (response.isSuccessful) {
                        val result = response.body()
                        if (result?.success == true) {
                            Toast.makeText(
                                this@DetailActivity,
                                "Đã thêm vào giỏ hàng!",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this@DetailActivity,
                                "${result?.message ?: "Thêm vào giỏ hàng thất bại"}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        try {
                            val errorBody = response.errorBody()?.string()
                            val errorJson = JSONObject(errorBody ?: "{}")
                            val errorMsg = errorJson.optString("message", "Lỗi không xác định")
                            Toast.makeText(
                                this@DetailActivity,
                                " $errorMsg",
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (e: Exception) {
                            Toast.makeText(
                                this@DetailActivity,
                                "Lỗi server: ${response.code()}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                override fun onFailure(call: Call<CommonResponse>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(
                        this@DetailActivity,
                        "Lỗi kết nối: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun submitReview() {
        val commentText = binding.commentInput.text.toString().trim()
        val rating = binding.ratingBar.rating.toInt()

        if (commentText.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập nội dung đánh giá", Toast.LENGTH_SHORT).show()
            binding.commentInput.requestFocus()
            return
        }

        if (rating == 0) {
            Toast.makeText(this, "Vui lòng chọn số sao đánh giá", Toast.LENGTH_SHORT).show()
            return
        }

        val tinyDB = TinyDB(this)
        val token = tinyDB.getString("token", "")

        if (token.isEmpty()) {
            Toast.makeText(this, "Vui lòng đăng nhập để đánh giá sản phẩm", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true, "Đang gửi đánh giá...")

        val body = hashMapOf<String, Any>(
            "productId" to productId,
            "rating" to rating,
            "content" to commentText,
            "title" to commentText.take(50)
        )

        RetrofitClient.reviewApi().submitReview(body)
            .enqueue(object : Callback<CommonResponse> {
                override fun onResponse(call: Call<CommonResponse>, response: Response<CommonResponse>) {
                    showLoading(false)

                    if (response.isSuccessful) {
                        val result = response.body()
                        if (result?.success == true) {
                            Toast.makeText(
                                this@DetailActivity,
                                "Đánh giá đã được gửi thành công!",
                                Toast.LENGTH_SHORT
                            ).show()

                            binding.commentInput.text.clear()
                            binding.ratingBar.rating = 5.0f

                            loadReviews()

                            val currentReviewCount = commentAdapter.itemCount
                            viewModel.updateRatingAfterReview(rating, currentReviewCount)

                        } else {
                            Toast.makeText(
                                this@DetailActivity,
                                "${result?.message ?: "Gửi đánh giá thất bại"}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        try {
                            val errorBody = response.errorBody()?.string()
                            val errorJson = JSONObject(errorBody ?: "{}")
                            val errorMsg = errorJson.optString("message", "Lỗi không xác định")
                            Toast.makeText(
                                this@DetailActivity,
                                "$errorMsg",
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (e: Exception) {
                            Toast.makeText(
                                this@DetailActivity,
                                "Lỗi server: ${response.code()}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                override fun onFailure(call: Call<CommonResponse>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(
                        this@DetailActivity,
                        "Lỗi kết nối: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun updateReviewCount(count: Int) {
        binding.txtComment.text = "Nhận xét khách hàng ($count)"
    }

    private fun showLoading(show: Boolean, message: String? = null) {
        if (show) {
            binding.progressBar.visibility = View.VISIBLE
            binding.commentInputLayout.isEnabled = false
            binding.addToCartBtn.isEnabled = false
            binding.submitCommentBtn.isEnabled = false
        } else {
            binding.progressBar.visibility = View.GONE
            binding.commentInputLayout.isEnabled = true
            binding.addToCartBtn.isEnabled = true
            binding.submitCommentBtn.isEnabled = true
        }
    }
}