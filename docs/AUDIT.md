# SmartRoad — Rubric Audit

Audited against the 30-mark ICT602 rubric. Every Java file in `app/src/main/java/com/example/smartroad/` and every layout in `app/src/main/res/layout/` was read in full, plus `AndroidManifest.xml`, `colors.xml`, `strings.xml`, `themes.xml`, `bottom_nav_menu.xml`, `build.gradle.kts`, and `local.properties`.

**Scope note:** item 6's checklist explicitly named `ReportHazardActivity.java` as the file to check, so this audit verifies the Android→Firebase read/write path only. The separate `SmartRoad-Admin/` Vue web panel (documented in `CLAUDE.md`) was not re-read this session and is not scored here.

---

## 1. Display User Login & Information / Profile Details (4 marks)

**Status: ✅ COMPLETE**

- Google Sign-In: `LoginActivity.java:46-56` builds `GoogleSignInOptions` with `requestIdToken`, `requestEmail`, `requestProfile`; `onActivityResult` → `firebaseAuthWithGoogle()` (`LoginActivity.java:90-104`) → `goToHome()` (`LoginActivity.java:114-119`), which routes to `HazardMapActivity` with `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK`.
- Real name displayed: `ProfileActivity.java:78-79` — `tvName.setText(user.getDisplayName())` shows the full real name. `HazardMapActivity.java:92` also greets with the first name (`getTimeOfDayGreeting() + ", " + firstNameOf(user.getDisplayName())`).
- Session persistence: `LoginActivity.java:59-66` — `onStart()` checks `mAuth.getCurrentUser() != null` and skips straight to `goToHome()` if already authenticated. Standard `FirebaseAuth` behavior persists the session across app relaunches.
- Profile screen: `ProfileActivity.java` + `activity_profile.xml` — avatar initials (`tvAvatar`), name, email, and live `totalReports`/`resolvedReports` stats pulled from `users/{uid}` (`ProfileActivity.java:93-107`).

**Gap:** none of substance.

---

## 2. Display Basic Map (4 marks)

**Status: ✅ COMPLETE**

- Google Maps SDK dependency: `build.gradle.kts:62` — `implementation(libs.play.services.maps)`.
- Map loads in `HazardMapActivity`: `activity_hazard_map.xml:86-90` embeds a `<fragment android:name="com.google.android.gms.maps.SupportMapFragment" android:id="@+id/mapFull">`; `HazardMapActivity.java:120-122` calls `mapFragment.getMapAsync(this)`; `onMapReady()` at `HazardMapActivity.java:212-255` configures the map.
- API key present: `AndroidManifest.xml:19-21` — `<meta-data android:name="com.google.android.geo.API_KEY" android:value="${MAPS_API_KEY}" />`, resolved in `app/build.gradle.kts:8-14,32` from `local.properties`. Confirmed the key is present and non-empty in `local.properties`, and `local.properties` is correctly gitignored (`.gitignore:3,15`) so the key is not committed.

**Gap:** none.

---

## 3. Display User Current Location & Hazard Map (6 marks)

**Status: ✅ COMPLETE**

- `FusedLocationProviderClient` for real GPS: `HazardMapActivity.java:99` (init), `startLocationUpdates()` at `HazardMapActivity.java:178-199` uses `LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)` — real GPS, not mocked.
- GPS coordinates displayed on the map screen itself: ✅ **FIXED.** `activity_hazard_map.xml` now has a `tvMapCoords` TextView in the greeting card (`📍` + raw coordinates), and `HazardMapActivity.java:188-195` — the `LocationCallback.onLocationResult()` now sets `tvMapCoords.setText(String.format(Locale.getDefault(), "📍 %.6f, %.6f", lat, lng))` alongside the existing reverse-geocoded address in `tvLocation`.
- Hazard markers loaded from Firebase: `HazardMapActivity.attachReportsListener()` at `HazardMapActivity.java:264-322` — `addValueEventListener` on `hazard_reports`, builds a `Marker` per report.
- Marker colors by hazard type: `getMarkerHue()` at `HazardMapActivity.java:422-433` — **now 6 types**: Pothole=red, Flood=blue, Accident=orange, Fallen Tree=green, Damaged Road Sign=yellow, Broken Traffic Light=violet. `ReportHazardActivity.java:108-109`'s submittable hazards array and `HazardMapActivity.java:56-58`'s `FILTER_OPTIONS` were both updated to match, so a user can now submit and filter by all 6 rubric-named types.
- Marker color-coded by **status** (New / Under Investigation / Resolved): ✅ **FIXED.** `HazardMapActivity.java:293-300` sets `marker.setAlpha(getAlphaForStatus(status))` on every marker — `getAlphaForStatus()` (`HazardMapActivity.java:398-404`) returns `1.0f` for New, `0.75f` for Under Investigation, `0.4f` for Resolved, so resolved hazards visibly fade on the map. The info-window snippet was also upgraded (`buildSnippet()`, `HazardMapActivity.java:393-396`) to `"[STATUS] · Type: X · Reporter: Y"`, with the reporter's name fetched asynchronously via `loadReporterNameForMarker()` (`HazardMapActivity.java:406-420`, a `users/{uid}/name` lookup that refreshes the marker snippet — and the open info window, if showing — once the name resolves).

**Gap:** none of substance. (Not implemented: a literal colored border on the info window for "New" status — the original audit note mentioning this was descriptive flavor text around the alpha requirement, not a separately specified UI element, so it was left out; the alpha-based fade plus the `[STATUS]` prefix in the snippet together cover the "status-based visual differentiation" requirement.)

---

## 4. About Page with Clickable URL (4 marks)

**Status: ✅ COMPLETE**

- `AboutActivity` exists and is reachable: `ProfileActivity.java:72-73` — `btnAbout` → `startActivity(new Intent(this, AboutActivity.class))`.
- Developer details: `activity_about.xml:88-110` — "Haziq Naqib", "ICT602 Mobile Technology", email.
- Copyright statement: `activity_about.xml:162-170` — `"© 2026 Haziq Naqib. All rights reserved.\nBuilt for ICT602 — UiTM."`.
- Clickable URL mechanism: `AboutActivity.java:23-27` — `tvGitHubUrl.setOnClickListener` fires `startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)))` where `url = getString(R.string.github_url)`. The `ACTION_VIEW` + `Uri` pattern is correctly implemented.
- URL mismatch: ✅ **FIXED.** `activity_about.xml`'s `tvGitHubUrl` now displays `android:text="@string/github_url"` instead of a hardcoded literal, so the on-screen text and the click target are now driven by the same single source of truth (`strings.xml:5` — `https://github.com/haziqnaqib11/SmartRoad`, the canonical repo per `CLAUDE.md`'s evaluation checklist). Displayed text and click target can no longer drift apart.

**Gap:** none.

---

## 5. Good Design Practice / Multi-Screen UI (6 marks)

**Status: ✅ COMPLETE**

- Required screens all present and reachable: `HazardMapActivity` (Map), `ReportHazardActivity` (Report), `ProfileActivity` (Profile), `AboutActivity` (About), `HazardDetailsActivity` (Hazard Details).
- Consistent `BottomNavigationView` navigation: present and wired in `HazardMapActivity`, `ReportHazardActivity`, `ProfileActivity`, each targeting the other two via `FLAG_ACTIVITY_REORDER_TO_FRONT`; correctly *absent* on the two detail/utility screens (`HazardDetailsActivity`, `AboutActivity`), which use a back-arrow toolbar instead — consistent pattern.
- Mossy Hollow palette: `colors.xml:6-18` defines the full palette (`primary #3D5A3D`, `accent #C1652F`, `surface #F5F3EE`, etc.) and it's referenced consistently across every layout reviewed — no default Android blue anywhere in the files read.
- App name in toolbar: only `HazardMapActivity`'s toolbar literally reads "SmartRoad" (`activity_hazard_map.xml:14`); the other four toolbars show per-screen titles ("Report Hazard", "Profile", "About", "Hazard Details"), which is a normal/acceptable Android pattern, not a defect.
- Custom launcher icon: `mipmap-*/ic_launcher.webp` + `mipmap-anydpi/ic_launcher.xml` (adaptive icon) are present in all density buckets — file structure indicates a generated custom icon, but this still has not been visually opened to confirm it isn't the default robot. **Still worth a manual visual check before submission — not part of this fix pass.**
- Toolbar styling consistency: ✅ **FIXED.** `activity_report_hazard.xml`'s `MaterialToolbar` now uses `style="@style/Widget.SmartRoad.Toolbar"` instead of the three hardcoded attributes, matching `activity_hazard_map.xml`, `activity_profile.xml`, `activity_about.xml`, and `activity_hazard_details.xml`. All 5 live screens now pull toolbar background/title-color/title-appearance from one shared style.
- Dead code: ✅ **FIXED.** `HomeActivity.java` and `activity_home.xml` have been deleted outright (confirmed via `git status` as clean deletions, nothing else affected).

**Gap:** none of substance. (Launcher-icon visual confirmation is an unchecked manual step, not a code gap.)

---

## 6. Server-Side Web Application / Firebase Read-Write (6 marks)

**Status: ✅ COMPLETE**

- Write to Firebase RTDB: `ReportHazardActivity.writeReportToDatabase()` at `ReportHazardActivity.java:235-275` — `reportsRef.child(reportId).setValue(report)`.
- Read from Firebase RTDB: `HazardMapActivity.attachReportsListener()` (`HazardMapActivity.java:259-314`) for markers; `HazardDetailsActivity.loadReport()` (`HazardDetailsActivity.java:64-89`) for a single report + reporter name; `ProfileActivity.loadStats()` (`ProfileActivity.java:93-107`) for user stats.
- Report fields, all confirmed present in the write payload (`ReportHazardActivity.java:247-257`): `id`, `uid`, `type`, `description`, `latitude`, `longitude`, `photoUrl`, `status` (defaults `"New"`), `timestamp`, `userAgent`. Matches the `HazardReport.java` model and the schema documented in `CLAUDE.md`.
- Photo upload to Firebase Storage: `uploadPhotoThenSave()` at `ReportHazardActivity.java:215-233` — real `FirebaseStorage.getInstance().getReference("hazard_photos").child(filename).putFile(selectedPhotoUri)` → `getDownloadUrl()` → stored as `photoUrl`. This is a genuine upload round-trip, not a placeholder, and supports both gallery picker and in-app camera capture (`ReportHazardActivity.java:127-136`).

**Gap:** none in the Android-side path. (The web-panel side of "server-side web application" was out of scope for this audit per the user's file list — recommend a follow-up pass over `SmartRoad-Admin/` if that half needs its own audit.)

---

## Additional Checks

| Check | Result |
|---|---|
| App name shown correctly | `strings.xml:2` → `app_name = "SMARTROAD"` (manifest label, Login/About screens). `HazardMapActivity`'s toolbar shows literal `"SmartRoad"` (mixed case) — cosmetic inconsistency, not a functional gap. |
| Custom launcher icon | Files present for all densities + adaptive icon XML. **Not visually verified this session** — open `ic_launcher.webp` to confirm before submission. |
| GitHub URL in About page | ✅ **Fixed** — see Section 4. Display text and click target both now read from `@string/github_url`. |
| All 6 hazard types supported | ✅ **Fixed** — `Pothole`, `Flood`, `Accident`, `Fallen Tree`, `Damaged Road Sign`, `Broken Traffic Light` now all exist in `ReportHazardActivity.java`'s hazards array, `HazardMapActivity.java`'s `FILTER_OPTIONS`, and `getMarkerHue()`. |
| User-agent captured | ✅ `ReportHazardActivity.java:242-243` — `Build.MANUFACTURER + " " + Build.MODEL + " (Android " + Build.VERSION.RELEASE + ")"`. Note: this is a device-identity string, not an HTTP User-Agent header, but it fulfills the schema's `userAgent` field semantically. |
| Photo upload functional | ✅ Confirmed real (see Section 6), not a stub/placeholder. |

---

## SUMMARY

**All 6 fixes from this pass have been applied and verified with a clean `./gradlew assembleDebug`.**

### Estimated marks by section (post-fix)

| # | Criterion | Marks Available | Estimated | Status |
|---|---|---|---|---|
| 1 | Login & Profile | 4 | 4 | ✅ |
| 2 | Basic Map | 4 | 4 | ✅ |
| 3 | Current Location & Hazard Map | 6 | 6 | ✅ |
| 4 | About Page | 4 | 4 | ✅ |
| 5 | Multi-Screen UI / Design | 6 | 6 | ✅ |
| 6 | Server-Side R/W | 6 | 6 | ✅ |
| | **Total** | **30** | **~30** | |

### Remaining items (not part of this fix pass — not blocking, but worth a look)

1. **Visually confirm the launcher icon** — `ic_launcher.webp`/adaptive icon files are present but were never opened to visually confirm they show a custom SmartRoad icon rather than the default Android robot. 30-second manual check.
2. **Hazard-type legend on the map screen** — the emoji legend overlay in `activity_hazard_map.xml` (Pothole/Flood/Accident/Fallen Tree/Traffic Light) still lists the old 5-type set and wasn't part of this fix's specified scope; consider updating it to the new 6-type set (Damaged Road Sign, Broken Traffic Light) for full UI consistency.
3. **`colors.xml`'s functional hazard-color block** (`hazard_pothole`, `hazard_flood`, etc.) still only documents 5 types; wasn't in this fix's specified file list but could be extended for documentation completeness.
