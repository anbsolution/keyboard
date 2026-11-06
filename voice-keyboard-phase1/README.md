# Voice Keyboard

Android keyboard project: standard typing first, with an offline voice-recognition engine planned for Phase 2.

## Phase 1
- Android Input Method Service (IME)
- English QWERTY keyboard
- Basic Hindi character mapping (not phonetic transliteration)
- Shift, backspace, space, enter, number/symbol toggle
- Keyboard enable/select screen
- GitHub Actions unit-test and APK workflow

## Offline voice recognition status
The microphone button is a Phase 2 placeholder. Phase 1 does not claim offline speech recognition. A genuine offline Hindi/English engine requires packaged on-device acoustic/language models and device testing. The app currently requests neither microphone nor internet permission.

## Build
GitHub Actions runs unit tests and creates a debug APK. The APK is available in the workflow run's Artifacts section.
