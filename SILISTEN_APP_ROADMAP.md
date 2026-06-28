# SiListen App Roadmap

## Product Direction

SiListen is an Android music app with an Apple Music inspired experience: clean hierarchy, immersive album-art backgrounds, glassy motion, a reliable mini player, a proper media notification controller, and lyrics that feel central rather than decorative.

The app should stay simple for users:

- Open the app and immediately find music.
- Tap any song and always see the mini player.
- Tap the mini player or notification and enter the player or lyrics quickly.
- Use NetEase Cloud Music without depending on a local LAN API whenever possible.
- Switch theme, blur, dock, lyrics, playback quality, and sources from settings without technical wording.

## Current Architecture Snapshot

The former large `SiListenApp.kt` has been split into focused UI files:

- `SiListenApp.kt`: app shell, pager navigation, modal player sheet, top-level back handling.
- `BottomChrome.kt`: mini player and floating bottom dock.
- `CommonUi.kt`: shared glass, cards, buttons, rows, formatting, and visual primitives.
- `HomeAccountScreens.kt`: home, search, library, account, login, QR, local music.
- `PlaylistScreens.kt`: playlist detail, comments, playlist actions.
- `SettingsScreens.kt`: theme, source, playback, donation, and settings panels.
- `PlayerScreens.kt`: full player, queue, comments, Apple Music style lyrics, particle lyrics.

## 8 Hour Execution Plan

### Hour 1: Stabilize Baseline

- Run `git status --short` before editing.
- Run `.\gradlew.bat assembleDebug`.
- Install on device and check `logcat -b crash`.
- Confirm bottom dock click, drag, page swipe, mini player, playlist overlay, and player sheet still open.
- Do not start visual tuning until the baseline is stable.

Acceptance:

- Debug build succeeds.
- App launches without crash.
- Home, Search, Music Library, Account all reachable by dock tap and horizontal swipe.

### Hour 2: Bottom Dock And Mini Player Polish

- Keep the KernelSU-like liquid dock architecture.
- Preserve `HorizontalPager` as the page driver.
- Make dock click, drag, and page swipe share one source of truth.
- Keep liquid blur enabled only where it does not trigger MIUI RenderThread crashes.
- Verify mini player remains above dock and appears after any song starts.

Acceptance:

- Dock click lands on the exact target page.
- Dock drag lands on the exact target page.
- Page swipe moves the dock indicator without lag.
- Mini player is visible whenever playback has a current song.

### Hour 3: Player Sheet And Lyrics

- Refine Apple Music style lyrics as the default experience.
- Keep current lyric line vertically anchored.
- Ensure non-current lyrics fade naturally and remain readable in light and dark modes.
- Keep particle lyrics as an optional style, not a replacement for Apple Music lyrics.
- Avoid duplicate full-line and per-word rendering overlap.

Acceptance:

- Lyrics do not overlap.
- Current lyric is clear, larger, and centered.
- Progress drag updates lyric position.
- Empty/loading lyric states are user-friendly.

### Hour 4: NetEase Login And API Reliability

- Prefer in-app/direct NetEase API client paths over local LAN services.
- Keep local API fallback only as a developer/testing path.
- Improve QR login failure messaging.
- Keep SMS login available as a practical fallback.
- Open the NetEase Cloud Music app, not a browser page, when asking the user to scan or confirm.

Acceptance:

- Failed QR generation explains what the user can do next.
- SMS login path is clear.
- App does not require the developer's computer to be on the same LAN for normal usage.

### Hour 5: Media Notification And Playback Session

- Verify notification uses media style controls.
- Notification tap should open the player/lyrics path.
- Previous, play/pause, next, and dismiss must update playback state.
- Check lockscreen visibility and Android notification permission flow.

Acceptance:

- Notification actions work while app is backgrounded.
- Tapping notification opens player lyrics by default.
- Notification text and artwork match current song when available.

### Hour 6: Settings And Theme Consistency

- Keep all user-facing copy in Chinese.
- Remove implementation wording such as internal flag names.
- Make light/dark colors readable for all headings, cards, rows, subtitles, and chips.
- Keep theme controls inspired by KernelSU's Material/Miuix choices, but phrase them for SiListen users.

Acceptance:

- Text remains readable in both light and dark mode.
- Settings labels are user-facing.
- Monet/dynamic color switches behave predictably.

### Hour 7: Code Cleanup And Boundaries

- Keep UI files focused by feature.
- Move new feature-specific UI into the correct file instead of growing `SiListenApp.kt`.
- Avoid adding network or playback logic to composables.
- Keep shared visual primitives in `CommonUi.kt` only if at least two screens need them.

Acceptance:

- `SiListenApp.kt` stays under roughly 600 lines.
- No large feature block is added to the app shell.
- Build still passes after cleanup.

### Hour 8: Device QA And Handoff

- Build debug APK.
- Install and launch on device.
- Test core flows: home refresh, song play, mini player, full player, lyrics, queue, comments, settings, account login entry, notification.
- Check crash log.
- Commit only after build and launch verification.
- Push only after the user explicitly asks or after a completed agreed milestone.

Acceptance:

- No new crash in `logcat -b crash`.
- All core navigation paths work.
- Commit message describes the user-visible change.

## Non-Negotiables

- Do not remove the mini player.
- Do not remove the liquid dock without explicit approval.
- Do not hide dock regressions behind unrelated visual tweaks.
- Do not expose implementation names in user-facing text.
- Do not commit unverified large UI rewrites.
- Do not push unless requested or clearly agreed.

## Useful Commands

```powershell
git status --short
.\gradlew.bat assembleDebug
& 'C:\Users\lllk\AppData\Local\Android\Sdk\platform-tools\adb.exe' devices
& 'C:\Users\lllk\AppData\Local\Android\Sdk\platform-tools\adb.exe' -s '<device>' install -r 'app\build\outputs\apk\debug\app-debug.apk'
& 'C:\Users\lllk\AppData\Local\Android\Sdk\platform-tools\adb.exe' -s '<device>' logcat -b crash -d -t 120
```

