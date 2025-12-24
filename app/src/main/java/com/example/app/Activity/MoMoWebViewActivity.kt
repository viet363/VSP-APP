package com.example.app.Activity

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.app.databinding.ActivityPaymentWebviewBinding

class MoMoWebViewActivity : AppCompatActivity() {

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

            val resultIntent = Intent().apply {
                putExtra("PAYMENT_SUCCESS", false)
                putExtra("ERROR_MESSAGE", "URL thanh toán không hợp lệ")
            }
            setResult(RESULT_CANCELED, resultIntent)
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

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)

                    url?.let {
                        if (it.contains("resultCode") || it.contains("status") || it.contains("payment/momo/return")) {
                            handleMoMoCallback(it)
                        }
                    }
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    url: String?
                ): Boolean {
                    url?.let {
                        // Xử lý deeplink đến app MoMo
                        if (it.startsWith("momo://") || it.startsWith("https://momo.vn/")) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                                startActivity(intent)
                                return true
                            } catch (e: Exception) {
                                // Fallback: tiếp tục trong WebView
                            }
                        }

                        if (it.contains("resultCode") || it.contains("status") || it.contains("payment/momo/return")) {
                            handleMoMoCallback(it)
                            return true
                        }
                    }
                    return false
                }
            }

            loadUrl(paymentUrl)
        }
    }

    private fun handleMoMoCallback(url: String) {
        try {
            val uri = Uri.parse(url)

            // Ưu tiên các param từ backend return URL
            val resultCode = uri.getQueryParameter("resultCode")
                ?: uri.getQueryParameter("status")
                ?: "99"

            val message = uri.getQueryParameter("message") ?: ""
            val extractedOrderId = uri.getQueryParameter("orderId")?.toLongOrNull() ?: orderId

            val isSuccess = resultCode == "0" || resultCode == "9000" || resultCode == "1000"

            val resultIntent = Intent().apply {
                putExtra("PAYMENT_SUCCESS", isSuccess)
                putExtra("ORDER_ID", extractedOrderId)

                if (!isSuccess) {
                    putExtra("ERROR_CODE", resultCode)
                    putExtra("ERROR_MESSAGE", getMoMoErrorMessage(resultCode, message))
                }
            }

            setResult(RESULT_OK, resultIntent)
            finish()
        } catch (e: Exception) {
            e.printStackTrace()
            val resultIntent = Intent().apply {
                putExtra("PAYMENT_SUCCESS", false)
                putExtra("ERROR_MESSAGE", "Lỗi xử lý thanh toán MoMo")
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun getMoMoErrorMessage(errorCode: String, message: String): String {
        return when (errorCode) {
            "0", "9000", "1000" -> "Thanh toán thành công"
            "11" -> "Đã hết hạn thanh toán"
            "12" -> "Thẻ/tài khoản bị khóa"
            "13" -> "Sai mật khẩu/OTP"
            "24" -> "Khách hàng hủy giao dịch"
            "51" -> "Tài khoản không đủ số dư"
            "65" -> "Vượt quá hạn mức giao dịch"
            "75" -> "Ngân hàng bảo trì"
            "79" -> "Sai mật khẩu quá nhiều lần"
            "99" -> "Lỗi không xác định"
            else -> message.ifEmpty { "Thanh toán thất bại, vui lòng thử lại (Mã: $errorCode)" }
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