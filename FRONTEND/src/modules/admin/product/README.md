# Báo cáo module Quản lý sản phẩm — SmashStep SD-013

## Phạm vi

Triển khai sản phẩm, biến thể, tám loại thuộc tính và ảnh URL cho trang Admin.
Frontend gọi Axios client hiện có, base URL `http://localhost:8080/api`.
Không thay schema, bảng/cột SQL, entity hoặc dependency trong package.json/pom.xml.
Không sửa các module invoice, promotion, customer, employee hay payment.

Đã đối chiếu 11 entity product với `sql_sd013.sql`. Mapping bảng, cột và khóa ngoại
của SanPham, SanPhamChiTiet, HinhAnhSanPham và tám thuộc tính khớp schema.
Script SQL và entity hiện có được giữ nguyên.

Repo chưa định nghĩa ý nghĩa số trangThai, cũng không có dữ liệu seed cho trạng thái.
Quy ước module mới: **1 = hoạt động, 0 = ngừng hoạt động**.
Tồn kho và khoảng giá được tính trên tất cả biến thể theo yêu cầu SUM(so_luong).
Kích hoạt và trạng thái biến thể là hai trường riêng, không tự ghi đè nhau.

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

Tổng: 36 file mới.

## FILES MODIFIED

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

Tổng: 14 file hiện có được sửa.

application.properties có thay đổi sẵn trước khi triển khai; không sửa hoặc ghi đè file đó.
AdminLayout.vue, utils/api.js, sql_sd013.sql và toàn bộ entity không bị chỉnh sửa.

## BACKEND API

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| GET | /api/products | Tìm/lọc/phân trang sản phẩm, tồn kho và khoảng giá |
| GET | /api/products/{id} | DTO chi tiết: product, variants, images |
| POST | /api/products | Tạo sản phẩm |
| PUT | /api/products/{id} | Sửa thông tin; giữ mã sản phẩm và biến thể |
| PATCH | /api/products/{id}/status | Đổi trạng thái |
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

Danh sách sản phẩm:
`page=0&size=10&keyword=&status=&categoryId=&brandId=&materialId=&styleId=&collarId=&originId=`.

Danh sách biến thể:
`page=0&size=10&keyword=&status=&productId=&colorId=&sizeId=`.

Danh sách thuộc tính:
`page=0&size=10&keyword=&status=`.

Response phân trang ổn định:
`{ content, number, size, totalElements, totalPages }`.
GET options là danh mục dùng chung cho dropdown; các màn hình danh sách vẫn phân trang tại database.

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
- Tồn kho tổng hợp và giá thấp/cao; hỗ trợ sản phẩm chưa có biến thể.
- Thêm sản phẩm bằng thuộc tính thật từ database, kiểm tra mã trùng và trường bắt buộc.
- Sửa sản phẩm, cập nhật ngày sửa; giữ nguyên mã và dữ liệu biến thể.
- Chi tiết sản phẩm, thuộc tính, mô tả, ảnh, tất cả biến thể, tổng tồn và tổng số biến thể.
- Tạo tổ hợp màu × size; bỏ tổ hợp đã có, cho sửa mã/SKU/giá/tồn trước khi lưu.
- Batch tối đa 200 dòng, kiểm tra trùng trong batch và database, rollback nếu có lỗi.
- Danh sách biến thể có tìm/lọc/phân trang; sửa giá/tồn/kích hoạt/trạng thái.
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

- Compile Java 17: thành công.
- Maven package: BUILD SUCCESS, tạo target/smashstep-backend-0.0.1-SNAPSHOT.jar.
- 22 test, 0 failure, 0 error, 0 skipped.
- 17 unit/validation test mới, 4 integration test mới, 1 context test hiện có.
- Integration sử dụng SQL Server SmashStep thật, ddl-auto=none,
  Open Session in View=false; dữ liệu test được rollback.
- Kiểm tra các bộ lọc, phân trang, aggregate, JSON DTO, cập nhật giữ biến thể,
  trạng thái, CORS, lỗi 400/404/409 và ảnh chính.
- JAR khởi động thành công trên cổng 8080.
- HTTP thực tế: products, product-details, attributes/options, attributes/colors trả 200;
  product ID không tồn tại trả 404.
- Maven wrapper hiện có bị lỗi “Cannot index into a null array” trên Windows.
  Không sửa wrapper ngoài phạm vi product; dùng Maven có sẵn trong IntelliJ để build.

Frontend:

- npm install: thành công, không thêm thư viện mới.
- npm run build: thành công, 188 module được compile, đủ sáu view mới.
- Kiểm tra trên trình duyệt: danh sách sản phẩm, form thêm, danh sách biến thể,
  trang thuộc tính, chuyển sang màu sắc, form HEX và lỗi HEX không hợp lệ.
  Route chi tiết/sửa hiển thị lỗi sản phẩm không tồn tại đúng thay vì trang trắng.
- Database hiện chưa có sản phẩm/thuộc tính; giao diện dùng trạng thái trống thật
  và hướng dẫn thêm thuộc tính trước khi tạo sản phẩm.

## Chạy lại

Frontend:

```powershell
cd FRONTEND
npm install
npm run dev
npm run build
```

Backend: dùng Maven/IntelliJ đã cấu hình, kết nối SQL Server hiện có:

```powershell
mvn compile
mvn test
mvn package
java -jar target/smashstep-backend-0.0.1-SNAPSHOT.jar
```

Trong môi trường hiện tại, Maven nằm ở:
C:\Program Files\JetBrains\IntelliJ IDEA 2020.3.4\plugins\maven\lib\maven3\bin\mvn.cmd.
Build dùng cache C:\Users\DELL\.m2\repository.
Không cần chạy lại script SQL nếu database đã tồn tại.
