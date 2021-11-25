package com.app.moodtrack_android.repository

import android.content.Context
import androidx.work.*
import com.app.moodtrack_android.firestore.FirestoreLogEntryDao
import com.app.moodtrack_android.model.log.LogEntry
import com.app.moodtrack_android.model.log.LogEntryAction
import com.app.moodtrack_android.model.log.LogEntryStatus
import com.app.moodtrack_android.tasks.LogEntryWorker
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import javax.inject.Inject

class LogEntryRepository @Inject constructor(
    @ApplicationContext val context: Context,
    private val logEntryDao: FirestoreLogEntryDao,
    private val gson: Gson)
{
    /**
     * Logs the result of receiving a FCM message
     * @param status The status of the operation
     */
    fun logFcmMessageReceived(status: LogEntryStatus){
        val logEntry = createLogEntry(
            action = LogEntryAction.APP_RECEIVED_FCM_MESSAGE,
            status = status.toString(),
            extras = null,
            description = "Message received by FirebaseMessagingService",
        )
        if(logEntry != null) startLogEntryWorker(logEntry)
    }

    /**
     * Logs the result of a user successfully signing in
     * @param status The status of the operation
     */
    fun logUserSignedIn(status: LogEntryStatus){
        val logEntry = createLogEntry(
            action = LogEntryAction.USER_SIGNED_IN,
            status = status.toString(),
            extras = null,
            description = "User successfully signed in",
        )
        if(logEntry != null) startLogEntryWorker(logEntry)
    }

    /**
     * Logs the result of a user successfully signing out
     * @param status The status of the operation
     */
    fun logUserSignedOut(status: LogEntryStatus){
        val logEntry = createLogEntry(
            action = LogEntryAction.USER_SIGNED_OUT,
            status = status.toString(),
            extras = null,
            description = "User successfully signed out",
        )
        if(logEntry != null) startLogEntryWorker(logEntry)
    }

    /**
     * Logs the result of a user successfully creating
     * an account
     * @param status The status of the operation
     */
    fun logUserSignedUp(status: LogEntryStatus){
        val logEntry = createLogEntry(
            action = LogEntryAction.USER_SIGNED_UP,
            status = status.toString(),
            extras = null,
            description = "User successfully created a new account",
        )
        if(logEntry != null) startLogEntryWorker(logEntry)
    }

    /**
     * Starts a worker for storing a log entry
     * @param logEntry The log entry to be stored.
     */
    private fun startLogEntryWorker(logEntry: LogEntry){
        val jsonLogEntry = gson.toJson(logEntry)
        val data = Data.Builder()
            .putString("jsonLogEntry", jsonLogEntry)
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request: WorkRequest = OneTimeWorkRequest.Builder(LogEntryWorker::class.java)
            .setInputData(data)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }

    /**
     * Creates a log entry if the user is logged in.
     * Returns null if the users does not have a valid session.
     */
    private fun createLogEntry(
        action: LogEntryAction,
        status: String,
        extras: String?,
        description: String?
    ): LogEntry? {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if(currentUser != null){
            return LogEntry(
                timestamp = Date(),
                userId = currentUser.uid,
                action = action.toString(),
                actionStatus = status,
                extras = extras,
                description = description
            )
        }
        return null
    }

    /**
     * Call the DAO class to store the log entry
     * @param logEntry the entry to be stored.
     */
    suspend fun addLogEntry(logEntry: LogEntry){
        logEntryDao.addLogEntry(logEntry)
    }

}