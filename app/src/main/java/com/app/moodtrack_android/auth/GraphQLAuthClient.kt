package com.app.moodtrack_android.auth

import android.util.Log
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.coroutines.await
import com.app.moodtrack_android.model.user.User
import com.app.moodtrack_android.RegisterMutation
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GraphQLAuthClient @Inject constructor(private val client: ApolloClient) {
    val TAG = "GraphQLAuthClient"

    suspend fun registerWithEmailAndPassword(email: String, password: String) : User {
        try {
            val registerMutation = RegisterMutation(Input.fromNullable(email), Input.fromNullable(password))
            val response = client.mutate(registerMutation).await()
            if(!response.hasErrors()){
                val data = response.data
                if(data != null){
                    val _id = data.registerUser?._id
                    if(_id != null){
                        val fcmToken = data.registerUser.fcmRegistrationToken
                        val notificationsEnabled = data.registerUser.notificationsEnabled
                        return User(_id = _id, notificationsEnabled = notificationsEnabled, fcmRegistrationToken = fcmToken, email = null)
                    } else {
                        throw IOException("Returned user did not contain an _id attribute.")
                    }
                } else {
                    throw IOException("The response contained no data")
                }
            } else {
                throw IOException("Response contained error(s): ${response.errors?.joinToString { e -> e.message }}")
            }
        } catch (e: Throwable) {
            Log.d(TAG, "Caught exception: " + e.stackTraceToString())
            throw e
        }
    }
}