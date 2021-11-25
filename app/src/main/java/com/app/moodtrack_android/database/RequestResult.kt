package com.app.moodtrack_android.database

import com.app.moodtrack_android.model.Status

data class RequestResult(
    val data: Any?,
    val status: Status,
    val hasError: Boolean,
    val errorMessage: String?,
)
