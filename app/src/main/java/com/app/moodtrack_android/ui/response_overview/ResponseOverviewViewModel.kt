package com.app.moodtrack_android.ui.response_overview

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.app.moodtrack_android.model.notificationquestionnaire.NQResponse
import com.app.moodtrack_android.repository.AuthRepository
import com.app.moodtrack_android.repository.FileRepository
import com.app.moodtrack_android.repository.NotificationQuestionnaireResponseRepository
import com.app.moodtrack_android.util.AppDateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ResponseOverviewViewModel @Inject constructor(
    application: Application,
    private val notificationQuestionnaireResponseRepository: NotificationQuestionnaireResponseRepository,
    private val authRepository: AuthRepository,
    private val fileRepository: FileRepository
) : AndroidViewModel(application) {
    private val tag = "ResponseOverviewViewModel"

    private val context
        get() = getApplication<Application>()

    private var fetchDataJob: Job? = null

    var currentDate: LocalDateTime = LocalDateTime.now()
    var selectedCellIndex: MutableLiveData<Int?> = MutableLiveData(null)
    var selectedYearMonth: MutableLiveData<YearMonth> =
        MutableLiveData(YearMonth.of(currentDate.year, currentDate.monthValue))
    var calendarCells: MediatorLiveData<List<CalendarCell>> = MediatorLiveData()
    var currentCellNotificationResponses: MediatorLiveData<CalendarNotificationResponse> =
        MediatorLiveData()

    init {
        // Should trigger whenever the year/month is changed
        calendarCells.addSource(selectedYearMonth) { yearMonth ->
            fetchDataJob?.cancel()
            fetchDataJob = viewModelScope.launch(Dispatchers.IO) {
                val cells = createCalendarCells(yearMonth)
                Log.d(tag, "init cells size = ${cells.size}")
                val mNotificationQuestionnaireResponses =
                    fetchNotificationQuestionnaireResponsesByYearMonth(yearMonth)
                val cellsWithData = createCalendarCellsWithData(cells, mNotificationQuestionnaireResponses)
                calendarCells.postValue(cellsWithData)
            }
        }
    }

    private suspend fun createCalendarCellsWithData(
        cells: List<CalendarCell>,
        responses: List<NQResponse>
    ): List<CalendarCell> {
        fileRepository.getIconFilesRequestAndSaveMissing(responses.mapNotNull { r ->
            if (r.responseData.selectedChoice.choiceIconId.isNotBlank())
                r.responseData.selectedChoice.choiceIconId else null
        })
        return cells.map { cell ->
            val cellData = responses.filter { r ->
                val ldt =
                    AppDateUtils.convertDateToLocalDateTime(r.timestamp, ZoneId.systemDefault())
                cell.yearMonth.year == ldt.year
                        && cell.yearMonth.monthValue == ldt.monthValue
                        && cell.dayOfMonth == ldt.dayOfMonth
            }.map { r -> convertResponseToCellData(r) }
            cell.copy(responses = cellData)
        }
    }

    private suspend fun convertResponseToCellData(response: NQResponse): CalendarNotificationResponse {
        var bitmap: Bitmap? = null
        val iconId = response.responseData.selectedChoice.choiceIconId
        if(iconId.isBlank()){
            Log.d(tag, "convertResponseToCellData: WARNING blank iconId.")
        }
        val fileName = response.responseData.selectedChoice.choiceIcon
        val sf = fileRepository.getIconFileRequestAndSaveIfMissing(iconId)
        if (sf != null) {
            bitmap = fileRepository.convertBase64StringToBitmap(fileName, sf.data)
        }
        return CalendarNotificationResponse(
            choiceIcon = bitmap,
            choiceValue = response.responseData.selectedChoice.choiceValue,
            question = response.responseData.questionText,
            timestamp = response.timestamp,
            showIcon = response.node?.data?.question?.visible ?: false
        )
    }


    private suspend fun fetchNotificationQuestionnaireResponsesByYearMonth(yearMonth: YearMonth): List<NQResponse> {
        val res = getNotificationQuestionnaireResponsesByStartEnd(
            AppDateUtils.convertLocalDateTimeToDate(
                yearMonth.atDay(1).atStartOfDay(),
                ZoneId.systemDefault()
            ),
            AppDateUtils.convertLocalDateTimeToDate(
                yearMonth.atEndOfMonth().atTime(LocalTime.MAX),
                ZoneId.systemDefault()
            )
        )
        Log.d(tag, "fetchNotificationQuestionnaireResponsesByYearMonth returned ${res.size} items")
        return res
    }

    fun setSelectedDayIndex(index: Int) {
        selectedCellIndex.postValue(index)
    }

    fun incrementCurrentYearMonth() {
        val currentYearMonth = selectedYearMonth.value
        val nextYearMonth = currentYearMonth!!.plusMonths(1)
        selectedYearMonth.postValue(nextYearMonth)
    }

    fun decrementCurrentYearMonth() {
        val currentYearMonth = selectedYearMonth.value
        val nextYearMonth = currentYearMonth!!.minusMonths(1)
        selectedYearMonth.postValue(nextYearMonth)
    }

    private suspend fun getNotificationQuestionnaireResponsesByStartEnd(
        start: Date,
        end: Date
    ): List<NQResponse> {
        Log.d(tag, "getNotificationQuestionnaireResponsesByStartEnd: start = $start, end = $end")
        val userId = authRepository.getUid()
        return if (userId != null) {
            notificationQuestionnaireResponseRepository.getNotificationQuestionnaireResponsesBetween(
                userId,
                start,
                end
            ) ?: emptyList()
        } else {
            Log.d(tag, "WARNING: AuthRepository returned null from getUid.")
            emptyList()
        }
    }

    private fun createCalendarCells(yearMonth: YearMonth): List<CalendarCell> {
        val cells = mutableListOf<CalendarCell>()
        val dayCount = yearMonth.lengthOfMonth()
        for (i in 1..dayCount) {
            val newCell = CalendarCell(yearMonth, i, true, listOf())
            cells.add(newCell)
        }
        val before = AppDateUtils.getDatesBeforeMonthStart(yearMonth)
        val after = AppDateUtils.getDatesAfterMonthEnd(yearMonth)
        val beforeCells = before.map { d -> createCalendarCellNotCurrentMonth(d) }
        val afterCells = after.map { d -> createCalendarCellNotCurrentMonth(d) }
        return beforeCells + cells + afterCells
    }

    private fun createCalendarCellNotCurrentMonth(localDateTime: LocalDateTime): CalendarCell {
        val localDate = localDateTime.toLocalDate()
        return CalendarCell(
            dayOfMonth = localDate.dayOfMonth,
            yearMonth = YearMonth.of(localDate.year, localDate.monthValue),
            responses = listOf(),
            isCurrentMonth = false
        )
    }
}