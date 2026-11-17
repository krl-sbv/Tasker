package com.kirillshybaiev.dailytracker

import android.app.AlarmManager
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.AndroidViewModel
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JTextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

// ═══════════════ Models ═══════════════

data class Task(
    val id: String,
    val title: String,
    val description: String = "",
    val repeating: Boolean,
    val date: LocalDate,
    val time: LocalTime? = null,
    val color: Int? = null,
    val interval: Int = 1,
    val weekdays: Int = 0,
    val shifts: List<Pair<LocalDate, Int>> = emptyList()
)

data class DoneRecord(val taskId: String, val title: String, val date: LocalDate, val repeating: Boolean)

data class AppState(
    val tasks: List<Task> = emptyList(),
    val history: List<DoneRecord> = emptyList(),
    val skips: Set<String> = emptySet(),
    val name: String = "Friend",
    val theme: Int = 0,
    val springy: Boolean = true,
    val showQuote: Boolean = true,
    val weekStartDay: Int = 1,
    val country: String = "AUTO",
    val language: String = "",
    val onboarded: Boolean = false,
    val tipsDone: Boolean = false,
    val blurMode: Int = 1,
    val showHolidays: Boolean = true,
    val nudgeOn: Boolean = true,
    val reviewOn: Boolean = true,
    val hintSeen: Boolean = false,
    val seenMainSwipe: Boolean = false,
    val seenInsightsSwipe: Boolean = false,
    val pfpVersion: Long = 0L
) {
    private val doneKeys: Set<String> by lazy { history.map { "${it.taskId}|${it.date}" }.toSet() }
    fun isDone(task: Task, date: LocalDate) = "${task.id}|$date" in doneKeys
    fun isSkipped(task: Task, date: LocalDate) = "${task.id}|$date" in skips

    fun holidayCountry(): String =
        if (country == "AUTO") Lang.deviceLocale.country.uppercase() else country
}

// ───────────── schedule ─────────────

fun Task.occursOn(d: LocalDate): Boolean {
    if (!repeating) return date == d
    return occursShifted(d, shifts.size)
}

private fun Task.baseOccursOn(d: LocalDate): Boolean {
    if (d.isBefore(date)) return false
    return if (weekdays != 0) ((weekdays shr (d.dayOfWeek.value - 1)) and 1) == 1
    else ChronoUnit.DAYS.between(date, d) % interval == 0L
}

private fun Task.occursShifted(d: LocalDate, n: Int): Boolean {
    if (n == 0) return baseOccursOn(d)
    val (from, days) = shifts[n - 1]
    return (d.isBefore(from) && occursShifted(d, n - 1)) ||
        (!d.isBefore(from.plusDays(days.toLong())) && occursShifted(d.minusDays(days.toLong()), n - 1))
}

fun intervalLabel(n: Int): String = when (n) {
    1 -> "Every day"
    2 -> "Every other day"
    7 -> "Every week"
    else -> "Every $n days"
}

fun Task.repeatLabel(): String = when {
    !repeating -> "One time"
    weekdays == 127 -> "Every day"
    weekdays != 0 -> DayOfWeek.values()
        .filter { ((weekdays shr (it.value - 1)) and 1) == 1 }
        .joinToString(", ") { it.getDisplayName(JTextStyle.SHORT, Locale.getDefault()) }
    else -> intervalLabel(interval)
}

fun Task.nextOccurrence(from: LocalDate): LocalDate? {
    for (i in 0L..800L) {
        val d = from.plusDays(i)
        if (occursOn(d)) return d
    }
    return null
}

fun AppState.tasksOn(d: LocalDate, includeSkipped: Boolean = false): List<Task> =
    tasks.filter { it.occursOn(d) && (includeSkipped || !isSkipped(it, d)) }

fun AppState.overdue(today: LocalDate): List<Task> =
    tasks.filter { !it.repeating && it.date.isBefore(today) && !isDone(it, it.date) && !isSkipped(it, it.date) }
        .sortedBy { it.date }

fun AppState.hasFutureSkips(task: Task, today: LocalDate): Boolean =
    skips.any { it.startsWith(task.id + "|") && !LocalDate.parse(it.substringAfter('|')).isBefore(today) }

// ───────────── changes ─────────────

fun AppState.setDone(task: Task, date: LocalDate, done: Boolean): AppState {
    if (done == isDone(task, date)) return this
    return if (done) copy(history = history + DoneRecord(task.id, task.title, date, task.repeating))
    else copy(history = history.filterNot { it.taskId == task.id && it.date == date })
}

// ───────────── statistics ─────────────

fun AppState.streak(today: LocalDate): Int {
    var n = 0
    for (i in 0 until 365) {
        val d = today.minusDays(i.toLong())
        val t = tasksOn(d)
        if (t.isEmpty()) continue
        if (t.all { isDone(it, d) }) n++
        else if (i > 0) break
    }
    return n
}

data class TaskStats(val done: Int, val total: Int, val current: Int, val best: Int) {
    val percent: Int get() = if (total == 0) 0 else (done * 100f / total).roundToInt()
}

fun AppState.taskStats(task: Task, today: LocalDate): TaskStats {
    var total = 0
    var done = 0
    var run = 0
    var best = 0
    var d = maxOf(task.date, today.minusDays(1095))
    while (!d.isAfter(today)) {
        if (task.occursOn(d) && !isSkipped(task, d)) {
            val ok = isDone(task, d)
            if (d.isBefore(today) || ok) {
                total++
                if (ok) { done++; run++; best = max(best, run) } else run = 0
            }
        }
        d = d.plusDays(1)
    }
    return TaskStats(done, total, run, best)
}

fun AppState.rateBetween(from: LocalDate, to: LocalDate): Float? {
    var planned = 0
    var done = 0
    var d = from
    while (!d.isAfter(to)) {
        val t = tasksOn(d)
        planned += t.size
        done += t.count { isDone(it, d) }
        d = d.plusDays(1)
    }
    return if (planned > 0) done.toFloat() / planned else null
}

fun weekStart(d: LocalDate, startDay: Int): LocalDate =
    d.minusDays(((d.dayOfWeek.value - startDay + 7) % 7).toLong())

fun AppState.dayKind(d: LocalDate, today: LocalDate): Int {
    if (d.isAfter(today)) return 0
    val t = tasksOn(d)
    if (t.isEmpty()) return 0
    val done = t.count { isDone(it, d) }
    return when {
        done == t.size -> 1
        done > 0 -> 2
        d == today -> 4
        else -> 3
    }
}

fun AppState.taskDayKind(task: Task, d: LocalDate, today: LocalDate): Int {
    if (d.isAfter(today) || !task.occursOn(d)) return 0
    return when {
        isSkipped(task, d) -> 2
        isDone(task, d) -> 1
        d == today -> 4
        else -> 3
    }
}

// ═══════════════ Store ═══════════════

object Store {
    private val lock = Any()
    private val _flow = MutableStateFlow(AppState())
    val flow: StateFlow<AppState> get() = _flow
    @Volatile private var loaded = false
    private val io = Executors.newSingleThreadExecutor()
    private val scheduled = AtomicBoolean(false)

    fun init(ctx: Context) {
        if (loaded) return
        synchronized(lock) {
            if (loaded) return
            val app = ctx.applicationContext
            Holidays.attach(app)
            Lang.load(app)
            _flow.value = read(app)
            Lang.applyLocale(_flow.value.language)
            loaded = true
        }
    }

    fun get(ctx: Context): AppState {
        init(ctx)
        return _flow.value
    }

    fun update(ctx: Context, f: (AppState) -> AppState): AppState {
        val app = ctx.applicationContext
        init(app)
        synchronized(lock) {
            val current = _flow.value
            val next = f(current)
            if (next != current) {
                _flow.value = next
                persist(app)
            }
            return next
        }
    }

    private fun persist(app: Context) {
        if (scheduled.getAndSet(true)) return
        io.execute {
            scheduled.set(false)
            try {
                val tmp = File(app.filesDir, "state.json.tmp")
                tmp.writeText(toJson(_flow.value, export = false))
                tmp.renameTo(File(app.filesDir, "state.json"))
            } catch (e: Exception) {
            }
        }
    }

    private fun read(app: Context): AppState {
        val f = File(app.filesDir, "state.json")
        if (f.exists()) parse(runCatching { f.readText() }.getOrNull())?.let { return it }
        val old = app.getSharedPreferences("tracker", Context.MODE_PRIVATE).getString("state", null)
        return parse(old) ?: AppState()
    }

    fun exportJson(ctx: Context, s: AppState = get(ctx)): String {
        val photo = runCatching {
            val f = File(ctx.applicationContext.filesDir, "pfp.jpg")
            if (f.exists()) android.util.Base64.encodeToString(f.readBytes(), android.util.Base64.NO_WRAP) else null
        }.getOrNull()
        return toJson(s, export = true, photo = photo)
    }

    fun hasSettings(json: String?): Boolean =
        runCatching { JSONObject(json ?: "").let { it.has("name") && it.has("theme") } }.getOrDefault(false)

    fun parsePhoto(json: String?): ByteArray? = runCatching {
        val b64 = JSONObject(json ?: "").optString("photo", "")
        if (b64.isBlank()) null else android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
    }.getOrNull()

    fun toJson(s: AppState, export: Boolean, photo: String? = null): String {
        val root = JSONObject()
        root.put("app", "DailyTracker")
        root.put("version", 2)
        root.put("name", s.name)
        root.put("theme", s.theme)
        root.put("springy", s.springy)
        root.put("showQuote", s.showQuote)
        root.put("weekStartDay", s.weekStartDay)
        root.put("country", s.country)
        root.put("blurMode", s.blurMode)
        root.put("showHolidays", s.showHolidays)
        root.put("nudgeOn", s.nudgeOn)
        root.put("reviewOn", s.reviewOn)
        root.put("pfp", s.pfpVersion)
        root.put("language", s.language)
        if (photo != null) root.put("photo", photo)
        if (!export) {
            root.put("onboarded", s.onboarded)
            root.put("tipsDone", s.tipsDone)
            root.put("hintSeen", s.hintSeen)
            root.put("seenMainSwipe", s.seenMainSwipe)
            root.put("seenInsightsSwipe", s.seenInsightsSwipe)
        }
        root.put("tasks", JSONArray().also { arr ->
            s.tasks.forEach { t ->
                arr.put(JSONObject().apply {
                    put("id", t.id)
                    put("title", t.title)
                    put("description", t.description)
                    put("daily", t.repeating)
                    put("date", t.date.toString())
                    t.time?.let { put("time", it.toString()) }
                    t.color?.let { put("color", it) }
                    put("interval", t.interval)
                    put("weekdays", t.weekdays)
                    if (t.shifts.isNotEmpty()) put("shifts", t.shifts.joinToString(";") { "${it.first}:${it.second}" })
                })
            }
        })
        root.put("history", JSONArray().also { arr ->
            s.history.forEach { r ->
                arr.put(JSONObject().apply {
                    put("taskId", r.taskId)
                    put("title", r.title)
                    put("date", r.date.toString())
                    put("daily", r.repeating)
                })
            }
        })
        root.put("skips", JSONArray(s.skips.toList()))
        return if (export) root.toString(2) else root.toString()
    }

    fun parse(json: String?): AppState? {
        if (json.isNullOrBlank()) return null
        return try {
            val root = JSONObject(json)
            val tasks = mutableListOf<Task>()
            val arr = root.getJSONArray("tasks")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                tasks += Task(
                    id = o.getString("id"),
                    title = o.getString("title"),
                    description = o.optString("description", ""),
                    repeating = o.optBoolean("daily", false),
                    date = LocalDate.parse(o.getString("date")),
                    time = if (o.has("time")) LocalTime.parse(o.getString("time")) else null,
                    color = if (o.has("color")) o.getInt("color") else null,
                    interval = o.optInt("interval", 1).coerceAtLeast(1),
                    weekdays = o.optInt("weekdays", 0),
                    shifts = o.optString("shifts", "").split(';').filter { it.contains(':') }.mapNotNull {
                        runCatching { LocalDate.parse(it.substringBefore(':')) to it.substringAfter(':').toInt() }.getOrNull()
                    }
                )
            }
            val history = mutableListOf<DoneRecord>()
            val h = root.optJSONArray("history")
            if (h != null) for (i in 0 until h.length()) {
                val o = h.getJSONObject(i)
                history += DoneRecord(o.getString("taskId"), o.getString("title"), LocalDate.parse(o.getString("date")), o.optBoolean("daily", false))
            }
            val skips = mutableSetOf<String>()
            val sk = root.optJSONArray("skips")
            if (sk != null) for (i in 0 until sk.length()) skips += sk.getString(i)

            val theme = if (root.has("theme")) root.getInt("theme") else {
                val oled = root.optBoolean("oled", false)
                when {
                    oled -> 3
                    !root.has("dark") -> 0
                    root.getBoolean("dark") -> 2
                    else -> 1
                }
            }
            AppState(
                tasks = tasks, history = history, skips = skips,
                name = root.optString("name", "Friend").ifBlank { "Friend" },
                theme = theme.coerceIn(0, 3),
                springy = root.optBoolean("springy", true),
                showQuote = root.optBoolean("showQuote", true),
                weekStartDay = root.optInt("weekStartDay", if (root.optBoolean("weekStartSunday", false)) 7 else 1).coerceIn(1, 7),
                country = root.optString("country", "AUTO").ifBlank { "AUTO" },
                language = root.optString("language", ""),
                tipsDone = root.optBoolean("tipsDone", root.optBoolean("onboarded", root.optBoolean("hintSeen", false))),
                onboarded = root.optBoolean("onboarded", root.optBoolean("hintSeen", false)),
                blurMode = root.optInt("blurMode", 1).coerceIn(0, 2),
                showHolidays = root.optBoolean("showHolidays", true),
                nudgeOn = root.optBoolean("nudgeOn", true),
                reviewOn = root.optBoolean("reviewOn", true),
                hintSeen = root.optBoolean("hintSeen", false),
                seenMainSwipe = root.optBoolean("seenMainSwipe", false),
                seenInsightsSwipe = root.optBoolean("seenInsightsSwipe", false),
                pfpVersion = root.optLong("pfp", 0L)
            )
        } catch (e: Exception) {
            null
        }
    }
}

// ═══════════════ Strings ═══════════════

val LocalLang = compositionLocalOf { "en" }

object Lang {
    val all = listOf(
        "en" to "English", "es" to "Español", "de" to "Deutsch", "fr" to "Français", "it" to "Italiano",
        "pt" to "Português", "pl" to "Polski", "ru" to "Русский", "uk" to "Українська", "tr" to "Türkçe"
    )
    val codes = all.map { it.first }

    val deviceLocale: Locale = Locale.getDefault()

    @Volatile private var table: Map<String, List<String>> = emptyMap()

    fun load(ctx: Context) {
        if (table.isNotEmpty()) return
        table = runCatching {
            ctx.assets.open("translations.tsv").bufferedReader().useLines { lines ->
                lines.filter { it.isNotBlank() && !it.startsWith("#") }.associate {
                    val p = it.split('\t')
                    p[0] to p.drop(1)
                }
            }
        }.getOrDefault(emptyMap())
    }

    fun nativeName(code: String) = all.firstOrNull { it.first == code }?.second ?: code

    fun resolve(setting: String): String {
        if (setting in codes) return setting
        return if (deviceLocale.language in codes) deviceLocale.language else "en"
    }

    fun applyLocale(setting: String) {
        Locale.setDefault(if (setting in codes) Locale.forLanguageTag(setting) else deviceLocale)
    }

    fun tr(lang: String, en: String): String {
        if (lang == "en") return en
        val i = codes.indexOf(lang) - 1
        if (i < 0) return en
        return table[en]?.getOrNull(i)?.takeIf { it.isNotBlank() } ?: en
    }
}

@Composable
fun tx(en: String): String = Lang.tr(LocalLang.current, en)

// ═══════════════ Quotes ═══════════════

val QUOTES = listOf(
    "Small steps, taken daily, beat big plans never started.",
    "Done is better than perfect.",
    "You don't need motivation, just the next tiny action.",
    "Consistency is a quiet superpower.",
    "Today's effort is tomorrow's momentum.",
    "Don't break the chain.",
    "Start where you are. Use what you have.",
    "One checked box at a time.",
    "Discipline is choosing what you want most over what you want now.",
    "Progress, not perfection.",
    "Make today count.",
    "Focus on the next hour, not the whole year.",
    "Your future self is watching. Make them proud.",
    "Energy flows where attention goes.",
    "A little every day adds up to a lot.",
    "Show up first, feel ready later.",
    "Habits are votes for the person you want to become.",
    "The best time to start was yesterday. The next best is now.",
    "Be stubborn about the goal, flexible about the path.",
    "Tiny wins build big confidence.",
    "You are one routine away from a different life.",
    "Do it tired. Do it bored. Do it anyway.",
    "Clarity first, speed second.",
    "Finish the easy thing, then the hard thing feels closer.",
    "What you repeat, you become.",
    "Rest is part of the plan, not a break from it.",
    "Ten minutes is better than zero minutes.",
    "Slow progress is still progress.",
    "Win the morning, win the day.",
    "Your only competition is who you were yesterday.",
    "Plan it, then do it. Not the other way round.",
    "Doing is the best way to stop worrying.",
    "Low motivation days are where habits are built.",
    "Be proud of the streak, not afraid of breaking it.",
    "If it matters, schedule it.",
    "Less thinking about starting, more starting.",
    "Every expert once had day one.",
    "A good day is a day you kept your word to yourself.",
    "Keep the promise, even the small ones.",
    "Make it easy. Make it obvious. Make it happen.",
    "Momentum loves a small first step.",
    "Calm mind, clear list, steady hands.",
    "Don't wait for the perfect moment. Take the moment and make it perfect.",
    "Effort counts twice: once when you try, once when you finish.",
    "Today is a good day to be a little better.",
    "You can do hard things, one at a time.",
    "Your habits are your quiet autobiography.",
    "Start small, stay long.",
    "Be kind to yourself, then get back to it.",
    "Missed a day? Never miss twice.",
    "Focus is saying no to good ideas.",
    "The list gets shorter when you start.",
    "Make a little room for what matters.",
    "Courage is just starting while afraid.",
    "Quiet work, loud results.",
    "Today's tasks, tomorrow's freedom.",
    "Keep going. Future you says thanks.",
    "Build the day you want, one task at a time.",
    "Not all progress is visible. Keep trusting the process.",
    "Good habits don't need drama, just repetition.",
    "Begin before you feel ready."
)

// ═══════════════ Holidays ═══════════════

object Holidays {
    private var app: Context? = null
    private var codes: Set<String>? = null
    private val cache = HashMap<String, Map<Int, Map<LocalDate, String>>>()

    fun attach(c: Context) { app = c.applicationContext }

    private fun available(): Set<String> = codes ?: run {
        val found = app?.assets?.list("holidays")?.map { it.removeSuffix(".tsv") }?.toSet() ?: return emptySet()
        codes = found
        found
    }

    fun countryName(code: String): String {
        val n = Locale("", code).getDisplayCountry(Locale.getDefault())
        return if (n.isBlank()) code else n
    }

    fun allCountries(): List<Pair<String, String>> =
        available().map { it to countryName(it) }.sortedBy { it.second.lowercase() }

    @Synchronized
    fun forYear(code: String, y: Int): Map<LocalDate, String> {
        if (code.isEmpty()) return emptyMap()
        return cache.getOrPut(code) { load(code) }[y] ?: emptyMap()
    }

    private fun load(code: String): Map<Int, Map<LocalDate, String>> {
        val a = app ?: return emptyMap()
        return runCatching {
            a.assets.open("holidays/$code.tsv").bufferedReader(Charsets.UTF_8).useLines { lines ->
                val out = HashMap<Int, MutableMap<LocalDate, String>>()
                lines.forEach { line ->
                    val tab = line.indexOf('\t')
                    if (tab < 10) return@forEach
                    val date = LocalDate.parse(line.substring(0, tab))
                    out.getOrPut(date.year) { LinkedHashMap() }.putIfAbsent(date, line.substring(tab + 1))
                }
                out
            }
        }.getOrDefault(emptyMap())
    }
}

// ═══════════════ Reminders ═══════════════

object Reminders {
    const val CHANNEL = "task_reminders"
    const val CHANNEL_SUMMARY = "daily_summary"
    const val ACTION_FIRE = "com.kirillshybaiev.dailytracker.FIRE"
    const val ACTION_DONE = "com.kirillshybaiev.dailytracker.DONE"
    const val ACTION_SNOOZE = "com.kirillshybaiev.dailytracker.SNOOZE"
    const val ACTION_NUDGE = "com.kirillshybaiev.dailytracker.NUDGE"
    const val ACTION_REVIEW = "com.kirillshybaiev.dailytracker.REVIEW"
    const val ACTION_MIDNIGHT = "com.kirillshybaiev.dailytracker.MIDNIGHT"
    private const val NUDGE_ID = 9001
    private const val REVIEW_ID = 9002

    fun ensureChannels(c: Context) {
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Task reminders", NotificationManager.IMPORTANCE_HIGH))
        nm.createNotificationChannel(NotificationChannel(CHANNEL_SUMMARY, "Evening nudge & weekly review", NotificationManager.IMPORTANCE_DEFAULT))
    }

    fun canExact(c: Context): Boolean =
        Build.VERSION.SDK_INT < 31 || c.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    private fun pending(c: Context, action: String, key: String, id: String? = null, snooze: Boolean = false): PendingIntent {
        val i = Intent(c, ReminderReceiver::class.java).setAction(action)
        if (id != null) i.putExtra("id", id)
        if (snooze) i.putExtra("snooze", true)
        return PendingIntent.getBroadcast(
            c, (action + key).hashCode(), i,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    internal fun setAlarm(c: Context, millis: Long, pi: PendingIntent) {
        val am = c.getSystemService(AlarmManager::class.java)
        if (canExact(c)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
    }

    private fun millis(at: LocalDateTime) = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    // ───────── task reminders ─────────

    fun schedule(c: Context, task: Task) {
        val time = task.time ?: return
        val state = Store.get(c)
        val now = LocalDateTime.now()
        val today = LocalDate.now()
        for (i in 0L..400L) {
            val d = today.plusDays(i)
            if (!task.occursOn(d) || state.isSkipped(task, d)) continue
            val at = d.atTime(time)
            if (!at.isAfter(now)) continue
            setAlarm(c, millis(at), pending(c, ACTION_FIRE, task.id, task.id))
            return
        }
    }

    fun cancel(c: Context, task: Task) {
        c.getSystemService(AlarmManager::class.java).cancel(pending(c, ACTION_FIRE, task.id, task.id))
    }

    // ───────── evening nudge + weekly review ─────────

    private fun nudgeAt(d: LocalDate): LocalDateTime {
        val r = kotlin.random.Random(d.toEpochDay() * 7919L + 13L)
        return d.atTime(19, r.nextInt(60), r.nextInt(60))
    }

    fun scheduleNudge(c: Context, s: AppState) {
        val pi = pending(c, ACTION_NUDGE, "nudge")
        if (!s.nudgeOn) { c.getSystemService(AlarmManager::class.java).cancel(pi); return }
        var d = LocalDate.now()
        var at = nudgeAt(d)
        if (!at.isAfter(LocalDateTime.now())) { d = d.plusDays(1); at = nudgeAt(d) }
        setAlarm(c, millis(at), pi)
    }

    fun scheduleReview(c: Context, s: AppState) {
        val pi = pending(c, ACTION_REVIEW, "review")
        if (!s.reviewOn) { c.getSystemService(AlarmManager::class.java).cancel(pi); return }
        val reviewDay = DayOfWeek.of((s.weekStartDay + 5) % 7 + 1)
        var d = LocalDate.now()
        var at = d.atTime(19, 0)
        var guard = 0
        while ((d.dayOfWeek != reviewDay || !at.isAfter(LocalDateTime.now())) && guard < 14) {
            d = d.plusDays(1)
            at = d.atTime(19, 0)
            guard++
        }
        setAlarm(c, millis(at), pi)
    }

    fun scheduleMidnight(c: Context) {
        val at = LocalDate.now().plusDays(1).atTime(0, 0, 10)
        val pi = pending(c, ACTION_MIDNIGHT, "midnight")
        val am = c.getSystemService(AlarmManager::class.java)
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis(at), pi)
    }

    fun scheduleAll(c: Context, s: AppState) {
        scheduleMidnight(c)
        s.tasks.forEach { schedule(c, it) }
        scheduleNudge(c, s)
        scheduleReview(c, s)
    }

    // ───────── notifications ─────────

    internal fun notifyTask(c: Context, task: Task) {
        val open = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val done = Notification.Action.Builder(
            Icon.createWithResource(c, R.drawable.ic_stat_check), "Done",
            pending(c, ACTION_DONE, task.id, task.id)
        ).build()
        val snooze = Notification.Action.Builder(
            Icon.createWithResource(c, R.drawable.ic_stat_snooze), "Snooze 10 min",
            pending(c, ACTION_SNOOZE, task.id, task.id)
        ).build()
        val n = Notification.Builder(c, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_check)
            .setContentTitle(task.title)
            .setContentText(task.description.ifBlank { if (task.repeating) "Repeating task – not done yet" else "Task reminder" })
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(done)
            .addAction(snooze)
            .build()
        c.getSystemService(NotificationManager::class.java).notify(task.id.hashCode(), n)
    }

    internal fun notifySummary(c: Context, id: Int, title: String, text: String) {
        val open = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(c, CHANNEL_SUMMARY)
            .setSmallIcon(R.drawable.ic_stat_check)
            .setContentTitle(title).setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setContentIntent(open).setAutoCancel(true).build()
        c.getSystemService(NotificationManager::class.java).notify(id, n)
    }

    internal fun snooze(c: Context, task: Task) {
        setAlarm(c, System.currentTimeMillis() + 10 * 60_000L, pending(c, ACTION_FIRE, task.id + "snooze", task.id, snooze = true))
    }

    internal fun nudgeText(s: AppState, today: LocalDate): String? {
        val left = s.tasksOn(today).filter { !s.isDone(it, today) }
        if (left.isEmpty()) return null
        val names = left.take(3).joinToString(", ") { it.title } + if (left.size > 3) "…" else ""
        return "${left.size} ${if (left.size == 1) "task" else "tasks"} left today: $names"
    }

    internal fun reviewText(s: AppState, today: LocalDate): String? {
        val start = weekStart(today, s.weekStartDay)
        val rate = s.rateBetween(start, today) ?: return null
        val last = s.rateBetween(start.minusDays(7), start.minusDays(1))
        val pct = (rate * 100).roundToInt()
        val trend = last?.let {
            val d = ((rate - it) * 100).roundToInt()
            when {
                d >= 3 -> " That's up $d points from last week."
                d <= -3 -> " That's down ${-d} points from last week."
                else -> " About the same as last week."
            }
        } ?: ""
        return "You finished $pct% of your tasks this week.$trend"
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) {
        val id = intent.getStringExtra("id")
        val today = LocalDate.now()
        when (intent.action) {
            Reminders.ACTION_FIRE -> {
                val s = Store.get(c)
                val task = s.tasks.find { it.id == id } ?: return
                if (task.occursOn(today) && !s.isDone(task, today) && !s.isSkipped(task, today)) Reminders.notifyTask(c, task)
                if (task.repeating && !intent.getBooleanExtra("snooze", false)) Reminders.schedule(c, task)
            }
            Reminders.ACTION_MIDNIGHT -> {
                val s = Store.get(c)
                Reminders.scheduleAll(c, s)
                TasksWidget.refresh(c)
            }
            Reminders.ACTION_DONE -> {
                Store.update(c) { s -> s.tasks.find { it.id == id }?.let { s.setDone(it, today, true) } ?: s }
                c.getSystemService(NotificationManager::class.java).cancel(id.hashCode())
                TasksWidget.refresh(c)
            }
            Reminders.ACTION_SNOOZE -> {
                val task = Store.get(c).tasks.find { it.id == id } ?: return
                c.getSystemService(NotificationManager::class.java).cancel(id.hashCode())
                Reminders.snooze(c, task)
            }
            Reminders.ACTION_NUDGE -> {
                val s = Store.get(c)
                Reminders.nudgeText(s, today)?.let { Reminders.notifySummary(c, 9001, "Evening check-in", it) }
                Reminders.scheduleNudge(c, s)
            }
            Reminders.ACTION_REVIEW -> {
                val s = Store.get(c)
                Reminders.reviewText(s, today)?.let { Reminders.notifySummary(c, 9002, "Your week in review", it) }
                Reminders.scheduleReview(c, s)
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) {
        Reminders.ensureChannels(c)
        Reminders.scheduleAll(c, Store.get(c))
        TasksWidget.refresh(c)
    }
}

// ═══════════════ TrackerViewModel ═══════════════

class TrackerViewModel(app: Application) : AndroidViewModel(app) {
    init { Store.init(app) }

    val state: StateFlow<AppState> = Store.flow
    private val ctx: Context = app.applicationContext

    private val bg = Executors.newSingleThreadExecutor()
    private val refreshPending = AtomicBoolean(false)

    init {
        bg.execute {
            Reminders.ensureChannels(ctx)
            TasksWidget.refresh(ctx)
        }
    }

    private fun mutate(f: (AppState) -> AppState): AppState {
        val s = Store.update(ctx, f)
        if (!refreshPending.getAndSet(true)) {
            bg.execute {
                refreshPending.set(false)
                TasksWidget.refresh(ctx)
                }
        }
        return s
    }

    // ───────── tasks ─────────

    fun toggleDone(task: Task, date: LocalDate): (() -> Unit)? {
        val wasDone = state.value.isDone(task, date)
        mutate { it.setDone(task, date, !wasDone) }
        val undo: () -> Unit = { mutate { s -> s.setDone(task, date, true) } }
        return if (wasDone) undo else null
    }

    fun saveTask(t: Task) {
        mutate { s ->
            val exists = s.tasks.any { it.id == t.id }
            s.copy(tasks = if (exists) s.tasks.map { if (it.id == t.id) t else it } else s.tasks + t)
        }
        Reminders.cancel(ctx, t)
        Reminders.schedule(ctx, t)
    }

    fun deleteTask(t: Task): () -> Unit {
        val index = state.value.tasks.indexOfFirst { it.id == t.id }
        Reminders.cancel(ctx, t)
        mutate { s -> s.copy(tasks = s.tasks.filter { it.id != t.id }) }
        return {
            mutate { s ->
                if (s.tasks.any { it.id == t.id }) s
                else s.copy(tasks = s.tasks.toMutableList().also { it.add(index.coerceIn(0, it.size), t) })
            }
            Reminders.schedule(ctx, t)
        }
    }

    fun reorder(fromId: String, toId: String) {
        mutate { s ->
            val list = s.tasks.toMutableList()
            val a = list.indexOfFirst { it.id == fromId }
            val b = list.indexOfFirst { it.id == toId }
            if (a < 0 || b < 0 || a == b) s else { list.add(b, list.removeAt(a)); s.copy(tasks = list) }
        }
    }

    // ───────── skip / pause / dismiss ─────────

    fun skip(task: Task, date: LocalDate): () -> Unit {
        val key = "${task.id}|$date"
        val had = key in state.value.skips
        mutate { it.copy(skips = it.skips + key) }
        Reminders.schedule(ctx, task)
        return {
            mutate { s -> s.copy(skips = if (had) s.skips else s.skips - key) }
            Reminders.schedule(ctx, task)
        }
    }

    fun moveToTomorrow(task: Task, from: LocalDate): () -> Unit {
        val to = from.plusDays(1)
        if (!task.repeating) {
            val old = task
            saveTask(task.copy(date = maxOf(task.date, LocalDate.now()).plusDays(1)))
            return { saveTask(old) }
        }
        val copy = task.copy(id = java.util.UUID.randomUUID().toString(), repeating = false, date = to, shifts = emptyList())
        val key = "${task.id}|$from"
        val had = key in state.value.skips
        mutate { it.copy(skips = it.skips + key, tasks = it.tasks + copy) }
        Reminders.schedule(ctx, task); Reminders.schedule(ctx, copy)
        return {
            mutate { s -> s.copy(skips = if (had) s.skips else s.skips - key, tasks = s.tasks.filter { it.id != copy.id }) }
            Reminders.cancel(ctx, copy); Reminders.schedule(ctx, task)
        }
    }

    fun shiftSchedule(task: Task, days: Int = 1): () -> Unit {
        val today = LocalDate.now()
        val from = if (state.value.isDone(task, today)) today.plusDays(1) else today
        val old = task
        saveTask(task.copy(shifts = task.shifts + (from to days)))
        return { saveTask(old) }
    }

    fun dismissOverdue(tasks: List<Task>): () -> Unit {
        val keys = tasks.map { "${it.id}|${it.date}" }.toSet()
        mutate { it.copy(skips = it.skips + keys) }
        return { mutate { s -> s.copy(skips = s.skips - keys) } }
    }

    fun pauseUntil(task: Task, end: LocalDate): () -> Unit {
        val before = state.value
        val keys = mutableSetOf<String>()
        var d = LocalDate.now()
        var n = 0
        while (!d.isAfter(end) && n < 400) {
            if (task.occursOn(d) && !before.isDone(task, d)) keys += "${task.id}|$d"
            d = d.plusDays(1)
            n++
        }
        mutate { it.copy(skips = it.skips + keys) }
        Reminders.schedule(ctx, task)
        return {
            mutate { s -> s.copy(skips = s.skips - keys) }
            Reminders.schedule(ctx, task)
        }
    }

    fun resume(task: Task): () -> Unit {
        val today = LocalDate.now()
        val removed = state.value.skips
            .filter { it.startsWith(task.id + "|") && !LocalDate.parse(it.substringAfter('|')).isBefore(today) }.toSet()
        mutate { it.copy(skips = it.skips - removed) }
        Reminders.schedule(ctx, task)
        return {
            mutate { s -> s.copy(skips = s.skips + removed) }
            Reminders.schedule(ctx, task)
        }
    }

    // ───────── settings ─────────

    fun setName(name: String) { mutate { it.copy(name = name) } }
    fun bumpPhoto() { mutate { it.copy(pfpVersion = System.currentTimeMillis()) } }
    fun setTheme(theme: Int) { mutate { it.copy(theme = theme) } }
    fun setSpringy(on: Boolean) { mutate { it.copy(springy = on) } }
    fun setShowQuote(on: Boolean) { mutate { it.copy(showQuote = on) } }
    fun setShowHolidays(on: Boolean) { mutate { it.copy(showHolidays = on) } }
    fun markHintSeen() { mutate { it.copy(hintSeen = true) } }
    fun markMainSwipeSeen() { mutate { it.copy(seenMainSwipe = true) } }
    fun markInsightsSwipeSeen() { mutate { it.copy(seenInsightsSwipe = true) } }

    fun setCountry(code: String) { mutate { it.copy(country = code) } }
    fun setLanguage(code: String) {
        Lang.applyLocale(code)
        mutate { it.copy(language = code) }
        TasksWidget.refresh(ctx)
    }
    fun markTipsDone() { mutate { it.copy(tipsDone = true) } }
    fun markOnboarded() { mutate { it.copy(onboarded = true, hintSeen = true, seenMainSwipe = true) } }

    private val main = android.os.Handler(android.os.Looper.getMainLooper())

    fun setBlur(mode: Int) { mutate { it.copy(blurMode = mode) } }

    fun setWeekStart(day: Int) {
        val s = mutate { it.copy(weekStartDay = day) }
        Reminders.scheduleReview(ctx, s)
    }

    fun setNudge(on: Boolean) {
        val s = mutate { it.copy(nudgeOn = on) }
        Reminders.scheduleNudge(ctx, s)
    }

    fun setReview(on: Boolean) {
        val s = mutate { it.copy(reviewOn = on) }
        Reminders.scheduleReview(ctx, s)
    }

    // ───────── data ─────────

    fun importMerge(imported: AppState, withSettings: Boolean = false, photo: ByteArray? = null) {
        val s = mutate { cur ->
            val merged = cur.copy(
                tasks = (cur.tasks + imported.tasks).distinctBy { it.id },
                history = (cur.history + imported.history).distinct(),
                skips = cur.skips + imported.skips
            )
            if (!withSettings) merged else merged.copy(
                name = imported.name, theme = imported.theme, springy = imported.springy, showQuote = imported.showQuote,
                weekStartDay = imported.weekStartDay, country = imported.country, language = imported.language,
                blurMode = imported.blurMode, showHolidays = imported.showHolidays,
                nudgeOn = imported.nudgeOn, reviewOn = imported.reviewOn
            )
        }
        if (withSettings) {
            Lang.applyLocale(s.language)
            if (photo != null && photo.isNotEmpty()) {
                runCatching { File(ctx.filesDir, "pfp.jpg").writeBytes(photo) }
                mutate { it.copy(pfpVersion = System.currentTimeMillis()) }
            }
        }
        Reminders.scheduleAll(ctx, s)
    }

    fun resetData() {
        state.value.tasks.forEach { Reminders.cancel(ctx, it) }
        mutate { it.copy(tasks = emptyList(), history = emptyList(), skips = emptySet()) }
    }
}

// ═══════════════ BitmapWidgets ═══════════════

open class BitmapWidget(private val kind: Int) : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { render(context, manager, it, kind) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, newOptions: android.os.Bundle) {
        render(context, manager, id, kind)
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        ids.forEach { WidgetPrefs.remove(context, it) }
    }

    companion object {
        const val CALENDAR = 0
        const val HEATMAP = 1

        fun render(c: Context, mgr: AppWidgetManager, id: Int, kind: Int) {
            val o = mgr.getAppWidgetOptions(id)
            val landscape = c.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val w = o.getInt(if (landscape) AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH else AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
                .takeIf { it > 0 } ?: 250
            val h = o.getInt(if (landscape) AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT else AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
                .takeIf { it > 0 } ?: 250
            val look = WidgetPrefs.look(c, id)
            val bmp = if (kind == CALENDAR) WidgetBitmaps.calendar(c, look, w, h) else WidgetBitmaps.heatmap(c, look, w, h)
            val views = RemoteViews(c.packageName, R.layout.widget_image)
            views.setImageViewBitmap(R.id.widget_image, bmp)
            views.setOnClickPendingIntent(
                R.id.widget_image,
                PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            )
            mgr.updateAppWidget(id, views)
        }
    }
}

class CalendarWidget : BitmapWidget(CALENDAR)
class InsightsWidget : BitmapWidget(HEATMAP)

object WidgetBitmaps {
    private val GREEN = 0xFF3DDC84.toInt()
    private val RED = 0xFFFF6B6B.toInt()

    private class Art(val bmp: Bitmap, val cv: Canvas, val sc: Float) {
        val w get() = bmp.width.toFloat()
        val h get() = bmp.height.toFloat()
    }

    private fun art(c: Context, wDp: Int, hDp: Int, bg: Int): Art {
        val sc = min(c.resources.displayMetrics.density, 1100f / max(wDp, hDp))
        val bmp = Bitmap.createBitmap(max(1, (wDp * sc).toInt()), max(1, (hDp * sc).toInt()), Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        cv.drawRoundRect(
            RectF(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat()), 28 * sc, 28 * sc,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bg }
        )
        return Art(bmp, cv, sc)
    }

    private fun paint(color: Int, size: Float, bold: Boolean = false, align: Paint.Align = Paint.Align.LEFT) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            textAlign = align
            typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        }

    private fun Canvas.centered(text: String, cx: Float, cy: Float, p: Paint) {
        val fm = p.fontMetrics
        drawText(text, cx, cy - (fm.ascent + fm.descent) / 2f, p)
    }

    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    private fun ring(color: Int, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = width
    }

    fun calendar(c: Context, look: WidgetLook, wDp: Int, hDp: Int): Bitmap {
        val s = Store.get(c)
        val p = WidgetStyle.palette(c, look)
        val fs = look.fontScale
        val today = LocalDate.now()
        val a = art(c, wDp, hDp, p.bgColor)
        val sc = a.sc
        val pad = 18 * sc
        val ym = YearMonth.from(today)

        a.cv.drawText(
            "${ym.month.getDisplayName(JTextStyle.FULL, Locale.getDefault())} ${ym.year}",
            pad, pad + 19 * sc * fs, paint(p.text, 22 * sc * fs, bold = true)
        )
        a.cv.drawText("${s.streak(today)}", a.w - pad, pad + 25 * sc * fs, paint(p.text, 30 * sc * fs, bold = true, align = Paint.Align.RIGHT))
        a.cv.drawText(
            Lang.tr(Lang.resolve(s.language), "day streak"), a.w - pad, pad + 40 * sc * fs,
            paint(p.secondary, 10.5f * sc * fs, align = Paint.Align.RIGHT)
        )

        val colW = (a.w - 2 * pad) / 7f
        val labelPaint = paint(p.secondary, 12 * sc * fs, align = Paint.Align.CENTER)
        val start = s.weekStartDay
        val order = DayOfWeek.values().let { v -> v.drop(start - 1) + v.take(start - 1) }
        order.forEachIndexed { i, d ->
            a.cv.centered(d.getDisplayName(JTextStyle.NARROW, Locale.getDefault()), pad + colW * (i + 0.5f), pad + 64 * sc * fs, labelPaint)
        }

        val top = pad + 76 * sc * fs
        val rowH = (a.h - pad - top) / 6f
        val diameter = min(colW, rowH) * 0.84f * look.iconScale.coerceIn(0.5f, 1.15f)
        val numPaint = paint(p.text, diameter * 0.42f, align = Paint.Align.CENTER)
        val numOnColor = paint(0xFFFFFFFF.toInt(), diameter * 0.42f, bold = true, align = Paint.Align.CENTER)
        val offset = (ym.atDay(1).dayOfWeek.value - start + 7) % 7
        for (day in 1..ym.lengthOfMonth()) {
            val date = ym.atDay(day)
            val idx = offset + day - 1
            val cx = pad + colW * (idx % 7 + 0.5f)
            val cy = top + rowH * (idx / 7 + 0.5f)
            val kind = s.dayKind(date, today)
            val color = when (kind) { 1 -> GREEN; 2 -> p.partial; 3 -> RED; else -> 0 }
            if (color != 0) a.cv.drawCircle(cx, cy, diameter / 2f, fill(color))
            if (date == today) a.cv.drawCircle(cx, cy, diameter / 2f + 2.5f * sc, ring(p.text, 2f * sc))
            a.cv.centered("$day", cx, cy, if (color != 0) numOnColor else numPaint)
        }
        return a.bmp
    }

    fun heatmap(c: Context, look: WidgetLook, wDp: Int, hDp: Int): Bitmap {
        val s = Store.get(c)
        val p = WidgetStyle.palette(c, look)
        val fs = look.fontScale
        val today = LocalDate.now()
        val weeks = look.weeks.coerceIn(1, 52)
        val task = if (look.source == "overview") null else s.tasks.find { it.id == look.source && it.repeating }
        val a = art(c, wDp, hDp, p.bgColor)
        val sc = a.sc
        val pad = 18 * sc
        val lang = Lang.resolve(s.language)

        a.cv.drawText(task?.title ?: Lang.tr(lang, "Overview"), pad, pad + 15 * sc * fs, paint(p.text, 19 * sc * fs, bold = true))
        a.cv.drawText("$weeks ${Lang.tr(lang, "weeks")}", a.w - pad, pad + 14 * sc * fs, paint(p.secondary, 12 * sc * fs, align = Paint.Align.RIGHT))

        val labels = if (task == null) listOf(GREEN to Lang.tr(lang, "all done"), p.partial to Lang.tr(lang, "partly"), RED to Lang.tr(lang, "missed"))
        else listOf(GREEN to Lang.tr(lang, "done"), RED to Lang.tr(lang, "missed"), p.partial to Lang.tr(lang, "skipped"))
        val lp = paint(p.secondary, 11 * sc * fs)
        val legendH = 12 * sc * fs
        val legendGap = 12 * sc

        val areaTop = pad + 30 * sc * fs
        val areaBottom = a.h - pad
        val gap = 3 * sc
        val availW = a.w - 2 * pad
        val availH = areaBottom - areaTop - legendGap - legendH
        val cellW0 = (availW - gap * (weeks - 1)) / weeks
        val fitH = (availH - gap * 6) / 7f
        val cell = max(2 * sc, min(cellW0, fitH))
        val cellW = cell
        val cellH = cell
        val gridW = weeks * cellW + (weeks - 1) * gap
        val gridH = 7 * cellH + 6 * gap
        val blockH = gridH + legendGap + legendH
        val x0 = (a.w - gridW) / 2f
        val y0 = areaTop + (areaBottom - areaTop - blockH) / 2f

        val first = weekStart(today, s.weekStartDay).minusWeeks((weeks - 1).toLong())
        val corner = min(cellW, cellH) * 0.28f
        for (col in 0 until weeks) {
            for (row in 0 until 7) {
                val day = first.plusDays((col * 7 + row).toLong())
                val kind = if (task == null) s.dayKind(day, today) else s.taskDayKind(task, day, today)
                val color = if (task == null) {
                    when (kind) { 1 -> GREEN; 2 -> p.partial; 3 -> RED; else -> p.faint }
                } else {
                    when (kind) { 1 -> GREEN; 2 -> p.partial; 3 -> (RED and 0x00FFFFFF) or 0x99000000.toInt(); else -> p.faint }
                }
                val x = x0 + col * (cellW + gap)
                val y = y0 + row * (cellH + gap)
                val r = RectF(x, y, x + cellW, y + cellH)
                a.cv.drawRoundRect(r, corner, corner, fill(color))
                if (day == today) a.cv.drawRoundRect(r, corner, corner, ring(p.text, max(1.5f * sc, min(cellW, cellH) * 0.1f)))
            }
        }

        var x = x0
        val y = y0 + gridH + legendGap + legendH * 0.8f
        labels.forEach { (color, label) ->
            a.cv.drawCircle(x + 4.5f * sc, y - 4f * sc * fs, 4.5f * sc, fill(color))
            a.cv.drawText(label, x + 14 * sc, y, lp)
            x += 14 * sc + lp.measureText(label) + 16 * sc
        }
        return a.bmp
    }
}

// ═══════════════ TasksWidget ═══════════════

class WidgetPalette(
    val bgColor: Int, val text: Int, val secondary: Int,
    val divider: Int, val ring: Int, val primary: Int, val onPrimary: Int,
    val faint: Int, val partial: Int
)

data class WidgetLook(
    val bg: Int = 1, val blur: Int = 60, val textMode: Int = 0,
    val fontScale: Float = 1f, val iconScale: Float = 1f,
    val clockSize: Int = 56, val weeks: Int = 12, val source: String = "overview"
)

object WidgetStyle {
    val names = listOf("Light", "Dark", "Black", "Transparent")
    const val TRANSPARENT = 3

    fun isNight(c: Context) =
        (c.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    private fun lightOnDark(textColor: Boolean) = if (textColor) WidgetPalette(
        0, 0xFFF5F5F7.toInt(), 0xFFC9C9D2.toInt(), 0x40FFFFFF, 0xFFE4E4EA.toInt(), 0xFFFFFFFF.toInt(),
        0xFF111111.toInt(), 0x33FFFFFF, 0xFFB4B4BE.toInt()
    ) else WidgetPalette(
        0, 0xFF111111.toInt(), 0xFF45454D.toInt(), 0x33000000, 0xFF2A2A30.toInt(), 0xFF111111.toInt(),
        0xFFFFFFFF.toInt(), 0x22000000, 0xFF6A6A74.toInt()
    )

    fun palette(c: Context, look: WidgetLook): WidgetPalette = when (look.bg) {
        0 -> WidgetPalette(
            0xFFFFFFFF.toInt(), 0xFF111111.toInt(), 0xFF8A8A93.toInt(), 0xFFE9E9EF.toInt(), 0xFFA9A9B4.toInt(),
            0xFF111111.toInt(), 0xFFFFFFFF.toInt(), 0xFFEDEDF2.toInt(), 0xFFA4A4B0.toInt()
        )
        1 -> WidgetPalette(
            0xFF1A1A1E.toInt(), 0xFFF2F2F5.toInt(), 0xFF8E8E98.toInt(), 0xFF2C2C32.toInt(), 0xFF6A6A74.toInt(),
            0xFFF5F5F7.toInt(), 0xFF111111.toInt(), 0xFF2A2A30.toInt(), 0xFF7A7A86.toInt()
        )
        2 -> WidgetPalette(
            0xFF000000.toInt(), 0xFFF2F2F5.toInt(), 0xFF8E8E98.toInt(), 0xFF222226.toInt(), 0xFF5A5A64.toInt(),
            0xFFF5F5F7.toInt(), 0xFF111111.toInt(), 0xFF1A1A1E.toInt(), 0xFF7A7A86.toInt()
        )
        else -> {
            val light = when (look.textMode) { 1 -> true; 2 -> false; else -> isNight(c) }
            val b = lightOnDark(light)
            WidgetPalette(0x00000000, b.text, b.secondary, b.divider, b.ring, b.primary, b.onPrimary, b.faint, b.partial)
        }
    }
}

object WidgetPrefs {
    private fun prefs(c: Context) = c.getSharedPreferences("widgets", Context.MODE_PRIVATE)

    fun look(c: Context, id: Int): WidgetLook {
        val p = prefs(c)
        return WidgetLook(
            bg = p.getInt("theme_$id", 1).coerceIn(0, 3),
            blur = p.getInt("blur_$id", 60),
            textMode = p.getInt("text_$id", 0),
            fontScale = p.getFloat("font_$id", 1f),
            iconScale = p.getFloat("icon_$id", 1f),
            clockSize = p.getInt("clock_$id", 56),
            weeks = p.getInt("weeks_$id", 12).coerceIn(1, 52),
            source = p.getString("source_$id", "overview") ?: "overview"
        )
    }

    fun save(c: Context, id: Int, l: WidgetLook) {
        prefs(c).edit()
            .putInt("theme_$id", l.bg).putInt("blur_$id", l.blur).putInt("text_$id", l.textMode)
            .putFloat("font_$id", l.fontScale).putFloat("icon_$id", l.iconScale)
            .putInt("clock_$id", l.clockSize).putInt("weeks_$id", l.weeks).putString("source_$id", l.source)
            .apply()
    }

    fun remove(c: Context, id: Int) {
        prefs(c).edit().remove("theme_$id").remove("blur_$id").remove("text_$id").remove("font_$id")
            .remove("icon_$id").remove("clock_$id").remove("weeks_$id").remove("source_$id").apply()
    }
}

open class BaseTasksWidget(private val clock: Boolean) : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { build(context, manager, it, clock) }
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        ids.forEach { WidgetPrefs.remove(context, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == TasksWidget.ACTION_TOGGLE) {
            val id = intent.getStringExtra("id") ?: return
            val today = LocalDate.now()
            Store.update(context) { s ->
                val t = s.tasks.find { it.id == id }
                if (t == null) s else s.setDone(t, today, !s.isDone(t, today))
            }
            TasksWidget.refresh(context)
        }
    }

    companion object {
        fun build(c: Context, mgr: AppWidgetManager, id: Int, clock: Boolean) {
            val look = WidgetPrefs.look(c, id)
            val p = WidgetStyle.palette(c, look)
            val fs = look.fontScale
            val today = LocalDate.now()
            val views = RemoteViews(c.packageName, R.layout.widget_tasks)

            val seeThrough = (p.bgColor ushr 24) == 0
            views.setViewVisibility(R.id.widget_bg, if (seeThrough) View.GONE else View.VISIBLE)
            if (!seeThrough) views.setInt(R.id.widget_bg, "setColorFilter", p.bgColor)
            views.setViewVisibility(R.id.widget_title, if (clock) View.GONE else View.VISIBLE)
            views.setViewVisibility(R.id.widget_clock, if (clock) View.VISIBLE else View.GONE)
            views.setTextColor(R.id.widget_title, p.text)
            views.setTextColor(R.id.widget_clock, p.text)
            views.setTextViewTextSize(R.id.widget_title, TypedValue.COMPLEX_UNIT_SP, 32f * fs)
            if (clock) views.setTextViewTextSize(R.id.widget_clock, TypedValue.COMPLEX_UNIT_SP, look.clockSize.toFloat())
            views.setTextColor(R.id.widget_date, p.secondary)
            views.setTextViewTextSize(R.id.widget_date, TypedValue.COMPLEX_UNIT_SP, 14f * fs)
            views.setTextColor(R.id.widget_empty, p.secondary)
            views.setTextViewTextSize(R.id.widget_empty, TypedValue.COMPLEX_UNIT_SP, 15f * fs)
            views.setTextViewText(R.id.widget_title, Lang.tr(Lang.resolve(Store.get(c).language), "Today"))
            views.setTextViewText(R.id.widget_empty, Lang.tr(Lang.resolve(Store.get(c).language), "Nothing planned for today"))
            views.setTextViewText(
                R.id.widget_date,
                today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))
            )

            val svc = Intent(c, TasksWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_list, svc)
            views.setEmptyView(R.id.widget_list, R.id.widget_empty)

            val receiver = if (clock) TasksClockWidget::class.java else TasksWidget::class.java
            val template = PendingIntent.getBroadcast(
                c, 0, Intent(c, receiver).setAction(TasksWidget.ACTION_TOGGLE),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_list, template)
            views.setOnClickPendingIntent(
                R.id.widget_header,
                PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            )
            mgr.updateAppWidget(id, views)
        }
    }
}

class TasksWidget : BaseTasksWidget(false) {
    companion object {
        const val ACTION_TOGGLE = "com.kirillshybaiev.dailytracker.WIDGET_TOGGLE"

        fun refresh(c: Context) {
            val mgr = AppWidgetManager.getInstance(c)
            listOf(TasksWidget::class.java to false, TasksClockWidget::class.java to true).forEach { (cls, clock) ->
                val ids = mgr.getAppWidgetIds(ComponentName(c, cls))
                if (ids.isNotEmpty()) {
                    ids.forEach { build(c, mgr, it, clock) }
                    mgr.notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
                }
            }
            listOf(CalendarWidget::class.java to BitmapWidget.CALENDAR, InsightsWidget::class.java to BitmapWidget.HEATMAP).forEach { (cls, kind) ->
                mgr.getAppWidgetIds(ComponentName(c, cls)).forEach { BitmapWidget.render(c, mgr, it, kind) }
            }
        }
    }
}

class TasksClockWidget : BaseTasksWidget(true)

class TasksWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        Factory(applicationContext, intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1))

    private class Factory(val c: Context, val widgetId: Int) : RemoteViewsFactory {
        private var state = AppState()
        private var items = listOf<Task>()
        private var today = LocalDate.now()
        private var look = WidgetLook()
        private var p = WidgetStyle.palette(c, look)

        override fun onCreate() {}
        override fun onDataSetChanged() {
            state = Store.get(c)
            today = LocalDate.now()
            items = state.tasksOn(today)
            look = WidgetPrefs.look(c, widgetId)
            p = WidgetStyle.palette(c, look)
        }
        override fun onDestroy() {}
        override fun getCount() = items.size
        override fun getViewAt(position: Int): RemoteViews {
            val t = items[position]
            val done = state.isDone(t, today)
            val rv = RemoteViews(c.packageName, R.layout.widget_item)

            val circleColor = t.color ?: if (done) p.primary else p.ring
            rv.setImageViewResource(R.id.item_circle, if (done) R.drawable.widget_dot else R.drawable.widget_ring)
            rv.setInt(R.id.item_circle, "setColorFilter", circleColor)
            rv.setViewVisibility(R.id.item_tick, if (done) View.VISIBLE else View.GONE)
            rv.setTextColor(R.id.item_tick, if (t.color != null) 0xFFFFFFFF.toInt() else p.onPrimary)

            val fs = look.fontScale
            val sz = look.iconScale
            rv.setTextViewTextSize(R.id.item_title, TypedValue.COMPLEX_UNIT_SP, 16f * fs)
            rv.setTextViewTextSize(R.id.item_time, TypedValue.COMPLEX_UNIT_SP, 12f * fs)
            rv.setTextViewTextSize(R.id.item_tick, TypedValue.COMPLEX_UNIT_SP, 14f * sz)
            if (Build.VERSION.SDK_INT >= 31) {
                listOf(R.id.item_circle_box, R.id.item_circle, R.id.item_tick).forEach {
                    rv.setViewLayoutWidth(it, 24f * sz, TypedValue.COMPLEX_UNIT_DIP)
                    rv.setViewLayoutHeight(it, 24f * sz, TypedValue.COMPLEX_UNIT_DIP)
                }
                rv.setViewLayoutMargin(R.id.item_divider, RemoteViews.MARGIN_START, 24f * sz + 14f, TypedValue.COMPLEX_UNIT_DIP)
            }

            rv.setTextViewText(R.id.item_title, t.title)
            rv.setTextColor(R.id.item_title, if (done) p.secondary else p.text)
            rv.setInt(R.id.item_title, "setPaintFlags", Paint.ANTI_ALIAS_FLAG or if (done) Paint.STRIKE_THRU_TEXT_FLAG else 0)

            if (t.time != null) {
                rv.setViewVisibility(R.id.item_time, View.VISIBLE)
                rv.setTextViewText(R.id.item_time, t.time.toString())
                rv.setTextColor(R.id.item_time, p.secondary)
            } else rv.setViewVisibility(R.id.item_time, View.GONE)

            rv.setInt(R.id.item_divider, "setBackgroundColor", p.divider)
            rv.setViewVisibility(R.id.item_divider, if (position == items.size - 1) View.INVISIBLE else View.VISIBLE)

            rv.setOnClickFillInIntent(R.id.item_root, Intent().putExtra("id", t.id))
            return rv
        }
        override fun getLoadingView(): RemoteViews? = null
        override fun getViewTypeCount() = 1
        override fun getItemId(position: Int) = items[position].id.hashCode().toLong()
        override fun hasStableIds() = true
    }
}
