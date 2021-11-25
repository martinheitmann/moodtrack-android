package com.app.moodtrack_android.repository

import android.util.Log
import com.app.moodtrack_android.auth.AuthResult
import com.app.moodtrack_android.auth.FirebaseAuthClient
import com.app.moodtrack_android.auth.GraphQLAuthClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val gqlAuth: GraphQLAuthClient,
    private val fbAuth: FirebaseAuthClient)
{
    val TAG = "AuthRepository"

    fun isLoggedIn() : Boolean {
        return fbAuth.isLoggedIn()
    }

    fun getUid() : String? {
        return fbAuth.getUid()
    }

    suspend fun signInWithEmailAndPassword(email: String, password: String) : AuthResult {
        return try {
            AuthResult(success = fbAuth.signInWithEmailAndPassword(email, password))
        }
        catch (e: Throwable){
            AuthResult(error = e)
        }
    }

    suspend fun registerWithEmailAndPassword(email: String, password: String) : AuthResult {
        return try {
            Log.d(TAG, "Registering user with email $email")
            val result = gqlAuth.registerWithEmailAndPassword(email, password)
            Log.d(TAG, "Register attempt returned successfully with user uid ${result._id}")
            val uid = fbAuth.signInWithEmailAndPassword(email, password)
            Log.d(TAG, "Successfully signed in user with uid $uid")
            AuthResult(success = result)
        } catch (e: Throwable){
            Log.d(TAG, "Register failed with exception: ${e.stackTraceToString()}")
            AuthResult(error = e)
        }
    }

}