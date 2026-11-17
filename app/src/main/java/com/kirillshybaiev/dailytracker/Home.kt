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

@Composable
fun TodayPage(
    state: AppState, onToggle: (Task, LocalDate) -> Unit, onOpen: (Task) -> Unit,
    onAdd: () -> Unit, onInsights: () -> Unit, onMenu: () -> Unit, springy: Boolean,
    onReorder: (String, String) -> Unit, onDismissAll: (List<Task>) -> Unit,
    coach: Int = 0
) {
    val cs = MaterialTheme.colorScheme
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); now = LocalDateTime.now() } }
    val today = now.toLocalDate()
    val tasks = state.tasksOn(today)
    val overdue = remember(state, today) { state.overdue(today) }
    var showOverdue by remember { mutableStateOf(false) }
    val doneCount = tasks.count { state.isDone(it, today) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
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
        val newOffset = if (target.offset > cur.offset) target.offset + target.size - cur.size else target.offset
        dragDy -= (newOffset - cur.offset)
    }

    fun finishDrag() {
        val start = dragDy
        scope.launch {
            Animatable(start).animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 500f)) { dragDy = value }
            draggingId = null
            dragDy = 0f
        }
    }

    val taskList: @Composable (Modifier) -> Unit = { mod ->
                if (tasks.isEmpty() && overdue.isEmpty()) {
                    Box(mod.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(tx("No tasks yet. Tap + to add one."), color = cs.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = mod,
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = bottomSpace(130.dp))
                    ) {
                        if (overdue.isNotEmpty()) {
                            item(key = "overdue_header") {
                                OverdueHeader(
                                    overdue.size, showOverdue, { showOverdue = !showOverdue },
                                    if (springy) Modifier.animateItem() else Modifier
                                )
                            }
                            if (showOverdue) {
                                items(overdue, key = { "od_" + it.id }) { t ->
                                    TaskRow(
                                        t, false, springy,
                                        onToggle = { onToggle(t, t.date) }, onOpen = { onOpen(t) },
                                        overdueText = "Was due ${fmtDate(t.date)}",
                                        modifier = if (springy) Modifier.animateItem() else Modifier
                                    )
                                }
                                item(key = "od_dismiss") {
                                    Row(
                                        (if (springy) Modifier.animateItem() else Modifier).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) { STextButton(onClick = { onDismissAll(overdue) }) { Text(tx("Dismiss all")) } }
                                }
                            }
                        }
                        if (tasks.isEmpty()) {
                            item(key = "nothing_today") {
                                Box(Modifier.fillMaxWidth().padding(vertical = 28.dp), contentAlignment = Alignment.Center) {
                                    Text(tx("Nothing planned for today."), color = cs.onSurfaceVariant)
                                }
                            }
                        }
                        items(tasks, key = { it.id }) { t ->
                            val dragging = draggingId == t.id
                            TaskRow(
                                t, state.isDone(t, today), springy,
                                onToggle = { onToggle(t, today) }, onOpen = { onOpen(t) },
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
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                    }
                }
    }
    val addButton: @Composable BoxScope.() -> Unit = {
            val fabSrc = remember { MutableInteractionSource() }
            var fabTurns by remember { mutableStateOf(0) }
            val fabRot by animateFloatAsState(
                fabTurns * 90f,
                animationSpec = if (springy) spring(dampingRatio = 0.45f, stiffness = 260f) else snap(), label = "fab"
            )
            FloatingActionButton(
                onClick = { fabTurns++; onAdd() },
                interactionSource = fabSrc, shape = RShape(20.dp),
                containerColor = cs.primary, contentColor = cs.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(20.dp)
                    .softShadow(RShape(20.dp), 14.dp).pressScale(fabSrc, 0.86f)
            ) { Icon(Icons.Default.Add, contentDescription = "Add task", modifier = Modifier.graphicsLayer { rotationZ = fabRot }) }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp)) {
                Box(Modifier.padding(start = 16.dp).coachRing(coach == 1, RShape(14.dp))) {
                    SIconButton(onClick = onMenu) { Icon(Icons.Default.Menu, contentDescription = "Menu") }
                }
                Column(
                    Modifier.fillMaxWidth()
                        .springClick(RShape(24.dp), 0.985f, onClick = onInsights)
                        .coachRing(coach == 2, RShape(24.dp))
                        .padding(horizontal = 28.dp).padding(top = 16.dp, bottom = 24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            tx("How it's going").uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Medium,
                            letterSpacing = 1.6.sp, color = cs.onSurfaceVariant, modifier = Modifier.weight(1f)
                        )
                        Text("›", fontSize = 22.sp, color = cs.onSurfaceVariant)
                    }
                    if (tasks.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
                                SwapText("$doneCount", fontSize = 64.sp, fontWeight = FontWeight.Light)
                                Text(
                                    "  ${tx("out of")} ${tasks.size}", fontSize = 20.sp, color = cs.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                            }
                            val left = tasks.size - doneCount
                            SwapText(
                                if (left <= 0) tx("All done") else "$left ${tx("left")}",
                                fontSize = 14.sp, color = if (left <= 0) GREEN else cs.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        DayBar(doneCount, tasks.size)
                    }
                }
            }
            SheetPanel(Modifier.weight(1f)) {
                Column(Modifier.fillMaxSize()) {
                    Spacer(Modifier.height(8.dp))
                    taskList(Modifier.weight(1f))
                }
                addButton()
            }
        }
    }
}

@Composable
fun DayBar(done: Int, total: Int, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val full = total > 0 && done >= total
    val gap = if (total > 24) 2.dp else 4.dp
    Row(modifier.fillMaxWidth().height(10.dp), horizontalArrangement = Arrangement.spacedBy(gap)) {
        repeat(total) { i ->
            val c by animateColorAsState(
                if (i < done) (if (full) GREEN else cs.onBackground) else cs.outline.copy(alpha = 0.35f),
                label = "segment"
            )
            Box(Modifier.weight(1f).fillMaxHeight().clip(RShape(4.dp)).background(c))
        }
    }
}

@Composable
fun OverdueHeader(count: Int, expanded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val rot by animateFloatAsState(if (expanded) 90f else 0f, spring(dampingRatio = 0.7f, stiffness = 400f), label = "overdueArrow")
    Row(
        modifier.fillMaxWidth().springClick(RShape(14.dp), 0.98f, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(RED))
        Spacer(Modifier.width(10.dp))
        Text("$count overdue", color = RED, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Text("›", fontSize = 22.sp, color = cs.onSurfaceVariant, modifier = Modifier.graphicsLayer { rotationZ = rot })
    }
}

@Composable
fun TaskRow(
    task: Task, done: Boolean, springy: Boolean, onToggle: () -> Unit, onOpen: () -> Unit,
    skipped: Boolean = false, overdueText: String? = null, toggleEnabled: Boolean = true, modifier: Modifier = Modifier
) = MinimalTaskRow(task, done, springy, onToggle, onOpen, skipped, overdueText, toggleEnabled, modifier)

@Composable
fun MinimalTaskRow(
    task: Task, done: Boolean, springy: Boolean, onToggle: () -> Unit, onOpen: () -> Unit,
    skipped: Boolean, overdueText: String?, toggleEnabled: Boolean, modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val accent = task.color?.let { Color(it) }
    val ringColor by animateColorAsState(
        when { done -> accent ?: cs.onSurface; accent != null -> accent; else -> cs.onSurfaceVariant.copy(alpha = 0.6f) },
        label = "ring"
    )
    val fill by animateColorAsState(if (done) (accent ?: cs.onSurface) else (accent ?: cs.onSurface).copy(alpha = 0f), label = "fill")
    val card by animateColorAsState(cs.surfaceVariant.copy(alpha = if (done || skipped) 0.45f else 1f), label = "card")
    val titleColor by animateColorAsState(if (done || skipped) cs.onSurfaceVariant else cs.onSurface, label = "title")
    val pop by animateFloatAsState(
        if (done) 1f else 0f,
        animationSpec = if (springy) spring(dampingRatio = 0.45f, stiffness = 500f) else tween(150), label = "pop"
    )
    val bump = remember { Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(done) {
        if (first) first = false
        else if (springy) {
            bump.snapTo(0.78f)
            bump.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 450f))
        }
    }
    val second = overdueText ?: task.description.takeIf { it.isNotBlank() } ?: if (skipped) tx("Skipped") else null
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier.fillMaxWidth().padding(vertical = 4.dp)
            .springClick(shape, 0.98f, onClick = onOpen)
            .background(card)
            .padding(start = 6.dp, end = 18.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(52.dp).springClick(CircleShape, 0.85f, enabled = toggleEnabled && !skipped, onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.size(24.dp).graphicsLayer { scaleX = bump.value; scaleY = bump.value }
                    .clip(CircleShape).background(fill).border(2.dp, ringColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check, null,
                    modifier = Modifier.size(14.dp).graphicsLayer {
                        scaleX = pop.coerceAtLeast(0f); scaleY = pop.coerceAtLeast(0f); alpha = pop.coerceIn(0f, 1f)
                    },
                    tint = cs.surface
                )
            }
        }
        Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
            Text(
                task.title, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                textDecoration = if (done) TextDecoration.LineThrough else null, color = titleColor
            )
            if (second != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    second, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = if (overdueText != null) RED else cs.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun Stat(label: String, value: String, sub: String, fraction: Float, modifier: Modifier, valueSize: TextUnit = 20.sp) {
    val cs = MaterialTheme.colorScheme
    val springy = LocalSpringy.current
    val f = remember { Animatable(0f) }
    LaunchedEffect(fraction) {
        f.animateTo(fraction.coerceIn(0f, 1f), if (springy) spring(dampingRatio = 0.8f, stiffness = 120f) else snap())
    }
    Column(modifier) {
        Text(label, fontSize = 12.sp, color = cs.onSurfaceVariant)
        SwapText(value, fontSize = valueSize, fontWeight = FontWeight.SemiBold)
        SwapText(sub, fontSize = 11.sp, color = cs.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(50)).background(cs.outline.copy(alpha = 0.45f))) {
            Box(Modifier.fillMaxWidth(f.value.coerceIn(0f, 1f)).fillMaxHeight().background(cs.onBackground))
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

@Composable
fun CalendarPage(
    state: AppState, onClose: () -> Unit, onOpen: (Task) -> Unit,
    onToggle: (Task, LocalDate) -> Unit, onAddOn: (LocalDate) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val springy = LocalSpringy.current
    val today = rememberToday()
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var selected by remember { mutableStateOf(today) }
    LaunchedEffect(today) { month = YearMonth.from(today); selected = today }
    val holidays = remember(selected.year, state.showHolidays, state.country) {
        if (state.showHolidays) Holidays.forYear(state.holidayCountry(), selected.year) else emptyMap<LocalDate, String>()
    }

    val slideSpec: FiniteAnimationSpec<IntOffset> =
        if (springy) spring(dampingRatio = 0.85f, stiffness = 380f, visibilityThreshold = IntOffset.VisibilityThreshold) else snap()
    val fadeSpec: FiniteAnimationSpec<Float> = if (springy) spring(stiffness = 500f) else tween(120)
    val sizeSpec: FiniteAnimationSpec<IntSize> =
        if (springy) spring(dampingRatio = 0.85f, stiffness = 380f, visibilityThreshold = IntSize.VisibilityThreshold) else snap()
    val monthTransition: AnimatedContentTransitionScope<YearMonth>.() -> ContentTransform = {
        val dir = if (targetState > initialState) 1 else -1
        (slideInHorizontally(slideSpec) { it * dir } + fadeIn(fadeSpec)) togetherWith
            (slideOutHorizontally(slideSpec) { -it * dir } + fadeOut(fadeSpec)) using SizeTransform(clip = true) { _, _ -> sizeSpec }
    }

    val swipeMonth = Modifier.pointerInput(Unit) {
        var total = 0f
        var fired = false
        detectHorizontalDragGestures(
            onDragStart = { total = 0f; fired = false },
            onHorizontalDrag = { change, dx ->
                change.consume()
                total += dx
                if (!fired && abs(total) > 56.dp.toPx()) {
                    fired = true
                    month = if (total < 0) month.plusMonths(1) else month.minusMonths(1)
                }
            }
        )
    }

    val density = LocalDensity.current
    val statusTop = WindowInsets.statusBars.getTop(density)
    val scope = rememberCoroutineScope()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val totalPx = constraints.maxHeight.toFloat()
        var gridPx by remember { mutableStateOf(0) }
        val keepPx = statusTop + with(density) { 64.dp.toPx() }
        val maxExtra = (gridPx - keepPx).coerceAtLeast(0f)
        val extra = remember { Animatable(0f) }
        val collapsedPx = if (gridPx == 0) totalPx * 0.55f else (totalPx - gridPx).coerceAtLeast(0f)
        val panelPx = (collapsedPx + extra.value.coerceIn(0f, maxExtra)).coerceAtMost(totalPx)
        val dragPanel = Modifier.pointerInput(maxExtra) {
            var last = 0f
            detectVerticalDragGestures(
                onDragStart = { last = 0f },
                onVerticalDrag = { change, dy ->
                    change.consume()
                    last = dy
                    scope.launch { extra.snapTo((extra.value - dy).coerceIn(0f, maxExtra)) }
                },
                onDragEnd = {
                    scope.launch {
                        val target = when { last < -1f -> maxExtra; last > 1f -> 0f; extra.value > maxExtra / 2f -> maxExtra; else -> 0f }
                        extra.animateTo(target, spring(0.8f, 400f))
                    }
                },
                onDragCancel = { scope.launch { extra.animateTo(0f, spring(0.8f, 400f)) } }
            )
        }
        Column(Modifier.align(Alignment.TopCenter).onSizeChanged { gridPx = it.height }.statusBarsPadding().then(swipeMonth).padding(horizontal = 14.dp).padding(top = 8.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(48.dp))
                Box(
                    Modifier.weight(1f).padding(vertical = 8.dp).springClick(RShape(12.dp), 0.96f) {
                        month = YearMonth.from(today); selected = today
                    },
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(targetState = month, transitionSpec = monthTransition, label = "title") { m ->
                        Text(
                            "${m.month.getDisplayName(JTextStyle.FULL, Locale.getDefault())} ${m.year}",
                            fontWeight = FontWeight.Bold, fontSize = 20.sp
                        )
                    }
                }
                SIconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Close") }
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
            AnimatedContent(targetState = month, transitionSpec = monthTransition, label = "grid") { m ->
                MonthGrid(state, m, today, selected) { selected = it }
            }
            Spacer(Modifier.height(8.dp))
        }

        SheetPanel(Modifier.align(Alignment.BottomCenter).height(with(density) { panelPx.toDp() })) {
            Column(Modifier.fillMaxSize()) {
                Box(
                    Modifier.fillMaxWidth().then(dragPanel).springClick(RShape(12.dp), 1f) {
                        scope.launch { extra.animateTo(if (extra.value > maxExtra / 2f) 0f else maxExtra, spring(0.8f, 400f)) }
                    }.padding(top = 10.dp, bottom = 2.dp),
                    contentAlignment = Alignment.Center
                ) { Box(Modifier.size(40.dp, 4.dp).clip(RShape(2.dp)).background(cs.outline)) }
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
                DayList(state, selected, today, onOpen, onToggle, onAddOn, Modifier.weight(1f).fillMaxWidth(), holidays[selected])
            }
        }
    }
}

@Composable
fun DayList(
    state: AppState, date: LocalDate, today: LocalDate, onOpen: (Task) -> Unit,
    onToggle: (Task, LocalDate) -> Unit, onAddOn: (LocalDate) -> Unit, modifier: Modifier, holiday: String? = null
) {
    val cs = MaterialTheme.colorScheme
    val springy = LocalSpringy.current
    val tasks = state.tasksOn(date, includeSkipped = true)
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
        if (tasks.isEmpty()) {
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
        item(key = "add") {
            Box(Modifier.fillMaxWidth().padding(top = 14.dp), contentAlignment = Alignment.Center) {
                STextButton(onClick = { onAddOn(date) }) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(tx("Add task on this day"))
                }
            }
        }
    }
}

@Composable
fun MonthGrid(state: AppState, m: YearMonth, today: LocalDate, selected: LocalDate, onSelect: (LocalDate) -> Unit) {
    val holidays = remember(m.year, state.showHolidays, state.country) {
        if (state.showHolidays) Holidays.forYear(state.holidayCountry(), m.year) else emptyMap<LocalDate, String>()
    }
    val first = m.atDay(1).dayOfWeek.value
    val offset = (first - state.weekStartDay + 7) % 7
    val len = m.lengthOfMonth()
    Column {
        for (r in 0 until (offset + len + 6) / 7) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0..6) {
                    val day = r * 7 + c - offset + 1
                    if (day in 1..len) {
                        val date = m.atDay(day)
                        val dayTasks = state.tasksOn(date)
                        val doneN = dayTasks.count { state.isDone(it, date) }
                        val status = when {
                            dayTasks.isEmpty() -> 0
                            doneN == dayTasks.size -> 1
                            date.isAfter(today) -> 0
                            doneN > 0 -> 3
                            date.isBefore(today) -> 2
                            else -> 0
                        }
                        DayCell(date, date == today, date == selected, date in holidays, status,
                            { onSelect(date) }, Modifier.weight(1f))
                    } else Spacer(Modifier.weight(1f).aspectRatio(1.15f))
                }
            }
        }
    }
}

@Composable
fun DayCell(
    date: LocalDate, isToday: Boolean, selected: Boolean, holiday: Boolean,
    status: Int, onClick: () -> Unit, modifier: Modifier
) {
    val cs = MaterialTheme.colorScheme
    val springy = LocalSpringy.current
    val (bgTarget, fgTarget) = when {
        selected -> cs.onBackground to cs.background
        status == 1 -> GREEN.copy(alpha = 0.16f) to GREEN
        status == 2 -> RED.copy(alpha = 0.13f) to RED
        status == 3 -> cs.onBackground.copy(alpha = 0.08f) to cs.onBackground
        else -> cs.onBackground.copy(alpha = 0f) to (if (date.dayOfWeek == DayOfWeek.SUNDAY) RED.copy(alpha = 0.85f) else cs.onBackground)
    }
    val bg by animateColorAsState(bgTarget, label = "dayBg")
    val fg by animateColorAsState(fgTarget, label = "dayFg")
    val ring by animateFloatAsState(if (isToday && !selected) 0.55f else 0f, tween(200), label = "dayRing")
    val bump = remember { Animatable(1f) }
    LaunchedEffect(selected) {
        if (selected && springy) {
            bump.snapTo(0.82f)
            bump.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 450f))
        }
    }
    Box(modifier.aspectRatio(1.15f), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(40.dp)
                .graphicsLayer { scaleX = bump.value; scaleY = bump.value }
                .springClick(CircleShape, 0.88f, onClick = onClick)
                .background(bg)
                .border(1.5.dp, cs.onBackground.copy(alpha = ring), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "${date.dayOfMonth}", fontSize = 15.sp, color = fg,
                fontWeight = if (isToday || selected) FontWeight.SemiBold else FontWeight.Normal,
                textDecoration = if (holiday) TextDecoration.Underline else null
            )
        }
    }
}

@Composable
fun CalendarScreen(
    state: AppState, onClose: () -> Unit, onOpen: (Task) -> Unit,
    onToggle: (Task, LocalDate) -> Unit, onAddOn: (LocalDate) -> Unit, modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val block = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier.fillMaxSize().clickable(interactionSource = block, indication = null, onClick = {}),
        color = cs.background, contentColor = cs.onBackground
    ) { CalendarPage(state, onClose, onOpen, onToggle, onAddOn) }
}

// ═══════════════ TaskSheet ═══════════════

enum class Mode { ONCE, INTERVAL, WEEKDAYS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskSheet(
    initial: Task?, defaultDate: LocalDate, overdue: Boolean, paused: Boolean,
    onDismiss: () -> Unit, onSave: (Task) -> Unit, onDelete: ((Task) -> Unit)?,
    onStats: ((Task) -> Unit)? = null, onSkipToday: ((Task) -> Unit)? = null,
    onPauseUntil: ((Task, LocalDate) -> Unit)? = null, onResume: ((Task) -> Unit)? = null,
    onDismissOverdue: ((Task) -> Unit)? = null,
    onMoveTomorrow: ((Task) -> Unit)? = null, onShiftAll: ((Task) -> Unit)? = null
) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val springy = LocalSpringy.current
    val isNew = initial == null
    val startMode = if (initial == null) Mode.ONCE
        else if (!initial.repeating) Mode.ONCE
        else if (initial.weekdays != 0) Mode.WEEKDAYS
        else Mode.INTERVAL
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var desc by remember { mutableStateOf(initial?.description ?: "") }
    var editDesc by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(startMode) }
    var detailsVisible by remember { mutableStateOf(isNew) }
    var intervalText by remember { mutableStateOf((initial?.interval ?: 1).toString()) }
    val interval = intervalText.toIntOrNull()?.coerceIn(1, 365) ?: 1
    var days by remember { mutableStateOf(initial?.weekdays?.takeIf { it != 0 } ?: 21) }
    var date by remember { mutableStateOf(initial?.date ?: defaultDate) }
    var time by remember { mutableStateOf(initial?.time) }
    var color by remember { mutableStateOf(initial?.color) }
    var showPicker by remember { mutableStateOf(false) }
    var askDelete by remember { mutableStateOf(false) }
    val modeNames = mapOf(Mode.ONCE to tx("One time"), Mode.INTERVAL to tx("Every X days"), Mode.WEEKDAYS to tx("Weekdays"))
    val repeatLabel = when (mode) {
        Mode.ONCE -> tx("One time")
        Mode.INTERVAL -> intervalLabel(interval)
        Mode.WEEKDAYS -> when (days) {
            0 -> tx("Pick days")
            127 -> tx("Every day")
            else -> DayOfWeek.values().filter { ((days shr (it.value - 1)) and 1) == 1 }
                .joinToString(", ") { it.getDisplayName(JTextStyle.SHORT, Locale.getDefault()) }
        }
    }

    fun pickPauseEnd() {
        val tomorrow = LocalDate.now().plusDays(1)
        val dlg = android.app.DatePickerDialog(
            ctx, { _, y, mo, d -> initial?.let { onPauseUntil?.invoke(it, LocalDate.of(y, mo + 1, d)) } },
            tomorrow.year, tomorrow.monthValue - 1, tomorrow.dayOfMonth
        )
        dlg.datePicker.minDate = tomorrow.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        dlg.show()
    }

    val slideSpec: FiniteAnimationSpec<IntOffset> =
        if (springy) spring(dampingRatio = 0.85f, stiffness = 380f, visibilityThreshold = IntOffset.VisibilityThreshold) else snap()
    val fadeSpec: FiniteAnimationSpec<Float> = if (springy) spring(stiffness = 500f) else tween(120)
    val sizeSpec: FiniteAnimationSpec<IntSize> =
        if (springy) spring(dampingRatio = 0.85f, stiffness = 380f, visibilityThreshold = IntSize.VisibilityThreshold) else snap()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = cs.surface
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp).padding(bottom = 16.dp)
                .navigationBarsPadding().imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            var menuOpen by remember { mutableStateOf(false) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it }, label = { Text(tx("Title")) },
                    singleLine = true, modifier = Modifier.weight(1f)
                )
                if (initial != null) {
                    Box {
                        SIconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, contentDescription = "More") }
                        DropdownMenu(
                            expanded = menuOpen, onDismissRequest = { menuOpen = false },
                            containerColor = cs.surface, shape = RoundedCornerShape(18.dp)
                        ) {
                            if (overdue) {
                                SheetMenuItem(tx("Move to today")) { menuOpen = false; onSave(initial.copy(date = LocalDate.now())) }
                                SheetMenuItem(tx("Dismiss")) { menuOpen = false; onDismissOverdue?.invoke(initial) }
                            }
                            if (initial.repeating) {
                                if (paused) SheetMenuItem(tx("Resume")) { menuOpen = false; onResume?.invoke(initial) }
                                else {
                                    SheetMenuItem(tx("Skip today")) { menuOpen = false; onSkipToday?.invoke(initial) }
                                    SheetMenuItem(tx("Pause until…")) { menuOpen = false; pickPauseEnd() }
                                }
                            }
                            SheetMenuItem(tx("Move to tomorrow")) { menuOpen = false; onMoveTomorrow?.invoke(initial) }
                            if (initial.repeating) {
                                SheetMenuItem(tx("Shift all +1 day")) { menuOpen = false; onShiftAll?.invoke(initial) }
                                SheetMenuItem(tx("Stats")) { menuOpen = false; onStats?.invoke(initial) }
                            }
                        }
                    }
                }
            }
            OutlinedTextField(
                value = desc, onValueChange = { desc = it }, label = { Text(tx("Description (optional)")) },
                minLines = 1, maxLines = 6,
                trailingIcon = {
                    SIconButton(onClick = { editDesc = true }) { ExpandIcon(18.dp, cs.onSurfaceVariant) }
                },
                modifier = Modifier.fillMaxWidth().animateContentSize()
            )

            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MenuButton(
                        repeatLabel, modifier = Modifier.weight(1f), fill = true,
                        onBeforeOpen = {
                            if (mode != Mode.ONCE && !detailsVisible) { detailsVisible = true; false } else true
                        }
                    ) { close ->
                        Mode.values().forEach { m ->
                            DropdownMenuItem(
                                text = { Text(modeNames.getValue(m)) },
                                onClick = { mode = m; detailsVisible = true; close() }
                            )
                        }
                    }
                    SOutlinedButton(
                        onClick = {
                            android.app.DatePickerDialog(
                                ctx, { _, y, mo, d -> date = LocalDate.of(y, mo + 1, d) },
                                date.year, date.monthValue - 1, date.dayOfMonth
                            ).show()
                        },
                        modifier = Modifier.animateContentSize()
                    ) { Text(if (mode == Mode.ONCE) "Date: $date" else "Starts: $date") }
                }
                AnimatedVisibility(
                    visible = mode != Mode.ONCE && detailsVisible,
                    enter = fadeIn(fadeSpec) + expandVertically(),
                    exit = fadeOut(fadeSpec) + shrinkVertically()
                ) {
                    Box(Modifier.padding(top = 12.dp)) {
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
                                Mode.INTERVAL -> Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val csI = MaterialTheme.colorScheme
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        SOutlinedButton(
                                            onClick = { intervalText = (interval - 1).coerceAtLeast(1).toString() },
                                            contentPadding = PaddingValues(0.dp), modifier = Modifier.size(44.dp)
                                        ) { Text("−", fontSize = 20.sp) }
                                        Text(
                                            "${tx("Every")} $interval ${if (interval == 1) tx("day") else tx("days")}",
                                            Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 16.sp, fontWeight = FontWeight.Medium
                                        )
                                        SOutlinedButton(
                                            onClick = { intervalText = (interval + 1).coerceAtMost(365).toString() },
                                            contentPadding = PaddingValues(0.dp), modifier = Modifier.size(44.dp)
                                        ) { Text("+", fontSize = 20.sp) }
                                    }
                                    val upcoming = (0..2).map { date.plusDays((interval * it).toLong()) }
                                    Text(
                                        tx("Next") + ": " + upcoming.joinToString("  ·  ") { d ->
                                            "${d.dayOfWeek.getDisplayName(JTextStyle.SHORT, Locale.getDefault())} ${d.dayOfMonth} " +
                                                d.month.getDisplayName(JTextStyle.SHORT, Locale.getDefault())
                                        },
                                        fontSize = 12.sp, color = csI.onSurfaceVariant
                                    )
                                }
                                Mode.WEEKDAYS -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    DayOfWeek.values().forEach { d ->
                                        val bit = 1 shl (d.value - 1)
                                        val on = (days and bit) != 0
                                        val bg by animateColorAsState(if (on) cs.primary else cs.surfaceVariant, label = "wd")
                                        val fg by animateColorAsState(if (on) cs.onPrimary else cs.onSurface, label = "wdt")
                                        Box(
                                            Modifier.size(38.dp).springClick(RShape(12.dp), 0.85f) { days = days xor bit }.background(bg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(d.getDisplayName(JTextStyle.NARROW, Locale.getDefault()), color = fg, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SOutlinedButton(
                    onClick = {
                        android.app.TimePickerDialog(
                            ctx, { _, h, mi -> time = LocalTime.of(h, mi) },
                            time?.hour ?: 9, time?.minute ?: 0, true
                        ).show()
                    },
                    modifier = Modifier.animateContentSize()
                ) {
                    if (time != null) {
                        ClockIcon(16.dp, LocalContentColor.current)
                        Spacer(Modifier.width(8.dp))
                        Text("$time")
                    } else Text(tx("Reminder"))
                }
                AnimatedVisibility(
                    visible = time != null,
                    enter = fadeIn(fadeSpec) + expandHorizontally(),
                    exit = fadeOut(fadeSpec) + shrinkHorizontally()
                ) { STextButton(onClick = { time = null }) { Text(tx("Clear")) } }
                MenuButton(
                    tx("Color"),
                    leading = { Box(Modifier.size(14.dp).clip(CircleShape).background(color?.let { Color(it) } ?: cs.outline)) }
                ) { close ->
                    val presets = TASK_COLORS.map { it.toArgb() }
                    (listOf<Color?>(null) + TASK_COLORS).chunked(5).forEachIndexed { index, rowColors ->
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            rowColors.forEach { c ->
                                ColorDot(c, if (c == null) color == null else color == c.toArgb()) { color = c?.toArgb(); close() }
                            }
                            if (index == 1) {
                                CustomColorDot(color, color != null && color !in presets) { close(); showPicker = true }
                            }
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (initial != null && onDelete != null) {
                    STextButton(onClick = { if (initial.repeating) askDelete = true else onDelete(initial) }) {
                        Text(tx("Delete"), color = RED)
                    }
                }
                Spacer(Modifier.weight(1f))
                SButton(
                    enabled = title.isNotBlank() && (mode != Mode.WEEKDAYS || days != 0),
                    onClick = {
                        onSave(
                            Task(
                                id = initial?.id ?: UUID.randomUUID().toString(),
                                title = title.trim(), description = desc.trim(),
                                repeating = mode != Mode.ONCE, date = date, time = time,
                                color = color,
                                interval = if (mode == Mode.INTERVAL) interval else 1,
                                weekdays = if (mode == Mode.WEEKDAYS) days else 0
                            )
                        )
                    }
                ) { Text(if (isNew) tx("Add") else tx("Save")) }
            }
        }
    }

    if (editDesc) {
        DescriptionEditor(desc, onDone = { desc = it; editDesc = false })
    }
    if (showPicker) {
        ColorPickerDialog(
            initial = color?.let { Color(it) },
            onDismiss = { showPicker = false },
            onPick = { color = it; showPicker = false }
        )
    }
    if (askDelete && initial != null && onDelete != null) {
        AlertDialog(
            containerColor = cs.surface,
            onDismissRequest = { askDelete = false },
            title = { Text(tx("Delete repeating task?")) },
            text = { Text("“${initial.title}” disappears from every day. Days you already finished stay in your export history.") },
            confirmButton = { STextButton(onClick = { onDelete(initial) }) { Text(tx("Delete"), color = RED) } },
            dismissButton = { STextButton(onClick = { askDelete = false }) { Text(tx("Cancel")) } }
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
fun DescriptionEditor(initial: String, onDone: (String) -> Unit) {
    val cs = MaterialTheme.colorScheme
    var text by remember { mutableStateOf(TextFieldValue(initial, TextRange(initial.length))) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = { onDone(text.text) },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(Modifier.fillMaxSize(), color = cs.background, contentColor = cs.onBackground) {
            Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(tx("Description"), fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    STextButton(onClick = { onDone(text.text) }) { Text(tx("Done"), fontWeight = FontWeight.SemiBold) }
                }
                BasicTextField(
                    value = text, onValueChange = { text = it },
                    textStyle = TextStyle(color = cs.onBackground, fontSize = 17.sp, lineHeight = 26.sp),
                    cursorBrush = SolidColor(cs.onBackground),
                    modifier = Modifier.fillMaxSize().focusRequester(focus).padding(horizontal = 24.dp, vertical = 12.dp),
                    decorationBox = { inner ->
                        Box {
                            if (text.text.isEmpty()) Text(tx("Write anything…"), fontSize = 17.sp, color = cs.onSurfaceVariant)
                            inner()
                        }
                    }
                )
            }
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
        if (springy) spring(dampingRatio = 0.85f, stiffness = 380f, visibilityThreshold = IntOffset.VisibilityThreshold) else tween(250)

    Surface(
        Modifier.fillMaxSize().clickable(interactionSource = block, indication = null, onClick = {}),
        color = cs.background, contentColor = cs.onBackground
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(2) { i ->
                    val c by animateColorAsState(if (i <= step) cs.onBackground else cs.outline, label = "dot$i")
                    Box(Modifier.weight(1f).height(5.dp).clip(RShape(3.dp)).background(c))
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
                        tx("Notifications"), tx("Get a reminder at the time you set for each task, with Done and Snooze buttons."),
                        if (granted) tx("Continue") else tx("Allow notifications"), { if (granted) step = 1 else onAskNotifications() },
                        secondary = if (granted) null else tx("Not now"), onSecondary = { step = 1 }
                    ) { StatusLine(granted, tx("Allowed"), tx("Not allowed yet")) }
                } else {
                    val exact = remember(tick) { Reminders.canExact(ctx) }
                    StepPage(
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
    title: String, text: String, primary: String, onPrimary: () -> Unit,
    modifier: Modifier = Modifier, secondary: String? = null, onSecondary: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Column(modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(10.dp))
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(text, fontSize = 15.sp, color = cs.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))
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
    val shape = RShape(14.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(cs.surfaceVariant).border(1.dp, cs.outline, shape).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(10.dp).clip(RShape(3.dp)).background(if (ok) GREEN else cs.onSurfaceVariant))
        Spacer(Modifier.width(12.dp))
        Text(if (ok) yes else no, fontWeight = FontWeight.Medium)
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
    val block = remember { MutableInteractionSource() }
    val repeating = state.tasks.filter { it.repeating }
    val byInterval = repeating.filter { it.weekdays == 0 }
    val byWeekdays = repeating.filter { it.weekdays != 0 }

    Surface(
        modifier = modifier.fillMaxSize().clickable(interactionSource = block, indication = null, onClick = {}),
        color = cs.background, contentColor = cs.onBackground
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.padding(start = 24.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tx("Repeating tasks"), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text(tx("Tap one to fix its schedule"), fontSize = 13.sp, color = cs.onSurfaceVariant)
                }
                SIconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Close") }
            }
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp).padding(top = 6.dp)
                    .navigationBarsPadding().padding(bottom = 28.dp)
            ) {
                if (repeating.isEmpty()) {
                    Text(tx("No repeating tasks yet."), color = cs.onSurfaceVariant, modifier = Modifier.padding(top = 24.dp))
                }
                if (byInterval.isNotEmpty()) RepeatSection(tx("Every X days"), byInterval, state, today, onOpen)
                if (byWeekdays.isNotEmpty()) RepeatSection(tx("Weekdays"), byWeekdays, state, today, onOpen)
            }
        }
    }
}

@Composable
fun RepeatSection(title: String, tasks: List<Task>, state: AppState, today: LocalDate, onOpen: (Task) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Text(
        title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = cs.onSurfaceVariant,
        modifier = Modifier.padding(top = 18.dp, bottom = 4.dp)
    )
    tasks.forEach { t ->
        val next = t.nextOccurrence(today)
        val nextText = when (next) {
            null -> null
            today -> "${tx("Next")}: ${tx("today")}"
            today.plusDays(1) -> "${tx("Next")}: ${tx("tomorrow")}"
            else -> "${tx("Next")}: ${fmtDate(next)}"
        }
        Column(Modifier.fillMaxWidth().springClick(pressedScale = 0.98f) { onOpen(t) }.padding(vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                t.color?.let { Box(Modifier.padding(end = 8.dp).size(10.dp).clip(CircleShape).background(Color(it))) }
                Text(t.title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
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
        HorizontalDivider(color = cs.outline.copy(alpha = 0.3f))
    }
}
