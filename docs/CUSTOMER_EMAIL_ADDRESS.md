# Email đơn hàng và địa chỉ nhận hàng

## Đăng ký tài khoản

- Không gửi OTP, không xác minh hộp thư và không gọi dịch vụ ngoài khi đăng ký. Giữ kiểm tra định dạng họ tên/email/số điện thoại/mật khẩu và chống trùng email, số điện thoại. Nút tạo tài khoản không phụ thuộc trạng thái xác thực email.
- Đã gỡ các API gửi/xác nhận OTP đăng ký và dịch vụ kiểm tra DNS/ZeroBounce. Không cần tạo tài khoản hoặc khóa API kiểm tra email.

## Tùy chọn email theo từng đơn

- Checkout có checkbox **Bạn muốn nhận email cập nhật trạng thái đơn hàng?**, mặc định bỏ chọn. Email nhận là email tài khoản lấy từ DB, không dùng email do request tự chỉ định.
- Khi chọn: gửi xác nhận sau khi đơn thực sự được lưu và gửi tiếp khi trạng thái thay đổi. VNPAY chỉ lưu lựa chọn và tạo thông báo sau xác nhận thanh toán; phiên chưa thanh toán không gửi mail đặt hàng.
- Khi không chọn hoặc request cũ không có receiveOrderEmails: không gửi email cho đơn này. Lựa chọn được giữ trong phiên checkout, biên nhận VNPAY và dấu kỹ thuật của từng đơn, không làm thay đổi dữ liệu hồ sơ hoặc đơn khác.
- Request checkout có Boolean receiveOrderEmails. Giá trị true được gắn vào dấu nhận diện để không thể đổi lựa chọn trong một phiên chưa xác định kết quả; các request cũ vẫn tương thích.
- Consent và các sự kiện nằm trong lịch sử hiện có với tiền tố [WEB-ORDER-MAIL:2]|. OPT_IN/OPT_OUT ghi lựa chọn. EVENT|QUEUED chuyển thành EVENT|SENT sau khi SMTP nhận thư. Không thay đổi schema/SQL. Mốc kỹ thuật phiên bản 1 và 2 được ẩn trong lịch sử giao diện. Những mốc phiên bản 1 không có lựa chọn rõ ràng không tự tiếp tục gửi thư.
- Trạng thái gửi tại GET /api/customer/shop/orders/{code}/email chỉ chủ đơn xem được: OPTED_OUT, NOT_REQUESTED, UNDELIVERABLE, QUEUED hoặc SENT. Mỗi sự kiện giữ trạng thái đã chốt lúc cập nhật, không lấy trạng thái mới hơn để thay thế nội dung email cũ.
- Email chạy sau commit, lỗi gửi không hủy đơn hay thay đổi trạng thái nghiệp vụ; sự kiện chưa gửi được giữ để thử lại sau restart. API cập nhật cùng trạng thái không tạo thêm sự kiện mail. SMTP không bảo đảm exactly-once khi process dừng sau nhận thư nhưng trước lưu SENT.
- Có thông báo cho cập nhật trạng thái quản trị, giao hàng thất bại/hàng trả/hoàn/hủy và khách hủy đơn. Đơn tại quầy không có lựa chọn nhận email sẽ không phát sinh mail.
- Các sự kiện email kỹ thuật không được tính vào số lần giao hàng thất bại.

## Tài khoản gửi và mẫu mail

- Dùng minhquang011007@gmail.com và Mật khẩu ứng dụng Gmail trong application-local.properties đã ignore hoặc MAIL_USERNAME/MAIL_PASSWORD. Không dùng khóa API bên thứ ba; không đưa thông tin đăng nhập vào Git.
- Mẫu navy/cyan lấy theo mẫu phiếu giảm giá của Hùng, có mã đơn, trạng thái, sản phẩm, giảm giá, phí giao hàng, tổng tiền và thông tin nhận hàng. Cập nhật trạng thái có tiêu đề và nội dung riêng.
- Link đơn hàng dùng localhost:5174. SENT chỉ có nghĩa SMTP nhận thư để chuyển tiếp, không bảo đảm Inbox; Gmail vẫn có thể tạo bounce nếu khách chọn nhận thư nhưng nhập email không nhận được. Không đọc hộp thư của chủ shop, không gửi thư thử hoặc tạo đơn thử trong lượt triển khai.
- Nếu thư hợp lệ vào Spam: người nhận chọn **Không phải thư rác** và có thể thêm địa chỉ gửi vào danh bạ. Không có cờ SMTP nào buộc Gmail chuyển thư vào Inbox/Chính; không giả mạo các header xác thực hoặc nhãn chống spam. Xem [Gmail Help](https://support.google.com/mail/answer/1366858?hl=vi).

## Đổi địa chỉ một lần

- Chỉ chủ đơn online còn **Chờ xác nhận** được đổi một lần. Mốc thay đổi được lưu cùng transaction dưới khóa hóa đơn. Các lượt thay đổi cũ cũng được tính; gửi lại qua API vẫn bị giới hạn.
- COD tính lại phí vận chuyển theo địa chỉ mới, giữ giá sản phẩm/voucher đã chốt.
- Đơn QR/VNPAY có số tiền đã chốt chỉ được đổi sang địa chỉ có cùng phí vận chuyển; khác phí phải liên hệ cửa hàng, không tự đổi số tiền đã thanh toán.

## Nhập tay và bản đồ

- Form checkout, sổ địa chỉ và đổi địa chỉ đơn thu gọn bản đồ mặc định, chỉ có nút **Chọn địa chỉ trên bản đồ**. Bấm nút mới khởi tạo/tải bản đồ Leaflet/OpenStreetMap và hiện ô tìm địa điểm. Có thể tìm bằng nút/Enter, chọn kết quả, bấm điểm hoặc kéo ghim rồi xác nhận **Dùng địa chỉ này**; xác nhận xong bản đồ tự thu gọn. Địa chỉ/vị trí đã chọn vẫn được giữ, có nút **Đổi vị trí trên bản đồ** để mở lại hoặc **Ẩn bản đồ** để đóng mà không chọn điểm mới.
- Có nút mở rộng/thu gọn, thông báo không có kết quả, tải lại khi lỗi tile; ResizeObserver cập nhật kích thước Leaflet khi form/modal thay đổi. Bản đồ và tra cứu bị khóa trong lúc lưu/thanh toán.
- Ở máy hiện tại, tile.openstreetmap.org không phân giải được DNS. Nền mặc định dùng OSM France/HOT, có attribution trực tiếp và giữ cache của trình duyệt; chỉ tải các ô đang xem. Có thể cấu hình VITE_MAP_TILE_URL và VITE_MAP_TILE_ATTRIBUTION cho nhà cung cấp khác. Dịch vụ cộng đồng phù hợp bản demo local nhỏ, không có SLA cho triển khai thương mại.
- GPS chỉ chạy sau thao tác và quyền của người dùng; hiển thị độ sai số thiết bị, không hứa độ chính xác tuyệt đối. Tên địa chỉ bản đồ cần kiểm tra lại và đối chiếu tỉnh/phường GHN trước khi đặt hàng.
- Tra địa chỉ qua Photon, không dùng public Nominatim. Chỉ tìm khi bấm/Enter/chọn điểm, giới hạn một request mỗi 1,1 giây và cache kết quả. Không gửi tên, số điện thoại hoặc email tới Photon.
- Tìm kiếm không phân biệt chữ hoa/thường: frontend và backend chuẩn hóa Unicode NFC, khoảng trắng, chữ thường trước khi gọi Photon; giữ nguyên chữ người dùng đang nhập và tên địa điểm trả về. Không bắt buộc viết hoa chữ đầu. Kiểm tra tối thiểu 3, tối đa 200 ký tự, chặn ký tự điều khiển và < >. Nút mở bản đồ có nền xanh đậm, biểu tượng và độ tương phản rõ; mặc định bản đồ vẫn ẩn.
- Tọa độ của đơn được lưu bằng mốc `[WEB-LOCATION:1]` trong bảng lịch sử hiện có, không thêm cột/bảng. Điểm ghim có thể xem trong chi tiết đơn. Sổ địa chỉ lưu địa chỉ chữ; điểm ghim của đơn được giữ riêng.
- Nguồn: [Leaflet](https://leafletjs.com/reference.html), [Photon API](https://github.com/komoot/photon/blob/master/docs/api-v1.md), [giới hạn Photon](https://github.com/komoot/photon#demo-server), [OpenStreetMap tiles](https://operations.osmfoundation.org/policies/tiles/).
