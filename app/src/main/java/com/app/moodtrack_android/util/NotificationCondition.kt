package com.app.moodtrack_android.util

enum class NotificationCondition(val condition: String) {
    EQUAL("equal"),
    NOT_EQUAL("not_equal"),
    LESS_THAN("less_than"),
    LESS_THAN_OR_EQUAL("less_than_or_equal"),
    GREATER_THAN("greater_than"),
    GREATER_THAN_OR_EQUAL("greater_than_or_equal"),
}