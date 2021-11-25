package com.app.moodtrack_android.messaging

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Typeface
import android.media.AudioAttributes
import android.media.AudioAttributes.USAGE_NOTIFICATION_EVENT
import android.media.RingtoneManager
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.app.moodtrack_android.MainActivity
import com.app.moodtrack_android.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject

class NotificationBuilder @Inject constructor(
    @ApplicationContext val context: Context
) {
    val TAG = "NotificationBuilder"

    fun createQuestionnaireNotification(
        title: String?,
        body: String?,
        pendingIntent: PendingIntent,
        notificationId: Int,
    ){
        createNotificationChannel()

        val notificationTitle = title ?: context.getString(R.string.in_app_questionnaire_notification_default_title)
        val notificationBody = body ?: context.getString(R.string.in_app_questionnaire_notification_default_title)

        val sb: Spannable = SpannableString(notificationTitle)
        sb.setSpan(StyleSpan(Typeface.BOLD), 0, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        val builder = NotificationCompat.Builder(context, context.getString(R.string.notification_channel_id))
            .setSmallIcon(R.drawable.ic_mt_notification_icon)
            .setContentTitle(sb) // Using Spannable for bold title.
            .setContentText(notificationBody)
            .setStyle(
                NotificationCompat.BigTextStyle() // Expandable notification style
                    .bigText(notificationBody)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Should be at least high.
            .setContentIntent(pendingIntent)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(2000, 0, 2000))
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.build())
        }
    }

    fun createQuestionNotification(
        text: String,
        buttons: List<Pair<Bitmap, PendingIntent>>,
        notificationId: Int,
    ) {
        // Create an explicit intent for an Activity in your app
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

        // Get the layouts to use in the custom notification

        val notificationLayout = createHorizontalNotificationRemoteView(text, buttons)
        createNotificationChannel()
        // Apply the layouts to the notification
        val customNotification =
            NotificationCompat.Builder(context, context.getString(R.string.notification_channel_id))
                .setSmallIcon(R.drawable.ic_mt_notification_icon)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(notificationLayout)
                .setContentIntent(pendingIntent)
                .build()

        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            notify(notificationId, customNotification)
        }
    }

    /**
     * This should probably be created at application startup rather than
     * for each notification.
     * TODO: Move [createNotificationChannel] to [MainActivity]
     */
    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_channel_name)
            val descriptionText = context.getString(R.string.notification_channel_description)
            val channel = NotificationChannel(
                context.getString(R.string.notification_channel_id),
                name,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = descriptionText
                vibrationPattern = longArrayOf(2000, 0, 2000)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder()
                        .setUsage(USAGE_NOTIFICATION_EVENT)
                        .build(),
                )
                enableVibration(true)
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * This is a really, really bad way to populate a RemoteView.
     * TODO: Fix messy [createHorizontalNotificationRemoteView] function.
     */
    private fun createHorizontalNotificationRemoteView(
        text: String,
        buttons: List<Pair<Bitmap, PendingIntent>>,
    ): RemoteViews {
        val remoteView =
            RemoteViews(context.packageName, R.layout.custom_notification_layout_horizontal)
        remoteView.setTextViewText(R.id.custom_notification_h_textview_question, text)
        Log.d(TAG, "Bitmap/PendingIntent list contained ${buttons.size} elements")
        when (buttons.size) {
            0 -> {
                throw IOException("Cannot render notification with zero button elements")
            }
            1 -> {
                remoteView.setImageViewBitmap(R.id.custom_notification_h_b1, buttons[0].first)
                remoteView.setOnClickPendingIntent(R.id.custom_notification_h_b1, buttons[0].second)
                remoteView.setViewVisibility(R.id.custom_notification_h_b1, View.VISIBLE)
            }
            2 -> {
                remoteView.setImageViewBitmap(R.id.custom_notification_h_b1, buttons[0].first)
                remoteView.setOnClickPendingIntent(R.id.custom_notification_h_b1, buttons[0].second)

                remoteView.setImageViewBitmap(R.id.custom_notification_h_b2, buttons[1].first)
                remoteView.setOnClickPendingIntent(R.id.custom_notification_h_b2, buttons[1].second)

                remoteView.setViewVisibility(R.id.custom_notification_h_b1, View.VISIBLE)
                remoteView.setViewVisibility(R.id.custom_notification_h_b2, View.VISIBLE)
            }
            3 -> {
                remoteView.setImageViewBitmap(R.id.custom_notification_h_b1, buttons[0].first)
                remoteView.setOnClickPendingIntent(R.id.custom_notification_h_b1, buttons[0].second)

                remoteView.setImageViewBitmap(R.id.custom_notification_h_b2, buttons[1].first)
                remoteView.setOnClickPendingIntent(R.id.custom_notification_h_b2, buttons[1].second)

                remoteView.setImageViewBitmap(R.id.custom_notification_h_b3, buttons[2].first)
                remoteView.setOnClickPendingIntent(R.id.custom_notification_h_b3, buttons[2].second)

                remoteView.setViewVisibility(R.id.custom_notification_h_b1, View.VISIBLE)
                remoteView.setViewVisibility(R.id.custom_notification_h_b2, View.VISIBLE)
                remoteView.setViewVisibility(R.id.custom_notification_h_b3, View.VISIBLE)
            }
            4 -> {
                remoteView.setImageViewBitmap(R.id.custom_notification_h_b1, buttons[0].first)
                remoteView.setOnClickPendingIntent(R.id.custom_notification_h_b1, buttons[0].second)

                remoteView.setImageViewBitmap(R.id.custom_notification_h_b2, buttons[1].first)
                remoteView.setOnClickPendingIntent(R.id.custom_notification_h_b2, buttons[1].second)

                remoteView.setImageViewBitmap(R.id.custom_notification_h_b3, buttons[2].first)
                remoteView.setOnClickPendingIntent(R.id.custom_notification_h_b3, buttons[2].second)

                remoteView.setImageViewBitmap(R.id.custom_notification_h_b4, buttons[3].first)
                remoteView.setOnClickPendingIntent(R.id.custom_notification_h_b4, buttons[3].second)

                remoteView.setViewVisibility(R.id.custom_notification_h_b1, View.VISIBLE)
                remoteView.setViewVisibility(R.id.custom_notification_h_b2, View.VISIBLE)
                remoteView.setViewVisibility(R.id.custom_notification_h_b3, View.VISIBLE)
                remoteView.setViewVisibility(R.id.custom_notification_h_b4, View.VISIBLE)
            }
            5 -> {
                remoteView.setImageViewBitmap(R.id.custom_notification_h_b1, buttons[0].first)
                remoteView.setOnClickPendingIntent(R.id.custom_notification_h_b1, buttons[0].second)

                remoteView.setImageViewBitmap(R.id.custom_notification_h_b2, buttons[1].first)
                remoteView.setOnClickPendingIntent(R.id.custom_notification_h_b2, buttons[1].second)

                remoteView.setImageViewBitmap(R.id.custom_notification_h_b3, buttons[2].first)
                remoteView.setOnClickPendingIntent(R.id.custom_notification_h_b3, buttons[2].second)

                remoteView.setImageViewBitmap(R.id.custom_notification_h_b4, buttons[3].first)
                remoteView.setOnClickPendingIntent(R.id.custom_notification_h_b4, buttons[3].second)

                remoteView.setImageViewBitmap(R.id.custom_notification_h_b5, buttons[4].first)
                remoteView.setOnClickPendingIntent(R.id.custom_notification_h_b5, buttons[4].second)

                remoteView.setViewVisibility(R.id.custom_notification_h_b1, View.VISIBLE)
                remoteView.setViewVisibility(R.id.custom_notification_h_b2, View.VISIBLE)
                remoteView.setViewVisibility(R.id.custom_notification_h_b3, View.VISIBLE)
                remoteView.setViewVisibility(R.id.custom_notification_h_b4, View.VISIBLE)
                remoteView.setViewVisibility(R.id.custom_notification_h_b5, View.VISIBLE)
            }
            else -> {
                throw IOException("Cannot render notification with more than 5 elements. Argument contained ${buttons.size}")
            }
        }

        return remoteView
    }
}