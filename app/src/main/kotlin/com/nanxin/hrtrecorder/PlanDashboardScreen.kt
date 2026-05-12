@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.nanxin.hrtrecorder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import kotlin.math.abs

/*
 * Android author watermark: Nanxin.
 * Plan dashboard stays in its own file so the execution/reminder UX can evolve
 * without expanding MainActivity.
 */

@Composable
fun PlanDashboardScreen(
    plans: List<MedicationPlan>,
    records: List<PlanDoseRecord>,
    language: AppLanguage,
    nowH: Double,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    onPickDate: () -> Unit = {},
    onCreatePlan: () -> Unit,
    onCreateReminder: () -> Unit = {},
    onAddDose: (MedicationPlan) -> Unit,
    onAddReminder: (MedicationPlan) -> Unit,
    onMarkTaken: (MedicationPlan, Double) -> Unit = { _, _ -> },
    onSkip: (MedicationPlan, Double) -> Unit = { _, _ -> },
    onToggle: (MedicationPlan) -> Unit,
    onDelete: (MedicationPlan) -> Unit,
) {
    val selectedOccurrences = remember(plans, records, selectedDate, nowH) {
        buildPlanOccurrencesForDate(plans, records, selectedDate, nowH)
    }
    val grouped = remember(plans) {
        plans.sortedWith(compareBy<MedicationPlan> { it.groupName.ifBlank { "~" } }.thenBy { it.timeMinutes })
            .groupBy { it.groupName.ifBlank { "" } }
    }
    val calendarDays = remember(plans, records, nowH, selectedDate) {
        buildPlanCalendarDays(plans, records, selectedDate, nowH)
    }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 128.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            LargePageTitle(
                title = language.t("计划", "Plans"),
                subtitle = language.t("今天要做什么，一眼看清", "What comes next, clearly"),
            )
        }
        item {
            PlanCalendarOverviewCard(
                days = calendarDays,
                language = language,
                nowH = nowH,
                selectedDate = selectedDate,
                onSelectDate = onSelectDate,
                onPickDate = onPickDate,
            )
        }
        item {
            NativeCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    SoftIconBadge("＋", E2Pink, sizeDp = 58)
                    Column(Modifier.weight(1f)) {
                        Text(language.t("创建计划与提醒", "Plan and reminders"), color = Ink, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            language.t(
                                "App 本地提醒会先询问“已服用/跳过”，确认后才写入记录；也可以一键打开系统日历同步。",
                                "Local reminders ask Taken/Skip first; only Taken writes a real dose. Calendar sync is still one tap away.",
                            ),
                            color = Muted,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onCreatePlan,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text(language.t("创建计划", "Create"), fontWeight = FontWeight.ExtraBold)
                    }
                    OutlinedButton(
                        onClick = onCreateReminder,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text(language.t("系统提醒", "Calendar"), fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
        item {
            SectionTitle(
                title = selectedDate.planDateTitle(language),
                subtitle = language.t("待执行、已服用、已跳过、遗漏状态", "Pending, taken, skipped, and missed"),
            )
        }
        if (selectedOccurrences.isEmpty()) {
            item {
                PlanEmptyState(
                    title = language.t("这一天没有计划", "No plans on this date"),
                    body = language.t("创建每日或每周计划后，这里会显示今日执行清单。", "Create a daily or weekly plan and today's checklist appears here."),
                )
            }
        } else {
            items(selectedOccurrences, key = { it.key }) { occurrence ->
                PlanOccurrenceCard(
                    occurrence = occurrence,
                    language = language,
                    onMarkTaken = { onMarkTaken(occurrence.plan, occurrence.scheduledTimeH) },
                    onSkip = { onSkip(occurrence.plan, occurrence.scheduledTimeH) },
                    onQuickLog = { onAddDose(occurrence.plan) },
                    onReminder = { onAddReminder(occurrence.plan) },
                )
            }
        }
        item {
            SectionTitle(
                title = language.t("分组方案", "Groups"),
                subtitle = language.t("按真实习惯管理方案和时间点", "Manage routines by real-life groups"),
            )
        }
        if (grouped.isEmpty()) {
            item {
                PlanEmptyState(
                    title = language.t("暂无方案", "No groups"),
                    body = language.t("比如“补佳乐”“睡前抗雄”“每周针剂”，都可以作为一个方案管理。", "For example: pills, bedtime antiandrogen, or weekly injections."),
                )
            }
        } else {
            grouped.forEach { (groupName, groupPlans) ->
                item(key = "group-$groupName") {
                    NativeCard {
                        Text(
                            groupName.ifBlank { language.t("默认方案", "Default routine") },
                            color = Ink,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            language.t("${groupPlans.size} 个计划", "${groupPlans.size} plans"),
                            color = Muted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            groupPlans.forEach { plan ->
                                GroupPlanRow(
                                    plan = plan,
                                    language = language,
                                    onAddDose = { onAddDose(plan) },
                                    onAddReminder = { onAddReminder(plan) },
                                    onToggle = { onToggle(plan) },
                                    onDelete = { onDelete(plan) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class PlanCalendarDay(
    val date: LocalDate,
    val inCurrentMonth: Boolean,
    val statuses: List<PlanDoseStatus>,
)

private fun buildPlanCalendarDays(
    plans: List<MedicationPlan>,
    records: List<PlanDoseRecord>,
    selectedDate: LocalDate,
    nowH: Double,
): List<PlanCalendarDay> {
    val nowLocal = localFromEpochHours(nowH)
    val today = nowLocal.toLocalDate()
    val month = YearMonth.from(selectedDate)
    val firstDay = month.atDay(1)
    val gridStart = firstDay.minusDays(((firstDay.dayOfWeek.value + 6) % 7).toLong())
    val activePlans = plans.filter { it.enabled }
    return (0 until 42).map { offset ->
        val date = gridStart.plusDays(offset.toLong())
        val statuses = activePlans
            .filter { it.shouldAppearOnCalendarDate(date, today) }
            .flatMap { plan ->
                plan.normalizedTimes().map { minutes ->
                    val scheduled = scheduledTimeForDate(date, minutes)
                    val record = records.firstOrNull {
                        it.planId == plan.id && abs(it.scheduledTimeH - scheduled) <= 1.0 / 60.0
                    }
                    record?.status ?: when {
                        scheduled + 2.0 < nowH -> PlanDoseStatus.Missed
                        else -> PlanDoseStatus.Pending
                    }
                }
            }
        PlanCalendarDay(
            date = date,
            inCurrentMonth = YearMonth.from(date) == month,
            statuses = statuses,
        )
    }
}

private fun MedicationPlan.shouldAppearOnCalendarDate(date: LocalDate, today: LocalDate): Boolean =
    when (repeat) {
        PlanRepeat.Daily -> true
        PlanRepeat.Weekly -> date.dayOfWeek == today.dayOfWeek
    }

private fun LocalDate.planDateTitle(language: AppLanguage): String =
    if (language == AppLanguage.English) {
        "${month.name.lowercase(Locale.US).replaceFirstChar { it.titlecase(Locale.US) }} $dayOfMonth"
    } else {
        "${monthValue}月${dayOfMonth}日计划"
    }

@Composable
private fun PlanCalendarOverviewCard(
    days: List<PlanCalendarDay>,
    language: AppLanguage,
    nowH: Double,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    onPickDate: () -> Unit,
) {
    val today = localFromEpochHours(nowH).toLocalDate()
    val monthTitle = remember(selectedDate, language) {
        if (language == AppLanguage.English) {
            selectedDate.month.name.lowercase(Locale.US).replaceFirstChar { it.titlecase(Locale.US) } + " ${selectedDate.year}"
        } else {
            "${selectedDate.year}年${selectedDate.monthValue}月"
        }
    }
    NativeCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(language.t("月度日历", "Monthly calendar"), color = Ink, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                Text(language.t("按天看待执行、已服用、跳过和遗漏", "Daily pending, taken, skipped, and missed overview"), color = Muted, fontSize = 12.sp)
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
                Text(it, color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(6.dp))
        days.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                week.forEach { day ->
                    CalendarDayCell(
                        day = day,
                        isToday = day.date == today,
                        isSelected = day.date == selectedDate,
                        onSelectDate = onSelectDate,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CalendarLegendDot(E2Pink, language.t("已服用", "Taken"))
            CalendarLegendDot(TBlue, language.t("待执行", "Pending"))
            CalendarLegendDot(Color(0xFFDC2626), language.t("遗漏", "Missed"))
            CalendarLegendDot(Muted, language.t("跳过", "Skipped"))
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: PlanCalendarDay,
    isToday: Boolean,
    isSelected: Boolean,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeColor = day.statuses.calendarStatusColor()
    Column(
        modifier = modifier
            .height(30.dp)
            .clickable { onSelectDate(day.date) }
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(
                    when {
                        isSelected -> E2Pink.copy(alpha = 0.22f)
                        isToday -> TBlue.copy(alpha = 0.18f)
                        else -> Color.Transparent
                    },
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                day.date.dayOfMonth.toString(),
                color = when {
                    isSelected -> E2Pink
                    isToday -> TBlue
                    !day.inCurrentMonth -> Muted.copy(alpha = 0.34f)
                    else -> Ink.copy(alpha = 0.78f)
                },
                fontSize = 10.sp,
                fontWeight = if (isToday || isSelected) FontWeight.ExtraBold else FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(1.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
            val dotCount = day.statuses.size.coerceAtMost(3)
            repeat(dotCount) {
                Box(Modifier.size(3.dp).background(activeColor, CircleShape))
            }
            if (day.statuses.size > 3) {
                Box(Modifier.width(6.dp).height(3.dp).background(activeColor.copy(alpha = 0.72f), CircleShape))
            }
        }
    }
}

@Composable
private fun CalendarLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(7.dp).background(color, CircleShape))
        Text(label, color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun List<PlanDoseStatus>.calendarStatusColor(): Color =
    when {
        isEmpty() -> Color.Transparent
        any { it == PlanDoseStatus.Missed } -> Color(0xFFDC2626)
        any { it == PlanDoseStatus.Pending } -> TBlue
        any { it == PlanDoseStatus.Skipped } -> Muted
        else -> E2Pink
    }

@Composable
private fun PlanOccurrenceCard(
    occurrence: PlanOccurrence,
    language: AppLanguage,
    onMarkTaken: () -> Unit,
    onSkip: () -> Unit,
    onQuickLog: () -> Unit,
    onReminder: () -> Unit,
) {
    val plan = occurrence.plan
    val accent = categoryAccent(plan)
    NativeCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            DrugGlyphBadge(route = plan.route, category = plan.category, accent = accent, sizeDp = 58)
            Column(Modifier.weight(1f)) {
                Text(plan.displayName(language), color = Ink, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${plan.route.label(language)} · ${planDoseText(plan, language)}", color = Muted, fontSize = 13.sp)
                Text(
                    occurrence.status.label(language),
                    color = occurrence.status.statusColor(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Text(
                formatWallClock(occurrence.scheduledTimeH),
                color = Ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.62f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = occurrence.status == PlanDoseStatus.Pending || occurrence.status == PlanDoseStatus.Missed,
                onClick = onMarkTaken,
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(language.t("已服用", "Taken"))
            }
            OutlinedButton(
                enabled = occurrence.status == PlanDoseStatus.Pending || occurrence.status == PlanDoseStatus.Missed,
                onClick = onSkip,
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(language.t("跳过", "Skip"))
            }
            OutlinedButton(onClick = onQuickLog, shape = RoundedCornerShape(16.dp)) {
                Text(language.t("手动记录", "Log"))
            }
            OutlinedButton(onClick = onReminder, shape = RoundedCornerShape(16.dp)) {
                Text(language.t("系统提醒", "Calendar"))
            }
        }
    }
}

@Composable
private fun GroupPlanRow(
    plan: MedicationPlan,
    language: AppLanguage,
    onAddDose: () -> Unit,
    onAddReminder: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    val accent = categoryAccent(plan)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(accent.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DrugGlyphBadge(route = plan.route, category = plan.category, accent = accent, sizeDp = 50)
        Column(Modifier.weight(1f)) {
            Text(plan.displayName(language), color = Ink, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${plan.normalizedTimes().joinToString(" / ") { formatPlanTime(it) }} · ${plan.route.label(language)} · ${planDoseText(plan, language)}",
                color = Muted,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(plan.repeat.label(language), color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Checkbox(checked = plan.enabled, onCheckedChange = { onToggle() })
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(start = 6.dp),
    ) {
        TextButton(onClick = onAddDose) { Text(language.t("记录", "Log"), color = E2Pink) }
        TextButton(onClick = onAddReminder) { Text(language.t("提醒", "Calendar"), color = TBlue) }
        TextButton(onClick = onDelete) { Text(language.t("删除", "Delete"), color = Color(0xFFDC2626)) }
    }
}

private fun categoryAccent(plan: MedicationPlan): Color =
    when (plan.category) {
        MedicationCategory.Estradiol -> E2Pink
        MedicationCategory.Cpa,
        MedicationCategory.AntiAndrogen -> CpaRose
        MedicationCategory.Testosterone -> TBlue
    }

private fun MedicationPlan.shortBadge(): String =
    when (category) {
        MedicationCategory.Estradiol -> compound.wire
        MedicationCategory.Cpa -> "CPA"
        MedicationCategory.Testosterone -> compound.wire
        MedicationCategory.AntiAndrogen -> recordOnlyMedication?.let {
            when (it) {
                RecordOnlyMedication.CyproteroneAcetate -> "CP"
                RecordOnlyMedication.Spironolactone -> "SP"
                RecordOnlyMedication.Bicalutamide -> "BI"
                RecordOnlyMedication.Finasteride -> "FI"
                RecordOnlyMedication.Dutasteride -> "DU"
            }
        } ?: "AN"
    }

private fun PlanDoseStatus.label(language: AppLanguage): String =
    language.t(
        labelZh,
        when (this) {
            PlanDoseStatus.Pending -> "Pending"
            PlanDoseStatus.Taken -> "Taken"
            PlanDoseStatus.Skipped -> "Skipped"
            PlanDoseStatus.Missed -> "Missed"
        },
    )

@Composable
private fun PlanDoseStatus.statusColor(): Color =
    when (this) {
        PlanDoseStatus.Pending -> TBlue
        PlanDoseStatus.Taken -> E2Pink
        PlanDoseStatus.Skipped -> Muted
        PlanDoseStatus.Missed -> Color(0xFFDC2626)
    }

private fun PlanRepeat.label(language: AppLanguage): String =
    language.t(
        labelZh,
        when (this) {
            PlanRepeat.Daily -> "Daily"
        PlanRepeat.Weekly -> "Weekly"
        },
    )

@Composable
private fun PlanEmptyState(title: String, body: String) {
    NativeCard {
        Text(title, color = Ink, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Text(body, color = Muted, lineHeight = 20.sp, modifier = Modifier.padding(top = 6.dp))
    }
}
