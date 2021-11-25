package com.app.moodtrack_android.model.notificationquestionnaire.inappquestionnaire

import java.io.Serializable

data class NQAppQuestionnaire(
    val qid: String,
    val customTitle: String?,
    val customBody: String?,
) : Serializable
