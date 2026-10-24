# Play Store Listing Draft

## Short Description

Offline-first meeting translator with live STT, OCR, TTS and an AI assistant.

## Full Description

AI Meeting helps bilingual meetings run on-device first. It can listen to speech, accept typed transcripts, recognize text from images or PDFs, translate between language pairs, speak the result aloud and export the conversation session.

The app is designed for privacy-conscious workflows: local runtimes are preferred when models are installed, and online providers are optional. Provider API keys are entered by the user inside the app and stored encrypted on the device.

Key capabilities:

- Live speech-to-text with local Vosk models.
- On-device translation with ML Kit where supported.
- OCR from images and PDFs with ML Kit.
- Local TTS with Sherpa-ONNX models.
- Optional online providers for STT, OCR, translation, TTS and chat.
- Local assistant with GGUF models.
- Runtime diagnostics and model management.
- Session export for meeting notes.

## Privacy Notes

- The app requests microphone access for live speech recognition.
- The app requests network access for model downloads and optional online providers.
- Local meeting data and provider credentials stay on the device unless the user chooses an online provider or shares/exports content.
- Android backups are disabled for app files, databases, shared preferences and external app data.

## Release Assets Needed

- App icon.
- Phone screenshots for core flows: Meeting, Models, Settings, Assistant, Runtime.
- Privacy policy URL.
- Support email.
- Final package name: `com.khoalpa.aimeeting`.
