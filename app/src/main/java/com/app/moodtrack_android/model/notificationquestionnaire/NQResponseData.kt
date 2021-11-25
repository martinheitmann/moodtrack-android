package com.app.moodtrack_android.model.notificationquestionnaire

import com.app.moodtrack_android.model.notificationquestionnaire.question.NQQuestionChoice

data class NQResponseData (
    val questionText: String,
    val choices: List<NQQuestionChoice>,
    val selectedChoice: NQQuestionChoice
)