# Báo cáo module Quản lý sản phẩm — SmashStep SD-013

## Final check và upload file local — 04/10/2026

Đã ghi file trực tiếp vào `D:\DATN_SD13\DATN_SMASHSTEP_SD13`, branch `quang`.
Phần này mô tả hiện trạng mới nhất; các mục bên dưới là lịch sử những lượt trước.

| Hạng mục | Kết quả |
| --- | --- |
| FINAL PRODUCT CHECK | PASS |
| FRONTEND BUILD | SUCCESS — 196 module |
| BACKEND BUILD | SUCCESS — clean compile Java 17, 78 file nguồn |
| BACKEND TEST | SUCCESS — 38 test, 0 failure/error/skipped |
| PRODUCT CRUD | PASS — create/read/update/status |
| VARIANT CRUD | PASS — create/read/update/status |
| CONFIRM FLOW | PASS — cancel không ghi; double click không nhân đôi request |
| LOCAL IMAGE PICKER / IMAGE PREVIEW | DONE |
| MULTIPART UPLOAD / LOCAL FILE STORAGE | DONE |
| IMAGE URL STORED IN DB | YES — `/uploads/products/{UUID}.{jpg/png/webp}` |
| IMAGE STILL WORKS AFTER REFRESH | YES — kiểm tra cả restart backend |
| IMAGE MAIN | DONE |
| IMAGE COLOR MAPPING | DB DOES NOT SUPPORT — bảng ảnh không có `id_mau_sac` |
| INVALID FILE VALIDATION / MAX SIZE VALIDATION | DONE |
| DATABASE SCHEMA / SIDEBAR / HEADER MODIFIED | NO |

### Upload và các sửa lỗi

- Picker chọn nhiều JPG/PNG/WEBP, tối đa 5 MB mỗi file, preview bằng blob URL.
  File rỗng/sai loại/quá dung lượng bị từ chối, file trùng không được thêm lại.
  Xóa preview và unmount revoke blob URL; input reset để chọn lại cùng file.
- Không có POST khi chọn ảnh hoặc hủy modal. Ảnh chỉ được tải sau xác nhận.
  Tạo sản phẩm lấy ID rồi upload từng file. Khi có lỗi một phần, giữ ảnh đã lưu,
  giữ file chưa lưu, báo đúng số ảnh còn lại; thử lại không POST sản phẩm lần nữa.
- Detail và edit có thêm ảnh, chọn ảnh chính và gỡ ảnh bằng API hiện có.
- POST multipart `/api/products/{id}/images/upload`: `file`, `isAnhChinh`.
  Browser tự tạo boundary; dùng Axios client hiện có. `mauSacId` bị từ chối vì DB
  hiện tại không có liên kết màu ở bảng ảnh; không thêm cột/schema.
- Backend kiểm tra MIME, chữ ký đầu file, file rỗng và giới hạn 5 MB. Sinh tên UUID,
  không dùng filename client làm path. Lưu trong `BACKEND/uploads/products/`.
  Thư mục được resolve từ module BACKEND, kể cả IntelliJ chạy từ thư mục cha.
  Có thể đặt `SMASHSTEP_PRODUCT_UPLOAD_DIR` thành đường dẫn tuyệt đối khi triển khai.
- Static resource `/uploads/products/**` đọc cùng thư mục lưu file. Vite dev/preview
  proxy đường dẫn này tới 8080; helper resolve ảnh theo Axios baseURL nếu dùng URL tuyệt đối.
- DB chỉ lưu URL tương đối. Rollback DB dọn file mới; gỡ ảnh dọn file sau commit,
  không xóa file còn được một record khác tham chiếu. URL HTTP/HTTPS cũ vẫn được hỗ trợ.
- Upload quá dung lượng trả HTTP 413 với message JSON. Mã sản phẩm dự kiến được
  quét trong số trang hữu hạn; kiểm tra `SP003 -> SP004`. Form sửa xử lý mã/tên null
  bằng validation thay vì lỗi `.trim()`. Lỗi danh sách hiển thị message thân thiện.

### Bằng chứng kiểm tra

- SQL Server thật: 14 integration test có rollback, gồm CRUD sản phẩm/biến thể,
  đủ tám loại thuộc tính, mapping DTO, ảnh chính và multipart/static resource.
- Browser thật: chọn 3 JPG, xóa preview B, upload các ảnh còn lại; thử PDF/TXT/EXE,
  file rỗng, file lớn hơn 5 MB; chọn lại cùng file, bỏ file trùng; upload PNG và WEBP.
- Thử nội dung PNG mang MIME JPEG: server từ chối. Create có lỗi một phần được
  kiểm tra hai lần trên browser; retry chỉ gửi file còn lại, không tạo sản phẩm trùng.
- Log HTTP của server test ghi multipart boundary thật, 201/400/413/404 và số request.
  Cặp cổng checkout chính `127.0.0.1:5173` / IntelliJ `8080` cũng upload thành công;
  mở URL ảnh trực tiếp và refresh vẫn hiển thị. Restart server test từ thư mục cha
  vẫn đọc được JPG/PNG/WEBP và bytes của JPG không đổi.
- Browser kiểm tra product/variant create, update, status, modal cancel và double click;
  phân trang 5 dòng với trang thứ hai, search trống, lọc trạng thái/danh mục và reset.
  Hủy thêm/sửa/status thuộc tính không gửi request ghi. Helper/state test kiểm tra
  cleanup blob, retry, resolve URL, formatter/query và field null.
- Ảnh bằng chứng: `BACKEND/target/product-check/browser-upload.jpg`
  (artifact test, được Git ignore).
- Hai sản phẩm kiểm thử, biến thể và các file ảnh kiểm thử đã được dọn; bốn sản phẩm
  có sẵn được giữ nguyên. Server test 8081/5174 đã dừng; server chính giữ nguyên.
- Hash sidebar/header/AdminLayout, logo, router, Axios helper, SQL, dependency không đổi.
  Đối chiếu trước/sau: metadata 199 cột của SQL Server không đổi;
  `git diff -- Database/` không có thay đổi.

### File tạo trong lượt này (5)

- `BACKEND/src/main/java/com/smashstep/datn/product/config/HinhAnhSanPhamConfig.java`
- `BACKEND/src/main/java/com/smashstep/datn/product/config/ThuMucAnhSanPham.java`
- `BACKEND/src/test/java/com/smashstep/datn/product/HinhAnhSanPhamUploadTest.java`
- `FRONTEND/src/modules/admin/product/components/ProductImagePicker.vue`
- `FRONTEND/src/modules/admin/product/services/imageUtils.js`

### File sửa trong lượt này (18)

- `BACKEND/.gitignore`
- `BACKEND/src/main/resources/application.properties`
- `BACKEND/src/main/java/com/smashstep/datn/common/exception/GlobalExceptionHandler.java`
- `BACKEND/src/main/java/com/smashstep/datn/product/controller/HinhAnhSanPhamController.java`
- `BACKEND/src/main/java/com/smashstep/datn/product/dto/HinhAnhSanPhamResponse.java`
- `BACKEND/src/main/java/com/smashstep/datn/product/repository/HinhAnhSanPhamRepository.java`
- `BACKEND/src/main/java/com/smashstep/datn/product/service/HinhAnhSanPhamService.java`
- `BACKEND/src/test/java/com/smashstep/datn/product/SanPhamTichHopTest.java`
- `FRONTEND/vite.config.js`
- `FRONTEND/src/modules/admin/product/components/ProductForm.vue`
- `FRONTEND/src/modules/admin/product/components/ProductImages.vue`
- `FRONTEND/src/modules/admin/product/components/ProductThumbnail.vue`
- `FRONTEND/src/modules/admin/product/services/productService.js`
- `FRONTEND/src/modules/admin/product/views/ProductCreate.vue`
- `FRONTEND/src/modules/admin/product/views/ProductEdit.vue`
- `FRONTEND/src/modules/admin/product/views/ProductList.vue`
- `FRONTEND/src/modules/admin/product/product.css`
- `FRONTEND/src/modules/admin/product/README.md`

REMAINING: không phát hiện lỗi compile/import hoặc lỗi product chặn các luồng đã kiểm tra.
Ảnh theo màu không triển khai vì schema hiện tại không hỗ trợ. Không commit/push/tạo worktree.

---

## Cập nhật danh sách frontend ngày 03/10/2026

Đã sửa trực tiếp checkout `D:\DATN_SD13\DATN_SMASHSTEP_SD13`, branch `quang`.
Mở đúng frontend của checkout ổ D: **http://127.0.0.1:5173/san-pham**.

Máy hiện có hai Vite server dùng cổng 5173: bản OneDrive nghe trên IPv6
`::1`, còn checkout ổ D nghe trên `127.0.0.1`. Vì vậy `localhost:5173`
có thể mở bản OneDrive vẫn còn mock. Lượt này không sửa hoặc dừng bản OneDrive.
Vite config của checkout ổ D đã đặt host mặc định `127.0.0.1`.

- Tái sử dụng Axios client `src/utils/api.js`, không tạo instance khác.
- Base URL mặc định `/api`; Vite dev/preview proxy tới backend
  `http://localhost:8080`. Có thể đặt `VITE_API_URL` để dùng API URL khác;
  URL tuyệt đối sẽ gọi trực tiếp và không đi qua proxy.
- GET `/api/products` khi mounted, không chờ các API thuộc tính và không
  fallback về dữ liệu mẫu. Chỉ tải sáu loại thuộc tính đang có trong bộ lọc.
- Search gửi `keyword` qua nút Tìm kiếm / Lọc hoặc Enter; trim khoảng trắng.
  Chọn dropdown gửi filter ngay, gồm brandId/categoryId/materialId/styleId/
  collarId/originId/status. Giá trị status=0 vẫn được gửi.
- Đặt lại xóa toàn bộ filter, gọi page=0. Phân trang lấy number/size/
  totalElements/totalPages thật; hiển thị số trang từ 1 và giữ chọn 5/10/20.
- Mapping dùng maSanPham, tenSanPham, tenThuongHieu, tenDanhMuc,
  tongSoLuong, giaThapNhat/giaCaoNhat, trangThai, anhChinh; không dùng SKU
  ở cấp sản phẩm. Giá dùng Intl.NumberFormat vi-VN/VND; thiếu giá hiện
  “Chưa có giá”, số lượng null hiện 0. Thumbnail null/lỗi có placeholder.
- Có loading trong bảng, “Không có sản phẩm”, lỗi tải và nút Thử lại.
  Axios timeout 10 giây; lỗi API không hiện stack trace trong giao diện.
- Sidebar, logo, AdminLayout, router, font và CSS tổng thể được giữ nguyên.
- Backend, SQL/schema/entity, package.json và package-lock.json giữ nguyên.

### Kiểm tra trong lượt này

- `npm.cmd run build`: SUCCESS, 190 module.
- Trình duyệt đã hiển thị SP001 / Nike Air Max Running 2026, Nike,
  Giày chạy bộ, tồn kho 35, giá 1.599.000 ₫ - 1.699.000 ₫, Hoạt động;
  ảnh null hiện placeholder.
- Đã kiểm tra search có/không có kết quả, sáu filter thuộc tính, status 0/1,
  đặt lại, chọn 5/20 bản ghi, page 0 -> nút trang 1 và giới hạn trước/sau.
  Dữ liệu ban đầu có một sản phẩm, nên chưa có trang thứ hai để kiểm tra
  chuyển trang trên nhiều trang dữ liệu thật.
- Loading và lỗi timeout được kiểm tra bằng server local tạm không trả
  response; không sửa/dừng backend thật, không ghi database và không trả
  sản phẩm giả. Server tạm đã dừng sau kiểm tra.
- 8 kiểm tra formatter/query qua source thật đều qua: giá null, giá bằng
  nhau, khoảng giá, số/string cùng giá, giá không hợp lệ, trim keyword,
  giữ status/page=0 và bỏ filter rỗng.

### File sửa của lượt frontend list này (8)

- FRONTEND/vite.config.js
- FRONTEND/src/utils/api.js
- FRONTEND/src/modules/admin/product/services/productService.js
- FRONTEND/src/modules/admin/product/services/productAttributeService.js
- FRONTEND/src/modules/admin/product/views/ProductList.vue
- FRONTEND/src/modules/admin/product/components/ProductFilter.vue
- FRONTEND/src/modules/admin/product/components/ProductTable.vue
- FRONTEND/src/modules/admin/product/README.md

FILES CREATED: 0. FILES DELETED: 0. Không commit/push/đổi branch/tạo worktree.
Các mục bên dưới là lịch sử những lượt triển khai trước, có tên class và
kết quả kiểm tra tại thời điểm cũ. Backend hiện tại xem POSTMAN_PRODUCT_TEST.md.

---

## Phạm vi

Triển khai sản phẩm, biến thể, tám loại thuộc tính và ảnh URL cho trang Admin.
Frontend gọi Axios client hiện có, base URL `http://localhost:8080/api`.
Không thay schema, bảng/cột SQL, entity hoặc dependency trong package.json/pom.xml.
Không sửa các module invoice, promotion, customer, employee hay payment.

Đã đối chiếu 11 entity product với `sql_sd013.sql`. Mapping bảng, cột và khóa ngoại
của SanPham, SanPhamChiTiet, HinhAnhSanPham và tám thuộc tính khớp schema.
Script SQL và entity hiện có được giữ nguyên.

File `Database/01_sqlSD13.sql` được nhắc trong yêu cầu không có trong checkout;
nguồn SQL thực tế là `sql_sd013.sql`. Đã kiểm tra cả metadata SQL Server thật:
`hinh_anh_san_pham` chỉ có `id`, `id_san_pham`, `url_anh`, `is_anh_chinh`,
không có `id_mau_sac`. Ảnh vì vậy dùng chung cho sản phẩm và các biến thể,
không lưu liên kết ảnh theo màu. Không thêm cột để làm theo mô tả khác schema.

Repo chưa định nghĩa ý nghĩa số trangThai, cũng không có dữ liệu seed cho trạng thái.
Quy ước module mới: **1 = hoạt động, 0 = ngừng hoạt động**.
Tồn kho và khoảng giá được tính trên tất cả biến thể theo yêu cầu SUM(so_luong).
Kích hoạt và trạng thái biến thể là hai trường riêng, không tự ghi đè nhau.

## Kết quả lượt cập nhật CRUD hiện tại

Đã ghi trực tiếp vào `D:\DATN_SD13\DATN_SMASHSTEP_SD13`, branch `quang`.
Giữ lại CRUD đã có và bổ sung các phần thiếu; không tạo lại module.
Không tạo worktree, chuyển branch, commit hoặc push.

| Nhóm | Thao tác | Kết quả |
| --- | --- | --- |
| Sản phẩm | CREATE, READ LIST, READ DETAIL, UPDATE, STATUS | DONE |
| Biến thể | CREATE, READ, UPDATE, STATUS | DONE |
| Tám loại thuộc tính | GET, POST, PUT, PATCH status | DONE |
| Ảnh URL | Thêm, đọc, sửa, chọn ảnh chính, gỡ liên kết | DONE |

Các thao tác đọc/ghi dùng backend API và SQL Server thật. Không dùng `productData.js`.
Checkout hiện tại dùng `ProductCreate.vue`, không có `AddProduct.vue` hoặc
`productData.js` trong module. Form đã dùng đúng sáu ID thuộc tính của entity.

**DATABASE CHANGES: NONE.** Script SQL, schema, entity, cấu hình kết nối,
dependency, router và API helper giữ nguyên trong lượt cập nhật này.

### File mới trong lượt này (1)

- FRONTEND/src/modules/admin/product/components/ProductThumbnail.vue

### File sửa trong lượt này (19)

- BACKEND/src/main/java/com/smashstep/datn/product/controller/ProductController.java
- BACKEND/src/main/java/com/smashstep/datn/product/dto/ProductDtos.java
- BACKEND/src/main/java/com/smashstep/datn/product/repository/InventorySummary.java
- BACKEND/src/main/java/com/smashstep/datn/product/repository/SanPhamChiTietRepository.java
- BACKEND/src/main/java/com/smashstep/datn/product/repository/HinhAnhSanPhamRepository.java
- BACKEND/src/main/java/com/smashstep/datn/product/service/ProductService.java
- BACKEND/src/main/java/com/smashstep/datn/product/service/VariantService.java
- BACKEND/src/test/java/com/smashstep/datn/product/VariantServiceTest.java
- BACKEND/src/test/java/com/smashstep/datn/product/ProductDatabaseIntegrationTest.java
- FRONTEND/src/modules/admin/product/services/productAttributeService.js
- FRONTEND/src/modules/admin/product/components/ProductTable.vue
- FRONTEND/src/modules/admin/product/components/ProductForm.vue
- FRONTEND/src/modules/admin/product/components/VariantTable.vue
- FRONTEND/src/modules/admin/product/components/VariantEditModal.vue
- FRONTEND/src/modules/admin/product/components/ProductImages.vue
- FRONTEND/src/modules/admin/product/views/ProductCreate.vue
- FRONTEND/src/modules/admin/product/views/ProductDetail.vue
- FRONTEND/src/modules/admin/product/views/ProductVariantList.vue
- FRONTEND/src/modules/admin/product/README.md

Các danh sách bên dưới là tổng hợp toàn bộ module, gồm cả thay đổi từ những lượt trước.

## FILES CREATED

Đường dẫn dưới đây tính từ thư mục repo DATN_SMASHSTEP_SD13.

Backend, trong `BACKEND/src/main/java/com/smashstep/datn/product/`:

- controller/ProductController.java
- controller/ProductVariantController.java
- controller/ProductAttributeController.java
- controller/ProductExceptionHandler.java
- dto/ProductDtos.java
- repository/InventorySummary.java
- service/ProductService.java
- service/VariantService.java
- service/ProductAttributeService.java
- service/ProductException.java
- service/ProductRules.java

Test, trong `BACKEND/src/test/java/com/smashstep/datn/product/`:

- ProductValidationTest.java
- ProductServiceTest.java
- VariantServiceTest.java
- ProductAttributeServiceTest.java
- ProductDatabaseIntegrationTest.java

Frontend, trong `FRONTEND/src/modules/admin/product/`:

- views/ProductList.vue
- views/ProductCreate.vue
- views/ProductDetail.vue
- views/ProductEdit.vue
- views/ProductVariantList.vue
- views/ProductAttributeList.vue
- components/ProductShell.vue
- components/ProductFilter.vue
- components/ProductTable.vue
- components/ProductThumbnail.vue
- components/ProductForm.vue
- components/ProductPagination.vue
- components/StatusBadge.vue
- components/VariantTable.vue
- components/VariantEditModal.vue
- components/VariantCreate.vue
- components/ProductImages.vue
- services/productService.js
- services/productAttributeService.js
- product.css
- README.md (báo cáo này)

Tổng: 37 file mới so với code trước khi triển khai module.

## FILES MODIFIED

- BACKEND/mvnw.cmd: sửa wrapper Windows ở lượt trước để chạy Maven được.
- BACKEND/src/main/java/com/smashstep/datn/product/repository/SanPhamRepository.java
- BACKEND/src/main/java/com/smashstep/datn/product/repository/SanPhamChiTietRepository.java
- BACKEND/src/main/java/com/smashstep/datn/product/repository/HinhAnhSanPhamRepository.java
- BACKEND/src/main/java/com/smashstep/datn/product/repository/DanhMucRepository.java
- BACKEND/src/main/java/com/smashstep/datn/product/repository/ThuongHieuRepository.java
- BACKEND/src/main/java/com/smashstep/datn/product/repository/ChatLieuRepository.java
- BACKEND/src/main/java/com/smashstep/datn/product/repository/KieuDangRepository.java
- BACKEND/src/main/java/com/smashstep/datn/product/repository/CoGiayRepository.java
- BACKEND/src/main/java/com/smashstep/datn/product/repository/XuatXuRepository.java
- BACKEND/src/main/java/com/smashstep/datn/product/repository/MauSacRepository.java
- BACKEND/src/main/java/com/smashstep/datn/product/repository/KichThuocRepository.java
- FRONTEND/src/router/index.js
- BACKEND/src/test/java/com/example/demo/DatnApplicationTests.java:
  chỉ định đúng DatnApplication để test package cũ tìm được ứng dụng;
  tắt Open Session in View trong test để kiểm tra DTO không phụ thuộc vào nó.
- FRONTEND/package-lock.json:
  npm install bổ sung ba cờ peer metadata; không thêm/đổi phiên bản dependency.

Tổng: 15 file hiện có được sửa so với code trước khi triển khai module.

application.properties có thay đổi sẵn trước khi triển khai; không sửa hoặc ghi đè file đó.
AdminLayout.vue, utils/api.js, sql_sd013.sql và toàn bộ entity không bị chỉnh sửa.

## BACKEND API

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| GET | /api/products | Tìm/lọc/phân trang, tồn kho, khoảng giá, số màu/size, ảnh chính |
| GET | /api/products/{id} | DTO chi tiết: product, variants, images |
| POST | /api/products | Tạo sản phẩm |
| PUT | /api/products/{id} | Sửa thông tin; giữ mã sản phẩm và biến thể |
| PATCH | /api/products/{id}/status | Đổi trạng thái |
| GET | /api/products/{id}/variants | Đọc toàn bộ biến thể của một sản phẩm |
| POST | /api/products/{id}/variants | Tạo hàng loạt biến thể trong một transaction |
| POST | /api/products/{id}/images | Thêm URL ảnh |
| PUT | /api/products/{id}/images/{imageId} | Sửa URL/chọn ảnh chính |
| DELETE | /api/products/{id}/images/{imageId} | Gỡ liên kết ảnh; không xóa sản phẩm |
| GET | /api/product-details | Tìm/lọc/phân trang biến thể |
| POST | /api/product-details | Tạo một biến thể |
| PUT | /api/product-details/{id} | Sửa biến thể, giá, tồn kho, kích hoạt, trạng thái |
| PATCH | /api/product-details/{id}/status | Đổi trạng thái biến thể |
| GET | /api/product-attributes/options | Tám danh sách thuộc tính để dùng trong dropdown |
| GET | /api/product-attributes/{type} | Tìm/lọc/phân trang một loại thuộc tính |
| POST | /api/product-attributes/{type} | Tạo thuộc tính |
| PUT | /api/product-attributes/{type}/{id} | Sửa thuộc tính |
| PATCH | /api/product-attributes/{type}/{id}/status | Đổi trạng thái thuộc tính |

Các type: `categories, brands, materials, styles, collars, origins, colors, sizes`.

Tám API đọc thuộc tính mà dropdown frontend đang gọi:

- GET /api/product-attributes/categories
- GET /api/product-attributes/brands
- GET /api/product-attributes/materials
- GET /api/product-attributes/styles
- GET /api/product-attributes/collars
- GET /api/product-attributes/origins
- GET /api/product-attributes/colors
- GET /api/product-attributes/sizes

Mỗi type cũng hỗ trợ POST, PUT /{id} và PATCH /{id}/status như bảng trên.

Danh sách sản phẩm:
`page=0&size=10&keyword=&status=&categoryId=&brandId=&materialId=&styleId=&collarId=&originId=`.

Danh sách biến thể:
`page=0&size=10&keyword=&status=&productId=&colorId=&sizeId=`.

Danh sách thuộc tính:
`page=0&size=10&keyword=&status=`.

Response phân trang ổn định:
`{ content, number, size, totalElements, totalPages }`.
GET options vẫn được giữ để tương thích. Dropdown gọi riêng tám API theo type,
đọc đủ các trang thuộc tính và bỏ ID trùng; các màn hình danh sách phân trang tại database.

ProductResponse bổ sung `soMau`, `soKichThuoc`, `anhChinh` và sáu nhãn
`danhMuc`, `thuongHieu`, `chatLieu`, `kieuDang`, `coGiay`, `xuatXu`.
Các trường ID/tenDanhMuc/tenThuongHieu... cũ được giữ để tương thích frontend.
VariantRequest nhận cả `sanPhamId`, `mauSacId`, `kichThuocId` và các alias
`idSanPham`, `idMauSac`, `idKichThuoc`; response trả cả hai cách đặt tên ID.
VariantResponse có ảnh chính của sản phẩm; không giả lập ảnh riêng theo màu.

HTTP: tạo mới 201, cập nhật/đọc 200, gỡ ảnh 204, validation 400,
không tìm thấy 404, trùng/xung đột 409.

CORS chỉ cho origin HTTP trên localhost và 127.0.0.1, phục vụ Vite dev/preview.
Exception handler giới hạn trong package controller product.

## FRONTEND ROUTES

| Route | View |
| --- | --- |
| /san-pham | ProductList.vue |
| /san-pham/them | ProductCreate.vue |
| /san-pham/:id | ProductDetail.vue |
| /san-pham/:id/sua | ProductEdit.vue |
| /bien-the-san-pham | ProductVariantList.vue |
| /thuoc-tinh | ProductAttributeList.vue |

Các route thêm/sửa được khai báo trước route chi tiết và dùng import theo yêu cầu.
Tất cả màn hình nằm trong AdminLayout hiện có.
CSS giới hạn dưới .product-page, không đổi phong cách các module khác.

## FUNCTIONS COMPLETED

- Danh sách sản phẩm với sáu bộ lọc thuộc tính, mã/tên, trạng thái.
- Pagination thật, chọn 5/10/20 bản ghi, trang trước/sau, số trang, tổng bản ghi.
- Tồn kho SUM, giá MIN/MAX, đếm màu/size DISTINCT; hỗ trợ sản phẩm chưa có biến thể.
- Ảnh chính và aggregate được đọc theo danh sách ID trong trang, tránh query ảnh từng dòng.
- Thumbnail dùng URL thật, có trạng thái thiếu ảnh/URL lỗi; không dùng ảnh mẫu hoặc Base64.
- Thêm sản phẩm bằng thuộc tính thật từ database, kiểm tra mã trùng và trường bắt buộc.
- Form thêm gồm hai bước trên cùng màn hình: lưu sản phẩm, nhận ID rồi tạo màu × size.
  Nếu batch biến thể lỗi, giữ sản phẩm đã tạo và dữ liệu form để sửa rồi thử lại,
  hiển thị rõ sản phẩm đã lưu và lỗi biến thể. Không POST lại sản phẩm khi thử lại batch.
- Sửa sản phẩm, cập nhật ngày sửa; giữ nguyên mã và dữ liệu biến thể.
- Chi tiết sản phẩm, thuộc tính, mô tả, ảnh, tất cả biến thể, tổng tồn và tổng số biến thể.
- Tạo tổ hợp màu × size; bỏ tổ hợp đã có, cho sửa mã/SKU/giá/tồn trước khi lưu.
- Batch tối đa 200 dòng, kiểm tra trùng trong batch và database, rollback nếu có lỗi.
- Danh sách biến thể có mã sản phẩm, mã CTSP, SKU, ảnh, màu/size và giá bán thật,
  tìm/lọc/phân trang; sửa mã CTSP/SKU/màu/size/giá/tồn/kích hoạt/trạng thái.
  Không hiển thị discount giả.
- Quản lý đủ tám loại thuộc tính: tìm kiếm, phân trang, thêm, sửa, bật/tắt.
- Màu sắc có HEX và preview; size có giá trị, ghi chú.
- Thuộc tính ngừng hoạt động không dùng để tạo mới, nhưng được giữ khi sửa liên kết cũ.
- Đổi trạng thái có confirmation, thao tác ghi có khóa nút khi gửi.
- Không có endpoint/UI xóa cứng sản phẩm, biến thể hoặc thuộc tính.
- Ảnh URL HTTP/HTTPS: nhiều ảnh, chọn một ảnh chính, sửa URL, gỡ ảnh.
- DTO được tạo trong transaction, không serialize trực tiếp entity LAZY.
- Validation backend bằng Bean Validation và service, frontend bằng form và kiểm tra nghiệp vụ.
- Thông báo lỗi kết nối, lỗi dữ liệu và trạng thái không tìm thấy.

## NOT COMPLETED / GIỚI HẠN

- Upload file ảnh thật chưa triển khai: repo chưa có storage/upload.
  Hiện dùng URL; không thêm Cloudinary, Firebase, AWS hoặc dịch vụ ngoài.
- Ảnh theo màu chưa hỗ trợ vì SQL và entity thật không có id_mau_sac trên bảng ảnh.
- Không thêm endpoint tùy chọn /api/products/full. Tạo sản phẩm và batch biến thể
  là hai request riêng; batch biến thể có transaction và rollback toàn batch khi lỗi.
- Mã sản phẩm nhập thủ công. Mã/SKU biến thể được gợi ý từ mã sản phẩm và ID màu/size,
  cho phép sửa trước khi lưu.
- Chưa có xác thực người dùng trong repo nên không tự gán ID người tạo/cập nhật.
- Schema không có UNIQUE. Service kiểm tra trùng; khóa bản ghi sản phẩm bảo vệ tổ hợp
  biến thể thuộc cùng sản phẩm. Chưa kiểm thử cạnh tranh nhiều instance cho mã/SKU
  toàn hệ thống, và thao tác SQL trực tiếp có thể bỏ qua kiểm tra service.
- Browser kiểm tra hiển thị/form/trạng thái trống/lỗi frontend. Luồng dữ liệu tạo/sửa
  được kiểm thử bằng API/JPA với rollback; không tạo dữ liệu mẫu lâu dài trong database.

## TEST RESULT

Backend:

- .\mvnw.cmd clean compile: BUILD SUCCESS, Java 17, compile 62 file nguồn.
- .\mvnw.cmd test: BUILD SUCCESS, 25 test, 0 failure, 0 error, 0 skipped.
- 17 unit/validation test, 7 integration test, 1 context test hiện có.
- Integration sử dụng SQL Server SmashStep thật, ddl-auto=none,
  Open Session in View=false; dữ liệu test được rollback.
- Kiểm tra các bộ lọc, phân trang, aggregate, JSON DTO, cập nhật giữ biến thể,
  trạng thái, CORS, lỗi 400/404/409 và ảnh chính.
- Test MockMvc thực hiện HTTP CRUD sản phẩm, biến thể, cả tám thuộc tính,
  thêm/sửa/gỡ ảnh, đọc lại sau flush/clear, kiểm tra URL Base64 bị từ chối.
- Test đối chiếu bốn cột bảng ảnh bằng INFORMATION_SCHEMA.COLUMNS của SQL Server thật.
- Backend chạy bằng Maven wrapper trên cổng 8080 để kiểm tra trình duyệt.
  HTTP thực tế products, product-details và cả tám API thuộc tính trả 200;
  product ID không tồn tại trả 404. Server kiểm tra đã dừng sau khi kiểm tra.
- Wrapper Windows đã được sửa ở lượt trước, các lệnh wrapper cuối lượt này chạy thành công.

Frontend:

- Không cần npm install trong lượt này; không thêm thư viện mới.
- npm run build: thành công, 190 module được compile, đủ sáu view.
- Kiểm tra trên trình duyệt: danh sách sản phẩm, form thêm, danh sách biến thể,
  trang thuộc tính đọc API thật và hiển thị cột/form mới.
  Route chi tiết/sửa hiển thị lỗi sản phẩm không tồn tại đúng thay vì trang trắng.
- Database hiện chưa có sản phẩm/thuộc tính; giao diện dùng trạng thái trống thật
  và hướng dẫn thêm thuộc tính trước khi tạo sản phẩm.
- Không có lỗi console trên trang sửa đã kiểm tra. Sidebar chung còn cảnh báo
  route /thong-ke, /ban-hang, /tai-khoan chưa được khai báo ở các module khác;
  không thay các module đó trong lượt này.

## Chạy lại

Frontend:

```powershell
cd FRONTEND
npm install
npm run dev
npm run build
```

Backend: JDK 17, Maven wrapper và kết nối SQL Server hiện có:

```powershell
cd BACKEND
.\mvnw.cmd clean compile
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Build dùng JDK 17 và cache C:\Users\DELL\.m2\repository.
Không cần chạy lại script SQL nếu database đã tồn tại.
