# 🛣️ SmartRoad — Crowdsourced Road Hazard Reporting & Monitoring System

SmartRoad is a two-part mobile + web system that lets everyday drivers report road hazards — potholes, floods, accidents, fallen trees, damaged signs, and broken traffic lights — in seconds, and lets an administrator track and resolve them from a live map dashboard.

---

## Overview

Road hazards often go unreported until someone gets hurt or stuck. **SmartRoad** turns every driver into a sensor: open the app, snap a photo of the hazard, and it's instantly pinned to a shared map with your GPS location, the time, and the hazard type — no phone calls, no waiting.

The project has two connected halves that share a single Firebase backend:

- **SmartRoad (Android app)** — for the public/commuters. Sign in, view nearby hazards on a map, and submit new reports with a photo.
- **SmartRoad-Admin (Vue.js web panel)** — for the authority/administrator managing reports. View live statistics, search and filter every submission, inspect evidence, update a hazard's status, or remove resolved/invalid entries.

**Target users:**
- 🚗 **Commuters / the public** — report hazards they encounter and see what's already been reported nearby.
- 🛠️ **Road authority administrators** — triage, verify, and resolve reports through a dedicated dashboard.

This project was built as a university group assignment for **ICT602 Mobile Technology**.

---

## Features

### 📱 Android App
- **Sign in with Google** or a traditional **email/password** account (with a dedicated Sign Up screen)
- Auto-login — returning signed-in users skip straight to the map
- **Live hazard map** with color-coded markers per hazard type, and marker opacity that fades as a report moves from *New → Under Investigation → Resolved*
- **Current GPS location** display with reverse-geocoded address (via `Geocoder`)
- **Filter dialog** to narrow the map by hazard type and/or status
- **Manual refresh** button to reload markers on demand
- Tapping a marker opens a **custom info window** (type, status, reporter) and can jump into full report details
- **Report a hazard**: choose a type from a dropdown (Pothole, Flood, Accident, Fallen Tree, Damaged Road Sign, Broken Traffic Light), add a description, attach a photo from the **camera or gallery**, with GPS coordinates and date/time captured automatically
- **Hazard details screen** with the full report, reporter name, and the attached photo (loaded via Glide)
- **Profile screen** — avatar initials, name, email, and live totals for reports submitted and resolved
- **About screen** with development team credits and a tappable link to the source repository
- Persistent bottom navigation (Report / Map / Profile) shared across the main screens
- Custom **Mossy Hollow** color theme, custom app icon, and the **Inclusive Sans** Google Font applied app-wide

### 🖥️ Admin Web Panel
- **Email/password login**, authorized against an `admins/{uid}` allow-list in the database (non-admin accounts are signed back out)
- **Dashboard** with live counts (total users, total reports, open, resolved), an overview hazard map, and a recent-reports table
- **Manage Reports** page with free-text search plus filters by hazard type, status, and date, and a **table/map view toggle**
- Inline **status update** (New / Under Investigation / Resolved) and **delete** (also removes the report's photo from Storage)
- **Report Details** page showing reporter, description, GPS coordinates, timestamp, device user-agent, and a full-size photo lightbox
- Shared, clickable hazard-map component with type-colored pins and a clickable legend that toggles pin visibility
- Same Mossy Hollow visual identity as the mobile app

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Mobile app language** | Java (no Kotlin) |
| **Mobile UI** | Android XML layouts (traditional Views, no Jetpack Compose), Material Components |
| **Mobile build** | Gradle (Kotlin DSL), Android Gradle Plugin 9.2.1, JDK 21 toolchain |
| **Admin panel framework** | Vue.js 3 (Composition API) |
| **Admin build tool** | Vite |
| **Admin routing** | Vue Router 4 |
| **Backend / Database** | Firebase Realtime Database |
| **Authentication** | Firebase Authentication (Google Sign-In + Email/Password) |
| **File storage** | Firebase Storage (hazard photos) |
| **Maps** | Google Maps SDK for Android · Google Maps JavaScript API (admin) |
| **Location** | `FusedLocationProviderClient` + Android `Geocoder` |
| **Image loading** | Glide (Android) |
| **Styling (admin)** | Plain CSS with CSS custom properties (no framework) |
| **Testing scaffold** | JUnit 4 + AndroidX Test / Espresso (default templates) |

---

## Project Structure

```
SmartRoad/
├── app/                          # Android application module
│   ├── src/main/java/com/example/smartroad/
│   │   ├── LoginActivity.java        # Google Sign-In + email/password login
│   │   ├── RegisterActivity.java     # Email/password account creation
│   │   ├── HazardMapActivity.java    # Main map screen, filters, GPS, greeting
│   │   ├── ReportHazardActivity.java # Hazard submission form + Storage upload
│   │   ├── HazardDetailsActivity.java# Full report view
│   │   ├── ProfileActivity.java      # User profile + stats + logout
│   │   ├── AboutActivity.java        # Credits + repo link
│   │   ├── HazardReport.java         # Firebase data model
│   │   └── MainActivity.java         # Default project template activity (unused in app flow)
│   ├── src/main/res/
│   │   ├── layout/                   # XML screen layouts
│   │   ├── values/colors.xml         # Mossy Hollow color palette
│   │   ├── font/                     # Dongle + Inclusive Sans font files
│   │   └── drawable/, mipmap-*/      # Icons and launcher assets
│   ├── google-services.json          # Firebase project configuration
│   └── build.gradle.kts              # App-level Gradle config & dependencies
├── SmartRoad-Admin/               # Vue.js admin web panel (separate project)
│   ├── src/
│   │   ├── views/                    # Dashboard.vue, Login.vue, ManageReports.vue, ReportDetails.vue
│   │   ├── components/               # HazardMap.vue, PhotoModal.vue
│   │   ├── router/index.js           # Route guard (admin-only access)
│   │   ├── firebase.js               # Firebase JS SDK initialization
│   │   ├── auth.js                   # Auth state + admin check
│   │   └── mapsLoader.js             # Dynamic Google Maps JS API loader
│   ├── package.json
│   └── vite.config.js
├── docs/                          # Internal rubric/audit notes
├── Screenshots/                   # App and admin panel screenshots
├── gradle/, gradlew, gradlew.bat  # Gradle wrapper
├── settings.gradle.kts
└── build.gradle.kts               # Root Gradle config
```

---

## Screenshots

## 📸 Screenshots

| Authentication | Home | Report |
|---------------|------|--------|
| <img src="Screenshots/LoginPage.png" width="200"> | <img src="Screenshots/MapHome.png" width="200"> | <img src="Screenshots/HazardReport.png" width="200"> |

| Details | Filter | Profile |
|---------|--------|---------|
| <img src="Screenshots/HazardDetail.png" width="200"> | <img src="Screenshots/HazardFilter.png" width="200"> | <img src="Screenshots/Profile.png" width="200"> |

### Admin Login
![Admin Login](Screenshots/AdminLogin.png)

### Admin Dashboard
![Admin Dashboard 1](Screenshots/AdminDashboard1.png)
![Admin Dashboard 2](Screenshots/AdminDashboard2.png)

### Admin Manage Reports
![Admin Manage Report](Screenshots/AdminManageReport.png)
![Admin Manage Report 2](Screenshots/AdminManageReport2.png)

### Admin Report Details
![Admin Report Details](Screenshots/AdminReportDetails.png)

---

## Requirements

**For the Android app:**
- Android Studio (recent stable version)
- JDK 21
- Android SDK with API level 36 installed (`minSdk 30`, `targetSdk 36`)
- A physical device or emulator running Android 11 (API 30) or higher
- A Firebase project with Realtime Database, Authentication (Google + Email/Password providers), and Storage enabled
- A Google Maps API key with the Maps SDK for Android enabled

**For the admin web panel:**
- Node.js `^22.18.0` or `>=24.12.0`
- npm
- The same Firebase project's web credentials
- A Google Maps JavaScript API key

---

## Installation

### 1. Clone the repository
```bash
git clone <repository-url>
cd SmartRoad
```

### 2. Android app setup
1. Open the project root in Android Studio.
2. Place your own `google-services.json` in `app/` (replace the existing one with your Firebase project's file).
3. Create a `local.properties` file in the project root (if not already present) and add your Maps API key:
   ```properties
   MAPS_API_KEY=your_google_maps_api_key_here
   ```
4. Let Gradle sync and download dependencies.

### 3. Admin panel setup
```bash
cd SmartRoad-Admin
npm install
```
Create a `.env` file inside `SmartRoad-Admin/` with your Maps key:
```
VITE_GOOGLE_MAPS_API_KEY=your_google_maps_api_key_here
```
Update the Firebase config object in `src/firebase.js` with your own Firebase project's web credentials if you are not using the same project as the Android app.

---

## How to Run

### Android app
Using the Gradle wrapper from the project root:
```bash
# Build a debug APK
./gradlew assembleDebug

# Install directly onto a connected device/emulator
./gradlew installDebug
```
Or in Android Studio: **Run ▶ Run 'app'**.

### Admin web panel
From `SmartRoad-Admin/`:
```bash
npm run dev        # starts the Vite dev server
```
For a production build:
```bash
npm run build       # outputs to SmartRoad-Admin/dist/
npm run preview     # preview the production build locally
```

---

## Usage

1. **Sign in** to the Android app with a Google account or an email/password account (new users can register in-app).
2. Land on the **hazard map**, which shows your current location and all reported hazards nearby, color-coded by type.
3. Tap **Report** in the bottom navigation to submit a new hazard: pick a type, describe it, attach a photo (camera or gallery), and submit — your GPS position and timestamp are captured automatically.
4. Tap any marker to view **hazard details**, including the reporter, description, status, and photo.
5. Check your **Profile** to see how many reports you've submitted and how many have been resolved.
6. Administrators log into the **web admin panel** to view the dashboard, search/filter all reports, inspect evidence photos, update a report's status, or delete it.

---

## Configuration

| File | Purpose | Notes |
|---|---|---|
| `app/google-services.json` | Firebase project configuration for the Android app | Provided per Firebase project; not typically edited by hand |
| `local.properties` → `MAPS_API_KEY` | Google Maps SDK key for Android, injected into the manifest via `manifestPlaceholders` | Not committed to version control |
| `SmartRoad-Admin/.env` → `VITE_GOOGLE_MAPS_API_KEY` | Google Maps JavaScript API key for the admin panel | Read by `src/mapsLoader.js`; gitignored |
| `SmartRoad-Admin/src/firebase.js` | Firebase JS SDK config object (apiKey, databaseURL, storageBucket, etc.) for the admin panel | Connects to the same Realtime Database the app writes to |

**Firebase Realtime Database structure:**
```
hazard_reports/
  {reportId}/
    id, uid, type, description, latitude, longitude,
    photoUrl, status, timestamp, userAgent
users/
  {uid}/
    name, email, totalReports, resolvedReports
admins/
  {uid}: true          # grants access to the admin web panel
```

---

## Future Improvements

- Push notifications to alert nearby users when a new hazard is reported close to their location
- Offline queuing of reports when connectivity is unavailable, syncing once back online
- Role-based admin accounts (e.g. read-only vs. full-edit permissions)
- In-app analytics/heatmaps of hazard density over time
- Migrating the admin panel's hardcoded palette values to its shared CSS custom properties for full visual parity with the Android app
- Responsive layout improvements for the admin dashboard on smaller screens

---

# Contributors

- MUHAMMAD AKMAL BIN MOHAMAD 'ASRI 
- ZARITH ASYRAF BIN ZUREIMI
- MOHAMAD HAZIQ NAQIB BIN ZAID
- RAFIQ SHAH BIN RAMLI

## License

This project currently does not specify a license.
