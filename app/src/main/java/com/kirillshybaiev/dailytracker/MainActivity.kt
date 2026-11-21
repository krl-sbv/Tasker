package com.kirillshybaiev.dailytracker

import android.Manifest
import android.app.Activity
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
import androidx.activity.compose.BackHandler
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.ui.graphics.TransformOrigin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned

// ═══════════════ MainActivity ═══════════════

class MainActivity : ComponentActivity() {
    private val vm: TrackerViewModel by viewModels()
    private val resumes = MutableStateFlow(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        preferHighRefreshRate()
        setContent { App(vm, resumes) }
    }

    override fun onResume() {
        super.onResume()
        resumes.value += 1
    }

    private fun preferHighRefreshRate() {
        val d = (if (Build.VERSION.SDK_INT >= 30) display else @Suppress("DEPRECATION") windowManager.defaultDisplay) ?: return
        val cur = d.mode
        val best = d.supportedModes
            .filter { it.physicalWidth == cur.physicalWidth && it.physicalHeight == cur.physicalHeight }
            .maxByOrNull { it.refreshRate } ?: return
        window.attributes = window.attributes.apply { preferredDisplayModeId = best.modeId }
    }
}

// ═══════════════ Navigation ═══════════════

private val ROOTS = setOf(0, 7, 8)
private val SETTINGS_CHILDREN = setOf(2, 4, 5)
private val TODAY_CHILDREN = setOf(1, 3)

private class DrawerCtl(private val scope: CoroutineScope) {
    var p by mutableFloatStateOf(0f)
    var target by mutableFloatStateOf(0f)
    var springy = true
    private var job: Job? = null

    fun stop() { job?.cancel() }

    fun animateTo(t: Float, velocity: Float = 0f) {
        target = t
        job?.cancel()
        job = scope.launch {
            val spec: AnimationSpec<Float> = if (springy) fluid(if (t > 0.5f) 0.44f else 0.38f) else tween(220)
            animate(p, t, velocity, spec) { v, _ -> p = v }
        }
    }

    fun open() = animateTo(1f)
    fun close() = animateTo(0f)

    fun dragBy(delta: Float) {
        job?.cancel()
        p = (p + delta).coerceIn(0f, 1f)
        target = if (p > 0f) 1f else 0f
    }

    fun settle(velocity: Float) = animateTo(
        when {
            velocity > 1.4f -> 1f
            velocity < -1.4f -> 0f
            p > 0.5f -> 1f
            else -> 0f
        },
        velocity
    )
}

@Composable
fun App(vm: TrackerViewModel, resumes: StateFlow<Int>) {
    val ctx = LocalContext.current
    val state by vm.state.collectAsState()
    val tick by resumes.collectAsState()
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    val dark = when (state.theme) { 1 -> false; 2, 3 -> true; else -> isSystemInDarkTheme() }
    val springy = state.springy
    val lang = remember(state.language) { Lang.applyLocale(state.language); Lang.resolve(state.language) }

    var adding by remember { mutableStateOf<Task?>(null) }
    var editing by remember { mutableStateOf<Task?>(null) }
    var detailId by remember { mutableStateOf<String?>(null) }
    var overlay by remember { mutableStateOf(0) }
    var fadeNav by remember { mutableStateOf(false) }
    var backProgress by remember { mutableFloatStateOf(0f) }
    var backCode by remember { mutableIntStateOf(-1) }
    var sheetP by remember { mutableFloatStateOf(0f) }
    var showPraise by remember { mutableStateOf(false) }
    var coach by remember { mutableStateOf(1) }
    var praiseText by remember { mutableStateOf("") }
    val drawer = remember { DrawerCtl(scope) }
    drawer.springy = springy
    val haptics = rememberHaptics()

    fun nav(code: Int) {
        if (code == overlay) return
        backCode = -1
        backProgress = 0f
        fadeNav = overlay in ROOTS && code in ROOTS
        overlay = code
    }

    fun parentOf(code: Int) = if (code in SETTINGS_CHILDREN) 8 else 0

    PredictiveBackHandler(enabled = overlay != 0) { progress ->
        val code = overlay
        try {
            backCode = code
            backProgress = 0f
            progress.collect { backProgress = it.progress }
            val to = parentOf(code)
            fadeNav = code in ROOTS && to in ROOTS
            overlay = to
        } catch (e: CancellationException) {
            val from = backProgress
            scope.launch { animate(from, 0f, animationSpec = fluid(0.3f)) { v, _ -> backProgress = v } }
        }
    }

    PredictiveBackHandler(enabled = drawer.target > 0f) { progress ->
        val from = drawer.p
        try {
            progress.collect { e ->
                drawer.stop()
                drawer.p = from * (1f - 0.3f * e.progress)
            }
            drawer.close()
        } catch (e: CancellationException) {
            drawer.open()
        }
    }

    fun undoable(message: String, undo: (() -> Unit)?) {
        scope.launch {
            snack.currentSnackbarData?.dismiss()
            val r = snack.showSnackbar(
                message = message, actionLabel = if (undo != null) Lang.tr(lang, "Undo") else null,
                duration = SnackbarDuration.Short
            )
            if (r == SnackbarResult.ActionPerformed) undo?.invoke()
        }
    }

    val onToggle: (Task, LocalDate) -> Unit = { t, d ->
        val today = LocalDate.now()
        val before = vm.state.value
        val wasAll = before.tasksOn(today).let { l -> l.isNotEmpty() && l.all { before.isDone(it, today) } }
        if (vm.toggleDone(t, d) != null) {
            haptics.tick()
        } else if (d != today) {
            haptics.confirm()
        } else {
            haptics.confirm()
            val after = vm.state.value
            val nowAll = after.tasksOn(today).let { l -> l.isNotEmpty() && l.all { after.isDone(it, today) } }
            if (nowAll && !wasAll) {
                praiseText = listOf(
                    "Well done!", "Good job!", "Nice work!", "All done for today!",
                    "You crushed it!", "Great job!", "Today is complete!", "Keep it up!"
                ).random().let { Lang.tr(lang, it) }
                showPraise = true
            }
        }
    }
    LaunchedEffect(showPraise) { if (showPraise) { delay(1800); showPraise = false } }

    val view = LocalView.current
    SideEffect {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    var pendingImport by remember { mutableStateOf<PendingImport?>(null) }
    LaunchedEffect(tick) {
        if (tick > 0) withContext(Dispatchers.Default) { Reminders.scheduleAll(ctx, Store.get(ctx)); TasksWidget.refresh(ctx) }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            ctx.contentResolver.openOutputStream(uri)?.use { it.write(Store.exportJson(ctx, state).toByteArray()) }
            Toast.makeText(ctx, Lang.tr(lang, "Exported"), Toast.LENGTH_SHORT).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            val imported = Store.parse(text)
            if (imported == null) {
                Toast.makeText(ctx, Lang.tr(lang, "Invalid file"), Toast.LENGTH_SHORT).show()
            } else if (Store.hasSettings(text)) {
                pendingImport = PendingImport(imported, Store.parsePhoto(text))
            } else {
                vm.importMerge(imported)
                Toast.makeText(ctx, Lang.tr(lang, "Imported"), Toast.LENGTH_SHORT).show()
            }
        }
    }
    CompositionLocalProvider(LocalLang provides lang, LocalLayoutDirection provides LayoutDirection.Ltr) {
    ThemeFade(Triple(dark, state.theme == 3, state.accent), enabled = springy) { look ->
    TrackerTheme(dark = look.first, oled = look.second, springy = springy, accent = look.third) {
        RootBox {
            val cs = MaterialTheme.colorScheme
            val dens = LocalDensity.current
            val sheetOpen = adding != null || editing != null
            LaunchedEffect(sheetOpen) { if (!sheetOpen) sheetP = 0f }
            val sheetBlur = when (state.blurMode) { 1 -> 12f; 2 -> 22f; else -> 0f }
            val drawerBlur = when (state.blurMode) { 1 -> 6f; 2 -> 12f; else -> 0f }
            val drawerW = 304.dp
            val drawerPx = with(dens) { drawerW.toPx() }
            fun openDrawer() { haptics.tick(); drawer.open() }
            var warm by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { delay(1200); warm = true }
            var previewing by remember { mutableStateOf(false) }
            val pv by animateFloatAsState(if (previewing) 1f else 0f, if (springy) fluid(0.3f) else tween(150), label = "preview")
            val menuIndex = when (overlay) {
                0 -> 0
                7 -> 1 + MENU_VIEWS.indexOf(state.calView).coerceAtLeast(0)
                8, in SETTINGS_CHILDREN -> MENU_VIEWS.size + 1
                else -> -1
            }
            fun applyMenu(i: Int) {
                when (i) {
                    0 -> nav(0)
                    in 1..MENU_VIEWS.size -> { vm.setCalView(MENU_VIEWS[i - 1]); nav(7) }
                    else -> nav(8)
                }
            }
            var previewView by remember { mutableStateOf<Int?>(null) }
            var pendingMenu by remember { mutableIntStateOf(-1) }
            fun previewMenu(i: Int) {
                when (i) {
                    0 -> { previewView = null; nav(0) }
                    in 1..MENU_VIEWS.size -> { previewView = MENU_VIEWS[i - 1]; nav(7) }
                    else -> { previewView = null; nav(8) }
                }
            }
            LaunchedEffect(pendingMenu) {
                if (pendingMenu < 0) return@LaunchedEffect
                delay(70)
                previewMenu(pendingMenu)
            }
            fun endPreview() {
                if (pendingMenu >= 0) previewMenu(pendingMenu)
                pendingMenu = -1
                previewView?.let { v -> vm.setCalView(v) }
                previewView = null
                previewing = false
                drawer.close()
            }
            fun pickMenu(i: Int) {
                val change = i != menuIndex
                if (change) { haptics.tick(); applyMenu(i) }
                scope.launch { if (change) delay(140); drawer.close() }
            }

            val pageSpec: AnimationSpec<Float> = if (springy) fluid(0.5f) else tween(300, easing = FastOutSlowInEasing)
            val pushP = remember { Animatable(0f) }
            val rootP = remember { Animatable(0f) }
            val childP = remember { Animatable(0f) }
            LaunchedEffect(overlay) {
                launch { pushP.animateTo(if (overlay in TODAY_CHILDREN) 1f else 0f, pageSpec) }
                launch { rootP.animateTo(if (overlay != 0 && overlay !in TODAY_CHILDREN) 1f else 0f, pageSpec) }
                launch { childP.animateTo(if (overlay in SETTINGS_CHILDREN) 1f else 0f, pageSpec) }
            }
            fun backFor(codes: Set<Int>): Float = if (backCode in codes) backProgress else 0f

            val openDrag = rememberDraggableState { d -> drawer.dragBy(d / drawerPx) }
            Box(
                Modifier.fillMaxSize()
                    .draggable(
                        openDrag, Orientation.Horizontal, enabled = overlay == 0 && !sheetOpen,
                        onDragStarted = { drawer.stop() }, onDragStopped = { v -> drawer.settle(v / drawerPx) }
                    )
                    .graphicsLayer {
                        val d = drawer.p
                        val q = 1f - pv
                        translationX = d * drawerPx * 0.16f * q
                        val r = maxOf(sheetBlur * sheetP, drawerBlur * d * q)
                        renderEffect = if (r > 0.3f && Build.VERSION.SDK_INT >= 31) {
                            val px = r * density
                            BlurEffect(px, px)
                        } else null
                    }
            ) {
                Box(
                    Modifier.fillMaxSize().graphicsLayer {
                        val push = pushP.value * (1f - backFor(TODAY_CHILDREN))
                        val root = rootP.value * (1f - backFor(ROOTS))
                        translationX = -size.width * 0.18f * push
                        val sc = 1f - 0.04f * root
                        scaleX = sc
                        scaleY = sc
                    }.drawWithContent {
                        drawContent()
                        val push = pushP.value * (1f - backFor(TODAY_CHILDREN))
                        if (push > 0.01f) drawRect(Color.Black.copy(alpha = 0.16f * push))
                    }
                ) {
                    TodayPage(
                        state = state, onToggle = onToggle, onOpen = { editing = it },
                        onAdd = { k -> adding = newDraft(k, LocalDate.now()) },
                        onInsights = { nav(1) },
                        onMenu = { openDrawer() },
                        coach = if (state.onboarded && !state.tipsDone) coach else 0,
                        springy = springy,
                        onReorder = { from, to -> vm.reorder(from, to) },
                        onDismissAll = { list -> undoable("Dismissed ${list.size}", vm.dismissOverdue(list)) }
                    )
                }

                // ── full-screen overlays ──
                fun fx(code: Int) = Modifier.graphicsLayer {
                    val p = if (backCode == code) backProgress else 0f
                    scaleX = 1f - 0.08f * p
                    scaleY = 1f - 0.08f * p
                    translationX = p * 40.dp.toPx()
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                }
                val fx7 = remember { fx(7) }
                val fx8 = remember {
                    fx(8).graphicsLayer {
                        translationX = -size.width * 0.18f * childP.value * (1f - backFor(SETTINGS_CHILDREN))
                    }.drawWithContent {
                        drawContent()
                        val c = childP.value * (1f - backFor(SETTINGS_CHILDREN))
                        if (c > 0.01f) drawRect(Color.Black.copy(alpha = 0.16f * c))
                    }
                }
                ScreenLayer(overlay == 7, fadeNav, springy, keep = true, warm = warm) {
                    CalendarScreen(
                        state = state, onClose = { nav(0) }, onMenu = { openDrawer() },
                        onOpen = { editing = it }, onToggle = onToggle, onAdd = { adding = it },
                        onQuickSave = { t -> vm.saveTask(t); undoable(Lang.tr(lang, "Saved"), null) },
                        modifier = fx7, viewOverride = previewView
                    )
                }
                ScreenLayer(overlay == 8 || overlay in SETTINGS_CHILDREN, fadeNav, springy, keep = true, warm = warm) {
                    SettingsScreen(
                        state = state, tick = tick, vm = vm, onClose = { nav(0) },
                        onImport = { importLauncher.launch(arrayOf("application/json", "text/*", "*/*")) },
                        onExport = { exportLauncher.launch("daily-tracker-backup.json") },
                        onRepeating = { nav(2) },
                        onLanguage = { nav(4) },
                        onHolidays = { nav(5) },
                        modifier = fx8
                    )
                }
                ScreenLayer(overlay == 1, fadeNav, springy, keep = false) {
                    InsightsScreen(
                        state = state, onClose = { nav(0) },
                        onSwipeHintSeen = { vm.markInsightsSwipeSeen() }, modifier = fx(1)
                    )
                }
                ScreenLayer(overlay == 2, fadeNav, springy, keep = false) {
                    RepeatingScreen(state = state, onClose = { nav(8) }, onOpen = { editing = it }, modifier = fx(2))
                }
                ScreenLayer(overlay == 4, fadeNav, springy, keep = false) {
                    LanguageScreen(state = state, vm = vm, onClose = { nav(8) }, modifier = fx(4))
                }
                ScreenLayer(overlay == 5, fadeNav, springy, keep = false) {
                    HolidaysScreen(state = state, vm = vm, onClose = { nav(8) }, modifier = fx(5))
                }
                ScreenLayer(overlay == 3, fadeNav, springy, keep = false) {
                    TaskDetailScreen(state = state, taskId = detailId ?: "", onClose = { nav(0) }, modifier = fx(3))
                }
            }

            // ── side menu ──
            val drawerShown by remember { derivedStateOf { drawer.p > 0.001f || drawer.target > 0f } }
            if (drawerShown) {
                val closeDrag = rememberDraggableState { d -> drawer.dragBy(d / drawerPx) }
                Box(
                    Modifier.fillMaxSize().draggable(
                        closeDrag, Orientation.Horizontal,
                        onDragStarted = { drawer.stop() }, onDragStopped = { v -> drawer.settle(v / drawerPx) }
                    )
                ) {
                    Box(
                        Modifier.fillMaxSize()
                            .graphicsLayer { alpha = drawer.p.coerceIn(0f, 1f) * (1f - 0.85f * pv) }
                            .background(Color.Black.copy(alpha = if (state.blurMode == 0) 0.4f else 0.24f))
                            .pointerInput(Unit) { detectTapGestures { drawer.close() } }
                    )
                    DrawerPanel(
                        selected = menuIndex, progress = { drawer.p },
                        onPick = { pickMenu(it) },
                        onPreviewStart = { haptics.tick(); previewing = true },
                        onPreview = { i -> haptics.step(); pendingMenu = i },
                        onPreviewEnd = { endPreview() },
                        modifier = Modifier.width(drawerW).fillMaxHeight().graphicsLayer {
                            translationX = -(1f - drawer.p) * size.width
                        }
                    )
                }
            }

            SnackbarHost(
                hostState = snack,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 76.dp)
            ) { data ->
                if (data.visuals.actionLabel == null) {
                    Snackbar(shape = RShape(16.dp), containerColor = cs.onSurface.copy(alpha = 0.92f), contentColor = cs.surface) {
                        Text(data.visuals.message, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
                    }
                } else {
                    Snackbar(
                        data, shape = RShape(16.dp),
                        containerColor = cs.onSurface.copy(alpha = 0.92f), contentColor = cs.surface, actionColor = cs.primary
                    )
                }
            }

            AnimatedVisibility(
                visible = showPraise,
                modifier = Modifier.align(Alignment.Center),
                enter = scaleIn(fluid(0.5f, 0.32f), initialScale = 0.6f) + fadeIn(fluid(0.25f)),
                exit = scaleOut(fluid(0.3f), targetScale = 0.9f) + fadeOut(fluid(0.22f))
            ) {
                val pop = remember { Animatable(0f) }
                LaunchedEffect(Unit) { delay(90); pop.animateTo(1f, fluid(0.5f, 0.45f)) }
                Column(
                    Modifier.softShadow(RoundedCornerShape(28.dp), 24.dp).clip(RoundedCornerShape(28.dp)).background(elevatedColor())
                        .padding(horizontal = 36.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(Modifier.size(56.dp).clip(CircleShape).background(GREEN), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Check, null, tint = Color.White,
                            modifier = Modifier.size(30.dp).graphicsLayer {
                                val k = pop.value
                                scaleX = k
                                scaleY = k
                                rotationZ = -35f * (1f - k)
                            }
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(praiseText, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = cs.onSurface)
                }
            }

            // ── task sheets ──
            adding?.let { draft ->
                TaskSheet(
                    initial = draft, isNew = true, overdue = false, paused = false,
                    onDismiss = { adding = null },
                    onSave = { vm.saveTask(it); adding = null },
                    onDelete = null,
                    onProgress = { sheetP = it }
                )
            }
            editing?.let { task ->
                val today = LocalDate.now()
                TaskSheet(
                    initial = task, isNew = false,
                    overdue = state.overdue(today).any { it.id == task.id },
                    paused = state.hasFutureSkips(task, today),
                    onDismiss = { editing = null },
                    onSave = { vm.saveTask(it); editing = null },
                    onDelete = { t -> val undo = vm.deleteTask(t); editing = null; undoable(Lang.tr(lang, if (t.isTask) "Task deleted" else "Deleted"), undo) },
                    onStats = { t -> editing = null; detailId = t.id; nav(3) },
                    onSkipToday = { t -> val u = vm.skip(t, LocalDate.now()); editing = null; undoable(Lang.tr(lang, "Skipped for today"), u) },
                    onPauseUntil = { t, d -> val u = vm.pauseUntil(t, d); editing = null; undoable("${Lang.tr(lang, "Back on")} ${d.plusDays(1).let { n -> "${n.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())} ${n.dayOfMonth} ${n.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())}" }}", u) },
                    onResume = { t -> val u = vm.resume(t); editing = null; undoable(Lang.tr(lang, "Resumed"), u) },
                    onDismissOverdue = { t -> val u = vm.skip(t, t.date); editing = null; undoable(Lang.tr(lang, "Dismissed"), u) },
                    onMoveTomorrow = { t ->
                        val from = if (!t.repeating && t.date.isBefore(today)) t.date else today
                        val u = vm.moveToTomorrow(t, from); editing = null; undoable(Lang.tr(lang, "Moved to tomorrow"), u)
                    },
                    onShiftAll = { t -> val u = vm.shiftSchedule(t, 1); editing = null; undoable(Lang.tr(lang, "Schedule shifted by 1 day"), u) },
                    onProgress = { sheetP = it }
                )
            }

            // ── import: tasks only, or settings and name too ──
            pendingImport?.let { p ->
                CDialog(
                    title = tx("Import"),
                    text = tx("This file also has your name and settings. Restore them too?"),
                    confirm = tx("Tasks and settings"),
                    onConfirm = {
                        vm.importMerge(p.state, withSettings = true, photo = p.photo); pendingImport = null
                        Toast.makeText(ctx, Lang.tr(lang, "Imported"), Toast.LENGTH_SHORT).show()
                    },
                    onDismiss = { pendingImport = null },
                    dismiss = tx("Tasks only"),
                    onDismissButton = {
                        vm.importMerge(p.state); pendingImport = null
                        Toast.makeText(ctx, Lang.tr(lang, "Imported"), Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // ── first start: ask for everything, then show how the app works ──
            if (!state.onboarded) {
                Onboarding(
                    tick = tick,
                    onAskNotifications = {
                        if (Build.VERSION.SDK_INT >= 33) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    onAskExact = {
                        if (Build.VERSION.SDK_INT >= 31) {
                            ctx.startActivity(
                                Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}"))
                            )
                        }
                    },
                    onFinish = { vm.markOnboarded() }
                )
            }

            // ── two short tips inside the app: the menu / Insights ──
            if (state.onboarded && !state.tipsDone && !sheetOpen && overlay == 0 && !drawerShown) {
                val tips = listOf(
                    tx("Tap the menu to open the Calendar and Settings."),
                    tx("Tap “How it's going” to open Insights.")
                )
                CoachCard(
                    step = coach.coerceIn(1, 2), total = 2, text = tips[coach.coerceIn(1, 2) - 1], last = coach >= 2,
                    onNext = { if (coach >= 2) vm.markTipsDone() else coach++ },
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 84.dp)
                )
            }
        }
    }
    }
    }
}

// ═══════════════ ScreenLayer ═══════════════

@Composable
private fun ScreenLayer(
    shown: Boolean, fade: Boolean, springy: Boolean, keep: Boolean, warm: Boolean = false,
    content: @Composable () -> Unit
) {
    var alive by remember { mutableStateOf(shown) }
    val p = remember { Animatable(if (shown) 1f else 0f) }
    val isShown by rememberUpdatedState(shown)
    var entering by remember { mutableStateOf(shown) }
    LaunchedEffect(shown) {
        if (shown) alive = true
        entering = shown
        val spec: AnimationSpec<Float> = when {
            !springy -> tween(if (fade) 200 else 280)
            fade -> fluid(if (shown) 0.4f else 0.3f)
            else -> fluid(0.5f)
        }
        p.animateTo(if (shown) 1f else 0f, spec)
        if (!shown && !keep) alive = false
    }
    LaunchedEffect(warm) { if (warm && keep) alive = true }
    if (!alive && !shown) return
    CompositionLocalProvider(LocalScreenActive provides shown) {
        Box(
            Modifier.fillMaxSize()
                .offset { if (!isShown && p.value <= 0.001f) IntOffset(0, 400_000) else IntOffset.Zero }
                .graphicsLayer {
                    val v = p.value.coerceIn(0f, 1f)
                    if (fade) {
                        alpha = v
                        val sc = if (entering) 0.92f + 0.08f * v else 1f + 0.05f * (1f - v)
                        scaleX = sc
                        scaleY = sc
                    } else {
                        translationX = (1f - p.value) * size.width
                        alpha = 0.4f + 0.6f * v
                    }
                }
        ) { content() }
    }
}

// ═══════════════ SideMenu ═══════════════

private val MENU_VIEWS = listOf(VIEW_SCHEDULE, VIEW_DAY, VIEW_3DAYS, VIEW_WEEK, VIEW_MONTH)

@Composable
private fun DrawerPanel(
    selected: Int, progress: () -> Float, onPick: (Int) -> Unit,
    onPreviewStart: () -> Unit, onPreview: (Int) -> Unit, onPreviewEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val springy = LocalSpringy.current
    val shape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
    val rects = remember { mutableStateMapOf<Int, Rect>() }
    var panel by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var preview by remember { mutableIntStateOf(-1) }
    val active = if (preview >= 0) preview else selected
    val top = remember { Animatable(0f) }
    val bottom = remember { Animatable(0f) }
    val left = remember { Animatable(0f) }
    val right = remember { Animatable(0f) }
    var placed by remember { mutableStateOf(false) }
    val target = rects[active]
    LaunchedEffect(active, target) {
        val t = target ?: return@LaunchedEffect
        if (!placed || !springy) {
            top.snapTo(t.top); bottom.snapTo(t.bottom); left.snapTo(t.left); right.snapTo(t.right)
            placed = true
            return@LaunchedEffect
        }
        val down = t.top > top.value
        launch { top.animateTo(t.top, fluid(if (down) 0.42f else 0.28f, 0.1f)) }
        launch { bottom.animateTo(t.bottom, fluid(if (down) 0.28f else 0.42f, 0.1f)) }
        launch { left.animateTo(t.left, fluid(0.34f)) }
        launch { right.animateTo(t.right, fluid(0.34f)) }
    }
    val pick by rememberUpdatedState(onPick)
    val start by rememberUpdatedState(onPreviewStart)
    val move by rememberUpdatedState(onPreview)
    val end by rememberUpdatedState(onPreviewEnd)
    val current by rememberUpdatedState(selected)
    fun hit(o: Offset): Int? = rects.entries.firstOrNull { (_, r) -> o.y >= r.top && o.y <= r.bottom }?.key
    fun Modifier.enter(i: Int) = graphicsLayer {
        val q = progress().coerceIn(0f, 1f)
        translationX = -(1f - q) * (18 + 16 * i).dp.toPx()
        alpha = (q * 1.4f - 0.15f * i).coerceIn(0f, 1f)
    }
    fun Modifier.spot(i: Int) = onGloballyPositioned { c ->
        val p = panel ?: return@onGloballyPositioned
        if (p.isAttached && c.isAttached) rects[i] = p.localBoundingBoxOf(c, clipBounds = false)
    }
    val pill = cs.primary.copy(alpha = if (isDarkUi()) 0.16f else 0.10f)
    Box(
        modifier.then(if (isDarkUi()) Modifier else Modifier.softShadow(shape, 24.dp)).clip(shape).background(cs.background)
            .fadingEdge(top = false, radius = 28.dp)
            .onGloballyPositioned { panel = it }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    var vertical = false
                    while (true) {
                        val ev = awaitPointerEvent(PointerEventPass.Initial)
                        val c = ev.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                        if (!c.pressed) return@awaitEachGesture
                        val d = c.position - down.position
                        if (d.getDistance() > viewConfiguration.touchSlop) {
                            vertical = abs(d.y) > abs(d.x)
                            break
                        }
                    }
                    if (!vertical) return@awaitEachGesture
                    val first = hit(down.position) ?: return@awaitEachGesture
                    preview = first
                    start()
                    if (first != current) move(first)
                    while (true) {
                        val ev = awaitPointerEvent(PointerEventPass.Initial)
                        val c = ev.changes.firstOrNull { it.id == down.id } ?: break
                        c.consume()
                        if (!c.pressed) break
                        val i = hit(c.position)
                        if (i != null && i != preview) { preview = i; move(i) }
                    }
                    preview = -1
                    end()
                }
            }
            .drawWithContent {
                drawContent()
                if (placed) {
                    val q = progress().coerceIn(0f, 1f)
                    val g = when (active) { 0 -> 1; in 1..MENU_VIEWS.size -> 2; else -> 3 }
                    val a = (q * 1.4f - 0.15f * g).coerceIn(0f, 1f)
                    val inset = 5.dp.toPx()
                    val side = 14.dp.toPx() + inset
                    translate(left = -(1f - q) * (18 + 16 * g).dp.toPx()) {
                        drawRoundRect(
                            pill.copy(alpha = pill.alpha * a), Offset(side, top.value + inset),
                            Size(size.width - side * 2, bottom.value - top.value - inset * 2), CornerRadius(14.dp.toPx())
                        )
                    }
                }
            }
    ) {
        Column(
            Modifier.statusBarsPadding().navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 20.dp)
        ) {
            Text(
                "Tasker", fontSize = 28.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.enter(0).padding(start = 12.dp, top = 8.dp, bottom = 18.dp)
            )
            CardGroup(Modifier.enter(1), inset = 54.dp) {
                DrawerItem("home", tx("Today"), active == 0, Modifier.spot(0)) { pick(0) }
            }
            GroupLabel(tx("Calendar"), Modifier.enter(2))
            CardGroup(Modifier.enter(2), inset = 54.dp) {
                MENU_VIEWS.forEachIndexed { k, v ->
                    DrawerItem(viewIcon(v), viewLabel(v), active == k + 1, Modifier.spot(k + 1)) { pick(k + 1) }
                }
            }
            Spacer(Modifier.height(16.dp))
            val last = MENU_VIEWS.size + 1
            CardGroup(Modifier.enter(3), inset = 54.dp) {
                DrawerItem("gear", tx("Settings"), active == last, Modifier.spot(last), turn = 90f) { pick(last) }
            }
        }
    }
}

@Composable
private fun DrawerItem(
    icon: String, label: String, selected: Boolean, modifier: Modifier = Modifier, turn: Float = 0f, onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val springy = LocalSpringy.current
    val spec: AnimationSpec<Color> = if (springy) fluid(0.26f) else tween(120)
    val fg by animateColorAsState(if (selected) cs.primary else cs.onSurface, spec, label = "drawerFg")
    val tint by animateColorAsState(if (selected) cs.primary else cs.onSurfaceVariant, spec, label = "drawerTint")
    val dot by animateFloatAsState(if (selected) 1f else 0f, if (springy) fluid(0.36f, 0.4f) else tween(120), label = "drawerDot")
    val src = remember { MutableInteractionSource() }
    val press = pressProgress(src)
    Row(
        modifier.fillMaxWidth().springClick(RectangleShape, 0.98f, source = src, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(22.dp).iconPop(selected, 0.7f, turn).graphicsLayer {
                val k = press.value
                rotationZ += turn * 0.25f * k
                val sc = 1f - 0.1f * k
                scaleX *= sc
                scaleY *= sc
            },
            contentAlignment = Alignment.Center
        ) { LineIcon(icon, 22.dp, tint) }
        Spacer(Modifier.width(16.dp))
        Text(
            label, fontSize = 16.sp, color = fg, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Box(Modifier.size(6.dp).graphicsLayer { scaleX = dot; scaleY = dot; alpha = dot.coerceIn(0f, 1f) }.clip(CircleShape).background(cs.primary))
    }
}

private class PendingImport(val state: AppState, val photo: ByteArray?)
