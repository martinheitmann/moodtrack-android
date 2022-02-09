package com.app.moodtrack_android.graphql

import android.util.Log
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.coroutines.await
import com.apollographql.apollo.request.RequestHeaders
import com.app.moodtrack_android.UpdateFcmTokenMutation
import com.app.moodtrack_android.UpdateNotificationsPrefsMutation
import com.app.moodtrack_android.UserQuery
import com.app.moodtrack_android.auth.FirebaseAuthIdTokenResolver
import com.app.moodtrack_android.model.user.User
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GraphQLUserDao @Inject constructor(
    private val client: ApolloClient,
    private val tokenResolver: FirebaseAuthIdTokenResolver
) {
    val tag = "GQLUserDao"

    suspend fun queryUser(id: String): User {
        try {
            val idToken = tokenResolver.fetchIdToken()
            val response = client.query(UserQuery(Input.fromNullable(id)))
                .toBuilder()
                .requestHeaders(
                    RequestHeaders
                        .builder()
                        .addHeader("Authorization", idToken)
                        .build()
                )
                .build()
                .await()
            if (!response.hasErrors()) {
                val data = response.data
                if (data?.user != null) {
                    val userData = data.user
                    if (userData._id != null) {
                        return User(
                            _id = userData._id,
                            email = userData.email,
                            fcmRegistrationToken = userData.fcmRegistrationToken,
                            notificationsEnabled = userData.notificationsEnabled,
                        )
                    } else {
                        throw IOException("User data contained in response contained no _id field")
                    }
                } else {
                    throw IOException("Received response contained no data or user")
                }
            } else {
                response.errors?.forEach { error ->
                    Log.d(tag, error.message)
                }
                throw IOException("Received response contained error(s): ${response.errors?.joinToString { e -> e.message }}")
            }
        } catch (e: Throwable) {
            throw e
        }
    }

    suspend fun updateNotificationPreferences(id: String, notificationPrefsValue: Boolean): User {
        try {
            Log.d(tag, "updateNotificationPreferences called with value $notificationPrefsValue")
            val idToken = tokenResolver.fetchIdToken()
            val response = client.mutate(
                UpdateNotificationsPrefsMutation(
                    Input.fromNullable(id),
                    Input.fromNullable(notificationPrefsValue)
                )
            )
                .toBuilder()
                .requestHeaders(
                    RequestHeaders
                        .builder()
                        .addHeader("Authorization", idToken)
                        .build()
                )
                .build()
                .await()
            if (!response.hasErrors()) {
                val data = response.data
                if (data?.modifyUser != null) {
                    val userData = data.modifyUser
                    if (userData._id != null) {
                        Log.d(
                            tag,
                            "updateNotificationPreferences returned ${userData.notificationsEnabled}"
                        )
                        return User(
                            _id = userData._id,
                            email = userData.email,
                            fcmRegistrationToken = userData.fcmRegistrationToken,
                            notificationsEnabled = userData.notificationsEnabled,
                        )
                    } else {
                        throw IOException("User data contained in response contained no _id field")
                    }
                } else {
                    throw IOException("Received response contained no data or user")
                }
            } else {
                throw IOException("Received response contained error(s): ${response.errors?.joinToString { e -> e.message }}")
            }
        } catch (e: Throwable) {
            throw e
        }
    }

    suspend fun updateUserRegistrationToken(uid: String, token: String): User {
        try {
            val idToken = tokenResolver.fetchIdToken()
            val response = client.mutate(
                UpdateFcmTokenMutation(
                    Input.fromNullable(uid),
                    Input.fromNullable(token)
                )
            )
                .toBuilder()
                .requestHeaders(
                    RequestHeaders
                        .builder()
                        .addHeader("Authorization", idToken)
                        .build()
                )
                .build()
                .await()
            if (!response.hasErrors()) {
                val data = response.data
                if (data?.modifyUser != null) {
                    val userData = data.modifyUser
                    if (userData._id != null) {
                        return User(
                            _id = userData._id,
                            email = userData.email,
                            fcmRegistrationToken = userData.fcmRegistrationToken,
                            notificationsEnabled = userData.notificationsEnabled,
                        )
                    } else {
                        throw IOException("User data contained in response contained no _id field")
                    }
                } else {
                    throw IOException("Received response contained no data or user")
                }
            } else {
                throw IOException("Received response contained error(s): ${response.errors?.joinToString { e -> e.message }}")
            }
        } catch (e: Throwable) {
            throw e
        }
    }

    suspend fun setUserRegistrationTokenToNull(uid: String): User {
        try {
            val idToken = tokenResolver.fetchIdToken()
            val response = client.mutate(
                UpdateFcmTokenMutation(
                    Input.fromNullable(uid),
                    Input.fromNullable(null)
                )
            )
                .toBuilder()
                .requestHeaders(
                    RequestHeaders
                        .builder()
                        .addHeader("Authorization", idToken)
                        .build()
                )
                .build()
                .await()
            if (!response.hasErrors()) {
                val data = response.data
                if (data?.modifyUser != null) {
                    val userData = data.modifyUser
                    if (userData._id != null) {
                        return User(
                            _id = userData._id,
                            email = userData.email,
                            fcmRegistrationToken = userData.fcmRegistrationToken,
                            notificationsEnabled = userData.notificationsEnabled,
                        )
                    } else {
                        throw IOException("User data contained in response contained no _id field")
                    }
                } else {
                    throw IOException("Received response contained no data or user")
                }
            } else {
                throw IOException("Received response contained error(s): ${response.errors?.joinToString { e -> e.message }}")
            }
        } catch (e: Throwable) {
            throw e
        }
    }

}