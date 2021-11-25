package com.app.moodtrack_android.model.notificationquestionnaire.question

import java.io.Serializable

data class NQQuestionChoice(
    var choiceIconMd5: String? = null,
    val choiceIconId: String,
    val choiceIcon: String,
    val choiceValueType: String,
    val choiceValue: String,
) : Serializable