# Patient Tracker (Android)

A Compose-first Android app for tracking patient locations on a Google Map and keeping records in a CSV file. The app ships with a starter CSV (in `assets/patients.csv`) and lets you create, read, update, and delete rows while keeping the map and list in sync.

## Features
- Google Map visualization with patient markers and optional current-location overlay.
- CSV-driven storage persisted to app files after the first launch; edits are saved back to CSV.
- CRUD UI with Material 3 dialogs, map focusing, and undo after deletes.
- Location permission prompt with rationale banner to help orient the map.

## Getting started
1. Open the project in Android Studio (Giraffe or newer) with the Android SDK installed.
2. Ensure you have network access for Gradle to download `com.android.tools.build:gradle`, Kotlin, and Google Maps dependencies.
3. Build and run on a device/emulator running Android 8.0 (API 26) or newer.

## CSV format
```
bil,zon,region,address,patients,latitude,longitude,contact_name,contact_phone,status
1,B1,GOMBAK SETIA,"JALAN SEKOLAH, GOMBAK SETIA 53100 SELANGOR",1,3.2170612876112217,101.71474789329302,Mat Rahim,012-3456789,Stable
```
- The header row is required.
- Fields containing commas, quotes, or newlines must be wrapped in quotes (`"`).
- New records automatically receive the next `bil` identifier.

## Maps API key
The manifest already includes the provided key:
```
<meta-data android:name="com.google.android.geo.API_KEY" android:value="AIzaSyAGVpbdswqcwiaOHFnF9gXVN7Ez607BeEw" />
```
Replace it with a restricted key if you rotate credentials.

## Notes
- The app enables `isMyLocationEnabled` on the map only after the user grants coarse or fine location permission.
- All patient edits are persisted to `filesDir/patients.csv` after the initial asset load.
