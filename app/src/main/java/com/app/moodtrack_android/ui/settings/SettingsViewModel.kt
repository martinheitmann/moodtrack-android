package com.app.moodtrack_android.ui.settings

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.app.moodtrack_android.database.RequestResult
import com.app.moodtrack_android.model.user.User
import com.app.moodtrack_android.repository.UserRepository
import com.google.firebase.installations.FirebaseInstallations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
@Inject constructor(
    application: Application,
    private val userRepository: UserRepository
) : AndroidViewModel(application) {

    private val TAG = "SettingsViewModel"

    private val viewModelJob = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)
    val user: MutableLiveData<RequestResult> = userRepository.user

    var signOutPending: MutableLiveData<Boolean> = MutableLiveData(false)

    init {
        uiScope.launch {
            fetchUser()
        }
    }

    fun fetchUser() {
        uiScope.launch(Dispatchers.IO) {
            if (user.value == null) userRepository.fetchUser()
        }
    }

    fun removeUserFcmToken(onFinished: () -> Unit){
        uiScope.launch {
            try{
                signOutPending.value = true
                userRepository.removeUserRegistrationToken()
            } catch (e: Throwable){
                Log.d(TAG, e.stackTraceToString())
                FirebaseInstallations.getInstance().delete()
            } finally{
                signOutPending.value = false
                onFinished()
            }
        }
    }

    fun updateNotificationPrefs() {
        uiScope.launch {
            val mUser = user.value?.data
            if (mUser != null && mUser is User && mUser.notificationsEnabled != null) {
                userRepository.updateUserNotificationPreferences(mUser.notificationsEnabled)
            }
        }
    }
}