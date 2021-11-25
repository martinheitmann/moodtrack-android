package com.app.moodtrack_android.tasks

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.app.moodtrack_android.model.log.LogEntry
import com.app.moodtrack_android.repository.LogEntryRepository
import com.google.gson.Gson
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.runBlocking

@HiltWorker
class LogEntryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val gson: Gson,
    private val logEntryRepository: LogEntryRepository
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        // Worker can't receive custom objects, hence serializing/deserializing is necessary
        val jsonLogEntry =  inputData.getString("jsonLogEntry")
        val logEntry = gson.fromJson(jsonLogEntry, LogEntry::class.java)
        return try {
            runBlocking { logEntryRepository.addLogEntry(logEntry) }
            Result.success()
        } catch (e: Exception){
            Result.failure()
        }
    }
}