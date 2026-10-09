package com.bodla.parivar.core.security

expect class TokenStorage {
    fun saveToken(token: String)
    fun getToken(): String?
    fun saveRefreshToken(token: String)
    fun getRefreshToken(): String?
    fun saveUserProfile(profileJson: String)
    fun getUserProfile(): String?
    fun clearToken()
}
