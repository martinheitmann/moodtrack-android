package com.app.moodtrack_android.model.questionnaire

data class QuestionnaireMultiChoiceQuestion (
    override val index: Int,
    val question: String,
    val choices: List<MultiChoiceQuestionItem>,
    val additionalProperties: List<QuestionProperties>
) : QuestionnaireElement