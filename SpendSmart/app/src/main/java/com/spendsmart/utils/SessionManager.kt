package com.spendsmart.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("spendsmart_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_DEMO_MODE = "demo_mode"
        private const val KEY_LOGGED_IN = "logged_in"
        private const val KEY_DARK_MODE = "dark_mode"
    }

    fun login(userId: Int, username: String) {
        prefs.edit()
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_USERNAME, username)
            .putBoolean(KEY_LOGGED_IN, true)
            .apply()
    }

    fun logout() {
        prefs.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_USERNAME)
            .putBoolean(KEY_LOGGED_IN, false)
            .putBoolean(KEY_DEMO_MODE, false)
            .apply()
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_LOGGED_IN, false)

    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, -1)

    fun getUsername(): String = prefs.getString(KEY_USERNAME, "") ?: ""

    fun setDemoMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEMO_MODE, enabled).apply()
    }

    fun isDemoMode(): Boolean = prefs.getBoolean(KEY_DEMO_MODE, false)

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }

    fun isDarkMode(): Boolean = prefs.getBoolean(KEY_DARK_MODE, false)
}
