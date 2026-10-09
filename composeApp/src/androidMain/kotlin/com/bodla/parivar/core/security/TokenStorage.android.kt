package com.bodla.parivar.core.security

import android.content.Context
import android.content.SharedPreferences
import com.bodla.parivar.core.AndroidPlatform

actual class TokenStorage {
    private val prefs: SharedPreferences = (AndroidPlatform.appContext
        ?: throw IllegalStateException("AndroidPlatform.appContext must be initialized before creating TokenStorage"))
        .getSharedPreferences("bodla_secure_prefs", Context.MODE_PRIVATE)

    actual fun saveToken(token: String) {
        prefs.edit().putString("auth_token", token).apply()
    }

    actual fun getToken(): String? {
        return prefs.getString("auth_token", null)
    }

    actual fun saveRefreshToken(token: String) {
        prefs.edit().putString("refresh_token", token).apply()
    }

    actual fun getRefreshToken(): String? {
        return prefs.getString("refresh_token", null)
    }

    actual fun saveUserProfile(profileJson: String) {
        prefs.edit().putString("saved_profile_json", profileJson).apply()
    }

    actual fun getUserProfile(): String? {
        return prefs.getString("saved_profile_json", null)
    }

    actual fun clearToken() {
        prefs.edit()
            .remove("auth_token")
            .remove("refresh_token")
            .remove("saved_profile_json")
            .apply()
    }
}
