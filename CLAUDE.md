# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

**SmartRoad** — crowdsourced road hazard reporting Android app (ICT602 Mobile Technology, due 5 July 2026).

Users log in via Google, get geo-located, report road hazards with a photo, and see hazard markers on a map. A companion admin web panel provides full CRUD over reports via the same Firebase backend.

## Repository layout

```
SmartRoad/          ← Android app (this repo root)
  app/
  gradle/
  ...
admin-web/          ← Vue.js admin panel (separate project, same repo)
  src/
  package.json
  vite.config.js
  ...
```

## Android build commands

All Gradle commands run from the repo root via the wrapper:

```
# Build debug APK
.\gradlew assembleDebug

# Run unit tests
.\gradlew test

# Run instrumented tests (device/emulator required)
.\gradlew connectedAndroidTest

# Install debug APK to connected device
.\gradlew installDebug

# Clean build
.\gradlew clean
```

Build in Android Studio: **Build > Make Project** (`Ctrl+F9`). Run on device/emulator: **Run > Run 'app'** (`Shift+F10`).

## Admin web panel build commands

Run from `admin-web/`:

```
npm install       # first-time setup
npm run dev       # dev server (Vite)
npm run build     # production build → admin-web/dist/
```

## Android tech stack (do not change without discussion)

- **Language**: Java only — no Kotlin. All activities, adapters, and helpers must be `.java` files.
- **UI**: XML layouts (`res/layout/`) with traditional Views — no Jetpack Compose.
- **Firebase Realtime Database** (not Firestore) — use `DatabaseReference`, `.push()`, `addValueEventListener` patterns from ICT602 Week 10/13 labs.
- **Firebase Authentication** — Google Sign-In only (Week 8 lab pattern).
- **Firebase Storage** — for hazard photo uploads.
- **Google Maps SDK** — `SupportMapFragment`, `BitmapDescriptorFactory` for custom markers (Week 12/13 pattern).
- **Location**: `FusedLocationProviderClient` + `Geocoder` reverse geocoding (Week 11 pattern).

## Admin panel tech stack

- **Framework**: Vue.js 3 (Composition API) — chosen because its reactivity model maps cleanly onto a live-updating report table: the Firebase `onValue` listener writes into a `ref([])`, and the template re-renders automatically. Doing the same with plain JS requires manual DOM updates.
- **Build tool**: Vite
- **Firebase**: Firebase JS SDK v10 (`firebase/app`, `firebase/auth`, `firebase/database`, `firebase/storage`) — same Realtime Database the Android app writes to, so no backend changes needed.
- **Styling**: Plain CSS (no framework) — keeps the bundle small and avoids introducing an unfamiliar dependency.

## App ID and package

`com.example.smartroad` — `minSdk 30`, `targetSdk 36`.

## Firebase Realtime Database schema

```
smartroad-default-rtdb/
  hazard_reports/
    -NxAbC123/
      id, uid, type, description, latitude, longitude,
      photoUrl, status, timestamp, userAgent
  users/
    uid123/
      name, email, totalReports, resolvedReports
```

Security rules: `.read: true`, `.write: auth != null`.

Hazard `status` values: `New`, `Under Investigation`, `Resolved`.
Hazard `type` values: `Pothole`, `Flood`, `Accident`, `Fallen Tree`, `Traffic Light`.

## Android screens (all implemented)

1. **LoginActivity** — Google Sign-In, title bar shows "SMARTROAD"
2. **HomeActivity** — greet by `user.getDisplayName()`, GPS card, embedded map with Your Location + hazard markers, bottom nav (Report / Map / Profile)
3. **ReportHazardActivity** — radio buttons for type, description, photo picker, auto-filled GPS + date/time, submits to Realtime DB + Storage; increments `users/{uid}/totalReports`
4. **HazardMapActivity** — full-screen map, color-coded markers by type, Filter dialog, Refresh; tapping info window → HazardDetailsActivity
5. **HazardDetailsActivity** — full report view, Glide photo, Change Status dialog; updates `resolvedReports` counter on reporter when status toggles to/from Resolved
6. **ProfileActivity** — initials avatar, name/email, live Total/Resolved stats, About button, logout with confirmation
7. **AboutActivity** — developer info, copyright, tappable GitHub URL (rubric requirement)

## Admin panel pages (to build in admin-web/)

1. **Dashboard** — summary cards (total users, total reports, open, resolved) + recent reports table
2. **Manage Reports** — searchable/filterable table (type, status, date); View / Update Status / Delete actions
3. **Report Details** — full report info, photo, status dropdown, Save button

## Rubric weights (30 marks total)

| Item | Marks |
|------|-------|
| Current location + hazard map with markers | 6 |
| Multi-screen UI/UX consistency | 6 |
| Admin web panel with full CRUD | 6 |
| Login & profile display | 4 |
| Basic map display | 4 |
| About page with clickable URL | 4 |

## Evaluation checklist

- Custom app colors in `res/values/colors.xml`, custom app name in title bar
- Unique launcher icon
- Source code pushed to a public GitHub repo (`https://github.com/haziqnaqib11/SmartRoad`)
- About page has a clickable URL pointing to that GitHub repo — update `strings.xml` once repo is live
- Video demo emphasizing rubric criteria
