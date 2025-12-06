package com.example.app.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.app.Helper.TinyDB
import com.example.app.Model.DirectLoginResponse
import com.example.app.Model.NestedUserResponse
import com.example.app.Model.UserData
import com.example.app.Network.AuthApi
import com.example.app.Network.RetrofitClient
import com.example.app.R
import com.example.app.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var api: AuthApi
    private lateinit var tinyDB: TinyDB
    private lateinit var mGoogleSignInClient: GoogleSignInClient

    private val TAG = "LoginActivity"
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "LoginActivity started")

        // KHỞI TẠO RETROFIT
        try {
            RetrofitClient.init(this)
            api = RetrofitClient.authApi()
            Log.d(TAG, "Retrofit API initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Retrofit", e)
            showToast("Không thể khởi tạo kết nối")
        }

        tinyDB = TinyDB(this)

        // Kiểm tra nếu đã đăng nhập
        val token = tinyDB.getString("token", "")
        if (token.isNotEmpty()) {
            Log.d(TAG, "Token exists, redirecting to MainActivity")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(getString(R.string.default_web_client_id))
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.btnSignIn.setOnClickListener {
            handleLogin()
        }

        binding.btnSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        binding.btnGoogleSignIn.setOnClickListener {
            handleGoogleSignIn()
        }

        binding.btnForgotPassword.setOnClickListener {
            showToast("Chức năng quên mật khẩu đang phát triển")
        }
    }

    private fun handleLogin() {
        val username = binding.edtUsername.text.toString().trim()
        val password = binding.edtPassword.text.toString().trim()

        if (username.isEmpty() || password.isEmpty()) {
            showToast("Vui lòng nhập đầy đủ thông tin")
            return
        }

        val body = hashMapOf(
            "username" to username,
            "password" to password
        )

        Log.d(TAG, "Attempting login for username: $username")

        if (!this::api.isInitialized) {
            showToast("Lỗi kết nối. Vui lòng thử lại.")
            return
        }

        // Thêm loading state
        binding.btnSignIn.isEnabled = false
        binding.btnSignIn.text = "Đang đăng nhập..."

        api.login(body).enqueue(object : Callback<DirectLoginResponse> {
            override fun onResponse(
                call: Call<DirectLoginResponse>,
                response: Response<DirectLoginResponse>
            ) {
                binding.btnSignIn.isEnabled = true
                binding.btnSignIn.text = "Đăng nhập"

                Log.d(TAG, "Login response received - Code: ${response.code()}")

                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    Log.d(TAG, "Response body: $loginResponse")

                    if (loginResponse != null && loginResponse.success) {
                        // CHUYỂN ĐỔI DirectLoginResponse sang UserData
                        val user = loginResponse.toUserData()
                        Log.i(TAG, "LOGIN SUCCESSFUL - User ID: ${user.id}")

                        // DEBUG
                        Log.d(TAG, "User ID before save: ${user.id}")
                        Log.d(TAG, "User token before save: ${user.token?.take(20)}...")

                        // Lưu thông tin người dùng
                        saveUserData(user)

                        // Chuyển đến MainActivity
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    } else {
                        Log.e(TAG, "Login failed: success=false or response null")
                        showToast("Đăng nhập thất bại: Sai tên đăng nhập hoặc mật khẩu")
                    }
                } else {
                    handleLoginError(response)
                }
            }

            override fun onFailure(call: Call<DirectLoginResponse>, t: Throwable) {
                binding.btnSignIn.isEnabled = true
                binding.btnSignIn.text = "Đăng nhập"

                Log.e(TAG, "Login failed: ${t.message}", t)
                showToast("Lỗi kết nối: ${t.message}")
            }
        })
    }

    private fun handleLoginError(response: Response<DirectLoginResponse>) {
        Log.e(TAG, "LOGIN FAILED - Code: ${response.code()}")
        try {
            val errorBody = response.errorBody()?.string()
            Log.e(TAG, "Error body: $errorBody")

            val errorMessage = if (errorBody != null && errorBody.isNotEmpty()) {
                try {
                    val json = JSONObject(errorBody)
                    if (json.has("message")) {
                        json.getString("message")
                    } else if (json.has("error")) {
                        json.getString("error")
                    } else {
                        "Đăng nhập thất bại (${response.code()})"
                    }
                } catch (e: Exception) {
                    "Đăng nhập thất bại (${response.code()})"
                }
            } else {
                "Đăng nhập thất bại (${response.code()})"
            }
            showToast(errorMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading error body", e)
            showToast("Đăng nhập thất bại (${response.code()})")
        }
    }

    private fun handleGoogleSignIn() {
        if (!this::api.isInitialized) {
            showToast("Lỗi kết nối. Vui lòng thử lại.")
            return
        }

        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken

                if (idToken != null) {
                    if (!this::api.isInitialized) {
                        Log.e(TAG, "API not initialized when handling Google Sign-In")
                        showToast("Lỗi kết nối. Vui lòng thử lại.")
                        return
                    }

                    val body = hashMapOf("idToken" to idToken)
                    Log.d(TAG, "Sending Google login request with token length: ${idToken.length}")

                    api.loginGoogle(body).enqueue(object : Callback<NestedUserResponse> {
                        override fun onResponse(
                            call: Call<NestedUserResponse>,
                            response: Response<NestedUserResponse>
                        ) {
                            Log.d(TAG, "Google login response code: ${response.code()}")

                            if (response.isSuccessful) {
                                val userResponse = response.body()

                                // DEBUG CHI TIẾT
                                Log.d(TAG, "NestedUserResponse: $userResponse")
                                Log.d(TAG, "NestedUserResponse.success: ${userResponse?.success}")
                                Log.d(TAG, "NestedUserResponse.user: ${userResponse?.user}")

                                if (userResponse?.success == true && userResponse.user != null) {
                                    val user = userResponse.user

                                    // DEBUG: Kiểm tra giá trị
                                    Log.d(TAG, "Google login - User ID: ${user.id}")
                                    Log.d(TAG, "Google login - User username: ${user.username}")
                                    Log.d(TAG, "Google login - User email: ${user.email}")

                                    // Lưu thông tin user
                                    saveUserData(user)

                                    // Chuyển đến MainActivity
                                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                    startActivity(intent)
                                    finish()

                                    showToast("Đăng nhập Google thành công")
                                } else {
                                    Log.e(TAG, "Google login failed: success=false or user null")
                                    showToast("Đăng nhập Google thất bại")
                                }
                            } else {
                                val errorBody = response.errorBody()?.string()
                                Log.e(TAG, "Google login failed: ${response.code()}, error: $errorBody")
                                showToast("Lỗi đăng nhập: ${response.code()}")
                            }
                        }

                        override fun onFailure(call: Call<NestedUserResponse>, t: Throwable) {
                            Log.e(TAG, "Google login network error", t)
                            showToast("Lỗi kết nối: ${t.message}")
                        }
                    })
                } else {
                    Log.e(TAG, "Google idToken is null")
                    showToast("Không nhận được token từ Google")
                }
            } catch (e: ApiException) {
                Log.e(TAG, "Google sign in failed", e)
                showToast("Đăng nhập Google thất bại: ${e.statusCode}")
            }
        }
    }

    private fun saveUserData(user: UserData) {
        try {
            Log.d(TAG, "=== SAVE USER DATA DEBUG ===")
            Log.d(TAG, "User object: $user")
            Log.d(TAG, "User ID: ${user.id}, Type: ${user.id::class.java}")
            Log.d(TAG, "User email: ${user.email}")
            Log.d(TAG, "User token: ${user.token?.take(20)}...")

            // Lưu token
            if (user.token != null) {
                tinyDB.putString("token", user.token!!)
                Log.d(TAG, "Token saved: ${user.token!!.take(20)}...")
            } else {
                Log.e(TAG, "Token is null!")
            }

            // Lưu user ID
            tinyDB.putLong("userId", user.id)
            Log.d(TAG, "User ID saved: ${user.id}")

            // Đọc lại để xác nhận
            val savedId = tinyDB.getLong("userId", -1L)
            Log.d(TAG, "Read back user ID from SharedPreferences: $savedId")

            if (savedId == -1L || savedId == 0L) {
                Log.e(TAG, "ERROR: User ID not saved correctly!")
            }

            // Lưu thông tin profile
            tinyDB.putString("username", user.username)
            Log.d(TAG, "Username saved: ${user.username}")

            tinyDB.putString("email", user.email ?: "")
            Log.d(TAG, "Email saved: ${user.email ?: "null"}")

            if (user.fullname != null) {
                tinyDB.putString("fullname", user.fullname!!)
                Log.d(TAG, "Profile name saved: ${user.fullname!!}")
            }

            if (user.avatar != null) {
                tinyDB.putString("avatar", user.avatar!!)
                Log.d(TAG, "Avatar saved: ${user.avatar!!}")
            }

            if (user.loginType != null) {
                tinyDB.putString("loginType", user.loginType!!)
                Log.d(TAG, "Login type saved: ${user.loginType!!}")
            }

            Log.d(TAG, "=== END SAVE DEBUG ===")
            Log.d(TAG, "User data saved successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user data", e)
        }
    }

    private fun showToast(message: String) {
        runOnUiThread {
            try {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Toast error: ${e.message}")
            }
        }
    }
}