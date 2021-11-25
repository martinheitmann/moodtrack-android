package com.app.moodtrack_android.model.notificationquestionnaire

import java.io.Serializable
import java.util.*

data class NQResponse(
    val messageId: String,
    val userId: String?,
    val notificationQuestionnaireId: String,
    val nodeId: String,
    val nextNodeId: String?,
    val previousNodeId: String? = null,
    val timestamp: Date,
    val responseData: NQResponseData
) : Serializable
