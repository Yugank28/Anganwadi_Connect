package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.AppNotification
import com.example.data.model.ChildProfile
import com.example.data.model.HealthRecord
import com.example.data.model.HelpTicket
import com.example.data.model.RationHandover
import com.example.data.model.VaccinationRecord
import com.example.data.parser.SampleDataGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ChildProfile::class,
        HealthRecord::class,
        VaccinationRecord::class,
        AppNotification::class,
        HelpTicket::class,
        RationHandover::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun childDao(): ChildDao
    abstract fun notificationDao(): NotificationDao
    abstract fun helpTicketDao(): HelpTicketDao
    abstract fun rationHandoverDao(): RationHandoverDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "poshan_tracker.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance

                // Immediately ensure demo records are populated if database is fresh or empty
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        populateInitialData(instance)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                instance
            }
        }

        suspend fun populateInitialData(database: AppDatabase) {
            val childDao = database.childDao()
            if (childDao.getChildCount() == 0) {
                childDao.upsertProfiles(SampleDataGenerator.getInitialProfiles())
                childDao.insertHealthRecords(SampleDataGenerator.getInitialHealthRecords())
                childDao.insertVaccinations(SampleDataGenerator.getInitialVaccinations())
            }
            val notifDao = database.notificationDao()
            if (notifDao.getNotificationCount() == 0) {
                notifDao.insertNotifications(SampleDataGenerator.getInitialNotifications())
            }
            val ticketDao = database.helpTicketDao()
            if (ticketDao.getTicketCount() == 0) {
                ticketDao.insertTickets(SampleDataGenerator.getInitialHelpTickets())
            }
            val handoverDao = database.rationHandoverDao()
            if (handoverDao.getHandoverCount() == 0) {
                handoverDao.upsertHandovers(SampleDataGenerator.getInitialHandovers())
            }
        }
    }
}
