package com.example.app.Activity

import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app.Adapter.CartAdapter
import com.example.app.Helper.TinyDB
import com.example.app.Model.ItemsModel
import com.example.app.Model.VNPayModel
import com.example.app.R
import com.example.app.Utilities.VNPayUtils
import com.example.app.databinding.ActivityCartBinding
import com.example.project1762.Helper.ChangeNumberItemsListener
import com.example.project1762.Helper.ManagmentCart
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.net.InetAddress
import java.text.NumberFormat
import java.util.Locale

class CartActivity : BaseActivity() {
    private lateinit var binding: ActivityCartBinding
    private lateinit var managerCart: ManagmentCart
    private lateinit var tinyDB: TinyDB
    private var tax: Double = 0.0
    private var selectedPaymentMethod: Int = 1 // 1: Cash, 2: VNPay

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        managerCart = ManagmentCart(this)
        tinyDB = TinyDB(this)

        setVariable()
        initCartList()
        calculatorCart()
        loadCartFromFirebase()
    }



    private fun initCartList() {
        binding.viewCart.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.viewCart.adapter = CartAdapter(managerCart.getListCart(), this, object : ChangeNumberItemsListener {
            override fun onChanged() {
                calculatorCart()
                saveCartToFirebase()
            }
        })
        with(binding) {
            emptyTxt.visibility = if (managerCart.getListCart().isEmpty()) View.VISIBLE else View.GONE
            scrollView3.visibility = if (managerCart.getListCart().isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun setVariable() {
        binding.apply {
            backBtn.setOnClickListener { finish() }
            button.setOnClickListener {
                if (selectedPaymentMethod == 1) {
                    saveOrderToFirebase()
                } else {
                    paymentVNPay()
                }
            }

            metod1.setOnClickListener {
                selectedPaymentMethod = 1
                metod1.setBackgroundResource(R.drawable.green_bg_selected)
                metodIc1.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this@CartActivity, R.color.green))
                methodtitle1.setTextColor(getResources().getColor(R.color.green))
                methodSubTitle1.setTextColor(getResources().getColor(R.color.green))

                method2.setBackgroundResource(R.drawable.grey_bg_selected)
                metodIc2.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this@CartActivity, R.color.black))
                methodtitle2.setTextColor(getResources().getColor(R.color.black))
                methodSubTitle2.setTextColor(getResources().getColor(R.color.grey))
            }

            method2.setOnClickListener {
                selectedPaymentMethod = 2
                method2.setBackgroundResource(R.drawable.green_bg_selected)
                metodIc2.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this@CartActivity, R.color.green))
                methodtitle2.setTextColor(getResources().getColor(R.color.green))
                methodSubTitle2.setTextColor(getResources().getColor(R.color.green))

                metod1.setBackgroundResource(R.drawable.grey_bg_selected)
                metodIc1.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this@CartActivity, R.color.black))
                methodtitle1.setTextColor(getResources().getColor(R.color.black))
                methodSubTitle1.setTextColor(getResources().getColor(R.color.grey))
            }
        }
    }



    private fun saveCartToFirebase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance().reference
        val cartRef = database.child("Users").child(userId).child("cart")
        val cartItems = managerCart.getListCart()
        cartRef.setValue(cartItems).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                tinyDB.putListObject("CartList", ArrayList(cartItems))
                Log.d("CartActivity", "Cart saved to Firebase: $cartItems")
            } else {
                Log.e("CartActivity", "Failed to save cart: ${task.exception?.message}")
                Toast.makeText(this, "Lỗi lưu giỏ hàng!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadCartFromFirebase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance().reference
        database.child("Users").child(userId).child("cart")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val cartItems = ArrayList<ItemsModel>()
                    for (itemSnapshot in snapshot.children) {
                        val item = itemSnapshot.getValue(ItemsModel::class.java)
                        item?.let { cartItems.add(it) }
                    }
                    if (cartItems.isNotEmpty()) {
                        managerCart.setCartList(cartItems)
                        binding.viewCart.adapter?.notifyDataSetChanged()
                        calculatorCart()
                    }
                    with(binding) {
                        emptyTxt.visibility = if (managerCart.getListCart().isEmpty()) View.VISIBLE else View.GONE
                        scrollView3.visibility = if (managerCart.getListCart().isEmpty()) View.GONE else View.VISIBLE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("CartActivity", "Failed to load cart: ${error.message}")
                    Toast.makeText(this@CartActivity, "Lỗi tải giỏ hàng!", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun saveOrderToFirebase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance().reference
        val randomId = "OD" + (100..999).random().toString()
        val orderRef = database.child("Users").child(userId).child("orders").child(randomId)
        val order = HashMap<String, Any>()

        order["orderId"] = randomId
        order["timestamp"] = System.currentTimeMillis()
        order["items"] = managerCart.getListCart()
        order["total"] = managerCart.getTotalFee() + tax + 10.0
        order["status"] = "Pending"
        order["paymentMethod"] = if (selectedPaymentMethod == 1) "Cash" else "VNPay"

        orderRef.setValue(order).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                managerCart.getListCart().clear()
                tinyDB.putListObject("CartList", ArrayList())
                database.child("Users").child(userId).child("cart").removeValue()
                binding.viewCart.adapter?.notifyDataSetChanged()
                calculatorCart()
                Toast.makeText(this, "Đặt hàng thành công!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Lỗi khi đặt hàng!", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun calculatorCart() {
        val percentTax = 0.02
        val delivery = 10.0
        tax = Math.round((managerCart.getTotalFee() * percentTax) * 100) / 100.0
        val itemTotal = Math.round((managerCart.getTotalFee()) * 100) / 100.0
        val total = itemTotal + tax + delivery

        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        formatter.minimumFractionDigits = 0
        formatter.maximumFractionDigits = 0

        with(binding) {
            totalFeeTxt.text = "${formatter.format(itemTotal.toLong())}$"
            taxTxt.text = "${formatter.format(tax.toLong())}$"
            deliveryTxt.text = "${formatter.format(delivery.toLong())}$"
            totalTxt.text = "${formatter.format(total.toLong())}$"
        }
    }

    private fun paymentVNPay() {
        val VND = 24000
        val totalAmountVND = ((managerCart.getTotalFee() + tax + 10.0) * VND).toLong()
        val totalAmount = totalAmountVND * 100

        // Lấy địa chỉ IP theo chuẩn mới
        val ipAddress = try {
            val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
            val ipInt = wifiManager.connectionInfo.ipAddress
            val ipByteArray = byteArrayOf(
                (ipInt and 0xff).toByte(),
                (ipInt shr 8 and 0xff).toByte(),
                (ipInt shr 16 and 0xff).toByte(),
                (ipInt shr 24 and 0xff).toByte()
            )
            InetAddress.getByAddress(ipByteArray).hostAddress ?: "127.0.0.1"
        } catch (e: Exception) {
            "127.0.0.1"
        }

        val txnRef = System.currentTimeMillis().toString()

        //  model thanh toán VNPay
        val vnPayParams = VNPayModel(
            vnp_TmnCode = "6E03FFCJ",
            vnp_Amount = totalAmount,
            vnp_IpAddr = ipAddress,
            vnp_TxnRef = txnRef,
            vnp_OrderInfo = "Thanh toán đơn hàng #$txnRef",
            vnp_ReturnUrl = "app://return"
        )

        // Tạo query string và mã hóa bằng secret key
        val queryString = vnPayParams.toQueryString()
        val secureHash = VNPayUtils.hmacSHA512(queryString, "8NIZ0VS17CGLAY964LR1YPF80B5XZXGM")

        // Tạo URL thanh toán
        val paymentUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?$queryString&vnp_SecureHash=$secureHash"

        try {
            // Lưu đơn hàng tạm thời để xử lý khi quay lại app
            val pendingOrder = HashMap<String, Any>().apply {
                put("timestamp", System.currentTimeMillis())
                put("items", managerCart.getListCart())
                put("total", managerCart.getTotalFee() + tax + 10.0)
                put("status", "Chờ thanh toán")
                put("paymentMethod", "VNPay")
                put("vnpayRef", txnRef)
            }
            tinyDB.putObject("pending_order", pendingOrder)

            // Mở trình duyệt để thanh toán
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(paymentUrl))
            startActivity(intent)

        } catch (e: Exception) {
            Log.e("VNPay", "Lỗi khi mở cổng thanh toán", e)
            Toast.makeText(this, "Không thể mở cổng thanh toán VNPay", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleVNPayReturn(intent)
    }

    private fun handleVNPayReturn(intent: Intent?) {
        val data: Uri? = intent?.data
        if (data != null && data.scheme == "app") {
            val responseCode = data.getQueryParameter("vnp_ResponseCode")
            if (responseCode == "00") {
                // Thanh toán thành công
                saveOrderToFirebase()
                tinyDB.remove("pending_order")
                // Navigate to MainActivity after successful payment
                val mainIntent = Intent(this, MainActivity::class.java)
                mainIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(mainIntent)
                finish()
            } else {
                Toast.makeText(this, "Thanh toán thất bại hoặc bị huỷ", Toast.LENGTH_LONG).show()
            }
        }
    }
}