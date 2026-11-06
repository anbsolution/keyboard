# Voice Keyboard

Android IME keyboard project.

## Version 1.0.1 (Phase 1 fix)
- Android Input Method Service (IME)
- English QWERTY input
- Hindi Devanagari key labels and direct Unicode input
- Correct symbol-mode output (symbols are never passed through Hindi mapping)
- Shift, backspace, space, enter, number/symbol toggle
- Keyboard enable/select screen
- GitHub Actions unit tests and debug APK artifact

## Voice recognition status
The microphone icon is a clearly marked Phase 2 placeholder. This release does not perform speech recognition. A genuine offline Hindi/English engine requires bundled on-device speech models and testing on supported devices. No microphone or internet permission is requested in this release.

## Build
GitHub Actions runs unit tests and assembles a debug APK. Download the APK from the successful workflow run's Artifacts section.
