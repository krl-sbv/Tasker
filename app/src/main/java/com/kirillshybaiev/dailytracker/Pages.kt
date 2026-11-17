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

data class Analysis(
    val rate: Float?, val dayRates: List<Float?>, val bestStreak: Int, val notes: List<Note>,
    val tip: String = ""
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
    if (state.tasks.isEmpty() && state.history.isEmpty()) {
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
    val perTask = state.tasks.mapNotNull { t ->
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
    return Analysis(rate, cur.map { it.rate }, best, notes.take(4), tip)
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
    val block = remember { MutableInteractionSource() }
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

    Surface(
        modifier = modifier.fillMaxSize().clickable(interactionSource = block, indication = null, onClick = {}),
        color = cs.background, contentColor = cs.onBackground
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Row(Modifier.then(swipeRange).padding(start = 24.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(tx("Insights"), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                    SIconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Close") }
                }
                HorizontalPager(
                    state = pager, modifier = Modifier.weight(1f).fillMaxWidth(), beyondViewportPageCount = 1
                ) { page ->
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp).padding(top = 14.dp)
                            .padding(bottom = bottomSpace(100.dp))
                    ) {
                        if (page == 0) {
                            MinimalOverview(a, range, ranges, { range = it }, streak, week, swipeRange) {
                                if (needHint) { showHint = false; onSwipeHintSeen() }
                                scope.launch { pager.animateScrollToPage(1) }
                            }
                        } else {
                            val tasks = state.tasks.filter { it.repeating }
                            if (tasks.isEmpty()) {
                                Text(
                                    "Add a repeating task first. Its stats will show up here.",
                                    color = cs.onSurfaceVariant, modifier = Modifier.padding(top = 24.dp)
                                )
                            } else {
                                val task = tasks.find { it.id == pickedId } ?: tasks.first()
                                MenuButton(
                                    task.title, fill = true,
                                    leading = { task.color?.let { Box(Modifier.size(12.dp).clip(CircleShape).background(Color(it))) } }
                                ) { close ->
                                    tasks.forEach { t2 ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(t2.title)
                                                    Text(t2.repeatLabel(), fontSize = 12.sp, color = cs.onSurfaceVariant)
                                                }
                                            },
                                            leadingIcon = { t2.color?.let { Box(Modifier.size(10.dp).clip(CircleShape).background(Color(it))) } },
                                            onClick = { pickedId = t2.id; close() }
                                        )
                                    }
                                }
                                Spacer(Modifier.height(18.dp))
                                TaskStatsContent(state, task, today)
                            }
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
fun SectionTitle(title: String, sub: String) {
    val cs = MaterialTheme.colorScheme
    HorizontalDivider(Modifier.padding(top = 26.dp, bottom = 16.dp), color = cs.outline.copy(alpha = 0.3f))
    Row(Modifier.padding(bottom = 12.dp), verticalAlignment = Alignment.Bottom) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(sub, fontSize = 12.sp, color = cs.onSurfaceVariant)
    }
}

@Composable
fun BigStat(label: String, value: String, sub: String, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    Column(modifier) {
        Text(label, fontSize = 12.sp, color = cs.onSurfaceVariant)
        SwapText(value, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text(sub, fontSize = 12.sp, color = cs.onSurfaceVariant)
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
                    when { r >= 0.999f -> cs.onBackground; r > 0f -> cs.onBackground.copy(alpha = 0.35f); else -> cs.outline.copy(alpha = 0.6f) }
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
        text.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.6.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier
    )
}

@Composable
fun MinimalOverview(
    a: Analysis, range: Int, ranges: List<Int>, onRange: (Int) -> Unit,
    streak: Int, week: Float?, swipe: Modifier, onPerTask: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().then(swipe)) {
        val shell = RShape(16.dp)
        Row(Modifier.clip(shell).border(1.dp, cs.outline.copy(alpha = 0.5f), shell).padding(4.dp)) {
            ranges.forEach { r ->
                val on = r == range
                val bg by animateColorAsState(if (on) cs.onBackground else cs.background.copy(alpha = 0f), label = "rangeBg")
                val fg by animateColorAsState(if (on) cs.background else cs.onSurfaceVariant, label = "rangeFg")
                val seg = RShape(12.dp)
                Box(
                    Modifier.springClick(seg, 0.94f) { onRange(r) }.background(bg).padding(horizontal = 18.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) { Text("$r ${tx("days")}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = fg) }
            }
        }

        Spacer(Modifier.height(36.dp))
        Eyebrow(tx("Completion"))
        Spacer(Modifier.height(4.dp))
        SwapText(pct(a.rate), fontSize = 76.sp, fontWeight = FontWeight.Light)
        Text("${tx("last")} $range ${tx("days")}", fontSize = 13.sp, color = cs.onSurfaceVariant)

        HorizontalDivider(Modifier.padding(top = 30.dp, bottom = 22.dp), color = cs.outline.copy(alpha = 0.25f))
        Row(Modifier.fillMaxWidth()) {
            listOf(tx("Streak") to "$streak", tx("This week") to pct(week)).forEach { (label, value) ->
                Column(Modifier.weight(1f)) {
                    Text(label, fontSize = 12.sp, color = cs.onSurfaceVariant)
                    SwapText(value, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(34.dp))
        Eyebrow(tx("Daily completion"))
        Spacer(Modifier.height(14.dp))
        DayBars(a.dayRates, Modifier.fillMaxWidth().height(96.dp), minimal = true)

        Spacer(Modifier.height(34.dp))
        Eyebrow(tx("To improve"))
        Spacer(Modifier.height(10.dp))
        Text(a.tip, fontSize = 17.sp, lineHeight = 24.sp)

        Spacer(Modifier.height(26.dp))
        val chip = RShape(12.dp)
        Row(
            Modifier.springClick(chip, 0.94f, onClick = onPerTask).border(1.dp, cs.outline.copy(alpha = 0.5f), chip)
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(tx("Per task"), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(8.dp))
            Text("›", fontSize = 16.sp, color = cs.onSurfaceVariant)
        }
    }
}

// ═══════════════ TaskDetailScreen ═══════════════

@Composable
fun TaskDetailScreen(state: AppState, taskId: String, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val today = LocalDate.now()
    val task = state.tasks.find { it.id == taskId }
    val block = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier.fillMaxSize().clickable(interactionSource = block, indication = null, onClick = {}),
        color = cs.background, contentColor = cs.onBackground
    ) {
        if (task != null) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Row(Modifier.padding(start = 24.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            task.color?.let { Box(Modifier.padding(end = 10.dp).size(12.dp).clip(CircleShape).background(Color(it))) }
                            Text(task.title, fontSize = 26.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Text("${task.repeatLabel()} · since ${fmtDate(task.date)}", fontSize = 13.sp, color = cs.onSurfaceVariant)
                    }
                    SIconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Close") }
                }
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp).padding(top = 18.dp)
                        .navigationBarsPadding().padding(bottom = 28.dp)
                ) { TaskStatsContent(state, task, today) }
            }
        }
    }
}

@Composable
fun TaskStatsContent(state: AppState, task: Task, today: LocalDate) {
    val cs = MaterialTheme.colorScheme
    val st = remember(state, task) { state.taskStats(task, today) }
    Column {
        Eyebrow(tx("Completion"))
        Spacer(Modifier.height(4.dp))
        SwapText("${st.percent}%", fontSize = 76.sp, fontWeight = FontWeight.Light)
        Text("${st.done} ${tx("out of")} ${st.total} ${tx("days")}", fontSize = 13.sp, color = cs.onSurfaceVariant)

        HorizontalDivider(Modifier.padding(top = 28.dp, bottom = 22.dp), color = cs.outline.copy(alpha = 0.25f))
        Row(Modifier.fillMaxWidth()) {
            listOf(tx("Streak") to "${st.current}", tx("Finished") to "${st.done}").forEach { (label, value) ->
                Column(Modifier.weight(1f)) {
                    Text(label, fontSize = 12.sp, color = cs.onSurfaceVariant)
                    SwapText(value, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(34.dp))
        Eyebrow(tx("Last 20 weeks"))
        Spacer(Modifier.height(14.dp))
        Heatmap(state, task, today, Modifier.fillMaxWidth().aspectRatio(20f / 7f))

        Spacer(Modifier.height(34.dp))
        Eyebrow(tx("Notes"))
        Spacer(Modifier.height(10.dp))
        Text(taskNote(state, task, today, st), fontSize = 17.sp, lineHeight = 24.sp)
        if (task.description.isNotBlank()) {
            Spacer(Modifier.height(14.dp))
            Text(task.description, fontSize = 15.sp, lineHeight = 21.sp, color = cs.onSurfaceVariant)
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
    val faint = cs.outline.copy(alpha = 0.22f)
    val skipped = cs.onSurfaceVariant.copy(alpha = 0.35f)
    val missed = RED.copy(alpha = 0.45f)
    val ring = cs.onBackground
    Canvas(modifier) {
        val gap = 3.dp.toPx()
        val cell = (size.width - gap * (weeks - 1)) / weeks
        for (c in 0 until weeks) {
            for (r in 0 until 7) {
                val x = c * (cell + gap)
                val y = r * (cell + gap)
                val kind = cells[c * 7 + r]
                val color = when (kind) { 2 -> ring; 3 -> missed; 1 -> skipped; else -> faint }
                drawRoundRect(color, Offset(x, y), Size(cell, cell), CornerRadius(cell * 0.28f))
                if (kind == 4) drawRoundRect(ring, Offset(x, y), Size(cell, cell), CornerRadius(cell * 0.28f), style = Stroke(1.5.dp.toPx()))
            }
        }
    }
}

// ═══════════════ ProfileScreen ═══════════════

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

    Column(modifier.fillMaxWidth()) {
        SheetPanel(Modifier.weight(1f)) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp).padding(top = 12.dp, bottom = bottomSpace(100.dp))
            ) {
                SectionLabel(tx("Language & holidays"))
                SettingRow(tx("Language"), tx("Language of the app"), onClick = onLanguage) {
                    Value(if (state.language.isEmpty()) tx("Device") + " · " + Lang.nativeName(langCode) else Lang.nativeName(langCode))
                }
                SettingRow(tx("Holidays"), tx("Country of the public holidays"), onClick = onHolidays) {
                    Value(if (state.country == "AUTO") tx("Automatic") else Holidays.countryName(state.country))
                }
                SettingRow(tx("Show holidays"), tx("Mark them in the calendar")) {
                    SSwitch(checked = state.showHolidays, onCheckedChange = { vm.setShowHolidays(it) })
                }

                SectionLabel(tx("Tasks"))
                SettingRow(
                    tx("Repeating tasks"), "${state.tasks.count { it.repeating }} · ${tx("Review and fix schedules")}",
                    onClick = onRepeating
                ) { Value("") }

                SectionLabel(tx("Appearance"))
                SettingRow(tx("Animations"), tx("Springy, fluid motion")) {
                    SSwitch(checked = state.springy, onCheckedChange = { vm.setSpringy(it) })
                }
                SettingRow(tx("Theme"), tx("Follows your phone by default")) {
                    MenuButton(themeNames[state.theme]) { close ->
                        themeNames.forEachIndexed { i, n -> DropdownMenuItem(text = { Text(n) }, onClick = { vm.setTheme(i); close() }) }
                    }
                }
                SettingRow(tx("Blur"), tx("Blurs the screen behind sheets and screens")) {
                    MenuButton(blurNames[state.blurMode]) { close ->
                        blurNames.forEachIndexed { i, n -> DropdownMenuItem(text = { Text(n) }, onClick = { vm.setBlur(i); close() }) }
                    }
                }

                SectionLabel(tx("Notifications"))
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

                SectionLabel(tx("Calendar"))
                SettingRow(tx("Week starts on"), tx("First day in calendar and charts")) {
                    MenuButton(DayOfWeek.of(state.weekStartDay).getDisplayName(JTextStyle.FULL, Locale.getDefault())) { close ->
                        DayOfWeek.values().forEach { d ->
                            DropdownMenuItem(
                                text = { Text(d.getDisplayName(JTextStyle.FULL, Locale.getDefault())) },
                                onClick = { vm.setWeekStart(d.value); close() }
                            )
                        }
                    }
                }

                SectionLabel(tx("Data"))
                SettingRow(tx("Import"), tx("Add tasks from a backup file"), onClick = onImport) { Value("") }
                SettingRow(tx("Export"), tx("Save tasks, history and settings"), onClick = onExport) { Value("") }
                SettingRow(tx("Reset all data"), tx("Delete all tasks and history"), onClick = { confirmReset = true }) { Value("") }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            containerColor = cs.surface, shape = RShape(24.dp),
            onDismissRequest = { confirmReset = false },
            title = { Text(tx("Reset all data?")) },
            text = { Text(tx("All tasks and your finished history will be deleted. Export first if you want a copy.")) },
            confirmButton = { STextButton(onClick = { vm.resetData(); confirmReset = false }) { Text(tx("Reset"), color = RED) } },
            dismissButton = { STextButton(onClick = { confirmReset = false }) { Text(tx("Cancel")) } }
        )
    }
}

@Composable
fun SettingsScreen(
    state: AppState, tick: Int, vm: TrackerViewModel, onClose: () -> Unit,
    onImport: () -> Unit, onExport: () -> Unit, onRepeating: () -> Unit,
    onLanguage: () -> Unit, onHolidays: () -> Unit, modifier: Modifier = Modifier
) {
    OverlayScaffold(tx("Settings"), "", onClose, modifier) {
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
            Spacer(Modifier.width(6.dp))
        }
        Text("›", fontSize = 22.sp, color = cs.onSurfaceVariant)
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, top = 18.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingRow(title: String, subtitle: String, onClick: (() -> Unit)? = null, trailing: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val shape = CtrlShape
    Column {
        Row(
            Modifier.fillMaxWidth()
                .then(if (onClick != null) Modifier.springClick(shape, 0.98f, onClick = onClick) else Modifier.clip(shape))
                .padding(horizontal = 8.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text(title, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                SwapText(subtitle, fontSize = 12.sp, color = cs.onSurfaceVariant)
            }
            trailing()
        }
        HorizontalDivider(Modifier.padding(horizontal = 8.dp), color = cs.outline.copy(alpha = 0.3f))
    }
}

// ═══════════════ PickerScreens ═══════════════

@Composable
fun OverlayScaffold(
    title: String, subtitle: String, onClose: () -> Unit, modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val block = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier.fillMaxSize().clickable(interactionSource = block, indication = null, onClick = {}),
        color = cs.background, contentColor = cs.onBackground
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.padding(start = 24.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    if (subtitle.isNotEmpty()) Text(subtitle, fontSize = 13.sp, color = cs.onSurfaceVariant)
                }
                SIconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Close") }
            }
            content()
        }
    }
}

@Composable
fun PickRow(
    title: String, sub: String?, selected: Boolean, onClick: () -> Unit,
    trailing: @Composable () -> Unit = {}
) {
    val cs = MaterialTheme.colorScheme
    val bg by animateColorAsState(if (selected) cs.surfaceVariant else cs.surfaceVariant.copy(alpha = 0f), label = "pickBg")
    Row(
        Modifier.fillMaxWidth().springClick(CtrlShape, 0.98f, onClick = onClick).background(bg)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            if (sub != null) Text(sub, fontSize = 12.sp, color = cs.onSurfaceVariant)
        }
        trailing()
        if (selected) Icon(Icons.Default.Check, null, modifier = Modifier.padding(start = 8.dp).size(20.dp))
    }
}

@Composable
private fun ListLabel(text: String) {
    Text(
        text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 14.dp, top = 18.dp, bottom = 4.dp)
    )
}

// ───────────── language ─────────────

@Composable
fun LanguageList(state: AppState, vm: TrackerViewModel, modifier: Modifier = Modifier, onPicked: () -> Unit = {}) {
    val deviceCode = if (Lang.deviceLocale.language in Lang.codes) Lang.deviceLocale.language else "en"
    LazyColumn(modifier, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
        item(key = "device") {
            PickRow(
                tx("Device language"), Lang.nativeName(deviceCode), state.language.isEmpty(),
                onClick = { vm.setLanguage(""); onPicked() }
            )
        }
        item(key = "label") { ListLabel(tx("Built in · works offline")) }
        items(Lang.all, key = { it.first }) { (code, name) ->
            PickRow(name, null, state.language == code, onClick = { vm.setLanguage(code); onPicked() })
        }
        item(key = "end") { Spacer(Modifier.height(bottomSpace(24.dp))) }
    }
}

@Composable
fun LanguageScreen(state: AppState, vm: TrackerViewModel, onClose: () -> Unit, modifier: Modifier = Modifier) {
    OverlayScaffold(tx("Language"), tx("Pick the language of the app"), onClose, modifier) {
        LanguageList(state, vm, Modifier.weight(1f).fillMaxWidth(), onPicked = onClose)
    }
}

// ───────────── holidays ─────────────

@Composable
fun HolidaysList(state: AppState, vm: TrackerViewModel, modifier: Modifier = Modifier, onPicked: () -> Unit = {}) {
    val lang = LocalLang.current
    var query by remember { mutableStateOf("") }
    val all = remember(lang) { Holidays.allCountries() }
    val deviceCode = Lang.deviceLocale.country.uppercase()
    val q = query.trim().lowercase()
    val shown = remember(all, q) {
        all.filter { q.isEmpty() || it.second.lowercase().contains(q) || it.first.lowercase() == q }
    }

    LazyColumn(modifier.imePadding(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
        item(key = "search") {
            OutlinedTextField(
                value = query, onValueChange = { query = it }, singleLine = true, shape = CtrlShape,
                placeholder = { Text(tx("Search countries")) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
        }
        item(key = "auto") {
            val sub = tx("Follows your phone") + if (deviceCode.isEmpty()) "" else " · " + Holidays.countryName(deviceCode)
            PickRow(tx("Automatic"), sub, state.country == "AUTO", onClick = { vm.setCountry("AUTO"); onPicked() })
        }
        item(key = "labelAll") { ListLabel("${tx("All countries")} · ${shown.size}") }
        items(shown, key = { it.first }) { (code, name) ->
            PickRow(name, null, state.country == code, onClick = { vm.setCountry(code); onPicked() })
        }
        item(key = "end") { Spacer(Modifier.height(bottomSpace(24.dp))) }
    }
}

@Composable
fun HolidaysScreen(state: AppState, vm: TrackerViewModel, onClose: () -> Unit, modifier: Modifier = Modifier) {
    OverlayScaffold(tx("Holidays"), tx("Names are in the language of the country"), onClose, modifier) {
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
        val tasks = Store.get(this).tasks.filter { it.repeating }
        val lang = Lang.resolve(Store.get(this).language)
        val ctx = this
        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(LocalLang provides lang, LocalLayoutDirection provides LayoutDirection.Ltr) {
                TrackerTheme(dark = isSystemInDarkTheme(), oled = false, springy = true) {
                    val cs = MaterialTheme.colorScheme
                    Surface(Modifier.fillMaxSize(), color = cs.background, contentColor = cs.onBackground) {
                        var look by remember { mutableStateOf(WidgetPrefs.look(ctx, id)) }
                        Column(Modifier.fillMaxSize().systemBarsPadding()) {
                            WidgetPreviewPanel(kind, look)
                            Column(
                                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)
                            ) {
                                SectionLabel(tx("Appearance"))
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
                                Spacer(Modifier.height(16.dp))
                            }
                            SButton(
                                onClick = {
                                    WidgetPrefs.save(ctx, id, look)
                                    TasksWidget.refresh(ctx)
                                    setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
                                    finish()
                                },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp).height(54.dp)
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
    Column {
        Column(Modifier.padding(horizontal = 8.dp).padding(top = 12.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), fontWeight = FontWeight.Medium, fontSize = 16.sp)
                Text(valueText, color = cs.onSurfaceVariant, fontSize = 14.sp)
            }
            SSlider(value, onChange, from, to, step = step)
        }
        HorizontalDivider(Modifier.padding(horizontal = 8.dp), color = cs.outline.copy(alpha = 0.3f))
    }
}

@Composable
private fun WidgetPreviewPanel(kind: Int, look: WidgetLook) {
    val shape = RShape(24.dp)
    val w = if (kind == 2) 240 else 320
    val h = when (kind) { 2 -> 240; 3 -> 140; else -> 180 }
    BoxWithConstraints(
        Modifier.fillMaxWidth().padding(14.dp).clip(shape)
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
    val fs = look.fontScale
    val ring = (24 * look.iconScale).dp
    val rows = listOf(Triple(tx("Morning run"), 0xFFFF6B6B.toInt(), true), Triple(tx("Read 20 pages"), 0xFF4D9BFF.toInt(), false), Triple(tx("Plan tomorrow"), p.ring, false))
    Column(
        Modifier.size(w.dp, h.dp).clip(RShape(28.dp)).background(Color(p.bgColor))
            .padding(start = 20.dp, end = 20.dp, top = 16.dp)
    ) {
        if (clock) Text("12:40", color = Color(p.text), fontWeight = FontWeight.Bold, fontSize = (look.clockSize * 0.7f).sp)
        else Text(tx("Today"), color = Color(p.text), fontWeight = FontWeight.Bold, fontSize = (32 * fs).sp)
        Text(
            LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())),
            color = Color(p.secondary), fontSize = (14 * fs).sp
        )
        Spacer(Modifier.height(6.dp))
        rows.forEach { (title, c, done) ->
            Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(ring).clip(CircleShape).then(if (done) Modifier.background(Color(c)) else Modifier.border(2.dp, Color(c), CircleShape)),
                    contentAlignment = Alignment.Center
                ) { if (done) Text("✓", color = Color.White, fontSize = (13 * look.iconScale).sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(14.dp))
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
