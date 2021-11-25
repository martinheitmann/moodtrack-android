package com.app.moodtrack_android.messaging

import android.app.job.JobParameters
import android.app.job.JobService
import android.util.Log
import com.app.moodtrack_android.R
import com.app.moodtrack_android.misc_async.StoredIconsResolver
import com.app.moodtrack_android.model.notificationquestionnaire.NQNode
import com.app.moodtrack_android.model.notificationquestionnaire.NotificationQuestionnaireMessage
import com.app.moodtrack_android.repository.NotificationQuestionnaireRepository
import com.app.moodtrack_android.util.NotificationQuestionnaireUtil
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.lang.IllegalStateException
import javax.inject.Inject


/**
 * A bootstrapper for the notification questionnaire process.
 * This class is responsible for building and sending the very first notification
 * from the questionnaire.
 */
@AndroidEntryPoint
class InitNotificationQuestionnaireJobService : JobService() {

    val TAG = "INotifQuestJobService"

    // Define the coroutine scope
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    @Inject
    lateinit var gson: Gson

    @Inject
    lateinit var storedIconsResolver: StoredIconsResolver

    @Inject
    lateinit var notificationBuilder: NotificationBuilder

    @Inject
    lateinit var notificationBuilderHelper: NotificationBuilderHelper

    @Inject
    lateinit var notificationQuestionnaireRepository: NotificationQuestionnaireRepository

    private fun isQuestion(node: NQNode?): Boolean {
        return node?.data?.type == applicationContext.getString(R.string.question)
    }

    private fun isQuestionnaire(node: NQNode?): Boolean {
        return node?.data?.type == applicationContext.getString(R.string.in_app_questionnaire)
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        serviceScope.launch(Dispatchers.IO) {
            try {
                // Extract the json data and parse it to a questionnaire object.
                val notificationQuestionnaireJson = params?.extras?.getString("json")
                if (notificationQuestionnaireJson != null) {
                    val notificationQuestionnaireMessage =
                        gson.fromJson(
                            notificationQuestionnaireJson,
                            NotificationQuestionnaireMessage::class.java
                        )
                    val isDryRun = notificationQuestionnaireMessage.isDryRun
                    Log.d(TAG, "isDryRun: $isDryRun")
                    val messageId = notificationQuestionnaireMessage.messageId
                    // Fetch the edges and nodes for this questionnaire.
                    notificationQuestionnaireRepository
                        .getNotificationQuestionnaireByTimeOfDay(
                            notificationQuestionnaireMessage.nqId,
                            notificationQuestionnaireMessage.timeOfDay
                        )
                        ?.let { notificationQuestionnaire -> // Not sure how this could ever be null.
                            // Extract initial node and fetch icons.
                            val initialNode =
                                NotificationQuestionnaireUtil.getInitialQuestion(
                                    notificationQuestionnaire
                                )
                            val notificationId =
                                notificationBuilderHelper.generateNotificationId()
                            if (initialNode != null && isQuestion(initialNode)) {
                                storedIconsResolver.checkAllIconsExistsInStorage(
                                    notificationQuestionnaire
                                )
                                // Create the notification.
                                notificationQuestionnaire.let { questionnaire ->
                                    notificationBuilderHelper.buildNotificationQuestionAndNotify(
                                        initialNode,
                                        questionnaire,
                                        notificationId,
                                        messageId,
                                        isDryRun
                                    )
                                }
                            } else if (initialNode != null && isQuestionnaire(initialNode)) {
                                notificationBuilderHelper.buildNotificationQuestionnaireAndNotify(
                                    initialNode,
                                    notificationId,
                                    messageId,
                                    isDryRun,
                                )
                            } else {
                                throw IllegalStateException("Value of node.data.type must be either 'question' or 'appquestionnaire'.")
                            }
                        }
                }
            } catch (e: Throwable) {
                Log.d(TAG, e.stackTraceToString())
            } finally {
                jobFinished(params, false)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return false
    }
}