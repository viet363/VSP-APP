package com.example.app.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
    private val api by lazy { RetrofitClient.authApi(this) }

    // Biến để kiểm tra activity có đang chạy không
    private var isActivityRunning = true

    // Sử dụng constant cho tag (cách này tốt hơn)
    private val TAG = "SIGN_UP_ACTIVITY"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Log khi activity khởi tạo
        Log.d(TAG, "SignUpActivity created")

        binding.btnSignUp.setOnClickListener {
            Log.d(TAG, "Sign up button clicked")

            val username = binding.edtUsername.text.toString().trim()
            val email = binding.edtsignupEmail.text.toString().trim()
            val pass = binding.edtPassword.text.toString().trim()
            val confirmPass = binding.edtConfirmPassword.text.toString().trim()
            val fullname = binding.edtFullname.text.toString().trim()

            // Kiểm tra dữ liệu đầu vào
            if (username.isEmpty() || email.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
                Log.w(TAG, "Validation failed: Empty fields")
                showToast("Vui lòng nhập đầy đủ thông tin")
                return@setOnClickListener
            }

            if (pass != confirmPass) {
                Log.w(TAG, "Validation failed: Password mismatch")
                showToast("Mật khẩu xác nhận không khớp")
                return@setOnClickListener
            }

            if (pass.length < 6) {
                Log.w(TAG, "Validation failed: Password too short")
                showToast("Mật khẩu phải có ít nhất 6 ký tự")
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Log.w(TAG, "Validation failed: Invalid email")
                showToast("Email không hợp lệ")
                return@setOnClickListener
            }

            val body = hashMapOf(
                "username" to username,
                "email" to email,
                "password" to pass,
                "fullname" to if (fullname.isNotEmpty()) fullname else username
            )

            // Log request
            Log.d(TAG, "=== SENDING REGISTER REQUEST ===")
            Log.d(TAG, "Request body: $body")
            Log.d(TAG, "API endpoint: register")

            // Ẩn bàn phím trước khi gửi request
            hideKeyboard()

            api.register(body).enqueue(object : Callback<com.example.app.Model.UserModel> {
                override fun onResponse(
                    call: Call<com.example.app.Model.UserModel>,
                    response: Response<com.example.app.Model.UserModel>
                ) {
                    // Log response
                    Log.d(TAG, "=== RECEIVED RESPONSE ===")
                    Log.d(TAG, "Response code: ${response.code()}")
                    Log.d(TAG, "Response isSuccessful: ${response.isSuccessful}")
                    Log.d(TAG, "Response has body: ${response.body() != null}")

                    if (response.isSuccessful && response.body() != null) {
                        Log.i(TAG, "✅ ĐĂNG KÝ THÀNH CÔNG")
                        Log.i(TAG, "User data: ${response.body()}")
                        showToast("Đăng ký thành công!")

                        // Chuyển sang LoginActivity sau khi đăng ký thành công
                        runOnUiThread {
                            if (isActivityRunning) {
                                Log.d(TAG, "Navigating to LoginActivity")
                                startActivity(Intent(this@SignUpActivity, LoginActivity::class.java))
                                finish()
                            }
                        }
                    } else {
                        Log.e(TAG, "❌ ĐĂNG KÝ THẤT BẠI")
                        Log.e(TAG, "Error code: ${response.code()}")

                        // Lấy thông báo lỗi từ response
                        val errorMessage = try {
                            val errorBody = response.errorBody()?.string()
                            Log.e(TAG, "Raw error body: $errorBody")

                            if (errorBody != null && errorBody.isNotEmpty()) {
                                try {
                                    val jsonObject = JSONObject(errorBody)
                                    val message = if (jsonObject.has("message")) {
                                        jsonObject.getString("message")
                                    } else if (jsonObject.has("error")) {
                                        jsonObject.getString("error")
                                    } else {
                                        "Đăng ký thất bại! (Mã lỗi: ${response.code()})"
                                    }
                                    Log.e(TAG, "Parsed error message: $message")
                                    message
                                } catch (e: Exception) {
                                    Log.e(TAG, "JSON parse error: ${e.message}")
                                    "Đăng ký thất bại! (Mã lỗi: ${response.code()})"
                                }
                            } else {
                                Log.e(TAG, "Empty error body")
                                "Đăng ký thất bại! (Mã lỗi: ${response.code()})"
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error reading error body: ${e.message}")
                            "Đăng ký thất bại! (Mã lỗi: ${response.code()})"
                        }

                        showToast(errorMessage)
                    }
                }

                override fun onFailure(call: Call<com.example.app.Model.UserModel>, t: Throwable) {
                    Log.e(TAG, "=== NETWORK FAILURE ===")
                    Log.e(TAG, "❌ LỖI KẾT NỐI SERVER")
                    Log.e(TAG, "Exception type: ${t.javaClass.name}")
                    Log.e(TAG, "Exception message: ${t.message}")

                    // Log full stack trace
                    Log.e(TAG, "Stack trace:", t)

                    var errorType = "Lỗi không xác định"
                    var errorDetail = t.message ?: "Không có thông tin chi tiết"

                    when (t) {
                        is SocketTimeoutException -> {
                            errorType = "Timeout kết nối"
                            Log.e(TAG, "⚠️ Timeout - Server không phản hồi kịp thời")
                        }
                        is ConnectException -> {
                            errorType = "Không thể kết nối đến server"
                            Log.e(TAG, "⚠️ ConnectException - Server có thể không chạy")
                        }
                        is UnknownHostException -> {
                            errorType = "Không tìm thấy server"
                            Log.e(TAG, "⚠️ UnknownHostException - URL có thể sai")
                        }
                        is SSLHandshakeException -> {
                            errorType = "Lỗi bảo mật kết nối"
                            Log.e(TAG, "⚠️ SSLHandshakeException - Vấn đề certificate")
                        }
                        else -> {
                            Log.e(TAG, "⚠️ Unknown exception type")
                        }
                    }

                    // Log thông tin request
                    try {
                        Log.e(TAG, "Request URL: ${call.request().url}")
                        Log.e(TAG, "Request method: ${call.request().method}")
                        Log.e(TAG, "Request headers: ${call.request().headers}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Cannot get request info: ${e.message}")
                    }

                    // Hiển thị thông báo cho người dùng
                    val userMessage = "$errorType: $errorDetail"
                    Log.e(TAG, "User message: $userMessage")
                    showToast(userMessage)
                }
            })
        }

        binding.btnChange.setOnClickListener {
            Log.d(TAG, "Change to login button clicked")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isActivityRunning = false
        Log.d(TAG, "SignUpActivity destroyed")
    }

    private fun showToast(message: String) {
        runOnUiThread {
            if (isActivityRunning && !isFinishing && !isDestroyed) {
                try {
                    Log.d(TAG, "Showing toast: $message")
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e(TAG, "Toast error: ${e.message}")
                }
            }
        }
    }

    private fun hideKeyboard() {
        try {
            val view = currentFocus
            view?.let {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(it.windowToken, 0)
                Log.d(TAG, "Keyboard hidden")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Hide keyboard error: ${e.message}")
        }
    }
}