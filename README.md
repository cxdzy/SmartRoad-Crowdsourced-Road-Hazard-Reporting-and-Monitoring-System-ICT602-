# 🛣️ SmartRoad — Crowdsourced Road Hazard Reporting & Monitoring System

SmartRoad lets drivers report road hazards instantly from their phones and gives administrators a live web dashboard to monitor and resolve them. Built for ICT602 Mobile Technology — UiTM.

---

## Overview

Two connected apps sharing one Firebase backend:
- **SmartRoad (Android)** — public users report hazards and view them on a live map
- **SmartRoad-Admin (Vue.js)** — administrators manage, filter, and resolve reports

---

## Features

### 📱 Android App
- Google Sign-In + Email/Password login with Register screen
- Auto-login for returning users
- Live hazard map with color-coded markers by type and status opacity
- Real-time GPS location with reverse-geocoded address
- Report a hazard: type, description, photo (camera/gallery), auto GPS + timestamp
- Filter markers by hazard type and status
- Hazard Details, Profile, and About screens
- Mossy Hollow color theme + Inclusive Sans font + custom app icon

### 🖥️ Admin Web Panel
- Email/password login with admin-only access gate
- Dashboard: live stats, hazard map, recent reports
- Manage Reports: search + filter by type, status, date
- View full report details with photo lightbox
- Update status (New / Under Investigation / Resolved)
- Delete report + auto-remove photo from Firebase Storage

---

## Tech Stack

| Layer | Technology |
|---|---|
| Mobile language | Java |
| Mobile UI | Android XML layouts, Material Components |
| Admin framework | Vue.js 3 (Composition API) + Vite |
| Database | Firebase Realtime Database |
| Authentication | Firebase Auth (Google + Email/Password) |
| File storage | Firebase Storage |
| Maps | Google Maps SDK (Android) + Maps JS API (Admin) |
| Location | FusedLocationProviderClient + Geocoder |
| Image loading | Glide |

---

## Project Structure

```
SmartRoad/
├── app/src/main/java/com/example/smartroad/
│   ├── LoginActivity.java
│   ├── RegisterActivity.java
│   ├── HazardMapActivity.java
│   ├── ReportHazardActivity.java
│   ├── HazardDetailsActivity.java
│   ├── ProfileActivity.java
│   ├── AboutActivity.java
│   └── HazardReport.java
├── SmartRoad-Admin/src/
│   ├── views/              # Dashboard, Login, ManageReports, ReportDetails
│   ├── components/         # HazardMap, PhotoModal
│   ├── router/index.js
│   ├── firebase.js
│   └── auth.js
└── docs/
```

---

## Screenshots

| Authentication | Home | Report |
|---|---|---|
| <img src="Screenshots/LoginPage.png" width="200"> | <img src="Screenshots/MapHome.png" width="200"> | <img src="Screenshots/HazardReport.png" width="200"> |

| Details | Filter | Profile |
|---|---|---|
| <img src="Screenshots/HazardDetail.png" width="200"> | <img src="Screenshots/HazardFilter.png" width="200"> | <img src="Screenshots/Profile.png" width="200"> |

### Admin Panel
![Admin Login](Screenshots/AdminLogin.png)
![Admin Dashboard](Screenshots/AdminDashboard1.png)
![Admin Manage Reports](Screenshots/AdminManageReport.png)
![Admin Report Details](Screenshots/AdminReportDetails.png)

---

## Requirements

**Android app:**
- Android Studio + JDK 21
- Android API 30+ (minSdk 30, targetSdk 36)
- Firebase project with Realtime Database, Auth, and Storage enabled
- Google Maps API key (Maps SDK for Android)

**Admin panel:**
- Node.js ^22.18.0 or >=24.12.0
- Same Firebase project web credentials
- Google Maps JavaScript API key

---

## Installation

### 1. Clone the repository
```bash
git clone <repository-url>
cd SmartRoad
```

### 2. Android app
1. Open project root in Android Studio
2. Place your `google-services.json` in `app/`
3. Add to `local.properties`:
```properties
MAPS_API_KEY=your_google_maps_api_key_here
```
4. Sync Gradle and run

### 3. Admin panel
```bash
cd SmartRoad-Admin
npm install
```
Add `.env` inside `SmartRoad-Admin/`:
```
VITE_GOOGLE_MAPS_API_KEY=your_google_maps_api_key_here
```

---

## How to Run

### Android app
```bash
./gradlew assembleDebug
./gradlew installDebug
```

### Admin panel
```bash
cd SmartRoad-Admin
npm run dev
```

---

## Firebase Database Structure

```
hazard_reports/
  {reportId}/
    id, uid, type, description, latitude, longitude,
    photoUrl, status, timestamp, userAgent

users/
  {uid}/
    name, email, totalReports, resolvedReports

admins/
  {uid}: true
```

---

## Contributors

| Name | Student ID |
|---|---|
| MOHAMAD HAZIQ NAQIB BIN ZAID | 2025181477 |
| MUHAMMAD AKMAL BIN MOHAMAD 'ASRI | 2025117863 |
| ZARITH ASYRAF BIN ZUREIMI | 2025161739 |
| MUHAMMAD RAFIQ SHAH BIN RAMLI | 2025120989 |

---

## License

This project does not currently specify a license.