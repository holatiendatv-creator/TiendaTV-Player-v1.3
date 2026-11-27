# TiendaTV Player (v1.0)

Streaming player app for **Android TV**, **Amazon Firestick**, **Google TV**, and **Android phones/tablets**, built with **Kotlin**, **Jetpack Compose**, **AndroidX Media3 (ExoPlayer)**, and **Room**.

## Build an Installable `.apk` with Zero Local Setup (GitHub Actions)

1. Create a new **private** repository on GitHub.
2. Unzip `TiendaTV-Player-v1.0.zip` and push all files to the `main` branch.
3. Open the **Actions** tab in GitHub — the **Build TiendaTV Player APK** workflow runs automatically (or click **Run workflow** manually).
4. Once finished, download **TiendaTV-Player-APK** from the workflow run's **Artifacts** section.
5. For direct Firestick distribution via **Downloader**:
   - Push a version tag (`git tag v1.0.0 && git push origin v1.0.0`) or host the `.apk` on your server/CDN, then shorten the direct `.apk` URL into a Downloader code.

### Optional: Production Release Signing
In your GitHub repository go to **Settings → Secrets and variables → Actions** and add:
- `KEYSTORE_BASE64` (base64-encoded `.jks`/`.keystore` file)
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

If these secrets are not set, the workflow automatically signs the APK with a debug key so it installs immediately on Firestick, Android TV, and Android devices.

### Optional: Pre-Configure a Default Xtream Server URL
Edit `app/build.gradle.kts` (`DEFAULT_XTREAM_SERVER`) or `app/src/main/java/com/tiendatv/player/config/AppConfig.kt` (`DEFAULT_SERVER_URL`) before pushing.
