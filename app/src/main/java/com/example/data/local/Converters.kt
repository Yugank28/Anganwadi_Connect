package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.NotificationPriority
import com.example.data.model.NotificationType
import com.example.data.model.NutritionStatus
import com.example.data.model.VaccineStatus

class Converters {
    @TypeConverter
    fun fromNutritionStatus(status: NutritionStatus?): String {
        return status?.name ?: NutritionStatus.NORMAL.name
    }

    @TypeConverter
    fun toNutritionStatus(value: String?): NutritionStatus {
        return if (value != null) {
            try {
                NutritionStatus.valueOf(value)
            } catch (_: Exception) {
                NutritionStatus.NORMAL
            }
        } else {
            NutritionStatus.NORMAL
        }
    }

    @TypeConverter
    fun fromVaccineStatus(status: VaccineStatus?): String {
        return status?.name ?: VaccineStatus.UPCOMING.name
    }

    @TypeConverter
    fun toVaccineStatus(value: String?): VaccineStatus {
        return if (value != null) {
            try {
                VaccineStatus.valueOf(value)
            } catch (_: Exception) {
                VaccineStatus.UPCOMING
            }
        } else {
            VaccineStatus.UPCOMING
        }
    }

    @TypeConverter
    fun fromNotificationType(type: NotificationType?): String {
        return type?.name ?: NotificationType.MESSAGE.name
    }

    @TypeConverter
    fun toNotificationType(value: String?): NotificationType {
        return if (value != null) {
            try {
                NotificationType.valueOf(value)
            } catch (_: Exception) {
                NotificationType.MESSAGE
            }
        } else {
            NotificationType.MESSAGE
        }
    }

    @TypeConverter
    fun fromNotificationPriority(priority: NotificationPriority?): String {
        return priority?.name ?: NotificationPriority.NORMAL.name
    }

    @TypeConverter
    fun toNotificationPriority(value: String?): NotificationPriority {
        return if (value != null) {
            try {
                NotificationPriority.valueOf(value)
            } catch (_: Exception) {
                NotificationPriority.NORMAL
            }
        } else {
            NotificationPriority.NORMAL
        }
    }
}
