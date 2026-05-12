@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.nanxin.hrtrecorder

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
 * Android author watermark: 南盺
 * Add-dose UI references the airy Journey-style medication picker, while keeping
 * HRT Recorder's route-first data model and offline-only local persistence.
 */

@Composable
fun DoseEditorDialogV2(
    initial: DoseEvent,
    isNew: Boolean,
    pickDateTime: (Double, (Double) -> Unit) -> Unit,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (DoseEvent) -> Unit,
) {
    val initialRoute = remember(initial) {
        initial.route.takeIf { medicationChoicesForRoute(it).isNotEmpty() } ?: Route.Oral
    }
    var route by remember { mutableStateOf(initialRoute) }
    var medicationChoice by remember {
        val restored = medicationChoiceForEvent(initial)
        mutableStateOf(restored.takeIf { it in medicationChoicesForRoute(initialRoute) } ?: defaultMedicationChoice(initialRoute))
    }
    var doseText by remember { mutableStateOf(if (initial.doseMG > 0.0) formatNumber(storedDoseToInputMG(initial), 3) else "1") }
    var patchReleaseText by remember {
        mutableStateOf(
            initial.extras[ExtraKey.ReleaseRateUGPerDay]
                ?.takeIf { it.isFinite() && it > 0.0 }
                ?.let { formatNumber(it, 1) }
                ?: "",
        )
    }
    var patchTotalText by remember {
        mutableStateOf(
            if (initial.doseMG > 0.0) formatNumber(storedDoseToInputMG(initial) * 1000.0, 1) else "",
        )
    }
    var eventTimeH by remember { mutableDoubleStateOf(initial.timeH) }
    var gelSiteIndex by remember {
        mutableStateOf(
            initial.extras[ExtraKey.GelSite]
                ?.takeIf { it.isFinite() }
                ?.toInt()
                ?.coerceIn(0, 2)
                ?: 0,
        )
    }
    val routeOptions = remember {
        listOf(Route.Oral, Route.Injection, Route.PatchApply, Route.Gel, Route.Sublingual, Route.PatchRemove)
            .filter { medicationChoicesForRoute(it).isNotEmpty() }
    }
    val currentMedicationChoices = remember(route) { medicationChoicesForRoute(route) }

    fun buildDoseEvent(): DoseEvent? {
        val rawDose = doseText.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }
        val releaseUg = patchReleaseText.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }
        val patchTotalUg = patchTotalText.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }
        val category = medicationChoice.category
        val finalCompound = if (category == MedicationCategory.AntiAndrogen) Compound.E2 else medicationChoice.compound
        val activeDose = when {
            route == Route.PatchRemove -> 0.0
            route == Route.PatchApply -> {
                val rawPatchMG = ((patchTotalUg ?: releaseUg) ?: return null) / 1000.0
                when (category) {
                    MedicationCategory.Estradiol -> rawPatchMG * toE2Factor(finalCompound)
                    MedicationCategory.Testosterone -> rawPatchMG * toActiveFactor(finalCompound)
                    else -> rawPatchMG
                }
            }
            else -> {
                val rawDoseValue = rawDose ?: return null
                when (category) {
                    MedicationCategory.Estradiol -> rawDoseValue * toE2Factor(finalCompound)
                    MedicationCategory.Cpa -> rawDoseValue
                    MedicationCategory.Testosterone -> rawDoseValue * toActiveFactor(finalCompound)
                    MedicationCategory.AntiAndrogen -> rawDoseValue
                }
            }
        }
        val extras = initial.extras.toMutableMap().apply {
            if (route == Route.PatchApply) {
                if (releaseUg != null) {
                    put(ExtraKey.ReleaseRateUGPerDay, releaseUg)
                } else {
                    remove(ExtraKey.ReleaseRateUGPerDay)
                }
            } else {
                remove(ExtraKey.ReleaseRateUGPerDay)
            }
            if (route == Route.Gel) {
                put(ExtraKey.GelSite, gelSiteIndex.toDouble())
            } else {
                remove(ExtraKey.GelSite)
            }
        }
        return initial.copy(
            category = category,
            compound = finalCompound,
            route = route,
            recordOnlyMedication = if (category == MedicationCategory.AntiAndrogen) medicationChoice.recordOnlyMedication else null,
            doseMG = activeDose,
            timeH = eventTimeH,
            extras = extras,
        )
    }

    val rawDose = doseText.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }
    val releaseUg = patchReleaseText.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }
    val patchTotalUg = patchTotalText.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }
    val canSave = when (route) {
        Route.PatchApply -> releaseUg != null || patchTotalUg != null
        Route.PatchRemove -> true
        else -> rawDose != null
    }
    val accent = when (medicationChoice.category) {
        MedicationCategory.Estradiol -> E2Pink
        MedicationCategory.Cpa, MedicationCategory.AntiAndrogen -> CpaRose
        MedicationCategory.Testosterone -> TBlue
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
                            if (isNew) language.t("添加药物", "Add medication") else language.t("编辑药物", "Edit medication"),
                            color = Ink,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.weight(1.22f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        GradientActionPill(
                            language.t("保存", "Save"),
                            onClick = { buildDoseEvent()?.let(onSave) },
                            enabled = canSave,
                            modifier = Modifier.weight(0.86f),
                            accent = accent,
                        )
                    }
                }
                item {
                    SoftListRow(
                        icon = "+",
                        title = language.t("自定义药物", "Custom medication"),
                        subtitle = language.t("当前版本先记录在备注字段规划中；常用药物请从下方推荐选择。", "For now, choose common medications below; custom entries are planned."),
                        accent = TBlue,
                    )
                }
                item {
                    NativeCard {
                        Text(language.t("给药方式", "Route"), color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        Text(language.t("先选方式，下面只显示能用这个方式添加的药。", "Pick the route first; only compatible medications appear below."), color = Muted, lineHeight = 19.sp)
                        Spacer(Modifier.height(12.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            routeOptions.forEach { item ->
                                val selected = item == route
                                if (selected) {
                                    GradientActionPill(
                                        text = item.label(language),
                                        onClick = {},
                                        accent = accent,
                                    )
                                } else {
                                    SecondaryGlassPill(
                                        text = item.label(language),
                                        onClick = {
                                            route = item
                                            medicationChoice = defaultMedicationChoice(item)
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
                        Text(language.t("推荐", "Recommended"), color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(10.dp))
                        currentMedicationChoices.forEach { item ->
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
                                trailing = {
                                    Text(
                                        if (item == medicationChoice) "✓" else "+",
                                        color = if (item == medicationChoice) itemAccent else TBlue,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                    )
                                },
                                onClick = { medicationChoice = item },
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                    }
                }
                item {
                    NativeCard {
                        Text(language.t("药物详情", "Medication Details"), color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(12.dp))
                        if (route == Route.Gel && medicationChoice.category == MedicationCategory.Estradiol) {
                            Text(
                                language.t("涂抹部位", "Application site"),
                                color = Ink,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(8.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(0, 1, 2).forEach { site ->
                                    if (site == gelSiteIndex) {
                                        GradientActionPill(
                                            text = gelSiteLabel(site.toDouble(), language),
                                            onClick = {},
                                            accent = accent,
                                        )
                                    } else {
                                        SecondaryGlassPill(
                                            text = gelSiteLabel(site.toDouble(), language),
                                            onClick = { gelSiteIndex = site },
                                            accent = Muted,
                                        )
                                    }
                                }
                            }
                            Text(
                                language.t("参考 hrt.mahiro：手臂/大腿 0.05，阴囊 0.40。仅用于趋势估算。", "Reference hrt.mahiro: arm/thigh 0.05, scrotal 0.40. Trend estimate only."),
                                color = Muted,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
                            )
                        }
                        if (route == Route.PatchApply) {
                            OutlinedTextField(
                                value = patchReleaseText,
                                onValueChange = {
                                    patchReleaseText = it
                                    if (it.isNotBlank()) patchTotalText = ""
                                },
                                label = { Text(language.t("每日释放（用于曲线）", "Daily release for curve")) },
                                suffix = { Text("ug/day") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = patchTotalText,
                                onValueChange = {
                                    patchTotalText = it
                                    if (it.isNotBlank()) patchReleaseText = ""
                                },
                                label = { Text(language.t("单贴含量（用于记录）", "Single patch amount for record")) },
                                suffix = { Text("ug") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                language.t("二选一：填每日释放则按释放量画曲线；填单贴含量则只作记录和扣药瓶。", "Choose one: daily release drives the curve; single patch amount is used for record and bottle deduction."),
                                color = Muted,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        } else if (route != Route.PatchRemove) {
                            OutlinedTextField(
                                value = doseText,
                                onValueChange = { doseText = it },
                                label = { Text(language.t("剂量", "Dose")) },
                                suffix = { Text("mg") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            Text(language.t("移除贴片只记录移除时间，不需要填写剂量。", "Patch removal only records removal time; no dose is needed."), color = Muted, lineHeight = 20.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        SoftListRow(
                            icon = "T",
                            title = language.t("时间", "Time"),
                            subtitle = formatWallTime(eventTimeH),
                            accent = accent,
                            onClick = { pickDateTime(eventTimeH) { eventTimeH = it } },
                        )
                    }
                }
            }
        }
    }
}
