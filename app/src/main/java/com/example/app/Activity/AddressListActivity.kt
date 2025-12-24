package com.example.app.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app.Adapter.AddressAdapter
import com.example.app.Model.AddressResponse
import com.example.app.Model.UserAddressModel
import com.example.app.Network.RetrofitClient
import com.example.app.databinding.ActivityAddressListBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AddressListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddressListBinding
    private lateinit var adapter: AddressAdapter
    private var addresses = mutableListOf<UserAddressModel>()

    companion object {
        private const val TAG = "AddressListActivity"
    }

    // Register for result từ AddEditAddressActivity
    private val addEditAddressLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Reload danh sách địa chỉ
            loadAddresses()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddressListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupViews()
        setupRecyclerView()
        loadAddresses()
    }

    private fun setupToolbar() {
        binding.toolbar.title = "Địa chỉ giao hàng"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupViews() {
        binding.addButton.setOnClickListener {
            val intent = Intent(this, AddEditAddressActivity::class.java)
            addEditAddressLauncher.launch(intent)
        }
    }

    private fun setupRecyclerView() {
        adapter = AddressAdapter(addresses,
            onAddressSelected = { address ->
                // Return selected address to CartActivity
                val resultIntent = Intent().apply {
                    putExtra("selectedAddress", address)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            },
            onEditAddress = { address ->
                // Mở màn hình sửa địa chỉ
                val intent = Intent(this, AddEditAddressActivity::class.java).apply {
                    putExtra("addressId", address.id)
                    putExtra("address", address)
                }
                addEditAddressLauncher.launch(intent)
            },
            onDeleteAddress = { address ->
                showDeleteConfirmation(address)
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun loadAddresses() {
        showLoading(true)
        Log.d(TAG, "Loading addresses...")

        RetrofitClient.addressApi().getAddresses()
            .enqueue(object : Callback<AddressResponse> {
                override fun onResponse(
                    call: Call<AddressResponse>,
                    response: Response<AddressResponse>
                ) {
                    showLoading(false)
                    Log.d(TAG, "Response received. Success: ${response.isSuccessful}, Code: ${response.code()}")

                    if (response.isSuccessful) {
                        val addressResponse = response.body()
                        Log.d(TAG, "Response body not null: ${addressResponse != null}")

                        if (addressResponse != null && addressResponse.success) {
                            Log.d(TAG, "Data count: ${addressResponse.data.size}")

                            addresses.clear()
                            addresses.addAll(addressResponse.data)

                            // Log từng item để debug
                            addresses.forEachIndexed { index, address ->
                                Log.d(TAG, "Address $index: id=${address.id}, name=${address.receiverName}, phone=${address.phone}, address=${address.addressDetail}, isDefault=${address.isDefault}")
                            }

                            adapter.notifyDataSetChanged()

                            Log.d(TAG, "Adapter item count after update: ${adapter.itemCount}")

                            if (addresses.isEmpty()) {
                                Log.d(TAG, "Showing empty state")
                                showEmptyState()
                            } else {
                                Log.d(TAG, "Hiding empty state")
                                hideEmptyState()
                            }
                        } else {
                            Log.d(TAG, "Response not successful or no data")
                            Toast.makeText(
                                this@AddressListActivity,
                                "Không có dữ liệu địa chỉ",
                                Toast.LENGTH_SHORT
                            ).show()
                            showEmptyState()
                        }
                    } else {
                        Log.d(TAG, "Response failed with code: ${response.code()}")
                        when (response.code()) {
                            401 -> {
                                Toast.makeText(
                                    this@AddressListActivity,
                                    "Phiên đăng nhập hết hạn",
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish()
                            }
                            else -> {
                                Toast.makeText(
                                    this@AddressListActivity,
                                    "Không thể tải danh sách địa chỉ",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        showEmptyState()
                    }
                }

                override fun onFailure(call: Call<AddressResponse>, t: Throwable) {
                    showLoading(false)
                    Log.e(TAG, "Network error: ${t.message}", t)
                    Toast.makeText(
                        this@AddressListActivity,
                        "Lỗi kết nối: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    showEmptyState()
                }
            })
    }

    private fun showDeleteConfirmation(address: UserAddressModel) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Xác nhận xóa")
            .setMessage("Bạn có chắc chắn muốn xóa địa chỉ này?")
            .setPositiveButton("Xóa") { _, _ ->
                deleteAddress(address.id)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun deleteAddress(addressId: Long?) {
        if (addressId == null) return

        RetrofitClient.addressApi().deleteAddress(addressId)
            .enqueue(object : Callback<UserAddressModel> {
                override fun onResponse(
                    call: Call<UserAddressModel>,
                    response: Response<UserAddressModel>
                ) {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@AddressListActivity,
                            "Xóa địa chỉ thành công",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadAddresses() // Reload danh sách
                    } else {
                        when (response.code()) {
                            400 -> Toast.makeText(
                                this@AddressListActivity,
                                "Không thể xóa địa chỉ mặc định",
                                Toast.LENGTH_SHORT
                            ).show()
                            else -> Toast.makeText(
                                this@AddressListActivity,
                                "Xóa địa chỉ thất bại",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                override fun onFailure(call: Call<UserAddressModel>, t: Throwable) {
                    Toast.makeText(
                        this@AddressListActivity,
                        "Lỗi kết nối: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        if (show) {
            binding.recyclerView.visibility = android.view.View.GONE
            binding.emptyState.visibility = android.view.View.GONE
            binding.addButton.visibility = android.view.View.GONE
        }
    }

    private fun showEmptyState() {
        binding.recyclerView.visibility = android.view.View.GONE
        binding.emptyState.visibility = android.view.View.VISIBLE
        binding.addButton.visibility = android.view.View.VISIBLE
    }

    private fun hideEmptyState() {
        binding.recyclerView.visibility = android.view.View.VISIBLE
        binding.emptyState.visibility = android.view.View.GONE
        binding.addButton.visibility = android.view.View.VISIBLE
    }
}