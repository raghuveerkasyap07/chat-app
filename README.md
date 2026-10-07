# 💬 Demo Chat — Android WebSocket Echo App

A native Android real-time chat application built with **XML Views + Kotlin**, implementing an **OkHttp WebSocket Client** connected to a public echo server.

![Android](https://img.shields.io/badge/Platform-Android-green.svg)
![Kotlin](https://img.shields.io/badge/Language-Kotlin_2.1.0-blue.svg)
![Architecture](https://img.shields.io/badge/Architecture-MVVM-orange.svg)
![Build](https://img.shields.io/badge/Build-Gradle_8.11.1-brightgreen.svg)

---

## 🌟 Key Features

- **🔌 Real-Time WebSocket Communication**:
  - Connects to public echo server (`wss://ws.postman-echo.com/raw`).
  - Sends text messages and instantly receives echoes back in real time.
- **🟢 Dynamic Connection Status Indicator**:
  - Live status pill in toolbar: `Connected 🟢`, `Connecting... 🟡`, `Disconnected 🔴`.
- **💬 Dual-Side Chat Bubble UI**:
  - Distinct right-aligned sent bubbles and left-aligned received bubbles ("Echo Bot").
  - System event badges for connection events, ping logs, and errors.
- **⚡ Message Delivery Status**:
  - Visual status indicators (`SENDING` check, `SENT` double-check, `FAILED` alert).
- **💡 Interactive Quick-Suggestion Chips**:
  - One-tap preset messages (*👋 Hello!*, *🧪 Test Echo*, *⏱️ Ping*, *❓ How are you?*).
- **⚙️ Configurable Server Settings**:
  - Change WebSocket URL on the fly via top action menu dialog.
- **📋 Message Copy & Quick Actions**:
  - Long-press any chat bubble to copy text to clipboard with feedback toast.
  - Toolbar actions to send manual `PING` or trigger a `Reconnect`.

---

## 🛠️ Architecture & Tech Stack

| Layer / Concern | Technology / Library |
|---|---|
| **Language** | Kotlin 2.1.0 |
| **UI Framework** | XML Layouts (`ConstraintLayout`, `RecyclerView`, `AppBarLayout`, `MaterialToolbar`, `FloatingActionButton`) |
| **View Binding** | Android ViewBinding enabled |
| **Networking** | OkHttp 4.12.0 (`WebSocket`, `WebSocketListener`) |
| **Architecture** | MVVM (`ViewModel`, `StateFlow`, `SharedFlow`, `viewModelScope`) |
| **Data Parsing** | Gson 2.10.1 |
| **Design System** | Material Components 1.12.0 (Material 3 Day/Night theme) |

---

## 📁 Project Structure

```
chat-app/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/demo/chat/
│       │   ├── DemoChatApplication.kt           # App entry point initializing WebSocket manager
│       │   ├── data/
│       │   │   ├── model/                       # ChatMessage, MessageType, MessageStatus, ConnectionState
│       │   │   └── remote/
│       │   │       └── WebSocketClientManager.kt # OkHttp WebSocket client & Flow emitters
│       │   └── ui/chat/
│       │       ├── ChatActivity.kt              # Main UI controller & lifecycle observer
│       │       ├── ChatViewModel.kt             # View State management & coroutine scopes
│       │       ├── ChatViewModelFactory.kt      # ViewModel Provider Factory
│       │       └── adapter/
│       │           ├── ChatAdapter.kt           # RecyclerView ListAdapter with 3 ViewTypes
│       │           └── ChatMessageDiffCallback.kt # DiffUtil implementation
│       └── res/
│           ├── drawable/                        # Vector drawables & chat bubble shapes
│           ├── layout/                          # activity_chat.xml, bubble items, dialog_server_config.xml
│           ├── menu/                            # menu_chat.xml toolbar menu
│           └── values/                          # colors.xml, strings.xml, themes.xml
```

---

## 🚀 How to Build & Run

### Prerequisites
- **JDK**: Java 17 (Eclipse Adoptium Temurin 17)
- **Android SDK**: Compile SDK 35, Min SDK 24

### 1. Build Debug APK via Command Line
```bash
# Build APK
./gradlew assembleDebug

# Output APK path:
# app/build/outputs/apk/debug/app-debug.apk
```

### 2. Install and Launch on Emulator / Device
```bash
# Install APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch App
adb shell am start -n com.demo.chat/.ui.chat.ChatActivity
```

---

## 📄 License
This project is created for demonstration purposes. Free to use and modify.
