package com.example.ui.navigation

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NotificationPriority
import com.example.data.model.SessionRole
import com.example.ui.admin.AdminHubScreen
import com.example.ui.admin.SuperAdminHubScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.parent.ParentHubScreen
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate900
import com.example.ui.theme.StatusSAM
import com.example.ui.viewmodel.PoshanViewModel
import kotlinx.coroutines.delay

@Composable
fun PoshanAppScaffold(
    viewModel: PoshanViewModel,
    modifier: Modifier = Modifier
) {
    val session by viewModel.userSession.collectAsState()
    val language by viewModel.appLanguage.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()
    val latestNotification by viewModel.latestReceivedNotification.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Request notification permission for Android 13+ (API 33+) so parent devices receive alerts
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { /* permission callback */ }

        LaunchedEffect(Unit) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Intercept back button when logged in to prevent app exit ("auto back")
    BackHandler(enabled = session.role != SessionRole.GUEST) {
        viewModel.logout()
    }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    // Auto-dismiss real-time popup banner after 7 seconds
    LaunchedEffect(latestNotification) {
        if (latestNotification != null) {
            delay(7000)
            viewModel.dismissLatestNotificationBanner()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = session.role, label = "role_transition") { role ->
                when (role) {
                    SessionRole.GUEST -> {
                        AuthScreen(viewModel = viewModel)
                    }
                    SessionRole.ADMIN -> {
                        AdminHubScreen(viewModel = viewModel)
                    }
                    SessionRole.PARENT -> {
                        ParentHubScreen(viewModel = viewModel)
                    }
                    SessionRole.SUPER_ADMIN -> {
                        SuperAdminHubScreen(viewModel = viewModel)
                    }
                }
            }

            // Real-Time Heads-Up In-App Notification Banner (Slides down when admin sends alert)
            AnimatedVisibility(
                visible = latestNotification != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                latestNotification?.let { notif ->
                    val isUrgent = notif.priority == NotificationPriority.URGENT || notif.priority == NotificationPriority.HIGH
                    val notifTitle = when (language) {
                        AppLanguage.MARATHI -> notif.titleMr
                        AppLanguage.HINDI -> notif.titleHi
                        AppLanguage.ENGLISH -> notif.titleEn
                    }
                    val notifMsg = when (language) {
                        AppLanguage.MARATHI -> notif.messageMr
                        AppLanguage.HINDI -> notif.messageHi
                        AppLanguage.ENGLISH -> notif.messageEn
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (isUrgent) StatusSAM else PrimaryIndigo,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                viewModel.markNotificationAsRead(notif.id)
                                viewModel.dismissLatestNotificationBanner()
                            }
                            .testTag("in_app_notification_banner"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (isUrgent) Color(0xFFFEE2E2) else Color(0xFFEFF6FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Notification",
                                    tint = if (isUrgent) StatusSAM else PrimaryIndigo,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = notif.senderName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUrgent) StatusSAM else PrimaryIndigo
                                    )
                                    Text(
                                        text = "LIVE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isUrgent) StatusSAM else PrimaryIndigo)
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }

                                Text(
                                    text = notifTitle,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = notifMsg,
                                    fontSize = 12.sp,
                                    color = Slate600,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = { viewModel.dismissLatestNotificationBanner() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = Slate600,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
