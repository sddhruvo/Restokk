package com.inventory.app.data.repository

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val analyticsRepository: AnalyticsRepository
) {
    val currentUser: FirebaseUser? get() = auth.currentUser

    /** Emits the current user (or null) whenever auth state changes. */
    val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /** Sign in anonymously — creates a UID without requiring any user interaction. */
    suspend fun signInAnonymously(): Result<FirebaseUser> {
        return try {
            val result = auth.signInAnonymously().await()
            val user = result.user ?: return Result.failure(Exception("Anonymous sign-in returned null user"))
            analyticsRepository.setUserId(user.uid)
            analyticsRepository.logSignIn("anonymous")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Link or sign in with Google credential.
     * If the user is currently anonymous, links the Google account to preserve their data.
     * If not signed in, does a fresh Google sign-in.
     */
    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val currentUser = auth.currentUser

            val result = if (currentUser != null && currentUser.isAnonymous) {
                // Link Google to anonymous account — preserves UID and data
                currentUser.linkWithCredential(credential).await()
            } else {
                auth.signInWithCredential(credential).await()
            }

            val user = result.user ?: return Result.failure(Exception("Google sign-in returned null user"))
            analyticsRepository.setUserId(user.uid)
            analyticsRepository.logSignIn("google")
            Result.success(user)
        } catch (e: Exception) {
            // If linking fails (e.g., Google account already linked to another UID),
            // fall back to regular sign-in
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val result = auth.signInWithCredential(credential).await()
                val user = result.user ?: return Result.failure(Exception("Google sign-in fallback returned null user"))
                analyticsRepository.setUserId(user.uid)
                analyticsRepository.logSignIn("google")
                Result.success(user)
            } catch (fallbackError: Exception) {
                Result.failure(fallbackError)
            }
        }
    }

    fun signOut() {
        analyticsRepository.logSignOut()
        analyticsRepository.setUserId(null)
        // Sign out from both Firebase and Google Sign-In client (clears cached account)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(context, gso).signOut()
        auth.signOut()
    }

    /** Ensure user has a UID — sign in anonymously if not already signed in. */
    suspend fun ensureAuthenticated(): FirebaseUser {
        auth.currentUser?.let { return it }
        val result = signInAnonymously()
        return result.getOrThrow()
    }
}
