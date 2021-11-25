package com.app.moodtrack_android.model.notificationquestionnaire

import java.io.Serializable

data class NQCondition(
    val condition: String,
    val conditionValue: String,
    val conditionType: String,
) : Serializable