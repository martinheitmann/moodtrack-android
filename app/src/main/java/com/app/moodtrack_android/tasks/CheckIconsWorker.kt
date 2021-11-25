package com.app.moodtrack_android.tasks

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.JobIntentService
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.app.moodtrack_android.R
import com.app.moodtrack_android.messaging.MessageJobIntentService
import com.app.moodtrack_android.model.notificationquestionnaire.NotificationQuestionnaireMessage
import com.app.moodtrack_android.repository.FileRepository
import com.app.moodtrack_android.repository.NotificationQuestionnaireRepository
import com.app.moodtrack_android.util.NotificationQuestionnaireUtil
import com.app.moodtrack_android.util.QuestionIconUtil
import com.google.gson.Gson
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class CheckIconsWorker @AssistedInject constructor(
    @Assisted val context: Context,
    @Assisted val workerParams: WorkerParameters,
    private val fileRepository: FileRepository,
    private val notificationQuestionnaireRepository: NotificationQuestionnaireRepository,
    private val gson: Gson,
) : CoroutineWorker(context, workerParams) {
    val TAG = "CheckIconsWorker"
    override suspend fun doWork(): Result {
        try {
            // Worker can't receive custom objects, hence serializing/deserializing is necessary
            val json = inputData.getString("json")

            val notificationQuestionnaireMessage =
                gson.fromJson(json, NotificationQuestionnaireMessage::class.java)
            val messageId = notificationQuestionnaireMessage.messageId

            Log.d(TAG, json ?: "Invalid json string passed to worker.")
            notificationQuestionnaireRepository.getNotificationQuestionnaireByTimeOfDay(
                notificationQuestionnaireMessage.nqId,
                notificationQuestionnaireMessage.timeOfDay
            )?.let { notificationQuestionnaire ->
                val initialNode =
                    NotificationQuestionnaireUtil.getInitialQuestion(notificationQuestionnaire)
                val notificationQuestionnaireId = notificationQuestionnaire.nqId

                //val allIcons = QuestionIconUtil.getAllIconNames(notificationQuestionnaire.nodes)
                val allChoices = QuestionIconUtil.getAllIcons(notificationQuestionnaire.nodes)
                Log.d(TAG, "Question set contained ${allChoices.size} icons")
                val filenameIdPairs = allChoices.map {choice -> Pair(choice.choiceIcon, choice.choiceIconId) }
                val iconsMissing = fileRepository.multipleExists(filenameIdPairs)
                Log.d(
                    TAG,
                    "Device was missing ${iconsMissing.size} icons: ${iconsMissing.joinToString()}"
                )
                if (iconsMissing.isNotEmpty()) {
                    val fileIds = filenameIdPairs.map { pair -> pair.second }
                    val files = fileRepository.fetchFiles(fileIds)
                    Log.d(TAG, "Fetched ${files.size} icons from remote source")
                    fileRepository.storeMultipleFiles(files)
                }
                val intent = Intent()
                intent.putExtra(
                    applicationContext.getString(R.string.notification_message_id),
                    messageId
                )
                intent.putExtra(
                    applicationContext.getString(R.string.notification_node),
                    initialNode
                )
                intent.putExtra(
                    applicationContext.getString(R.string.notification_questionnaire),
                    notificationQuestionnaire
                )
                intent.putExtra(
                    applicationContext.getString(R.string.notification_questionnaire_id),
                    notificationQuestionnaireId
                )
                JobIntentService.enqueueWork(
                    applicationContext,
                    MessageJobIntentService::class.java,
                    1,
                    intent
                )
                return Result.success()
            }
            return Result.failure()
        } catch (exception: Throwable) {
            Log.d(TAG, exception.stackTraceToString())
            return Result.failure()
        }
    }
}