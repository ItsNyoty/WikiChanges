# WikiChanges 🧹🌍

**WikiChanges** is an Android application designed for Wikipedia and Wikimedia project administrators and patrollers. It allows you to monitor recent changes on the go, view edit diffs, and quickly take moderation actions right from your phone.

<p align="center">
  <img src="favicon.svg" width="128" height="128" alt="WikiChanges Logo">
</p>

## 📥 Download

Currently, WikiChanges is distributed exclusively via GitHub rather than the Google Play Store. This allows us to gauge user interest before committing to developer registration fees. You can safely download the latest APK directly from here:

1. Go to the [Releases](../../releases/latest) page.
2. Under **Assets**, click on the `app-release.apk` (or similar `.apk` file) to download it.
3. Once downloaded, open the file on your Android device to install it. 
   *(Note: You may need to enable "Install unknown apps" in your Android settings for your browser or file manager).*

## ✨ Features

- **Recent Changes Feed**: Auto-updating list of the latest edits across your selected Wikimedia project.
- **Diff Viewer**: Easily view visually clear diffs between revisions right in the app.
- **Moderation Tools**: 
  - Mark edits as patrolled ✅
  - Rollback vandalism ⏪
  - Warn or Block users directly from the app 🚫
- **Customizable Filters**: Filter out bots, minor edits, or focus specifically on anonymous editors and new pages.
- **Multi-Wiki Support**: Log in once using your Wikimedia account (via secure OAuth 2.0) and easily switch between different wikis.

## 🚀 Getting Started for Developers

WikiChanges is built natively with Kotlin and Jetpack Compose. 

### Requirements
- Android Studio
- Minimum SDK: 24 (Android 7.0)
- Target SDK: 34 (Android 14)

### Building the Project
1. Clone the repository:
   ```bash
   git clone https://github.com/ItsNyoty/WikiChanges.git
   ```
2. Open the project in Android Studio.
3. Sync the project with Gradle files.
4. Run the app on your emulator or physical device.

### Authentication setup
The app uses Wikimedia's OAuth 2.0 PKCE flow (Public Client). The `WIKIMEDIA_OAUTH_CLIENT_ID` is included in the `app/build.gradle.kts` file. No client secret is required for public clients.

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! Feel free to open an issue or pull request.

## 📄 License

This project is open-source. *(Note: This project is created by a volunteer and is not officially affiliated with the Wikimedia Foundation).*

## ⚠️ Disclaimer

You take full responsibility for any actions taken using WikiChanges. You must read and abide by all relevant Wikipedia policies when using this tool; failure to do so may result in being blocked from editing.
