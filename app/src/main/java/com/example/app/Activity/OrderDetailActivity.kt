package com.example.app.Activity

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app.Adapter.OrderItemAdapter
import com.example.app.Helper.TinyDB
import com.example.app.Model.OrderItemModel
import com.example.app.Network.RetrofitClient
import com.example.app.databinding.ActivityOrderDetailBinding
import java.text.NumberFormat
import java.util.*

class OrderDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderDetailBinding
    private lateinit var tinyDB: TinyDB
    private var orderId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tinyDB = TinyDB(this)
        orderId = intent.getLongExtra("ORDER_ID", 0)

        if (orderId == 0L) {
            Toast.makeText(this, "Không tìm thấy đơn hàng", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        loadOrderDetails()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun loadOrderDetails() {
        RetrofitClient.ordersApi().getOrderDetails(orderId).enqueue(
            object : retrofit2.Callback<Map<String, Any>> {
                override fun onResponse(
                    call: retrofit2.Call<Map<String, Any>>,
                    response: retrofit2.Response<Map<String, Any>>
                ) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.get("success") as? Boolean == true) {
                            displayOrderDetails(body)
                        } else {
                            Toast.makeText(
                                this@OrderDetailActivity,
                                "Không tìm thấy thông tin đơn hàng",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            this@OrderDetailActivity,
                            "Lỗi tải thông tin đơn hàng",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: retrofit2.Call<Map<String, Any>>, t: Throwable) {
                    Toast.makeText(
                        this@OrderDetailActivity,
                        "Lỗi kết nối: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun displayOrderDetails(orderData: Map<String, Any>) {
        val order = orderData["order"] as? Map<String, Any> ?: return

        binding.orderIdTxt.text = "Mã đơn: #${order["Id"]}"
        binding.orderDateTxt.text = "Ngày đặt: ${order["Order_date"]}"
        binding.statusTxt.text = "Trạng thái: ${order["Order_status"]}"
        binding.addressTxt.text = "Địa chỉ: ${order["Ship_address"]}"
        binding.paymentTypeTxt.text = "Phương thức: ${order["Payment_type"]}"

        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        val total = (order["total"] as? Number)?.toDouble() ?: 0.0
        binding.totalTxt.text = "Tổng tiền: ${formatter.format(total)}đ"

        // Hiển thị items
        val items = order["items"] as? List<Map<String, Any>> ?: emptyList()
        val orderItems = items.map {
            OrderItemModel(
                id = (it["Id"] as? Number)?.toInt() ?: 0,
                Product_name = it["Product_name"] as? String,
                Unit_price = (it["Unit_price"] as? Number)?.toDouble() ?: 0.0,
                Quantity = (it["Quantity"] as? Number)?.toInt() ?: 0,
                picUrl = it["picUrl"] as? String
            )
        }

        binding.itemsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.itemsRecyclerView.adapter = OrderItemAdapter(orderItems)
    }
}