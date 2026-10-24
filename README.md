# AI Meeting

AI Meeting la ung dung Android ho tro hop song ngu: nghe hoac nhap transcript, nhan dien ngon ngu, dich hai chieu, doc ban dich va luu lai dong hoi thoai tren thiet bi. Ung dung uu tien runtime offline, dong thoi co the cau hinh cac provider online cho STT, OCR, dich, TTS va tro ly AI.

## Tinh nang chinh

- Chon cap ngon ngu hop va tu dong xac dinh chieu dich tu transcript.
- Nghe truc tiep bang Vosk offline hoac provider STT online da cau hinh.
- Dich bang ML Kit on-device, MyMemory, LibreTranslate hoac provider online.
- Nhan dang chu tu anh/PDF bang ML Kit OCR hoac provider OCR online.
- Phat am nguon/ban dich bang Sherpa-ONNX TTS, Android/Google TTS hoac provider TTS online.
- Quan ly model ngay trong app: tai model tot nhat, tai tung model, kiem tra trang thai, chon runtime uu tien.
- Assistant rieng voi model GGUF local hoac provider LLM online, co ghi nho hoi thoai tren may.
- Runtime screen de xem thong tin thiet bi, model dang dung, dung luong model va log chan doan.
- Chia se transcript, ban dich hoac toan bo session.

## Ngon ngu

App hien co lua chon: Tieng Viet, English, Japanese, Korean, Chinese, Francais, Deutsch, Espanol va Thai. Muc ho tro thuc te phu thuoc vao model/runtimes da cai cho tung tac vu.

## Yeu cau phat trien

- Android Studio ban moi.
- JDK 17.
- Android SDK 37.
- Gradle wrapper trong repo: Gradle 9.5.1.
- Android Gradle Plugin 9.2.1.
- Thiet bi/emulator Android API 28 tro len.
- Thiet bi `arm64-v8a` cho ban build hien tai.
- Ket noi mang trong lan dau tai model hoac khi dung provider online.

## Cau hinh build

Module chinh la `app`.

```text
applicationId: com.khoalpa.aimeeting
minSdk: 28
targetSdk: 37
compileSdk: 37
abiFilters: arm64-v8a
```

Quan trong: `applicationId` hien la ID chinh thuc dung de upload Play Console. Khong doi gia tri nay sau khi publish, vi Play coi day la dinh danh vinh vien cua app.

Version co the truyen qua Gradle property:

```bash
./gradlew bundleRelease -PAI_MEETING_VERSION_CODE=2 -PAI_MEETING_VERSION_NAME=1.1
```

Tren Windows PowerShell:

```powershell
.\gradlew.bat bundleRelease -PAI_MEETING_VERSION_CODE=2 -PAI_MEETING_VERSION_NAME=1.1
```

## Ky release

Release AAB chi duoc ky khi co day du upload keystore:

```text
AI_MEETING_UPLOAD_STORE_FILE
AI_MEETING_UPLOAD_STORE_PASSWORD
AI_MEETING_UPLOAD_KEY_ALIAS
AI_MEETING_UPLOAD_KEY_PASSWORD
```

Co the truyen bang Gradle property hoac bien moi truong. Khong commit keystore, password, file `.jks`, `.keystore`, `.p12`, `.pem` hoac `.key`.

## Chay du an

1. Mo thu muc repo bang Android Studio.
2. Cho Gradle Sync hoan tat.
3. Chay module `app` tren thiet bi hoac emulator API 28+.
4. Cap quyen microphone khi app yeu cau.
5. Mo man hinh `Models` de tai hoac chon model can dung.

Co the build tu terminal:

```bash
./gradlew assembleDebug
```

Tren Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

## Model va du lieu runtime

Model local duoc luu trong external files cua app:

```text
Android/data/com.khoalpa.aimeeting/files/models/
```

Nhom model chinh:

```text
models/stt/         Vosk speech-to-text
models/tts/         Sherpa-ONNX TTS
models/assistant/   GGUF assistant models
```

Man hinh `Models` ho tro tai tiep cac file dang tai do. Voi ML Kit Translate, app dung `RemoteModelManager` de tai va luu model dich tren thiet bi.

## Runtime ho tro

- STT local: Vosk.
- STT online: OpenAI Transcribe, Groq Whisper, Hugging Face Whisper.
- OCR local: ML Kit Text Recognition.
- OCR online: OpenAI vision-compatible OCR, Google Cloud Vision.
- Dich local: ML Kit Translate.
- Dich online/public: MyMemory, LibreTranslate, Google Translate service neu cau hinh.
- TTS local: Sherpa-ONNX Supertonic/VITS.
- TTS online/service: Android/Google TTS, OpenAI TTS, StreamElements, Hugging Face SpeechT5.
- Assistant local: GGUF qua Llamatik/LlamaBridge.
- Assistant online: OpenAI Chat, Gemini, OpenRouter, Groq, Hugging Face.

API key va endpoint provider duoc cau hinh trong app; Runtime screen mask cac gia tri nhay cam.

## Kiem thu

Chay unit test:

```bash
./gradlew testDebugUnitTest
```

Chay lint:

```bash
./gradlew lintDebug
```

Kiem tra truoc release:

```bash
./gradlew testDebugUnitTest lintDebug bundleRelease
```

Checklist release chi tiet nam o `docs/RELEASE_CHECKLIST.md`.
