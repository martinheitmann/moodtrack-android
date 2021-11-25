package com.app.moodtrack_android.repository

import android.util.Log
import com.app.moodtrack_android.graphql.GraphQLNotificationQuestionnaireResponseDao
import com.app.moodtrack_android.model.notificationquestionnaire.NQResponseData
import java.util.*
import javax.inject.Inject

class NotificationQuestionnaireResponseRepository @Inject constructor(
    val notificationQuestionnaireResponseDao: GraphQLNotificationQuestionnaireResponseDao
) {
    val TAG = "NotiQuestResRepository"

    suspend fun submitNotificationQuestionnaireResponse(
        messageId: String,
        userId: String,
        notificationQuestionnaireId: String,
        nodeId: String,
        nextNodeId: String?,
        previousNodeId: String?,
        timestamp: Date,
        responseData: NQResponseData
    ): Boolean {
        return try {
            notificationQuestionnaireResponseDao.submitNotificationQuestionnaireResponse(
                messageId,
                userId,
                notificationQuestionnaireId,
                nodeId,
                nextNodeId,
                previousNodeId,
                timestamp,
                responseData
            )
        } catch (e: Throwable){
            Log.d(TAG, e.stackTraceToString())
            false
        }
    }
}