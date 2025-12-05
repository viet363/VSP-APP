package com.example.app.Activity

import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app.Adapter.CartAdapter
import com.example.app.Network.RetrofitClient
import com.example.app.databinding.ActivityCartBinding
import com.example.app.Helper.ManagmentCart
import com.example.app.Helper.ChangeNumberItemsListener // Thêm import này
import com.example.app.Model.CartResponse
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        managerCart = ManagmentCart(this)

        loadCartFromServer()
        setVariable()
    }

    private fun initCartList() {
        val list = managerCart.getLocalCart()

        // Sửa ở đây: Truyền đủ 3 tham số
        val adapter = CartAdapter(
            listItemSelected = list,
            context = this,
            listener = object : ChangeNumberItemsListener {
                override fun onChanged() {
                    calculatorCart()
                }
            }
        )

        binding.viewCart.layoutManager = LinearLayoutManager(this)
        binding.viewCart.adapter = adapter
    }

    private fun setVariable() {
        binding.backBtn.setOnClickListener { finish() }

        binding.button.setOnClickListener {
            if (managerCart.getLocalCart().isEmpty()) {
                Toast.makeText(this, "Giỏ hàng đang trống!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedPaymentMethod == 1) {
                createOrderCash()
            } else {
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
        Toast.makeText(this, "API Order đang xử lý", Toast.LENGTH_SHORT).show()
    }

    private fun createOrderForVNPay() {
        Toast.makeText(this, "API VNPay đang xử lý", Toast.LENGTH_SHORT).show()
    }

    private fun loadCartFromServer() {
        RetrofitClient.cartApi().getCart()
            .enqueue(object : Callback<CartResponse> {
                override fun onResponse(
                    call: Call<CartResponse>,
                    response: Response<CartResponse>
                ) {
                    val body = response.body()
                    if (body != null && body.success) {
                        managerCart.syncFromServer(body.items)
                        initCartList()
                        calculatorCart()
                    }
                }

                override fun onFailure(call: Call<CartResponse>, t: Throwable) {
                    Toast.makeText(this@CartActivity, "Lỗi tải giỏ hàng", Toast.LENGTH_SHORT).show()
                }
            })
    }
}