# SmashStep Customer Storefront

Ứng dụng khách hàng độc lập: Vue 3, Vite, Vue Router, Axios.

## Chạy local

Yêu cầu Node tương thích Vite 7 (cùng runtime với FRONTEND hiện tại), JDK 17 và BACKEND đang chạy tại localhost:8080.

```sh
npm ci
npm run dev
```

Mở http://localhost:5174. Admin FRONTEND tiếp tục dùng 5173.

```sh
npm test
npm run build
```

## Dữ liệu và giá

- Public GET /api/store/products, /products/{id}, /filters, /brands.
- Chỉ sản phẩm đang kinh doanh, biến thể active/kích hoạt và giá gốc hợp lệ. Biến thể tồn kho 0 vẫn có thể xem, hiển thị Hết hàng.
- Search tên/mã, checkbox nhiều lựa chọn, màu + size + giá cùng một biến thể; page bắt đầu từ 0 trên API, tối đa 48 card/request.
- Giá từ là giá hiện hành thấp nhất trong các biến thể phù hợp; giá gốc, badge và phần trăm thuộc đúng biến thể đại diện đó. Chọn màu/size sẽ hiển thị giá/tồn kho/ảnh riêng.
- Giá luôn từ CurrentPriceService hiện có; không thêm công thức campaign ở frontend.
- Nổi bật ưu tiên có ảnh, đang giảm giá rồi mới nhất, hoàn toàn suy ra từ dữ liệu hiện có. Không có cột featured.
- Tự refresh sau 30 giây và khi quay lại tab; hủy request cũ/timer khi rời trang.
- Backend xử lý giá trong batch 250 biến thể để sort/filter giá hiện hành nhất quán; ảnh cả trang được lấy chung. Không gọi detail API cho từng card.
- Không có rating/sold count/checkout/cart giả. Backend hiện chỉ có checkout POS, không có cart/order khách hàng phù hợp; không tạo bảng mới.

## Database và admin

Không sửa Entity/schema/SQL/seed; storefront là read-only. Giữ nguyên toàn bộ source FRONTEND admin. Security chỉ bổ sung permit GET /api/store/**; các API admin giữ chính sách cũ.

Proxy /api và /uploads đến backend dùng chung; không hardcode server trong các component. Chỉ dùng logo gốc và ảnh thật do backend trả về.
