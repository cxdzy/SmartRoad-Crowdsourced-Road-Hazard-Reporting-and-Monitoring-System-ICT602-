# SmartRoad — Project Brief for Claude Code

## Context
This is a university group assignment (ICT602 Mobile Technology, due 5 July 2026).
I'm continuing development started in a Claude.ai chat — this file summarizes everything
already decided so we don't have to re-plan from scratch.

## What we're building
**SmartRoad**: a crowdsourced road hazard reporting Android app.
- Users log in, get geo-located, and report road hazards (potholes, floods, fallen trees,
  accidents, damaged signs, broken traffic lights) with a photo, auto GPS, date, and time.
- Hazards appear as markers on a map, color/icon-coded by type and status
  (New / Under Investigation / Resolved).
- A companion admin web panel lets an admin manage reports: view, edit, delete, update
  status, search/filter by type/date/status, view uploaded photos and reporter metadata
  (username, timestamp, user-agent, GPS).

## Tech stack (decided, do not change without discussion)
- **Language**: Java (matches my ICT602 lab code — Weeks 7–13 all used Java)
- **UI**: XML layouts (traditional Views, not Jetpack Compose)
- **Backend**: Firebase
  - **Firebase Authentication** — Google Sign-In (pattern from Week 8 lab)
  - **Firebase Realtime Database** — NOT Firestore. Chosen because all reference lab
    code (Week 10, Week 13) uses Realtime DB syntax (`DatabaseReference`, `.push()`,
    `addValueEventListener`), and project scale doesn't need Firestore's stronger
    querying. Filtering in the admin panel will be done client-side in JS after
    pulling the full report list.
  - **Firebase Storage** — for hazard report photos
- **Maps**: Google Maps SDK (`SupportMapFragment`, `BitmapDescriptorFactory` for
  custom markers — pattern from Week 12/13 labs)
- **Location**: `FusedLocationProviderClient` + Geocoder reverse geocoding (Week 11 lab)
- **Admin web panel**: plain HTML/CSS/JS, can reuse Material Design Lite (MDL) cards
  from Week 7 lab for consistent styling

## Screens (Android app)
1. **Login screen** — Google Sign-In, app name "SMARTROAD" in title bar, custom colors/icon
2. **Home screen** — greets user by real name (`user.getDisplayName()`), shows current
   GPS lat/long, embedded Google Map with "Your Location" + "Hazards Nearby", bottom
   nav: Report / Map / Profile
3. **Hazard map screen** — full map view, hazard markers color-coded by type, with
   Filter and Refresh buttons
4. **Report hazard screen** — radio buttons for hazard type (Pothole, Flood, Accident,
   Fallen Tree, Traffic Light), description text field, photo picker, auto-filled GPS
   lat/long + date + time, Submit button
5. **Hazard details screen** — shows type, reporter, description, status, location, photo
6. **Profile screen** — name, username, total reports, reports resolved, logout button
7. **About page** — developer details, copyright statement, clickable GitHub URL
   (required by rubric)

## Admin web panel pages
1. **Dashboard** — total users, total reports, open reports, resolved reports, recent
   reports table
2. **Manage reports** — searchable/filterable table (by hazard type, date, status),
   View / Update Status / Delete actions
3. **Report details page** — full report info, photo, status dropdown, Save button

## Firebase Realtime Database structure
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
Security rules: `.read: true`, `.write: auth != null` (only signed-in users can write).

## Rubric weighting (30 marks total — prioritize accordingly)
1. Login & profile display — 4 marks
2. Basic map display — 4 marks
3. Current location + hazard map with markers — 6 marks (highest single item)
4. About page with clickable URL — 4 marks
5. Multi-screen UI/UX consistency — 6 marks
6. Server-side admin web app with full CRUD — 6 marks

## Build order (recommended, not yet started in code)
1. Firebase project setup + `google-services.json` + Gradle config
2. LoginActivity (Google Sign-In, adapted from Week 8 lab)
3. Home screen (GPS + map, greet by name)
4. Report Hazard screen (form + Realtime DB write + Storage photo upload)
5. Hazard Map screen (markers pulled live from Realtime DB, adapted from Week 13 lab)
6. Hazard Details screen
7. Profile screen
8. About page
9. Admin web panel (HTML/JS reading same Realtime DB via Firebase JS SDK)

## Evaluation requirements (don't forget)
- Custom app colors, custom app name in title bar, unique launcher icon
- Source code uploaded to GitHub (public repo)
- About page must include a clickable URL (can point to the GitHub repo)
- Video demo required, emphasizing rubric criteria

## Status as of this brief
Nothing has been coded yet. We've only finalized the architecture/tech stack decisions
above. Next concrete step: Firebase Console setup + Gradle dependency configuration,
then LoginActivity.java + activity_login.xml.

## My lab reference material
I have ICT602 lab PDFs covering Weeks 7–13 (Material Design Lite cards, Google Sign-In,
SharedPreferences/storage, RESTful APIs with Firebase, reverse geocoding, Google Maps +
OpenStreetMap integration, Firebase + Maps with JSON markers). Reuse these patterns
wherever they fit instead of introducing unfamiliar approaches — I need to be able to
explain every line of code in a viva.
