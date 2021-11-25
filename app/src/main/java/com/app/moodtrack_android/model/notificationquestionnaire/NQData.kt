package com.app.moodtrack_android.model.notificationquestionnaire

import com.app.moodtrack_android.model.notificationquestionnaire.inappquestionnaire.NQAppQuestionnaire
import com.app.moodtrack_android.model.notificationquestionnaire.question.NQQuestion
import java.io.Serializable

data class NQData (
    val nqId: String,
    val type: String,
    val question: NQQuestion,
    val appquestionnaire: NQAppQuestionnaire,
) : Serializable