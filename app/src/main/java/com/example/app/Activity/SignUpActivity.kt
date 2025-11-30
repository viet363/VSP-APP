package com.example.app.Activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.app.Network.AuthApi
import com.example.app.Network.RetrofitClient
import com.example.app.databinding.ActivitySignUpBinding
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private val api = RetrofitClient.instance.create(AuthApi::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSignUp.setOnClickListener {
            val username = binding.edtUsername.text.toString().trim()
            val email = binding.edtsignupEmail.text.toString().trim()
            val pass = binding.edtPassword.text.toString().trim()
            val confirmPass = binding.edtConfirmPassword.text.toString().trim()
            val fullname = binding.edtFullname.text.toString().trim()

            // Kiểm tra dữ liệu đầu vào
            if (username.isEmpty() || email.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass != confirmPass) {
                Toast.makeText(this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass.length < 6) {
                Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val body = hashMapOf(
                "username" to username,
                "email" to email,
                "password" to pass,
                "fullname" to if (fullname.isNotEmpty()) fullname else username
            )

            api.register(body).enqueue(object : Callback<com.example.app.Model.UserModel> {
                override fun onResponse(
                    call: Call<com.example.app.Model.UserModel>,
                    response: Response<com.example.app.Model.UserModel>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        Toast.makeText(this@SignUpActivity, "Đăng ký thành công!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@SignUpActivity, LoginActivity::class.java))
                        finish()
                    } else {
                        // Lấy thông báo lỗi từ response
                        val errorMessage = try {
                            val errorBody = response.errorBody()?.string()
                            if (errorBody != null) {
                                val jsonObject = JSONObject(errorBody)
                                jsonObject.getString("message")
                            } else {
                                "Đăng ký thất bại!"
                            }
                        } catch (e: Exception) {
                            "Đăng ký thất bại!"
                        }
                        Toast.makeText(this@SignUpActivity, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<com.example.app.Model.UserModel>, t: Throwable) {
                    Toast.makeText(this@SignUpActivity, "Lỗi kết nối server: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        binding.btnChange.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}