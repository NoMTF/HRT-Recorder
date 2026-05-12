@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.nanxin.hrtrecorder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/*
 * Android author watermark: Nanxin.
 * Route-first plan editor: choose how the medicine is taken, then show only
 * medicines that support that route.
 */

@Composable
fun PlanEditorDialog(
    initial: MedicationPlan,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (MedicationPlan) -> Unit,
) {
    val initialRoute = remember(initial) {
        initial.route.takeIf { it in planRouteOptionsV2() && medicationChoicesForRoute(it).isNotEmpty() } ?: Route.Oral
    }
    var route by remember(initial) { mutableStateOf(initialRoute) }
    var choice by remember(initial) {
        val restored = if (initial.category == MedicationCategory.AntiAndrogen) {
            MedicationChoice(
                category = MedicationCategory.AntiAndrogen,
                compound = Compound.E2,
                recordOnlyMedication = initial.recordOnlyMedication ?: RecordOnlyMedication.Spironolactone,
            )
        } else {
            MedicationChoice(initial.category, initial.compound)
        }
        mutableStateOf(restored.takeIf { it in medicationChoicesForRoute(initialRoute) } ?: defaultMedicationChoice(initialRoute))
    }
    var labelText by remember(initial) { mutableStateOf(initial.label) }
    var groupText by remember(initial) { mutableStateOf(initial.groupName) }
    var doseText by remember(initial) { mutableStateOf(if (initial.doseMG > 0.0) formatNumber(planDoseInputMGV2(initial), 3) else "1") }
    var timeText by remember(initial) { mutableStateOf(initial.normalizedTimes().joinToString(", ") { formatPlanTime(it) }) }
    var repeat by remember(initial) { mutableStateOf(initial.repeat) }
    var enabled by remember(initial) { mutableStateOf(initial.enabled) }
    var reminderEnabled by remember(initial) { mutableStateOf(initial.reminderEnabled) }
    var systemSyncEnabled by remember(initial) { mutableStateOf(initial.systemSyncEnabled) }

    val choices = remember(route) { medicationChoicesForRoute(route) }
    val rawDose = doseText.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }
    val times = parsePlanTimesInput(timeText)
    val accent = when (choice.category) {
        MedicationCategory.Estradiol -> E2Pink
        MedicationCategory.Cpa, MedicationCategory.AntiAndrogen -> CpaRose
        MedicationCategory.Testosterone -> TBlue
    }

    fun save() {
        val dose = rawDose ?: return
        val minutes = times ?: return
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
                timeMinutes = minutes.first(),
                timeMinutesList = minutes,
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
                            language.t("添加计划", "Medication Plan"),
                            color = Ink,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.weight(1.22f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        GradientActionPill(
                            language.t("保存", "Save"),
                            onClick = { save() },
                            enabled = rawDose != null && times != null,
                            modifier = Modifier.weight(0.86f),
                            accent = accent,
                        )
                    }
                }
                item {
                    NativeCard {
                        Text(language.t("先选给药方式", "Choose route first"), color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            language.t("这样只会出现真的能用这种方式记录或计算的药物。", "Only medicines compatible with this route will be shown."),
                            color = Muted,
                            lineHeight = 19.sp,
                        )
                        Spacer(Modifier.height(12.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            planRouteOptionsV2().forEach { item ->
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
                                subtitle = item.category.label(language),
                                accent = itemAccent,
                                trailing = {
                                    Text(
                                        if (item == choice) "✓" else "+",
                                        color = if (item == choice) itemAccent else TBlue,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                    )
                                },
                                onClick = { choice = item },
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
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
                            placeholder = { Text(language.t("例如：睡前抗雄", "e.g. bedtime antiandrogen")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = timeText,
                            onValueChange = { timeText = it },
                            label = { Text(language.t("时间点", "Times")) },
                            placeholder = { Text("09:00, 21:00") },
                            isError = times == null,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(language.t("多个时间点用逗号分隔。", "Separate multiple times with commas."), color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
                        Spacer(Modifier.height(10.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            PlanRepeat.entries.forEach { item ->
                                if (item == repeat) {
                                    GradientActionPill(item.labelForPlanEditor(language), onClick = {}, accent = accent)
                                } else {
                                    SecondaryGlassPill(item.labelForPlanEditor(language), onClick = { repeat = item }, accent = Muted)
                                }
                            }
                        }
                    }
                }
                item {
                    NativeCard {
                        Text(language.t("剂量与提醒", "Dose and reminders"), color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
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
                        Spacer(Modifier.height(10.dp))
                        PlanToggleRow(checked = enabled, onChange = { enabled = it }, text = language.t("启用计划", "Enable plan"))
                        PlanToggleRow(checked = reminderEnabled, onChange = { reminderEnabled = it }, text = language.t("App 本地提醒", "App local reminder"))
                        PlanToggleRow(checked = systemSyncEnabled, onChange = { systemSyncEnabled = it }, text = language.t("保存后同步到系统日历", "Sync to system calendar after save"))
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanToggleRow(checked: Boolean, onChange: (Boolean) -> Unit, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(text, color = Ink, fontWeight = FontWeight.Bold)
    }
}

private fun planRouteOptionsV2(): List<Route> =
    listOf(Route.Oral, Route.Injection, Route.PatchApply, Route.Gel, Route.Sublingual)
        .filter { medicationChoicesForRoute(it).isNotEmpty() }

private fun planDoseInputMGV2(plan: MedicationPlan): Double =
    when (plan.category) {
        MedicationCategory.Estradiol -> plan.doseMG / toE2Factor(plan.compound).coerceAtLeast(1e-9)
        MedicationCategory.Testosterone -> plan.doseMG / toActiveFactor(plan.compound).coerceAtLeast(1e-9)
        MedicationCategory.Cpa,
        MedicationCategory.AntiAndrogen -> plan.doseMG
    }.takeIf { it.isFinite() && it > 0.0 } ?: plan.doseMG.coerceAtLeast(0.0)

private fun parsePlanTimesInput(value: String): List<Int>? {
    val parts = value
        .split(",", "，", ";", "；", "|", " ")
        .map { it.trim() }
        .filter { it.isNotBlank() }
    if (parts.isEmpty()) return null
    val values = parts.map { parseSinglePlanTime(it) ?: return null }
    return values.distinct().sorted().ifEmpty { null }
}

private fun parseSinglePlanTime(value: String): Int? {
    val parts = value.trim().split(":", "：")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

private fun PlanRepeat.labelForPlanEditor(language: AppLanguage): String =
    language.t(
        labelZh,
        when (this) {
            PlanRepeat.Daily -> "Daily"
            PlanRepeat.Weekly -> "Weekly"
        },
    )
