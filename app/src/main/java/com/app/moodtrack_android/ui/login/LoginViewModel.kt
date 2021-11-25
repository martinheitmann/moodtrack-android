package com.app.moodtrack_android.ui.login

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.app.moodtrack_android.model.user.User
import com.app.moodtrack_android.repository.AuthRepository
import com.app.moodtrack_android.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel
@Inject constructor(
    application: Application,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : AndroidViewModel(application) {

    private val viewModelJob = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    val errorMessage = MutableLiveData<String?>()
    val isLoading = MutableLiveData(false)

    fun authenticateWithEmailAndPassword(
        email: String,
        password: String,
        onSuccess: (User) -> Unit
    ) {
        uiScope.launch {
            try {
                isLoading.postValue(true)
                val res = authRepository.signInWithEmailAndPassword(email, password)
                if (!res.hasError() && res.success != null) {
                    val uid = res.success as String
                    val userForUid = userRepository.fetchUserOnce(uid)
                    if (userForUid != null) {
                        onSuccess(userForUid)
                    } else {
                        errorMessage.postValue("Noe galt skjedde under henting av brukerkonto. Prøv igjen senere eller kontakt administrator.")
                    }
                } else {
                    errorMessage.postValue("Feil brukernavn eller passord.")
                }
            } catch (error: Throwable) {
                Log.d("LoginViewModel", error.stackTraceToString())
            } finally {
                isLoading.postValue(false)
            }
        }
    }
}