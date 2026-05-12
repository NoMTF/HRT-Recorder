// Android 版作者：南盺
// Calibration references: HRT-Recorder-online TypeScript EKF / OU-Kalman implementation.
package com.nanxin.hrtrecorder

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

private const val EKF_RLOG = 0.04
private const val EKF_EPS = 0.1
private const val EKF_CHI2_95 = 3.841
private const val EKF_DELTA_K = 0.01
private const val EKF_CI_MAX_E2 = 5000.0
private const val EKF_SIGMA_RESIDUAL_LOG = 0.27
private const val EKF_Q_REF_PERIOD_H = 30.0 * 24.0

private const val OU_TAU = 0.198
private val OU_THETA = ln(2.0) / (7.0 * 24.0)
private const val OU_SIGMA = 0.02
private const val OU_MU = 0.0

private data class CalMatrix2(
    val a00: Double,
    val a01: Double,
    val a10: Double,
    val a11: Double,
)

private val EKF_INITIAL_COV = CalMatrix2(0.25, 0.0, 0.0, 0.09)
private val EKF_Q = CalMatrix2(0.0004, 0.0, 0.0, 0.0001)

data class ResidualAnchor(
    val timeH: Double,
    val logRatio: Double,
    val weight: Double,
    val kind: String = "lab",
)

data class PersonalModelState(
    val thetaMean0: Double = 0.0,
    val thetaMean1: Double = 0.0,
    val cov00: Double = 0.25,
    val cov01: Double = 0.0,
    val cov10: Double = 0.0,
    val cov11: Double = 0.09,
    val anchors: List<ResidualAnchor> = emptyList(),
    val observationCount: Int = 0,
    val postDoseObservationCount: Int = 0,
    val baselinePGmL: Double? = null,
)

data class EKFDiagnostics(
    val nis: Double,
    val isOutlier: Boolean,
    val residualLog: Double,
    val predictedPGmL: Double,
    val observedPGmL: Double,
    val ci95Low: Double,
    val ci95High: Double,
    val convergenceScore: Double,
    val thetaS: Double,
    val thetaK: Double,
)

data class PersonalReplayResult(
    val state: PersonalModelState,
    val diagnostics: EKFDiagnostics?,
)

data class SimulationWithCI(
    val timeH: DoubleArray,
    val e2Adjusted: DoubleArray,
    val ci95Low: DoubleArray,
    val ci95High: DoubleArray,
    val ci68Low: DoubleArray,
    val ci68High: DoubleArray,
)

data class CalibrationSummary(
    val replay: PersonalReplayResult,
    val calibrated: SimulationWithCI?,
    val model: String,
) {
    val hasPostDoseCalibration: Boolean
        get() = replay.state.postDoseObservationCount > 0 && calibrated != null
}

private data class CalPKParams(
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

private val calFracFast = mapOf(
    Compound.EB to 0.90,
    Compound.EV to 0.40,
    Compound.EPP to 0.55,
    Compound.EC to 0.229164549,
    Compound.EN to 0.05,
    Compound.EU to 0.08,
    Compound.E2 to 1.0,
)

private val calK1Fast = mapOf(
    Compound.EB to 0.144,
    Compound.EV to 0.0216,
    Compound.EPP to 0.038,
    Compound.EC to 0.005035046,
    Compound.EN to 0.0010,
    Compound.EU to 0.006,
    Compound.E2 to 0.0,
)

private val calK1Slow = mapOf(
    Compound.EB to 0.114,
    Compound.EV to 0.0138,
    Compound.EPP to 0.010,
    Compound.EC to 0.004510574,
    Compound.EN to 0.0050,
    Compound.EU to 0.0022,
    Compound.E2 to 0.0,
)

private val calFormationFraction = mapOf(
    Compound.EB to 0.1092,
    Compound.EV to 0.0623,
    Compound.EPP to 0.075,
    Compound.EC to 0.1173,
    Compound.EN to 0.12,
    Compound.EU to 0.04,
    Compound.E2 to 1.0,
)

private val calEsterK2 = mapOf(
    Compound.EB to 0.090,
    Compound.EV to 0.070,
    Compound.EPP to 0.060,
    Compound.EC to 0.045,
    Compound.EN to 0.015,
    Compound.EU to 0.012,
    Compound.E2 to 0.0,
)

fun convertToPgMl(value: Double, unit: String): Double =
    if (unit.lowercase().contains("pmol")) value / 3.671 else value

private fun resolveE2CalibrationParams(event: DoseEvent): CalPKParams {
    val compound = event.compound
    val k3 = if (event.route == Route.Injection) 0.041 else 0.41
    return when (event.route) {
        Route.Injection -> {
            if (compound == Compound.E2) {
                return CalPKParams(
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
            val form = calFormationFraction[compound] ?: 0.08
            val f = form
            CalPKParams(
                fracFast = calFracFast[compound] ?: 1.0,
                k1Fast = calK1Fast[compound] ?: 0.0,
                k1Slow = calK1Slow[compound] ?: 0.0,
                k2 = calEsterK2[compound] ?: 0.0,
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
                CalPKParams(1.0, 0.0, 0.0, 0.0, k3, 1.0, rate, 1.0, 1.0)
            } else {
                CalPKParams(1.0, 0.0075, 0.0, 0.0, k3, 1.0, 0.0, 1.0, 1.0)
            }
        }
        Route.Gel -> {
            val f = when (event.extras[ExtraKey.GelSite]?.toInt()) {
                2 -> 0.40
                else -> 0.05
            }
            CalPKParams(1.0, 0.022, 0.0, 0.0, k3, f, 0.0, f, f)
        }
        Route.Oral -> {
            val k1 = if (compound == Compound.EV) 0.05 else 0.32
            val k2 = if (compound == Compound.EV) calEsterK2[Compound.EV] ?: 0.0 else 0.0
            CalPKParams(1.0, k1, 0.0, k2, k3, 0.03, 0.0, 0.03, 0.03)
        }
        Route.Sublingual -> {
            val theta = event.extras[ExtraKey.SublingualTheta]?.coerceIn(0.0, 1.0)
                ?: when (event.extras[ExtraKey.SublingualTier]?.toInt()) {
                    0 -> 0.01
                    1 -> 0.04
                    2 -> 0.11
                    3 -> 0.18
                    else -> 0.11
                }
            val k1SlowValue = if (compound == Compound.EV) 0.05 else 0.32
            val k2 = if (compound == Compound.EV) calEsterK2[Compound.EV] ?: 0.0 else 0.0
            CalPKParams(theta, 1.8, k1SlowValue, k2, k3, 1.0, 0.0, 1.0, 0.03)
        }
        Route.PatchRemove -> CalPKParams(0.0, 0.0, 0.0, 0.0, k3, 0.0, 0.0, 0.0, 0.0)
    }
}

private fun calAnalytic3C(tau: Double, doseMG: Double, f: Double, k1: Double, k2: Double, k3: Double): Double {
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

private fun calOneCompAmount(tau: Double, doseMG: Double, params: CalPKParams): Double {
    val k1 = params.k1Fast
    if (k1 <= 0.0 || doseMG <= 0.0) return 0.0
    if (abs(k1 - params.k3) < 1e-9) {
        return doseMG * params.f * k1 * tau * exp(-params.k3 * tau)
    }
    return doseMG * params.f * k1 / (k1 - params.k3) * (exp(-params.k3 * tau) - exp(-k1 * tau))
}

private fun computeEventAmountWithKScale(
    event: DoseEvent,
    allEvents: List<DoseEvent>,
    tau: Double,
    kScale: Double,
): Double {
    if (tau < 0.0 || event.route == Route.PatchRemove || eventAnalyte(event) != Analyte.E2) return 0.0
    val params = resolveE2CalibrationParams(event)
    val k3 = params.k3 * kScale
    val scaled = params.copy(k3 = k3)
    return when (event.route) {
        Route.Injection -> {
            if (params.k2 <= 0.0) {
                return calOneCompAmount(tau, event.doseMG, scaled)
            }
            val doseFast = event.doseMG * params.fracFast
            val doseSlow = event.doseMG * (1.0 - params.fracFast)
            calAnalytic3C(tau, doseFast, params.f, params.k1Fast, params.k2, k3) +
                calAnalytic3C(tau, doseSlow, params.f, params.k1Slow, params.k2, k3)
        }
        Route.Gel, Route.Oral -> calOneCompAmount(tau, event.doseMG, scaled)
        Route.Sublingual -> {
            val doseFast = event.doseMG * params.fracFast
            val doseSlow = event.doseMG * (1.0 - params.fracFast)
            fun branch(dose: Double, bioavailability: Double, ka: Double, ke: Double, t: Double): Double {
                if (abs(ka - ke) < 1e-9) return dose * bioavailability * ka * t * exp(-ke * t)
                return dose * bioavailability * ka / (ka - ke) * (exp(-ke * t) - exp(-ka * t))
            }
            val fast = if (params.k2 > 0.0) {
                calAnalytic3C(tau, doseFast, params.fFast, params.k1Fast, params.k2, k3)
            } else {
                branch(doseFast, params.fFast, params.k1Fast, k3, tau)
            }
            val slow = branch(doseSlow, params.fSlow, params.k1Slow, k3, tau)
            fast + slow
        }
        Route.PatchApply -> {
            val wearH = (allEvents.firstOrNull { it.route == Route.PatchRemove && it.timeH > event.timeH }?.timeH
                ?: Double.MAX_VALUE) - event.timeH
            if (params.rateMGh > 0.0) {
                if (tau <= wearH) {
                    params.rateMGh / k3 * (1.0 - exp(-k3 * tau))
                } else {
                    val amountAtRemoval = params.rateMGh / k3 * (1.0 - exp(-k3 * wearH))
                    amountAtRemoval * exp(-k3 * (tau - wearH))
                }
            } else {
                val underPatch = calOneCompAmount(tau, event.doseMG, scaled)
                if (tau > wearH) {
                    val atRemoval = calOneCompAmount(wearH, event.doseMG, scaled)
                    atRemoval * exp(-k3 * (tau - wearH))
                } else {
                    underPatch
                }
            }
        }
        Route.PatchRemove -> 0.0
    }
}

fun computeE2AtTimeWithTheta(events: List<DoseEvent>, weightKg: Double, timeH: Double, theta0: Double, theta1: Double): Double {
    val scale = exp(theta0)
    val kScale = exp(theta1)
    val sorted = events.filter { eventAnalyte(it) == Analyte.E2 }.sortedBy { it.timeH }
    var totalMG = 0.0
    for (event in sorted) {
        if (event.timeH > timeH) continue
        totalMG += computeEventAmountWithKScale(event, sorted, timeH - event.timeH, kScale)
    }
    val plasmaVolML = 2.0 * weightKg.coerceAtLeast(1.0) * 1000.0
    return max(0.0, (totalMG * 1e9) / plasmaVolML * scale)
}

fun ekfUpdatePersonalModel(
    events: List<DoseEvent>,
    weightKg: Double,
    state: PersonalModelState,
    labResult: LabResult,
    prevLabTimeH: Double?,
): Pair<PersonalModelState, EKFDiagnostics> {
    val hasDoseBeforeLab = events.any {
        it.timeH <= labResult.timeH && it.route != Route.PatchRemove && eventAnalyte(it) == Analyte.E2
    }
    val observedPGmL = convertToPgMl(labResult.concValue, labResult.unit)
    val dtH = prevLabTimeH?.let { max(24.0, labResult.timeH - it) } ?: EKF_Q_REF_PERIOD_H
    val qScale = dtH / EKF_Q_REF_PERIOD_H
    val p = CalMatrix2(
        state.cov00 + EKF_Q.a00 * qScale,
        state.cov01 + EKF_Q.a01 * qScale,
        state.cov10 + EKF_Q.a10 * qScale,
        state.cov11 + EKF_Q.a11 * qScale,
    )
    val predictedPGmL = computeE2AtTimeWithTheta(events, weightKg, labResult.timeH, state.thetaMean0, state.thetaMean1)

    if (!hasDoseBeforeLab) {
        val previousBaseline = state.baselinePGmL ?: 0.0
        val previousCount = state.observationCount
        val baselinePGmL = if (previousCount == 0) {
            observedPGmL
        } else {
            (previousBaseline * previousCount + observedPGmL) / (previousCount + 1)
        }
        val initialTrace = EKF_INITIAL_COV.a00 + EKF_INITIAL_COV.a11
        val currentTrace = state.cov00 + state.cov11
        val convergenceScore = (1.0 - currentTrace / initialTrace).coerceIn(0.0, 1.0)
        val newState = state.copy(
            baselinePGmL = baselinePGmL,
            observationCount = state.observationCount + 1,
        )
        return newState to EKFDiagnostics(
            nis = 0.0,
            isOutlier = false,
            residualLog = 0.0,
            predictedPGmL = predictedPGmL,
            observedPGmL = observedPGmL,
            ci95Low = observedPGmL,
            ci95High = observedPGmL,
            convergenceScore = convergenceScore,
            thetaS = exp(state.thetaMean0),
            thetaK = exp(state.thetaMean1),
        )
    }

    val yhat = ln(max(predictedPGmL, EKF_EPS))
    val predPerturbed = computeE2AtTimeWithTheta(
        events = events,
        weightKg = weightKg,
        timeH = labResult.timeH,
        theta0 = state.thetaMean0,
        theta1 = state.thetaMean1 + EKF_DELTA_K,
    )
    val yhatPerturbed = ln(max(predPerturbed, EKF_EPS))
    val h0 = 1.0
    val h1 = (yhatPerturbed - yhat) / EKF_DELTA_K
    val baseline = state.baselinePGmL?.takeIf { it.isFinite() }?.let { max(0.0, it) } ?: 0.0
    val observedDrugPGmL = max(observedPGmL - baseline, EKF_EPS)
    val innovation = ln(observedDrugPGmL) - yhat
    val s = h0 * h0 * p.a00 + 2.0 * h0 * h1 * p.a01 + h1 * h1 * p.a11 + EKF_RLOG
    val nis = if (s > 0.0) (innovation * innovation) / s else 0.0
    val isOutlier = nis > EKF_CHI2_95
    val rEff = if (isOutlier) EKF_RLOG * 4.0 else EKF_RLOG
    val sEff = h0 * h0 * p.a00 + 2.0 * h0 * h1 * p.a01 + h1 * h1 * p.a11 + rEff
    val k0 = (p.a00 * h0 + p.a01 * h1) / sEff
    val k1 = (p.a10 * h0 + p.a11 * h1) / sEff
    val thetaNew0 = state.thetaMean0 + k0 * innovation
    val thetaNew1 = state.thetaMean1 + k1 * innovation
    val i00 = 1.0 - k0 * h0
    val i01 = -k0 * h1
    val i10 = -k1 * h0
    val i11 = 1.0 - k1 * h1
    var pn00 = i00 * p.a00 + i01 * p.a10
    var pn01 = i00 * p.a01 + i01 * p.a11
    var pn10 = i10 * p.a00 + i11 * p.a10
    var pn11 = i10 * p.a01 + i11 * p.a11
    val symmetric = (pn01 + pn10) / 2.0
    pn01 = symmetric
    pn10 = symmetric
    pn00 = max(pn00, 1e-6)
    pn11 = max(pn11, 1e-6)

    val newPredPGmL = computeE2AtTimeWithTheta(events, weightKg, labResult.timeH, thetaNew0, thetaNew1)
    val logRatioPost = ln(observedDrugPGmL) - ln(max(newPredPGmL, EKF_EPS))
    val anchors = (state.anchors + ResidualAnchor(labResult.timeH, logRatioPost, if (isOutlier) 0.3 else 1.0))
        .sortedBy { it.timeH }
        .takeLast(20)
    val varYhat = pn00 + 2.0 * pn01 * h1 + pn11 * h1 * h1
    val std95 = sqrt(max(0.0, varYhat + rEff))
    val logPredNew = ln(max(newPredPGmL, EKF_EPS))
    val ci95Low = exp(logPredNew - 1.96 * std95)
    val ci95High = exp(logPredNew + 1.96 * std95)
    val initialTrace = EKF_INITIAL_COV.a00 + EKF_INITIAL_COV.a11
    val currentTrace = pn00 + pn11
    val convergenceScore = (1.0 - currentTrace / initialTrace).coerceIn(0.0, 1.0)
    val newState = state.copy(
        thetaMean0 = thetaNew0,
        thetaMean1 = thetaNew1,
        cov00 = pn00,
        cov01 = pn01,
        cov10 = pn10,
        cov11 = pn11,
        anchors = anchors,
        observationCount = state.observationCount + 1,
        postDoseObservationCount = state.postDoseObservationCount + 1,
    )
    return newState to EKFDiagnostics(
        nis = nis,
        isOutlier = isOutlier,
        residualLog = innovation,
        predictedPGmL = predictedPGmL,
        observedPGmL = observedPGmL,
        ci95Low = ci95Low,
        ci95High = ci95High,
        convergenceScore = convergenceScore,
        thetaS = exp(thetaNew0),
        thetaK = exp(thetaNew1),
    )
}

fun replayPersonalModelWithDiagnostics(
    events: List<DoseEvent>,
    weightKg: Double,
    labResults: List<LabResult>,
): PersonalReplayResult {
    var state = PersonalModelState()
    var diagnostics: EKFDiagnostics? = null
    val sorted = labResults.sortedBy { it.timeH }
    for (index in sorted.indices) {
        val previousTime = if (index > 0) sorted[index - 1].timeH else null
        val result = ekfUpdatePersonalModel(events, weightKg, state, sorted[index], previousTime)
        state = result.first
        diagnostics = result.second
    }
    return PersonalReplayResult(state, diagnostics)
}

private data class OUKalmanCalibration(val m: DoubleArray, val p: DoubleArray)

private fun buildOUKalmanCalibration(simulation: SimulationResult, labResults: List<LabResult>): OUKalmanCalibration {
    val n = simulation.timeH.size
    if (n == 0) return OUKalmanCalibration(DoubleArray(0), DoubleArray(0))
    val tau2 = OU_TAU * OU_TAU
    val pInf = (OU_SIGMA * OU_SIGMA) / (2.0 * OU_THETA)
    val tMin = simulation.timeH.first()
    val tMax = simulation.timeH.last()
    val labs = labResults.mapNotNull { lab ->
        if (lab.timeH < tMin || lab.timeH > tMax) return@mapNotNull null
        val observed = convertToPgMl(lab.concValue, lab.unit)
        val base = interpolateConcentration(simulation, lab.timeH)
        if (observed <= 0.0 || base < EKF_EPS) return@mapNotNull null
        val z = ln(observed) - ln(base)
        if (z.isFinite() && abs(z) <= 3.5) lab.timeH to z else null
    }.sortedBy { it.first }

    val grid = (simulation.timeH.toList() + labs.map { it.first }).distinct().sorted()
    val gridIndex = grid.withIndex().associate { it.value to it.index }
    val mFwd = DoubleArray(grid.size) { OU_MU }
    val pFwd = DoubleArray(grid.size) { pInf }
    val mPred = DoubleArray(grid.size) { OU_MU }
    val pPred = DoubleArray(grid.size) { pInf }
    var mean = OU_MU
    var variance = pInf
    var labPtr = 0

    for (i in grid.indices) {
        if (i > 0) {
            val dt = grid[i] - grid[i - 1]
            if (dt > 0.0) {
                val phi = exp(-OU_THETA * dt)
                val q = pInf * (1.0 - phi * phi)
                mean = OU_MU + phi * (mean - OU_MU)
                variance = phi * phi * variance + q
            }
        }
        mPred[i] = mean
        pPred[i] = variance
        while (labPtr < labs.size && labs[labPtr].first == grid[i]) {
            val s = variance + tau2
            val k = variance / s
            mean += k * (labs[labPtr].second - mean)
            variance *= 1.0 - k
            labPtr++
        }
        mFwd[i] = mean
        pFwd[i] = max(variance, 1e-12)
    }

    val mSmooth = mFwd.copyOf()
    val pSmooth = pFwd.copyOf()
    for (i in grid.size - 2 downTo 0) {
        val dt = grid[i + 1] - grid[i]
        if (dt <= 0.0) continue
        val phi = exp(-OU_THETA * dt)
        val gain = if (pPred[i + 1] > 1e-12) pFwd[i] * phi / pPred[i + 1] else 0.0
        mSmooth[i] = mFwd[i] + gain * (mSmooth[i + 1] - mPred[i + 1])
        pSmooth[i] = max(pFwd[i] + gain * gain * (pSmooth[i + 1] - pPred[i + 1]), 1e-9)
    }

    val outM = DoubleArray(n)
    val outP = DoubleArray(n)
    for (i in 0 until n) {
        val index = gridIndex[simulation.timeH[i]]
        outM[i] = if (index == null) OU_MU else mSmooth[index]
        outP[i] = if (index == null) pInf else pSmooth[index]
    }
    return OUKalmanCalibration(outM, outP)
}

fun computeSimulationWithCI(
    simulation: SimulationResult,
    events: List<DoseEvent>,
    weightKg: Double,
    state: PersonalModelState,
    labResults: List<LabResult> = emptyList(),
    calibrationModel: String = "ekf",
): SimulationWithCI {
    val n = simulation.timeH.size
    val empty = SimulationWithCI(DoubleArray(0), DoubleArray(0), DoubleArray(0), DoubleArray(0), DoubleArray(0), DoubleArray(0))
    if (n == 0) return empty
    val baselinePGmL = state.baselinePGmL?.takeIf { it.isFinite() }?.let { max(0.0, it) } ?: 0.0
    val adjusted = DoubleArray(n)
    val ci95Low = DoubleArray(n)
    val ci95High = DoubleArray(n)
    val ci68Low = DoubleArray(n)
    val ci68High = DoubleArray(n)

    fun clampLow(value: Double): Double = if (value.isFinite()) min(max(0.0, value), EKF_CI_MAX_E2) else 0.0
    fun clampHigh(low: Double, value: Double): Double = if (value.isFinite()) min(max(low, value), EKF_CI_MAX_E2) else low

    if (calibrationModel == "ou-kalman") {
        val ou = buildOUKalmanCalibration(simulation, labResults)
        for (i in 0 until n) {
            val c0 = max(simulation.concentration[i], EKF_EPS)
            val mean = ou.m.getOrElse(i) { OU_MU }
            val variance = max(0.0, ou.p.getOrElse(i) { 0.0 })
            val std = sqrt(variance)
            adjusted[i] = min(baselinePGmL + c0 * exp(mean + 0.5 * variance), EKF_CI_MAX_E2)
            val low95 = clampLow(baselinePGmL + c0 * exp(mean - 1.96 * std))
            ci95Low[i] = low95
            ci95High[i] = clampHigh(low95, baselinePGmL + c0 * exp(mean + 1.96 * std))
            val low68 = clampLow(baselinePGmL + c0 * exp(mean - std))
            ci68Low[i] = low68
            ci68High[i] = clampHigh(low68, baselinePGmL + c0 * exp(mean + std))
        }
    } else {
        val sampleStep = max(1, n / 100)
        data class Sample(val idx: Int, val mean: Double, val lo95: Double, val hi95: Double, val lo68: Double, val hi68: Double)
        val samples = mutableListOf<Sample>()
        var idx = 0
        while (idx < n) {
            val time = simulation.timeH[idx]
            val e2Base = computeE2AtTimeWithTheta(events, weightKg, time, state.thetaMean0, state.thetaMean1)
            val yhat = ln(max(e2Base, EKF_EPS))
            val yhatPlus = ln(max(computeE2AtTimeWithTheta(events, weightKg, time, state.thetaMean0, state.thetaMean1 + EKF_DELTA_K), EKF_EPS))
            val h1 = (yhatPlus - yhat) / EKF_DELTA_K
            val sigma2Param = max(0.0, state.cov00 + 2.0 * state.cov01 * h1 + state.cov11 * h1 * h1)
            val sigmaTotal = sqrt(sigma2Param + EKF_SIGMA_RESIDUAL_LOG * EKF_SIGMA_RESIDUAL_LOG)
            val mean = min(baselinePGmL + e2Base * exp(0.5 * sigma2Param), EKF_CI_MAX_E2)
            val low95 = clampLow(baselinePGmL + e2Base * exp(-1.96 * sigmaTotal))
            val low68 = clampLow(baselinePGmL + e2Base * exp(-sigmaTotal))
            samples += Sample(
                idx = idx,
                mean = mean,
                lo95 = low95,
                hi95 = clampHigh(low95, baselinePGmL + e2Base * exp(1.96 * sigmaTotal)),
                lo68 = low68,
                hi68 = clampHigh(low68, baselinePGmL + e2Base * exp(sigmaTotal)),
            )
            idx += sampleStep
        }
        if (samples.lastOrNull()?.idx != n - 1) {
            val last = n - 1
            val time = simulation.timeH[last]
            val e2Base = computeE2AtTimeWithTheta(events, weightKg, time, state.thetaMean0, state.thetaMean1)
            samples += Sample(
                idx = last,
                mean = min(baselinePGmL + e2Base, EKF_CI_MAX_E2),
                lo95 = max(0.0, baselinePGmL + e2Base * 0.55),
                hi95 = min(EKF_CI_MAX_E2, baselinePGmL + e2Base * 1.8),
                lo68 = max(0.0, baselinePGmL + e2Base * 0.75),
                hi68 = min(EKF_CI_MAX_E2, baselinePGmL + e2Base * 1.35),
            )
        }
        for (j in samples.indices) {
            val a = samples[j]
            val b = samples.getOrElse(j + 1) { a }
            val span = b.idx - a.idx
            for (i in a.idx..b.idx) {
                val frac = if (span > 0) (i - a.idx).toDouble() / span else 0.0
                adjusted[i] = a.mean + (b.mean - a.mean) * frac
                ci95Low[i] = a.lo95 + (b.lo95 - a.lo95) * frac
                ci95High[i] = a.hi95 + (b.hi95 - a.hi95) * frac
                ci68Low[i] = a.lo68 + (b.lo68 - a.lo68) * frac
                ci68High[i] = a.hi68 + (b.hi68 - a.hi68) * frac
            }
        }
    }
    return SimulationWithCI(simulation.timeH.copyOf(), adjusted, ci95Low, ci95High, ci68Low, ci68High)
}

fun buildCalibrationSummary(
    events: List<DoseEvent>,
    weightKg: Double,
    labs: List<LabResult>,
    rawE2Simulation: SimulationResult?,
    calibrationModel: String,
): CalibrationSummary {
    val replay = replayPersonalModelWithDiagnostics(events, weightKg, labs)
    val calibrated = if (rawE2Simulation != null && replay.state.postDoseObservationCount > 0) {
        computeSimulationWithCI(rawE2Simulation, events, weightKg, replay.state, labs, calibrationModel)
    } else {
        null
    }
    return CalibrationSummary(replay, calibrated, calibrationModel)
}

fun calibratedSimulationResult(ci: SimulationWithCI): SimulationResult {
    var auc = 0.0
    for (i in 1 until ci.timeH.size) {
        val step = ci.timeH[i] - ci.timeH[i - 1]
        auc += 0.5 * (ci.e2Adjusted[i] + ci.e2Adjusted[i - 1]) * step
    }
    return SimulationResult(ci.timeH.copyOf(), ci.e2Adjusted.copyOf(), auc, Analyte.E2)
}
