package com.app.moodtrack_android.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Singleton
class FirebaseAuthClient @Inject constructor(
    private val auth: FirebaseAuth
) {

    fun getUid() : String? {
        val currentUser = auth.currentUser
        if(currentUser != null) {
            return currentUser.uid
        }
        return null
    }

    fun isLoggedIn() : Boolean {
        return auth.currentUser != null
    }

    suspend fun signInWithEmailAndPassword(email: String, password: String) : String? {
        return suspendCoroutine {  continuation ->
            Log.d("FirebaseAuthClient", "Signing in user with email $email")
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    Log.d("FirebaseAuthClient", "Sign in for email $email succeeded")
                    val currentUser = auth.currentUser
                    continuation.resume(currentUser?.uid)
                }
                .addOnFailureListener { exception ->
                    Log.d("FirebaseAuthClient", "Sign in for email $email failed")
                    exception.printStackTrace()
                    continuation.resumeWithException(exception)
                }
        }
    }
}
