package com.app.moodtrack_android.settings

import android.content.Context
import com.app.moodtrack_android.R

class NotificationSettingsPrefs(val context: Context) {

    fun storeNotificationSettings(notificationsIsEnabled: Boolean){
        val sharedPref = context.getSharedPreferences(
            context.getString(R.string.app_shared_prefs), Context.MODE_PRIVATE) ?: return
        with (sharedPref.edit()) {
            putBoolean(context.getString(R.string.notification_settings_sharedprefs), notificationsIsEnabled)
            apply()
        }

    }

    fun getNotificationSettings(){

    }

}