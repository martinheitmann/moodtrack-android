package com.app.moodtrack_android.ui.response_overview

import java.time.YearMonth

data class CalendarCell(
    val yearMonth: YearMonth,
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val responses: List<CalendarNotificationResponse>
)
