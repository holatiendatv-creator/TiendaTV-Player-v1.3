# TiendaTV Player (v1.9.0)

Streaming player app for **Android TV**, **Amazon Firestick**, **Google TV**, and **Android phones/tablets**, built with **Kotlin**, **Jetpack Compose**, **AndroidX Media3 (ExoPlayer)**, and **Room**.

## What's new in v1.9.0

- **No more login flash on cold start** — the app shows a TiendaTV splash until the saved profile has actually loaded from Room, instead of flashing the login screen for a frame.
- **Audio picker works on a Firestick remote** — the track picker is now a real `Dialog` with focus trapped inside it, so D-pad navigation reaches the rows. A manual audio pick is protected from the Spanish auto-pick, and the stale checkmark refresh is gone.
- **Crisp design** — pure black background, opaque `#1C1C1E` surfaces, white 70% secondary text, one type scale (28 / 13 / 11sp), 12dp cards with no border, fast 120–150ms fade-only motion, no spring wobble, orange only for Play / progress.
- **Less UI lag** — tab switch is fade-only, Home no longer auto-rotates its backdrop every 7.5s (850ms crossfade removed), Live EPG fetches are debounced 150ms after focus settles.
- **No double-tap** — `tvFocusable` no longer fires `onClick` twice (Enter key + clickable).

## Build an Installable `.apk` with Zero Local Setup (GitHub Actions)

1. Create a new **private** repository on GitHub.
2. Unzip `TiendaTV-Player-v1.9.zip` and push all files to the `main` branch.
3. Open the **Actions** tab in GitHub — the **Build TiendaTV Player APK** workflow runs automatically (or click **Run workflow** manually).
4. Once finished, download **TiendaTV-Player-APK** from the workflow run's **Artifacts** section.
5. For direct Firestick distribution via **Downloader**:
   - Push a version tag (`git tag v1.9.0 && git push origin v1.9.0`) or host the `.apk` on your server/CDN, then shorten the direct `.apk` URL into a Downloader code.

Note: the in-app updater still checks the `latest` release in the `holatiendatv-creator/TiendaTV-Player-v1.3` GitHub repo (that is the repo name, not the app version). If you move the repo, update `VERSION_CHECK_URL` / `LATEST_APK_URL` in `AppConfig.kt` and the `apkUrl` in `.github/workflows/build-apk.yml` to match.

### Optional: Production Release Signing
In your GitHub repository go to **Settings → Secrets and variables → Actions** and add:
- `KEYSTORE_BASE64` (base64-encoded `.jks`/`.keystore` file)
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

If these secrets are not set, the workflow automatically signs the APK with a debug key so it installs immediately on Firestick, Android TV, and Android devices.

### Optional: Pre-Configure a Default Xtream Server URL
Edit `app/build.gradle.kts` (`DEFAULT_XTREAM_SERVER`) or `app/src/main/java/com/tiendatv/player/config/AppConfig.kt` (`DEFAULT_SERVER_URL`) before pushing.

## Known limitations (not fixed in v1.9)
- Xtream username/password are stored in plain text in Room and the parental PIN in plain text in DataStore; the updater downloads an APK with no hash/signature check. See the v1.8 audit for the full list.
- No WhatsApp support link or expiry warning in the app yet.
