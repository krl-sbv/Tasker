package com.kirillshybaiev.dailytracker

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedContent
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.TextStyle as JTextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

// ═══════════════ CalendarViews ═══════════════

const val VIEW_SCHEDULE = 0
const val VIEW_DAY = 1
const val VIEW_3DAYS = 3
const val VIEW_WEEK = 7
const val VIEW_MONTH = 30

private val HOUR = 56.dp
private val GUTTER = 52.dp

data class Draft(val date: LocalDate, val from: Int, val to: Int)

fun minuteTime(m: Int): LocalTime = if (m >= 1440) LocalTime.MIDNIGHT else LocalTime.of(m / 60, m % 60)

fun newDraft(kind: Kind, date: LocalDate, start: LocalTime? = null, end: LocalTime? = null): Task {
    val id = UUID.randomUUID().toString()
    return when (kind) {
        Kind.TASK -> Task(
            id, "", repeating = false, date = date, time = start,
            alerts = if (start != null) listOf(0) else emptyList()
        )
        Kind.EVENT -> {
            val s = start ?: LocalTime.now().withMinute(0).withSecond(0).withNano(0).plusHours(1)
            val e = end ?: s.plusHours(1)
            Task(
                id, "", repeating = false, date = date, time = s, endTime = e,
                spanDays = if (!e.isAfter(s)) 1 else 0, kind = Kind.EVENT, alerts = listOf(30)
            )
        }
        Kind.BIRTHDAY -> Task(
            id, "", repeating = true, date = date, unit = 2, interval = 1, kind = Kind.BIRTHDAY, alerts = listOf(0)
        )
    }
}

fun draftTask(d: Draft, title: String, kind: Kind): Task =
    newDraft(kind, d.date, minuteTime(d.from), minuteTime(d.to)).copy(title = title.trim())

fun kindIcon(k: Kind): String = when (k) {
    Kind.EVENT -> "event"
    Kind.TASK -> "task"
    Kind.BIRTHDAY -> "cake"
}

fun viewIcon(v: Int): String = when (v) {
    VIEW_SCHEDULE -> "schedule"
    VIEW_DAY -> "day"
    VIEW_3DAYS -> "3days"
    VIEW_WEEK -> "week"
    else -> "month"
}

@Composable
fun kindLabel(k: Kind): String = when (k) {
    Kind.EVENT -> tx("Event")
    Kind.TASK -> tx("Task")
    Kind.BIRTHDAY -> tx("Birthday")
}

@Composable
fun viewLabel(v: Int): String = when (v) {
    VIEW_SCHEDULE -> tx("Schedule")
    VIEW_DAY -> tx("Day")
    VIEW_3DAYS -> tx("3 days")
    VIEW_WEEK -> tx("Week")
    else -> tx("Month")
}

@Composable
fun itemAccent(t: Task): Color {
    val cs = MaterialTheme.colorScheme
    return t.color?.let { Color(it) } ?: if (t.kind == Kind.BIRTHDAY) Color(0xFFFF7EB6) else cs.onSurfaceVariant
}

@Composable
fun eventSubtitle(task: Task, start: LocalDate, day: LocalDate): String {
    val parts = mutableListOf<String>()
    if (task.kind == Kind.BIRTHDAY) {
        parts += tx("Birthday")
        val age = start.year - task.date.year
        if (age > 0) parts += "$age"
    } else {
        val t = task.time
        if (t == null) parts += tx("All day")
        else {
            val end = start.plusDays(task.spanDays.toLong())
            parts += when {
                task.spanDays == 0 -> task.timeRange() ?: "$t"
                day == start -> "${tx("From")} $t"
                day == end -> "${tx("Until")} ${task.endTime ?: ""}"
                else -> tx("All day")
            }
        }
        if (task.location.isNotBlank()) parts += task.location
    }
    return parts.filter { it.isNotBlank() }.joinToString(" · ")
}

// ═══════════════ Icons ═══════════════

@Composable
fun LineIcon(name: String, size: Dp, color: Color) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val sw = w * 0.085f
        val st = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val r = CornerRadius(w * 0.12f)
        fun frame(top: Float = 0.16f) =
            drawRoundRect(color, Offset(w * 0.12f, w * top), Size(w * 0.76f, w * (0.86f - top)), r, style = st)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(color, Offset(w * x1, w * y1), Offset(w * x2, w * y2), sw, StrokeCap.Round)
        when (name) {
            "event" -> {
                frame(0.2f)
                line(0.12f, 0.42f, 0.88f, 0.42f)
                line(0.34f, 0.08f, 0.34f, 0.28f)
                line(0.66f, 0.08f, 0.66f, 0.28f)
            }
            "task" -> {
                drawCircle(color, w * 0.38f, center, style = st)
                val p = Path().apply {
                    moveTo(w * 0.32f, w * 0.5f); lineTo(w * 0.45f, w * 0.63f); lineTo(w * 0.69f, w * 0.38f)
                }
                drawPath(p, color, style = st)
            }
            "cake" -> {
                drawRoundRect(color, Offset(w * 0.14f, w * 0.48f), Size(w * 0.72f, w * 0.38f), CornerRadius(w * 0.08f), style = st)
                line(0.14f, 0.66f, 0.86f, 0.66f)
                line(0.5f, 0.48f, 0.5f, 0.3f)
                drawCircle(color, w * 0.07f, Offset(w * 0.5f, w * 0.17f))
            }
            "target" -> {
                drawCircle(color, w * 0.4f, center, style = st)
                drawCircle(color, w * 0.22f, center, style = st)
                drawCircle(color, w * 0.07f, center)
            }
            "text" -> {
                line(0.14f, 0.3f, 0.86f, 0.3f)
                line(0.14f, 0.5f, 0.86f, 0.5f)
                line(0.14f, 0.7f, 0.6f, 0.7f)
            }
            "home" -> {
                val p = Path().apply {
                    moveTo(w * 0.14f, w * 0.46f); lineTo(w * 0.5f, w * 0.16f); lineTo(w * 0.86f, w * 0.46f)
                }
                drawPath(p, color, style = st)
                drawRoundRect(color, Offset(w * 0.24f, w * 0.4f), Size(w * 0.52f, w * 0.46f), CornerRadius(w * 0.08f), style = st)
                line(0.5f, 0.66f, 0.5f, 0.86f)
            }
            "gear" -> {
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0)
                    val c = kotlin.math.cos(a).toFloat()
                    val s = kotlin.math.sin(a).toFloat()
                    drawLine(color, Offset(center.x + c * w * 0.3f, center.y + s * w * 0.3f), Offset(center.x + c * w * 0.42f, center.y + s * w * 0.42f), sw * 1.25f, StrokeCap.Round)
                }
                drawCircle(color, w * 0.28f, center, style = st)
                drawCircle(color, w * 0.1f, center, style = st)
            }
            "schedule" -> {
                drawRoundRect(color, Offset(w * 0.12f, w * 0.16f), Size(w * 0.76f, w * 0.3f), r, style = st)
                drawRoundRect(color, Offset(w * 0.12f, w * 0.56f), Size(w * 0.76f, w * 0.3f), r, style = st)
            }
            "day" -> {
                frame()
                line(0.12f, 0.66f, 0.88f, 0.66f)
            }
            "3days" -> {
                frame()
                line(0.373f, 0.16f, 0.373f, 0.86f)
                line(0.627f, 0.16f, 0.627f, 0.86f)
            }
            "week" -> {
                frame()
                line(0.31f, 0.16f, 0.31f, 0.86f)
                line(0.5f, 0.16f, 0.5f, 0.86f)
                line(0.69f, 0.16f, 0.69f, 0.86f)
            }
            else -> {
                frame()
                line(0.373f, 0.16f, 0.373f, 0.86f)
                line(0.627f, 0.16f, 0.627f, 0.86f)
                line(0.12f, 0.51f, 0.88f, 0.51f)
            }
        }
    }
}

@Composable
fun TodayIcon(day: Int, color: Color) {
    val press = LocalIconPress.current
    Box(
        Modifier.size(22.dp).graphicsLayer {
            val k = press.value
            rotationZ = -10f * k
            translationY = -1.5.dp.toPx() * k
        }.border(1.6.dp, color, RoundedCornerShape(5.dp)),
        contentAlignment = Alignment.Center
    ) { Text("$day", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1) }
}

// ═══════════════ Rows ═══════════════

@Composable
fun EventRow(task: Task, start: LocalDate, day: LocalDate, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val accent = itemAccent(task)
    val sub = eventSubtitle(task, start, day)
    Row(
        modifier.fillMaxWidth().padding(vertical = 4.dp)
            .springClick(CardShape, 0.98f, onClick = onOpen)
            .background(cardColor())
            .heightIn(min = 64.dp)
            .padding(start = 4.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
            if (task.kind == Kind.BIRTHDAY) LineIcon("cake", 22.dp, accent)
            else Box(Modifier.size(4.dp, 28.dp).clip(RShape(2.dp)).background(accent))
        }
        Column(Modifier.weight(1f).padding(vertical = 12.dp)) {
            Text(task.title, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (sub.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(sub, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = cs.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun DeadlineRow(task: Task, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().padding(vertical = 4.dp)
            .springClick(CardShape, 0.98f, onClick = onOpen)
            .background(cardColor())
            .heightIn(min = 64.dp)
            .padding(start = 4.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) { LineIcon("target", 22.dp, itemAccent(task)) }
        Column(Modifier.weight(1f).padding(vertical = 12.dp)) {
            Text(task.title, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(
                "${tx("Deadline")} · ${fmtDate(task.date)} →", fontSize = 13.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis, color = cs.onSurfaceVariant
            )
        }
    }
}

@Composable
fun KindChips(kind: Kind, onKind: (Kind) -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val springy = LocalSpringy.current
    val haptics = rememberHaptics()
    val kinds = listOf(Kind.EVENT, Kind.TASK, Kind.BIRTHDAY)
    val spots = remember { mutableStateMapOf<Int, Pair<Float, Float>>() }
    val left = remember { Animatable(0f) }
    val right = remember { Animatable(0f) }
    var placed by remember { mutableStateOf(false) }
    val sel = kinds.indexOf(kind)
    val target = spots[sel]
    LaunchedEffect(sel, target) {
        val t = target ?: return@LaunchedEffect
        if (!placed || !springy) { left.snapTo(t.first); right.snapTo(t.second); placed = true; return@LaunchedEffect }
        val goingRight = t.first > left.value
        launch { left.animateTo(t.first, fluid(if (goingRight) 0.42f else 0.3f, 0.12f)) }
        launch { right.animateTo(t.second, fluid(if (goingRight) 0.3f else 0.42f, 0.12f)) }
    }
    val idle = cs.onSurface.copy(alpha = 0.06f)
    val pill = cs.primary
    Row(
        modifier.drawBehind {
            val r = CornerRadius(size.height / 2f)
            spots.values.forEach { (a, b) -> drawRoundRect(idle, Offset(a, 0f), Size(b - a, size.height), r) }
            if (placed) drawRoundRect(pill, Offset(left.value, 0f), Size(right.value - left.value, size.height), r)
        },
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        kinds.forEachIndexed { i, k ->
            val on = i == sel
            val fg by animateColorAsState(if (on) cs.onPrimary else cs.onSurface, if (springy) fluid(0.28f) else tween(120), label = "kindFg")
            Row(
                Modifier.onPlaced { c -> val x = c.positionInParent().x; spots[i] = x to (x + c.size.width) }
                    .springClick(RShape(12.dp), 0.94f) { if (!on) { haptics.tick(); onKind(k) } }
                    .padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.iconPop(on, 0.7f, if (k == Kind.BIRTHDAY) -14f else 10f)) { LineIcon(kindIcon(k), 16.dp, fg) }
                Spacer(Modifier.width(6.dp))
                Text(kindLabel(k), color = fg, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }
        }
    }
}

// ═══════════════ AddMenu ═══════════════

@Composable
fun BoxScope.AddMenuFab(onPick: (Kind) -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val springy = LocalSpringy.current
    val haptics = rememberHaptics()
    var open by remember { mutableStateOf(false) }
    BackHandler(enabled = open) { open = false }
    val p by animateFloatAsState(
        if (open) 1f else 0f,
        if (springy) fluid(0.42f, 0.16f) else tween(160), label = "addMenu"
    )
    if (p > 0.01f) {
        val dim = remember { MutableInteractionSource() }
        Box(
            Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.28f * p.coerceIn(0f, 1f)))
                .clickable(interactionSource = dim, indication = null) { open = false }
        )
    }
    Column(
        modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(20.dp),
        horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (p > 0.01f) {
            listOf(Kind.BIRTHDAY, Kind.TASK, Kind.EVENT).forEachIndexed { i, k ->
                val shape = RShape(20.dp)
                val delay = (2 - i) * 0.14f
                Row(
                    Modifier
                        .graphicsLayer {
                            val q = ((p - delay) / (1f - delay)).coerceAtLeast(0f)
                            alpha = q.coerceIn(0f, 1f)
                            translationY = (1f - q) * (3 - i) * 18.dp.toPx()
                            val sc = 0.84f + 0.16f * q
                            scaleX = sc
                            scaleY = sc
                            transformOrigin = TransformOrigin(1f, 0.5f)
                        }
                        .softShadow(shape, 10.dp)
                        .springClick(shape, 0.95f) { open = false; onPick(k) }
                        .background(elevatedColor())
                        .padding(start = 16.dp, end = 22.dp, top = 13.dp, bottom = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(30.dp).graphicsLayer {
                            val q = ((p - delay) / (1f - delay)).coerceIn(0f, 1.2f)
                            rotationZ = -40f * (1f - q)
                            scaleX = 0.6f + 0.4f * q
                            scaleY = 0.6f + 0.4f * q
                        }.clip(CircleShape).background(cs.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        LineIcon(kindIcon(k), 17.dp, cs.primary)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(kindLabel(k), fontSize = 16.sp, fontWeight = FontWeight.Medium, color = cs.onSurface)
                }
            }
        }
        val src = remember { MutableInteractionSource() }
        val fabShape = RShape(28.dp)
        Box(
            Modifier.size(58.dp).pressScale(src, 0.88f)
                .shadow(14.dp, fabShape, clip = false, ambientColor = cs.primary, spotColor = cs.primary)
                .clip(fabShape).background(cs.primary)
                .clickable(interactionSource = src, indication = LocalIndication.current) { haptics.tick(); open = !open },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Add, contentDescription = "Add", tint = cs.onPrimary,
                modifier = Modifier.size(28.dp).graphicsLayer { rotationZ = 135f * p }
            )
        }
    }
}

// ═══════════════ CalendarScreen ═══════════════

@Composable
fun CalendarScreen(
    state: AppState, onClose: () -> Unit, onMenu: () -> Unit, onOpen: (Task) -> Unit,
    onToggle: (Task, LocalDate) -> Unit, onAdd: (Task) -> Unit, onQuickSave: (Task) -> Unit,
    modifier: Modifier = Modifier, viewOverride: Int? = null
) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    val today = rememberToday()
    val view = viewOverride ?: state.calView
    var focus by remember { mutableStateOf(today) }
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var jumpKey by remember { mutableIntStateOf(0) }
    var jumpDate by remember { mutableStateOf(today) }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf<Draft?>(null) }
    var draftTitle by remember { mutableStateOf("") }
    var draftKind by remember { mutableStateOf(Kind.EVENT) }
    LaunchedEffect(view) { draft = null; searching = false; query = "" }
    fun jump(d: LocalDate) {
        focus = d
        month = YearMonth.from(d)
        jumpDate = d
        jumpKey++
    }
    BackHandler(enabled = LocalScreenActive.current && (draft != null || searching)) { draft = null; searching = false; query = "" }
    val block = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier.fillMaxSize().clickable(interactionSource = block, indication = null, onClick = {}),
        color = cs.background, contentColor = cs.onBackground
    ) {
        Box(Modifier.fillMaxSize()) {
            val springy = LocalSpringy.current
            val currentView = view
            val liveView by rememberUpdatedState(view)
            val screenActive = LocalScreenActive.current
            val seen = remember { mutableStateListOf(view) }
            LaunchedEffect(view) { if (view !in seen) seen += view }
            LaunchedEffect(screenActive) {
                if (!screenActive) return@LaunchedEffect
                delay(700)
                listOf(VIEW_SCHEDULE, VIEW_DAY, VIEW_3DAYS, VIEW_WEEK, VIEW_MONTH).forEach { v ->
                    if (v !in seen) { seen += v; delay(180) }
                }
            }
            seen.toList().forEach { view ->
            key(view) {
            ViewLayer(view == currentView, springy) {
            if (view == VIEW_MONTH) {
                CalendarPage(state, onClose, onOpen, onToggle, onMenu = onMenu, onSelect = { if (view == liveView) focus = it })
            } else {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SIconButton(onClick = onMenu) { MenuIcon() }
                        if (searching) {
                            val fr = remember { FocusRequester() }
                            LaunchedEffect(Unit) { fr.requestFocus() }
                            BasicTextField(
                                value = query, onValueChange = { query = it }, singleLine = true,
                                textStyle = TextStyle(color = cs.onBackground, fontSize = 18.sp),
                                cursorBrush = SolidColor(cs.onBackground),
                                modifier = Modifier.weight(1f).padding(horizontal = 10.dp).focusRequester(fr),
                                decorationBox = { inner ->
                                    Box {
                                        if (query.isEmpty()) Text(tx("Search"), fontSize = 18.sp, color = cs.onSurfaceVariant)
                                        inner()
                                    }
                                }
                            )
                        } else {
                            Row(
                                Modifier.weight(1f).springClick(RShape(12.dp), 0.97f) {
                                    val d0 = if (view == VIEW_SCHEDULE) month.atDay(1) else focus
                                    android.app.DatePickerDialog(
                                        ctx, { _, y, m, d -> jump(LocalDate.of(y, m + 1, d)) },
                                        d0.year, d0.monthValue - 1, d0.dayOfMonth
                                    ).show()
                                }.padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val m = if (view == VIEW_SCHEDULE) month else YearMonth.from(focus)
                                val name = m.month.getDisplayName(JTextStyle.FULL_STANDALONE, Locale.getDefault())
                                    .replaceFirstChar { it.titlecase(Locale.getDefault()) }
                                SwapText(
                                    name + if (m.year != today.year) " ${m.year}" else "",
                                    fontSize = 22.sp, fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("▾", color = cs.onSurfaceVariant)
                            }
                        }
                        if (view == VIEW_SCHEDULE) {
                            SIconButton(onClick = { searching = !searching; if (!searching) query = "" }) {
                                Crossfade(searching, animationSpec = fluid(0.25f), label = "searchIcon") { s ->
                                    Box(Modifier.iconPop(s, 0.7f, 45f)) { if (s) CloseIcon(cs.onBackground) else SearchIcon(cs.onBackground) }
                                }
                            }
                        }
                        SIconButton(onClick = { jump(today) }) { TodayIcon(today.dayOfMonth, cs.onBackground) }
                        if (!searching) SIconButton(onClick = onClose) { CloseIcon(cs.onBackground) }
                    }
                    if (view == VIEW_SCHEDULE) {
                        ScheduleView(
                            state, today, query, jumpKey, jumpDate, onMonth = { if (view == liveView) month = it },
                            onOpen = onOpen, onToggle = onToggle, modifier = Modifier.weight(1f)
                        )
                    } else {
                        key(view) {
                            TimelineView(
                                state, today, view, focus, onFocus = { if (view == liveView) focus = it }, jumpKey = jumpKey, jumpDate = jumpDate,
                                draft = draft,
                                onDraft = { nd ->
                                    if (draft == null) { draftTitle = ""; draftKind = Kind.EVENT }
                                    draft = nd
                                },
                                onOpen = onOpen, modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            }
            }
            }
            val addDate = when {
                view == VIEW_SCHEDULE -> today
                view == VIEW_MONTH -> focus
                !today.isBefore(focus) && today.isBefore(focus.plusDays(view.toLong())) -> today
                else -> focus
            }
            if (draft == null) AddMenuFab(onPick = { k -> onAdd(newDraft(k, addDate)) })
            draft?.let { d ->
                QuickCreate(
                    d, draftTitle, { draftTitle = it }, draftKind, { draftKind = it },
                    onClose = { draft = null },
                    onSave = { onQuickSave(draftTask(d, draftTitle, draftKind)); draft = null },
                    onMore = { onAdd(draftTask(d, draftTitle, draftKind)); draft = null },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
private fun ViewLayer(shown: Boolean, springy: Boolean, content: @Composable () -> Unit) {
    val p = remember { Animatable(if (shown) 1f else 0f) }
    val isShown by rememberUpdatedState(shown)
    var entering by remember { mutableStateOf(shown) }
    LaunchedEffect(shown) {
        entering = shown
        p.animateTo(if (shown) 1f else 0f, if (springy) fluid(if (shown) 0.36f else 0.26f) else tween(150))
    }
    Box(
        Modifier.fillMaxSize()
            .offset { if (!isShown && p.value <= 0.001f) IntOffset(0, 400_000) else IntOffset.Zero }
            .graphicsLayer {
                val v = p.value.coerceIn(0f, 1f)
                alpha = v
                val sc = if (entering) 0.96f + 0.04f * v else 1f + 0.02f * (1f - v)
                scaleX = sc
                scaleY = sc
            }
    ) { content() }
}

// ═══════════════ ScheduleView ═══════════════

private sealed class AgendaRow(val key: String, val ym: YearMonth)
private class MonthRow(ym: YearMonth) : AgendaRow("m$ym", ym)
private class WeekRow(val from: LocalDate, val n: Int) : AgendaRow("w$from", YearMonth.from(from))
private class DayRow(
    val date: LocalDate, val events: List<Pair<Task, LocalDate>>, val tasks: List<Task>,
    val deadlines: List<Task>, val holiday: String?
) : AgendaRow("d$date", YearMonth.from(date)) {
    val empty: Boolean get() = events.isEmpty() && tasks.isEmpty() && deadlines.isEmpty() && holiday == null
}

private fun buildAgenda(state: AppState, from: LocalDate, to: LocalDate, today: LocalDate, query: String): List<AgendaRow> {
    val q = query.trim().lowercase()
    fun match(t: Task) = q.isEmpty() || t.title.lowercase().contains(q) ||
        t.description.lowercase().contains(q) || t.location.lowercase().contains(q)
    val wf = WeekFields.of(DayOfWeek.of(state.weekStartDay), if (state.weekStartDay == 1) 4 else 1)
    val code = state.holidayCountry()
    val out = mutableListOf<AgendaRow>()
    var lastMonth: YearMonth? = null
    var d = from
    while (!d.isAfter(to)) {
        val ym = YearMonth.from(d)
        if (q.isEmpty()) {
            if (ym != lastMonth) { out += MonthRow(ym); lastMonth = ym }
            if (d.dayOfWeek.value == state.weekStartDay) out += WeekRow(d, d.get(wf.weekOfWeekBasedYear()))
        }
        val row = DayRow(
            d,
            state.eventsOn(d).filter { match(it.first) },
            state.tasksOn(d).filter { match(it) },
            state.deadlinesOn(d).filter { match(it) },
            if (q.isEmpty() && state.showHolidays) Holidays.forYear(code, d.year)[d] else null
        )
        if (!row.empty || (d == today && q.isEmpty())) {
            if (q.isNotEmpty() && ym != lastMonth) { out += MonthRow(ym); lastMonth = ym }
            out += row
        }
        d = d.plusDays(1)
    }
    return out
}

@Composable
fun ScheduleView(
    state: AppState, today: LocalDate, query: String, jumpKey: Int, jumpDate: LocalDate,
    onMonth: (YearMonth) -> Unit, onOpen: (Task) -> Unit, onToggle: (Task, LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    var back by remember(today, jumpDate) { mutableIntStateOf(60) }
    var ahead by remember(today, jumpDate) { mutableIntStateOf(365) }
    val from = remember(today, jumpDate, back) { minOf(today, jumpDate).minusDays(back.toLong()) }
    val to = remember(today, jumpDate, ahead) { maxOf(today, jumpDate).plusDays(ahead.toLong()) }
    val rows = remember(state, from, to, query, today) { buildAgenda(state, from, to, today, query) }
    val list = rememberLazyListState()
    LaunchedEffect(jumpKey) {
        val i = rows.indexOfFirst { it is DayRow && !it.date.isBefore(jumpDate) }
        if (i >= 0) list.scrollToItem(max(0, i - 1))
    }
    LaunchedEffect(query) { if (query.isNotBlank()) list.scrollToItem(0) }
    val rowsNow by rememberUpdatedState(rows)
    val monthCb by rememberUpdatedState(onMonth)
    LaunchedEffect(list) {
        snapshotFlow { list.firstVisibleItemIndex }.collect { i -> rowsNow.getOrNull(i)?.let { monthCb(it.ym) } }
    }
    val searching by rememberUpdatedState(query.isNotBlank())
    LaunchedEffect(list) {
        snapshotFlow { list.firstVisibleItemIndex to (list.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) }
            .collect { (first, last) ->
                val total = list.layoutInfo.totalItemsCount
                if (searching || total == 0) return@collect
                if (last >= total - 12 && ahead < 36500) ahead += 365
                if (first <= 2 && back < 36500 && list.isScrollInProgress) back += 180
            }
    }
    LazyColumn(modifier.fillMaxWidth().rubberBand(), state = list, contentPadding = PaddingValues(bottom = bottomSpace(110.dp))) {
        if (rows.isEmpty()) {
            item(key = "none") {
                Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                    Text(tx("No results"), color = cs.onSurfaceVariant)
                }
            }
        }
        items(rows, key = { it.key }) { r ->
            when (r) {
                is MonthRow -> {
                    val name = r.ym.month.getDisplayName(JTextStyle.FULL_STANDALONE, Locale.getDefault())
                        .replaceFirstChar { it.titlecase(Locale.getDefault()) }
                    Text(
                        "$name ${r.ym.year}", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 22.dp, top = 26.dp, bottom = 6.dp)
                    )
                }
                is WeekRow -> Text(
                    weekLabel(r.from, r.n), fontSize = 13.sp, color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(start = 76.dp, top = 12.dp, bottom = 4.dp)
                )
                is DayRow -> AgendaDay(r, state, today, onOpen, onToggle)
            }
        }
    }
}

@Composable
private fun weekLabel(from: LocalDate, n: Int): String {
    val to = from.plusDays(6)
    fun ms(d: LocalDate) = d.month.getDisplayName(JTextStyle.SHORT, Locale.getDefault())
    val range = if (from.month == to.month) "${from.dayOfMonth}–${to.dayOfMonth} ${ms(to)}"
        else "${from.dayOfMonth} ${ms(from)} – ${to.dayOfMonth} ${ms(to)}"
    return "${tx("Week")} $n, $range"
}

@Composable
private fun AgendaDay(r: DayRow, state: AppState, today: LocalDate, onOpen: (Task) -> Unit, onToggle: (Task, LocalDate) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val isToday = r.date == today
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Column(Modifier.width(56.dp).padding(top = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                r.date.dayOfWeek.getDisplayName(JTextStyle.SHORT, Locale.getDefault()), fontSize = 12.sp,
                color = if (isToday) cs.primary else cs.onSurfaceVariant
            )
            Box(
                Modifier.padding(top = 2.dp).size(38.dp).clip(CircleShape)
                    .background(if (isToday) cs.primary else cs.primary.copy(alpha = 0f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${r.date.dayOfMonth}", fontSize = 18.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                    color = if (isToday) cs.onPrimary else cs.onBackground
                )
            }
        }
        Column(Modifier.weight(1f).padding(start = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            r.holiday?.let { h ->
                Row(Modifier.padding(start = 8.dp, top = 8.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(cs.onSurfaceVariant.copy(alpha = 0.6f)))
                    Spacer(Modifier.width(10.dp))
                    Text(h, fontSize = 13.sp, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            r.events.forEach { (t, s) ->
                AgendaChip(t, eventSubtitle(t, s, r.date), onClick = { onOpen(t) }, leading = if (t.kind == Kind.BIRTHDAY) "cake" else null)
            }
            r.tasks.forEach { t ->
                AgendaChip(
                    t, t.timeRange() ?: t.description.takeIf { it.isNotBlank() }, onClick = { onOpen(t) },
                    done = state.isDone(t, r.date), canCheck = !r.date.isAfter(today), onCheck = { onToggle(t, r.date) }
                )
            }
            r.deadlines.forEach { t -> AgendaChip(t, tx("Deadline"), onClick = { onOpen(t) }, leading = "target") }
            if (r.empty) {
                Text(
                    tx("Nothing planned"), fontSize = 14.sp, color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, top = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun AgendaChip(
    task: Task, sub: String?, onClick: () -> Unit, done: Boolean = false,
    canCheck: Boolean = false, onCheck: (() -> Unit)? = null, leading: String? = null
) {
    val cs = MaterialTheme.colorScheme
    val accent = itemAccent(task)
    val bg = if (task.color != null) accent.copy(alpha = 0.22f) else cs.surfaceVariant
    val shape = RoundedCornerShape(16.dp)
    val lead = onCheck != null || leading != null
    Row(
        Modifier.fillMaxWidth().alpha(if (done) 0.55f else 1f)
            .springClick(shape, 0.98f, onClick = onClick).background(bg)
            .padding(start = if (lead) 2.dp else 14.dp, end = 14.dp, top = if (lead) 3.dp else 10.dp, bottom = if (lead) 3.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onCheck != null) {
            Box(
                Modifier.size(38.dp).springClick(CircleShape, 0.85f, enabled = canCheck, onClick = onCheck),
                contentAlignment = Alignment.Center
            ) {
                val ring = task.color?.let { Color(it) } ?: cs.onSurfaceVariant
                Box(
                    Modifier.size(18.dp).clip(CircleShape)
                        .background(if (done) (task.color?.let { Color(it) } ?: cs.onSurface) else cs.onSurface.copy(alpha = 0f))
                        .border(1.8.dp, if (done) (task.color?.let { Color(it) } ?: cs.onSurface) else ring, CircleShape),
                    contentAlignment = Alignment.Center
                ) { if (done) Icon(Icons.Default.Check, null, Modifier.size(12.dp), tint = cs.surface) }
            }
        } else if (leading != null) {
            Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) { LineIcon(leading, 18.dp, accent) }
        }
        Column(Modifier.weight(1f).padding(vertical = if (lead) 6.dp else 0.dp)) {
            StrikeText(task.title, done, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            if (!sub.isNullOrBlank()) {
                Text(sub, fontSize = 13.sp, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ═══════════════ TimelineView ═══════════════

private class Block(val task: Task, val start: LocalDate, val from: Int, val to: Int) {
    var col = 0
    var cols = 1
}

private class AllDayItem(val task: Task?, val label: String, val icon: String? = null)

private class ScrollMemo(start: Int) {
    var px by mutableIntStateOf(start)
    val headers = mutableStateMapOf<Int, Int>()
    val band = mutableFloatStateOf(0f)
}

private fun layoutBlocks(state: AppState, d: LocalDate): List<Block> {
    val list = mutableListOf<Block>()
    state.tasksOn(d).forEach { t ->
        val tm = t.time ?: return@forEach
        val f = tm.hour * 60 + tm.minute
        list += Block(t, d, f, min(1440, f + 30))
    }
    state.eventsOn(d).forEach { (t, s) ->
        val tm = t.time ?: return@forEach
        val startMin = (tm.hour * 60 + tm.minute).toLong()
        val shift = ChronoUnit.DAYS.between(s, d) * 1440L
        val f = max(0L, startMin - shift).toInt()
        val e = min(1440L, startMin + t.durationMinutes() - shift).toInt()
        if (e > f) list += Block(t, s, f, max(e, f + 15))
    }
    list.sortWith(compareBy({ it.from }, { -it.to }))
    var i = 0
    while (i < list.size) {
        var j = i
        var end = list[i].to
        val ends = mutableListOf<Int>()
        while (j < list.size && (j == i || list[j].from < end)) {
            val b = list[j]
            val c = ends.indexOfFirst { it <= b.from }
            if (c >= 0) { b.col = c; ends[c] = b.to } else { b.col = ends.size; ends += b.to }
            end = max(end, b.to)
            j++
        }
        for (k in i until j) list[k].cols = ends.size
        i = j
    }
    return list
}

private fun allDayItems(state: AppState, d: LocalDate, holiday: String?, deadline: String): List<AllDayItem> {
    val out = mutableListOf<AllDayItem>()
    if (holiday != null) out += AllDayItem(null, holiday)
    state.eventsOn(d).filter { it.first.time == null }.forEach { (t, _) ->
        out += AllDayItem(t, t.title, if (t.kind == Kind.BIRTHDAY) "cake" else null)
    }
    state.tasksOn(d).filter { it.time == null }.forEach { out += AllDayItem(it, it.title, if (state.isDone(it, d)) "done" else null) }
    state.deadlinesOn(d).forEach { out += AllDayItem(it, "$deadline: ${it.title}", "target") }
    return out
}

@Composable
fun TimelineView(
    state: AppState, today: LocalDate, days: Int, focus: LocalDate, onFocus: (LocalDate) -> Unit,
    jumpKey: Int, jumpDate: LocalDate, draft: Draft?, onDraft: (Draft?) -> Unit,
    onOpen: (Task) -> Unit, modifier: Modifier = Modifier
) {
    val mid = 5000
    val base = remember(state.weekStartDay) { if (days == 7) weekStart(focus, state.weekStartDay) else focus }
    val pager = rememberPagerState(initialPage = mid) { mid * 2 }
    fun startOf(p: Int): LocalDate = base.plusDays((p - mid).toLong() * days)
    fun pageOf(d: LocalDate): Int = mid + Math.floorDiv(ChronoUnit.DAYS.between(base, d), days.toLong()).toInt()
    val focusCb by rememberUpdatedState(onFocus)
    LaunchedEffect(pager.currentPage) { focusCb(startOf(pager.currentPage)) }
    LaunchedEffect(jumpKey) {
        if (jumpKey > 0) {
            val p = pageOf(jumpDate)
            if (abs(p - pager.currentPage) > 3) pager.scrollToPage(p) else pager.animateScrollToPage(p)
        }
    }
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); now = LocalTime.now() } }
    val density = LocalDensity.current
    val shared = remember { ScrollMemo(with(density) { (HOUR * max(0, LocalTime.now().hour - 1).toFloat()).roundToPx() }) }
    Row(modifier.fillMaxSize()) {
        TimeGutter(shared, pager, Modifier.width(GUTTER).fillMaxHeight())
        HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxHeight(), key = { it }) { page ->
            val start = startOf(page)
            TimelinePage(
                state, today, (0 until days).map { start.plusDays(it.toLong()) }, now,
                page == pager.currentPage, page, shared, draft, onDraft, onOpen
            )
        }
    }
}

@Composable
private fun TimeGutter(shared: ScrollMemo, pager: PagerState, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val style = TextStyle(fontSize = 11.sp, color = cs.onSurfaceVariant, fontFeatureSettings = "tnum")
    val labels = remember(style) { (1..23).map { measurer.measure(String.format(Locale.US, "%02d:00", it), style) } }
    val line = cs.onSurface.copy(alpha = if (isDarkUi()) 0.14f else 0.1f)
    Canvas(modifier.clipToBounds()) {
        val cur = pager.currentPage
        val f = pager.currentPageOffsetFraction
        val h0 = (shared.headers[cur] ?: 0).toFloat()
        val h1 = (shared.headers[cur + if (f >= 0f) 1 else -1] ?: shared.headers[cur] ?: 0).toFloat()
        val header = h0 + (h1 - h0) * abs(f)
        val hourPx = HOUR.toPx()
        val top = header - shared.px + shared.band.floatValue
        drawLine(line, Offset(0f, header), Offset(size.width, header), 1.dp.toPx())
        clipRect(top = header + 1f) {
            labels.forEachIndexed { i, l ->
                val y = top + (i + 1) * hourPx - l.size.height / 2f
                if (y > header - l.size.height && y < size.height) {
                    drawText(l, topLeft = Offset(size.width - 6.dp.toPx() - l.size.width, y))
                }
            }
        }
    }
}

@Composable
private fun TimelinePage(
    state: AppState, today: LocalDate, dates: List<LocalDate>, now: LocalTime, current: Boolean, page: Int,
    shared: ScrollMemo, draft: Draft?, onDraft: (Draft?) -> Unit, onOpen: (Task) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val wide = dates.size == 7
    val code = state.holidayCountry()
    val deadlineWord = tx("Deadline")
    val allDay = remember(state, dates, deadlineWord) {
        dates.map { d ->
            val h = if (state.showHolidays) Holidays.forYear(code, d.year)[d] else null
            allDayItems(state, d, h, deadlineWord)
        }
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().onSizeChanged { shared.headers[page] = it.height }) {
        Row(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 2.dp)) {
            dates.forEach { d ->
                val isToday = d == today
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        d.dayOfWeek.getDisplayName(JTextStyle.SHORT, Locale.getDefault()), fontSize = 12.sp, maxLines = 1,
                        color = if (isToday) cs.primary else cs.onSurfaceVariant
                    )
                    Box(
                        Modifier.padding(vertical = 2.dp).size(if (wide) 32.dp else 38.dp).clip(CircleShape)
                            .background(if (isToday) cs.primary else cs.primary.copy(alpha = 0f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${d.dayOfMonth}", fontSize = if (wide) 15.sp else 18.sp,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                            color = if (isToday) cs.onPrimary else cs.onBackground
                        )
                    }
                }
            }
        }
        if (allDay.any { it.isNotEmpty() }) {
            Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                allDay.forEach { items ->
                    Column(Modifier.weight(1f).padding(horizontal = 1.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        val shown = if (items.size > 3) items.take(2) else items
                        shown.forEach { AllDayChip(it, onOpen) }
                        if (items.size > 3) {
                            Text("+${items.size - 2}", fontSize = 11.sp, color = cs.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }
        }
        HorizontalDivider(color = cs.onSurface.copy(alpha = if (isDarkUi()) 0.14f else 0.1f))
        }
        val scroll = rememberScrollState(shared.px)
        val isCurrent by rememberUpdatedState(current)
        LaunchedEffect(scroll) { snapshotFlow { scroll.value }.collect { if (isCurrent) shared.px = it } }
        LaunchedEffect(scroll, current) {
            if (!current) snapshotFlow { shared.px }.collect { if (scroll.value != it) scroll.scrollTo(it) }
        }
        Row(Modifier.fillMaxSize().rubberBand(external = shared.band).verticalScroll(scroll)) {
            dates.forEach { d ->
                DayColumn(
                    state, d, today, now, draft?.takeIf { it.date == d }, onDraft, onOpen,
                    Modifier.weight(1f).height(HOUR * 24 + 24.dp)
                )
            }
        }
    }
}

@Composable
private fun AllDayChip(item: AllDayItem, onOpen: (Task) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val t = item.task
    val accent = if (t != null) itemAccent(t) else cs.onSurfaceVariant
    val bg = when {
        t == null -> cs.onSurfaceVariant.copy(alpha = 0.14f)
        t.color != null -> accent.copy(alpha = 0.3f)
        else -> cs.surfaceVariant
    }
    val shape = RoundedCornerShape(6.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(bg)
            .then(if (t != null) Modifier.clickable { onOpen(t) } else Modifier)
            .padding(horizontal = 5.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (item.icon) {
            "cake", "target" -> { LineIcon(item.icon ?: "target", 12.dp, accent); Spacer(Modifier.width(3.dp)) }
            "done" -> { Icon(Icons.Default.Check, null, Modifier.size(12.dp), tint = accent); Spacer(Modifier.width(3.dp)) }
        }
        StrikeText(item.label, item.icon == "done", fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun DayColumn(
    state: AppState, d: LocalDate, today: LocalDate, now: LocalTime, draft: Draft?,
    onDraft: (Draft?) -> Unit, onOpen: (Task) -> Unit, modifier: Modifier
) {
    val cs = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val hourPx = with(density) { HOUR.toPx() }
    val minPx = with(density) { 22.dp.toPx() }
    var colW by remember { mutableIntStateOf(0) }
    val blocks = remember(state, d) { layoutBlocks(state, d) }
    val line = cs.onSurface.copy(alpha = if (isDarkUi()) 0.13f else 0.1f)
    val half = line.copy(alpha = line.alpha * 0.45f)
    val draftCb by rememberUpdatedState(onDraft)
    Box(
        modifier.onSizeChanged { colW = it.width }
            .pointerInput(d) {
                detectTapGestures { o ->
                    val m = ((o.y / hourPx) * 60f).toInt().coerceIn(0, 1439)
                    val s = (m / 30) * 30
                    draftCb(Draft(d, s, min(1440, s + 60)))
                }
            }
    ) {
        Canvas(Modifier.fillMaxWidth().height(HOUR * 24)) {
            val sw = 1.dp.toPx()
            for (h in 0..23) {
                if (h > 0) drawLine(line, Offset(0f, h * hourPx), Offset(size.width, h * hourPx), sw)
                drawLine(half, Offset(0f, (h + 0.5f) * hourPx), Offset(size.width, (h + 0.5f) * hourPx), sw * 0.8f)
            }
            drawLine(line, Offset(0f, 0f), Offset(0f, size.height), sw)
        }
        blocks.forEach { b ->
            val w = if (b.cols > 0) colW / b.cols else colW
            val top = b.from / 60f * hourPx
            val hPx = max((b.to - b.from) / 60f * hourPx, minPx)
            TimelineBlock(
                b, done = b.task.isTask && state.isDone(b.task, d), onOpen = { onOpen(b.task) },
                modifier = Modifier.offset { IntOffset(b.col * w, top.roundToInt()) }
                    .size(with(density) { w.toDp() }, with(density) { hPx.toDp() }).padding(1.dp)
            )
        }
        if (d == today) {
            val y = (now.hour * 60 + now.minute) / 60f * hourPx
            Box(Modifier.offset { IntOffset(0, (y - 1f).roundToInt()) }.fillMaxWidth().height(2.dp).background(RED))
            Box(
                Modifier.offset { IntOffset(-5.dp.roundToPx(), (y - 5.dp.toPx()).roundToInt()) }
                    .size(10.dp).clip(CircleShape).background(RED)
            )
        }
        if (draft != null) DraftBlock(draft, hourPx, onDraft)
    }
}

@Composable
private fun TimelineBlock(b: Block, done: Boolean, onOpen: () -> Unit, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    val accent = itemAccent(b.task)
    val bg = if (b.task.color != null) accent.copy(alpha = 0.3f) else cs.surfaceVariant
    val shape = RoundedCornerShape(8.dp)
    val len = b.to - b.from
    Row(modifier.alpha(if (done) 0.5f else 1f).clip(shape).background(bg).clickable(onClick = onOpen)) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(accent))
        Column(Modifier.padding(horizontal = 5.dp, vertical = 3.dp)) {
            StrikeText(b.task.title, done, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = if (len >= 60) 2 else 1)
            if (len >= 45) {
                b.task.timeRange()?.let { Text(it, fontSize = 11.sp, color = cs.onSurfaceVariant, maxLines = 1) }
            }
        }
    }
}

@Composable
private fun DraftBlock(draft: Draft, hourPx: Float, onDraft: (Draft?) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val cur by rememberUpdatedState(draft)
    val change by rememberUpdatedState(onDraft)
    val top = draft.from / 60f * hourPx
    val h = (draft.to - draft.from) / 60f * hourPx
    fun drag(mode: Int) = Modifier.pointerInput(mode) {
        var f0 = 0
        var t0 = 0
        var acc = 0f
        detectVerticalDragGestures(
            onDragStart = { f0 = cur.from; t0 = cur.to; acc = 0f },
            onVerticalDrag = { c, dy ->
                c.consume()
                acc += dy
                val m = ((acc / hourPx * 60f) / 15f).roundToInt() * 15
                val nd = when (mode) {
                    0 -> {
                        val s = m.coerceIn(-f0, 1440 - t0)
                        cur.copy(from = f0 + s, to = t0 + s)
                    }
                    1 -> cur.copy(from = (f0 + m).coerceIn(0, t0 - 15))
                    else -> cur.copy(to = (t0 + m).coerceIn(f0 + 15, 1440))
                }
                if (nd != cur) change(nd)
            }
        )
    }
    val shape = RoundedCornerShape(8.dp)
    Box(Modifier.offset { IntOffset(0, top.roundToInt()) }.fillMaxWidth().height(with(density) { h.toDp() })) {
        Box(
            Modifier.matchParentSize().padding(1.dp).clip(shape).background(cs.primary.copy(alpha = 0.12f))
                .border(2.dp, cs.primary, shape).then(drag(0))
        )
        Box(Modifier.align(Alignment.TopStart).offset(x = 2.dp, y = (-16).dp).size(32.dp).then(drag(1)), contentAlignment = Alignment.Center) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(cs.primary))
        }
        Box(Modifier.align(Alignment.BottomEnd).offset(x = (-2).dp, y = 16.dp).size(32.dp).then(drag(2)), contentAlignment = Alignment.Center) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(cs.primary))
        }
    }
}

// ═══════════════ QuickCreate ═══════════════

@Composable
fun QuickCreate(
    draft: Draft, title: String, onTitle: (String) -> Unit, kind: Kind, onKind: (Kind) -> Unit,
    onClose: () -> Unit, onSave: () -> Unit, onMore: () -> Unit, modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val slot = when (kind) {
        Kind.EVENT -> "${minuteTime(draft.from)}–${minuteTime(draft.to)}"
        Kind.TASK -> "${minuteTime(draft.from)}"
        Kind.BIRTHDAY -> tx("Every year")
    }
    Surface(
        modifier.fillMaxWidth().softShadow(shape, 18.dp),
        shape = shape, color = cs.surface, contentColor = cs.onSurface
    ) {
        Column(Modifier.navigationBarsPadding().imePadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SIconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Close") }
                Spacer(Modifier.weight(1f))
                STextButton(onClick = onMore) { Text(tx("More options")) }
                Spacer(Modifier.width(6.dp))
                SButton(onClick = onSave, enabled = title.isNotBlank()) { Text(tx("Save")) }
            }
            BasicTextField(
                value = title, onValueChange = onTitle, singleLine = true,
                textStyle = TextStyle(color = cs.onSurface, fontSize = 26.sp),
                cursorBrush = SolidColor(cs.onSurface),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth().padding(start = 10.dp, top = 8.dp, bottom = 6.dp),
                decorationBox = { inner ->
                    Box {
                        if (title.isEmpty()) Text(tx("Add title"), fontSize = 26.sp, color = cs.onSurfaceVariant)
                        inner()
                    }
                }
            )
            Text(
                "${dayTitle(draft.date, LocalDate.now())}  ·  $slot", fontSize = 15.sp, color = cs.onSurfaceVariant,
                modifier = Modifier.padding(start = 10.dp, bottom = 14.dp)
            )
            KindChips(kind, onKind, Modifier.padding(start = 6.dp, bottom = 6.dp))
        }
    }
}
