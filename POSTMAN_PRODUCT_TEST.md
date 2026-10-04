# Test backend quản lý sản phẩm bằng Postman

Project: `D:\DATN_SD13\DATN_SMASHSTEP_SD13`, branch `quang`.
Backend: Java 17, Spring Boot 4.0.0, Spring Data JPA, Jakarta Validation,
Lombok, SQL Server; dùng Maven Wrapper hiện có.

## 1. Chuẩn bị

Chạy trong PowerShell:

```powershell
cd D:\DATN_SD13\DATN_SMASHSTEP_SD13\BACKEND
.\mvnw.cmd clean compile
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.arguments=--server.port=18080'
```

Chờ log `Started DatnApplication` và Tomcat nghe cổng 18080.
Lượt kiểm tra này dùng 18080 vì 8080 đang được dùng. Port mặc định trong
`application.properties` vẫn là 8080; tham số chạy không sửa file cấu hình.
SQL Server phải chạy và cấu hình kết nối hiện có phải hợp lệ. Không đổi password
trong code hoặc chạy lại SQL để test API. `spring.jpa.hibernate.ddl-auto=none`.

Tạo Postman Environment, chọn environment trước khi gửi request:

| Biến | Giá trị |
| --- | --- |
| baseUrl | http://localhost:18080/api |
| runKey | Chuỗi khác nhau cho mỗi lượt, ví dụ 20261003_1600 |
| categoryId, brandId, materialId, styleId, collarId, originId | ID lấy từ API thuộc tính |
| colorId, sizeId, sizeId2 | ID màu và hai kích thước khác nhau lấy từ API |
| productId, variantId, imageId | ID từ response POST tương ứng |

Mọi request POST/PUT/PATCH: Body → raw → JSON,
header `Content-Type: application/json`. Các biến ID không đặt trong dấu nháy;
Postman thay `{{categoryId}}` bằng số trước khi gửi. Không gửi ID chưa được gán.
Không mặc định ID bằng 1: database hiện có thể chưa có dữ liệu reference.

Module chưa có user context xác thực; không gửi ID nhân viên giả cho audit.

## 2. Lấy hoặc tạo dữ liệu thuộc tính trước

GET tám URL sau. `status=1` chỉ lấy thuộc tính hoạt động; `page` bắt đầu **0**,
`size` từ **1 đến 100**. Nếu có nhiều trang, đọc tiếp theo `totalPages`.

| Thuộc tính | Request | Biến lưu ID từ content |
| --- | --- | --- |
| Danh mục | GET {{baseUrl}}/product-attributes/categories?status=1&page=0&size=100 | categoryId |
| Thương hiệu | GET {{baseUrl}}/product-attributes/brands?status=1&page=0&size=100 | brandId |
| Chất liệu | GET {{baseUrl}}/product-attributes/materials?status=1&page=0&size=100 | materialId |
| Kiểu dáng | GET {{baseUrl}}/product-attributes/styles?status=1&page=0&size=100 | styleId |
| Cổ giày | GET {{baseUrl}}/product-attributes/collars?status=1&page=0&size=100 | collarId |
| Xuất xứ | GET {{baseUrl}}/product-attributes/origins?status=1&page=0&size=100 | originId |
| Màu sắc | GET {{baseUrl}}/product-attributes/colors?status=1&page=0&size=100 | colorId |
| Kích thước | GET {{baseUrl}}/product-attributes/sizes?status=1&page=0&size=100 | sizeId, sizeId2 |

Response danh sách:

```json
{
  "content": [],
  "number": 0,
  "size": 100,
  "totalElements": 0,
  "totalPages": 0
}
```

Nếu `content` trống, dùng POST thuộc tính bên dưới để tạo reference bằng API.
Không INSERT SQL trực tiếp hoặc sửa schema. Chọn hai size khác nhau để test batch.

### CRUD cho cả tám loại

Thay `{type}` bằng `categories`, `brands`, `materials`, `styles`, `collars`,
`origins`, `colors` hoặc `sizes`:

| Method | URL | Kết quả |
| --- | --- | --- |
| GET | {{baseUrl}}/product-attributes/{type}?page=0&size=10&keyword=&status=1 | 200, trang dữ liệu |
| GET | {{baseUrl}}/product-attributes/{type}/{id} | 200, một DTO; không có ID → 404 |
| POST | {{baseUrl}}/product-attributes/{type} | 201, DTO có id |
| PUT | {{baseUrl}}/product-attributes/{type}/{id} | 200, DTO sau sửa |
| PATCH | {{baseUrl}}/product-attributes/{type}/{id}/status | 200, DTO sau đổi trạng thái |

Không truyền `status` thì danh sách trả cả trạng thái để giữ API admin hiện có.
GET `{{baseUrl}}/product-attributes/options` vẫn được giữ để tương thích.
Không có DELETE thuộc tính.

POST/PUT cho sáu loại `categories`, `brands`, `materials`, `styles`, `collars`,
`origins` dùng format thống nhất `ma`, `ten`, `ghiChu`, `trangThai`.
Ví dụ tạo danh mục:

```json
{
  "ma": "DM_PM_{{runKey}}",
  "ten": "Danh mục Postman {{runKey}}",
  "ghiChu": "Mô tả danh mục",
  "trangThai": 1
}
```

Tạo từng loại khác bằng cùng format, đổi mã/tên phù hợp. `ghiChu` của danh mục
map vào `moTa`; sáu bảng thuộc tính đơn giản không có audit date nên không thêm field.
`ghiChu` không được lưu ở những bảng không có cột tương ứng.

POST/PUT màu, chấp nhận tên field entity thực tế:

```json
{
  "maMauSac": "MS_PM_{{runKey}}",
  "tenMauSac": "Màu Postman {{runKey}}",
  "maMauHex": "#112233",
  "trangThai": 1
}
```

POST/PUT size: `giaTri` là **String**, không ép về Integer:

```json
{
  "giaTri": "40.5",
  "ghiChu": "Kích thước nửa số",
  "trangThai": 1
}
```

Nếu size 40.5 đã có thì lấy ID từ GET, không tạo trùng. Tạo size thứ hai, ví dụ
"41", để lưu vào `sizeId2`. Mã/giá trị/tên trùng bị trả 409 theo kiểm tra service.
Request cũng chấp nhận `maDanhMuc/tenDanhMuc/moTa`, `maThuongHieu/tenThuongHieu`,
`maChatLieu/tenChatLieu`, `maKieuDang/tenKieuDang`, `maCoGiay/tenCoGiay`,
`maXuatXu/tenXuatXu`. **Response thuộc tính giữ format chung**:
`id`, `ma`, `ten`, `ghiChu`, `maMauHex`, `trangThai`.

Lưu ID sau POST trong tab Scripts → Post-response. Đổi tên biến tùy loại:

```javascript
pm.test("Tạo thuộc tính thành công", () => pm.response.to.have.status(201));
pm.environment.set("categoryId", pm.response.json().id);
```

PATCH status dùng body `{"trangThai":0}` hoặc `{"trangThai":1}`.
Thực hiện test tắt thuộc tính sau khi tạo sản phẩm/biến thể hoặc bật lại trước
khi dùng làm reference mới. Liên kết cũ vẫn được giữ khi thuộc tính bị tắt.

## 3. CREATE sản phẩm

POST `{{baseUrl}}/products`:

```json
{
  "maSanPham": "SP_TEST_{{runKey}}",
  "tenSanPham": "Sản phẩm test Postman",
  "danhMucId": {{categoryId}},
  "thuongHieuId": {{brandId}},
  "chatLieuId": {{materialId}},
  "kieuDangId": {{styleId}},
  "coGiayId": {{collarId}},
  "xuatXuId": {{originId}},
  "moTaChiTiet": "Test CRUD sản phẩm",
  "trangThai": 1
}
```

Kỳ vọng **201**, response ProductResponse có `id`, mã/tên/ID và tên thuộc tính,
`ngayTao`, `tongSoLuong=0`, `soBienThe=0`, `tongSoBienThe=0`;
giá min/max và ảnh chính null khi chưa có dữ liệu. Mã/tên được trim.

Scripts → Post-response:

```javascript
pm.test("Tạo sản phẩm", () => pm.response.to.have.status(201));
pm.environment.set("productId", pm.response.json().id);
```

## 4. READ LIST và READ DETAIL sản phẩm

GET `{{baseUrl}}/products?page=0&size=10&keyword=SP_TEST_{{runKey}}&status=1`.

Filter đầy đủ: `categoryId`, `brandId`, `materialId`, `styleId`, `collarId`,
`originId`, `status`. Keyword tìm mã hoặc tên sản phẩm, không phân biệt hoa/thường.
Tìm kiếm, lọc và phân trang được thực hiện tại database.

GET `{{baseUrl}}/products/{{productId}}` trả:

```json
{
  "product": { "id": 123, "maSanPham": "...", "tongSoLuong": 0, "soBienThe": 0 },
  "variants": [],
  "images": []
}
```

123 chỉ minh họa response; dùng `productId` thực tế. ProductResponse còn có
`giaThapNhat`, `giaCaoNhat`, `soMau`, `soKichThuoc`, `anhChinh`.
`soBienThe` và `tongSoBienThe` là cùng số lượng, giữ cả hai tên để tương thích.
Aggregate tính trên tất cả biến thể, kể cả biến thể ngừng hoạt động.
GET ID không tồn tại trả 404 với message tiếng Việt.

## 5. UPDATE sản phẩm

PUT `{{baseUrl}}/products/{{productId}}`:

```json
{
  "tenSanPham": "Tên sản phẩm đã sửa Postman",
  "danhMucId": {{categoryId}},
  "thuongHieuId": {{brandId}},
  "chatLieuId": {{materialId}},
  "kieuDangId": {{styleId}},
  "coGiayId": {{collarId}},
  "xuatXuId": {{originId}},
  "moTaChiTiet": "Mô tả đã cập nhật",
  "trangThai": 1
}
```

Kỳ vọng **200**, có `ngayCapNhat`, mã sản phẩm giữ nguyên, không mất biến thể.
Không cần gửi `maSanPham`. Nếu gửi, phải giống mã cũ; đổi mã trả 400.
PUT yêu cầu đầy đủ các field sửa bắt buộc; không phải API partial update.

## 6. CREATE biến thể đơn hoặc batch

POST `{{baseUrl}}/products/{{productId}}/variants` với một object:

```json
{
  "mauSacId": {{colorId}},
  "kichThuocId": {{sizeId}},
  "maChiTietSanPham": "CTSP_TEST_{{runKey}}",
  "sku": "SKU_TEST_{{runKey}}",
  "soLuong": 10,
  "giaBan": 1200000,
  "kichHoat": true,
  "trangThai": 1
}
```

Kỳ vọng **201**, một VariantResponse có `id`, `sanPhamId`, `productId`, mã/tên
sản phẩm, mã/tên/HEX màu, ID/giá trị size và giá/tồn/kích hoạt/trạng thái.
`sanPhamId`, `productId`, `idSanPham` trong response là cùng ID.
`maChiTietSanPham` và `sku` trim, kiểm tra trùng không phân biệt hoa/thường.

```javascript
pm.test("Tạo biến thể", () => pm.response.to.have.status(201));
pm.environment.set("variantId", pm.response.json().id);
```

Tạo thêm batch bằng cùng URL, dùng size thứ hai để tránh trùng tổ hợp:

```json
{
  "variants": [
    {
      "mauSacId": {{colorId}},
      "kichThuocId": {{sizeId2}},
      "maChiTietSanPham": "CTSP_BATCH_{{runKey}}",
      "sku": "SKU_BATCH_{{runKey}}",
      "soLuong": 5,
      "giaBan": 1500000,
      "kichHoat": true,
      "trangThai": 1
    }
  ]
}
```

Kỳ vọng **201**, response là **array**. Batch phải có 1–200 dòng, không có phần tử
null. Tất cả dòng nằm trong một transaction; lỗi một dòng thì rollback toàn batch.
Nested endpoint lấy ID sản phẩm từ path nên không bắt buộc `sanPhamId` trong body.
Nếu vẫn gửi ID trong body, phải khớp path. Batch cũ của frontend vẫn được hỗ trợ.

POST `{{baseUrl}}/product-details` cũng tạo một biến thể, body như object đơn
nhưng **phải thêm** `"sanPhamId": {{productId}}` và dùng mã/SKU/tổ hợp chưa có.
ID request chấp nhận alias `productId`/`idSanPham`, `idMauSac`, `idKichThuoc`.

## 7. READ LIST, READ DETAIL biến thể và kiểm tra aggregate

GET `{{baseUrl}}/products/{{productId}}/variants`: array tất cả biến thể của sản phẩm.

GET `{{baseUrl}}/product-details/{{variantId}}`: một VariantResponse.

GET `{{baseUrl}}/product-details?page=0&size=10&productId={{productId}}&active=true`:
PageResponse. Filter: `productId`, `colorId`, `sizeId`, `status`, `active`.
Keyword tìm mã CTSP, SKU, mã sản phẩm hoặc tên sản phẩm.
`active` nhận true/false và lọc `kichHoat`; độc lập với `status` lọc `trangThai`.

Đọc lại GET sản phẩm sau hai request tạo biến thể ở trên:

- `tongSoLuong=15`, `soBienThe=2`.
- `giaThapNhat=1200000`, `giaCaoNhat=1500000`.
- `soMau=1`, `soKichThuoc=2`.

Các số này áp dụng cho sản phẩm vừa tạo chỉ có đúng hai biến thể mẫu nêu trên.

## 8. UPDATE biến thể

PUT `{{baseUrl}}/product-details/{{variantId}}`:

```json
{
  "mauSacId": {{colorId}},
  "kichThuocId": {{sizeId}},
  "sku": "SKU_TEST_{{runKey}}",
  "soLuong": 12,
  "giaBan": 1250000,
  "kichHoat": false,
  "trangThai": 1
}
```

Kỳ vọng **200**, có `ngayCapNhat`; SKU/tổ hợp hiện tại không bị tính là trùng với
chính record đang sửa. Không cần gửi `sanPhamId` hoặc `maChiTietSanPham`;
hai field này được giữ khi bỏ khỏi request. Có thể sửa mã CTSP theo nghiệp vụ
đã có, nhưng vẫn phải hợp lệ và không trùng. Không chuyển biến thể sang sản phẩm khác.
Đổi màu/size phải dùng thuộc tính đang hoạt động hoặc giữ liên kết cũ.
Đọc lại GET detail và GET list với `active=false` để xác nhận dữ liệu đã lưu.

## 9. STATUS sản phẩm, biến thể và thuộc tính

PATCH `{{baseUrl}}/products/{{productId}}/status`:

```json
{ "trangThai": 0 }
```

PATCH `{{baseUrl}}/product-details/{{variantId}}/status`: cùng body.
PATCH `{{baseUrl}}/product-attributes/colors/{{colorId}}/status`: cùng body.

Kỳ vọng **200** và `trangThai=0`. Gửi 1 để bật lại.
Chỉ nhận 0/1; status không thay đổi `kichHoat` của biến thể.
Không hard delete sản phẩm, biến thể hoặc thuộc tính.
Thuộc tính bị tắt vẫn giữ liên kết cũ nhưng bị từ chối khi tạo liên kết mới.

## 10. IMAGE metadata theo schema thật

File SQL thực tế của checkout là `sql_sd013.sql`, không có `Database/01_sqlSD13.sql`.
Bảng `hinh_anh_san_pham` và entity thật chỉ có:
`id`, `id_san_pham`, `url_anh`, `is_anh_chinh`.
**Không có `id_mau_sac`**, nên ảnh thuộc sản phẩm, dùng chung cho các biến thể.
Không gửi `mauSacId`/`idMauSac`; nếu có giá trị sẽ bị trả 400 với field error.
Không upload file, không tích hợp storage ngoài, không lưu Base64.

GET `{{baseUrl}}/products/{{productId}}/images`: array ảnh; chưa có ảnh trả `[]`.

POST `{{baseUrl}}/products/{{productId}}/images`:

```json
{
  "urlAnh": "https://example.com/product-postman.jpg",
  "isAnhChinh": true
}
```

Kỳ vọng **201** với `id`, `urlAnh`, `isAnhChinh`. URL này minh họa metadata;
thay bằng URL ảnh HTTP/HTTPS thực tế để hiển thị được ảnh trong frontend.
Backend không tải URL từ mạng để kiểm tra nội dung ảnh.

```javascript
pm.test("Thêm metadata ảnh", () => pm.response.to.have.status(201));
pm.environment.set("imageId", pm.response.json().id);
```

PUT `{{baseUrl}}/product-images/{{imageId}}`:

```json
{
  "urlAnh": "https://example.com/product-postman-updated.jpg",
  "isAnhChinh": true
}
```

Kỳ vọng **200**. URL phải hợp lệ, HTTP/HTTPS, có host, không chứa user/password;
tối đa 1000 ký tự. `data:image/...;base64,...` bị từ chối.

Quy tắc ảnh chính: ảnh đầu tiên tự là ảnh chính dù gửi false; chọn một ảnh mới
là chính thì bỏ cờ chính của ảnh khác. PUT false lên ảnh đang là chính vẫn giữ
ảnh đó là chính; muốn đổi thì PUT true lên ảnh khác.

DELETE `{{baseUrl}}/product-images/{{imageId}}`: **204**, chỉ gỡ metadata ảnh.
Nếu xóa ảnh chính, ảnh còn lại đầu tiên theo ID được chọn làm chính;
nếu không còn ảnh thì `anhChinh=null`. Sản phẩm và biến thể giữ nguyên.

Hai endpoint cũ vẫn được giữ:

- PUT {{baseUrl}}/products/{{productId}}/images/{{imageId}}
- DELETE {{baseUrl}}/products/{{productId}}/images/{{imageId}}

GET detail/list sản phẩm và biến thể để kiểm tra `anhChinh` sau thao tác ảnh.

## 11. Negative cases cần test

| Thao tác | Kỳ vọng |
| --- | --- |
| POST product giữ nguyên mã vừa tạo | 409, mã sản phẩm đã tồn tại |
| POST product thiếu/để trống tên hoặc mã | 400, errors chỉ rõ field |
| GET product ID 9223372036854775807 | 404, Không tìm thấy sản phẩm |
| PUT product gửi mã khác mã cũ | 400, mã không được thay đổi |
| POST product dùng danhMucId 9223372036854775807 | 400, danh mục không tồn tại |
| POST variant giaBan = 0 | 400, field giaBan |
| POST variant soLuong = -1 | 400, field soLuong |
| POST variant đổi mã CTSP nhưng giữ SKU đã có | 409, SKU đã tồn tại |
| POST variant giữ mã CTSP đã có, đổi SKU | 409, mã biến thể đã tồn tại |
| POST variant mã/SKU mới nhưng cùng product + color + size đã có | 409, trùng tổ hợp |
| POST variant màu/size không tồn tại | 400, message rõ thuộc tính không tồn tại |
| POST /product-details không gửi sanPhamId | 400, errors.sanPhamId |
| Nested POST variant gửi sanPhamId khác path | 400, biến thể phải thuộc sản phẩm đang chọn |
| POST batch variants rỗng hoặc chứa null | 400 |
| GET variant/attribute ID không tồn tại | 404, message tương ứng |
| POST attribute trùng mã hoặc tên/giá trị | 409 |
| POST/PUT color HEX khác #RRGGBB | 400 |
| PATCH status = 2 hoặc null | 400 |
| GET list page=-1 hoặc size=101 | 400 |
| GET variants active=invalid | 400 |
| POST image URL Base64 hoặc mauSacId có giá trị | 400 |
| PUT /product-images/9223372036854775807, body URL hợp lệ | 404, Không tìm thấy ảnh sản phẩm |
| DELETE product | 405, không có API hard delete sản phẩm |

Khi test SKU/tổ hợp trùng, dùng mã CTSP mới để không bị chặn trước bởi kiểm tra
mã CTSP. Khi test FK, dùng mã/SKU mới để không bị chặn trước bởi kiểm tra trùng.

Response validation mẫu:

```json
{
  "message": "Dữ liệu không hợp lệ. Hãy kiểm tra các trường nhập.",
  "errors": { "tenSanPham": "Tên sản phẩm không được trống" }
}
```

Nested variant có field path như `variants[0].soLuong`.
Lỗi nghiệp vụ có format `{"message":"..."}`. Không trả entity JPA/proxy.
Checkout chưa có common AppException/ApiResponse/GlobalExceptionHandler;
module giữ DTO và ProductExceptionHandler hiện có, không đổi response frontend.

## 12. Database và transaction

- Không đổi SQL, schema, entity, migration, constraint hoặc cấu hình kết nối.
- SQL hiện có không khai báo UNIQUE cho mã/tổ hợp; service kiểm tra trùng và
  khóa sản phẩm khi ghi biến thể/ảnh. Không thêm constraint để xử lý task.
- Chưa bảo đảm cạnh tranh nhiều instance cho mã/SKU toàn hệ thống khi schema
  không có UNIQUE; thao tác SQL trực tiếp cũng có thể bỏ qua validation service.
- Tạo sản phẩm và tạo biến thể là hai API riêng. Sản phẩm đã lưu vẫn tồn tại nếu
  batch biến thể thất bại; sửa lỗi rồi thử lại batch, không tạo lại sản phẩm.
- Test Maven sử dụng SQL Server thật, transaction rollback, không lưu seed lâu dài.
- Request Postman tạo/sửa là thao tác ghi thật và không tự rollback; giữ lại
  dữ liệu test hoặc dùng PATCH trạng thái 0 khi kết thúc. Không DELETE sản phẩm.

## 13. Danh sách endpoint chính

| Method | Endpoint |
| --- | --- |
| POST / GET | /api/products |
| GET / PUT | /api/products/{id} |
| PATCH | /api/products/{id}/status |
| POST / GET | /api/products/{id}/variants |
| POST / GET | /api/product-details |
| GET / PUT | /api/product-details/{id} |
| PATCH | /api/product-details/{id}/status |
| GET / POST | /api/products/{id}/images |
| PUT / DELETE | /api/product-images/{id} |
| PUT / DELETE | /api/products/{id}/images/{imageId} |
| GET / POST | /api/product-attributes/{type} |
| GET / PUT | /api/product-attributes/{type}/{id} |
| PATCH | /api/product-attributes/{type}/{id}/status |
| GET | /api/product-attributes/options |

## 14. Kết quả kiểm tra local ngày 03/10/2026

- `.\mvnw.cmd clean compile`: **BUILD SUCCESS**, Java 17, 63 file nguồn.
- `.\mvnw.cmd test`: **BUILD SUCCESS**, 29 test; 0 failure, 0 error, 0 skipped.
- Có 11 integration test trên SQL Server thật, kiểm tra HTTP CRUD, FK,
  aggregate, field alias, DTO update, batch, filter active, ảnh và validation.
  Dữ liệu test rollback; không sửa test của module khác.
- Spring Boot: **SUCCESS**, `Started DatnApplication`, Tomcat cổng **18080**.
  Lần chạy 8080 bị xung đột cổng với tiến trình có sẵn; không dừng tiến trình đó
  hoặc sửa `application.properties`.
- HTTP thực tế GET products, product-details và cả tám thuộc tính: **200**.
  API detail/ảnh/biến thể với ID không tồn tại trả **404** theo service.
- SQL Server hiện có 0 sản phẩm, 0 biến thể và không có thuộc tính hoạt động.
  Tạo reference qua POST thuộc tính trước khi test sản phẩm bằng Postman.
- Không có PropertyReferenceException, QueryCreationException, JPQL validation
  error, BeanCreationException hoặc ambiguous mapping trong các lần chạy thành công.
- Database/schema/SQL/entity/dependency/config và frontend không thay đổi trong lượt này.
- **POSTMAN READY: YES**, baseUrl `http://localhost:18080/api`.

## 15. File của lượt backend này

Đường dẫn bên dưới tính từ `D:\DATN_SD13\DATN_SMASHSTEP_SD13`.
Các file module đã có từ lượt trước được giữ; không tạo lại module.

### FILES CREATED (2)

- BACKEND/src/main/java/com/smashstep/datn/product/controller/ProductImageController.java
- POSTMAN_PRODUCT_TEST.md

### FILES MODIFIED (10)

- BACKEND/src/main/java/com/smashstep/datn/product/controller/ProductController.java
- BACKEND/src/main/java/com/smashstep/datn/product/controller/ProductVariantController.java
- BACKEND/src/main/java/com/smashstep/datn/product/controller/ProductAttributeController.java
- BACKEND/src/main/java/com/smashstep/datn/product/controller/ProductExceptionHandler.java
- BACKEND/src/main/java/com/smashstep/datn/product/dto/ProductDtos.java
- BACKEND/src/main/java/com/smashstep/datn/product/repository/HinhAnhSanPhamRepository.java
- BACKEND/src/main/java/com/smashstep/datn/product/service/ProductService.java
- BACKEND/src/main/java/com/smashstep/datn/product/service/VariantService.java
- BACKEND/src/main/java/com/smashstep/datn/product/service/ProductAttributeService.java
- BACKEND/src/test/java/com/smashstep/datn/product/ProductDatabaseIntegrationTest.java

### Phần còn lại

Không còn lỗi compile/test/start của code product trong các kiểm tra trên.
Ảnh chỉ hỗ trợ URL dùng chung cho sản phẩm theo schema hiện tại; upload file
và ảnh theo màu chưa có. Không thêm schema/storage để thực hiện hai phần này.
