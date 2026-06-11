package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainLayout(viewModel: MainViewModel) {
    val activeUserKey by viewModel.activeUserKey.collectAsState()
    val user by viewModel.currentUserModel.collectAsState()

    // Host custom toast messages
    var toastMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val showToast: (String) -> Unit = { msg ->
        coroutineScope.launch {
            toastMessage = msg
            delay(3500)
            if (toastMessage == msg) {
                toastMessage = null
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C10)),
        topBar = {
            if (activeUserKey != null && user != null) {
                // Sticky Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF14141C))
                        .border(width = 1.dp, color = Color(0xFF2A2A3E))
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Title / Logo
                        Column {
                            Text(
                                text = "КОМАНДНА ЗП",
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color(0xFF8B78FF), Color(0xFF34D47A))
                                    )
                                )
                            )
                            Text(
                                text = if (user?.role == "admin") "Кабінет Адміністратора" else "Особистий кабінет",
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                        }

                        // User profile + logout button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .background(
                                        color = if (user?.role == "admin") Color(0xFF241020) else Color(0xFF102024),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (user?.role == "admin") Color(0xFF8B1F52) else Color(0xFF2A2A3E),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (user?.role == "admin") Icons.Default.Security else Icons.Default.Face,
                                    contentDescription = "Користувач",
                                    tint = if (user?.role == "admin") Color(0xFFE91E63) else Color(0xFF34D47A),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = user?.displayName ?: activeUserKey ?: "",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 120.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    viewModel.logout()
                                    showToast("Ви вийшли з облікового запису")
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = "Вийти",
                                    tint = Color(0xFFF05C52)
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Keep room safe or add subtext
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0C0C10))
                .padding(innerPadding)
        ) {
            // Router Page Screen Transitions with smooth fade transitions
            AnimatedContent(
                targetState = activeUserKey,
                transitionSpec = {
                    fadeIn() with fadeOut()
                },
                label = "ScreenTransition"
            ) { stateKey ->
                if (stateKey == null) {
                    LoginScreen(
                        onLoginSuccess = { user, pass ->
                            val err = viewModel.login(user, pass)
                            if (err != null) {
                                showToast(err)
                            } else {
                                showToast("Вітаємо, ${user}!")
                            }
                        }
                    )
                } else {
                    val currentRole = user?.role ?: "worker"
                    if (currentRole == "admin") {
                        AdminScreen(viewModel = viewModel, onShowToast = showToast)
                    } else {
                        WorkerScreen(viewModel = viewModel, onShowToast = showToast)
                    }
                }
            }

            // Beautiful Custom floating Snackbar (Toast implementation)
            AnimatedVisibility(
                visible = toastMessage != null,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .border(1.dp, Color(0xFF8B78FF), shape = RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Інформація",
                            tint = Color(0xFF8B78FF),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = toastMessage ?: "",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
