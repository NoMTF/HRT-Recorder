// Android 版作者：南盺
// Reusable Compose atoms shared by overview, records, labs, settings, and tools.
package com.nanxin.hrtrecorder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

data class MetricPillActions(
    val onWeightClick: () -> Unit,
    val onTimeClick: () -> Unit,
    val onCpaClick: () -> Unit,
)

val LocalMetricPillActions = staticCompositionLocalOf<MetricPillActions?> { null }

@Composable
fun FeaturePageSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val palette = LocalAppPalette.current
    val isDark = palette.card != Color.White
    val pageBrush = remember(isDark) {
        if (isDark) {
            Brush.linearGradient(
                colors = listOf(Color(0xFF071724), Color(0xFF111B2D), Color(0xFF171C2A)),
                start = Offset.Zero,
                end = Offset(0f, 1450f),
            )
        } else {
            Brush.linearGradient(
                colors = listOf(Color(0xFFF4FCFF), Color(0xFFFFFFFF), Color(0xFFFFF7FB)),
                start = Offset.Zero,
                end = Offset(0f, 1450f),
            )
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(pageBrush),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            content()
        }
    }
}

@Composable
fun MetricPill(
    label: String,
    value: String,
    accent: Color = E2Pink,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(18.dp)
    val headerActions = LocalMetricPillActions.current
    val inferredClick = onClick ?: when {
        headerActions == null -> null
        value.endsWith(" kg") -> headerActions.onWeightClick
        value.contains("-") -> headerActions.onTimeClick
        value.endsWith("ng/mL") -> headerActions.onCpaClick
        else -> null
    }
    val interaction = remember(label, value) { MutableInteractionSource() }
    val scale = colorOsPressScale(interaction, pressedScale = 0.975f, label = "metric-press-${label.hashCode()}-${value.hashCode()}")
    val interactiveModifier = if (inferredClick != null) {
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = inferredClick,
            )
    } else {
        modifier.clip(shape)
    }
    Column(
        interactiveModifier
            .background(accent.copy(alpha = 0.13f), shape)
            .border(1.dp, accent.copy(alpha = 0.04f), shape)
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Text(label, color = Muted, fontSize = 11.sp, lineHeight = 13.sp)
        Text(value, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChipFlow(items: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    val palette = LocalAppPalette.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            val isSelected = item == selected
            Surface(
                onClick = { onSelect(item) },
                color = if (isSelected) E2Pink.copy(alpha = 0.20f) else palette.subtleSurface,
                contentColor = if (isSelected) E2Pink else Ink,
                shape = RoundedCornerShape(18.dp),
            ) {
                Text(label(item), modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun NativeCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val palette = LocalAppPalette.current
    val shape = RoundedCornerShape(30.dp)
    val isDark = palette.card != Color.White
    val cardBrush = remember(isDark, palette.card) {
        if (isDark) {
            Brush.linearGradient(
                listOf(
                    palette.card.copy(alpha = 0.96f),
                    palette.card.copy(alpha = 0.90f),
                ),
            )
        } else {
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.94f),
                    Color.White.copy(alpha = 0.88f),
                ),
            )
        }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cardBrush, shape),
    ) {
        Column(Modifier.padding(horizontal = 22.dp, vertical = 23.dp), content = content)
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(8.dp, 38.dp)
            .background(E2Pink, RoundedCornerShape(8.dp)),
        )
        Column {
            Text(title, fontSize = 31.sp, color = Ink, fontWeight = FontWeight.ExtraBold, lineHeight = 35.sp)
            Text(subtitle, fontSize = 13.sp, color = Muted, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun LargePageTitle(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 28.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            color = Ink,
            fontSize = 40.sp,
            lineHeight = 43.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                color = Ink.copy(alpha = 0.78f),
                fontSize = 21.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun SoftIconBadge(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier,
    sizeDp: Int = 58,
) {
    val badgeBrush = if (accent == E2Pink) {
        Brush.linearGradient(listOf(TBlue.copy(alpha = 0.88f), E2Pink.copy(alpha = 0.90f)))
    } else {
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.72f), accent.copy(alpha = 0.18f)))
    }
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape((sizeDp / 3).dp))
            .background(badgeBrush, RoundedCornerShape((sizeDp / 3).dp))
            .border(1.dp, accent.copy(alpha = 0.05f), RoundedCornerShape((sizeDp / 3).dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (accent == E2Pink) Color.White else accent, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
    }
}

@Composable
fun DrugGlyphBadge(
    route: Route,
    category: MedicationCategory,
    accent: Color,
    modifier: Modifier = Modifier,
    sizeDp: Int = 58,
) {
    val shape = RoundedCornerShape((sizeDp / 3).dp)
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(shape)
            .background(accent.copy(alpha = 0.12f), shape)
            .border(1.dp, Color.White.copy(alpha = 0.82f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size((sizeDp * 0.58f).dp)) {
            val stroke = size.minDimension * 0.12f
            val ink = accent.copy(alpha = 0.92f)
            val w = size.width
            val h = size.height
            when (route) {
                Route.Injection -> {
                    drawLine(ink, Offset(w * 0.22f, h * 0.78f), Offset(w * 0.70f, h * 0.30f), strokeWidth = stroke, cap = StrokeCap.Round)
                    drawLine(ink, Offset(w * 0.58f, h * 0.18f), Offset(w * 0.82f, h * 0.42f), strokeWidth = stroke * 0.70f, cap = StrokeCap.Round)
                    drawLine(ink, Offset(w * 0.14f, h * 0.86f), Offset(w * 0.04f, h * 0.96f), strokeWidth = stroke * 0.62f, cap = StrokeCap.Round)
                    drawLine(ink, Offset(w * 0.34f, h * 0.50f), Offset(w * 0.50f, h * 0.66f), strokeWidth = stroke * 0.52f, cap = StrokeCap.Round)
                }
                Route.PatchApply, Route.PatchRemove -> {
                    drawRoundRect(
                        color = ink,
                        topLeft = Offset(w * 0.18f, h * 0.18f),
                        size = Size(w * 0.64f, h * 0.64f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.14f, h * 0.14f),
                        style = Stroke(width = stroke * 0.82f),
                    )
                    drawLine(ink, Offset(w * 0.34f, h * 0.50f), Offset(w * 0.66f, h * 0.50f), strokeWidth = stroke * 0.7f, cap = StrokeCap.Round)
                    if (route == Route.PatchApply) {
                        drawLine(ink, Offset(w * 0.50f, h * 0.34f), Offset(w * 0.50f, h * 0.66f), strokeWidth = stroke * 0.7f, cap = StrokeCap.Round)
                    }
                }
                Route.Gel -> {
                    val path = Path().apply {
                        moveTo(w * 0.50f, h * 0.05f)
                        cubicTo(w * 0.22f, h * 0.35f, w * 0.18f, h * 0.58f, w * 0.32f, h * 0.76f)
                        cubicTo(w * 0.48f, h * 0.96f, w * 0.78f, h * 0.86f, w * 0.80f, h * 0.58f)
                        cubicTo(w * 0.82f, h * 0.38f, w * 0.65f, h * 0.20f, w * 0.50f, h * 0.05f)
                        close()
                    }
                    drawPath(path, color = ink, style = Stroke(width = stroke * 0.78f, cap = StrokeCap.Round))
                    drawCircle(ink.copy(alpha = 0.18f), radius = w * 0.16f, center = Offset(w * 0.56f, h * 0.57f))
                }
                Route.Oral, Route.Sublingual -> {
                    drawRoundRect(
                        color = ink,
                        topLeft = Offset(w * 0.10f, h * 0.30f),
                        size = Size(w * 0.52f, h * 0.30f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.16f, h * 0.16f),
                    )
                    drawLine(Color.White.copy(alpha = 0.72f), Offset(w * 0.36f, h * 0.31f), Offset(w * 0.36f, h * 0.59f), strokeWidth = stroke * 0.44f)
                    drawCircle(ink.copy(alpha = 0.86f), radius = w * 0.14f, center = Offset(w * 0.74f, h * 0.55f))
                    if (category == MedicationCategory.AntiAndrogen || category == MedicationCategory.Cpa) {
                        drawCircle(Color.White.copy(alpha = 0.72f), radius = w * 0.06f, center = Offset(w * 0.74f, h * 0.55f))
                    }
                }
            }
        }
    }
}

@Composable
fun GradientActionPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = E2Pink,
    enabled: Boolean = true,
) {
    val interaction = remember(text) { MutableInteractionSource() }
    val scale = colorOsPressScale(interaction, pressedScale = 0.97f, label = "gradient-action-${text.hashCode()}")
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.48f
            }
            .clip(shape)
            .background(
                if (enabled) {
                    Brush.linearGradient(listOf(TBlue.copy(alpha = 0.94f), E2Pink.copy(alpha = 0.88f), accent.copy(alpha = 0.96f)))
                } else {
                    Brush.linearGradient(listOf(accent.copy(alpha = 0.34f), accent.copy(alpha = 0.42f)))
                },
                shape,
            )
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun SecondaryGlassPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = TBlue,
) {
    val palette = LocalAppPalette.current
    val interaction = remember(text) { MutableInteractionSource() }
    val scale = colorOsPressScale(interaction, pressedScale = 0.975f, label = "secondary-action-${text.hashCode()}")
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(Color.White.copy(alpha = 0.58f), shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = accent, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun <T> SoftSegmentedControl(
    items: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalAppPalette.current
    val shape = RoundedCornerShape(26.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.copy(alpha = 0.52f), shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEach { item ->
            val isSelected = item == selected
            val interaction = remember(item) { MutableInteractionSource() }
            val scale = colorOsPressScale(interaction, pressedScale = 0.97f, label = "soft-segment-${item.hashCode()}")
            val bg = if (isSelected) Color.White.copy(alpha = 0.72f) else Color.Transparent
            val fg = if (isSelected) Ink else Muted
            Box(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(22.dp))
                    .background(bg, RoundedCornerShape(22.dp))
                    .clickable(interactionSource = interaction, indication = null) { onSelect(item) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label(item), color = fg, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            }
        }
    }
}

@Composable
fun SoftListRow(
    icon: String,
    title: String,
    subtitle: String,
    accent: Color,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val palette = LocalAppPalette.current
    val shape = RoundedCornerShape(28.dp)
    val interaction = remember(title, subtitle) { MutableInteractionSource() }
    val scale = colorOsPressScale(interaction, pressedScale = 0.982f, label = "list-row-${title.hashCode()}")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(Color.White.copy(alpha = 0.54f), shape)
            .let { base ->
                if (onClick != null) {
                    base.clickable(interactionSource = interaction, indication = null, onClick = onClick)
                } else {
                    base
                }
            }
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SoftIconBadge(icon, accent, sizeDp = 52)
        Column(Modifier.weight(1f)) {
            Text(title, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = Muted, fontSize = 13.sp, lineHeight = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        trailing?.invoke()
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun SupplyUnitBlocks(
    total: Double,
    remaining: Double,
    accent: Color,
    modifier: Modifier = Modifier,
    maxBlocks: Int = 500,
) {
    val totalSafe = max(0.0, total)
    val remainRatio = if (totalSafe > 0.0) (remaining / totalSafe).coerceIn(0.0, 1.0) else 0.0
    val exactTotal = ceil(totalSafe).toInt()
    val exactMode = exactTotal <= maxBlocks
    val blocks = when {
        totalSafe <= 0.0 -> 0
        exactMode -> max(1, exactTotal)
        else -> maxBlocks
    }
    val filled = if (exactMode) {
        ceil(remaining.coerceIn(0.0, totalSafe)).toInt().coerceIn(0, blocks)
    } else {
        ((blocks * remainRatio) + 0.5).toInt().coerceIn(0, blocks)
    }
    val blockWidth = when {
        blocks > 360 -> 5.dp
        blocks > 240 -> 6.dp
        blocks > 160 -> 7.dp
        blocks > 96 -> 9.dp
        else -> 14.dp
    }
    val blockHeight = when {
        blocks > 360 -> 8.dp
        blocks > 240 -> 10.dp
        blocks > 160 -> 12.dp
        blocks > 96 -> 15.dp
        else -> 22.dp
    }
    val blockRadius = if (blocks > 160) 3.dp else 5.dp
    val horizontalGap = if (blocks > 160) 3.dp else 5.dp
    val verticalGap = if (blocks > 160) 4.dp else 6.dp
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(horizontalGap),
        verticalArrangement = Arrangement.spacedBy(verticalGap),
    ) {
        repeat(blocks) { index ->
            val active = index < filled
            Box(
                Modifier
                    .width(blockWidth)
                    .height(blockHeight)
                    .clip(RoundedCornerShape(blockRadius))
                    .background(if (active) accent.copy(alpha = 0.66f) else accent.copy(alpha = 0.13f)),
            )
        }
    }
}

fun analyteColor(analyte: Analyte?): Color =
    when (analyte) {
        Analyte.CPA -> CpaRose
        Analyte.Testosterone -> TBlue
        Analyte.E2, null -> E2Pink
    }
