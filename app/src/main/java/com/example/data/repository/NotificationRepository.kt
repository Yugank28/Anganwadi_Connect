package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.NotificationDao
import com.example.data.model.AppNotification
import com.example.data.model.NotificationPriority
import com.example.data.model.NotificationType
import com.example.service.NotificationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dedicated Repository for managing in-app notifications and background broadcast dispatch.
 * Decoupled from child and parent profile data to satisfy clean architecture requirements.
 */
interface NotificationRepository {
    fun getAllNotifications(): Flow<List<AppNotification>>
    fun getNotificationsForChild(childId: String): Flow<List<AppNotification>>
    fun getNotifications(targetChildId: String? = null): Flow<List<AppNotification>>
    fun getUnreadCount(targetChildId: String? = null): Flow<Int>

    suspend fun sendNotification(notification: AppNotification): Long

    suspend fun sendBroadcastNotification(
        titleEn: String,
        titleMr: String = titleEn,
        titleHi: String = titleEn,
        messageEn: String,
        messageMr: String = messageEn,
        messageHi: String = messageEn,
        type: NotificationType = NotificationType.MESSAGE,
        priority: NotificationPriority = NotificationPriority.NORMAL,
        targetChildId: String? = null
    ): Long

    suspend fun markAsRead(notificationId: Long)
    suspend fun markAllAsRead()
    suspend fun deleteNotification(notificationId: Long)
    suspend fun clearAllNotifications()
    suspend fun getNotificationCount(): Int
}

class NotificationRepositoryImpl(
    private val notificationDao: NotificationDao,
    private val notificationService: NotificationService,
    private val cloudSyncManager: com.example.data.remote.FirebaseCloudSyncManager? = null
) : NotificationRepository {

    init {
        cloudSyncManager?.startRealtimeAnnouncementListener { incomingNotification, isRealtimeIncoming ->
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                notificationDao.insertNotification(incomingNotification)
                if (isRealtimeIncoming) {
                    notificationService.showSystemNotification(incomingNotification)
                }
            }
        }
    }

    override fun getAllNotifications(): Flow<List<AppNotification>> =
        notificationDao.getAllNotifications()

    override fun getNotificationsForChild(childId: String): Flow<List<AppNotification>> =
        notificationDao.getNotificationsForChild(childId)

    override fun getNotifications(targetChildId: String?): Flow<List<AppNotification>> {
        return if (targetChildId.isNullOrEmpty()) {
            notificationDao.getAllNotifications()
        } else {
            notificationDao.getNotificationsForChild(targetChildId)
        }
    }

    override fun getUnreadCount(targetChildId: String?): Flow<Int> {
        return if (targetChildId.isNullOrEmpty()) {
            notificationDao.getTotalUnreadCount()
        } else {
            notificationDao.getUnreadCountForChild(targetChildId)
        }
    }

    override suspend fun sendNotification(notification: AppNotification): Long = withContext(Dispatchers.IO) {
        val insertedId = notificationDao.insertNotification(notification)
        val finalNotification = notification.copy(id = insertedId)

        // 1. Post to real-time Cloud Firestore (Phone A -> Phone B instant cloud push)
        try {
            cloudSyncManager?.postAnnouncementToCloud(finalNotification)
        } catch (e: Exception) {
            android.util.Log.w("NotificationRepo", "Cloud sync queued/offline: ${e.message}")
        }

        // 2. Trigger immediate heads-up status bar notification on active devices
        notificationService.showSystemNotification(finalNotification)

        // 3. Enqueue guaranteed background delivery via WorkManager
        notificationService.enqueueBackgroundBroadcast(finalNotification)

        insertedId
    }

    override suspend fun sendBroadcastNotification(
        titleEn: String,
        titleMr: String,
        titleHi: String,
        messageEn: String,
        messageMr: String,
        messageHi: String,
        type: NotificationType,
        priority: NotificationPriority,
        targetChildId: String?
    ): Long = withContext(Dispatchers.IO) {
        val timeFormat = SimpleDateFormat("h:mm a, dd MMM", Locale.getDefault())
        val formattedTime = timeFormat.format(Date())

        val notification = AppNotification(
            titleEn = titleEn,
            titleMr = titleMr,
            titleHi = titleHi,
            messageEn = messageEn,
            messageMr = messageMr,
            messageHi = messageHi,
            senderName = "Rekha Tai Patil (Anganwadi Sevika)",
            senderRole = "Anganwadi Worker / Teacher",
            centerId = "AW-Center-042",
            centerName = "AW-Center-042 (Shivaji Nagar)",
            timestamp = System.currentTimeMillis(),
            formattedTime = formattedTime,
            type = type,
            priority = priority,
            targetChildId = targetChildId,
            isRead = false
        )

        sendNotification(notification)
    }

    override suspend fun markAsRead(notificationId: Long) = withContext(Dispatchers.IO) {
        notificationDao.markAsRead(notificationId)
    }

    override suspend fun markAllAsRead() = withContext(Dispatchers.IO) {
        notificationDao.markAllAsRead()
    }

    override suspend fun deleteNotification(notificationId: Long) = withContext(Dispatchers.IO) {
        notificationDao.deleteNotification(notificationId)
    }

    override suspend fun clearAllNotifications() = withContext(Dispatchers.IO) {
        notificationDao.clearAllNotifications()
    }

    override suspend fun getNotificationCount(): Int = withContext(Dispatchers.IO) {
        notificationDao.getNotificationCount()
    }

    companion object {
        @Volatile
        private var instance: NotificationRepository? = null

        fun getInstance(context: Context): NotificationRepository {
            return instance ?: synchronized(this) {
                instance ?: run {
                    val db = AppDatabase.getDatabase(context)
                    val service = NotificationService.getInstance(context)
                    val syncManager = com.example.data.remote.FirebaseCloudSyncManager.getInstance(context)
                    NotificationRepositoryImpl(db.notificationDao(), service, syncManager).also {
                        instance = it
                    }
                }
            }
        }
    }
}
