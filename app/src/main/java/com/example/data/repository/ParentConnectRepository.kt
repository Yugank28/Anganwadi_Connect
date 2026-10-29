package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.AppDatabase
import com.example.data.local.ChildDao
import com.example.data.local.HelpTicketDao
import com.example.data.local.NotificationDao
import com.example.data.local.RationHandoverDao
import com.example.data.model.AnnouncementCategory
import com.example.data.model.AppNotification
import com.example.data.model.CentreAnnouncement
import com.example.data.model.ChildProfile
import com.example.data.model.DiffImportResult
import com.example.data.model.HealthRecord
import com.example.data.model.HelpTicket
import com.example.data.model.NotificationPriority
import com.example.data.model.NotificationType
import com.example.data.model.NutritionStatus
import com.example.data.model.ParsedChildRecord
import com.example.data.model.RationHandover
import com.example.data.model.RationSchedule
import com.example.data.model.SessionRole
import com.example.data.model.TicketStatus
import com.example.data.model.UserSession
import com.example.data.model.VaccinationRecord
import com.example.data.parser.CsvParser
import com.example.data.parser.SampleDataGenerator
import com.example.data.remote.GeminiService
import com.example.data.remote.GrowthSummaryResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

interface ParentConnectRepository {
    val currentSession: Flow<UserSession>

    suspend fun loginAdmin(username: String, pin: String): Result<UserSession>
    suspend fun loginParent(mobile: String, code: String): Result<UserSession>
    suspend fun loginSuperAdmin(username: String, pin: String): Result<UserSession>
    fun logout()

    fun getChildProfile(childId: String): Flow<ChildProfile?>
    fun getChildHealthRecords(childId: String): Flow<List<HealthRecord>>
    fun getChildVaccinations(childId: String): Flow<List<VaccinationRecord>>
    fun getAllChildren(): Flow<List<ChildProfile>>

    fun getRationSchedules(): Flow<List<RationSchedule>>
    fun getAnnouncements(): Flow<List<CentreAnnouncement>>
    fun getAttendanceMap(): Flow<Map<String, Boolean>>

    // Help Center & Complaints (Super Admin)
    fun getHelpTicketsForParent(parentContact: String): Flow<List<HelpTicket>>
    fun getAllHelpTickets(): Flow<List<HelpTicket>>
    fun getPendingHelpTicketsCount(): Flow<Int>
    suspend fun submitHelpTicket(ticket: HelpTicket): Result<Long>
    suspend fun respondToHelpTicket(
        ticketId: Long,
        ticketNumber: String,
        response: String,
        newStatus: TicketStatus
    ): Result<Unit>

    // Ration Handover & Parent Authentication
    fun getRationHandoverForChild(childId: String): Flow<RationHandover?>
    fun getAllRationHandovers(): Flow<List<RationHandover>>
    suspend fun verifyAndCompleteRationHandover(childId: String, authCode: String): Result<Unit>

    // Real-Time In-App Notifications
    fun getNotifications(childId: String? = null): Flow<List<AppNotification>>
    fun getUnreadNotificationCount(childId: String? = null): Flow<Int>
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
    suspend fun markNotificationAsRead(notificationId: Long)
    suspend fun markAllNotificationsAsRead()

    suspend fun toggleAttendance(childId: String, isPresent: Boolean)
    suspend fun dispatchRationSchedule(schedule: RationSchedule)
    suspend fun dispatchAnnouncement(announcement: CentreAnnouncement)

    suspend fun parseAndValidateUpload(uri: Uri, isExcel: Boolean): DiffImportResult
    suspend fun parseSampleExcel(): DiffImportResult
    suspend fun commitImport(diff: DiffImportResult): Result<Int>
    fun getTemplateCsvContent(): String

    suspend fun getAiGrowthAdvice(childId: String, forceRefresh: Boolean = false): GrowthSummaryResult
    suspend fun resetDatabaseToInitial()
}

class ParentConnectRepositoryImpl(
    private val context: Context,
    private val dao: ChildDao,
    private val notificationDao: NotificationDao,
    private val helpTicketDao: HelpTicketDao = AppDatabase.getDatabase(context).helpTicketDao(),
    private val rationHandoverDao: RationHandoverDao = AppDatabase.getDatabase(context).rationHandoverDao(),
    private val notificationRepository: NotificationRepository = NotificationRepositoryImpl.getInstance(context),
    private val cloudSyncManager: com.example.data.remote.FirebaseCloudSyncManager = com.example.data.remote.FirebaseCloudSyncManager.getInstance(context)
) : ParentConnectRepository {

    private val _currentSession = MutableStateFlow(UserSession())
    override val currentSession: Flow<UserSession> = _currentSession.asStateFlow()

    private val _attendanceMap = MutableStateFlow<Map<String, Boolean>>(SampleDataGenerator.initialAttendanceMap)
    override fun getAttendanceMap(): Flow<Map<String, Boolean>> = cloudSyncManager.getAttendanceMapFlow()

    override fun getRationSchedules(): Flow<List<RationSchedule>> = cloudSyncManager.getRationSchedulesFlow()

    override fun getAnnouncements(): Flow<List<CentreAnnouncement>> = cloudSyncManager.getAnnouncementsFlow()

    private val summaryCache = ConcurrentHashMap<String, GrowthSummaryResult>()

    init {
        // Guarantee Cloud Firestore server database has initial records seeded
        CoroutineScope(Dispatchers.IO).launch {
            try {
                cloudSyncManager.seedInitialServerDataIfEmpty()
            } catch (e: Exception) {
                android.util.Log.w("ParentConnectRepo", "Cloud Firestore initialization: ${e.message}")
            }
            if (dao.getChildCount() == 0) {
                dao.upsertProfiles(SampleDataGenerator.getInitialProfiles())
                dao.insertHealthRecords(SampleDataGenerator.getInitialHealthRecords())
                dao.insertVaccinations(SampleDataGenerator.getInitialVaccinations())
            }
            if (notificationDao.getNotificationCount() == 0) {
                notificationDao.insertNotifications(SampleDataGenerator.getInitialNotifications())
            }
            if (helpTicketDao.getTicketCount() == 0) {
                helpTicketDao.insertTickets(SampleDataGenerator.getInitialHelpTickets())
            }
            if (rationHandoverDao.getHandoverCount() == 0) {
                rationHandoverDao.upsertHandovers(SampleDataGenerator.getInitialHandovers())
            }
        }
    }

    override suspend fun loginAdmin(username: String, pin: String): Result<UserSession> = withContext(Dispatchers.Default) {
        val cleanUser = username.trim()
        val cleanPin = pin.trim()
        if (cleanUser == SampleDataGenerator.ADMIN_USERNAME && cleanPin == SampleDataGenerator.ADMIN_PASSWORD) {
            val session = UserSession(
                role = SessionRole.ADMIN,
                username = cleanUser,
                displayName = SampleDataGenerator.ADMIN_NAME,
                centerId = "AW-Center-042",
                centerName = SampleDataGenerator.ADMIN_CENTER,
                authenticatedChildId = null
            )
            _currentSession.value = session
            Result.success(session)
        } else {
            Result.failure(IllegalArgumentException("Invalid Admin Username or PIN. (Use: admin_aw042 / 123456)"))
        }
    }

    override suspend fun loginParent(mobile: String, code: String): Result<UserSession> = withContext(Dispatchers.IO) {
        val cleanMobile = mobile.trim()
        val cleanCode = code.trim().uppercase()

        // 1. Verify credentials directly against live Cloud Firestore server database
        val cloudChild = cloudSyncManager.verifyParentInServer(cleanMobile, cleanCode)

        // Fallback to local cache if offline
        val matchedChild = cloudChild ?: dao.verifyParentDirect(cleanMobile, cleanCode, cleanCode)
            ?: dao.getAllChildren().firstOrNull()?.firstOrNull { child ->
                (child.parentContact == cleanMobile && child.regCode.equals(cleanCode, ignoreCase = true)) ||
                child.childId.equals(cleanCode, ignoreCase = true) ||
                child.regCode.equals(cleanCode, ignoreCase = true) ||
                (child.parentContact == cleanMobile)
            }

        if (matchedChild != null) {
            val session = UserSession(
                role = SessionRole.PARENT,
                username = matchedChild.parentContact,
                displayName = "${matchedChild.guardian} (${matchedChild.name}'s Parent)",
                centerId = matchedChild.anganwadiCenterId,
                centerName = matchedChild.anganwadiCenterName,
                authenticatedChildId = matchedChild.childId
            )
            _currentSession.value = session
            Result.success(session)
        } else {
            Result.failure(IllegalArgumentException("No child found matching Mobile '$cleanMobile' and Code '$cleanCode'. Check credentials issued by Anganwadi."))
        }
    }

    override fun logout() {
        _currentSession.value = UserSession()
    }

    override suspend fun loginSuperAdmin(username: String, pin: String): Result<UserSession> = withContext(Dispatchers.Default) {
        val cleanUser = username.trim()
        val cleanPin = pin.trim()
        if (cleanUser == SampleDataGenerator.SUPER_ADMIN_USERNAME && cleanPin == SampleDataGenerator.SUPER_ADMIN_PASSWORD) {
            val session = UserSession(
                role = SessionRole.SUPER_ADMIN,
                username = cleanUser,
                displayName = SampleDataGenerator.SUPER_ADMIN_NAME,
                centerId = "ALL-CENTERS",
                centerName = "District Oversight HQ (District Officer)",
                authenticatedChildId = null
            )
            _currentSession.value = session
            Result.success(session)
        } else {
            Result.failure(IllegalArgumentException("Invalid Super Admin Username or PIN. (Use: superadmin / super123)"))
        }
    }

    override fun getHelpTicketsForParent(parentContact: String): Flow<List<HelpTicket>> =
        helpTicketDao.getTicketsForParent(parentContact)

    override fun getAllHelpTickets(): Flow<List<HelpTicket>> =
        helpTicketDao.getAllTickets()

    override fun getPendingHelpTicketsCount(): Flow<Int> =
        helpTicketDao.getPendingTicketsCount()

    override suspend fun submitHelpTicket(ticket: HelpTicket): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val id = helpTicketDao.insertTicket(ticket)
            val savedTicket = ticket.copy(id = id)
            cloudSyncManager.submitHelpTicketToCloud(savedTicket)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun respondToHelpTicket(
        ticketId: Long,
        ticketNumber: String,
        response: String,
        newStatus: TicketStatus
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            helpTicketDao.updateTicketResponse(ticketId, newStatus, response, now)
            cloudSyncManager.updateTicketResponseInCloud(ticketNumber, newStatus.name, response)

            sendBroadcastNotification(
                titleEn = "Help Desk Response: $ticketNumber",
                titleMr = "तक्रार निवारण अभिप्राय: $ticketNumber",
                titleHi = "शिकायत समाधान उत्तर: $ticketNumber",
                messageEn = "District Officer (Super Admin) responded: \"$response\"",
                messageMr = "सुपर ॲडमिन (जिल्हा अधिकारी) यांचा अभिप्राय: \"$response\"",
                messageHi = "सुपर एडमिन (जिला अधिकारी) का उत्तर: \"$response\"",
                type = NotificationType.MESSAGE,
                priority = NotificationPriority.HIGH
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getRationHandoverForChild(childId: String): Flow<RationHandover?> =
        rationHandoverDao.getHandoverForChild(childId)

    override fun getAllRationHandovers(): Flow<List<RationHandover>> =
        rationHandoverDao.getAllHandovers()

    override suspend fun verifyAndCompleteRationHandover(childId: String, authCode: String): Result<Unit> = withContext(Dispatchers.IO) {
        val child = dao.getChildByIdDirect(childId)
            ?: return@withContext Result.failure(IllegalArgumentException("Child record '$childId' not found."))

        val cleanCode = authCode.trim().uppercase()
        val matchCode = child.regCode.uppercase()
        val matchPhone = child.parentContact.trim()

        val isCodeValid = cleanCode == matchCode ||
            cleanCode == matchPhone ||
            cleanCode == child.childId.uppercase() ||
            (cleanCode.length >= 4 && matchCode.endsWith(cleanCode)) ||
            (cleanCode.length >= 4 && matchPhone.endsWith(cleanCode))

        if (isCodeValid) {
            val now = System.currentTimeMillis()
            val sdf = SimpleDateFormat("dd-MMM-yyyy 'at' hh:mm a", Locale.getDefault())
            val formattedTime = sdf.format(Date(now))
            val sevikaName = SampleDataGenerator.ADMIN_NAME

            rationHandoverDao.completeHandoverForChild(childId, now, formattedTime, sevikaName)

            sendBroadcastNotification(
                titleEn = "✅ Ration Pickup Verified: ${child.name}",
                titleMr = "✅ पोषण आहार वाटप पडताळणी: ${child.name}",
                titleHi = "✅ पोषण राशन वितरण सत्यापित: ${child.name}",
                messageEn = "Take-Home Ration packet for ${child.name} was successfully authenticated and handed over to ${child.guardian} at Anganwadi Centre on $formattedTime.",
                messageMr = "${child.name} साठीचा पोषण आहार (THR) यशस्वीरित्या पडताळणी करून ${child.guardian} यांना अंगणवाडी केंद्रावर सोपवण्यात आला आहे.",
                messageHi = "${child.name} का टेक-होम राशन (THR) आंगनवाड़ी केंद्र में ${child.guardian} को सफलतापूर्वक दिया गया।",
                type = NotificationType.RATION_SCHEDULE,
                priority = NotificationPriority.HIGH,
                targetChildId = childId
            )
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Parent Auth Failed: Code '$authCode' does not match registered Pass Code (${child.regCode}) for ${child.name}."))
        }
    }

    // Direct Cloud Firestore Server Data Streams
    override fun getChildProfile(childId: String): Flow<ChildProfile?> =
        cloudSyncManager.getChildProfileFlow(childId)

    override fun getChildHealthRecords(childId: String): Flow<List<HealthRecord>> =
        cloudSyncManager.getHealthRecordsFlow(childId)

    override fun getChildVaccinations(childId: String): Flow<List<VaccinationRecord>> =
        cloudSyncManager.getVaccinationsFlow(childId)

    override fun getAllChildren(): Flow<List<ChildProfile>> =
        cloudSyncManager.getAllChildrenFlow()

    // Real-Time Notification Implementation delegated to dedicated NotificationRepository
    override fun getNotifications(childId: String?): Flow<List<AppNotification>> =
        notificationRepository.getNotifications(childId)

    override fun getUnreadNotificationCount(childId: String?): Flow<Int> =
        notificationRepository.getUnreadCount(childId)

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
    ): Long = notificationRepository.sendBroadcastNotification(
        titleEn = titleEn,
        titleMr = titleMr,
        titleHi = titleHi,
        messageEn = messageEn,
        messageMr = messageMr,
        messageHi = messageHi,
        type = type,
        priority = priority,
        targetChildId = targetChildId
    )

    override suspend fun markNotificationAsRead(notificationId: Long) {
        notificationRepository.markAsRead(notificationId)
    }

    override suspend fun markAllNotificationsAsRead() {
        notificationRepository.markAllAsRead()
    }

    override suspend fun toggleAttendance(childId: String, isPresent: Boolean): Unit = withContext(Dispatchers.Default) {
        // 1. Update live in Cloud Firestore database
        try {
            cloudSyncManager.toggleAttendanceInServer(childId, isPresent)
        } catch (e: Exception) {
            android.util.Log.w("ParentConnectRepo", "Cloud attendance toggle: ${e.message}")
        }

        val map = _attendanceMap.value.toMutableMap()
        map[childId] = isPresent
        _attendanceMap.value = map

        // Real-time notification to the child's guardian
        val child = dao.getChildByIdDirect(childId)
        val childName = child?.name ?: childId
        val statusTextEn = if (isPresent) "Present" else "Absent"
        val statusTextMr = if (isPresent) "उपस्थित" else "अनुपस्थित"
        val statusTextHi = if (isPresent) "उपस्थित" else "अनुपस्थित"

        sendBroadcastNotification(
            titleEn = "Daily Attendance: $childName",
            titleMr = "दैनिक उपस्थिती: $childName",
            titleHi = "दैनिक उपस्थिति: $childName",
            messageEn = "$childName has been marked $statusTextEn at Anganwadi Centre today.",
            messageMr = "$childName आज अंगणवाडी केंद्रात $statusTextMr नोंदवले गेले आहे.",
            messageHi = "$childName आज आंगनवाड़ी केंद्र में $statusTextHi दर्ज किया गया है।",
            type = NotificationType.ATTENDANCE_UPDATE,
            priority = NotificationPriority.NORMAL,
            targetChildId = childId
        )
        Unit
    }

    override suspend fun dispatchRationSchedule(schedule: RationSchedule): Unit = withContext(Dispatchers.Default) {
        // 1. Post to real-time Cloud Firestore (Phone A -> Phone B instant sync)
        try {
            cloudSyncManager.postRationScheduleToCloud(schedule)
        } catch (e: Exception) {
            android.util.Log.w("ParentConnectRepo", "Cloud ration dispatch queued/offline: ${e.message}")
        }

        // 2. Real-time broadcast notification to all parents for upcoming ration pickup!
        sendBroadcastNotification(
            titleEn = "Ration Distribution Scheduled: ${schedule.distributionDate}",
            titleMr = "पोषण आहार वाटप तारीख: ${schedule.distributionDate}",
            titleHi = "राशन वितरण तारीख: ${schedule.distributionDate}",
            messageEn = "Take-Home Ration distribution on ${schedule.distributionDate} at ${schedule.location} (${schedule.pickupTiming}). Please collect your packets.",
            messageMr = "${schedule.distributionDate} रोजी ${schedule.location} येथे (${schedule.pickupTiming}) पोषण आहार वाटप होणार आहे.",
            messageHi = "${schedule.distributionDate} को ${schedule.location} पर (${schedule.pickupTiming}) राशन वितरण किया जाएगा।",
            type = NotificationType.RATION_SCHEDULE,
            priority = NotificationPriority.HIGH,
            targetChildId = null // Broadcast to ALL parents
        )
        Unit
    }

    override suspend fun dispatchAnnouncement(announcement: CentreAnnouncement): Unit = withContext(Dispatchers.Default) {
        // Real-time broadcast to all parents for this announcement via Cloud Firestore
        sendBroadcastNotification(
            titleEn = announcement.titleEn,
            titleMr = announcement.titleMr,
            titleHi = announcement.titleHi,
            messageEn = announcement.descriptionEn,
            messageMr = announcement.descriptionMr,
            messageHi = announcement.descriptionHi,
            type = NotificationType.ANNOUNCEMENT,
            priority = if (announcement.isUrgent) NotificationPriority.URGENT else NotificationPriority.NORMAL,
            targetChildId = null // Broadcast to ALL parents
        )
        Unit
    }

    override suspend fun parseAndValidateUpload(uri: Uri, isExcel: Boolean): DiffImportResult = withContext(Dispatchers.IO) {
        val parseResult = CsvParser.parseUri(context, uri)
        buildDiffResult(parseResult.successfulRecords, parseResult.errors, uri.lastPathSegment ?: "records.xlsx")
    }

    override suspend fun parseSampleExcel(): DiffImportResult = withContext(Dispatchers.IO) {
        val parseResult = CsvParser.parseText(SampleDataGenerator.sampleCsvTemplate)
        buildDiffResult(parseResult.successfulRecords, parseResult.errors, "Anganwadi_Child_Service_Template.xlsx")
    }

    private suspend fun buildDiffResult(
        parsed: List<ParsedChildRecord>,
        errors: List<String>,
        fileName: String
    ): DiffImportResult {
        val existingChildren = dao.getAllChildren().firstOrNull() ?: emptyList()
        val existingMap = existingChildren.associateBy { it.childId }

        val newRecords = mutableListOf<ParsedChildRecord>()
        val updatedRecords = mutableListOf<ParsedChildRecord>()
        val duplicateIds = mutableListOf<String>()
        val seenInFile = mutableSetOf<String>()

        for (item in parsed) {
            if (seenInFile.contains(item.childId)) {
                duplicateIds.add(item.childId)
            }
            seenInFile.add(item.childId)

            if (existingMap.containsKey(item.childId)) {
                updatedRecords.add(item.copy(isUpdate = true))
            } else {
                newRecords.add(item.copy(isUpdate = false))
            }
        }

        return DiffImportResult(
            fileName = fileName,
            totalRows = parsed.size,
            newRecords = newRecords,
            updatedRecords = updatedRecords,
            duplicateIds = duplicateIds,
            syntaxErrors = errors
        )
    }

    override suspend fun commitImport(diff: DiffImportResult): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val allToCommit = diff.newRecords + diff.updatedRecords
            val profilesToCommit = mutableListOf<ChildProfile>()
            val healthToCommit = mutableListOf<HealthRecord>()
            var count = 0
            for (item in allToCommit) {
                val existing = dao.getChildByIdDirect(item.childId)
                val profile = ChildProfile(
                    id = existing?.id ?: 0,
                    childId = item.childId,
                    name = item.name,
                    dob = item.dob.ifEmpty { existing?.dob ?: "14-Aug-2023" },
                    ageMonths = item.ageMonths,
                    gender = item.gender,
                    parentName = item.guardian,
                    guardian = item.guardian,
                    parentContact = item.parentContact.ifEmpty { existing?.parentContact ?: "" },
                    regCode = item.regCode.ifEmpty { existing?.regCode ?: "ACT-AUTO-AW" },
                    anganwadiCenterId = "AW-Center-042",
                    anganwadiCenterName = "AW-Center-042 (Shivaji Nagar)",
                    address = existing?.address ?: "Shivaji Nagar",
                    nextVaccine = item.nextVaccine.ifEmpty { existing?.nextVaccine ?: "DPT Booster" },
                    avatarColorIndex = (item.childId.hashCode() and 0x7FFFFFFF) % 5
                )
                dao.upsertProfile(profile)

                val healthRecord = HealthRecord(
                    childId = item.childId,
                    date = item.date,
                    weightKg = item.weightKg,
                    heightCm = item.heightCm,
                    nutritionStatus = item.nutritionStatus,
                    attendanceDays = item.attendanceCount,
                    notes = item.notes
                )
                dao.insertHealthRecord(healthRecord)
                profilesToCommit.add(profile)
                healthToCommit.add(healthRecord)
                count++
            }

            // Sync all imported children and health records directly to Cloud Firestore database
            try {
                cloudSyncManager.commitImportToCloud(profilesToCommit, healthToCommit)
            } catch (e: Exception) {
                android.util.Log.w("ParentConnectRepo", "Cloud commit import: ${e.message}")
            }

            // Real-time broadcast notification of updated records
            sendBroadcastNotification(
                titleEn = "Records Updated by Sevika",
                titleMr = "सेविकेकडून बाल आरोग्य नोंदी अद्ययावत",
                titleHi = "कार्यकर्ता द्वारा बाल स्वास्थ्य रिकॉर्ड अपडेट",
                messageEn = "$count child growth and health records have been synchronized and updated by Anganwadi Sevika.",
                messageMr = "$count बालकांच्या पोषण व वाढीच्या नोंदी अंगणवाडी सेविकेकडून अद्ययावत करण्यात आल्या आहेत.",
                messageHi = "$count बच्चों के पोषण एवं विकास रिकॉर्ड आंगनवाड़ी कार्यकर्ता द्वारा अपडेट किए गए हैं।",
                type = NotificationType.ANNOUNCEMENT,
                priority = NotificationPriority.NORMAL
            )

            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getTemplateCsvContent(): String = SampleDataGenerator.sampleCsvTemplate

    override suspend fun getAiGrowthAdvice(childId: String, forceRefresh: Boolean): GrowthSummaryResult = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            summaryCache[childId]?.let { return@withContext it }
        }

        val profile = dao.getChildByIdDirect(childId)
            ?: return@withContext GrowthSummaryResult("रेकॉर्ड सापडला नाही", "Record not found")
        val latest = dao.getLatestHealthRecordDirect(childId)
        val allRecords = dao.getHealthRecords(childId).firstOrNull() ?: emptyList()
        val vaccinations = dao.getVaccinations(childId).firstOrNull() ?: emptyList()

        val summary = GeminiService.generateGrowthSummary(
            profile = profile,
            latestRecord = latest,
            previousRecords = allRecords,
            vaccinations = vaccinations
        )
        summaryCache[childId] = summary
        summary
    }

    override suspend fun resetDatabaseToInitial() = withContext(Dispatchers.IO) {
        dao.clearAllProfiles()
        dao.clearAllHealthRecords()
        dao.clearAllVaccinations()
        notificationDao.clearAllNotifications()
        helpTicketDao.clearAllTickets()
        rationHandoverDao.clearAllHandovers()

        dao.upsertProfiles(SampleDataGenerator.getInitialProfiles())
        dao.insertHealthRecords(SampleDataGenerator.getInitialHealthRecords())
        dao.insertVaccinations(SampleDataGenerator.getInitialVaccinations())
        notificationDao.insertNotifications(SampleDataGenerator.getInitialNotifications())
        helpTicketDao.insertTickets(SampleDataGenerator.getInitialHelpTickets())
        rationHandoverDao.upsertHandovers(SampleDataGenerator.getInitialHandovers())

        summaryCache.clear()
        _attendanceMap.value = SampleDataGenerator.initialAttendanceMap
    }

    companion object {
        @Volatile
        private var instance: ParentConnectRepository? = null

        fun getInstance(context: Context): ParentConnectRepository {
            return instance ?: synchronized(this) {
                val db = AppDatabase.getDatabase(context)
                val syncManager = com.example.data.remote.FirebaseCloudSyncManager.getInstance(context)
                val notifRepo = NotificationRepositoryImpl.getInstance(context)
                val newInstance = ParentConnectRepositoryImpl(
                    context.applicationContext,
                    db.childDao(),
                    db.notificationDao(),
                    db.helpTicketDao(),
                    db.rationHandoverDao(),
                    notifRepo,
                    syncManager
                )
                instance = newInstance
                newInstance
            }
        }
    }
}
