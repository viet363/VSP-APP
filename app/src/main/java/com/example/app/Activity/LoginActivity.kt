package com.example.app.Activity

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.app.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class LoginActivity : AppCompatActivity() {
    private lateinit var mAuth: FirebaseAuth
    private lateinit var edtlogin: EditText
    private lateinit var edtpass: EditText
    private lateinit var btnlogin: Button
    private lateinit var btnChange: ImageButton

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        edtlogin = findViewById(R.id.edtemail_login)
        edtpass = findViewById(R.id.edtpassword_login)
        btnlogin = findViewById(R.id.btn_Login)
        btnChange = findViewById(R.id.btn_Change_signUp)
        mAuth = FirebaseAuth.getInstance()

        btnlogin.setOnClickListener {
            val login = edtlogin.text.toString().trim()
            val pass = edtpass.text.toString().trim()

            if (login.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập email và mật khẩu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(login).matches()) {
                Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            mAuth.signInWithEmailAndPassword(login, pass)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user: FirebaseUser? = mAuth.currentUser
                        if (user != null && user.isEmailVerified) {
                            Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            showVerificationDialog(user)
                            mAuth.signOut()
                        }
                    } else {
                        Toast.makeText(this, "Sai email hoặc mật khẩu", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        btnChange.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }
    }

    private fun showVerificationDialog(user: FirebaseUser?) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Email chưa xác thực")
            .setMessage("Tài khoản của bạn chưa được xác thực. Bạn có muốn gửi lại email xác thực không?")
            .setPositiveButton("Gửi email") { _, _ ->
                user?.sendEmailVerification()
                    ?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Đã gửi lại email xác thực. Vui lòng kiểm tra hộp thư.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "Gửi email xác thực thất bại.", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
            .setNegativeButton("Hủy", null)
            .create()

        dialog.show()
    }
}
