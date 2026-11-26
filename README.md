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

## Build and install (no computer needed)

GitHub builds the app on its own servers every time new code is pushed to `main`
(see `.github/workflows/build.yml`). Each build becomes a **Release** with the APK attached.

### Install or update the app (on the phone)
1. Open https://github.com/Eversorm/weekly-schedule/releases in the phone's browser (logged in to GitHub),
   or in the GitHub app: repository → **Releases**.
2. Open the newest **Build N** → under *Assets* tap `weekly-schedule-N.apk` to download it.
3. Open the downloaded file. The first time, Android asks to allow installing apps from your browser:
   allow it, go back, tap **Install** (or **Update**). Play Protect may warn about an unknown app: choose
   **More details → Install anyway**.

Updates install over the old version and keep your data, because every build is signed with the same key.

### Check a build
Repository → **Actions** tab: a green ✓ means the build worked, a red ✗ that it failed (tap it to see why).
To build again by hand: Actions → **Build APK** → **Run workflow**.

### ⚠️ The signing key
The key is stored in the repository's secrets (Settings → Secrets and variables → Actions):
`SIGNING_KEYSTORE_BASE64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`.
GitHub never shows secrets again after saving them, so **keep your own copy** of the key file
(`weekly-schedule-release.jks`) and its password somewhere safe (e.g. Google Drive / a password manager).
If the key is lost, the next version cannot update the installed app: you would have to export a backup,
uninstall, install, and import.

**Before any uninstall: export a backup in the app (Settings → Export).**

### Optional: Android Studio on a computer
Useful only for debugging crashes (the *Logcat* panel shows the exact error).
**File → New → Project from Version Control** → `https://github.com/Eversorm/weekly-schedule`.
Builds made there are signed with a different (debug) key, so they cannot update the GitHub version:
uninstall first (after exporting a backup).

### Run the unit tests
They run automatically in every GitHub build. In Android Studio: right-click `app/src/test/.../domain` → **Run**.

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
.github/workflows/build.yml        GitHub build: tests, signed APK, Release
```
