package com.bodla.parivar.core.security

import platform.Foundation.NSUserDefaults

actual class TokenStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun saveToken(token: String) {
        defaults.setObject(token, forKey = "auth_token")
    }

    actual fun getToken(): String? {
        return defaults.stringForKey("auth_token")
    }

    actual fun saveRefreshToken(token: String) {
        defaults.setObject(token, forKey = "refresh_token")
    }

    actual fun getRefreshToken(): String? {
        return defaults.stringForKey("refresh_token")
    }

    actual fun saveUserProfile(profileJson: String) {
        defaults.setObject(profileJson, forKey = "saved_profile_json")
    }

    actual fun getUserProfile(): String? {
        return defaults.stringForKey("saved_profile_json")
    }

    actual fun clearToken() {
        defaults.removeObjectForKey("auth_token")
        defaults.removeObjectForKey("refresh_token")
        defaults.removeObjectForKey("saved_profile_json")
    }
}
