package com.app.moodtrack_android.messaging

import android.app.job.JobParameters
import android.app.job.JobService
import android.util.Log
import androidx.work.*
import com.app.moodtrack_android.R
import com.app.moodtrack_android.model.notificationquestionnaire.NQNode
import com.app.moodtrack_android.model.notificationquestionnaire.NQResponse
import com.app.moodtrack_android.model.notificationquestionnaire.NQResponseData
import com.app.moodtrack_android.model.notificationquestionnaire.query_objects.NotificationQuestionnaireByTimeOfDay
import com.app.moodtrack_android.model.notificationquestionnaire.question.NQQuestionChoice
import com.app.moodtrack_android.repository.NotificationQuestionnaireRepository
import com.app.moodtrack_android.tasks.SubmitQuestionnaireResponseWorker
import com.app.moodtrack_android.util.NotificationQuestionnaireUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

/**
 * Class responsible for the notification flow logic.
 */
@AndroidEntryPoint
class  NotificationLoopJobService : JobService() {

    val TAG = "NotifLoopJobService"

    // Define the coroutine scope
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    @Inject
    lateinit var gson: Gson

    @Inject
    lateinit var notificationBuilder: NotificationBuilder

    @Inject
    lateinit var notificationBuilderHelper: NotificationBuilderHelper

    @Inject
    lateinit var notificationQuestionnaireRepository: NotificationQuestionnaireRepository

    override fun onStartJob(params: JobParameters?): Boolean {
        serviceScope.launch(Dispatchers.IO) {
            try {
                // String/JSON representations of params
                val sMessageId =
                    params?.extras?.getString(getString(R.string.notification_message_id))
                val sChoiceIconId = params?.extras?.getString(getString(R.string.choice_icon_id))
                val sChoiceIconMd5 = params?.extras?.getString(getString(R.string.choice_icon_md5))
                val sChoiceIcon = params?.extras?.getString(getString(R.string.choice_icon))
                val sChoiceValue = params?.extras?.getString(getString(R.string.choice_value))
                val sChoiceType = params?.extras?.getString(getString(R.string.choice_type))
                val bIsDryRun = params?.extras?.getBoolean(getString(R.string.is_dry_run))
                val sNotificationNode =
                    params?.extras?.getString(getString(R.string.notification_node))
                val sNotificationQuestionnaire =
                    params?.extras?.getString(getString(R.string.notification_questionnaire)) // JSON serialized
                Log.d(TAG, "isDryRun: $bIsDryRun")

                // Would probably be a good idea to do more null checks than just these.
                if (sMessageId != null && sChoiceIcon != null && sChoiceType != null && sChoiceValue != null && sChoiceIconId != null) {
                    // Deserialize the node data.
                    val notificationNode = gson.fromJson(sNotificationNode, NQNode::class.java)
                    val notificationQuestionnaire = gson.fromJson(
                        sNotificationQuestionnaire,
                        NotificationQuestionnaireByTimeOfDay::class.java
                    )
                    // The notification node data should not be null.
                    // Fail/break the chain if null.
                    notificationNode?.let { currentNode ->
                        val choice = NQQuestionChoice(
                            choiceIcon = sChoiceIcon, // Already a string,
                            choiceValue = sChoiceValue, // Already a string,
                            choiceValueType = sChoiceType, // Already a string
                            choiceIconId = sChoiceIconId, // Already a string
                            choiceIconMd5 = sChoiceIconMd5 // Already a string
                        )

                        // Get all outgoing edges from the node.
                        val outgoingEdgesFromCurrentNode =
                            NotificationQuestionnaireUtil.getOutgoingEdgesForNode(
                                currentNode,
                                notificationQuestionnaire
                            )

                        // Check the conditions, and if any return true, find the edge.
                        val edgeToNextNode = NotificationQuestionnaireUtil.evaluateEdgeConditions(
                            receivedChoice = choice,
                            edges = outgoingEdgesFromCurrentNode
                        )

                        val responseData = NQResponseData(
                            questionText = currentNode.data.question.questionText,
                            choices = currentNode.data.question.questionChoices,
                            selectedChoice = NQQuestionChoice(
                                choiceIcon = sChoiceIcon, // Already a string,
                                choiceValueType = sChoiceType, // Already a string,
                                choiceValue = sChoiceValue, // Already a string
                                choiceIconId = sChoiceIconId, // Already a string
                                choiceIconMd5 = sChoiceIconMd5 // Already a string
                            )
                        )

                        // If edgeToNextNode is null, we're dealing with a leaf node.
                        edgeToNextNode?.let { nextNodeEdge ->
                            val nextNode = NotificationQuestionnaireUtil.getNextNode(
                                nextNodeEdge,
                                notificationQuestionnaire
                            )

                            submitNotificationQuestionnaireResponse(
                                notificationQuestionnaireId = notificationQuestionnaire.nqId,
                                nodeId = currentNode._id,
                                nextNodeId = nextNode?._id,
                                responseData = responseData,
                                messageId = sMessageId, // Already a string
                                isDryRun = bIsDryRun
                            )

                            // Should prompt next notification and finish job
                            val notificationId = notificationBuilderHelper.generateNotificationId()
                            notificationBuilderHelper.buildNotificationQuestionAndNotify(
                                mNode = nextNode,
                                questionnaire = notificationQuestionnaire,
                                notificationId = notificationId,
                                messageId = sMessageId, // Already a string
                                isDryRun = bIsDryRun
                            )
                        } ?: run {
                            Log.d(
                                TAG,
                                "No edge to next node found. Assuming leaf node and sending response with next as null."
                            )
                            submitNotificationQuestionnaireResponse(
                                notificationQuestionnaireId = notificationQuestionnaire.nqId,
                                nodeId = currentNode._id,
                                nextNodeId = null,
                                responseData = responseData,
                                messageId = sMessageId, // Already a string
                                isDryRun = bIsDryRun
                            )
                        }
                    } ?: run {
                        Log.d(
                            TAG,
                            "Service invariant voided: Received extra 'notificationNode' was null."
                        )
                    }
                } else {
                    Log.d(
                        TAG,
                        "sMessageId, sChoiceIcon, sChoiceType or sChoiceValue was null, ending job."
                    )
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

    /**
     * Submits the response received from the resulting notification
     * interaction.
     */
    private fun submitNotificationQuestionnaireResponse(
        notificationQuestionnaireId: String,
        nodeId: String,
        nextNodeId: String?,
        previousNodeId: String? = null,
        responseData: NQResponseData,
        messageId: String,
        isDryRun: Boolean?
    ) {
        if(isDryRun != null && isDryRun){
            Log.d(
                TAG,
                "Dry run registered, skipping response submission."
            )
        } else {
            val response = NQResponse(
                messageId,
                FirebaseAuth.getInstance().currentUser?.uid,
                notificationQuestionnaireId,
                nodeId,
                nextNodeId,
                previousNodeId,
                Date(),
                responseData
            )
            // Since WorkData can't take custom objects, serialize the data.
            val responseString = gson.toJson(response)

            // We use the WorkManager API here since sending the response isn't a
            // time-sensitive task which must be executed immediately.
            val workData =
                workDataOf(getString(R.string.notification_questionnaire_response) to responseString)
            val constraints = Constraints
                .Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val workRequest = OneTimeWorkRequestBuilder<SubmitQuestionnaireResponseWorker>()
                .setConstraints(constraints)
                .setInputData(workData)
                .build()
            WorkManager.getInstance(applicationContext).enqueue(workRequest)
        }
    }
}