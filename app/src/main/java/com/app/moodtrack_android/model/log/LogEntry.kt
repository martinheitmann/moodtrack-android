package com.app.moodtrack_android.model.log

import java.util.*

data class LogEntry (
    val userId: String,
    val action: String,
    val actionStatus: String,
    val timestamp: Date,
    val extras: String?,
    val description: String?
)
