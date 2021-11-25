package com.app.moodtrack_android.model.notificationquestionnaire.question

import com.app.moodtrack_android.model.notificationquestionnaire.TimeOfDay
import java.io.Serializable

data class NQQuestion(
    val timeOfDay: TimeOfDay,
    val questionText: String,
    val questionChoices: List<NQQuestionChoice>
) : Serializable
