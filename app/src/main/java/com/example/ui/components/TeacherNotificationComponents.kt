package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppNotification
import com.example.data.model.NotificationPriority
import com.example.data.model.NotificationType
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.AppStrings
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.PrimaryIndigoContainer
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.StatusMAM
import com.example.ui.theme.StatusNormal
import com.example.ui.theme.StatusSAM

@Composable
fun TeacherNotificationsFeedCard(
    notifications: List<AppNotification>,
    unreadCount: Int,
    language: AppLanguage,
    onOpenAll: () -> Unit,
    onMarkAsRead: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, PrimaryIndigo.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .testTag("teacher_notifications_feed_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(PrimaryIndigoContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = "Alerts",
                            tint = PrimaryIndigo,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = AppStrings.notificationsTitle(language),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Slate900
                        )
                        Text(
                            text = "Rekha Tai Patil (Anganwadi Sevika)",
                            fontSize = 10.sp,
                            color = Slate600
                        )
                    }
                }

                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFEE2E2))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "$unreadCount new",
                            color = StatusSAM,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Slate50)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = AppStrings.noNotifications(language),
                        fontSize = 12.sp,
                        color = Slate600
                    )
                }
            } else {
                // Show up to 3 recent items
                val displayList = notifications.take(3)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    displayList.forEach { notif ->
                        NotificationSnippetCard(
                            notification = notif,
                            language = language,
                            onMarkAsRead = { onMarkAsRead(notif.id) }
                        )
                    }
                }

                if (notifications.size > 3) {
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = onOpenAll,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = if (language == AppLanguage.MARATHI)
                                "सर्व सूचना पहा (${notifications.size})"
                            else
                                "View all notifications (${notifications.size})",
                            color = PrimaryIndigo,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationSnippetCard(
    notification: AppNotification,
    language: AppLanguage,
    onMarkAsRead: () -> Unit
) {
    val isUrgent = notification.priority == NotificationPriority.URGENT || notification.priority == NotificationPriority.HIGH
    val title = when (language) {
        AppLanguage.MARATHI -> notification.titleMr
        AppLanguage.HINDI -> notification.titleHi
        AppLanguage.ENGLISH -> notification.titleEn
    }
    val message = when (language) {
        AppLanguage.MARATHI -> notification.messageMr
        AppLanguage.HINDI -> notification.messageHi
        AppLanguage.ENGLISH -> notification.messageEn
    }

    val typeIcon: ImageVector = when (notification.type) {
        NotificationType.MESSAGE -> Icons.Default.Mail
        NotificationType.ANNOUNCEMENT -> Icons.Default.Campaign
        NotificationType.RATION_SCHEDULE -> Icons.Default.Fastfood
        NotificationType.VACCINATION_ALERT -> Icons.Default.Vaccines
        NotificationType.NUTRITION_ALERT -> Icons.Default.NotificationsActive
        NotificationType.ATTENDANCE_UPDATE -> Icons.AutoMirrored.Filled.EventNote
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (!notification.isRead) Color(0xFFF8FAFC) else Color.White)
            .border(
                1.dp,
                if (!notification.isRead) PrimaryIndigo.copy(alpha = 0.35f) else Slate200,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onMarkAsRead)
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isUrgent) Color(0xFFFEE2E2) else Color(0xFFEFF6FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = typeIcon,
                    contentDescription = null,
                    tint = if (isUrgent) StatusSAM else PrimaryIndigo,
                    modifier = Modifier.size(15.dp)
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
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = notification.formattedTime,
                        fontSize = 10.sp,
                        color = Slate600
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = message,
                    fontSize = 11.sp,
                    color = Slate700,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!notification.isRead) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(PrimaryIndigo)
                )
            }
        }
    }
}

@Composable
fun NotificationTrayDialog(
    notifications: List<AppNotification>,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onMarkAsRead: (Long) -> Unit,
    onMarkAllAsRead: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = AppStrings.notificationsTitle(language),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }

                if (notifications.any { !it.isRead }) {
                    TextButton(onClick = onMarkAllAsRead) {
                        Text(
                            text = AppStrings.markAllRead(language),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryIndigo
                        )
                    }
                }
            }
        },
        text = {
            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = AppStrings.noNotifications(language),
                        fontSize = 13.sp,
                        color = Slate600
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notifications, key = { it.id }) { notif ->
                        val isUrgent = notif.priority == NotificationPriority.URGENT || notif.priority == NotificationPriority.HIGH
                        val title = when (language) {
                            AppLanguage.MARATHI -> notif.titleMr
                            AppLanguage.HINDI -> notif.titleHi
                            AppLanguage.ENGLISH -> notif.titleEn
                        }
                        val message = when (language) {
                            AppLanguage.MARATHI -> notif.messageMr
                            AppLanguage.HINDI -> notif.messageHi
                            AppLanguage.ENGLISH -> notif.messageEn
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    if (!notif.isRead) PrimaryIndigo.copy(alpha = 0.4f) else Slate200,
                                    RoundedCornerShape(12.dp)
                                ),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (!notif.isRead) Color(0xFFF8FAFC) else Color.White
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = notif.senderName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryIndigo
                                        )
                                        if (isUrgent) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFFFEE2E2))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = "URGENT",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = StatusSAM
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = notif.formattedTime,
                                        fontSize = 10.sp,
                                        color = Slate600
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = message,
                                    fontSize = 12.sp,
                                    color = Slate700,
                                    lineHeight = 17.sp
                                )

                                if (!notif.isRead) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        OutlinedButton(
                                            onClick = { onMarkAsRead(notif.id) },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (language == AppLanguage.MARATHI) "वाचले" else "Read",
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
            ) {
                Text(
                    text = if (language == AppLanguage.MARATHI) "बंद करा" else "Close",
                    fontSize = 12.sp
                )
            }
        }
    )
}
