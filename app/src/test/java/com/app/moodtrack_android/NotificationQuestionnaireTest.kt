package com.app.moodtrack_android

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.GsonBuilder

class NotificationQuestionnaireTest {

    var gson = GsonBuilder()
        .setExclusionStrategies(object : ExclusionStrategy {
            override fun shouldSkipField(f: FieldAttributes): Boolean {
                return f.name == "id"
            }

            override fun shouldSkipClass(clazz: Class<*>?): Boolean {
                return false
            }
        }).create()


}