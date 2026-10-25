# Spendo AI — Android Expense Tracker & Personal Finance App

<p align="center">
  <strong>Understand your money. Spend with intention.</strong><br/>
  A privacy-first, Android-focused personal finance app for tracking transactions, monitoring budgets, and reviewing spending.
</p>

<p align="center">
  <a href="https://github.com/prudhvikakkunuri/Spendo-AI/releases/download/dev-latest/spendo-ai.apk">
    <img alt="Download APK" src="https://img.shields.io/badge/Download-Android%20APK-111111?style=for-the-badge&logo=android&logoColor=white" />
  </a>
  <a href="https://github.com/prudhvikakkunuri/Spendo-AI/actions/workflows/android-ci.yml">
    <img alt="Android CI" src="https://img.shields.io/github/actions/workflow/status/prudhvikakkunuri/Spendo-AI/android-ci.yml?branch=main&label=Android%20CI&style=for-the-badge" />
  </a>
  <a href="https://github.com/prudhvikakkunuri/Spendo-AI/releases">
    <img alt="GitHub release downloads" src="https://img.shields.io/github/downloads/prudhvikakkunuri/Spendo-AI/total?label=APK%20downloads&logo=github&style=for-the-badge" />
  </a>
</p>

<p align="center">
  <img alt="Android" src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white" />
  <img alt="Kotlin" src="https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white" />
  <img alt="Jetpack Compose" src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white" />
  <img alt="Local-first" src="https://img.shields.io/badge/Design-Local--first-555555" />
  <img alt="Status" src="https://img.shields.io/badge/Status-In%20development-orange" />
</p>

<p align="center">
  <a href="https://prudhvikakkunuri.github.io/Spendo-AI/">🌐 Project website</a> ·
  <a href="https://github.com/prudhvikakkunuri/Spendo-AI/releases/download/dev-latest/spendo-ai.apk">⬇ Download Android APK</a> ·
  <a href="https://github.com/prudhvikakkunuri/Spendo-AI/releases">Releases</a>
</p>

---

## Table of contents

- [Overview](#overview)
- [Download Spendo AI](#download-spendo-ai)
- [Features](#features)
- [Privacy and security](#privacy-and-security)
- [Technology stack](#technology-stack)
- [Build from source](#build-from-source)
- [Install the APK](#install-the-apk)
- [Troubleshooting](#troubleshooting)
- [Report a bug or request a feature](#report-a-bug-or-request-a-feature)
- [Contributing](#contributing)
- [License](#license)

## Overview

**Spendo AI** is an Android personal finance and expense-tracking project built with Kotlin and Jetpack Compose. It is designed to help users organize transactions, review spending, manage budgets, and understand how their money is being used.

The current architecture prioritizes local data storage and user-controlled notification access. This repository contains the Android source code, transaction-processing logic, UI, and automated tests. Features under development are identified as such; the presence of a screen does not necessarily mean every connected workflow is complete.

**Project status:** In development. Check [Releases](https://github.com/prudhvikakkunuri/Spendo-AI/releases) for available APK builds and release notes.

## Download Spendo AI

**[Download the latest development APK](https://github.com/prudhvikakkunuri/Spendo-AI/releases/download/dev-latest/spendo-ai.apk)** · [Release notes](https://github.com/prudhvikakkunuri/Spendo-AI/releases/tag/dev-latest) · [Browse all releases](https://github.com/prudhvikakkunuri/Spendo-AI/releases)

Each published release will include its version notes and, when the release workflow completes successfully, a downloadable `spendo-ai.apk` asset.

- **Latest development APK:** [Download `spendo-ai.apk` directly](https://github.com/prudhvikakkunuri/Spendo-AI/releases/download/dev-latest/spendo-ai.apk) or read the [release notes](https://github.com/prudhvikakkunuri/Spendo-AI/releases/tag/dev-latest). This pre-release is refreshed after a successful main-branch test/build.
- **Development build:** Open [Android CI workflow runs](https://github.com/prudhvikakkunuri/Spendo-AI/actions/workflows/android-ci.yml), select a successful run, and download the `spendo-ai-debug-apk` artifact. CI artifacts are temporary and are not a substitute for a versioned release.
- **Versioned releases:** Stable version tags such as `v0.1.0` trigger the versioned release workflow. The first run needs to complete successfully before its APK appears on the Releases page.

> **Signing note:** A public release intended for ongoing use should be signed with a persistent release keystore. If repository signing secrets are not configured, the automated release workflow publishes a clearly labelled pre-release/testing APK. Debug APKs produced by separate CI runs may have different signing certificates and may not update an earlier installation. Never uninstall an existing app with important local data unless you have exported or backed it up.

Only install APKs from this repository's official [Releases](https://github.com/prudhvikakkunuri/Spendo-AI/releases) page, and review the release notes before installing.

## Features

The repository includes implementation for the following areas; exact completeness can vary while the app is under active development.

- **Transaction tracking:** transaction list, manual entry, search, filters, and transaction details.
- **Notification-based detection:** optional monitoring of user-selected banking and payment app notifications through Android's Notification Listener permission.
- **Transaction parsing:** local heuristic parsing for supported notification formats, with ambiguous cases eligible for review.
- **Budgets and spending insights:** budgeting tools, monthly spending calculations, and Safe-to-Spend logic.
- **Bank and account views:** account-related UI and transaction association, with balance functionality continuing to evolve.
- **Review queue:** a workflow for examining uncertain transaction detections.
- **Import and export:** structured CSV export and related utilities.
- **Privacy controls:** monitored-app selection, detection pause controls, and data-clearing options.
- **Android-native UI:** Kotlin, Jetpack Compose, and Material 3.

Features such as broad bank-specific message coverage, live bank connectivity, cloud synchronization, and fully integrated AI assistance must not be assumed unless a release explicitly documents and verifies them.

## Privacy and security

Spendo AI currently follows a local-first design in its Android manifest and core storage architecture.

- Financial records use local Room/SQLite storage.
- Automatic detection is based on Android notification access granted explicitly by the user; the app does not request SMS-reading permissions in the current manifest.
- The current app manifest does not declare Android's `INTERNET` permission. Do not assume cloud sync or remote AI calls are available in this build.
- The application disables standard Android backup and includes backup exclusion rules for local financial data.
- Transaction exports are designed to avoid including raw notification message text or internal database identifiers.
- Never commit API keys, passwords, signing keystores, account information, or real bank messages to source control.
- Notification access can expose sensitive content from selected apps. Enable it only after reviewing the permission explanation and select only sources you intend to monitor.

Please report security issues privately when possible; see [SECURITY.md](SECURITY.md) if available.

## Technology stack

| Area | Technology |
| --- | --- |
| Platform | Android |
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM, StateFlow, Coroutines |
| Local database | Room / SQLite |
| Local preferences | AndroidX DataStore |
| Build system | Gradle, Android Gradle Plugin |
| Tests | JUnit and Kotlin coroutine testing |
| Minimum Android version | Android 8.0 (API 26), as configured in the app module |

## Build from source

### Prerequisites

- Android Studio or a compatible command-line environment.
- JDK 17.
- Android SDK Platform 34 and Build Tools 34.0.0, as required by the current project configuration.
- Git.

### Clone the repository

```bash
git clone https://github.com/prudhvikakkunuri/Spendo-AI.git
cd Spendo-AI
```

### Run unit tests

On Windows PowerShell:

```powershell
.\gradlew.bat test
```

On macOS/Linux:

```bash
chmod +x gradlew
./gradlew test
```

### Build a debug APK

On Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

On macOS/Linux:

```bash
chmod +x gradlew
./gradlew assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### Publish a versioned APK release

The latest development build is published to the fixed [dev-latest release](https://github.com/prudhvikakkunuri/Spendo-AI/releases/tag/dev-latest) after a successful push to `main`. For a versioned release, create and push a tag matching `v*`:

```bash
git checkout main
git pull origin main
git tag v0.1.0
git push origin v0.1.0
```

Replace `v0.1.0` with the next version. The workflow tests the project, builds an APK, and attaches it to a GitHub Release. If release-signing secrets are configured, it builds a signed release APK; otherwise it publishes a pre-release testing APK.

For a signed release, configure these repository Actions secrets before tagging:

- `ANDROID_KEYSTORE_BASE64` — the Base64-encoded release keystore file.
- `ANDROID_KEYSTORE_PASSWORD` — the keystore password.
- `ANDROID_KEY_ALIAS` — the signing key alias.
- `ANDROID_KEY_PASSWORD` — the signing key password.

Create and securely back up the release keystore before the first signed release. Keep the keystore and passwords out of Git. Losing or changing the signing key can prevent users from installing future versions as updates.

## Install the APK

1. Download `spendo-ai.apk` from the repository's [Releases page](https://github.com/prudhvikakkunuri/Spendo-AI/releases).
2. Open the APK on your Android device.
3. If Android asks, allow your browser or file manager to install apps from that source.
4. Review the requested permissions before enabling optional notification access.
5. After installation, open Spendo AI and configure its settings.

The exact prompts vary by Android version and device manufacturer. Install only builds whose source and release notes you trust.

## Troubleshooting

**The APK cannot be installed**
- Confirm that the device meets the minimum supported Android version.
- Download the APK again if the file appears incomplete.
- Check whether an existing installation was signed with a different certificate. A debug APK from a different CI run may require uninstalling the previous debug build; uninstalling can erase local app data.

**Transactions are not detected**
- Open Android Settings and verify that notification access is enabled for Spendo AI.
- Confirm that the relevant banking or payment app is selected in the app's monitored-app settings.
- Check whether the source app publishes a transaction notification. The app cannot parse an event it never receives.
- Review uncertain detections instead of assuming every bank message format is supported.

**The build fails**
- Verify JDK 17 is selected.
- Install Android SDK Platform 34 and Build Tools 34.0.0.
- Run `./gradlew test` (or `.\gradlew.bat test` on Windows) and inspect the first meaningful error in the output.

## Report a bug or request a feature

Use GitHub Issues to report reproducible problems or propose improvements:

- [Report a bug](https://github.com/prudhvikakkunuri/Spendo-AI/issues/new?template=bug_report.yml)
- [Request a feature](https://github.com/prudhvikakkunuri/Spendo-AI/issues/new?template=feature_request.yml)
- [Browse existing issues](https://github.com/prudhvikakkunuri/Spendo-AI/issues)

For bugs, include the app version, Android version, steps to reproduce, expected behaviour, and actual behaviour. **Do not post bank statements, real transaction messages, account numbers, API keys, passwords, or other personal financial data.** Use synthetic examples with amounts and identifiers you made up.

## Contributing

Contributions are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a pull request. Keep changes focused, add or update tests for behaviour changes, and never submit real financial data or secrets.

## License

A license has not yet been declared for this repository. Until the owner adds a license, assume that normal copyright applies and do not assume the code is licensed for reuse or redistribution.

---

<p align="center">
  <sub>Spendo AI · Android personal finance and expense tracking · Built with Kotlin</sub>
</p>
