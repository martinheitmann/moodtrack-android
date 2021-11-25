package com.app.moodtrack_android.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.app.moodtrack_android.database.RequestResult
import com.app.moodtrack_android.graphql.GraphQLUserDao
import com.app.moodtrack_android.model.Status
import com.app.moodtrack_android.model.user.User
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
  private val graphQLUserDao: GraphQLUserDao,
  private val authRepository: AuthRepository
){


    val TAG = "UserRepository"
    val user = MutableLiveData<RequestResult>()

    /***
     * Called by the repository during initialization.
     */

    suspend fun fetchUserOnce(uid: String): User? {
        try {
            return graphQLUserDao.queryUser(uid)
        } catch(error: Throwable){
            Log.d(TAG, error.stackTraceToString())
            return null
        }
    }

    suspend fun fetchUser() {
        try {
            user.postValue(
                RequestResult(
                    data = null,
                    status = Status.LOADING,
                    hasError = false,
                    errorMessage = null
                )
            )
            val uid = authRepository.getUid()
            if(uid != null){
                val mUser = graphQLUserDao.queryUser(uid)
                user.postValue(
                    RequestResult(
                        data = mUser,
                        status = Status.SUCCESS,
                        hasError = false,
                        errorMessage = null
                    )
                )
            }
            else user.postValue(
                RequestResult(
                    data = null,
                    status = Status.ERROR,
                    hasError = true,
                    errorMessage = "Kan ikke hente brukerprofil uten en gyldig bruker-id."
                )
            )
        } catch(exception: Throwable){
            Log.d(TAG, "Fething user failed with error: ${exception.stackTraceToString()}")
            user.postValue(
                RequestResult(
                    data = null,
                    status = Status.ERROR,
                    hasError = true,
                    errorMessage = "En feil forekom under henting av bruker."
                )
            )
        }
    }

    suspend fun updateUserNotificationPreferences(value: Boolean){
        try {
            val uid = authRepository.getUid()
            if(uid != null){
                val mUser = graphQLUserDao.updateNotificationPreferences(uid, !value)
                user.postValue(
                    RequestResult(
                        data = mUser,
                        status = Status.SUCCESS,
                        hasError = false,
                        errorMessage = null
                    )
                )
            }
            else user.postValue(
                RequestResult(
                    data = null,
                    status = Status.ERROR,
                    hasError = true,
                    errorMessage = "Kan ikke hente brukerprofil uten en gyldig bruker-id."
                )
            )
        } catch(exception: Throwable){
            Log.d(TAG, "Operation failed with error: ${exception.stackTraceToString()}")
            user.postValue(
                RequestResult(
                    data = null,
                    status = Status.ERROR,
                    hasError = true,
                    errorMessage = "En feil forekom under henting av bruker."
                )
            )
        }
    }

    suspend fun removeUserRegistrationToken(){
        try {
            val uid = authRepository.getUid()
            if(uid != null){
                graphQLUserDao.setUserRegistrationTokenToNull(uid)
            }
        } catch (exception: Throwable){
            exception.printStackTrace()
        }
    }

    suspend fun updateUserRegistrationToken(token: String){
        try {
            val uid = authRepository.getUid()
            if(uid != null){
                graphQLUserDao.updateUserRegistrationToken(uid, token)
            }
        } catch (exception: Throwable){
            exception.printStackTrace()
        }
    }
}