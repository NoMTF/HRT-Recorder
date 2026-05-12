// Android 版作者：南盺
// Reference compatibility: hrt.mahiro.uk plain JSON and HRT Recorder web backups.
package com.nanxin.hrtrecorder

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

private enum class ImportSource {
    HrtRecorder,
    HrtMahiro,
    Legacy,
}

class NativeStore(context: Context) {
    private val prefs = context.getSharedPreferences("hrt-native-store", Context.MODE_PRIVATE)

    fun hasAcceptedPrivacy(): Boolean =
        prefs.getBoolean("hrt-privacy-accepted-v1", false)

    fun savePrivacyAccepted() {
        prefs.edit()
            .putBoolean("hrt-privacy-accepted-v1", true)
            .putLong("hrt-privacy-accepted-at", System.currentTimeMillis())
            .apply()
    }

    fun loadThemeMode(): ThemeMode =
        ThemeMode.from(prefs.getString("hrt-theme-mode", ThemeMode.System.wire))

    fun saveThemeMode(themeMode: ThemeMode) {
        prefs.edit().putString("hrt-theme-mode", themeMode.wire).apply()
    }

    fun loadAppLanguage(): AppLanguage =
        AppLanguage.from(prefs.getString("hrt-language", AppLanguage.ZhHans.wire))

    fun saveAppLanguage(language: AppLanguage) {
        prefs.edit().putString("hrt-language", language.wire).apply()
    }

    fun load(): AppStateSnapshot {
        val eventsRaw = prefs.getString("hrt-events", "[]") ?: "[]"
        val labsRaw = prefs.getString("hrt-lab-results", "[]") ?: "[]"
        val plansRaw = prefs.getString("hrt-medication-plans", "[]") ?: "[]"
        val planRecordsRaw = prefs.getString("hrt-plan-dose-records", "[]") ?: "[]"
        val bottlesRaw = prefs.getString("hrt-pill-bottles", "[]") ?: "[]"
        val events = runCatching { parseEvents(JSONArray(eventsRaw), ImportSource.HrtRecorder) }.getOrDefault(emptyList())
        val labs = runCatching { parseLabs(JSONArray(labsRaw), ImportSource.HrtRecorder) }.getOrDefault(emptyList())
        val plans = runCatching { parsePlans(JSONArray(plansRaw), ImportSource.HrtRecorder) }.getOrDefault(emptyList())
        val planRecords = runCatching { parsePlanDoseRecords(JSONArray(planRecordsRaw), ImportSource.HrtRecorder) }.getOrDefault(emptyList())
        val bottles = runCatching { parseBottles(JSONArray(bottlesRaw), ImportSource.HrtRecorder) }.getOrDefault(emptyList())
        val storedWeight = prefs.getFloat("hrt-weight", 70f).toDouble().takeIf { it.isFinite() && it > 0.0 } ?: 70.0
        return AppStateSnapshot(
            events = events,
            labResults = labs,
            weightKg = storedWeight,
            calibrationModel = prefs.getString("hrt-calibration-model", "ekf") ?: "ekf",
            doseTemplatesRaw = emptyList(),
            medicationPlans = plans,
            planDoseRecords = planRecords,
            pillBottles = bottles,
        )
    }

    fun save(snapshot: AppStateSnapshot) {
        prefs.edit()
            .putString("hrt-events", eventsToJson(snapshot.events).toString())
            .putString("hrt-lab-results", labsToJson(snapshot.labResults).toString())
            .putString("hrt-medication-plans", plansToJson(snapshot.medicationPlans).toString())
            .putString("hrt-plan-dose-records", planDoseRecordsToJson(snapshot.planDoseRecords).toString())
            .putString("hrt-pill-bottles", bottlesToJson(snapshot.pillBottles).toString())
            .putFloat("hrt-weight", snapshot.weightKg.toFloat())
            .putString("hrt-calibration-model", snapshot.calibrationModel)
            .apply()
    }
}

fun parseBackupJson(raw: String): AppStateSnapshot {
    val trimmed = raw.trim()
    if (trimmed.startsWith("[")) {
        val events = parseEvents(JSONArray(trimmed), ImportSource.Legacy)
        return AppStateSnapshot(events, emptyList(), 70.0, "ekf", emptyList())
    }

    val root = JSONObject(trimmed)
    val source = detectSource(root)
    val data = root.optJSONObject("data") ?: root
    val events = parseEvents(data.optJSONArray("events") ?: JSONArray(), source)
    val labs = parseLabs(data.optJSONArray("labResults") ?: JSONArray(), source)
    val plans = parsePlans(data.optJSONArray("medicationPlans") ?: JSONArray(), source)
    val planRecords = parsePlanDoseRecords(data.optJSONArray("planDoseRecords") ?: JSONArray(), source)
    val bottles = parseBottles(data.optJSONArray("pillBottles") ?: JSONArray(), source)
    val templates = mutableListOf<String>()
    val templatesArray = data.optJSONArray("doseTemplates") ?: JSONArray()
    for (i in 0 until templatesArray.length()) templates += templatesArray.opt(i)?.toString().orEmpty()

    return AppStateSnapshot(
        events = events,
        labResults = labs,
        weightKg = data.optDouble("weight", data.optDouble("weightKg", 70.0)).takeIf { it.isFinite() } ?: 70.0,
        calibrationModel = data.optString("calibrationModel", "ekf").ifBlank { "ekf" },
        doseTemplatesRaw = templates,
        medicationPlans = plans,
        planDoseRecords = planRecords,
        pillBottles = bottles,
    )
}

fun buildBackupJson(snapshot: AppStateSnapshot): String {
    val root = JSONObject()
        .put("meta", JSONObject().put("version", 2).put("exportedAt", Instant.now().toString()))
        .put("weight", snapshot.weightKg)
        .put("events", eventsToJson(snapshot.events))
        .put("labResults", labsToJson(snapshot.labResults))
        .put("medicationPlans", plansToJson(snapshot.medicationPlans))
        .put("planDoseRecords", planDoseRecordsToJson(snapshot.planDoseRecords))
        .put("pillBottles", bottlesToJson(snapshot.pillBottles))
        .put("doseTemplates", JSONArray(snapshot.doseTemplatesRaw))
        .put(
            "hrtRecorder",
            JSONObject()
                .put("platform", "android-native")
                .put("author", "南盺")
                .put("version", BuildConfig.VERSION_NAME)
                .put("versionCode", BuildConfig.VERSION_CODE),
        )
    return root.toString(2)
}

fun buildCsv(snapshot: AppStateSnapshot): String {
    fun cell(value: Any?): String {
        val text = value?.toString().orEmpty()
        return "\"" + text.replace("\"", "\"\"") + "\""
    }
    val rows = mutableListOf("\uFEFFtype,id,time,category,compound,route,dose,unit,extras")
    snapshot.events.sortedByDescending { it.timeH }.forEach { event ->
        rows += listOf(
            "dose",
            event.id,
            formatWallTime(event.timeH),
            event.category.wire,
            event.recordOnlyMedication?.wire ?: event.compound.wire,
            event.route.wire,
            event.doseMG,
            if (event.category == MedicationCategory.AntiAndrogen) "mg" else "mg active/equiv",
            JSONObject(event.extras.mapKeys { it.key.wire }).toString(),
        ).joinToString(",") { cell(it) }
    }
    snapshot.labResults.sortedByDescending { it.timeH }.forEach { lab ->
        rows += listOf("lab", lab.id, formatWallTime(lab.timeH), "E2", "", "", lab.concValue, lab.unit, "").joinToString(",") { cell(it) }
    }
    snapshot.medicationPlans.sortedBy { it.timeMinutes }.forEach { plan ->
        rows += listOf(
            "plan",
            plan.id,
            formatPlanTime(plan.timeMinutes),
            plan.category.wire,
            plan.recordOnlyMedication?.wire ?: plan.compound.wire,
            plan.route.wire,
            plan.doseMG,
            "mg",
            "enabled=${plan.enabled};repeat=${plan.repeat.wire};times=${plan.timeMinutesList.joinToString("|")}",
        ).joinToString(",") { cell(it) }
    }
    snapshot.planDoseRecords.sortedByDescending { it.scheduledTimeH }.forEach { record ->
        rows += listOf(
            "planStatus",
            record.id,
            formatWallTime(record.scheduledTimeH),
            record.planId,
            record.status.wire,
            "",
            "",
            "",
            "acted=${record.actedTimeH?.let(::formatWallTime).orEmpty()};dose=${record.generatedDoseId.orEmpty()}",
        ).joinToString(",") { cell(it) }
    }
    snapshot.pillBottles.sortedByDescending { it.createdTimeH }.forEach { bottle ->
        rows += listOf(
            "bottle",
            bottle.id,
            formatWallTime(bottle.createdTimeH),
            bottle.category.wire,
            bottle.recordOnlyMedication?.wire ?: bottle.compound.wire,
            "",
            "${bottle.remainingUnits}/${bottle.totalUnits}",
            bottle.unit,
            "perDose=${bottle.perDoseUnits};expires=${bottle.expiresTimeH?.let(::formatWallTime).orEmpty()}",
        ).joinToString(",") { cell(it) }
    }
    return rows.joinToString("\n")
}

private fun detectSource(root: JSONObject): ImportSource {
    if (root.has("hrtRecorder")) return ImportSource.HrtRecorder
    val data = root.optJSONObject("data")
    if (data?.has("hrtRecorder") == true) return ImportSource.HrtRecorder
    if (root.has("meta") && root.has("events") && root.has("weight")) return ImportSource.HrtMahiro
    return ImportSource.Legacy
}

private fun parseEvents(array: JSONArray, source: ImportSource): List<DoseEvent> {
    val events = mutableListOf<DoseEvent>()
    for (i in 0 until array.length()) {
        val obj = array.optJSONObject(i) ?: continue
        val compound = Compound.from(obj.optString("compound", obj.optString("ester", "EV")))
        val recordOnly = RecordOnlyMedication.from(obj.optString("recordOnlyMedication", ""))
        val explicitCategory = MedicationCategory.from(obj.optString("category", ""))
        val category = explicitCategory ?: recordOnly?.let { MedicationCategory.AntiAndrogen } ?: categoryForCompound(compound)
        val route = Route.from(obj.optString("route", "injection"))
        val extras = mutableMapOf<ExtraKey, Double>()
        val extrasObject = obj.optJSONObject("extras") ?: JSONObject()
        extrasObject.keys().forEach { key ->
            val extraKey = ExtraKey.from(key)
            val value = extrasObject.optDouble(key, Double.NaN)
            if (extraKey != null && value.isFinite()) extras[extraKey] = value
        }
        val time = normalizeImportedTimeH(obj.optDouble("timeH", nowEpochHours()), source)
        val rawDose = obj.optDouble("doseMG", 0.0).takeIf { it.isFinite() } ?: 0.0
        val dose = normalizeImportedDoseMG(rawDose, category, compound, source)
        events += DoseEvent(
            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
            category = category,
            route = route,
            timeH = time,
            doseMG = dose,
            compound = compound,
            recordOnlyMedication = recordOnly,
            extras = extras,
        )
    }
    return events
}

private fun parseLabs(array: JSONArray, source: ImportSource): List<LabResult> {
    val labs = mutableListOf<LabResult>()
    for (i in 0 until array.length()) {
        val obj = array.optJSONObject(i) ?: continue
        val time = normalizeImportedTimeH(obj.optDouble("timeH", nowEpochHours()), source)
        val value = obj.optDouble("concValue", Double.NaN)
        if (!value.isFinite()) continue
        labs += LabResult(
            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
            timeH = time,
            concValue = value,
            unit = obj.optString("unit", "pg/ml"),
        )
    }
    return labs
}

private fun parsePlans(array: JSONArray, source: ImportSource): List<MedicationPlan> {
    val plans = mutableListOf<MedicationPlan>()
    for (i in 0 until array.length()) {
        val obj = array.optJSONObject(i) ?: continue
        val compound = Compound.from(obj.optString("compound", obj.optString("ester", "EV")))
        val recordOnly = RecordOnlyMedication.from(obj.optString("recordOnlyMedication", ""))
        val category = MedicationCategory.from(obj.optString("category", ""))
            ?: recordOnly?.let { MedicationCategory.AntiAndrogen }
            ?: categoryForCompound(compound)
        plans += MedicationPlan(
            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
            label = obj.optString("label", ""),
            groupName = obj.optString("groupName", ""),
            category = category,
            route = Route.from(obj.optString("route", "oral")),
            compound = if (category == MedicationCategory.AntiAndrogen) Compound.E2 else compound,
            recordOnlyMedication = recordOnly,
            doseMG = obj.optDouble("doseMG", 0.0).takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0,
            timeMinutes = obj.optInt("timeMinutes", 21 * 60).coerceIn(0, 1439),
            timeMinutesList = parseTimeMinutesList(obj.optJSONArray("timeMinutesList"), obj.optInt("timeMinutes", 21 * 60)),
            repeat = PlanRepeat.from(obj.optString("repeat", "daily")),
            reminderEnabled = obj.optBoolean("reminderEnabled", true),
            systemSyncEnabled = obj.optBoolean("systemSyncEnabled", false),
            enabled = obj.optBoolean("enabled", true),
        )
    }
    return plans
}

private fun parseTimeMinutesList(array: JSONArray?, fallback: Int): List<Int> {
    val values = mutableListOf<Int>()
    if (array != null) {
        for (i in 0 until array.length()) {
            val value = array.optInt(i, -1)
            if (value in 0..1439) values += value
        }
    }
    return values.distinct().sorted().ifEmpty { listOf(fallback.coerceIn(0, 1439)) }
}

private fun parsePlanDoseRecords(array: JSONArray, source: ImportSource): List<PlanDoseRecord> {
    val records = mutableListOf<PlanDoseRecord>()
    for (i in 0 until array.length()) {
        val obj = array.optJSONObject(i) ?: continue
        val planId = obj.optString("planId", "")
        if (planId.isBlank()) continue
        val scheduled = normalizeImportedTimeH(obj.optDouble("scheduledTimeH", Double.NaN), source)
        if (!scheduled.isFinite()) continue
        val acted = if (obj.has("actedTimeH") && !obj.isNull("actedTimeH")) {
            normalizeImportedTimeH(obj.optDouble("actedTimeH", Double.NaN), source).takeIf { it.isFinite() }
        } else {
            null
        }
        records += PlanDoseRecord(
            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
            planId = planId,
            scheduledTimeH = scheduled,
            status = PlanDoseStatus.from(obj.optString("status", "pending")),
            actedTimeH = acted,
            generatedDoseId = obj.optString("generatedDoseId", "").ifBlank { null },
        )
    }
    return records
}

private fun parseBottles(array: JSONArray, source: ImportSource): List<PillBottle> {
    val bottles = mutableListOf<PillBottle>()
    for (i in 0 until array.length()) {
        val obj = array.optJSONObject(i) ?: continue
        val compound = Compound.from(obj.optString("compound", obj.optString("ester", "EV")))
        val recordOnly = RecordOnlyMedication.from(obj.optString("recordOnlyMedication", ""))
        val category = MedicationCategory.from(obj.optString("category", ""))
            ?: recordOnly?.let { MedicationCategory.AntiAndrogen }
            ?: categoryForCompound(compound)
        val expires = if (obj.has("expiresTimeH") && !obj.isNull("expiresTimeH")) {
            normalizeImportedTimeH(obj.optDouble("expiresTimeH", Double.NaN), source)
        } else {
            null
        }
        bottles += PillBottle(
            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
            name = obj.optString("name", ""),
            category = category,
            compound = if (category == MedicationCategory.AntiAndrogen) Compound.E2 else compound,
            recordOnlyMedication = recordOnly,
            unit = obj.optString("unit", "片").ifBlank { "片" },
            totalUnits = obj.optDouble("totalUnits", 0.0).takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0,
            remainingUnits = obj.optDouble("remainingUnits", 0.0).takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0,
            perDoseUnits = obj.optDouble("perDoseUnits", 1.0).takeIf { it.isFinite() && it > 0.0 } ?: 1.0,
            createdTimeH = normalizeImportedTimeH(obj.optDouble("createdTimeH", nowEpochHours()), source),
            expiresTimeH = expires,
        )
    }
    return bottles
}

private fun normalizeImportedTimeH(rawTimeH: Double, source: ImportSource): Double {
    if (!rawTimeH.isFinite()) return nowEpochHours()
    return when (source) {
        ImportSource.HrtRecorder,
        ImportSource.HrtMahiro,
        ImportSource.Legacy -> rawTimeH
    }
}

private fun normalizeImportedDoseMG(
    rawDoseMG: Double,
    category: MedicationCategory,
    compound: Compound,
    source: ImportSource,
): Double {
    if (!rawDoseMG.isFinite()) return 0.0
    if (source == ImportSource.HrtRecorder) return rawDoseMG.coerceAtLeast(0.0)
    val converted = when (category) {
        MedicationCategory.Estradiol -> rawDoseMG * toE2Factor(compound)
        MedicationCategory.Testosterone -> rawDoseMG * toActiveFactor(compound)
        MedicationCategory.Cpa,
        MedicationCategory.AntiAndrogen -> rawDoseMG
    }
    return converted.coerceAtLeast(0.0)
}

private fun eventsToJson(events: List<DoseEvent>): JSONArray {
    val array = JSONArray()
    events.forEach { event ->
        val extras = JSONObject()
        event.extras.forEach { (key, value) -> extras.put(key.wire, value) }
        array.put(
            JSONObject()
                .put("id", event.id)
                .put("category", event.category.wire)
                .put("route", event.route.wire)
                .put("timeH", event.timeH)
                .put("doseMG", event.doseMG)
                .put("ester", event.compound.wire)
                .put("compound", event.compound.wire)
                .put("recordOnlyMedication", event.recordOnlyMedication?.wire)
                .put("extras", extras),
        )
    }
    return array
}

private fun labsToJson(labs: List<LabResult>): JSONArray {
    val array = JSONArray()
    labs.forEach { lab ->
        array.put(
            JSONObject()
                .put("id", lab.id)
                .put("timeH", lab.timeH)
                .put("concValue", lab.concValue)
                .put("unit", lab.unit),
        )
    }
    return array
}

private fun plansToJson(plans: List<MedicationPlan>): JSONArray {
    val array = JSONArray()
    plans.forEach { plan ->
        array.put(
            JSONObject()
                .put("id", plan.id)
                .put("label", plan.label)
                .put("category", plan.category.wire)
                .put("route", plan.route.wire)
                .put("doseMG", plan.doseMG)
                .put("ester", plan.compound.wire)
                .put("compound", plan.compound.wire)
                .put("recordOnlyMedication", plan.recordOnlyMedication?.wire)
                .put("timeMinutes", plan.timeMinutes)
                .put("timeMinutesList", JSONArray(plan.timeMinutesList))
                .put("groupName", plan.groupName)
                .put("repeat", plan.repeat.wire)
                .put("reminderEnabled", plan.reminderEnabled)
                .put("systemSyncEnabled", plan.systemSyncEnabled)
                .put("enabled", plan.enabled),
        )
    }
    return array
}

private fun planDoseRecordsToJson(records: List<PlanDoseRecord>): JSONArray {
    val array = JSONArray()
    records.forEach { record ->
        val obj = JSONObject()
            .put("id", record.id)
            .put("planId", record.planId)
            .put("scheduledTimeH", record.scheduledTimeH)
            .put("status", record.status.wire)
            .put("generatedDoseId", record.generatedDoseId)
        if (record.actedTimeH == null) {
            obj.put("actedTimeH", JSONObject.NULL)
        } else {
            obj.put("actedTimeH", record.actedTimeH)
        }
        array.put(obj)
    }
    return array
}

private fun bottlesToJson(bottles: List<PillBottle>): JSONArray {
    val array = JSONArray()
    bottles.forEach { bottle ->
        val obj = JSONObject()
            .put("id", bottle.id)
            .put("name", bottle.name)
            .put("category", bottle.category.wire)
            .put("ester", bottle.compound.wire)
            .put("compound", bottle.compound.wire)
            .put("recordOnlyMedication", bottle.recordOnlyMedication?.wire)
            .put("unit", bottle.unit)
            .put("totalUnits", bottle.totalUnits)
            .put("remainingUnits", bottle.remainingUnits)
            .put("perDoseUnits", bottle.perDoseUnits)
            .put("createdTimeH", bottle.createdTimeH)
        if (bottle.expiresTimeH == null) {
            obj.put("expiresTimeH", JSONObject.NULL)
        } else {
            obj.put("expiresTimeH", bottle.expiresTimeH)
        }
        array.put(obj)
    }
    return array
}
