package com.example.greenfocus.ui.screen.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.greenfocus.GreenFocusApp
import com.example.greenfocus.data.repository.AuthRepository

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    fun register(email: String, pass: String, name: String, onSuccess: () -> Unit) {
        if (email.isEmpty() || pass.isEmpty() || name.isEmpty()) {
            errorMessage = "Vui lòng nhập đầy đủ thông tin"
            return
        }

        isLoading = true
        errorMessage = null
        repository.signUp(email, pass, name) { success, error ->
            isLoading = false
            if (success) onSuccess() else errorMessage = error
        }
    }

    fun login(email: String, pass: String, onSuccess: () -> Unit) {
        if (email.isEmpty() || pass.isEmpty()) {
            errorMessage = "Vui lòng nhập đầy đủ thông tin"
            return
        }

        isLoading = true
        errorMessage = null
        repository.login(email, pass) { success, error ->
            isLoading = false
            if (success) {
                onSuccess()
            } else {
                errorMessage = error
            }
        }
    }

    fun clearErrors() {
        errorMessage = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as GreenFocusApp)
                val authRepository = application.container.authRepository
                AuthViewModel(repository = authRepository)
            }
        }
    }
}