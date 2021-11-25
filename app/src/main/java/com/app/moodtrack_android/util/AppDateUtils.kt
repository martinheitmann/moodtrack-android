package com.app.moodtrack_android.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AppDateUtils {
    companion object {
        fun parseIsoDate(dateString: String): LocalDateTime {
            val pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
            val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern(pattern)
            return LocalDateTime.parse(dateString, formatter)
        }
    }
}