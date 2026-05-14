package com.spendsmart.utils

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object FormatUtils {

    fun formatRand(amount: Double): String {
        val nf = NumberFormat.getNumberInstance(Locale("en", "ZA"))
        nf.minimumFractionDigits = 2
        nf.maximumFractionDigits = 2
        return "R ${nf.format(amount)}"
    }

    fun formatRandCompact(amount: Double): String {
        return if (amount >= 1000) {
            "R ${String.format("%.1f", amount / 1000)}k"
        } else {
            formatRand(amount)
        }
    }

    fun todayString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    fun firstDayOfMonth(): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(cal.time)
    }

    fun displayDate(raw: String): String {
        return try {
            val input = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val output = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            output.format(input.parse(raw)!!)
        } catch (e: Exception) {
            raw
        }
    }

    fun parseColor(hex: String): Int {
        return try {
            android.graphics.Color.parseColor(hex)
        } catch (e: Exception) {
            android.graphics.Color.parseColor("#111110")
        }
    }
}
