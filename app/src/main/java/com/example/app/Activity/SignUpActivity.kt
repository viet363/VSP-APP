package com.example.app.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.app.Helper.TinyDB
import com.example.app.Model.UserData
import com.example.app.Model.UserResponse
import com.example.app.Network.AuthApi
import com.example.app.Network.RetrofitClient
import com.example.app.databinding.ActivitySignUpBinding
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var api: AuthApi

    private var isActivityRunning = true
    private val TAG = "SIGN_UP_ACTIVITY"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "=== SIGN UP ACTIVITY STARTED ===")

        // Khởi tạo Retrofit
        try {
            RetrofitClient.init(this)  // Thêm dòng này nếu chưa có
            api = RetrofitClient.authApi()
            Log.d(TAG, "Retrofit API initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Retrofit", e)
            showToastSafe("Không thể khởi tạo kết nối. Vui lòng thử lại.")
        }

        binding.btnSignUp.setOnClickListener {
            Log.d(TAG, "Sign up button clicked")

            val username = binding.edtUsername.text.toString().trim()
            val email = binding.edtsignupEmail.text.toString().trim()
            val pass = binding.edtPassword.text.toString().trim()
            val confirmPass = binding.edtConfirmPassword.text.toString().trim()
            val fullname = binding.edtFullname.text.toString().trim()

            if (username.isEmpty()) {
                showToastSafe("Vui lòng nhập tên đăng nhập")
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                showToastSafe("Vui lòng nhập email")
                return@setOnClickListener
            }
            if (pass.isEmpty()) {
                showToastSafe("Vui lòng nhập mật khẩu")
                return@setOnClickListener
            }
            if (confirmPass.isEmpty()) {
                showToastSafe("Vui lòng nhập xác nhận mật khẩu")
                return@setOnClickListener
            }
            if (pass != confirmPass) {
                showToastSafe("Mật khẩu xác nhận không khớp")
                return@setOnClickListener
            }
            if (pass.length < 6) {
                showToastSafe("Mật khẩu phải có ít nhất 6 ký tự")
                return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showToastSafe("Email không hợp lệ")
                return@setOnClickListener
            }

            val body = hashMapOf(
                "username" to username,
                "email" to email,
                "password" to pass,
                "fullname" to if (fullname.isNotEmpty()) fullname else username
            )

            // Ẩn bàn phím
            hideKeyboard()

            if (!this::api.isInitialized) {
                showToastSafe("Lỗi kết nối. Vui lòng thử lại.")
                return@setOnClickListener
            }

            // Loading state
            binding.btnSignUp.isEnabled = false
            binding.btnSignUp.text = "Đang xử lý..."

            Log.d(TAG, "Sending signup request for username: $username, email: $email")

            api.register(body).enqueue(object : Callback<UserResponse> {  // Sửa: UserResponse
                override fun onResponse(
                    call: Call<UserResponse>,
                    response: Response<UserResponse>
                ) {
                    binding.btnSignUp.isEnabled = true
                    binding.btnSignUp.text = "Đăng ký"

                    Log.d(TAG, "=== RESPONSE RECEIVED ===")
                    Log.d(TAG, "Request URL: ${call.request().url}")
                    Log.d(TAG, "Response code: ${response.code()}")

                    if (response.isSuccessful) {
                        val userResponse = response.body()
                        if (userResponse?.success == true && userResponse.user != null) {
                            val user = userResponse.user
                            showToastSafe("Đăng ký thành công!")

                            saveUserData(user)

                            startActivity(Intent(this@SignUpActivity, LoginActivity::class.java))
                            finish()
                        } else {
                            val errorMessage = userResponse?.let {
                                if (!it.success) "Đăng ký thất bại"
                                else "Không nhận được thông tin user"
                            } ?: "Đăng ký thất bại"
                            showToastSafe(errorMessage)
                        }
                    } else {
                        val errorMessage = try {
                            val errorBody = response.errorBody()?.string()
                            val json = JSONObject(errorBody ?: "")
                            json.optString("message", "Đăng ký thất bại (Code: ${response.code()})")
                        } catch (e: Exception) {
                            "Đăng ký thất bại (Code: ${response.code()})"
                        }
                        showToastSafe(errorMessage)
                    }
                }

                override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                    binding.btnSignUp.isEnabled = true
                    binding.btnSignUp.text = "Đăng ký"

                    var errorMessage = when (t) {
                        is SocketTimeoutException -> "Timeout kết nối. Server không phản hồi."
                        is ConnectException -> "Không thể kết nối đến server."
                        is UnknownHostException -> "Không tìm thấy server. Kiểm tra kết nối mạng."
                        is SSLHandshakeException -> "Lỗi bảo mật kết nối."
                        else -> "Lỗi không xác định: ${t.message}"
                    }

                    showToastSafe(errorMessage)
                    Log.e(TAG, "Signup failed: ${t.message}", t)

                    // Test direct connection (tùy chọn)
                    // testDirectConnection()
                }
            })
        }

        binding.btnChange.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun saveUserData(user: UserData) {
        val tinyDB = TinyDB(this)
        if (user.token != null) {
            tinyDB.putString("token", user.token!!)
        }
        tinyDB.putLong("userId", user.id)
        tinyDB.putString("username", user.username)
        tinyDB.putString("email", user.email)
        user.fullname?.let { tinyDB.putString("fullname", it) }
        user.avatar?.let { tinyDB.putString("avatar", it) }
        user.loginType?.let { tinyDB.putString("loginType", it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        isActivityRunning = false
    }

    private fun showToastSafe(message: String) {
        runOnUiThread {
            if (isActivityRunning) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun hideKeyboard() {
        try {
            val view = currentFocus
            view?.let {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(it.windowToken, 0)
            }
        } catch (_: Exception) {
        }
    }
}