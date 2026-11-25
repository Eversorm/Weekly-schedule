# Weekly Schedule

A small personal Android app to plan and track a weekly routine (German, climbing, running, …).
Free, offline, no account, no ads. All data stays on the phone; backups are JSON files you export yourself.

**Status: Phase 1** — activities with weekly targets, done tracking, weekly screen, export/import.
Coming next: Phase 2 notifications, Phase 3 home screen widget, Phase 4 skip & reschedule suggestions.

---

## How to use it

- **Main screen (week):** goals at the top (`done / target` for each activity), then Monday → Sunday.
  - Tap a session to mark it done (tap again to undo).
  - `+1` next to a goal = "I did this today": marks today's planned session done, or records an extra one.
  - `+` next to a day adds a session of any activity. `×` removes a session.
  - Arrows at the top move between weeks.
- **Activities** (list icon, top right): add/edit activities, their weekly target, and their *typical days*.
  Every new week is pre-filled once from the typical days, the first time you open that week.
  Archiving an activity hides it but keeps its history.
- **Settings** (gear icon): export a backup file / import one (replaces all data).

---

## Build and install (Android Studio)

### One-time setup
1. Install **Android Studio** (free): https://developer.android.com/studio
2. Open it and choose **File → New → Project from Version Control** (on the welcome screen: **Clone Repository**).
   URL: `https://github.com/Eversorm/weekly-schedule` → **Clone**. Log in to GitHub if asked.
3. Wait for the "Gradle sync" to finish (progress bar at the bottom; the first time it downloads a lot, several minutes).
   If Android Studio asks to install a missing SDK platform (Android 17 / API 37.2), accept.

### Prepare your phone (once)
1. **Settings → About phone →** tap **Build number** 7 times → "You are now a developer".
2. **Settings → System → Developer options →** turn on **USB debugging**.
   (Menu names vary a bit between phone brands; search "USB debugging" in Settings.)
3. Connect the phone with a USB cable and accept the "Allow USB debugging?" prompt on the phone.

### Install / update
- In Android Studio, select your phone in the device drop-down at the top, then press the green **▶ Run** button.
  The app is built, installed and started. Later updates: pull the new code (**Git → Pull**) and press ▶ again — your data is kept.

**Alternative without cable:** **Build → Generate App Bundles or APKs → Generate APKs**. When finished, click "locate";
the file is `app/build/outputs/apk/debug/app-debug.apk`. Copy it to the phone (e.g. Google Drive), open it and allow
"Install unknown apps" for that app when asked.

### ⚠️ Important: the signing key
Android only accepts an update if it is signed with the **same key** as the installed version. Otherwise you must
uninstall first, which **deletes all data**. Android Studio signs debug builds with a key stored on your computer:

- Windows: `C:\Users\<you>\.android\debug.keystore`
- macOS/Linux: `~/.android/debug.keystore`

Keep building from the same computer, and copy that file somewhere safe. If you ever change computer, copy it back
to the same place. And in any case: **export a backup in the app before any uninstall.**

### Run the unit tests
In Android Studio, open `app/src/test/...`, right-click the `domain` folder → **Run 'Tests in domain'**.

---

## Permissions

| Phase | Permission | Why |
|---|---|---|
| 1 | *none* | Backup uses the system file picker, which needs no storage permission. The app has no internet permission at all. |
| 2 | `POST_NOTIFICATIONS` | Show reminders (Android 13+ asks you once) |
| 2 | `USE_EXACT_ALARM` | Reminders at the exact time you chose (auto-granted for an app you install yourself) |
| 2 | `RECEIVE_BOOT_COMPLETED` | Re-create reminders after the phone restarts |

---

## Project structure (for reference)

```
app/src/main/java/com/eversorm/weeklyschedule/
├── RoutineApp.kt        app start: creates the database once
├── MainActivity.kt      the single window; shows the Compose UI
├── data/                storage (Room / SQLite)
│   ├── Entities.kt        tables: Activity, TemplateSlot (typical week), Session, InitializedWeek
│   ├── RoutineDao.kt      all database queries
│   ├── RoutineDatabase.kt database definition + date conversion
│   ├── DefaultData.kt     activities created on first launch
│   └── RoutineRepository.kt  the API the rest of the app uses
├── domain/              pure logic, unit-tested (week dates, progress)
├── backup/              JSON export/import
└── ui/                  screens (Week, Activities, Settings), their ViewModels, theme
app/src/main/AndroidManifest.xml   app components and permissions
app/src/main/res/                  icon, app name, window theme
app/schemas/                       database structure history (needed for future migrations — keep it)
gradle/libs.versions.toml          all library versions in one place
```
