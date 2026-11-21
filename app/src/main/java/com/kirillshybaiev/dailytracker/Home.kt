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
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.layout
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.TextStyle as JTextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ═══════════════ TodayScreen ═══════════════

fun todayLine(d: LocalDate): String =
    d.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))
        .replaceFirstChar { it.titlecase(Locale.getDefault()) }

@Composable
fun TodayPage(
    state: AppState, onToggle: (Task, LocalDate) -> Unit, onOpen: (Task) -> Unit,
    onAdd: (Kind) -> Unit, onInsights: () -> Unit, onMenu: () -> Unit, springy: Boolean,
    onReorder: (String, String) -> Unit, onDismissAll: (List<Task>) -> Unit,
    coach: Int = 0
) {
    val cs = MaterialTheme.colorScheme
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); now = LocalDateTime.now() } }
    val today = now.toLocalDate()
    val tasks = remember(state, today) { state.tasksOn(today) }
    val overdue = remember(state, today) { state.overdue(today) }
    val events = remember(state, today) { state.eventsOn(today) }
    val carried = remember(state, today) { state.carried(today) }
    var showOverdue by remember { mutableStateOf(false) }
    val doneCount = tasks.count { state.isDone(it, today) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    val reorder by rememberUpdatedState(onReorder)
    val taskIds by rememberUpdatedState(tasks.map { it.id }.toSet())
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragDy by remember { mutableStateOf(0f) }

    fun swapIfNeeded(id: String) {
        val infos = listState.layoutInfo.visibleItemsInfo
        val cur = infos.firstOrNull { it.key == id } ?: return
        val center = cur.offset + cur.size / 2f + dragDy
        val target = infos.firstOrNull { info ->
            val k = info.key as? String
            k != null && k != id && k in taskIds && center >= info.offset && center <= info.offset + info.size
        } ?: return
        reorder(id, target.key as String)
        haptics.step()
        val newOffset = if (target.offset > cur.offset) target.offset + target.size - cur.size else target.offset
        dragDy -= (newOffset - cur.offset)
    }

    fun finishDrag() {
        val start = dragDy
        scope.launch {
            Animatable(start).animateTo(0f, fluid(0.36f, 0.12f)) { dragDy = value }
            draggingId = null
            dragDy = 0f
        }
    }

    val anim: @Composable LazyItemScope.() -> Modifier = { if (springy) Modifier.animateItem() else Modifier }
    val nothing = tasks.isEmpty() && overdue.isEmpty() && events.isEmpty() && carried.isEmpty()

    Box(Modifier.fillMaxSize()) {
        LargeTitleScaffold(
            title = tx("Today"), subtitle = todayLine(today),
            nav = {
                Box(Modifier.coachRing(coach == 1, CircleShape)) {
                    SIconButton(onClick = onMenu) { MenuIcon() }
                }
            }
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = bottomSpace(130.dp))
            ) {
                item(key = "progress") {
                    ProgressCard(doneCount, tasks.size, coach == 2, onInsights, anim().padding(bottom = 8.dp))
                }
                if (nothing) {
                    item(key = "empty") {
                        Column(
                            anim().fillMaxWidth().padding(top = 56.dp, start = 24.dp, end = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(Modifier.size(64.dp).clip(CircleShape).background(cs.primaryContainer), contentAlignment = Alignment.Center) {
                                LineIcon("task", 28.dp, cs.primary)
                            }
                            Spacer(Modifier.height(14.dp))
                            Text(tx("No tasks yet. Tap + to add one."), color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
                        }
                    }
                }
                if (overdue.isNotEmpty()) {
                    item(key = "overdue_header") {
                        OverdueHeader(overdue.size, showOverdue, { showOverdue = !showOverdue }, anim())
                    }
                    if (showOverdue) {
                        items(overdue, key = { "od_" + it.id }) { t ->
                            TaskRow(
                                t, false, springy,
                                onToggle = { onToggle(t, t.date) }, onOpen = { onOpen(t) },
                                overdueText = "Was due ${fmtDate(t.date)}",
                                modifier = anim()
                            )
                        }
                        item(key = "od_dismiss") {
                            Row(anim().fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                STextButton(onClick = { onDismissAll(overdue) }) { Text(tx("Dismiss all")) }
                            }
                        }
                    }
                }
                items(events, key = { "ev_" + it.first.id }) { (t, s) ->
                    EventRow(t, s, today, onOpen = { onOpen(t) }, modifier = anim())
                }
                if (!nothing && tasks.isEmpty() && events.isEmpty()) {
                    item(key = "nothing_today") {
                        Box(anim().fillMaxWidth().padding(vertical = 28.dp), contentAlignment = Alignment.Center) {
                            Text(tx("Nothing planned for today."), color = cs.onSurfaceVariant)
                        }
                    }
                }
                items(tasks, key = { it.id }) { t ->
                    val dragging = draggingId == t.id
                    TaskRow(
                        t, state.isDone(t, today), springy,
                        onToggle = { onToggle(t, today) }, onOpen = { onOpen(t) },
                        lifted = dragging,
                        modifier = (if (springy && !dragging) Modifier.animateItem() else Modifier)
                            .zIndex(if (dragging) 1f else 0f)
                            .graphicsLayer {
                                translationY = if (dragging) dragDy else 0f
                                val sc = if (dragging) 1.03f else 1f
                                scaleX = sc
                                scaleY = sc
                            }
                            .pointerInput(t.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        haptics.lift()
                                        draggingId = t.id
                                        dragDy = 0f
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragDy += amount.y
                                        swapIfNeeded(t.id)
                                    },
                                    onDragEnd = { finishDrag() },
                                    onDragCancel = { finishDrag() }
                                )
                            }
                    )
                }
                if (carried.isNotEmpty()) {
                    item(key = "carried_header") { GroupLabel(tx("Due soon"), anim().padding(top = 4.dp)) }
                    items(carried, key = { "cr_" + it.id }) { t ->
                        TaskRow(
                            t, false, springy,
                            onToggle = { onToggle(t, t.date) }, onOpen = { onOpen(t) },
                            note = "${tx("Deadline")}: ${fmtDate(t.deadline ?: t.date)}",
                            modifier = anim()
                        )
                    }
                }
            }
        }
        AddMenuFab(onPick = onAdd)
    }
}

@Composable
fun ProgressCard(done: Int, total: Int, coach: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val full = total > 0 && done >= total
    val tint = cs.primary.copy(alpha = if (isDarkUi()) 0.17f else 0.09f).compositeOver(cs.surface)
    CompositionLocalProvider(LocalCardColor provides tint) {
    Card(modifier.coachRing(coach, CardShape), onClick = onClick) {
        Row(
            Modifier.drawBehind {
                drawCircle(
                    Brush.radialGradient(listOf(cs.primary.copy(alpha = 0.16f), Color.Transparent), center = Offset(size.width * 0.85f, size.height * 0.5f), radius = size.height * 1.1f),
                    radius = size.height * 1.1f, center = Offset(size.width * 0.85f, size.height * 0.5f)
                )
            }.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tx("How it's going"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = cs.primary)
                    Spacer(Modifier.width(2.dp))
                    Chevron(color = cs.primary.copy(alpha = 0.8f))
                }
                Spacer(Modifier.height(6.dp))
                if (total > 0) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        SwapText("$done", fontSize = 40.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            " / $total", fontSize = 20.sp, color = cs.onSurfaceVariant, fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 7.dp)
                        )
                    }
                    val left = total - done
                    SwapText(
                        if (left <= 0) tx("All done") else "$left ${tx("left")}",
                        fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (left <= 0) GREEN else cs.onSurfaceVariant
                    )
                } else {
                    Text(tx("Nothing planned for today."), fontSize = 15.sp, color = cs.onSurface)
                }
            }
            if (total > 0) {
                ProgressRing(done.toFloat() / total, if (full) GREEN else cs.primary, Modifier.size(76.dp), stroke = 8.dp) {
                    SwapText("${done * 100 / total}%", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
    }
}

@Composable
fun OverdueHeader(count: Int, expanded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val rot by animateFloatAsState(if (expanded) 90f else 0f, fluid(0.36f, 0.12f), label = "overdueArrow")
    Row(
        modifier.fillMaxWidth().padding(vertical = 4.dp).springClick(CardShape, 0.98f, onClick = onClick)
            .background(RED.copy(alpha = 0.09f).compositeOver(cardColor()))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(RED))
        Spacer(Modifier.width(12.dp))
        Text("$count overdue", color = RED, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Chevron(Modifier.graphicsLayer { rotationZ = rot }, color = RED.copy(alpha = 0.8f))
    }
}

@Composable
fun TaskRow(
    task: Task, done: Boolean, springy: Boolean, onToggle: () -> Unit, onOpen: () -> Unit,
    skipped: Boolean = false, overdueText: String? = null, toggleEnabled: Boolean = true, modifier: Modifier = Modifier,
    note: String? = null, lifted: Boolean = false
) {
    val cs = MaterialTheme.colorScheme
    val accent = task.color?.let { Color(it) } ?: cs.primary
    val dim by animateFloatAsState(if (done || skipped) 0.45f else 1f, tween(240), label = "dim")
    val lift by animateFloatAsState(if (lifted) 1f else 0f, fluid(0.36f, 0.12f), label = "lift")
    val bump = remember { Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(done) {
        if (first) first = false
        else if (springy) {
            bump.snapTo(0.82f)
            bump.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 520f))
        }
    }
    val desc = task.description.takeIf { it.isNotBlank() }
    val second = overdueText ?: note ?: desc ?: if (skipped) tx("Skipped") else null
    Row(
        modifier.fillMaxWidth().padding(vertical = 4.dp)
            .graphicsLayer { shadowElevation = lift * 14.dp.toPx(); shape = CardShape; clip = false }
            .springClick(CardShape, 0.98f, onClick = onOpen)
            .background(cardColor())
            .heightIn(min = 64.dp)
            .padding(start = 4.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(52.dp).springClick(CircleShape, 0.8f, enabled = toggleEnabled && !skipped, onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            SCheck(done, accent, Modifier.graphicsLayer { scaleX = bump.value; scaleY = bump.value })
        }
        Column(Modifier.weight(1f).padding(vertical = 12.dp).graphicsLayer { alpha = dim }) {
            StrikeText(task.title, done, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 2)
            if (second != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    second, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = if (overdueText != null) RED else cs.onSurfaceVariant
                )
            }
        }
        val time = task.time
        if (time != null && overdueText == null) {
            Text(
                "$time", fontSize = 14.sp, color = cs.onSurfaceVariant, style = TextStyle(fontFeatureSettings = "tnum"),
                modifier = Modifier.padding(start = 10.dp).graphicsLayer { alpha = dim }
            )
        }
    }
}

// ═══════════════ CalendarScreen ═══════════════

@Composable
fun dayTitle(d: LocalDate, today: LocalDate): String = when (d) {
    today -> tx("Today")
    today.plusDays(1) -> tx("Tomorrow")
    today.minusDays(1) -> tx("Yesterday")
    else -> "${d.dayOfWeek.getDisplayName(JTextStyle.SHORT, Locale.getDefault())}, " +
        "${d.dayOfMonth} ${d.month.getDisplayName(JTextStyle.FULL, Locale.getDefault())}"
}

const val MONTH_PAGES = 2400

@Suppress("UnusedBoxWithConstraintsScope")
@Composable
fun CalendarPage(
    state: AppState, onClose: () -> Unit, onOpen: (Task) -> Unit,
    onToggle: (Task, LocalDate) -> Unit, onMenu: () -> Unit, onSelect: (LocalDate) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    val springy = LocalSpringy.current
    val today = rememberToday()
    val base = remember { YearMonth.from(today) }
    val pager = rememberPagerState(initialPage = MONTH_PAGES / 2) { MONTH_PAGES }
    val month by remember { derivedStateOf { base.plusMonths((pager.currentPage - MONTH_PAGES / 2).toLong()) } }
    var selected by remember { mutableStateOf(today) }
    val scope = rememberCoroutineScope()
    fun showMonth(m: YearMonth) {
        val page = MONTH_PAGES / 2 + ChronoUnit.MONTHS.between(base, m).toInt()
        scope.launch { pager.animateScrollToPage(page, animationSpec = if (springy) fluid(0.5f) else tween(250)) }
    }
    LaunchedEffect(today) { if (month != YearMonth.from(today)) showMonth(YearMonth.from(today)); selected = today }
    LaunchedEffect(selected) { onSelect(selected) }
    val holidays = remember(selected.year, state.showHolidays, state.country) {
        if (state.showHolidays) Holidays.forYear(state.holidayCountry(), selected.year) else emptyMap<LocalDate, String>()
    }

    val slideSpec: FiniteAnimationSpec<IntOffset> =
        if (springy) fluidOffset(0.42f, 0.1f) else snap()
    val fadeSpec: FiniteAnimationSpec<Float> = if (springy) fluid(0.28f) else tween(120)
    val sizeSpec: FiniteAnimationSpec<IntSize> =
        if (springy) fluidSize(0.42f, 0.1f) else snap()
    val monthTransition: AnimatedContentTransitionScope<YearMonth>.() -> ContentTransform = {
        val dir = if (targetState > initialState) 1 else -1
        (slideInHorizontally(slideSpec) { it * dir } + fadeIn(fadeSpec)) togetherWith
            (slideOutHorizontally(slideSpec) { -it * dir } + fadeOut(fadeSpec)) using SizeTransform(clip = true) { _, _ -> sizeSpec }
    }

    val density = LocalDensity.current
    val statusTop = WindowInsets.statusBars.getTop(density)
    val dayDrag = remember { Animatable(0f) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val totalPx = constraints.maxHeight.toFloat()
        var gridPx by remember { mutableIntStateOf(0) }
        val keepPx = statusTop + with(density) { 64.dp.toPx() }
        fun maxExtraNow() = (gridPx - keepPx).coerceAtLeast(0f)
        val extra = remember { Animatable(0f) }
        val dragPanel = Modifier.pointerInput(Unit) {
            var last = 0f
            detectVerticalDragGestures(
                onDragStart = { last = 0f },
                onVerticalDrag = { change, dy ->
                    change.consume()
                    last = dy
                    scope.launch { extra.snapTo((extra.value - dy).coerceIn(0f, maxExtraNow())) }
                },
                onDragEnd = {
                    scope.launch {
                        val maxExtra = maxExtraNow()
                        val target = when { last < -1f -> maxExtra; last > 1f -> 0f; extra.value > maxExtra / 2f -> maxExtra; else -> 0f }
                        extra.animateTo(target, fluid(0.42f, 0.12f))
                    }
                },
                onDragCancel = { scope.launch { extra.animateTo(0f, fluid(0.42f, 0.12f)) } }
            )
        }
        Column(Modifier.align(Alignment.TopCenter).onSizeChanged { gridPx = it.height }.statusBarsPadding().padding(horizontal = 14.dp).padding(top = 8.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SIconButton(onClick = onMenu) { MenuIcon() }
                Row(
                    Modifier.weight(1f).springClick(RShape(12.dp), 0.97f) {
                        val d0 = if (YearMonth.from(selected) == month) selected else month.atDay(1)
                        android.app.DatePickerDialog(
                            ctx, { _, y, mo, d -> val pick = LocalDate.of(y, mo + 1, d); selected = pick; showMonth(YearMonth.from(pick)) },
                            d0.year, d0.monthValue - 1, d0.dayOfMonth
                        ).show()
                    }.padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedContent(targetState = month, transitionSpec = monthTransition, label = "title") { m ->
                        val name = m.month.getDisplayName(JTextStyle.FULL_STANDALONE, Locale.getDefault())
                            .replaceFirstChar { it.titlecase(Locale.getDefault()) }
                        Text(name + if (m.year != today.year) " ${m.year}" else "", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("▾", color = cs.onSurfaceVariant)
                }
                SIconButton(onClick = { showMonth(YearMonth.from(today)); selected = today }) { TodayIcon(today.dayOfMonth, cs.onBackground) }
                SIconButton(onClick = onClose) { CloseIcon(cs.onBackground) }
            }
            Row(Modifier.fillMaxWidth()) {
                val order = DayOfWeek.values().let { v -> v.drop(state.weekStartDay - 1) + v.take(state.weekStartDay - 1) }
                order.forEach { d ->
                    Text(
                        d.getDisplayName(JTextStyle.NARROW, Locale.getDefault()), Modifier.weight(1f),
                        textAlign = TextAlign.Center, color = cs.onSurfaceVariant, fontSize = 12.sp
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            HorizontalPager(
                state = pager, beyondViewportPageCount = 1, key = { it },
                modifier = Modifier.fillMaxWidth().clipToBounds().layout { measurable, constraints ->
                    val pl = measurable.measure(constraints)
                    val f = pager.currentPageOffsetFraction
                    val here = monthRows(base.plusMonths((pager.currentPage - MONTH_PAGES / 2).toLong()), state.weekStartDay)
                    val next = monthRows(base.plusMonths((pager.currentPage - MONTH_PAGES / 2 + if (f >= 0f) 1 else -1).toLong()), state.weekStartDay)
                    val rows = here + (next - here) * abs(f)
                    val h = (pl.height * rows / 6f).roundToInt()
                    layout(pl.width, h) { pl.place(0, 0) }
                }
            ) { page ->
                MonthGrid(state, base.plusMonths((page - MONTH_PAGES / 2).toLong()), today, selected) { selected = it }
            }
            Spacer(Modifier.height(8.dp))
        }

        val panelShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().layout { measurable, constraints ->
                val g = gridPx
                val collapsed = if (g == 0) totalPx * 0.55f else (totalPx - g).coerceAtLeast(0f)
                val h = (collapsed + extra.value.coerceIn(0f, maxExtraNow())).coerceAtMost(totalPx).roundToInt()
                val pl = measurable.measure(androidx.compose.ui.unit.Constraints.fixed(constraints.maxWidth, h))
                layout(constraints.maxWidth, h) { pl.place(0, 0) }
            }
                .then(if (isDarkUi()) Modifier else Modifier.softShadow(panelShape, 18.dp)).clip(panelShape).background(cs.background)
                .fadingEdge(top = true, radius = 28.dp)
        ) {
            Column(Modifier.fillMaxSize()) {
                Box(
                    Modifier.fillMaxWidth().then(dragPanel).springClick(RShape(12.dp), 1f) {
                        scope.launch { val maxExtra = maxExtraNow(); extra.animateTo(if (extra.value > maxExtra / 2f) 0f else maxExtra, fluid(0.42f, 0.12f)) }
                    }.padding(top = 10.dp, bottom = 2.dp),
                    contentAlignment = Alignment.Center
                ) { Box(Modifier.size(36.dp, 4.dp).clip(RShape(2.dp)).background(cs.onSurfaceVariant.copy(alpha = 0.35f))) }
                val swipeDay = Modifier.pointerInput(Unit) {
                    var total = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { total = 0f },
                        onDragEnd = {
                            val step = when { total < -64.dp.toPx() -> 1; total > 64.dp.toPx() -> -1; else -> 0 }
                            if (step != 0) {
                                val nd = selected.plusDays(step.toLong())
                                selected = nd
                                if (YearMonth.from(nd) != month) showMonth(YearMonth.from(nd))
                            }
                            scope.launch { dayDrag.animateTo(0f, fluid(0.36f)) }
                        },
                        onDragCancel = { scope.launch { dayDrag.animateTo(0f, fluid(0.36f)) } },
                        onHorizontalDrag = { change, dx ->
                            change.consume()
                            total += dx
                            scope.launch { dayDrag.snapTo(total * 0.35f) }
                        }
                    )
                }
                Column(Modifier.weight(1f).fillMaxWidth().then(swipeDay).graphicsLayer {
                    translationX = dayDrag.value
                    alpha = 1f - (abs(dayDrag.value) / size.width.coerceAtLeast(1f)) * 1.2f
                }) {
                    Row(
                        Modifier.then(dragPanel).padding(start = 24.dp, end = 18.dp, top = 12.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            SwapText(dayTitle(selected, today), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            val holiday = holidays[selected]
                            SwapText(holiday ?: tx("Tasks for this day"), fontSize = 13.sp, color = cs.onSurfaceVariant)
                        }
                    }
                    AnimatedContent(
                        targetState = selected, label = "dayList", modifier = Modifier.weight(1f).fillMaxWidth(),
                        transitionSpec = {
                            val dir = if (targetState > initialState) 1 else -1
                            if (springy) {
                                (slideInHorizontally(fluidOffset(0.4f)) { it * dir / 4 } + fadeIn(fluid(0.26f))) togetherWith
                                    (slideOutHorizontally(fluidOffset(0.4f)) { -it * dir / 4 } + fadeOut(fluid(0.16f)))
                            } else fadeIn(tween(120)) togetherWith fadeOut(tween(120))
                        }
                    ) { d ->
                        DayList(state, d, today, onOpen, onToggle, Modifier.fillMaxSize().rubberBand(), holidays[d])
                    }
                }
            }
        }
    }
}

@Composable
fun DayList(
    state: AppState, date: LocalDate, today: LocalDate, onOpen: (Task) -> Unit,
    onToggle: (Task, LocalDate) -> Unit, modifier: Modifier, holiday: String? = null
) {
    val cs = MaterialTheme.colorScheme
    val springy = LocalSpringy.current
    val tasks = state.tasksOn(date, includeSkipped = true)
    val events = state.eventsOn(date)
    val deadlines = state.deadlinesOn(date)
    CompositionLocalProvider(LocalCardColor provides cs.surface) {
    LazyColumn(modifier, contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = bottomSpace(90.dp))) {
        if (holiday != null) {
            item(key = "holiday") {
                val shape = RShape(16.dp)
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp).padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(8.dp).clip(RShape(3.dp)).background(cs.onSurfaceVariant.copy(alpha = 0.55f)))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(holiday, fontWeight = FontWeight.SemiBold)
                        Text(tx("Public holiday"), fontSize = 12.sp, color = cs.onSurfaceVariant)
                    }
                }
            }
        }
        items(events, key = { "ev_" + it.first.id }) { (t, s) ->
            EventRow(t, s, date, onOpen = { onOpen(t) }, modifier = if (springy) Modifier.animateItem() else Modifier)
        }
        if (tasks.isEmpty() && events.isEmpty() && deadlines.isEmpty()) {
            item(key = "empty") {
                Box(Modifier.fillMaxWidth().padding(vertical = 26.dp), contentAlignment = Alignment.Center) {
                    Text(tx("Nothing on this day"), color = cs.onSurfaceVariant)
                }
            }
        }
        items(tasks, key = { it.id }) { t ->
            TaskRow(
                t, state.isDone(t, date), springy,
                onToggle = { onToggle(t, date) }, onOpen = { onOpen(t) },
                skipped = state.isSkipped(t, date),
                toggleEnabled = !date.isAfter(today),
                modifier = if (springy) Modifier.animateItem() else Modifier
            )
        }
        items(deadlines, key = { "dl_" + it.id }) { t ->
            DeadlineRow(t, onOpen = { onOpen(t) }, modifier = if (springy) Modifier.animateItem() else Modifier)
        }
    }
    }
}

fun monthRows(m: YearMonth, weekStart: Int): Int {
    val offset = (m.atDay(1).dayOfWeek.value - weekStart + 7) % 7
    return (offset + m.lengthOfMonth() + 6) / 7
}

@Composable
fun MonthGrid(state: AppState, m: YearMonth, today: LocalDate, selected: LocalDate, onSelect: (LocalDate) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val springy = LocalSpringy.current
    val haptics = rememberHaptics()
    val holidays = remember(m.year, state.showHolidays, state.country) {
        if (state.showHolidays) Holidays.forYear(state.holidayCountry(), m.year) else emptyMap<LocalDate, String>()
    }
    val offset = (m.atDay(1).dayOfWeek.value - state.weekStartDay + 7) % 7
    val len = m.lengthOfMonth()
    val measurer = rememberTextMeasurer(cacheSize = 80)
    val normal = TextStyle(fontSize = 15.sp, fontFeatureSettings = "tnum")
    val bold = normal.copy(fontWeight = FontWeight.SemiBold)
    val selIndex = if (YearMonth.from(selected) == m) offset + selected.dayOfMonth - 1 else -1
    val col = remember { Animatable(if (selIndex >= 0) (selIndex % 7).toFloat() else 0f) }
    val row = remember { Animatable(if (selIndex >= 0) (selIndex / 7).toFloat() else 0f) }
    val show = remember { Animatable(if (selIndex >= 0) 1f else 0f) }
    LaunchedEffect(selIndex) {
        if (selIndex < 0) { show.animateTo(0f, if (springy) fluid(0.2f) else snap()); return@LaunchedEffect }
        val c = (selIndex % 7).toFloat()
        val r = (selIndex / 7).toFloat()
        if (show.value < 0.05f || !springy) {
            col.snapTo(c); row.snapTo(r)
            show.snapTo(0f)
            show.animateTo(1f, if (springy) fluid(0.36f, 0.3f) else snap())
        } else {
            launch { col.animateTo(c, fluid(0.34f, 0.12f)) }
            launch { row.animateTo(r, fluid(0.34f, 0.12f)) }
        }
    }
    val pick by rememberUpdatedState(onSelect)
    Canvas(
        Modifier.fillMaxWidth().aspectRatio(7f * 1.15f / 6f).pointerInput(m, offset) {
            detectTapGestures { pos ->
                val cw = size.width / 7f
                val ch = size.height / 6f
                val day = (pos.y / ch).toInt() * 7 + (pos.x / cw).toInt() - offset + 1
                if (day in 1..len) { haptics.tick(); pick(m.atDay(day)) }
            }
        }
    ) {
        val cw = size.width / 7f
        val ch = size.height / 6f
        val radius = minOf(20.dp.toPx(), cw / 2f, ch / 2f)
        val q = show.value
        if (q > 0.01f) {
            drawCircle(
                cs.primary.copy(alpha = q.coerceIn(0f, 1f)), radius * (0.55f + 0.45f * q),
                Offset(cw * (col.value + 0.5f), ch * (row.value + 0.5f))
            )
        }
        for (day in 1..len) {
            val idx = offset + day - 1
            val date = m.atDay(day)
            val isSel = idx == selIndex
            val holiday = date in holidays
            val color = when {
                isSel && q > 0.5f -> cs.onPrimary
                date == today -> cs.primary
                holiday -> RED
                date.dayOfWeek == DayOfWeek.SUNDAY -> RED.copy(alpha = 0.85f)
                else -> cs.onBackground
            }
            val layout = measurer.measure("$day", if (date == today || isSel || holiday) bold else normal)
            val cx = cw * (idx % 7 + 0.5f)
            val cy = ch * (idx / 7 + 0.5f)
            drawText(layout, color, Offset(cx - layout.size.width / 2f, cy - layout.size.height / 2f))
            if (holiday && !isSel) drawCircle(RED, 2.4.dp.toPx(), Offset(cx, cy + radius - 7.dp.toPx()))
        }
    }
}

@Composable
fun DayCell(
    date: LocalDate, isToday: Boolean, selected: Boolean, holiday: Boolean,
    onClick: () -> Unit, modifier: Modifier
) {
    val cs = MaterialTheme.colorScheme
    val springy = LocalSpringy.current
    val fgTarget = when {
        selected -> cs.onPrimary
        isToday -> cs.primary
        holiday -> RED
        date.dayOfWeek == DayOfWeek.SUNDAY -> RED.copy(alpha = 0.85f)
        else -> cs.onBackground
    }
    val fg by animateColorAsState(fgTarget, if (springy) fluid(0.26f) else tween(120), label = "dayFg")
    val sel by animateFloatAsState(if (selected) 1f else 0f, if (springy) fluid(0.42f, 0.22f) else tween(120), label = "daySel")
    Box(modifier.aspectRatio(1.15f), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(40.dp)
                .springClick(CircleShape, 0.88f, onClick = onClick)
                .drawBehind {
                    if (sel > 0.001f) drawCircle(cs.primary.copy(alpha = sel.coerceIn(0f, 1f)), size.minDimension / 2f * (0.55f + 0.45f * sel))
                    val dotA = (1f - sel).coerceIn(0f, 1f)
                    if (holiday && dotA > 0.01f) {
                        drawCircle(RED.copy(alpha = dotA), 2.4.dp.toPx(), Offset(size.width / 2f, size.height - 7.dp.toPx()))
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "${date.dayOfMonth}", fontSize = 15.sp, color = fg,
                fontWeight = if (isToday || selected || holiday) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

// ═══════════════ TaskSheet ═══════════════

enum class Mode { ONCE, INTERVAL, WEEKDAYS, MONTHLY, YEARLY }

fun defaultAlerts(kind: Kind, allDay: Boolean): List<Int> = when (kind) {
    Kind.TASK -> if (allDay) emptyList() else listOf(0)
    Kind.EVENT -> if (allDay) emptyList() else listOf(30)
    Kind.BIRTHDAY -> listOf(0)
}

fun fmtLong(d: LocalDate) =
    "${d.dayOfWeek.getDisplayName(JTextStyle.SHORT, Locale.getDefault())}, " +
        "${d.dayOfMonth} ${d.month.getDisplayName(JTextStyle.SHORT, Locale.getDefault())} ${d.year}"

@Composable
fun alertLabel(min: Int, allDay: Boolean): String {
    if (allDay) {
        when (min) {
            0 -> return tx("On the day at 09:00")
            1440 -> return tx("1 day before at 09:00")
            10080 -> return tx("1 week before at 09:00")
        }
    } else {
        when (min) {
            0 -> return tx("At start")
            5 -> return tx("5 minutes before")
            10 -> return tx("10 minutes before")
            15 -> return tx("15 minutes before")
            30 -> return tx("30 minutes before")
            60 -> return tx("1 hour before")
            1440 -> return tx("1 day before")
        }
    }
    return when {
        min % 1440 == 0 -> "${min / 1440} ${tx("days before")}"
        min % 60 == 0 -> "${min / 60} ${tx("hours before")}"
        else -> "$min ${tx("minutes before")}"
    }
}

@Composable
private fun EditRow(
    icon: (@Composable () -> Unit)?, onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.springClick(RectangleShape, 0.99f, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(40.dp), contentAlignment = Alignment.CenterStart) { icon?.invoke() }
        content()
    }
}

@Composable
private fun PickText(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis,
        style = TextStyle(fontFeatureSettings = "tnum"),
        modifier = modifier.springClick(RShape(10.dp), 0.95f, onClick = onClick).padding(horizontal = 6.dp, vertical = 10.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskSheet(
    initial: Task, isNew: Boolean, overdue: Boolean, paused: Boolean,
    onDismiss: () -> Unit, onSave: (Task) -> Unit, onDelete: ((Task) -> Unit)?,
    onStats: ((Task) -> Unit)? = null, onSkipToday: ((Task) -> Unit)? = null,
    onPauseUntil: ((Task, LocalDate) -> Unit)? = null, onResume: ((Task) -> Unit)? = null,
    onDismissOverdue: ((Task) -> Unit)? = null,
    onMoveTomorrow: ((Task) -> Unit)? = null, onShiftAll: ((Task) -> Unit)? = null,
    onProgress: (Float) -> Unit = {}
) {
    val ctx = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetScope = rememberCoroutineScope()
    var closing by remember { mutableStateOf(false) }
    fun closeThen(action: () -> Unit) {
        if (closing) return
        closing = true
        sheetScope.launch { runCatching { sheetState.hide() } }.invokeOnCompletion { action() }
    }
    val rootView = LocalView.current
    var sheetH by remember { mutableIntStateOf(0) }
    val progress by rememberUpdatedState(onProgress)
    LaunchedEffect(sheetState) {
        snapshotFlow { runCatching { sheetState.requireOffset() }.getOrNull() }.collect { off ->
            val h = sheetH
            if (off != null && h > 0) progress(((rootView.rootView.height - off) / h).coerceIn(0f, 1f))
        }
    }
    val cs = MaterialTheme.colorScheme
    val springy = LocalSpringy.current
    val startMode = when {
        !initial.repeating -> Mode.ONCE
        initial.weekdays != 0 -> Mode.WEEKDAYS
        initial.unit == 1 -> Mode.MONTHLY
        initial.unit == 2 -> Mode.YEARLY
        else -> Mode.INTERVAL
    }
    val defaultStart = remember { LocalTime.now().withMinute(0).withSecond(0).withNano(0).plusHours(1) }
    var kind by remember { mutableStateOf(initial.kind) }
    var title by remember { mutableStateOf(initial.title) }
    var desc by remember { mutableStateOf(initial.description) }
    var editDesc by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(startMode) }
    var detailsVisible by remember { mutableStateOf(isNew) }
    var intervalText by remember { mutableStateOf(initial.interval.toString()) }
    val interval = intervalText.toIntOrNull()?.coerceIn(1, 365) ?: 1
    var days by remember { mutableStateOf(initial.weekdays.takeIf { it != 0 } ?: 21) }
    var allDay by remember { mutableStateOf(initial.time == null) }
    var date by remember { mutableStateOf(initial.date) }
    var time by remember { mutableStateOf(initial.time ?: defaultStart) }
    var endDate by remember { mutableStateOf(initial.date.plusDays(initial.spanDays.toLong())) }
    var endTime by remember { mutableStateOf(initial.endTime ?: (initial.time ?: defaultStart).plusHours(1)) }
    var color by remember { mutableStateOf(initial.color) }
    var location by remember { mutableStateOf(initial.location) }
    var alerts by remember { mutableStateOf(initial.alerts) }
    var alertsTouched by remember { mutableStateOf(!isNew) }
    var deadline by remember { mutableStateOf(initial.deadline) }
    var showPicker by remember { mutableStateOf(false) }
    var askDelete by remember { mutableStateOf(false) }

    fun startAt() = date.atTime(if (allDay) LocalTime.MIDNIGHT else time)
    fun endAt() = endDate.atTime(if (allDay) LocalTime.MIDNIGHT else endTime)
    fun setStart(nd: LocalDate, nt: LocalTime) {
        val dur = ChronoUnit.MINUTES.between(startAt(), endAt()).coerceAtLeast(0L)
        date = nd
        time = nt
        val ne = nd.atTime(if (allDay) LocalTime.MIDNIGHT else nt).plusMinutes(dur)
        endDate = ne.toLocalDate()
        if (!allDay) endTime = ne.toLocalTime()
        deadline = deadline?.takeIf { it.isAfter(nd) }
    }
    fun setEnd(nd: LocalDate, nt: LocalTime) {
        var ne = nd.atTime(if (allDay) LocalTime.MIDNIGHT else nt)
        if (ne.isBefore(startAt())) ne = if (allDay) startAt() else startAt().plusHours(1)
        endDate = ne.toLocalDate()
        if (!allDay) endTime = ne.toLocalTime()
    }
    fun refreshAlerts() { if (!alertsTouched) alerts = defaultAlerts(kind, allDay) }
    fun applyKind(k: Kind) {
        val was = kind
        kind = k
        when (k) {
            Kind.BIRTHDAY -> { mode = Mode.YEARLY; intervalText = "1"; allDay = true }
            Kind.EVENT -> { if (was == Kind.BIRTHDAY) mode = Mode.ONCE; allDay = false }
            Kind.TASK -> { if (was == Kind.BIRTHDAY) mode = Mode.ONCE }
        }
        if (k != Kind.EVENT) { endDate = date }
        refreshAlerts()
    }
    fun pickDate(d0: LocalDate, min: LocalDate? = null, onPick: (LocalDate) -> Unit) {
        val dlg = android.app.DatePickerDialog(
            ctx, { _, y, mo, d -> onPick(LocalDate.of(y, mo + 1, d)) }, d0.year, d0.monthValue - 1, d0.dayOfMonth
        )
        if (min != null) dlg.datePicker.minDate = min.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        dlg.show()
    }
    fun pickTime(t0: LocalTime, onPick: (LocalTime) -> Unit) {
        android.app.TimePickerDialog(ctx, { _, h, mi -> onPick(LocalTime.of(h, mi)) }, t0.hour, t0.minute, true).show()
    }
    fun pickPauseEnd() {
        val tomorrow = LocalDate.now().plusDays(1)
        pickDate(tomorrow, tomorrow) { d -> closeThen { onPauseUntil?.invoke(initial, d) } }
    }
    fun build(): Task {
        val event = kind == Kind.EVENT
        val sameSchedule = mode == startMode && date == initial.date && interval == initial.interval &&
            (mode != Mode.WEEKDAYS || days == initial.weekdays)
        return initial.copy(
            title = title.trim(), description = desc.trim(), kind = kind,
            repeating = mode != Mode.ONCE, date = date,
            time = if (allDay) null else time,
            color = color,
            interval = if (mode == Mode.INTERVAL || mode == Mode.MONTHLY || mode == Mode.YEARLY) interval else 1,
            weekdays = if (mode == Mode.WEEKDAYS) days else 0,
            unit = when (mode) { Mode.MONTHLY -> 1; Mode.YEARLY -> 2; else -> 0 },
            shifts = if (sameSchedule) initial.shifts else emptyList(),
            endTime = if (event && !allDay) endTime else null,
            spanDays = if (event) ChronoUnit.DAYS.between(date, endDate).toInt().coerceIn(0, 60) else 0,
            location = if (event) location.trim() else "",
            alerts = alerts.distinct().sorted(),
            deadline = if (kind == Kind.TASK && mode == Mode.ONCE) deadline?.takeIf { it.isAfter(date) } else null
        )
    }

    val modeNames = mapOf(
        Mode.ONCE to tx("Does not repeat"), Mode.INTERVAL to tx("Every X days"), Mode.WEEKDAYS to tx("Weekdays"),
        Mode.MONTHLY to tx("Every month"), Mode.YEARLY to tx("Every year")
    )
    val repeatText = when (mode) {
        Mode.ONCE -> tx("Does not repeat")
        Mode.INTERVAL -> when (interval) {
            1 -> tx("Every day")
            7 -> tx("Every week")
            else -> "${tx("Every")} $interval ${tx("days")}"
        }
        Mode.MONTHLY -> if (interval == 1) tx("Every month") else "${tx("Every")} $interval ${tx("months")}"
        Mode.YEARLY -> if (interval == 1) tx("Every year") else "${tx("Every")} $interval ${tx("years")}"
        Mode.WEEKDAYS -> when (days) {
            0 -> tx("Pick days")
            127 -> tx("Every day")
            else -> DayOfWeek.values().filter { ((days shr (it.value - 1)) and 1) == 1 }
                .joinToString(", ") { it.getDisplayName(JTextStyle.SHORT, Locale.getDefault()) }
        }
    }
    val fadeSpec: FiniteAnimationSpec<Float> = if (springy) spring(stiffness = 500f) else tween(120)
    val slideSpec: FiniteAnimationSpec<IntOffset> =
        if (springy) fluidOffset(0.42f, 0.1f) else snap()
    val sizeSpec: FiniteAnimationSpec<IntSize> =
        if (springy) fluidSize(0.42f, 0.1f) else snap()
    val rowIn = if (springy) expandVertically(fluidSize(0.36f), expandFrom = Alignment.Top) + fadeIn(fluid(0.32f)) else fadeIn(tween(120))
    val rowOut = if (springy) shrinkVertically(fluidSize(0.32f), shrinkTowards = Alignment.Top) + fadeOut(fluid(0.18f)) else fadeOut(tween(120))
    val chipIn = if (springy) fadeIn(fluid(0.28f)) + scaleIn(fluid(0.34f, 0.2f), initialScale = 0.8f) else fadeIn(tween(120))
    val chipOut = if (springy) fadeOut(fluid(0.18f)) + scaleOut(fluid(0.26f), targetScale = 0.8f) else fadeOut(tween(120))
    val grow = Modifier.animateContentSize(if (springy) fluidSize(0.36f) else tween(150))
    val iconTint = cs.onSurfaceVariant
    val canSave = title.isNotBlank() && (mode != Mode.WEEKDAYS || days != 0)
    val bellIcon: @Composable () -> Unit = { Icon(Icons.Default.Notifications, null, tint = iconTint, modifier = Modifier.size(22.dp)) }
    fun setAllDay(v: Boolean) {
        allDay = v
        if (!v && !endAt().isAfter(startAt())) {
            val ne = startAt().plusHours(1)
            endDate = ne.toLocalDate()
            endTime = ne.toLocalTime()
        }
        refreshAlerts()
    }

    val sheetDark = isDarkUi()
    CompositionLocalProvider(LocalCardColor provides if (sheetDark) cs.surfaceContainerHigh else cs.surface) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            properties = ModalBottomSheetProperties(shouldDismissOnBackPress = !editDesc),
            containerColor = if (sheetDark) cs.surface else cs.background,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                Box(
                    Modifier.padding(top = 10.dp, bottom = 2.dp).size(36.dp, 4.dp)
                        .clip(RShape(2.dp)).background(cs.onSurfaceVariant.copy(alpha = 0.35f))
                )
            }
        ) {
            val dens = LocalDensity.current
            val handlePx = with(dens) { 16.dp.roundToPx() }
            val statusTop = WindowInsets.statusBars.getTop(dens)
            val bottomInset = maxOf(WindowInsets.ime.getBottom(dens), WindowInsets.navigationBars.getBottom(dens))
            val maxH = with(dens) { (rootView.rootView.height - statusTop - handlePx - bottomInset).coerceAtLeast(0).toDp() - 8.dp }
            AnimatedContent(
                targetState = editDesc, label = "descPane",
                modifier = Modifier.onSizeChanged { sheetH = it.height + handlePx },
                transitionSpec = {
                    val dir = if (targetState) 1 else -1
                    if (springy) {
                        (slideInHorizontally(fluidOffset(0.4f)) { it * dir / 3 } + fadeIn(fluid(0.26f))) togetherWith
                            (slideOutHorizontally(fluidOffset(0.4f)) { -it * dir / 3 } + fadeOut(fluid(0.18f))) using
                            SizeTransform(clip = false) { _, _ -> fluidSize(0.42f) }
                    } else fadeIn(tween(120)) togetherWith fadeOut(tween(120))
                }
            ) { editing ->
            if (editing) {
                DescriptionPane(
                    desc, title, onDone = { desc = it; editDesc = false },
                    modifier = Modifier.fillMaxWidth().height(maxH)
                )
            } else {
                Column(
                    Modifier.fillMaxWidth().heightIn(max = maxH).verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp).padding(bottom = 16.dp)
                        .navigationBarsPadding().imePadding()
                ) {
                    var menuOpen by remember { mutableStateOf(false) }
                    Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                        STextButton(onClick = { closeThen(onDismiss) }) { Text(tx("Cancel")) }
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            SwapText(kindLabel(kind), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (!isNew && initial.isTask) {
                            Box {
                                SIconButton(onClick = { menuOpen = true }) { MoreIcon(cs.onSurface) }
                                DropdownMenu(
                                    expanded = menuOpen, onDismissRequest = { menuOpen = false },
                                    containerColor = elevatedColor(), shape = RoundedCornerShape(18.dp), shadowElevation = 3.dp,
                                border = menuBorder()
                                ) {
                                    if (overdue) {
                                        SheetMenuItem(tx("Move to today")) { menuOpen = false; closeThen { onSave(initial.copy(date = LocalDate.now())) } }
                                        SheetMenuItem(tx("Dismiss")) { menuOpen = false; closeThen { onDismissOverdue?.invoke(initial) } }
                                    }
                                    if (initial.repeating) {
                                        if (paused) SheetMenuItem(tx("Resume")) { menuOpen = false; closeThen { onResume?.invoke(initial) } }
                                        else {
                                            SheetMenuItem(tx("Skip today")) { menuOpen = false; closeThen { onSkipToday?.invoke(initial) } }
                                            SheetMenuItem(tx("Pause until…")) { menuOpen = false; pickPauseEnd() }
                                        }
                                    }
                                    SheetMenuItem(tx("Move to tomorrow")) { menuOpen = false; closeThen { onMoveTomorrow?.invoke(initial) } }
                                    if (initial.repeating) {
                                        SheetMenuItem(tx("Shift all +1 day")) { menuOpen = false; closeThen { onShiftAll?.invoke(initial) } }
                                        SheetMenuItem(tx("Stats")) { menuOpen = false; closeThen { onStats?.invoke(initial) } }
                                    }
                                }
                            }
                        }
                        STextButton(enabled = canSave, onClick = { val t = build(); closeThen { onSave(t) } }) {
                            Text(if (isNew) tx("Add") else tx("Save"))
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Card(grow) {
                        BasicTextField(
                            value = title, onValueChange = { title = it }, singleLine = true,
                            textStyle = TextStyle(color = cs.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Medium),
                            cursorBrush = SolidColor(cs.primary),
                            keyboardOptions = KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp),
                            decorationBox = { inner ->
                                Box {
                                    if (title.isEmpty()) {
                                        Text(tx("Add title"), fontSize = 22.sp, fontWeight = FontWeight.Medium, color = cs.onSurfaceVariant.copy(alpha = 0.7f))
                                    }
                                    inner()
                                }
                            }
                        )
                        if (isNew) KindChips(kind, { applyKind(it) }, Modifier.padding(start = 14.dp, end = 14.dp, bottom = 16.dp))
                    }

                    Spacer(Modifier.height(12.dp))
                    CardGroup(grow, inset = 56.dp) {
                        AnimatedVisibility(kind != Kind.BIRTHDAY, enter = rowIn, exit = rowOut) {
                            EditRow(icon = { ClockIcon(20.dp, iconTint) }, onClick = { setAllDay(!allDay) }) {
                                Text(tx("All day"), fontSize = 16.sp, modifier = Modifier.weight(1f))
                                SSwitch(allDay) { setAllDay(it) }
                            }
                        }
                        Column(Modifier.fillMaxWidth().padding(start = 50.dp, end = 10.dp, top = 6.dp, bottom = 6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PickText(fmtLong(date), { pickDate(date) { setStart(it, time) } }, Modifier.weight(1f, fill = false))
                                Spacer(Modifier.weight(1f))
                                AnimatedVisibility(!allDay, enter = chipIn, exit = chipOut) { PickText("$time", { pickTime(time) { setStart(date, it) } }) }
                            }
                            AnimatedVisibility(kind == Kind.EVENT, enter = rowIn, exit = rowOut) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    PickText(fmtLong(endDate), { pickDate(endDate, date) { setEnd(it, endTime) } }, Modifier.weight(1f, fill = false))
                                    Spacer(Modifier.weight(1f))
                                    AnimatedVisibility(!allDay, enter = chipIn, exit = chipOut) { PickText("$endTime", { pickTime(endTime) { setEnd(endDate, it) } }) }
                                }
                            }
                        }
                        AnimatedVisibility(kind == Kind.BIRTHDAY, enter = rowIn, exit = rowOut) {
                            EditRow(icon = { Icon(Icons.Default.Refresh, null, tint = iconTint, modifier = Modifier.size(22.dp)) }) {
                                Text(tx("Every year"), fontSize = 16.sp)
                            }
                        }
                        AnimatedVisibility(kind != Kind.BIRTHDAY, enter = rowIn, exit = rowOut) {
                        Column {
                            var repeatOpen by remember { mutableStateOf(false) }
                            Box {
                                EditRow(
                                    icon = { Icon(Icons.Default.Refresh, null, tint = iconTint, modifier = Modifier.size(22.dp)) },
                                    onClick = { if (mode != Mode.ONCE && !detailsVisible) detailsVisible = true else repeatOpen = true }
                                ) {
                                    Text(repeatText, fontSize = 16.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Chevron(down = true)
                                }
                                DropdownMenu(
                                    expanded = repeatOpen, onDismissRequest = { repeatOpen = false },
                                    containerColor = elevatedColor(), shape = RoundedCornerShape(18.dp), shadowElevation = 3.dp,
                                border = menuBorder()
                                ) {
                                    Mode.values().forEach { m ->
                                        DropdownMenuItem(
                                            text = { Text(modeNames.getValue(m), color = if (m == mode) cs.primary else cs.onSurface) },
                                            trailingIcon = if (m == mode) ({ Icon(Icons.Default.Check, null, tint = cs.primary, modifier = Modifier.size(18.dp)) }) else null,
                                            onClick = {
                                                mode = m
                                                if (m == Mode.MONTHLY || m == Mode.YEARLY) intervalText = "1"
                                                detailsVisible = true
                                                repeatOpen = false
                                            }
                                        )
                                    }
                                }
                            }
                            AnimatedVisibility(
                                visible = mode != Mode.ONCE && detailsVisible,
                                enter = fadeIn(fadeSpec) + expandVertically(),
                                exit = fadeOut(fadeSpec) + shrinkVertically()
                            ) {
                                Box(Modifier.padding(start = 56.dp, end = 16.dp, top = 12.dp, bottom = 14.dp)) {
                                    AnimatedContent(
                                        targetState = mode,
                                        transitionSpec = {
                                            (fadeIn(fadeSpec) + slideInVertically(slideSpec) { it / 4 }) togetherWith
                                                fadeOut(fadeSpec) using SizeTransform(clip = true) { _, _ -> sizeSpec }
                                        },
                                        label = "mode"
                                    ) { m ->
                                        when (m) {
                                            Mode.ONCE -> Spacer(Modifier.height(1.dp))
                                            Mode.WEEKDAYS -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                DayOfWeek.values().forEach { d ->
                                                    val bit = 1 shl (d.value - 1)
                                                    val on = (days and bit) != 0
                                                    val bg by animateColorAsState(if (on) cs.primary else cs.onSurface.copy(alpha = 0.06f), label = "wd")
                                                    val fg by animateColorAsState(if (on) cs.onPrimary else cs.onSurface, label = "wdt")
                                                    Box(
                                                        Modifier.size(36.dp).springClick(RShape(12.dp), 0.85f) { days = days xor bit }.background(bg),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(d.getDisplayName(JTextStyle.NARROW, Locale.getDefault()), color = fg, fontWeight = FontWeight.Medium)
                                                    }
                                                }
                                            }
                                            else -> Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                val unitWord = when (m) {
                                                    Mode.MONTHLY -> if (interval == 1) tx("month") else tx("months")
                                                    Mode.YEARLY -> if (interval == 1) tx("year") else tx("years")
                                                    else -> if (interval == 1) tx("day") else tx("days")
                                                }
                                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                                    SOutlinedButton(
                                                        onClick = { intervalText = (interval - 1).coerceAtLeast(1).toString() },
                                                        contentPadding = PaddingValues(0.dp), modifier = Modifier.size(40.dp)
                                                    ) { Text("−", fontSize = 20.sp) }
                                                    SwapText(
                                                        "${tx("Every")} $interval $unitWord",
                                                        Modifier.weight(1f).wrapContentWidth(), fontSize = 16.sp, fontWeight = FontWeight.Medium
                                                    )
                                                    SOutlinedButton(
                                                        onClick = { intervalText = (interval + 1).coerceAtMost(365).toString() },
                                                        contentPadding = PaddingValues(0.dp), modifier = Modifier.size(40.dp)
                                                    ) { Text("+", fontSize = 20.sp) }
                                                }
                                                val upcoming = (0..2).map { i ->
                                                    when (m) {
                                                        Mode.MONTHLY -> date.plusMonths((interval * i).toLong())
                                                        Mode.YEARLY -> date.plusYears((interval * i).toLong())
                                                        else -> date.plusDays((interval * i).toLong())
                                                    }
                                                }
                                                Text(
                                                    tx("Next") + ": " + upcoming.joinToString("  ·  ") { d ->
                                                        "${d.dayOfMonth} ${d.month.getDisplayName(JTextStyle.SHORT, Locale.getDefault())}" +
                                                            if (m == Mode.YEARLY || d.year != LocalDate.now().year) " ${d.year}" else ""
                                                    },
                                                    fontSize = 12.sp, color = cs.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    CardGroup(grow, inset = 56.dp) {
                        AnimatedVisibility(kind == Kind.TASK && mode == Mode.ONCE, enter = rowIn, exit = rowOut) {
                            EditRow(
                                icon = { LineIcon("target", 21.dp, iconTint) },
                                onClick = { pickDate(deadline ?: date.plusDays(1), date.plusDays(1)) { deadline = it } }
                            ) {
                                val dl = deadline
                                Text(
                                    if (dl == null) tx("Add deadline") else "${tx("Deadline")}: ${fmtLong(dl)}",
                                    fontSize = 16.sp, modifier = Modifier.weight(1f),
                                    color = if (dl == null) cs.onSurfaceVariant else cs.onSurface
                                )
                                if (dl != null) {
                                    SIconButton(onClick = { deadline = null }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Close, "Clear", Modifier.size(16.dp), tint = cs.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                        AnimatedVisibility(kind == Kind.EVENT, enter = rowIn, exit = rowOut) {
                            EditRow(icon = { Icon(Icons.Default.Place, null, tint = iconTint, modifier = Modifier.size(22.dp)) }) {
                                BasicTextField(
                                    value = location, onValueChange = { location = it }, singleLine = true,
                                    textStyle = TextStyle(color = cs.onSurface, fontSize = 16.sp),
                                    cursorBrush = SolidColor(cs.primary),
                                    modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                                    decorationBox = { inner ->
                                        Box {
                                            if (location.isEmpty()) Text(tx("Add location"), fontSize = 16.sp, color = cs.onSurfaceVariant)
                                            inner()
                                        }
                                    }
                                )
                            }
                        }
                        alerts.sorted().forEachIndexed { i, a ->
                            EditRow(icon = if (i == 0) bellIcon else null) {
                                Text(alertLabel(a, allDay), fontSize = 16.sp, modifier = Modifier.weight(1f))
                                SIconButton(onClick = { alertsTouched = true; alerts = alerts - a }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Close, "Remove", Modifier.size(16.dp), tint = cs.onSurfaceVariant)
                                }
                            }
                        }
                        var alertMenu by remember { mutableStateOf(false) }
                        Box {
                            EditRow(
                                icon = if (alerts.isEmpty()) bellIcon else null,
                                onClick = { alertMenu = true }
                            ) { Text(tx("Add notification"), fontSize = 16.sp, color = cs.primary, fontWeight = FontWeight.Medium) }
                            DropdownMenu(
                                expanded = alertMenu, onDismissRequest = { alertMenu = false },
                                containerColor = elevatedColor(), shape = RoundedCornerShape(18.dp), shadowElevation = 3.dp,
                                border = menuBorder()
                            ) {
                                val options = if (allDay) listOf(0, 1440, 10080) else listOf(0, 5, 10, 15, 30, 60, 1440)
                                options.filter { it !in alerts }.forEach { o ->
                                    DropdownMenuItem(
                                        text = { Text(alertLabel(o, allDay)) },
                                        onClick = { alertsTouched = true; alerts = alerts + o; alertMenu = false }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    CardGroup(grow, inset = 56.dp) {
                        var colorMenu by remember { mutableStateOf(false) }
                        Box {
                            EditRow(
                                icon = { Box(Modifier.padding(start = 2.dp).size(18.dp).clip(CircleShape).background(color?.let { Color(it) } ?: cs.primary)) },
                                onClick = { colorMenu = true }
                            ) {
                                Text(if (color == null) tx("Default color") else tx("Color"), fontSize = 16.sp, modifier = Modifier.weight(1f))
                                Chevron(down = true)
                            }
                            DropdownMenu(
                                expanded = colorMenu, onDismissRequest = { colorMenu = false },
                                containerColor = elevatedColor(), shape = RoundedCornerShape(18.dp), shadowElevation = 3.dp,
                                border = menuBorder()
                            ) {
                                val presets = TASK_COLORS.map { it.toArgb() }
                                (listOf<Color?>(null) + TASK_COLORS).chunked(5).forEachIndexed { index, rowColors ->
                                    Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        rowColors.forEach { c ->
                                            ColorDot(c, if (c == null) color == null else color == c.toArgb()) { color = c?.toArgb(); colorMenu = false }
                                        }
                                        if (index == 1) {
                                            CustomColorDot(color, color != null && color !in presets) { colorMenu = false; showPicker = true }
                                        }
                                    }
                                }
                            }
                        }
                        EditRow(icon = { LineIcon("text", 22.dp, iconTint) }) {
                            BasicTextField(
                                value = desc, onValueChange = { desc = it }, maxLines = 6,
                                textStyle = TextStyle(color = cs.onSurface, fontSize = 16.sp),
                                cursorBrush = SolidColor(cs.primary),
                                modifier = Modifier.weight(1f).padding(vertical = 4.dp).animateContentSize(),
                                decorationBox = { inner ->
                                    Box {
                                        if (desc.isEmpty()) Text(tx("Add description"), fontSize = 16.sp, color = cs.onSurfaceVariant)
                                        inner()
                                    }
                                }
                            )
                            SIconButton(onClick = { editDesc = true }, modifier = Modifier.size(32.dp)) { ExpandIcon(16.dp, cs.onSurfaceVariant) }
                        }
                    }

                    if (!isNew && onDelete != null) {
                        Spacer(Modifier.height(12.dp))
                        Card(onClick = { if (initial.repeating) askDelete = true else closeThen { onDelete(initial) } }) {
                            Text(
                                tx("Delete"), color = RED, fontSize = 16.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                            )
                        }
                    }
                }
            }
            }
        }
    }

    if (showPicker) {
        ColorPickerDialog(
            initial = color?.let { Color(it) },
            onDismiss = { showPicker = false },
            onPick = { color = it; showPicker = false }
        )
    }
    if (askDelete && onDelete != null) {
        CDialog(
            title = if (initial.isTask) tx("Delete repeating task?") else tx("Delete all occurrences?"),
            text = if (initial.isTask) "“${initial.title}” disappears from every day. Days you already finished stay in your export history."
            else "“${initial.title}”",
            confirm = tx("Delete"), destructive = true,
            onConfirm = { closeThen { onDelete(initial) } }, onDismiss = { askDelete = false }
        )
    }
}

@Composable
private fun SheetMenuItem(label: String, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(label) }, onClick = onClick)
}

@Composable
fun ExpandIcon(size: Dp, color: Color) {
    androidx.compose.foundation.Canvas(Modifier.size(size)) {
        val w = this.size.width
        val l = w * 0.32f
        val sw = w * 0.11f
        val cap = androidx.compose.ui.graphics.StrokeCap.Round
        val p = sw / 2
        drawLine(color, Offset(p, p), Offset(p + l, p), sw, cap); drawLine(color, Offset(p, p), Offset(p, p + l), sw, cap)
        drawLine(color, Offset(w - p, p), Offset(w - p - l, p), sw, cap); drawLine(color, Offset(w - p, p), Offset(w - p, p + l), sw, cap)
        drawLine(color, Offset(p, w - p), Offset(p + l, w - p), sw, cap); drawLine(color, Offset(p, w - p), Offset(p, w - p - l), sw, cap)
        drawLine(color, Offset(w - p, w - p), Offset(w - p - l, w - p), sw, cap); drawLine(color, Offset(w - p, w - p), Offset(w - p, w - p - l), sw, cap)
    }
}

@Composable
fun DescriptionPane(initial: String, title: String, onDone: (String) -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    var text by remember { mutableStateOf(TextFieldValue(initial, TextRange(initial.length))) }
    val focus = remember { FocusRequester() }
    val done by rememberUpdatedState(onDone)
    LaunchedEffect(Unit) { delay(220); runCatching { focus.requestFocus() } }
    BackHandler { done(text.text) }
    Column(modifier.padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
            SIconButton(onClick = { done(text.text) }) { BackIcon() }
            Column(Modifier.weight(1f).padding(horizontal = 6.dp)) {
                Text(tx("Description"), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                if (title.isNotBlank()) {
                    Text(title, fontSize = 13.sp, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Box(
                Modifier.springClick(RShape(18.dp), 0.94f) { done(text.text) }.background(cs.primary)
                    .padding(horizontal = 18.dp, vertical = 9.dp)
            ) { Text(tx("Done"), color = cs.onPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier.weight(1f).fillMaxWidth().padding(bottom = 12.dp).clip(CardShape).background(cardColor())
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            BasicTextField(
                value = text, onValueChange = { text = it },
                textStyle = TextStyle(color = cs.onSurface, fontSize = 17.sp, lineHeight = 26.sp),
                cursorBrush = SolidColor(cs.primary),
                keyboardOptions = KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxSize().focusRequester(focus),
                decorationBox = { inner ->
                    Box {
                        if (text.text.isEmpty()) Text(tx("Write anything…"), fontSize = 17.sp, color = cs.onSurfaceVariant.copy(alpha = 0.7f))
                        inner()
                    }
                }
            )
        }
    }
}

// ═══════════════ Onboarding ═══════════════

@Composable
fun Onboarding(
    tick: Int, onAskNotifications: () -> Unit, onAskExact: () -> Unit, onFinish: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    val block = remember { MutableInteractionSource() }
    var step by remember { mutableStateOf(0) }
    val springy = LocalSpringy.current
    val slide: FiniteAnimationSpec<IntOffset> =
        if (springy) fluidOffset(0.42f, 0.1f) else tween(250)

    Surface(
        Modifier.fillMaxSize().clickable(interactionSource = block, indication = null, onClick = {}),
        color = cs.background, contentColor = cs.onBackground
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(2) { i ->
                    val c by animateColorAsState(if (i <= step) cs.primary else cs.onSurface.copy(alpha = 0.1f), label = "dot$i")
                    Box(Modifier.weight(1f).height(4.dp).clip(RShape(2.dp)).background(c))
                }
            }
            AnimatedContent(
                targetState = step, modifier = Modifier.weight(1f).fillMaxWidth(), label = "onboardingStep",
                transitionSpec = {
                    (slideInHorizontally(slide) { it / 3 } + fadeIn(tween(200))) togetherWith
                        (slideOutHorizontally(slide) { -it / 3 } + fadeOut(tween(150)))
                }
            ) { st ->
                if (st == 0) {
                    val granted = remember(tick) {
                        Build.VERSION.SDK_INT < 33 ||
                            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    }
                    StepPage(
                        { Icon(Icons.Default.Notifications, null, tint = cs.primary, modifier = Modifier.size(30.dp)) },
                        tx("Notifications"), tx("Get a reminder at the time you set for each task, with Done and Snooze buttons."),
                        if (granted) tx("Continue") else tx("Allow notifications"), { if (granted) step = 1 else onAskNotifications() },
                        secondary = if (granted) null else tx("Not now"), onSecondary = { step = 1 }
                    ) { StatusLine(granted, tx("Allowed"), tx("Not allowed yet")) }
                } else {
                    val exact = remember(tick) { Reminders.canExact(ctx) }
                    StepPage(
                        { ClockIcon(28.dp, cs.primary) },
                        tx("Exact reminders"), tx("Without this, Android may delay reminders by several minutes."),
                        if (exact) tx("Start") else tx("Allow exact reminders"), { if (exact) onFinish() else onAskExact() },
                        secondary = if (exact) null else tx("Not now"), onSecondary = onFinish
                    ) { StatusLine(exact, tx("Allowed"), tx("Not allowed yet")) }
                }
            }
        }
    }
}

@Composable
private fun StepPage(
    icon: @Composable () -> Unit, title: String, text: String, primary: String, onPrimary: () -> Unit,
    modifier: Modifier = Modifier, secondary: String? = null, onSecondary: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Column(modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(36.dp))
        Box(Modifier.size(64.dp).clip(CircleShape).background(cs.primaryContainer), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.height(22.dp))
        Text(title, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(text, fontSize = 15.sp, lineHeight = 22.sp, color = cs.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Column(Modifier.weight(1f).fillMaxWidth(), content = content)
        SButton(onClick = onPrimary, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(primary, fontSize = 16.sp) }
        if (secondary != null) {
            STextButton(onClick = onSecondary, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text(secondary) }
        } else Spacer(Modifier.height(48.dp))
    }
}

@Composable
private fun StatusLine(ok: Boolean, yes: String, no: String) {
    val cs = MaterialTheme.colorScheme
    Card {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (ok) yes else no, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            val c by animateColorAsState(if (ok) GREEN else cs.onSurface.copy(alpha = 0.15f), label = "status")
            Box(Modifier.size(22.dp).clip(CircleShape).background(c), contentAlignment = Alignment.Center) {
                if (ok) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

// ═══════════════ RepeatingScreen ═══════════════

fun fmtDate(d: LocalDate) =
    "${d.dayOfWeek.getDisplayName(JTextStyle.SHORT, Locale.getDefault())}, " +
        "${d.dayOfMonth} ${d.month.getDisplayName(JTextStyle.SHORT, Locale.getDefault())}"

@Composable
fun RepeatingScreen(state: AppState, onClose: () -> Unit, onOpen: (Task) -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val today = LocalDate.now()
    val repeating = state.tasks.filter { it.repeating && it.isTask }
    val byInterval = repeating.filter { it.weekdays == 0 }
    val byWeekdays = repeating.filter { it.weekdays != 0 }

    LargeTitleScaffold(tx("Repeating tasks"), modifier, subtitle = tx("Tap one to fix its schedule"), onBack = onClose) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp).navigationBarsPadding().padding(bottom = 28.dp)
        ) {
            if (repeating.isEmpty()) {
                Text(tx("No repeating tasks yet."), color = cs.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp, top = 24.dp))
            }
            if (byInterval.isNotEmpty()) RepeatSection(tx("Every X days"), byInterval, state, today, onOpen)
            if (byWeekdays.isNotEmpty()) RepeatSection(tx("Weekdays"), byWeekdays, state, today, onOpen)
        }
    }
}

@Composable
fun RepeatSection(title: String, tasks: List<Task>, state: AppState, today: LocalDate, onOpen: (Task) -> Unit) {
    val cs = MaterialTheme.colorScheme
    GroupLabel(title)
    CardGroup {
        tasks.forEach { t ->
            val next = t.nextOccurrence(today)
            val nextText = when (next) {
                null -> null
                today -> "${tx("Next")}: ${tx("today")}"
                today.plusDays(1) -> "${tx("Next")}: ${tx("tomorrow")}"
                else -> "${tx("Next")}: ${fmtDate(next)}"
            }
            Row(
                Modifier.fillMaxWidth().springClick(RectangleShape, 0.99f) { onOpen(t) }.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.padding(end = 10.dp).size(10.dp).clip(CircleShape).background(t.color?.let { Color(it) } ?: cs.primary))
                        Text(t.title, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(t.repeatLabel(), fontSize = 14.sp)
                    val pr = state.taskStats(t, today)
                    Text(
                        if (pr.total > 0) "${pr.percent}% completion (${pr.done}/${pr.total} days)" else "No finished days counted yet",
                        fontSize = 12.sp, color = cs.onSurfaceVariant
                    )
                    Text(
                        listOfNotNull("Starts ${fmtDate(t.date)}", nextText, t.time?.toString()).joinToString("  ·  "),
                        fontSize = 12.sp, color = cs.onSurfaceVariant
                    )
                }
                Chevron()
            }
        }
    }
}
