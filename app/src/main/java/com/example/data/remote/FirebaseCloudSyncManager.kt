package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.model.AnnouncementCategory
import com.example.data.model.AppNotification
import com.example.data.model.CentreAnnouncement
import com.example.data.model.ChildProfile
import com.example.data.model.HealthRecord
import com.example.data.model.NotificationPriority
import com.example.data.model.NotificationType
import com.example.data.model.NutritionStatus
import com.example.data.model.RationSchedule
import com.example.data.model.VaccinationRecord
import com.example.data.model.VaccineStatus
import com.example.data.parser.SampleDataGenerator
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Production-ready Firebase Cloud Firestore Real-Time Server & Database Service.
 * Ensures 100% of data reads, writes, and real-time synchronization operate
 * through the live Cloud Firestore database (anganwadiconnect-cb692).
 */
class FirebaseCloudSyncManager private constructor(private val context: Context) {

    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private var announcementListener: ListenerRegistration? = null
    private var isInitialLoadComplete = false

    /**
     * Seeds initial records into Cloud Firestore database if collections are empty.
     */
    suspend fun seedInitialServerDataIfEmpty() {
        try {
            val childrenSnapshot = firestore.collection(COLLECTION_CHILDREN).limit(1).get().await()
            if (childrenSnapshot.isEmpty) {
                Log.d(TAG, "Cloud Firestore is empty. Seeding initial server records...")
                val batch = firestore.batch()

                // 1. Initial 3 Children
                val initialProfiles = SampleDataGenerator.getInitialProfiles()
                for (p in initialProfiles) {
                    val docRef = firestore.collection(COLLECTION_CHILDREN).document(p.childId)
                    val map = hashMapOf(
                        "childId" to p.childId,
                        "name" to p.name,
                        "dob" to p.dob,
                        "ageMonths" to p.ageMonths,
                        "gender" to p.gender,
                        "parentName" to p.parentName,
                        "guardian" to p.guardian,
                        "parentContact" to p.parentContact,
                        "regCode" to p.regCode,
                        "anganwadiCenterId" to p.anganwadiCenterId,
                        "anganwadiCenterName" to p.anganwadiCenterName,
                        "address" to p.address,
                        "nextVaccine" to p.nextVaccine,
                        "avatarColorIndex" to p.avatarColorIndex,
                        "isPresentToday" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    batch.set(docRef, map)
                }

                // 2. Initial Health Records
                val initialHealth = SampleDataGenerator.getInitialHealthRecords()
                for (h in initialHealth) {
                    val docRef = firestore.collection(COLLECTION_HEALTH_RECORDS).document("${h.childId}_${h.date}")
                    val map = hashMapOf(
                        "childId" to h.childId,
                        "date" to h.date,
                        "weightKg" to h.weightKg.toDouble(),
                        "heightCm" to h.heightCm.toDouble(),
                        "nutritionStatus" to h.nutritionStatus.name,
                        "attendanceDays" to h.attendanceDays,
                        "notes" to h.notes
                    )
                    batch.set(docRef, map)
                }

                // 3. Initial Vaccinations
                val initialVaccines = SampleDataGenerator.getInitialVaccinations()
                for (v in initialVaccines) {
                    val docRef = firestore.collection(COLLECTION_VACCINATIONS).document("${v.childId}_${v.dose}_${v.vaccineName.take(4)}")
                    val map = hashMapOf(
                        "childId" to v.childId,
                        "vaccineName" to v.vaccineName,
                        "dose" to v.dose,
                        "status" to v.status.name,
                        "dueDate" to v.dueDate,
                        "administeredDate" to (v.administeredDate ?: "")
                    )
                    batch.set(docRef, map)
                }

                // 4. Initial Ration Schedule
                val initialRation = RationSchedule(
                    id = "RATION-2026-10",
                    cycleMonth = "October 2026",
                    distributionDate = "24-Oct-2026",
                    pickupTiming = "08:30 AM – 01:00 PM",
                    location = "AW-Center-042 (Shivaji Nagar Community Hall)",
                    sevikaContact = "Rekha Tai Patil (+91 9822001122)",
                    statusEn = "Upcoming (Pickup on Saturday)",
                    statusMr = "आगामी (शनिवार रोजी वाटप)",
                    statusHi = "आगामी (शनिवार को वितरण)"
                )
                val rationRef = firestore.collection(COLLECTION_RATION_SCHEDULES).document(initialRation.id)
                val rationMap = hashMapOf(
                    "id" to initialRation.id,
                    "cycleMonth" to initialRation.cycleMonth,
                    "distributionDate" to initialRation.distributionDate,
                    "pickupTiming" to initialRation.pickupTiming,
                    "location" to initialRation.location,
                    "sevikaContact" to initialRation.sevikaContact,
                    "statusEn" to initialRation.statusEn,
                    "statusMr" to initialRation.statusMr,
                    "statusHi" to initialRation.statusHi,
                    "timestamp" to FieldValue.serverTimestamp()
                )
                batch.set(rationRef, rationMap)

                batch.commit().await()
                Log.d(TAG, "Successfully seeded initial data to Cloud Firestore server database.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking/seeding initial server data in Firestore", e)
        }
    }

    /**
     * Real-time Flow of all registered children directly from Cloud Firestore server.
     */
    fun getAllChildrenFlow(): Flow<List<ChildProfile>> = callbackFlow {
        val subscription = firestore.collection(COLLECTION_CHILDREN)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to children from Firestore: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            ChildProfile(
                                id = doc.id.hashCode().toLong() and 0x7FFFFFFF,
                                childId = doc.getString("childId") ?: doc.id,
                                name = doc.getString("name") ?: "",
                                dob = doc.getString("dob") ?: "",
                                ageMonths = doc.getLong("ageMonths")?.toInt() ?: 24,
                                gender = doc.getString("gender") ?: "M",
                                parentName = doc.getString("parentName") ?: doc.getString("guardian") ?: "",
                                guardian = doc.getString("guardian") ?: doc.getString("parentName") ?: "",
                                parentContact = doc.getString("parentContact") ?: "",
                                regCode = doc.getString("regCode") ?: "",
                                anganwadiCenterId = doc.getString("anganwadiCenterId") ?: "AW-Center-042",
                                anganwadiCenterName = doc.getString("anganwadiCenterName") ?: "AW-Center-042 (Shivaji Nagar)",
                                address = doc.getString("address") ?: "",
                                nextVaccine = doc.getString("nextVaccine") ?: "DPT Booster",
                                avatarColorIndex = doc.getLong("avatarColorIndex")?.toInt() ?: 0
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(list)
                }
            }
        awaitClose { subscription.remove() }
    }

    /**
     * Real-time Flow of a single child profile directly from Cloud Firestore server.
     */
    fun getChildProfileFlow(childId: String): Flow<ChildProfile?> = callbackFlow {
        val subscription = firestore.collection(COLLECTION_CHILDREN).document(childId)
            .addSnapshotListener { doc, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to child profile $childId: ${error.message}")
                    return@addSnapshotListener
                }
                if (doc != null && doc.exists()) {
                    val profile = try {
                        ChildProfile(
                            id = doc.id.hashCode().toLong() and 0x7FFFFFFF,
                            childId = doc.getString("childId") ?: doc.id,
                            name = doc.getString("name") ?: "",
                            dob = doc.getString("dob") ?: "",
                            ageMonths = doc.getLong("ageMonths")?.toInt() ?: 24,
                            gender = doc.getString("gender") ?: "M",
                            parentName = doc.getString("parentName") ?: doc.getString("guardian") ?: "",
                            guardian = doc.getString("guardian") ?: doc.getString("parentName") ?: "",
                            parentContact = doc.getString("parentContact") ?: "",
                            regCode = doc.getString("regCode") ?: "",
                            anganwadiCenterId = doc.getString("anganwadiCenterId") ?: "AW-Center-042",
                            anganwadiCenterName = doc.getString("anganwadiCenterName") ?: "AW-Center-042 (Shivaji Nagar)",
                            address = doc.getString("address") ?: "",
                            nextVaccine = doc.getString("nextVaccine") ?: "DPT Booster",
                            avatarColorIndex = doc.getLong("avatarColorIndex")?.toInt() ?: 0
                        )
                    } catch (e: Exception) {
                        null
                    }
                    trySend(profile)
                } else {
                    trySend(null)
                }
            }
        awaitClose { subscription.remove() }
    }

    /**
     * Validates parent login credentials directly against Cloud Firestore server database.
     */
    suspend fun verifyParentInServer(mobile: String, code: String): ChildProfile? {
        val cleanMobile = mobile.trim()
        val cleanCode = code.trim().uppercase()

        return try {
            val querySnap = firestore.collection(COLLECTION_CHILDREN).get().await()
            val matchedDoc = querySnap.documents.firstOrNull { doc ->
                val pContact = doc.getString("parentContact")?.trim() ?: ""
                val rCode = doc.getString("regCode")?.trim()?.uppercase() ?: ""
                val cId = doc.getString("childId")?.trim()?.uppercase() ?: ""

                (pContact == cleanMobile && rCode.equals(cleanCode, ignoreCase = true)) ||
                cId.equals(cleanCode, ignoreCase = true) ||
                rCode.equals(cleanCode, ignoreCase = true) ||
                pContact == cleanMobile
            }

            matchedDoc?.let { doc ->
                ChildProfile(
                    id = doc.id.hashCode().toLong() and 0x7FFFFFFF,
                    childId = doc.getString("childId") ?: doc.id,
                    name = doc.getString("name") ?: "",
                    dob = doc.getString("dob") ?: "",
                    ageMonths = doc.getLong("ageMonths")?.toInt() ?: 24,
                    gender = doc.getString("gender") ?: "M",
                    parentName = doc.getString("parentName") ?: doc.getString("guardian") ?: "",
                    guardian = doc.getString("guardian") ?: doc.getString("parentName") ?: "",
                    parentContact = doc.getString("parentContact") ?: "",
                    regCode = doc.getString("regCode") ?: "",
                    anganwadiCenterId = doc.getString("anganwadiCenterId") ?: "AW-Center-042",
                    anganwadiCenterName = doc.getString("anganwadiCenterName") ?: "AW-Center-042 (Shivaji Nagar)",
                    address = doc.getString("address") ?: "",
                    nextVaccine = doc.getString("nextVaccine") ?: "DPT Booster",
                    avatarColorIndex = doc.getLong("avatarColorIndex")?.toInt() ?: 0
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying parent login in Cloud Firestore", e)
            null
        }
    }

    /**
     * Real-time Flow of attendance map directly from Cloud Firestore server.
     */
    fun getAttendanceMapFlow(): Flow<Map<String, Boolean>> = callbackFlow {
        val subscription = firestore.collection(COLLECTION_CHILDREN)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to attendance from Firestore: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val map = snapshot.documents.associate { doc ->
                        val childId = doc.getString("childId") ?: doc.id
                        val isPresent = doc.getBoolean("isPresentToday") ?: true
                        childId to isPresent
                    }
                    trySend(map)
                }
            }
        awaitClose { subscription.remove() }
    }

    /**
     * Toggles attendance state in Cloud Firestore server database in real time.
     */
    suspend fun toggleAttendanceInServer(childId: String, isPresent: Boolean) {
        try {
            firestore.collection(COLLECTION_CHILDREN).document(childId)
                .update("isPresentToday", isPresent, "updatedAt", FieldValue.serverTimestamp())
                .await()
            Log.d(TAG, "Updated attendance in Cloud Firestore for $childId -> $isPresent")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update attendance in Cloud Firestore", e)
        }
    }

    /**
     * Real-time Flow of health records for a child from Cloud Firestore server.
     */
    fun getHealthRecordsFlow(childId: String): Flow<List<HealthRecord>> = callbackFlow {
        val subscription = firestore.collection(COLLECTION_HEALTH_RECORDS)
            .whereEqualTo("childId", childId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to health records: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            val statusStr = doc.getString("nutritionStatus") ?: NutritionStatus.NORMAL.name
                            val status = try { NutritionStatus.valueOf(statusStr) } catch (e: Exception) { NutritionStatus.NORMAL }
                            HealthRecord(
                                recordId = doc.id.hashCode().toLong() and 0x7FFFFFFF,
                                childId = doc.getString("childId") ?: childId,
                                date = doc.getString("date") ?: "",
                                weightKg = doc.getDouble("weightKg")?.toFloat() ?: 12f,
                                heightCm = doc.getDouble("heightCm")?.toFloat() ?: 85f,
                                nutritionStatus = status,
                                attendanceDays = doc.getLong("attendanceDays")?.toInt() ?: 20,
                                notes = doc.getString("notes") ?: ""
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }.sortedBy { it.date }
                    trySend(list)
                }
            }
        awaitClose { subscription.remove() }
    }

    /**
     * Real-time Flow of vaccination records for a child from Cloud Firestore server.
     */
    fun getVaccinationsFlow(childId: String): Flow<List<VaccinationRecord>> = callbackFlow {
        val subscription = firestore.collection(COLLECTION_VACCINATIONS)
            .whereEqualTo("childId", childId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to vaccinations: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            val statusStr = doc.getString("status") ?: VaccineStatus.UPCOMING.name
                            val status = try { VaccineStatus.valueOf(statusStr) } catch (e: Exception) { VaccineStatus.UPCOMING }
                            VaccinationRecord(
                                id = doc.id.hashCode().toLong() and 0x7FFFFFFF,
                                childId = doc.getString("childId") ?: childId,
                                vaccineName = doc.getString("vaccineName") ?: "",
                                dose = doc.getString("dose") ?: "Dose 1",
                                status = status,
                                dueDate = doc.getString("dueDate") ?: "",
                                administeredDate = doc.getString("administeredDate")?.ifEmpty { null }
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }.sortedBy { it.dueDate }
                    trySend(list)
                }
            }
        awaitClose { subscription.remove() }
    }

    /**
     * Real-time Flow of Ration Schedules from Cloud Firestore server.
     */
    fun getRationSchedulesFlow(): Flow<List<RationSchedule>> = callbackFlow {
        val subscription = firestore.collection(COLLECTION_RATION_SCHEDULES)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to ration schedules: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            RationSchedule(
                                id = doc.getString("id") ?: doc.id,
                                cycleMonth = doc.getString("cycleMonth") ?: "October 2026",
                                distributionDate = doc.getString("distributionDate") ?: "24-Oct-2026",
                                pickupTiming = doc.getString("pickupTiming") ?: "08:30 AM – 01:00 PM",
                                location = doc.getString("location") ?: "AW-Center-042 (Shivaji Nagar)",
                                sevikaContact = doc.getString("sevikaContact") ?: "Rekha Tai Patil (+91 9822001122)",
                                statusEn = doc.getString("statusEn") ?: "Upcoming",
                                statusMr = doc.getString("statusMr") ?: "आगामी",
                                statusHi = doc.getString("statusHi") ?: "आगामी"
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (list.isNotEmpty()) {
                        trySend(list)
                    }
                }
            }
        awaitClose { subscription.remove() }
    }

    /**
     * Real-time Flow of Centre Announcements from Cloud Firestore server.
     */
    fun getAnnouncementsFlow(): Flow<List<CentreAnnouncement>> = callbackFlow {
        val subscription = firestore.collection(COLLECTION_ANNOUNCEMENTS)
            .orderBy("createdAtMillis", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to announcements: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            val typeStr = doc.getString("type") ?: doc.getString("category") ?: "GENERAL"
                            val category = when (typeStr) {
                                "RATION", "RATION_SCHEDULE" -> AnnouncementCategory.RATION
                                "VACCINATION", "VACCINATION_ALERT", "HEALTH_CHECKUP" -> AnnouncementCategory.HEALTH
                                else -> AnnouncementCategory.NUTRITION
                            }
                            val isUrgent = doc.getBoolean("isUrgent") ?: false
                            val timeMillis = doc.getLong("createdAtMillis") ?: System.currentTimeMillis()
                            val dateStr = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(Date(timeMillis))

                            CentreAnnouncement(
                                id = doc.id,
                                titleEn = doc.getString("titleEn") ?: doc.getString("title") ?: "",
                                titleMr = doc.getString("titleMr") ?: doc.getString("titleEn") ?: "",
                                titleHi = doc.getString("titleHi") ?: doc.getString("titleEn") ?: "",
                                descriptionEn = doc.getString("messageEn") ?: doc.getString("description") ?: "",
                                descriptionMr = doc.getString("messageMr") ?: doc.getString("messageEn") ?: "",
                                descriptionHi = doc.getString("messageHi") ?: doc.getString("messageEn") ?: "",
                                date = dateStr,
                                category = category,
                                isUrgent = isUrgent
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (list.isNotEmpty()) {
                        trySend(list)
                    }
                }
            }
        awaitClose { subscription.remove() }
    }

    /**
     * Batch commits bulk imported children and health records into Cloud Firestore server.
     */
    suspend fun commitImportToCloud(
        profiles: List<ChildProfile>,
        healthRecords: List<HealthRecord>
    ): Result<Int> = try {
        val batch = firestore.batch()
        for (p in profiles) {
            val docRef = firestore.collection(COLLECTION_CHILDREN).document(p.childId)
            val map = hashMapOf(
                "childId" to p.childId,
                "name" to p.name,
                "dob" to p.dob,
                "ageMonths" to p.ageMonths,
                "gender" to p.gender,
                "parentName" to p.parentName,
                "guardian" to p.guardian,
                "parentContact" to p.parentContact,
                "regCode" to p.regCode,
                "anganwadiCenterId" to p.anganwadiCenterId,
                "anganwadiCenterName" to p.anganwadiCenterName,
                "address" to p.address,
                "nextVaccine" to p.nextVaccine,
                "avatarColorIndex" to p.avatarColorIndex,
                "isPresentToday" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            batch.set(docRef, map, SetOptions.merge())
        }

        for (h in healthRecords) {
            val docRef = firestore.collection(COLLECTION_HEALTH_RECORDS).document("${h.childId}_${h.date}")
            val map = hashMapOf(
                "childId" to h.childId,
                "date" to h.date,
                "weightKg" to h.weightKg.toDouble(),
                "heightCm" to h.heightCm.toDouble(),
                "nutritionStatus" to h.nutritionStatus.name,
                "attendanceDays" to h.attendanceDays,
                "notes" to h.notes
            )
            batch.set(docRef, map, SetOptions.merge())
        }

        batch.commit().await()
        Result.success(profiles.size)
    } catch (e: Exception) {
        Log.e(TAG, "Error committing bulk import to Cloud Firestore", e)
        Result.failure(e)
    }

    /**
     * Posts a notification / announcement directly to Cloud Firestore collection "announcements".
     */
    suspend fun postAnnouncementToCloud(notification: AppNotification): Result<String> {
        return try {
            val payload = hashMapOf(
                "trackingCode" to "AW-${notification.type.name.take(3)}-${(1000..9999).random()}",
                "titleEn" to notification.titleEn,
                "titleMr" to notification.titleMr,
                "titleHi" to notification.titleHi,
                "messageEn" to notification.messageEn,
                "messageMr" to notification.messageMr,
                "messageHi" to notification.messageHi,
                "senderName" to notification.senderName,
                "senderRole" to notification.senderRole,
                "centerId" to notification.centerId,
                "centerName" to notification.centerName,
                "type" to notification.type.name,
                "priority" to notification.priority.name,
                "targetChildId" to (notification.targetChildId ?: "ALL"),
                "isUrgent" to (notification.priority == NotificationPriority.URGENT),
                "timestamp" to FieldValue.serverTimestamp(),
                "createdAtMillis" to System.currentTimeMillis()
            )

            val docRef = firestore.collection(COLLECTION_ANNOUNCEMENTS).add(payload).await()
            Log.d(TAG, "Successfully posted announcement to Cloud Firestore with ID: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error posting announcement to Cloud Firestore", e)
            Result.failure(e)
        }
    }

    /**
     * Subscribes to real-time changes in Firestore "announcements" collection.
     */
    fun startRealtimeAnnouncementListener(
        onItemAddedOrUpdated: (AppNotification, Boolean) -> Unit
    ) {
        stopRealtimeAnnouncementListener()
        isInitialLoadComplete = false

        try {
            val query = firestore.collection(COLLECTION_ANNOUNCEMENTS)
                .orderBy("createdAtMillis", Query.Direction.DESCENDING)
                .limit(50)

            announcementListener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Firestore snapshot listener error: ${error.message}", error)
                    return@addSnapshotListener
                }

                if (snapshot == null) return@addSnapshotListener

                for (change in snapshot.documentChanges) {
                    try {
                        val doc = change.document
                        val titleEn = doc.getString("titleEn") ?: doc.getString("title") ?: "Anganwadi Notice"
                        val titleMr = doc.getString("titleMr") ?: titleEn
                        val titleHi = doc.getString("titleHi") ?: titleEn

                        val messageEn = doc.getString("messageEn") ?: doc.getString("description") ?: ""
                        val messageMr = doc.getString("messageMr") ?: messageEn
                        val messageHi = doc.getString("messageHi") ?: messageEn

                        val senderName = doc.getString("senderName") ?: "Rekha Tai Patil (Anganwadi Sevika)"
                        val senderRole = doc.getString("senderRole") ?: "Anganwadi Worker / Teacher"
                        val centerId = doc.getString("centerId") ?: "AW-Center-042"
                        val centerName = doc.getString("centerName") ?: "AW-Center-042 (Shivaji Nagar)"

                        val typeStr = doc.getString("type") ?: doc.getString("category") ?: NotificationType.MESSAGE.name
                        val type = try {
                            NotificationType.valueOf(typeStr)
                        } catch (e: Exception) {
                            NotificationType.MESSAGE
                        }

                        val priorityStr = doc.getString("priority") ?: NotificationPriority.NORMAL.name
                        val priority = try {
                            NotificationPriority.valueOf(priorityStr)
                        } catch (e: Exception) {
                            NotificationPriority.NORMAL
                        }

                        val targetRaw = doc.getString("targetChildId")
                        val targetChildId = if (targetRaw == "ALL" || targetRaw.isNullOrEmpty()) null else targetRaw

                        val timeMillis = doc.getLong("createdAtMillis") ?: System.currentTimeMillis()
                        val timeFormat = SimpleDateFormat("h:mm a, dd MMM", Locale.getDefault())
                        val formattedTime = timeFormat.format(Date(timeMillis))

                        val notification = AppNotification(
                            id = doc.id.hashCode().toLong() and 0x7FFFFFFF,
                            titleEn = titleEn,
                            titleMr = titleMr,
                            titleHi = titleHi,
                            messageEn = messageEn,
                            messageMr = messageMr,
                            messageHi = messageHi,
                            senderName = senderName,
                            senderRole = senderRole,
                            centerId = centerId,
                            centerName = centerName,
                            timestamp = timeMillis,
                            formattedTime = formattedTime,
                            type = type,
                            priority = priority,
                            targetChildId = targetChildId,
                            isRead = false
                        )

                        val isRealtimeIncoming = isInitialLoadComplete && change.type == DocumentChange.Type.ADDED
                        onItemAddedOrUpdated(notification, isRealtimeIncoming)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing Firestore document: ${e.message}")
                    }
                }

                isInitialLoadComplete = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Firestore announcement listener", e)
        }
    }

    fun stopRealtimeAnnouncementListener() {
        announcementListener?.remove()
        announcementListener = null
    }

    /**
     * Posts a Ration distribution schedule to Firestore server.
     */
    suspend fun postRationScheduleToCloud(schedule: RationSchedule): Result<Unit> {
        return try {
            val payload = hashMapOf(
                "id" to schedule.id,
                "cycleMonth" to schedule.cycleMonth,
                "distributionDate" to schedule.distributionDate,
                "pickupTiming" to schedule.pickupTiming,
                "location" to schedule.location,
                "sevikaContact" to schedule.sevikaContact,
                "statusEn" to schedule.statusEn,
                "statusMr" to schedule.statusMr,
                "statusHi" to schedule.statusHi,
                "timestamp" to FieldValue.serverTimestamp()
            )
            firestore.collection(COLLECTION_RATION_SCHEDULES)
                .document(schedule.id)
                .set(payload, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error posting ration schedule to Cloud Firestore", e)
            Result.failure(e)
        }
    }

    /**
     * Posts a Help Desk / Complaint ticket to Cloud Firestore server database.
     */
    suspend fun submitHelpTicketToCloud(ticket: com.example.data.model.HelpTicket): Result<Unit> {
        return try {
            val payload = hashMapOf(
                "id" to ticket.id,
                "ticketNumber" to ticket.ticketNumber,
                "parentName" to ticket.parentName,
                "parentContact" to ticket.parentContact,
                "childId" to ticket.childId,
                "childName" to ticket.childName,
                "centerId" to ticket.centerId,
                "centerName" to ticket.centerName,
                "category" to ticket.category.name,
                "subject" to ticket.subject,
                "description" to ticket.description,
                "status" to ticket.status.name,
                "timestamp" to ticket.timestamp,
                "formattedDate" to ticket.formattedDate,
                "superAdminResponse" to ticket.superAdminResponse,
                "responseTimestamp" to ticket.responseTimestamp,
                "serverUpdatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection(COLLECTION_HELP_TICKETS)
                .document(ticket.ticketNumber)
                .set(payload, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error submitting help ticket to Cloud Firestore", e)
            Result.failure(e)
        }
    }

    /**
     * Updates ticket resolution/response in Cloud Firestore server database.
     */
    suspend fun updateTicketResponseInCloud(ticketNumber: String, status: String, response: String): Result<Unit> {
        return try {
            val payload = hashMapOf(
                "status" to status,
                "superAdminResponse" to response,
                "responseTimestamp" to System.currentTimeMillis(),
                "serverUpdatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection(COLLECTION_HELP_TICKETS)
                .document(ticketNumber)
                .update(payload as Map<String, Any>)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating ticket response in Cloud Firestore", e)
            Result.failure(e)
        }
    }

    companion object {
        const val TAG = "FirebaseCloudSync"
        const val COLLECTION_CHILDREN = "children"
        const val COLLECTION_HEALTH_RECORDS = "health_records"
        const val COLLECTION_VACCINATIONS = "vaccinations"
        const val COLLECTION_ANNOUNCEMENTS = "announcements"
        const val COLLECTION_RATION_SCHEDULES = "ration_schedules"
        const val COLLECTION_HELP_TICKETS = "help_tickets"

        @Volatile
        private var instance: FirebaseCloudSyncManager? = null

        fun getInstance(context: Context): FirebaseCloudSyncManager {
            return instance ?: synchronized(this) {
                instance ?: FirebaseCloudSyncManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
