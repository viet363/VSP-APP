package com.example.app.Activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.app.databinding.ActivityPaymentWebviewBinding

class PaymentWebViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentWebviewBinding
    private var orderId: Long = 0

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val paymentUrl = intent.getStringExtra("PAYMENT_URL") ?: ""
        orderId = intent.getLongExtra("ORDER_ID", 0)

        if (paymentUrl.isEmpty()) {
            Toast.makeText(this, "URL thanh toán không hợp lệ", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupWebView(paymentUrl)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(paymentUrl: String) {
        binding.webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.builtInZoomControls = true
            settings.displayZoomControls = false

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)

                    if (url?.contains("payment/return") == true) {
                        handleVNPayReturn(url)
                    }
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    url: String?
                ): Boolean {
                    url?.let {
                        if (it.contains("payment/return")) {
                            handleVNPayReturn(it)
                            return true
                        }
                    }
                    return false
                }
            }

            loadUrl(paymentUrl)
        }
    }

    private fun handleVNPayReturn(url: String) {
        val isSuccess = url.contains("vnp_ResponseCode=00")

        val resultIntent = Intent().apply {
            putExtra("PAYMENT_SUCCESS", isSuccess)
            putExtra("ORDER_ID", orderId)

            if (!isSuccess) {
                val errorCode = extractErrorCodeFromUrl(url)
                putExtra("ERROR_CODE", errorCode)
                putExtra("ERROR_MESSAGE", getErrorMessage(errorCode))
            }
        }

        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun extractErrorCodeFromUrl(url: String): String {
        return try {
            val params = url.split("?").getOrNull(1)?.split("&") ?: emptyList()
            params.find { it.startsWith("vnp_ResponseCode=") }
                ?.split("=")
                ?.getOrNull(1) ?: "99"
        } catch (e: Exception) {
            "99"
        }
    }

    private fun getErrorMessage(errorCode: String): String {
        return when (errorCode) {
            "00" -> "Thanh toán thành công"
            "07" -> "Trừ tiền thành công. Giao dịch bị nghi ngờ"
            "09" -> "Thẻ/Tài khoản chưa đăng ký dịch vụ InternetBanking"
            "10" -> "Xác thực thông tin thẻ/tài khoản không đúng quá 3 lần"
            "11" -> "Đã hết hạn chờ thanh toán"
            "12" -> "Thẻ/Tài khoản bị khóa"
            "13" -> "Sai mật khẩu xác thực giao dịch (OTP)"
            "24" -> "Khách hàng hủy giao dịch"
            "51" -> "Tài khoản không đủ số dư"
            "65" -> "Tài khoản đã vượt quá hạn mức giao dịch"
            "75" -> "Ngân hàng thanh toán đang bảo trì"
            "79" -> "Sai mật khẩu thanh toán quá số lần quy định"
            else -> "Thanh toán thất bại (Mã lỗi: $errorCode)"
        }
    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            val resultIntent = Intent().apply {
                putExtra("PAYMENT_SUCCESS", false)
                putExtra("ERROR_MESSAGE", "Người dùng hủy thanh toán")
            }
            setResult(RESULT_OK, resultIntent)
            super.onBackPressed()
        }
    }
}