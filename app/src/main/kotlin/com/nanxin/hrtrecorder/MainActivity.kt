// Android 版作者：南盺
// Source-level references: Journey, HRT-Recorder-online, hrt.mahiro.uk, Transmtf-HRT-Tracker, MtF-wiki.
package com.nanxin.hrtrecorder

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.util.Locale
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

private enum class Screen(val labelZh: String, val labelEn: String, val icon: String) {
    Overview("概览", "Home", "⌁"),
    Records("记录", "Record", "▤"),
    Labs("化验", "Labs", "⚗"),
    Bottles("药瓶", "Bottle", "▣"),
    Cup("罩杯", "Cup", "◒"),
    Plan("计划", "Plan", "◈"),
    Settings("设置", "Settings", "⚙")
}

private fun Screen.label(language: AppLanguage): String =
    language.t(labelZh, labelEn)

private fun newBottle(timeH: Double): PillBottle =
    PillBottle(
        createdTimeH = timeH,
        totalUnits = 30.0,
        remainingUnits = 30.0,
        perDoseUnits = 1.0,
    )

private data class HeaderBadge(
    val label: String,
    val detail: String,
    val accent: Color,
)

class MainActivity : ComponentActivity() {
    private lateinit var store: NativeStore
    private var pendingExportContents: String? = null
    private var pendingImageBytes: ByteArray? = null
    private var importSnapshotHandler: ((AppStateSnapshot) -> Unit)? = null
    private var importErrorHandler: (() -> Unit)? = null
    private var currentAppLanguage: AppLanguage = AppLanguage.ZhHans

    private val createJsonDocument = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> writePendingExport(uri) }

    private val createCsvDocument = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri -> writePendingExport(uri) }

    private val createPngDocument = registerForActivityResult(
        ActivityResultContracts.CreateDocument("image/png"),
    ) { uri -> writePendingImage(uri) }

    private val openJsonDocument = registerForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        runCatching {
            contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: error("empty stream")
        }.mapCatching(::parseBackupJson)
            .onSuccess { importSnapshotHandler?.invoke(it) }
            .onFailure {
                importErrorHandler?.invoke()
                Toast.makeText(this, currentAppLanguage.t("导入失败，请检查 JSON 文件。", "Import failed. Please check the JSON file."), Toast.LENGTH_LONG).show()
            }
    }

    private val requestNotifications = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferHighRefreshRate()
        ensureInteractiveWindow()
        store = NativeStore(this)
        val initial = store.load()
        val initialThemeMode = store.loadThemeMode()
        applyEdgeToEdgeSystemBars(resolveDarkTheme(initialThemeMode))
        ReminderScheduler.createChannel(this)
        ReminderScheduler.scheduleAll(this, initial)
        val initialLanguage = store.loadAppLanguage()
        currentAppLanguage = initialLanguage
        setContent {
            var themeMode by remember { mutableStateOf(initialThemeMode) }
            var appLanguage by remember { mutableStateOf(initialLanguage) }
            var privacyAccepted by remember { mutableStateOf(store.hasAcceptedPrivacy()) }
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                ThemeMode.System -> systemDark
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            DisposableEffect(darkTheme) {
                applyEdgeToEdgeSystemBars(darkTheme)
                onDispose {}
            }
            NativeHrtTheme(darkTheme = darkTheme) {
                Box(Modifier.fillMaxSize()) {
                    NativeHrtApp(
                        initialSnapshot = initial,
                        themeMode = themeMode,
                        onThemeModeChange = { next ->
                            themeMode = next
                            store.saveThemeMode(next)
                        },
                        appLanguage = appLanguage,
                        onLanguageChange = { next ->
                            appLanguage = next
                            currentAppLanguage = next
                            store.saveAppLanguage(next)
                        },
                        showFirstLaunch = !privacyAccepted,
                        onFirstLaunchAccepted = { selectedLanguage ->
                            appLanguage = selectedLanguage
                            store.saveAppLanguage(selectedLanguage)
                            store.savePrivacyAccepted()
                            privacyAccepted = true
                        },
                        saveSnapshot = store::save,
                        registerImportHandlers = { success, failure ->
                            importSnapshotHandler = success
                            importErrorHandler = failure
                        },
                        importJson = { openJsonDocument.launch("*/*") },
                        exportJson = { fileName, contents ->
                            pendingExportContents = contents
                            createJsonDocument.launch(fileName)
                        },
                        exportCsv = { fileName, contents ->
                            pendingExportContents = contents
                            createCsvDocument.launch(fileName)
                        },
                        shareImage = ::shareImage,
                        saveImage = { fileName, bytes ->
                            pendingImageBytes = bytes
                            createPngDocument.launch(fileName)
                        },
                        pickDateTime = ::pickDateTime,
                        requestNotificationPermission = ::requestNotificationPermissionIfNeeded,
                        scheduleReminders = { snapshot -> ReminderScheduler.scheduleAll(this@MainActivity, snapshot) },
                        cancelPlanReminder = { planId -> ReminderScheduler.cancelPlan(this@MainActivity, planId) },
                        addPlanReminder = ::addPlanReminder,
                        openUri = ::openExternalUri,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ensureInteractiveWindow()
    }

    private fun applyEdgeToEdgeSystemBars(darkTheme: Boolean) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT,
            ) { darkTheme },
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT,
            ) { darkTheme },
        )
    }

    private fun resolveDarkTheme(themeMode: ThemeMode): Boolean = when (themeMode) {
        ThemeMode.System -> {
            val mask = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            mask == Configuration.UI_MODE_NIGHT_YES
        }
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    @Suppress("DEPRECATION")
    private fun preferHighRefreshRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val display = windowManager.defaultDisplay
            val currentMode = display.mode
            val bestMode = display.supportedModes
                .filter { mode ->
                    mode.physicalWidth == currentMode.physicalWidth &&
                        mode.physicalHeight == currentMode.physicalHeight
                }
                .maxByOrNull { it.refreshRate }
            if (bestMode != null && bestMode.refreshRate >= currentMode.refreshRate) {
                val attrs = window.attributes
                attrs.preferredDisplayModeId = bestMode.modeId
                window.attributes = attrs
            }
        }
        if (Build.VERSION.SDK_INT >= 35) {
            window.decorView.setRequestedFrameRate(View.REQUESTED_FRAME_RATE_CATEGORY_HIGH)
            window.setFrameRateBoostOnTouchEnabled(true)
            window.setFrameRatePowerSavingsBalanced(false)
        }
    }

    private fun ensureInteractiveWindow() {
        window.clearFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        )
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotifications.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun writePendingExport(uri: Uri?) {
        val contents = pendingExportContents
        pendingExportContents = null
        if (uri == null || contents == null) return
        runCatching {
            contentResolver.openOutputStream(uri)?.use { it.write(contents.toByteArray(Charsets.UTF_8)) }
                ?: error("no output stream")
        }.onFailure {
            Toast.makeText(this, currentAppLanguage.t("导出失败，请重新选择保存位置。", "Export failed. Please choose a save location again."), Toast.LENGTH_LONG).show()
        }
    }

    private fun writePendingImage(uri: Uri?) {
        val bytes = pendingImageBytes
        pendingImageBytes = null
        if (uri == null || bytes == null) return
        runCatching {
            contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("no output stream")
        }.onFailure {
            Toast.makeText(this, currentAppLanguage.t("图片保存失败，请重新选择保存位置。", "Image save failed. Please choose a save location again."), Toast.LENGTH_LONG).show()
        }
    }

    private fun shareImage(fileName: String, bytes: ByteArray, text: String) {
        runCatching {
            val dir = File(cacheDir, "share_images").apply { mkdirs() }
            val file = File(dir, fileName)
            file.writeBytes(bytes)
            val uri = FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
            val baseIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, text)
                clipData = ClipData.newUri(contentResolver, fileName, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val xPackage = listOf("com.x.android", "com.twitter.android").firstOrNull { packageName ->
                val candidate = Intent(baseIntent).setPackage(packageName)
                packageManager.queryIntentActivities(candidate, 0).isNotEmpty()
            }
            if (xPackage != null) {
                startActivity(Intent(baseIntent).setPackage(xPackage))
            } else {
                startActivity(Intent.createChooser(baseIntent, currentAppLanguage.t("分享到 X / Twitter", "Share to X / Twitter")))
            }
        }.onFailure {
            Toast.makeText(this, currentAppLanguage.t("分享图片生成失败，请重试。", "Share image generation failed. Please try again."), Toast.LENGTH_LONG).show()
        }
    }

    private fun pickDateTime(initialTimeH: Double, onPicked: (Double) -> Unit) {
        val initial = LocalDateTime.ofInstant(
            Instant.ofEpochMilli((initialTimeH * 3_600_000.0).toLong()),
            ZoneId.systemDefault(),
        )
        DatePickerDialog(
            this,
            { _, year, month, day ->
                TimePickerDialog(
                    this,
                    { _, hour, minute ->
                        val picked = LocalDateTime.of(year, month + 1, day, hour, minute)
                        onPicked(picked.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli().toDouble() / 3_600_000.0)
                    },
                    initial.hour,
                    initial.minute,
                    true,
                ).show()
            },
            initial.year,
            initial.monthValue - 1,
            initial.dayOfMonth,
        ).show()
    }

    private fun openExternalUri(uri: String) {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
        }.onFailure {
            if (it is ActivityNotFoundException) {
                Toast.makeText(this, currentAppLanguage.t("没有可打开此链接的应用。", "No app can open this link."), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addPlanReminder(plan: MedicationPlan, language: AppLanguage) {
        val now = LocalDateTime.now()
        val targetToday = now
            .withHour((plan.timeMinutes / 60).coerceIn(0, 23))
            .withMinute((plan.timeMinutes % 60).coerceIn(0, 59))
            .withSecond(0)
            .withNano(0)
        val firstTime = if (targetToday.isBefore(now)) targetToday.plusDays(1) else targetToday
        val startMillis = firstTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val description = "${plan.displayName(language)} · ${formatNumber(plan.doseMG, 3)} mg · ${plan.route.label(language)}"
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, language.t("HRT 用药提醒", "HRT dose reminder"))
            putExtra(CalendarContract.Events.DESCRIPTION, description)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startMillis + 15 * 60 * 1000L)
            putExtra(CalendarContract.Events.RRULE, "FREQ=DAILY")
        }
        runCatching { startActivity(intent) }.onFailure {
            Toast.makeText(this, language.t("没有可用的日历/提醒应用。", "No calendar/reminder app is available."), Toast.LENGTH_LONG).show()
        }
    }
}

private data class SimulationBundle(
    val rawE2: SimulationResult? = null,
    val cpa: SimulationResult? = null,
    val testosterone: SimulationResult? = null,
    val calibrationSummary: CalibrationSummary = emptyCalibrationSummary("ekf"),
)

private fun emptyCalibrationSummary(model: String): CalibrationSummary =
    CalibrationSummary(PersonalReplayResult(PersonalModelState(), null), null, model)

private data class BottleDeductionResult(
    val bottles: List<PillBottle>,
    val event: DoseEvent,
    val deductedUnits: Double,
    val bottleName: String,
)

private fun applyBottleDeduction(bottles: List<PillBottle>, event: DoseEvent): BottleDeductionResult {
    if (event.route == Route.PatchRemove || event.doseMG <= 0.0) {
        return BottleDeductionResult(bottles, event, 0.0, "")
    }
    val index = bottles.indexOfFirst { bottle ->
        bottle.medicationKey() == event.medicationKey() &&
            bottle.createdTimeH <= event.timeH &&
            (bottle.expiresTimeH == null || bottle.expiresTimeH >= event.timeH) &&
            bottle.remainingUnits > 0.0
    }
    if (index < 0) return BottleDeductionResult(bottles, event, 0.0, "")
    val bottle = bottles[index]
    val deducted = min(bottle.perDoseUnits, bottle.remainingUnits).coerceAtLeast(0.0)
    if (deducted <= 0.0) return BottleDeductionResult(bottles, event, 0.0, "")
    val nextBottle = bottle.copy(remainingUnits = (bottle.remainingUnits - deducted).coerceAtLeast(0.0))
    val nextBottles = bottles.toMutableList().also { it[index] = nextBottle }
    val nextEvent = event.copy(extras = event.extras + (ExtraKey.BottleDeductedUnits to deducted))
    return BottleDeductionResult(nextBottles, nextEvent, deducted, bottle.name)
}

@Composable
private fun rememberAnalyteEvents(events: List<DoseEvent>, analyte: Analyte): List<DoseEvent> =
    remember(events, analyte) { events.filter { eventAnalyte(it) == analyte } }

@Composable
private fun rememberAnalyteSimulation(
    scopedEvents: List<DoseEvent>,
    weightKg: Double,
    analyte: Analyte,
): SimulationResult? {
    val simulation by produceState<SimulationResult?>(
        initialValue = null,
        scopedEvents,
        weightKg,
        analyte,
    ) {
        val eventsSnapshot = scopedEvents.toList()
        value = if (eventsSnapshot.isEmpty()) {
            null
        } else {
            withContext(Dispatchers.Default) {
                runSimulationForAnalyte(eventsSnapshot, weightKg, analyte)
            }
        }
    }
    return simulation
}

@Composable
private fun rememberCalibrationSummary(
    e2Events: List<DoseEvent>,
    weightKg: Double,
    labResults: List<LabResult>,
    rawE2Simulation: SimulationResult?,
    calibrationModel: String,
): CalibrationSummary {
    val fallback = remember(calibrationModel) {
        emptyCalibrationSummary(calibrationModel)
    }
    val summary by produceState(
        initialValue = fallback,
        e2Events,
        weightKg,
        labResults,
        rawE2Simulation,
        calibrationModel,
    ) {
        val eventsSnapshot = e2Events.toList()
        val labsSnapshot = labResults.toList()
        value = withContext(Dispatchers.Default) {
            buildCalibrationSummary(eventsSnapshot, weightKg, labsSnapshot, rawE2Simulation, calibrationModel)
        }
    }
    return summary
}

@Composable
private fun rememberSimulationBundle(
    events: List<DoseEvent>,
    weightKg: Double,
    labResults: List<LabResult>,
    calibrationModel: String,
    activeAnalyte: Analyte,
): SimulationBundle {
    val e2Events = rememberAnalyteEvents(events, Analyte.E2)
    val cpaEvents = rememberAnalyteEvents(events, Analyte.CPA)
    val rawE2 = rememberAnalyteSimulation(e2Events, weightKg, Analyte.E2)
    val cpa = rememberAnalyteSimulation(cpaEvents, weightKg, Analyte.CPA)
    val testosterone = if (activeAnalyte == Analyte.Testosterone) {
        val testosteroneEvents = rememberAnalyteEvents(events, Analyte.Testosterone)
        rememberAnalyteSimulation(testosteroneEvents, weightKg, Analyte.Testosterone)
    } else {
        null
    }
    val summary = rememberCalibrationSummary(e2Events, weightKg, labResults, rawE2, calibrationModel)
    return remember(rawE2, cpa, testosterone, summary) {
        SimulationBundle(
            rawE2 = rawE2,
            cpa = cpa,
            testosterone = testosterone,
            calibrationSummary = summary,
        )
    }
}

@Composable
private fun NativeHrtApp(
    initialSnapshot: AppStateSnapshot,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    appLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    showFirstLaunch: Boolean,
    onFirstLaunchAccepted: (AppLanguage) -> Unit,
    saveSnapshot: (AppStateSnapshot) -> Unit,
    registerImportHandlers: (((AppStateSnapshot) -> Unit), (() -> Unit)) -> Unit,
    importJson: () -> Unit,
    exportJson: (String, String) -> Unit,
    exportCsv: (String, String) -> Unit,
    shareImage: (String, ByteArray, String) -> Unit,
    saveImage: (String, ByteArray) -> Unit,
    pickDateTime: (Double, (Double) -> Unit) -> Unit,
    requestNotificationPermission: () -> Unit,
    scheduleReminders: (AppStateSnapshot) -> Unit,
    cancelPlanReminder: (String) -> Unit,
    addPlanReminder: (MedicationPlan, AppLanguage) -> Unit,
    openUri: (String) -> Unit,
) {
    var screen by remember { mutableStateOf(Screen.Overview) }
    var events by remember { mutableStateOf(initialSnapshot.events) }
    var labResults by remember { mutableStateOf(initialSnapshot.labResults) }
    var weightKg by remember { mutableDoubleStateOf(initialSnapshot.weightKg) }
    var calibrationModel by remember { mutableStateOf(initialSnapshot.calibrationModel) }
    var doseTemplatesRaw by remember { mutableStateOf(initialSnapshot.doseTemplatesRaw) }
    var medicationPlans by remember { mutableStateOf(initialSnapshot.medicationPlans) }
    var planDoseRecords by remember { mutableStateOf(initialSnapshot.planDoseRecords) }
    var pillBottles by remember { mutableStateOf(initialSnapshot.pillBottles) }
    var activeAnalyte by remember { mutableStateOf(Analyte.E2) }
    var selectedTimeH by remember { mutableDoubleStateOf(nowEpochHours()) }
    var selectedPlanDate by remember { mutableStateOf(localFromEpochHours(selectedTimeH).toLocalDate()) }
    var selectedRecordDate by remember { mutableStateOf(localFromEpochHours(selectedTimeH).toLocalDate()) }
    var showDoseDialog by remember { mutableStateOf<DoseEvent?>(null) }
    var showLabDialog by remember { mutableStateOf<LabResult?>(null) }
    var showPlanDialog by remember { mutableStateOf<MedicationPlan?>(null) }
    var showReminderPicker by remember { mutableStateOf(false) }
    var showBottleDialog by remember { mutableStateOf<PillBottle?>(null) }
    var showWeightDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<DoseEvent?>(null) }
    var message by remember { mutableStateOf("") }
    var warmupStage by remember { mutableStateOf(0) }

    val snapshot by remember(events, labResults, weightKg, calibrationModel, doseTemplatesRaw, medicationPlans, planDoseRecords, pillBottles) {
        derivedStateOf {
            AppStateSnapshot(
                events = events,
                labResults = labResults,
                weightKg = weightKg,
                calibrationModel = calibrationModel,
                doseTemplatesRaw = doseTemplatesRaw,
                medicationPlans = medicationPlans,
                planDoseRecords = planDoseRecords,
                pillBottles = pillBottles,
            )
        }
    }

    LaunchedEffect(snapshot) {
        saveSnapshot(snapshot)
    }

    LaunchedEffect(medicationPlans, planDoseRecords) {
        scheduleReminders(snapshot)
    }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        delay(220)
        warmupStage = 1
    }

    DisposableEffect(appLanguage) {
        registerImportHandlers(
            { imported ->
                events = imported.events
                labResults = imported.labResults
                weightKg = imported.weightKg
                calibrationModel = imported.calibrationModel
                doseTemplatesRaw = imported.doseTemplatesRaw
                medicationPlans = imported.medicationPlans
                planDoseRecords = imported.planDoseRecords
                pillBottles = imported.pillBottles
                selectedTimeH = nowEpochHours()
                selectedPlanDate = localFromEpochHours(selectedTimeH).toLocalDate()
                selectedRecordDate = selectedPlanDate
                message = appLanguage.t(
                    "导入完成：${imported.events.size} 条记录，体重 ${formatNumber(imported.weightKg, 1)} kg。估算值使用本 App 原生算法。",
                    "Import complete: ${imported.events.size} records, weight ${formatNumber(imported.weightKg, 1)} kg. Estimates use this app's native algorithm.",
                )
            },
            {
                message = appLanguage.t(
                    "导入失败：JSON 结构或编码无法识别。",
                    "Import failed: JSON structure or encoding was not recognized.",
                )
            },
        )
        onDispose {
            registerImportHandlers({}, {})
        }
    }

    val simulationBundle = rememberSimulationBundle(events, weightKg, labResults, calibrationModel, activeAnalyte)
    val rawE2Simulation = simulationBundle.rawE2
    val cpaSimulation = simulationBundle.cpa
    val calibrationSummary = simulationBundle.calibrationSummary
    val simulation by remember(activeAnalyte, rawE2Simulation, cpaSimulation, simulationBundle.testosterone, calibrationSummary) {
        derivedStateOf {
            when (activeAnalyte) {
                Analyte.E2 ->
                    calibrationSummary.calibrated?.takeIf { calibrationSummary.hasPostDoseCalibration }?.let(::calibratedSimulationResult)
                        ?: rawE2Simulation
                Analyte.CPA -> cpaSimulation
                Analyte.Testosterone -> simulationBundle.testosterone
            }
        }
    }
    val currentValue by remember(simulation, selectedTimeH) {
        derivedStateOf { interpolateConcentration(simulation, selectedTimeH) }
    }
    val cpaCurrentValue by remember(cpaSimulation, selectedTimeH) {
        derivedStateOf { interpolateConcentration(cpaSimulation, selectedTimeH) }
    }
    val antiAndrogenLabel = remember(events, appLanguage) { antiAndrogenDisplayLabel(events, appLanguage) }
    val antiAndrogenMedication = remember(events) { antiAndrogenReferenceMedication(events) }
    val statusNowH = nowEpochHours()
    val medicationStatusSummaries = remember(events, medicationPlans, appLanguage) {
        buildMedicationStatusSummaries(events, medicationPlans, appLanguage, nowEpochHours())
    }
    val visibleMedicationStatusSummaries = remember(medicationStatusSummaries) {
        medicationStatusSummaries.filter { summary ->
            summary.analyte == Analyte.E2 ||
                summary.sourcePlan != null ||
                summary.lastTimeH != null ||
                summary.nextTimeH != null
        }
    }
    val palette = LocalAppPalette.current

    Box(Modifier.fillMaxSize()) {
        AmbientPastelBackdrop()
        StartupWarmupLayer(stage = warmupStage)
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                GlassBottomBar(current = screen, language = appLanguage, onScreenChange = { screen = it })
            },
            floatingActionButton = {
                val fabInteraction = remember { MutableInteractionSource() }
                val fabScale = colorOsPressScale(fabInteraction, pressedScale = 0.92f, label = "fab-press")
                AnimatedVisibility(
                    visible = screen == Screen.Overview || screen == Screen.Plan || screen == Screen.Records || screen == Screen.Labs || screen == Screen.Bottles,
                    enter = fadeIn(tween(palette.fadeMotionDuration(118), easing = AppSmoothEasing)) +
                        scaleIn(
                            tween(palette.motionDuration(142), easing = AppSmoothEasing),
                            initialScale = if (palette.reducedMotion) 0.99f else 0.97f,
                        ),
                    exit = fadeOut(tween(palette.fadeMotionDuration(92), easing = FastOutSlowInEasing)) +
                        scaleOut(
                            tween(palette.motionDuration(96), easing = FastOutSlowInEasing),
                            targetScale = if (palette.reducedMotion) 0.99f else 0.97f,
                        ),
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .graphicsLayer {
                                scaleX = fabScale
                                scaleY = fabScale
                            }
                            .background(Brush.linearGradient(listOf(TBlue.copy(alpha = 0.96f), E2Pink.copy(alpha = 0.98f))), CircleShape)
                            .clickable(
                                interactionSource = fabInteraction,
                                indication = null,
                                onClick = {
                                    when (screen) {
                                        Screen.Plan -> showPlanDialog = MedicationPlan()
                                        Screen.Labs -> showLabDialog = LabResult(timeH = selectedTimeH)
                                        Screen.Bottles -> showBottleDialog = newBottle(selectedTimeH)
                                        else -> showDoseDialog = DoseEvent(timeH = selectedTimeH)
                                    }
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("+", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Light)
                    }
                }
            },
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                ) {
                    AnimatedContent(
                        targetState = screen,
                        transitionSpec = {
                            val enter = fadeIn(tween(palette.fadeMotionDuration(132), easing = ColorOsEasing)) +
                                scaleIn(
                                    tween(palette.motionDuration(176), easing = ColorOsEasing),
                                    initialScale = 0.985f,
                                )
                            val exit = fadeOut(tween(palette.fadeMotionDuration(92), easing = FastOutSlowInEasing)) +
                                scaleOut(
                                    tween(palette.motionDuration(112), easing = FastOutSlowInEasing),
                                    targetScale = 0.992f,
                                )
                            enter togetherWith exit
                        },
                        label = "screen-motion",
                    ) { targetScreen ->
                    when (targetScreen) {
                        Screen.Overview -> Box(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                            OverviewScreen(
                            activeAnalyte = activeAnalyte,
                            onAnalyteChange = { activeAnalyte = it },
                            simulation = simulation,
                            currentValue = currentValue,
                            cpaCurrentValue = cpaCurrentValue,
                            hasCpaData = cpaSimulation != null,
                            antiAndrogenLabel = antiAndrogenLabel,
                            antiAndrogenMedication = antiAndrogenMedication,
                            medicationStatusSummaries = visibleMedicationStatusSummaries,
                            statusNowH = statusNowH,
                            weightKg = weightKg,
                            selectedTimeH = selectedTimeH,
                            message = message,
                            language = appLanguage,
                            onOpenPlan = { screen = Screen.Plan },
                            onWeightClick = { showWeightDialog = true },
                            onTimeClick = {
                                pickDateTime(selectedTimeH) { picked ->
                                    selectedTimeH = picked
                                    selectedPlanDate = localFromEpochHours(picked).toLocalDate()
                                    selectedRecordDate = selectedPlanDate
                                    message = ""
                                }
                            },
                            onCpaPreviewClick = { activeAnalyte = Analyte.CPA },
                            onTimeSelected = {
                                selectedTimeH = it
                                selectedPlanDate = localFromEpochHours(it).toLocalDate()
                                selectedRecordDate = selectedPlanDate
                            },
                            onResetTime = {
                                selectedTimeH = nowEpochHours()
                                selectedPlanDate = localFromEpochHours(selectedTimeH).toLocalDate()
                                selectedRecordDate = selectedPlanDate
                                message = appLanguage.t("已重置为当前真实时间。", "Reset to the current real time.")
                            },
                            onPickTime = {
                                pickDateTime(selectedTimeH) { picked ->
                                    selectedTimeH = picked
                                    selectedPlanDate = localFromEpochHours(picked).toLocalDate()
                                    selectedRecordDate = selectedPlanDate
                                    message = ""
                                }
                            },
                            onShare = {
                                val bytes = buildSharePng(
                                    activeAnalyte,
                                    currentValue,
                                    selectedTimeH,
                                    appLanguage,
                                    antiAndrogenLabel,
                                )
                                shareImage(
                                    shareFileName(selectedTimeH),
                                    bytes,
                                    shareText(activeAnalyte, currentValue, appLanguage, antiAndrogenLabel),
                                )
                            },
                            onSaveImage = {
                                val bytes = buildSharePng(
                                    activeAnalyte,
                                    currentValue,
                                    selectedTimeH,
                                    appLanguage,
                                    antiAndrogenLabel,
                                )
                                saveImage(shareFileName(selectedTimeH), bytes)
                                message = appLanguage.t("已打开系统图片保存器。", "Opened the system image saver.")
                            },
                            )
                        }
                        Screen.Plan -> FeaturePageSurface {
                            PlanDashboardScreen(
                            plans = medicationPlans,
                            records = planDoseRecords,
                            language = appLanguage,
                            nowH = statusNowH,
                            selectedDate = selectedPlanDate,
                            onSelectDate = { selectedPlanDate = it },
                            onPickDate = {
                                val local = localFromEpochHours(selectedTimeH)
                                pickDateTime(scheduledTimeForDate(selectedPlanDate, local.hour * 60 + local.minute)) { picked ->
                                    selectedTimeH = picked
                                    selectedPlanDate = localFromEpochHours(picked).toLocalDate()
                                    selectedRecordDate = selectedPlanDate
                                }
                            },
                            onCreatePlan = {
                                showPlanDialog = MedicationPlan()
                            },
                            onCreateReminder = {
                                if (medicationPlans.isEmpty()) {
                                    showPlanDialog = MedicationPlan()
                                    message = appLanguage.t("先创建一个计划，再把它加入系统提醒。", "Create a plan first, then add it as a system reminder.")
                                } else {
                                    showReminderPicker = true
                                }
                            },
                            onAddDose = { plan ->
                                val plannedTime = scheduledTimeForDate(
                                    selectedPlanDate,
                                    plan.normalizedTimes().firstOrNull() ?: plan.timeMinutes,
                                )
                                showDoseDialog = DoseEvent(
                                    category = plan.category,
                                    route = plan.route,
                                    doseMG = plan.doseMG,
                                    compound = plan.compound,
                                    recordOnlyMedication = plan.recordOnlyMedication,
                                    timeH = plannedTime,
                                )
                            },
                            onAddReminder = { addPlanReminder(it, appLanguage) },
                            onMarkTaken = { plan, scheduled ->
                                val acted = nowEpochHours()
                                val result = markPlanTaken(snapshot, plan.id, scheduled, actedTimeH = acted)
                                events = result.snapshot.events
                                planDoseRecords = result.snapshot.planDoseRecords
                                pillBottles = result.snapshot.pillBottles
                                selectedTimeH = acted
                                selectedPlanDate = localFromEpochHours(scheduled).toLocalDate()
                                selectedRecordDate = selectedPlanDate
                                eventAnalyte(result.generatedDose ?: createDoseFromPlan(plan))?.let { activeAnalyte = it }
                                message = appLanguage.t("已记录为已服用，曲线已刷新。", "Marked taken and refreshed the curve.")
                            },
                            onSkip = { plan, scheduled ->
                                val result = markPlanSkipped(snapshot, plan.id, scheduled)
                                planDoseRecords = result.snapshot.planDoseRecords
                                message = appLanguage.t("已跳过本次计划，不会写入用药记录。", "Skipped this occurrence; no dose was logged.")
                            },
                            onToggle = { plan ->
                                medicationPlans = medicationPlans.map { if (it.id == plan.id) it.copy(enabled = !it.enabled) else it }
                            },
                            onDelete = { plan ->
                                medicationPlans = medicationPlans.filterNot { it.id == plan.id }
                                planDoseRecords = planDoseRecords.filterNot { it.planId == plan.id }
                                cancelPlanReminder(plan.id)
                                message = appLanguage.t("计划已删除。", "Plan deleted.")
                            },
                            )
                        }
                        Screen.Records -> FeaturePageSurface {
                            RecordsScreenV2(
                            events = events,
                            language = appLanguage,
                            selectedDate = selectedRecordDate,
                            onDateSelected = { date ->
                                selectedRecordDate = date
                                val local = localFromEpochHours(selectedTimeH)
                                selectedTimeH = scheduledTimeForDate(date, local.hour * 60 + local.minute)
                            },
                            onPickDate = {
                                val local = localFromEpochHours(selectedTimeH)
                                pickDateTime(scheduledTimeForDate(selectedRecordDate, local.hour * 60 + local.minute)) { picked ->
                                    selectedTimeH = picked
                                    selectedRecordDate = localFromEpochHours(picked).toLocalDate()
                                    selectedPlanDate = selectedRecordDate
                                }
                            },
                            onAdd = {
                                val local = localFromEpochHours(selectedTimeH)
                                showDoseDialog = DoseEvent(timeH = scheduledTimeForDate(selectedRecordDate, local.hour * 60 + local.minute))
                            },
                            onEdit = { showDoseDialog = it },
                            onDelete = { pendingDelete = it },
                            )
                        }
                        Screen.Labs -> FeaturePageSurface {
                            LabsScreen(
                            labs = labResults,
                            calibrationSummary = calibrationSummary,
                            calibrationModel = calibrationModel,
                            onCalibrationModelChange = { calibrationModel = it },
                            onAdd = { showLabDialog = LabResult(timeH = selectedTimeH) },
                            language = appLanguage,
                            onDelete = { lab ->
                                labResults = labResults.filterNot { it.id == lab.id }
                                message = appLanguage.t("已删除化验记录。", "Lab result deleted.")
                            },
                            )
                        }
                        Screen.Cup -> FeaturePageSurface {
                            CupCalculatorScreen(
                                language = appLanguage,
                                onShare = { result ->
                                    val shareTimeH = nowEpochHours()
                                    shareImage(
                                        shareFileName(shareTimeH),
                                        buildCupSharePng(result, shareTimeH, appLanguage),
                                        cupShareText(result, appLanguage),
                                    )
                                },
                            )
                        }
                        Screen.Bottles -> FeaturePageSurface {
                            BottlesScreenV2(
                            bottles = pillBottles,
                            language = appLanguage,
                            onAdd = { showBottleDialog = newBottle(selectedTimeH) },
                            onEdit = { showBottleDialog = it },
                            onRefill = { bottle ->
                                pillBottles = pillBottles.map { if (it.id == bottle.id) it.copy(remainingUnits = it.totalUnits) else it }
                                message = appLanguage.t("药瓶已补满。", "Bottle refilled.")
                            },
                            onDelete = { bottle ->
                                pillBottles = pillBottles.filterNot { it.id == bottle.id }
                                message = appLanguage.t("药瓶已删除。", "Bottle deleted.")
                            },
                            )
                        }
                        Screen.Settings -> FeaturePageSurface {
                            SettingsScreen(
                            weightKg = weightKg,
                            onWeightChange = { weightKg = it },
                            themeMode = themeMode,
                            onThemeModeChange = onThemeModeChange,
                            appLanguage = appLanguage,
                            onLanguageChange = onLanguageChange,
                            snapshot = snapshot,
                            onImport = importJson,
                            onExportJson = {
                                val fileName = "hrt-recorder-native-${System.currentTimeMillis()}.json"
                                exportJson(fileName, buildBackupJson(snapshot))
                                message = appLanguage.t("已打开系统 JSON 保存器。", "Opened the system JSON saver.")
                            },
                            onExportCsv = {
                                val fileName = "hrt-recorder-native-${System.currentTimeMillis()}.csv"
                                exportCsv(fileName, buildCsv(snapshot))
                                message = appLanguage.t("已打开系统 CSV 保存器。", "Opened the system CSV saver.")
                            },
                            onOpenUri = openUri,
                            )
                        }
                    }
                    }
                }
            }
        }
    }

    showPlanDialog?.let { editing ->
        PlanEditorDialog(
            initial = editing,
            language = appLanguage,
            onDismiss = { showPlanDialog = null },
            onSave = { saved ->
                medicationPlans = if (medicationPlans.any { it.id == saved.id }) {
                    medicationPlans.map { if (it.id == saved.id) saved else it }
                } else {
                    medicationPlans + saved
                }.sortedBy { it.timeMinutes }
                if (saved.reminderEnabled) {
                    requestNotificationPermission()
                }
                if (saved.systemSyncEnabled) {
                    addPlanReminder(saved, appLanguage)
                }
                showPlanDialog = null
                message = appLanguage.t("计划已保存，可一键记录或添加系统提醒。", "Plan saved. You can quick-log it or add a system reminder.")
            },
        )
    }

    if (showReminderPicker) {
        PlanReminderPickerDialog(
            plans = medicationPlans,
            language = appLanguage,
            onDismiss = { showReminderPicker = false },
            onPick = { plan ->
                addPlanReminder(plan, appLanguage)
                showReminderPicker = false
                message = appLanguage.t("已打开系统提醒/日历。", "Opened system reminders/calendar.")
            },
        )
    }

    showDoseDialog?.let { editing ->
        val isNewDose = events.none { it.id == editing.id }
        DoseEditorDialogV2(
            initial = editing,
            isNew = isNewDose,
            pickDateTime = pickDateTime,
            language = appLanguage,
            onDismiss = { showDoseDialog = null },
            onSave = { saved ->
                val deduction = if (isNewDose) applyBottleDeduction(pillBottles, saved) else BottleDeductionResult(pillBottles, saved, 0.0, "")
                if (isNewDose && deduction.deductedUnits > 0.0) {
                    pillBottles = deduction.bottles
                }
                val finalSaved = deduction.event
                events = if (events.any { it.id == saved.id }) {
                    events.map { if (it.id == saved.id) finalSaved else it }
                } else {
                    events + finalSaved
                }.sortedByDescending { it.timeH }
                selectedTimeH = finalSaved.timeH
                selectedPlanDate = localFromEpochHours(finalSaved.timeH).toLocalDate()
                selectedRecordDate = selectedPlanDate
                eventAnalyte(finalSaved)?.let { activeAnalyte = it }
                val bottleHint = if (deduction.deductedUnits > 0.0) {
                    appLanguage.t("，已从药瓶扣除 ${formatNumber(deduction.deductedUnits, 2)}", ", deducted ${formatNumber(deduction.deductedUnits, 2)} from bottle")
                } else {
                    ""
                }
                message = appLanguage.t(
                    "已保存 ${saved.category.label(appLanguage)} 记录，曲线已切换并刷新。",
                    "Saved ${saved.category.label(appLanguage)} record. The chart has switched and refreshed.",
                ) + bottleHint
                showDoseDialog = null
            },
        )
    }

    showBottleDialog?.let { editing ->
        BottleEditorDialogV2(
            initial = editing,
            language = appLanguage,
            pickDateTime = pickDateTime,
            onDismiss = { showBottleDialog = null },
            onSave = { saved ->
                pillBottles = if (pillBottles.any { it.id == saved.id }) {
                    pillBottles.map { if (it.id == saved.id) saved else it }
                } else {
                    pillBottles + saved
                }.sortedByDescending { it.createdTimeH }
                showBottleDialog = null
                message = appLanguage.t("药瓶已保存。", "Bottle saved.")
            },
        )
    }

    showLabDialog?.let { editing ->
        LabEditorDialog(
            initial = editing,
            pickDateTime = pickDateTime,
            language = appLanguage,
            onDismiss = { showLabDialog = null },
            onSave = { saved ->
                labResults = if (labResults.any { it.id == saved.id }) {
                    labResults.map { if (it.id == saved.id) saved else it }
                } else {
                    labResults + saved
                }.sortedByDescending { it.timeH }
                message = appLanguage.t(
                    "已保存化验记录，EKF / OU-Kalman 诊断已刷新。",
                    "Lab result saved. EKF / OU-Kalman diagnostics refreshed.",
                )
                showLabDialog = null
            },
        )
    }

    if (showWeightDialog) {
        QuickWeightDialog(
            weightKg = weightKg,
            language = appLanguage,
            onDismiss = { showWeightDialog = false },
            onSave = { next ->
                weightKg = next
                showWeightDialog = false
                message = appLanguage.t(
                    "体重已更新为 ${formatNumber(next, 1)} kg。",
                    "Weight updated to ${formatNumber(next, 1)} kg.",
                )
            },
        )
    }

    pendingDelete?.let { event ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(appLanguage.t("删除记录", "Delete Record")) },
            text = {
                Text(
                    appLanguage.t(
                        "确定删除 ${medicationDisplayName(event.category, event.compound, event.recordOnlyMedication, appLanguage)} ${formatWallTime(event.timeH)} 这条记录吗？",
                        "Delete this ${medicationDisplayName(event.category, event.compound, event.recordOnlyMedication, appLanguage)} record from ${formatWallTime(event.timeH)}?",
                    ),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        events = events.filterNot { it.id == event.id }
                        pendingDelete = null
                        message = appLanguage.t("已删除记录，曲线已刷新。", "Record deleted. Chart refreshed.")
                    },
                ) { Text(appLanguage.t("确定", "Delete")) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(appLanguage.t("取消", "Cancel")) }
            },
        )
    }

    if (showFirstLaunch) {
        FirstLaunchPrivacyDialog(
            initialLanguage = appLanguage,
            initialWeightKg = weightKg,
            onAccepted = { selectedLanguage, selectedWeight ->
                weightKg = selectedWeight
                onLanguageChange(selectedLanguage)
                onFirstLaunchAccepted(selectedLanguage)
            },
        )
    }
}

@Composable
private fun QuickWeightDialog(
    weightKg: Double,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
) {
    var text by remember(weightKg) { mutableStateOf(formatNumber(weightKg, 1)) }
    val parsed = text.toDoubleOrNull()?.takeIf { it in 20.0..250.0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(language.t("修改体重", "Edit Weight"), color = Ink, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    language.t(
                        "体重会立即用于本地药代估算，可随时修改。",
                        "Weight is used immediately by local PK estimates and can be changed anytime.",
                    ),
                    color = Muted,
                    lineHeight = 20.sp,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(language.t("体重", "Weight")) },
                    suffix = { Text("kg") },
                    singleLine = true,
                    isError = parsed == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { parsed?.let(onSave) },
                enabled = parsed != null,
                shape = RoundedCornerShape(16.dp),
            ) { Text(language.t("保存", "Save")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(language.t("取消", "Cancel")) }
        },
    )
}

private data class IntroPage(
    val title: String,
    val body: String,
    val accent: Color,
)

private fun introPages(language: AppLanguage): List<IntroPage> =
    if (language == AppLanguage.English) {
        listOf(
            IntroPage(
                title = "Set Up HRT Recorder",
                body = "Choose a language and enter your weight first. Weight is used by local concentration estimates and can be changed later in Settings.",
                accent = TBlue,
            ),
            IntroPage(
                title = "Offline by Default",
                body = "HRT Recorder keeps medication, labs, weight and trend estimates on this device. It does not request network permission, login, cloud sync, ads or analytics.",
                accent = E2Pink,
            ),
            IntroPage(
                title = "For Trend Reference",
                body = "Curves, cycle references, cup calculator, lab calibration and share images are not medical diagnosis, prescriptions or dosage advice. Please consult a clinician or pharmacist for medical decisions.",
                accent = CpaRose,
            ),
            IntroPage(
                title = "Your Data, Your Control",
                body = "Data is stored locally. After exporting JSON/CSV, saving images or sharing to other apps, further management and risk are controlled by you.",
                accent = Color(0xFFB9A7F0),
            ),
        )
    } else {
        listOf(
            IntroPage(
                title = "设置 HRT Recorder",
                body = "先选择语言并输入体重。体重会参与本地浓度估算，之后也可以在设置里随时修改。",
                accent = TBlue,
            ),
            IntroPage(
                title = "本地离线记录",
                body = "HRT Recorder 用于记录给药、化验、体重和本地趋势估算。默认不申请网络权限，不接入登录、云同步、广告或统计。",
                accent = E2Pink,
            ),
            IntroPage(
                title = "仅供趋势参考",
                body = "所有曲线、周期参考、罩杯计算器、化验校准和分享图都不是医疗诊断、处方建议或剂量建议。涉及用药和身体不适，请咨询医生或药师。",
                accent = CpaRose,
            ),
            IntroPage(
                title = "数据由你掌控",
                body = "数据默认保存在本机。导出 JSON/CSV、保存图片、分享到 X 或发送给其它应用后，后续管理与风险由你自行决定。",
                accent = Color(0xFFB9A7F0),
            ),
        )
    }

private fun parseOnboardingWeight(text: String): Double? =
    text.trim().replace(",", ".").toDoubleOrNull()?.takeIf { it in 20.0..250.0 }

@Composable
private fun FirstLaunchPrivacyDialog(
    initialLanguage: AppLanguage,
    initialWeightKg: Double,
    onAccepted: (AppLanguage, Double) -> Unit,
) {
    var language by remember { mutableStateOf(initialLanguage) }
    var pageIndex by remember { mutableStateOf(0) }
    var weightText by remember { mutableStateOf(formatNumber(initialWeightKg, 1)) }
    var showPrivacyTerms by remember { mutableStateOf(false) }
    var hasAgreedTerms by remember { mutableStateOf(false) }
    val pages = remember(language) { introPages(language) }
    val page = pages[pageIndex.coerceAtMost(pages.lastIndex)]
    val parsedWeight = parseOnboardingWeight(weightText)
    val isEnglish = language == AppLanguage.English
    val isLastPage = pageIndex == pages.lastIndex
    val canContinue = (pageIndex != 0 || parsedWeight != null) && (!isLastPage || hasAgreedTerms)
    if (showPrivacyTerms) {
        PrivacyTermsDialog(
            language = language,
            onDismiss = { showPrivacyTerms = false },
        )
    }
    AlertDialog(
        onDismissRequest = {},
        title = { Text(page.title, color = Ink, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(LocalAppPalette.current.subtleSurface, RoundedCornerShape(8.dp)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(((pageIndex + 1).toFloat() / pages.size.toFloat()).coerceIn(0f, 1f))
                            .height(8.dp)
                            .background(page.accent, RoundedCornerShape(8.dp)),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(page.body, color = Muted, lineHeight = 21.sp)
                if (pageIndex == 0) {
                    Spacer(Modifier.height(16.dp))
                    Text(if (isEnglish) "Language" else "语言", color = Ink, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    ChipFlow(AppLanguage.entries, language, { it.displayLabel(language) }) { selected ->
                        language = selected
                    }
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        label = { Text(if (isEnglish) "Weight" else "体重") },
                        suffix = { Text("kg") },
                        isError = parsedWeight == null,
                        supportingText = {
                            Text(
                                if (parsedWeight == null) {
                                    if (isEnglish) "Enter 20-250 kg." else "请输入 20-250 kg。"
                                } else {
                                    if (isEnglish) "Saved locally; changeable later." else "仅保存到本机，之后可在设置里修改。"
                                },
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    if (isEnglish) "Page ${pageIndex + 1} / ${pages.size}" else "第 ${pageIndex + 1} / ${pages.size} 页",
                    color = Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(onClick = { showPrivacyTerms = true }) {
                    Text(if (isEnglish) "View full Terms and Privacy Agreement" else "查看完整用户与隐私协议")
                }
                if (isLastPage) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { hasAgreedTerms = !hasAgreedTerms }
                            .padding(vertical = 6.dp),
                    ) {
                        Checkbox(checked = hasAgreedTerms, onCheckedChange = { hasAgreedTerms = it })
                        Text(
                            if (isEnglish) {
                                "I have read and agree to the full Terms and Privacy Agreement. I understand this app is offline trend reference only, not medical advice."
                            } else {
                                "我已阅读并同意完整用户与隐私协议，理解本软件仅为离线趋势参考，不构成医疗建议。"
                            },
                            color = Ink,
                            lineHeight = 19.sp,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Text(
                        if (isEnglish) {
                            "If you do not agree, you cannot enter the app."
                        } else {
                            "若不同意，本软件将不会进入主界面。"
                        },
                        color = Muted,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!canContinue) return@Button
                    if (pageIndex < pages.lastIndex) {
                        pageIndex += 1
                    } else {
                        onAccepted(language, parsedWeight ?: initialWeightKg)
                    }
                },
                enabled = canContinue,
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    if (pageIndex < pages.lastIndex) {
                        if (isEnglish) "Next" else "下一页"
                    } else {
                        if (isEnglish) "Agree and Start" else "同意并开始使用"
                    },
                )
            }
        },
        dismissButton = {
            if (pageIndex > 0) {
                TextButton(onClick = { pageIndex -= 1 }) {
                    Text(if (isEnglish) "Back" else "上一页")
                }
            }
        },
    )
}

@Composable
private fun StartupWarmupLayer(stage: Int) {
    if (stage <= 0) return
    Box(
        modifier = Modifier
            .size(4.dp)
            .graphicsLayer {
                alpha = 0.01f
                clip = true
            },
    ) {
        WarmupShaderCanvas()
    }
}

@Composable
private fun WarmupShaderCanvas() {
    val palette = LocalAppPalette.current
    Canvas(Modifier.size(4.dp)) {
        drawRect(color = TBlue.copy(alpha = 0.22f), size = size)
        val path = Path().apply {
            moveTo(0f, size.height * 0.82f)
            cubicTo(size.width * 0.20f, 0f, size.width * 0.55f, size.height, size.width, size.height * 0.28f)
        }
        drawPath(path, E2Pink.copy(alpha = 0.9f), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(palette.card, radius = 1.4.dp.toPx(), center = Offset(size.width * 0.68f, size.height * 0.45f))
        drawRoundRect(
            color = palette.subtleSurface,
            topLeft = Offset(0.4.dp.toPx(), 0.4.dp.toPx()),
            size = Size(3.2.dp.toPx(), 2.dp.toPx()),
            cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx()),
        )
    }
}

@Composable
private fun WarmupMaterialControls(language: AppLanguage) {
    Column {
        Button(onClick = {}, shape = RoundedCornerShape(18.dp)) {
            Text(language.t("预热", "Warmup"))
        }
        OutlinedButton(onClick = {}, shape = RoundedCornerShape(18.dp)) {
            Text(language.t("曲线", "Curve"))
        }
        OutlinedTextField(
            value = "70",
            onValueChange = {},
            label = { Text(language.t("体重", "Weight")) },
            suffix = { Text("kg") },
            singleLine = true,
        )
    }
}

@Composable
private fun WarmupNavigationAndChips(language: AppLanguage) {
    Column {
        GlassBottomBar(current = Screen.Overview, language = language, onScreenChange = {})
        ChipFlow(Analyte.entries, Analyte.E2, { it.label }) {}
        ChipFlow(ThemeMode.entries, ThemeMode.System, { it.label(language) }) {}
    }
}

@Composable
private fun WarmupTypography(language: AppLanguage) {
    Column {
        GradientNumber("512", Analyte.E2)
        Text(language.t("当前估算浓度", "Current Estimate"), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Text(language.t("仅供趋势参考，无医疗价值。", "Trend reference only, not medical advice."), fontSize = 12.sp)
        MetricPill("E2", "512 pg/mL", E2Pink)
    }
}

@Composable
private fun AmbientPastelBackdrop() {
    val palette = LocalAppPalette.current
    val backgroundBrush = AppGradient
    Canvas(Modifier.fillMaxSize()) {
        val alphaScale = if (palette.reducedMotion) 0.72f else 1f
        drawRect(brush = backgroundBrush, size = size)
        drawCircle(
            color = MistBlue.copy(alpha = 0.82f * alphaScale),
            radius = size.minDimension * 0.58f,
            center = Offset(size.width * 0.02f, size.height * 0.05f),
        )
        drawCircle(
            color = MistPink.copy(alpha = 0.68f * alphaScale),
            radius = size.minDimension * 0.50f,
            center = Offset(size.width * 0.95f, size.height * 0.20f),
        )
        drawCircle(
            color = MistLavender.copy(alpha = 0.42f * alphaScale),
            radius = size.minDimension * 0.62f,
            center = Offset(size.width * 0.50f, size.height * 0.82f),
        )
        drawCircle(
            color = palette.card.copy(alpha = 0.34f * alphaScale),
            radius = size.minDimension * 0.38f,
            center = Offset(size.width * 0.74f, size.height * 0.52f),
        )
    }
}

@Composable
private fun GlassBottomBar(current: Screen, language: AppLanguage, onScreenChange: (Screen) -> Unit) {
    val palette = LocalAppPalette.current
    val shape = RoundedCornerShape(18.dp)
    val selectedIndex = Screen.entries.indexOf(current).coerceAtLeast(0)
    val selectedAccent = screenAccent(current)
    val lightMode = palette.card == Color.White
    val navBrush =
        if (lightMode) {
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.98f),
                    palette.nav,
                    palette.subtleSurface,
                    Color(0xFFFFE6F3).copy(alpha = 0.86f),
                ),
                start = Offset(0f, 0f),
                end = Offset(980f, 260f),
            )
        } else {
            Brush.linearGradient(
                listOf(
                    palette.card,
                    palette.nav,
                    palette.subtleSurface,
                    Color(0xFF241B2C).copy(alpha = 0.88f),
                ),
                start = Offset(0f, 0f),
                end = Offset(980f, 260f),
            )
        }
    Box(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(navBrush, shape)
            .padding(horizontal = 6.dp, vertical = 6.dp),
    ) {
        BoxWithConstraints(modifier = Modifier.height(64.dp)) {
            val itemWidth = maxWidth / Screen.entries.size.toFloat()
            val density = LocalDensity.current
            val indicatorOffsetPx by animateFloatAsState(
                targetValue = with(density) { (itemWidth * selectedIndex.toFloat()).toPx() },
                animationSpec = tween(palette.motionDuration(COLOROS_SLIDE_MS), easing = ColorOsEasing),
                label = "nav-indicator-offset",
            )
            val indicatorColor = appColorAsState(if (lightMode) Color.White.copy(alpha = 0.94f) else selectedAccent.copy(alpha = 0.18f), "nav-indicator-color")
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(itemWidth)
                    .height(54.dp)
                    .graphicsLayer { translationX = indicatorOffsetPx }
                    .padding(horizontal = 3.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(indicatorColor, RoundedCornerShape(18.dp)),
            )
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Screen.entries.forEach { item ->
                    val selected = current == item
                    val accent = screenAccent(item)
                    val interaction = remember(item) { MutableInteractionSource() }
                    val scale = colorOsPressScale(interaction, pressedScale = 0.94f, label = "nav-press-${item.name}")
                    val fg = appColorAsState(if (selected) accent else Muted, "nav-fg-${item.name}")
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(
                                interactionSource = interaction,
                                indication = null,
                                onClick = { onScreenChange(item) },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(item.icon, fontSize = if (selected) 20.sp else 18.sp, color = fg, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp)
                            Text(
                                item.label(language),
                                color = fg,
                                fontSize = if (language == AppLanguage.English) 9.sp else 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                lineHeight = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun screenAccent(screen: Screen): Color =
    when (screen) {
        Screen.Overview -> Color(0xFF54B7E8)
        Screen.Plan -> Color(0xFF6BCDF4)
        Screen.Records -> E2Pink
        Screen.Labs -> Color(0xFFB9A7F0)
        Screen.Cup -> Color(0xFFE8A9C8)
        Screen.Bottles -> Color(0xFF7AD7C7)
        Screen.Settings -> Color(0xFF6F7E8E)
    }

@Composable
private fun HeaderCard(
    activeAnalyte: Analyte,
    value: Double,
    unit: String,
    cpaValue: Double?,
    antiAndrogenLabel: String,
    antiAndrogenMedication: RecordOnlyMedication?,
    weightKg: Double,
    selectedTimeH: Double,
    message: String,
    language: AppLanguage,
    onWeightClick: () -> Unit,
    onTimeClick: () -> Unit,
    onCpaPreviewClick: () -> Unit,
) {
    val palette = LocalAppPalette.current
    val wallClock = remember(selectedTimeH) { formatWallClock(selectedTimeH) }
    val wallTimeShort = remember(selectedTimeH) { formatWallTime(selectedTimeH).substring(5) }
    val valueText = remember(value) { formatNumber(value, 1) }
    val weightText = remember(weightKg) { formatNumber(weightKg, 1) }
    val cpaText = remember(cpaValue) { cpaValue?.let { formatNumber(it, 1) } }
    CompositionLocalProvider(
        LocalMetricPillActions provides MetricPillActions(onWeightClick, onTimeClick, onCpaPreviewClick),
    ) {
    NativeCard(Modifier.padding(top = 0.dp, bottom = 6.dp)) {
        val badge = headerBadge(activeAnalyte, value, antiAndrogenLabel, antiAndrogenMedication, language)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier
                        .size(46.dp)
                        .background(
                            Brush.linearGradient(listOf(TBlue, E2Pink)),
                            RoundedCornerShape(17.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(activeAnalyte.tabLabel(language), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
                Column {
                    Text("HRT Recorder", fontSize = 18.sp, color = Ink, fontWeight = FontWeight.ExtraBold)
                    Text(language.t("本地实时估算", "Local live estimate"), fontSize = 12.sp, color = Muted, fontWeight = FontWeight.SemiBold)
                }
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(onClick = onTimeClick)
                    .background(palette.subtleSurface, RoundedCornerShape(18.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
            ) {
                Text(wallClock, color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(language.t("当前估算浓度", "Current Estimate"), fontSize = 14.sp, color = Muted, fontWeight = FontWeight.ExtraBold)
        Row(verticalAlignment = Alignment.Bottom) {
            GradientNumber(valueText, activeAnalyte)
            Spacer(Modifier.width(8.dp))
            Text(unit, fontSize = 26.sp, color = Muted, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        }
        if (badge != null) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .background(badge.accent.copy(alpha = 0.13f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("ⓘ  ${badge.label}", color = badge.accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
            Text(badge.detail, color = Muted, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MetricPill(language.t("体重", "Weight"), "${formatNumber(weightKg, 1)} kg", TBlue, Modifier.weight(1f))
            MetricPill(language.t("估算时间", "Estimate time"), formatWallTime(selectedTimeH).substring(5), E2Pink, Modifier.weight(1f))
        }
        if (activeAnalyte == Analyte.E2 && cpaValue != null) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MetricPill(language.t("雌二醇 E2", "Estradiol E2"), "${formatNumber(value, 1)} pg/mL", E2Pink, Modifier.weight(1f))
                MetricPill(antiAndrogenLabel, "${formatNumber(cpaValue, 1)} ng/mL", CpaRose, Modifier.weight(1f))
            }
        }
        AnimatedVisibility(
            visible = message.isNotBlank(),
            enter = fadeIn(tween(palette.fadeMotionDuration(72), easing = FastOutSlowInEasing)),
            exit = fadeOut(tween(palette.fadeMotionDuration(56), easing = FastOutSlowInEasing)),
        ) {
            Text(message, color = Muted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
    }
}

@Composable
private fun GradientNumber(text: String, analyte: Analyte) {
    val brush = when (analyte) {
        Analyte.E2 -> Brush.linearGradient(listOf(TBlue, E2Pink))
        Analyte.CPA -> Brush.linearGradient(listOf(CpaRose, E2Pink))
        Analyte.Testosterone -> Brush.linearGradient(listOf(TBlue, Color(0xFF4C8FD8)))
    }
    Text(
        text = text,
        fontSize = 58.sp,
        lineHeight = 62.sp,
        fontWeight = FontWeight.ExtraBold,
        style = androidx.compose.ui.text.TextStyle(brush = brush),
    )
}

private fun headerBadge(
    analyte: Analyte,
    value: Double,
    antiAndrogenLabel: String,
    antiAndrogenMedication: RecordOnlyMedication?,
    language: AppLanguage,
): HeaderBadge? =
    when (analyte) {
        Analyte.E2 -> cycleReferenceBadge(value, language)
        Analyte.CPA -> antiAndrogenReferenceBadge(value, antiAndrogenLabel, antiAndrogenMedication, language)
        Analyte.Testosterone -> testosteroneReferenceBadge(value, language)
    }

private fun testosteroneReferenceBadge(value: Double, language: AppLanguage): HeaderBadge {
    if (!value.isFinite() || value <= 0.0) {
        return HeaderBadge(
            language.t("等待 T 曲线", "Waiting for T curve"),
            language.t("添加睾酮记录后，会按 ng/dL 显示独立参考曲线。", "After adding testosterone records, the app will show an independent ng/dL reference curve."),
            TBlue,
        )
    }
    return when {
        value < 300.0 -> HeaderBadge(
            language.t("低于男性化参考", "Below masculinizing reference"),
            language.t("低于常见男性生理范围量级；模型值不能替代化验。", "Below common physiologic male-range magnitudes; model estimates do not replace labs."),
            TBlue,
        )
        value < 400.0 -> HeaderBadge(
            language.t("接近男性化参考下限", "Near lower masculinizing reference"),
            language.t("接近但仍低于常见 400-700 ng/dL 男性化治疗监测目标。", "Near but still below the common 400-700 ng/dL masculinizing therapy monitoring target."),
            TBlue,
        )
        value < 700.0 -> HeaderBadge(
            language.t("男性化 GAHT 目标参考", "Masculinizing GAHT target reference"),
            language.t("多份指南常用 400-700 ng/dL 作为睾酮治疗监测目标；此处只是估算。", "Many guidelines use 400-700 ng/dL as a testosterone therapy monitoring target; this is only an estimate."),
            Color(0xFF6EADE8),
        )
        value < 1_000.0 -> HeaderBadge(
            language.t("男性化参考上沿", "Upper masculinizing reference"),
            language.t("高于 400-700 ng/dL 目标但仍在许多成人男性参考量级内；需结合化验时点。", "Above the 400-700 ng/dL target but still within many adult male reference magnitudes; interpret with lab timing."),
            Color(0xFF8798D8),
        )
        else -> HeaderBadge(
            language.t("高于男性化参考", "Above masculinizing reference"),
            language.t("模型值高于常见男性化治疗目标和许多成人男性参考上沿，请核对记录、路线和化验。", "The model is above common masculinizing therapy targets and many adult male upper references; check records, route and labs."),
            CpaRose,
        )
    }
}

private fun antiAndrogenReferenceBadge(
    value: Double,
    antiAndrogenLabel: String,
    medication: RecordOnlyMedication?,
    language: AppLanguage,
): HeaderBadge {
    if (!value.isFinite() || value <= 0.0) {
        return HeaderBadge(
            language.t("等待${antiAndrogenLabel}曲线", "Waiting for $antiAndrogenLabel curve"),
            language.t("添加抗雄记录后，会按药品说明书/文献 PK 参数显示本地参考曲线。", "After adding antiandrogen records, the app will show a local reference curve based on label/literature PK parameters."),
            CpaRose,
        )
    }
    return when (medication) {
        RecordOnlyMedication.CyproteroneAcetate -> cpaReferenceBadge(value, antiAndrogenLabel, language)
        RecordOnlyMedication.Spironolactone -> spironolactoneReferenceBadge(value, antiAndrogenLabel, language)
        RecordOnlyMedication.Bicalutamide -> bicalutamideReferenceBadge(value, antiAndrogenLabel, language)
        RecordOnlyMedication.Finasteride -> finasterideReferenceBadge(value, antiAndrogenLabel, language)
        RecordOnlyMedication.Dutasteride -> dutasterideReferenceBadge(value, antiAndrogenLabel, language)
        null -> HeaderBadge(
            language.t("混合抗雄参考", "Mixed antiandrogen reference"),
            language.t("当前包含多种抗雄，图表为同轴趋势叠加；不要按单一药物范围解释。", "Multiple antiandrogens are combined on one trend axis; do not interpret it using a single-drug range."),
            CpaRose,
        )
    }
}

private fun cpaReferenceBadge(value: Double, label: String, language: AppLanguage): HeaderBadge =
    when {
        value < 25.0 -> HeaderBadge(
            language.t("${label}低暴露参考", "$label low-exposure reference"),
            language.t("低于 50 mg 口服说明书峰值量级，常见于低剂量或给药间隔尾段。", "Below the label peak magnitude for oral 50 mg, often seen with low dose or late dosing interval."),
            CpaRose,
        )
        value < 140.0 -> HeaderBadge(
            language.t("${label}常见尾段参考", "$label common tail reference"),
            language.t("低于 50 mg 口服约 140 ng/mL 峰值，仍只代表 PK 暴露趋势。", "Below the about 140 ng/mL peak after oral 50 mg; this only represents PK exposure trend."),
            CpaRose,
        )
        value < 300.0 -> HeaderBadge(
            language.t("${label}50 mg 峰值量级参考", "$label 50 mg peak-magnitude reference"),
            language.t("接近 50-100 mg 口服说明书峰值量级；不代表抗雄效果强弱。", "Near the label peak magnitude for oral 50-100 mg; it does not represent antiandrogen effect strength."),
            CpaRose,
        )
        value < 500.0 -> HeaderBadge(
            language.t("${label}偏高 PK 参考", "$label elevated PK reference"),
            language.t("已高于常见 50 mg 峰值量级，请核对剂量、单位和导入记录。", "Above the common 50 mg peak magnitude; check dose, units and imported records."),
            Color(0xFFCF4E9A),
        )
        else -> HeaderBadge(
            language.t("${label}高暴露提示", "$label high-exposure hint"),
            language.t("明显高于常见说明书 PK 量级；优先按记录/单位错误排查。", "Clearly above common label PK magnitudes; first rule out record or unit errors."),
            CpaRose,
        )
    }

private fun spironolactoneReferenceBadge(value: Double, label: String, language: AppLanguage): HeaderBadge =
    when {
        value < 25.0 -> HeaderBadge(
            language.t("${label}尾段参考", "$label tail reference"),
            language.t("螺内酯母药半衰期短，低值常见于给药间隔后段。", "Spironolactone parent drug has a short half-life; low values are common late in the interval."),
            CpaRose,
        )
        value < 90.0 -> HeaderBadge(
            language.t("${label}母药量级参考", "$label parent-drug magnitude"),
            language.t("接近 100 mg 口服后母药 Cmax 量级；模型同时保留代谢物趋势。", "Near the parent-drug Cmax magnitude after oral 100 mg; the model also keeps metabolite trend."),
            CpaRose,
        )
        value < 220.0 -> HeaderBadge(
            language.t("${label}活性代谢物参考", "$label active-metabolite reference"),
            language.t("接近 canrenone 等活性代谢物说明书量级；不等于抗雄疗效。", "Near label magnitudes for active metabolites such as canrenone; not equivalent to antiandrogen effect."),
            Color(0xFFCF4E9A),
        )
        value < 420.0 -> HeaderBadge(
            language.t("${label}偏高 PK 参考", "$label elevated PK reference"),
            language.t("高于常用 100 mg PK 量级，请核对剂量、频率和药瓶扣除。", "Above common 100 mg PK magnitudes; check dose, frequency and bottle deduction."),
            Color(0xFFD95A94),
        )
        else -> HeaderBadge(
            language.t("${label}高暴露提示", "$label high-exposure hint"),
            language.t("明显高于说明书代谢物量级；请优先排查记录和单位。", "Clearly above label metabolite magnitudes; first check records and units."),
            CpaRose,
        )
    }

private fun bicalutamideReferenceBadge(value: Double, label: String, language: AppLanguage): HeaderBadge =
    when {
        value < 800.0 -> HeaderBadge(
            language.t("${label}单次峰值以下", "$label below single-dose peak"),
            language.t("低于 50 mg 单次 R-比卡峰值约 0.768 mcg/mL，可能处于累积早期。", "Below the 50 mg single-dose R-bicalutamide peak of about 0.768 mcg/mL; may be early accumulation."),
            CpaRose,
        )
        value < 5_000.0 -> HeaderBadge(
            language.t("${label}累积中参考", "$label accumulating reference"),
            language.t("高于单次峰值但低于常见稳态治疗浓度，适合看趋势不适合判断疗效。", "Above single-dose peak but below common steady-state therapeutic concentrations; useful for trend, not effect."),
            CpaRose,
        )
        value < 15_000.0 -> HeaderBadge(
            language.t("${label}50 mg 稳态参考", "$label 50 mg steady-state reference"),
            language.t("DailyMed 儿科 CAH 段提到 5-15 mcg/mL 为成人 50 mg 常见治疗浓度范围。", "DailyMed's pediatric CAH section cites 5-15 mcg/mL as a common adult 50 mg therapeutic concentration range."),
            Color(0xFFCF4E9A),
        )
        value < 25_000.0 -> HeaderBadge(
            language.t("${label}高于 50 mg 稳态参考", "$label above 50 mg steady-state reference"),
            language.t("高于 50 mg 常见稳态范围；请核对剂量、单位和是否长期累积。", "Above the common 50 mg steady-state range; check dose, units and long accumulation."),
            Color(0xFFD95A94),
        )
        else -> HeaderBadge(
            language.t("${label}极高暴露提示", "$label very high-exposure hint"),
            language.t("明显高于说明书稳态范围；优先按记录、单位或导入错误排查。", "Clearly above label steady-state ranges; first rule out record, unit or import errors."),
            CpaRose,
        )
    }

private fun finasterideReferenceBadge(value: Double, label: String, language: AppLanguage): HeaderBadge =
    when {
        value < 5.0 -> HeaderBadge(
            language.t("${label}低值参考", "$label low reference"),
            language.t("低于常见血药量级，可能处于给药间隔尾段或剂量较低。", "Below common plasma magnitudes; may be late interval or low dose."),
            CpaRose,
        )
        value < 25.0 -> HeaderBadge(
            language.t("${label}低剂量 PK 参考", "$label low-dose PK reference"),
            language.t("接近低剂量口服后的估算量级；DHT 变化不能由浓度线性推出。", "Near an estimated low-dose oral magnitude; DHT change cannot be inferred linearly from concentration."),
            CpaRose,
        )
        value < 60.0 -> HeaderBadge(
            language.t("${label}5 mg Cmax 参考", "$label 5 mg Cmax reference"),
            language.t("DailyMed 5 mg 多次给药 Cmax 约 46-48 ng/mL，半衰期约 6-8 小时。", "DailyMed lists about 46-48 ng/mL Cmax after 5 mg repeated dosing, with a half-life around 6-8 h."),
            Color(0xFFCF4E9A),
        )
        value < 100.0 -> HeaderBadge(
            language.t("${label}高于 5 mg 峰值参考", "$label above 5 mg peak reference"),
            language.t("高于说明书 5 mg 峰值量级，请核对剂量和单位。", "Above the label 5 mg peak magnitude; check dose and units."),
            Color(0xFFD95A94),
        )
        else -> HeaderBadge(
            language.t("${label}极高暴露提示", "$label very high-exposure hint"),
            language.t("明显高于非那雄胺说明书 PK 量级；优先排查记录错误。", "Clearly above finasteride label PK magnitudes; first check record errors."),
            CpaRose,
        )
    }

private fun dutasterideReferenceBadge(value: Double, label: String, language: AppLanguage): HeaderBadge =
    when {
        value < 5.0 -> HeaderBadge(
            language.t("${label}单次/早期参考", "$label single-dose or early reference"),
            language.t("度他雄胺半衰期很长，早期低值不代表稳态。", "Dutasteride has a very long half-life; early low values do not represent steady state."),
            CpaRose,
        )
        value < 25.0 -> HeaderBadge(
            language.t("${label}累积中参考", "$label accumulating reference"),
            language.t("低于 0.5 mg/day 长期稳态约 40 ng/mL，可能仍在累积。", "Below the about 40 ng/mL long-term steady state for 0.5 mg/day; may still be accumulating."),
            CpaRose,
        )
        value < 60.0 -> HeaderBadge(
            language.t("${label}0.5 mg 稳态参考", "$label 0.5 mg steady-state reference"),
            language.t("DailyMed 写明 0.5 mg/day 一年平均稳态约 40 ng/mL，终末半衰期约 5 周。", "DailyMed lists about 40 ng/mL average steady state after 0.5 mg/day for 1 year, with about 5-week terminal half-life."),
            Color(0xFFCF4E9A),
        )
        value < 100.0 -> HeaderBadge(
            language.t("${label}高于稳态参考", "$label above steady-state reference"),
            language.t("高于 0.5 mg/day 常见稳态量级；请核对剂量、频率和单位。", "Above common 0.5 mg/day steady-state magnitudes; check dose, frequency and units."),
            Color(0xFFD95A94),
        )
        else -> HeaderBadge(
            language.t("${label}极高暴露提示", "$label very high-exposure hint"),
            language.t("明显高于说明书稳态量级；优先排查记录或单位错误。", "Clearly above label steady-state magnitudes; first check record or unit errors."),
            CpaRose,
        )
    }

private fun cycleReferenceBadge(value: Double, language: AppLanguage): HeaderBadge {
    if (!value.isFinite() || value <= 0.0) {
        return HeaderBadge(
            language.t("等待 E2 曲线", "Waiting for E2 curve"),
            language.t("添加雌二醇记录后，会按 E2 浓度给出周期参考。", "After adding estradiol records, the app will show a cycle reference based on E2."),
            E2Pink,
        )
    }
    if (value >= 10_000.0) {
        return HeaderBadge(
            language.t("万雌王", "E2 Overlord"),
            language.t("彩蛋已触发：数值远高于常见周期参考区间，请确认记录与单位。", "Easter egg triggered: this is far above common cycle reference ranges. Please check records and units."),
            CpaRose,
        )
    }
    return when {
        value < 12.0 -> HeaderBadge(language.t("低于常见周期参考", "Below common cycle reference"), language.t("参考范围随实验室差异很大；此处仅按 E2 粗略提示。", "Reference ranges vary by lab; this is only a rough E2 hint."), Color(0xFF8AA1B3))
        value < 43.8 -> HeaderBadge(language.t("女性卵泡期参考", "Follicular-phase reference"), language.t("按常见实验室区间粗略匹配；周期阶段不能只靠 E2 判断。", "Roughly matched against common lab ranges; cycle phase cannot be determined from E2 alone."), E2Pink)
        value <= 211.0 -> HeaderBadge(language.t("女性黄体期参考", "Luteal-phase reference"), language.t("该区间与卵泡期/排卵期存在重叠，仅作视觉参考。", "This range overlaps follicular/ovulatory references and is visual guidance only."), E2Pink)
        value <= 498.0 -> HeaderBadge(language.t("女性排卵期参考", "Ovulatory reference"), language.t("E2 已接近常见排卵期参考区间；不是医学判断。", "E2 is near common ovulatory references; this is not a medical judgment."), Color(0xFFB58AF0))
        else -> HeaderBadge(language.t("高于常见周期参考", "Above common cycle reference"), language.t("已超过多数非妊娠周期参考区间，请结合化验与医生意见。", "Above many non-pregnancy cycle reference ranges; interpret with labs and clinician guidance."), CpaRose)
    }
}

@Composable
private fun OverviewScreen(
    activeAnalyte: Analyte,
    onAnalyteChange: (Analyte) -> Unit,
    simulation: SimulationResult?,
    currentValue: Double,
    cpaCurrentValue: Double,
    hasCpaData: Boolean,
    antiAndrogenLabel: String,
    antiAndrogenMedication: RecordOnlyMedication?,
    medicationStatusSummaries: List<MedicationStatusSummary>,
    statusNowH: Double,
    weightKg: Double,
    selectedTimeH: Double,
    message: String,
    language: AppLanguage,
    onOpenPlan: () -> Unit,
    onWeightClick: () -> Unit,
    onTimeClick: () -> Unit,
    onCpaPreviewClick: () -> Unit,
    onTimeSelected: (Double) -> Unit,
    onResetTime: () -> Unit,
    onPickTime: () -> Unit,
    onShare: () -> Unit,
    onSaveImage: () -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 132.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            HeaderCard(
                activeAnalyte = activeAnalyte,
                value = currentValue,
                unit = activeAnalyte.unit,
                cpaValue = cpaCurrentValue.takeIf { hasCpaData },
                antiAndrogenLabel = antiAndrogenLabel,
                antiAndrogenMedication = antiAndrogenMedication,
                weightKg = weightKg,
                selectedTimeH = selectedTimeH,
                message = message,
                language = language,
                onWeightClick = onWeightClick,
                onTimeClick = onTimeClick,
                onCpaPreviewClick = onCpaPreviewClick,
            )
        }
        item {
            NativeCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = language.t("血药浓度", "Concentration"),
                        color = Ink,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(language.t("点击曲线选点", "Tap"), color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                Spacer(Modifier.height(16.dp))
                SegmentedAnalyte(activeAnalyte, language, onAnalyteChange)
                Spacer(Modifier.height(18.dp))
                ResultChart(
                    analyte = activeAnalyte,
                    simulation = simulation,
                    selectedTimeH = selectedTimeH,
                    onTimeSelected = onTimeSelected,
                    onResetTime = onResetTime,
                    color = analyteColor(activeAnalyte),
                    unit = activeAnalyte.unit,
                    chartLabel = activeAnalyte.displayLabel(language, antiAndrogenLabel),
                    language = language,
                )
            }
        }
        item {
            MedicationStatusCards(
                summaries = medicationStatusSummaries,
                language = language,
                nowH = statusNowH,
                onOpenPlan = onOpenPlan,
            )
        }
        item {
            TimeGaugeCard(
                analyte = activeAnalyte,
                value = currentValue,
                unit = activeAnalyte.unit,
                selectedTimeH = selectedTimeH,
                onTimeSelected = onTimeSelected,
                onResetTime = onResetTime,
                onPickTime = onPickTime,
                onShare = onShare,
                onSaveImage = onSaveImage,
                language = language,
            )
        }
    }
}

@Composable
private fun JourneyPreviewCard(language: AppLanguage) {
    NativeCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SoftIconBadge("1", E2Pink, sizeDp = 62)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(language.t("起始期", "Initiation"), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text(language.t("0-3 个月", "0-3 months"), color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    language.t("皮肤、情绪、身体感受都可以慢慢记录；所有数据只留在本机。", "Track changes gently. All data stays on this device."),
                    color = Muted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(6) { index ->
                        Box(
                            Modifier
                                .size(if (index == 0) 7.dp else 6.dp)
                                .background(if (index == 0) E2Pink else LocalAppPalette.current.divider, CircleShape),
                        )
                    }
                }
            }
            Text("›", color = Muted, fontSize = 32.sp, fontWeight = FontWeight.Light)
        }
    }
}

@Composable
private fun SegmentedAnalyte(active: Analyte, language: AppLanguage, onChange: (Analyte) -> Unit) {
    val palette = LocalAppPalette.current
    val shape = RoundedCornerShape(28.dp)
    val selectedIndex = Analyte.entries.indexOf(active).coerceAtLeast(0)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.subtleSurface, shape)
            .padding(4.dp),
    ) {
        val itemWidth = maxWidth / Analyte.entries.size.toFloat()
        val density = LocalDensity.current
        val indicatorOffsetPx by animateFloatAsState(
            targetValue = with(density) { (itemWidth * selectedIndex.toFloat()).toPx() },
            animationSpec = tween(palette.motionDuration(COLOROS_SLIDE_MS), easing = ColorOsEasing),
            label = "analyte-indicator-offset",
        )
        val indicatorBorder = appColorAsState(analyteColor(active).copy(alpha = 0.28f), "analyte-indicator-border")
        Box(
            modifier = Modifier
                .width(itemWidth)
                .height(48.dp)
                .graphicsLayer { translationX = indicatorOffsetPx }
                .padding(horizontal = 2.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.card, RoundedCornerShape(24.dp)),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Analyte.entries.forEach { item ->
                val selected = item == active
                val interaction = remember(item) { MutableInteractionSource() }
                val scale = colorOsPressScale(interaction, pressedScale = 0.95f, label = "analyte-press-${item.name}")
                val fg = appColorAsState(if (selected) analyteColor(item) else Muted, "analyte-fg-${item.name}")
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            onClick = { onChange(item) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(item.tabLabel(language), color = fg, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun TimeGaugeCard(
    analyte: Analyte,
    value: Double,
    unit: String,
    selectedTimeH: Double,
    onTimeSelected: (Double) -> Unit,
    onResetTime: () -> Unit,
    onPickTime: () -> Unit,
    onShare: () -> Unit,
    onSaveImage: () -> Unit,
    language: AppLanguage,
) {
    NativeCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TimeGauge(
                selectedTimeH = selectedTimeH,
                color = analyteColor(analyte),
                modifier = Modifier.size(136.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(language.t("时间仪表盘", "Time Dial"), fontSize = 20.sp, color = Ink, fontWeight = FontWeight.Bold)
                Text(formatWallTime(selectedTimeH), color = Muted, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                Text("${formatNumber(value, 1)} $unit", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                if (analyte == Analyte.E2 && value >= 10_000.0) {
                    Text(language.t("万雌王时间线已记录", "E2 Overlord timeline recorded"), color = CpaRose, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onTimeSelected(selectedTimeH - 6.0) }, shape = RoundedCornerShape(16.dp)) { Text("-6h") }
            OutlinedButton(onClick = { onTimeSelected(selectedTimeH - 1.0) }, shape = RoundedCornerShape(16.dp)) { Text("-1h") }
            Button(onClick = onPickTime, shape = RoundedCornerShape(16.dp)) { Text(language.t("选择时间", "Pick Time")) }
            OutlinedButton(onClick = { onTimeSelected(selectedTimeH + 1.0) }, shape = RoundedCornerShape(16.dp)) { Text("+1h") }
            OutlinedButton(onClick = { onTimeSelected(selectedTimeH + 6.0) }, shape = RoundedCornerShape(16.dp)) { Text("+6h") }
            OutlinedButton(onClick = onResetTime, shape = RoundedCornerShape(16.dp)) { Text(language.t("当前", "Now")) }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onShare, modifier = Modifier.weight(1f), shape = RoundedCornerShape(18.dp)) {
                Text(language.t("分享到 X", "Share to X"))
            }
            OutlinedButton(onClick = onSaveImage, modifier = Modifier.weight(1f), shape = RoundedCornerShape(18.dp)) {
                Text(language.t("保存图片", "Save Image"))
            }
        }
    }
}

@Composable
private fun TimeGauge(selectedTimeH: Double, color: Color, modifier: Modifier = Modifier) {
    val palette = LocalAppPalette.current
    val local = remember(selectedTimeH) {
        LocalDateTime.ofInstant(
            Instant.ofEpochMilli((selectedTimeH * 3_600_000.0).toLong()),
            ZoneId.systemDefault(),
        )
    }
    val hourText = remember(local) { "%02d:%02d".format(Locale.US, local.hour, local.minute) }
    val dateText = remember(local) { "%02d-%02d".format(Locale.US, local.monthValue, local.dayOfMonth) }
    Canvas(modifier = modifier) {
        val stroke = 11.dp.toPx()
        val radius = (min(size.width, size.height) - stroke) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(palette.subtleSurface, radius = radius, center = center, style = Stroke(width = stroke))
        val minutes = local.hour * 60 + local.minute
        val sweep = (minutes / 1440f) * 360f
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        drawCircle(palette.card, radius = radius - stroke * 1.15f, center = center)
        drawContext.canvas.nativeCanvas.apply {
            val hourPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                this.color = palette.ink.toArgb()
                textSize = 30.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            }
            val datePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                this.color = palette.muted.toArgb()
                textSize = 12.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
            }
            drawText(hourText, center.x, center.y + 2.dp.toPx(), hourPaint)
            drawText(dateText, center.x, center.y + 27.dp.toPx(), datePaint)
        }
    }
}

@Composable
private fun LabActionCard(count: Int, language: AppLanguage, onAdd: () -> Unit) {
    NativeCard(modifier = Modifier.clickable(onClick = onAdd)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MistBlue, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("⚗", color = E2Pink, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    language.t("化验结果", "Lab Results"),
                    color = Ink,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(language.t("$count 条 E2 化验", "$count E2 lab results"), color = Muted, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = onAdd,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text(language.t("+ 新增化验", "+ Add Lab"), fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun LabsScreen(
    labs: List<LabResult>,
    calibrationSummary: CalibrationSummary,
    calibrationModel: String,
    onCalibrationModelChange: (String) -> Unit,
    onAdd: () -> Unit,
    language: AppLanguage,
    onDelete: (LabResult) -> Unit,
) {
    val summary = calibrationSummary
    val state = summary.replay.state
    val diagnostics = summary.replay.diagnostics
    val newestLabs = remember(labs) { labs.sortedByDescending { it.timeH } }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 120.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            LargePageTitle(
                title = language.t("化验", "Lab Reports"),
                subtitle = language.t("个体化校准", "Personal calibration"),
            )
        }
        item {
            LabActionCard(count = labs.size, language = language, onAdd = onAdd)
        }
        item {
            NativeCard {
                Text(language.t("个体化 E2 校准", "Personalized E2 Calibration"), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                Text(
                    language.t(
                        "保留双模型：EKF 用化验学习吸收倍率与清除倍率；OU-Kalman 在原始曲线的 log residual 上做平滑校准。仅供趋势参考，无医疗价值。",
                        "Dual models stay enabled: EKF learns absorption and clearance multipliers from labs; OU-Kalman smooths log residuals over the raw curve. Trend reference only, not medical advice.",
                    ),
                    color = Muted,
                    lineHeight = 21.sp,
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LocalAppPalette.current.subtleSurface, RoundedCornerShape(24.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    listOf("ekf" to "EKF", "ou-kalman" to "OU-Kalman").forEach { (key, label) ->
                        val selected = calibrationModel == key
                        val bg = appColorAsState(
                            if (selected) LocalAppPalette.current.card else Color.Transparent,
                            "model-bg-$key",
                        )
                        val fg = appColorAsState(if (selected) E2Pink else Muted, "model-fg-$key")
                        Surface(
                            onClick = { onCalibrationModelChange(key) },
                            color = bg,
                            contentColor = fg,
                            border = if (selected) BorderStroke(1.dp, E2Pink.copy(alpha = 0.10f)) else null,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                Text(label, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricPill("baseline E2", state.baselinePGmL?.let { "${formatNumber(it, 1)} pg/mL" } ?: language.t("未建立", "Not established"))
                    MetricPill(language.t("有效化验", "Valid labs"), "${state.postDoseObservationCount}/${state.observationCount}")
                    MetricPill(language.t("校准状态", "Calibration"), if (summary.hasPostDoseCalibration) language.t("已启用", "Enabled") else language.t("等待给药后化验", "Waiting for post-dose lab"))
                    MetricPill(language.t("收敛度", "Convergence"), "${formatNumber((diagnostics?.convergenceScore ?: 0.0) * 100.0, 0)}%")
                }
                if (summary.hasPostDoseCalibration) {
                    Text(
                        language.t(
                            "概览 E2 当前浓度和分享图已使用所选模型的校准曲线；没有有效校准时自动回到原始模拟曲线。",
                            "Overview E2 and share images use the selected calibrated curve. Without valid calibration, the app falls back to the raw simulated curve.",
                        ),
                        color = Muted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
        }
        if (diagnostics != null) {
            item {
                NativeCard {
                    Text(language.t("最近一次诊断", "Latest Diagnostics"), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                    Spacer(Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricPill(language.t("观测值", "Observed"), "${formatNumber(diagnostics.observedPGmL, 1)} pg/mL")
                        MetricPill(language.t("预测值", "Predicted"), "${formatNumber(diagnostics.predictedPGmL, 1)} pg/mL")
                        MetricPill("残差 log", formatNumber(diagnostics.residualLog, 3))
                        MetricPill("NIS", formatNumber(diagnostics.nis, 2), if (diagnostics.isOutlier) Color(0xFFDC2626) else E2Pink)
                        MetricPill(language.t("吸收倍率", "Absorption x"), "x${formatNumber(diagnostics.thetaS, 2)}")
                        MetricPill(language.t("清除倍率", "Clearance x"), "x${formatNumber(diagnostics.thetaK, 2)}")
                        MetricPill("95% CI", "${formatNumber(diagnostics.ci95Low, 0)}-${formatNumber(diagnostics.ci95High, 0)}")
                        MetricPill(language.t("异常点", "Outlier"), if (diagnostics.isOutlier) language.t("是，已降权", "Yes, downweighted") else language.t("否", "No"))
                    }
                }
            }
        }
        item {
            NativeCard {
                Text(language.t("算法说明", "Algorithm Notes"), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                Text(language.t("EKF：把化验 E2 与本地药代曲线在对数域比较，学习整体吸收倍率 thetaS 与清除倍率 thetaK，并用 NIS 判断异常点；异常值不会强行拖垮曲线。", "EKF compares lab E2 against the local PK curve in log space, learns thetaS / thetaK multipliers, and uses NIS to detect outliers so one bad lab does not dominate the curve."), color = Muted, lineHeight = 21.sp)
                Spacer(Modifier.height(8.dp))
                Text(language.t("OU-Kalman：不直接改药代参数，而是学习化验相对原始曲线的 log residual，适合在化验较少时做更平滑的个体化校准。", "OU-Kalman does not directly edit PK parameters; it learns smoothed log residuals against the raw curve, which is useful when lab data is sparse."), color = Muted, lineHeight = 21.sp)
            }
        }
        items(newestLabs, key = { it.id }) { lab ->
            NativeCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("${formatNumber(lab.concValue, 1)} ${lab.unit}", fontSize = 20.sp, color = Ink, fontWeight = FontWeight.Bold)
                        Text(formatWallTime(lab.timeH), color = Muted)
                    }
                    TextButton(onClick = { onDelete(lab) }) { Text(language.t("删除", "Delete"), color = Color(0xFFDC2626)) }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    weightKg: Double,
    onWeightChange: (Double) -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    appLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    snapshot: AppStateSnapshot,
    onImport: () -> Unit,
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit,
    onOpenUri: (String) -> Unit,
) {
    var weightText by remember(weightKg) { mutableStateOf(formatNumber(weightKg, 1)) }
    var showAbout by remember { mutableStateOf(false) }
    var showPrivacyTerms by remember { mutableStateOf(false) }
    if (showAbout) {
        AboutDialog(
            language = appLanguage,
            onOpenUri = onOpenUri,
            onDismiss = { showAbout = false },
        )
    }
    if (showPrivacyTerms) {
        PrivacyTermsDialog(
            language = appLanguage,
            onDismiss = { showPrivacyTerms = false },
        )
    }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 120.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            SectionTitle(appLanguage.t("设置", "Settings"), appLanguage.t("偏好与数据", "Preferences & Data"))
        }
        item {
            NativeCard {
                Text(appLanguage.t("语言", "Language"), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text(appLanguage.t("首次引导中的语言选择会保存在本机，后续可在这里调整。", "The language chosen in onboarding is saved locally and can be changed here."), color = Muted, lineHeight = 20.sp)
                Spacer(Modifier.height(12.dp))
                ChipFlow(AppLanguage.entries, appLanguage, { it.displayLabel(appLanguage) }) { selected ->
                    onLanguageChange(selected)
                }
                Text(appLanguage.t("当前：", "Current: ") + appLanguage.displayLabel(appLanguage), color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
        item {
            NativeCard {
                Text(appLanguage.t("外观", "Appearance"), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text(appLanguage.t("可以固定浅色/深色，也可以跟随系统深色模式。", "Use light or dark mode, or follow the system setting."), color = Muted, lineHeight = 20.sp)
                Spacer(Modifier.height(12.dp))
                ChipFlow(ThemeMode.entries, themeMode, { it.label(appLanguage) }) { selected ->
                    onThemeModeChange(selected)
                }
                Text(appLanguage.t("当前：", "Current: ") + themeMode.label(appLanguage), color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
        item {
            NativeCard {
                Text(appLanguage.t("体重", "Weight"), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = weightText,
                    onValueChange = {
                        weightText = it
                        it.toDoubleOrNull()?.takeIf { value -> value > 0.0 }?.let(onWeightChange)
                    },
                    suffix = { Text("kg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            NativeCard {
                Text(appLanguage.t("数据", "Data"), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text(appLanguage.t("本地保存，不申请网络权限。JSON 保持 hrt.mahiro / HRT Recorder 兼容结构。", "Saved locally with no network permission. JSON stays compatible with hrt.mahiro / HRT Recorder structures."), color = Muted, lineHeight = 20.sp)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = onExportJson, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) { Text(appLanguage.t("导出 JSON", "Export JSON")) }
                    OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) { Text(appLanguage.t("导入", "Import")) }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onExportCsv, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Text(appLanguage.t("导出 CSV", "Export CSV"))
                }
                Text(appLanguage.t("当前：${snapshot.events.size} 条用药，${snapshot.labResults.size} 条化验。", "Current: ${snapshot.events.size} dose records, ${snapshot.labResults.size} lab results."), color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
        item {
            LegalPrivacyCard(
                language = appLanguage,
                onOpenFullAgreement = { showPrivacyTerms = true },
            )
        }
        item {
            NativeCard {
                Text(appLanguage.t("关于 HRT Recorder", "About HRT Recorder"), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text(
                    appLanguage.t(
                        "开发初衷、数据主权原则、参考来源与署名都放在这里。",
                        "Purpose, data sovereignty principles, references and credits live here.",
                    ),
                    color = Muted,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = { showAbout = true }, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(appLanguage.t("打开关于页面", "Open About Page"))
                }
            }
        }
        item {
            NativeCard {
                Text(appLanguage.t("作者与协议", "Author and Terms"), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = { onOpenUri("https://x.com/xynMTFxyn") }) { Text(appLanguage.t("作者：南盺", "Author: Nanxin")) }
                TextButton(onClick = { onOpenUri("mailto:wangyanluo233@gmail.com") }) { Text(appLanguage.t("联系邮箱：wangyanluo233@gmail.com", "Email: wangyanluo233@gmail.com")) }
                TextButton(onClick = { showPrivacyTerms = true }) { Text(appLanguage.t("用户与隐私协议", "Terms and Privacy")) }
                TextButton(onClick = { onOpenUri("https://orange-truth-08b4.guhuao666.workers.dev/") }) { Text(appLanguage.t("公开隐私政策网页", "Public Privacy Policy Page")) }
                Text(appLanguage.t("本软件仅供记录与趋势参考，无医疗价值；不收集个人信息，可无网络运行。", "This app is for records and trend reference only, has no medical value, collects no personal information, and works offline."), color = Muted, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
private fun AboutDialog(
    language: AppLanguage,
    onOpenUri: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(language.t("关于 HRT Recorder", "About HRT Recorder"), color = Ink, fontWeight = FontWeight.ExtraBold) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(560.dp),
            ) {
                item {
                    Text(language.t("开发初衷", "Purpose"), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        language.t(
                            "HRT Recorder 的 Android 版由南盺开发。它想解决一个很朴素的问题：让 MTF 们在安卓手机上更简单、更快速地记录 HRT，用本地曲线和化验校准观察趋势，而不是把私密数据交给网站账号体系。",
                            "The Android version of HRT Recorder is developed by Nanxin. It exists for a simple reason: helping MTF users record HRT faster and more easily on Android, using local curves and lab calibration to view trends without handing private data to a website account system.",
                        ),
                        color = Muted,
                        lineHeight = 21.sp,
                    )
                }
                item {
                    Text(language.t("数据主权", "Data Sovereignty"), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        language.t(
                            "本软件默认不申请网络权限，不做登录、云同步、远程配置、删号机制或服务端权限判断。你的用药、化验、体重和围度数据默认只保存在本机；只要你保留自己的备份，就不应该因为某个平台、某个网站或某个运营方的立场变化而失去自己的记录。",
                            "The app requests no network permission by default, and has no login, cloud sync, remote config, account deletion mechanism or server-side access gate. Your medication, labs, weight and measurements stay on your device by default. As long as you keep your own backups, your records should not disappear because a platform, website or operator changes their position.",
                        ),
                        color = Muted,
                        lineHeight = 21.sp,
                    )
                }
                item {
                    Text(language.t("为什么坚持离线", "Why Offline Matters"), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        language.t(
                            "作者曾亲眼见过一位 MTF 因自身立场与 hrt.mahiro.hk 的运营方理念不同，而遭遇账号删除和拒绝服务。无论争议来自哪里，HRT Recorder 的原则都很明确：血药记录属于用户本人，我们绝不把核心记录能力做成可以被删号、封禁或远程拒绝服务的东西。",
                            "The author personally witnessed an MTF user lose account access and service because their own stance differed from the operator of hrt.mahiro.hk. Whatever the dispute is, HRT Recorder has a clear principle: blood-level records belong to the user. We will never make the core recording ability something that can be removed by account deletion, bans or remote service denial.",
                        ),
                        color = Muted,
                        lineHeight = 21.sp,
                    )
                }
                item {
                    Text(language.t("医学边界", "Medical Boundary"), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        language.t(
                            "本软件只做记录、趋势估算、分享图和离线参考，不提供诊断、处方、剂量建议或治疗承诺。任何 HRT 决策都应结合化验、身体反应和合格医疗专业人员意见。",
                            "This app only provides records, trend estimates, share images and offline reference. It does not provide diagnosis, prescriptions, dosage advice or treatment promises. HRT decisions should involve labs, body response and qualified medical professionals.",
                        ),
                        color = Muted,
                        lineHeight = 21.sp,
                    )
                }
                item {
                    Text(language.t("作者与署名", "Authorship"), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(language.t("Android 版作者：南盺", "Android version author: Nanxin"), color = Ink, fontWeight = FontWeight.Bold)
                    Text(
                        language.t(
                            "下列项目是文件层、代码层、说明层或算法思路的参考来源，不作为本 Android App 的用户界面作者署名。",
                            "The following projects are references for files, code, documentation or algorithm ideas. They are not user-visible authorship credits for this Android app.",
                        ),
                        color = Muted,
                        lineHeight = 21.sp,
                    )
                }
                item {
                    Text(language.t("参考来源", "References"), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    AboutReferenceButton("Journey / m1zukiqaqaqaq", "https://x.com/m1zukiqaqaqaq", onOpenUri)
                    AboutReferenceButton("HRT-Recorder-online / LaoZhong-Mihari", "https://github.com/LaoZhong-Mihari/HRT-Recorder-online", onOpenUri)
                    AboutReferenceButton("Transmtf-HRT-Tracker / TransmtfTeam", "https://github.com/TransmtfTeam/Transmtf-HRT-Tracker", onOpenUri)
                    AboutReferenceButton("HRT-Recorder-PKcomponent-Test / LaoZhong-Mihari", "https://github.com/LaoZhong-Mihari/HRT-Recorder-PKcomponent-Test", onOpenUri)
                    AboutReferenceButton("hrt.mahiro.uk", "https://hrt.mahiro.uk/", onOpenUri)
                    AboutReferenceButton("MtF-wiki 罩杯计算器", "https://mtf.wiki/zh-cn/cup-calculator", onOpenUri)
                    AboutReferenceButton("MtF-wiki 内容仓库 / project-trans", "https://github.com/project-trans/MtF-wiki", onOpenUri)
                    AboutReferenceButton("University of Iowa Pathology Handbook - Estradiol", "https://www.healthcare.uiowa.edu/path_handbook/handbook/test748.html", onOpenUri)
                    AboutReferenceButton("Labcorp Estradiol reference range", "https://www.labcorp.com/tests/004515/estradiol", onOpenUri)
                    AboutReferenceButton("University of Washington Test Guide - Estradiol", "https://testguide.labmed.uw.edu/view/EDOL", onOpenUri)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(16.dp)) {
                Text(language.t("知道了", "OK"))
            }
        },
    )
}

@Composable
private fun AboutReferenceButton(label: String, url: String, onOpenUri: (String) -> Unit) {
    TextButton(onClick = { onOpenUri(url) }, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            Text(label, color = Ink, fontWeight = FontWeight.Bold)
            Text(url, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private data class LegalSection(val title: String, val body: String)

private fun privacyTermsSections(language: AppLanguage): List<LegalSection> =
    listOf(
        LegalSection(
            language.t("一、协议范围与生效", "1. Scope and Effective Date"),
            language.t(
                "本《用户与隐私协议》适用于 HRT Recorder Android 版。生效日期：2026-04-26。继续安装、打开、浏览、记录、导入、导出、分享或以其它方式使用本软件，即表示你已阅读并理解本协议。若你不同意本协议，请停止使用并删除本软件。本协议不替代任何医疗机构、医生、药师、实验室或监管机构提供的专业意见。",
                "This Terms and Privacy Agreement applies to the Android version of HRT Recorder. Effective date: 2026-04-26. By installing, opening, browsing, recording, importing, exporting, sharing or otherwise using the app, you confirm that you have read and understood this agreement. If you do not agree, stop using and delete the app. This agreement does not replace professional advice from medical institutions, clinicians, pharmacists, laboratories or regulators.",
            ),
        ),
        LegalSection(
            language.t("二、软件定位", "2. Nature of the App"),
            language.t(
                "HRT Recorder 是一个本地离线记录与趋势估算工具，用于帮助用户整理给药记录、化验结果、体重、围度、罩杯参考、分享图片和 JSON/CSV 数据。软件中的 E2、CPA、睾酮曲线、EKF / OU-Kalman 校准、周期参考、罩杯计算器、报告和分享图均属于模型估算或尺码参考，不构成医疗诊断、治疗方案、处方建议、剂量建议、用药安全承诺或实验室结果解释。",
                "HRT Recorder is a local offline record and trend-estimation tool for medication records, lab results, weight, measurements, cup-size reference, share images and JSON/CSV data. E2, CPA and testosterone curves, EKF / OU-Kalman calibration, cycle references, cup calculator, reports and share cards are model estimates or sizing references only. They are not medical diagnosis, treatment plans, prescriptions, dosage advice, medication safety guarantees or laboratory interpretation.",
            ),
        ),
        LegalSection(
            language.t("三、本地优先与不收集原则", "3. Local-First and No-Collection Principle"),
            language.t(
                "本软件默认不申请 INTERNET 网络权限，不提供账号系统，不接入登录、广告、统计 SDK、云同步、远程配置、远程风控、远程删号或服务端权限判断。你输入的用药、化验、体重、围度、语言、主题、校准模型和本地设置默认保存在你的设备内。软件开发者不会通过本软件主动收集、上传、查看、出售、出租、共享或分析你的个人信息、健康信息或使用行为。",
                "The app requests no INTERNET permission by default. It has no account system, login, ads, analytics SDK, cloud sync, remote config, remote risk control, remote account deletion or server-side access gate. Medication, labs, weight, measurements, language, theme, calibration model and local settings are stored on your device by default. The developer does not actively collect, upload, view, sell, rent, share or analyze your personal information, health information or usage behavior through this app.",
            ),
        ),
        LegalSection(
            language.t("四、本软件可能处理的数据", "4. Data Processed by the App"),
            language.t(
                "为了实现本地功能，软件会在设备上处理你主动输入或导入的数据，包括但不限于：给药时间、药物名称、给药方式、剂量、贴片每日释放量与总剂量、化验时间、化验数值、单位、体重、围度、罩杯计算输入、语言、主题、校准模型、导入导出内容和分享图片。上述数据属于敏感健康相关信息，请你谨慎录入、保管、展示、导出和分享。",
                "To provide local features, the app processes data you enter or import on your device, including but not limited to medication time, medication name, route, dose, patch daily release and total dose, lab time, lab value, units, weight, measurements, cup calculator inputs, language, theme, calibration model, import/export content and share images. This may be sensitive health-related information. Please enter, store, display, export and share it carefully.",
            ),
        ),
        LegalSection(
            language.t("五、存储、备份与删除", "5. Storage, Backup and Deletion"),
            language.t(
                "应用数据主要保存在 Android 应用私有存储和系统偏好设置中。你可以在软件内删除单条记录、导入覆盖数据、导出备份，或通过 Android 系统清除应用数据/卸载应用来删除本机数据。卸载应用、清除数据、系统故障、设备损坏、换机或备份丢失可能导致记录不可恢复。请在需要时自行导出 JSON/CSV 并妥善保存；导出文件默认不进行额外加密。",
                "App data is mainly stored in Android app-private storage and system preferences. You can delete records in the app, overwrite data by import, export backups, or delete local data through Android's clear-data/uninstall features. Uninstalling, clearing data, system failures, device damage, changing phones or losing backups may make records unrecoverable. Export JSON/CSV when needed and keep them safely. Exported files are not additionally encrypted by default.",
            ),
        ),
        LegalSection(
            language.t("六、权限与系统能力", "6. Permissions and System Capabilities"),
            language.t(
                "本软件默认不使用网络权限。保存图片、导出文件、导入文件、复制内容、调用系统分享面板、发送邮件或打开外部参考链接时，可能会调用 Android 系统文件选择器、分享面板、浏览器、邮件应用或其它由你选择的第三方应用。此类操作发生在你的设备和你选择的应用之间；第三方应用如何处理数据，请以其自身协议为准。",
                "The app does not use network permission by default. Saving images, exporting files, importing files, copying content, using the system share sheet, sending email or opening external reference links may invoke Android file pickers, share sheets, browsers, mail apps or other apps you choose. Those actions occur between your device and the apps you choose. How third-party apps process data is governed by their own policies.",
            ),
        ),
        LegalSection(
            language.t("七、导入、导出、分享与剪贴板", "7. Import, Export, Sharing and Clipboard"),
            language.t(
                "你可以将 JSON/CSV、图片或文本导出到本机文件、其它应用、社交平台或剪贴板。导出或分享后，数据可能被目标应用、系统相册、文件管理器、云盘、输入法、社交平台、浏览器、联系人或接收方保存、同步、转发、截图或再次公开。请在分享前检查内容是否包含姓名、时间、用药、化验、体重、围度等敏感信息。软件无法撤回你已导出的文件、已发送的图片或已复制的文本。",
                "You may export JSON/CSV, images or text to local files, other apps, social platforms or the clipboard. After export or sharing, data may be saved, synced, forwarded, screenshotted or republished by target apps, galleries, file managers, cloud drives, keyboards, social platforms, browsers, contacts or recipients. Before sharing, check whether the content contains sensitive information such as name, time, medication, labs, weight or measurements. The app cannot recall exported files, sent images or copied text.",
            ),
        ),
        LegalSection(
            language.t("八、医疗与安全免责声明", "8. Medical and Safety Disclaimer"),
            language.t(
                "请不要仅依据本软件开始、停止、增减、替换或混用任何药物；不要仅依据本软件判断化验异常、药物过量、血栓风险、肝肾功能、心血管风险、心理状态或其它健康问题。不同个体、剂型、批次、注射部位、吸收情况、检测方法、单位换算、记录误差和算法假设都可能造成数值偏差。若出现胸痛、呼吸困难、晕厥、严重过敏、剧烈疼痛、明显精神危机或其它紧急情况，请立即寻求线下急救或医疗帮助。",
                "Do not start, stop, increase, decrease, replace or combine medications based only on this app. Do not use it alone to judge abnormal labs, overdose, thrombosis risk, liver/kidney function, cardiovascular risk, mental state or other health issues. Individual differences, formulations, batches, injection sites, absorption, lab methods, unit conversion, recording errors and algorithm assumptions may all cause deviations. For chest pain, breathing difficulty, fainting, severe allergy, intense pain, mental-health crisis or other emergencies, seek in-person emergency or medical help immediately.",
            ),
        ),
        LegalSection(
            language.t("九、算法、参考资料与误差", "9. Algorithms, References and Error"),
            language.t(
                "软件中的药代曲线、校准模型、参考区间和罩杯计算基于公开资料、开源项目、工程近似和本地实现。参考来源不代表其作者参与、审核、认可或担保本软件。由于模型限制，结果可能与真实血药浓度、其它网站、实验室报告或临床判断不同。兼容 hrt.mahiro 等格式仅代表数据结构兼容，不代表算法完全一致。",
                "PK curves, calibration models, reference ranges and cup calculations are based on public materials, open-source projects, engineering approximations and local implementation. References do not imply participation, review, endorsement or guarantee by their authors. Due to model limitations, results may differ from real blood levels, other websites, lab reports or clinical judgment. Compatibility with formats such as hrt.mahiro means data-structure compatibility, not algorithm identity.",
            ),
        ),
        LegalSection(
            language.t("十、用户义务", "10. User Responsibilities"),
            language.t(
                "你应确保自己有权记录、导入、导出和分享相关数据；不得使用本软件从事违法、有害、骚扰、冒充他人、泄露他人隐私、传播恶意文件或侵犯第三方权利的行为。若你为他人记录数据，应先取得其明确同意并遵守适用法律、伦理和平台规则。未成年人或受监护人使用本软件时，应在监护人、医生或当地法律允许的范围内进行。",
                "You are responsible for ensuring that you have the right to record, import, export and share relevant data. Do not use the app for illegal, harmful, harassing, impersonating, privacy-violating, malicious-file-distributing or rights-infringing activities. If you record data for someone else, obtain clear consent and follow applicable law, ethics and platform rules. Minors or people under guardianship should use the app only within the scope allowed by guardians, clinicians and local law.",
            ),
        ),
        LegalSection(
            language.t("十一、知识产权与开源参考", "11. Intellectual Property and Open References"),
            language.t(
                "HRT Recorder Android 版的原生应用实现、离线打包、UI、数据导入导出、分享图、校准融合和多药物兼容由南盺完成。软件可能在说明、文件或代码中标注 Journey、HRT-Recorder-online、Transmtf-HRT-Tracker、HRT-Recorder-PKcomponent-Test、hrt.mahiro.uk、MtF-wiki 等参考来源。除非另有说明，参考来源不作为本 Android App 的用户界面作者署名，也不代表其作者对本软件提供医疗、法律、安全或商业背书。",
                "The native Android implementation, offline packaging, UI, import/export, share images, calibration integration and multi-medication compatibility of HRT Recorder are completed by Nanxin. Documentation, files or code may reference Journey, HRT-Recorder-online, Transmtf-HRT-Tracker, HRT-Recorder-PKcomponent-Test, hrt.mahiro.uk, MtF-wiki and other sources. Unless stated otherwise, references are not user-visible authorship credits for this Android app and do not imply medical, legal, safety or commercial endorsement by their authors.",
            ),
        ),
        LegalSection(
            language.t("十二、无账号、不删号与服务边界", "12. No Account, No Account Deletion, Service Boundary"),
            language.t(
                "本软件的核心记录能力设计为本地离线功能，不依赖开发者服务器账号，因此开发者不会通过账号删除、封禁、服务端风控或远程拒绝服务来剥夺你访问本机记录的能力。但应用商店、操作系统、设备厂商、第三方分享平台、浏览器、邮箱、云盘或你自行使用的其它服务仍可能受其自身规则影响；这些不属于本软件可控制范围。",
                "The app's core recording ability is designed as a local offline feature and does not depend on a developer-server account. Therefore, the developer will not remove your access to local records through account deletion, bans, server-side risk control or remote service denial. However, app stores, operating systems, device manufacturers, third-party sharing platforms, browsers, mail apps, cloud drives or other services you choose may still be governed by their own rules. These are outside this app's control.",
            ),
        ),
        LegalSection(
            language.t("十三、安全措施与限制", "13. Security Measures and Limits"),
            language.t(
                "软件尽量采用本地存储、系统文件选择器和 Android 应用沙箱降低数据暴露风险，但任何软件都无法保证绝对安全。Root 环境、恶意软件、系统漏洞、屏幕录制、截屏、剪贴板读取、备份泄露、他人接触设备或第三方应用权限都可能造成数据泄露。请为设备设置锁屏密码，谨慎安装应用，谨慎分享文件，并按需自行使用系统级或第三方加密方案保护导出文件。",
                "The app uses local storage, system file pickers and Android app sandboxing to reduce exposure, but no software can guarantee absolute security. Root environments, malware, system vulnerabilities, screen recording, screenshots, clipboard reading, leaked backups, physical access to the device or third-party app permissions may expose data. Use a screen lock, install apps carefully, share files carefully, and use system-level or third-party encryption for exported files when needed.",
            ),
        ),
        LegalSection(
            language.t("十四、协议更新", "14. Updates to This Agreement"),
            language.t(
                "随着功能、参考来源、导出方式或系统能力变化，本协议可能更新。若发生重要变化，软件可能在更新报告、设置页或首次启动提示中说明。继续使用更新后的软件，即表示你理解并接受更新后的协议。若你不同意更新内容，可以停止使用并删除软件；删除前请自行导出需要保留的数据。",
                "This agreement may be updated as features, references, export methods or system capabilities change. Important changes may be described in update reports, settings or startup prompts. Continuing to use the updated app means you understand and accept the updated agreement. If you disagree, stop using and delete the app. Export any data you need before deletion.",
            ),
        ),
        LegalSection(
            language.t("十五、联系方式", "15. Contact"),
            language.t(
                "如需反馈隐私、用户协议、数据处理、软件缺陷或参考署名问题，可联系：wangyanluo233@gmail.com。请不要在反馈中发送不必要的身份证件、完整病历、住址、手机号、账号密码或其它高度敏感信息。",
                "For privacy, terms, data processing, bug or reference-credit issues, contact: wangyanluo233@gmail.com. Do not send unnecessary identity documents, full medical records, address, phone number, account passwords or other highly sensitive information in feedback.",
            ),
        ),
    )

@Composable
private fun PrivacyTermsDialog(language: AppLanguage, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(language.t("用户与隐私协议", "Terms and Privacy Agreement"), color = Ink, fontWeight = FontWeight.ExtraBold) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(560.dp),
            ) {
                item {
                    Text(
                        language.t(
                            "HRT Recorder Android 版 · 作者：南盺 · 生效日期：2026-04-26",
                            "HRT Recorder Android · Author: Nanxin · Effective date: 2026-04-26",
                        ),
                        color = Muted,
                        lineHeight = 20.sp,
                    )
                }
                items(privacyTermsSections(language)) { section ->
                    Text(section.title, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    Text(section.body, color = Muted, lineHeight = 21.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(16.dp)) {
                Text(language.t("我已阅读", "I Have Read"))
            }
        },
    )
}

@Composable
private fun LegalPrivacyCard(language: AppLanguage, onOpenFullAgreement: () -> Unit) {
    NativeCard {
        Text(language.t("用户与隐私声明", "User and Privacy Statement"), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(8.dp))
        Text(
            language.t(
                "摘要：本软件默认不申请网络权限，不接入登录、统计、广告、云同步或远程配置；用药、化验、体重和围度等数据保存在本机。",
                "Summary: the app requests no network permission by default and has no login, analytics, ads, cloud sync or remote config. Medication, labs, weight and measurements are stored on this device.",
            ),
            color = Muted,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            language.t(
                "本软件仅用于个人记录、趋势估算、数据整理和离线参考，不提供医疗诊断、治疗方案、处方建议、剂量建议或用药安全承诺。",
                "This app is only for personal records, trend estimates, data organization and offline reference. It does not provide diagnosis, treatment plans, prescriptions, dosage advice or medication safety guarantees.",
            ),
            color = Muted,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            language.t(
                "导出、分享、截图、发送到其它应用后的数据由你自行管理；继续使用前请阅读完整用户与隐私协议。",
                "Exported, shared, screenshotted or forwarded data is managed by you. Please read the full Terms and Privacy Agreement before continuing.",
            ),
            color = Muted,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(6.dp))
        Button(onClick = onOpenFullAgreement, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Text(language.t("查看完整用户与隐私协议", "View Full Terms and Privacy Agreement"))
        }
    }
}

data class CupCalculatorResult(
    val band: Int,
    val cup: String,
    val displaySize: String,
    val diffCm: Double,
    val underbustCm: Double,
    val bustCm: Double,
    val note: String,
)

@Composable
private fun CupCalculatorScreen(
    language: AppLanguage,
    onShare: (CupCalculatorResult) -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 120.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            SectionTitle(language.t("罩杯", "Cup"), language.t("离线尺码参考", "Offline sizing reference"))
        }
        item {
            CupCalculatorCard(language = language, onShare = onShare)
        }
        item {
            NativeCard {
                Text(language.t("使用说明", "How to Use"), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
                Spacer(Modifier.height(8.dp))
                Text(language.t("下胸围取平静与吐气两次测量的平均值；胸围取站立、45° 前倾、90° 弯腰三次测量的平均值。结果适合做选购参考，最终仍以品牌尺码表和试穿舒适度为准。", "Use the average of relaxed and exhaled underbust. Use the average of standing, 45-degree leaning and 90-degree bending bust. Results are shopping references; brand charts and fit comfort still matter most."), color = Muted, lineHeight = 21.sp)
                Spacer(Modifier.height(8.dp))
                Text(language.t("本模块完全离线运行，不联网、不上传、不收集围度数据。", "This module runs fully offline. It does not connect, upload, or collect measurements."), color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun CupCalculatorCard(
    language: AppLanguage,
    onShare: (CupCalculatorResult) -> Unit,
) {
    var relaxedUnder by remember { mutableStateOf("") }
    var exhaleUnder by remember { mutableStateOf("") }
    var standingBust by remember { mutableStateOf("") }
    var leaningBust by remember { mutableStateOf("") }
    var bendingBust by remember { mutableStateOf("") }
    val result = calculateCupSize(
        relaxedUnder.toDoubleOrNull(),
        exhaleUnder.toDoubleOrNull(),
        standingBust.toDoubleOrNull(),
        leaningBust.toDoubleOrNull(),
        bendingBust.toDoubleOrNull(),
        language,
    )

    NativeCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(language.t("离线工具", "Offline Tool"), color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(language.t("罩杯计算器", "Cup Calculator"), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
            }
            Box(
                Modifier
                    .size(48.dp)
                    .background(MistBlue, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("cm", color = E2Pink, fontWeight = FontWeight.ExtraBold)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(language.t("输入 5 个围度，结果仅用于内衣尺码参考；所有计算都在本机完成。", "Enter 5 measurements. Results are only bra sizing references, and all calculation happens locally."), color = Muted, lineHeight = 20.sp)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            CupInputField(language.t("平静下胸围", "Relaxed underbust"), relaxedUnder, { relaxedUnder = it }, Modifier.weight(1f))
            CupInputField(language.t("吐气下胸围", "Exhaled underbust"), exhaleUnder, { exhaleUnder = it }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        CupInputField(language.t("站立胸围：经乳点最丰满处", "Standing bust: fullest point"), standingBust, { standingBust = it }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            CupInputField(language.t("45° 前倾", "45° leaning"), leaningBust, { leaningBust = it }, Modifier.weight(1f))
            CupInputField(language.t("90° 弯腰", "90° bending"), bendingBust, { bendingBust = it }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        if (result != null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(26.dp))
                    .padding(18.dp),
            ) {
                Column {
                    Text(language.t("估算尺码", "Estimated Size"), color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.Bottom) {
                        GradientCupText(result.displaySize)
                        Spacer(Modifier.width(8.dp))
                        Text(language.t("参考", "reference"), color = Muted, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    Text(
                        language.t(
                            "下胸围 ${formatNumber(result.underbustCm, 1)} cm · 平均胸围 ${formatNumber(result.bustCm, 1)} cm · 胸围差 ${formatNumber(result.diffCm, 1)} cm",
                            "Underbust ${formatNumber(result.underbustCm, 1)} cm · Avg bust ${formatNumber(result.bustCm, 1)} cm · Difference ${formatNumber(result.diffCm, 1)} cm",
                        ),
                        color = Ink,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    )
                    if (result.note.isNotBlank()) {
                        Text(cupNote(result, language), color = Muted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { onShare(result) },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(language.t("\u5206\u4eab\u56fe\u7247", "Share Image"))
                    }
                }
            }
        } else {
            Text(language.t("填完整且大于 0 的厘米数后自动计算。", "Enter complete positive centimeter values to calculate automatically."), color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun CupInputField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { next ->
            onValueChange(next.filter { it.isDigit() || it == '.' }.take(6))
        },
        label = { Text(label, fontSize = 12.sp) },
        suffix = { Text("cm") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

@Composable
private fun GradientCupText(text: String) {
    Text(
        text = text,
        fontSize = 42.sp,
        lineHeight = 46.sp,
        fontWeight = FontWeight.ExtraBold,
        color = E2Pink,
    )
}

private fun calculateCupSize(
    relaxedUnder: Double?,
    exhaleUnder: Double?,
    standingBust: Double?,
    leaningBust: Double?,
    bendingBust: Double?,
    language: AppLanguage,
): CupCalculatorResult? {
    val rawValues = listOf(relaxedUnder, exhaleUnder, standingBust, leaningBust, bendingBust)
    if (rawValues.any { value ->
            value == null || !value.isFinite() || value <= 0.0
        }
    ) return null
    val underbust = (relaxedUnder!! + exhaleUnder!!) / 2.0
    val bust = (standingBust!! + leaningBust!! + bendingBust!!) / 3.0
    val diff = bust - underbust
    val band = (ceil(underbust / 5.0) * 5.0).toInt()
    val cup = when {
        diff < 5.0 -> ""
        diff <= 7.5 -> "AA"
        diff <= 10.0 -> "A"
        diff <= 12.5 -> "B"
        diff <= 15.0 -> "C"
        diff <= 17.5 -> "D"
        diff <= 20.0 -> "E"
        else -> "E+"
    }
    val displaySize = if (cup.isBlank()) language.t("未达 AA", "Below AA") else "$band$cup"
    val note = when {
        diff < 5.0 -> "胸围差较小，可能更适合小背心或无钢圈款式。"
        diff > 20.0 -> "胸围差超出当前预设区间，建议结合试穿与品牌尺码表判断。"
        else -> "不同品牌版型差异很大，最终仍建议以试穿舒适度为准。"
    }
    return CupCalculatorResult(band, cup, displaySize, diff, underbust, bust, note)
}

@Composable
private fun LabEditorDialog(
    initial: LabResult,
    pickDateTime: (Double, (Double) -> Unit) -> Unit,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (LabResult) -> Unit,
) {
    var valueText by remember { mutableStateOf(if (initial.concValue > 0.0) formatNumber(initial.concValue, 1) else "") }
    var unit by remember { mutableStateOf(initial.unit) }
    var labTimeH by remember { mutableDoubleStateOf(initial.timeH) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(language.t("新增化验", "Add Lab Result")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = valueText,
                    onValueChange = { valueText = it },
                    label = { Text(language.t("雌二醇数值", "Estradiol value")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = { unit = "pg/ml" }, label = { Text("pg/mL") })
                    AssistChip(onClick = { unit = "pmol/l" }, label = { Text("pmol/L") })
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(formatWallTime(labTimeH), color = Ink, modifier = Modifier.weight(1f))
                    OutlinedButton(
                        onClick = { pickDateTime(labTimeH) { labTimeH = it } },
                        shape = RoundedCornerShape(16.dp),
                    ) { Text(language.t("选择时间", "Pick Time")) }
                }
            }
        },
        confirmButton = {
            val parsedValue = valueText.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }
            Button(
                onClick = {
                    onSave(initial.copy(concValue = parsedValue ?: return@Button, unit = unit, timeH = labTimeH))
                },
                enabled = parsedValue != null,
            ) { Text(language.t("保存", "Save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(language.t("取消", "Cancel")) } },
    )
}
