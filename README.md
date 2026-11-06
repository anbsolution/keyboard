# Voice Keyboard v1.1.0

Android English/Hindi keyboard with speech-to-text input using the device's Android speech recognition service.

## What's new
- Hindi and English voice typing using Android SpeechRecognizer.
- Microphone permission requested from the app screen before voice use.
- Voice button starts recognition; recognized text is inserted into the focused text field.
- Light, Dark, and Blue keyboard themes.
- Settings screen for theme and voice-language preference.
- Existing application ID retained: `com.voicetype.keyboard`.

## Privacy and availability
- Microphone access is used only after the user grants Android's `RECORD_AUDIO` permission and taps the voice button.
- Speech recognition is provided by the Android device's installed recognition service. Depending on device settings and provider, audio may be processed on-device or by that provider; this app does not promise offline recognition.
- No app-owned server or analytics endpoint is used.
- Recognition availability and supported languages depend on the device/provider.

## Build
GitHub Actions workflow runs unit tests and assembles a debug APK. Tests are included but have intentionally not been run as part of this prepared source handoff.
