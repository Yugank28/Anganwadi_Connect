package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.NotificationPriority
import com.example.data.model.NotificationType
import com.example.data.model.NutritionStatus
import com.example.data.parser.SampleDataGenerator
import com.example.data.parser.XmlParser
import com.example.data.repository.ParentConnectRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Anganwadi Connect", appName)
  }

  @Test
  fun `xmlParser parses sample records correctly under Robolectric`() {
    val result = XmlParser.parseText(SampleDataGenerator.sampleXmlText)
    assertTrue("XML errors: ${result.errors}", result.errors.isEmpty())
    assertEquals(2, result.successfulRecords.size)

    val first = result.successfulRecords.first()
    assertEquals("AW-042-06", first.childId)
    assertEquals("Meera Yadav", first.name)
    assertEquals(20, first.ageMonths)
    assertEquals(9.2f, first.weightKg, 0.01f)
    assertEquals(NutritionStatus.MAM, first.nutritionStatus)
  }

  @Test
  fun `room database initializes and persists children and notifications`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getDatabase(context)
    db.childDao().clearAllProfiles()
    db.notificationDao().clearAllNotifications()
    AppDatabase.populateInitialData(db)

    val childCount = db.childDao().getChildCount()
    assertTrue("Child count should be at least 3", childCount >= 3)

    val notifCount = db.notificationDao().getNotificationCount()
    assertTrue("Notification count should be at least 3", notifCount >= 3)

    val child1 = db.childDao().getChildByIdDirect("AW-042-01")
    assertNotNull(child1)
    assertEquals("Aarav Sachin Gaikwad", child1?.name)
  }

  @Test
  fun `admin broadcast creates in-app notification delivered to parents in real time`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = ParentConnectRepositoryImpl.getInstance(context)

    val notifId = repository.sendBroadcastNotification(
        titleEn = "Emergency Alert",
        titleMr = "तातडीची सूचना",
        titleHi = "तत्काल सूचना",
        messageEn = "Take home ration pickup starts today.",
        messageMr = "पोषण आहार वाटप आज सुरू झाले आहे.",
        messageHi = "राशन वितरण आज शुरू हो गया है।",
        type = NotificationType.RATION_SCHEDULE,
        priority = NotificationPriority.URGENT
    )
    assertTrue("Generated notification ID should be positive", notifId > 0)

    val notifications = repository.getNotifications(null).first()
    val found = notifications.find { it.id == notifId }
    assertNotNull("New notification must be in notifications stream", found)
    assertEquals("Emergency Alert", found?.titleEn)
    assertEquals(false, found?.isRead)

    // Mark as read
    repository.markNotificationAsRead(notifId)
    val updatedNotifications = repository.getNotifications(null).first()
    val updated = updatedNotifications.find { it.id == notifId }
    assertEquals(true, updated?.isRead)
  }

  @Test
  fun `notificationRepository delivers notifications and interacts with notificationService`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val notifRepo = com.example.data.repository.NotificationRepositoryImpl.getInstance(context)

    val id = notifRepo.sendBroadcastNotification(
      titleEn = "Vaccine Due Alert",
      messageEn = "Measles 2nd dose due tomorrow at 9 AM",
      type = NotificationType.VACCINATION_ALERT,
      priority = NotificationPriority.URGENT,
      targetChildId = "AW-042-02"
    )

    assertTrue("Notification ID should be > 0", id > 0)

    val childNotifs = notifRepo.getNotificationsForChild("AW-042-02").first()
    assertTrue(childNotifs.any { it.id == id })

    val unreadCount = notifRepo.getUnreadCount("AW-042-02").first()
    assertTrue(unreadCount >= 1)

    notifRepo.markAsRead(id)
    val readNotif = notifRepo.getNotificationsForChild("AW-042-02").first().find { it.id == id }
    assertEquals(true, readNotif?.isRead)
  }

  @Test
  fun `notificationBroadcastWorker runs successfully with input data`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val inputData = androidx.work.workDataOf(
      com.example.service.worker.NotificationBroadcastWorker.KEY_ID to 999L,
      com.example.service.worker.NotificationBroadcastWorker.KEY_TITLE_EN to "Test Alert",
      com.example.service.worker.NotificationBroadcastWorker.KEY_MESSAGE_EN to "Test Message",
      com.example.service.worker.NotificationBroadcastWorker.KEY_TYPE to NotificationType.MESSAGE.name,
      com.example.service.worker.NotificationBroadcastWorker.KEY_PRIORITY to NotificationPriority.HIGH.name
    )

    val worker = androidx.work.testing.TestListenableWorkerBuilder<com.example.service.worker.NotificationBroadcastWorker>(context)
      .setInputData(inputData)
      .build()

    val result = worker.doWork()
    assertEquals(androidx.work.ListenableWorker.Result.success(), result)
  }

  @Test
  fun `mainActivity launches without crashing`() {
    val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
    val activity = controller.get()
    assertNotNull(activity)
  }
}

