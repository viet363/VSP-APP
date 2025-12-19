package com.example.app

import java.text.NumberFormat
import java.util.*


fun Double.formatToVND(): String {
    val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
    formatter.maximumFractionDigits = 0
    return "${formatter.format(this)} ₫"
}

fun String.formatToVND(): String {
    val price = this.toDoubleOrNull() ?: 0.0
    return price.formatToVND()
}

fun Int.formatToVND(): String {
    return this.toDouble().formatToVND()
}