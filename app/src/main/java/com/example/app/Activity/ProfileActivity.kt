package com.example.app.Activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.app.Helper.TinyDB
import com.example.app.Network.RetrofitClient
import com.example.app.Network.UserApi
import com.example.app.databinding.ActivityProfileBinding
import com.example.app.databinding.DialogChangePasswordBinding
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import okhttp3.MediaType.Companion.toMediaType


class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var tinyDB: TinyDB
    private val api = RetrofitClient.instance.create(UserApi::class.java)
    private var selectedImageUri: Uri? = null

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            binding.profileImage.setImageURI(it)
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedImageUri?.let {
                binding.profileImage.setImageURI(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        tinyDB = TinyDB(this)

        val userId = tinyDB.getLong("userId")
        if (userId == 0L) {
            Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupClickListeners()
        loadProfile(userId)
    }

    private fun setupClickListeners() {
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        binding.profileImage.setOnClickListener {
            showImagePickerDialog()
        }

        binding.saveBtn.setOnClickListener {
            val userId = tinyDB.getLong("userId")
            if (userId != 0L) {
                updateProfile(userId)
            }
        }

        binding.logoutBtn.setOnClickListener {
            showLogoutConfirmation()
        }

        binding.changePasswordBtn.setOnClickListener {
            showChangePasswordDialog()
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Chụp ảnh", "Chọn từ thư viện", "Hủy")
        AlertDialog.Builder(this)
            .setTitle("Chọn ảnh đại diện")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> takePhotoFromCamera()
                    1 -> selectImageFromGallery()
                    // 2 là Hủy, không cần xử lý
                }
            }
            .show()
    }

    private fun takePhotoFromCamera() {
        try {
            val tempFile = File.createTempFile("temp_image", ".jpg", cacheDir)
            selectedImageUri = Uri.fromFile(tempFile)
            cameraLauncher.launch(selectedImageUri!!)
        } catch (e: Exception) {
            Toast.makeText(this, "Lỗi khi chụp ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun selectImageFromGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun loadProfile(id: Long) {
        showLoading()
        api.getUserProfile(id).enqueue(object : Callback<com.example.app.Model.UserModel> {
            override fun onResponse(
                call: Call<com.example.app.Model.UserModel>,
                response: Response<com.example.app.Model.UserModel>
            ) {
                hideLoading()
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    displayUserData(user)
                } else {
                    // Xử lý lỗi response
                    val errorMessage = when (response.code()) {
                        404 -> "Không tìm thấy thông tin người dùng"
                        401 -> "Phiên đăng nhập hết hạn"
                        else -> "Lỗi tải thông tin: ${response.code()}"
                    }
                    Toast.makeText(this@ProfileActivity, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.example.app.Model.UserModel>, t: Throwable) {
                hideLoading()
                Toast.makeText(this@ProfileActivity, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayUserData(user: com.example.app.Model.UserModel) {
        binding.nameEditTxt.setText(user.fullname ?: "")
        binding.emailEditTxt.setText(user.email ?: "")
        binding.phoneEditTxt.setText(user.phone ?: "")
        binding.currentEmailTxt.text = user.email ?: "Chưa có email"
        binding.usernameTxt.text = user.username ?: "Chưa có username"

        // Hiển thị avatar nếu có
        user.avatar?.let { avatarUrl ->
            Glide.with(this).load(avatarUrl).into(binding.profileImage)
        }
    }

    private fun updateProfile(id: Long) {
        val fullname = binding.nameEditTxt.text.toString().trim()
        val email = binding.emailEditTxt.text.toString().trim()
        val phone = binding.phoneEditTxt.text.toString().trim()

        // Validation
        if (fullname.isEmpty()) {
            binding.nameEditTxt.error = "Vui lòng nhập họ tên"
            return
        }

        if (email.isEmpty()) {
            binding.emailEditTxt.error = "Vui lòng nhập email"
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailEditTxt.error = "Email không hợp lệ"
            return
        }

        showLoading()

        if (selectedImageUri != null) {
            // Cập nhật cả ảnh và thông tin
            uploadImageAndUpdateProfile(id, fullname, email, phone)
        } else {
            // Chỉ cập nhật thông tin
            updateUserInfo(id, fullname, email, phone)
        }
    }

    private fun uploadImageAndUpdateProfile(id: Long, fullname: String, email: String, phone: String) {
        selectedImageUri?.let { uri ->
            try {
                val file = File(cacheDir, "avatar_${System.currentTimeMillis()}.jpg")
                val inputStream = contentResolver.openInputStream(uri)
                val outputStream = FileOutputStream(file)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()

                val imageMediaType = "image/*".toMediaType()
                val textMediaType = "text/plain".toMediaType()

                val requestFile = RequestBody.create(imageMediaType, file)
                val avatarPart = MultipartBody.Part.createFormData("avatar", file.name, requestFile)

                val idPart = RequestBody.create(textMediaType, id.toString())
                val fullnamePart = RequestBody.create(textMediaType, fullname)
                val emailPart = RequestBody.create(textMediaType, email)
                val phonePart = RequestBody.create(textMediaType, phone)

                api.updateUserWithAvatar(idPart, fullnamePart, emailPart, phonePart, avatarPart)
                    .enqueue(object : Callback<com.example.app.Model.UserModel> {
                        override fun onResponse(
                            call: Call<com.example.app.Model.UserModel>,
                            response: Response<com.example.app.Model.UserModel>
                        ) {
                            hideLoading()
                            if (response.isSuccessful && response.body() != null) {
                                Toast.makeText(this@ProfileActivity, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                                // Cập nhật local data
                                response.body()?.let { user ->
                                    tinyDB.putString("fullname", user.fullname ?: "")
                                    tinyDB.putString("email", user.email ?: "")
                                    tinyDB.putString("avatar", user.avatar ?: "")
                                }
                            } else {
                                val errorMessage = when (response.code()) {
                                    400 -> "Dữ liệu không hợp lệ"
                                    404 -> "Không tìm thấy người dùng"
                                    else -> "Cập nhật thất bại: ${response.code()}"
                                }
                                Toast.makeText(this@ProfileActivity, errorMessage, Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onFailure(call: Call<com.example.app.Model.UserModel>, t: Throwable) {
                            hideLoading()
                            Toast.makeText(this@ProfileActivity, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                        }
                    })

            } catch (e: Exception) {
                hideLoading()
                Toast.makeText(this, "Lỗi xử lý ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            updateUserInfo(id, fullname, email, phone)
        }
    }

    private fun updateUserInfo(id: Long, fullname: String, email: String, phone: String) {
        val body = hashMapOf<String, Any>(
            "id" to id,
            "fullname" to fullname,
            "email" to email,
            "phone" to phone
        )

        api.updateUser(body).enqueue(object : Callback<com.example.app.Model.UserModel> {
            override fun onResponse(
                call: Call<com.example.app.Model.UserModel>,
                response: Response<com.example.app.Model.UserModel>
            ) {
                hideLoading()
                if (response.isSuccessful && response.body() != null) {
                    Toast.makeText(this@ProfileActivity, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                    // Cập nhật local data
                    response.body()?.let { user ->
                        tinyDB.putString("fullname", user.fullname ?: "")
                        tinyDB.putString("email", user.email ?: "")
                    }
                } else {
                    val errorMessage = when (response.code()) {
                        400 -> "Dữ liệu không hợp lệ"
                        404 -> "Không tìm thấy người dùng"
                        409 -> "Email đã được sử dụng"
                        else -> "Cập nhật thất bại: ${response.code()}"
                    }
                    Toast.makeText(this@ProfileActivity, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.example.app.Model.UserModel>, t: Throwable) {
                hideLoading()
                Toast.makeText(this@ProfileActivity, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showChangePasswordDialog() {
        val dialogBinding = DialogChangePasswordBinding.inflate(LayoutInflater.from(this))

        val dialog = AlertDialog.Builder(this)
            .setTitle("Đổi mật khẩu")
            .setView(dialogBinding.root)
            .setPositiveButton("Đổi mật khẩu", null) // Set null first to prevent auto-dismiss
            .setNegativeButton("Hủy", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val currentPassword = dialogBinding.edtCurrentPassword.text.toString().trim()
                val newPassword = dialogBinding.edtNewPassword.text.toString().trim()
                val confirmPassword = dialogBinding.edtConfirmPassword.text.toString().trim()

                if (validatePasswordInput(currentPassword, newPassword, confirmPassword)) {
                    changePassword(currentPassword, newPassword)
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun validatePasswordInput(currentPassword: String, newPassword: String, confirmPassword: String): Boolean {
        if (currentPassword.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập mật khẩu hiện tại", Toast.LENGTH_SHORT).show()
            return false
        }

        if (newPassword.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập mật khẩu mới", Toast.LENGTH_SHORT).show()
            return false
        }

        if (newPassword.length < 6) {
            Toast.makeText(this, "Mật khẩu mới phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show()
            return false
        }

        if (newPassword != confirmPassword) {
            Toast.makeText(this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun changePassword(currentPassword: String, newPassword: String) {
        val userId = tinyDB.getLong("userId")
        if (userId == 0L) {
            Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading()

        // SỬA LỖI: Sử dụng HashMap<String, String> và convert id thành String
        val body = hashMapOf<String, String>(
            "id" to userId.toString(),
            "currentPassword" to currentPassword,
            "newPassword" to newPassword
        )

        api.changePassword(body).enqueue(object : Callback<com.example.app.Model.UserModel> {
            override fun onResponse(
                call: Call<com.example.app.Model.UserModel>,
                response: Response<com.example.app.Model.UserModel>
            ) {
                hideLoading()
                if (response.isSuccessful) {
                    Toast.makeText(this@ProfileActivity, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show()
                } else {
                    val errorMessage = when (response.code()) {
                        400 -> "Mật khẩu hiện tại không đúng"
                        404 -> "Không tìm thấy người dùng"
                        else -> "Đổi mật khẩu thất bại: ${response.code()}"
                    }
                    Toast.makeText(this@ProfileActivity, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.example.app.Model.UserModel>, t: Throwable) {
                hideLoading()
                Toast.makeText(this@ProfileActivity, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Đăng xuất")
            .setMessage("Bạn có chắc chắn muốn đăng xuất?")
            .setPositiveButton("Đăng xuất") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun performLogout() {
        tinyDB.clear()
        Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
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
