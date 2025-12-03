package com.example.app.Activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.app.Helper.TinyDB
import com.example.app.Model.UserData
import com.example.app.Network.AuthApi
import com.example.app.Network.RetrofitClient
import com.example.app.Model.UserModel
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
    private lateinit var tinyDB: TinyDB
    private lateinit var googleSignInClient: GoogleSignInClient
    private val api by lazy { RetrofitClient.authApi(this) }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.let {
                    handleGoogleSignIn(it.idToken!!)
                }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign in failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        tinyDB = TinyDB(this)

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("538712076460-abe9pms62q8qfobboq7eg9u75solisna.apps.googleusercontent.com")
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.btnLogin.setOnClickListener {
            val usernameOrEmail = binding.edtemailLogin.text.toString().trim()
            val pass = binding.edtpasswordLogin.text.toString().trim()

            if (usernameOrEmail.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập username/email và mật khẩu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginWithUsernameOrEmail(usernameOrEmail, pass)
        }

        binding.btnGoogleLogin.setOnClickListener {
            signInWithGoogle()
        }

        binding.btnChangeSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }
    }

    private fun loginWithUsernameOrEmail(usernameOrEmail: String, password: String) {
        val body = hashMapOf(
            "username" to usernameOrEmail,
            "password" to password
        )

        RetrofitClient.authApi(this).login(body).enqueue(object : Callback<UserModel> {
            override fun onResponse(call: Call<UserModel>, response: Response<UserModel>) {
                if (response.isSuccessful && response.body()?.success == true) {

                    val user = response.body()!!.user!!
                    saveUserData(user)
                    Toast.makeText(this@LoginActivity, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, "Sai tài khoản hoặc mật khẩu", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserModel>, t: Throwable) {
                Toast.makeText(this@LoginActivity, "Lỗi server: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun handleGoogleSignIn(idToken: String) {
        val body = hashMapOf(
            "idToken" to idToken
        )

        api.loginWithGoogle(body).enqueue(object : Callback<UserModel> {
            override fun onResponse(call: Call<UserModel>, response: Response<UserModel>) {
                if (response.isSuccessful && response.body() != null) {
                    val userModel = response.body()!!
                    // SỬA LỖI Ở ĐÂY: Lấy user từ UserModel thay vì truyền trực tiếp UserModel
                    if (userModel.success == true && userModel.user != null) {
                        saveUserData(userModel.user!!)
                        Toast.makeText(this@LoginActivity, "Đăng nhập Google thành công!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Đăng nhập Google thất bại", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorMessage = try {
                        val errorBody = response.errorBody()?.string()
                        if (errorBody != null) {
                            val jsonObject = JSONObject(errorBody)
                            jsonObject.getString("message")
                        } else {
                            "Đăng nhập Google thất bại"
                        }
                    } catch (e: Exception) {
                        "Đăng nhập Google thất bại"
                    }
                    Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserModel>, t: Throwable) {
                Toast.makeText(this@LoginActivity, "Lỗi kết nối server: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveUserData(user: UserData) {
        tinyDB.putLong("userId", user.id ?: 0)
        tinyDB.putString("username", user.username ?: "")
        tinyDB.putString("email", user.email ?: "")
        tinyDB.putString("fullname", user.fullname ?: "")
        tinyDB.putString("avatar", user.avatar ?: "")
    }
}