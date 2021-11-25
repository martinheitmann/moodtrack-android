package com.app.moodtrack_android.repository

import android.util.Log
import com.app.moodtrack_android.graphql.GraphQLQuestionnaireResponseDao
import com.app.moodtrack_android.model.questionnaireresponse.QuestionnaireResponse
import javax.inject.Inject

/**
 * Repository class responsible for questionnaire response
 * related operations.
 */
class QuestionnaireResponseRepository @Inject constructor(
    private val questionnaireResponseDao: GraphQLQuestionnaireResponseDao
) {
    val TAG = "QuestRepRepo"

    /**
     * Submits the questionnaire response. Returns true if the operations succeeds,
     * false if exception or failure.
     */
    suspend fun submitQuestionnaireResponse(
        questionnaireResponse: QuestionnaireResponse,
        messageId: String?
    ): Boolean {
        return try {
            questionnaireResponseDao.submitQuestionnaireResponse(questionnaireResponse, messageId)
        } catch (e: Throwable) {
            Log.d(TAG, e.stackTraceToString())
            false
        }
    }
}