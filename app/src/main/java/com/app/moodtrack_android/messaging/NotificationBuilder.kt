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
import android.widget.ImageButton
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.app.moodtrack_android.MainActivity
import com.app.moodtrack_android.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject

/**
 * Class for building and sending notifications related to questions and questionnaires.
 *
 */
class NotificationBuilder @Inject constructor(
    @ApplicationContext val context: Context
) {
    val tag = "NotificationBuilder"

    /**
     * Creates and notifies with a questionnaire notification,
     * sending the user to answer a questionnaire upon opening.
     * @param title The title of the notification.
     * @param body The notification body.
     * @param pendingIntent The [PendingIntent] to be attached.
     * @param notificationId the id of the notification.
     */
    fun createQuestionnaireNotification(
        title: String?,
        body: String?,
        pendingIntent: PendingIntent,
        notificationId: Int,
    ){
        createNotificationChannel()

        // If null, default to values from strings.
        val notificationTitle = title ?: context.getString(R.string.in_app_questionnaire_notification_default_title)
        val notificationBody = body ?: context.getString(R.string.in_app_questionnaire_notification_default_title)

        val sb: Spannable = SpannableString(notificationTitle)
        sb.setSpan(StyleSpan(Typeface.BOLD), 0, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        val builder = NotificationCompat.Builder(context, context.getString(R.string.notification_channel_id))
            .setSmallIcon(R.mipmap.moodtrack_now_letter_ic)
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

    /**
     * Creates and notifies with a question notification,
     * sending the user to answer a questionnaire upon opening.
     * @param text The question text to display.
     * @param buttons Buttons to be displayed in notification.
     * @param notificationId ID of notification.
     */
    fun createQuestionNotification(
        text: String,
        buttons: List<Pair<Bitmap, PendingIntent>>,
        notificationId: Int,
    ) {
        // Create an explicit intent for an Activity
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or 0)

        // Get the layouts to use in the custom notification
        val notificationLayout = generateHorizontalNotificationRemoteView(text, buttons)
        createNotificationChannel()
        // Apply the layouts to the notification
        val customNotification =
            NotificationCompat.Builder(context, context.getString(R.string.notification_channel_id))
                .setSmallIcon(R.mipmap.moodtrack_now_letter_ic)
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
     * Creates the notification channel.
     *
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
     * Creates and populates a [RemoteViews] with the given
     * list of [Bitmap] objects. Additionally sets the corresponding [PendingIntent]
     * for the notification [ImageButton].
     * @param text Notification text to be displayed.
     * @param buttons A list of [Pair] containing [Bitmap] and [PendingIntent] objects.
     */
    private fun generateHorizontalNotificationRemoteView(
        text: String,
        buttons: List<Pair<Bitmap, PendingIntent>>,
    ): RemoteViews {
        val remoteView = selectRemoteView(buttons.size)
        remoteView.removeAllViews(R.id.r_custom_notification_h_icon_container)
        remoteView.setTextViewText(R.id.r_custom_notification_h_textview_question, text)
        for(i in buttons.indices){
            val imageButton = RemoteViews(context.packageName, R.layout.notification_imagebutton)
            imageButton.setImageViewBitmap(R.id.notification_imagebutton_view, buttons[i].first)
            imageButton.setOnClickPendingIntent(R.id.notification_imagebutton_view, buttons[i].second)
            remoteView.addView(R.id.r_custom_notification_h_icon_container, imageButton)
        }
        return remoteView
    }

    /**
     * Selects an appropriate [RemoteViews] based on the argument integer.
     * @param elementCount the element count.
     * @return [RemoteViews] corresponding to the argument integer.
     * @throws [IllegalArgumentException] if the argument is less than 1 or greater than 5.
     */
    private fun selectRemoteView(elementCount: Int): RemoteViews {
        if(elementCount > 5 || elementCount < 1){
            throw IllegalArgumentException("Argument 'elementCount' must be a value between 1 and 5. Received ${elementCount}.")
        }
        return when {
            elementCount < 3 -> {
                RemoteViews(context.packageName, R.layout.custom_notification_layout_horizontal_4_2)
            }
            elementCount > 3 -> {
                RemoteViews(context.packageName, R.layout.custom_notification_layout_horizontal_2_3)
            }
            else -> {
                RemoteViews(context.packageName, R.layout.custom_notification_layout_horizontal_equal)
            }
        }
    }

    /**
     * This is a really, really bad way to populate a RemoteView.
     *
     * TODO: Remove method.
     */
    @Deprecated("Replaced by NotificationBuilder.generateHorizontalNotificationRemoteView")
    private fun createHorizontalNotificationRemoteView(
        text: String,
        buttons: List<Pair<Bitmap, PendingIntent>>,
    ): RemoteViews {
        val remoteView =
            RemoteViews(context.packageName, R.layout.custom_notification_layout_horizontal)
        remoteView.setTextViewText(R.id.custom_notification_h_textview_question, text)
        Log.d(tag, "Bitmap/PendingIntent list contained ${buttons.size} elements")
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