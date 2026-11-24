package com.dorai.ai.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.auth_email
import dorai.composeapp.generated.resources.auth_error_credentials_required
import dorai.composeapp.generated.resources.auth_error_invalid_credentials
import dorai.composeapp.generated.resources.auth_error_network
import dorai.composeapp.generated.resources.auth_full_name
import dorai.composeapp.generated.resources.auth_login
import dorai.composeapp.generated.resources.auth_password
import dorai.composeapp.generated.resources.auth_view_profile
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Optional sign-in screen: the rest of the app remains usable without an account. */
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToProfile: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val isBusy by viewModel.isBusy.collectAsState()

    val credentialsError = stringResource(Res.string.auth_error_credentials_required)
    val invalidCredentialsError = stringResource(Res.string.auth_error_invalid_credentials)
    val networkError = stringResource(Res.string.auth_error_network)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "DORAI AI",
            style = TextStyle(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Blue,
            ),
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(Res.string.auth_email)) },
            placeholder = { Text(stringResource(Res.string.auth_email)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isBusy,
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(Res.string.auth_password)) },
            placeholder = { Text(stringResource(Res.string.auth_password)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isBusy,
            visualTransformation = PasswordVisualTransformation(),
        )

        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text(stringResource(Res.string.auth_full_name)) },
            placeholder = { Text(stringResource(Res.string.auth_full_name)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isBusy,
        )

        if (message != null) {
            Text(
                text = message!!,
                style = TextStyle(color = Color.Red),
                modifier = Modifier.padding(8.dp),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    message = credentialsError
                } else {
                    scope.launch {
                        viewModel.signIn(email, password, displayName)
                            .onSuccess { onLoginSuccess() }
                            .onFailure { error ->
                                val text = error.message.orEmpty()
                                message = if (text.contains("[401") || text.contains("[400")) {
                                    invalidCredentialsError
                                } else {
                                    networkError
                                }
                            }
                    }
                }
            },
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.auth_login))
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onNavigateToProfile,
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.auth_view_profile))
        }
    }
}
