package com.example.greenfocus.ui.screen.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.greenfocus.data.repository.AuthRepository

class AuthViewModel(private val repository: AuthRepository = AuthRepository()) : ViewModel() {
    // Trạng thái hiển thị trên màn hình
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    fun register(email: String, pass: String, name: String, onSuccess: () -> Unit) {
//        isLoading = true
//        repository.signUp(email, pass, name) { success, error ->
//            isLoading = false
//            if (success) onSuccess() else errorMessage = error
//        }
        onSuccess()
    }
}