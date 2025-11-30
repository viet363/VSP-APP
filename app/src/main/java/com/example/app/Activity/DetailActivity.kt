package com.example.app.Activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.app.Adapter.CommentAdapter
import com.example.app.Adapter.PicAdapter
import com.example.app.Adapter.SpecificationAdapter
import com.example.app.Model.ItemsModel
import com.example.app.Model.ProductReviewModel
import com.example.app.Model.ProductSpecificationModel
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

        getIntentData()
        initUI()
        initObservers()
        loadData()
    }

    private fun getIntentData() {
        item = intent.getSerializableExtra("object") as? ItemsModel
        productId = item?.id ?: -1
    }

    private fun initUI() {
        binding.backBtn.setOnClickListener { finish() }

        Glide.with(this)
            .load(item?.picUrl?.firstOrNull())
            .into(binding.img)

        binding.picList.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        binding.titleTxt.text = item?.title
        binding.priceTxt.text = "${item?.price}₫"
        binding.raitingTxt.text = item?.rating.toString()
    }

    private fun initObservers() {

        // Chi tiết sản phẩm
        viewModel.productDetail.observe(this) { detail ->
            binding.derscriptionTxt.text = detail.Description ?: "Không có mô tả"
        }

        // Thông số kỹ thuật
        viewModel.productSpecifications.observe(this) { specs ->
            binding.modelList.apply {
                layoutManager = LinearLayoutManager(this@DetailActivity)
                adapter = SpecificationAdapter(specs.toMutableList()) // Sửa ở đây
            }
        }

        // Danh sách ảnh phụ
        viewModel.productImages.observe(this) { images ->
            binding.picList.adapter = PicAdapter(images.toMutableList()) { url -> // Sửa ở đây
                Glide.with(this)
                    .load(url)
                    .into(binding.img)
            }
        }

        // Comment
        viewModel.productReviews.observe(this) { comments ->
            binding.modelComment.apply {
                layoutManager = LinearLayoutManager(this@DetailActivity)
                adapter = CommentAdapter(comments)
            }
        }
    }

    private fun loadData() {
        if (productId == -1) return

        viewModel.loadProductDetail(productId)
        viewModel.loadSpecifications(productId)
        viewModel.loadReviews(productId)
        viewModel.loadProductImages(productId)
    }
}