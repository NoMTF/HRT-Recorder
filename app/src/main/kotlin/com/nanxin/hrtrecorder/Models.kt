// Android 版作者：南盺
// Reference notes: Journey, HRT-Recorder-online, hrt.mahiro.uk, HRT-Recorder-PKcomponent-Test.
package com.nanxin.hrtrecorder

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

enum class Route(val wire: String, val labelZh: String) {
    Injection("injection", "注射"),
    PatchApply("patchApply", "贴片"),
    PatchRemove("patchRemove", "移除贴片"),
    Gel("gel", "凝胶"),
    Oral("oral", "口服"),
    Sublingual("sublingual", "舌下");

    companion object {
        fun from(value: String?): Route =
            entries.firstOrNull { it.wire == value } ?: Injection
    }
}

enum class MedicationCategory(val wire: String, val labelZh: String) {
    Estradiol("estradiol", "雌二醇"),
    Cpa("cpa", "CPA"),
    Testosterone("testosterone", "睾酮"),
    AntiAndrogen("antiAndrogen", "抗雄");

    companion object {
        fun from(value: String?): MedicationCategory? =
            entries.firstOrNull { it.wire == value }
    }
}

enum class Analyte(val wire: String, val label: String, val unit: String) {
    E2("e2", "E2", "pg/mL"),
    CPA("antiAndrogen", "抗雄", "ng/mL"),
    Testosterone("testosterone", "T", "ng/dL")
}

enum class ThemeMode(val wire: String, val labelZh: String) {
    System("system", "跟随系统"),
    Light("light", "浅色"),
    Dark("dark", "深色");

    companion object {
        fun from(value: String?): ThemeMode =
            entries.firstOrNull { it.wire == value } ?: System
    }
}

enum class AppLanguage(val wire: String, val labelZh: String, val nativeLabel: String) {
    ZhHans("zh-Hans", "简体中文", "简体中文"),
    English("en", "英文", "English");

    companion object {
        fun from(value: String?): AppLanguage =
            entries.firstOrNull { it.wire == value } ?: ZhHans
    }
}

enum class PlanRepeat(val wire: String, val labelZh: String) {
    Daily("daily", "每日"),
    Weekly("weekly", "每周");

    companion object {
        fun from(value: String?): PlanRepeat =
            entries.firstOrNull { it.wire == value } ?: Daily
    }
}

enum class PlanDoseStatus(val wire: String, val labelZh: String) {
    Pending("pending", "待执行"),
    Taken("taken", "已服用"),
    Skipped("skipped", "已跳过"),
    Missed("missed", "已遗漏");

    companion object {
        fun from(value: String?): PlanDoseStatus =
            entries.firstOrNull { it.wire == value } ?: Pending
    }
}

enum class Compound(val wire: String, val labelZh: String) {
    E2("E2", "雌二醇"),
    EB("EB", "苯甲酸雌二醇"),
    EV("EV", "戊酸雌二醇"),
    EPP("EPP", "苯丙酸雌二醇"),
    EC("EC", "环戊丙酸雌二醇"),
    EN("EN", "庚酸雌二醇"),
    EU("EU", "十一酸雌二醇"),
    CPA("CPA", "醋酸环丙孕酮"),
    T("T", "睾酮"),
    TC("TC", "环戊丙酸睾酮"),
    TE("TE", "庚酸睾酮"),
    TU("TU", "十一酸睾酮");

    companion object {
        fun from(value: String?): Compound =
            entries.firstOrNull { it.wire == value } ?: EV
    }
}

enum class RecordOnlyMedication(val wire: String, val labelZh: String) {
    CyproteroneAcetate("cyproteroneAcetate", "醋酸环丙孕酮"),
    Spironolactone("spironolactone", "螺内酯"),
    Bicalutamide("bicalutamide", "比卡鲁胺"),
    Finasteride("finasteride", "非那雄胺"),
    Dutasteride("dutasteride", "度他雄胺");

    companion object {
        fun from(value: String?): RecordOnlyMedication? =
            entries.firstOrNull { it.wire == value }
    }
}

enum class ExtraKey(val wire: String) {
    ConcentrationMGmL("concentrationMGmL"),
    AreaCM2("areaCM2"),
    ReleaseRateUGPerDay("releaseRateUGPerDay"),
    SublingualTheta("sublingualTheta"),
    SublingualTier("sublingualTier"),
    GelSite("gelSite"),
    BottleDeductedUnits("bottleDeductedUnits");

    companion object {
        fun from(value: String?): ExtraKey? =
            entries.firstOrNull { it.wire == value }
    }
}

data class DoseEvent(
    val id: String = UUID.randomUUID().toString(),
    val category: MedicationCategory = MedicationCategory.Estradiol,
    val route: Route = Route.Injection,
    val timeH: Double = nowEpochHours(),
    val doseMG: Double = 0.0,
    val compound: Compound = Compound.EV,
    val recordOnlyMedication: RecordOnlyMedication? = null,
    val extras: Map<ExtraKey, Double> = emptyMap(),
)

data class MedicationChoice(
    val category: MedicationCategory,
    val compound: Compound = Compound.E2,
    val recordOnlyMedication: RecordOnlyMedication? = null,
)

data class LabResult(
    val id: String = UUID.randomUUID().toString(),
    val timeH: Double = nowEpochHours(),
    val concValue: Double = 0.0,
    val unit: String = "pg/ml",
)

data class MedicationPlan(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "",
    val groupName: String = "",
    val category: MedicationCategory = MedicationCategory.Estradiol,
    val route: Route = Route.Oral,
    val compound: Compound = Compound.EV,
    val recordOnlyMedication: RecordOnlyMedication? = null,
    val doseMG: Double = 0.0,
    val timeMinutes: Int = 21 * 60,
    val timeMinutesList: List<Int> = listOf(timeMinutes),
    val repeat: PlanRepeat = PlanRepeat.Daily,
    val reminderEnabled: Boolean = true,
    val systemSyncEnabled: Boolean = false,
    val enabled: Boolean = true,
)

data class PlanDoseRecord(
    val id: String = UUID.randomUUID().toString(),
    val planId: String,
    val scheduledTimeH: Double,
    val status: PlanDoseStatus = PlanDoseStatus.Pending,
    val actedTimeH: Double? = null,
    val generatedDoseId: String? = null,
)

data class PillBottle(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val category: MedicationCategory = MedicationCategory.Estradiol,
    val compound: Compound = Compound.EV,
    val recordOnlyMedication: RecordOnlyMedication? = null,
    val unit: String = "片",
    val totalUnits: Double = 0.0,
    val remainingUnits: Double = 0.0,
    val perDoseUnits: Double = 1.0,
    val createdTimeH: Double = nowEpochHours(),
    val expiresTimeH: Double? = null,
)

data class SimulationResult(
    val timeH: DoubleArray,
    val concentration: DoubleArray,
    val auc: Double,
    val analyte: Analyte,
)

data class AppStateSnapshot(
    val events: List<DoseEvent>,
    val labResults: List<LabResult>,
    val weightKg: Double,
    val calibrationModel: String,
    val doseTemplatesRaw: List<String>,
    val medicationPlans: List<MedicationPlan> = emptyList(),
    val planDoseRecords: List<PlanDoseRecord> = emptyList(),
    val pillBottles: List<PillBottle> = emptyList(),
)

fun nowEpochHours(): Double = System.currentTimeMillis().toDouble() / 3_600_000.0

private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

fun formatWallTime(timeH: Double): String =
    LocalDateTime.ofInstant(Instant.ofEpochMilli((timeH * 3_600_000.0).toLong()), ZoneId.systemDefault())
        .format(dateTimeFormatter)

fun parseWallTimeOrNow(value: String): Double =
    runCatching {
        val local = LocalDateTime.parse(value.trim(), dateTimeFormatter)
        local.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli().toDouble() / 3_600_000.0
    }.getOrElse { nowEpochHours() }

fun categoryForCompound(compound: Compound): MedicationCategory =
    when (compound) {
        Compound.CPA -> MedicationCategory.Cpa
        Compound.T, Compound.TC, Compound.TE, Compound.TU -> MedicationCategory.Testosterone
        else -> MedicationCategory.Estradiol
    }

fun allowedCompounds(category: MedicationCategory): List<Compound> =
    when (category) {
        MedicationCategory.Estradiol -> listOf(Compound.E2, Compound.EB, Compound.EV, Compound.EPP, Compound.EC, Compound.EN, Compound.EU)
        MedicationCategory.Cpa -> listOf(Compound.CPA)
        MedicationCategory.Testosterone -> listOf(Compound.T, Compound.TC, Compound.TE, Compound.TU)
        MedicationCategory.AntiAndrogen -> listOf(Compound.E2)
    }

fun defaultCompound(category: MedicationCategory): Compound =
    when (category) {
        MedicationCategory.Estradiol -> Compound.EV
        MedicationCategory.Cpa -> Compound.CPA
        MedicationCategory.Testosterone -> Compound.TC
        MedicationCategory.AntiAndrogen -> Compound.E2
    }

fun allowedRoutes(category: MedicationCategory, compound: Compound): List<Route> =
    when (category) {
        MedicationCategory.Estradiol ->
            when (compound) {
                Compound.EB, Compound.EPP, Compound.EC, Compound.EN, Compound.EU -> listOf(Route.Injection)
                else -> listOf(Route.Injection, Route.Gel, Route.Oral, Route.Sublingual, Route.PatchApply, Route.PatchRemove)
            }
        MedicationCategory.Cpa -> listOf(Route.Oral)
        MedicationCategory.Testosterone ->
            when (compound) {
                Compound.T -> listOf(Route.Gel, Route.PatchApply, Route.PatchRemove)
                Compound.TU -> listOf(Route.Injection, Route.Oral)
                else -> listOf(Route.Injection)
            }
        MedicationCategory.AntiAndrogen -> listOf(Route.Oral)
    }

fun medicationChoicesForRoute(route: Route): List<MedicationChoice> {
    fun compoundsForRoute(category: MedicationCategory): List<Compound> {
        val allowed = allowedCompounds(category).filter { compound -> route in allowedRoutes(category, compound) }
        return when {
            category == MedicationCategory.Estradiol && route == Route.Injection ->
                listOf(Compound.EV, Compound.EB, Compound.EPP, Compound.EC, Compound.EN, Compound.EU, Compound.E2)
                    .filter { it in allowed }
            category == MedicationCategory.Testosterone && route == Route.Injection ->
                listOf(Compound.TC, Compound.TE, Compound.TU).filter { it in allowed }
            else -> allowed
        }
    }

    val curveChoices = listOf(
        MedicationCategory.Estradiol,
        MedicationCategory.Cpa,
        MedicationCategory.Testosterone,
    ).flatMap { category ->
        compoundsForRoute(category)
            .map { compound -> MedicationChoice(category = category, compound = compound) }
    }
    val recordOnlyChoices = if (route in allowedRoutes(MedicationCategory.AntiAndrogen, Compound.E2)) {
        RecordOnlyMedication.entries
            .filter { it != RecordOnlyMedication.CyproteroneAcetate }
            .map {
                MedicationChoice(
                    category = MedicationCategory.AntiAndrogen,
                    compound = Compound.E2,
                    recordOnlyMedication = it,
                )
            }
    } else {
        emptyList()
    }
    return curveChoices + recordOnlyChoices
}

fun defaultMedicationChoice(route: Route): MedicationChoice =
    medicationChoicesForRoute(route).firstOrNull()
        ?: MedicationChoice(MedicationCategory.Estradiol, Compound.EV)

fun medicationChoiceForEvent(event: DoseEvent): MedicationChoice =
    if (event.category == MedicationCategory.AntiAndrogen && event.recordOnlyMedication == RecordOnlyMedication.CyproteroneAcetate) {
        MedicationChoice(category = MedicationCategory.Cpa, compound = Compound.CPA)
    } else if (event.category == MedicationCategory.AntiAndrogen) {
        MedicationChoice(
            category = MedicationCategory.AntiAndrogen,
            compound = Compound.E2,
            recordOnlyMedication = event.recordOnlyMedication ?: RecordOnlyMedication.Spironolactone,
        )
    } else {
        MedicationChoice(category = event.category, compound = event.compound)
    }

fun eventAnalyte(event: DoseEvent): Analyte? =
    when (event.category) {
        MedicationCategory.Estradiol -> Analyte.E2
        MedicationCategory.Cpa -> Analyte.CPA
        MedicationCategory.Testosterone -> Analyte.Testosterone
        MedicationCategory.AntiAndrogen ->
            if (event.recordOnlyMedication == RecordOnlyMedication.CyproteroneAcetate) Analyte.CPA else null
    }

fun medicationKey(
    category: MedicationCategory,
    compound: Compound,
    recordOnlyMedication: RecordOnlyMedication?,
): String =
    if (category == MedicationCategory.AntiAndrogen) {
        "${category.wire}:${recordOnlyMedication?.wire.orEmpty()}"
    } else {
        "${category.wire}:${compound.wire}"
    }

fun DoseEvent.medicationKey(): String = medicationKey(category, compound, recordOnlyMedication)

fun MedicationPlan.medicationKey(): String = medicationKey(category, compound, recordOnlyMedication)

fun PillBottle.medicationKey(): String = medicationKey(category, compound, recordOnlyMedication)
