# Local Playlists And README Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add playlist-local search, local playlist library management, and a clean Gitee-ready README without committing unrelated reference projects.

**Architecture:** Keep the current single-ViewModel architecture and add one small repository store for local playlists. Persist local playlist metadata in SharedPreferences as JSON, then rebuild playable playlists from scanned `localSongs`. Reuse the existing playlist detail screen and add-to-playlist sheet so UI behavior stays consistent.

**Tech Stack:** Kotlin, Jetpack Compose, SharedPreferences, org.json, Gradle Android debug build.

## Global Constraints

- Do not push before the user confirms the tested build is acceptable, unless the user explicitly requests a push for this turn.
- Do not include `.reference/`, `.services/`, `.tmp*/`, Gradle build output, APKs, or other third-party scratch projects in commits.
- Do not introduce Room, Navigation Compose, Hilt, or a new dependency for local playlist persistence.
- Kotlin changes must pass `.\gradlew.bat :app:assembleDebug --console=plain`.

---

### Task 1: Gitee README And Git Scope

**Files:**
- Modify: `D:\SiListen\README.md`
- Verify: `D:\SiListen\.gitignore`

**Interfaces:**
- Consumes: existing app modules and `.gitignore`.
- Produces: a Chinese README that explains features, build steps, permissions, local music, custom sources, and excludes scratch projects from the intended commit scope.

- [ ] **Step 1: Replace README with a readable Chinese project overview**

Write sections for project positioning, feature list, build/run commands, source strategy, local music/desktop lyric permissions, repository hygiene, and disclaimer.

- [ ] **Step 2: Verify ignored scratch directories**

Run: `git status --short --ignored | Select-String '.tmp|.reference|.services|build|apk'`
Expected: scratch/reference/build files are ignored and not staged.

### Task 2: Playlist-Only Search Entry

**Files:**
- Modify: `D:\SiListen\app\src\main\java\com\silisten\app\ui\PlaylistScreens.kt`
- Existing state: `SiListenViewModel.updatePlaylistSongSearchQuery(query: String)`

**Interfaces:**
- Consumes: `songSearchQuery`, `onSongSearchQueryChange`.
- Produces: a visible search button in the playlist page top-right, expanding to search only `playlist.songs` by title, artist, or album.

- [ ] **Step 1: Keep search state local to the playlist page**

Use the existing `showInlineSearch` and `filteredSongs` implementation.

- [ ] **Step 2: Make the right-top search button explicit**

Ensure the top-right search icon is visible on song overview, expands into the inline field, and collapses with query cleared.

- [ ] **Step 3: Build**

Run: `.\gradlew.bat :app:assembleDebug --console=plain`
Expected: build succeeds.

### Task 3: Local Playlist Store

**Files:**
- Create: `D:\SiListen\app\src\main\java\com\silisten\app\data\repository\LocalPlaylistStore.kt`
- Modify: `D:\SiListen\app\src\main\java\com\silisten\app\data\model\Song.kt`
- Modify: `D:\SiListen\app\src\main\java\com\silisten\app\SiListenViewModel.kt`

**Interfaces:**
- Produces: `LocalPlaylistStore.load(): List<LocalPlaylistRecord>`, `save(records: List<LocalPlaylistRecord>)`, and `newLocalPlaylistId(): String`.
- Produces: `PlaylistKind.LocalPlaylist`.
- Produces UI state `localPlaylists: List<MusicPlaylist>`.

- [ ] **Step 1: Create persistent local playlist records**

Store each playlist as `{ id, title, songIds }` in SharedPreferences key `local_playlists`.

- [ ] **Step 2: Add ViewModel actions**

Add `createLocalPlaylist(title: String)`, `addPlaylistChooserSongToPlaylist()` support for `PlaylistKind.LocalPlaylist`, and `openLocalPlaylist`.

- [ ] **Step 3: Rebuild local playlists after scans**

After `scanLocalMusic()`, map saved song ids to scanned `localSongs`, update counts and covers, and remove no data automatically only when the user edits a playlist.

### Task 4: Local Library UI

**Files:**
- Modify: `D:\SiListen\app\src\main\java\com\silisten\app\ui\HomeAccountScreens.kt`
- Modify: `D:\SiListen\app\src\main\java\com\silisten\app\ui\SearchOverlaySheets.kt`
- Modify: `D:\SiListen\app\src\main\java\com\silisten\app\ui\SiListenApp.kt`

**Interfaces:**
- Consumes: `uiState.localPlaylists`, `viewModel.createLocalPlaylist`, `viewModel.openPlaylist`.
- Produces: a “本地歌单库” section under local music, create button, local playlist rows, and local playlist options in the add-to-playlist sheet.

- [ ] **Step 1: Hide local song previews from the library card**

Keep only scan status and “查看全部本地歌曲” entry after scanning.

- [ ] **Step 2: Add local playlist library section**

Show a compact create field/button and list user-created local playlists below local music.

- [ ] **Step 3: Include local playlists in AddToPlaylistSheet**

Pass local playlists beside user playlists and label local playlists clearly in subtitles.

### Task 5: Verification And Commit Preparation

**Files:**
- Verify all modified project files.

**Interfaces:**
- Produces: build result and staged file list ready for user review.

- [ ] **Step 1: Run build**

Run:
```powershell
$env:JAVA_HOME='E:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug --console=plain
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Review git status**

Run: `git status --short`
Expected: only SiListen source/docs/resources files are changed; no `.tmp*`, `.reference`, `.services`, build output, or third-party repos appear.
