# Release Checklist

Use this checklist before every GitHub or Play Store release.

## 1. Identity

- Confirm the Play package name is `com.khoalpa.aimeeting` before the first Play Console upload.
- Keep the same `applicationId` forever after the first public/internal Play release.
- Confirm app name, icon, screenshots, privacy policy URL and support email in Play Console.

## 2. Version

- Set `AI_MEETING_VERSION_CODE` to a value higher than the last Play upload.
- Set `AI_MEETING_VERSION_NAME` to the public release version.
- Suggested local command:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug bundleRelease -PAI_MEETING_VERSION_CODE=2 -PAI_MEETING_VERSION_NAME=1.1
```

## 3. Signing

- Use an upload keystore enrolled with Play App Signing.
- Provide these values as Gradle properties or environment variables:
  - `AI_MEETING_UPLOAD_STORE_FILE`
  - `AI_MEETING_UPLOAD_STORE_PASSWORD`
  - `AI_MEETING_UPLOAD_KEY_ALIAS`
  - `AI_MEETING_UPLOAD_KEY_PASSWORD`
- Do not commit keystores or passwords.
- Verify the AAB is signed:

```powershell
jarsigner -verify -verbose -certs app\build\outputs\bundle\release\app-release.aab
```

## 4. Automated Checks

- Unit tests pass: `testDebugUnitTest`.
- Lint passes with no errors: `lintDebug`.
- Release bundle builds: `bundleRelease`.
- Native debug symbols are generated: `app/build/outputs/native-debug-symbols/release/native-debug-symbols.zip`.

## 5. Manual Smoke Test

Run the release build on an arm64 device and check:

- Microphone permission and live listening.
- Model download, pause/resume and delete.
- Offline STT and translation for the primary language pair.
- PDF/image OCR.
- Assistant model load and response.
- TTS playback and stop playback.
- Session export/reset.
- Runtime screen masks API keys and encrypted values.

## 6. Play Store Upload

- Upload `app/build/outputs/bundle/release/app-release.aab`.
- Upload `app/build/outputs/native-debug-symbols/release/native-debug-symbols.zip` if Play Console requests native symbols.
- Complete Data safety, Privacy policy, App content, Target audience and Content rating.
- Use managed publishing for the first production rollout.

## 7. GitHub Release

- Configure repository secrets used by `.github/workflows/android-release.yml`:
  - `AI_MEETING_UPLOAD_KEYSTORE_BASE64`
  - `AI_MEETING_UPLOAD_STORE_PASSWORD`
  - `AI_MEETING_UPLOAD_KEY_ALIAS`
  - `AI_MEETING_UPLOAD_KEY_PASSWORD`
- Create and push a tag such as `v1.1`.
- The release workflow builds the signed AAB, verifies the signature and attaches the AAB plus native symbols to the GitHub release.
