# AI Meeting

AI Meeting là ứng dụng Android hỗ trợ hội thoại song ngữ theo thời gian thực. Ứng dụng có thể thu âm hoặc nhận văn bản nhập tay, tự xác định chiều dịch, đọc bản dịch và lưu lại nội dung cuộc họp ngay trên thiết bị.

Các tác vụ chính ưu tiên chạy cục bộ để bảo vệ dữ liệu và giảm phụ thuộc vào mạng. Khi cần, người dùng vẫn có thể cấu hình dịch vụ trực tuyến cho nhận dạng giọng nói, OCR, dịch thuật, tổng hợp giọng nói và trợ lý AI.

## Khả năng chính

- Chọn hai ngôn ngữ cho cuộc họp và tự xác định chiều dịch từ nội dung đầu vào.
- Nhận dạng giọng nói ngoại tuyến bằng Vosk hoặc qua nhà cung cấp STT trực tuyến.
- Dịch trên thiết bị bằng ML Kit; hỗ trợ MyMemory, LibreTranslate và các dịch vụ đã cấu hình.
- Trích xuất văn bản từ ảnh và PDF bằng ML Kit OCR hoặc dịch vụ OCR trực tuyến.
- Đọc văn bản bằng Sherpa-ONNX, Android/Google TTS hoặc nhà cung cấp TTS trực tuyến.
- Tải, nhập, kiểm tra và chọn model ưu tiên ngay trong ứng dụng.
- Trò chuyện với trợ lý dùng model GGUF cục bộ hoặc LLM trực tuyến; lịch sử được lưu trên thiết bị.
- Theo dõi thông tin thiết bị, runtime, model, dung lượng và nhật ký chẩn đoán.
- Chia sẻ transcript, bản dịch hoặc toàn bộ phiên họp.

## Ngôn ngữ

Giao diện hiện cho phép chọn tiếng Việt, tiếng Anh, tiếng Nhật, tiếng Hàn, tiếng Trung, tiếng Pháp, tiếng Đức, tiếng Tây Ban Nha và tiếng Thái. Khả năng xử lý thực tế phụ thuộc vào model và runtime được cài cho từng tác vụ.

## Yêu cầu

- Android Studio phiên bản mới.
- JDK 17.
- Android SDK 37.
- Thiết bị hoặc emulator chạy Android 9 (API 28) trở lên.
- Thiết bị `arm64-v8a` cho cấu hình build hiện tại.
- Kết nối Internet khi tải model lần đầu hoặc sử dụng dịch vụ trực tuyến.

Repository sử dụng Gradle Wrapper 9.5.1 và Android Gradle Plugin 9.2.1, vì vậy không cần cài Gradle riêng.

## Bắt đầu

1. Mở repository bằng Android Studio.
2. Chờ Gradle Sync hoàn tất.
3. Chạy module `app` trên thiết bị hoặc emulator tương thích.
4. Cấp quyền microphone khi ứng dụng yêu cầu.
5. Vào màn hình **Models** để tải hoặc chọn model cho từng tác vụ.

Build bản debug từ dòng lệnh:

```powershell
.\gradlew.bat assembleDebug
```

Trên macOS hoặc Linux:

```bash
./gradlew assembleDebug
```

APK được tạo trong `app/build/outputs/apk/debug/`.

## Cấu hình Android

Ứng dụng nằm trong module `app` với cấu hình chính:

```text
applicationId: com.khoalpa.aimeeting
minSdk: 28
targetSdk: 37
compileSdk: 37
abiFilters: arm64-v8a
```

`applicationId` là định danh dùng để phát hành trên Play Console. Không thay đổi giá trị này sau khi ứng dụng đã được xuất bản.

Có thể truyền phiên bản qua Gradle property:

```powershell
.\gradlew.bat bundleRelease `
  -PAI_MEETING_VERSION_CODE=2 `
  -PAI_MEETING_VERSION_NAME=1.1
```

Tương đương trên macOS hoặc Linux:

```bash
./gradlew bundleRelease \
  -PAI_MEETING_VERSION_CODE=2 \
  -PAI_MEETING_VERSION_NAME=1.1
```

## Model và dữ liệu cục bộ

Model được lưu trong vùng external files riêng của ứng dụng:

```text
Android/data/com.khoalpa.aimeeting/files/models/
├── stt/         # Model Vosk
├── tts/         # Model Sherpa-ONNX
└── assistant/   # Model GGUF cho trợ lý
```

Màn hình **Models** hỗ trợ tiếp tục bản tải xuống bị gián đoạn và nhập model tùy chỉnh. Model dịch của ML Kit được tải và quản lý riêng bằng `RemoteModelManager`.

## Runtime và dịch vụ hỗ trợ

| Tác vụ | Cục bộ | Trực tuyến hoặc dịch vụ hệ thống |
|---|---|---|
| Nhận dạng giọng nói | Vosk | OpenAI Transcribe, Groq Whisper, Hugging Face Whisper |
| OCR | ML Kit Text Recognition | OpenAI Vision-compatible OCR, Google Cloud Vision |
| Dịch thuật | ML Kit Translate | MyMemory, LibreTranslate, Google Translate khi được cấu hình |
| Tổng hợp giọng nói | Sherpa-ONNX Supertonic/VITS | Android/Google TTS, OpenAI TTS, StreamElements, Hugging Face SpeechT5 |
| Trợ lý AI | GGUF qua Llamatik/LlamaBridge | OpenAI Chat, Gemini, OpenRouter, Groq, Hugging Face |

API key và endpoint được nhập trong ứng dụng. Màn hình **Runtime** che các giá trị nhạy cảm khi hiển thị cấu hình.

## Kiểm thử

Chạy unit test:

```powershell
.\gradlew.bat testDebugUnitTest
```

Chạy Android Lint:

```powershell
.\gradlew.bat lintDebug
```

Kiểm tra đầy đủ trước khi phát hành:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug bundleRelease
```

CI chạy unit test, lint, build APK debug và AAB release cho mỗi pull request hoặc thay đổi trên nhánh `main`/`master`.

## Ký và phát hành

AAB release chỉ được ký khi có đủ bốn giá trị sau dưới dạng Gradle property hoặc biến môi trường:

```text
AI_MEETING_UPLOAD_STORE_FILE
AI_MEETING_UPLOAD_STORE_PASSWORD
AI_MEETING_UPLOAD_KEY_ALIAS
AI_MEETING_UPLOAD_KEY_PASSWORD
```

Không commit keystore, mật khẩu hoặc các tệp khóa như `.jks`, `.keystore`, `.p12`, `.pem` và `.key`.

Workflow phát hành chạy khi push tag `v*`. Trên GitHub Actions, keystore được cung cấp qua secret `AI_MEETING_UPLOAD_KEYSTORE_BASE64`; số phiên bản lấy từ tên tag và số lần chạy workflow.

Xem [checklist phát hành](docs/RELEASE_CHECKLIST.md) và [nội dung Play Store](docs/PLAY_STORE_LISTING.md) trước khi tạo bản chính thức.
