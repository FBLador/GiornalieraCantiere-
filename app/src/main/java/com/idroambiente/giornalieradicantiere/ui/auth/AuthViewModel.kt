package com.idroambiente.giornalieradicantiere.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idroambiente.giornalieradicantiere.data.auth.AuthRepository
import com.idroambiente.giornalieradicantiere.data.auth.AuthUser
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val user: StateFlow<AuthUser?> = authRepository.user
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    var name by mutableStateOf("")
        private set

    var pin by mutableStateOf("")
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun updateName(value: String) {
        name = value
    }

    fun updatePin(value: String) {
        pin = value
    }

    fun register(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            val trimmedPin = pin.trim()
            if (trimmedName.isBlank() || trimmedPin.isBlank()) {
                errorMessage = "Inserisci nome e PIN."
                return@launch
            }
            authRepository.registerUser(trimmedName, trimmedPin)
            errorMessage = null
            pin = ""
            onSuccess()
        }
    }

    fun login(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val trimmedPin = pin.trim()
            if (trimmedPin.isBlank()) {
                errorMessage = "Inserisci il PIN."
                return@launch
            }
            val isValid = authRepository.verifyPin(trimmedPin)
            if (isValid) {
                errorMessage = null
                pin = ""
                onSuccess()
            } else {
                errorMessage = "PIN non valido."
            }
        }
    }
}
