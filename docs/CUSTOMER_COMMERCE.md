# Customer storefront

## Routes

Customer application remains in `CUSTOMER_FRONTEND`, port 5174. Admin remains on 5173.

- `/`, `/san-pham`, `/san-pham/:id`: public catalogue and live campaign pricing.
- `/gio-hang`: guest/customer cart. Persistent cart contains only variant IDs and quantities.
- `/thanh-toan`: authenticated customer checkout.
- `/don-hang`, `/don-hang/:ma`: authenticated customer's own invoices.
- `/goi-y-giay`: six steps mapping directly to actual catalogue filters.
- `/tai-khoan`: existing profile, address book and password flow.

## Shipping configuration

GHN fee lookup is server-side. Set these environment variables on the process launching BACKEND:

| Variable | Value |
| --- | --- |
| `GHN_TOKEN` | Token issued to the store by GHN. Keep outside Git. |
| `GHN_SHOP_ID` | Shop belonging to that token; its registered pickup address is used. |
| `GHN_GRAMS_PER_PAIR` | Actual packed weight per pair, in grams. Uses the same setting as POS. |
| `GHN_BASE_URL` | Carrier environment URL, shared with POS. Default is the production GHN gateway. |

Restart the backend after configuring its environment. No database migration is required.
Customer checkout now reuses the team's `GhnPhiVanChuyenService` two-level province/ward catalogue
and shipping preview. POS keeps its employee-scoped quote cache; customers do not call POS controllers
or establish a POS identity. Prices are calculated again on order submission.

Choose the province and ward from GHN's current two-level selectors; saved legacy addresses must
be reselected. Packages above the carrier's 50kg limit must be split. Fee lookup is a preview:
it does not create a shipment, buy a label or arrange pickup. There is no invented free-shipping fallback.
Recipient name, phone and delivery address are sent to GHN for the preview; the form states this.
Carrier tokens stay on the backend. Configure GHN_TOKEN outside Git; no token is hardcoded in source.

## Order lifecycle

`/api/customer/shop/**` requires CUSTOMER role, KHACH_HANG JWT identity and an active customer.
The server takes customer ID from the authenticated subject. It never trusts a request's customer ID.
Existing `/api/ban-hang/**` restrictions remain unchanged.

Checkout recalculates current prices, stock, voucher and shipping in the backend. Only the highest
valid campaign percentage applies to each variant. Product badges have a separate maximum across
eligible variants; product representative price and its crossed-out original remain paired.

The existing invoice creation lock serializes idempotency checks and invoice insertion. Variant
rows are locked, legacy POS reservations respected, and stock deducted once. Client retries reuse
the same request ID and payload, including after reload. Request fingerprints live in existing
invoice history, with no new table/column. No payment is recorded when placing an unpaid COD order.

Only the customer's own new web orders at status 0 can have shipping edited or be canceled.
Shipping changes recalculate shipping only; sold unit prices and voucher amounts remain historical.
Cancellation restores stock and consumed voucher usage once. Admin cancellation uses the same
web-order inventory helper. Admin completion of a web COD invoice records its collected COD amount;
staff must only mark complete after actual collection. Existing legacy/POS invoices do not enter
this web-order-specific collection path.

All active payment methods are read from existing data. Only active COD supports this online order
flow; bank/card methods are shown as unavailable until a genuine payment gateway is integrated.
No simulated payment success or transfer confirmation is accepted from the customer.

Notifications derive from own recent invoices and usable vouchers. Read markers are local browser
state. No review, cart, notification, recommendation or shipment table was added.

## Checks

- `BACKEND`: JDK 17, `mvnw.cmd compile`.
- Run integration fixtures only against a disposable existing database whose name contains `Audit`.
  New commerce fixtures assert that guard and are transactionally rolled back. Carrier responses are
  mocked only inside tests. Do not run them against the live SmashStep database.
- `CUSTOMER_FRONTEND`: `npm run build`, `node --test scripts/commerce.test.mjs`.
- `FRONTEND`: `npm run build`.
- `git diff -- Database` must stay empty. No migrations, seeds or runtime schema updates are needed.

## Account and checkout address recovery

Expired or malformed customer sessions return to login with the intended protected URL. The backend returns 401 for unauthenticated requests and retains 403 for insufficient roles. Profile and addresses are always read for the authenticated customer.

Checkout preselects the saved default address (or the first active address) and prefills the recipient from that address, falling back to profile name/phone. Names are resolved against GHN aliases only when a single province and ward match; ambiguous/obsolete locations require explicit selection. Account and checkout use the same address selector. No carrier IDs are added to the database.

A new address can be saved immediately, or saved atomically with a successful order using saveAddress/makeDefaultAddress. Repeated identical saves reuse the existing own address. Address writes serialize on the customer row, preserving one default address. Failed checkout and idempotent retry do not add extra addresses. Account email is displayed but is not submitted as a shipping snapshot or used to block checkout.

## Separate virtual carts and About page

Customer carts now use separate browser localStorage keys for each authenticated customer ID. The guest cart is separate and is not automatically merged into customer carts. Unattributed legacy shared-cart data is left untouched and is never assigned to an account. This is browser-local persistence, not cross-device synchronization.

Login, logout, session expiry and cross-tab account changes switch cart scope immediately. Late catalogue responses cannot populate another account cart. Checkout captures its original owner, closes on account changes, and applies a late successful purchase only to that original owner cart.

Add/change/remove operations persist only variant IDs and quantities in the browser. Their only product API is the read-only GET /api/store/variants. They do not create invoices, reserve stock, write cart rows or mutate database quantities. Checkout retains the existing transactional stock deduction only after a successful order.

The public /ve-chung-toi page introduces SmashStep and the actual shopping flow, with links from desktop/mobile navigation, footer and the homepage. No invented business statistics, reviews, addresses or contact details were added.


## Thanh toán chuyển khoản QR (20/10/2026)

- Checkout hỗ trợ phương thức đang hoạt động có mã `CHUYEN_KHOAN`, ngoài COD. Không tự thêm/sửa phương thức trong DB.
- Sau khi tạo đơn thành công, chi tiết đơn hiển thị VietQR đúng tổng tiền đã chốt, mã đơn, ngân hàng và người nhận. Chỉ chủ đơn được đọc API QR. Đơn chờ QR vẫn dùng quy tắc trừ kho/hoàn kho hiện có. Giỏ hàng vẫn chỉ lưu trên trình duyệt.
- Sử dụng mặc định tài khoản nhận của POS hiện tại; có thể cấu hình backend qua `PAYMENT_QR_ENABLED`, `PAYMENT_QR_BANK_BIN`, `PAYMENT_QR_BANK_LABEL`, `PAYMENT_QR_ACCOUNT`, `PAYMENT_QR_ACCOUNT_NAME`. Đây là thông tin nhận tiền công khai, không phải API key. Khởi động lại backend sau khi đổi cấu hình. Trước khi đổi tài khoản nhận, cần đối soát các đơn đang chờ; không đổi người nhận khi khách đang chuyển tiền.
- QR dùng Quick Link SVG theo tài liệu chính thức: https://www.vietqr.io/qr-dong ; cập nhật định dạng: https://www.vietqr.io/thong-bao-cap-nhat . Ảnh gửi tới VietQR chỉ gồm tài khoản nhận, tên nhận, tổng tiền và mã đơn; không gửi tên/địa chỉ/điện thoại khách. Nếu ảnh lỗi, giao diện vẫn hiển thị thông tin chuyển khoản thủ công.
- Không có webhook/ngân hàng báo tiền vào tự động. Quét QR, mở ảnh hoặc bấm kiểm tra trạng thái không ghi nhận thanh toán. Trang kiểm tra trạng thái backend mỗi 15 giây khi còn chờ.
- Cập nhật mới: đã bỏ panel đối soát thủ công khỏi giao diện quản trị theo yêu cầu. Danh sách/chi tiết quản lý ẩn đơn online chuyển khoản chưa có ngày thanh toán; COD giữ luồng hiện tại. API đối soát cũ vẫn giữ kiểm soát quyền và tiền thực nhận cho tương thích. Chưa có kết nối ngân hàng báo có tự động; thanh toán giả lập bên dưới không xác nhận đơn QR thật.
- Số tiền xác nhận phải đúng tổng đơn; mã ngân hàng được chuẩn hóa chữ hoa, không được dùng lại. Gửi lại cùng mã/số tiền cho cùng đơn đã xác nhận trả về kết quả hiện tại, không tạo khoản thu thứ hai. Dùng khóa giao dịch chung với hóa đơn để tuần tự hóa xác nhận/hủy.
- Chặn chuyển đơn QR sang bước xử lý tiếp theo khi chưa nhận tiền. Khóa sửa thông tin giao hàng của đơn QR sau khi tạo để giữ nguyên tổng tiền. Khách chỉ hủy đơn đang chờ, chưa có khoản thu; nếu đã chuyển khoản thì giữ đơn và liên hệ cửa hàng. Mã QR cũ/ảnh tải xuống không thể thu hồi từ ứng dụng ngân hàng, vì vậy không chuyển tiền vào đơn đã hủy.
- Không tạo bảng/cột/script SQL, không thêm API key hoặc token trong frontend. Không có test tự động mới và không chạy test trong lượt này.


## Hạn QR 5 phút và thanh toán thử (20/10/2026)

- QR thật có hạn cố định 300 giây từ `HoaDon.ngayTao`. API trả thêm `expiresAt` và `serverTime` theo UTC. Tải lại trang/kiểm tra trạng thái không cấp hạn mới. Từ đúng thời điểm hết hạn, backend trả `EXPIRED`, `payable=false`, `qrUrl=null`; frontend ẩn cả ảnh lẫn thông tin chuyển khoản. Đồng hồ dùng mốc giờ server và thời gian đơn điệu của trình duyệt, không tin giờ cài trên máy khách.
- Tên chủ tài khoản được chuẩn hóa chữ IN HOA, bỏ dấu tiếng Việt và gộp khoảng trắng: `VU CHI TUAN ANH`. Cùng một giá trị được dùng trên ảnh QR và thông tin bên cạnh. Cấu hình tên không đúng định dạng chữ bị chặn tạo QR.
- Hết hạn QR không tự hủy đơn/hoàn kho. Khách không nên chuyển tiền sau hạn. Quick Link là thông tin chuyển khoản tiêu chuẩn: ứng dụng không thể thu hồi ảnh QR đã chụp hoặc ngăn ngân hàng nhận một khoản chuyển muộn. Nhân viên vẫn được đối soát khoản tiền thực tế đã vào, kể cả khi trạng thái hướng dẫn QR là EXPIRED.
- Trang công khai `/thanh-toan-thu` hiện dùng form thẻ NCB và OTP thay cho các nút gán kết quả trực tiếp. Mẫu tiền 999.000đ, dữ liệu thử riêng trong tab, không kết nối ngân hàng.
- Phiên trải nghiệm độc lập lưu ở sessionStorage theo owner của giỏ hàng; dùng được khi chưa đăng nhập. Phiên tạo từ checkout đăng nhập dùng API giả lập riêng và bộ nhớ backend. Cả hai đều không tạo khoản thu/hóa đơn thật.
- Thanh toán thành công sau khi hết 5 phút bị từ chối trong luồng thử. Phiên cuối không thể thanh toán lần hai hoặc đổi từ thất bại/hết hạn thành thành công; cần tạo phiên mới. Tải lại giữ nguyên phiên và thời hạn.
- Test lượt này: OnlineBankPaymentTest (7 unit test, Mockito, không DB); tests/payment-demo.test.js (8 unit test Node). Đã kiểm tra trên trình duyệt: thành công, thất bại, hết hạn ẩn QR, reload không gia hạn. Không tạo đơn hay xác nhận tiền vào trên DB thật.


## Chọn sản phẩm trong giỏ và thanh toán thẻ giả lập

- Checkbox từng biến thể, chọn tất cả và trạng thái chọn một phần. Lưu lựa chọn riêng theo tài khoản; bỏ chọn toàn bộ vẫn giữ khi tải lại. Tổng/voucher/phí/checkout chỉ tính các sản phẩm được chọn. Sản phẩm hết hàng đã bỏ chọn không chặn sản phẩm còn lại. Đặt đơn thật thành công chỉ bỏ số lượng đã mua, giữ các dòng còn lại. Mua ngay chọn riêng biến thể đang xem.
- Giả lập được bật bằng PAYMENT_DEMO_ENABLED=true hoặc --app.payment.demo.enabled=true; mặc định tắt. Không thêm phương thức thanh toán vào DB. Checkout gọi API quote/create riêng, không đi qua CustomerCommerceService.checkout. Tạo phiên không lưu địa chỉ, dùng voucher, giữ/trừ kho hay tạo hóa đơn. Giỏ hàng được giữ nguyên khi thử.
- Dữ liệu NCB công khai từ https://sandbox.vnpayment.vn/apis/vnpay-demo/ : số thẻ thành công 9704198526191432198, tên NGUYEN VAN A, ngày phát hành 07/15, OTP 123456. Có mẫu không đủ số dư/chưa kích hoạt/khóa/hết hạn. Chỉ whitelist thẻ mẫu này, không dùng thẻ/mật khẩu thật. Không lưu hoặc ghi log thông tin thẻ/OTP. Không phải tích hợp VNPAY chính thức.
- Luồng: AWAITING_CARD → AWAITING_OTP → PAID_DEMO. Hết hạn cố định 300 giây, tối đa 5 lần nhập sai trên server, hủy/thất bại/hết hạn không chuyển thành thành công; retry thanh toán thành công không phát sinh giao dịch thứ hai. Owner lấy từ phiên đăng nhập, không nhận customer ID từ request.
- API customer: GET /api/customer/shop/demo-payments/config; POST /quote; POST /demo-payments; GET /{code}; POST /{code}/card, /otp, /cancel. Các path rút gọn cùng tiền tố /api/customer/shop/demo-payments. Chỉ CUSTOMER được truy cập phiên của mình.
- API quản lý: GET /api/hoa-don/paid-demo-orders, chỉ ADMIN/STAFF, chỉ trả PAID_DEMO. UI Quản lý hóa đơn hiển thị mục Đơn thử đã thanh toán riêng, không thao tác giao hàng hay xuất vào hóa đơn/doanh thu thật.
- Bộ nhớ phiên có giới hạn 20 phiên mỗi khách/1.000 phiên toàn bộ; phiên chưa thanh toán xóa sau hạn + 10 phút, phiên thành công giữ 24 giờ, mất khi backend restart. Không sửa bảng/cột hoặc SQL.
- Kiểm tra: DemoCardPaymentTest 8 test + OnlineBankPaymentTest 7 test, dùng mock không DB; Node demo-card/payment-demo 11 test, commerce 8 test; build cả CUSTOMER_FRONTEND và FRONTEND.
