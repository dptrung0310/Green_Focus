package com.example.greenfocus.ui.screen.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.greenfocus.data.repository.AuthRepository

class AuthViewModel(private val repository: AuthRepository = AuthRepository()) : ViewModel() {
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    fun register(email: String, pass: String, name: String, onSuccess: () -> Unit) {
        isLoading = true
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

}