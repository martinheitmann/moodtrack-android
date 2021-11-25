package com.app.moodtrack_android.ui.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.app.moodtrack_android.database.RequestResult
import com.app.moodtrack_android.model.user.User
import com.app.moodtrack_android.repository.AuthRepository
import com.app.moodtrack_android.repository.UserRepository
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Viewmodel representing the state of the home fragment.
 */
@HiltViewModel
class HomeViewModel
@Inject constructor(
    application: Application,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : AndroidViewModel(application) {
    private val TAG = "HomeViewModel"
    private val viewModelJob = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    val user: MutableLiveData<RequestResult> = userRepository.user

    // Fetch user on creation.
    init {
        fetchUser()
    }

    /**
     * Fetches the current user.
     */
    fun fetchUser() {
        uiScope.launch(Dispatchers.IO) {
            try {
                userRepository.fetchUser()
            } catch(e: Throwable){
                Log.d(TAG, e.stackTraceToString())
            }
        }
    }

    /** Checks if the current user is logged in. */
    fun isLoggedIn() = authRepository.isLoggedIn()

    /**
     * Updates the user's FCM registration token.
     * Checks first if the user's token matches the instance
     * token and updates accordingly.
     */
    fun updateRegistrationToken(user: User) {
        // Register callback in order to fetch token
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isComplete) {
                if (task.isSuccessful) {
                    task.result.let { token ->
                        Log.d("HomeViewModel", "Found FCM token $token.")
                        uiScope.launch {
                            // If the user's token doesn't match the instance token, update it.
                            if (token != null && token != user.fcmRegistrationToken) {
                                userRepository.updateUserRegistrationToken(token)
                            } else {
                                Log.d(
                                    "HomeViewModel",
                                    "User already has registration token, returning."
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}