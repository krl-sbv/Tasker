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

// ═══════════════ Theme ═══════════════

val GREEN = Color(0xFF3DDC84)
val RED = Color(0xFFFF6B6B)
val CtrlShape: RoundedCornerShape get() = RShape(14.dp)

fun RShape(r: Dp): RoundedCornerShape = if (r < 24.dp) RoundedCornerShape(50) else RoundedCornerShape(r)

@Composable
fun Modifier.softShadow(shape: Shape, elevation: Dp = 14.dp): Modifier {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val c = if (dark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.55f)
    return this.shadow(elevation, shape, clip = false, ambientColor = c, spotColor = c)
}

val TASK_COLORS = listOf(
    Color(0xFFFF6B6B), Color(0xFFFFA24D), Color(0xFFF5C542), Color(0xFF3DDC84),
    Color(0xFF2EC4B6), Color(0xFF4D9BFF), Color(0xFFA78BFA), Color(0xFFFF7EB6)
)

@Composable
fun TrackerTheme(dark: Boolean, oled: Boolean, springy: Boolean, content: @Composable () -> Unit) {
    val scheme = if (dark) {
        darkColorScheme(
            primary = Color(0xFFF5F5F7), onPrimary = Color(0xFF111111),
            tertiary = Color(0xFFFF9A6B),
            background = if (oled) Color.Black else Color(0xFF0E0E10), onBackground = Color(0xFFF2F2F5),
            surface = if (oled) Color(0xFF0B0B0B) else Color(0xFF1A1A1E), onSurface = Color(0xFFF2F2F5),
            surfaceVariant = if (oled) Color(0xFF161616) else Color(0xFF26262B),
            onSurfaceVariant = Color(0xFF8E8E98), outline = Color(0xFF3A3A42)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF111111), onPrimary = Color.White,
            tertiary = Color(0xFFFF7A45),
            background = Color(0xFFF1F1F4), onBackground = Color(0xFF111111),
            surface = Color.White, onSurface = Color(0xFF111111),
            surfaceVariant = Color(0xFFF4F4F7), onSurfaceVariant = Color(0xFF8A8A93),
            outline = Color(0xFFD6D6DC)
        )
    }
    MaterialTheme(colorScheme = animateScheme(scheme, springy)) {
        CompositionLocalProvider(LocalSpringy provides springy, content = content)
    }
}

@Composable
fun animateScheme(t: ColorScheme, springy: Boolean): ColorScheme {
    val spec: AnimationSpec<Color> = if (springy) tween(350) else snap()
    return t.copy(
        primary = animateColorAsState(t.primary, spec, label = "c1").value,
        onPrimary = animateColorAsState(t.onPrimary, spec, label = "c2").value,
        tertiary = animateColorAsState(t.tertiary, spec, label = "c3").value,
        background = animateColorAsState(t.background, spec, label = "c4").value,
        onBackground = animateColorAsState(t.onBackground, spec, label = "c5").value,
        surface = animateColorAsState(t.surface, spec, label = "c6").value,
        onSurface = animateColorAsState(t.onSurface, spec, label = "c7").value,
        surfaceVariant = animateColorAsState(t.surfaceVariant, spec, label = "c8").value,
        onSurfaceVariant = animateColorAsState(t.onSurfaceVariant, spec, label = "c9").value,
        outline = animateColorAsState(t.outline, spec, label = "c10").value
    )
}

// ═══════════════ Components ═══════════════

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
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        color = MaterialTheme.colorScheme.surface
    ) { Box(Modifier.fillMaxSize(), content = content) }
}

@Composable
fun bottomSpace(extra: Dp): Dp =
    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + extra

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
        animationSpec = if (springy) spring(dampingRatio = 0.65f, stiffness = 420f) else tween(200), label = "pill"
    )
    if (show > 0.01f) {
        TabPill(
            labels = labels, pager = pager, onSelect = onSelect, slotW = slotW, enabled = show > 0.6f,
            modifier = modifier.graphicsLayer {
                alpha = show.coerceIn(0f, 1f)
                translationY = (1f - show) * 36.dp.toPx()
                if (springy) { val sc = 0.75f + 0.25f * show; scaleX = sc; scaleY = sc }
            }
        )
    }
}

@Composable
fun PageIndicator(
    pager: PagerState, springy: Boolean, onSelect: (Int) -> Unit,
    forceVisible: Boolean = false, modifier: Modifier = Modifier
) = AutoHidePill(
    listOf(tx("Calendar"), tx("Today"), tx("Profile")), pager, springy, onSelect, modifier, forceVisible
)

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
    val a by t.animateFloat(0.2f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "coachAlpha")
    return this.border(2.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = a), shape)
}

@Composable
fun CoachCard(step: Int, total: Int, text: String, last: Boolean, onNext: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier.padding(horizontal = 20.dp).fillMaxWidth().softShadow(shape, 16.dp)
            .clip(shape).background(cs.surfaceVariant).border(1.dp, cs.outline.copy(alpha = 0.7f), shape).padding(18.dp)
    ) {
        Text("$step / $total", fontSize = 12.sp, color = cs.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(12.dp))
        SButton(onClick = onNext, modifier = Modifier.align(Alignment.End)) { Text(if (last) tx("Got it") else tx("Next tip")) }
    }
}

@Composable
fun Avatar(name: String, version: Long, size: Dp, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val bmp by produceState<ImageBitmap?>(initialValue = null, version) {
        value = withContext(Dispatchers.IO) {
            val f = File(ctx.filesDir, "pfp.jpg")
            if (f.exists()) BitmapFactory.decodeFile(f.path)?.asImageBitmap() else null
        }
    }
    Box(modifier.size(size).clip(CircleShape).background(cs.primary), contentAlignment = Alignment.Center) {
        Crossfade(targetState = bmp, animationSpec = tween(350), label = "avatar") { b ->
            if (b != null) {
                Image(bitmap = b, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Text(
                    name.firstOrNull()?.uppercase() ?: "?", color = cs.onPrimary,
                    fontSize = (size.value * 0.4f).sp, fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

fun saveProfilePhoto(ctx: Context, uri: Uri): Boolean {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: return false
        val orientation = ctx.contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        val m = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
        }
        val side = minOf(bmp.width, bmp.height)
        val square = Bitmap.createBitmap(bmp, (bmp.width - side) / 2, (bmp.height - side) / 2, side, side, m, true)
        val out = Bitmap.createScaledBitmap(square, 512, 512, true)
        File(ctx.filesDir, "pfp.jpg").outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        true
    } catch (e: Exception) {
        false
    }
}

@Composable
fun ModeChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val shape = RShape(12.dp)
    val bg by animateColorAsState(if (selected) cs.primary else cs.surface, label = "chipBg")
    val fg by animateColorAsState(if (selected) cs.onPrimary else cs.onSurface, label = "chipFg")
    val line by animateColorAsState(if (selected) cs.primary else cs.outline, label = "chipLine")
    Box(
        Modifier.springClick(shape, 0.92f, onClick = onClick).background(bg).border(1.dp, line, shape)
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) { Text(text, color = fg, fontSize = 14.sp) }
}

@Composable
fun ColorDot(c: Color?, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val bw by animateDpAsState(if (selected) 2.5.dp else 1.dp, label = "dotW")
    val bc by animateColorAsState(if (selected) cs.onSurface else cs.outline, label = "dotC")
    Box(
        Modifier.size(36.dp).springClick(RShape(11.dp), 0.85f, onClick = onClick)
            .background(c ?: Color.Transparent).border(bw, bc, RShape(11.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (c == null) Text("–", color = cs.onSurfaceVariant)
        AnimatedVisibility(
            visible = selected && c != null,
            enter = scaleIn(spring(dampingRatio = 0.5f, stiffness = 500f)) + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) { Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
    }
}

@Composable
fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(5.dp))
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun EditableName(name: String, onName: (String) -> Unit) {
    val cs = MaterialTheme.colorScheme
    var editing by remember { mutableStateOf(false) }
    var field by remember { mutableStateOf(TextFieldValue(name)) }
    var gotFocus by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    fun finish() {
        val t = field.text.trim()
        if (t.isNotEmpty() && t != name) onName(t)
        editing = false
        gotFocus = false
    }

    if (editing) {
        BasicTextField(
            value = field,
            onValueChange = { field = it.copy(text = it.text.take(24)) },
            singleLine = true,
            textStyle = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = cs.onBackground),
            cursorBrush = SolidColor(cs.onBackground),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier.fillMaxWidth().focusRequester(focus).onFocusChanged { f ->
                if (f.isFocused) gotFocus = true else if (gotFocus) finish()
            }
        )
        LaunchedEffect(Unit) { focus.requestFocus(); keyboard?.show() }
    } else {
        Box(
            Modifier.springClick(RShape(10.dp), 0.97f) {
                field = TextFieldValue(name, TextRange(0, name.length))
                editing = true
            }
        ) { SwapText(name, fontSize = 28.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun SwipeHint(visible: Boolean, text: String, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "swipeHint")
    val dx by transition.animateFloat(
        initialValue = -8f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "wiggle"
    )
    AnimatedVisibility(
        visible = visible, modifier = modifier,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 }
    ) {
        Surface(shape = RShape(18.dp), color = cs.onSurface, contentColor = cs.surface, shadowElevation = 8.dp) {
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
    val slotH = 42.dp
    val slotPx = with(LocalDensity.current) { slotW.toPx() }
    Surface(
        modifier = modifier.softShadow(RShape(22.dp), 12.dp),
        shape = RShape(22.dp),
        color = cs.surfaceVariant.copy(alpha = 0.92f),
        contentColor = cs.onSurface,
        border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.7f))
    ) {
        Box(Modifier.padding(6.dp)) {
            Box(
                Modifier.size(slotW, slotH)
                    .offset { IntOffset(((pager.currentPage + pager.currentPageOffsetFraction) * slotPx).roundToInt(), 0) }
                    .padding(2.dp)
                    .border(1.5.dp, cs.onSurface, RShape(13.dp))
            )
            Row {
                labels.forEachIndexed { i, label ->
                    Box(
                        Modifier.size(slotW, slotH).springClick(RShape(16.dp), 0.95f, enabled = enabled) { onSelect(i) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false,
                            modifier = Modifier.graphicsLayer {
                                val f = (1f - abs(pager.currentPage + pager.currentPageOffsetFraction - i)).coerceIn(0f, 1f)
                                alpha = 0.5f + 0.5f * f
                            }
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
        val sw = w * 0.12f
        drawCircle(color, radius = w / 2f - sw / 2f, style = Stroke(width = sw))
        drawLine(color, Offset(w / 2f, w * 0.27f), Offset(w / 2f, w / 2f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color, Offset(w / 2f, w / 2f), Offset(w * 0.68f, w * 0.62f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

@Composable
fun TimeBadge(time: String, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val shape = RShape(8.dp)
    Row(
        modifier.clip(shape).border(1.dp, cs.outline.copy(alpha = 0.7f), shape).padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ClockIcon(11.dp, cs.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        Text(time, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = cs.onSurfaceVariant)
    }
}

// ═══════════════ Motion ═══════════════

val LocalSpringy = compositionLocalOf { true }

@Composable
fun Modifier.pressScale(source: InteractionSource, pressedScale: Float = 0.92f): Modifier {
    val springy = LocalSpringy.current
    val pressed by source.collectIsPressedAsState()
    val sc by animateFloatAsState(
        if (pressed && springy) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f), label = "press"
    )
    return this.graphicsLayer { scaleX = sc; scaleY = sc }
}

@Composable
fun Modifier.springClick(
    shape: Shape = RectangleShape, pressedScale: Float = 0.97f, enabled: Boolean = true, onClick: () -> Unit
): Modifier {
    val src = remember { MutableInteractionSource() }
    return this.pressScale(src, pressedScale).clip(shape)
        .clickable(interactionSource = src, indication = LocalIndication.current, enabled = enabled, onClick = onClick)
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
                (slideInVertically(spring(dampingRatio = 0.8f, stiffness = 500f, visibilityThreshold = IntOffset.VisibilityThreshold)) { it / 2 } +
                    fadeIn(spring(stiffness = 600f))) togetherWith
                    (slideOutVertically(spring(dampingRatio = 0.8f, stiffness = 500f, visibilityThreshold = IntOffset.VisibilityThreshold)) { -it / 2 } +
                        fadeOut(spring(stiffness = 600f)))
            } else fadeIn(tween(120)) togetherWith fadeOut(tween(120))
        }
    ) { t -> Text(t, fontSize = fontSize, fontWeight = fontWeight, color = color) }
}

@Composable
fun Staggered(index: Int, seen: MutableSet<Int>, content: @Composable () -> Unit) {
    val springy = LocalSpringy.current
    var shown by remember { mutableStateOf(!springy || index in seen) }
    LaunchedEffect(Unit) {
        if (!shown) {
            delay(index.coerceAtMost(10) * 40L)
            seen += index
            shown = true
        }
    }
    AnimatedVisibility(
        visible = shown,
        enter = fadeIn(spring(stiffness = 400f)) +
            slideInVertically(spring(dampingRatio = 0.8f, stiffness = 380f, visibilityThreshold = IntOffset.VisibilityThreshold)) { it / 3 }
    ) { content() }
}

@Composable
fun SButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    val src = remember { MutableInteractionSource() }
    Button(onClick = onClick, modifier = modifier.pressScale(src), enabled = enabled, shape = CtrlShape, interactionSource = src, content = content)
}

@Composable
fun SOutlinedButton(
    onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding, content: @Composable RowScope.() -> Unit
) {
    val src = remember { MutableInteractionSource() }
    OutlinedButton(
        onClick = onClick, modifier = modifier.pressScale(src), enabled = enabled, shape = CtrlShape,
        contentPadding = contentPadding, interactionSource = src, content = content
    )
}

@Composable
fun STextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    val src = remember { MutableInteractionSource() }
    TextButton(onClick = onClick, modifier = modifier.pressScale(src), enabled = enabled, shape = CtrlShape, interactionSource = src, content = content)
}

@Composable
fun SIconButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.size(44.dp).springClick(CtrlShape, 0.8f, onClick = onClick), contentAlignment = Alignment.Center) { content() }
}

@Composable
fun SSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val springy = LocalSpringy.current
    val shape = RShape(11.dp)
    val x by animateFloatAsState(
        if (checked) 1f else 0f,
        animationSpec = if (springy) spring(dampingRatio = 0.6f, stiffness = 500f) else tween(120), label = "switchX"
    )
    val track by animateColorAsState(if (checked) cs.primary else cs.surfaceVariant, label = "switchTrack")
    val knob by animateColorAsState(if (checked) cs.onPrimary else cs.onSurfaceVariant, label = "switchKnob")
    Box(
        Modifier.size(52.dp, 30.dp).springClick(shape, 0.94f) { onCheckedChange(!checked) }
            .background(track).border(1.dp, cs.outline, shape)
    ) {
        Box(
            Modifier.offset { IntOffset((x * 22.dp.toPx()).roundToInt(), 0) }
                .padding(3.dp).size(24.dp).clip(RShape(8.dp)).background(knob)
        )
    }
}

@Composable
fun SSlider(value: Float, onChange: (Float) -> Unit, from: Float, to: Float, modifier: Modifier = Modifier, step: Float = 0f) {
    val cs = MaterialTheme.colorScheme
    var w by remember { mutableStateOf(0) }
    val knob = 26.dp
    val knobPx = with(LocalDensity.current) { knob.toPx() }
    val current by rememberUpdatedState(onChange)
    fun setFromX(x: Float) {
        val span = (w - knobPx).coerceAtLeast(1f)
        val f = ((x - knobPx / 2f) / span).coerceIn(0f, 1f)
        var v = from + f * (to - from)
        if (step > 0f) v = (from + Math.round((v - from) / step) * step).coerceIn(from, to)
        current(v)
    }
    val frac = ((value - from) / (to - from)).coerceIn(0f, 1f)
    Box(
        modifier.fillMaxWidth().height(40.dp).onSizeChanged { w = it.width }
            .pointerInput(from, to, step) { detectTapGestures { setFromX(it.x) } }
            .pointerInput(from, to, step) { detectHorizontalDragGestures { change, _ -> change.consume(); setFromX(change.position.x) } },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(Modifier.fillMaxWidth().height(10.dp).clip(RShape(5.dp)).background(cs.surfaceVariant).border(1.dp, cs.outline, RShape(5.dp)))
        Box(
            Modifier.offset { IntOffset(0, 0) }.height(10.dp)
                .width(with(LocalDensity.current) { (frac * (w - knobPx) + knobPx / 2f).toDp() })
                .clip(RShape(5.dp)).background(cs.primary)
        )
        Box(
            Modifier.offset { IntOffset((frac * (w - knobPx)).roundToInt(), 0) }
                .size(knob).clip(RShape(9.dp)).background(cs.primary)
                .border(2.dp, cs.surface, RShape(9.dp))
        )
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
    val rot by animateFloatAsState(if (open) 180f else 0f, spring(dampingRatio = 0.7f, stiffness = 400f), label = "menuArrow")
    Box(modifier) {
        SOutlinedButton(onClick = { if (onBeforeOpen()) open = true }, modifier = if (fill) Modifier.fillMaxWidth() else Modifier) {
            if (leading != null) { leading(); Spacer(Modifier.width(8.dp)) }
            if (fill) Text(label, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            else Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(8.dp))
            Text("▾", modifier = Modifier.graphicsLayer { rotationZ = rot })
        }
        DropdownMenu(
            expanded = open, onDismissRequest = { open = false },
            containerColor = cs.surface, shape = RoundedCornerShape(18.dp)
        ) { menu { open = false } }
    }
}

// ═══════════════ ColorPicker ═══════════════

private fun hsvToArgb(h: Float, s: Float, v: Float): Int =
    android.graphics.Color.HSVToColor(floatArrayOf(h.coerceIn(0f, 359.9f), s.coerceIn(0f, 1f), v.coerceIn(0f, 1f)))

private fun argbToHex(argb: Int): String = String.format("#%06X", argb and 0xFFFFFF)

private val HUE_COLORS = (0..6).map { Color(android.graphics.Color.HSVToColor(floatArrayOf((it * 60f).coerceAtMost(359.9f), 1f, 1f))) }

@Composable
fun ColorPickerDialog(initial: Color?, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val start = (initial ?: Color(0xFF4D9BFF)).toArgb()
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
        containerColor = cs.surface,
        onDismissRequest = onDismiss,
        title = { Text(tx("Custom color")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    Modifier.fillMaxWidth().height(170.dp).clip(RShape(14.dp))
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
                        drawCircle(Color.Black, 11.dp.toPx(), c, style = Stroke(3.dp.toPx()))
                        drawCircle(Color.White, 10.dp.toPx(), c, style = Stroke(2.dp.toPx()))
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
                    Box(Modifier.size(44.dp).clip(RShape(12.dp)).background(Color(current)).border(1.dp, cs.outline, RShape(12.dp)))
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
                        label = { Text(tx("Hex code")) }, singleLine = true, modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = { STextButton(onClick = { onPick(current) }) { Text(tx("Use")) } },
        dismissButton = { STextButton(onClick = onDismiss) { Text(tx("Cancel")) } }
    )
}

@Composable
fun CustomColorDot(color: Int?, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val bw by animateDpAsState(if (selected) 2.5.dp else 1.dp, label = "customW")
    val bc by animateColorAsState(if (selected) cs.onSurface else cs.outline, label = "customC")
    Box(
        Modifier.size(36.dp).springClick(RShape(11.dp), 0.85f, onClick = onClick)
            .background(if (selected && color != null) Brush.linearGradient(listOf(Color(color), Color(color))) else Brush.sweepGradient(HUE_COLORS + HUE_COLORS.first()))
            .border(bw, bc, RShape(11.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (selected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
        else Text("+", color = Color.White, fontWeight = FontWeight.Bold)
    }
}
