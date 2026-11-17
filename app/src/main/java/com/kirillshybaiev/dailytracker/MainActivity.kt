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

// ═══════════════ MainActivity ═══════════════

class MainActivity : ComponentActivity() {
    private val vm: TrackerViewModel by viewModels()
    private val resumes = MutableStateFlow(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { App(vm, resumes) }
    }

    override fun onResume() {
        super.onResume()
        resumes.value += 1
    }
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

    var adding by remember { mutableStateOf<LocalDate?>(null) }
    var editing by remember { mutableStateOf<Task?>(null) }
    var detailId by remember { mutableStateOf<String?>(null) }
    var overlay by remember { mutableStateOf(0) }
    var backProgress by remember { mutableStateOf(0f) }
    var showPraise by remember { mutableStateOf(false) }
    var coach by remember { mutableStateOf(1) }
    var praiseText by remember { mutableStateOf("") }

    PredictiveBackHandler(enabled = overlay != 0) { progress ->
        try {
            progress.collect { backProgress = it.progress }
            overlay = if (overlay == 2 || overlay == 4 || overlay == 5) 8 else 0
        } catch (e: CancellationException) {
            backProgress = 0f
        }
    }

    fun undoable(message: String, undo: (() -> Unit)?) {
        scope.launch {
            snack.currentSnackbarData?.dismiss()
            val r = snack.showSnackbar(
                message = message, actionLabel = if (undo != null) "Undo" else null,
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
            undoable(Lang.tr(lang, "Marked as not done"), null)
        } else if (d == today) {
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
    TrackerTheme(dark = dark, oled = state.theme == 3, springy = springy) {
        RootBox {
            val cs = MaterialTheme.colorScheme
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val drawerOpen by animateFloatAsState(
                if (drawerState.targetValue == DrawerValue.Open) 1f else 0f,
                if (springy) spring(dampingRatio = 0.9f, stiffness = 300f) else tween(220), label = "drawerBlur"
            )
            fun go(code: Int) { scope.launch { drawerState.close() }; backProgress = 0f; overlay = code }
            val swipeBlurDp = when (state.blurMode) { 1 -> 8f; 2 -> 16f; else -> 0f }
            val sheetOpen = adding != null || editing != null
            val blurTarget = when {
                state.blurMode == 0 -> 0f
                sheetOpen -> 18f
                else -> 0f
            }
            val fastBlur = state.blurMode == 1
            val steadyBlur = remember { Animatable(0f) }
            LaunchedEffect(blurTarget, fastBlur) {
                steadyBlur.animateTo(
                    blurTarget,
                    tween(if (blurTarget == 0f) (if (fastBlur) 70 else 200) else (if (fastBlur) 110 else 240))
                )
            }
            val ovp = remember { Animatable(0f) }
            LaunchedEffect(overlay != 0) {
                ovp.animateTo(if (overlay != 0) 1f else 0f, if (springy) spring(dampingRatio = 0.85f, stiffness = 380f) else tween(300))
            }
            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = overlay == 0 && !sheetOpen,
                scrimColor = Color.Black.copy(alpha = if (state.blurMode == 0) 0.45f else 0.2f),
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = cs.surface, drawerContentColor = cs.onSurface,
                        drawerShape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp),
                        modifier = Modifier.width(280.dp)
                    ) {
                        Column(Modifier.statusBarsPadding().navigationBarsPadding().padding(horizontal = 14.dp, vertical = 26.dp)) {
                            Eyebrow(tx("Menu"), Modifier.padding(start = 16.dp, bottom = 16.dp))
                            DrawerItem(Icons.Default.DateRange, tx("Calendar")) { go(7) }
                            DrawerItem(Icons.Default.Settings, tx("Settings")) { go(8) }
                        }
                    }
                }
            ) {
            Box(
                Modifier.fillMaxSize().then(
                    if (state.blurMode == 0) Modifier else Modifier.graphicsLayer {
                        val p = ovp.value.coerceIn(0f, 1f)
                        val travel = (4f * p * (1f - p)).coerceIn(0f, 1f)
                        val dp = maxOf(steadyBlur.value, swipeBlurDp * travel, 16f * drawerOpen)
                        if (dp > 0.1f && Build.VERSION.SDK_INT >= 31) {
                            val px = dp * density
                            renderEffect = BlurEffect(px, px)
                        }
                    }
                )
            ) {
            TodayPage(
                state = state, onToggle = onToggle, onOpen = { editing = it },
                onAdd = { adding = LocalDate.now() },
                onInsights = { backProgress = 0f; overlay = 1 },
                onMenu = { scope.launch { drawerState.open() } },
                coach = if (state.onboarded && !state.tipsDone) coach else 0,
                springy = springy,
                onReorder = { from, to -> vm.reorder(from, to) },
                onDismissAll = { list -> undoable("Dismissed ${list.size}", vm.dismissOverdue(list)) }
            )

            // ── full-screen overlays ──
            val overlayFx = Modifier.graphicsLayer {
                val p = backProgress
                scaleX = 1f - 0.08f * p
                scaleY = 1f - 0.08f * p
                translationX = p * 40.dp.toPx()
            }
            val slideSpec: FiniteAnimationSpec<IntOffset> =
                if (springy) spring(dampingRatio = 0.82f, stiffness = 320f, visibilityThreshold = IntOffset.VisibilityThreshold)
                else tween(300)
            val fadeSpec: FiniteAnimationSpec<Float> = if (springy) spring(stiffness = 500f) else tween(300)
            AnimatedVisibility(
                visible = overlay == 7,
                enter = slideInHorizontally(slideSpec) { it } + fadeIn(fadeSpec),
                exit = slideOutHorizontally(slideSpec) { it } + fadeOut(fadeSpec)
            ) {
                CalendarScreen(
                    state = state, onClose = { overlay = 0 }, onOpen = { editing = it },
                    onToggle = onToggle, onAddOn = { adding = it }, modifier = overlayFx
                )
            }
            AnimatedVisibility(
                visible = overlay == 8 || overlay == 2 || overlay == 4 || overlay == 5,
                enter = slideInHorizontally(slideSpec) { it } + fadeIn(fadeSpec),
                exit = slideOutHorizontally(slideSpec) { it } + fadeOut(fadeSpec)
            ) {
                SettingsScreen(
                    state = state, tick = tick, vm = vm, onClose = { overlay = 0 },
                    onImport = { importLauncher.launch(arrayOf("application/json", "text/*", "*/*")) },
                    onExport = { exportLauncher.launch("daily-tracker-backup.json") },
                    onRepeating = { backProgress = 0f; overlay = 2 },
                    onLanguage = { backProgress = 0f; overlay = 4 },
                    onHolidays = { backProgress = 0f; overlay = 5 },
                    modifier = overlayFx
                )
            }
            AnimatedVisibility(
                visible = overlay == 1,
                enter = slideInHorizontally(slideSpec) { it } + fadeIn(fadeSpec),
                exit = slideOutHorizontally(slideSpec) { it } + fadeOut(fadeSpec)
            ) { InsightsScreen(
                    state = state, onClose = { overlay = 0 },
                    onSwipeHintSeen = { vm.markInsightsSwipeSeen() }, modifier = overlayFx
                ) }
            AnimatedVisibility(
                visible = overlay == 2,
                enter = slideInHorizontally(slideSpec) { it } + fadeIn(fadeSpec),
                exit = slideOutHorizontally(slideSpec) { it } + fadeOut(fadeSpec)
            ) {
                RepeatingScreen(state = state, onClose = { overlay = 8 }, onOpen = { editing = it }, modifier = overlayFx)
            }
            AnimatedVisibility(
                visible = overlay == 4,
                enter = slideInHorizontally(slideSpec) { it } + fadeIn(fadeSpec),
                exit = slideOutHorizontally(slideSpec) { it } + fadeOut(fadeSpec)
            ) { LanguageScreen(state = state, vm = vm, onClose = { overlay = 8 }, modifier = overlayFx) }
            AnimatedVisibility(
                visible = overlay == 5,
                enter = slideInHorizontally(slideSpec) { it } + fadeIn(fadeSpec),
                exit = slideOutHorizontally(slideSpec) { it } + fadeOut(fadeSpec)
            ) { HolidaysScreen(state = state, vm = vm, onClose = { overlay = 8 }, modifier = overlayFx) }
            AnimatedVisibility(
                visible = overlay == 3,
                enter = slideInHorizontally(slideSpec) { it } + fadeIn(fadeSpec),
                exit = slideOutHorizontally(slideSpec) { it } + fadeOut(fadeSpec)
            ) {
                TaskDetailScreen(state = state, taskId = detailId ?: "", onClose = { overlay = 0 }, modifier = overlayFx)
            }
            }
            }

            SnackbarHost(
                hostState = snack,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 76.dp)
            ) { data ->
                if (data.visuals.actionLabel == null) {
                    Snackbar(shape = RShape(16.dp), containerColor = cs.onSurface, contentColor = cs.surface) {
                        Text(data.visuals.message, Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    }
                } else {
                    Snackbar(
                        data, shape = RShape(16.dp),
                        containerColor = cs.onSurface, contentColor = cs.surface, actionColor = cs.surface
                    )
                }
            }

            AnimatedVisibility(
                visible = showPraise,
                modifier = Modifier.align(Alignment.Center),
                enter = scaleIn(spring(dampingRatio = 0.5f, stiffness = 300f), initialScale = 0.6f) + fadeIn(),
                exit = scaleOut(targetScale = 0.9f) + fadeOut()
            ) {
                Surface(shape = RShape(28.dp), color = cs.onSurface, contentColor = cs.surface, shadowElevation = 12.dp) {
                    Text(
                        praiseText, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 22.dp)
                    )
                }
            }

            // ── task sheets ──
            adding?.let { day ->
                TaskSheet(
                    initial = null, defaultDate = day, overdue = false, paused = false,
                    onDismiss = { adding = null },
                    onSave = { vm.saveTask(it); adding = null },
                    onDelete = null
                )
            }
            editing?.let { task ->
                val today = LocalDate.now()
                TaskSheet(
                    initial = task, defaultDate = today,
                    overdue = state.overdue(today).any { it.id == task.id },
                    paused = state.hasFutureSkips(task, today),
                    onDismiss = { editing = null },
                    onSave = { vm.saveTask(it); editing = null },
                    onDelete = { t -> val undo = vm.deleteTask(t); editing = null; undoable(Lang.tr(lang, "Task deleted"), undo) },
                    onStats = { t -> editing = null; detailId = t.id; backProgress = 0f; overlay = 3 },
                    onSkipToday = { t -> val u = vm.skip(t, LocalDate.now()); editing = null; undoable(Lang.tr(lang, "Skipped for today"), u) },
                    onPauseUntil = { t, d -> val u = vm.pauseUntil(t, d); editing = null; undoable("${Lang.tr(lang, "Back on")} ${d.plusDays(1).let { n -> "${n.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())} ${n.dayOfMonth} ${n.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())}" }}", u) },
                    onResume = { t -> val u = vm.resume(t); editing = null; undoable(Lang.tr(lang, "Resumed"), u) },
                    onDismissOverdue = { t -> val u = vm.skip(t, t.date); editing = null; undoable(Lang.tr(lang, "Dismissed"), u) },
                    onMoveTomorrow = { t ->
                        val from = if (!t.repeating && t.date.isBefore(today)) t.date else today
                        val u = vm.moveToTomorrow(t, from); editing = null; undoable(Lang.tr(lang, "Moved to tomorrow"), u)
                    },
                    onShiftAll = { t -> val u = vm.shiftSchedule(t, 1); editing = null; undoable(Lang.tr(lang, "Schedule shifted by 1 day"), u) }
                )
            }

            // ── import: tasks only, or settings and name too ──
            pendingImport?.let { p ->
                AlertDialog(
                    containerColor = cs.surface, shape = RShape(24.dp),
                    onDismissRequest = { pendingImport = null },
                    title = { Text(tx("Import")) },
                    text = { Text(tx("This file also has your name and settings. Restore them too?")) },
                    confirmButton = {
                        STextButton(onClick = {
                            vm.importMerge(p.state, withSettings = true, photo = p.photo); pendingImport = null
                            Toast.makeText(ctx, Lang.tr(lang, "Imported"), Toast.LENGTH_SHORT).show()
                        }) { Text(tx("Tasks and settings")) }
                    },
                    dismissButton = {
                        STextButton(onClick = {
                            vm.importMerge(p.state); pendingImport = null
                            Toast.makeText(ctx, Lang.tr(lang, "Imported"), Toast.LENGTH_SHORT).show()
                        }) { Text(tx("Tasks only")) }
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
            if (state.onboarded && !state.tipsDone && !sheetOpen && overlay == 0) {
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

@Composable
private fun DrawerItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp).springClick(RShape(16.dp), 0.97f, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}

private class PendingImport(val state: AppState, val photo: ByteArray?)
