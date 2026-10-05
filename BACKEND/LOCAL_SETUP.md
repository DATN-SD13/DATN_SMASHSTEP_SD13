# Chạy backend local

Backend dùng SQL Server `localhost:1433`, database `SmashStep`, port HTTP `8080`.
`spring.jpa.hibernate.ddl-auto=none`: ứng dụng không tạo hoặc sửa schema.

## Credential

Config chung dùng `DB_USERNAME` (mặc định `sa`) và `DB_PASSWORD`.
Đặt các biến này trong terminal hoặc Environment variables của Run Configuration IntelliJ.

Máy local cũng có thể dùng file riêng:
`src/main/resources/application-local.properties`.
File được import qua classpath nên hoạt động với Maven và IntelliJ ở các working directory khác nhau.
File này bị Git bỏ qua và không được commit/push.

Nội dung mẫu, thay password mẫu bằng password SQL Server đã xác nhận:

```properties
spring.datasource.username=${DB_USERNAME:sa}
spring.datasource.password=${DB_PASSWORD:your-local-sql-password}
spring.jpa.mapping-resources=META-INF/orm-local-smashstep.xml
```

Biến môi trường được ưu tiên hơn fallback trong file local.
Không commit credential vào config chung, Java source hoặc log.
Khi đóng gói artifact để chia sẻ/deploy, bỏ file local khỏi classpath và cung cấp biến môi trường.

## Chạy và kiểm tra

```powershell
.\mvnw.cmd clean compile
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Chỉ chạy một backend trên port 8080. Trước khi dừng process đang giữ port,
đối chiếu PID và command line để chắc chắn đó là backend dev của project này.

API kiểm tra:

- `/api/health`
- `/api/products?page=0&size=10`
- `/api/product-attributes/categories?page=0&size=10`
- `/api/product-attributes/brands?page=0&size=10`

## Mapping DB hiện tại

DB local không có các cột `hinh_anh_san_pham.id_mau_sac`,
`phieu_giam_gia.hinh_thuc_phieu`, `dot_giam_gia.mo_ta`,
`lich_su_thanh_toan.id_phuong_thuc_thanh_toan`,
`hoa_don.tien_giam_gia`, `hoa_don.dia_chi_giao_hang`.

Annotation Entity của các module hóa đơn/giảm giá trên main được giữ nguyên.
File `META-INF/orm-local-smashstep.xml` chỉ được bật khi local config có
`spring.jpa.mapping-resources=META-INF/orm-local-smashstep.xml`.
Nó ghi đè mapping của 5 field chưa có cột trong DB local thành transient:
giữ field/getter/setter, không đọc/ghi các cột chưa tồn tại.
Entity ảnh tiếp tục giữ mapping tương thích DB hiện tại từ merge product trước đó.

Nếu dùng DB có đầy đủ các cột trong schema tham chiếu của team,
bỏ property `spring.jpa.mapping-resources` trong file local để dùng annotation Entity gốc.
Không ánh xạ địa chỉ dạng String vào cột số `gia_chi_giao_hang`.
Không thay đổi file SQL hoặc schema để xử lý sai lệch này.
Test `EntityMappingTichHopTest` đọc tất cả Entity bằng SQL Server thật để phát hiện mapping sai.
