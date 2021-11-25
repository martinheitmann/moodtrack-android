package com.app.moodtrack_android.util

import android.util.Log

class NotificationConditionUtil {
    companion object {
        val TAG = "NotificationCondUtil"
        fun evaluate(condition: String, value: Int, conditionValue: Int): Boolean? {
            try {
                when(condition){
                    NotificationCondition.EQUAL.condition -> {
                        return value == conditionValue
                    }
                    NotificationCondition.NOT_EQUAL.condition -> {
                        return value != conditionValue
                    }
                    NotificationCondition.LESS_THAN.condition -> {
                        return value < conditionValue
                    }
                    NotificationCondition.LESS_THAN_OR_EQUAL.condition -> {
                        return value <= conditionValue
                    }
                    NotificationCondition.GREATER_THAN.condition -> {
                        return value > conditionValue
                    }
                    NotificationCondition.GREATER_THAN_OR_EQUAL.condition -> {
                        return value >= conditionValue
                    }
                    else ->  {
                        Log.d(TAG, "No matching condition clause for int with argument '$condition', returning null.")
                        return null
                    }
                }
            } catch (e: Throwable){
                Log.d(TAG, e.stackTraceToString())
                return null
            }
        }

        fun evaluate(value: String, condition: String): Boolean? {
            return try {
                when(NotificationCondition.valueOf(condition)){
                    NotificationCondition.EQUAL -> {
                        value == condition
                    }
                    NotificationCondition.NOT_EQUAL -> {
                        value != condition
                    }
                    else ->  {
                    Log.d(TAG, "No matching condition clause for string, returning null.")
                    return null
                }
                }
            } catch (e: Throwable){
                Log.d(TAG, e.stackTraceToString())
                null
            }
        }

        fun evaluate(value: Boolean, condition: String): Boolean? {
            return try {
                when(NotificationCondition.valueOf(condition)){
                    NotificationCondition.EQUAL -> {
                        value == condition.toBoolean()
                    }
                    NotificationCondition.NOT_EQUAL -> {
                        value != condition.toBoolean()
                    }
                    else ->  {
                        Log.d(TAG, "No matching condition clause for boolean, returning null.")
                        return null
                    }
                }
            } catch (e: Throwable){
                Log.d(TAG, e.stackTraceToString())
                null
            }
        }
    }
}