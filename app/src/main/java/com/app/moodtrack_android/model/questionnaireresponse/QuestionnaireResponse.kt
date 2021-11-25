package com.app.moodtrack_android.model.questionnaireresponse

import java.util.*

data class QuestionnaireResponse(
    val precedingNotificationQuestion: String,
    val timestamp: Date,
    val user: String,
    val questionnaire: String,
    val questionnaireContent: String,
    val multipleChoiceItems: List<QuestionnaireResponseMultiChoiceItem>,
    val freeTextItems: List<QuestionnaireResponseFreeTextItem>
)
