package com.app.moodtrack_android.firestore

import android.util.Log
import com.app.moodtrack_android.model.user.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
class FirestoreUserDao @Inject constructor(
    private val db: FirebaseFirestore
) {

    val doc = db.collection("users")

    suspend fun updateUserRegistrationToken(token: String, userId: String) : Boolean {
        return suspendCoroutine {continuation ->
            Log.d("FirestoreUserDao", "updateUserRegistrationToken called")
            val docRef = doc.whereEqualTo("id", userId)
            docRef.get()
                .addOnSuccessListener {documents ->
                    if(!documents.isEmpty && documents.size() == 1){
                        Log.d("FirestoreUserDao", "User successfully found")
                        val userDoc = documents.documents[0]
                        userDoc.reference.update("registrationToken", token).addOnCompleteListener { task ->
                            if(task.isComplete){
                                if(task.isSuccessful){
                                    Log.d("FirestoreUserDao", "Token for user ${userDoc["id"]} successfully updated")
                                    continuation.resume(true)
                                } else {
                                    continuation.resume(false)
                                }
                            }
                        }
                    } else {
                        continuation.resume(false)
                    }
                }
                .addOnFailureListener {
                    continuation.resume(false)
                }
        }
    }
}