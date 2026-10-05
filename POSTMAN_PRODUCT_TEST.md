# Test backend quản lý sản phẩm bằng Postman

Project: `D:\DATN_SD13\DATN_SMASHSTEP_SD13`, branch `quang`.
Backend: Java 17, Spring Boot 4.0.0, Spring Data JPA, Jakarta Validation,
Lombok, SQL Server; dùng Maven Wrapper hiện có.

**BASE URL:** `http://localhost:8080`

API prefix: `/api`. Trong Postman đặt `baseUrl=http://localhost:8080/api`.

## 1. Chuẩn bị

Chạy trong PowerShell:

```powershell
cd D:\DATN_SD13\DATN_SMASHSTEP_SD13\BACKEND
.\mvnw.cmd clean compile
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Chờ log `Started DatnApplication` và Tomcat nghe cổng 8080.
Lần kiểm tra hiện tại khởi động thành công trên cổng mặc định 8080,
không sửa `application.properties`.
SQL Server phải chạy và cấu hình kết nối hiện có phải hợp lệ. Không đổi password
trong code hoặc chạy lại SQL để test API. `spring.jpa.hibernate.ddl-auto=none`.

Tạo Postman Environment, chọn environment trước khi gửi request:

| Biến | Giá trị |
| --- | --- |
| baseUrl | http://localhost:8080/api |
| runKey | Chuỗi khác nhau cho mỗi lượt, ví dụ 20261003_1600 |
| categoryId, brandId, materialId, styleId, collarId, originId | ID lấy từ API thuộc tính |
| colorId, sizeId, sizeId2 | ID màu và hai kích thước khác nhau lấy từ API |
| productId, variantId, imageId | ID từ response POST tương ứng |

Mọi request POST/PUT/PATCH: Body → raw → JSON,
header `Content-Type: application/json`. Các biến ID không đặt trong dấu nháy;
Postman thay `{{categoryId}}` bằng số trước khi gửi. Không gửi ID chưa được gán.
Không mặc định ID bằng 1: database hiện có thể chưa có dữ liệu reference.

Module chưa có user context xác thực; không gửi ID nhân viên giả cho audit.


### Thứ tự test Postman

1. GET thuộc tính và lấy ID thật; POST thuộc tính nếu danh sách trống.
2. POST sản phẩm, lưu `productId`.
3. GET danh sách sản phẩm.
4. GET chi tiết sản phẩm.
5. PUT sản phẩm.
6. PATCH trạng thái sản phẩm; có thể bật lại bằng trạng thái 1.
7. POST biến thể, lưu `variantId`.
8. GET danh sách biến thể.
9. GET chi tiết biến thể.
10. PUT biến thể.
11. PATCH trạng thái biến thể.
12. GET/POST ảnh, lưu `imageId`, rồi PUT ảnh.
13. Chạy các negative test ở mục 11.

Các phần bên dưới ghi đầy đủ URL, JSON và mã HTTP kỳ vọng cho từng bước.

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
"41", để lưu vào `sizeId2`. Trùng mã của bảy loại thuộc tính hoặc trùng `giaTri`
kích thước trả 409. Tên thuộc tính giống nhau nhưng mã khác nhau vẫn được phép.
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

Kỳ vọng **201**, response SanPhamResponse có `id`, mã/tên/ID và tên thuộc tính,
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

123 chỉ minh họa response; dùng `productId` thực tế. SanPhamResponse còn có
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

Kỳ vọng **201**, một BienTheResponse có `id`, `sanPhamId`, `productId`, mã/tên
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

GET `{{baseUrl}}/product-details/{{variantId}}`: một BienTheResponse.

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
| POST product dùng danhMucId 9223372036854775807 | 404, Không tìm thấy danh mục |
| POST variant giaBan = 0 | 400, field giaBan |
| POST variant soLuong = -1 | 400, field soLuong |
| POST variant đổi mã CTSP nhưng giữ SKU đã có | 409, SKU đã tồn tại |
| POST variant giữ mã CTSP đã có, đổi SKU | 409, mã biến thể đã tồn tại |
| POST variant mã/SKU mới nhưng cùng product + color + size đã có | 409, trùng tổ hợp |
| POST variant màu/size không tồn tại | 404, Không tìm thấy màu sắc/kích thước |
| POST variant dùng sanPhamId không tồn tại | 404, Không tìm thấy sản phẩm |
| POST /product-details không gửi sanPhamId | 400, errors.sanPhamId |
| Nested POST variant gửi sanPhamId khác path | 400, biến thể phải thuộc sản phẩm đang chọn |
| POST batch variants rỗng hoặc chứa null | 400 |
| GET variant/attribute ID không tồn tại | 404, message tương ứng |
| POST/PUT attribute trùng mã hoặc size trùng giaTri | 409 |
| POST attribute có tên đã tồn tại nhưng mã mới | 201 |
| PUT attribute ID không tồn tại, kể cả gửi mã đã có | 404 |
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
Đã chuyển exception và handler cũ sang `common/exception/AppException.java`
và `GlobalExceptionHandler.java`; `PageResponse` được tách sang `common/response`.
Module dùng chung các class này, không tạo exception riêng cho từng nghiệp vụ.
Checkout trước lượt này không có package common hoặc `ApiResponse`; response
JSON hiện có được giữ để frontend không cần sửa. Phân trang vẫn bắt đầu từ 0.

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

- `.\mvnw.cmd clean compile`: **BUILD SUCCESS**, Java 17, **76 file nguồn**.
- `.\mvnw.cmd test`: **BUILD SUCCESS**, **31 test**, 0 failure, 0 error, 0 skipped.
- Có **13 integration test trên SQL Server thật**: HTTP CRUD, FK trả 404,
  kiểm tra trùng đúng mã/giá trị thuộc tính, aggregate, field alias, batch,
  filter active, ảnh và validation. Các test ghi dữ liệu được rollback;
  không sửa test của module khác.
- Spring Boot: **SUCCESS**, log `Started DatnApplication`, Tomcat cổng **8080**.
  Process chạy bằng code mới trong local checkout này.
- Kiểm tra HTTP thực tế trên 8080: danh sách products, product-details và cả
  tám thuộc tính trả **200**. ID sản phẩm/biến thể/thuộc tính không tồn tại
  trả **404**; page âm và active không hợp lệ trả **400**.
- Không có lỗi PropertyReferenceException, QueryCreationException, JPQL
  validation, BeanCreationException hoặc ambiguous mapping khi khởi động.
- `git diff -- Database/01_sqlSD13.sql` và `git diff -- sql_sd013.sql`
  không có output. File SQL thực tế là `sql_sd013.sql`.
- Đối chiếu SHA256 trước/sau: SQL, entity, dependency, Maven wrapper,
  cấu hình kết nối, test của module khác và frontend giữ nguyên trong lượt này.
  Một số file đã có thay đổi local từ trước; các thay đổi đó được giữ nguyên.
- **DATABASE / SCHEMA MODIFIED BY THIS TASK: NO**.
- **POSTMAN GUIDE: EXISTS; POSTMAN READY: YES**.

## 15. File thực tế của lượt này

Root checkout: `D:\DATN_SD13\DATN_SMASHSTEP_SD13`, branch `quang`.
Danh sách dưới đây chỉ tính thay đổi của lượt dọn lại tên tiếng Việt và hoàn thiện
backend; code đã hoàn thành từ các lượt trước được tái sử dụng.

### FILES CREATED (1)

- [PageResponse.java](D:/DATN_SD13/DATN_SMASHSTEP_SD13/BACKEND/src/main/java/com/smashstep/datn/common/response/PageResponse.java)

Tách class phân trang đang dùng từ DTO cũ sang common; giữ JSON
`content`, `number`, `size`, `totalElements`, `totalPages` và page bắt đầu 0.

### FILES RENAMED (23)

Các file bên phải tồn tại thật và chứa logic; import/reference đã cập nhật.

| File trước | File hiện tại |
| --- | --- |
| BACKEND/src/main/java/com/smashstep/datn/product/controller/ProductAttributeController.java | [ThuocTinhSanPhamController.java](D:/DATN_SD13/DATN_SMASHSTEP_SD13/BACKEND/src/main/java/com/smashstep/datn/product/controller/ThuocTinhSanPhamController.java) |
| BACKEND/src/main/java/com/smashstep/datn/product/controller/ProductExceptionHandler.java | [GlobalExceptionHandler.java](D:/DATN_SD13/DATN_SMASHSTEP_SD13/BACKEND/src/main/java/com/smashstep/datn/common/exception/GlobalExceptionHandler.java) |
| BACKEND/src/main/java/com/smashstep/datn/product/dto/ImageRequest.java | [HinhAnhSanPhamRequest.java](D:/DATN_SD13/DATN_SMASHSTEP_SD13/BACKEND/src/main/java/com/smashstep/datn/product/dto/HinhAnhSanPhamRequest.java) |
| BACKEND/src/main/java/com/smashstep/datn/product/dto/ImageResponse.java | [HinhAnhSanPhamResponse.java](D:/DATN_SD13/DATN_SMASHSTEP_SD13/BACKEND/src/main/java/com/smashstep/datn/product/dto/HinhAnhSanPhamResponse.java) |
| BACKEND/src/main/java/com/smashstep/datn/product/dto/ProductCreateRequest.java | [SanPhamThemRequest.java](D:/DATN_SD13/DATN_SMASHSTEP_SD13/BACKEND/src/main/java/com/smashstep/datn/product/dto/SanPhamThemRequest.java) |
| BACKEND/src/main/java/com/smashstep/datn/product/dto/ProductDetailResponse.java | [SanPhamChiTietResponse.java](D:/DATN_SD13/DATN_SMASHSTEP_SD13/BACKEND/src/main/java/com/smashstep/datn/product/dto/SanPhamChiTietResponse.java) |
| BACKEND/src/main/java/com/smashstep/datn/product/dto/ProductDtos.java | [DuLieuSanPham.java](D:/DATN_SD13/DATN_SMASHSTEP_SD13/BACKEND/src/main/java/com/smashstep/datn/product/dto/DuLieuSanPham.java) |
| BACKEND/src/main/java/com/smashstep/datn/product/dto/ProductResponse.java | [SanPhamResponse.java](D:/DATN_SD13/DATN_SMASHSTEP_SD13/BACKEND/src/main/java/com/smashstep/datn/product/dto/SanPhamResponse.java) |
| BACKEND/src/main/java/com/smashstep/datn/product/dto/ProductStatusRequest.java | [SanPhamTrangThaiRequest.java](D:/DATN_SD13/DATN_SMASHSTEP_SD13/BACKEND/src/main/java/com/smashstep/datn/product/dto/SanPhamTrangThaiRequest.java) |
| BACKEND/src/main/java/com/smashstep/datn/product/dto/ProductUpdateRequest.java | [SanPhamSuaRequest.java](D:/DATN_SD13/DATN_SMASHSTEP_SD13/BACKEND/src/main/java/com/smashstep/datn/product/dto/SanPhamSuaRequest.java) |
| BACKEND/src/main/java/com/smashstep/datn/product/dto/VariantCreateRequest.java | [SanPhamChiTietThemRequest.java](D:/DATN_SD13/DATN_SMASHSTEP_SD13/BACKEND/src/main/java/com/smashstep/datn/product/dto/SanPhamChiTietThemRequest.java) |
| BACKEND/src/main/java/com/smashstep/datn/product/dto/VariantResponse.java | [BienTheResponse.java](D:/DATN_SD13/DATN_SMASHSTEP_SD13/BACKEND/src/main/java/com/smashstep/datn/product/dto/BienTheResponse.java) |
| BACKEND/src/main/java/com/smashstep/datn/product/dto/VariantStatusRequest.java | [SanPhamChiTietTrangThaiRequest.java](D:/DATN_SD13/DATN_SMASHSTEP_SD13/BACKEND/src/main/java/com/smashstep/datn/product/dto/SanPhamChiTietTrangThaiRequest.java) |
| BACKEND/src/main/java/com/smashstep/datn/product/dto/VariantUpdateRequest.java | [SanPhamChiTietSuaRequest.java](D:/DATN_SD13/DATN_SMASHSTEP_SD13/BACKEND/src/main/java/com/smashstep/datn/product/dto/SanPhamChiTietSuaRequest.java) |
| BACKEND/src/main/java/com/smashstep/datn/product/repository/InventorySummary.java | [TongHopSanPham.java](D:/DATN_SD13/DATN_SMASHSTEP_SD13/BACKEND/src/main/java/com/smashstep/datn/product/repository/TongHopSanPham.java) |
| BACKEND/src/main/java/com/smashstep/datn/product/service/ProductAttributeService.java | [ThuocTinhSanPhamService.java](D:/DATN_SD13/DATN_SMASHSTEP_SD13/BACKEND/src/main/java/com/smashstep/datn/product/service/ThuocTinhSanPhamService.java) |
| BACKEND/src/main/java/com/smashstep/datn/product/service/ProductException.java | [AppException.java](D:/DATN_SD13/DATN_SMASHSTEP_SD13/BACKEND/src/main/java/com/smashstep/datn/common/exception/AppException.java) |
| BACKEND/src/main/java/com/smashstep/datn/product/service/ProductRules.java | [QuyTacSanPham.java](D:/DATN_SD13/DATN_SMASHSTEP_SD13/BACKEND/src/main/java/com/smashstep/datn/product/service/QuyTacSanPham.java) |
| BACKEND/src/test/java/com/smashstep/datn/product/ProductAttributeServiceTest.java | [ThuocTinhSanPhamServiceTest.java](D:/DATN_SD13/DATN_SMASHSTEP_SD13/BACKEND/src/test/java/com/smashstep/datn/product/ThuocTinhSanPhamServiceTest.java) |
| BACKEND/src/test/java/com/smashstep/datn/product/ProductDatabaseIntegrationTest.java | [SanPhamTichHopTest.java](D:/DATN_SD13/DATN_SMASHSTEP_SD13/BACKEND/src/test/java/com/smashstep/datn/product/SanPhamTichHopTest.java) |
| BACKEND/src/test/java/com/smashstep/datn/product/ProductServiceTest.java | [SanPhamServiceTest.java](D:/DATN_SD13/DATN_SMASHSTEP_SD13/BACKEND/src/test/java/com/smashstep/datn/product/SanPhamServiceTest.java) |
| BACKEND/src/test/java/com/smashstep/datn/product/ProductValidationTest.java | [KiemTraDuLieuSanPhamTest.java](D:/DATN_SD13/DATN_SMASHSTEP_SD13/BACKEND/src/test/java/com/smashstep/datn/product/KiemTraDuLieuSanPhamTest.java) |
| BACKEND/src/test/java/com/smashstep/datn/product/VariantServiceTest.java | [SanPhamChiTietServiceTest.java](D:/DATN_SD13/DATN_SMASHSTEP_SD13/BACKEND/src/test/java/com/smashstep/datn/product/SanPhamChiTietServiceTest.java) |

Hai file exception chuyển sang common theo tên yêu cầu `AppException` và
`GlobalExceptionHandler`. Handler vẫn áp dụng cho module product để không đổi
cách xử lý lỗi của module khác. Không tồn tại hai bộ exception song song.

### FILES MODIFIED (10, giữ nguyên đường dẫn)

- [SanPhamController.java](D:/DATN_SD13/DATN_SMASHSTEP_SD13/BACKEND/src/main/java/com/smashstep/datn/product/controller/SanPhamController.java)
- [SanPhamChiTietController.java](D:/DATN_SD13/DATN_SMASHSTEP_SD13/BACKEND/src/main/java/com/smashstep/datn/product/controller/SanPhamChiTietController.java)
- [HinhAnhSanPhamController.java](D:/DATN_SD13/DATN_SMASHSTEP_SD13/BACKEND/src/main/java/com/smashstep/datn/product/controller/HinhAnhSanPhamController.java)
- [SanPhamService.java](D:/DATN_SD13/DATN_SMASHSTEP_SD13/BACKEND/src/main/java/com/smashstep/datn/product/service/SanPhamService.java)
- [SanPhamChiTietService.java](D:/DATN_SD13/DATN_SMASHSTEP_SD13/BACKEND/src/main/java/com/smashstep/datn/product/service/SanPhamChiTietService.java)
- [HinhAnhSanPhamService.java](D:/DATN_SD13/DATN_SMASHSTEP_SD13/BACKEND/src/main/java/com/smashstep/datn/product/service/HinhAnhSanPhamService.java)
- [SanPhamRepository.java](D:/DATN_SD13/DATN_SMASHSTEP_SD13/BACKEND/src/main/java/com/smashstep/datn/product/repository/SanPhamRepository.java)
- [SanPhamChiTietRepository.java](D:/DATN_SD13/DATN_SMASHSTEP_SD13/BACKEND/src/main/java/com/smashstep/datn/product/repository/SanPhamChiTietRepository.java)
- [HinhAnhSanPhamRepository.java](D:/DATN_SD13/DATN_SMASHSTEP_SD13/BACKEND/src/main/java/com/smashstep/datn/product/repository/HinhAnhSanPhamRepository.java)
- [POSTMAN_PRODUCT_TEST.md](D:/DATN_SD13/DATN_SMASHSTEP_SD13/POSTMAN_PRODUCT_TEST.md)

Các file đã đổi tên và sửa nội dung nằm ở mục RENAMED, không tính lặp lại ở đây.
Repository dùng đúng Java property của Entity. Projection `TongHopSanPham`
khớp alias của query tổng hợp và đã được kiểm tra bằng integration test.

### FILES DELETED

**0 file xóa độc lập.** Các đường dẫn tên cũ đã chuyển sang tên mới ở bảng trên.
Đã bỏ một DTO batch lồng không còn được dùng; API batch vẫn hoạt động qua
`DuLieuSanPham.DanhSachBienTheRequest`.

### Controller, service và DTO đã tồn tại trên ổ đĩa

| Folder | File chính |
| --- | --- |
| `BACKEND/src/main/java/com/smashstep/datn/product/controller` | SanPhamController, SanPhamChiTietController, ThuocTinhSanPhamController, HinhAnhSanPhamController |
| `BACKEND/src/main/java/com/smashstep/datn/product/service` | SanPhamService, SanPhamChiTietService, ThuocTinhSanPhamService, HinhAnhSanPhamService, QuyTacSanPham |
| `BACKEND/src/main/java/com/smashstep/datn/product/dto` | 11 DTO sản phẩm/biến thể/ảnh và DuLieuSanPham |

DTO dùng class Java thông thường, Jakarta Validation, không dùng record.
`SanPhamChiTietResponse` chứa product/variants/images;
`BienTheResponse` chứa thông tin một biến thể. Cấu trúc JSON không đổi.

`SanPhamService`: themSanPham, layDanhSachSanPham, layChiTietSanPham,
suaSanPham, doiTrangThai.

`SanPhamChiTietService`: themSanPhamChiTiet, themDanhSachBienThe,
layDanhSach, layTheoSanPham, layChiTiet, suaSanPhamChiTiet, doiTrangThai.

`ThuocTinhSanPhamService`: layLuaChon, layDanhSach, layChiTiet,
luuThuocTinh, doiTrangThai; dùng chung cho tám loại thuộc tính.

`HinhAnhSanPhamService`: themAnh, layDanhSachAnh, suaAnhTheoId, xoaAnhTheoId;
giữ các endpoint ảnh theo sản phẩm và quy tắc một ảnh chính.

### Checklist

| Hạng mục | Kết quả |
| --- | --- |
| product/dto | EXISTS |
| product/service | EXISTS |
| product/controller | EXISTS |
| PRODUCT create/list/detail/update/status | DONE |
| BIẾN THỂ create/list/detail/update/status và batch | DONE |
| THUỘC TÍNH GET/create/update/status tám loại | DONE |
| ẢNH GET/POST/PUT/DELETE | DONE |
| COMPILE | BUILD SUCCESS |
| TEST | 31 passed |
| SPRING BOOT START | SUCCESS, 8080 |
| DATABASE / SCHEMA MODIFIED BY THIS TASK | NO |
| FRONTEND MODIFIED BY THIS TASK | NO |
| POSTMAN READY | YES |
| Tên file/class mới trong module product | Tiếng Việt không dấu |

Không còn file Java tên Product*, Variant*, Image* hoặc Inventory* trong module
product và các test product trên ổ đĩa. Các đường dẫn cũ vẫn có thể xuất hiện
trong `git status` vì index có thay đổi từ trước; chúng không còn trong source.
Lượt này không chạy git add, commit, push, checkout branch hoặc tạo worktree.

Không còn lỗi compile/test/start trong các kiểm tra trên. Ảnh lưu URL theo sản
phẩm đúng Entity/SQL thực tế; không thêm cột màu hoặc storage ngoài.
