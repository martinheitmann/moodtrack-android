package com.app.moodtrack_android.model.questionnaire

data class Questionnaire (
    val _id: String,
    val name: String,
    val multipleChoiceItems: List<QuestionnaireMultiChoiceQuestion>,
    val freeTextItems: List<QuestionnaireFreeTextQuestion>,
)