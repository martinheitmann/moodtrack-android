package com.app.moodtrack_android.messaging

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.core.app.JobIntentService
import androidx.navigation.NavDeepLinkBuilder
import com.app.moodtrack_android.R
import com.app.moodtrack_android.model.StoredFile
import com.app.moodtrack_android.model.notificationquestionnaire.NQNode
import com.app.moodtrack_android.model.notificationquestionnaire.query_objects.NotificationQuestionnaireByTimeOfDay
import com.app.moodtrack_android.model.notificationquestionnaire.question.NQQuestionChoice
import com.app.moodtrack_android.repository.FileRepository
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.apache.commons.io.FilenameUtils
import java.io.IOException
import java.util.*
import javax.inject.Inject

@Deprecated("Service is no longer needed and uses deprecated features.")
@AndroidEntryPoint
class MessageJobIntentService : JobIntentService() {
    val TAG = "MessageJobIntentService"

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    @Inject
    lateinit var gson: Gson

    @Inject
    lateinit var fileRepository: FileRepository

    @Inject
    lateinit var notificationBuilder: NotificationBuilder

    override fun onHandleWork(intent: Intent) {
        serviceScope.launch(Dispatchers.IO) {
            try {
                val notificationNode =
                    intent.getSerializableExtra(applicationContext.getString(R.string.notification_node)) as NQNode?
                val notificationQuestionnaire =
                    intent.getSerializableExtra(applicationContext.getString(R.string.notification_questionnaire)) as NotificationQuestionnaireByTimeOfDay?
                val messageId =
                    intent.getSerializableExtra(getString(R.string.notification_message_id)) as String

                val notificationId = generateNotificationId()
                notificationQuestionnaire?.let { questionnaire ->
                    notificationNode?.let { node ->
                        when (node.data.type) {
                            getString(R.string.in_app_questionnaire) -> {
                                val b = Bundle()
                                b.putString(getString(R.string.notification_node_id), node._id)
                                b.putString(
                                    getString(R.string.in_app_questionnaire_id),
                                    node.data.appquestionnaire.qid
                                )
                                val pi = NavDeepLinkBuilder(applicationContext)
                                    .setGraph(R.navigation.nav_graph)
                                    .setDestination(R.id.questionnaireFragment)
                                    .setArguments(b)
                                    .createPendingIntent()
                                pi.send()
                            }
                            getString(R.string.question) -> {
                                val notificationData = createNotificationViewData(
                                    node,
                                    questionnaire,
                                    notificationId,
                                    messageId
                                )
                                notificationBuilder.createQuestionNotification(
                                    text = node.data.question.questionText,
                                    buttons = notificationData,
                                    notificationId = notificationId
                                )
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.d(TAG, e.stackTraceToString())
            }
        }
    }

    private fun generateNotificationId(): Int {
        return System.currentTimeMillis().toInt()
    }

    private suspend fun createNotificationViewData(
        questionNode: NQNode,
        questionnaire: NotificationQuestionnaireByTimeOfDay,
        notificationId: Int,
        messageId: String
    ): List<Pair<Bitmap, PendingIntent>> {
        val pendingIntents = createPendingIntentsForQuestionNode(
            questionNode,
            questionnaire,
            notificationId,
            messageId
        )
        val bitmaps = getBitmapsForQuestionNode(questionNode)
        val notificationData = stitchPendingIntentsAndBitmaps(pendingIntents, bitmaps)
        return notificationData
    }

    private fun stitchPendingIntentsAndBitmaps(
        pendingIntents: List<PendingIntent>,
        bitmaps: List<Bitmap>
    ): List<Pair<Bitmap, PendingIntent>> {
        if (pendingIntents.size == bitmaps.size) {
            return pendingIntents.mapIndexed { i, pendingIntent -> Pair(bitmaps[i], pendingIntent) }
        } else throw IOException("Unable to stitch list of different sizes: ${pendingIntents.size}, ${bitmaps.size}")
    }

    private fun createPendingIntentsForQuestionNode(
        questionNode: NQNode,
        notificationQuestionnaire: NotificationQuestionnaireByTimeOfDay,
        notificationId: Int,
        messageId: String
    ): List<PendingIntent> {
        return questionNode.data.question.questionChoices.map {
            createPendingIntentForQuestion(
                it,
                questionNode,
                notificationQuestionnaire,
                notificationId,
                messageId
            )
        }
    }

    private suspend fun getBitmapsForQuestionNode(questionNode: NQNode): List<Bitmap> {
        val storedFiles = getStoredFiles(questionNode.data.question.questionChoices)
        return storedFiles.map { sf -> convertStoredFileToBitmap(sf) }
    }

    private suspend fun getStoredFiles(choices: List<NQQuestionChoice>): List<StoredFile> {
        val fileNames = choices.map { c -> c.choiceIcon }
        return fileRepository.fetchFiles(fileNames)
    }

    private fun convertStoredFileToBitmap(storedFile: StoredFile): Bitmap {
        try {
            val filename = storedFile.filename
            var bitmap: Bitmap?
            val ext = FilenameUtils.getExtension(filename) // returns file extension
            // Check since only png, jpg and bmp is supported
            if (ext == "png" || ext == "jpg") {
                val data: String = storedFile.data
                val decodedString: ByteArray = Base64.decode(data, Base64.DEFAULT)
                val bitmapFromString =
                    BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                bitmap = bitmapFromString
                return bitmap
                    ?: throw IOException("Could not convert bitmap for file with name $filename.")
            } else {
                throw IOException("Invalid file format received. Found $ext.")
            }
        } catch (e: Throwable) {
            throw e
        }
    }

    private fun createPendingIntentForQuestion(
        questionChoice: NQQuestionChoice,
        questionNode: NQNode,
        notificationQuestionnaire: NotificationQuestionnaireByTimeOfDay,
        notificationId: Int,
        messageId: String
    ): PendingIntent {
        if (questionNode.data.type == getString(R.string.question)) {
            val intent = Intent(applicationContext, MessagingBroadcastReceiver::class.java)
            val nid = notificationId
            val icon = questionChoice.choiceIcon
            val value = questionChoice.choiceValue
            val type = questionChoice.choiceValueType
            intent.putExtra(getString(R.string.notification_message_id), messageId)
            intent.putExtra(getString(R.string.choice_icon), icon)
            intent.putExtra(getString(R.string.choice_value), value)
            intent.putExtra(getString(R.string.choice_type), type)
            intent.putExtra(getString(R.string.notification_id), nid)
            intent.putExtra(getString(R.string.notification_node), questionNode)
            intent.putExtra(
                getString(R.string.notification_questionnaire),
                notificationQuestionnaire
            )
            intent.setPackage(applicationContext.packageName)
            return PendingIntent.getBroadcast(
                application.applicationContext,
                Random().nextInt(),
                intent,
                0
            )
        }
        throw IOException("Invalid NQNode passed: type must be 'question'.")
    }
}