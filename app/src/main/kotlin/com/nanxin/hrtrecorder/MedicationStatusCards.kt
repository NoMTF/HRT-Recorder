package com.nanxin.hrtrecorder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/*
 * Android author watermark: Nanxin.
 * Compact home status cards mirror the "what did I take / what is next" mental
 * model without changing PK calculations.
 */

@Composable
fun MedicationStatusCards(
    summaries: List<MedicationStatusSummary>,
    language: AppLanguage,
    nowH: Double,
    onOpenPlan: () -> Unit,
) {
    NativeCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(language.t("今日用药状态", "Medication Status"), color = Ink, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                Text(language.t("上次与下次，一眼确认", "Last and next dose at a glance"), color = Muted, fontSize = 12.sp)
            }
            Text(
                language.t("去计划", "Plans"),
                color = TBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.clickable(onClick = onOpenPlan).padding(8.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            summaries.forEach { summary ->
                MedicationStatusRow(summary = summary, language = language, nowH = nowH)
            }
        }
    }
}

@Composable
private fun MedicationStatusRow(
    summary: MedicationStatusSummary,
    language: AppLanguage,
    nowH: Double,
) {
    val accent = analyteColor(summary.analyte)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.64f), RoundedCornerShape(26.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DrugGlyphBadge(
            route = summary.sourcePlan?.route ?: summary.fallbackRoute(),
            category = summary.sourcePlan?.category ?: summary.fallbackCategory(),
            accent = accent,
            sizeDp = 54,
        )
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(summary.title, color = accent, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                Text(summary.routeName, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                summary.medicationName,
                color = Ink,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(summary.doseText, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            StatusTimePill(label = language.t("上次", "Last"), timeH = summary.lastTimeH, nowH = nowH, language = language, accent = accent)
            Spacer(Modifier.height(6.dp))
            StatusTimePill(label = language.t("下次", "Next"), timeH = summary.nextTimeH, nowH = nowH, language = language, accent = TBlue)
        }
    }
}

@Composable
private fun StatusTimePill(
    label: String,
    timeH: Double?,
    nowH: Double,
    language: AppLanguage,
    accent: Color,
) {
    Box(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.56f), RoundedCornerShape(99.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$label ${relativeDoseTime(timeH, nowH, language)}",
            color = if (timeH == null) Muted else accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
        )
    }
}

private fun MedicationStatusSummary.statusIcon(): String =
    when (analyte) {
        Analyte.E2 -> "E2"
        Analyte.CPA -> "抗"
        Analyte.Testosterone -> "T"
    }

private fun MedicationStatusSummary.fallbackRoute(): Route =
    when (analyte) {
        Analyte.E2 -> Route.Injection
        Analyte.CPA -> Route.Oral
        Analyte.Testosterone -> Route.Injection
    }

private fun MedicationStatusSummary.fallbackCategory(): MedicationCategory =
    when (analyte) {
        Analyte.E2 -> MedicationCategory.Estradiol
        Analyte.CPA -> MedicationCategory.AntiAndrogen
        Analyte.Testosterone -> MedicationCategory.Testosterone
    }

private fun relativeDoseTime(timeH: Double?, nowH: Double, language: AppLanguage): String {
    if (timeH == null || !timeH.isFinite()) return language.t("暂无", "None")
    val deltaMinutes = ((timeH - nowH) * 60.0).toLong()
    val absMinutes = abs(deltaMinutes)
    val value = when {
        absMinutes < 2 -> language.t("刚刚", "now")
        absMinutes < 60 -> {
            val n = absMinutes.coerceAtLeast(1)
            language.t("${n}分钟", "${n}m")
        }
        absMinutes < 24 * 60 -> {
            val n = (absMinutes / 60).coerceAtLeast(1)
            language.t("${n}小时", "${n}h")
        }
        else -> {
            val n = (absMinutes / (24 * 60)).coerceAtLeast(1)
            language.t("${n}天", "${n}d")
        }
    }
    return when {
        absMinutes < 2 -> value
        deltaMinutes >= 0 -> language.t("${value}后", "in $value")
        else -> language.t("${value}前", "$value ago")
    }
}
