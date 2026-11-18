# ParkSpace — Mobile-Only Backup, Export & Testing Workflow

This workflow is designed so the project owner can manage every milestone directly from an Android phone using a mobile browser and Android apps—with zero desktop PC dependency.

## 1. Source Backup Strategy (Every Major Milestone)
> **Rule:** APK files are not substitutes for complete source backups. Always archive full source + Gradle configs (`app/`, `gradle/`, `build.gradle.kts`, `settings.gradle.kts`, `metadata.json`, `docs/`).

### Option A: AI Studio -> GitHub Push (Recommended Primary Backup)
1. Open AI Studio in your mobile browser (Chrome on Android).
2. Tap the **Settings / Project Menu** -> **Push to GitHub**.
3. Use a dedicated branch or commit message tagged with the milestone (e.g., `milestone-0-foundation`, `milestone-1-core-navigation`, `milestone-2-maplibre-discovery`).
4. Verify in the GitHub Android app or mobile browser that `app/src/`, `gradle/libs.versions.toml`, `docs/`, and `metadata.json` are present.

### Option B: Full Project ZIP Snapshot (Offline / Cloud Drive Redundancy)
1. From the AI Studio project settings menu in your mobile browser, tap **Export as ZIP**.
2. Save the archive to Google Drive or device Downloads as `parkspace-m<X>-YYYYMMDD.zip`.
3. Never delete prior milestone ZIPs until the next milestone builds and passes all unit/Robolectric tests.

## 2. Mobile-Only Testing & Verification Workflow
- **Visual UI Verification:** Use the **AI Studio Streaming Android Emulator** directly inside Chrome on your Android phone to inspect layouts, light/dark themes, touch targets, and MapLibre map rendering.
- **On-Device APK Testing:** Download the generated debug APK from AI Studio's export menu and install it on your Android phone (enable *Install unknown apps* for your browser/file manager) to test GPS, real touch ergonomics, and RTL/font scaling.
- **Automated JVM & Robolectric Tests:** Executed inside the cloud build environment (`gradle :app:testDebugUnitTest`) at the end of every milestone before tagging a backup.
