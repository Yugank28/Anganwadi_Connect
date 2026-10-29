package com.example.ui.admin

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChildProfile
import com.example.data.model.DiffImportResult
import com.example.data.model.NutritionStatus
import com.example.data.model.ParsedChildRecord
import com.example.ui.components.NutritionBadge
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.AppStrings
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.PrimaryIndigoContainer
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.StatusMAM
import com.example.ui.theme.StatusNormal
import com.example.ui.theme.StatusSAM
import com.example.ui.viewmodel.DiffUiState
import com.example.ui.viewmodel.PoshanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHubScreen(
    viewModel: PoshanViewModel,
    modifier: Modifier = Modifier
) {
    val language by viewModel.appLanguage.collectAsState()
    val session by viewModel.userSession.collectAsState()
    val allChildren by viewModel.allChildren.collectAsState()
    val attendanceMap by viewModel.attendanceMap.collectAsState()
    val diffState by viewModel.diffState.collectAsState()
    val broadcastTitle by viewModel.broadcastTitleInput.collectAsState()
    val broadcastMessage by viewModel.broadcastMessageInput.collectAsState()
    val broadcastPriority by viewModel.broadcastPriorityInput.collectAsState()
    val broadcastTarget by viewModel.broadcastTargetChildId.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val allHandovers by viewModel.allRationHandovers.collectAsState()
    val authCodeInput by viewModel.handoverAuthCodeInput.collectAsState()
    val filteredChildren by viewModel.filteredChildren.collectAsState()
    val searchQuery by viewModel.adminSearchQuery.collectAsState()
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }
    var showScheduleRationDialog by remember { mutableStateOf(false) }

    // Intercept back button to dismiss modal dialogs or logout cleanly
    BackHandler(enabled = true) {
        if (showScheduleRationDialog) {
            showScheduleRationDialog = false
        } else {
            viewModel.logout()
        }
    }

    // SAF Document Picker for Excel/CSV Workbooks
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val isExcel = it.toString().endsWith(".xlsx", ignoreCase = true) ||
                it.toString().endsWith(".xls", ignoreCase = true)
            viewModel.parseUploadFile(it, isExcel)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Rekha Tai Patil",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(PrimaryIndigoContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "ADMIN",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryIndigo
                                )
                            }
                        }
                        Text(
                            text = session.centerName,
                            fontSize = 11.sp,
                            color = Slate600
                        )
                    }
                },
                actions = {
                    // Trilingual Switcher
                    FilterChip(
                        selected = true,
                        onClick = {
                            val nextLang = when (language) {
                                AppLanguage.MARATHI -> AppLanguage.HINDI
                                AppLanguage.HINDI -> AppLanguage.ENGLISH
                                AppLanguage.ENGLISH -> AppLanguage.MARATHI
                            }
                            viewModel.setLanguage(nextLang)
                        },
                        label = {
                            Text(
                                text = "🌐 ${language.displayName}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryIndigoContainer,
                            selectedLabelColor = PrimaryIndigo
                        ),
                        modifier = Modifier.testTag("admin_lang_toggle")
                    )

                    IconButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier.testTag("admin_logout_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = Slate600)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Slate50)
                .padding(innerPadding)
        ) {
            // Always-Visible Download Official Template Hub (PRD v1.2)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .border(1.dp, PrimaryIndigo.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .testTag("download_template_hub"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Template",
                            tint = PrimaryIndigo,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = AppStrings.downloadTemplate(language),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Slate900
                            )
                            Text(
                                text = "Sheets: Children, Guardians, Growth, Rations, Vaccines",
                                fontSize = 10.sp,
                                color = Slate600
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val contentText = viewModel.getTemplateCsvText()
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Anganwadi_Child_Service_Template.xlsx")
                                putExtra(Intent.EXTRA_TEXT, contentText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Save Official Template"))
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        modifier = Modifier.testTag("btn_download_template")
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = "Download", modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export", fontSize = 12.sp, maxLines = 1, softWrap = false)
                    }
                }
            }

            // Tabs: [1. Attendance & Services] [2. Broadcast Alerts 📢] [3. Bulk File Import] [4. Roster] [5. Ration Auth 🔑]
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 12.dp,
                containerColor = Color.White,
                contentColor = PrimaryIndigo
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = AppStrings.dailyAttendance(language),
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("tab_attendance")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = if (language == AppLanguage.MARATHI) "संदेश 📢" else "Broadcast 📢",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("tab_broadcast")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text(
                            text = if (language == AppLanguage.MARATHI) "इम्पोर्ट" else "Import",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("tab_bulk_upload")
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = {
                        Text(
                            text = "Children (${allChildren.size})",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("tab_roster")
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = {
                        Text(
                            text = if (language == AppLanguage.MARATHI) "राशन पडताळणी 🔑" else "Ration Auth 🔑",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 4) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("tab_ration_auth")
                )
            }

            when (selectedTab) {
                0 -> AttendanceServicesTab(
                    children = allChildren,
                    attendanceMap = attendanceMap,
                    language = language,
                    onToggleAttendance = { id, present -> viewModel.toggleChildAttendance(id, present) },
                    onOpenScheduleRation = { showScheduleRationDialog = true },
                    onOpenBroadcast = { selectedTab = 1 },
                    onResetSeed = { viewModel.resetSeedData() }
                )
                1 -> AdminBroadcastTab(
                    titleInput = broadcastTitle,
                    messageInput = broadcastMessage,
                    priorityInput = broadcastPriority,
                    targetChildId = broadcastTarget,
                    allChildren = allChildren,
                    recentNotifications = notifications,
                    language = language,
                    onTitleChange = { viewModel.setBroadcastTitle(it) },
                    onMessageChange = { viewModel.setBroadcastMessage(it) },
                    onPriorityChange = { viewModel.setBroadcastPriority(it) },
                    onTargetChange = { viewModel.setBroadcastTarget(it) },
                    onSendBroadcast = { viewModel.sendBroadcastNotification() }
                )
                2 -> BulkExcelImportTab(
                    diffState = diffState,
                    language = language,
                    onSelectFile = {
                        filePickerLauncher.launch(
                            arrayOf(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "application/vnd.ms-excel",
                                "text/csv",
                                "text/comma-separated-values",
                                "*/*"
                            )
                        )
                    },
                    onLoadSampleTemplate = { viewModel.parseSampleExcelTemplate() },
                    onCommitDiff = { viewModel.commitDiffImport(it) },
                    onResetDiff = { viewModel.resetDiffState() }
                )
                3 -> ChildrenRosterTab(
                    children = filteredChildren,
                    searchQuery = searchQuery,
                    language = language,
                    onSearchChange = { viewModel.setAdminSearch(it) }
                )
                4 -> RationHandoverAuthTab(
                    children = allChildren,
                    handovers = allHandovers,
                    authCodeInput = authCodeInput,
                    language = language,
                    onAuthCodeChange = { viewModel.setHandoverAuthCodeInput(it) },
                    onVerifyHandover = { childId, code -> viewModel.verifyAndCompleteRationHandover(childId, code) }
                )
            }
        }
    }

    if (showScheduleRationDialog) {
        ScheduleRationDialog(
            language = language,
            onDismiss = { showScheduleRationDialog = false },
            onConfirm = { date, timing, venue ->
                viewModel.scheduleRation(date, timing, venue)
                showScheduleRationDialog = false
            }
        )
    }
}

@Composable
private fun AttendanceServicesTab(
    children: List<ChildProfile>,
    attendanceMap: Map<String, Boolean>,
    language: AppLanguage,
    onToggleAttendance: (String, Boolean) -> Unit,
    onOpenScheduleRation: () -> Unit,
    onOpenBroadcast: () -> Unit,
    onResetSeed: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Service Dispatch Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onOpenScheduleRation,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_dispatch_ration"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Icon(imageVector = Icons.Default.Fastfood, contentDescription = "Ration", modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (language == AppLanguage.MARATHI) "आहार वाटप" else "Rations",
                        fontSize = 11.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = onOpenBroadcast,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_quick_broadcast"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)) // Emerald
                ) {
                    Icon(imageVector = Icons.Default.Campaign, contentDescription = "Broadcast", modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (language == AppLanguage.MARATHI) "पालकांना अलर्ट 📢" else "Alert 📢",
                        fontSize = 11.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onResetSeed,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_reset_seed"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.RestartAlt, contentDescription = "Reset", modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Reset", fontSize = 11.sp, maxLines = 1, softWrap = false)
                }
            }
        }

        item {
            // Attendance Summary Banner
            val presentCount = children.count { attendanceMap[it.childId] == true }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate200))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (language == AppLanguage.MARATHI) "आजची दैनिक उपस्थिती" else "Today's Daily Attendance",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$presentCount उपस्थित / ${children.size} एकूण नोंदणीकृत",
                            fontSize = 12.sp,
                            color = Slate600,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    val pct = if (children.isNotEmpty()) (presentCount.toFloat() / children.size.toFloat() * 100).toInt() else 0
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFECFDF5))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "$pct% Present",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = StatusNormal,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        items(children) { child ->
            val isPresent = attendanceMap[child.childId] ?: true
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                    .testTag("attendance_row_${child.childId}"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = child.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${child.childId} • Guardian: ${child.guardian}",
                            fontSize = 11.sp,
                            color = Slate600,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Responsive Toggle Pills (Present / Absent)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Slate100)
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isPresent) StatusNormal else Color.Transparent)
                                .clickable { onToggleAttendance(child.childId, true) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("btn_present_${child.childId}")
                        ) {
                            Text(
                                text = AppStrings.present(language),
                                color = if (isPresent) Color.White else Slate700,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (!isPresent) StatusSAM else Color.Transparent)
                                .clickable { onToggleAttendance(child.childId, false) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("btn_absent_${child.childId}")
                        ) {
                            Text(
                                text = AppStrings.absent(language),
                                color = if (!isPresent) Color.White else Slate700,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BulkExcelImportTab(
    diffState: DiffUiState,
    language: AppLanguage,
    onSelectFile: () -> Unit,
    onLoadSampleTemplate: () -> Unit,
    onCommitDiff: (DiffImportResult) -> Unit,
    onResetDiff: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate200, RoundedCornerShape(16.dp))
                    .testTag("bulk_excel_import_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PrimaryIndigoContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = "Excel Upload",
                                tint = PrimaryIndigo,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = AppStrings.bulkUploadTitle(language),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Slate900
                            )
                            Text(
                                text = "Multi-sheet Excel Workbook (.xlsx / .csv)",
                                fontSize = 11.sp,
                                color = Slate600
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (language == AppLanguage.MARATHI)
                            "एकाच वेळी सर्व मुलांचे वजन, उंची व लसीकरण रेकॉर्ड्स एक्सेल फाईलद्वारे अपलोड करा. सिस्टम नवीन व अद्ययावत नोंदींची तुलना (Diff Preview) दाखवेल."
                        else
                            "Upload bulk child health records via Excel workbook. The engine generates an instant Diff Preview comparing new records with existing Child IDs.",
                        fontSize = 12.sp,
                        color = Slate700,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onSelectFile,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_select_excel_file"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                    ) {
                        Icon(imageVector = Icons.Default.FileUpload, contentDescription = "SAF Picker")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.MARATHI) "डिवाइसमधून .xlsx फाईल निवडा" else "Select .xlsx File (SAF)",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onLoadSampleTemplate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_load_sample_excel"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Description, contentDescription = "Sample", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("📄 Test Excel Ingestion (Sample Workbook)", fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            when (diffState) {
                is DiffUiState.Parsing -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = PrimaryIndigo)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Validating sheets & generating Diff Preview...")
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = PrimaryIndigo)
                        }
                    }
                }

                is DiffUiState.DiffPreview -> {
                    val diff = diffState.diff
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, PrimaryIndigo.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .testTag("diff_preview_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Difference, contentDescription = "Diff", tint = PrimaryIndigo)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Diff Preview: ${diff.fileName}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                                TextButton(onClick = onResetDiff) {
                                    Text("Dismiss", color = Slate600)
                                }
                            }

                            // Diff Summary Statistics Badges
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFECFDF5))
                                        .padding(8.dp)
                                ) {
                                    Column {
                                        Text(text = "NEW ROWS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StatusNormal)
                                        Text(text = "${diff.newRecords.size} records", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StatusNormal)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PrimaryIndigoContainer)
                                        .padding(8.dp)
                                ) {
                                    Column {
                                        Text(text = "MATCHED UPDATES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                                        Text(text = "${diff.updatedRecords.size} records", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Slate100)
                                        .padding(8.dp)
                                ) {
                                    Column {
                                        Text(text = "TOTAL PARSED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate700)
                                        Text(text = "${diff.totalRows} rows", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate900)
                                    }
                                }
                            }

                            // Detected Sheets Checklist
                            Text(
                                text = "Detected Workbook Sheets: ${diff.detectedSheets.joinToString(", ")}",
                                fontSize = 11.sp,
                                color = Slate600
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Record Preview & Validation Checks:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            val previewList = (diff.newRecords + diff.updatedRecords).take(5)
                            previewList.forEach { record ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${record.name} (${record.childId})",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(if (record.isUpdate) PrimaryIndigoContainer else Color(0xFFECFDF5))
                                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = if (record.isUpdate) "UPDATE" else "NEW",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (record.isUpdate) PrimaryIndigo else StatusNormal
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Guardian: ${record.guardian} • Wt: ${record.weightKg}kg • Ht: ${record.heightCm}cm",
                                            fontSize = 11.sp,
                                            color = Slate600
                                        )
                                    }
                                    NutritionBadge(status = record.nutritionStatus, language = language)
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = Slate100)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Controlled Commit Button
                            Button(
                                onClick = { onCommitDiff(diff) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("btn_commit_diff"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                            ) {
                                Icon(imageVector = Icons.Default.CloudDone, contentDescription = "Commit")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Commit & Upsert ${diff.totalValidCount} Records to Live State",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                is DiffUiState.Committing -> {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = PrimaryIndigo)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Saving and broadcasting changes to parent accounts...")
                        }
                    }
                }

                is DiffUiState.Success -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFECFDF5), RoundedCornerShape(14.dp))
                            .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Success", tint = StatusNormal, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "Import Successfully Committed!", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = StatusNormal)
                            Text(text = "${diffState.count} child records synchronized and published to linked parents.", fontSize = 12.sp, color = Slate700)
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onResetDiff,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = StatusNormal)
                            ) {
                                Text("Done")
                            }
                        }
                    }
                }

                is DiffUiState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "Error: ${diffState.message}", color = StatusSAM, fontSize = 13.sp)
                            TextButton(onClick = onResetDiff) { Text("Retry") }
                        }
                    }
                }

                DiffUiState.Idle -> {}
            }
        }
    }
}

@Composable
private fun ChildrenRosterTab(
    children: List<ChildProfile>,
    searchQuery: String,
    language: AppLanguage,
    onSearchChange: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                label = {
                    Text(
                        if (language == AppLanguage.MARATHI) "बालक, कोड किंवा पालकांचे नाव शोधा..."
                        else if (language == AppLanguage.HINDI) "बच्चे, कोड या अभिभावक का नाम खोजें..."
                        else "Search child name, ID, code or guardian..."
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryIndigo)
            )
        }

        if (children.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (language == AppLanguage.MARATHI) "कोणताही बालक सापडला नाही." else "No matching child record found.",
                            fontSize = 12.sp,
                            color = Slate600
                        )
                    }
                }
            }
        } else {
            items(children) { child ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Slate200, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = child.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Slate900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${child.childId} • DOB: ${child.dob} • Code: ${child.regCode}",
                                fontSize = 11.sp,
                                color = PrimaryIndigo,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Guardian: ${child.guardian} (📞 ${child.parentContact})",
                                fontSize = 11.sp,
                                color = Slate600,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFECFDF5))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Linked ✅",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusNormal,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleRationDialog(
    language: AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: (date: String, timing: String, venue: String) -> Unit
) {
    var date by remember { mutableStateOf("26-Oct-2026") }
    var timing by remember { mutableStateOf("09:00 AM – 01:30 PM") }
    var venue by remember { mutableStateOf("AW-Center-042 (Shivaji Nagar Hall)") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (language == AppLanguage.MARATHI) "पोषण आहार वाटप निश्चित करा" else "Schedule Ration Distribution",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Distribution Date") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_ration_date")
                )
                OutlinedTextField(
                    value = timing,
                    onValueChange = { timing = it },
                    label = { Text("Pickup Timing") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_ration_timing")
                )
                OutlinedTextField(
                    value = venue,
                    onValueChange = { venue = it },
                    label = { Text("Pickup Location / Venue") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_ration_venue")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(date, timing, venue) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                modifier = Modifier.testTag("dialog_ration_confirm")
            ) {
                Text("Broadcast Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun RationHandoverAuthTab(
    children: List<ChildProfile>,
    handovers: List<com.example.data.model.RationHandover>,
    authCodeInput: String,
    language: AppLanguage,
    onAuthCodeChange: (String) -> Unit,
    onVerifyHandover: (childId: String, code: String) -> Unit
) {
    var selectedChildId by remember { mutableStateOf(children.firstOrNull()?.childId ?: "AW-042-01") }
    val selectedChild = children.firstOrNull { it.childId == selectedChildId } ?: children.firstOrNull()
    val handoverMap = handovers.associateBy { it.childId }
    val currentHandover = selectedChild?.let { handoverMap[it.childId] }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PrimaryIndigo.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PrimaryIndigoContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fastfood,
                                contentDescription = null,
                                tint = PrimaryIndigo,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (language == AppLanguage.MARATHI) "पोषण आहार वाटप व पालक पडताळणी"
                                else "Ration Handover & Parent Authenticator",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Slate900
                            )
                            Text(
                                text = "Verify parent Pass Code or Mobile before handing over ration packets",
                                fontSize = 11.sp,
                                color = Slate600
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (language == AppLanguage.MARATHI) "१. बालक व पालक निवडा:" else "1. Select Child / Guardian:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Child selector list
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        children.forEach { child ->
                            val isDone = handoverMap[child.childId]?.isHandedOver == true
                            val isSel = child.childId == selectedChildId

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = if (isSel) 2.dp else 1.dp,
                                        color = if (isSel) PrimaryIndigo else Slate200,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedChildId = child.childId },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSel) PrimaryIndigoContainer else Color.White
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${child.name} (${child.childId})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Slate900
                                        )
                                        Text(
                                            text = "Guardian: ${child.guardian} • Pass Code: ${child.regCode}",
                                            fontSize = 11.sp,
                                            color = Slate600
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isDone) Color(0xFFECFDF5) else Color(0xFFFEF3C7))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isDone) "✅ Delivered" else "⌛ Pending",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDone) StatusNormal else Color(0xFFD97706)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    selectedChild?.let { child ->
                        HorizontalDivider(color = Slate200)

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (language == AppLanguage.MARATHI) "२. पालकांचा सुरक्षा कोड प्रविष्ट करा:" else "2. Enter Parent Security Pass Code / PIN:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = authCodeInput,
                            onValueChange = onAuthCodeChange,
                            label = { Text("Enter Code (e.g. ${child.regCode})") },
                            placeholder = { Text(child.regCode) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryIndigo)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick demo code fill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "Quick Demo Code:", fontSize = 11.sp, color = Slate600)
                            FilterChip(
                                selected = false,
                                onClick = { onAuthCodeChange(child.regCode) },
                                label = { Text(child.regCode, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (currentHandover?.isHandedOver == true) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFECFDF5))
                                    .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusNormal, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "AUTHENTICATED & HANDED OVER",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = StatusNormal
                                        )
                                        Text(
                                            text = "Verified on ${currentHandover.formattedHandoverTime} by ${currentHandover.verifiedBySevika}",
                                            fontSize = 11.sp,
                                            color = Slate900
                                        )
                                    }
                                }
                            }
                        } else {
                            Button(
                                onClick = { onVerifyHandover(child.childId, authCodeInput) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (language == AppLanguage.MARATHI) "पालक पडताळणी व राशन वाटप पूर्ण करा"
                                    else "Verify Parent & Complete Handover",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
