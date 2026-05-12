// Android 版作者：南盺
// Canvas chart renderer split out so overview state changes don't make MainActivity a rendering dump.
package com.nanxin.hrtrecorder

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun ResultChart(
    analyte: Analyte,
    simulation: SimulationResult?,
    selectedTimeH: Double,
    onTimeSelected: (Double) -> Unit,
    onResetTime: () -> Unit,
    color: Color,
    unit: String,
    chartLabel: String,
    language: AppLanguage,
) {
    val palette = LocalAppPalette.current
    var rangeHours by remember { mutableStateOf(72.0) }
    var tooltipTimeH by remember { mutableStateOf<Double?>(selectedTimeH) }
    var showCustomRangeDialog by remember { mutableStateOf(false) }
    var customRangeText by remember { mutableStateOf("72") }
    LaunchedEffect(analyte, selectedTimeH) {
        tooltipTimeH = selectedTimeH
    }
    val rangeOptions = remember(language) {
        listOf(
            336.0 to language.t("14天", "14d"),
            168.0 to language.t("7天", "7d"),
            72.0 to language.t("72小时", "72h"),
            48.0 to language.t("48小时", "48h"),
            24.0 to language.t("24小时", "24h"),
            12.0 to language.t("12小时", "12h"),
        )
    }
    val isCustomRange = remember(rangeHours, rangeOptions) { rangeOptions.none { it.first == rangeHours } }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rangeOptions.forEach { (hours, label) ->
            AssistChip(
                onClick = { rangeHours = hours },
                label = { Text(label) },
                border = null,
                modifier = if (rangeHours == hours) Modifier.background(Color.Transparent) else Modifier,
            )
        }
        AssistChip(
            onClick = {
                customRangeText = formatNumber(rangeHours, if (rangeHours % 1.0 == 0.0) 0 else 1)
                showCustomRangeDialog = true
            },
            label = {
                Text(
                    if (isCustomRange) {
                        language.t("自定义 ${formatNumber(rangeHours, 1)}h", "Custom ${formatNumber(rangeHours, 1)}h")
                    } else {
                        language.t("自定义", "Custom")
                    },
                )
            },
            border = null,
        )
        AssistChip(
            onClick = {
                tooltipTimeH = selectedTimeH
                onResetTime()
            },
            label = { Text(language.t("当前", "Now")) },
            border = null,
        )
    }
    if (showCustomRangeDialog) {
        val parsedHours = customRangeText.toDoubleOrNull()?.takeIf { it in 1.0..8760.0 }
        AlertDialog(
            onDismissRequest = { showCustomRangeDialog = false },
            title = { Text(language.t("自定义图表范围", "Custom Chart Range"), color = Ink, fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        language.t(
                            "输入要显示的时间范围，单位为小时。图表会以当前估算时间为中心展开。",
                            "Enter the visible time range in hours. The chart stays centered on the selected estimate time.",
                        ),
                        color = Muted,
                        lineHeight = 20.sp,
                    )
                    OutlinedTextField(
                        value = customRangeText,
                        onValueChange = { customRangeText = it },
                        label = { Text(language.t("范围", "Range")) },
                        suffix = { Text(language.t("小时", "h")) },
                        singleLine = true,
                        isError = parsedHours == null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        parsedHours?.let {
                            rangeHours = it
                            showCustomRangeDialog = false
                        }
                    },
                    enabled = parsedHours != null,
                    shape = RoundedCornerShape(16.dp),
                ) { Text(language.t("应用", "Apply")) }
            },
            dismissButton = {
                TextButton(onClick = { showCustomRangeDialog = false }) { Text(language.t("取消", "Cancel")) }
            },
        )
    }
    Spacer(Modifier.height(16.dp))
    val density = LocalDensity.current
    val latestSelectedTime by rememberUpdatedState(selectedTimeH)
    val latestOnTimeSelected by rememberUpdatedState(onTimeSelected)
    val unitLabel = remember(unit) { unit.uppercase(Locale.ROOT) }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(palette.chartSurface, RoundedCornerShape(24.dp))
            .pointerInput(rangeHours) {
                detectTapGestures { offset ->
                    val anchorTime = latestSelectedTime
                    val start = anchorTime - rangeHours / 2.0
                    val end = anchorTime + rangeHours / 2.0
                    val plotLeft = 48.dp.toPx()
                    val plotRight = size.width - 16.dp.toPx()
                    val ratio = ((offset.x - plotLeft) / (plotRight - plotLeft)).coerceIn(0f, 1f)
                    val pickedTimeH = start + (end - start) * ratio
                    tooltipTimeH = pickedTimeH
                    latestOnTimeSelected(pickedTimeH)
                }
            },
    ) {
        val left = 48.dp.toPx()
        val top = 18.dp.toPx()
        val right = size.width - 18.dp.toPx()
        val bottom = size.height - 42.dp.toPx()
        val width = right - left
        val height = bottom - top
        val start = selectedTimeH - rangeHours / 2.0
        val end = selectedTimeH + rangeHours / 2.0
        val times = simulation?.timeH
        val values = simulation?.concentration
        var firstVisible = -1
        var lastVisible = -1
        var maxValue = 1.0
        if (times != null && values != null && times.isNotEmpty()) {
            val count = min(times.size, values.size)
            firstVisible = lowerBound(times, start, count)
            lastVisible = upperBound(times, end, count) - 1
            if (firstVisible < count && lastVisible >= firstVisible) {
                var index = firstVisible
                while (index <= lastVisible) {
                    val concentration = values[index]
                    if (concentration.isFinite()) {
                        maxValue = max(maxValue, concentration)
                    }
                    index += 1
                }
            }
        }
        val hasVisible = simulation != null && firstVisible >= 0 && lastVisible >= firstVisible
        val labelPaint = android.graphics.Paint().apply {
            this.color = palette.muted.toArgb()
            textSize = with(density) { 11.sp.toPx() }
            isAntiAlias = true
        }

        for (i in 0..4) {
            val y = bottom - height * i / 4f
            drawLine(palette.divider.copy(alpha = 0.72f), Offset(left, y), Offset(right, y), strokeWidth = 1.dp.toPx())
            drawContext.canvas.nativeCanvas.drawText(formatNumber(maxValue * i / 4.0, 0), 6.dp.toPx(), y + 4.dp.toPx(), labelPaint)
        }
        drawContext.canvas.nativeCanvas.drawText(unitLabel, left, 14.dp.toPx(), labelPaint)

        if (!hasVisible || times == null || values == null) {
            drawContext.canvas.nativeCanvas.drawText(language.t("暂无 ${unit} 曲线，点击右下角添加给药。", "No $unit curve yet. Tap + to add a dose."), left, top + height / 2f, labelPaint)
            return@Canvas
        }

        fun xFor(t: Double): Float = left + (((t - start) / (end - start)).toFloat().coerceIn(0f, 1f) * width)
        fun yFor(v: Double): Float = bottom - ((v / maxValue).toFloat().coerceIn(0f, 1f) * height)

        val visibleCount = lastVisible - firstVisible + 1
        val maxDrawPoints = if (analyte == Analyte.CPA) 960.0 else 760.0
        val stride = max(1, ceil(visibleCount / maxDrawPoints).toInt())
        val path = Path()
        var drawOrder = 0
        var lastDrawn = Int.MIN_VALUE
        fun appendPoint(index: Int) {
            if (index == lastDrawn || index !in firstVisible..lastVisible) return
            val x = xFor(times[index])
            val y = yFor(values[index])
            if (drawOrder == 0) path.moveTo(x, y) else path.lineTo(x, y)
            lastDrawn = index
            drawOrder += 1
        }
        if (stride == 1) {
            var pointIndex = firstVisible
            while (pointIndex <= lastVisible) {
                appendPoint(pointIndex)
                pointIndex += 1
            }
        } else {
            var bucketStart = firstVisible
            while (bucketStart <= lastVisible) {
                val bucketEnd = min(lastVisible, bucketStart + stride - 1)
                var minIndex = bucketStart
                var maxIndex = bucketStart
                var scan = bucketStart
                while (scan <= bucketEnd) {
                    if (values[scan] < values[minIndex]) minIndex = scan
                    if (values[scan] > values[maxIndex]) maxIndex = scan
                    scan += 1
                }
                appendPoint(bucketStart)
                listOf(minIndex, maxIndex)
                    .filter { it != bucketStart && it != bucketEnd }
                    .sorted()
                    .forEach(::appendPoint)
                appendPoint(bucketEnd)
                bucketStart = bucketEnd + 1
            }
        }
        appendPoint(lastVisible)
        drawPath(path, color.copy(alpha = 0.92f), style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round))

        val markerTimeH = tooltipTimeH ?: selectedTimeH
        val markerLabel = formatWallTime(markerTimeH).substring(5)
        val selectedX = xFor(markerTimeH)
        drawLine(Color(0xFFB8C0CC), Offset(selectedX, top), Offset(selectedX, bottom), strokeWidth = 1.5.dp.toPx())
        val selectedValue = interpolateConcentration(simulation, markerTimeH)
        drawCircle(palette.card, radius = 7.dp.toPx(), center = Offset(selectedX, yFor(selectedValue)))
        drawCircle(color, radius = 4.dp.toPx(), center = Offset(selectedX, yFor(selectedValue)))
        if (tooltipTimeH != null) {
            val selectedY = yFor(selectedValue)
            val bubbleWidth = min(width - 8.dp.toPx(), 286.dp.toPx())
            val bubbleHeight = if (analyte == Analyte.E2 && selectedValue >= 10_000.0) 98.dp.toPx() else 78.dp.toPx()
            val bubbleX = (selectedX - bubbleWidth / 2f).coerceIn(left + 4.dp.toPx(), right - bubbleWidth - 4.dp.toPx())
            val preferAbove = selectedY - bubbleHeight - 14.dp.toPx() > top
            val preferPinnedTop = selectedY > top + height * 0.58f
            val rawBubbleY = when {
                preferPinnedTop -> top + 28.dp.toPx()
                preferAbove -> selectedY - bubbleHeight - 14.dp.toPx()
                else -> selectedY + 16.dp.toPx()
            }
            val bubbleY = rawBubbleY.coerceIn(top + 4.dp.toPx(), bottom - bubbleHeight - 4.dp.toPx())
            drawRoundRect(
                color = palette.card,
                topLeft = Offset(bubbleX, bubbleY),
                size = Size(bubbleWidth, bubbleHeight),
                cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
            )
            drawRoundRect(
                color = color.copy(alpha = 0.22f),
                topLeft = Offset(bubbleX, bubbleY),
                size = Size(bubbleWidth, bubbleHeight),
                cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
                style = Stroke(width = 1.2.dp.toPx()),
            )
            val titlePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                this.color = palette.muted.toArgb()
                textSize = 12.sp.toPx()
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            }
            val valuePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                this.color = palette.ink.toArgb()
                textSize = 18.sp.toPx()
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            }
            val accentPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color.toArgb()
                textSize = 13.sp.toPx()
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            }
            drawContext.canvas.nativeCanvas.apply {
                val valueText = "${formatNumber(selectedValue, 1)} $unit"
                val labelX = bubbleX + 16.dp.toPx()
                val valueX = (labelX + accentPaint.measureText(chartLabel) + 16.dp.toPx())
                    .coerceAtMost(bubbleX + bubbleWidth - valuePaint.measureText(valueText) - 16.dp.toPx())
                drawText(markerLabel, bubbleX + 16.dp.toPx(), bubbleY + 24.dp.toPx(), titlePaint)
                drawText(chartLabel, labelX, bubbleY + 52.dp.toPx(), accentPaint)
                drawText(
                    valueText,
                    valueX,
                    bubbleY + 53.dp.toPx(),
                    valuePaint,
                )
                if (analyte == Analyte.E2 && selectedValue >= 10_000.0) {
                    drawText(language.t("称号：万雌王", "Title: E2 Overlord"), bubbleX + 16.dp.toPx(), bubbleY + 78.dp.toPx(), accentPaint)
                }
            }
        }
        drawContext.canvas.nativeCanvas.drawText(markerLabel, left, size.height - 14.dp.toPx(), labelPaint)
    }
}

private fun lowerBound(values: DoubleArray, target: Double, limit: Int = values.size): Int {
    var low = 0
    var high = limit.coerceIn(0, values.size)
    while (low < high) {
        val mid = (low + high) ushr 1
        if (values[mid] < target) low = mid + 1 else high = mid
    }
    return low
}

private fun upperBound(values: DoubleArray, target: Double, limit: Int = values.size): Int {
    var low = 0
    var high = limit.coerceIn(0, values.size)
    while (low < high) {
        val mid = (low + high) ushr 1
        if (values[mid] <= target) low = mid + 1 else high = mid
    }
    return low
}
