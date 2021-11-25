package com.app.moodtrack_android.firestore

import android.util.Log
import com.app.moodtrack_android.model.log.LogEntry
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
class FirestoreLogEntryDao @Inject constructor(
    private val db: FirebaseFirestore
) {
    suspend fun addLogEntry(logEntry: LogEntry) : String? {
        return suspendCoroutine { continuation ->
            val doc = db
                .collection("clientlogs")
                .add(logEntry)
                .addOnSuccessListener { documentReference ->
                    continuation.resume(documentReference.id)
                }
                .addOnFailureListener { e ->
                    Log.e("FirestoreLogEntryDao", e.stackTraceToString())
                    continuation.resume(null)
                }
        }
    }

}