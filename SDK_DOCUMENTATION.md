# 📦 Chat SDK Architecture & Conversion Documentation

This document explains **what an SDK is**, how the **Demo Chat application was transformed into a reusable Android SDK (`:chat-sdk`)**, and how developers can integrate it into any Android app.

---

## 💡 1. What is an SDK?

**SDK** stands for **Software Development Kit**.

It is a pre-packaged software library containing code, APIs, UI components, and documentation that enables external developers to quickly add complex functionality (like real-time chat, payments, maps, or analytics) into their own mobile or web applications without building it from scratch.

### 🚗 The Analogy
- **App (APK)** = A complete, finished car ready to drive.
- **SDK** = An engine kit or navigation system that any car manufacturer (app developer) can plug into their vehicle.

---

## 🔄 2. What Changed: App vs. SDK Architecture

Previously, the app was a single monolithic application module (`:app`). To make the chat engine reusable, we restructured the project into a **Multi-Module Gradle Architecture**:

```
Monolithic App (Before)                 Modular SDK Architecture (Now)
┌──────────────────────┐                ┌─────────────────────────────┐
│  :app                │                │  :app (Sample Consumer)     │
│  ├── WebSockets      │   ──Converted─>│  └── ChatView Integration   │
│  ├── UI & Layouts    │      To        └──────────────┬──────────────┘
│  └── ViewModels      │                               │ Uses
└──────────────────────┘                ┌──────────────▼──────────────┐
                                        │  :chat-sdk (Library Module) │
                                        │  ├── ChatEngine Singleton   │
                                        │  ├── ChatClient & WebSockets│
                                        │  └── ChatView Custom View   │
                                        └─────────────────────────────┘
```

---

## 🛠️ 3. Detailed Component Breakdown of `:chat-sdk`

### A. Core Engine (`com.demo.chat.sdk.ChatEngine`)
The main entry point for the SDK. Uses the Singleton pattern for thread-safe global access.

```kotlin
// Developer initializes SDK once (e.g. in Application class or Activity):
ChatEngine.initialize(
    context = this,
    serverUrl = "wss://ws.postman-echo.com/raw"
)
```

---

### B. Networking Client (`com.demo.chat.sdk.core.ChatClient`)
Encapsulates low-level OkHttp `WebSocket` connection, frame dispatch, and thread-safe callbacks:
- **`StateFlow<SdkConnectionState>`**: Live connection state (`Connected`, `Connecting`, `Disconnected`, `Error`).
- **`SharedFlow<String>`**: Asynchronous incoming WebSocket text message stream.
- **Auto Ping/Pong & Heartbeat**: Configured with 15-second automatic ping intervals and timeout handling.

---

### C. Reusable UI Custom View (`com.demo.chat.sdk.ui.ChatView`)
A plug-and-play **Compound XML View** that encapsulates the entire chat interface (header status bar, RecyclerView, suggestion chips, and input field).

#### Custom Attributes (`res/values/attrs.xml`):
```xml
<declare-styleable name="ChatView">
    <attr name="titleText" format="string" />
    <attr name="showSuggestions" format="boolean" />
    <attr name="sentBubbleColor" format="color" />
    <attr name="receivedBubbleColor" format="color" />
</declare-styleable>
```

---

## 🚀 4. How Other Developers Integrate the SDK

### Step 1: Add the SDK to Layout XML
Developers can drop `ChatView` into any XML layout file:

```xml
<com.demo.chat.sdk.ui.ChatView
    android:id="@+id/chatView"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:showSuggestions="true"
    app:titleText="Customer Live Support" />
```

---

### Step 2: Initialize in Kotlin (2 Lines of Code)
```kotlin
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Initialize Engine
        val chatEngine = ChatEngine.initialize(context = this)

        // 2. Attach UI
        findViewById<ChatView>(R.id.chatView).setupWithEngine(chatEngine)
    }
}
```

---

### Step 3: Programmatic SDK Control
Developers can also control the SDK directly without UI:

```kotlin
val client = ChatEngine.getInstance().client

// Send custom message
client.sendMessage("Hello via SDK API!")

// Send manual Ping frame
client.sendPing()

// Listen to connection status changes
client.addConnectionListener { state ->
    when (state) {
        is SdkConnectionState.Connected -> Log.d("SDK", "Connected!")
        is SdkConnectionState.Disconnected -> Log.d("SDK", "Disconnected!")
        else -> {}
    }
}
```

---

## 🎁 5. Exporting as an `.AAR` Library File

The SDK compiles into a standalone **`.aar`** (Android Archive) package that can be hosted on Maven Central, GitHub Packages, or shared directly with third-party developers:

```bash
# Command to build SDK Release AAR
./gradlew :chat-sdk:assembleRelease

# Generated AAR location:
# chat-sdk/build/outputs/aar/chat-sdk-release.aar
```

---

## 📊 Summary Comparison

| Aspect | Original App (:app) | New Chat SDK (:chat-sdk) |
|---|---|---|
| **Module Type** | `com.android.application` | `com.android.library` |
| **Output** | Standalone `.apk` | Reusable `.aar` library package |
| **Reusability** | Hard to copy into other projects | Drop-in XML `<ChatView>` & 2 lines of Kotlin |
| **Integration Effort** | Manual copy of ~30 files | Import 1 dependency |
| **Customization** | Hardcoded layout | Configurable XML attributes & theme overrides |
