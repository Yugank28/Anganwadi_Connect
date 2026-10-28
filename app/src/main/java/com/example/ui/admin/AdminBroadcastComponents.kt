package com.example.ui.admin

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppNotification
import com.example.data.model.ChildProfile
import com.example.data.model.NotificationPriority
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.AppStrings
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.PrimaryIndigoContainer
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.StatusMAM
import com.example.ui.theme.StatusNormal
import com.example.ui.theme.StatusSAM

@Composable
fun AdminBroadcastTab(
    titleInput: String,
    messageInput: String,
    priorityInput: NotificationPriority,
    targetChildId: String?,
    allChildren: List<ChildProfile>,
    recentNotifications: List<AppNotification>,
    language: AppLanguage,
    onTitleChange: (String) -> Unit,
    onMessageChange: (String) -> Unit,
    onPriorityChange: (NotificationPriority) -> Unit,
    onTargetChange: (String?) -> Unit,
    onSendBroadcast: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Broadcast Creation Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PrimaryIndigo.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .testTag("admin_broadcast_compose_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PrimaryIndigoContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = null,
                                tint = PrimaryIndigo,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = AppStrings.broadcastMessageTitle(language),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Slate900
                            )
                            Text(
                                text = "Instantly sends real-time in-app alert to every student & parent",
                                fontSize = 11.sp,
                                color = Slate600
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick-Preset Templates
                    Text(
                        text = if (language == AppLanguage.MARATHI) "त्वरित टेम्पलेट्स (१-क्लिक)" else "Quick Preset Templates:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate700
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PresetChip(
                            label = "📢 Notice",
                            onClick = {
                                onTitleChange(if (language == AppLanguage.MARATHI) "अंगणवाडी केंद्र वेळ सूचना" else "Anganwadi Center Timing Notice")
                                onMessageChange(if (language == AppLanguage.MARATHI) "उद्या सकाळी ठीक ९:०० वाजता सर्व बालकांनी केंद्रात उपस्थित राहावे." else "All students must arrive at the centre by 9:00 AM tomorrow for morning assembly.")
                                onPriorityChange(NotificationPriority.NORMAL)
                            }
                        )
                        PresetChip(
                            label = "📦 Ration",
                            onClick = {
                                onTitleChange(if (language == AppLanguage.MARATHI) "पोषण आहार (THR) वाटप सुरू आहे" else "Take-Home Ration Distribution Live")
                                onMessageChange(if (language == AppLanguage.MARATHI) "सर्व नोंदणीकृत पालकांनी केंद्र क्रमांक ४२ वर येऊन पोषण आहार पाकिटे घेऊन जावे." else "Take-Home Ration packets, fortified oil, and eggs are now ready for pickup at Center 042.")
                                onPriorityChange(NotificationPriority.HIGH)
                            }
                        )
                        PresetChip(
                            label = "💉 Vaccine",
                            onClick = {
                                onTitleChange(if (language == AppLanguage.MARATHI) "विशेष लसीकरण शिबीर उद्या" else "Immunization Camp Scheduled")
                                onMessageChange(if (language == AppLanguage.MARATHI) "उद्या प्राथमिक आरोग्य केंद्राच्या वतीने प्रलंबित लसी टोचल्या जातील. लसीकरण कार्ड आणावे." else "PHC team will administer due DPT/Measles doses tomorrow. Please bring immunization card.")
                                onPriorityChange(NotificationPriority.HIGH)
                            }
                        )
                        PresetChip(
                            label = "🚨 Urgent",
                            onClick = {
                                onTitleChange(if (language == AppLanguage.MARATHI) "अतिमहत्त्वाची सूचना: सुपोषण दिवस" else "Urgent: Poshan Counseling Day")
                                onMessageChange(if (language == AppLanguage.MARATHI) "सर्व मातांनी उद्या दुपारी १२ वाजता विशेष पोषण आहार प्रात्यक्षिक सत्रास हजर राहावे." else "All mothers are requested to attend the special supplementary nutrition demo tomorrow.")
                                onPriorityChange(NotificationPriority.URGENT)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Target Audience
                    Text(
                        text = if (language == AppLanguage.MARATHI) "लक्ष्यित वर्ग (Audience):" else "Target Audience:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate700
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = targetChildId == null,
                            onClick = { onTargetChange(null) },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("All Parents (${allChildren.size})", fontSize = 11.sp)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryIndigoContainer,
                                selectedLabelColor = PrimaryIndigo
                            )
                        )

                        allChildren.take(2).forEach { child ->
                            FilterChip(
                                selected = targetChildId == child.childId,
                                onClick = { onTargetChange(child.childId) },
                                label = {
                                    Text(child.name.split(" ").firstOrNull() ?: child.childId, fontSize = 11.sp)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryIndigoContainer,
                                    selectedLabelColor = PrimaryIndigo
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Title Input
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = onTitleChange,
                        label = { Text(if (language == AppLanguage.MARATHI) "संदेश शीर्षक (Title)" else "Notification Title") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_broadcast_title_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigo,
                            unfocusedBorderColor = Slate200
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Message Body Input
                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = onMessageChange,
                        label = { Text(if (language == AppLanguage.MARATHI) "संदेश मजकूर (Message Body)" else "Message Body for Parents") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("admin_broadcast_message_input"),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigo,
                            unfocusedBorderColor = Slate200
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Priority Selector & Send Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            PriorityBadge(
                                label = "Normal",
                                selected = priorityInput == NotificationPriority.NORMAL,
                                color = PrimaryIndigo,
                                onClick = { onPriorityChange(NotificationPriority.NORMAL) }
                            )
                            PriorityBadge(
                                label = "High",
                                selected = priorityInput == NotificationPriority.HIGH,
                                color = StatusMAM,
                                onClick = { onPriorityChange(NotificationPriority.HIGH) }
                            )
                            PriorityBadge(
                                label = "Urgent",
                                selected = priorityInput == NotificationPriority.URGENT,
                                color = StatusSAM,
                                onClick = { onPriorityChange(NotificationPriority.URGENT) }
                            )
                        }

                        Button(
                            onClick = onSendBroadcast,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            modifier = Modifier.testTag("btn_send_broadcast")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (language == AppLanguage.MARATHI) "थेट पाठवा" else "Send Live",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Sent Notifications History Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (language == AppLanguage.MARATHI) "पाठवलेले संदेश व इतिहास (Sent Log)" else "Broadcast History & Live Feed",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Slate900
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(StatusNormal)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Real-Time Room Sync",
                        fontSize = 11.sp,
                        color = StatusNormal,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // List of Broadcast Items
        items(recentNotifications, key = { it.id }) { notif ->
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
                    .border(1.dp, Slate200, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = notif.formattedTime,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate600
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isUrgent) Color(0xFFFEE2E2) else Color(0xFFEFF6FF))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (notif.targetChildId == null) "TO ALL STUDENTS" else "TO ${notif.targetChildId}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUrgent) StatusSAM else PrimaryIndigo
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Delivered",
                                tint = StatusNormal,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Delivered",
                                fontSize = 10.sp,
                                color = StatusNormal,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = message,
                        fontSize = 12.sp,
                        color = Slate700,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Slate100)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Slate900
        )
    }
}

@Composable
private fun PriorityBadge(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) color else Color.Transparent)
            .border(1.dp, color, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else color
        )
    }
}
