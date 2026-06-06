package com.example.greenfocus.data.repository

import android.util.Log
import com.example.greenfocus.data.model.User
import com.example.greenfocus.di.FirebaseModule
import com.example.greenfocus.util.FirestoreCollections
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Result of validating a (possibly cached) Firebase session against the backend.
 * Used to decide whether the app should land on the main flow or the auth flow.
 */
sealed interface SessionResult {
    object Valid : SessionResult
    data class Invalid(val reason: String) : SessionResult
}

interface AuthRepository {
    /** UID of the locally cached user, or null when no session exists. */
    val currentUserId: String?

    fun signUp(email: String, password: String, name: String, onResult: (Boolean, String?) -> Unit)
    fun login(email: String, pass: String, onResult: (isSuccess: Boolean, errorMessage: String?) -> Unit)
    fun signOut()

    /**
     * Confirms the cached session still maps to a real account + profile on the server.
     * This catches the "stale session" case where a local token survives a backend reset.
     */
    suspend fun validateSession(): SessionResult

    /** Emits true/false whenever the auth state changes (e.g. sign-in, sign-out, token revoked). */
    fun authStateFlow(): Flow<Boolean>
}

class ProdAuthRepository : AuthRepository {
    private val auth = FirebaseModule.auth
    private val db = FirebaseModule.firestore

    override val currentUserId: String?
        get() = auth.currentUser?.uid

    override fun signUp(email: String, password: String, name: String, onResult: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val firebaseUser = result.user
                if (firebaseUser == null) {
                    onResult(false, "Khong the tao tai khoan. Vui long thu lai.")
                    return@addOnSuccessListener
                }

                val newUser = User(uid = firebaseUser.uid, email = email, displayName = name)
                db.collection(FirestoreCollections.USERS).document(firebaseUser.uid).set(newUser)
                    .addOnSuccessListener { onResult(true, null) }
                    .addOnFailureListener { e ->
                        // Roll back the orphaned auth account so the user can retry from a clean state,
                        // instead of being left with a login that has no profile behind it.
                        Log.e(TAG, "Profile creation failed, rolling back auth account", e)
                        firebaseUser.delete()
                        onResult(false, "Tao ho so that bai: ${mapAuthError(e)}")
                    }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Sign up failed", e)
                onResult(false, mapAuthError(e))
            }
    }

    override fun login(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                val firebaseUser = result.user
                if (firebaseUser == null) {
                    onResult(false, "Khong the dang nhap. Vui long thu lai.")
                    return@addOnSuccessListener
                }

                // Ensure a Firestore profile exists for this auth user. If missing,
                // sign out the account and return an error so the app doesn't proceed
                // with a half-baked session.
                db.collection(FirestoreCollections.USERS).document(firebaseUser.uid).get()
                    .addOnSuccessListener { doc ->
                        if (doc.exists()) {
                            onResult(true, null)
                        } else {
                            Log.w(TAG, "Login succeeded but user profile missing; signing out")
                            auth.signOut()
                            onResult(false, "Khong tim thay ho so nguoi dung. Vui long dang ky.")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Failed to verify user profile after login", e)
                        auth.signOut()
                        onResult(false, e.localizedMessage ?: "Khong the xac thuc ho so nguoi dung")
                    }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Login failed", e)
                onResult(false, mapAuthError(e))
            }
    }

    override fun signOut() {
        auth.signOut()
    }

    override suspend fun validateSession(): SessionResult {
        val user = auth.currentUser ?: return SessionResult.Invalid("Chua dang nhap")

        return try {
            // Forces a round-trip; throws FirebaseAuthInvalidUserException if the account
            // was deleted or disabled on the server while a local token lingered.
            user.reload().await()
            val uid = auth.currentUser?.uid
                ?: return SessionResult.Invalid("Phien dang nhap da het han")

            try {
                val profile = db.collection(FirestoreCollections.USERS).document(uid).get().await()
                if (profile.exists()) {
                    SessionResult.Valid
                } else {
                    // Account exists but its profile is gone (e.g. Firestore was reset). Treat as logged out.
                    Log.w(TAG, "Auth account valid but profile missing; signing out")
                    auth.signOut()
                    SessionResult.Invalid("Khong tim thay ho so nguoi dung")
                }
            } catch (e: Exception) {
                // Profile lookup failed for a non-auth reason (offline/Firestore hiccup).
                // The account itself is valid, so keep the session rather than locking the user out.
                Log.w(TAG, "Profile check failed; keeping validated session", e)
                SessionResult.Valid
            }
        } catch (e: FirebaseAuthInvalidUserException) {
            Log.w(TAG, "Cached session points to a deleted/disabled account; signing out", e)
            auth.signOut()
            SessionResult.Invalid("Tai khoan khong con ton tai")
        } catch (e: FirebaseNetworkException) {
            // Offline with a cached token: stay optimistic and keep the user signed in.
            Log.w(TAG, "Network unavailable during session validation; keeping cached session", e)
            SessionResult.Valid
        } catch (e: Exception) {
            Log.e(TAG, "Session validation failed", e)
            SessionResult.Invalid(e.localizedMessage ?: "Xac thuc phien that bai")
        }
    }

    override fun authStateFlow(): Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser != null)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    private fun mapAuthError(error: Exception): String = when (error) {
        is FirebaseAuthInvalidUserException ->
            "Tai khoan khong ton tai hoac da bi vo hieu hoa."
        is FirebaseAuthInvalidCredentialsException ->
            "Email hoac mat khau khong dung."
        is FirebaseAuthUserCollisionException ->
            "Email nay da duoc su dung."
        is FirebaseAuthWeakPasswordException ->
            "Mat khau qua yeu (toi thieu 6 ky tu)."
        is FirebaseNetworkException ->
            "Khong co ket noi mang. Vui long thu lai."
        else -> error.localizedMessage ?: "Da co loi xay ra. Vui long thu lai."
    }

    companion object {
        private const val TAG = "AuthRepository"
    }
}
