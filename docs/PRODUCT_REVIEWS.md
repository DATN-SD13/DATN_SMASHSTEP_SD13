# Đánh giá sản phẩm

## Cài đặt

Chạy `Database/09_danh_gia_san_pham.sql` trên database SmashStep hiện có trước khi khởi động backend mới. Migration chỉ thêm bảng và ràng buộc, chạy lại an toàn. `Database/FULL_DB.sql` cũng đã gồm bảng này cho cài đặt mới. Không chạy lại FULL_DB trên dữ liệu đang sử dụng chỉ để thêm chức năng này.

## Quy tắc

- Khách đăng nhập, tài khoản còn hoạt động, đã mua sản phẩm trong đơn có trạng thái 4 (đã giao) hoặc 5 (hoàn thành) được đánh giá.
- Mỗi tài khoản có một đánh giá cho mỗi sản phẩm, gộp chung các màu/size. Gửi lại sẽ sửa đánh giá cũ, không tăng số lượt.
- Chọn số sao nguyên từ 1 đến 5 và nhập bình luận không rỗng, tối đa 2.000 ký tự. Cả API và giao diện đều kiểm tra.
- API lấy danh tính từ JWT, không nhận ID khách hàng từ trình duyệt. Tên công khai được che một phần, không trả email/số điện thoại.
- Điểm trung bình = tổng sao / tổng số đánh giá, hiển thị một chữ số thập phân. Không có đánh giá thì hiện trạng thái trống, không mặc định 5 sao.
- Danh sách phân trang 10 đánh giá, mới nhất trước; lọc 1–5 sao. Bộ lọc không thay đổi điểm trung bình chung.
- Bình luận được hiển thị bằng nội suy văn bản Vue, không render HTML do khách nhập.

## API

- `GET /api/store/products/{id}/reviews?page=0&stars=5`: công khai, bỏ `stars` để xem tất cả.
- `GET /api/customer/products/{id}/review`: quyền đánh giá và đánh giá của tài khoản hiện tại.
- `PUT /api/customer/products/{id}/review`: `{ "stars": 5, "comment": "Giày vừa chân, đi êm." }`.
- API danh mục và chi tiết sản phẩm bổ sung `averageRating`, `reviewCount`. Danh sách lấy thống kê gộp theo trang, không truy vấn từng thẻ sản phẩm.

Đánh giá được lưu trong SQL Server, không mất khi tải lại trang hoặc khởi động backend. Không tạo đánh giá/dữ liệu mẫu tự động.

## Ảnh trong bình luận

- Mở **Đơn hàng của bạn**, chọn đánh giá ở sản phẩm đã giao/hoàn thành. Có thể kèm tối đa 5 ảnh JPG/PNG, mỗi ảnh tối đa 5 MB; xem trước, bỏ ảnh hoặc giữ ảnh khi sửa đánh giá.
- Bấm ảnh trong đánh giá để mở lớn, dùng nút hoặc phím mũi tên chuyển ảnh và Escape để đóng.
- Upload yêu cầu `productId` và tài khoản có quyền đánh giá sản phẩm đó. Backend kiểm tra nội dung ảnh, tối đa 20 megapixel/8.000 px mỗi chiều và mã hóa lại ảnh trước khi lưu, không chỉ dựa vào tên tệp.
- API lưu đánh giá chỉ nhận ảnh tồn tại, thuộc tài khoản hiện tại hoặc ảnh đã gắn trong chính đánh giá đó; không nhận URL ảnh bên ngoài. Bỏ trường `images` khi gọi API cũ sẽ giữ ảnh hiện có; gửi mảng rỗng để bỏ ảnh khỏi đánh giá.
- File ảnh lưu ngoài Git tại `BACKEND/uploads/reviews`, đường dẫn lưu trong cột `hinh_anh`. Migration 09 cũng cập nhật bảng đã tạo trước đây. Sao lưu DB cần kèm thư mục ảnh này nếu chuyển máy.
