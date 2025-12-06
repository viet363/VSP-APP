package com.example.app.Activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app.Adapter.CartAdapter
import com.example.app.Network.RetrofitClient
import com.example.app.databinding.ActivityCartBinding
import com.example.app.Helper.ManagmentCart
import com.example.app.Helper.ChangeNumberItemsListener
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
    private var delivery: Double = 10000.0 // Đã chuyển thành biến
    private var selectedPaymentMethod = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        managerCart = ManagmentCart(this)

        loadCartFromServer()
        setVariable()
        calculatorCart() // Tính toán lần đầu để hiển thị
    }

    private fun initCartList() {
        val list = managerCart.getLocalCart()

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

        // Kiểm tra nếu giỏ hàng trống
        if (list.isEmpty()) {
            showEmptyCart()
        }
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
        val totalFee = managerCart.getTotalFee()

        // Tính toán tax và tổng tiền
        tax = totalFee * percentTax
        val total = totalFee + tax + delivery

        // Format số tiền theo VNĐ
        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))

        // Hiển thị lên UI
        binding.totalFeeTxt.text = formatter.format(totalFee) + "đ"
        binding.taxTxt.text = formatter.format(tax) + "đ"
        binding.deliveryTxt.text = formatter.format(delivery) + "đ"
        binding.totalTxt.text = formatter.format(total) + "đ"

        // Cập nhật trạng thái nút thanh toán
        updateCheckoutButton(total)
    }

    private fun updateCheckoutButton(total: Double) {
        // Có thể thêm logic để disable/enable nút thanh toán dựa trên tổng tiền
        if (total > 0) {
            binding.button.isEnabled = true
            binding.button.alpha = 1f
        } else {
            binding.button.isEnabled = false
            binding.button.alpha = 0.5f
        }
    }

    private fun showEmptyCart() {
        // Có thể hiển thị UI khi giỏ hàng trống
        binding.button.isEnabled = false
        binding.button.alpha = 0.5f
        binding.button.text = "Giỏ hàng trống"
    }

    private fun createOrderCash() {
        Toast.makeText(this, "Đang xử lý đơn hàng...", Toast.LENGTH_SHORT).show()

        // Giả lập API call thành công
        handleOrderSuccess()
    }

    private fun createOrderForVNPay() {
        Toast.makeText(this, "Đang chuyển hướng đến VNPay...", Toast.LENGTH_SHORT).show()

        // Giả lập xử lý VNPay thành công
        // Trong thực tế, bạn sẽ xử lý redirect đến VNPay và quay lại sau khi thanh toán
        handleOrderSuccess()
    }

    private fun handleOrderSuccess() {
        // Xóa giỏ hàng sau khi đặt hàng thành công
        managerCart.clearCart()

        // Hiển thị thông báo thành công
        Toast.makeText(this, "Đặt hàng thành công!", Toast.LENGTH_LONG).show()

        // Delay một chút để người dùng thấy thông báo
        binding.root.postDelayed({
            // Quay về MainActivity và xóa stack activity
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish() // Đóng CartActivity
        }, 1500) // Delay 1.5 giây
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
                    } else {
                        // Nếu API trả về lỗi, vẫn hiển thị giỏ hàng local
                        initCartList()
                        calculatorCart()
                    }
                }

                override fun onFailure(call: Call<CartResponse>, t: Throwable) {
                    Toast.makeText(this@CartActivity, "Lỗi tải giỏ hàng", Toast.LENGTH_SHORT).show()
                    // Vẫn hiển thị giỏ hàng local nếu có
                    initCartList()
                    calculatorCart()
                }
            })
    }

    // Các phương thức để thay đổi delivery nếu cần
    fun updateDelivery(newDelivery: Double) {
        delivery = newDelivery
        calculatorCart() // Tính toán lại
    }

    fun getCurrentDelivery(): Double {
        return delivery
    }

    fun getCurrentTax(): Double {
        return tax
    }
}