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
SmartRoad-Admin/    ← Vue.js admin panel (separate project, same repo)
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

Run from `SmartRoad-Admin/`:

```
npm install       # first-time setup
npm run dev       # dev server (Vite)
npm run build     # production build → SmartRoad-Admin/dist/
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
- **Styling**: Plain CSS (no framework) — keeps the bundle small and avoids introducing an unfamiliar dependency. Uses the same Mossy Hollow palette as the Android app (flat cards, thin divider borders, accent-colored primary buttons, danger-colored destructive actions) — see `src/assets/base.css` for the CSS variables.
- **Maps**: Google Maps JS API, loaded dynamically via `src/mapsLoader.js` using an API key from `VITE_GOOGLE_MAPS_API_KEY` in `SmartRoad-Admin/.env` (gitignored — not committed to the repo).
- **Auth**: Firebase email/password sign-in gates every route except `/login`; authorization is checked against an `admins/{uid}` node in the Realtime Database (`.write` locked to `false` — add admins by hand via the Firebase console).

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

## UI design — current state

Mirrored by the admin panel in `SmartRoad-Admin/src/assets/base.css` (same hex values, mapped to CSS custom properties) so both apps share one visual identity.

### Mossy Hollow color palette (`res/values/colors.xml`)

| Name | Hex | Usage |
|------|-----|-------|
| `primary` | `#3D5A3D` | Toolbars, filled buttons, bottom nav tint |
| `primary_light` | `#5C7A5C` | — |
| `primary_dark` | `#2A3F2A` | Status bar |
| `accent` | `#C1652F` | Select Photo button, About tagline, GitHub URL |
| `accent_light` | `#A8501F` | — |
| `surface` | `#F5F3EE` | Page/window background (warm off-white) |
| `text_primary` | `#2D2D2D` | Body text |
| `text_secondary` | `#6B6B6B` | Labels, secondary info |
| `divider` | `#DDD9D0` | Card strokes, separators |
| `surface_translucent` | `#D9F5F3EE` | Map legend overlay background |
| `danger` | `#C62828` | Logout button text/stroke |

Hazard and status colors (`hazard_pothole`, `status_new`, etc.) are functional — do not remap them to the palette.

### Typography — Dongle (Google Font)

- Font files live in `res/font/`: `dongle_regular.ttf`, `dongle_bold.ttf`, `dongle_light.ttf`
- Font family descriptor: `res/font/dongle.xml`
- Applied globally via `Base.Theme.SmartRoad` in `res/values/themes.xml`:
  ```xml
  <item name="android:fontFamily">@font/dongle</item>
  <item name="fontFamily">@font/dongle</item>
  ```
- Toolbar title size override: `TextAppearance.SmartRoad.ToolbarTitle` (40sp) referenced via `app:titleTextAppearance` on every `MaterialToolbar`.
- **Dongle renders visually smaller than system fonts** — all declared sp values are significantly larger than they would be with Roboto to compensate. Current scale: section labels 24sp, body/radio text 30sp, buttons 28sp, headings 40–66sp.

### Minimalist design conventions

- Cards use `cardElevation="0dp"` + `strokeWidth="1dp"` `strokeColor="@color/divider"` instead of shadows — exception: Profile stats panel uses a plain `LinearLayout` on `@color/white` with no border.
- No decorative dividers — spacing handles separation.
- Section labels: 24sp `@color/text_secondary`, no bold.
- Outlined buttons (`Widget.MaterialComponents.Button.OutlinedButton`) for secondary actions (About, Log Out).

## Android screens (all implemented)

1. **LoginActivity** — Google Sign-In; title "SMARTROAD" at 66sp
2. **HomeActivity** — greeting + GPS card + embedded mini-map with hazard markers; bottom nav with Map tab pre-selected
3. **ReportHazardActivity** — radio buttons for type, description `TextInputEditText`, photo picker (`ActivityResultContracts.GetContent`), auto-filled GPS + date/time; submits to Realtime DB + Storage; increments `users/{uid}/totalReports`; **bottom nav present, Report tab selected**
4. **HazardMapActivity** — full-screen map (toolbar above map, not overlaid), color-coded markers, Filter dialog, Refresh, empty-state overlay; tapping info window → HazardDetailsActivity; **bottom nav present, Map tab selected**
5. **HazardDetailsActivity** — full report card, Glide photo, Change Status dialog; updates `resolvedReports` on reporter when status toggles to/from Resolved; back arrow (`ic_arrow_back`) in toolbar
6. **ProfileActivity** — initials avatar (burnt orange circle), name/email, live Total/Resolved stats panel, About + Logout outlined buttons; **bottom nav present, Profile tab selected**
7. **AboutActivity** — developer info, copyright, tappable GitHub URL (rubric requirement); back arrow in toolbar

### Bottom nav wiring

`bottom_nav_menu.xml` has three items: `nav_report`, `nav_map`, `nav_profile`.

HomeActivity, ReportHazardActivity, HazardMapActivity, and ProfileActivity all host a `BottomNavigationView`. Each pre-selects its own tab and navigates to the other screens using `FLAG_ACTIVITY_REORDER_TO_FRONT` to avoid a deep back stack. HazardDetailsActivity and AboutActivity do **not** have a bottom nav (they are detail screens, not tabs).

## Admin panel pages (SmartRoad-Admin/) — all implemented

1. **Login** — email/password sign-in, gated on the `admins/{uid}` check; unauthorized accounts are signed back out
2. **Dashboard** — summary cards (total users, total reports, open, resolved), a compact Hazard Map overview, and a recent reports table
3. **Manage Reports** — searchable/filterable table (type, status, date) with a Table/Map toggle; View / inline Update Status / Delete (also removes the Storage photo) actions
4. **Report Details** — full report info, clickable photo (opens a lightbox), status dropdown, Save button

Logout is available in the nav bar on every page.

### Hazard Map (Dashboard + Manage Reports)

- `src/components/HazardMap.vue` — shared component rendering a Google Map with one marker per report
- Markers are custom inline-SVG pins colored to match the legend exactly (no reliance on Google's stock icon set, whose "red" and "orange" read as too similar at marker size): red = Pothole, blue = Flood, orange = Accident, green = Fallen Tree, yellow = Traffic Light — same mapping as the Android app's `HazardMapActivity.getMarkerHue()`
- Legend items are clickable and act as a filter — clicking a hazard type toggles that type's markers on/off (dims + strikethroughs the label when hidden), independent of the report data re-render
- Clicking a marker opens an info window with type/status and a "View details" button that routes to that report's Report Details page
- On Manage Reports, the map reflects the same filtered/searched report set as the table (not the full unfiltered list)

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
