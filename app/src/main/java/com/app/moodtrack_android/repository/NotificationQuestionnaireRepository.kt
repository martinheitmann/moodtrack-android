package com.app.moodtrack_android.repository

import android.util.Log
import com.app.moodtrack_android.graphql.GraphQLNotificationQuestionnaireDao
import com.app.moodtrack_android.model.notificationquestionnaire.TimeOfDay
import com.app.moodtrack_android.model.notificationquestionnaire.query_objects.NotificationQuestionnaireByTimeOfDay
import javax.inject.Inject

class NotificationQuestionnaireRepository @Inject constructor(
    val notificationQuestionnaireDao: GraphQLNotificationQuestionnaireDao
) {
    val TAG = "NotiQuestRepository"

    suspend fun getNotificationQuestionnaireByTimeOfDay(
        notificationQuestionnaireId: String,
        timeOfDay: TimeOfDay
    ): NotificationQuestionnaireByTimeOfDay? {
        return try {
            notificationQuestionnaireDao.queryNotificationQuestionnaireByTimeOfDay(notificationQuestionnaireId, timeOfDay)
        } catch (e: Throwable){
            Log.d(TAG, e.stackTraceToString())
            null
        }
    }
}