package com.app.moodtrack_android.model.notificationquestionnaire

import java.io.Serializable

data class TimeOfDay(
    val minute: Int,
    val hour: Int
) : Serializable
