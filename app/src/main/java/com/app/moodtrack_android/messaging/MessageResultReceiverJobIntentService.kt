package com.app.moodtrack_android.messaging

import android.content.Intent
import android.util.Log
import androidx.core.app.JobIntentService
import androidx.work.*
import com.app.moodtrack_android.R
import com.app.moodtrack_android.model.notificationquestionnaire.NQNode
import com.app.moodtrack_android.model.notificationquestionnaire.NQResponse
import com.app.moodtrack_android.model.notificationquestionnaire.NQResponseData
import com.app.moodtrack_android.model.notificationquestionnaire.query_objects.NotificationQuestionnaireByTimeOfDay
import com.app.moodtrack_android.model.notificationquestionnaire.question.NQQuestionChoice
import com.app.moodtrack_android.repository.AuthRepository
import com.app.moodtrack_android.tasks.SubmitQuestionnaireResponseWorker
import com.app.moodtrack_android.util.NotificationQuestionnaireUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject

@Deprecated("Service is no longer needed and uses deprecated features.")
@AndroidEntryPoint
class MessageResultReceiverJobIntentService : JobIntentService() {
    val TAG = "MRRJobIntentService"

    @Inject
    lateinit var gson: Gson

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onHandleWork(intent: Intent) {
        Log.d(TAG, "onHandleWork invoked.")
        val messageId = intent.extras?.get(getString(R.string.notification_message_id)) as String
        val choiceIcon = intent.extras?.get(getString(R.string.choice_icon)) as String
        val choiceIconId = intent.extras?.get(getString(R.string.choice_icon_id)) as String
        val choiceIconMd5 = intent.extras?.get(getString(R.string.choice_icon_md5)) as String?
        val choiceValue = intent.extras?.get(getString(R.string.choice_value)) as String
        val choiceType = intent.extras?.get(getString(R.string.choice_type)) as String
        val notificationNode = intent.extras?.get(getString(R.string.notification_node)) as NQNode?
        val notificationQuestionnaire = intent.extras?.get(
            getString(R.string.notification_questionnaire)
        ) as NotificationQuestionnaireByTimeOfDay

        notificationNode?.let { currentNode ->
            val choice = NQQuestionChoice(
                choiceIcon = choiceIcon,
                choiceValue = choiceValue,
                choiceValueType = choiceType,
                choiceIconId = choiceIconId,
                choiceIconMd5 = choiceIconMd5
            )

            val outgoingEdgesFromCurrentNode =
                NotificationQuestionnaireUtil.getOutgoingEdgesForNode(
                    currentNode,
                    notificationQuestionnaire
                )

            val edgeToNextNode = NotificationQuestionnaireUtil.evaluateEdgeConditions(
                receivedChoice = choice,
                edges = outgoingEdgesFromCurrentNode
            )

            val responseData = NQResponseData(
                questionText = currentNode.data.question.questionText,
                choices = currentNode.data.question.questionChoices,
                selectedChoice = NQQuestionChoice(
                    choiceIcon = choiceIcon,
                    choiceValueType = choiceType,
                    choiceValue = choiceValue,
                    choiceIconMd5 = choiceIconMd5,
                    choiceIconId = choiceIconId
                )
            )

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
                    messageId = messageId
                )

                val nextIntent = Intent()
                nextIntent.putExtra(
                    applicationContext.getString(R.string.notification_node),
                    nextNode
                )
                nextIntent.putExtra(
                    applicationContext.getString(R.string.notification_questionnaire),
                    notificationQuestionnaire
                )
                nextIntent.putExtra(getString(R.string.notification_message_id), messageId)
                Log.d(TAG, "Enqueuing next job.")
                enqueueWork(this, MessageJobIntentService::class.java, 1, nextIntent)
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
                    messageId = messageId
                )
            }
        } ?: Log.d(TAG, "Service invariant voided: Received extra 'notificationNode' was null.")
    }

    private fun submitNotificationQuestionnaireResponse(
        notificationQuestionnaireId: String,
        nodeId: String,
        nextNodeId: String?,
        previousNodeId: String? = null,
        responseData: NQResponseData,
        messageId: String
    ) {
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

        val responseString = gson.toJson(response)
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