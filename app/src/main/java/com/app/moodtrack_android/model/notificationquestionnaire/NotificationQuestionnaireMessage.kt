package com.app.moodtrack_android.model.notificationquestionnaire

import java.io.Serializable

data class NotificationQuestionnaireMessage(
    val messageId: String,
    val nqId: String,
    val timeOfDay: TimeOfDay,
    val isDryRun: Boolean? = null
) : Serializable