package com.example.greenfocus.ui.screen.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.greenfocus.GreenFocusApp
import com.example.greenfocus.data.repository.AuthRepository
import com.example.greenfocus.data.repository.SessionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Single source of truth for whether the app should show the authenticated flow.
 *
 * Resolves the cached Firebase session against the backend on startup, and keeps
 * reacting to auth-state changes so a revoked/expired session always falls back to login.
 */
sealed interface SessionState {
    /** Validation in progress; show the splash/opening screen. */
    object Checking : SessionState
    object Authenticated : SessionState
    data class Unauthenticated(val reason: String? = null) : SessionState
}

class SessionViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow<SessionState>(SessionState.Checking)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    init {
        validateSession()
        observeAuthState()
    }

    /** Validates the cached session and routes to the correct flow. Safe to re-invoke (e.g. retry). */
    fun validateSession() {
        _state.value = SessionState.Checking
        viewModelScope.launch {
            _state.value = when (val result = authRepository.validateSession()) {
                is SessionResult.Valid -> SessionState.Authenticated
                is SessionResult.Invalid -> {
                    Log.d(TAG, "Session invalid: ${result.reason}")
                    SessionState.Unauthenticated(result.reason)
                }
            }
        }
    }

    /** Called by the auth flow once a login/registration succeeds. */
    fun onAuthenticated() {
        _state.value = SessionState.Authenticated
    }

    /** Called on explicit logout; the auth-state listener also covers token revocation. */
    fun onSignedOut() {
        authRepository.signOut()
        _state.value = SessionState.Unauthenticated()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            // drop(1): skip the current state emitted on subscribe; we only act on real transitions
            // so we don't fight the in-flight startup validation.
            authRepository.authStateFlow().drop(1).collect { isSignedIn ->
                if (!isSignedIn && _state.value is SessionState.Authenticated) {
                    Log.d(TAG, "Auth state lost while authenticated; redirecting to login")
                    _state.value = SessionState.Unauthenticated()
                }
            }
        }
    }

    companion object {
        private const val TAG = "SessionViewModel"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as GreenFocusApp
                SessionViewModel(application.container.authRepository)
            }
        }
    }
}