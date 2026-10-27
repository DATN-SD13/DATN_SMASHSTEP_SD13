# VNPAY Sandbox trên localhost

Checkout chỉ hiển thị một lựa chọn **Thanh toán bằng VNPAY** cho cổng thử.

## Tự thanh toán

1. Đăng nhập khách hàng trên http://localhost:5174, chọn sản phẩm và địa chỉ nhận hàng.
2. Chọn Thanh toán bằng VNPAY; website chuyển tới cổng VNPAY Sandbox chính thức.
3. Thẻ NCB thử công khai: 9704198526191432198, chủ thẻ NGUYEN VAN A, phát hành 07/15, OTP 123456. Chỉ nhập trên cổng Sandbox.
4. Cổng trả về http://localhost:5174/thanh-toan-vnpay/ket-qua cùng chữ ký kết quả.
5. Backend xác minh chữ ký, mã phiên, merchant và số tiền; tự truy vấn QueryDr để lấy trạng thái trực tiếp từ VNPAY nếu chưa có IPN. Chỉ ghi thành công khi chữ ký phản hồi QueryDr hợp lệ, đúng mã đơn/số tiền, loại giao dịch thanh toán 01 và TransactionStatus=00. ResponseCode=00 riêng lẻ không phải thành công thanh toán.
6. Chỉ sau khi VNPAY xác nhận, VnpayOrderService tạo hóa đơn chính trong DB: hoa_don, hoa_don_chi_tiet, lich_su_thanh_toan và lich_su_hoa_don. Hiện trong danh sách Quản lý hóa đơn trên http://localhost:5173/hoa-don và Đơn hàng của đúng khách. Giữ nguyên giá/phí đã thanh toán, trừ tồn khả dụng và ghi lượt voucher trong cùng transaction. Đơn bắt đầu ở trạng thái Chờ xác nhận; không tự chuyển sang hoàn thành/giao hàng. Không có khung đơn thử riêng.

Trang Return tự kiểm tra lại tối đa 90 giây, sau đó hiển thị rõ chưa xác nhận được và cho kiểm tra lại cùng giao dịch; không quay vô hạn. Backend giới hạn QueryDr một lần mỗi 60 giây cho một phiên. IPN hợp lệ vẫn xác nhận ngay. Return GET chỉ đọc; POST đối soát mới gọi API từ server. Tham số trình duyệt không trực tiếp gán trạng thái đã thanh toán.

## Cấu hình

- app.payment.vnpay.enabled mặc định false; thông tin merchant qua VNPAY_TMN_CODE, VNPAY_HASH_SECRET. Secret không đưa vào Git/frontend/log.
- Payment URL được cố định https://sandbox.vnpayment.vn/paymentv2/vpcpay.html; QueryDr cố định https://sandbox.vnpayment.vn/merchant_webapi/api/transaction, không theo redirect.
- VNPAY_RETURN_URL nhận HTTPS; riêng local cho phép đúng http://localhost:5174 hoặc http://127.0.0.1:5174 với đường dẫn /thanh-toan-vnpay/ket-qua khi VNPAY_ALLOW_LOCAL_RETURN=true. Mặc định không mở HTTP Return URL.
- VNPAY_SANDBOX_QUERY_ENABLED mặc định true, chỉ có hiệu lực khi merchant Sandbox đã bật và hợp lệ. VNPAY_QUERY_IP là địa chỉ IP server thực hiện truy vấn.
- Local hiện đọc cấu hình từ D:/DATN_SD13/vnpay-sandbox-local.properties ngoài repository, launcher D:/DATN_SD13/vnpay-public-link-20261022/start-backend-vnpay.ps1. Không chạy SQL init hoặc sửa schema.
- Local chỉ dùng localhost/loopback; Cloudflare tunnel đã dừng. QueryDr xác nhận bằng kết nối đi tới VNPAY, không cần mở ứng dụng local ra Internet. Nhập thẻ diễn ra trên cổng VNPAY Sandbox chính thức.
- Journal biên nhận đã xác nhận ở app.payment.demo.ledger-file ngoài Git/uploads; không lưu thẻ, mật khẩu hay OTP. Bản mới lưu cả request gốc để truy vết voucher, ghi chú và tùy chọn địa chỉ. Phiên chưa thanh toán còn trong bộ nhớ và hết hạn sau 5 phút. Hóa đơn đã ghi nằm trong DB, không phụ thuộc vòng đời của phiên.

## API

- CUSTOMER: GET /api/customer/shop/vnpay-sandbox/config, POST /quote, POST tạo phiên, GET /{code}, POST /{code}/reconcile. Owner lấy từ JWT hợp lệ, không nhận từ body.
- Public: GET /api/payments/vnpay/ipn; GET /return-check chỉ đọc; POST /reconcile-return nhận map tên tham số VNPAY → mảng chuỗi để giữ và từ chối tham số trùng. Proof sai/phiên lạ không gọi QueryDr và không ghi đơn.
- API quản trị: GET /api/hoa-don/paid-demo-orders, chỉ ADMIN/STAFF. confirmationMethod phân biệt IPN, QUERYDR và OTP của trình giả lập cũ.

Nguồn chính thức: https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html và https://sandbox.vnpayment.vn/apis/docs/truy-van-hoan-tien/querydr%26refund.html

## Chứng chỉ HTTPS trên Windows

Nếu kho CA của JDK không xác minh được chuỗi chứng chỉ Sandbox (PKIX path building failed), riêng client QueryDr thử lại bằng Windows-ROOT của hệ điều hành. Vẫn kiểm tra chứng chỉ hợp lệ và hostname, không dùng trust-all hoặc tắt TLS. Không đổi cấu hình TLS của các module khác.

## Ghi hóa đơn chính và khôi phục

- VnpayOrderService chỉ nhận PAID_SANDBOX được xác nhận qua IPN/QUERYDR; không chuyển kết quả OTP giả lập nội bộ thành hóa đơn. Khách hàng, sản phẩm, số lượng và tiền lấy từ journal server, không nhận từ callback client.
- Khóa tạo hóa đơn chung với POS, khóa biến thể và kiểm tra tồn khả dụng. Mã SB và fingerprint được gắn trong lịch sử kỹ thuật, mã giao dịch VNPAY được gắn trong lịch sử thanh toán. Gửi callback/Return lặp không tạo hóa đơn, trừ kho hoặc dùng voucher lần hai.
- Hóa đơn giữ nguyên subtotal/discount/shipping/amount và giá dòng đã được VNPAY xác nhận. Không gọi GHN hoặc tính lại campaign sau khi khách đã thanh toán. Voucher được khóa và kiểm tra với thời điểm báo giá gốc; lượt dùng vẫn kiểm tra khi ghi hóa đơn.
- Ghi DB trong một transaction; lỗi thiếu sản phẩm/tồn kho/voucher sẽ rollback toàn bộ thay đổi. Biên nhận xác nhận vẫn giữ trong journal; trang kết quả báo đã thanh toán nhưng đơn cần xử lý, không báo đặt hàng thành công khi chưa có orderCode. Kiểm tra lại cùng giao dịch sẽ thử ghi đơn an toàn.
- VnpayOrderRecovery tự nối các biên nhận đã xác nhận chưa ghi DB sau restart. Giao dịch cũ thiếu request chỉ được chuyển tự động nếu không có giảm voucher; không đoán voucher đã dùng.
- Chỉ sau khi có orderCode và API đơn hàng xác nhận đúng khách, frontend xóa sản phẩm đã mua khỏi giỏ của tài khoản đó đúng một lần. Các sản phẩm khác được giữ.
- Ghi rõ VNPAY Sandbox trong lịch sử thanh toán. Đây là hóa đơn nghiệp vụ thật trong database local; Sandbox không thu tiền ngân hàng thật.
- Không thay đổi schema hoặc chạy SQL init. Phương thức VNPAY được dùng/tạo bằng dữ liệu cấu hình hiện có; backend và cả hai frontend chỉ bind 127.0.0.1.
