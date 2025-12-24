package com.example.app.Activity

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.app.Model.UserAddressModel
import com.example.app.Network.RetrofitClient
import com.example.app.databinding.ActivityAddEditAddressBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AddEditAddressActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditAddressBinding
    private var addressId: Long? = null
    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditAddressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupViews()
        loadAddressData()
        setupListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupViews() {
        addressId = intent.getLongExtra("addressId", 0L)
        isEditMode = addressId != null && addressId != 0L

        if (isEditMode) {
            binding.toolbar.title = "Sửa địa chỉ"
            binding.deleteButton.visibility = android.view.View.VISIBLE
        } else {
            binding.toolbar.title = "Thêm địa chỉ mới"
        }

        binding.defaultCheckBox.setOnCheckedChangeListener { _, isChecked ->
            binding.defaultInfoText.visibility = if (isChecked) android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    private fun loadAddressData() {
        if (!isEditMode) return

        val address = intent.getSerializableExtra("address") as? UserAddressModel
        address?.let {
            binding.nameEditText.setText(it.receiverName ?: "")
            binding.phoneEditText.setText(it.phone ?: "")
            binding.addressEditText.setText(it.addressDetail)
            // SỬA: dùng isDefaultBoolean thay vì isDefault
            binding.defaultCheckBox.isChecked = it.isDefaultBoolean
        }
    }

    private fun setupListeners() {
        binding.saveButton.setOnClickListener {
            if (validateInput()) {
                saveAddress()
            }
        }

        binding.deleteButton.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun validateInput(): Boolean {
        var isValid = true

        // Validate tên
        val name = binding.nameEditText.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên người nhận", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validate số điện thoại
        val phone = binding.phoneEditText.text.toString().trim()
        if (phone.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số điện thoại", Toast.LENGTH_SHORT).show()
            return false
        } else if (!isValidPhone(phone)) {
            Toast.makeText(this, "Số điện thoại không hợp lệ", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validate địa chỉ
        val address = binding.addressEditText.text.toString().trim()
        if (address.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập địa chỉ chi tiết", Toast.LENGTH_SHORT).show()
            return false
        } else if (address.length < 10) {
            Toast.makeText(this, "Địa chỉ quá ngắn, vui lòng nhập chi tiết hơn", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun isValidPhone(phone: String): Boolean {
        val phoneRegex = "^(0|\\+84)(3[2-9]|5[6|8|9]|7[0|6-9]|8[1-9]|9[0-9])[0-9]{7}$".toRegex()
        return phoneRegex.matches(phone)
    }

    private fun saveAddress() {
        val name = binding.nameEditText.text.toString().trim()
        val phone = binding.phoneEditText.text.toString().trim()
        val address = binding.addressEditText.text.toString().trim()
        val isDefault = binding.defaultCheckBox.isChecked

        val requestBody = hashMapOf(
            "receiver_name" to name,
            "phone" to phone,
            "address_detail" to address,
            "is_default" to (if (isDefault) 1 else 0).toString()
        )

        if (isEditMode) {
            RetrofitClient.addressApi().updateAddress(addressId!!, requestBody)
                .enqueue(object : Callback<UserAddressModel> {
                    override fun onResponse(
                        call: Call<UserAddressModel>,
                        response: Response<UserAddressModel>
                    ) {
                        handleResponse(response, "Cập nhật địa chỉ thành công", "Cập nhật địa chỉ thất bại")
                    }

                    override fun onFailure(call: Call<UserAddressModel>, t: Throwable) {
                        Toast.makeText(
                            this@AddEditAddressActivity,
                            "Lỗi kết nối: ${t.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        } else {
            RetrofitClient.addressApi().addAddress(requestBody)
                .enqueue(object : Callback<UserAddressModel> {
                    override fun onResponse(
                        call: Call<UserAddressModel>,
                        response: Response<UserAddressModel>
                    ) {
                        handleResponse(response, "Thêm địa chỉ thành công", "Thêm địa chỉ thất bại")
                    }

                    override fun onFailure(call: Call<UserAddressModel>, t: Throwable) {
                        Toast.makeText(
                            this@AddEditAddressActivity,
                            "Lỗi kết nối: ${t.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }
    }

    private fun handleResponse(response: Response<UserAddressModel>, successMessage: String, errorMessage: String) {
        if (response.isSuccessful) {
            Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        } else {
            when (response.code()) {
                400 -> Toast.makeText(this, "Dữ liệu không hợp lệ", Toast.LENGTH_SHORT).show()
                404 -> Toast.makeText(this, "Địa chỉ không tồn tại", Toast.LENGTH_SHORT).show()
                else -> Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Xác nhận xóa")
            .setMessage("Bạn có chắc chắn muốn xóa địa chỉ này?")
            .setPositiveButton("Xóa") { _, _ ->
                deleteAddress()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun deleteAddress() {
        if (addressId == null) return

        RetrofitClient.addressApi().deleteAddress(addressId!!)
            .enqueue(object : Callback<UserAddressModel> {
                override fun onResponse(
                    call: Call<UserAddressModel>,
                    response: Response<UserAddressModel>
                ) {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@AddEditAddressActivity,
                            "Xóa địa chỉ thành công",
                            Toast.LENGTH_SHORT
                        ).show()
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        when (response.code()) {
                            400 -> Toast.makeText(
                                this@AddEditAddressActivity,
                                "Không thể xóa địa chỉ mặc định",
                                Toast.LENGTH_SHORT
                            ).show()
                            else -> Toast.makeText(
                                this@AddEditAddressActivity,
                                "Xóa địa chỉ thất bại",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                override fun onFailure(call: Call<UserAddressModel>, t: Throwable) {
                    Toast.makeText(
                        this@AddEditAddressActivity,
                        "Lỗi kết nối: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
}