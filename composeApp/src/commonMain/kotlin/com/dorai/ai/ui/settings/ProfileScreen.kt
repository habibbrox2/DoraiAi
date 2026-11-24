package com.dorai.ai.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.auth_default_user
import dorai.composeapp.generated.resources.auth_email
import dorai.composeapp.generated.resources.auth_error_sign_out
import dorai.composeapp.generated.resources.auth_full_name
import dorai.composeapp.generated.resources.auth_profile
import dorai.composeapp.generated.resources.auth_save
import dorai.composeapp.generated.resources.auth_sign_out
import dorai.composeapp.generated.resources.auth_welcome_user
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Profile sheet for a signed-in user; the display name can be edited locally. */
@Composable
fun ProfileScreen(
    viewModel: AuthViewModel,
    onSignOut: () -> Unit,
) {
    val userEmail by viewModel.userEmail.collectAsState()
    val userDisplayName by viewModel.userDisplayName.collectAsState()
    val isBusy by viewModel.isBusy.collectAsState()

    var displayName by remember(userDisplayName) { mutableStateOf(userDisplayName) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val signOutError = stringResource(Res.string.auth_error_sign_out)
    val defaultUser = stringResource(Res.string.auth_default_user)
    val welcomeName = if (displayName.isNotBlank()) displayName else defaultUser

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.auth_profile),
            style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(vertical = 8.dp),
        )

        Text(
            text = stringResource(Res.string.auth_welcome_user, welcomeName),
            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(vertical = 8.dp),
        )

        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text(stringResource(Res.string.auth_full_name)) },
            placeholder = { Text(stringResource(Res.string.auth_full_name)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isBusy,
        )

        OutlinedTextField(
            value = userEmail,
            onValueChange = {},
            label = { Text(stringResource(Res.string.auth_email)) },
            placeholder = { Text(stringResource(Res.string.auth_email)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = false, // email is read-only
        )

        if (message != null) {
            Text(
                text = message!!,
                style = TextStyle(color = Color.Red),
                modifier = Modifier.padding(8.dp),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedButton(
                onClick = { viewModel.updateDisplayName(displayName) },
                enabled = !isBusy,
            ) {
                Text(stringResource(Res.string.auth_save))
            }
            OutlinedButton(
                onClick = {
                    scope.launch {
                        viewModel.signOut()
                            .onFailure { message = signOutError }
                        onSignOut()
                    }
                },
                enabled = !isBusy,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
            ) {
                Text(stringResource(Res.string.auth_sign_out))
            }
        }
    }
}
