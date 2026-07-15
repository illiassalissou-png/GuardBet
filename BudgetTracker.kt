package com.guardbet.overlay

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.*

class BudgetTracker(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("guardbet_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_DAILY_LIMIT = "daily_limit"
        const val KEY_TODAY_SPENT = "today_spent"
        const val KEY_TODAY_DATE = "today_date"
        const val KEY_TOTAL_LIFETIME = "total_lifetime_spent"
        const val KEY_SESSION_LOG = "session_log"
    }

    private fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE).format(Date())

    fun setDailyLimit(amount: Double) {
        prefs.edit().putFloat(KEY_DAILY_LIMIT, amount.toFloat()).apply()
    }

    fun getDailyLimit(): Double = prefs.getFloat(KEY_DAILY_LIMIT, 0f).toDouble()

    private fun rolloverIfNeeded() {
        val storedDate = prefs.getString(KEY_TODAY_DATE, "")
        if (storedDate != todayKey()) {
            prefs.edit()
                .putString(KEY_TODAY_DATE, todayKey())
                .putFloat(KEY_TODAY_SPENT, 0f)
                .apply()
        }
    }

    fun getTodaySpent(): Double {
        rolloverIfNeeded()
        return prefs.getFloat(KEY_TODAY_SPENT, 0f).toDouble()
    }

    fun addSpent(amount: Double) {
        rolloverIfNeeded()
        val newTotal = getTodaySpent() + amount
        val lifetime = prefs.getFloat(KEY_TOTAL_LIFETIME, 0f) + amount
        prefs.edit()
            .putFloat(KEY_TODAY_SPENT, newTotal.toFloat())
            .putFloat(KEY_TOTAL_LIFETIME, lifetime)
            .apply()

        val log = prefs.getStringSet(KEY_SESSION_LOG, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        log.add("${System.currentTimeMillis()}|$amount")
        prefs.edit().putStringSet(KEY_SESSION_LOG, log).apply()
    }

    fun getLifetimeSpent(): Double = prefs.getFloat(KEY_TOTAL_LIFETIME, 0f).toDouble()

    fun isOverLimit(): Boolean {
        val limit = getDailyLimit()
        return limit > 0 && getTodaySpent() >= limit
    }

    fun remainingToday(): Double {
        val limit = getDailyLimit()
        if (limit <= 0) return Double.MAX_VALUE
        return (limit - getTodaySpent()).coerceAtLeast(0.0)
    }

    fun resetAll() {
        prefs.edit().clear().apply()
    }
}
