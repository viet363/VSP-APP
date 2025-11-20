package com.example.app.Utilities

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object VNPayUtils {
    fun hmacSHA512(data: String, key: String): String {
        val HMAC_SHA512 = "HmacSHA512"
        val spec = SecretKeySpec(key.toByteArray(), HMAC_SHA512)
        val mac = Mac.getInstance(HMAC_SHA512)
        mac.init(spec)
        val byteArray = mac.doFinal(data.toByteArray())
        return byteArray.joinToString("") { "%02x".format(it) }
    }

    fun getCurrentTimestamp(): String {
        return SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
    }

    fun generateRandomString(length: Int = 8): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }
}