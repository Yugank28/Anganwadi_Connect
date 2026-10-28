package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.MainActivity
import com.example.R
import com.example.data.model.AppNotification
import com.example.data.model.NotificationPriority
import com.example.data.model.NotificationType
import com.example.service.worker.NotificationBroadcastWorker

/**
 * Dedicated NotificationService that manages Android notification channels,
 * heads-up system notifications with action intents, and background WorkManager
 * broadcast delivery from the Admin dashboard to parent devices.
 */
class NotificationService(private val context: Context) {

    private val notificationManager: NotificationManagerCompat = NotificationManagerCompat.from(context)

    init {
        createNotificationChannels()
    }

    /**
     * Initializes Android 8.0+ (API 26+) notification channels for different Anganwadi alert types.
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // Channel 1: High Priority Announcements & Broadcasts from Teacher
            val broadcastChannel = NotificationChannel(
                CHANNEL_BROADCASTS,
                "Anganwadi Teacher Broadcasts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent alerts, notices, and messages sent by Anganwadi Sevika / Teacher"
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
            }

            // Channel 2: Ration & THR Distribution Schedules
            val rationChannel = NotificationChannel(
                CHANNEL_RATION,
                "Ration Distribution Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pickup dates, venues, and timings for Take-Home Ration (THR) packets"
                enableLights(true)
                lightColor = Color.GREEN
                enableVibration(true)
            }

            // Channel 3: Attendance & Child Health Alerts
            val attendanceChannel = NotificationChannel(
                CHANNEL_ATTENDANCE,
                "Child Attendance & Daily Status",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily attendance confirmations and child welfare updates"
                enableVibration(false)
            }

            // Channel 4: Urgent Emergency Reminders (Vaccination & Nutrition)
            val urgentChannel = NotificationChannel(
                CHANNEL_URGENT,
                "Urgent Vaccination & Health Camps",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority immunization camp alerts and emergency health announcements"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
            }

            systemNotificationManager.createNotificationChannels(
                listOf(broadcastChannel, rationChannel, attendanceChannel, urgentChannel)
            )
        }
    }

    /**
     * Posts a rich heads-up system notification to the Android status bar and notification shade.
     */
    fun showSystemNotification(notification: AppNotification) {
        val channelId = when (notification.type) {
            NotificationType.RATION_SCHEDULE -> CHANNEL_RATION
            NotificationType.ATTENDANCE_UPDATE -> CHANNEL_ATTENDANCE
            NotificationType.VACCINATION_ALERT, NotificationType.NUTRITION_ALERT -> CHANNEL_URGENT
            else -> if (notification.priority == NotificationPriority.URGENT) CHANNEL_URGENT else CHANNEL_BROADCASTS
        }

        // Tap intent to open MainActivity directly into the Parent Connect Hub
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIFICATION_ID, notification.id)
            putExtra(EXTRA_TARGET_CHILD_ID, notification.targetChildId)
            putExtra(EXTRA_NOTIFICATION_TYPE, notification.type.name)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notification.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val priorityLevel = when (notification.priority) {
            NotificationPriority.URGENT -> NotificationCompat.PRIORITY_MAX
            NotificationPriority.HIGH -> NotificationCompat.PRIORITY_HIGH
            NotificationPriority.NORMAL -> NotificationCompat.PRIORITY_DEFAULT
        }

        // Title and body according to notification details
        val title = notification.titleMr.ifBlank { notification.titleEn }
        val message = notification.messageMr.ifBlank { notification.messageEn }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("📢 $title")
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("📢 $title")
                    .bigText("$message\n\n— ${notification.senderName} (${notification.centerName})")
                    .setSummaryText(notification.senderRole)
            )
            .setPriority(priorityLevel)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 250, 150, 250))

        try {
            // Respects permission checks on Android 13+ (POST_NOTIFICATIONS)
            if (notificationManager.areNotificationsEnabled()) {
                val notificationId = if (notification.id > 0) notification.id.toInt() else System.currentTimeMillis().toInt()
                notificationManager.notify(notificationId, builder.build())
            } else {
                Log.w(TAG, "Notifications are disabled in system settings for this app")
            }
        } catch (se: SecurityException) {
            Log.w(TAG, "Cannot post notification: missing POST_NOTIFICATIONS permission", se)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display system notification", e)
        }
    }

    /**
     * Enqueues a background broadcast task via WorkManager so notifications are
     * reliably dispatched and delivered even if the application is backgrounded or suspended.
     */
    fun enqueueBackgroundBroadcast(notification: AppNotification) {
        val inputData = Data.Builder()
            .putLong(NotificationBroadcastWorker.KEY_ID, notification.id)
            .putString(NotificationBroadcastWorker.KEY_TITLE_EN, notification.titleEn)
            .putString(NotificationBroadcastWorker.KEY_TITLE_MR, notification.titleMr)
            .putString(NotificationBroadcastWorker.KEY_TITLE_HI, notification.titleHi)
            .putString(NotificationBroadcastWorker.KEY_MESSAGE_EN, notification.messageEn)
            .putString(NotificationBroadcastWorker.KEY_MESSAGE_MR, notification.messageMr)
            .putString(NotificationBroadcastWorker.KEY_MESSAGE_HI, notification.messageHi)
            .putString(NotificationBroadcastWorker.KEY_SENDER, notification.senderName)
            .putString(NotificationBroadcastWorker.KEY_CENTER, notification.centerName)
            .putString(NotificationBroadcastWorker.KEY_TYPE, notification.type.name)
            .putString(NotificationBroadcastWorker.KEY_PRIORITY, notification.priority.name)
            .putString(NotificationBroadcastWorker.KEY_TARGET_CHILD, notification.targetChildId ?: "")
            .build()

        val workRequest = OneTimeWorkRequestBuilder<NotificationBroadcastWorker>()
            .setInputData(inputData)
            .addTag(TAG_BROADCAST_WORK)
            .build()

        try {
            WorkManager.getInstance(context).enqueueUniqueWork(
                "broadcast_${notification.id}_${System.currentTimeMillis()}",
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                workRequest
            )
        } catch (e: Exception) {
            Log.e(TAG, "WorkManager enqueue failed: ${e.message}")
        }
    }

    companion object {
        const val TAG = "NotificationService"
        const val CHANNEL_BROADCASTS = "channel_anganwadi_broadcasts"
        const val CHANNEL_RATION = "channel_anganwadi_ration"
        const val CHANNEL_ATTENDANCE = "channel_anganwadi_attendance"
        const val CHANNEL_URGENT = "channel_anganwadi_urgent"

        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_TARGET_CHILD_ID = "extra_target_child_id"
        const val EXTRA_NOTIFICATION_TYPE = "extra_notification_type"
        const val TAG_BROADCAST_WORK = "anganwadi_notification_broadcast"

        @Volatile
        private var instance: NotificationService? = null

        fun getInstance(context: Context): NotificationService {
            return instance ?: synchronized(this) {
                instance ?: NotificationService(context.applicationContext).also { instance = it }
            }
        }
    }
}
