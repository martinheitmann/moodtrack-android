package com.app.moodtrack_android

import com.app.moodtrack_android.util.AppDateUtils
import org.junit.Assert
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.YearMonth

class AppDateUtilsTest {

    @Test
    fun padding_month_starting_with_monday_returns_empty_list() {
        val date = YearMonth.of(2021, 11)
        val days = AppDateUtils.getDatesBeforeMonthStart(date)
        Assert.assertTrue(days.isEmpty())
    }

    @Test
    fun padding_month_starting_with_tuesday_returns_one_element() {
        val date = YearMonth.of(2021, 6)
        val days = AppDateUtils.getDatesBeforeMonthStart(date)
        Assert.assertEquals(1, days.size)
        Assert.assertEquals(DayOfWeek.MONDAY, days.first().dayOfWeek)
    }

    @Test
    fun padding_month_starting_with_tuesday_returns_two_elements() {
        val date = YearMonth.of(2021, 12)
        val days = AppDateUtils.getDatesBeforeMonthStart(date)
        Assert.assertEquals(2, days.size)
        val day1 = days.find { d -> d.dayOfWeek == DayOfWeek.MONDAY }
        val day2 = days.find { d -> d.dayOfWeek == DayOfWeek.TUESDAY }
        Assert.assertNotNull(day1)
        Assert.assertEquals(29, day1?.dayOfMonth)
        Assert.assertNotNull(day2)
        Assert.assertEquals(30, day2?.dayOfMonth)
    }

    @Test
    fun padding_month_ending_with_sunday_returns_empty_list() {
        val date = YearMonth.of(2021, 10)
        val days = AppDateUtils.getDatesAfterMonthEnd(date)
        Assert.assertTrue(days.isEmpty())
    }

    @Test
    fun padding_month_ending_with_saturday_returns_one_element() {
        val date = YearMonth.of(2021, 7)
        val days = AppDateUtils.getDatesAfterMonthEnd(date)
        Assert.assertEquals(1, days.size)
    }

    @Test
    fun padding_month_ending_with_friday_returns_two_elements() {
        val date = YearMonth.of(2021, 4)
        val days = AppDateUtils.getDatesAfterMonthEnd(date)
        Assert.assertEquals(2, days.size)
    }

    @Test
    fun first_day_of_month_is_returned_for_a_given_date(){
        val date = YearMonth.of(2019, 5)
        val firstDay = AppDateUtils.getFirstDayOfMonth(date)
        Assert.assertEquals(1, firstDay.dayOfMonth)
        Assert.assertEquals(5, firstDay.monthValue)
    }

    @Test
    fun last_day_of_month_is_returned_for_a_given_date(){
        val date = YearMonth.of(2018, 8)
        val firstDay = AppDateUtils.getLastDayOfMonth(date)
        Assert.assertEquals(31, firstDay.dayOfMonth)
        Assert.assertEquals(8, firstDay.monthValue)
    }
}