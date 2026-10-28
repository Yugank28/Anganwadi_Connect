package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class NotificationPriority {
    NORMAL,
    HIGH,
    URGENT
}

enum class NotificationType {
    MESSAGE,
    ANNOUNCEMENT,
    RATION_SCHEDULE,
    NUTRITION_ALERT,
    VACCINATION_ALERT,
    ATTENDANCE_UPDATE
}

@Entity(tableName = "in_app_notifications")
data class AppNotification(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val titleEn: String,
    val titleMr: String = titleEn,
    val titleHi: String = titleEn,
    val messageEn: String,
    val messageMr: String = messageEn,
    val messageHi: String = messageEn,
    val senderName: String = "Rekha Tai Patil (Anganwadi Sevika)",
    val senderRole: String = "Anganwadi Worker / Teacher",
    val centerId: String = "AW-Center-042",
    val centerName: String = "AW-Center-042 (Shivaji Nagar)",
    val timestamp: Long = System.currentTimeMillis(),
    val formattedTime: String = "Just now",
    val type: NotificationType = NotificationType.MESSAGE,
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    val targetChildId: String? = null, // null means broadcast to ALL parents/students
    val isRead: Boolean = false
)
