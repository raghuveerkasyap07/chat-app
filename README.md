# Demo Chat App & Android SDK (`chat-app`)

A modern, production-ready real-time chat application and Android SDK built with Kotlin, MVVM architecture, Coroutines `StateFlow`, ViewBinding, and WebSockets.

---

## 🚀 Features

- **Real-Time WebSocket Communication**: Connects to a live WebSocket echo server with automatic connection state monitoring (`Connected`, `Connecting`, `Disconnected`, `Error`).
- **Thread List Screen (`Conversations`)**: Displays recent chat threads with circular initials avatars, message previews, timestamps, and unread count badges.
- **Message List Screen (Chat Detail)**: RecyclerView supporting multiple message view types (`SENT`, `RECEIVED`, `SYSTEM`), auto-scrolling, clipboard copy, and quick response suggestion chips.
- **Modular Android SDK (`chatsdk`)**: Packaged as a reusable Android Archive (`.aar`) library for easy integration into other Android apps.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 2.1.0
- **Architecture**: MVVM (Model-View-ViewModel)
- **Asynchronous Flow**: Kotlin Coroutines & `StateFlow` / `SharedFlow`
- **UI Binding**: ViewBinding (Zero `findViewById`)
- **Networking**: OkHttp 4 WebSocket Client & Gson
- **Build System**: Gradle 9.5.0 & Android Gradle Plugin (AGP) 8.8.0

---

## 📦 Project Structure

```text
chat-app/
├── app/                  # Main Android application module
│   └── src/main/java/com/demo/chat/
│       ├── ui/chat/      # ChatActivity, ChatViewModel, ChatAdapter
│       ├── ui/threads/   # ThreadListActivity, ThreadListViewModel, ThreadAdapter
│       └── data/         # WebSocketClientManager, Models (ChatMessage, ChatThread)
├── chatsdk/              # Reusable Android SDK module (.aar output)
└── README.md
```

---

## ⚙️ Building & Running

1. **Clone the repository**:
   ```bash
   git clone https://github.com/raghuveerkasyap07/chat-app.git
   ```
2. **Open in Android Studio** (Ladybug / Jellyfish or newer).
3. **Build the App / APK**:
   ```bash
   ./gradlew assembleDebug
   ```
4. **Build the Android SDK (.aar)**:
   ```bash
   ./gradlew :chatsdk:assembleRelease
   ```
   *(Output path: `chatsdk/build/outputs/aar/chatsdk-release.aar`)*

---

## 📄 License
Project developed for Phase 2 Android Development Curriculum Sprint Task.
