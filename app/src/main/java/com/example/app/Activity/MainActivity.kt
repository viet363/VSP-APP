package com.example.app.Activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app.Adapter.CategoryAdapter
import com.example.app.Adapter.RecommendedAdapter
import com.example.app.Adapter.SliderAdapter
import com.example.app.Helper.TinyDB
import com.example.app.Model.CategoryModel
import com.example.app.Model.SliderModel
import com.example.app.ViewModel.MainViewModel
import com.example.app.databinding.ActivityMainBinding

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var tinyDB: TinyDB
    private val viewModel: MainViewModel by viewModels()

    // FIX lỗi userId không tồn tại trong hàm khác
    private var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tinyDB = TinyDB(this)

        userId = tinyDB.getString("userId") ?: ""

        if (userId.isEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding.nametitle.text =
            tinyDB.getString("profile_name") ?: "Khách hàng thân mến"

        initBanner()
        initCategory()
        initRecommended()
        initBottomMenu()
        initSearch()
    }

    // ------------------------- SEARCH -------------------------
    private fun initSearch() {
        binding.btnSearch.setOnClickListener {
            binding.btnSearch.visibility = View.GONE
            binding.searchView.visibility = View.VISIBLE
            binding.btnSearchSubmit.visibility = View.VISIBLE
            binding.searchView.requestFocus()
        }

        binding.btnSearchSubmit.setOnClickListener {
            val query = binding.searchView.query.toString().trim()

            if (query.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên sản phẩm", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.searchProducts(query)

                viewModel.searchResults.observe(this) { results ->
                    // Xử lý kết quả tìm kiếm nếu cần
                }

                // CHỈ GỬI query – KHÔNG gửi Parcelable
                val intent = Intent(this, ListItemsActivity::class.java)
                intent.putExtra("searchQuery", query)
                startActivity(intent)
            }

            binding.searchView.setQuery("", false)
            binding.searchView.visibility = View.GONE
            binding.btnSearchSubmit.visibility = View.GONE
            binding.btnSearch.visibility = View.VISIBLE
        }
    }

    private fun initRecommended() {
        binding.progressBarRecommend.visibility = View.VISIBLE
        binding.viewRecommendation.layoutManager = GridLayoutManager(this, 2)

        viewModel.recommended.observe(this) { items ->
            binding.progressBarRecommend.visibility = View.GONE
            if (items.isNotEmpty()) {
                binding.viewRecommendation.adapter =
                    RecommendedAdapter(items.toMutableList())
            }
        }

        val userIdLong = userId.toLongOrNull() ?: 0L
        viewModel.loadRecommended(userIdLong)
    }

    private fun initCategory() {
        binding.progressBarCategory.visibility = View.VISIBLE

        binding.viewCategory.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        viewModel.categories.observe(this) { list ->
            binding.progressBarCategory.visibility = View.GONE

            if (list.isEmpty()) {
                binding.viewCategory.adapter = CategoryAdapter(
                    mutableListOf(
                        CategoryModel(1, "Electronic", "cat1.png"),
                        CategoryModel(2, "Fashion", "cat2.png")
                    )
                )
            } else {
                binding.viewCategory.adapter = CategoryAdapter(list.toMutableList())
            }
        }

        viewModel.loadCategories()
    }

    private fun initBanner() {
        binding.progressBarSlider.visibility = View.VISIBLE

        // Sửa lỗi: Thay 'banners' bằng 'recommended' hoặc tạo LiveData riêng cho banner
        // Tạm thời tạo banner giả
        val fakeBanners = listOf(
            SliderModel("banner1.png"),
            SliderModel("banner2.png")
        )
        showBanner(fakeBanners)

        // Nếu có API banner thì sử dụng:
        // viewModel.banners.observe(this) { bannerList ->
        //     val list = if (bannerList.isEmpty()) {
        //         fakeBanners
        //     } else bannerList
        //     showBanner(list)
        // }
    }

    private fun showBanner(images: List<SliderModel>) {
        binding.progressBarSlider.visibility = View.GONE

        val adapter = SliderAdapter(images, binding.viewPager2)
        binding.viewPager2.adapter = adapter
        binding.viewPager2.clipToPadding = false
        binding.viewPager2.clipChildren = false
        binding.viewPager2.offscreenPageLimit = 3

        // Kiểm tra null trước khi truy cập
        val child = binding.viewPager2.getChildAt(0)
        if (child != null) {
            child.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        }
    }

    private fun initBottomMenu() {
        binding.cartBtn.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }
        binding.profileBtn.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        binding.orderBtn.setOnClickListener {
            startActivity(Intent(this, MyOrderActivity::class.java))
        }
        binding.chatBtn.setOnClickListener {
            startActivity(Intent(this, MyChatActivity::class.java))
        }
    }
}