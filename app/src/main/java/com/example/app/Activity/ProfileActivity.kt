package com.example.app.Activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.app.Helper.TinyDB
import com.example.app.Model.UserData
import com.example.app.Model.UserResponse
import com.example.app.Network.RetrofitClient
import com.example.app.Network.UserApi
import com.example.app.databinding.ActivityProfileBinding
import com.example.app.databinding.DialogChangePasswordBinding
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var tinyDB: TinyDB
    private val api: UserApi by lazy { RetrofitClient.userApi() }

    private var selectedImageUri: Uri? = null

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                selectedImageUri = it
                binding.profileImage.setImageURI(it)
            }
        }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) selectedImageUri?.let {
                binding.profileImage.setImageURI(it)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tinyDB = TinyDB(this)

        if (tinyDB.getString("token", "").isEmpty()) {
            navigateToLogin()
            return
        }

        setupListeners()
        loadProfile()
    }

    private fun navigateToLogin() {
        Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(this)
        }
        finish()
    }

    private fun setupListeners() {
        binding.backBtn.setOnClickListener { onBackPressed() }
        binding.profileImage.setOnClickListener { showImagePickerDialog() }
        binding.saveBtn.setOnClickListener { updateProfile() }
        binding.logoutBtn.setOnClickListener { showLogoutConfirmation() }
        binding.changePasswordBtn.setOnClickListener { showChangePasswordDialog() }
    }

    private fun loadProfile() {
        showLoading()

        api.getUserProfile().enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, resp: Response<UserResponse>) {
                hideLoading()

                if (resp.isSuccessful && resp.body() != null) {
                    displayUserData(resp.body()!!.user)
                } else {
                    Toast.makeText(this@ProfileActivity, "Không tải được thông tin", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                hideLoading()
                Toast.makeText(this@ProfileActivity, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayUserData(user: UserData) {
        binding.nameEditTxt.setText(user.fullname ?: "")
        binding.emailEditTxt.setText(user.email ?: "")
        binding.phoneEditTxt.setText(user.phone ?: "")

        binding.usernameTxt.text = user.username ?: "Chưa có username"
        binding.currentEmailTxt.text = user.email ?: "Chưa có email"

        if (!user.avatar.isNullOrEmpty()) {
            Glide.with(this).load(user.avatar).into(binding.profileImage)
        }
    }
    private fun updateProfile() {
        val fullname = binding.nameEditTxt.text.toString().trim()
        val email = binding.emailEditTxt.text.toString().trim()
        val phone = binding.phoneEditTxt.text.toString().trim()

        if (fullname.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedImageUri != null) {
            uploadAvatarAndUpdate(fullname, email, phone)
        } else {
            updateUserInfo(fullname, email, phone)
        }
    }

    private fun updateUserInfo(fullname: String, email: String, phone: String) {
        showLoading()

        val body = hashMapOf(
            "fullname" to fullname,
            "email" to email,
            "phone" to phone
        )

        api.updateUser(body).enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, resp: Response<UserResponse>) {
                hideLoading()

                if (resp.isSuccessful && resp.body() != null) {
                    Toast.makeText(this@ProfileActivity, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                    displayUserData(resp.body()!!.user)
                } else {
                    Toast.makeText(this@ProfileActivity, "Cập nhật thất bại: ${resp.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                hideLoading()
                Toast.makeText(this@ProfileActivity, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun uploadAvatarAndUpdate(fullname: String, email: String, phone: String) {
        selectedImageUri?.let { uri ->
            lifecycleScope.launch {
                try {
                    showLoading()

                    val file = File(cacheDir, "avatar_${System.currentTimeMillis()}.jpg")
                    contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(file).use { output -> input.copyTo(output) }
                    }

                    val fileBody = file.asRequestBody("image/*".toMediaType())
                    val avatarPart = MultipartBody.Part.createFormData("avatar", file.name, fileBody)

                    val fullnameBody = fullname.toRequestBody()
                    val emailBody = email.toRequestBody()
                    val phoneBody = phone.toRequestBody()

                    // Sửa lại: gửi avatarPart có thể null
                    api.updateUserWithAvatar(fullnameBody, emailBody, phoneBody, avatarPart)
                        .enqueue(object : Callback<UserResponse> {
                            override fun onResponse(call: Call<UserResponse>, resp: Response<UserResponse>) {
                                hideLoading()

                                if (resp.isSuccessful && resp.body() != null) {
                                    Toast.makeText(this@ProfileActivity, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                                    displayUserData(resp.body()!!.user)
                                } else {
                                    Toast.makeText(this@ProfileActivity, "Cập nhật thất bại", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                                hideLoading()
                                Toast.makeText(this@ProfileActivity, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                            }
                        })

                } catch (e: Exception) {
                    hideLoading()
                    Toast.makeText(this@ProfileActivity, "Lỗi xử lý ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun String.toRequestBody(): RequestBody =
        RequestBody.create("text/plain".toMediaType(), this)

    private fun showChangePasswordDialog() {
        val bindingDialog = DialogChangePasswordBinding.inflate(LayoutInflater.from(this))

        val dialog = AlertDialog.Builder(this)
            .setTitle("Đổi mật khẩu")
            .setView(bindingDialog.root)
            .setPositiveButton("Đổi mật khẩu", null)
            .setNegativeButton("Hủy", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val current = bindingDialog.edtCurrentPassword.text.toString().trim()
                val new = bindingDialog.edtNewPassword.text.toString().trim()
                val confirm = bindingDialog.edtConfirmPassword.text.toString().trim()

                if (current.isEmpty() || new.isEmpty() || new != confirm) {
                    Toast.makeText(this, "Vui lòng kiểm tra lại mật khẩu", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                changePassword(current, new)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun changePassword(current: String, new: String) {
        showLoading()

        val body = hashMapOf(
            "currentPassword" to current,
            "newPassword" to new
        )

        api.changePassword(body).enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, resp: Response<UserResponse>) {
                hideLoading()
                if (resp.isSuccessful) {
                    Toast.makeText(this@ProfileActivity, "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ProfileActivity, "Đổi mật khẩu thất bại", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                hideLoading()
                Toast.makeText(this@ProfileActivity, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showImagePickerDialog() {
        AlertDialog.Builder(this)
            .setTitle("Chọn ảnh đại diện")
            .setItems(arrayOf("Chụp ảnh", "Chọn từ thư viện", "Hủy")) { _, which ->
                when (which) {
                    0 -> takePhoto()
                    1 -> selectFromGallery()
                }
            }
            .show()
    }

    private fun takePhoto() {
        val temp = File.createTempFile("camera_temp", ".jpg", cacheDir)
        selectedImageUri = Uri.fromFile(temp)
        cameraLauncher.launch(selectedImageUri!!)
    }

    private fun selectFromGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Đăng xuất")
            .setMessage("Bạn có chắc chắn muốn đăng xuất?")
            .setPositiveButton("Đăng xuất") { _, _ ->
                tinyDB.clear()
                navigateToLogin()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showLoading() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.saveBtn.isEnabled = false
    }

    private fun hideLoading() {
        binding.progressBar.visibility = android.view.View.GONE
        binding.saveBtn.isEnabled = true
    }
}