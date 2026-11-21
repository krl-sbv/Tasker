package com.kirillshybaiev.dailytracker

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.luminance
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import java.io.File
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Period
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JTextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ═══════════════ InsightsScreen ═══════════════

data class DayStat(val date: LocalDate, val planned: Int, val done: Int) {
    val rate: Float? get() = if (planned > 0) (done.toFloat() / planned).coerceAtMost(1f) else null
}

data class Note(val text: String, val tone: Int)

data class Advice(val title: String, val detail: String, val gain: Int, val score: Float)

data class Analysis(
    val rate: Float?, val dayRates: List<Float?>, val bestStreak: Int, val notes: List<Note>,
    val tip: String = "", val advice: List<Advice> = emptyList()
)

fun pct(x: Float?) = if (x == null) "–" else "${(x * 100).roundToInt()}%"
fun fmt1(x: Float) = String.format(Locale.getDefault(), "%.1f", x)

fun rateOf(list: List<DayStat>): Float? {
    val planned = list.sumOf { it.planned }
    return if (planned > 0) list.sumOf { it.done }.toFloat() / planned else null
}

fun computeStats(state: AppState, today: LocalDate, days: Int): List<DayStat> {
    val ids = state.tasks.map { it.id }.toSet()
    val byDate = state.history.groupBy { it.date }
    return ((days - 1) downTo 0).map { i ->
        val d = today.minusDays(i.toLong())
        val sched = state.tasksOn(d)
        val deletedDone = byDate[d].orEmpty().count { it.taskId !in ids }
        DayStat(d, sched.size + deletedDone, sched.count { state.isDone(it, d) } + deletedDone)
    }
}

fun analyze(state: AppState, all: List<DayStat>, range: Int, today: LocalDate): Analysis {
    val cur = all.takeLast(range)
    val prev = all.dropLast(range).takeLast(range)
    val rate = rateOf(cur)
    val prevRate = rateOf(prev)

    var run = 0
    var best = 0
    for (d in all) {
        if (d.planned == 0) continue
        if (d.done >= d.planned) { run++; best = max(best, run) } else run = 0
    }

    val notes = mutableListOf<Note>()
    var fix: String? = null
    if (state.tasks.none { it.isTask } && state.history.isEmpty()) {
        notes += Note("Add a few tasks and finish them. Your insights will show up here.", 0)
        return Analysis(rate, cur.map { it.rate }, best, notes, notes.first().text)
    }
    if (rate == null) {
        notes += Note("Nothing was scheduled in the last $range days.", 0)
        return Analysis(rate, cur.map { it.rate }, best, notes, notes.first().text)
    }
    val past = cur.filter { it.date != today }

    val planDays = cur.filter { it.planned > 0 }
    if (planDays.isNotEmpty()) {
        val avgPlanned = planDays.sumOf { it.planned }.toFloat() / planDays.size
        val avgDone = planDays.sumOf { it.done }.toFloat() / planDays.size
        if (avgPlanned >= 4f && rate < 0.7f) {
            val cut = ceil(avgPlanned - avgDone).toInt().coerceAtLeast(1)
            notes += Note("You plan ${fmt1(avgPlanned)} tasks a day but finish ${fmt1(avgDone)}. Try removing or merging $cut of them.", -1)
            if (fix == null) fix = "Plan fewer tasks: you finish about ${fmt1(avgDone)} of ${fmt1(avgPlanned)} a day."
        }
    }

    data class TR(val task: Task, val done: Int, val total: Int) { val r get() = done.toFloat() / total }
    val perTask = state.tasks.filter { it.isTask }.mapNotNull { t ->
        val sched = past.filter { t.occursOn(it.date) && !state.isSkipped(t, it.date) }
        if (sched.size < 4) null else TR(t, sched.count { state.isDone(t, it.date) }, sched.size)
    }
    val worst = perTask.minByOrNull { it.r }
    if (worst != null && worst.r < 0.6f) {
        val tip = if (worst.task.time == null) "Give it a reminder time." else "Try a lighter schedule, for example every other day."
        notes += Note("“${worst.task.title}” slips the most: done ${worst.done} of ${worst.total} times (${pct(worst.r)}). $tip", -1)
        if (fix == null) fix = if (worst.task.time == null) "Add a reminder time to “${worst.task.title}”." else "Try a lighter schedule for “${worst.task.title}”."
    }

    val withTime = perTask.filter { it.task.time != null }
    val noTime = perTask.filter { it.task.time == null }
    if (withTime.isNotEmpty() && noTime.isNotEmpty()) {
        val a1 = withTime.sumOf { it.done }.toFloat() / withTime.sumOf { it.total }
        val a2 = noTime.sumOf { it.done }.toFloat() / noTime.sumOf { it.total }
        if (a1 - a2 >= 0.15f) {
            val names = noTime.filter { it.r < 0.7f }.sortedBy { it.r }.take(2).joinToString(", ") { "“${it.task.title}”" }
            notes += Note("Tasks with a reminder get done ${pct(a1)} of the time, without one ${pct(a2)}." + if (names.isNotEmpty()) " Add a time to $names." else "", 0)
            if (fix == null && names.isNotEmpty()) fix = "Add a reminder time to $names."
        }
    }

    val byDay = DayOfWeek.values().map { dow -> dow to past.filter { it.date.dayOfWeek == dow && it.planned > 0 } }
        .filter { it.second.size >= 2 }.map { it.first to rateOf(it.second)!! }
    if (byDay.size >= 3) {
        val hi = byDay.maxByOrNull { it.second }!!
        val lo = byDay.minByOrNull { it.second }!!
        if (hi.second - lo.second >= 0.2f) {
            val n = { d: DayOfWeek -> d.getDisplayName(JTextStyle.FULL, Locale.getDefault()) }
            notes += Note("${n(lo.first)} is your weakest day (${pct(lo.second)}), ${n(hi.first)} your best (${pct(hi.second)}). Plan less on ${n(lo.first)}.", -1)
            if (fix == null) fix = "Plan less on ${n(lo.first)}, it is your weakest day."
        }
    }

    if (prevRate != null) {
        val pts = (abs(rate - prevRate) * 100).roundToInt()
        when {
            rate - prevRate >= 0.05f -> notes += Note("Up $pts points on the previous $range days (${pct(prevRate)} → ${pct(rate)}). Whatever you changed, keep it.", 1)
            prevRate - rate >= 0.05f -> {
                notes += Note("Down $pts points on the previous $range days (${pct(prevRate)} → ${pct(rate)}). Look at the first note above for the likely cause.", -1)
                if (fix == null) fix = "You slipped $pts points: finish one task early each day to turn it around."
            }
        }
    }

    val streak = state.streak(today)
    when {
        streak >= 3 && streak >= best -> notes += Note("Longest streak of the year: $streak days. Don't break it today.", 1)
        streak > 0 -> notes += Note("Streak: $streak days. Your best is $best: ${best - streak + 1} more days to beat it.", 0)
        best > 0 -> notes += Note("No active streak. Finish everything today to start a new one (best: $best days).", 0)
    }
    if (notes.isEmpty() || notes.all { it.tone >= 0 } && rate >= 0.85f) {
        notes.add(0, Note("Very consistent at ${pct(rate)}. If it feels easy, add one small new habit.", 1))
    }
    val tip = fix ?: if (rate >= 0.85f) "Very consistent. If it feels easy, add one small new habit." else "Keep going: finish one task early each day."
    val advice = buildAdvice(state, cur, past, rate, perTask.map { Triple(it.task, it.done, it.total) }, today)
    return Analysis(rate, cur.map { it.rate }, best, notes.take(4), advice.firstOrNull()?.title ?: tip, advice)
}

private fun plural(n: Int, one: String, many: String) = if (n == 1) "1 $one" else "$n $many"

fun buildAdvice(
    state: AppState, cur: List<DayStat>, past: List<DayStat>, rate: Float,
    perTask: List<Triple<Task, Int, Int>>, today: LocalDate
): List<Advice> {
    val out = mutableListOf<Advice>()
    val plannedAll = cur.sumOf { it.planned }
    val doneAll = cur.sumOf { it.done }
    fun points(x: Float) = (x * 100).roundToInt().coerceAtLeast(0)

    val todayTasks = state.tasksOn(today)
    val left = todayTasks.count { !state.isDone(it, today) }
    val streak = state.streak(today)
    if (left > 0 && streak >= 2) {
        out += Advice(
            "Keep your $streak-day streak",
            "${plural(left, "task", "tasks")} left today. Finish ${if (left == 1) "it" else "them"} to reach ${streak + 1} days.",
            0, 0.35f
        )
    }

    val planDays = cur.filter { it.planned > 0 }
    if (planDays.size >= 5) {
        val avgPlanned = planDays.sumOf { it.planned }.toFloat() / planDays.size
        val avgDone = planDays.sumOf { it.done }.toFloat() / planDays.size
        if (avgPlanned >= 3f && rate < 0.75f) {
            val cut = ceil(avgPlanned - avgDone).toInt().coerceIn(1, (avgPlanned - 1).toInt().coerceAtLeast(1))
            val newRate = (avgDone / (avgPlanned - cut).coerceAtLeast(1f)).coerceAtMost(1f)
            val gain = points(newRate - rate)
            out += Advice(
                "Plan ${plural(cut, "task", "tasks")} fewer a day",
                "You plan ${fmt1(avgPlanned)} and finish ${fmt1(avgDone)}. Planning ${fmt1(avgPlanned - cut)} would put you near ${pct(newRate)}.",
                gain, gain / 100f + 0.1f
            )
        }
    }

    val withTime = perTask.filter { it.first.time != null }
    val timeRate = if (withTime.isNotEmpty()) withTime.sumOf { it.second }.toFloat() / withTime.sumOf { it.third } else null
    perTask.filter { it.second.toFloat() / it.third < 0.6f }.sortedBy { it.second.toFloat() / it.third }.take(2).forEach { (t, done, total) ->
        val r = done.toFloat() / total
        val missed = total - done
        val without = if (plannedAll - total > 0) (doneAll - done).toFloat() / (plannedAll - total) else rate
        val daily = t.repeating && ((t.unit == 0 && t.interval == 1 && t.weekdays == 0) || t.weekdays == 127)
        val name = "“${t.title}”"
        val a = when {
            done == 0 && total >= 5 -> Advice(
                "Pause or remove $name", "Not done once in its last $total days. Dropping it alone lifts you to ${pct(without)}.",
                points(without - rate), (without - rate) + 0.2f
            )
            t.time == null -> Advice(
                "Give $name a reminder time",
                "Done $done of $total times (${pct(r)})." + if (timeRate != null && timeRate > r) " Your tasks with a time get done ${pct(timeRate)} of the time." else "",
                points(missed.toFloat() / plannedAll * 0.5f), missed.toFloat() / plannedAll + 0.05f
            )
            daily -> Advice(
                "Make $name every other day", "Done $done of $total days (${pct(r)}). Fewer days you keep beat many you miss.",
                points(missed.toFloat() / plannedAll * 0.5f), missed.toFloat() / plannedAll
            )
            else -> Advice(
                "Move $name to an easier time", "Done $done of $total times (${pct(r)}). Try a slot where your other tasks get done.",
                points(missed.toFloat() / plannedAll * 0.4f), missed.toFloat() / plannedAll * 0.8f
            )
        }
        out += a
    }

    val byDay = DayOfWeek.values().map { dow -> dow to past.filter { it.date.dayOfWeek == dow && it.planned > 0 } }
        .filter { it.second.size >= 2 }.map { Triple(it.first, rateOf(it.second)!!, it.second.sumOf { d -> d.planned }) }
    if (byDay.size >= 3) {
        val hi = byDay.maxByOrNull { it.second }!!
        val lo = byDay.minByOrNull { it.second }!!
        if (hi.second - lo.second >= 0.2f && plannedAll > 0) {
            val n = { d: DayOfWeek -> d.getDisplayName(JTextStyle.FULL, Locale.getDefault()) }
            val gain = (hi.second - lo.second) * lo.third / plannedAll * 0.6f
            out += Advice(
                "Plan less on ${n(lo.first)}", "${n(lo.first)}: ${pct(lo.second)} done. ${n(hi.first)}: ${pct(hi.second)}. Move one task to a stronger day.",
                points(gain), gain + 0.03f
            )
        }
    }

    fun part(h: Int) = when { h < 12 -> 0; h < 17 -> 1; else -> 2 }
    val partNames = listOf("Morning", "Afternoon", "Evening")
    val parts = withTime.groupBy { part(it.first.time!!.hour) }
        .mapValues { (_, l) -> l.sumOf { it.second }.toFloat() / l.sumOf { it.third } to l.sumOf { it.third } }
        .filter { it.value.second >= 6 }
    if (parts.size >= 2) {
        val best = parts.maxByOrNull { it.value.first }!!
        val worst = parts.minByOrNull { it.value.first }!!
        if (best.value.first - worst.value.first >= 0.2f && plannedAll > 0) {
            val gain = (best.value.first - worst.value.first) * worst.value.second / plannedAll * 0.5f
            out += Advice(
                "Shift ${partNames[worst.key].lowercase()} tasks to the ${partNames[best.key].lowercase()}",
                "${partNames[worst.key]} tasks: ${pct(worst.value.first)} done. ${partNames[best.key]}: ${pct(best.value.first)}.",
                points(gain), gain + 0.02f
            )
        }
    }

    val last7 = rateOf(cur.takeLast(7))
    val prev7 = rateOf(cur.dropLast(7).takeLast(7))
    if (last7 != null && prev7 != null && prev7 - last7 >= 0.15f) {
        out += Advice(
            "Get back on track this week", "This week ${pct(last7)}, the week before ${pct(prev7)}. Start each day with your easiest task.",
            0, (prev7 - last7) * 0.5f
        )
    }

    val overdue = state.overdue(today).size
    if (overdue >= 3) {
        out += Advice("Clear $overdue overdue tasks", "Move them to a new day or dismiss them so today stays clear.", 0, 0.04f)
    }

    if (out.isEmpty()) {
        out += if (rate >= 0.85f) Advice("Ready for one more habit", "You finish ${pct(rate)} of what you plan. Add one small daily task.", 0, 0f)
        else Advice("Finish one task early each day", "Starting the day with a quick win makes the rest easier.", 0, 0f)
    }
    return out.sortedByDescending { it.score }.take(3)
}

@Composable
fun AdviceList(advice: List<Advice>) {
    val cs = MaterialTheme.colorScheme
    advice.forEachIndexed { i, ad ->
        Spacer(Modifier.height(if (i == 0) 12.dp else 16.dp))
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(26.dp).clip(CircleShape).background(cs.primary.copy(alpha = if (isDarkUi()) 0.2f else 0.1f)),
                contentAlignment = Alignment.Center
            ) { Text("${i + 1}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = cs.primary) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(ad.title, fontSize = 16.sp, fontWeight = FontWeight.Medium, lineHeight = 21.sp)
                Spacer(Modifier.height(2.dp))
                Text(ad.detail, fontSize = 13.sp, lineHeight = 18.sp, color = cs.onSurfaceVariant)
            }
            if (ad.gain > 0) {
                Spacer(Modifier.width(8.dp))
                Text(
                    "+${ad.gain}%", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GREEN,
                    modifier = Modifier.clip(RShape(10.dp)).background(GREEN.copy(alpha = 0.13f)).padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
fun InsightsScreen(state: AppState, onClose: () -> Unit, onSwipeHintSeen: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val today = rememberToday()
    var range by remember { mutableStateOf(30) }
    var pickedId by remember { mutableStateOf<String?>(null) }
    val all = remember(state) { computeStats(state, today, 365) }
    val a = remember(state, range) { analyze(state, all, range, today) }
    val streak = state.streak(today)
    val week = state.rateBetween(weekStart(today, state.weekStartDay), today)
    val ranges = listOf(7, 30, 90)
    val swipeRange = Modifier.pointerInput(Unit) {
        var total = 0f
        var fired = false
        detectHorizontalDragGestures(
            onDragStart = { total = 0f; fired = false },
            onHorizontalDrag = { change, dx ->
                change.consume()
                total += dx
                if (!fired && abs(total) > 48.dp.toPx()) {
                    fired = true
                    val i = ranges.indexOf(range).coerceAtLeast(0)
                    range = ranges[(if (total < 0) i + 1 else i - 1).coerceIn(0, ranges.size - 1)]
                }
            }
        )
    }
    val pager = rememberPagerState(initialPage = 0) { 2 }
    val scope = rememberCoroutineScope()

    var showHint by remember { mutableStateOf(false) }
    val needHint = !state.seenInsightsSwipe
    LaunchedEffect(needHint) {
        if (needHint) {
            delay(900)
            showHint = true
            delay(5000)
            showHint = false
            onSwipeHintSeen()
        }
    }
    LaunchedEffect(pager.isScrollInProgress) {
        if (pager.isScrollInProgress && needHint) { showHint = false; onSwipeHintSeen() }
    }

    LargeTitleScaffold(tx("Insights"), modifier, onBack = onClose) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize().rubberBand(vertical = false), beyondViewportPageCount = 1) { page ->
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp).padding(top = 6.dp)
                        .padding(bottom = bottomSpace(100.dp))
                ) {
                    if (page == 0) {
                        MinimalOverview(a, range, ranges, { range = it }, streak, week, swipeRange) {
                            if (needHint) { showHint = false; onSwipeHintSeen() }
                            scope.launch { pager.animateScrollToPage(1) }
                        }
                    } else {
                        val tasks = state.tasks.filter { it.repeating && it.isTask }
                        if (tasks.isEmpty()) {
                            Text(
                                "Add a repeating task first. Its stats will show up here.",
                                color = cs.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp, top = 24.dp)
                            )
                        } else {
                            val task = tasks.find { it.id == pickedId } ?: tasks.first()
                            MenuButton(
                                task.title, fill = true,
                                leading = { Box(Modifier.size(12.dp).clip(CircleShape).background(task.color?.let { Color(it) } ?: cs.primary)) }
                            ) { close ->
                                tasks.forEach { t2 ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(t2.title, color = if (t2.id == task.id) cs.primary else cs.onSurface)
                                                Text(t2.repeatLabel(), fontSize = 12.sp, color = cs.onSurfaceVariant)
                                            }
                                        },
                                        leadingIcon = { Box(Modifier.size(10.dp).clip(CircleShape).background(t2.color?.let { Color(it) } ?: cs.primary)) },
                                        onClick = { pickedId = t2.id; close() }
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            TaskStatsContent(state, task, today)
                        }
                    }
                }
            }
            SwipeHint(showHint, tx("Swipe to switch"), Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 76.dp))
            AutoHidePill(
                labels = listOf(tx("Overview"), tx("Per task")), pager = pager, springy = LocalSpringy.current,
                onSelect = { i ->
                    if (needHint) { showHint = false; onSwipeHintSeen() }
                    scope.launch { pager.animateScrollToPage(i) }
                },
                forceVisible = showHint, startVisible = true,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 14.dp)
            )
        }
    }
}

@Composable
fun DayBars(rates: List<Float?>, modifier: Modifier, minimal: Boolean = false) {
    val cs = MaterialTheme.colorScheme
    val grid = cs.outline.copy(alpha = 0.35f)
    val stub = cs.outline.copy(alpha = 0.5f)
    val partial = cs.onBackground.copy(alpha = 0.75f)
    val springy = LocalSpringy.current
    val grow = remember { Animatable(0f) }
    LaunchedEffect(rates) {
        grow.snapTo(if (springy) 0f else 1f)
        grow.animateTo(1f, if (springy) spring(dampingRatio = 0.7f, stiffness = 120f) else snap())
    }
    Canvas(modifier) {
        val n = rates.size.coerceAtLeast(1)
        val gap = 2.dp.toPx()
        val bw = (size.width - gap * (n - 1)) / n
        if (!minimal) listOf(0f, 0.5f, 1f).forEach { f ->
            val y = size.height * (1f - f)
            drawLine(grid, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
        }
        val lastIndex = rates.indexOfLast { it != null }
        val win = (rates.size / 8).coerceIn(2, 10)
        val pts = mutableListOf<Offset>()
        rates.forEachIndexed { i, _ ->
            val w = rates.subList(max(0, i - win + 1), i + 1).filterNotNull()
            if (w.isNotEmpty()) pts += Offset(i * (bw + gap) + bw / 2f, size.height * (1f - w.average().toFloat() * grow.value))
        }
        rates.forEachIndexed { i, r ->
            val x = i * (bw + gap)
            if (r == null) {
                drawRoundRect(stub, Offset(x, size.height - 3.dp.toPx()), Size(bw, 3.dp.toPx()), CornerRadius(bw / 2f))
            } else {
                val h = max(r * size.height * grow.value, 3.dp.toPx())
                val c = if (minimal) {
                    if (i == lastIndex) cs.primary else cs.onSurface.copy(alpha = 0.1f + 0.38f * r)
                } else when { r >= 0.999f -> GREEN; r > 0f -> partial; else -> RED.copy(alpha = 0.8f) }
                drawRoundRect(c, Offset(x, size.height - h), Size(bw, h), CornerRadius(min(bw / 2f, 4.dp.toPx())))
            }
        }
        if (!minimal && pts.size >= 2) {
            val path = Path().apply { moveTo(pts[0].x, pts[0].y); pts.drop(1).forEach { lineTo(it.x, it.y) } }
            drawPath(path, cs.onBackground, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(
        text, fontSize = 14.sp, fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier
    )
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Eyebrow(label)
            Spacer(Modifier.height(4.dp))
            Spacer(Modifier.height(4.dp))
            SwapText(value, fontSize = 40.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun MinimalOverview(
    a: Analysis, range: Int, ranges: List<Int>, onRange: (Int) -> Unit,
    streak: Int, week: Float?, swipe: Modifier, onPerTask: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().then(swipe)) {
        Segmented(ranges.map { "$it ${tx("days")}" }, ranges.indexOf(range).coerceAtLeast(0), { onRange(ranges[it]) })

        Spacer(Modifier.height(12.dp))
        Card {
            Column(Modifier.padding(20.dp)) {
                Eyebrow(tx("Completion"))
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    SwapText(pct(a.rate), fontSize = 40.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${tx("last")} $range ${tx("days")}", fontSize = 13.sp, color = cs.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Spacer(Modifier.height(18.dp))
                Eyebrow(tx("Daily completion"))
                Spacer(Modifier.height(10.dp))
                DayBars(a.dayRates, Modifier.fillMaxWidth().height(88.dp), minimal = true)
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(tx("Streak"), "$streak", Modifier.weight(1f))
            StatCard(tx("This week"), pct(week), Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))
        Card {
            Column(Modifier.padding(20.dp)) {
                Eyebrow(tx("To improve"))
                if (a.advice.isEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(a.tip, fontSize = 15.sp, lineHeight = 22.sp)
                } else AdviceList(a.advice)
            }
        }

    }
}

// ═══════════════ TaskDetailScreen ═══════════════

@Composable
fun TaskDetailScreen(state: AppState, taskId: String, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    val task = state.tasks.find { it.id == taskId }
    LargeTitleScaffold(
        task?.title ?: "", modifier,
        subtitle = task?.let { "${it.repeatLabel()} · since ${fmtDate(it.date)}" } ?: "",
        onBack = onClose
    ) {
        if (task != null) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp).padding(top = 6.dp)
                    .navigationBarsPadding().padding(bottom = 28.dp)
            ) { TaskStatsContent(state, task, today) }
        }
    }
}

@Composable
fun TaskStatsContent(state: AppState, task: Task, today: LocalDate) {
    val cs = MaterialTheme.colorScheme
    val st = remember(state, task) { state.taskStats(task, today) }
    Column {
        Card {
            Column(Modifier.padding(20.dp)) {
                Eyebrow(tx("Completion"))
                SwapText("${st.percent}%", fontSize = 56.sp, fontWeight = FontWeight.SemiBold)
                Text("${st.done} ${tx("out of")} ${st.total} ${tx("days")}", fontSize = 13.sp, color = cs.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(tx("Streak"), "${st.current}", Modifier.weight(1f))
            StatCard(tx("Finished"), "${st.done}", Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Card {
            Column(Modifier.padding(20.dp)) {
                Eyebrow(tx("Last 20 weeks"))
                Spacer(Modifier.height(14.dp))
                Heatmap(state, task, today, Modifier.fillMaxWidth().aspectRatio(20f / 7f))
            }
        }
        Spacer(Modifier.height(12.dp))
        Card {
            Column(Modifier.padding(20.dp)) {
                Eyebrow(tx("Notes"))
                Spacer(Modifier.height(8.dp))
                Text(taskNote(state, task, today, st), fontSize = 16.sp, lineHeight = 23.sp)
                if (task.description.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(task.description, fontSize = 15.sp, lineHeight = 21.sp, color = cs.onSurfaceVariant)
                }
            }
        }
    }
}

private fun taskNote(state: AppState, task: Task, today: LocalDate, st: TaskStats): String {
    if (st.total < 3) return "Not enough days yet to see a pattern. Keep going!"
    val misses = IntArray(7)
    var missed = 0
    var d = maxOf(task.date, today.minusDays(1095))
    while (d.isBefore(today)) {
        if (task.occursOn(d) && !state.isSkipped(task, d) && !state.isDone(task, d)) {
            misses[d.dayOfWeek.value - 1]++
            missed++
        }
        d = d.plusDays(1)
    }
    if (missed >= 3) {
        val worst = misses.indices.maxByOrNull { misses[it] } ?: 0
        if (misses[worst] * 100 / missed >= 40) {
            return "You miss this one most often on ${DayOfWeek.of(worst + 1).getDisplayName(JTextStyle.FULL, Locale.getDefault())}s."
        }
    }
    return when {
        st.percent >= 90 -> "Rock solid. This habit is well established."
        st.percent >= 60 -> "Going well, with a few misses along the way."
        else -> "This one needs attention: it's slipping more often than not."
    }
}

@Composable
private fun Heatmap(state: AppState, task: Task, today: LocalDate, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    val weeks = 20
    val start = weekStart(today, state.weekStartDay).minusWeeks((weeks - 1).toLong())
    val cells = remember(state, task, today) {
        IntArray(weeks * 7) { idx ->
            val c = idx / 7
            val r = idx % 7
            val day = start.plusDays((c * 7 + r).toLong())
            when {
                day.isAfter(today) -> 0
                !task.occursOn(day) -> 0
                state.isSkipped(task, day) -> 1
                state.isDone(task, day) -> 2
                day == today -> 4
                else -> 3
            }
        }
    }
    val faint = cs.onSurface.copy(alpha = 0.06f)
    val zero = cs.onSurfaceVariant.copy(alpha = 0.4f)
    val full = GREEN
    val ring = cs.primary
    Canvas(modifier) {
        val gap = 3.dp.toPx()
        val cell = (size.width - gap * (weeks - 1)) / weeks
        for (c in 0 until weeks) {
            for (r in 0 until 7) {
                val x = c * (cell + gap)
                val y = r * (cell + gap)
                val kind = cells[c * 7 + r]
                val color = when (kind) { 2 -> full; 3 -> zero; else -> faint }
                drawRoundRect(color, Offset(x, y), Size(cell, cell), CornerRadius(cell * 0.28f))
                if (kind == 4) drawRoundRect(ring, Offset(x, y), Size(cell, cell), CornerRadius(cell * 0.28f), style = Stroke(1.5.dp.toPx()))
            }
        }
    }
}

// ═══════════════ SettingsScreen ═══════════════

@Composable
fun SettingsPage(
    state: AppState, tick: Int, vm: TrackerViewModel,
    onImport: () -> Unit, onExport: () -> Unit,
    onRepeating: () -> Unit,
    onLanguage: () -> Unit, onHolidays: () -> Unit, modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    var confirmReset by remember { mutableStateOf(false) }
    val exact = remember(tick) { Reminders.canExact(ctx) }
    val themeNames = listOf(tx("System"), tx("Light"), tx("Dark"), tx("Black"))
    val blurNames = listOf(tx("Off"), tx("Light"), tx("Strong"))
    val langCode = Lang.resolve(state.language)
    val version = remember {
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: ""
    }

    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp).padding(bottom = bottomSpace(32.dp))
    ) {
        GroupLabel(tx("Appearance"), Modifier.padding(top = 0.dp))
        CardGroup {
            Column(Modifier.padding(16.dp)) {
                Text(tx("Theme"), fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))
                Segmented(themeNames, state.theme, { vm.setTheme(it) })
            }
            Column(Modifier.padding(16.dp)) {
                Text(tx("Accent color"), fontSize = 16.sp)
                SwapText(if (state.accent == 0) tx("Follows your phone") else " ", fontSize = 13.sp, color = cs.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                AccentPicker(state.accent) { vm.setAccent(it) }
            }
            SettingRow(tx("Animations"), tx("Springy, fluid motion")) {
                SSwitch(checked = state.springy, onCheckedChange = { vm.setSpringy(it) })
            }
            SettingRow(tx("Blur"), tx("Blurs the screen behind sheets and screens")) {
                MenuButton(blurNames[state.blurMode]) { close ->
                    blurNames.forEachIndexed { i, n -> MenuItem(n, i == state.blurMode) { vm.setBlur(i); close() } }
                }
            }
        }

        GroupLabel(tx("Language & holidays"))
        CardGroup {
            SettingRow(tx("Language"), tx("Language of the app"), onClick = onLanguage) {
                Value(if (state.language.isEmpty()) tx("Device") + " · " + Lang.nativeName(langCode) else Lang.nativeName(langCode))
            }
            SettingRow(tx("Holidays"), tx("Country of the public holidays"), onClick = onHolidays) {
                Value(if (state.country == "AUTO") tx("Automatic") else Holidays.countryName(state.country))
            }
            SettingRow(tx("Show holidays"), tx("Mark them in the calendar")) {
                SSwitch(checked = state.showHolidays, onCheckedChange = { vm.setShowHolidays(it) })
            }
        }

        GroupLabel(tx("Tasks"))
        CardGroup {
            SettingRow(
                tx("Repeating tasks"), "${state.tasks.count { it.repeating && it.isTask }} · ${tx("Review and fix schedules")}",
                onClick = onRepeating
            ) { Value("") }
        }

        GroupLabel(tx("Notifications"))
        CardGroup {
            SettingRow(
                tx("Exact reminders"), if (exact) tx("On · alarms arrive on time") else tx("Off · tap to allow"),
                onClick = {
                    if (Build.VERSION.SDK_INT >= 31) {
                        ctx.startActivity(
                            Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}"))
                        )
                    }
                }
            ) { Value("") }
            SettingRow(tx("Evening nudge"), tx("A reminder at 19:00–20:00 if tasks are left")) {
                SSwitch(checked = state.nudgeOn, onCheckedChange = { vm.setNudge(it) })
            }
            SettingRow(tx("Weekly review"), tx("A short summary every week")) {
                SSwitch(checked = state.reviewOn, onCheckedChange = { vm.setReview(it) })
            }
        }

        GroupLabel(tx("Calendar"))
        CardGroup {
            SettingRow(tx("Week starts on"), tx("First day in calendar and charts")) {
                MenuButton(DayOfWeek.of(state.weekStartDay).getDisplayName(JTextStyle.FULL, Locale.getDefault())) { close ->
                    DayOfWeek.values().forEach { d ->
                        MenuItem(d.getDisplayName(JTextStyle.FULL, Locale.getDefault()), d.value == state.weekStartDay) {
                            vm.setWeekStart(d.value); close()
                        }
                    }
                }
            }
        }

        GroupLabel(tx("Data"))
        CardGroup {
            SettingRow(tx("Import"), tx("Add tasks from a backup file"), onClick = onImport) { Value("") }
            SettingRow(tx("Export"), tx("Save tasks, history and settings"), onClick = onExport) { Value("") }
        }
        Spacer(Modifier.height(12.dp))
        Card(onClick = { confirmReset = true }) {
            Text(
                tx("Reset all data"), color = RED, fontSize = 16.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            )
        }
        Text(
            "Tasker $version", fontSize = 12.sp, color = cs.onSurfaceVariant.copy(alpha = 0.7f), textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
        )
    }

    if (confirmReset) {
        CDialog(
            title = tx("Reset all data?"),
            text = tx("All tasks and your finished history will be deleted. Export first if you want a copy."),
            confirm = tx("Reset"), destructive = true,
            onConfirm = { vm.resetData(); confirmReset = false }, onDismiss = { confirmReset = false }
        )
    }
}

@Composable
private fun AccentPicker(selected: Int, onPick: (Int) -> Unit) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val dark = cs.background.luminance() < 0.5f
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        (0 until ACCENT_COUNT).forEach { i ->
            val c = accentColor(ctx, i, dark)
            val on = i == selected
            val ring by animateDpAsState(if (on) 2.dp else 0.dp, spring(dampingRatio = 0.8f, stiffness = 600f), label = "accentRing")
            Box(
                Modifier.size(38.dp).then(if (ring > 0.dp) Modifier.border(ring, c, CircleShape) else Modifier).padding(4.dp)
                    .springClick(CircleShape, 0.85f) { onPick(i) }
                    .background(
                        if (i == 0) Brush.sweepGradient(listOf(c, Color(0xFF1FB8B0), Color(0xFFFF8A34), Color(0xFFF0386B), c))
                        else Brush.linearGradient(listOf(c, c))
                    ),
                contentAlignment = Alignment.Center
            ) {
                val k by animateFloatAsState(if (on) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 600f), label = "accentCheck")
                if (k > 0.01f) {
                    Icon(
                        Icons.Default.Check, null, tint = onColor(c),
                        modifier = Modifier.size(16.dp).graphicsLayer { scaleX = k; scaleY = k; alpha = k.coerceIn(0f, 1f) }
                    )
                }
            }
        }
    }
}

@Composable
fun MenuItem(label: String, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    DropdownMenuItem(
        text = { Text(label, color = if (selected) cs.primary else cs.onSurface, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal) },
        trailingIcon = if (selected) ({ Icon(Icons.Default.Check, null, tint = cs.primary, modifier = Modifier.size(18.dp)) }) else null,
        onClick = onClick
    )
}

@Composable
fun SettingsScreen(
    state: AppState, tick: Int, vm: TrackerViewModel, onClose: () -> Unit,
    onImport: () -> Unit, onExport: () -> Unit, onRepeating: () -> Unit,
    onLanguage: () -> Unit, onHolidays: () -> Unit, modifier: Modifier = Modifier
) {
    LargeTitleScaffold(tx("Settings"), modifier, onBack = onClose) {
        SettingsPage(
            state, tick, vm, onImport, onExport, onRepeating, onLanguage, onHolidays,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun Value(text: String) {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (text.isNotEmpty()) {
            Text(text, fontSize = 14.sp, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 150.dp))
            Spacer(Modifier.width(4.dp))
        }
        Chevron()
    }
}

@Composable
fun SectionLabel(text: String) = GroupLabel(text)

@Composable
fun SettingRow(title: String, subtitle: String, onClick: (() -> Unit)? = null, trailing: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val src = remember { MutableInteractionSource() }
    val press = pressProgress(src)
    CompositionLocalProvider(LocalIconPress provides press) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.springClick(RectangleShape, 1f, source = src, onClick = onClick) else Modifier)
            .padding(start = 16.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 10.dp)) {
            Text(title, fontSize = 16.sp)
            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                SwapText(subtitle, fontSize = 13.sp, color = cs.onSurfaceVariant)
            }
        }
        trailing()
    }
    }
}

// ═══════════════ PickerScreens ═══════════════

@Composable
fun Modifier.groupItem(index: Int, count: Int): Modifier {
    val top = if (index == 0) 20.dp else 0.dp
    val bottom = if (index == count - 1) 20.dp else 0.dp
    val line = MaterialTheme.colorScheme.outline
    return this.clip(RoundedCornerShape(top, top, bottom, bottom)).background(cardColor()).drawWithContent {
        drawContent()
        if (index > 0) {
            val x = 16.dp.toPx()
            drawLine(line, Offset(x, 0f), Offset(size.width - x, 0f), 0.6.dp.toPx().coerceAtLeast(1f))
        }
    }
}

@Composable
fun PickRow(
    title: String, sub: String?, selected: Boolean, onClick: () -> Unit,
    modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}
) {
    val cs = MaterialTheme.colorScheme
    val fg by animateColorAsState(if (selected) cs.primary else cs.onSurface, label = "pickFg")
    Row(
        modifier.fillMaxWidth().springClick(RectangleShape, 1f, onClick = onClick)
            .heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, fontSize = 16.sp, color = fg, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal)
            if (sub != null) Text(sub, fontSize = 13.sp, color = cs.onSurfaceVariant)
        }
        trailing()
        AnimatedVisibility(
            visible = selected,
            enter = scaleIn(spring(dampingRatio = 0.6f, stiffness = 600f)) + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) { Icon(Icons.Default.Check, null, tint = cs.primary, modifier = Modifier.padding(start = 8.dp).size(20.dp)) }
    }
}

// ───────────── language ─────────────

@Composable
fun LanguageList(state: AppState, vm: TrackerViewModel, modifier: Modifier = Modifier, onPicked: () -> Unit = {}) {
    val deviceCode = if (Lang.deviceLocale.language in Lang.codes) Lang.deviceLocale.language else "en"
    LazyColumn(modifier, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
        item(key = "device") {
            PickRow(
                tx("Device language"), Lang.nativeName(deviceCode), state.language.isEmpty(),
                onClick = { vm.setLanguage(""); onPicked() }, modifier = Modifier.groupItem(0, 1)
            )
        }
        item(key = "label") { GroupLabel(tx("Built in · works offline")) }
        itemsIndexed(Lang.all, key = { _, it -> it.first }) { i, (code, name) ->
            PickRow(
                name, null, state.language == code, onClick = { vm.setLanguage(code); onPicked() },
                modifier = Modifier.groupItem(i, Lang.all.size)
            )
        }
        item(key = "end") { Spacer(Modifier.height(bottomSpace(24.dp))) }
    }
}

@Composable
fun LanguageScreen(state: AppState, vm: TrackerViewModel, onClose: () -> Unit, modifier: Modifier = Modifier) {
    LargeTitleScaffold(tx("Language"), modifier, subtitle = tx("Pick the language of the app"), onBack = onClose) {
        LanguageList(state, vm, Modifier.weight(1f).fillMaxWidth(), onPicked = onClose)
    }
}

// ───────────── holidays ─────────────

@Composable
fun HolidaysList(state: AppState, vm: TrackerViewModel, modifier: Modifier = Modifier, onPicked: () -> Unit = {}) {
    val cs = MaterialTheme.colorScheme
    val lang = LocalLang.current
    var query by remember { mutableStateOf("") }
    val all = remember(lang) { Holidays.allCountries() }
    val deviceCode = Lang.deviceLocale.country.uppercase()
    val q = query.trim().lowercase()
    val shown = remember(all, q) {
        all.filter { q.isEmpty() || it.second.lowercase().contains(q) || it.first.lowercase() == q }
    }

    LazyColumn(modifier.imePadding(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
        item(key = "search") {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 12.dp).height(44.dp).clip(RShape(22.dp))
                    .background(cs.onSurface.copy(alpha = 0.06f)).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    textStyle = TextStyle(color = cs.onSurface, fontSize = 16.sp),
                    cursorBrush = SolidColor(cs.primary),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        Box {
                            if (query.isEmpty()) Text(tx("Search countries"), fontSize = 16.sp, color = cs.onSurfaceVariant)
                            inner()
                        }
                    }
                )
                if (query.isNotEmpty()) {
                    SIconButton(onClick = { query = "" }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, "Clear", tint = cs.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        item(key = "auto") {
            val sub = tx("Follows your phone") + if (deviceCode.isEmpty()) "" else " · " + Holidays.countryName(deviceCode)
            PickRow(
                tx("Automatic"), sub, state.country == "AUTO", onClick = { vm.setCountry("AUTO"); onPicked() },
                modifier = Modifier.groupItem(0, 1)
            )
        }
        item(key = "labelAll") { GroupLabel("${tx("All countries")} · ${shown.size}") }
        itemsIndexed(shown, key = { _, it -> it.first }) { i, (code, name) ->
            PickRow(
                name, null, state.country == code, onClick = { vm.setCountry(code); onPicked() },
                modifier = Modifier.groupItem(i, shown.size)
            )
        }
        item(key = "end") { Spacer(Modifier.height(bottomSpace(24.dp))) }
    }
}

@Composable
fun HolidaysScreen(state: AppState, vm: TrackerViewModel, onClose: () -> Unit, modifier: Modifier = Modifier) {
    LargeTitleScaffold(tx("Holidays"), modifier, subtitle = tx("Names are in the language of the country"), onBack = onClose) {
        HolidaysList(state, vm, Modifier.weight(1f).fillMaxWidth(), onPicked = onClose)
    }
}

// ═══════════════ WidgetConfigActivity ═══════════════

class WidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }

        val cls = AppWidgetManager.getInstance(this).getAppWidgetInfo(id)?.provider?.className ?: ""
        val kind = when {
            cls.endsWith("TasksClockWidget") -> 1
            cls.endsWith("CalendarWidget") -> 2
            cls.endsWith("InsightsWidget") -> 3
            else -> 0
        }
        val tasks = Store.get(this).tasks.filter { it.repeating && it.isTask }
        val lang = Lang.resolve(Store.get(this).language)
        val ctx = this
        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(LocalLang provides lang, LocalLayoutDirection provides LayoutDirection.Ltr) {
                TrackerTheme(dark = isSystemInDarkTheme(), oled = false, springy = true, accent = Store.get(ctx).accent) {
                    val cs = MaterialTheme.colorScheme
                    Surface(Modifier.fillMaxSize(), color = cs.background, contentColor = cs.onBackground) {
                        var look by remember { mutableStateOf(WidgetPrefs.look(ctx, id)) }
                        Column(Modifier.fillMaxSize().systemBarsPadding()) {
                            WidgetPreviewPanel(kind, look)
                            Column(
                                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
                            ) {
                                SectionLabel(tx("Appearance"))
                                CardGroup {
                                SettingRow(tx("Widget background"), tx("Light, dark, black or see-through")) {
                                    MenuButton(tx(WidgetStyle.names[look.bg])) { close ->
                                        WidgetStyle.names.forEachIndexed { i, n ->
                                            DropdownMenuItem(text = { Text(tx(n)) }, onClick = { look = look.copy(bg = i); close() })
                                        }
                                    }
                                }
                                if (look.bg >= WidgetStyle.TRANSPARENT) {
                                    val names = listOf(tx("System"), tx("Light"), tx("Dark"))
                                    SettingRow(tx("Widget text color"), tx("For the see-through background")) {
                                        MenuButton(names[look.textMode.coerceIn(0, 2)]) { close ->
                                            names.forEachIndexed { i, n ->
                                                DropdownMenuItem(text = { Text(n) }, onClick = { look = look.copy(textMode = i); close() })
                                            }
                                        }
                                    }
                                }
                                SliderRow(
                                    tx("Font size"), "%.1f×".format(look.fontScale), look.fontScale, 0.6f, 2.0f, 0.1f
                                ) { look = look.copy(fontScale = (it * 10f).roundToInt() / 10f) }
                                if (kind != 3) {
                                    SliderRow(
                                        tx("Icon size"), "%.1f×".format(look.iconScale), look.iconScale, 0.6f, 1.6f, 0.1f
                                    ) { look = look.copy(iconScale = (it * 10f).roundToInt() / 10f) }
                                }
                                if (kind == 1) {
                                    SliderRow(
                                        tx("Clock size"), "${look.clockSize}", look.clockSize.toFloat(), 40f, 120f, 5f
                                    ) { look = look.copy(clockSize = it.roundToInt()) }
                                }
                                if (kind == 3) {
                                    SettingRow(tx("Weeks shown"), tx("How far back the squares go")) {
                                        MenuButton("${look.weeks} ${tx("weeks")}") { close ->
                                            listOf(4, 8, 12, 16, 20, 26).forEach { v ->
                                                DropdownMenuItem(text = { Text("$v ${tx("weeks")}") }, onClick = { look = look.copy(weeks = v); close() })
                                            }
                                        }
                                    }
                                    val shown = if (look.source == "overview") tx("Overview") else tasks.find { it.id == look.source }?.title ?: tx("Overview")
                                    SettingRow(tx("Show"), tx("All tasks or one repeating task")) {
                                        MenuButton(shown) { close ->
                                            DropdownMenuItem(text = { Text(tx("Overview")) }, onClick = { look = look.copy(source = "overview"); close() })
                                            tasks.forEach { t ->
                                                DropdownMenuItem(text = { Text(t.title) }, onClick = { look = look.copy(source = t.id); close() })
                                            }
                                        }
                                    }
                                }
                                }
                                Spacer(Modifier.height(16.dp))
                            }
                            SButton(
                                onClick = {
                                    WidgetPrefs.save(ctx, id, look)
                                    TasksWidget.refresh(ctx)
                                    setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
                                    finish()
                                },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).height(52.dp)
                            ) { Text(tx("Save widget"), fontSize = 16.sp) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SliderRow(
    title: String, valueText: String, value: Float, from: Float, to: Float, step: Float, onChange: (Float) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.padding(horizontal = 16.dp).padding(top = 14.dp, bottom = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), fontSize = 16.sp)
            Text(valueText, color = cs.onSurfaceVariant, fontSize = 14.sp, style = TextStyle(fontFeatureSettings = "tnum"))
        }
        SSlider(value, onChange, from, to, step = step)
    }
}

@Suppress("UnusedBoxWithConstraintsScope")
@Composable
private fun WidgetPreviewPanel(kind: Int, look: WidgetLook) {
    val shape = RShape(24.dp)
    val w = if (kind == 2) 240 else 320
    val h = when (kind) { 2 -> 240; 3 -> 140; else -> 180 }
    BoxWithConstraints(
        Modifier.fillMaxWidth().padding(16.dp).clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF4C6EF5), Color(0xFFB197FC), Color(0xFFFFA8A8))))
    ) {
        val maxW = maxWidth - 36.dp
        val maxH = if (kind == 2) 330.dp else 260.dp
        val scale = minOf(maxW.value / w, maxH.value / h)
        Box(Modifier.fillMaxWidth().height((h * scale).dp + 36.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.align(Alignment.TopStart).padding(start = 24.dp, top = 18.dp).size(80.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.35f)))
            Box(Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 14.dp).size(110.dp).clip(CircleShape).background(Color(0xFFFFD43B).copy(alpha = 0.55f)))
            Box(Modifier.size((w * scale).dp, (h * scale).dp), contentAlignment = Alignment.Center) {
                Box(Modifier.requiredSize(w.dp, h.dp).graphicsLayer { scaleX = scale; scaleY = scale }) {
                    if (kind <= 1) TasksPreview(kind == 1, look, w, h) else BitmapPreview(kind, look, w, h)
                }
            }
        }
    }
}

@Composable
private fun TasksPreview(clock: Boolean, look: WidgetLook, w: Int, h: Int) {
    val ctx = LocalContext.current
    val p = WidgetStyle.palette(ctx, look)
    val accent = Color(WidgetStyle.accent(ctx, p))
    val fs = look.fontScale
    val ring = (22 * look.iconScale).dp
    val rows = listOf(
        Triple(tx("Morning run"), 0xFFFF6B6B.toInt(), true),
        Triple(tx("Read 20 pages"), accent.toArgb(), true),
        Triple(tx("Plan tomorrow"), p.ring, false)
    )
    val radius = WidgetBitmaps.cornerDp(ctx).dp
    Column(
        Modifier.size(w.dp, h.dp).clip(RoundedCornerShape(radius)).background(Color(p.bgColor))
            .padding(start = 18.dp, end = 16.dp, top = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                if (clock) Text("12:40", color = Color(p.text), fontWeight = FontWeight.Medium, fontSize = (look.clockSize * 0.7f).sp)
                else Text(tx("Today"), color = accent, fontWeight = FontWeight.Medium, fontSize = (13 * fs).sp)
                Text(
                    LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))
                        .replaceFirstChar { it.titlecase(Locale.getDefault()) },
                    color = Color(if (clock) p.secondary else p.text), fontWeight = FontWeight.Bold,
                    fontSize = ((if (clock) 14 else 19) * fs).sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            val ringBmp = remember(look, p.bgColor) {
                WidgetBitmaps.progressRing(ctx, p, accent.toArgb(), 2, 3, 44f * look.iconScale.coerceIn(0.7f, 1.4f), fs).asImageBitmap()
            }
            Image(ringBmp, null, Modifier.padding(start = 10.dp).size((44f * look.iconScale.coerceIn(0.7f, 1.4f)).dp))
        }
        Spacer(Modifier.height(4.dp))
        rows.forEach { (title, c, done) ->
            Row(Modifier.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(ring).clip(CircleShape).then(if (done) Modifier.background(Color(c)) else Modifier.border(1.8.dp, Color(c), CircleShape)),
                    contentAlignment = Alignment.Center
                ) { if (done) Text("✓", color = onColor(Color(c)), fontSize = (12 * look.iconScale).sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(12.dp))
                Text(
                    title, color = Color(if (done) p.secondary else p.text), fontSize = (16 * fs).sp, maxLines = 1,
                    textDecoration = if (done) TextDecoration.LineThrough else null
                )
            }
        }
    }
}

@Composable
private fun BitmapPreview(kind: Int, look: WidgetLook, w: Int, h: Int) {
    val ctx = LocalContext.current
    val bmp = remember(kind, look) {
        if (kind == 2) WidgetBitmaps.calendar(ctx, look, w, h) else WidgetBitmaps.heatmap(ctx, look, w, h)
    }
    Image(bmp.asImageBitmap(), null, Modifier.size(w.dp, h.dp), contentScale = ContentScale.FillBounds)
}
