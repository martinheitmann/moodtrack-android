package com.app.moodtrack_android.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
class FirebaseAuthIdTokenResolver @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    val TAG = "FAuthIdTokenResolver"
    suspend fun fetchIdToken(): String? {
      return suspendCoroutine { continuation ->
          firebaseAuth.currentUser?.getIdToken(true)
              ?.addOnSuccessListener { result ->
                  continuation.resume(result.token)
              }
              ?.addOnFailureListener { failure ->
                  Log.d(TAG, failure.message ?: "Unresolved error.")
                  continuation.resume(null)
              }
      }
    }
}