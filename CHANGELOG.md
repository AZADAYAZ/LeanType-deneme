# Changelog

All notable changes to LeanType will be documented in this file.

## [v4.0.2] - 2026-07-20

### Added
- **Next-Word Suggestion Controls**: Added preference controls to enable/disable and customize next-word predictions.

### Refactored
- **Removed Java Gesture Engine**: Completely removed the experimental Java gesture engine in favor of the native C++ gesture engine, preventing crashes and reducing codebase bloat.

### Fixed
- **Native Gesture Library Safety**: Strictly validated native gesture library compatibility in JNI loader to prevent `SIGSEGV` native crashes with incompatible libraries.
- **IME Lifecycle & ANR Prevention**: Cancelled gesture indexing tasks and made dictionary cleanup non-blocking on `LatinIME.onDestroy()`.
- **Background Service & Observer Leaks**: Unified background service toggles (Contacts, Apps, SMS OTP) and safely unregistered `ContentObserver`s on teardown.
- **Android 14+ System Fixes**: Exported ringer mode broadcast receiver and supported `onBackInvokedCallback` system behavior.
- **Personal Dictionary Fallback**: Gated personal dictionary unigram fallbacks behind `mPrioritizePersonalSuggestions` and disabled history dictionary unigram fallbacks.

## [v4.0.1] - 2026-07-19

### Fixed
- **PointerTracker TimerProxy Safety**: Fixed `NullPointerException` on `TimerProxy.startTypingStateTimer` by defensively defaulting static `sTimerProxy` to `TimerProxy.NULL` and guarding accesses when proxy map references are cleared during view teardowns or transitions.
- **Dynamic InputConnection & Long Press Fix**: Fixed issue where re-opening the keyboard in Launcher or search fields dropped character input and long-press popup key selections by dynamically fetching the live system `InputConnection`.

## [v4.0.0] - 2026-07-19

### Added
- **First-Word Prediction Toggle**: User setting to enable/disable suggestions for first word in text field.
- **Hardware Keyboard Mode**: Option to display toolbar-only mode when physical keyboard is connected.
- **Equal Toolbar Key Spacing**: Equal key distribution for unscrollable expanded and dual toolbars.

### Fixed
- **Native JNI Protection**: Added `isValidDictionary()` guards and exception handling around `BinaryDictionary` JNI calls.
- **ANR & Freeze Prevention**: Added non-blocking timeouts to `CountDownLatch.await()` across backup/restore and file lookup paths.
- **Gesture Index Thread Storm**: Switched gesture index building to managed `KEYBOARD` executor pool.
- **Memory & View Leak Fixes**: Cleared static proxy references in `PointerTracker.clearOldViewData()`, enabled `largeHeap`, and trimmed memory when UI hidden.
- **Screenshot Scanner Optimization**: Replaced active background `ContentObserver` with on-demand check.
- **Regional Dictionary Fallback**: Aggregated main/emoji dictionaries across variant and language fallbacks (`en-IN` -> `en`).
- **Emoji Dictionary Persistence**: Fixed recognition of downloaded `emoji_*.dict` files and added preference tracking to prevent deletion on upgrade.
- **Thai Word Segmentation**: Preserved Thai word boundaries during text expansion.

### AI Enhancements
- **Proofread Anti-Answering Guard**: Prevented models from expanding prompts into multi-paragraph answers during proofreading.
- **Clean Translation Output**: Stripped section headers and reasoning/thinking blocks from translation outputs.
