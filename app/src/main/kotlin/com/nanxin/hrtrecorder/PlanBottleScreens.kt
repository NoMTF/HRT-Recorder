@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.nanxin.hrtrecorder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/*
 * Android author watermark: 南盺
 * Plan and bottle UI is split from MainActivity to keep the original app shell
 * lighter while adding supply tracking and daily reminder workflows.
 */

@Composable
fun PlansScreen(
    plans: List<MedicationPlan>,
    language: AppLanguage,
    onCreatePlan: () -> Unit,
    onCreateReminder: () -> Unit,
    onAddDose: (MedicationPlan) -> Unit,
    onAddReminder: (MedicationPlan) -> Unit,
    onToggle: (MedicationPlan) -> Unit,
    onDelete: (MedicationPlan) -> Unit,
) {
    val sorted = remember(plans) { plans.sortedBy { it.timeMinutes } }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 128.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            SectionTitle(language.t("用药计划", "Dose Plans"), language.t("每日时间、快捷记录、系统提醒", "Daily time, quick logging, system reminders"))
        }
        item {
            NativeCard {
                Text(language.t("计划与系统提醒", "Plans and reminders"), color = Ink, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    language.t(
                        "提醒入口集中放在计划页，避免在新增用药时误触。你可以在这里一键记录计划，也可以把计划写入 Android 日历/提醒应用。",
                        "Reminders live on the Plan page to avoid accidental taps while saving a dose. From here, you can quick-log a plan or send it to Android Calendar/Reminders.",
                    ),
                    color = Muted,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onCreatePlan,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                    ) {
                        Text(language.t("创建计划", "Create plan"), fontWeight = FontWeight.ExtraBold)
                    }
                    OutlinedButton(
                        onClick = onCreateReminder,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                    ) {
                        Text(language.t("添加提醒", "Add reminder"), fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
        if (sorted.isEmpty()) {
            item {
                EmptyGlassState(
                    title = language.t("还没有计划", "No plans yet"),
                    body = language.t("创建计划后，就能在这里一键记录或加入系统提醒。", "Create a plan, then quick-log it or add a system reminder here."),
                )
            }
        } else {
            items(sorted, key = { it.id }) { plan ->
                NativeCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .background(MistBlue, RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(formatPlanTime(plan.timeMinutes), color = Ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(plan.displayName(language), color = Ink, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${plan.route.label(language)} · ${formatNumber(plan.doseMG, 3)} mg", color = Muted, fontSize = 13.sp)
                            Text(if (plan.enabled) language.t("已启用", "Enabled") else language.t("已暂停", "Paused"), color = if (plan.enabled) E2Pink else Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Checkbox(checked = plan.enabled, onCheckedChange = { onToggle(plan) })
                    }
                    Spacer(Modifier.height(12.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onAddDose(plan) }, shape = RoundedCornerShape(16.dp)) {
                            Text(language.t("一键记录", "Log"))
                        }
                        OutlinedButton(onClick = { onAddReminder(plan) }, shape = RoundedCornerShape(16.dp)) {
                            Text(language.t("加入提醒", "Reminder"))
                        }
                        TextButton(onClick = { onDelete(plan) }) {
                            Text(language.t("删除", "Delete"), color = androidx.compose.ui.graphics.Color(0xFFDC2626))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegacyPlanEditorDialog(
    initial: MedicationPlan,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (MedicationPlan) -> Unit,
) {
    val initialRoute = remember(initial) {
        initial.route.takeIf { it in planRouteOptions() && medicationChoicesForRoute(it).isNotEmpty() } ?: Route.Oral
    }
    var route by remember(initial) { mutableStateOf(initialRoute) }
    var choice by remember(initial) {
        val restored = if (initial.category == MedicationCategory.AntiAndrogen) {
            MedicationChoice(MedicationCategory.AntiAndrogen, Compound.E2, initial.recordOnlyMedication ?: RecordOnlyMedication.Spironolactone)
        } else {
            MedicationChoice(initial.category, initial.compound)
        }
        mutableStateOf(restored.takeIf { it in medicationChoicesForRoute(initialRoute) } ?: defaultMedicationChoice(initialRoute))
    }
    var labelText by remember(initial) { mutableStateOf(initial.label) }
    var groupText by remember(initial) { mutableStateOf(initial.groupName) }
    var doseText by remember(initial) { mutableStateOf(if (initial.doseMG > 0.0) formatNumber(planDoseInputMG(initial), 3) else "1") }
    var timeText by remember(initial) { mutableStateOf(initial.normalizedTimes().joinToString(", ") { formatPlanTime(it) }) }
    var repeat by remember(initial) { mutableStateOf(initial.repeat) }
    var reminderEnabled by remember(initial) { mutableStateOf(initial.reminderEnabled) }
    var systemSyncEnabled by remember(initial) { mutableStateOf(initial.systemSyncEnabled) }
    var enabled by remember(initial) { mutableStateOf(initial.enabled) }
    val choices = remember(route) { medicationChoicesForRoute(route) }
    val rawDose = doseText.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }
    val minutes = parsePlanTimeMinutesList(timeText)
    val accent = when (choice.category) {
        MedicationCategory.Estradiol -> E2Pink
        MedicationCategory.Cpa, MedicationCategory.AntiAndrogen -> CpaRose
        MedicationCategory.Testosterone -> TBlue
    }

    fun savePlan() {
        val dose = rawDose ?: return
        val planMinutes = minutes ?: return
        val primaryMinute = planMinutes.first()
        val category = choice.category
        val compound = if (category == MedicationCategory.AntiAndrogen) Compound.E2 else choice.compound
        val activeDose = when (category) {
            MedicationCategory.Estradiol -> dose * toE2Factor(compound)
            MedicationCategory.Testosterone -> dose * toActiveFactor(compound)
            MedicationCategory.Cpa,
            MedicationCategory.AntiAndrogen -> dose
        }
        onSave(
            initial.copy(
                label = labelText.trim(),
                groupName = groupText.trim(),
                category = category,
                route = route,
                compound = compound,
                recordOnlyMedication = if (category == MedicationCategory.AntiAndrogen) choice.recordOnlyMedication else null,
                doseMG = activeDose,
                timeMinutes = primaryMinute,
                timeMinutesList = planMinutes,
                repeat = repeat,
                reminderEnabled = reminderEnabled,
                systemSyncEnabled = systemSyncEnabled,
                enabled = enabled,
            ),
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppGradient)
                .padding(horizontal = 20.dp),
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 32.dp, bottom = 36.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SecondaryGlassPill(language.t("取消", "Cancel"), onClick = onDismiss, modifier = Modifier.weight(0.86f), accent = Ink)
                        Text(
                            language.t("创建计划", "Create plan"),
                            color = Ink,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.weight(1.22f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        GradientActionPill(
                            language.t("保存", "Save"),
                            onClick = { savePlan() },
                            enabled = rawDose != null && minutes != null,
                            modifier = Modifier.weight(0.86f),
                            accent = accent,
                        )
                    }
                }
                item {
                    NativeCard {
                        Text(language.t("计划信息", "Plan details"), color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = labelText,
                            onValueChange = { labelText = it },
                            label = { Text(language.t("名称（可选）", "Name (optional)")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = groupText,
                            onValueChange = { groupText = it },
                            label = { Text(language.t("方案分组（可选）", "Routine group (optional)")) },
                            placeholder = { Text(language.t("例如：睡前抗雄 / 每周针剂", "e.g. bedtime antiandrogen / weekly injection")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = timeText,
                            onValueChange = { timeText = it },
                            label = { Text(language.t("每天时间", "Daily time")) },
                            placeholder = { Text("21:00") },
                            singleLine = true,
                            isError = minutes == null,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Checkbox(checked = enabled, onCheckedChange = { enabled = it })
                            Text(language.t("启用计划", "Enable plan"), color = Ink, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                item {
                    NativeCard {
                        Text(language.t("给药方式", "Route"), color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        Text(language.t("先选方式，再选可用于该方式的药物。", "Pick the route first, then choose a compatible medication."), color = Muted, lineHeight = 19.sp)
                        Spacer(Modifier.height(12.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            planRouteOptions().forEach { item ->
                                if (item == route) {
                                    GradientActionPill(item.label(language), onClick = {}, accent = accent)
                                } else {
                                    SecondaryGlassPill(
                                        item.label(language),
                                        onClick = {
                                            route = item
                                            choice = defaultMedicationChoice(item)
                                        },
                                        accent = Muted,
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    NativeCard {
                        Text(language.t("药物", "Medication"), color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(10.dp))
                        choices.forEach { item ->
                            val itemAccent = when (item.category) {
                                MedicationCategory.Estradiol -> E2Pink
                                MedicationCategory.Cpa, MedicationCategory.AntiAndrogen -> CpaRose
                                MedicationCategory.Testosterone -> TBlue
                            }
                            SoftListRow(
                                icon = item.shortCode(language),
                                title = item.label(language),
                                subtitle = route.label(language),
                                accent = itemAccent,
                                trailing = { Text(if (item == choice) "✓" else "+", color = if (item == choice) itemAccent else TBlue, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold) },
                                onClick = { choice = item },
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                    }
                }
                item {
                    NativeCard {
                        Text(language.t("剂量", "Dose"), color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = doseText,
                            onValueChange = { doseText = it },
                            label = { Text(language.t("每次剂量", "Dose per time")) },
                            suffix = { Text("mg") },
                            isError = rawDose == null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlanReminderPickerDialog(
    plans: List<MedicationPlan>,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onPick: (MedicationPlan) -> Unit,
) {
    val sorted = remember(plans) { plans.sortedBy { it.timeMinutes } }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppGradient)
                .padding(horizontal = 20.dp),
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 32.dp, bottom = 36.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SecondaryGlassPill(language.t("取消", "Cancel"), onClick = onDismiss, modifier = Modifier.weight(0.75f), accent = Ink)
                        Text(language.t("添加提醒", "Add reminder"), color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1.4f))
                    }
                }
                items(sorted, key = { it.id }) { plan ->
                    SoftListRow(
                        icon = formatPlanTime(plan.timeMinutes),
                        title = plan.displayName(language),
                        subtitle = "${plan.route.label(language)} · ${formatNumber(plan.doseMG, 3)} mg",
                        accent = E2Pink,
                        trailing = { Text("+", color = TBlue, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold) },
                        onClick = { onPick(plan) },
                    )
                }
            }
        }
    }
}

private fun planRouteOptions(): List<Route> =
    listOf(Route.Oral, Route.Injection, Route.PatchApply, Route.Gel, Route.Sublingual)
        .filter { medicationChoicesForRoute(it).isNotEmpty() }

private fun planDoseInputMG(plan: MedicationPlan): Double =
    when (plan.category) {
        MedicationCategory.Estradiol -> plan.doseMG / toE2Factor(plan.compound).coerceAtLeast(1e-9)
        MedicationCategory.Testosterone -> plan.doseMG / toActiveFactor(plan.compound).coerceAtLeast(1e-9)
        MedicationCategory.Cpa,
        MedicationCategory.AntiAndrogen -> plan.doseMG
    }.takeIf { it.isFinite() && it > 0.0 } ?: plan.doseMG.coerceAtLeast(0.0)

private fun parsePlanTimeMinutes(value: String): Int? {
    val parts = value.trim().split(":", "：")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

private fun parsePlanTimeMinutesList(value: String): List<Int>? {
    val parts = value.split(",", "，", ";", "；", "|", " ")
        .map { it.trim() }
        .filter { it.isNotBlank() }
    if (parts.isEmpty()) return null
    return parts.map { parsePlanTimeMinutes(it) ?: return null }.distinct().sorted()
}

private fun PlanRepeat.editorLabel(language: AppLanguage): String =
    language.t(
        labelZh,
        when (this) {
            PlanRepeat.Daily -> "Daily"
            PlanRepeat.Weekly -> "Weekly"
        },
    )

@Composable
fun BottlesScreen(
    bottles: List<PillBottle>,
    language: AppLanguage,
    onAdd: () -> Unit,
    onEdit: (PillBottle) -> Unit,
    onRefill: (PillBottle) -> Unit,
    onDelete: (PillBottle) -> Unit,
) {
    val sorted = remember(bottles) { bottles.sortedByDescending { it.createdTimeH } }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 128.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            SectionTitle(language.t("药瓶", "Bottles"), language.t("库存、保质期、自动扣减", "Supply, expiry, automatic deduction"))
        }
        item {
            NativeCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        Modifier
                            .size(58.dp)
                            .background(MistBlue, RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("▣", color = TBlue, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(language.t("药瓶优先扣减", "Bottle-first deduction"), color = Ink, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Text(language.t("新增记录时间晚于药瓶创建时间，且药物匹配时，会优先扣除对应药瓶库存。", "When a new dose is after bottle creation and medication matches, the app deducts that bottle first."), color = Muted, lineHeight = 19.sp)
                    }
                    Button(onClick = onAdd, shape = RoundedCornerShape(18.dp)) { Text("+") }
                }
            }
        }
        if (sorted.isEmpty()) {
            item {
                EmptyGlassState(
                    title = language.t("还没有药瓶", "No bottles yet"),
                    body = language.t("创建药瓶后，新增匹配用药会自动扣除库存。", "Create a bottle, then matching new doses will deduct supply automatically."),
                )
            }
        } else {
            items(sorted, key = { it.id }) { bottle ->
                BottleCard(bottle, language, onEdit = { onEdit(bottle) }, onRefill = { onRefill(bottle) }, onDelete = { onDelete(bottle) })
            }
        }
    }
}

@Composable
private fun BottleCard(
    bottle: PillBottle,
    language: AppLanguage,
    onEdit: () -> Unit,
    onRefill: () -> Unit,
    onDelete: () -> Unit,
) {
    val ratio = if (bottle.totalUnits > 0.0) (bottle.remainingUnits / bottle.totalUnits).coerceIn(0.0, 1.0) else 0.0
    NativeCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier
                    .size(62.dp)
                    .background(MistPink, RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("💊", fontSize = 24.sp)
            }
            Column(Modifier.weight(1f)) {
                Text(bottle.displayName(language), color = Ink, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${formatNumber(bottle.remainingUnits, 2)} / ${formatNumber(bottle.totalUnits, 2)} ${displayUnit(bottle.unit, language)}", color = Muted, fontSize = 13.sp)
                Text(language.t("创建：", "Created: ") + formatWallTime(bottle.createdTimeH), color = Muted, fontSize = 11.sp)
                Text(
                    bottle.expiresTimeH?.let { language.t("保质期：", "Expires: ") + formatWallTime(it) } ?: language.t("无保质期", "No expiry"),
                    color = Muted,
                    fontSize = 11.sp,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(14.dp)
                .background(LocalAppPalette.current.subtleSurface, RoundedCornerShape(99.dp)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(ratio.toFloat())
                    .height(14.dp)
                    .background(E2Pink, RoundedCornerShape(99.dp)),
            )
        }
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onRefill, shape = RoundedCornerShape(16.dp)) { Text(language.t("补满", "Fill")) }
            OutlinedButton(onClick = onEdit, shape = RoundedCornerShape(16.dp)) { Text(language.t("编辑", "Edit")) }
            TextButton(onClick = onDelete) { Text(language.t("删除", "Delete"), color = androidx.compose.ui.graphics.Color(0xFFDC2626)) }
        }
    }
}

@Composable
fun BottleEditorDialog(
    initial: PillBottle,
    language: AppLanguage,
    pickDateTime: (Double, (Double) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSave: (PillBottle) -> Unit,
) {
    var category by remember(initial) { mutableStateOf(initial.category) }
    var choice by remember(initial) {
        val restored = if (initial.category == MedicationCategory.AntiAndrogen) {
            MedicationChoice(MedicationCategory.AntiAndrogen, Compound.E2, initial.recordOnlyMedication)
        } else {
            MedicationChoice(initial.category, initial.compound)
        }
        mutableStateOf(restored)
    }
    var nameText by remember(initial) { mutableStateOf(initial.name) }
    var unitText by remember(initial, language) { mutableStateOf(displayUnit(initial.unit, language)) }
    var totalText by remember(initial) { mutableStateOf(if (initial.totalUnits > 0.0) formatNumber(initial.totalUnits, 2) else "") }
    var remainingText by remember(initial) { mutableStateOf(if (initial.remainingUnits > 0.0) formatNumber(initial.remainingUnits, 2) else "") }
    var remainingTouched by remember(initial) { mutableStateOf(initial.remainingUnits != initial.totalUnits) }
    var perDoseText by remember(initial) { mutableStateOf(if (initial.perDoseUnits > 0.0) formatNumber(initial.perDoseUnits, 2) else "1") }
    var createdTimeH by remember(initial) { mutableDoubleStateOf(initial.createdTimeH) }
    var hasExpiry by remember(initial) { mutableStateOf(initial.expiresTimeH != null) }
    var expiresTimeH by remember(initial) { mutableDoubleStateOf(initial.expiresTimeH ?: (nowEpochHours() + 365.0 * 24.0)) }
    val choices = remember(category) {
        when (category) {
            MedicationCategory.Estradiol -> allowedCompounds(MedicationCategory.Estradiol).map { MedicationChoice(MedicationCategory.Estradiol, it) }
            MedicationCategory.Cpa -> listOf(MedicationChoice(MedicationCategory.Cpa, Compound.CPA))
            MedicationCategory.Testosterone -> allowedCompounds(MedicationCategory.Testosterone).map { MedicationChoice(MedicationCategory.Testosterone, it) }
            MedicationCategory.AntiAndrogen -> RecordOnlyMedication.entries
                .filter { it != RecordOnlyMedication.CyproteroneAcetate }
                .map { MedicationChoice(MedicationCategory.AntiAndrogen, Compound.E2, it) }
        }
    }
    val total = totalText.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }
    val remaining = remainingText.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }
    val perDose = perDoseText.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(language.t("药瓶", "Bottle"), color = Ink, fontWeight = FontWeight.ExtraBold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.height(540.dp)) {
                item {
                    Text(language.t("类别", "Category"), color = Ink, fontWeight = FontWeight.Bold)
                    ChipFlow(listOf(MedicationCategory.Estradiol, MedicationCategory.Cpa, MedicationCategory.Testosterone, MedicationCategory.AntiAndrogen), category, { it.label(language) }) {
                        category = it
                        choice = when (it) {
                            MedicationCategory.Estradiol -> MedicationChoice(it, defaultCompound(it))
                            MedicationCategory.Cpa -> MedicationChoice(it, Compound.CPA)
                            MedicationCategory.Testosterone -> MedicationChoice(it, defaultCompound(it))
                            MedicationCategory.AntiAndrogen -> MedicationChoice(it, Compound.E2, RecordOnlyMedication.Spironolactone)
                        }
                    }
                }
                item {
                    Text(language.t("药物", "Medication"), color = Ink, fontWeight = FontWeight.Bold)
                    ChipFlow(choices, choice, { it.label(language) }) { choice = it }
                }
                item {
                    OutlinedTextField(value = nameText, onValueChange = { nameText = it }, label = { Text(language.t("自定义名称（可空）", "Custom name (optional)")) }, modifier = Modifier.fillMaxWidth())
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = totalText, onValueChange = { totalText = it; if (remainingText.isBlank()) remainingText = it }, label = { Text(language.t("总量", "Total")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = unitText, onValueChange = { unitText = it.take(6) }, label = { Text(language.t("单位", "Unit")) }, modifier = Modifier.weight(1f))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = remainingText, onValueChange = { remainingText = it }, label = { Text(language.t("剩余", "Remaining")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = perDoseText, onValueChange = { perDoseText = it }, label = { Text(language.t("每次扣除", "Per dose")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                    }
                }
                item {
            HorizontalDivider(color = LocalAppPalette.current.divider.copy(alpha = 0.16f))
                    Spacer(Modifier.height(8.dp))
                    Text(language.t("创建时间", "Created time"), color = Ink, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(formatWallTime(createdTimeH), color = Muted, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = { pickDateTime(createdTimeH) { createdTimeH = it } }, shape = RoundedCornerShape(16.dp)) {
                            Text(language.t("选择", "Pick"))
                        }
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = hasExpiry, onCheckedChange = { hasExpiry = it })
                        Text(language.t("有保质期", "Has expiry"), color = Ink, fontWeight = FontWeight.Bold)
                    }
                    if (hasExpiry) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(formatWallTime(expiresTimeH), color = Muted, modifier = Modifier.weight(1f))
                            OutlinedButton(onClick = { pickDateTime(expiresTimeH) { expiresTimeH = it } }, shape = RoundedCornerShape(16.dp)) {
                                Text(language.t("选择", "Pick"))
                            }
                        }
                    } else {
                        AssistChip(onClick = { hasExpiry = false }, label = { Text(language.t("无保质期", "No expiry")) })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = total != null && remaining != null && perDose != null,
                onClick = {
                    val selectedCategory = if (choice.category == MedicationCategory.Cpa) MedicationCategory.Cpa else category
                    onSave(
                        initial.copy(
                            name = nameText.trim(),
                            category = selectedCategory,
                            compound = if (choice.category == MedicationCategory.AntiAndrogen) Compound.E2 else choice.compound,
                            recordOnlyMedication = if (choice.category == MedicationCategory.AntiAndrogen) choice.recordOnlyMedication else null,
                            unit = unitText.ifBlank { language.t("片", "pcs") },
                            totalUnits = total ?: return@Button,
                            remainingUnits = (remaining ?: return@Button).coerceAtMost(total ?: 0.0),
                            perDoseUnits = perDose ?: return@Button,
                            createdTimeH = createdTimeH,
                            expiresTimeH = if (hasExpiry) expiresTimeH else null,
                        ),
                    )
                },
                shape = RoundedCornerShape(16.dp),
            ) { Text(language.t("保存", "Save")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(language.t("取消", "Cancel")) }
        },
    )
}

@Composable
private fun EmptyGlassState(title: String, body: String) {
    NativeCard {
        Text(title, color = Ink, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Text(body, color = Muted, lineHeight = 20.sp, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
fun BottlesScreenV2(
    bottles: List<PillBottle>,
    language: AppLanguage,
    onAdd: () -> Unit,
    onEdit: (PillBottle) -> Unit,
    onRefill: (PillBottle) -> Unit,
    onDelete: (PillBottle) -> Unit,
) {
    val sorted = remember(bottles) { bottles.sortedByDescending { it.createdTimeH } }
    var celebrateName by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(celebrateName) {
        if (celebrateName != null) {
            delay(1100)
            celebrateName = null
        }
    }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 132.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                LargePageTitle(
                    title = language.t("药瓶", "Bottles"),
                    subtitle = language.t("供应与库存", "Supply tracking"),
                )
            }
            if (sorted.isNotEmpty()) {
                items(sorted, key = { it.id }) { bottle ->
                    BottleCardV2(
                        bottle = bottle,
                        language = language,
                        onEdit = { onEdit(bottle) },
                        onRefill = {
                            celebrateName = bottle.displayName(language)
                            onRefill(bottle)
                        },
                        onDelete = { onDelete(bottle) },
                    )
                }
            }
            item {
                NativeCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        SoftIconBadge("▣", TBlue, sizeDp = 58)
                        Column(Modifier.weight(1f)) {
                            Text(language.t("药瓶优先扣量", "Bottle-first deduction"), color = Ink, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                            Text(language.t("新增匹配用药时，优先从创建时间之后的药瓶库存里扣除。", "Matching new doses deduct from bottles created before the dose time."), color = Muted, lineHeight = 19.sp)
                        }
                        GradientActionPill("+", onClick = onAdd, modifier = Modifier.width(62.dp), accent = TBlue)
                    }
                }
            }
            if (sorted.isEmpty()) {
                item {
                    EmptyGlassState(
                        title = language.t("还没有药瓶", "No bottles yet"),
                        body = language.t("创建药瓶后，匹配用药会自动扣除库存；也可以一键补满。", "Create a bottle, then matching doses can deduct supply and refill in one tap."),
                    )
                }
            }
        }
        if (celebrateName != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                NativeCard(Modifier.padding(horizontal = 42.dp)) {
                    Box(
                        Modifier
                            .size(74.dp)
                            .background(Color(0xFF5DD06A), CircleShape)
                            .align(Alignment.CenterHorizontally),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("✓", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Text(
                        language.t("已补满！", "Refilled!"),
                        color = Ink,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 14.dp),
                    )
                    Text(
                        celebrateName.orEmpty(),
                        color = Muted,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BottleCardV2(
    bottle: PillBottle,
    language: AppLanguage,
    onEdit: () -> Unit,
    onRefill: () -> Unit,
    onDelete: () -> Unit,
) {
    val ratio = if (bottle.totalUnits > 0.0) (bottle.remainingUnits / bottle.totalUnits).coerceIn(0.0, 1.0) else 0.0
    val remainingPercent = (ratio * 100.0).roundToInt().coerceIn(0, 100)
    val accent = when (bottle.category) {
        MedicationCategory.Estradiol -> E2Pink
        MedicationCategory.Cpa, MedicationCategory.AntiAndrogen -> CpaRose
        MedicationCategory.Testosterone -> TBlue
    }
    NativeCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            SoftIconBadge("▣", accent, sizeDp = 62)
            Column(Modifier.weight(1f)) {
                Text(bottle.displayName(language), color = Ink, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${formatNumber(bottle.remainingUnits, 2)} / ${formatNumber(bottle.totalUnits, 2)} ${displayUnit(bottle.unit, language)}", color = accent, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                Text(language.t("创建：", "Created: ") + formatWallTime(bottle.createdTimeH), color = Muted, fontSize = 11.sp)
                Text(
                    bottle.expiresTimeH?.let { language.t("保质期：", "Expires: ") + formatWallTime(it) } ?: language.t("无保质期", "No expiry"),
                    color = Muted,
                    fontSize = 11.sp,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        SupplyUnitBlocks(total = bottle.totalUnits, remaining = bottle.remainingUnits, accent = accent)
        Spacer(Modifier.height(10.dp))
        Text(language.t("剩余 ", "Remaining ") + "$remainingPercent%", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GradientActionPill(language.t("补满新药瓶", "Refill"), onClick = onRefill, accent = TBlue)
            SecondaryGlassPill(language.t("编辑", "Edit"), onClick = onEdit, accent = accent)
            TextButton(onClick = onDelete) { Text(language.t("删除", "Delete"), color = Color(0xFFDC2626)) }
        }
    }
}

@Composable
fun BottleEditorDialogV2(
    initial: PillBottle,
    language: AppLanguage,
    pickDateTime: (Double, (Double) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSave: (PillBottle) -> Unit,
) {
    var category by remember(initial) { mutableStateOf(initial.category) }
    var choice by remember(initial) {
        val restored = if (initial.category == MedicationCategory.AntiAndrogen) {
            MedicationChoice(MedicationCategory.AntiAndrogen, Compound.E2, initial.recordOnlyMedication)
        } else {
            MedicationChoice(initial.category, initial.compound)
        }
        mutableStateOf(restored)
    }
    var nameText by remember(initial) { mutableStateOf(initial.name) }
    var unitText by remember(initial, language) { mutableStateOf(displayUnit(initial.unit, language)) }
    var totalText by remember(initial) { mutableStateOf(if (initial.totalUnits > 0.0) formatNumber(initial.totalUnits, 2) else "") }
    var remainingText by remember(initial) { mutableStateOf(if (initial.remainingUnits > 0.0) formatNumber(initial.remainingUnits, 2) else "") }
    var perDoseText by remember(initial) { mutableStateOf(if (initial.perDoseUnits > 0.0) formatNumber(initial.perDoseUnits, 2) else "1") }
    var createdTimeH by remember(initial) { mutableDoubleStateOf(initial.createdTimeH) }
    var hasExpiry by remember(initial) { mutableStateOf(initial.expiresTimeH != null) }
    var expiresTimeH by remember(initial) { mutableDoubleStateOf(initial.expiresTimeH ?: (nowEpochHours() + 365.0 * 24.0)) }
    val choices = remember(category) {
        when (category) {
            MedicationCategory.Estradiol -> allowedCompounds(MedicationCategory.Estradiol).map { MedicationChoice(MedicationCategory.Estradiol, it) }
            MedicationCategory.Cpa -> listOf(MedicationChoice(MedicationCategory.Cpa, Compound.CPA))
            MedicationCategory.Testosterone -> allowedCompounds(MedicationCategory.Testosterone).map { MedicationChoice(MedicationCategory.Testosterone, it) }
            MedicationCategory.AntiAndrogen -> RecordOnlyMedication.entries
                .filter { it != RecordOnlyMedication.CyproteroneAcetate }
                .map { MedicationChoice(MedicationCategory.AntiAndrogen, Compound.E2, it) }
        }
    }
    val total = totalText.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }
    val remaining = remainingText.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }
    val perDose = perDoseText.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }
    val canSave = total != null && remaining != null && perDose != null
    val accent = when (category) {
        MedicationCategory.Estradiol -> E2Pink
        MedicationCategory.Cpa, MedicationCategory.AntiAndrogen -> CpaRose
        MedicationCategory.Testosterone -> TBlue
    }

    fun saveBottle() {
        val savedTotal = total ?: return
        val savedRemaining = remaining ?: return
        val savedPerDose = perDose ?: return
        val selectedCategory = if (choice.category == MedicationCategory.Cpa) MedicationCategory.Cpa else category
        onSave(
            initial.copy(
                name = nameText.trim(),
                category = selectedCategory,
                compound = if (choice.category == MedicationCategory.AntiAndrogen) Compound.E2 else choice.compound,
                recordOnlyMedication = if (choice.category == MedicationCategory.AntiAndrogen) choice.recordOnlyMedication else null,
                unit = unitText.ifBlank { language.t("片", "pcs") },
                totalUnits = savedTotal,
                remainingUnits = savedRemaining.coerceAtMost(savedTotal),
                perDoseUnits = savedPerDose,
                createdTimeH = createdTimeH,
                expiresTimeH = if (hasExpiry) expiresTimeH else null,
            ),
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppGradient)
                .padding(horizontal = 20.dp),
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 32.dp, bottom = 34.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SecondaryGlassPill(language.t("取消", "Cancel"), onClick = onDismiss, modifier = Modifier.weight(0.82f), accent = Ink)
                        Text(language.t("药瓶", "Bottle"), color = Ink, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), maxLines = 1)
                        GradientActionPill(
                            language.t("保存", "Save"),
                            onClick = { saveBottle() },
                            enabled = canSave,
                            modifier = Modifier.weight(0.82f),
                            accent = accent,
                        )
                    }
                }
                item {
                    NativeCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            SoftIconBadge("▣", accent, sizeDp = 64)
                            Column(Modifier.weight(1f)) {
                                Text(language.t("库存", "Supply"), color = Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                                Text(language.t("设置总量、剩余量和每次扣除量。", "Set total, remaining, and per-dose deduction."), color = Muted, lineHeight = 19.sp)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        SupplyUnitBlocks(total = total ?: 24.0, remaining = remaining ?: total ?: 24.0, accent = accent)
                    }
                }
                item {
                    NativeCard {
                        Text(language.t("类别", "Category"), color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Spacer(Modifier.height(10.dp))
                        SoftSegmentedControl(
                            items = listOf(MedicationCategory.Estradiol, MedicationCategory.Cpa, MedicationCategory.Testosterone, MedicationCategory.AntiAndrogen),
                            selected = category,
                            label = { it.label(language) },
                            onSelect = {
                                category = it
                                choice = when (it) {
                                    MedicationCategory.Estradiol -> MedicationChoice(it, defaultCompound(it))
                                    MedicationCategory.Cpa -> MedicationChoice(it, Compound.CPA)
                                    MedicationCategory.Testosterone -> MedicationChoice(it, defaultCompound(it))
                                    MedicationCategory.AntiAndrogen -> MedicationChoice(it, Compound.E2, RecordOnlyMedication.Spironolactone)
                                }
                            },
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(language.t("药物", "Medication"), color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Spacer(Modifier.height(10.dp))
                        choices.forEach { item ->
                            val selectedMedication = item == choice
                            SoftListRow(
                                icon = item.shortCode(language),
                                title = item.label(language),
                                subtitle = language.t("用于库存匹配和自动扣量", "Used for bottle matching and automatic deduction"),
                                accent = accent,
                                trailing = { Text(if (selectedMedication) "✓" else "+", color = if (selectedMedication) accent else TBlue, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold) },
                                onClick = { choice = item },
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                    }
                }
                item {
                    NativeCard {
                        Text(language.t("药物详情", "Medication Details"), color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(value = nameText, onValueChange = { nameText = it }, label = { Text(language.t("自定义名称（可空）", "Custom name (optional)")) }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = totalText,
                                onValueChange = { next ->
                                    val shouldSyncRemaining = remainingText.isBlank() || remainingText == totalText
                                    totalText = next
                                    if (shouldSyncRemaining) remainingText = next
                                },
                                label = { Text(language.t("总量", "Total")) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(value = unitText, onValueChange = { unitText = it.take(8) }, label = { Text(language.t("单位", "Unit")) }, modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(value = remainingText, onValueChange = { remainingText = it }, label = { Text(language.t("剩余", "Remaining")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                            OutlinedTextField(value = perDoseText, onValueChange = { perDoseText = it }, label = { Text(language.t("每次扣除", "Per dose")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                        }
                    }
                }
                item {
                    NativeCard {
                        Text(language.t("时间与保质期", "Time & Expiry"), color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(10.dp))
                        SoftListRow(
                            icon = "T",
                            title = language.t("创建时间", "Created time"),
                            subtitle = formatWallTime(createdTimeH),
                            accent = TBlue,
                            onClick = { pickDateTime(createdTimeH) { createdTimeH = it } },
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = hasExpiry, onCheckedChange = { hasExpiry = it })
                            Text(language.t("有保质期", "Has expiry"), color = Ink, fontWeight = FontWeight.Bold)
                        }
                        if (hasExpiry) {
                            SoftListRow(
                                icon = "E",
                                title = language.t("保质期", "Expiry"),
                                subtitle = formatWallTime(expiresTimeH),
                                accent = E2Pink,
                                onClick = { pickDateTime(expiresTimeH) { expiresTimeH = it } },
                            )
                        } else {
                            Text(language.t("已选择无保质期。", "No expiry selected."), color = Muted, fontSize = 13.sp)
                        }
                    }
                }
                item {
                    GradientActionPill(
                        language.t("保存药瓶", "Save Bottle"),
                        onClick = { saveBottle() },
                        enabled = canSave,
                        modifier = Modifier.fillMaxWidth(),
                        accent = accent,
                    )
                }
            }
        }
    }
}
