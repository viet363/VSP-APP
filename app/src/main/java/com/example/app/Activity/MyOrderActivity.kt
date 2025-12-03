package com.example.app.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app.Adapter.OrderAdapter
import com.example.app.Helper.TinyDB
import com.example.app.Model.OrderModel
import com.example.app.Model.OrderResponse
import com.example.app.Network.RetrofitClient
import com.example.app.databinding.ActivityMyOrderBinding

class MyOrderActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMyOrderBinding
    private lateinit var tinyDB: TinyDB
    private val orders = mutableListOf<OrderModel>()
    private val TAG = "MyOrderActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tinyDB = TinyDB(this)

        binding.backBtn.setOnClickListener {
            try {
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                Log.e(TAG, "Error starting MainActivity: ${e.message}")
                Toast.makeText(this, "Lỗi mở MainActivity: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        loadProfile()
        initOrderList()
        loadOrders()
    }

    private fun loadProfile() {
        val userId = tinyDB.getLong("userId")
        if (userId == 0L) {
            Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Hiển thị thông tin user từ TinyDB
        val fullname = tinyDB.getString("fullname") ?: "Chưa cập nhật"
        val email = tinyDB.getString("email") ?: "Chưa cập nhật"
        val phone = tinyDB.getString("phone") ?: "Chưa cập nhật"

        binding.nameTxt.text = "Tên: $fullname"
        binding.addressTxt.text = "Email: $email"
        binding.phoneTxt.text = "Số điện thoại: $phone"

        if (phone == "Chưa cập nhật") {
            Toast.makeText(this, "Vui lòng cập nhật số điện thoại trong phần thông tin cá nhân", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadOrders() {

        showLoading()

        RetrofitClient.ordersApi.getOrders().enqueue(object : retrofit2.Callback<OrderResponse> {
            override fun onResponse(call: retrofit2.Call<OrderResponse>, response: retrofit2.Response<OrderResponse>) {
                hideLoading()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        orders.clear()
                        orders.addAll(body.orders)
                        binding.orderRecyclerView.adapter?.notifyDataSetChanged()
                        Log.d(TAG, "Orders loaded: ${orders.size}")

                        if (orders.isEmpty()) {
                            Toast.makeText(this@MyOrderActivity, "Bạn chưa có đơn hàng nào", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val errorMsg = body?.message ?: "Lỗi không xác định"
                        Toast.makeText(this@MyOrderActivity, errorMsg, Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "API error: $errorMsg")
                    }
                } else {
                    val errorMessage = when (response.code()) {
                        404 -> "Không tìm thấy đơn hàng"
                        401 -> "Phiên đăng nhập hết hạn"
                        500 -> "Lỗi server"
                        else -> "Lỗi tải đơn hàng: ${response.code()}"
                    }
                    Toast.makeText(this@MyOrderActivity, errorMessage, Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Failed to load orders: ${response.code()} - ${response.message()}")
                }
            }

            override fun onFailure(call: retrofit2.Call<OrderResponse>, t: Throwable) {
                hideLoading()
                Toast.makeText(this@MyOrderActivity, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Failed to load orders: ${t.message}")
            }
        })
    }

    private fun initOrderList() {
        binding.orderRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.orderRecyclerView.adapter = OrderAdapter(orders)
    }

    private fun showLoading() {
        binding.progressBar.visibility = android.view.View.VISIBLE
    }

    private fun hideLoading() {
        binding.progressBar.visibility = android.view.View.GONE
    }
}