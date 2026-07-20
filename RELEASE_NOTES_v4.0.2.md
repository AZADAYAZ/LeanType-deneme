# LeanType v4.0.2 Release Notes

## 🚀 Key Highlights

### 👆 Native Gesture Engine Cleanup & Safety
- **Removed Experimental Java Gesture Engine**: Completely stripped the unstable Java gesture engine. Gesture typing now relies exclusively on the high-performance native C++ library (`libjni_latinimegoogle.so` / imported `libjni_latinime.so`).
- **SIGSEGV Crash Protection**: Added strict validation during JNI library loading so incompatible libraries fail gracefully instead of crashing the process.

### 🛠️ Stability, Performance & Lifecycle Fixes
- **IME Teardown ANR Fix**: Made dictionary closing non-blocking and cancelled active gesture indexing threads on `LatinIME.onDestroy()`.
- **Background Observer Leak Fix**: Fixed lifecycle management and toggle sync for Contacts, Apps, and SMS OTP background observers.
- **Android 14+ Compatibility**: Exported system broadcast receivers for ringer mode changes and enabled `onBackInvokedCallback` support.
- **Dictionary Ranking Fine-tuning**: Gated personal dictionary unigram fallback behind `mPrioritizePersonalSuggestions` and removed unigram fallbacks from history dictionaries.

---

## 📄 Full Changelog
See [CHANGELOG.md](file:///home/arjun/extra/Projects/HeliboardL/CHANGELOG.md) for full history.
