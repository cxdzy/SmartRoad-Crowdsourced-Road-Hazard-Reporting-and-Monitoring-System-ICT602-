# SmartRoad — Final Comprehensive Audit (Mobile + Web Admin)

**Scope:** Full system audit against the 30-mark ICT602 rubric, covering both the Android app (`app/src/main/`) and the SmartRoad-Admin Vue 3 web panel (`SmartRoad-Admin/src/`). Every Java file, every Android layout, every Vue component/view, and the shared CSS were read in full for this pass.

**Backend confirmation:** Both apps target the **same Firebase project** — `smartroad-1bf13` (project number `428556923249`). Verified: Android `app/google-services.json:2-7` and admin `SmartRoad-Admin/src/firebase.js:6-14` share the identical `project_id`, `databaseURL` (`asia-southeast1`), and `storageBucket`. The admin panel reads/writes the same `hazard_reports`, `users`, and `admins` nodes the mobile app writes to — no backend divergence.

**Build status:** `./gradlew assembleDebug` passes cleanly (last verified this session).

---

## 1. Display User Login & Information / Profile (4 marks)

**Status: ✅ COMPLETE**

**Mobile:**
- Google Sign-In configured and functional: `LoginActivity.java:46-56` (`GoogleSignInOptions` with `requestIdToken`/`requestEmail`/`requestProfile`) → `firebaseAuthWithGoogle()` `LoginActivity.java:90-104` → `goToHome()` `LoginActivity.java:114-119` routes to `HazardMapActivity` with `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK`.
- Greets by real name: `HazardMapActivity.java:91-94` — `getTimeOfDayGreeting() + ", " + firstNameOf(user.getDisplayName())`.
- Profile shows real name + email + live stats: `ProfileActivity.java:78-80` (name/email), `82-90` (initials avatar), `93-107` (`totalReports`/`resolvedReports` from `users/{uid}`).
- Session maintained: `LoginActivity.java:59-66` — `onStart()` skips to home if `mAuth.getCurrentUser() != null`; FirebaseAuth persists across relaunch.
- Logout returns to login: `ProfileActivity.java:109-121` — `FirebaseAuth.signOut()` + intent to `LoginActivity` with `CLEAR_TASK`.

**Web Admin:**
- Login page functional: `Login.vue:14-31` — `signInWithEmailAndPassword` then verifies `admins/{uid} === true`, signing back out unauthorized accounts.
- Session maintained / protected routes: `router/index.js:18-28` — `beforeEach` guard awaits `waitForAuthReady()` and redirects any non-login route to `/login` unless `currentUser && isAdmin`; `auth.js:10-19` reactively tracks auth + admin status.

**Gap:** none.

---

## 2. Display Basic Map (4 marks)

**Status: ✅ COMPLETE**

**Mobile:**
- Google Maps loads: `activity_hazard_map.xml` embeds `SupportMapFragment` (`@+id/mapFull`); `HazardMapActivity.java:122-124` calls `getMapAsync(this)`; `onMapReady()` `HazardMapActivity.java:218-260`.
- Interactive (zoom/pan): `HazardMapActivity.java:220-221` enables zoom controls + compass; pan/zoom are default `GoogleMap` gestures.
- API key configured: `AndroidManifest.xml:19-21` `com.google.android.geo.API_KEY = ${MAPS_API_KEY}`, injected from `local.properties` via `app/build.gradle.kts:8-14,32` (key present, and `local.properties` is gitignored).

**Gap:** none.

---

## 3. Display User Current Location & Hazard Map (6 marks)

**Status: ✅ COMPLETE (mobile) / ⚠️ minor web legend gap**

**Mobile:**
- Real-time GPS via `FusedLocationProviderClient`: `HazardMapActivity.java:99` (init), `startLocationUpdates()` `178-199` with `PRIORITY_HIGH_ACCURACY`.
- Current GPS coordinates shown on screen: `HazardMapActivity.java:191-194` sets `tvMapCoords` to `"📍 %.6f, %.6f"`; the `tvMapCoords` TextView lives in the greeting card in `activity_hazard_map.xml`. (Reverse-geocoded address also shown in `tvLocation`.)
- Markers from Firebase: `attachReportsListener()` `HazardMapActivity.java:264-322` — `addValueEventListener` on `hazard_reports`.
- 6 distinct type colors: `getMarkerHue()` `HazardMapActivity.java:422-433` — Pothole=red, Flood=blue, Accident=orange, Fallen Tree=green, **Damaged Road Sign=yellow**, **Broken Traffic Light=violet**. Submittable list matches (`ReportHazardActivity.java:108-109`); filter list matches (`HazardMapActivity.java:56-58`); on-map legend matches all 6 (`activity_hazard_map.xml` legend overlay).
- Status-based marker visual: `HazardMapActivity.java:294` `marker.setAlpha(getAlphaForStatus(status))`; `getAlphaForStatus()` `398-404` → New=1.0, Under Investigation=0.75, Resolved=0.4 (resolved hazards fade). Info-window snippet `"[STATUS] · Type: X · Reporter: Y"` via `buildSnippet()` `393-396`, reporter resolved async `406-420`.

**Web Admin:**
- Hazard map with markers on dashboard: `Dashboard.vue:74-77` renders `<HazardMap :reports="reportList">`; `HazardMap.vue:65-117` plots one custom SVG pin per report, clickable info window with "View details" → report page.

**Gap (web only):** `HazardMap.vue:12-18`'s `LEGEND`/color map and `ManageReports.vue:10`'s `HAZARD_TYPES` still list the **old 5 types** (`Pothole, Flood, Accident, Fallen Tree, Traffic Light`). Since the mobile app now writes `"Damaged Road Sign"` and `"Broken Traffic Light"`, those two types render on the admin map as the **gray default pin** (`HazardMap.vue:20`, `DEFAULT_COLOR = '#9e9e9e'`) with no legend entry, and cannot be selected in the Manage Reports **type filter**. Such reports still appear in the (unfiltered) table with correct type text, so no data is lost — but the map color-coding and type filter are incomplete for the two newest types.

**Fix:** update `HazardMap.vue:12-18` `LEGEND` and `ManageReports.vue:10` `HAZARD_TYPES` to the same 6-type list the mobile app uses (add `Damaged Road Sign` + rename `Traffic Light` → `Broken Traffic Light`, with colors matching the mobile hues — yellow + violet/purple). Low effort, ~2 lines each.

---

## 4. About Page with Clickable URL (4 marks)

**Status: ✅ COMPLETE**

**Mobile:**
- Reachable: `ProfileActivity.java:72-73` — About button → `AboutActivity`.
- Developer details: `activity_about.xml` — "Haziq Naqib", "ICT602 Mobile Technology", email.
- Copyright: `activity_about.xml` — `"© 2026 Haziq Naqib. All rights reserved.\nBuilt for ICT602 — UiTM."`.
- Clickable URL via `ACTION_VIEW`: `AboutActivity.java:23-27` — `startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_url))))`.
- Displayed URL == clicked URL: both are driven by `@string/github_url` — `activity_about.xml`'s `tvGitHubUrl` shows `android:text="@string/github_url"` and the click handler reads the same resource (`strings.xml:5` = `https://github.com/haziqnaqib11/SmartRoad`). Single source of truth; cannot drift. (This was the earlier mismatch bug — now fixed.)

**Gap:** none. GitHub repo URL is referenced in-app, satisfying the "source code on GitHub / clickable URL" requirement.

---

## 5. Good Design Practice / Multi-Screen UI (6 marks)

**Status: ✅ COMPLETE (mobile) / ⚠️ PARTIAL (web palette + responsiveness)**

**Mobile:**
- All required screens present: `LoginActivity`, `HazardMapActivity` (Map), `ReportHazardActivity` (Report), `ProfileActivity` (Profile), `HazardDetailsActivity`, `AboutActivity`.
- Consistent bottom nav (Report/Map/Profile): wired in `HazardMapActivity.java:101-120`, `ReportHazardActivity.java`, `ProfileActivity.java:30-53`; correctly absent on the two detail screens (back-arrow toolbar instead).
- Mossy Hollow palette: `colors.xml` (`primary #3D5A3D`, `accent #C1652F`, `surface #F5F3EE`), referenced consistently; no default Android blue.
- App name in toolbar: `activity_hazard_map.xml` toolbar reads "SmartRoad"; other screens use per-screen titles (normal pattern).
- Shared toolbar style: all 5 live screens use `style="@style/Widget.SmartRoad.Toolbar"` (`themes.xml`) — single point of control.
- Inclusive Sans applied globally: `themes.xml:8-9` `android:fontFamily`/`fontFamily = @font/inclusive_sans` on `Base.Theme.SmartRoad`; font files bundled in `res/font/`.
- Custom launcher icon: adaptive icon present in all density buckets (`mipmap-*`). **Not visually opened this session** — recommend a 30-second eyeball to confirm it isn't the stock robot.

**Web Admin:**
- Clean, professional UI across all pages: Login, Dashboard, Manage Reports, Report Details — card-based layout, status badges, hover states, empty states.
- Palette **thematically** consistent but **not an exact hex match** to mobile: `base.css:2-20` defines the *canonical* Mossy Hollow variables (`--color-primary: #3d5a3d`, `--color-accent: #c1652f`) matching mobile exactly — but most components **hardcode a different olive/moss palette** directly (`#636B2F`, `#3D4127`, `#BAC095`, `#E87000`, `#D4DE95` — e.g. `Login.vue`, `Dashboard.vue:121-178`, `ManageReports.vue`, `ReportDetails.vue`). So the rendered admin greens/orange differ from the mobile app's `#3D5A3D`/`#C1652F`, and there's internal inconsistency (`.btn-primary` in `main.css:38` uses `var(--color-accent)` = `#c1652f`, while `Login.vue:128` overrides its button to `#E87000`). It reads as one coherent earthy green+orange theme, just not pixel-identical to mobile.
- Responsive: **partial.** `ManageReports.vue` filters use `flex-wrap` ✅ and the table has `overflow-x: auto` ✅; but `Dashboard.vue:130-134` `.cards` is a fixed `grid-template-columns: repeat(4, 1fr)` with **no media query**, so the 4 stat cards do not reflow on narrow/phone widths.

**Gaps:**
1. Admin palette hardcoded to olive hexes rather than the shared `var(--color-*)` Mossy Hollow tokens — cosmetic drift from mobile.
2. Dashboard stat-card grid not responsive below ~4-column widths.

**Fix:** (optional polish) replace hardcoded hex in the admin components with the `var(--color-*)` tokens from `base.css` for exact mobile parity; add a `@media (max-width: 640px)` rule making `.cards` `grid-template-columns: repeat(2, 1fr)` (or `1fr`). Neither is likely to cost a full mark.

---

## 6. Server-Side Web Application (6 marks)

**Status: ✅ COMPLETE**

**Mobile (data sending):**
- Writes to Firebase RTDB: `ReportHazardActivity.writeReportToDatabase()` `ReportHazardActivity.java:235-275`.
- Payload includes all required fields (`ReportHazardActivity.java:247-257`): `id`, `uid`, `type`, `description`, `latitude`, `longitude`, `photoUrl`, `status` (`"New"`), `timestamp` (`244-245`), `userAgent` (`242-243`, device string).
- Photo upload to Firebase Storage: `uploadPhotoThenSave()` `ReportHazardActivity.java:215-233` — real `putFile` → `getDownloadUrl()` → stored as `photoUrl`; supports gallery + in-app camera.

**Web Admin (CRUD):**
- Dashboard stats: `Dashboard.vue:33-42` — total users, total reports, open, resolved (all computed live).
- Recent reports list: `Dashboard.vue:42,79-106` — top 10 by timestamp.
- Dashboard hazard map: `Dashboard.vue:74-77`.
- Manage Reports table with search + filters: `ManageReports.vue:46-58` filters by type / status / date + free-text search; template `94-121`.
- View individual report: `ReportDetails.vue` (route `/reports/:id`).
- Update status (New/Under Investigation/Resolved): `ReportDetails.vue:56-78` `saveStatus()`, adjusting `resolvedReports` counter on transition.
- Delete report: `ManageReports.vue:66-86` — removes DB node, deletes Storage photo (tolerates already-missing), decrements counters.
- View uploaded photos: `PhotoModal.vue` lightbox, opened from thumbnail in `ManageReports.vue:146-148` and `ReportDetails.vue:90-92`.
- Admin can view username, date/time, user-agent, type, description, photo, GPS: all present in `ReportDetails.vue:95-117`.

**Gap:** the type filter dropdown option list is stale (see Section 3 — 5 old types), so filtering by the two newest types is impossible; the filter *capability* itself (type/date/status/search) is fully implemented. Same one-line fix as Section 3.

---

## Additional Assignment Requirements

| Requirement | Status | Evidence |
|---|---|---|
| App name "SMARTROAD" in title bar | ✅ | `strings.xml:2` `app_name`; manifest label; map toolbar "SmartRoad" |
| Custom colors + unique icon | ✅ / ⚠️ | Mossy Hollow palette throughout; adaptive launcher icon present (not visually confirmed) |
| Source code on GitHub | ✅ | `@string/github_url` = `github.com/haziqnaqib11/SmartRoad`, surfaced in About page |
| About page clickable URL | ✅ | `AboutActivity.java:23-27` `ACTION_VIEW`; display == target |
| Report auto-captures date/time + GPS | ✅ | `ReportHazardActivity.java:244-245` (timestamp), `163-164`/`252-253` (GPS) |
| Report photo upload as evidence | ✅ | `ReportHazardActivity.java:215-233` (Storage), camera + gallery |
| Admin views username/datetime/user-agent/type/desc/photo/GPS | ✅ | `ReportDetails.vue:95-117` |
| Search + filter by type / date / status | ✅ / ⚠️ | `ManageReports.vue:46-58` — all four work; type option list missing 2 newest types |
| Inclusive Sans font (mobile) | ✅ | `themes.xml:8-9` |

---

## FINAL MARK ESTIMATE

| # | Criterion | Available | Estimated | Notes |
|---|---|---|---|---|
| 1 | Login & Profile (mobile + admin) | 4 | 4 | Complete both sides |
| 2 | Basic Map | 4 | 4 | Complete |
| 3 | Current Location & Hazard Map | 6 | 6 | Mobile fully complete; minor web legend/type gap (non-blocking) |
| 4 | About Page + Clickable URL | 4 | 4 | Complete |
| 5 | Design / Multi-Screen UI | 6 | 5.5 | Mobile complete; admin palette hex drift + dashboard non-responsive |
| 6 | Server-Side Web App (mobile write + admin CRUD) | 6 | 6 | Full CRUD; stale type-filter list is cosmetic |
| | **TOTAL** | **30** | **~29.5 / 30** | |

---

## Remaining gaps that could lose marks

1. **(Highest value, ~15 min) Admin hazard-type desync** — `HazardMap.vue:12-18` and `ManageReports.vue:10` list 5 old types (`Traffic Light`, no `Damaged Road Sign`/`Broken Traffic Light`). Mobile now submits the two new types → they show as gray unlabeled pins on the admin map and can't be type-filtered. This is the one cross-system inconsistency a careful grader could notice. Fix both arrays to the 6-type set with matching colors.
2. **(Cosmetic) Admin palette hex drift** — components hardcode `#636B2F`/`#E87000`/`#BAC095` instead of the `var(--color-*)` Mossy Hollow tokens already defined in `base.css`, so the admin doesn't pixel-match the mobile app. Swap hardcoded hexes for the CSS variables for exact parity.
3. **(Cosmetic) Dashboard not responsive** — `.cards` grid is fixed 4-column; add a `@media (max-width: 640px)` breakpoint.
4. **(Verification, 30 sec) Launcher icon** — confirm visually it's a custom SmartRoad icon, not the stock Android robot. Files are present and correctly structured, but not opened this session.

None of these are functional failures on the mobile side (which carries most of the marks); all four are polish/consistency items on the web panel or unverified visuals.

---

## VERDICT: ✅ READY FOR SUBMISSION

The system is functionally complete against all six rubric criteria on the mobile side, and the web admin panel delivers full CRUD (dashboard, search/filter, view, status update, delete, photo viewing) against the same live Firebase backend. Estimated **~29.5/30**.

The four remaining items are polish, not blockers. If time permits before submission, do item **#1** (admin hazard-type sync) first — it's the only one that produces a visible inconsistency between what the mobile app records and what the admin panel displays — then the quick launcher-icon eyeball (#4). Items #2 and #3 are safe to leave if time is short.
