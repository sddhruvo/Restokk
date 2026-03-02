package com.inventory.app.ui.components

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import com.inventory.app.ui.components.ThemedAlertDialog
import com.inventory.app.ui.components.ThemedButton
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.inventory.app.R
import com.inventory.app.data.repository.AuthRepository
import com.inventory.app.ui.theme.appColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiSignInViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val isSignedIn: StateFlow<Boolean> = authRepository.authStateFlow
        .map { user -> user != null && !user.isAnonymous }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            authRepository.currentUser?.let { !it.isAnonymous } ?: false
        )

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = authRepository.signInWithGoogle(idToken)
            _isLoading.value = false
            if (result.isSuccess) {
                onSuccess()
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Sign-in failed"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}

class AiSignInGateState(
    val requireSignIn: (featureDescription: String, onAuthorized: () -> Unit) -> Unit
)

@Composable
fun rememberAiSignInGate(): AiSignInGateState {
    val viewModel: AiSignInViewModel = hiltViewModel()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    val showDialogState = remember { mutableStateOf(false) }
    val pendingActionState = remember { mutableStateOf<(() -> Unit)?>(null) }
    val featureDescState = remember { mutableStateOf("") }

    // Google Sign-In launcher (must be registered at composition level)
    val googleSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            val token = account.idToken
            if (token != null) {
                viewModel.signInWithGoogle(token) {
                    val action = pendingActionState.value
                    showDialogState.value = false
                    pendingActionState.value = null
                    action?.invoke()
                }
            }
        } catch (e: ApiException) {
            Log.e("AiSignInGate", "Google sign-in failed: status=${e.statusCode}", e)
            // 12501 = user cancelled — don't show error
        }
    }

    // Dialog
    if (showDialogState.value) {
        ThemedAlertDialog(
            onDismissRequest = {
                if (!isLoading) {
                    showDialogState.value = false
                    pendingActionState.value = null
                    viewModel.clearError()
                }
            },
            icon = {
                ThemedIcon(
                    materialIcon = Icons.Filled.AutoAwesome,
                    inkIconRes = R.drawable.ic_ink_sparkle,
                    contentDescription = null,
                    tint = MaterialTheme.appColors.accentGold,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "Sign in to use AI",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "This feature uses AI to ${featureDescState.value}. Sign in with Google to continue.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isLoading) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ThemedCircularProgress(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Text("Signing in...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    error?.let { err ->
                        Text(
                            err,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                ThemedButton(
                    onClick = {
                        try {
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken(context.getString(R.string.default_web_client_id))
                                .requestEmail()
                                .build()
                            val client = GoogleSignIn.getClient(context, gso)
                            googleSignInLauncher.launch(client.signInIntent)
                        } catch (e: Exception) {
                            Log.e("AiSignInGate", "Failed to launch Google sign-in", e)
                        }
                    },
                    enabled = !isLoading
                ) {
                    Text("Sign in with Google")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDialogState.value = false
                        pendingActionState.value = null
                        viewModel.clearError()
                    },
                    enabled = !isLoading
                ) {
                    Text("Not now")
                }
            }
        )
    }

    return AiSignInGateState { description, onAuthorized ->
        if (viewModel.isSignedIn.value) {
            onAuthorized()
        } else {
            featureDescState.value = description
            pendingActionState.value = onAuthorized
            showDialogState.value = true
        }
    }
}
