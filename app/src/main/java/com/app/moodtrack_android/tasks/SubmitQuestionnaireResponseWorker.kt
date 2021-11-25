package com.app.moodtrack_android.tasks

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.app.moodtrack_android.R
import com.app.moodtrack_android.model.notificationquestionnaire.NQResponse
import com.app.moodtrack_android.repository.NotificationQuestionnaireResponseRepository
import com.google.gson.Gson
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SubmitQuestionnaireResponseWorker @AssistedInject constructor(
    @Assisted val context: Context,
    @Assisted val workerParams: WorkerParameters,
    private val notificationQuestionnaireResponseRepository: NotificationQuestionnaireResponseRepository,
    private val gson: Gson,
) : CoroutineWorker(context, workerParams) {
    val TAG = "SubmitQuestResWorker"
    override suspend fun doWork(): Result {
        try {
            Log.d(TAG, "Worker started.")
            val responseString = inputData.getString(context.getString(R.string.notification_questionnaire_response))
            val response = gson.fromJson(responseString, NQResponse::class.java)
            if(response.userId != null){
                notificationQuestionnaireResponseRepository.submitNotificationQuestionnaireResponse(
                    messageId = response.messageId,
                    userId = response.userId,
                    notificationQuestionnaireId = response.notificationQuestionnaireId,
                    nodeId = response.nodeId,
                    nextNodeId = response.nextNodeId,
                    previousNodeId = null,
                    timestamp = response.timestamp,
                    responseData = response.responseData
                )
                return Result.success()
            }
            return Result.failure()
        } catch(e: Throwable){
            Log.d(TAG, e.stackTraceToString())
            return Result.failure()
        }
    }
}