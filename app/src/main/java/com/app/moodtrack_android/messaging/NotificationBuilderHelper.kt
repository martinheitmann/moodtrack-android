package com.app.moodtrack_android.messaging

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.navigation.NavDeepLinkBuilder
import com.app.moodtrack_android.R
import com.app.moodtrack_android.model.StoredFile
import com.app.moodtrack_android.model.notificationquestionnaire.NQNode
import com.app.moodtrack_android.model.notificationquestionnaire.query_objects.NotificationQuestionnaireByTimeOfDay
import com.app.moodtrack_android.model.notificationquestionnaire.question.NQQuestionChoice
import com.app.moodtrack_android.repository.FileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import org.apache.commons.io.FilenameUtils
import java.io.IOException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Helper class for building notifications. Encapsulates notification
 * building logic for reusability across services.
 */
@Singleton
class NotificationBuilderHelper @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val fileRepository: FileRepository,
    private val notificationBuilder: NotificationBuilder
) {
    val TAG = "NotifBuilderHelper"

    // Need this for semi-unique notification ids.
    fun generateNotificationId(): Int {
        return System.currentTimeMillis().toInt()
    }

    /***
     * Creates the icons and pendingintents required
     * for the notification to display and behave properly.
     */
    private suspend fun createNotificationQuestionViewData(
        questionNode: NQNode,
        questionnaire: NotificationQuestionnaireByTimeOfDay,
        notificationId: Int,
        messageId: String,
        isDryRun: Boolean?
    ): List<Pair<Bitmap, PendingIntent>> {
        // Fetch and create pendingintents and bitmaps
        // before stitching them together in corresponding pairs.
        val pendingIntents = createPendingIntentsForQuestionNode(
            questionNode,
            questionnaire,
            notificationId,
            messageId,
            isDryRun
        )
        val bitmaps = getBitmapsForQuestionNode(questionNode)
        return stitchPendingIntentsAndBitmaps(pendingIntents, bitmaps)
    }

    /***
     * Creates a list of <Bitmap, PendingIntent> pairs. Bitmaps are
     * paired with the corresponding index from the PendingIntent list.
     * Could probably be replaced with a zip() call instead of map().
     */
    private fun stitchPendingIntentsAndBitmaps(
        pendingIntents: List<PendingIntent>,
        bitmaps: List<Bitmap>
    ): List<Pair<Bitmap, PendingIntent>> {
        if (pendingIntents.size == bitmaps.size) {
            return pendingIntents.mapIndexed { i, pendingIntent -> Pair(bitmaps[i], pendingIntent) }
        } else throw IOException("Unable to stitch list of different sizes: ${pendingIntents.size}, ${bitmaps.size}")
    }

    /***
     * Creates a list of PendingIntents from the argument question node.
     */
    private fun createPendingIntentsForQuestionNode(
        questionNode: NQNode,
        notificationQuestionnaire: NotificationQuestionnaireByTimeOfDay,
        notificationId: Int,
        messageId: String,
        isDryRun: Boolean?
    ): List<PendingIntent> {
        return questionNode.data.question.questionChoices.map {
            createPendingIntentForQuestion(
                it,
                questionNode,
                notificationQuestionnaire,
                notificationId,
                messageId,
                isDryRun
            )
        }
    }

    /***
     * Fetches the icon files for each node and converts them to bitmaps.
     */
    private suspend fun getBitmapsForQuestionNode(questionNode: NQNode): List<Bitmap> {
        val storedFiles = getStoredFiles(questionNode.data.question.questionChoices)
        return storedFiles.map { sf -> convertStoredFileToBitmap(sf) }
    }

    /***
     * Fetches files from the repository by their remote id. Returns a list
     * of files based on the argument question choices.
     */
    private suspend fun getStoredFiles(choices: List<NQQuestionChoice>): List<StoredFile> {
        val remoteIds = choices.map { c -> c.choiceIconId }
        return fileRepository.getFilesByRemoteId(remoteIds)
    }

    /***
     * Converts a file to a bitmap. Accepts .png and .jpg file
     * extensions. File must be base64 encoded.
     */
    private fun convertStoredFileToBitmap(storedFile: StoredFile): Bitmap {
        try {
            val filename = storedFile.filename
            val bitmap: Bitmap?
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

    /***
     * Creates a PendingIntent for the argument question choice
     * by assigning its data to the intent extras.
     */
    private fun createPendingIntentForQuestion(
        questionChoice: NQQuestionChoice,
        questionNode: NQNode,
        notificationQuestionnaire: NotificationQuestionnaireByTimeOfDay,
        notificationId: Int,
        messageId: String,
        isDryRun: Boolean?
    ): PendingIntent {
        if (questionNode.data.type == applicationContext.getString(R.string.question)) {
            val intent = Intent(applicationContext, MessagingBroadcastReceiver::class.java)
            val nid = notificationId
            val icon = questionChoice.choiceIcon
            val iconId = questionChoice.choiceIconId
            val iconMd5 = questionChoice.choiceIconMd5
            val value = questionChoice.choiceValue
            val type = questionChoice.choiceValueType
            intent.putExtra(applicationContext.getString(R.string.is_dry_run), isDryRun)
            intent.putExtra(
                applicationContext.getString(R.string.notification_message_id),
                messageId
            )
            intent.putExtra(applicationContext.getString(R.string.choice_icon_id), iconId)
            intent.putExtra(applicationContext.getString(R.string.choice_icon_md5), iconMd5)
            intent.putExtra(applicationContext.getString(R.string.choice_icon), icon)
            intent.putExtra(applicationContext.getString(R.string.choice_value), value)
            intent.putExtra(applicationContext.getString(R.string.choice_type), type)
            intent.putExtra(applicationContext.getString(R.string.notification_id), nid)
            intent.putExtra(applicationContext.getString(R.string.notification_node), questionNode)
            intent.putExtra(
                applicationContext.getString(R.string.notification_questionnaire),
                notificationQuestionnaire
            )
            intent.setPackage(applicationContext.packageName)
            return PendingIntent.getBroadcast(
                applicationContext,
                Random().nextInt(),
                intent,
                0
            )
        }
        throw IOException("Invalid NQNode passed: type must be 'question'.")
    }

    /**
     * Builds and sends the notification for the current node.
     */
    suspend fun buildNotificationQuestionAndNotify(
        mNode: NQNode?,
        questionnaire: NotificationQuestionnaireByTimeOfDay,
        notificationId: Int,
        messageId: String,
        isDryRun: Boolean?
    ) {
        Log.d(TAG, "isDryRun: $isDryRun")
        mNode?.let { node ->
            when (node.data.type) {
                applicationContext.getString(R.string.in_app_questionnaire) -> {
                    prepareInAppQuestionnaire(applicationContext, mNode, isDryRun, messageId)
                }
                applicationContext.getString(R.string.question) -> {
                    val notificationData = createNotificationQuestionViewData(
                        node,
                        questionnaire,
                        notificationId,
                        messageId,
                        isDryRun
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

    suspend fun buildNotificationQuestionnaireAndNotify(
        mNode: NQNode?,
        notificationId: Int,
        messageId: String,
        isDryRun: Boolean?
    ) {
        Log.d(TAG, "isDryRun: $isDryRun")
        mNode?.let { node ->
            val pendingIntent = createQuestionnaireArguments(
                applicationContext,
                node,
                isDryRun,
                messageId
            )
            notificationBuilder.createQuestionnaireNotification(
                title = getQuestionnaireNotificationTitle(node, applicationContext),
                body = getQuestionnaireNotificationBody(node, applicationContext),
                notificationId = notificationId,
                pendingIntent = pendingIntent
            )
        }
    }

    private fun getQuestionnaireNotificationTitle(node: NQNode, context: Context): String {
        val customTitle = node.data.appquestionnaire.customTitle
        return if(customTitle != null && customTitle.isNotEmpty()) customTitle
        else context.getString(R.string.in_app_questionnaire_notification_default_title)
    }

    private fun getQuestionnaireNotificationBody(node: NQNode, context: Context): String {
        val customBody = node.data.appquestionnaire.customBody
        return if(customBody != null && customBody.isNotEmpty()) customBody
        else context.getString(R.string.in_app_questionnaire_notification_default_body)
    }

    private fun createQuestionnaireArguments(
        context: Context,
        node: NQNode,
        isDryRun: Boolean?,
        messageId: String
    ): PendingIntent {
        val b = Bundle()
        b.putString(
            context.getString(R.string.notification_node_id),
            node._id
        )
        b.putString(
            context.getString(R.string.in_app_questionnaire_id),
            node.data.appquestionnaire.qid
        )
        b.putBoolean(
            context.getString(R.string.is_dry_run),
            isDryRun ?: true
        )
        b.putString(
            context.getString(R.string.notification_message_id),
            messageId
        )
        return NavDeepLinkBuilder(context)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.questionnaireFragment)
            .setArguments(b)
            .createPendingIntent()
    }

    private fun prepareInAppQuestionnaire(
        context: Context,
        node: NQNode,
        isDryRun: Boolean?,
        messageId: String
    ) {
        val pi = createQuestionnaireArguments(
            context,
            node,
            isDryRun,
            messageId
        )
        pi.send()
    }
}