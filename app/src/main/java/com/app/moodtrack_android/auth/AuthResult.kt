package com.app.moodtrack_android.auth


data class AuthResult (
    val success: Any? = null,
    val error: Any? = null
) {
    fun hasError() : Boolean {
        return this.error != null
    }
}