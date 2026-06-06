package com.example.greenfocus.fcm

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.example.greenfocus.di.FirebaseModule
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Handles fetching the current FCM token, uploading it to Firestore under the authenticated user's profile,
 * and deleting it upon user logout to secure push notifications on multi-device systems.
 */
object FcmTokenManager {
    private const val TAG = "FcmTokenManager"

    /**
     * Fetches the current FCM token and updates it in Firestore if the user is authenticated.
     */
    fun updateTokenInFirestore(context: Context) {
        val uid = FirebaseModule.auth.currentUser?.uid
        if (uid == null) {
            Log.d(TAG, "No user is currently authenticated. Skipping FCM token update.")
            return
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            if (!token.isNullOrBlank()) {
                saveTokenToFirestore(context, uid, token)
            }
        }
    }

    /**
     * Saves a specific token to the user's fcmTokens subcollection in Firestore.
     */
    fun saveTokenToFirestore(context: Context, uid: String, token: String) {
        val db = FirebaseModule.firestore
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
        
        val tokenDocRef = db.collection("users")
            .document(uid)
            .collection("fcmTokens")
            .document(token)

        val tokenData = hashMapOf(
            "token" to token,
            "platform" to "android",
            "updatedAt" to FieldValue.serverTimestamp(),
            "deviceId" to deviceId
        )

        tokenDocRef.set(tokenData, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "FCM Token successfully synced to Firestore for user: $uid")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to sync FCM Token to Firestore", e)
            }
    }

    /**
     * Deletes the current device's FCM token from the Firestore database (used upon user logout).
     */
    fun deleteTokenFromFirestore(context: Context) {
        val uid = FirebaseModule.auth.currentUser?.uid ?: return
        
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Failed to get token for deletion", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            if (!token.isNullOrBlank()) {
                val db = FirebaseModule.firestore
                db.collection("users")
                    .document(uid)
                    .collection("fcmTokens")
                    .document(token)
                    .delete()
                    .addOnSuccessListener {
                        Log.d(TAG, "FCM Token successfully deleted from Firestore.")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to delete FCM Token from Firestore", e)
                    }
            }
        }
    }
}
