package com.example.app.Activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app.Adapter.CartAdapter
import com.example.app.Network.RetrofitClient
import com.example.app.databinding.ActivityCartBinding
import com.example.project1762.Helper.ChangeNumberItemsListener
import com.example.project1762.Helper.ManagmentCart
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.*

class CartActivity : BaseActivity() {

    private lateinit var binding: ActivityCartBinding
    private lateinit var managerCart: ManagmentCart
    private var tax: Double = 0.0
    private var selectedPaymentMethod = 1

    // Thêm biến để lưu orderId tạm thời
    private var tempOrderId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        managerCart = ManagmentCart(this)

        setVariable()
        initCartList()
        calculatorCart()
    }

    private fun initCartList() {
        val adapter = CartAdapter(managerCart.getListCart(), this, object : ChangeNumberItemsListener {
            override fun onChanged() {
                calculatorCart()
            }
        })

        binding.viewCart.layoutManager = LinearLayoutManager(this)
        binding.viewCart.adapter = adapter

        binding.emptyTxt.visibility =
            if (managerCart.getListCart().isEmpty()) View.VISIBLE else View.GONE

        binding.scrollView3.visibility =
            if (managerCart.getListCart().isEmpty()) View.GONE else View.VISIBLE
    }

    private fun setVariable() {
        binding.backBtn.setOnClickListener { finish() }

        binding.button.setOnClickListener {
            if (managerCart.getListCart().isEmpty()) {
                Toast.makeText(this, "Giỏ hàng đang trống!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedPaymentMethod == 1) {
                createOrderCash()
            } else {
                // Tạo order trước khi thanh toán VNPay
                createOrderForVNPay()
            }
        }

        binding.metod1.setOnClickListener {
            selectedPaymentMethod = 1
            highlightMethod()
        }

        binding.method2.setOnClickListener {
            selectedPaymentMethod = 2
            highlightMethod()
        }

        highlightMethod()
    }

    private fun highlightMethod() {
        if (selectedPaymentMethod == 1) {
            binding.metod1.setBackgroundResource(com.example.app.R.drawable.green_bg_selected)
            binding.method2.setBackgroundResource(com.example.app.R.drawable.grey_bg_selected)
        } else {
            binding.method2.setBackgroundResource(com.example.app.R.drawable.green_bg_selected)
            binding.metod1.setBackgroundResource(com.example.app.R.drawable.grey_bg_selected)
        }
    }

    private fun calculatorCart() {
        val percentTax = 0.02
        val delivery = 10000.0
        val totalFee = managerCart.getTotalFee()

        tax = totalFee * percentTax
        val total = totalFee + tax + delivery

        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))

        binding.totalFeeTxt.text = formatter.format(totalFee) + "đ"
        binding.taxTxt.text = formatter.format(tax) + "đ"
        binding.deliveryTxt.text = formatter.format(delivery) + "đ"
        binding.totalTxt.text = formatter.format(total) + "đ"
    }

    private fun createOrderCash() {
        val items = managerCart.getListCart()

        // Sửa lại body để đúng định dạng
        val body = hashMapOf<String, Any>(
            "items" to items,
            "paymentMethod" to "Cash",
            "total" to (managerCart.getTotalFee() + tax + 10000)
        )

        RetrofitClient.ordersApi.createOrder(body).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(
                call: Call<Map<String, Any>>,
                response: Response<Map<String, Any>>
            ) {
                if (response.isSuccessful) {
                    Toast.makeText(this@CartActivity, "Đặt hàng thành công!", Toast.LENGTH_SHORT).show()
                    managerCart.setCartList(arrayListOf())
                    finish()
                } else {
                    Toast.makeText(this@CartActivity, "Lỗi đặt hàng!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Toast.makeText(this@CartActivity, "Lỗi kết nối!", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun createOrderForVNPay() {
        val items = managerCart.getListCart()

        val body = hashMapOf<String, Any>(
            "items" to items,
            "paymentMethod" to "VNPay",
            "total" to (managerCart.getTotalFee() + tax + 10000)
        )

        RetrofitClient.ordersApi.createOrder(body).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(
                call: Call<Map<String, Any>>,
                response: Response<Map<String, Any>>
            ) {
                if (response.isSuccessful) {
                    // Lấy orderId từ response và thực hiện thanh toán VNPay
                    val orderId = response.body()?.get("id") as? Long
                    if (orderId != null) {
                        tempOrderId = orderId
                        createVNPayOrder(orderId)
                    } else {
                        Toast.makeText(this@CartActivity, "Không thể lấy ID đơn hàng!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@CartActivity, "Lỗi tạo đơn hàng!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Toast.makeText(this@CartActivity, "Lỗi kết nối!", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun createVNPayOrder(orderId: Long) {
        val amount = ((managerCart.getTotalFee() + tax + 10000) * 100).toLong()

        RetrofitClient.paymentApi.createVNPayUrlMobile(orderId, amount)
            .enqueue(object : Callback<Map<String, Any>> {
                override fun onResponse(
                    call: Call<Map<String, Any>>,
                    response: Response<Map<String, Any>>
                ) {
                    if (!response.isSuccessful) {
                        Toast.makeText(this@CartActivity, "Lỗi tạo VNPay URL!", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val url = response.body()?.get("paymentUrl") as? String
                    if (url != null) {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(browserIntent)
                    }
                }

                override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                    Toast.makeText(this@CartActivity, "Không thể kết nối server!", Toast.LENGTH_SHORT).show()
                }
            })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        val data = intent.data ?: return
        val responseCode = data.getQueryParameter("vnp_ResponseCode")

        if (responseCode == "00") {
            Toast.makeText(this, "Thanh toán thành công!", Toast.LENGTH_LONG).show()
            managerCart.setCartList(arrayListOf())
            finish()
        } else {
            Toast.makeText(this, "Thanh toán thất bại!", Toast.LENGTH_LONG).show()
        }
    }
}