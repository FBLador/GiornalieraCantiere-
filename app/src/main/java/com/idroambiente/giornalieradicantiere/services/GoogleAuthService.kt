package com.idroambiente.giornalieradicantiere.services

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service responsible for Google Sign-In authentication.
 * Uses Google Sign-In API with Drive.FILE scope for limited access.
 */
@Singleton
class GoogleAuthService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: Flow<Boolean> = _isSignedIn.asStateFlow()

    private val _currentAccount = MutableStateFlow<GoogleSignInAccount?>(null)
    val currentAccount: Flow<GoogleSignInAccount?> = _currentAccount.asStateFlow()

    private val googleSignInClient: GoogleSignInClient by lazy {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                Scope(DriveScopes.DRIVE_READONLY),
                Scope(DriveScopes.DRIVE_FILE)
            )
            .build()
        GoogleSignIn.getClient(context, signInOptions)
    }

    init {
        // Check if user is already signed in
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null && hasRequiredScopes(account)) {
            _currentAccount.value = account
            _isSignedIn.value = true
        }
    }

    /**
     * Returns the Intent to launch Google Sign-In flow.
     * Use with ActivityResultContracts.StartActivityForResult
     */
    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    /**
     * Handles the result from Google Sign-In activity.
     * @param data The Intent data from onActivityResult
     * @return Result containing the signed-in account or error
     */
    suspend fun handleSignInResult(data: Intent?): Result<GoogleSignInAccount> {
        return withContext(Dispatchers.IO) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.await()
                
                if (hasRequiredScopes(account)) {
                    _currentAccount.value = account
                    _isSignedIn.value = true
                    Result.success(account)
                } else {
                    Result.failure(Exception("Permessi Drive non concessi"))
                }
            } catch (e: Exception) {
                _isSignedIn.value = false
                _currentAccount.value = null
                Result.failure(e)
            }
        }
    }

    /**
     * Gets the current signed-in account if available.
     * @return The current GoogleSignInAccount or null
     */
    fun getCurrentAccount(): GoogleSignInAccount? {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return if (account != null && hasRequiredScopes(account)) account else null
    }

    /**
     * Signs out the current user.
     */
    suspend fun signOut() {
        withContext(Dispatchers.IO) {
            try {
                googleSignInClient.signOut().await()
            } catch (e: Exception) {
                // Ignore sign out errors
            } finally {
                _isSignedIn.value = false
                _currentAccount.value = null
            }
        }
    }

    /**
     * Revokes access for the current user.
     * This removes all granted permissions.
     */
    suspend fun revokeAccess() {
        withContext(Dispatchers.IO) {
            try {
                googleSignInClient.revokeAccess().await()
            } catch (e: Exception) {
                // Ignore revoke errors
            } finally {
                _isSignedIn.value = false
                _currentAccount.value = null
            }
        }
    }

    /**
     * Checks if the account has the required Drive scopes.
     */
    private fun hasRequiredScopes(account: GoogleSignInAccount): Boolean {
        return GoogleSignIn.hasPermissions(
            account,
            Scope(DriveScopes.DRIVE_READONLY),
            Scope(DriveScopes.DRIVE_FILE)
        )
    }
}
