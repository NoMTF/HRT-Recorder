package com.nanxin.hrtrecorder

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.math.abs
import kotlin.math.min

/*
 * Android author watermark: Nanxin.
 * Plan execution is intentionally isolated from UI. A scheduled plan becomes a
 * real dose only after the user confirms "taken"; skipped/missed states never
 * alter the PK curve.
 */

data class PlanOccurrence(
    val key: String,
    val plan: MedicationPlan,
    val scheduledTimeH: Double,
    val status: PlanDoseStatus,
    val record: PlanDoseRecord?,
)

data class MedicationStatusSummary(
    val analyte: Analyte,
    val title: String,
    val medicationName: String,
    val routeName: String,
    val doseText: String,
    val lastTimeH: Double?,
    val nextTimeH: Double?,
    val sourcePlan: MedicationPlan?,
)

data class PlanActionResult(
    val snapshot: AppStateSnapshot,
    val generatedDose: DoseEvent? = null,
    val deductedUnits: Double = 0.0,
    val bottleName: String = "",
)

private const val OCCURRENCE_MATCH_TOLERANCE_H = 1.0 / 60.0
private const val MISSED_GRACE_H = 2.0

fun occurrenceKey(planId: String, scheduledTimeH: Double): String =
    "$planId:${(scheduledTimeH * 60.0).toLong()}"

fun epochHoursFromLocal(local: LocalDateTime): Double =
    local.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli().toDouble() / 3_600_000.0

fun localFromEpochHours(timeH: Double): LocalDateTime =
    LocalDateTime.ofInstant(
        Instant.ofEpochMilli((timeH * 3_600_000.0).toLong()),
        ZoneId.systemDefault(),
    )

fun scheduledTimeForDate(date: LocalDate, timeMinutes: Int): Double =
    epochHoursFromLocal(
        date
            .atStartOfDay()
            .plusMinutes(timeMinutes.coerceIn(0, 1439).toLong()),
    )

fun buildTodayPlanOccurrences(
    plans: List<MedicationPlan>,
    records: List<PlanDoseRecord>,
    nowH: Double = nowEpochHours(),
): List<PlanOccurrence> {
    return buildPlanOccurrencesForDate(
        plans = plans,
        records = records,
        date = localFromEpochHours(nowH).toLocalDate(),
        nowH = nowH,
    )
}

fun buildPlanOccurrencesForDate(
    plans: List<MedicationPlan>,
    records: List<PlanDoseRecord>,
    date: LocalDate,
    nowH: Double = nowEpochHours(),
): List<PlanOccurrence> {
    val referenceDate = localFromEpochHours(nowH).toLocalDate()
    return plans
        .filter { it.enabled }
        .filter { it.appearsOnDate(date, referenceDate) }
        .flatMap { plan ->
            plan.normalizedTimes().map { minutes ->
                val scheduled = scheduledTimeForDate(date, minutes)
                val record = findPlanRecord(records, plan.id, scheduled)
                PlanOccurrence(
                    key = occurrenceKey(plan.id, scheduled),
                    plan = plan,
                    scheduledTimeH = scheduled,
                    status = record?.status ?: if (scheduled + MISSED_GRACE_H < nowH) PlanDoseStatus.Missed else PlanDoseStatus.Pending,
                    record = record,
                )
            }
        }
        .sortedWith(compareBy<PlanOccurrence> { it.scheduledTimeH }.thenBy { it.plan.displaySortName() })
}

private fun MedicationPlan.appearsOnDate(date: LocalDate, referenceDate: LocalDate): Boolean =
    when (repeat) {
        PlanRepeat.Daily -> true
        PlanRepeat.Weekly -> date.dayOfWeek == referenceDate.dayOfWeek
    }

fun nextOccurrenceTime(plan: MedicationPlan, nowH: Double = nowEpochHours()): Double? {
    if (!plan.enabled) return null
    val now = localFromEpochHours(nowH)
    val today = now.toLocalDate()
    val futureToday = plan.normalizedTimes()
        .map { minutes -> scheduledTimeForDate(today, minutes) }
        .firstOrNull { it >= nowH - OCCURRENCE_MATCH_TOLERANCE_H }
    if (futureToday != null) return futureToday

    val nextDate = when (plan.repeat) {
        PlanRepeat.Daily -> today.plusDays(1)
        PlanRepeat.Weekly -> today.plusWeeks(1)
    }
    return plan.normalizedTimes().firstOrNull()?.let { scheduledTimeForDate(nextDate, it) }
}

fun previousOccurrenceTime(plan: MedicationPlan, nowH: Double = nowEpochHours()): Double? {
    if (!plan.enabled) return null
    val now = localFromEpochHours(nowH)
    val today = now.toLocalDate()
    val pastToday = plan.normalizedTimes()
        .map { minutes -> scheduledTimeForDate(today, minutes) }
        .lastOrNull { it <= nowH + OCCURRENCE_MATCH_TOLERANCE_H }
    if (pastToday != null) return pastToday

    val previousDate = when (plan.repeat) {
        PlanRepeat.Daily -> today.minusDays(1)
        PlanRepeat.Weekly -> today.minusWeeks(1)
    }
    return plan.normalizedTimes().lastOrNull()?.let { scheduledTimeForDate(previousDate, it) }
}

fun buildMedicationStatusSummaries(
    events: List<DoseEvent>,
    plans: List<MedicationPlan>,
    language: AppLanguage,
    nowH: Double = nowEpochHours(),
): List<MedicationStatusSummary> {
    return listOf(Analyte.E2, Analyte.CPA, Analyte.Testosterone).map { analyte ->
        val lastEvent = events
            .asSequence()
            .filter { eventAnalyte(it) == analyte }
            .maxByOrNull { it.timeH }
        val nextPlan = plans
            .filter { planMatchesAnalyte(it, analyte) && it.enabled }
            .minByOrNull { nextOccurrenceTime(it, nowH) ?: Double.POSITIVE_INFINITY }
        val representativePlan = nextPlan ?: lastEvent?.let(::planLikeEvent)
        MedicationStatusSummary(
            analyte = analyte,
            title = analyte.displayLabel(language, if (analyte == Analyte.CPA) antiAndrogenDisplayLabel(events, language) else null),
            medicationName = representativePlan?.displayName(language)
                ?: when (analyte) {
                    Analyte.E2 -> language.t("暂无雌二醇计划", "No estradiol plan")
                    Analyte.CPA -> language.t("暂无抗雄计划", "No antiandrogen plan")
                    Analyte.Testosterone -> language.t("暂无睾酮计划", "No testosterone plan")
                },
            routeName = representativePlan?.route?.label(language) ?: language.t("未设置", "Not set"),
            doseText = representativePlan?.let { planDoseText(it, language) } ?: language.t("等待添加", "Add one"),
            lastTimeH = lastEvent?.timeH,
            nextTimeH = nextPlan?.let { nextOccurrenceTime(it, nowH) },
            sourcePlan = nextPlan,
        )
    }
}

fun markPlanTaken(
    snapshot: AppStateSnapshot,
    planId: String,
    scheduledTimeH: Double,
    actedTimeH: Double = nowEpochHours(),
): PlanActionResult {
    val plan = snapshot.medicationPlans.firstOrNull { it.id == planId }
        ?: return PlanActionResult(snapshot)
    val dose = createDoseFromPlan(plan, scheduledTimeH)
    val deduction = deductBottleForEvent(snapshot.pillBottles, dose)
    val finalDose = deduction.event
    val records = upsertPlanRecord(
        records = snapshot.planDoseRecords,
        planId = plan.id,
        scheduledTimeH = scheduledTimeH,
        status = PlanDoseStatus.Taken,
        actedTimeH = actedTimeH,
        generatedDoseId = finalDose.id,
    )
    val nextSnapshot = snapshot.copy(
        events = (snapshot.events + finalDose).sortedByDescending { it.timeH },
        planDoseRecords = records,
        pillBottles = deduction.bottles,
    )
    return PlanActionResult(
        snapshot = nextSnapshot,
        generatedDose = finalDose,
        deductedUnits = deduction.deductedUnits,
        bottleName = deduction.bottleName,
    )
}

fun markPlanSkipped(
    snapshot: AppStateSnapshot,
    planId: String,
    scheduledTimeH: Double,
    actedTimeH: Double = nowEpochHours(),
): PlanActionResult {
    val records = upsertPlanRecord(
        records = snapshot.planDoseRecords,
        planId = planId,
        scheduledTimeH = scheduledTimeH,
        status = PlanDoseStatus.Skipped,
        actedTimeH = actedTimeH,
        generatedDoseId = null,
    )
    return PlanActionResult(snapshot.copy(planDoseRecords = records))
}

fun createDoseFromPlan(plan: MedicationPlan, timeH: Double = nowEpochHours()): DoseEvent =
    DoseEvent(
        category = plan.category,
        route = plan.route,
        timeH = timeH,
        doseMG = plan.doseMG,
        compound = plan.compound,
        recordOnlyMedication = plan.recordOnlyMedication,
    )

data class SharedBottleDeductionResult(
    val bottles: List<PillBottle>,
    val event: DoseEvent,
    val deductedUnits: Double,
    val bottleName: String,
)

fun deductBottleForEvent(bottles: List<PillBottle>, event: DoseEvent): SharedBottleDeductionResult {
    if (event.route == Route.PatchRemove || event.doseMG <= 0.0) {
        return SharedBottleDeductionResult(bottles, event, 0.0, "")
    }
    val index = bottles.indexOfFirst { bottle ->
        bottle.medicationKey() == event.medicationKey() &&
            bottle.createdTimeH <= event.timeH &&
            (bottle.expiresTimeH == null || bottle.expiresTimeH >= event.timeH) &&
            bottle.remainingUnits > 0.0
    }
    if (index < 0) return SharedBottleDeductionResult(bottles, event, 0.0, "")
    val bottle = bottles[index]
    val deducted = min(bottle.perDoseUnits, bottle.remainingUnits).coerceAtLeast(0.0)
    if (deducted <= 0.0) return SharedBottleDeductionResult(bottles, event, 0.0, "")
    val nextBottle = bottle.copy(remainingUnits = (bottle.remainingUnits - deducted).coerceAtLeast(0.0))
    val nextBottles = bottles.toMutableList().also { it[index] = nextBottle }
    val nextEvent = event.copy(extras = event.extras + (ExtraKey.BottleDeductedUnits to deducted))
    return SharedBottleDeductionResult(nextBottles, nextEvent, deducted, bottle.displaySortName())
}

fun planDoseText(plan: MedicationPlan, language: AppLanguage): String =
    when (plan.category) {
        MedicationCategory.Estradiol -> "${formatNumber(plan.doseMG, 3)} mg E2-eq"
        MedicationCategory.Testosterone -> "${formatNumber(plan.doseMG, 3)} mg T-eq"
        MedicationCategory.Cpa -> "${formatNumber(plan.doseMG, 3)} mg CPA"
        MedicationCategory.AntiAndrogen -> "${formatNumber(plan.doseMG, 3)} mg"
    }

fun MedicationPlan.normalizedTimes(): List<Int> =
    timeMinutesList
        .ifEmpty { listOf(timeMinutes) }
        .map { it.coerceIn(0, 1439) }
        .distinct()
        .sorted()

private fun findPlanRecord(records: List<PlanDoseRecord>, planId: String, scheduledTimeH: Double): PlanDoseRecord? =
    records.firstOrNull { it.planId == planId && abs(it.scheduledTimeH - scheduledTimeH) <= OCCURRENCE_MATCH_TOLERANCE_H }

private fun upsertPlanRecord(
    records: List<PlanDoseRecord>,
    planId: String,
    scheduledTimeH: Double,
    status: PlanDoseStatus,
    actedTimeH: Double?,
    generatedDoseId: String?,
): List<PlanDoseRecord> {
    var replaced = false
    val next = records.map { record ->
        if (record.planId == planId && abs(record.scheduledTimeH - scheduledTimeH) <= OCCURRENCE_MATCH_TOLERANCE_H) {
            replaced = true
            record.copy(status = status, actedTimeH = actedTimeH, generatedDoseId = generatedDoseId)
        } else {
            record
        }
    }
    if (replaced) return next.sortedByDescending { it.scheduledTimeH }
    return (next + PlanDoseRecord(
        id = UUID.randomUUID().toString(),
        planId = planId,
        scheduledTimeH = scheduledTimeH,
        status = status,
        actedTimeH = actedTimeH,
        generatedDoseId = generatedDoseId,
    )).sortedByDescending { it.scheduledTimeH }
}

private fun planMatchesAnalyte(plan: MedicationPlan, analyte: Analyte): Boolean =
    when (analyte) {
        Analyte.E2 -> plan.category == MedicationCategory.Estradiol
        Analyte.CPA -> plan.category == MedicationCategory.Cpa || plan.category == MedicationCategory.AntiAndrogen
        Analyte.Testosterone -> plan.category == MedicationCategory.Testosterone
    }

private fun planLikeEvent(event: DoseEvent): MedicationPlan =
    MedicationPlan(
        category = event.category,
        route = event.route,
        compound = event.compound,
        recordOnlyMedication = event.recordOnlyMedication,
        doseMG = event.doseMG,
        timeMinutes = localFromEpochHours(event.timeH).let { it.hour * 60 + it.minute },
    )

private fun MedicationPlan.displaySortName(): String =
    label.ifBlank { recordOnlyMedication?.wire ?: compound.wire }

private fun PillBottle.displaySortName(): String =
    name.ifBlank { recordOnlyMedication?.wire ?: compound.wire }
