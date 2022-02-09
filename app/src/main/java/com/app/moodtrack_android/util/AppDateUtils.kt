package com.app.moodtrack_android.util

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjuster
import java.time.temporal.TemporalAdjusters
import java.util.*

class AppDateUtils {
    companion object {

        @SuppressLint("SimpleDateFormat")
        fun parseIsoDateStringToDate(dateString: String): Date {
            val pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
            val sdf = SimpleDateFormat(pattern)
            return sdf.parse(dateString) ?: Date()
        }

        fun parseIsoDateStringToLocalDateTime(dateString: String): LocalDateTime {
            val pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
            val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern(pattern)
            return LocalDateTime.parse(dateString, formatter)
        }

        /**
         * Fetch the number of days before the start of the current month.
         * Useful for padding months which don't start on a monday, but still
         * requires a full week row.
         */
        fun getDatesBeforeMonthStart(yearMonth: YearMonth): MutableList<LocalDateTime>{
            var start = yearMonth.atDay(1).atStartOfDay()
            val datesBeforeMonthStart = mutableListOf<LocalDateTime>()
            while(start.dayOfWeek != DayOfWeek.MONDAY){
                start = start.minusDays(1)
                datesBeforeMonthStart += start
            }
            return datesBeforeMonthStart
        }

        /**
         * Fetch the number of days before the start of the current month.
         * Useful for padding months which don't start on a monday, but still
         * requires a full week row.
         */
        fun getDatesAfterMonthEnd(yearMonth: YearMonth): MutableList<LocalDateTime>{
            var end = yearMonth.atEndOfMonth().atTime(LocalTime.MAX)
            val datesAfterMonthEnd = mutableListOf<LocalDateTime>()
            while(end.dayOfWeek != DayOfWeek.SUNDAY){
                end = end.plusDays(1)
                datesAfterMonthEnd += end
            }
            return datesAfterMonthEnd
        }

        fun getFirstDayOfMonth(yearMonth: YearMonth): LocalDateTime {
            return yearMonth.atDay(1).atTime(LocalTime.MIN)
        }

        fun getLastDayOfMonth(yearMonth: YearMonth): LocalDateTime {
            return yearMonth.atEndOfMonth().atTime(LocalTime.MAX)
        }

        fun convertLocalDateTimeToDate(ldt: LocalDateTime, zoneId: ZoneId): Date {
            return Date.from(ldt.atZone(zoneId).toInstant())
        }

        fun convertDateToLocalDateTime(date: Date, zoneId: ZoneId): LocalDateTime {
            return LocalDateTime.ofInstant(date.toInstant(), zoneId)
        }
    }
}