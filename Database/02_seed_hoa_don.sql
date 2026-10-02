/* =========================================================
   SEED HÓA ĐƠN  -  module: Hóa đơn (Vũ Chí Tuấn Anh)
   Chạy SAU 01_sqlSD13.sql, chỉ trên máy mình.
   - Có thể chạy lại nhiều lần: dòng nào có rồi thì bỏ qua (kiểm tra theo mã).
   - Nhân viên (NV001..003) và khách hàng (KH001..010) chỉ là dữ liệu TẠM để
     hóa đơn có chỗ tham chiếu. Khi module Nhân viên / Khách hàng có seed riêng
     thì dùng đúng các mã này (hoặc xóa khối 2 và 3 bên dưới).
   - Chưa seed hoa_don_chi_tiet vì cần san_pham_chi_tiet (module Sản phẩm).
   - Giá trị INT theo khối F trong 01_sqlSD13.sql:
       loai_hoa_don: 0 Tại quầy | 1 Trực tuyến | 2 Giao hàng
       trang_thai  : 0 Chờ xác nhận | 3 Đang giao | 4 Đã giao | 5 Hoàn thành | 6 Đã hủy ...
   ========================================================= */
USE SmashStep;
GO
SET NOCOUNT ON;
GO

/* ---------- 1. Hình thức + phương thức thanh toán ---------- */
IF NOT EXISTS (SELECT 1 FROM hinh_thuc_thanh_toan WHERE ma_hinh_thuc = 'TRUC_TIEP')
    INSERT INTO hinh_thuc_thanh_toan (ma_hinh_thuc, ten_hinh_thuc, trang_thai) VALUES ('TRUC_TIEP', N'Trực tiếp', 1);
IF NOT EXISTS (SELECT 1 FROM hinh_thuc_thanh_toan WHERE ma_hinh_thuc = 'TRUC_TUYEN')
    INSERT INTO hinh_thuc_thanh_toan (ma_hinh_thuc, ten_hinh_thuc, trang_thai) VALUES ('TRUC_TUYEN', N'Trực tuyến', 1);
GO

INSERT INTO phuong_thuc_thanh_toan (id_hinh_thuc_thanh_toan, ma_phuong_thuc, ten_phuong_thuc, trang_thai)
SELECT ht.id, v.ma, v.ten, 1
FROM (VALUES ('TIEN_MAT',     N'Tiền mặt',     'TRUC_TIEP'),
             ('THE',          N'Thẻ',          'TRUC_TIEP'),
             ('CHUYEN_KHOAN', N'Chuyển khoản', 'TRUC_TUYEN'),
             ('COD',          N'COD',          'TRUC_TUYEN')) AS v(ma, ten, ma_hinh_thuc)
JOIN hinh_thuc_thanh_toan ht ON ht.ma_hinh_thuc = v.ma_hinh_thuc
WHERE NOT EXISTS (SELECT 1 FROM phuong_thuc_thanh_toan p WHERE p.ma_phuong_thuc = v.ma);
GO

/* ---------- 2. Nhân viên tạm (mật khẩu để trống) ---------- */
IF NOT EXISTS (SELECT 1 FROM vai_tro WHERE ten_vai_tro = N'Nhân viên')
    INSERT INTO vai_tro (ten_vai_tro, mo_ta, trang_thai) VALUES (N'Nhân viên', N'Nhân viên bán hàng', 1);
GO

INSERT INTO nhan_vien (id_vai_tro, ma_nhan_vien, ten_dang_nhap, ten_nhan_vien, email, so_dien_thoai, trang_thai, ngay_tao, ngay_cap_nhat)
SELECT (SELECT TOP 1 id FROM vai_tro WHERE ten_vai_tro = N'Nhân viên'), v.ma, v.tdn, v.ten, v.email, v.sdt, 1, SYSDATETIME(), SYSDATETIME()
FROM (VALUES
    ('NV001', 'nguyenvana', N'Nguyễn Văn A', 'nguyenvana@smashstep.local', '0900000001'),
    ('NV002', 'khanhha', N'Khánh Hà', 'khanhha@smashstep.local', '0900000002'),
    ('NV003', 'tranhuy', N'Trần Huy', 'tranhuy@smashstep.local', '0900000003')
     ) AS v(ma, tdn, ten, email, sdt)
WHERE NOT EXISTS (SELECT 1 FROM nhan_vien n WHERE n.ma_nhan_vien = v.ma
                                          OR n.ten_dang_nhap = v.tdn
                                          OR n.email = v.email);
GO

/* ---------- 3. Khách hàng tạm ---------- */
INSERT INTO khach_hang (ma_khach_hang, ten_tai_khoan, ten_khach_hang, so_dien_thoai, trang_thai, ngay_tao, ngay_cap_nhat)
SELECT v.ma, LOWER(v.ma), v.ten, v.sdt, 1, SYSDATETIME(), SYSDATETIME()
FROM (VALUES
    ('KH001', N'Trần Minh Bảo Hoàng', '0909899999'),
    ('KH002', N'Nguyễn Thị An', '0911111111'),
    ('KH003', N'Lê Quốc Hưng', '0911111111'),
    ('KH004', N'Phạm Thanh Tú', '0983214567'),
    ('KH005', N'Võ Gia Hân', '0905882114'),
    ('KH006', N'Đặng Hoài Nam', '0934625881'),
    ('KH007', N'Bùi Mỹ Linh', '0972230456'),
    ('KH008', N'Ngô Đức Anh', '0902718663'),
    ('KH009', N'Đỗ Phương Vy', '0968440127'),
    ('KH010', N'Mai Tiến Thành', '0918305902')
     ) AS v(ma, ten, sdt)
WHERE NOT EXISTS (SELECT 1 FROM khach_hang k WHERE k.ma_khach_hang = v.ma OR k.ten_tai_khoan = LOWER(v.ma));
GO

/* ---------- 4. Hóa đơn ----------
   tong_tien  = tiền hàng
   thanh_tien = tong_tien - tien_giam_gia + phi_van_chuyen   (khớp cách FE tính)  */
DECLARE @hd TABLE (
    ma VARCHAR(50), ma_kh VARCHAR(50), ma_nv VARCHAR(50), ma_pt VARCHAR(50),
    loai INT, tong DECIMAL(18,2), giam DECIMAL(18,2), ship DECIMAL(18,2), trang_thai INT,
    nguoi_nhan NVARCHAR(255), sdt VARCHAR(20), dia_chi NVARCHAR(500), ngay DATETIME2
);

INSERT INTO @hd VALUES
    ('HD000081', 'KH001', 'NV002', 'TIEN_MAT', 0, 2200000, 0, 0, 5, N'Trần Minh Bảo Hoàng', '0909899999', N'123 Nguyễn Trãi, Thanh Xuân, Hà Nội', '2026-09-26T09:15:00'),
    ('HD000080', 'KH002', 'NV001', 'CHUYEN_KHOAN', 1, 2370000, 100000, 30000, 5, N'Nguyễn Thị An', '0911111111', N'Hai Bà Trưng, Hà Nội', '2026-09-26T10:40:00'),
    ('HD000079', 'KH003', 'NV001', 'COD', 2, 2070000, 0, 30000, 3, N'Lê Quốc Hưng', '0911111111', N'Cầu Giấy, Hà Nội', '2026-09-25T14:05:00'),
    ('HD000078', 'KH004', 'NV002', 'CHUYEN_KHOAN', 1, 1920000, 100000, 30000, 4, N'Phạm Thanh Tú', '0983214567', N'Nam Từ Liêm, Hà Nội', '2026-09-25T16:20:00'),
    ('HD000077', 'KH005', 'NV003', 'TIEN_MAT', 0, 3420000, 0, 0, 0, N'Võ Gia Hân', '0905882114', N'Hà Nội', '2026-09-24T08:30:00'),
    ('HD000076', 'KH006', 'NV002', 'CHUYEN_KHOAN', 1, 1260000, 0, 30000, 6, N'Đặng Hoài Nam', '0934625881', N'Hà Nội', '2026-09-24T11:10:00'),
    ('HD000075', 'KH007', 'NV003', 'COD', 2, 2620000, 0, 30000, 4, N'Bùi Mỹ Linh', '0972230456', N'Thanh Xuân, Hà Nội', '2026-09-23T15:45:00'),
    ('HD000074', 'KH008', 'NV001', 'THE', 0, 1760000, 0, 0, 5, N'Ngô Đức Anh', '0902718663', N'Hà Nội', '2026-09-23T09:50:00'),
    ('HD000073', 'KH009', 'NV003', 'CHUYEN_KHOAN', 1, 2950000, 0, 30000, 0, N'Đỗ Phương Vy', '0968440127', N'Hà Nội', '2026-09-22T13:25:00'),
    ('HD000072', 'KH010', 'NV002', 'COD', 2, 4120000, 0, 30000, 3, N'Mai Tiến Thành', '0918305902', N'Hoàng Mai, Hà Nội', '2026-09-22T17:00:00'),
    ('HD000071', 'KH001', 'NV001', 'TIEN_MAT', 0, 1590000, 0, 0, 5, N'Trần Minh Bảo Hoàng', '0909899999', N'123 Nguyễn Trãi, Thanh Xuân, Hà Nội', '2026-09-21T10:00:00'),
    ('HD000070', 'KH003', 'NV002', 'CHUYEN_KHOAN', 1, 2890000, 150000, 30000, 5, N'Lê Quốc Hưng', '0911111111', N'Cầu Giấy, Hà Nội', '2026-09-21T15:30:00'),
    ('HD000069', 'KH005', 'NV003', 'COD', 2, 1980000, 0, 30000, 6, N'Võ Gia Hân', '0905882114', N'Hà Nội', '2026-09-20T09:10:00'),
    ('HD000068', 'KH007', 'NV001', 'THE', 0, 3150000, 0, 0, 5, N'Bùi Mỹ Linh', '0972230456', N'Thanh Xuân, Hà Nội', '2026-09-20T12:45:00'),
    ('HD000067', 'KH009', 'NV002', 'CHUYEN_KHOAN', 1, 2240000, 0, 30000, 5, N'Đỗ Phương Vy', '0968440127', N'Hà Nội', '2026-09-19T16:15:00');

INSERT INTO hoa_don (id_khach_hang, id_nhan_vien, id_phuong_thuc_thanh_toan, ma_hoa_don, loai_hoa_don,
                     tong_tien, phi_van_chuyen, tien_giam_gia, thanh_tien,
                     ho_ten_nguoi_nhan, so_dien_thoai_nguoi_nhan, dia_chi_giao_hang,
                     ngay_thanh_toan, ngay_tao, ngay_cap_nhat, trang_thai)
SELECT kh.id, nv.id, pt.id, h.ma, h.loai,
       h.tong, h.ship, h.giam, h.tong - h.giam + h.ship,
       h.nguoi_nhan, h.sdt, h.dia_chi,
       CASE WHEN h.trang_thai = 5 THEN h.ngay ELSE NULL END, h.ngay, h.ngay, h.trang_thai
FROM @hd h
LEFT JOIN khach_hang kh ON kh.ma_khach_hang = h.ma_kh
LEFT JOIN nhan_vien nv ON nv.ma_nhan_vien = h.ma_nv
LEFT JOIN phuong_thuc_thanh_toan pt ON pt.ma_phuong_thuc = h.ma_pt
WHERE NOT EXISTS (SELECT 1 FROM hoa_don x WHERE x.ma_hoa_don = h.ma);
GO

/* ---------- 5. Lịch sử hóa đơn (dùng cho timeline ở ngày 3) ----------
   Mỗi hóa đơn có 1 dòng "Tạo hóa đơn" (trạng thái 0) và, nếu đã đổi trạng thái, 1 dòng trạng thái hiện tại. */
INSERT INTO lich_su_hoa_don (id_hoa_don, nguoi_tao, trang_thai, ghi_chu, ngay_tao)
SELECT hd.id, hd.id_nhan_vien, 0, N'Tạo hóa đơn', hd.ngay_tao
FROM hoa_don hd
WHERE hd.ma_hoa_don LIKE 'HD0000%'
  AND NOT EXISTS (SELECT 1 FROM lich_su_hoa_don l WHERE l.id_hoa_don = hd.id AND l.trang_thai = 0);
GO

INSERT INTO lich_su_hoa_don (id_hoa_don, nguoi_tao, trang_thai, ghi_chu, ngay_tao)
SELECT hd.id, hd.id_nhan_vien, hd.trang_thai, N'Cập nhật trạng thái', DATEADD(HOUR, 2, hd.ngay_tao)
FROM hoa_don hd
WHERE hd.ma_hoa_don LIKE 'HD0000%' AND hd.trang_thai <> 0
  AND NOT EXISTS (SELECT 1 FROM lich_su_hoa_don l WHERE l.id_hoa_don = hd.id AND l.trang_thai = hd.trang_thai);
GO

/* Kiểm tra nhanh */
SELECT TOP 20 ma_hoa_don, loai_hoa_don, trang_thai, tong_tien, tien_giam_gia, phi_van_chuyen, thanh_tien, ngay_tao
FROM hoa_don ORDER BY ngay_tao DESC, id DESC;
GO