package com.example.app.Activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app.Adapter.CartAdapter
import com.example.app.Network.RetrofitClient
import com.example.app.databinding.ActivityCartBinding
import com.example.app.Helper.ManagmentCart
import com.example.app.Helper.ChangeNumberItemsListener
import com.example.app.Helper.TinyDB
import com.example.app.Model.AddressResponse
import com.example.app.Model.CartResponse
import com.example.app.Model.UserAddressModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.*

class CartActivity : BaseActivity() {

    private lateinit var binding: ActivityCartBinding
    private lateinit var managerCart: ManagmentCart
    private var tax: Double = 0.0
    private var delivery: Double = 0.0
    private var selectedPaymentMethod = 1
    private var selectedAddress: UserAddressModel? = null

    // Register for activity result to handle address selection
    private val addressResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val address = result.data?.getSerializableExtra("selectedAddress") as? UserAddressModel
            address?.let {
                selectedAddress = it
                updateAddressUI(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        managerCart = ManagmentCart(this)

        loadCartFromServer()
        setVariable()
        calculatorCart()
        loadDefaultAddress()
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

        if (list.isEmpty()) {
            showEmptyCart()
        }
    }

    private fun setVariable() {
        binding.backBtn.setOnClickListener { finish() }

        // Handle address selection button click
        binding.selectAddressBtn.setOnClickListener {
            // Launch AddressListActivity
            val intent = Intent(this, AddressListActivity::class.java)
            addressResultLauncher.launch(intent)
        }

        binding.button.setOnClickListener {
            if (managerCart.getLocalCart().isEmpty()) {
                Toast.makeText(this, "Giỏ hàng đang trống!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if address is selected
            if (selectedAddress == null) {
                Toast.makeText(this, "Vui lòng chọn địa chỉ giao hàng", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, CheckoutActivity::class.java).apply {
                putExtra("selectedAddress", selectedAddress)
                putExtra("deliveryFee", delivery)
                putExtra("tax", tax)
                putExtra("paymentMethod", selectedPaymentMethod)
            }
            startActivity(intent)
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

    private fun updateAddressUI(address: UserAddressModel) {
        val addressText = StringBuilder()

        // Add receiver name if available
        address.receiverName?.let {
            addressText.append(it)
        }

        // Add phone if available
        address.phone?.let {
            if (addressText.isNotEmpty()) {
                addressText.append(" • ")
            }
            addressText.append(it)
        }

        // Add new line
        if (addressText.isNotEmpty()) {
            addressText.append("\n")
        }

        // Add address detail
        addressText.append(address.addressDetail)

        binding.addressText.text = addressText.toString()
        binding.addressText.setTextColor(resources.getColor(com.example.app.R.color.black))
    }

    private fun loadDefaultAddress() {
        RetrofitClient.addressApi().getAddresses()
            .enqueue(object : Callback<AddressResponse> {
                override fun onResponse(
                    call: Call<AddressResponse>,
                    response: Response<AddressResponse>
                ) {
                    if (response.isSuccessful) {
                        val addressResponse = response.body()
                        if (addressResponse != null && addressResponse.success && addressResponse.data.isNotEmpty()) {
                            // Find default address or use first one
                            val defaultAddress = addressResponse.data.find { it.isDefaultBoolean } ?: addressResponse.data.first()
                            selectedAddress = defaultAddress
                            updateAddressUI(defaultAddress)
                        } else {
                            binding.addressText.text = "Chưa chọn địa chỉ"
                            binding.addressText.setTextColor(resources.getColor(com.example.app.R.color.grey))
                        }
                    } else {
                        // Handle error
                        when (response.code()) {
                            401 -> {
                                binding.addressText.text = "Vui lòng đăng nhập"
                            }
                            else -> {
                                binding.addressText.text = "Không thể tải địa chỉ"
                            }
                        }
                        binding.addressText.setTextColor(resources.getColor(com.example.app.R.color.grey))
                    }
                }

                override fun onFailure(call: Call<AddressResponse>, t: Throwable) {
                    binding.addressText.text = "Lỗi kết nối"
                    binding.addressText.setTextColor(resources.getColor(com.example.app.R.color.grey))
                }
            })
    }

    private fun highlightMethod() {
        if (selectedPaymentMethod == 1) {
            binding.metod1.setBackgroundResource(com.example.app.R.drawable.green_bg_selected)
            binding.method2.setBackgroundResource(com.example.app.R.drawable.grey_bg_selected)

            // Update text colors
            binding.methodtitle1.setTextColor(resources.getColor(com.example.app.R.color.green))
            binding.methodSubTitle1.setTextColor(resources.getColor(com.example.app.R.color.green))
            binding.methodtitle2.setTextColor(resources.getColor(com.example.app.R.color.black))
            binding.methodSubTitle2.setTextColor(resources.getColor(com.example.app.R.color.darkGrey))
        } else {
            binding.method2.setBackgroundResource(com.example.app.R.drawable.green_bg_selected)
            binding.metod1.setBackgroundResource(com.example.app.R.drawable.grey_bg_selected)

            // Update text colors
            binding.methodtitle2.setTextColor(resources.getColor(com.example.app.R.color.green))
            binding.methodSubTitle2.setTextColor(resources.getColor(com.example.app.R.color.green))
            binding.methodtitle1.setTextColor(resources.getColor(com.example.app.R.color.black))
            binding.methodSubTitle1.setTextColor(resources.getColor(com.example.app.R.color.darkGrey))
        }
    }

    private fun calculatorCart() {
        val percentTax = 0.02
        val totalFee = managerCart.getTotalFee()

        tax = totalFee * percentTax
        val percentdelivery = 0.001
        val totalFee1 = managerCart.getTotalFee()

        delivery = totalFee1 * percentdelivery
        val total = totalFee + tax + delivery

        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))

        binding.totalFeeTxt.text = formatter.format(totalFee) + "đ"
        binding.taxTxt.text = formatter.format(tax) + "đ"
        binding.deliveryTxt.text = formatter.format(delivery) + "đ"
        binding.totalTxt.text = formatter.format(total) + "đ"

        updateCheckoutButton(total)
    }

    private fun updateCheckoutButton(total: Double) {
        if (total > 0) {
            binding.button.isEnabled = true
            binding.button.alpha = 1f
            binding.button.text = "Thanh toán"
        } else {
            binding.button.isEnabled = false
            binding.button.alpha = 0.5f
        }
    }

    private fun showEmptyCart() {
        binding.button.isEnabled = false
        binding.button.alpha = 0.5f
        binding.button.text = "Giỏ hàng trống"
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
                        initCartList()
                        calculatorCart()
                    }
                }

                override fun onFailure(call: Call<CartResponse>, t: Throwable) {
                    Toast.makeText(this@CartActivity, "Lỗi tải giỏ hàng", Toast.LENGTH_SHORT).show()
                    initCartList()
                    calculatorCart()
                }
            })
    }

    fun updateDelivery(newDelivery: Double) {
        delivery = newDelivery
        calculatorCart()
    }

    fun getCurrentDelivery(): Double {
        return delivery
    }

    fun getCurrentTax(): Double {
        return tax
    }

    fun getSelectedAddress(): UserAddressModel? {
        return selectedAddress
    }
}