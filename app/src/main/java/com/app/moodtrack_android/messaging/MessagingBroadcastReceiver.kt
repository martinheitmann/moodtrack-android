package com.app.moodtrack_android.messaging

import android.app.NotificationManager
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PersistableBundle
import android.util.Log
import com.app.moodtrack_android.R
import com.app.moodtrack_android.model.notificationquestionnaire.NQNode
import com.app.moodtrack_android.model.notificationquestionnaire.query_objects.NotificationQuestionnaireByTimeOfDay
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MessagingBroadcastReceiver : BroadcastReceiver() {
    val TAG = "MessagingBroadcastRec"

    @Inject
    lateinit var gson: Gson

    override fun onReceive(context: Context?, intent: Intent?) {
        try {
            if (intent != null && context != null) {
                val isDryRun = intent.extras?.get(context.getString(R.string.is_dry_run)) as Boolean?
                val messageId =
                    intent.extras?.get(context.getString(R.string.notification_message_id)) as String
                val choiceIcon =
                    intent.extras?.get(context.getString(R.string.choice_icon)) as String
                val choiceIconId =
                    intent.extras?.get(context.getString(R.string.choice_icon_id)) as String
                val choiceIconMd5 =
                    intent.extras?.get(context.getString(R.string.choice_icon_md5)) as String?
                val choiceValue =
                    intent.extras?.get(context.getString(R.string.choice_value)) as String
                val choiceType =
                    intent.extras?.get(context.getString(R.string.choice_type)) as String
                val notificationId =
                    intent.extras?.get(context.getString(R.string.notification_id)) as Int
                val notificationNode =
                    intent.extras?.get(context.getString(R.string.notification_node)) as NQNode
                val notificationQuestionnaire = intent.extras?.get(
                    context.getString(R.string.notification_questionnaire)
                ) as NotificationQuestionnaireByTimeOfDay

                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(notificationId)

                val notificationNodeAsString = gson.toJson(notificationNode)
                val notificationQuestionnaireAsString = gson.toJson(notificationQuestionnaire)

                val persistableBundle = PersistableBundle()
                persistableBundle.putString(
                    context.getString(R.string.notification_message_id),
                    messageId
                )
                persistableBundle.putBoolean(context.getString(R.string.is_dry_run), isDryRun ?: true)
                persistableBundle.putString(context.getString(R.string.choice_icon_id), choiceIconId)
                persistableBundle.putString(context.getString(R.string.choice_icon_md5), choiceIconMd5)
                persistableBundle.putString(context.getString(R.string.choice_icon), choiceIcon)
                persistableBundle.putString(context.getString(R.string.choice_value), choiceValue)
                persistableBundle.putString(context.getString(R.string.choice_type), choiceType)
                persistableBundle.putString(
                    context.getString(R.string.notification_node),
                    notificationNodeAsString
                )
                persistableBundle.putString(
                    context.getString(R.string.notification_questionnaire),
                    notificationQuestionnaireAsString
                )

                val jobInfo = JobInfo
                    .Builder(1, ComponentName(context, NotificationLoopJobService::class.java))
                    .setExtras(persistableBundle)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setOverrideDeadline(300000)
                    .build()
                val jobScheduler: JobScheduler = context.getSystemService(JobScheduler::class.java)
                jobScheduler.schedule(jobInfo)
            } else {
                Log.d(TAG, "Received intent or context was null.")
            }
        } catch (e: Throwable) {
            Log.d(TAG, e.stackTraceToString())
        }
    }
}