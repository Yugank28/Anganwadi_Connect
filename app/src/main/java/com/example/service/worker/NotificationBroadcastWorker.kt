package com.example.service.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.model.AppNotification
import com.example.data.model.NotificationPriority
import com.example.data.model.NotificationType
import com.example.service.NotificationService

/**
 * WorkManager CoroutineWorker for reliable, guaranteed background delivery
 * of teacher broadcasts and urgent child health/ration notifications to parent devices.
 */
class NotificationBroadcastWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val id = inputData.getLong(KEY_ID, 0L)
            val titleEn = inputData.getString(KEY_TITLE_EN) ?: "Anganwadi Update"
            val titleMr = inputData.getString(KEY_TITLE_MR) ?: titleEn
            val titleHi = inputData.getString(KEY_TITLE_HI) ?: titleEn
            val messageEn = inputData.getString(KEY_MESSAGE_EN) ?: ""
            val messageMr = inputData.getString(KEY_MESSAGE_MR) ?: messageEn
            val messageHi = inputData.getString(KEY_MESSAGE_HI) ?: messageEn
            val senderName = inputData.getString(KEY_SENDER) ?: "Rekha Tai Patil (Anganwadi Sevika)"
            val centerName = inputData.getString(KEY_CENTER) ?: "AW-Center-042 (Shivaji Nagar)"
            val typeStr = inputData.getString(KEY_TYPE) ?: NotificationType.MESSAGE.name
            val priorityStr = inputData.getString(KEY_PRIORITY) ?: NotificationPriority.NORMAL.name
            val targetChildId = inputData.getString(KEY_TARGET_CHILD)?.ifEmpty { null }

            val type = try {
                NotificationType.valueOf(typeStr)
            } catch (e: Exception) {
                NotificationType.MESSAGE
            }

            val priority = try {
                NotificationPriority.valueOf(priorityStr)
            } catch (e: Exception) {
                NotificationPriority.NORMAL
            }

            val notification = AppNotification(
                id = id,
                titleEn = titleEn,
                titleMr = titleMr,
                titleHi = titleHi,
                messageEn = messageEn,
                messageMr = messageMr,
                messageHi = messageHi,
                senderName = senderName,
                senderRole = "Anganwadi Worker / Teacher",
                centerId = "AW-Center-042",
                centerName = centerName,
                timestamp = System.currentTimeMillis(),
                type = type,
                priority = priority,
                targetChildId = targetChildId,
                isRead = false
            )

            // Trigger system notification banner
            val service = NotificationService.getInstance(applicationContext)
            service.showSystemNotification(notification)

            Log.d(TAG, "Successfully processed broadcast worker for notification #$id ($titleEn)")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "NotificationBroadcastWorker execution error", e)
            Result.retry()
        }
    }

    companion object {
        const val TAG = "NotifBroadcastWorker"
        const val KEY_ID = "broadcast_id"
        const val KEY_TITLE_EN = "title_en"
        const val KEY_TITLE_MR = "title_mr"
        const val KEY_TITLE_HI = "title_hi"
        const val KEY_MESSAGE_EN = "message_en"
        const val KEY_MESSAGE_MR = "message_mr"
        const val KEY_MESSAGE_HI = "message_hi"
        const val KEY_SENDER = "sender_name"
        const val KEY_CENTER = "center_name"
        const val KEY_TYPE = "notification_type"
        const val KEY_PRIORITY = "notification_priority"
        const val KEY_TARGET_CHILD = "target_child_id"
    }
}
