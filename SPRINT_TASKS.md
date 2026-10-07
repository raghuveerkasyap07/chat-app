# 🚀 Final Practice Sprint: 2-Way Real-Time Chat App with Media Attachments (WhatsApp Clone)

**Project**: Demo Chat App (XML + Kotlin)  
**Reviewer / Evaluator**: Subhadeep  
**Architecture Rules**: MVVM | ViewBinding | Coroutines & StateFlow/SharedFlow | OkHttp WebSockets & Multipart Uploads | Coil Image Loading | FCM Push Notifications  

---

## 👥 Workload Allocation Overview

| Team Member | Workload Level | Primary Role & Responsibilities |
|---|---|---|
| **Raghuveer** | 🔴 **High (Engine & Media API)** | WebSocket Engine, JWT Auth, REST Repositories, Multipart Photo Upload API, StateFlow Emitters |
| **Deepa** | 🟡 **Moderate (UI Screens & Photo Picker)** | Thread List (`ThreadListActivity`), Photo Picker Integration, Image ViewTypes & `ListAdapter`s |
| **Ajit** | 🟢 **Targeted (Image Layouts, Drawables & FCM)** | XML Image Bubble Layouts, Attachment Icons, Coil Setup & FCM Push Notifications |

---

## 📋 Detailed Task Breakdown by Member

---

### 🔴 1. Raghuveer (High Workload — Core Engine, WebSockets, REST & Media Upload APIs)

#### 🎯 Goal:
Implement the complete backend integration layer based on `postman_collection.json`, managing REST API authentication, session token persistence, multipart photo upload endpoints, and the live 2-way WebSocket connection engine.

#### 📄 Assigned Files & Classes:
- `com.demo.chat.data.remote.AuthRepository.kt`
- `com.demo.chat.data.remote.ChatRepository.kt`
- `com.demo.chat.data.remote.MediaRepository.kt`
- `com.demo.chat.data.remote.WebSocketClientManager.kt`
- `com.demo.chat.sdk.core.ChatClient.kt`

#### 🛠️ Tasks:
1. **JWT Authentication & Session Manager (`POST /api/auth/*`)**:
   - Implement `AuthRepository` for `POST /api/auth/register` and `POST /api/auth/login`.
   - Store `authToken` securely in `SessionManager`.
2. **REST & Multipart Media Upload APIs**:
   - `GET /api/users`: Fetch registered contacts.
   - `POST /api/chats`: Create or retrieve 1-on-1 chat room (`chatId`).
   - `GET /api/chats`: Fetch active conversation list for the current user.
   - `GET /api/chats/{chatId}/messages`: Fetch paginated chat history.
   - `POST /api/chats/{chatId}/attachments`: Multipart photo upload returning image URL and metadata.
3. **2-Way WebSocket Client Engine with Image Payload Support**:
   - Build `WebSocketClientManager` supporting both text and image payload types (`TYPE_TEXT`, `TYPE_IMAGE`).
   - Socket event handling: `join_chat`, `send_message`, `send_image`, `new_message`, `ping_pong`.
   - Expose live asynchronous streams via `StateFlow<ConnectionState>` and `SharedFlow<ChatMessage>`.

#### ✅ Acceptance Criteria for Raghuveer:
- User registers/logs in and JWT `authToken` persists across app launches.
- Multipart photo upload successfully uploads images and transmits image URL over WebSocket.
- Incoming text and image WebSocket frames are emitted to `SharedFlow` instantly without blocking the UI.

---

### 🟡 2. Deepa (Moderate Workload — UI Screens, Photo Picker & Multi-ViewType Adapters)

#### 🎯 Goal:
Build the WhatsApp-style Thread List screen, Chat Activity controllers, System Photo Picker integration, ViewModels, and RecyclerView adapters with image view types.

#### 📄 Assigned Files & Classes:
- `com.demo.chat.ui.threads.ThreadListActivity.kt` & `ThreadListViewModel.kt`
- `com.demo.chat.ui.chat.ChatActivity.kt` & `ChatViewModel.kt`
- `com.demo.chat.ui.chat.adapter.ChatAdapter.kt` & `ChatMessageDiffCallback.kt`

#### 🛠️ Tasks:
1. **Thread List Screen (WhatsApp Home - `ThreadListActivity`)**:
   - Displays active conversation threads with peer avatar, name, last message/photo preview, time, and unread count badge.
   - Contacts selection dialog (`GET /api/users`) to initiate a 1-on-1 chat.
   - Launches `ChatActivity` passing `chatId` and `partnerName` via Intent Extras.
2. **Photo Picker Integration & Chat Controller (`ChatActivity`)**:
   - Integrated System Photo Picker (`ActivityResultContracts.PickVisualMedia()`) triggered via attachment button.
   - Uploads selected photo, displays optimistic sending bubble, and broadcasts image URL over WebSocket.
   - Fullscreen image preview dialog when tapping on an image bubble in the chat.
3. **RecyclerView Adapters & DiffUtil**:
   - `ChatAdapter`: Multi-ViewType `ListAdapter` rendering:
     - `TYPE_SENT_TEXT`: Right-aligned text messages.
     - `TYPE_SENT_IMAGE`: Right-aligned photo bubbles with Coil image loading.
     - `TYPE_RECEIVED_TEXT`: Left-aligned text messages.
     - `TYPE_RECEIVED_IMAGE`: Left-aligned photo bubbles.
     - `TYPE_SYSTEM`: Centered event badges.

#### ✅ Acceptance Criteria for Deepa:
- Photo Picker opens, allows selecting an image, and sends the photo into the chat timeline.
- Tapping any photo bubble opens the full-screen image preview.

---

### 🟢 3. Ajit (Targeted Workload — XML Layouts, Attachment Icons & FCM Notifications)

#### 🎯 Goal:
Design Material 3 XML layouts for text and image message bubbles, attachment icons, Coil image loading configuration, and Firebase Cloud Messaging (FCM) for push notifications.

#### 📄 Assigned Files & Classes:
- `res/layout/item_chat_sent.xml`, `item_chat_sent_image.xml`
- `res/layout/item_chat_received.xml`, `item_chat_received_image.xml`
- `res/layout/dialog_image_preview.xml`
- `res/drawable/ic_attach.xml`, `ic_photo.xml`, vector status icons
- `com.demo.chat.service.MyFirebaseMessagingService.kt`

#### 🛠️ Tasks:
1. **XML Message Bubbles & Photo Layouts**:
   - `item_chat_sent_image.xml`: Right-aligned rounded image card with rounded corners, timestamp, and double-check status.
   - `item_chat_received_image.xml`: Left-aligned rounded image card with sender title, partner avatar, and timestamp.
   - `dialog_image_preview.xml`: Fullscreen dialog layout for viewing high-res photos.
2. **Drawables & Attachment Button**:
   - `ic_attach.xml` (Paperclip attachment icon), `ic_photo.xml` (Gallery icon).
   - Configure Coil image loader dependency for rounded corners and crossfade animations.
3. **FCM Push Notification Handling**:
   - Implement `MyFirebaseMessagingService` handling incoming notification payloads (including image notification previews).
   - Triggers high-priority Android notification with `PendingIntent` launching `ChatActivity`.

#### ✅ Acceptance Criteria for Ajit:
- Image bubbles render with smooth rounded corners, aspect-ratio constraint, and Coil placeholder/error states.
- FCM handles background notifications containing photo attachment previews.

---

## 🧪 Evaluation & Acceptance Criteria (For Subhadeep)

To approve Phase 2 start, Subhadeep will review:

1. **Real-Time 2-Way Text & Photo Sharing**: Sending text or photo attachments from User A instantly displays on User B's device in real-time.
2. **Photo Picker & Image Rendering**: System Photo Picker selects images cleanly; Coil renders images with progress animation and full-screen zoom preview on tap.
3. **Push Notification Support**: FCM triggers system banner with photo preview when app is in background.
4. **Code Review Workflow**: PRs reviewed and merged cleanly on GitHub (`main` / feature branches).
