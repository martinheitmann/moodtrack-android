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

    suspend fun getUser(id: String) : User? {
        return suspendCoroutine { continuation ->
            val docRef = doc.whereEqualTo("id", id)
            docRef.get()
                .addOnSuccessListener { documents ->
                    // We have to assume a list here since whereEqualTo always returns a list
                    if(!documents.isEmpty && documents.size() == 1){
                        val userDoc = documents.documents[0]
                        Log.d("FirestoreUserDao", "get succeeded with doc id ${userDoc.id}")
                        val user = userDoc.toObject<User>()
                        Log.d("FirestoreUserDao", "document successfully converted to user with id: ${user?._id}")
                        continuation.resume(user)
                    } else {
                        continuation.resume(null)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d("FirestoreUserDao", "get failed with ", exception)
                    continuation.resume(null)
                }
        }
    }

    suspend fun addUser(user: User) : String?{
        return suspendCoroutine {continuation ->
            doc
                .add(user)
                .addOnSuccessListener { documentReference ->
                    continuation.resume(documentReference.id)
                }
                .addOnFailureListener { e ->
                    continuation.resume(null)
                }
        }
    }

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

    /**
     * Updates the user's consent variable. Returns the new value if it succeeds,
     * returns null otherwise.
     */
    suspend fun updateUserNotificationConsent(value: Boolean, userId: String) : Boolean? {
        Log.d("FirestoreUserDao", "updateUserNotificationConsent called")
        return suspendCoroutine { continuation ->
            val docRef = doc.whereEqualTo("id", userId)
            docRef.get()
                .addOnSuccessListener {documents ->
                    if(!documents.isEmpty && documents.size() == 1){
                        Log.d("FirestoreUserDao", "User successfully found")
                        val userDoc = documents.documents[0]
                        userDoc.reference.update("consentsToPushNotifications", value).addOnCompleteListener { task ->
                            if(task.isComplete){
                                if(task.isSuccessful){
                                    Log.d("FirestoreUserDao", "Token for user ${userDoc["id"]} successfully updated")
                                    continuation.resume(value)
                                } else {
                                    continuation.resume(null)
                                }
                            }
                        }
                    } else {
                        continuation.resume(null)
                    }
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
        }
    }
}