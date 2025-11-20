package com.example.app.Model

import com.example.app.Utilities.VNPayUtils
import java.net.URLEncoder

data class VNPayModel(
    val vnp_Version: String = "2.1.0",
    val vnp_Command: String = "pay",
    val vnp_TmnCode: String,
    val vnp_Amount: Long,
    val vnp_BankCode: String? = "",
    val vnp_CreateDate: String = VNPayUtils.getCurrentTimestamp(),
    val vnp_CurrCode: String = "VND",
    val vnp_IpAddr: String,
    val vnp_Locale: String = "vn", // vn/en
    val vnp_OrderInfo: String,
    val vnp_OrderType: String = "other",
    val vnp_ReturnUrl: String,
    val vnp_TxnRef: String = VNPayUtils.generateRandomString(12),
    val vnp_ExpireDate: String? = null
) {
    fun toQueryString(): String {
        val params = mutableMapOf<String, String>().apply {
            put("vnp_Version", vnp_Version)
            put("vnp_Command", vnp_Command)
            put("vnp_TmnCode", vnp_TmnCode)
            put("vnp_Amount", vnp_Amount.toString())
            put("vnp_CreateDate", vnp_CreateDate)
            put("vnp_CurrCode", vnp_CurrCode)
            put("vnp_IpAddr", vnp_IpAddr)
            put("vnp_Locale", vnp_Locale)
            put("vnp_OrderInfo", vnp_OrderInfo)
            put("vnp_OrderType", vnp_OrderType)
            put("vnp_ReturnUrl", vnp_ReturnUrl)
            put("vnp_TxnRef", vnp_TxnRef)

            if (!vnp_BankCode.isNullOrEmpty()) {
                put("vnp_BankCode", vnp_BankCode)
            }
            if (!vnp_ExpireDate.isNullOrEmpty()) {
                put("vnp_ExpireDate", vnp_ExpireDate)
            }
        }

        // Sắp xếp các tham số theo thứ tự alphabet
        val sortedParams = params.toSortedMap()

        // Tạo query string
        return sortedParams.map { (key, value) ->
            "$key=${URLEncoder.encode(value, "UTF-8")}"
        }.joinToString("&")
    }
}
