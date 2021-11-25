package com.app.moodtrack_android.repository

import android.util.Log
import com.app.moodtrack_android.graphql.GraphQLQuestionnaireDao
import com.app.moodtrack_android.model.questionnaire.QuestionnaireContent
import javax.inject.Inject

class QuestionnaireRepository @Inject constructor(
    val questionnaireDao: GraphQLQuestionnaireDao
) {
    val TAG = "QuestRepository"

    suspend fun getQuestionnaire(
        questionnaireId: String
    ): QuestionnaireContent? {
        return try {
            questionnaireDao.queryQuestionnaire(questionnaireId)
        } catch (e: Throwable) {
            Log.d(TAG, e.stackTraceToString())
            null
        }
    }
}