package com.example.greenfocus.ui.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.greenfocus.data.model.User
import com.example.greenfocus.data.repository.ProdUserRepository
import com.example.greenfocus.data.repository.UserRepository
import com.example.greenfocus.di.FirebaseModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: UserRepository = ProdUserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            repository.getCurrentUserProfileFlow().collect { user ->
                if (user != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = user,
                        errorMessage = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Chưa đăng nhập hoặc không tìm thấy dữ liệu"
                    )
                }
            }
        }
    }
}

data class ProfileUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val errorMessage: String? = null
)
