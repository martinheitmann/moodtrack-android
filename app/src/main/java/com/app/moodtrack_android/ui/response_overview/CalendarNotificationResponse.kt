package com.app.moodtrack_android.ui.response_overview

import android.graphics.Bitmap
import java.util.*

data class CalendarNotificationResponse(
    val choiceIcon: Bitmap?,
    val choiceValue: String,
    val question: String,
    val timestamp: Date,
    val showIcon: Boolean,
)
