# Tasker

A calm, minimal daily task and habit tracker for Android, built with Kotlin and Jetpack Compose.

## Features

- **Today screen** with a simple "How it's going" score and a progress bar with one segment per task.
- **Tasks** with title, description, time, colour, and repeat rules (every N days or chosen weekdays).
- **Skip, move to tomorrow, or shift** a repeating task by one day.
- **Calendar** with a month grid; each day is tinted by how much you completed, and tapping a day shows only that day's tasks.
- **Holidays for every country**, bundled with the app (works offline), with names in each country's own language.
- **Insights** for the last 7, 30 or 90 days: completion rate, streak, weekly bars and per-task stats.
- **Home-screen widgets** and reminders.
- **Side menu** with Calendar and Settings, with an optional blur effect on Android 12+.
- **Full-screen description editor** for longer task notes.
- **Languages:** English, Spanish, German, French, Italian, Portuguese, Polish, Russian, Ukrainian and Turkish.

All data is stored locally on the device, in a JSON file in the app's private storage. There is no account and no cloud sync.

## Tech stack

- Kotlin, Jetpack Compose, Material 3
- Gradle (Kotlin DSL), Android Studio
- Package: `com.kirillshybaiev.dailytracker`
- Min SDK 26

## Build

1. Open the project in Android Studio.
2. Let Gradle sync.
3. Run on a device or emulator, or build an APK with **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.

The debug APK is created at `app/build/outputs/apk/debug/app-debug.apk`.

## Install

Download the APK from the **Releases** page of this repository and open it on your phone. You may need to allow installing apps from unknown sources.

## Project layout

```
app/src/main/java/com/kirillshybaiev/dailytracker/
  Data.kt          models, storage, translations, holidays, reminders, widgets
  Ui.kt            theme, shared components, animations
  Home.kt          Today screen, calendar, task editor
  Pages.kt         insights, settings, language and holiday screens
  MainActivity.kt  entry point, navigation, side menu
app/src/main/assets/
  translations.tsv UI translations
  holidays/        per-country holiday files
```
