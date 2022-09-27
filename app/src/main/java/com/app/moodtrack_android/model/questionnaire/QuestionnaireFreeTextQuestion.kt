package com.app.moodtrack_android.model.questionnaire

data class QuestionnaireFreeTextQuestion(
    override val index: Int,
    val question: String,
    val additionalProperties: List<QuestionProperties>
) : QuestionnaireElement
