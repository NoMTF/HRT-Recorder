package com.nanxin.hrtrecorder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

/*
 * Android author watermark: 南盺
 * Records list is isolated from MainActivity so list rendering and record UI
 * can be optimized without dragging the whole app shell through recomposition.
 */

@Composable
fun RecordsScreen(
    events: List<DoseEvent>,
    language: AppLanguage,
    onAdd: () -> Unit,
    onEdit: (DoseEvent) -> Unit,
    onDelete: (DoseEvent) -> Unit,
) {
    val newestFirst = remember(events) { events.sortedByDescending { it.timeH } }
    val grouped = remember(newestFirst, language) { newestFirst.groupBy { formatDateLabel(it.timeH, language) }.toList() }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 120.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            RecordActionCard(count = events.size, language = language, onAdd = onAdd)
        }
        items(grouped, key = { it.first }) { (dateLabel, dayEvents) ->
            RecordGroupCard(dateLabel = dateLabel, events = dayEvents, language = language, onEdit = onEdit, onDelete = onDelete)
        }
    }
}

@Composable
fun RecordsScreenV2(
    events: List<DoseEvent>,
    language: AppLanguage,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onPickDate: () -> Unit = {},
    onAdd: () -> Unit,
    onEdit: (DoseEvent) -> Unit,
    onDelete: (DoseEvent) -> Unit,
) {
    val newestFirst = remember(events) { events.sortedByDescending { it.timeH } }
    val filtered = remember(newestFirst, selectedDate) {
        newestFirst.filter { localFromEpochHours(it.timeH).toLocalDate() == selectedDate }
    }
    val grouped = remember(filtered, language) { filtered.groupBy { formatDateLabel(it.timeH, language) }.toList() }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 132.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            LargePageTitle(
                title = language.t("记录", "Record"),
                subtitle = language.t("用药历史", "Dose history"),
            )
        }
        item {
            NativeCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    SoftIconBadge("⌁", E2Pink, sizeDp = 58)
                    Column(Modifier.weight(1f)) {
                        Text(language.t("用药记录", "Medication Records"), color = Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                        Text(language.t("${events.size} 条记录", "${events.size} records"), color = Muted, fontSize = 13.sp)
                    }
                    GradientActionPill(language.t("+ 新增", "+ Add"), onClick = onAdd, accent = E2Pink)
                }
            }
        }
        item {
            RecordCalendarCard(
                events = events,
                selectedDate = selectedDate,
                language = language,
                onDateSelected = onDateSelected,
                onPickDate = onPickDate,
            )
        }
        if (grouped.isEmpty()) {
            item {
                NativeCard {
                    Text(language.t("这一天没有用药记录", "No dose records on this date"), color = Ink, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                    Text(language.t("点新增会默认使用当前选中的日期。", "Add uses the selected date by default."), color = Muted, fontSize = 13.sp)
                }
            }
        }
        items(grouped, key = { it.first }) { (dateLabel, dayEvents) ->
            RecordGroupCardV2(dateLabel = dateLabel, events = dayEvents, language = language, onEdit = onEdit, onDelete = onDelete)
        }
    }
}

private data class RecordCalendarDay(
    val date: LocalDate,
    val inCurrentMonth: Boolean,
    val count: Int,
    val accent: Color,
)

private fun buildRecordCalendarDays(events: List<DoseEvent>, selectedDate: LocalDate): List<RecordCalendarDay> {
    val month = YearMonth.from(selectedDate)
    val firstDay = month.atDay(1)
    val gridStart = firstDay.minusDays(((firstDay.dayOfWeek.value + 6) % 7).toLong())
    return (0 until 42).map { offset ->
        val date = gridStart.plusDays(offset.toLong())
        val dayEvents = events.filter { localFromEpochHours(it.timeH).toLocalDate() == date }
        RecordCalendarDay(
            date = date,
            inCurrentMonth = YearMonth.from(date) == month,
            count = dayEvents.size,
            accent = dayEvents.firstOrNull()?.let { analyteColor(eventAnalyte(it)) } ?: Color.Transparent,
        )
    }
}

@Composable
private fun RecordCalendarCard(
    events: List<DoseEvent>,
    selectedDate: LocalDate,
    language: AppLanguage,
    onDateSelected: (LocalDate) -> Unit,
    onPickDate: () -> Unit,
) {
    val days = remember(events, selectedDate) { buildRecordCalendarDays(events, selectedDate) }
    val monthTitle = remember(selectedDate, language) {
        if (language == AppLanguage.English) {
            selectedDate.month.name.lowercase(Locale.US).replaceFirstChar { it.titlecase(Locale.US) } + " ${selectedDate.year}"
        } else {
            "${selectedDate.year}年${selectedDate.monthValue}月"
        }
    }
    NativeCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(language.t("历史日历", "History Calendar"), color = Ink, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                Text(language.t("点日期查看当天记录", "Tap a date to view records"), color = Muted, fontSize = 12.sp)
            }
            TextButton(
                onClick = onPickDate,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                modifier = Modifier.background(TBlue.copy(alpha = 0.10f), RoundedCornerShape(99.dp)),
            ) {
                Text(
                    text = monthTitle,
                    color = TBlue,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            language.t("点月份可调用系统日期/时间选择器；点日期格可快速切换。", "Tap the month for the Android date/time picker; tap a day for quick selection."),
            color = Muted,
            fontSize = 11.sp,
            lineHeight = 15.sp,
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(
                language.t("一", "M"),
                language.t("二", "T"),
                language.t("三", "W"),
                language.t("四", "T"),
                language.t("五", "F"),
                language.t("六", "S"),
                language.t("日", "S"),
            ).forEach {
                Text(it, color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(6.dp))
        days.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                week.forEach { day ->
                    RecordCalendarDayCell(
                        day = day,
                        selected = day.date == selectedDate,
                        onDateSelected = onDateSelected,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordCalendarDayCell(
    day: RecordCalendarDay,
    selected: Boolean,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(30.dp)
            .clickable { onDateSelected(day.date) }
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(if (selected) E2Pink.copy(alpha = 0.22f) else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                day.date.dayOfMonth.toString(),
                color = when {
                    selected -> E2Pink
                    !day.inCurrentMonth -> Muted.copy(alpha = 0.34f)
                    else -> Ink.copy(alpha = 0.78f)
                },
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(1.dp))
        if (day.count > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(day.count.coerceAtMost(3)) {
                    Box(Modifier.size(3.dp).background(day.accent, CircleShape))
                }
            }
        }
    }
}

@Composable
private fun RecordGroupCardV2(
    dateLabel: String,
    events: List<DoseEvent>,
    language: AppLanguage,
    onEdit: (DoseEvent) -> Unit,
    onDelete: (DoseEvent) -> Unit,
) {
    val palette = LocalAppPalette.current
    NativeCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(Modifier.size(9.dp).background(E2Pink.copy(alpha = 0.76f), CircleShape))
            Text(dateLabel, color = Muted, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(12.dp))
        events.forEachIndexed { index, event ->
            RecordRowV2(event = event, language = language, onEdit = { onEdit(event) }, onDelete = { onDelete(event) })
            if (index != events.lastIndex) {
                    HorizontalDivider(color = palette.divider.copy(alpha = 0.16f), modifier = Modifier.padding(vertical = 12.dp))
            }
        }
    }
}

@Composable
private fun RecordRowV2(event: DoseEvent, language: AppLanguage, onEdit: () -> Unit, onDelete: () -> Unit) {
    val accent = analyteColor(eventAnalyte(event))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        DrugGlyphBadge(route = event.route, category = event.category, accent = accent, sizeDp = 56)
        Column(Modifier.weight(1f)) {
            Text(event.recordOnlyMedication?.label(language) ?: "${event.compound.label(language)} (${event.compound.wire})", fontSize = 18.sp, color = Ink, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(event.route.label(language), color = Muted, fontSize = 14.sp)
            Text(doseDisplayText(event, language), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatWallClock(event.timeH), color = Muted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Row {
                TextButton(onClick = onEdit) { Text(language.t("编辑", "Edit"), color = accent) }
                TextButton(onClick = onDelete) { Text(language.t("删", "Del"), color = Color(0xFFDC2626)) }
            }
        }
    }
}

@Composable
private fun RecordActionCard(count: Int, language: AppLanguage, onAdd: () -> Unit) {
    NativeCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier
                    .size(56.dp)
                    .background(E2Pink.copy(alpha = 0.12f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("⌁", color = E2Pink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f)) {
                Text(language.t("用药记录", "Medication Records"), color = Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text(language.t("$count 条记录", "$count records"), color = Muted, fontSize = 13.sp)
            }
            Button(onClick = onAdd, shape = RoundedCornerShape(18.dp)) {
                Text(language.t("+ 新增用药", "+ Add"))
            }
        }
    }
}

@Composable
private fun RecordGroupCard(
    dateLabel: String,
    events: List<DoseEvent>,
    language: AppLanguage,
    onEdit: (DoseEvent) -> Unit,
    onDelete: (DoseEvent) -> Unit,
) {
    val palette = LocalAppPalette.current
    NativeCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(8.dp).background(E2Pink, CircleShape))
            Text(dateLabel, color = Muted, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        events.forEachIndexed { index, event ->
            RecordRow(event = event, language = language, onEdit = { onEdit(event) }, onDelete = { onDelete(event) })
            if (index != events.lastIndex) {
                    HorizontalDivider(color = palette.divider.copy(alpha = 0.16f), modifier = Modifier.padding(vertical = 12.dp))
            }
        }
    }
}

@Composable
private fun RecordRow(event: DoseEvent, language: AppLanguage, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(52.dp)
                .background(analyteColor(eventAnalyte(event)).copy(alpha = 0.13f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(recordBadgeText(event, language), color = analyteColor(eventAnalyte(event)), fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(event.recordOnlyMedication?.label(language) ?: "${event.compound.label(language)} (${event.compound.wire})", fontSize = 18.sp, color = Ink, fontWeight = FontWeight.Bold)
            Text(event.route.label(language), color = Muted, fontSize = 14.sp)
            Text(doseDisplayText(event, language), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatWallClock(event.timeH), color = Muted, fontSize = 14.sp)
            Row {
                TextButton(onClick = onEdit) { Text(language.t("编辑", "Edit")) }
                TextButton(onClick = onDelete) { Text(language.t("删", "Delete"), color = Color(0xFFDC2626)) }
            }
        }
    }
}

private fun recordBadgeText(event: DoseEvent, language: AppLanguage): String =
    when (event.category) {
        MedicationCategory.AntiAndrogen -> event.recordOnlyMedication?.shortCode(language) ?: language.t("抗雄", "Anti")
        MedicationCategory.Cpa -> "CPA"
        else -> eventAnalyte(event)?.label ?: language.t("抗雄", "Anti")
    }
