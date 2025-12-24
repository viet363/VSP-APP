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
import com.example.app.Model.OrdersResponse
import com.example.app.Network.RetrofitClient
import com.example.app.databinding.ActivityMyOrderBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyOrderActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMyOrderBinding
    private lateinit var tinyDB: TinyDB
    private val orders = mutableListOf<OrderModel>()
    private lateinit var orderAdapter: OrderAdapter
    private val TAG = "MyOrderActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tinyDB = TinyDB(this)

        binding.backBtn.setOnClickListener {
            try {
                finish()
            } catch (e: Exception) {
                Log.e(TAG, "Error: ${e.message}")
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

        val fullname = tinyDB.getString("fullname") ?: "Chưa cập nhật"
        val email = tinyDB.getString("email") ?: "Chưa cập nhật"
        val phone = tinyDB.getString("phone") ?: "Chưa cập nhật"

        binding.nameTxt.text = "Tên: $fullname"
        binding.addressTxt.text = "Email: $email"
        binding.phoneTxt.text = "Số điện thoại: $phone"

        if (phone == "Chưa cập nhật") {
            Toast.makeText(this, "Vui lòng cập nhật số điện thoại", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadOrders() {
        showLoading()

        RetrofitClient.ordersApi().getOrders().enqueue(object : Callback<OrdersResponse> {
            override fun onResponse(
                call: Call<OrdersResponse>,
                response: Response<OrdersResponse>
            ) {
                hideLoading()
                if (response.isSuccessful) {
                    val ordersResponse = response.body()
                    if (ordersResponse?.success == true) {
                        val orderList = ordersResponse.orders ?: emptyList()

                        orders.clear()
                        orders.addAll(orderList)
                        orderAdapter.updateData(orders)

                        Log.d(TAG, "Orders loaded: ${orders.size}")

                        if (orders.isEmpty()) {
                            Toast.makeText(this@MyOrderActivity, "Bạn chưa có đơn hàng nào", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(
                            this@MyOrderActivity,
                            ordersResponse?.message ?: "Không thể tải đơn hàng",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    val errorMessage = when (response.code()) {
                        404 -> "Không tìm thấy đơn hàng"
                        401 -> "Phiên đăng nhập hết hạn"
                        500 -> "Lỗi server"
                        else -> "Lỗi tải đơn hàng: ${response.code()}"
                    }
                    Toast.makeText(this@MyOrderActivity, errorMessage, Toast.LENGTH_SHORT).show()

                    try {
                        val errorBody = response.errorBody()?.string()
                        Log.e(TAG, "Error response body: $errorBody")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error reading error body: ${e.message}")
                    }
                }
            }

            override fun onFailure(call: Call<OrdersResponse>, t: Throwable) {
                hideLoading()
                Toast.makeText(this@MyOrderActivity, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Failed to load orders: ${t.message}")
                t.printStackTrace()
            }
        })
    }

    private fun initOrderList() {
        binding.orderRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        orderAdapter = OrderAdapter(orders)
        binding.orderRecyclerView.adapter = orderAdapter
    }

    private fun showLoading() {
        binding.progressBar.visibility = android.view.View.VISIBLE
    }

    private fun hideLoading() {
        binding.progressBar.visibility = android.view.View.GONE
    }
}