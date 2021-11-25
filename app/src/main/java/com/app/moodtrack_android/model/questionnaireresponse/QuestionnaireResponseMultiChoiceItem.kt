package com.app.moodtrack_android.model.questionnaireresponse

data class QuestionnaireResponseMultiChoiceItem(
    override val index: Int,
    val question: String,
    val choices: List<QuestionnaireResponseMultiChoiceAlternative>,
    val selectedChoice: QuestionnaireResponseMultiChoiceAlternative
) : QuestionnaireResponseElement
