# Trình giả lập nội bộ cũ (không hiển thị ở checkout)

Checkout hiện dùng cổng VNPAY Sandbox; xem VNPAY_SANDBOX.md. Các API và phiên tài khoản/thẻ giả lập cũ được giữ để tương thích.

Đây là mô phỏng nội bộ, tách khỏi VNPAY Sandbox thực và dữ liệu kinh doanh. Không thu tiền thật.

## Tự thử trên giao diện

1. Luồng này chỉ dành cho phiên giả lập nội bộ cũ đã có mã DEMO; /thanh-toan-thu hiện chuyển tới checkout VNPAY.
2. Với mã phiên DEMO cũ còn hạn, mở /thanh-toan-thu/{code}.
3. Bấm tiếp tục và nhập tài khoản mẫu **smashstep_test**, mật khẩu mẫu **Test@123456**.
4. Nhập OTP công khai **123456**, bấm Xác nhận thanh toán thử.
5. Vào giao diện quản trị, Quản lý hóa đơn, mục **Đơn thử đã thanh toán**. Danh sách tự làm mới sau 15 giây, hoặc bấm Làm mới.

Mật khẩu này chỉ là fixture mô phỏng công khai, không phải mật khẩu ngân hàng, tài khoản cửa hàng hay Merchant VNPAY. Tùy chọn thẻ NCB thử cũ vẫn được giữ.

## Lưu và bảo vệ kết quả

- Backend xác minh tài khoản mẫu, mật khẩu mẫu và OTP; chỉ customer sở hữu phiên được thao tác. Không nhận owner hoặc trạng thái thành công từ client.
- Phiên có hạn 5 phút, tối đa 5 lần sai gộp cho thông tin tài khoản/thẻ và OTP. OTP gắn với challenge của phiên. Phiên hủy, thất bại, hết hạn không được ghi thành công.
- Biên nhận chỉ ghi sau OTP đúng; chỉ phản hồi thành công sau khi ghi bền vững. Gửi lại cùng requestId không tạo đơn trùng, kể cả sau backend restart; đổi payload hoặc cổng thanh toán với requestId cũ bị từ chối.
- File lưu theo app.payment.demo.ledger-file hoặc PAYMENT_DEMO_LEDGER_FILE, mặc định thư mục smashstep-demo trong java.io.tmpdir. Local hiện cấu hình D:/DATN_SD13/payment-demo-private/ledger.json ngoài repository và thư mục public. Sao lưu file này nếu chuyển máy.
- File có version, giới hạn 1.000 biên nhận / 20 MB; không tự xóa lịch sử đã thanh toán. Nếu file hỏng, đầy hoặc không ghi được, API báo lỗi và không báo thanh toán thành công. Mỗi backend dùng riêng một file, không dùng chung giữa nhiều tiến trình.
- Không lưu tài khoản/mật khẩu/OTP hoặc số thẻ vào file. Chỉ lưu snapshot người nhận, sản phẩm, tổng tiền, mã tham chiếu, thời gian, owner và fingerprint request. API danh sách yêu cầu ADMIN/STAFF; khách chỉ đọc biên nhận của mình.
- Không tạo hóa đơn/biên nhận tiền thật, không trừ kho, không tiêu voucher, không cộng doanh thu, không lưu địa chỉ, không xóa giỏ hàng.
- Trang không còn tự báo thành công bằng sessionStorage. Phiên chưa trả tiền bị mất khi backend restart; đơn đã thanh toán vẫn đọc được từ file.

## Cấu hình

app.payment.demo.enabled mặc định false; bật PAYMENT_DEMO_ENABLED=true ở môi trường local cần mô phỏng. Không bật mô phỏng cho cửa hàng production. Không thay đổi schema SQL.
