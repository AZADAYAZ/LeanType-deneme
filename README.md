# LeanType

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="docs/images/leantype_banner_dark.svg">
  <source media="(prefers-color-scheme: light)" srcset="docs/images/leantype_banner_light.svg">
  <img alt="LeanType Banner" src="docs/images/leantype_banner_light.svg">
</picture>

[![Download](docs/badges/download.svg)](https://github.com/LeanBitLab/HeliboardL/releases/latest) [![Downloads](docs/badges/downloads.svg)](https://github.com/LeanBitLab/HeliboardL/releases) [![Stars](docs/badges/stars.svg)](https://github.com/LeanBitLab/HeliboardL/stargazers)

**LeanType** is a fork of [HeliBoard](https://github.com/Helium314/HeliBoard) - a privacy-conscious and customizable open-source keyboard based on AOSP/OpenBoard.



## What's New in LeanType

- **[🤖 Multi-Provider AI](docs/FEATURES.md#supported-ai-providers)** - Proofread using **Gemini**, **Groq** (Llama 3, Mixtral), or **OpenAI-compatible** providers. Supports dynamic fetching of latest models directly from providers.
- **[🛡️ Offline AI (GGUF)](docs/FEATURES.md#5-offline-proofreading-privacy-focused)** - Private, on-device proofreading and translation using local **GGUF models** powered by `llama.cpp` (Offline build only).
- **🌐 AI Translation** - Translate selected text directly using your chosen AI provider, with a separate model selector.
- **[🧠 Custom AI Keys](docs/FEATURES.md#4-custom-ai-keys--keywords)** - Assign custom prompts, personas (#editor, #proofread), and custom text labels/tags (showing as themed capsules) to 10 customizable toolbar keys.
- **🧭 Text Editing Mode** - Gboard-style dedicated editing panel with precise DPAD navigation, custom selection mode (Shift + arrows), clipboard actions, and editing utilities.
- **[✍️ Handwriting Input](docs/FEATURES.md#8-handwriting-input)** - Draw characters directly on a handwriting recognition canvas (Standard version, requires [Leantype-Handwriting-Plugin](https://github.com/LeanBitLab/Leantype-Handwriting-Plugin)).
- **[👆 Built-in Gesture Typing](docs/FEATURES.md#9-built-in-gesture-typing)** - Gesture typing works out of the box using our new built-in pure-Java fallback engine, removing the strict dependency on native Google libraries.
- **🖱️ Touchpad Mode** - Swipe spacebar up to toggle touchpad with custom sensitivity controls, including full-screen laptop-style touchpad mode.
- **📝 Text Expander** - Built-in expansion tool supporting custom shortcuts and dynamic template variables (date, time, clipboard, custom placeholders).
- **🪟 Floating Keyboard** - Detach the keyboard into a draggable window for seamless multitasking. Includes a persistent mode option to keep the keyboard floating.
- **⌨️ Dual Toolbar / Split Suggestions** - Option to split suggestions and toolbar for easier access.
- **📥 Dynamic Downloader** - Standard version dynamically downloads layout dictionaries, emoji dictionaries, and handwriting plugins on demand, keeping initial app size small.
- **🔍 Clipboard Search & Undo** - Search through your clipboard history directly from the toolbar, undo accidental item deletions, and fold/collapse pinned items by default to save space.
- **📸 Screenshot Suggestion & Clipboard** - Suggests recently taken screenshots for quick sharing via the suggestion strip and saves them to your clipboard history.
- **🔎 Emoji Search** - Search for emojis by name. *Requires loading an Emoji Dictionary.*
- **🚫 Blocked Words Blacklist** - Prevent unwanted words from being suggested, with custom regex pattern support and a blacklist management screen.
- **✉️ Auto-Read OTP** - Automatically reads OTP codes from SMS notifications for quick and easy suggestion strip entry.
- **💾 Selective Backup & Restore** - Backup and restore settings, dictionaries, and AI prompt configurations selectively.
- **[⌨️ Direct Switch IME](docs/FEATURES.md#10-direct-switch-target-ime)** - Map custom keycode (`-10076`) to any toolbar key to switch directly to another input method.
- **[🎨 Custom Layouts](docs/FEATURES.md#11-custom-layouts-customization)** - Save up to five custom layout profiles with persistent slot index tracking.
- **🔒 Privacy Choices** - Choose **Standard** (Opt-in AI, Handwriting), **Offline** (Hard-disabled network, offline GGUF model load), or **Offline Lite** (Minimalist, no AI) versions.
- **🎨 Modern UI** - "Squircle" key backgrounds, refined icons, and polished aesthetics.
- **🕵️ Clear Incognito Mode** - Distinct "Hat & Glasses" icon for clear visibility.
- **🔄 Google Dictionary Import** - Easily import your personal dictionary words.
- **⚙️ Enhanced Customization** - Force auto-capitalization toggle, reorganized settings, and more.



## Download

<table border="0">
  <tr>
    <td align="center" valign="middle">
      <a href="https://github.com/LeanBitLab/HeliboardL/releases/latest">
        <img alt="Get it on GitHub" src="docs/images/get-it-on-github.png" height="90">
      </a>
    </td>
    <td align="center" valign="middle">
      <a href="https://apps.obtainium.imranr.dev/redirect.html?r=obtainium://add/https://github.com/LeanBitLab/HeliboardL">
        <img alt="Get it on Obtainium" src="docs/images/get-it-on-obtainium.png" height="60">
      </a>
    </td>
    <!-- ponytail: add fdroid button back -->
    <td align="center" valign="middle">
      <a href="https://f-droid.org/en/packages/com.leanbitlab.leantype/index.html">
        <img alt="Get it on F-Droid" src="docs/images/get-it-on-fdroid.png" height="90">
      </a>
    </td>
  </tr>
</table>

## Screenshots

<table>
  <tr>
    <td><img src="docs/images/1.png" height="500" alt="Screenshot 1"/></td>
    <td><img src="docs/images/2.png" height="500" alt="Screenshot 2"/></td>
    <td><img src="docs/images/3.png" height="500" alt="Screenshot 3"/></td>
    <td><img src="docs/images/4.png" height="500" alt="Screenshot 4"/></td>
    <td><img src="docs/images/5.png" height="500" alt="Screenshot 5"/></td>
    <td><img src="docs/images/6.png" height="500" alt="Screenshot 6"/></td>
  </tr>
</table>


### 📦 Choose Your Version

#### 1. Standard Version (`-standard-release.apk`)
*   **Features:** Full suite including **AI Proofreading**, **AI Translation**, **Handwriting Input**, and **Gesture Library Downloader**.
*   **Permissions:** Request `INTERNET` permission (used *only* when you explicitly use AI features, download plugins, or update libraries).
*   **Setup:** Use the built-in downloader for Gesture Typing and Handwriting Input. Configure AI keys in Settings.

#### 2. Offline Version (`-offline-release.apk`)
*   **Features:** All UI/UX enhancements and **Offline Neural Proofreading** (via `llama.cpp` using local **GGUF models**).
*   **Permissions:** **NO INTERNET PERMISSION**. Guaranteed at OS level.
*   **Best For:** Privacy purists.
*   **Manual Setup Required:**
    *   **Gesture Typing:** [Download library manually](https://github.com/erkserkserks/openboard/tree/46fdf2b550035ca69299ce312fa158e7ade36967/app/src/main/jniLibs) and load via *Settings > Gesture typing*.
    *   **Offline AI:** Download GGUF models and load via *Settings > Advanced > GGUF Model (.gguf)*. 👉 **[See Offline Setup Instructions](docs/FEATURES.md#5-offline-proofreading-privacy-focused)**

#### 3. Offline Lite Version (`-offlinelite-release.apk`)
*   **Features:** All UI/UX enhancements but **NO AI FEATURES**.
*   **Permissions:** **NO INTERNET PERMISSION**. Guaranteed at OS level.
*   **Best For:** Minimalists who want a modern keyboard without any AI components (~20MB size).
*   **Manual Setup Required:**
    *   **Gesture Typing:** [Download library manually](https://github.com/erkserkserks/openboard/tree/46fdf2b550035ca69299ce312fa158e7ade36967/app/src/main/jniLibs) and load via *Settings > Gesture typing*.

## Original HeliBoard Features

<ul>
  <li>Add dictionaries for suggestions and spell check</li>
  <li>Customize keyboard themes (style, colors and background image)</li>
  <li>Customize keyboard layouts</li>
  <li>Multilingual typing</li>
  <li>Glide typing (works out of the box with built-in pure-Java fallback engine, or use native library)</li>
  <li>Clipboard history</li>
  <li>One-handed mode</li>
  <li>Split keyboard</li>
  <li>Number pad</li>
  <li>Backup and restore settings</li>
</ul>

For original feature documentation, visit the [HeliBoard Wiki](https://github.com/Helium314/HeliBoard/wiki).

## Setup

### AI Features Setup

LeanType supports multiple AI providers: **Google Gemini**, **Groq**, and **OpenAI-compatible** (OpenRouter, HuggingFace, etc.).

👉 **[Read the Full AI Setup & Features Guide](docs/FEATURES.md)**

**Quick Start:**
1.  Get a free key from [Google AI Studio](https://aistudio.google.com/apikey) (Gemini) or [Groq Console](https://console.groq.com/keys) (Groq).
2.  Copy the API key.
3.  Go to **Settings → AI Integration → Set AI Provider**.
4.  Select your provider and paste the API Token.
5.  Select Model and target language

> [!IMPORTANT]
> **Privacy**: Your input data is sent to the configured provider.
> 👉 **[View Privacy Policies for Providers](docs/FEATURES.md#supported-ai-providers)**

## Contributing

For issues specific to LeanType features, please open an issue in this repository.

For issues with core HeliBoard functionality, please report to the [original HeliBoard repository](https://github.com/Helium314/HeliBoard/issues).

## Community Themes

Discover and share community-made themes: [🎨 Browse Themes](https://github.com/topics/leantype-theme)

> Theme creators: tag your repo with the `leantype-theme` topic to appear here.

## License

LeanType (as a fork of HeliBoard/OpenBoard) is licensed under **GNU General Public License v3.0**.

See [LICENSE](/LICENSE) file.

## Credits

### Original Projects
- **[HeliBoard](https://github.com/Helium314/HeliBoard)** by Helium314 - The excellent keyboard this fork is based on
- [OpenBoard](https://github.com/openboard-team/openboard)
- [AOSP Keyboard](https://android.googlesource.com/platform/packages/inputmethods/LatinIME/)
- All [HeliBoard Contributors](https://github.com/Helium314/HeliBoard/graphs/contributors)

### LeanType
- Built with ❤️ by [LeanBitLab](https://github.com/LeanBitLab)

## 🛡️ LeanBitLab Ecosystem

Check out our other projects:
👉 **[LeanBitLab Projects](https://github.com/LeanBitLab#-current-projects)**

---

## Support the Development

Building and maintaining privacy-focused, offline AI apps takes time and resources (test devices, server costs, etc.).

If you love LeanType, please consider supporting the project!

<a href="https://github.com/sponsors/LeanBitLab">
  <img src="https://img.shields.io/static/v1?label=Sponsor&message=%E2%9D%A4&logo=GitHub&color=%23fe8e86" width="150" alt="Sponsor on GitHub"/>
</a>

Your support keeps the code **100% Free and Open Source**.

---

*LeanType • Privacy-focused keyboard with AI enhancements*
