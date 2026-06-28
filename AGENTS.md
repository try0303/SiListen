# AGENTS.md

## Project Goal

Build SiListen into a stable, clean, Apple Music inspired Android music app with reliable playback, mini player, media notification controls, immersive lyrics, NetEase Cloud Music support, and a KernelSU-like liquid floating bottom dock.

## Working Rules

- Always inspect `git status --short` before editing.
- Treat existing uncommitted changes as user work unless you made them in the current task.
- Do not revert user changes without explicit permission.
- Prefer small, verifiable edits.
- Run `.\gradlew.bat assembleDebug` after Kotlin changes.
- Install and check crash logs after risky UI, playback, notification, blur, or navigation changes.
- Do not push unless the user asks.

## UI Architecture

- Keep `SiListenApp.kt` as the app shell only.
- Put bottom dock and mini player changes in `BottomChrome.kt`.
- Put player sheet, lyrics, queue, and player comments in `PlayerScreens.kt`.
- Put playlist detail and playlist comments in `PlaylistScreens.kt`.
- Put settings pages in `SettingsScreens.kt`.
- Put home, search, library, account, login, QR, and local music in `HomeAccountScreens.kt`.
- Put shared visual primitives in `CommonUi.kt`.

## Design Direction

- Use Apple Music as the primary product reference.
- Keep the app simple, high-contrast, and readable.
- Use glass, blur, and motion intentionally.
- The liquid dock should follow the KernelSU/Miuix liquid-glass behavior where stable.
- Do not over-blur text until it becomes unreadable.
- Light and dark themes must both be checked.

## Bottom Dock Rules

- The dock is driven by `HorizontalPager`.
- Tapping a dock item must land on the exact page.
- Dragging the dock indicator must land on the exact page.
- Swiping pages must move the dock indicator without lag.
- Do not remove the liquid dock to fix unrelated bugs.
- If MIUI native blur crashes, isolate backdrop sampling instead of deleting the feature.

## Mini Player Rules

- The mini player must appear whenever a song is active.
- It should stay above the dock.
- It should open lyrics/player when tapped.
- It should not cover important text with avoidable hard clipping.

## Lyrics Rules

- Apple Music style lyrics are the default target.
- Current lyric should be centered, large, bold, and readable.
- Non-current lyrics should fade with distance.
- Avoid duplicate rendering of full-line and word-highlight text.
- Particle lyrics are an optional mode and should remain simple.

## Network And Login Rules

- Prefer end-user friendly direct client behavior.
- Do not require a local API server for normal usage.
- Keep local API URLs as fallback/testing configuration only.
- Login failures should explain the next action in Chinese.
- Opening NetEase confirmation should target the NetEase Cloud Music app, not a browser, when possible.

## Notification Rules

- Use Android media notification controls.
- Notification actions must map to previous, play/pause, next, and dismiss.
- Notification tap should open the player/lyrics flow.
- Always test notification behavior after playback controller changes.

## User-Facing Text

- User-visible text should be Chinese.
- Do not show implementation names such as `enableFloatingBottomBar`, `donate_qr`, or internal API details.
- Error messages should be practical and calm.

## Verification Checklist

- `git status --short`
- `.\gradlew.bat assembleDebug`
- App launches on device.
- No new `logcat -b crash` entries.
- Dock tap, dock drag, and page swipe all work.
- Mini player appears after playback starts.
- Player sheet opens lyrics, queue, comments, and detail pages.
- Settings remain readable in light and dark modes.

