package com.app.moodtrack_android.messaging

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.os.PersistableBundle
import android.util.Log
import com.app.moodtrack_android.auth.FirebaseAuthClient
import com.app.moodtrack_android.firestore.FirestoreUserDao
import com.app.moodtrack_android.repository.LogEntryRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class MessagingService : FirebaseMessagingService() {
    private val TAG = "MessagingService"

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    @Inject
    lateinit var gson: Gson
    @Inject
    lateinit var userDao: FirestoreUserDao
    @Inject
    lateinit var authClient: FirebaseAuthClient
    @Inject
    lateinit var loggingRepository: LogEntryRepository

    /* Called when the old token expires or is compromised*/
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "onNewToken called")
        val userId = authClient.getUid()
        serviceScope.launch {
            if(userId != null) userDao.updateUserRegistrationToken(token, userId)
        }
    }

    /* Called with the data segment of the message as the argument.*/
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "onMessageReceived called.")
        try {
            if(authClient.isLoggedIn()){
                val msg = remoteMessage.data
                Log.d(TAG, msg.toString())
                val json = msg["item"]
                if(json != null){
                    Log.d(TAG, json)
                    val persistableBundle = PersistableBundle()
                    persistableBundle.putString("json", json)
                    val jobInfo = JobInfo
                        .Builder(0, ComponentName(applicationContext, InitNotificationQuestionnaireJobService::class.java))
                        .setExtras(persistableBundle)
                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                        .setOverrideDeadline(300000)
                        .build()
                    val jobScheduler: JobScheduler = applicationContext.getSystemService(JobScheduler::class.java)
                    jobScheduler.schedule(jobInfo)
                } else {
                    Log.d(TAG, "Received json was null, skipping message.")
                }
            } else {
                Log.d(TAG, "User is not logged in, skipping message.")
            }
        } catch (e: Throwable){
            Log.d(TAG, e.stackTraceToString())
        }
    }
}