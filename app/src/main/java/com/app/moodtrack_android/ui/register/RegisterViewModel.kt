package com.app.moodtrack_android.ui.register

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.apollographql.apollo.exception.ApolloNetworkException
import com.app.moodtrack_android.auth.AuthResult
import com.app.moodtrack_android.model.user.User
import com.app.moodtrack_android.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel
@Inject constructor(
    application: Application,
    private val authRepository: AuthRepository,
) : AndroidViewModel(application) {
    val TAG = "RegisterViewModel"
    val errorMessage = MutableLiveData<String?>(null)
    val isLoading = MutableLiveData(false)

    fun registerUserWithPasswordAndEmail(
        email: String,
        password: String,
        onSuccess: (AuthResult) -> Unit
    ) {
        Log.d("RegisterViewModel", "Attempting to register user with email $email")
        viewModelScope.launch {
            try {
                isLoading.postValue(true)
                val result = authRepository.registerWithEmailAndPassword(email, password)
                if (!result.hasError()) {
                    Log.d(TAG, "Register result contained no errors")
                    if (result.success != null && result.success is User) {
                        onSuccess(result)
                    }
                } else {
                    Log.d(TAG, "Attempt to register returned an exception")
                    val message = resolveExceptionMessage(result.error as Throwable)
                    errorMessage.postValue(message)
                }
            } catch (error: Throwable) {
                Log.d("RegisterViewModel", error.stackTraceToString())
            } finally {
                isLoading.postValue(false)
            }
        }
    }

    fun resolveExceptionMessage(exception: Throwable): String {
        if (exception is ApolloNetworkException) {
            return "En nettverksfeil forekom. Sjekk at du er tilkoblet et nettverk eller prøv igjen senere."
        }
        return "En feil forekom. Prøv igjen senere."
    }

}