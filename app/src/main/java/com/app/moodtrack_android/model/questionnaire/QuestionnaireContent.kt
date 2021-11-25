package com.app.moodtrack_android.model.questionnaire

import java.time.LocalDateTime

data class QuestionnaireContent(
    val _id: String,
    val questionnaireId: String,
    val creationDate: LocalDateTime,
    val multipleChoiceItems: List<QuestionnaireMultiChoiceQuestion>,
    val freeTextItems: List<QuestionnaireFreeTextQuestion>,
)