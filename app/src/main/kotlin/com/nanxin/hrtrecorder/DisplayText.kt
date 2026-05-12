package com.nanxin.hrtrecorder

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

/*
 * Android author watermark: 南盺
 * Display labels stay in one small layer so UI text can evolve without touching
 * the PK engine. Reference-only credits live in docs/code, not visible authorship.
 */

fun Route.label(language: AppLanguage): String =
    language.t(
        labelZh,
        when (this) {
            Route.Injection -> "Injection"
            Route.PatchApply -> "Patch apply"
            Route.PatchRemove -> "Patch remove"
            Route.Gel -> "Gel"
            Route.Oral -> "Oral"
            Route.Sublingual -> "Sublingual"
        },
    )

fun MedicationCategory.label(language: AppLanguage): String =
    language.t(
        labelZh,
        when (this) {
            MedicationCategory.Estradiol -> "Estradiol"
            MedicationCategory.Cpa -> "CPA"
            MedicationCategory.Testosterone -> "Testosterone"
            MedicationCategory.AntiAndrogen -> "Antiandrogen"
        },
    )

fun ThemeMode.label(language: AppLanguage): String =
    language.t(
        labelZh,
        when (this) {
            ThemeMode.System -> "Follow system"
            ThemeMode.Light -> "Light"
            ThemeMode.Dark -> "Dark"
        },
    )

fun AppLanguage.displayLabel(interfaceLanguage: AppLanguage): String =
    interfaceLanguage.t(
        nativeLabel,
        when (this) {
            AppLanguage.ZhHans -> "Simplified Chinese"
            AppLanguage.English -> "English"
        },
    )

fun displayUnit(unit: String, language: AppLanguage): String {
    val normalized = unit.trim()
    return when {
        language == AppLanguage.English && normalized == "片" -> "pcs"
        language == AppLanguage.ZhHans && normalized.equals("pcs", ignoreCase = true) -> "片"
        normalized.isBlank() -> language.t("片", "pcs")
        else -> normalized
    }
}

fun Compound.label(language: AppLanguage): String =
    language.t(
        labelZh,
        when (this) {
            Compound.E2 -> "Estradiol"
            Compound.EB -> "Estradiol benzoate"
            Compound.EV -> "Estradiol valerate"
            Compound.EPP -> "Estradiol phenylpropionate"
            Compound.EC -> "Estradiol cypionate"
            Compound.EN -> "Estradiol enanthate"
            Compound.EU -> "Estradiol undecylate"
            Compound.CPA -> "Cyproterone acetate"
            Compound.T -> "Testosterone"
            Compound.TC -> "Testosterone cypionate"
            Compound.TE -> "Testosterone enanthate"
            Compound.TU -> "Testosterone undecanoate"
        },
    )

fun RecordOnlyMedication.label(language: AppLanguage): String =
    language.t(
        labelZh,
        when (this) {
            RecordOnlyMedication.CyproteroneAcetate -> "Cyproterone acetate"
            RecordOnlyMedication.Spironolactone -> "Spironolactone"
            RecordOnlyMedication.Bicalutamide -> "Bicalutamide"
            RecordOnlyMedication.Finasteride -> "Finasteride"
            RecordOnlyMedication.Dutasteride -> "Dutasteride"
        },
    )

fun RecordOnlyMedication.shortCode(language: AppLanguage): String =
    language.t(
        when (this) {
            RecordOnlyMedication.CyproteroneAcetate -> "CPA"
            RecordOnlyMedication.Spironolactone -> "螺内"
            RecordOnlyMedication.Bicalutamide -> "比卡"
            RecordOnlyMedication.Finasteride -> "非那"
            RecordOnlyMedication.Dutasteride -> "度他"
        },
        when (this) {
            RecordOnlyMedication.CyproteroneAcetate -> "CPA"
            RecordOnlyMedication.Spironolactone -> "SP"
            RecordOnlyMedication.Bicalutamide -> "BI"
            RecordOnlyMedication.Finasteride -> "FI"
            RecordOnlyMedication.Dutasteride -> "DU"
        },
    )

fun MedicationChoice.shortCode(language: AppLanguage): String =
    when (category) {
        MedicationCategory.Estradiol -> compound.wire
        MedicationCategory.Cpa -> "CPA"
        MedicationCategory.Testosterone -> compound.wire
        MedicationCategory.AntiAndrogen -> recordOnlyMedication?.shortCode(language) ?: language.t("抗雄", "AN")
    }

fun MedicationChoice.label(language: AppLanguage): String =
    if (category == MedicationCategory.AntiAndrogen) {
        recordOnlyMedication?.label(language) ?: RecordOnlyMedication.Spironolactone.label(language)
    } else {
        "${compound.label(language)} (${compound.wire})"
    }

fun Analyte.displayLabel(language: AppLanguage, antiAndrogenLabel: String? = null): String =
    when (this) {
        Analyte.E2 -> "E2"
        Analyte.CPA -> antiAndrogenLabel?.takeIf { it.isNotBlank() } ?: language.t("抗雄", "Antiandrogen")
        Analyte.Testosterone -> "T"
    }

fun Analyte.tabLabel(language: AppLanguage): String =
    when (this) {
        Analyte.E2 -> "E2"
        Analyte.CPA -> language.t("抗雄", "Anti")
        Analyte.Testosterone -> "T"
    }

fun medicationDisplayName(
    category: MedicationCategory,
    compound: Compound,
    recordOnlyMedication: RecordOnlyMedication?,
    language: AppLanguage,
): String =
    when (category) {
        MedicationCategory.AntiAndrogen -> recordOnlyMedication?.label(language)
            ?: language.t("抗雄", "Antiandrogen")
        else -> "${compound.label(language)} (${compound.wire})"
    }

fun MedicationPlan.displayName(language: AppLanguage): String =
    label.ifBlank { medicationDisplayName(category, compound, recordOnlyMedication, language) }

fun PillBottle.displayName(language: AppLanguage): String =
    name.ifBlank { medicationDisplayName(category, compound, recordOnlyMedication, language) }

fun formatPlanTime(timeMinutes: Int): String {
    val normalized = ((timeMinutes % 1440) + 1440) % 1440
    return "%02d:%02d".format(Locale.US, normalized / 60, normalized % 60)
}

fun antiAndrogenDisplayLabel(events: List<DoseEvent>, language: AppLanguage): String {
    val names = events
        .filter { eventAnalyte(it) == Analyte.CPA }
        .mapNotNull {
            when (it.category) {
                MedicationCategory.Cpa -> "CPA"
                MedicationCategory.AntiAndrogen -> it.recordOnlyMedication?.label(language)
                else -> null
            }
        }
        .distinct()
    return when {
        names.isEmpty() -> language.t("抗雄", "Antiandrogen")
        names.size == 1 -> names.first()
        names.contains("CPA") -> "CPA+"
        else -> language.t("抗雄", "Antiandrogen")
    }
}

fun antiAndrogenReferenceMedication(events: List<DoseEvent>): RecordOnlyMedication? {
    val medications = events
        .filter { eventAnalyte(it) == Analyte.CPA }
        .mapNotNull {
            when (it.category) {
                MedicationCategory.Cpa -> RecordOnlyMedication.CyproteroneAcetate
                MedicationCategory.AntiAndrogen -> it.recordOnlyMedication
                else -> null
            }
        }
        .distinct()
    return medications.singleOrNull()
}

fun formatDateLabel(timeH: Double, language: AppLanguage = AppLanguage.ZhHans): String {
    val local = LocalDateTime.ofInstant(Instant.ofEpochMilli((timeH * 3_600_000.0).toLong()), ZoneId.systemDefault())
    return if (language == AppLanguage.English) {
        "%02d/%02d".format(Locale.US, local.monthValue, local.dayOfMonth)
    } else {
        "${local.monthValue}月${local.dayOfMonth}日"
    }
}

fun formatWallClock(timeH: Double): String {
    val local = LocalDateTime.ofInstant(Instant.ofEpochMilli((timeH * 3_600_000.0).toLong()), ZoneId.systemDefault())
    return "%02d:%02d".format(Locale.US, local.hour, local.minute)
}

fun doseDisplayText(event: DoseEvent, language: AppLanguage = AppLanguage.ZhHans): String =
    when {
        event.route == Route.PatchRemove -> language.t("贴片移除记录", "Patch removal")
        event.route == Route.PatchApply -> {
            val release = event.extras[ExtraKey.ReleaseRateUGPerDay]
            val totalUg = storedDoseToInputMG(event) * 1000.0
            val releaseText = release?.takeIf { it.isFinite() && it > 0.0 }?.let { "${formatNumber(it, 1)} ug/day" }
                ?: language.t("未填写释放量", "release not set")
            language.t("释放：", "Release: ") + "$releaseText · ${formatNumber(totalUg, 1)} ug"
        }
        event.category == MedicationCategory.Estradiol -> {
            val rawDose = storedDoseToInputMG(event)
            val equivalent = "${formatNumber(event.doseMG, 3)} mg E2-eq"
            val gelSite = if (event.route == Route.Gel) {
                " · " + gelSiteLabel(event.extras[ExtraKey.GelSite], language)
            } else {
                ""
            }
            val doseText = if (event.compound == Compound.E2) {
                equivalent
            } else {
                "${formatNumber(rawDose, 3)} mg ($equivalent)"
            }
            language.t("剂量：", "Dose: ") + doseText + gelSite
        }
        event.category == MedicationCategory.Cpa -> language.t("剂量：", "Dose: ") + "${formatNumber(event.doseMG, 3)} mg CPA"
        event.category == MedicationCategory.Testosterone -> {
            val rawDose = storedDoseToInputMG(event)
            val equivalent = "${formatNumber(event.doseMG, 3)} mg T-eq"
            language.t("剂量：", "Dose: ") + if (event.compound == Compound.T) {
                equivalent
            } else {
                "${formatNumber(rawDose, 3)} mg ($equivalent)"
            }
        }
        else -> {
            val deducted = event.extras[ExtraKey.BottleDeductedUnits]?.takeIf { it.isFinite() && it > 0.0 }
            val base = language.t("剂量：", "Dose: ") + "${formatNumber(event.doseMG, 3)} mg"
            deducted?.let { "$base · ${language.t("药瓶扣除", "bottle")} ${formatNumber(it, 2)}" } ?: base
        }
    }

fun storedDoseToInputMG(event: DoseEvent): Double =
    when (event.category) {
        MedicationCategory.Estradiol -> event.doseMG / toE2Factor(event.compound).coerceAtLeast(1e-9)
        MedicationCategory.Testosterone -> event.doseMG / toActiveFactor(event.compound).coerceAtLeast(1e-9)
        MedicationCategory.Cpa,
        MedicationCategory.AntiAndrogen -> event.doseMG
    }.takeIf { it.isFinite() && it > 0.0 } ?: event.doseMG.coerceAtLeast(0.0)

fun gelSiteLabel(raw: Double?, language: AppLanguage): String =
    when (raw?.toInt()) {
        1 -> language.t("大腿", "Thigh")
        2 -> language.t("阴囊", "Scrotal")
        else -> language.t("手臂", "Arm")
    }

fun shareFileName(selectedTimeH: Double): String =
    "hrt-recorder-${formatWallTime(selectedTimeH).replace(":", "").replace(" ", "-")}.png"

fun cupNote(result: CupCalculatorResult, language: AppLanguage): String =
    when {
        result.diffCm < 5.0 -> language.t(
            "胸围差较小，可能更适合小背心或无钢圈款式。",
            "The bust-underbust difference is small; bralettes or wireless styles may fit better.",
        )
        result.diffCm > 20.0 -> language.t(
            "胸围差超出当前预设区间，建议结合试穿与品牌尺码表判断。",
            "The difference is above the preset range. Compare brand charts and fit comfort.",
        )
        else -> language.t(
            "不同品牌版型差异很大，最终仍建议以试穿舒适度为准。",
            "Brand sizing varies a lot; fit comfort should still decide.",
        )
    }

fun shareText(
    analyte: Analyte,
    value: Double,
    language: AppLanguage = AppLanguage.ZhHans,
    antiAndrogenLabel: String? = null,
): String {
    val name = if (language == AppLanguage.English) {
        when (analyte) {
            Analyte.E2 -> "estradiol"
            Analyte.CPA -> antiAndrogenLabel?.takeIf { it.isNotBlank() } ?: "antiandrogen"
            Analyte.Testosterone -> "testosterone"
        }
    } else {
        when (analyte) {
            Analyte.E2 -> "雌二醇"
            Analyte.CPA -> antiAndrogenLabel?.takeIf { it.isNotBlank() } ?: "抗雄"
            Analyte.Testosterone -> "睾酮"
        }
    }
    return language.t(
        "我在 HRT Recorder 记录到当前估算${name}浓度达到了 ${formatNumber(value, 1)} ${analyte.unit}！ #HRTRecorder",
        "My estimated $name level in HRT Recorder reached ${formatNumber(value, 1)} ${analyte.unit}! #HRTRecorder",
    )
}

fun cupShareText(result: CupCalculatorResult, language: AppLanguage = AppLanguage.ZhHans): String =
    language.t(
        "\u6211\u5728 HRT Recorder \u8ba1\u7b97\u5230\u5f53\u524d\u7f69\u676f\u53c2\u8003\u4e3a ${result.displaySize}\uff01 #HRTRecorder",
        "My HRT Recorder cup-size reference is ${result.displaySize}! #HRTRecorder",
    )
