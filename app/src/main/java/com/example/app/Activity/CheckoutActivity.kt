package com.example.app.Activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app.Adapter.CartAdapter
import com.example.app.Helper.ChangeNumberItemsListener
import com.example.app.Helper.ManagmentCart
import com.example.app.Helper.TinyDB
import com.example.app.Model.MoMoRequest
import com.example.app.Model.MoMoResponse
import com.example.app.Model.OrderItemRequest
import com.example.app.Model.OrderRequest
import com.example.app.Model.OrderResponse
import com.example.app.Model.VNPayRequest
import com.example.app.Model.VNPayResponse
import com.example.app.Network.RetrofitClient
import com.example.app.databinding.ActivityCheckoutBinding
import retrofit2.Call
import retrofit2.Response
import java.text.NumberFormat
import java.util.*

class CheckoutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCheckoutBinding
    private lateinit var managerCart: ManagmentCart
    private lateinit var tinyDB: TinyDB

    private var selectedPaymentMethod = "COD"
    private var selectedAddressId = 0
    private var deliveryFee = 0
    private var lastCreatedOrderId: Long = 0 // Lưu lại orderId để xử lý khi quay về

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        managerCart = ManagmentCart(this)
        tinyDB = TinyDB(this)

        // Kiểm tra nếu quay về từ thanh toán thất bại
        val paymentFailed = intent.getBooleanExtra("PAYMENT_FAILED", false)
        val errorMessage = intent.getStringExtra("ERROR_MESSAGE")

        if (paymentFailed) {
            Toast.makeText(this, errorMessage ?: "Thanh toán thất bại, vui lòng thử lại", Toast.LENGTH_LONG).show()
        }

        setupUI()
        loadAddress()
        calculateTotals()
    }

    private fun setupUI() {
        val cartItems = managerCart.getLocalCart()

        val changeNumberListener = object : ChangeNumberItemsListener {
            override fun onChanged() {
                calculateTotals()
            }
        }

        binding.recyclerViewCart.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewCart.adapter = CartAdapter(
            ArrayList(cartItems),
            this,
            changeNumberListener
        )

        // Payment method listeners
        binding.radioCash.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) selectedPaymentMethod = "COD"
        }

        binding.radioVNPay.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) selectedPaymentMethod = "VNPay"
        }

        binding.radioMoMo.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) selectedPaymentMethod = "MoMo"
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnPlaceOrder.setOnClickListener {
            if (selectedAddressId == 0) {
                Toast.makeText(this, "Vui lòng chọn địa chỉ giao hàng", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (managerCart.getLocalCart().isEmpty()) {
                Toast.makeText(this, "Giỏ hàng trống", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            placeOrder()
        }
    }

    private fun loadAddress() {
        // TODO: Load address từ API hoặc database
        selectedAddressId = 1
        binding.txtAddress.text = "Địa chỉ đã chọn: ID $selectedAddressId"
    }

    private fun calculateTotals() {
        val subtotal = managerCart.getTotalFee()
        val tax = subtotal * 0.02
        val total = subtotal + tax + deliveryFee

        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))

        binding.txtSubtotal.text = formatter.format(subtotal) + "đ"
        binding.txtTax.text = formatter.format(tax) + "đ"
        binding.txtDelivery.text = formatter.format(deliveryFee) + "đ"
        binding.txtTotal.text = formatter.format(total) + "đ"
    }

    private fun calculateTotalAmount(): Double {
        val subtotal = managerCart.getTotalFee()
        val tax = subtotal * 0.02
        return subtotal + tax + deliveryFee
    }

    private fun placeOrder() {
        val cartItems = managerCart.getLocalCart()

        val orderItems = cartItems.map { cartItem ->
            OrderItemRequest(
                productId = cartItem.item.id.toLong(),
                quantity = cartItem.quantity,
                price = cartItem.item.price
            )
        }

        val orderRequest = OrderRequest(
            addressId = selectedAddressId,
            note = binding.edtNote.text.toString(),
            paymentMethod = selectedPaymentMethod,
            items = orderItems
        )

        Toast.makeText(this, "Đang tạo đơn hàng...", Toast.LENGTH_SHORT).show()

        RetrofitClient.ordersApi().createOrder(orderRequest).enqueue(
            object : retrofit2.Callback<OrderResponse> {
                override fun onResponse(
                    call: Call<OrderResponse>,
                    response: Response<OrderResponse>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val orderId = response.body()!!.orderId
                        val totalAmount = calculateTotalAmount()

                        // Lưu lại orderId để xử lý khi quay về
                        lastCreatedOrderId = orderId

                        when (selectedPaymentMethod) {
                            "VNPay" -> startVNPayPayment(orderId)
                            "MoMo" -> startMoMoPayment(orderId, totalAmount)
                            else -> handleOrderSuccess(orderId) // COD
                        }
                    } else {
                        val errorMsg = if (response.errorBody() != null) {
                            response.errorBody()?.string() ?: "Lỗi không xác định"
                        } else {
                            response.message()
                        }
                        Toast.makeText(
                            this@CheckoutActivity,
                            "Lỗi tạo đơn hàng: $errorMsg",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<OrderResponse>, t: Throwable) {
                    Toast.makeText(
                        this@CheckoutActivity,
                        "Lỗi kết nối: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun startVNPayPayment(orderId: Long) {
        val request = VNPayRequest(orderId = orderId)

        RetrofitClient.paymentApi().createVNPayUrlMobile(request).enqueue(
            object : retrofit2.Callback<VNPayResponse> {
                override fun onResponse(
                    call: Call<VNPayResponse>,
                    response: Response<VNPayResponse>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val paymentUrl = response.body()!!.paymentUrl

                        val intent = Intent(this@CheckoutActivity, PaymentWebViewActivity::class.java).apply {
                            putExtra("PAYMENT_URL", paymentUrl)
                            putExtra("ORDER_ID", orderId)
                        }
                        startActivityForResult(intent, REQUEST_VNPAY_PAYMENT)
                    } else {
                        Toast.makeText(
                            this@CheckoutActivity,
                            "Lỗi tạo URL thanh toán VNPay",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<VNPayResponse>, t: Throwable) {
                    Toast.makeText(
                        this@CheckoutActivity,
                        "Lỗi kết nối: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun startMoMoPayment(orderId: Long, amount: Double) {
        val request = MoMoRequest(
            orderId = orderId,
            amount = amount,
            orderInfo = "Thanh toán đơn hàng $orderId"
        )

        RetrofitClient.paymentApi().createMoMoUrlMobile(request).enqueue(
            object : retrofit2.Callback<MoMoResponse> {
                override fun onResponse(
                    call: Call<MoMoResponse>,
                    response: Response<MoMoResponse>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val paymentUrl = response.body()!!.payUrl

                        val intent = Intent(this@CheckoutActivity, MoMoWebViewActivity::class.java).apply {
                            putExtra("PAYMENT_URL", paymentUrl)
                            putExtra("ORDER_ID", orderId)
                        }
                        startActivityForResult(intent, REQUEST_MOMO_PAYMENT)
                    } else {
                        val errorMsg = if (response.errorBody() != null) {
                            response.errorBody()?.string() ?: "Lỗi không xác định"
                        } else {
                            response.message()
                        }
                        Toast.makeText(
                            this@CheckoutActivity,
                            "Lỗi tạo URL thanh toán MoMo: $errorMsg",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<MoMoResponse>, t: Throwable) {
                    Toast.makeText(
                        this@CheckoutActivity,
                        "Lỗi kết nối: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun handleOrderSuccess(orderId: Long) {
        managerCart.clearCart()

        Toast.makeText(
            this,
            "Đặt hàng thành công! Mã đơn hàng: $orderId",
            Toast.LENGTH_LONG
        ).show()

        // Đi đến màn hình đơn hàng
        val intent = Intent(this, MyOrderActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_VNPAY_PAYMENT, REQUEST_MOMO_PAYMENT -> {
                if (resultCode == RESULT_OK) {
                    val isSuccess = data?.getBooleanExtra("PAYMENT_SUCCESS", false) ?: false

                    if (isSuccess) {
                        // Thanh toán thành công
                        val orderId = data?.getLongExtra("ORDER_ID", lastCreatedOrderId) ?: lastCreatedOrderId
                        handleOrderSuccess(orderId)
                    } else {
                        // Thanh toán thất bại - Quay lại CheckoutActivity với thông báo lỗi
                        val errorMessage = data?.getStringExtra("ERROR_MESSAGE") ?: "Thanh toán thất bại, vui lòng thử lại"

                        // Không clear cart, giữ lại giỏ hàng
                        val intent = Intent(this, CheckoutActivity::class.java).apply {
                            putExtra("PAYMENT_FAILED", true)
                            putExtra("ERROR_MESSAGE", errorMessage)
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(intent)
                        finish()
                    }
                } else {
                    // Người dùng hủy thanh toán
                    val errorMessage = data?.getStringExtra("ERROR_MESSAGE") ?: "Người dùng hủy thanh toán"

                    val intent = Intent(this, CheckoutActivity::class.java).apply {
                        putExtra("PAYMENT_FAILED", true)
                        putExtra("ERROR_MESSAGE", errorMessage)
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    companion object {
        private const val REQUEST_VNPAY_PAYMENT = 1001
        private const val REQUEST_MOMO_PAYMENT = 1002
    }
}