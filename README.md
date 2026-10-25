# ⚡ TapFlow

> **Intelligent, Offline NFC Automation Engine for Android**  
> Designed with an editorial research aesthetic, enterprise-grade reliability, and zero telemetry.

[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-brightgreen?style=flat-square&logo=android)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple?style=flat-square&logo=kotlin)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4?style=flat-square&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Room](https://img.shields.io/badge/Database-Room%20SQLite%202.6.1-orange?style=flat-square)](https://developer.android.com/training/data-storage/room)
[![Privacy](https://img.shields.io/badge/Privacy-100%25%20Offline%20%7C%20Zero%20Telemetry-1E6C65?style=flat-square)](#privacy--security)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)

---

## 📖 Overview

**TapFlow** transforms any passive physical NFC tag, NFC sticker, transit card, hotel keycard, or keychain fob into a tactile trigger for custom multi-step automation workflows on Android.

Whether you want to tap your nightstand tag to trigger Bedtime mode (toggle flashlight, launch sleep playlist, and silence alerts), tap your desk to copy your guest Wi-Fi credentials to the clipboard, or tap your car mount to launch Google Maps and sound a tone, **TapFlow executes the entire sequence instantaneously**.

TapFlow is built 100% offline, privacy-first, and designed with a **clean, editorial light/beige research aesthetic** inspired by Perplexity Research—featuring warm parchment paper tones, crisp hairline-bordered cards, high-contrast ink typography, and muted teal accents.

---

## ✨ Features

### 🛠️ 7 Modular Automation Actions
Create rich, re-orderable action sequences that execute top-to-bottom on a single tap:
1. **📱 Open App**: Instantly launches any installed application (Spotify, Camera, Navigation, Settings, etc.) with Android 14+ background launch capabilities (`ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED`).
2. **🌐 Open URL**: Opens any web URL, deep link, or custom intent URI in the system default handler.
3. **🔦 Toggle Flashlight**: Controls the device's hardware LED torch (Turn ON or Turn OFF) directly via `CameraManager`.
4. **📋 Copy to Clipboard**: Copies predefined text (e.g. Wi-Fi passwords, phone numbers, snippets) directly to your clipboard.
5. **🔊 Play Sound**: Emits an immediate audio chime or frequency beep via Android's `ToneGenerator` for instant physical feedback.
6. **🔔 Show Notification**: Emits a priority notification in the system notification drawer with your custom title and message.
7. **💬 In-App Message**: Displays real-time status banners within the active UI session.

---

### 🎯 Reliable NFC Processing Engine
- **Permanent UID Fallback Resolution**: In background dispatch, Android OS may report varying technology lists (`techList`) for the same physical tag (e.g., dynamically adding `NdefFormatable`). TapFlow resolves triggers against composite keys *and* falls back to the permanent hardware UID (`rawUidHex`), guaranteeing 100% trigger reliability.
- **Priority `999` Intent Resolution**: Declares highest intent filter priority to resolve ambiguous tag intercepts cleanly over default system handlers.
- **2-Second Anti-Bounce Guard**: Built-in 2000ms debounce protection prevents accidental multiple executions when holding a phone over an NFC field.
- **Single Crisp Vibration (Double Vibration Fix)**: Android's hardware NFC driver naturally triggers a vibration when an RF field connects. TapFlow defaults its secondary software haptic pulse to OFF (configurable in Settings), providing a single, clean vibration upon scan.

---

### 🎨 Perplexity Research Editorial Beige Design
TapFlow replaces generic dark glassmorphism with an intentional, legible editorial design:
- **Warm Parchment Canvas (`#FBF9F5`)**: Softer on the eyes than stark white, evoking physical paper.
- **Ivory Research Cards (`#FFFFFF`)**: Zero heavy drop shadows, paired with 1dp hairline warm stone borders (`#E6E0D4`).
- **Deep Ink Typography (`#1B1A17`)**: High-contrast, publication-grade reading comfort.
- **Muted Research Teal Accent (`#1E6C65`)**: Sophisticated semantic highlighting for active states and indicators.
- **Terracotta Amber Accent (`#B45309`)**: Warm, balanced warnings and action alerts.

---

### 🧪 Built-in NFC Simulator
Don't have physical NFC tags on hand? TapFlow includes a hardware simulator allowing you to test:
- NFC-A (ISO 14443-3A)
- IsoDep (ISO 14443-4)
- NDEF (NFC Data Exchange Format)
- Mifare Ultralight & Classic
- Custom hardware UID injection

---

### 📜 Execution Audit Log
Every NFC trigger is recorded locally in Room SQLite with:
- Tag Name & Hardware UID
- Execution duration in milliseconds
- Individual action status (Success / Skipped / Failure)
- Expandable step-by-step diagnostic trace

---

## 🏗️ Technical Architecture

TapFlow follows **Clean Architecture** and **Unidirectional Data Flow (UDF)** principles:

```
┌────────────────────────────────────────────────────────┐
│                   UI Layer (Compose)                   │
│  HomeScreen • TagListScreen • AutomationScreen • Logs  │
└──────────────────────────▲─────────────────────────────┘
                           │ StateFlow / Events
┌──────────────────────────┴─────────────────────────────┐
│                 MainScreenViewModel                    │
│      Reactive UI State Combine & Action Orchestration  │
└──────────────┬──────────────────────────┬──────────────┘
               │                          │
┌──────────────▼─────────────┐ ┌──────────▼──────────────┐
│       NFC Engine           │ │   Automation Engine     │
│  • Foreground Dispatch     │ │  • AutomationExecutor   │
│  • Priority Intent Filter  │ │  • ActionMapper (JSON)  │
│  • TagIdentifier (UID/Tech)│ │  • Debounce Guard (2.0s)│
└──────────────┬─────────────┘ └──────────┬──────────────┘
               │                          │
┌──────────────▼──────────────────────────▼──────────────┐
│                   Data / Local DB                      │
│        Room 2.6.1 SQLite (RegisteredTag / Actions)     │
│        SharedPreferences (Haptic / Preferences)        │
└────────────────────────────────────────────────────────┘
```

### Tech Stack
| Component | Technology | Version |
|---|---|---|
| **Language** | Kotlin | `2.0.21` |
| **UI Toolkit** | Jetpack Compose + Material 3 | `1.7.5` / `1.3.1` |
| **Architecture** | MVVM + Unidirectional Data Flow | Modern Android Architecture |
| **Local Persistence** | Android Room Database (SQLite) | `2.6.1` |
| **Asynchronous Engine** | Kotlin Coroutines & StateFlow | `1.9.0` |
| **Dependency Injection** | Manual Service Locator (`AppContainer`) | Lightweight, zero-reflection |
| **Minimum SDK** | Android 8.0 (Oreo) | API Level 26 |
| **Target SDK** | Android 15 (Vanilla Ice Cream) | API Level 35 |

---

## 🔒 Privacy & Security

- **100% Offline**: TapFlow requests zero internet permissions (`android.permission.INTERNET` is not even present in the manifest).
- **No Third-Party SDKs or Trackers**: No analytics, no crash reporters, no ads, no telemetry.
- **Local Storage Only**: All tags, automation sequences, and history remain exclusively in encrypted/sandboxed Room SQLite on your device.

---

## 📱 Android NFC Operating System Realities

TapFlow interacts directly with Android's low-level NFC subsystem:

1. **Screen State Requirement (Anti-Skimming Security)**:
   AOSP (`NfcService.java`) deliberately disables the RF polling field when the device screen is fully off or locked to protect users against unauthorized pocket relay attacks and reduce battery drain.
   - **Requirement**: Simply wake your screen (tap screen or glance at lockscreen) and tap your NFC tag. TapFlow will instantly catch the intent and fire your automation sequence.
2. **Default App Conflict Resolution**:
   If another app (such as Google TagViewer or a third-party tool) has captured default NFC handling:
   - Go to **TapFlow Settings** → Tap **Default NFC App Resolver**.
   - Select the conflicting app and tap **Clear defaults**.

---

## 🚀 Building & Installing

### Prerequisites
- Android Studio Ladybug / Meerkat or standalone JDK 17+
- Android SDK Platform 35
- Physical Android device with NFC hardware or ADB emulator

### 1. Clone Repository
```bash
git clone https://github.com/NotAadhil/Tap_Tap.git
cd Tap_Tap
```

### 2. Run Unit Tests
```bash
# Windows
.\gradlew.bat test

# macOS / Linux
./gradlew test
```

### 3. Build Debug APK
```bash
# Windows
.\gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```
The compiled APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### 4. Install via ADB
```bash
# Check connected device
adb devices

# Install APK to device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch TapFlow
adb shell am start -n com.tapflow/.MainActivity
```

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
