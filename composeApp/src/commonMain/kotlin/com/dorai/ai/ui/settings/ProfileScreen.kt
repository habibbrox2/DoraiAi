package com.dorai.ai.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.TextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextStyle
import com.dorai.ai.data.AppSettings

@Composable
fun ProfileScreen(
    viewModel: AuthViewModel,
    onSignOut: () -> Unit,
) {
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    // Initialize from view model state
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val userDisplayName by viewModel.userDisplayName.collectAsState()

    // Sync local state with view model on composition start
    DisposableEffect(isLoggedIn) {
        if (isLoggedIn != null) {
            displayName = userDisplayName.valueOrNull() ?: ""
            email = userEmail.valueOrNull() ?: ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        topAppBar = topAppBar {
            MaterialTheme.topAppBar(
                elevation = 4.dp,
                colors = MaterialTheme.colors,
                title = {
                    Text(
                        text = "Profile",
                        style = TextStyle(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                },
                supportingIcon = {
                    Icon(
                        imageVector = androidx.compose.material.icons.Filled.Person,
                        contentDescription = stringResource(
                            R.string.user_profile_content_description
                        ),
                    )
                },
            )
        }

        Text(
            text = "Welcome, ${displayName.capitalize() or "User"}",
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            modifier = Modifier.padding(vertical = 8.dp),
            alignment = Alignment.CenterHorizontally,
        )

        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Full name") },
            placeholder = { Text("Enter your full name") },
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email address") },
            placeholder = { Text("Enter your email") },
            modifier = Modifier.fillMaxWidth(),
            enabled = false, // email is read‑only
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            arrangement = Arrangement.End,
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            OutlinedButton(
                onClick = onSignOut,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.Error,
                ),
            ) {
                Text("Sign Out", modifier = Modifier.align(Alignment.CenterHorizontal))
            }
        }
    }
}