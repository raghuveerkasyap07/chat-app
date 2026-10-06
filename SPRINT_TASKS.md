# 🚀 Final Practice Sprint: 2-Way Real-Time Chat App (WhatsApp Clone)

**Project**: Demo Chat App (XML + Kotlin)  
**Reviewer / Evaluator**: Subhadeep  
**Architecture Rules**: MVVM | ViewBinding | Coroutines & StateFlow/SharedFlow | OkHttp WebSockets | FCM Push Notifications  

---

## 👥 Workload Allocation Overview

| Team Member | Workload Level | Primary Role & Responsibilities |
|---|---|---|
| **Raghuveer** | 🔴 **High (Engine & API)** | WebSocket Client Engine, JWT Auth, REST Repositories, StateFlow Emitters |
| **Deepa** | 🟡 **Moderate (UI Screens & Adapters)** | Thread List (`ThreadListActivity`), Chat Screen Controllers & `ListAdapter`s |
| **Ajit** | 🟢 **Targeted (Layouts, Drawables & FCM)** | XML Message Bubble Layouts, Custom Vector Drawables & FCM Push Notifications |

---

## 📋 Detailed Task Breakdown by Member

---

### 🔴 1. Raghuveer (High Workload — Core Engine, WebSockets & REST APIs)

#### 🎯 Goal:
Implement the complete backend integration layer based on `postman_collection.json`, managing REST API authentication, session token persistence, and the live 2-way WebSocket connection engine.

#### 📄 Assigned Files & Classes:
- `com.demo.chat.data.remote.AuthRepository.kt`
- `com.demo.chat.data.remote.ChatRepository.kt`
- `com.demo.chat.data.remote.WebSocketClientManager.kt`
- `com.demo.chat.data.local.SessionManager.kt`
- `com.demo.chat.sdk.core.ChatClient.kt`

#### 🛠️ Tasks:
1. **JWT Authentication & Session Manager (`POST /api/auth/*`)**:
   - Implement `AuthRepository` for `POST /api/auth/register` and `POST /api/auth/login`.
   - Store `authToken` securely in `SessionManager` (EncryptedSharedPreferences / DataStore).
2. **REST API Repositories**:
   - `GET /api/users`: Fetch registered contacts.
   - `POST /api/chats`: Create or retrieve 1-on-1 chat room (`chatId`).
   - `GET /api/chats`: Fetch active conversation list for the current user.
   - `GET /api/chats/{chatId}/messages`: Fetch paginated chat history.
3. **2-Way WebSocket Client Engine (`ws://<host>:5000`)**:
   - Build `WebSocketClientManager` with JWT handshake token.
   - Socket event handling: `join_chat`, `send_message`, `new_message`, `ping_pong`.
   - Expose live asynchronous streams via `StateFlow<ConnectionState>` and `SharedFlow<ChatMessage>`.
   - Implement auto-reconnection with exponential backoff if network drops.

#### ✅ Acceptance Criteria for Raghuveer:
- User registers/logs in and JWT `authToken` persists across app launches.
- WebSocket connects successfully with `Authorization: Bearer <token>` handshake.
- Incoming WebSocket messages are emitted to `SharedFlow` instantly without blocking the UI thread.

---

### 🟡 2. Deepa (Moderate Workload — UI Screens, ViewModels & Adapters)

#### 🎯 Goal:
Build the WhatsApp-style Thread List screen, Chat Activity controllers, ViewModels, and RecyclerView adapters with multiple view types.

#### 📄 Assigned Files & Classes:
- `com.demo.chat.ui.threads.ThreadListActivity.kt` & `ThreadListViewModel.kt`
- `com.demo.chat.ui.threads.adapter.ThreadAdapter.kt` & `ThreadDiffCallback.kt`
- `com.demo.chat.ui.chat.ChatActivity.kt` & `ChatViewModel.kt`
- `com.demo.chat.ui.chat.adapter.ChatAdapter.kt` & `ChatMessageDiffCallback.kt`

#### 🛠️ Tasks:
1. **Thread List Screen (WhatsApp Home - `ThreadListActivity`)**:
   - Displays active conversation threads with peer avatar, name, last message preview, time, and unread count badge.
   - Floating Action Button (FAB) or Contacts dialog to pick a contact from `GET /api/users` and initiate a chat.
   - Clicking a thread launches `ChatActivity` passing `chatId` and `partnerName` via Intent Extras.
2. **Chat Screen Controller (`ChatActivity`)**:
   - Reads `chatId` and `partnerName` from Intent.
   - Shows live connection status light in toolbar (🟢 Connected, 🟡 Connecting, 🔴 Disconnected).
   - Handles message sending and auto-scrolls RecyclerView to bottom on new messages.
3. **RecyclerView Adapters & DiffUtil**:
   - `ThreadAdapter`: Efficient `ListAdapter` handling thread item clicks.
   - `ChatAdapter`: Multi-ViewType `ListAdapter` rendering:
     - `TYPE_SENT`: Right-aligned sent messages.
     - `TYPE_RECEIVED`: Left-aligned received messages.
     - `TYPE_SYSTEM`: Centered system event pills.

#### ✅ Acceptance Criteria for Deepa:
- Thread list loads active chats and updates preview text dynamically when new messages arrive.
- Tapping a thread opens `ChatActivity` with correct conversation data and auto-scrolls to the newest message.

---

### 🟢 3. Ajit (Targeted Workload — XML Layouts, Drawables & FCM Push Notifications)

#### 🎯 Goal:
Design clean Material 3 XML layouts, custom vector drawables, message bubbles, and integrate Firebase Cloud Messaging (FCM) for push notifications.

#### 📄 Assigned Files & Classes:
- `res/layout/activity_thread_list.xml` & `item_chat_thread.xml`
- `res/layout/activity_chat.xml`, `item_chat_sent.xml`, `item_chat_received.xml`, `item_chat_system.xml`
- `res/drawable/*` (Message bubble shapes, status dots, vector icons)
- `com.demo.chat.service.MyFirebaseMessagingService.kt`

#### 🛠️ Tasks:
1. **XML Message Bubbles & Layouts**:
   - `item_chat_sent.xml`: Rounded bubble with tail, right-aligned, displaying message text, time, and delivery status icon.
   - `item_chat_received.xml`: Left-aligned bubble with sender avatar, partner name, text, and timestamp.
   - `item_chat_system.xml`: Center-aligned subtle pill tag.
   - `item_chat_thread.xml`: WhatsApp-style list item with circular avatar, bold peer name, ellipsis text preview, and unread badge.
2. **Drawables & Visual Polish**:
   - `bg_bubble_sent.xml`, `bg_bubble_received.xml`, `bg_bubble_system.xml`, `bg_unread_badge.xml`.
   - Vector status icons (`ic_check.xml`, `ic_double_check.xml`, `ic_error.xml`, `ic_send.xml`).
3. **FCM Push Notification Handling**:
   - Implement `MyFirebaseMessagingService` extending `FirebaseMessagingService`.
   - On `onMessageReceived`: Parse data payload (`title`, `body`, `chatId`) and trigger a high-priority system Notification with PendingIntent targeting `ChatActivity`.
   - On `onNewToken`: Post FCM registration token to backend.

#### ✅ Acceptance Criteria for Ajit:
- XML layouts render cleanly across all screen sizes with ViewBinding.
- Software keyboard (`adjustResize`) resizes input bar without covering message bubbles.
- FCM push notification triggers system banner when a new message arrives while app is in background.

---

## 🧪 Evaluation & Acceptance Criteria (For Subhadeep)

To approve Phase 2 start, Subhadeep will review:

1. **Real-time 2-Way Messaging**: Sending a message from User A instantly appears on User B's device via WebSocket without manual refresh.
2. **UI & Keyboard Handling**: Clean ViewBinding execution, proper `DiffUtil` list updates, and `fitsSystemWindows` / `adjustResize` keyboard handling.
3. **Push Notification Support**: FCM handles incoming notifications gracefully in background/foreground.
4. **Code Review Workflow**: PRs reviewed and approved cleanly before merging to `main`.
