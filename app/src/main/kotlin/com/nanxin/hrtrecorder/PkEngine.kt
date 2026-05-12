// Android 版作者：南盺
// References are source-level notes only; visible app authorship remains 南盺.
package com.nanxin.hrtrecorder

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

private data class CompoundInfo(val mw: Double, val activeMw: Double)

private val compoundInfo = mapOf(
    Compound.E2 to CompoundInfo(272.38, 272.38),
    Compound.EB to CompoundInfo(376.50, 272.38),
    Compound.EV to CompoundInfo(356.50, 272.38),
    Compound.EPP to CompoundInfo(404.55, 272.38),
    Compound.EC to CompoundInfo(396.58, 272.38),
    Compound.EN to CompoundInfo(384.56, 272.38),
    Compound.EU to CompoundInfo(440.66, 272.38),
    Compound.CPA to CompoundInfo(416.94, 416.94),
    Compound.T to CompoundInfo(288.42, 288.42),
    Compound.TC to CompoundInfo(412.61, 288.42),
    Compound.TE to CompoundInfo(400.59, 288.42),
    Compound.TU to CompoundInfo(456.70, 288.42),
)

private object CorePK {
    const val vdPerKG = 2.0
    const val kClear = 0.41
    const val kClearInjection = 0.041
    const val depotK1Corr = 1.0
}

private object TestosteroneCorePK {
    const val vdPerKG = 1.0
    const val kClear = 0.5
    const val kClearInjection = 0.035
    const val depotK1Corr = 1.0
    const val patchFallbackK1 = 0.03
    const val gelK1 = 0.05
    const val gelFmax = 0.10
}

private object CPAPK {
    const val vdPerKG = 14.0
    const val labelReferenceDoseMG = 50.0
    const val labelReferenceCmaxNgML = 140.0
    const val labelTmaxH = 3.0
    const val labelHalfLifeH = 43.9
    const val kClear = 0.0158
}

private data class AntiAndrogenComponent(
    val referenceDoseMG: Double,
    val cMaxNgML: Double,
    val tMaxH: Double,
    val halfLifeH: Double,
    val weight: Double = 1.0,
)

private data class AntiAndrogenPK(
    val components: List<AntiAndrogenComponent>,
    val vdPerKG: Double = 10.0,
)

/*
 * Android author watermark: Nanxin / 南盺.
 * Antiandrogen trend curves use a Cmax/Tmax/half-life constrained Bateman shape
 * instead of a naked one-compartment amount model. This keeps the curve smooth
 * between samples and avoids the old "vertical cliff" when the chart skips over
 * a very short absorption phase. Reference notes are in ANTIANDROGEN_PK_REFERENCES.md.
 */
private val antiAndrogenPk = mapOf(
    RecordOnlyMedication.CyproteroneAcetate to AntiAndrogenPK(
        vdPerKG = CPAPK.vdPerKG,
        components = listOf(
            // Androcur labeling anchors oral CPA at 50 mg -> about 140 ng/mL near 3 h.
            AntiAndrogenComponent(
                referenceDoseMG = CPAPK.labelReferenceDoseMG,
                cMaxNgML = CPAPK.labelReferenceCmaxNgML,
                tMaxH = CPAPK.labelTmaxH,
                halfLifeH = CPAPK.labelHalfLifeH,
            ),
        ),
    ),
    RecordOnlyMedication.Spironolactone to AntiAndrogenPK(
        vdPerKG = 6.0,
        components = listOf(
            AntiAndrogenComponent(referenceDoseMG = 100.0, cMaxNgML = 80.0, tMaxH = 2.6, halfLifeH = 1.4, weight = 0.20),
            AntiAndrogenComponent(referenceDoseMG = 100.0, cMaxNgML = 181.0, tMaxH = 4.3, halfLifeH = 16.5, weight = 1.0),
        ),
    ),
    RecordOnlyMedication.Bicalutamide to AntiAndrogenPK(
        vdPerKG = 9.0,
        components = listOf(
            AntiAndrogenComponent(referenceDoseMG = 50.0, cMaxNgML = 768.0, tMaxH = 31.3, halfLifeH = 5.8 * 24.0),
        ),
    ),
    RecordOnlyMedication.Finasteride to AntiAndrogenPK(
        vdPerKG = 1.1,
        components = listOf(
            AntiAndrogenComponent(referenceDoseMG = 1.0, cMaxNgML = 9.2, tMaxH = 1.5, halfLifeH = 4.5),
        ),
    ),
    RecordOnlyMedication.Dutasteride to AntiAndrogenPK(
        vdPerKG = 6.0,
        components = listOf(
            AntiAndrogenComponent(referenceDoseMG = 0.5, cMaxNgML = 2.4, tMaxH = 2.5, halfLifeH = 5.0 * 7.0 * 24.0),
        ),
    ),
)

private val fracFast = mapOf(
    Compound.EB to 0.90,
    Compound.EV to 0.40,
    Compound.EPP to 0.55,
    Compound.EC to 0.229164549,
    Compound.EN to 0.05,
    Compound.EU to 0.08,
    Compound.E2 to 1.0,
    Compound.CPA to 1.0,
    Compound.T to 1.0,
    Compound.TC to 0.35,
    Compound.TE to 0.40,
    Compound.TU to 0.10,
)

private val k1Fast = mapOf(
    Compound.EB to 0.144,
    Compound.EV to 0.0216,
    Compound.EPP to 0.038,
    Compound.EC to 0.005035046,
    Compound.EN to 0.0010,
    Compound.EU to 0.006,
    Compound.E2 to 0.0,
    Compound.CPA to 0.0,
    Compound.T to 0.0,
    Compound.TC to 0.025,
    Compound.TE to 0.035,
    Compound.TU to 0.008,
)

private val k1Slow = mapOf(
    Compound.EB to 0.114,
    Compound.EV to 0.0138,
    Compound.EPP to 0.010,
    Compound.EC to 0.004510574,
    Compound.EN to 0.0050,
    Compound.EU to 0.0022,
    Compound.E2 to 0.0,
    Compound.CPA to 0.0,
    Compound.T to 0.0,
    Compound.TC to 0.005,
    Compound.TE to 0.008,
    Compound.TU to 0.0009,
)

private val formationFraction = mapOf(
    Compound.EB to 0.1092,
    Compound.EV to 0.0623,
    Compound.EPP to 0.075,
    Compound.EC to 0.1173,
    Compound.EN to 0.12,
    Compound.EU to 0.04,
    Compound.E2 to 1.0,
    Compound.TC to 0.025,
    Compound.TE to 0.025,
    Compound.TU to 0.025,
    Compound.T to 1.0,
)

private val esterK2 = mapOf(
    Compound.EB to 0.090,
    Compound.EV to 0.070,
    Compound.EPP to 0.060,
    Compound.EC to 0.045,
    Compound.EN to 0.015,
    Compound.EU to 0.012,
    Compound.E2 to 0.0,
    Compound.TC to 0.200,
    Compound.TE to 0.200,
    Compound.TU to 0.200,
    Compound.T to 0.0,
    Compound.CPA to 0.0,
)

private object OralPK {
    const val kAbsE2 = 0.32
    const val kAbsEV = 0.05
    const val kAbsTU = 0.04
    const val bioavailability = 0.03
    const val kAbsSL = 1.8
}

private val hrtMahiroAnchorOffsetsH = doubleArrayOf(0.25, 0.5, 1.0, 2.0, 4.0, 6.0, 8.0, 12.0, 24.0, 48.0)

private val antiAndrogenAnchorOffsetsH = doubleArrayOf(
    0.05, 0.10, 0.20, 0.35, 0.50, 0.75,
    1.0, 1.5, 2.0, 3.0, 4.0, 6.0, 8.0,
    12.0, 16.0, 24.0, 36.0, 48.0, 72.0, 96.0, 168.0,
)

private data class PKParams(
    val fracFast: Double,
    val k1Fast: Double,
    val k1Slow: Double,
    val k2: Double,
    val k3: Double,
    val f: Double,
    val rateMGh: Double,
    val fFast: Double,
    val fSlow: Double,
)

fun toE2Factor(compound: Compound): Double {
    if (compound !in listOf(Compound.E2, Compound.EB, Compound.EV, Compound.EPP, Compound.EC, Compound.EN, Compound.EU)) return 1.0
    if (compound == Compound.E2) return 1.0
    return (compoundInfo[Compound.E2]?.mw ?: 272.38) / (compoundInfo[compound]?.mw ?: 272.38)
}

fun toActiveFactor(compound: Compound): Double {
    val info = compoundInfo[compound] ?: return 1.0
    return info.activeMw / info.mw
}

private fun resolveParams(event: DoseEvent, analyte: Analyte): PKParams {
    val compound = event.compound

    if (analyte == Analyte.CPA) {
        if (event.route != Route.Oral) {
            return PKParams(0.0, 0.0, 0.0, 0.0, CPAPK.kClear, 0.0, 0.0, 0.0, 0.0)
        }
        if (event.category == MedicationCategory.AntiAndrogen) {
            val pk = antiAndrogenPk[event.recordOnlyMedication]
                ?: return PKParams(0.0, 0.0, 0.0, 0.0, CPAPK.kClear, 0.0, 0.0, 0.0, 0.0)
            val dominant = pk.components.maxByOrNull { it.cMaxNgML * it.weight }
                ?: return PKParams(0.0, 0.0, 0.0, 0.0, CPAPK.kClear, 0.0, 0.0, 0.0, 0.0)
            val ke = ln(2.0) / dominant.halfLifeH.coerceAtLeast(1.0)
            return PKParams(1.0, kaForTmax(dominant.tMaxH, dominant.halfLifeH), 0.0, 0.0, ke, 1.0, 0.0, 1.0, 1.0)
        }
        if (compound != Compound.CPA) {
            return PKParams(0.0, 0.0, 0.0, 0.0, CPAPK.kClear, 0.0, 0.0, 0.0, 0.0)
        }
        val ka = kaForTmax(CPAPK.labelTmaxH, CPAPK.labelHalfLifeH)
        val ke = ln(2.0) / CPAPK.labelHalfLifeH
        return PKParams(1.0, ka, 0.0, 0.0, ke, 1.0, 0.0, 1.0, 1.0)
    }

    val isT = analyte == Analyte.Testosterone
    val k3 = if (event.route == Route.Injection) {
        if (isT) TestosteroneCorePK.kClearInjection else CorePK.kClearInjection
    } else {
        if (isT) TestosteroneCorePK.kClear else CorePK.kClear
    }

    return when (event.route) {
        Route.Injection -> {
            if (!isT && compound == Compound.E2) {
                return PKParams(
                    fracFast = 1.0,
                    k1Fast = 1.8,
                    k1Slow = 0.0,
                    k2 = 0.0,
                    k3 = k3,
                    f = 1.0,
                    rateMGh = 0.0,
                    fFast = 1.0,
                    fSlow = 1.0,
                )
            }
            val k1corr = if (isT) TestosteroneCorePK.depotK1Corr else CorePK.depotK1Corr
            val form = formationFraction[compound] ?: if (isT) 1.0 else 0.08
            val f = form
            PKParams(
                fracFast = fracFast[compound] ?: 1.0,
                k1Fast = (k1Fast[compound] ?: 0.0) * k1corr,
                k1Slow = (k1Slow[compound] ?: 0.0) * k1corr,
                k2 = esterK2[compound] ?: 0.0,
                k3 = k3,
                f = f,
                rateMGh = 0.0,
                fFast = f,
                fSlow = f,
            )
        }
        Route.PatchApply -> {
            val rate = event.extras[ExtraKey.ReleaseRateUGPerDay]?.let { it / 24_000.0 } ?: 0.0
            if (rate > 0.0) {
                PKParams(1.0, 0.0, 0.0, 0.0, k3, 1.0, rate, 1.0, 1.0)
            } else {
                val k1 = if (isT) TestosteroneCorePK.patchFallbackK1 else 0.0075
                PKParams(1.0, k1, 0.0, 0.0, k3, 1.0, 0.0, 1.0, 1.0)
            }
        }
        Route.Gel -> {
            val k1 = if (isT) TestosteroneCorePK.gelK1 else 0.022
            val e2GelF = when (event.extras[ExtraKey.GelSite]?.toInt()) {
                2 -> 0.40
                else -> 0.05
            }
            val f = if (isT) TestosteroneCorePK.gelFmax else e2GelF
            PKParams(1.0, k1, 0.0, 0.0, k3, f, 0.0, f, f)
        }
        Route.Oral -> {
            if (isT && compound != Compound.TU) {
                return PKParams(0.0, 0.0, 0.0, 0.0, k3, 0.0, 0.0, 0.0, 0.0)
            }
            val k1 = when (compound) {
                Compound.TU -> OralPK.kAbsTU
                Compound.EV -> OralPK.kAbsEV
                else -> OralPK.kAbsE2
            }
            val k2 = if (analyte == Analyte.E2 && compound == Compound.EV) esterK2[Compound.EV] ?: 0.0 else 0.0
            PKParams(1.0, k1, 0.0, k2, k3, OralPK.bioavailability, 0.0, OralPK.bioavailability, OralPK.bioavailability)
        }
        Route.Sublingual -> {
            if (analyte != Analyte.E2) {
                return PKParams(0.0, 0.0, 0.0, 0.0, k3, 0.0, 0.0, 0.0, 0.0)
            }
            val theta = event.extras[ExtraKey.SublingualTheta]?.coerceIn(0.0, 1.0)
                ?: when (event.extras[ExtraKey.SublingualTier]?.toInt()) {
                    0 -> 0.01
                    1 -> 0.04
                    2 -> 0.11
                    3 -> 0.18
                    else -> 0.11
                }
            val k1SlowValue = if (compound == Compound.EV) OralPK.kAbsEV else OralPK.kAbsE2
            val k2 = if (compound == Compound.EV) esterK2[Compound.EV] ?: 0.0 else 0.0
            PKParams(theta, OralPK.kAbsSL, k1SlowValue, k2, k3, 1.0, 0.0, 1.0, OralPK.bioavailability)
        }
        Route.PatchRemove -> PKParams(0.0, 0.0, 0.0, 0.0, k3, 0.0, 0.0, 0.0, 0.0)
    }
}

private fun analytic3C(tau: Double, doseMG: Double, f: Double, k1: Double, k2: Double, k3: Double): Double {
    if (k1 <= 0.0 || doseMG <= 0.0) return 0.0
    val k1k2 = k1 - k2
    val k1k3 = k1 - k3
    val k2k3 = k2 - k3
    if (abs(k1k2) < 1e-9 || abs(k1k3) < 1e-9 || abs(k2k3) < 1e-9) return 0.0
    val term1 = exp(-k1 * tau) / (k1k2 * k1k3)
    val term2 = exp(-k2 * tau) / (-k1k2 * k2k3)
    val term3 = exp(-k3 * tau) / (k1k3 * k2k3)
    return doseMG * f * k1 * k2 * (term1 + term2 + term3)
}

private fun oneCompAmount(tau: Double, doseMG: Double, params: PKParams): Double {
    val k1 = params.k1Fast
    if (abs(k1 - params.k3) < 1e-9) {
        return doseMG * params.f * k1 * tau * exp(-params.k3 * tau)
    }
    return doseMG * params.f * k1 / (k1 - params.k3) * (exp(-params.k3 * tau) - exp(-k1 * tau))
}

private fun batemanShape(tau: Double, ka: Double, ke: Double): Double {
    if (tau < 0.0) return 0.0
    if (abs(ka - ke) < 1e-9) return ka * tau * exp(-ke * tau)
    return ka / (ka - ke) * (exp(-ke * tau) - exp(-ka * tau))
}

private fun kaForTmax(tMaxH: Double, halfLifeH: Double): Double {
    val target = tMaxH.coerceIn(0.35, 96.0)
    val ke = ln(2.0) / halfLifeH.coerceAtLeast(0.25)
    var low = ke * 1.0001
    var high = max(ke * 2.0, 0.02)
    fun predicted(ka: Double): Double = ln(ka / ke) / (ka - ke)
    var guard = 0
    while (predicted(high) > target && high < 12.0 && guard++ < 80) high *= 1.7
    repeat(80) {
        val mid = (low + high) / 2.0
        if (predicted(mid) > target) low = mid else high = mid
    }
    return ((low + high) / 2.0).coerceIn(ke * 1.0001, 12.0)
}

private fun antiAndrogenConcentrationNgML(tau: Double, doseMG: Double, medication: RecordOnlyMedication): Double {
    val pk = antiAndrogenPk[medication] ?: return 0.0
    return pk.components.sumOf { component ->
        val ka = kaForTmax(component.tMaxH, component.halfLifeH)
        val ke = ln(2.0) / component.halfLifeH.coerceAtLeast(0.25)
        val peak = batemanShape(component.tMaxH, ka, ke).coerceAtLeast(1e-9)
        val normalized = batemanShape(tau, ka, ke) / peak
        val doseScale = doseMG.coerceAtLeast(0.0) / component.referenceDoseMG.coerceAtLeast(1e-9)
        component.cMaxNgML * doseScale * component.weight * normalized
    }.takeIf { it.isFinite() } ?: 0.0
}

private class EventModel(event: DoseEvent, allEvents: List<DoseEvent>, analyte: Analyte, weightKg: Double) {
    val startTimeH = event.timeH
    private val dose = event.doseMG
    private val route = event.route
    private val category = event.category
    private val recordOnlyMedication = event.recordOnlyMedication
    val needsDenseAnchors = analyte == Analyte.CPA &&
        route == Route.Oral &&
        (category == MedicationCategory.Cpa || recordOnlyMedication == RecordOnlyMedication.CyproteroneAcetate)
    private val params = resolveParams(event, analyte)
    private val localPlasmaVolumeML = when (analyte) {
        Analyte.CPA -> {
            val medication = when {
                category == MedicationCategory.Cpa -> RecordOnlyMedication.CyproteroneAcetate
                category == MedicationCategory.AntiAndrogen -> recordOnlyMedication
                else -> null
            }
            val vd = antiAndrogenPk[medication]?.vdPerKG ?: CPAPK.vdPerKG
            vd * weightKg.coerceAtLeast(1.0) * 1000.0
        }
        Analyte.Testosterone -> TestosteroneCorePK.vdPerKG * weightKg.coerceAtLeast(1.0) * 1000.0
        Analyte.E2 -> CorePK.vdPerKG * weightKg.coerceAtLeast(1.0) * 1000.0
    }
    private val localConcentrationScale = when (analyte) {
        Analyte.CPA -> 1e6
        Analyte.Testosterone -> 1e8
        Analyte.E2 -> 1e9
    }
    private val wearH: Double = if (route == Route.PatchApply) {
        (allEvents.firstOrNull { it.route == Route.PatchRemove && it.timeH > startTimeH }?.timeH ?: Double.POSITIVE_INFINITY) - startTimeH
    } else {
        Double.POSITIVE_INFINITY
    }
    val maxLifetimeH: Double = computeMaxLifetimeH()

    private fun computeMaxLifetimeH(): Double {
        if (route == Route.PatchApply) {
            if (!wearH.isFinite()) return Double.POSITIVE_INFINITY
            val tailH = if (params.k3 > 0.0) ceil(13.816 / params.k3) else 10_000.0
            return wearH + tailH
        }
        val rates = listOf(params.k1Fast, params.k1Slow, params.k2, params.k3).filter { it > 0.0 }
        if (rates.isEmpty()) return Double.POSITIVE_INFINITY
        return ceil(13.816 / rates.minOrNull()!!)
    }

    fun amount(timeH: Double): Double {
        val tau = timeH - startTimeH
        if (tau < 0.0 || tau > maxLifetimeH) return 0.0
        return when (route) {
            Route.Injection -> {
                if (params.k2 <= 0.0) {
                    return oneCompAmount(tau, dose, params)
                }
                val doseFast = dose * params.fracFast
                val doseSlow = dose * (1.0 - params.fracFast)
                analytic3C(tau, doseFast, params.f, params.k1Fast, params.k2, params.k3) +
                    analytic3C(tau, doseSlow, params.f, params.k1Slow, params.k2, params.k3)
            }
            Route.Gel, Route.Oral -> {
                val medication = when {
                    route == Route.Oral && category == MedicationCategory.Cpa -> RecordOnlyMedication.CyproteroneAcetate
                    route == Route.Oral && category == MedicationCategory.AntiAndrogen -> recordOnlyMedication
                    else -> null
                }
                if (medication != null) {
                    antiAndrogenConcentrationNgML(tau, dose, medication) * localPlasmaVolumeML / localConcentrationScale
                } else {
                    oneCompAmount(tau, dose, params)
                }
            }
            Route.Sublingual -> {
                val doseFast = dose * params.fracFast
                val doseSlow = dose * (1.0 - params.fracFast)
                fun branch(d: Double, f: Double, ka: Double, ke: Double, t: Double): Double {
                    if (abs(ka - ke) < 1e-9) return d * f * ka * t * exp(-ke * t)
                    return d * f * ka / (ka - ke) * (exp(-ke * t) - exp(-ka * t))
                }
                val fast = if (params.k2 > 0.0) {
                    analytic3C(tau, doseFast, params.fFast, params.k1Fast, params.k2, params.k3)
                } else {
                    branch(doseFast, params.fFast, params.k1Fast, params.k3, tau)
                }
                val slow = branch(doseSlow, params.fSlow, params.k1Slow, params.k3, tau)
                fast + slow
            }
            Route.PatchApply -> {
                if (params.rateMGh > 0.0) {
                    if (tau <= wearH) {
                        params.rateMGh / params.k3 * (1.0 - exp(-params.k3 * tau))
                    } else {
                        val amountAtRemoval = params.rateMGh / params.k3 * (1.0 - exp(-params.k3 * wearH))
                        amountAtRemoval * exp(-params.k3 * (tau - wearH))
                    }
                } else {
                    val underPatch = oneCompAmount(tau, dose, params)
                    if (tau > wearH) {
                        val atRemoval = oneCompAmount(wearH, dose, params)
                        atRemoval * exp(-params.k3 * (tau - wearH))
                    } else {
                        underPatch
                    }
                }
            }
            Route.PatchRemove -> 0.0
        }
    }
}

fun runSimulationForAnalyte(events: List<DoseEvent>, weightKg: Double, analyte: Analyte): SimulationResult? {
    val simulatedEvents = events.filter { eventAnalyte(it) == analyte }
    if (simulatedEvents.isEmpty()) return null

    val sorted = simulatedEvents.sortedBy { it.timeH }
    val models = sorted.filter { it.route != Route.PatchRemove }.map { EventModel(it, sorted, analyte, weightKg) }
    val nowH = nowEpochHours()
    val startTime = sorted.first().timeH - 24.0
    val endTime = max(sorted.last().timeH + 24.0 * 14.0, nowH + 24.0)
    val totalHours = max(1.0, endTime - startTime)
    val baseSteps = min(5000, max(2000, ceil(totalHours).toInt()))
    val baseStepSize = (endTime - startTime) / (baseSteps - 1)
    val sampleTimes = java.util.TreeSet<Double>()
    for (i in 0 until baseSteps) {
        sampleTimes += startTime + i * baseStepSize
    }
    sorted.forEach { event ->
        if (event.timeH in startTime..endTime) sampleTimes += event.timeH
    }
    models.forEach { model ->
        if (!(model.maxLifetimeH.isFinite() && endTime - model.startTimeH > 2.0 * model.maxLifetimeH)) {
            val anchors = if (model.needsDenseAnchors) antiAndrogenAnchorOffsetsH else hrtMahiroAnchorOffsetsH
            anchors.forEach { offset ->
                val anchor = model.startTimeH + offset
                if (anchor in startTime..endTime) sampleTimes += anchor
            }
        }
    }
    val vdPerKg = when (analyte) {
        Analyte.CPA -> {
            val antiVd = simulatedEvents.mapNotNull { antiAndrogenPk[it.recordOnlyMedication]?.vdPerKG }
            if (antiVd.isNotEmpty()) antiVd.average() else CPAPK.vdPerKG
        }
        Analyte.Testosterone -> TestosteroneCorePK.vdPerKG
        Analyte.E2 -> CorePK.vdPerKG
    }
    val concentrationScale = when (analyte) {
        Analyte.CPA -> 1e6
        Analyte.Testosterone -> 1e8
        Analyte.E2 -> 1e9
    }
    val plasmaVolumeML = vdPerKg * weightKg.coerceAtLeast(1.0) * 1000.0
    val times = DoubleArray(sampleTimes.size)
    val concentrations = DoubleArray(sampleTimes.size)
    var auc = 0.0

    var i = 0
    for (t in sampleTimes) {
        var totalAmountMg = 0.0
        for (model in models) {
            val tau = t - model.startTimeH
            if (tau >= 0.0 && tau <= model.maxLifetimeH) totalAmountMg += model.amount(t)
        }
        val concentration = max(0.0, (totalAmountMg * concentrationScale) / plasmaVolumeML)
        times[i] = t
        concentrations[i] = concentration
        if (i > 0) auc += 0.5 * (concentration + concentrations[i - 1]) * (t - times[i - 1])
        i += 1
    }

    return SimulationResult(times, concentrations, auc, analyte)
}

fun interpolateConcentration(simulation: SimulationResult?, hour: Double): Double {
    simulation ?: return 0.0
    val times = simulation.timeH
    val values = simulation.concentration
    if (times.isEmpty() || values.isEmpty()) return 0.0
    if (hour <= times.first()) return max(0.0, values.first())
    if (hour >= times.last()) return max(0.0, values.last())

    var low = 0
    var high = times.lastIndex
    while (high - low > 1) {
        val mid = (low + high) / 2
        when {
            times[mid] == hour -> return max(0.0, values[mid])
            times[mid] < hour -> low = mid
            else -> high = mid
        }
    }
    val t0 = times[low]
    val t1 = times[high]
    val c0 = values[low]
    val c1 = values[high]
    if (t1 == t0) return max(0.0, c0)
    return max(0.0, c0 + (c1 - c0) * ((hour - t0) / (t1 - t0)))
}
