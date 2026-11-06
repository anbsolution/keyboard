# Voice Keyboard v1.2.0

Android input method for English, Hindi and Punjabi layouts, with optional voice input, local clipboard history and text-editing shortcuts.

## v1.2.0 updates
- English, Hindi and Punjabi keyboard layout selector.
- Voice language selector for Auto, Hindi, English and Punjabi.
- Hinglish voice typing is best-effort through the selected Android speech recognition provider; mixed-language accuracy is not guaranteed.
- Clipboard menu stores up to 20 recent entries locally. It reads the current clipboard only when the user taps the clipboard button; it does not monitor clipboard in the background.
- Quick actions: select all, delete previous word, paste, cursor left/right.
- Light, Dark and Blue themes retained.
- Existing application ID preserved: `com.voicetype.keyboard`.

## Privacy and compatibility
- Microphone is used only after permission and an explicit Voice-button tap.
- Recognition availability, language support and offline operation depend on the Android recognition service and device settings.
- Clipboard history is stored in app-private preferences on the device. It is not uploaded.
- Clipboard access is user-initiated; Android may display clipboard access indicators depending on OS version.

## Build
GitHub Actions runs unit tests before assembling the debug APK.
