package com.app.moodtrack_android.model.questionnaireresponse


data class QuestionnaireResponseFreeTextItem(
    override val index: Int,
    val question: String,
    val response: String,
) : QuestionnaireResponseElement
