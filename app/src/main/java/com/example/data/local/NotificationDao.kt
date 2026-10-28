package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.AppNotification
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Query("SELECT * FROM in_app_notifications WHERE targetChildId IS NULL OR targetChildId = :childId ORDER BY timestamp DESC")
    fun getNotificationsForChild(childId: String): Flow<List<AppNotification>>

    @Query("SELECT * FROM in_app_notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<AppNotification>>

    @Query("SELECT COUNT(*) FROM in_app_notifications WHERE isRead = 0 AND (targetChildId IS NULL OR targetChildId = :childId)")
    fun getUnreadCountForChild(childId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM in_app_notifications WHERE isRead = 0")
    fun getTotalUnreadCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: AppNotification): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<AppNotification>)

    @Query("UPDATE in_app_notifications SET isRead = 1 WHERE id = :notificationId")
    suspend fun markAsRead(notificationId: Long)

    @Query("UPDATE in_app_notifications SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM in_app_notifications WHERE id = :notificationId")
    suspend fun deleteNotification(notificationId: Long)

    @Query("DELETE FROM in_app_notifications")
    suspend fun clearAllNotifications()

    @Query("SELECT COUNT(*) FROM in_app_notifications")
    suspend fun getNotificationCount(): Int
}
