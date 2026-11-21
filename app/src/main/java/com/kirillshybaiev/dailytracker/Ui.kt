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
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
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
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer


// ═══════════════ Theme ═══════════════

val GREEN = Color(0xFF2FC56F)
val RED = Color(0xFFF2474E)
val CtrlShape: RoundedCornerShape get() = RShape(14.dp)
val CardShape = RoundedCornerShape(20.dp)

fun RShape(r: Dp): RoundedCornerShape = if (r < 24.dp) RoundedCornerShape(50) else RoundedCornerShape(r)

const val ACCENT_COUNT = 7
private val ACCENT_LIGHT = listOf(
    Color(0xFF2F6BFF), Color(0xFF2F6BFF), Color(0xFF1AAE63), Color(0xFFFF6A1F),
    Color(0xFF7458FF), Color(0xFFF0386B), Color(0xFF1C1C1E)
)
private val ACCENT_DARK = listOf(
    Color(0xFF4F84FF), Color(0xFF4F84FF), Color(0xFF2CC575), Color(0xFFFF8442),
    Color(0xFF8C75FF), Color(0xFFFF5A86), Color(0xFFEDEDF0)
)

fun accentColor(ctx: Context, index: Int, dark: Boolean): Color {
    if (index == 0 && Build.VERSION.SDK_INT >= 31) {
        return Color(ctx.getColor(if (dark) android.R.color.system_accent1_400 else android.R.color.system_accent1_600))
    }
    return (if (dark) ACCENT_DARK else ACCENT_LIGHT)[index.coerceIn(0, ACCENT_COUNT - 1)]
}

fun accentSwatch(index: Int): Color = ACCENT_LIGHT[index.coerceIn(0, ACCENT_COUNT - 1)]

fun onColor(c: Color): Color = if (c.luminance() > 0.55f) Color(0xFF111114) else Color.White

@Composable
fun isDarkUi(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

@Composable
fun Modifier.softShadow(shape: Shape, elevation: Dp = 14.dp): Modifier {
    if (isDarkUi()) return this.border(0.7.dp, Color.White.copy(alpha = 0.07f), shape)
    val c = Color.Black.copy(alpha = 0.22f)
    return this.shadow(elevation, shape, clip = false, ambientColor = c, spotColor = c)
}

@Composable
fun elevatedColor(): Color = MaterialTheme.colorScheme.surfaceContainerHigh

val TASK_COLORS = listOf(
    Color(0xFFF2474E), Color(0xFFFF8A34), Color(0xFFF5C23A), Color(0xFF2FC56F),
    Color(0xFF1FB8B0), Color(0xFF2F6BFF), Color(0xFF8C6BFF), Color(0xFFFF6FA8)
)

@Composable
fun TrackerTheme(dark: Boolean, oled: Boolean, springy: Boolean, accent: Int = 0, content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val primary = remember(accent, dark) { accentColor(ctx, accent, dark) }
    val scheme = remember(primary, dark, oled) {
        if (dark) {
            val bg = if (oled) Color.Black else Color(0xFF101012)
            val surface = if (oled) Color(0xFF141415) else Color(0xFF1C1C1F)
            val raised = if (oled) Color(0xFF212123) else Color(0xFF28282C)
            darkColorScheme(
                primary = primary, onPrimary = onColor(primary),
                primaryContainer = primary.copy(alpha = 0.16f).compositeOver(surface), onPrimaryContainer = primary,
                tertiary = Color(0xFFFF9A6B),
                background = bg, onBackground = Color(0xFFF2F2F5),
                surface = surface, onSurface = Color(0xFFF2F2F5),
                surfaceVariant = if (oled) Color(0xFF1E1E21) else Color(0xFF26262A),
                onSurfaceVariant = Color(0xFF909096),
                outline = Color.White.copy(alpha = if (oled) 0.07f else 0.08f).compositeOver(surface),
                surfaceContainer = surface, surfaceContainerHigh = raised, surfaceContainerHighest = raised
            )
        } else {
            lightColorScheme(
                primary = primary, onPrimary = onColor(primary),
                primaryContainer = primary.copy(alpha = 0.11f).compositeOver(Color.White), onPrimaryContainer = primary,
                tertiary = Color(0xFFFF7A45),
                background = Color(0xFFF3F4F6), onBackground = Color(0xFF16171A),
                surface = Color.White, onSurface = Color(0xFF16171A),
                surfaceVariant = Color(0xFFEEEFF2), onSurfaceVariant = Color(0xFF8A8D94),
                outline = Color(0xFFE6E7EB),
                surfaceContainer = Color.White, surfaceContainerHigh = Color.White, surfaceContainerHighest = Color.White
            )
        }
    }
    MaterialTheme(colorScheme = scheme) {
        val dim = remember(scheme) { PressDim(scheme.onSurface.copy(alpha = if (dark) 0.07f else 0.06f)) }
        @OptIn(ExperimentalFoundationApi::class)
        CompositionLocalProvider(
            LocalSpringy provides springy, LocalIndication provides dim,
            LocalOverscrollConfiguration provides null, content = content
        )
    }
}

@Composable
fun <T> ThemeFade(target: T, enabled: Boolean, content: @Composable (T) -> Unit) {
    val layer = rememberGraphicsLayer()
    var applied by remember { mutableStateOf(target) }
    var shot by remember { mutableStateOf<ImageBitmap?>(null) }
    val fade = remember { Animatable(0f) }
    LaunchedEffect(target) {
        if (target == applied) return@LaunchedEffect
        try {
            if (enabled) {
                shot = runCatching { layer.toImageBitmap() }.getOrNull()
                fade.snapTo(1f)
            }
            applied = target
            if (shot != null) fade.animateTo(0f, tween(460, easing = LinearOutSlowInEasing))
        } finally {
            shot = null
        }
    }
    Box(
        Modifier.fillMaxSize().drawWithContent {
            layer.record { this@drawWithContent.drawContent() }
            drawLayer(layer)
            val s = shot
            if (s != null) drawImage(s, alpha = fade.value.coerceIn(0f, 1f))
        }
    ) { content(applied) }
}

private class PressDim(private val color: Color) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = PressDimNode(interactionSource, color)
    override fun equals(other: Any?) = other is PressDim && other.color == color
    override fun hashCode() = color.hashCode()
}

private class PressDimNode(private val source: InteractionSource, private val color: Color) : Modifier.Node(), DrawModifierNode {
    private val level = Animatable(0f)
    override fun onAttach() {
        coroutineScope.launch {
            source.interactions.collect { i ->
                when (i) {
                    is PressInteraction.Press -> launch { level.animateTo(1f, tween(80)) { invalidateDraw() } }
                    is PressInteraction.Release, is PressInteraction.Cancel ->
                        launch { level.animateTo(0f, tween(260, easing = LinearOutSlowInEasing)) { invalidateDraw() } }
                }
            }
        }
    }
    override fun ContentDrawScope.draw() {
        drawContent()
        val a = level.value
        if (a > 0.001f) drawRect(color.copy(alpha = color.alpha * a))
    }
}

// ═══════════════ Haptics ═══════════════

class Haptics(private val view: View) {
    fun tick() { view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
    fun confirm() {
        view.performHapticFeedback(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY)
    }
    fun toggle(on: Boolean) {
        view.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= 34) (if (on) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF)
            else HapticFeedbackConstants.CLOCK_TICK
        )
    }
    fun lift() { view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) }
    fun step() {
        view.performHapticFeedback(if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.SEGMENT_FREQUENT_TICK else HapticFeedbackConstants.CLOCK_TICK)
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val v = LocalView.current
    return remember(v) { Haptics(v) }
}

// ═══════════════ Layout ═══════════════

@Composable
fun RootBox(content: @Composable BoxScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxSize(), color = cs.background, contentColor = cs.onBackground) {
        Box(Modifier.fillMaxSize(), content = content)
    }
}

@Composable
fun SheetPanel(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surface
    ) { Box(Modifier.fillMaxSize(), content = content) }
}

@Composable
fun bottomSpace(extra: Dp): Dp =
    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + extra

val LocalCardColor = compositionLocalOf<Color?> { null }

@Composable
fun cardColor(): Color = LocalCardColor.current ?: MaterialTheme.colorScheme.surface

@Composable
fun Card(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val src = remember { MutableInteractionSource() }
    val press = pressProgress(src)
    CompositionLocalProvider(LocalIconPress provides press) {
        Column(
            modifier.fillMaxWidth()
                .then(if (onClick != null) Modifier.springClick(CardShape, 0.98f, source = src, onClick = onClick) else Modifier.clip(CardShape))
                .background(cardColor()),
            content = content
        )
    }
}

@Composable
fun CardGroup(modifier: Modifier = Modifier, inset: Dp = 16.dp, content: @Composable () -> Unit) {
    val line = MaterialTheme.colorScheme.outline
    val cuts = remember { mutableListOf<Int>() }
    Layout(
        content = content,
        modifier = modifier.fillMaxWidth().clip(CardShape).background(cardColor()).drawWithContent {
            drawContent()
            val x = inset.toPx()
            val w = 0.6.dp.toPx().coerceAtLeast(1f)
            val end = size.width - 16.dp.toPx()
            cuts.forEach { y -> drawLine(line, Offset(x, y.toFloat()), Offset(end, y.toFloat()), w) }
        }
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minHeight = 0)) }
        val height = placeables.sumOf { it.height }
        layout(constraints.maxWidth, height) {
            cuts.clear()
            var y = 0
            var seen = false
            placeables.forEach { p ->
                if (p.height > 0) {
                    if (seen) cuts += y
                    seen = true
                }
                p.place(0, y)
                y += p.height
            }
        }
    }
}

@Composable
fun GroupLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
fun Chevron(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), down: Boolean = false) {
    val press = LocalIconPress.current
    Canvas(modifier.size(14.dp).graphicsLayer {
        val k = press.value * 3.dp.toPx()
        if (down) translationY = k * 0.6f else translationX = k
    }) {
        val w = size.width
        val sw = 1.7.dp.toPx()
        val p = Path().apply {
            if (down) { moveTo(w * 0.22f, w * 0.38f); lineTo(w * 0.5f, w * 0.66f); lineTo(w * 0.78f, w * 0.38f) }
            else { moveTo(w * 0.38f, w * 0.2f); lineTo(w * 0.66f, w * 0.5f); lineTo(w * 0.38f, w * 0.8f) }
        }
        drawPath(p, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun BackIcon(color: Color = MaterialTheme.colorScheme.onBackground) {
    val press = LocalIconPress.current
    Canvas(Modifier.size(22.dp)) {
        val w = size.width
        val sw = 2.dp.toPx()
        val k = press.value
        val dx = -w * 0.12f * k
        val spread = w * 0.04f * k
        val p = Path().apply {
            moveTo(w * 0.62f + dx, w * 0.18f + spread); lineTo(w * 0.3f + dx, w * 0.5f); lineTo(w * 0.62f + dx, w * 0.82f - spread)
        }
        drawPath(p, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun MenuIcon(color: Color = MaterialTheme.colorScheme.onBackground) {
    val press = LocalIconPress.current
    Canvas(Modifier.size(22.dp)) {
        val w = size.width
        val sw = 2.dp.toPx()
        val k = press.value
        drawLine(color, Offset(w * 0.16f, w * 0.3f), Offset(w * (0.84f - 0.2f * k), w * 0.3f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * 0.16f, w * 0.5f), Offset(w * (0.84f - 0.08f * k), w * 0.5f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * 0.16f, w * 0.7f), Offset(w * (0.56f + 0.28f * k), w * 0.7f), sw, StrokeCap.Round)
    }
}

@Composable
fun LargeTitleScaffold(
    title: String, modifier: Modifier = Modifier, subtitle: String = "",
    onBack: (() -> Unit)? = null, nav: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val block = remember { MutableInteractionSource() }
    val rangePx = with(density) { (if (subtitle.isEmpty()) 50.dp else 72.dp).toPx() }
    var off by remember { mutableFloatStateOf(0f) }
    val springy = LocalSpringy.current
    val conn = remember(rangePx, springy) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y >= 0f) return Offset.Zero
                val next = (off + available.y).coerceIn(-rangePx, 0f)
                val used = next - off
                off = next
                return Offset(0f, used)
            }
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y <= 0f) return Offset.Zero
                val next = (off + available.y).coerceIn(-rangePx, 0f)
                val used = next - off
                off = next
                return Offset(0f, used)
            }
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (off < 0f && off > -rangePx) {
                    val target = if (off < -rangePx / 2f) -rangePx else 0f
                    animate(off, target, animationSpec = if (springy) spring(dampingRatio = 1f, stiffness = 600f) else snap()) { v, _ -> off = v }
                }
                return Velocity.Zero
            }
        }
    }
    val f = (-off / rangePx).coerceIn(0f, 1f)
    Surface(
        modifier = modifier.fillMaxSize().clickable(interactionSource = block, indication = null, onClick = {}),
        color = cs.background, contentColor = cs.onBackground
    ) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxWidth().statusBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when {
                        nav != null -> nav()
                        onBack != null -> SIconButton(onClick = onBack) { BackIcon() }
                        else -> Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp).graphicsLayer {
                            val a = ((f - 0.55f) / 0.45f).coerceIn(0f, 1f)
                            alpha = a
                            translationY = (1f - a) * 6.dp.toPx()
                        }
                    )
                    actions()
                }
                Box(
                    Modifier.fillMaxWidth().height(with(density) { (rangePx + off).coerceAtLeast(0f).toDp() }).clipToBounds()
                ) {
                    Column(
                        Modifier.padding(horizontal = 24.dp).graphicsLayer {
                            alpha = (1f - f * 1.6f).coerceIn(0f, 1f)
                            val sc = 1f - 0.06f * f
                            scaleX = sc
                            scaleY = sc
                            transformOrigin = TransformOrigin(0f, 0f)
                        }
                    ) {
                        Text(title, fontSize = 30.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (subtitle.isNotEmpty()) {
                            Spacer(Modifier.height(2.dp))
                            SwapText(subtitle, fontSize = 14.sp, color = cs.onSurfaceVariant)
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().height(0.6.dp).background(cs.outline.copy(alpha = f)))
            }
            Column(Modifier.weight(1f).fillMaxWidth().rubberBand().nestedScroll(conn), content = content)
        }
    }
}

@Composable
fun AutoHidePill(
    labels: List<String>, pager: PagerState, springy: Boolean, onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier, forceVisible: Boolean = false, startVisible: Boolean = false, slotW: Dp = 96.dp
) {
    var visible by remember { mutableStateOf(startVisible) }
    LaunchedEffect(pager.isScrollInProgress) {
        if (pager.isScrollInProgress) visible = true else { delay(if (startVisible) 2200 else 1100); visible = false }
    }
    val show by animateFloatAsState(
        if (visible || forceVisible) 1f else 0f,
        animationSpec = if (springy) spring(dampingRatio = 0.85f, stiffness = 500f) else tween(200), label = "pill"
    )
    if (show > 0.01f) {
        TabPill(
            labels = labels, pager = pager, onSelect = onSelect, slotW = slotW, enabled = show > 0.6f,
            modifier = modifier.graphicsLayer {
                alpha = show.coerceIn(0f, 1f)
                translationY = (1f - show) * 28.dp.toPx()
                if (springy) { val sc = 0.9f + 0.1f * show; scaleX = sc; scaleY = sc }
            }
        )
    }
}

@Composable
fun rememberToday(): LocalDate {
    var today by remember { mutableStateOf(LocalDate.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(20_000)
            val now = LocalDate.now()
            if (now != today) today = now
        }
    }
    return today
}

@Composable
fun Modifier.coachRing(active: Boolean, shape: Shape = RShape(14.dp)): Modifier {
    if (!active) return this
    val t = rememberInfiniteTransition(label = "coach")
    val a by t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "coachAlpha")
    return this.border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = a), shape)
}

@Composable
fun CoachCard(step: Int, total: Int, text: String, last: Boolean, onNext: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier.padding(horizontal = 16.dp).fillMaxWidth().softShadow(shape, 18.dp)
            .clip(shape).background(elevatedColor()).padding(20.dp)
    ) {
        Text("$step / $total", fontSize = 12.sp, color = cs.primary, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Medium, lineHeight = 22.sp)
        Spacer(Modifier.height(14.dp))
        SButton(onClick = onNext, modifier = Modifier.align(Alignment.End)) { Text(if (last) tx("Got it") else tx("Next tip")) }
    }
}

@Composable
fun ColorDot(c: Color?, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val ring by animateDpAsState(if (selected) 2.dp else 0.dp, spring(dampingRatio = 0.8f, stiffness = 600f), label = "dotW")
    Box(
        Modifier.size(38.dp).then(if (ring > 0.dp) Modifier.border(ring, c ?: cs.onSurfaceVariant, CircleShape) else Modifier).padding(4.dp)
            .springClick(CircleShape, 0.85f, onClick = onClick)
            .background(c ?: cs.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (c == null) Text("–", color = cs.onSurfaceVariant)
        AnimatedVisibility(
            visible = selected && c != null,
            enter = scaleIn(spring(dampingRatio = 0.6f, stiffness = 600f)) + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) { Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
    }
}

@Composable
fun SwipeHint(visible: Boolean, text: String, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "swipeHint")
    val dx by transition.animateFloat(
        initialValue = -6f, targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "wiggle"
    )
    AnimatedVisibility(
        visible = visible, modifier = modifier,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 }
    ) {
        Surface(shape = RShape(18.dp), color = cs.onSurface, contentColor = cs.surface, shadowElevation = 6.dp) {
            Row(
                Modifier.padding(horizontal = 18.dp, vertical = 11.dp).graphicsLayer { translationX = dx * density },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("‹", fontSize = 22.sp)
                Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text("›", fontSize = 22.sp)
            }
        }
    }
}

@Composable
fun TabPill(
    labels: List<String>, pager: PagerState, onSelect: (Int) -> Unit, modifier: Modifier = Modifier,
    slotW: Dp = 104.dp, enabled: Boolean = true
) {
    val cs = MaterialTheme.colorScheme
    val slotH = 40.dp
    val slotPx = with(LocalDensity.current) { slotW.toPx() }
    Surface(
        modifier = modifier.softShadow(RShape(22.dp), 16.dp),
        shape = RShape(22.dp),
        color = elevatedColor(),
        contentColor = cs.onSurface
    ) {
        Box(Modifier.padding(4.dp)) {
            Box(
                Modifier.size(slotW, slotH)
                    .offset { IntOffset(((pager.currentPage + pager.currentPageOffsetFraction) * slotPx).roundToInt(), 0) }
                    .clip(RShape(14.dp)).background(cs.primary)
            )
            Row {
                labels.forEachIndexed { i, label ->
                    Box(
                        Modifier.size(slotW, slotH).springClick(RShape(16.dp), 0.95f, enabled = enabled) { onSelect(i) },
                        contentAlignment = Alignment.Center
                    ) {
                        val f = (1f - abs(pager.currentPage + pager.currentPageOffsetFraction - i)).coerceIn(0f, 1f)
                        Text(
                            label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false,
                            color = lerp(cs.onSurface, cs.onPrimary, f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ClockIcon(size: Dp, color: Color) {
    Canvas(Modifier.size(size)) {
        val w = this.size.minDimension
        val sw = w * 0.1f
        drawCircle(color, radius = w / 2f - sw / 2f, style = Stroke(width = sw))
        drawLine(color, Offset(w / 2f, w * 0.27f), Offset(w / 2f, w / 2f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color, Offset(w / 2f, w / 2f), Offset(w * 0.68f, w * 0.62f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

@Composable
fun ProgressRing(
    progress: Float, color: Color, modifier: Modifier = Modifier, stroke: Dp = 7.dp,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val cs = MaterialTheme.colorScheme
    val springy = LocalSpringy.current
    val p by animateFloatAsState(
        progress.coerceIn(0f, 1f), if (springy) spring(dampingRatio = 1f, stiffness = 90f) else snap(), label = "ring"
    )
    val c by animateColorAsState(color, tween(300), label = "ringColor")
    val track = cs.onSurface.copy(alpha = 0.07f)
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val sw = stroke.toPx()
            val tl = Offset(sw / 2f, sw / 2f)
            val sz = Size(size.width - sw, size.height - sw)
            drawArc(track, 0f, 360f, false, tl, sz, style = Stroke(sw))
            if (p > 0.001f) drawArc(c, -90f, 360f * p, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
        }
        content()
    }
}

// ═══════════════ Motion ═══════════════

val LocalSpringy = compositionLocalOf { true }

const val MOTION_SCALE = 0.72f

fun fluidStiffness(seconds: Float): Float = (2.0 * Math.PI / (seconds * MOTION_SCALE)).let { (it * it).toFloat() }

fun <T> fluid(seconds: Float = 0.4f, bounce: Float = 0f, visibilityThreshold: T? = null): SpringSpec<T> =
    spring(dampingRatio = 1f - bounce, stiffness = fluidStiffness(seconds), visibilityThreshold = visibilityThreshold)

@Composable
fun menuBorder(): BorderStroke =
    BorderStroke(0.7.dp, if (isDarkUi()) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.07f))

@Composable
fun StrikeText(
    text: String, done: Boolean, modifier: Modifier = Modifier, fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null, color: Color = Color.Unspecified, maxLines: Int = Int.MAX_VALUE
) {
    val springy = LocalSpringy.current
    val p by animateFloatAsState(if (done) 1f else 0f, if (springy) fluid(0.42f) else tween(120), label = "strike")
    var layout by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
    val lineColor = (if (color == Color.Unspecified) LocalContentColor.current else color)
    Text(
        text, fontSize = fontSize, fontWeight = fontWeight, color = color, maxLines = maxLines, overflow = TextOverflow.Ellipsis,
        onTextLayout = { layout = it },
        modifier = modifier.drawWithContent {
            drawContent()
            val l = layout ?: return@drawWithContent
            val q = p.coerceIn(0f, 1f)
            if (q <= 0.001f) return@drawWithContent
            val widths = (0 until l.lineCount).map { l.getLineRight(it) - l.getLineLeft(it) }
            var left = widths.sum() * q
            val sw = 1.6.dp.toPx()
            for (i in 0 until l.lineCount) {
                if (left <= 0f) break
                val w = minOf(widths[i], left)
                val y = (l.getLineTop(i) + l.getLineBottom(i)) / 2f + 0.5.dp.toPx()
                val x0 = l.getLineLeft(i)
                drawLine(lineColor.copy(alpha = lineColor.alpha * 0.8f), Offset(x0, y), Offset(x0 + w, y), sw, StrokeCap.Round)
                left -= w
            }
        }
    )
}

fun fluidOffset(seconds: Float = 0.45f, bounce: Float = 0f): SpringSpec<IntOffset> =
    fluid(seconds, bounce, IntOffset.VisibilityThreshold)

fun fluidSize(seconds: Float = 0.45f, bounce: Float = 0f): SpringSpec<IntSize> =
    fluid(seconds, bounce, IntSize.VisibilityThreshold)

private val NoPress: State<Float> = mutableFloatStateOf(0f)
val LocalIconPress = compositionLocalOf { NoPress }

@Composable
fun pressProgress(source: InteractionSource): State<Float> {
    val springy = LocalSpringy.current
    val a = remember { Animatable(0f) }
    LaunchedEffect(source, springy) {
        if (!springy) return@LaunchedEffect
        source.interactions.collect { i ->
            when (i) {
                is PressInteraction.Press -> launch { a.animateTo(1f, fluid(0.16f)) }
                is PressInteraction.Release, is PressInteraction.Cancel -> launch {
                    if (a.value < 0.65f) a.animateTo(1f, fluid(0.1f))
                    a.animateTo(0f, fluid(0.5f, 0.38f))
                }
            }
        }
    }
    return a.asState()
}

@Composable
fun Modifier.pressScale(source: InteractionSource, pressedScale: Float = 0.96f): Modifier {
    val press = pressProgress(source)
    return this.graphicsLayer {
        val sc = 1f - (1f - pressedScale) * press.value
        scaleX = sc
        scaleY = sc
    }
}

@Composable
fun Modifier.springClick(
    shape: Shape = RectangleShape, pressedScale: Float = 0.97f, enabled: Boolean = true,
    source: MutableInteractionSource? = null, onClick: () -> Unit
): Modifier {
    val src = source ?: remember { MutableInteractionSource() }
    return this.pressScale(src, pressedScale).clip(shape)
        .clickable(interactionSource = src, indication = LocalIndication.current, enabled = enabled, onClick = onClick)
}

val LocalScreenActive = compositionLocalOf { true }

@Composable
fun Modifier.fadingEdge(top: Boolean, radius: Dp, fade: Dp = 28.dp): Modifier {
    val c = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDarkUi()) 0.18f else 0.1f)
    return this.drawWithContent {
        drawContent()
        val r = radius.toPx()
        val sw = 1.dp.toPx()
        val h = sw / 2f
        val len = r + fade.toPx()
        val w = size.width
        val path = Path()
        val brush: Brush
        if (top) {
            path.moveTo(h, len)
            path.lineTo(h, r)
            path.arcTo(Rect(h, h, h + 2 * r, h + 2 * r), 180f, 90f, false)
            path.lineTo(w - r - h, h)
            path.arcTo(Rect(w - 2 * r - h, h, w - h, h + 2 * r), 270f, 90f, false)
            path.lineTo(w - h, len)
            brush = Brush.verticalGradient(0f to c, 1f to Color.Transparent, startY = r * 0.5f, endY = len)
        } else {
            val hh = size.height
            path.moveTo(w - len, h)
            path.lineTo(w - r - h, h)
            path.arcTo(Rect(w - 2 * r - h, h, w - h, h + 2 * r), 270f, 90f, false)
            path.lineTo(w - h, hh - r - h)
            path.arcTo(Rect(w - 2 * r - h, hh - 2 * r - h, w - h, hh - h), 0f, 90f, false)
            path.lineTo(w - len, hh - h)
            brush = Brush.horizontalGradient(0f to Color.Transparent, 1f to c, startX = w - len, endX = w - r * 0.5f)
        }
        drawPath(path, brush, style = Stroke(sw))
    }
}

@Composable
fun Modifier.rubberBand(vertical: Boolean = true, external: MutableFloatState? = null): Modifier {
    val springy = LocalSpringy.current
    val limit = with(LocalDensity.current) { 150.dp.toPx() }
    val state = external ?: remember { mutableFloatStateOf(0f) }
    val conn = remember(vertical, limit, springy) {
        object : NestedScrollConnection {
            fun axis(o: Offset) = if (vertical) o.y else o.x
            fun speed(v: Velocity) = if (vertical) v.y else v.x
            fun pack(a: Float) = if (vertical) Offset(0f, a) else Offset(a, 0f)
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val o = state.floatValue
                val a = axis(available)
                if (o == 0f || a == 0f || source != NestedScrollSource.UserInput || (o > 0f) == (a > 0f)) return Offset.Zero
                val next = if (o > 0f) max(0f, o + a) else min(0f, o + a)
                state.floatValue = next
                return pack(next - o)
            }
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                val a = axis(available)
                if (a == 0f || source != NestedScrollSource.UserInput || !springy) return Offset.Zero
                val o = state.floatValue
                val k = (1f - abs(o) / limit).coerceIn(0.06f, 1f) * 0.5f
                state.floatValue = o + a * k
                return pack(a)
            }
            override suspend fun onPreFling(available: Velocity): Velocity {
                val o = state.floatValue
                if (o == 0f) return Velocity.Zero
                animate(o, 0f, 0f, fluid(0.42f)) { v, _ -> state.floatValue = v }
                return available
            }
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val v = speed(available).coerceIn(-5000f, 5000f)
                val o = state.floatValue
                if (!springy || (v == 0f && o == 0f)) return Velocity.Zero
                animate(o, 0f, v * 0.6f, fluid(0.46f)) { x, _ -> state.floatValue = x }
                return available
            }
        }
    }
    return this.nestedScroll(conn).graphicsLayer {
        if (vertical) translationY = state.floatValue else translationX = state.floatValue
    }
}

@Composable
fun Modifier.iconPop(trigger: Any?, depth: Float = 0.74f, turn: Float = 0f): Modifier {
    val springy = LocalSpringy.current
    val s = remember { Animatable(0f) }
    val last = remember { arrayOf(trigger) }
    LaunchedEffect(trigger) {
        if (last[0] == trigger) return@LaunchedEffect
        last[0] = trigger
        if (!springy) return@LaunchedEffect
        s.animateTo(1f, fluid(0.12f))
        s.animateTo(0f, fluid(0.55f, 0.45f))
    }
    return this.graphicsLayer {
        val k = s.value
        val sc = 1f - (1f - depth) * k
        scaleX = sc
        scaleY = sc
        rotationZ = turn * k
    }
}

@Composable
fun CloseIcon(color: Color = LocalContentColor.current, size: Dp = 22.dp) {
    val press = LocalIconPress.current
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val sw = 2.dp.toPx()
        val k = press.value
        val a = w * (0.27f + 0.03f * k)
        val b = w - a
        withTransform({ rotate(90f * k, center) }) {
            drawLine(color, Offset(a, a), Offset(b, b), sw, StrokeCap.Round)
            drawLine(color, Offset(b, a), Offset(a, b), sw, StrokeCap.Round)
        }
    }
}

@Composable
fun SearchIcon(color: Color = LocalContentColor.current, size: Dp = 22.dp) {
    val press = LocalIconPress.current
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val sw = 2.dp.toPx()
        val k = press.value
        val c = Offset(w * 0.44f, w * 0.44f)
        val r = w * (0.24f + 0.03f * k)
        drawCircle(color, r, c, style = Stroke(sw))
        val d = r * 0.7071f
        val len = w * (0.2f + 0.06f * k)
        drawLine(color, Offset(c.x + d, c.y + d), Offset(c.x + d + len * 0.7071f, c.y + d + len * 0.7071f), sw, StrokeCap.Round)
    }
}

@Composable
fun MoreIcon(color: Color = LocalContentColor.current, size: Dp = 22.dp) {
    val press = LocalIconPress.current
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val gap = w * (0.26f + 0.06f * press.value)
        val r = w * 0.085f
        for (i in -1..1) drawCircle(color, r, Offset(w / 2f, w / 2f + gap * i))
    }
}

@Composable
fun SwapText(
    text: String, modifier: Modifier = Modifier, fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null, color: Color = Color.Unspecified
) {
    val springy = LocalSpringy.current
    AnimatedContent(
        targetState = text, modifier = modifier, label = "swap",
        transitionSpec = {
            if (springy) {
                (slideInVertically(fluidOffset(0.36f, 0.12f)) { it / 2 } + fadeIn(fluid(0.24f))) togetherWith
                    (slideOutVertically(fluidOffset(0.32f)) { -it / 2 } + fadeOut(fluid(0.16f))) using
                    SizeTransform(clip = false) { _, _ -> fluidSize(0.36f) }
            } else fadeIn(tween(120)) togetherWith fadeOut(tween(120))
        }
    ) { t -> Text(t, fontSize = fontSize, fontWeight = fontWeight, color = color, style = TextStyle(fontFeatureSettings = "tnum")) }
}

@Composable
fun SButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    val src = remember { MutableInteractionSource() }
    Button(
        onClick = onClick, modifier = modifier.pressScale(src), enabled = enabled, shape = CtrlShape, interactionSource = src,
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp)
    ) { ProvideTextStyle(TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp)) { content() } }
}

@Composable
fun SOutlinedButton(
    onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding, content: @Composable RowScope.() -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val src = remember { MutableInteractionSource() }
    Button(
        onClick = onClick, modifier = modifier.pressScale(src), enabled = enabled, shape = CtrlShape,
        colors = ButtonDefaults.buttonColors(containerColor = cs.onSurface.copy(alpha = 0.06f), contentColor = cs.onSurface),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        contentPadding = contentPadding, interactionSource = src, content = content
    )
}

@Composable
fun STextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    val src = remember { MutableInteractionSource() }
    TextButton(
        onClick = onClick, modifier = modifier.pressScale(src), enabled = enabled, shape = CtrlShape, interactionSource = src
    ) { ProvideTextStyle(TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp)) { content() } }
}

@Composable
fun SIconButton(onClick: () -> Unit, modifier: Modifier = Modifier, filled: Boolean = false, content: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val src = remember { MutableInteractionSource() }
    val press = pressProgress(src)
    Box(
        modifier.size(44.dp).padding(2.dp).springClick(CircleShape, 0.86f, source = src, onClick = onClick)
            .then(if (filled) Modifier.background(cs.surface) else Modifier),
        contentAlignment = Alignment.Center
    ) { CompositionLocalProvider(LocalIconPress provides press) { content() } }
}

@Composable
fun SSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val springy = LocalSpringy.current
    val haptics = rememberHaptics()
    val src = remember { MutableInteractionSource() }
    val press = pressProgress(src)
    val current by rememberUpdatedState(checked)
    val change by rememberUpdatedState(onCheckedChange)
    val x by animateFloatAsState(
        if (checked) 1f else 0f,
        animationSpec = if (springy) fluid(0.34f, 0.16f) else tween(140), label = "switchX"
    )
    val track by animateColorAsState(
        if (checked) cs.primary else cs.onSurface.copy(alpha = 0.13f), if (springy) fluid(0.3f) else tween(140), label = "switchTrack"
    )
    val shadow = Color.Black.copy(alpha = 0.18f)
    Canvas(
        Modifier.size(50.dp, 30.dp).clip(RShape(15.dp))
            .pointerInput(Unit) {
                var moved = 0f
                detectHorizontalDragGestures(
                    onDragStart = { moved = 0f },
                    onHorizontalDrag = { ch, dx ->
                        ch.consume()
                        moved += dx
                        val want = if (moved > 0f) true else if (moved < 0f) false else current
                        if (abs(moved) > 6.dp.toPx() && want != current) { haptics.toggle(want); change(want); moved = 0f }
                    }
                )
            }
            .clickable(interactionSource = src, indication = null) { haptics.toggle(!checked); onCheckedChange(!checked) }
    ) {
        val h = size.height
        val pad = 3.dp.toPx()
        val d = h - pad * 2
        val extra = 6.dp.toPx() * press.value.coerceIn(0f, 1f)
        val travel = size.width - pad * 2 - d - extra
        val left = pad + travel * x
        drawRoundRect(track, cornerRadius = CornerRadius(h / 2f))
        drawRoundRect(shadow, Offset(left, pad + 0.8.dp.toPx()), Size(d + extra, d), CornerRadius(d / 2f))
        drawRoundRect(Color.White, Offset(left, pad), Size(d + extra, d), CornerRadius(d / 2f))
    }
}

@Composable
fun SCheck(checked: Boolean, color: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    val cs = MaterialTheme.colorScheme
    val springy = LocalSpringy.current
    val p by animateFloatAsState(
        if (checked) 1f else 0f, if (springy) fluid(0.36f, 0.12f) else tween(140), label = "check"
    )
    val burst = remember { Animatable(1f) }
    val was = remember { booleanArrayOf(checked) }
    LaunchedEffect(checked) {
        val rising = checked && !was[0]
        was[0] = checked
        if (rising && springy) {
            burst.snapTo(0f)
            burst.animateTo(1f, tween(560, easing = LinearOutSlowInEasing))
        }
    }
    val ring = cs.onSurfaceVariant.copy(alpha = 0.5f)
    val mark = onColor(color)
    Canvas(modifier.size(size)) {
        val w = this.size.minDimension
        val r = w / 2f
        val sw = 1.6.dp.toPx()
        val q = p.coerceIn(0f, 1f)
        val b = burst.value
        if (b < 1f) drawCircle(color.copy(alpha = 0.4f * (1f - b)), r * (1f + 0.62f * b), style = Stroke(sw * 1.4f * (1f - b) + 0.5f))
        drawCircle(lerp(ring, color, q), r - sw / 2f, style = Stroke(sw))
        if (q > 0f) drawCircle(color, r * q)
        val t = ((p - 0.3f) / 0.7f).coerceIn(0f, 1f)
        if (t > 0f) {
            val a = Offset(w * 0.28f, w * 0.52f)
            val b = Offset(w * 0.44f, w * 0.67f)
            val c = Offset(w * 0.73f, w * 0.37f)
            val l1 = (b - a).getDistance()
            val l2 = (c - b).getDistance()
            val len = (l1 + l2) * t
            val path = Path().apply {
                moveTo(a.x, a.y)
                if (len <= l1) lineTo(a.x + (b.x - a.x) * len / l1, a.y + (b.y - a.y) * len / l1)
                else {
                    lineTo(b.x, b.y)
                    val k = (len - l1) / l2
                    lineTo(b.x + (c.x - b.x) * k, b.y + (c.y - b.y) * k)
                }
            }
            drawPath(path, mark, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val springy = LocalSpringy.current
    val haptics = rememberHaptics()
    val density = LocalDensity.current
    val pos by animateFloatAsState(
        selected.toFloat(), if (springy) fluid(0.38f, 0.14f) else tween(160), label = "seg"
    )
    val dark = cs.background.luminance() < 0.5f
    val thumb = if (dark) cs.onSurface.copy(alpha = 0.16f).compositeOver(cs.surface) else cs.surface
    var w by remember { mutableIntStateOf(0) }
    val n = options.size.coerceAtLeast(1)
    val slot = ((w - with(density) { 6.dp.toPx() }) / n).coerceAtLeast(0f)
    Box(
        modifier.fillMaxWidth().height(38.dp).clip(RShape(19.dp)).background(cs.onSurface.copy(alpha = 0.06f))
            .onSizeChanged { w = it.width }.padding(3.dp)
    ) {
        Box(
            Modifier.fillMaxHeight().width(with(density) { slot.toDp() })
                .offset { IntOffset((pos * slot).roundToInt(), 0) }
                .softShadow(RShape(16.dp), 3.dp).clip(RShape(16.dp)).background(thumb)
        )
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { i, label ->
                val on = i == selected
                val fg by animateColorAsState(if (on) cs.onSurface else cs.onSurfaceVariant, label = "segFg")
                Box(
                    Modifier.weight(1f).fillMaxHeight().clip(RShape(16.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            if (i != selected) { haptics.tick(); onSelect(i) }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label, fontSize = 13.sp, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun SSlider(value: Float, onChange: (Float) -> Unit, from: Float, to: Float, modifier: Modifier = Modifier, step: Float = 0f) {
    val cs = MaterialTheme.colorScheme
    val haptics = rememberHaptics()
    var w by remember { mutableStateOf(0) }
    val knobPx = with(LocalDensity.current) { 24.dp.toPx() }
    val current by rememberUpdatedState(onChange)
    val now by rememberUpdatedState(value)
    fun setFromX(x: Float) {
        val span = (w - knobPx).coerceAtLeast(1f)
        val f = ((x - knobPx / 2f) / span).coerceIn(0f, 1f)
        var v = from + f * (to - from)
        if (step > 0f) v = (from + Math.round((v - from) / step) * step).coerceIn(from, to)
        if (step > 0f && v != now) haptics.step()
        current(v)
    }
    val frac = ((value - from) / (to - from)).coerceIn(0f, 1f)
    val track = cs.onSurface.copy(alpha = 0.1f)
    val active = cs.primary
    Canvas(
        modifier.fillMaxWidth().height(40.dp).onSizeChanged { w = it.width }
            .pointerInput(from, to, step) { detectTapGestures { setFromX(it.x) } }
            .pointerInput(from, to, step) { detectHorizontalDragGestures { change, _ -> change.consume(); setFromX(change.position.x) } }
    ) {
        val cy = size.height / 2f
        val th = 4.dp.toPx()
        val cx = knobPx / 2f + frac * (size.width - knobPx)
        drawRoundRect(track, Offset(0f, cy - th / 2f), Size(size.width, th), CornerRadius(th / 2f))
        drawRoundRect(active, Offset(0f, cy - th / 2f), Size(cx, th), CornerRadius(th / 2f))
        drawCircle(Color.Black.copy(alpha = 0.16f), knobPx / 2f, Offset(cx, cy + 1.dp.toPx()))
        drawCircle(Color.White, knobPx / 2f, Offset(cx, cy))
    }
}

// ═══════════════ Menus ═══════════════

@Composable
fun MenuButton(
    label: String, modifier: Modifier = Modifier, fill: Boolean = false,
    onBeforeOpen: () -> Boolean = { true },
    leading: (@Composable () -> Unit)? = null,
    menu: @Composable ColumnScope.(close: () -> Unit) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    val rot by animateFloatAsState(if (open) 180f else 0f, fluid(0.4f, 0.2f), label = "menuArrow")
    Box(modifier) {
        Row(
            if (fill) Modifier.fillMaxWidth().springClick(CardShape, 0.98f) { if (onBeforeOpen()) open = true }
                .background(cardColor()).padding(horizontal = 16.dp, vertical = 16.dp)
            else Modifier.springClick(RShape(12.dp), 0.95f) { if (onBeforeOpen()) open = true }
                .padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading != null) { leading(); Spacer(Modifier.width(10.dp)) }
            Text(
                label, maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontSize = if (fill) 16.sp else 14.sp,
                fontWeight = if (fill) FontWeight.Medium else FontWeight.Normal,
                color = if (fill) cs.onSurface else cs.onSurfaceVariant,
                modifier = if (fill) Modifier.weight(1f) else Modifier.widthIn(max = 150.dp)
            )
            Spacer(Modifier.width(4.dp))
            Chevron(Modifier.graphicsLayer { rotationZ = rot }, down = true)
        }
        DropdownMenu(
            expanded = open, onDismissRequest = { open = false },
            containerColor = elevatedColor(), shape = RoundedCornerShape(18.dp), shadowElevation = 3.dp,
            border = menuBorder()
        ) { menu { open = false } }
    }
}

// ═══════════════ Dialogs ═══════════════

@Composable
fun CDialog(
    title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit,
    dismiss: String = tx("Cancel"), onDismissButton: () -> Unit = onDismiss, destructive: Boolean = false
) {
    val cs = MaterialTheme.colorScheme
    val shown = remember { MutableTransitionState(false).apply { targetState = true } }
    var after by remember { mutableStateOf<(() -> Unit)?>(null) }
    fun closeThen(action: () -> Unit) {
        if (after != null) return
        after = action
        shown.targetState = false
    }
    LaunchedEffect(shown.isIdle, shown.currentState) {
        if (shown.isIdle && !shown.currentState) after?.let { a -> after = null; a() }
    }
    Dialog(onDismissRequest = { closeThen(onDismiss) }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        AnimatedVisibility(
            visibleState = shown,
            enter = fadeIn(fluid(0.22f)) + scaleIn(fluid(0.4f, 0.18f), initialScale = 0.9f),
            exit = fadeOut(fluid(0.18f)) + scaleOut(fluid(0.26f), targetScale = 0.94f)
        ) {
            Column(
                Modifier.padding(horizontal = 24.dp).widthIn(max = 420.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp)).background(elevatedColor()).padding(top = 24.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                if (text.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text, fontSize = 14.sp, lineHeight = 20.sp, color = cs.onSurfaceVariant, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
                Spacer(Modifier.height(22.dp))
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DialogButton(dismiss, cs.onSurface.copy(alpha = 0.06f), cs.onSurface, Modifier.weight(1f)) { closeThen(onDismissButton) }
                    DialogButton(
                        confirm, if (destructive) RED.copy(alpha = 0.12f) else cs.primary,
                        if (destructive) RED else cs.onPrimary, Modifier.weight(1f)
                    ) { closeThen(onConfirm) }
                }
            }
        }
    }
}

@Composable
private fun DialogButton(label: String, bg: Color, fg: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(46.dp).springClick(RShape(16.dp), 0.96f, onClick = onClick).background(bg),
        contentAlignment = Alignment.Center
    ) { Text(label, color = fg, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
}

// ═══════════════ ColorPicker ═══════════════

private fun hsvToArgb(h: Float, s: Float, v: Float): Int =
    android.graphics.Color.HSVToColor(floatArrayOf(h.coerceIn(0f, 359.9f), s.coerceIn(0f, 1f), v.coerceIn(0f, 1f)))

private fun argbToHex(argb: Int): String = String.format("#%06X", argb and 0xFFFFFF)

private val HUE_COLORS = (0..6).map { Color(android.graphics.Color.HSVToColor(floatArrayOf((it * 60f).coerceAtMost(359.9f), 1f, 1f))) }

@Composable
fun ColorPickerDialog(initial: Color?, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val start = (initial ?: Color(0xFF2F6BFF)).toArgb()
    val hsv0 = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(start, it) } }
    var h by remember { mutableStateOf(hsv0[0]) }
    var s by remember { mutableStateOf(hsv0[1]) }
    var v by remember { mutableStateOf(hsv0[2]) }
    var hex by remember { mutableStateOf(argbToHex(start)) }
    val current = hsvToArgb(h, s, v)

    fun fromPalette(nh: Float, ns: Float, nv: Float) {
        h = nh; s = ns; v = nv
        hex = argbToHex(hsvToArgb(h, s, v))
    }

    AlertDialog(
        containerColor = cs.surface, shape = RoundedCornerShape(28.dp),
        onDismissRequest = onDismiss,
        title = { Text(tx("Custom color"), fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(16.dp))
                        .pointerInput(Unit) {
                            detectTapGestures { o ->
                                fromPalette(h, (o.x / size.width).coerceIn(0f, 1f), 1f - (o.y / size.height).coerceIn(0f, 1f))
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { o ->
                                    fromPalette(h, (o.x / size.width).coerceIn(0f, 1f), 1f - (o.y / size.height).coerceIn(0f, 1f))
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    fromPalette(
                                        h, (change.position.x / size.width).coerceIn(0f, 1f),
                                        1f - (change.position.y / size.height).coerceIn(0f, 1f)
                                    )
                                }
                            )
                        }
                ) {
                    Canvas(Modifier.matchParentSize()) {
                        drawRect(Brush.horizontalGradient(listOf(Color.White, Color(hsvToArgb(h, 1f, 1f)))))
                        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                        val c = Offset(s * size.width, (1f - v) * size.height)
                        drawCircle(Color.Black.copy(alpha = 0.3f), 12.dp.toPx(), c, style = Stroke(3.dp.toPx()))
                        drawCircle(Color.White, 11.dp.toPx(), c, style = Stroke(2.5.dp.toPx()))
                    }
                }
                Box(Modifier.fillMaxWidth().height(30.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.fillMaxWidth().height(12.dp).clip(CircleShape).background(Brush.horizontalGradient(HUE_COLORS)))
                    Slider(
                        value = h, onValueChange = { fromPalette(it, s, v) }, valueRange = 0f..359f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White, activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent,
                            activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent
                        )
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(Color(current)))
                    OutlinedTextField(
                        value = hex,
                        onValueChange = { t ->
                            hex = t.filter { ch -> ch.isLetterOrDigit() || ch == '#' }.take(7).uppercase()
                            val digits = hex.removePrefix("#")
                            if (digits.length == 6 && digits.all { ch -> ch in "0123456789ABCDEF" }) {
                                val parsed = android.graphics.Color.parseColor("#$digits")
                                val arr = FloatArray(3)
                                android.graphics.Color.colorToHSV(parsed, arr)
                                h = arr[0]; s = arr[1]; v = arr[2]
                            }
                        },
                        label = { Text(tx("Hex code")) }, singleLine = true, shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = { STextButton(onClick = { onPick(current) }) { Text(tx("Use")) } },
        dismissButton = { STextButton(onClick = onDismiss) { Text(tx("Cancel"), color = cs.onSurfaceVariant) } }
    )
}

@Composable
fun CustomColorDot(color: Int?, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val ring by animateDpAsState(if (selected) 2.dp else 0.dp, label = "customW")
    Box(
        Modifier.size(38.dp).then(if (ring > 0.dp) Modifier.border(ring, color?.let { Color(it) } ?: cs.onSurfaceVariant, CircleShape) else Modifier).padding(4.dp)
            .springClick(CircleShape, 0.85f, onClick = onClick)
            .background(if (selected && color != null) Brush.linearGradient(listOf(Color(color), Color(color))) else Brush.sweepGradient(HUE_COLORS + HUE_COLORS.first())),
        contentAlignment = Alignment.Center
    ) {
        if (selected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
        else Text("+", color = Color.White, fontWeight = FontWeight.Bold)
    }
}
