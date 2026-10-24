# AI Themes 1.0.0

`themes/` là bản đóng gói hệ thống giao diện dùng chung cho AI Meeting và các dự án trong `D:/project`. Nguồn chuẩn được duy trì tại `ai.atelier/themes`; mỗi dự án nhận một bản sao đã ghim phiên bản để có thể chạy độc lập, không cần truy cập repository bên cạnh.

## Cấu trúc cấu hình

`project.json` xác định bốn thuộc tính:

| Thuộc tính | Giá trị hỗ trợ | Ý nghĩa |
|---|---|---|
| `theme` | `neutral`, `meeting`, `paper` | Bảng màu |
| `mode` | `system`, `light`, `dark` | Chế độ sáng/tối |
| `style` | `minimal`, `soft`, `editorial` | Phong cách trình bày |
| `density` | `comfortable`, `compact` | Mật độ bố cục |

Token nguồn nằm trong `tokens.json`. Component web chỉ nên dùng biến semantic có tiền tố `--ui-*`. Màu của ảnh, video, phụ đề và các trạng thái mang ý nghĩa nghiệp vụ không thuộc phạm vi thay đổi của theme.

## Hành vi theo nền tảng

- Web hiển thị nút **Giao diện** ở góc dưới bên phải và lưu lựa chọn riêng cho từng ứng dụng trong trình duyệt.
- Chế độ `system` tự cập nhật khi thiết lập sáng/tối của hệ điều hành thay đổi.
- Streamlit hiển thị nhóm **Giao diện** trong thanh bên và lưu lựa chọn theo phiên.
- Windows và Gradio dùng cấu hình mặc định hoặc đọc `AI_THEME`, `AI_THEME_MODE`, `AI_THEME_STYLE`, `AI_THEME_DENSITY` khi khởi động.
- Android dùng bảng màu Neutral sáng/tối theo hệ thống.
- Telegram tiếp tục dùng bộ chọn chế độ hiện có.

## Tích hợp vào ứng dụng web

Sao chép `dist/theme.css` và `dist/theme.js` vào thư mục tài nguyên tĩnh của ứng dụng. Nạp JavaScript trong `<head>` trước khi dựng nội dung để khôi phục lựa chọn đã lưu; nạp stylesheet sau CSS riêng của ứng dụng.

```html
<html
  data-theme="neutral"
  data-mode="system"
  data-style="minimal"
  data-density="comfortable"
  data-theme-app="my-project"
>
  <head>
    <script src="/ai-theme.js"></script>
    <link rel="stylesheet" href="/ai-theme.css">
  </head>
  <body class="ui-scope">
    <button class="ui-button ui-primary">Lưu</button>
  </body>
</html>
```

Các class component có sẵn gồm `.ui-input`, `.ui-select`, `.ui-textarea`, `.ui-card`, `.ui-table`, `.ui-feedback`, `.ui-tabs` và `.ui-actions`.

Ứng dụng vẫn chịu trách nhiệm xử lý bàn phím và trạng thái ARIA cho tab. Bộ chọn giao diện dùng dialog gốc của trình duyệt. Font mặc định là font hệ thống có hỗ trợ tiếng Việt; phong cách Editorial dùng serif cho tiêu đề. Khoảng cách, bo góc, mật độ và màu sắc được cấu hình độc lập.

## Build, triển khai và xác minh

Chạy các lệnh sau từ repository AI Atelier:

```powershell
.venv-ai/Scripts/python.exe themes/tools/build.py
.venv-ai/Scripts/python.exe themes/tools/deploy.py
.venv-ai/Scripts/python.exe themes/tools/verify.py
node themes/tools/theme.test.js
```

- `build.py` sinh CSS từ token nguồn.
- `deploy.py` đồng bộ bản ghim và tài nguyên runtime, đồng thời cập nhật các điểm nạp đã biết.
- `verify.py` kiểm tra kết quả triển khai.
- `theme.test.js` chạy bộ kiểm thử JavaScript của theme.

Trước khi ghi đè tệp nguồn, quy trình triển khai tạo bản sao trong `.theme-backups/` của từng dự án. Manifest trong `themes/deployment/` lưu đường dẫn và checksum; chạy lại sẽ bỏ qua những tệp có nội dung không đổi.

Hoàn tác một lần triển khai bằng manifest tương ứng:

```powershell
.venv-ai/Scripts/python.exe themes/tools/deploy.py --rollback themes/deployment/TIMESTAMP.json
```

Quá trình hoàn tác sẽ dừng nếu tệp đã được chỉnh sửa sau lần triển khai đó, nhờ vậy không ghi đè thay đổi mới của người dùng. Bản đóng gói cũ hoặc tiến trình đang chạy phải được build lại hay khởi động lại để nhận theme mới.

Lần triển khai đầu tiên dùng manifest tổng hợp `deployment/initial-rollout.json`. Kết quả xác minh nằm trong `verification/results.json`; ảnh trang mẫu nằm tại `verification/preview.png`.

## Phạm vi tích hợp

| Dự án | Cách tích hợp |
|---|---|
| `ai.atelier` | Web, ánh xạ token và palette cũ |
| `ai.studio` | Web, ánh xạ biến và danh sách tài nguyên máy chủ |
| `watermarks-remover` | Web, ánh xạ biến và tuyến tài nguyên |
| `ai.slide`, `ai.story` | Studio và cửa sổ Audio/Video Streamlit |
| `ai.player` | QSS; giữ bố cục và thay bảng màu cửa sổ chính |
| `ai.meeting`, `ai.player.android` | Resource màu; giữ ID cũ và `values-night` |
| `ai.telegram.android` | Material 3 `ColorScheme`; giữ bộ chọn sáng/tối |
| `VieNeu-TTS` | Gradio CPU/CUDA, XPU và giao diện streaming FastAPI |
| `sd.webui` | Extension `ai-shared-themes`; không sửa mã upstream |

CSS component đầy đủ chỉ dành cho web. Adapter native ánh xạ token theo khả năng của từng nền tảng; Android hiện chưa có bộ chọn ba phong cách và hai mức mật độ. QSS giữ nguyên các quy tắc bố cục đặc thù. Khi thêm màn hình hoặc component mới, hãy dùng semantic token để không phải mở rộng lớp tương thích màu cũ.
