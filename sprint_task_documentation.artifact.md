# Deepa's Sprint Task: Thread List & Message List UI Integration — Technical Documentation

## Overview

This document outlines the architecture, components, implementation details, and troubleshooting steps for the **Thread List & Message List UI Integration** in the `Textchat_UI` Android application.

---

## Architecture & Technical Stack

- **Pattern**: Model-View-ViewModel (MVVM)
- **Asynchronous Programming**: Kotlin Coroutines & `StateFlow`
- **UI Framework**: Android Views with ViewBinding (Zero `findViewById`)
- **Lists & Adapters**: `RecyclerView` with `ListAdapter` and `DiffUtil.ItemCallback`
- **Lifecycle Safety**: `lifecycleScope.launch` with `repeatOnLifecycle(Lifecycle.State.STARTED)`

---

## Component Breakdown

### 1. Data Models
- **`ChatThread`**: Represents an active conversation thread.
  ```kotlin
  data class ChatThread(
      val threadId: String,
      val peerName: String,
      val peerAvatarUrl: String? = null,
      val lastMessageText: String,
      val lastMessageTimestamp: Long = System.currentTimeMillis(),
      val unreadCount: Int = 0
  )
  ```
- **`ThreadListUiState`**: Sealed interface for managing loading, success, and empty states in the thread list.

### 2. Thread List Screen
- **`ThreadListActivity`**: The primary launch activity (`MAIN/LAUNCHER`) displaying recent conversations.
- **`item_chat_thread.xml`**: Thread item layout featuring:
  - Circular avatar / initials badge view
  - Bold peer name TextView
  - Single-line ellipsis message preview TextView
  - Timestamp TextView
  - Conditional unread count badge
- **`ThreadAdapter`**: Optimized `ListAdapter` handling efficient diffing and click navigation to `ChatActivity` passing `threadId` and `peerName`.

### 3. Thread List ViewModel
- **`ThreadListViewModel`**: Manages thread data, exposing `threads` (`StateFlow<List<ChatThread>>`) and `uiState` (`StateFlow<ThreadListUiState>`) with initial mock conversations.

### 4. Message List Screen & Integration
- **`ChatActivity`**: Receives `threadId` and `peerName` from the intent, updates the header toolbar title with the peer's name, and manages real-time messaging.
- **`ChatAdapter`**: Renders multiple view types (`TYPE_SENT`, `TYPE_RECEIVED`, `TYPE_SYSTEM`) using dedicated ViewHolders and ViewBinding.
- **Auto-Scrolling**: Automatically smooth-scrolls to the latest message on new arrivals.

---

## Troubleshooting & System Fixes

> [!NOTE]
> **Gradle Download Timeout (`SocketTimeoutException`)**
> Resolved by switching to the fully cached local Gradle distribution (`9.5.0`) and configuring offline toolchain installation paths in `gradle.properties`.

> [!IMPORTANT]
> **Android SDK Path (`local.properties`)**
> Configured `sdk.dir=C\:\\Users\\dell\\AppData\\Local\\Android\\Sdk` to enable successful compilation.

> [!TIP]
> **Keyboard & Display Overlap (`adjustResize`)**
> Added `android:fitsSystemWindows="true"` to `activity_chat.xml` and configured `windowSoftInputMode="adjustResize"` in `AndroidManifest.xml` so the software keyboard dynamically resizes the layout and keeps input text fully visible.

---

## Verification & Build Status

- **Build Tool**: Gradle 9.5.0
- **Build Status**: ✅ **SUCCESSFUL** (`app:assembleDebug` completed with 0 errors).
