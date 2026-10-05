USE SmashStep;
GO
SET NOCOUNT ON;
GO

/* =========================================================
   03_seed_test_sanpham_hoadon.sql
   Mục đích:
   - BỔ SUNG dữ liệu test sản phẩm + biến thể + hóa đơn chi tiết.
   - KHÔNG DELETE dữ liệu.
   - KHÔNG DBCC CHECKIDENT / RESEED.
   - KHÔNG sửa/xóa 15 hóa đơn hiện có.
   - Có thể chạy lại: dữ liệu đã có theo mã sẽ được bỏ qua.
   ========================================================= */

BEGIN TRY
    BEGIN TRANSACTION;

    /* =====================================================
       1. THUỘC TÍNH SẢN PHẨM
       ===================================================== */

    INSERT INTO danh_muc (ma_danh_muc, ten_danh_muc, mo_ta, trang_thai)
    SELECT v.ma, v.ten, v.mo_ta, 1
    FROM (VALUES
        ('DM001', N'Giày chạy bộ', N'Giày dành cho chạy bộ và luyện tập'),
        ('DM002', N'Giày thể thao', N'Giày thể thao sử dụng hàng ngày'),
        ('DM003', N'Giày bóng rổ', N'Giày chuyên dụng cho bóng rổ'),
        ('DM004', N'Giày thời trang', N'Giày sneaker và thời trang đường phố'),
        ('DM005', N'Giày đá bóng', N'Giày sử dụng khi chơi bóng đá'),
        ('DM006', N'Giày tennis', N'Giày dành cho tennis và thể thao sân'),
        ('DM007', N'Giày đi bộ', N'Giày nhẹ dùng để đi bộ hàng ngày'),
        ('DM008', N'Giày tập gym', N'Giày phục vụ tập luyện và fitness')
    ) v(ma, ten, mo_ta)
    WHERE NOT EXISTS (
        SELECT 1 FROM danh_muc x WHERE x.ma_danh_muc = v.ma
    );

    INSERT INTO thuong_hieu (ma_thuong_hieu, ten_thuong_hieu, trang_thai)
    SELECT v.ma, v.ten, 1
    FROM (VALUES
        ('TH001', N'Nike'),
        ('TH002', N'Adidas'),
        ('TH003', N'Puma'),
        ('TH004', N'New Balance'),
        ('TH005', N'Converse'),
        ('TH006', N'Vans'),
        ('TH007', N'ASICS'),
        ('TH008', N'Under Armour'),
        ('TH009', N'Reebok'),
        ('TH010', N'Skechers')
    ) v(ma, ten)
    WHERE NOT EXISTS (
        SELECT 1 FROM thuong_hieu x WHERE x.ma_thuong_hieu = v.ma
    );

    INSERT INTO chat_lieu (ma_chat_lieu, ten_chat_lieu, trang_thai)
    SELECT v.ma, v.ten, 1
    FROM (VALUES
        ('CL001', N'Vải Mesh'),
        ('CL002', N'Da tổng hợp'),
        ('CL003', N'Da thật'),
        ('CL004', N'Vải Canvas'),
        ('CL005', N'Vải Knit'),
        ('CL006', N'Da lộn'),
        ('CL007', N'Polyester'),
        ('CL008', N'Cao su'),
        ('CL009', N'Vải dệt'),
        ('CL010', N'Sợi tổng hợp')
    ) v(ma, ten)
    WHERE NOT EXISTS (
        SELECT 1 FROM chat_lieu x WHERE x.ma_chat_lieu = v.ma
    );

    INSERT INTO xuat_xu (ma_xuat_xu, ten_xuat_xu, trang_thai)
    SELECT v.ma, v.ten, 1
    FROM (VALUES
        ('XX001', N'Việt Nam'),
        ('XX002', N'Indonesia'),
        ('XX003', N'Trung Quốc'),
        ('XX004', N'Mỹ'),
        ('XX005', N'Nhật Bản'),
        ('XX006', N'Hàn Quốc'),
        ('XX007', N'Thái Lan'),
        ('XX008', N'Đức')
    ) v(ma, ten)
    WHERE NOT EXISTS (
        SELECT 1 FROM xuat_xu x WHERE x.ma_xuat_xu = v.ma
    );

    INSERT INTO co_giay (ma_co_giay, ten_co_giay, trang_thai)
    SELECT v.ma, v.ten, 1
    FROM (VALUES
        ('CG001', N'Cổ thấp'),
        ('CG002', N'Cổ trung'),
        ('CG003', N'Cổ cao')
    ) v(ma, ten)
    WHERE NOT EXISTS (
        SELECT 1 FROM co_giay x WHERE x.ma_co_giay = v.ma
    );

    INSERT INTO kieu_dang (ma_kieu_dang, ten_kieu_dang, trang_thai)
    SELECT v.ma, v.ten, 1
    FROM (VALUES
        ('KD001', N'Running'),
        ('KD002', N'Sneaker'),
        ('KD003', N'Basketball'),
        ('KD004', N'Casual'),
        ('KD005', N'Training'),
        ('KD006', N'Football'),
        ('KD007', N'Tennis'),
        ('KD008', N'Walking'),
        ('KD009', N'Skate'),
        ('KD010', N'Lifestyle')
    ) v(ma, ten)
    WHERE NOT EXISTS (
        SELECT 1 FROM kieu_dang x WHERE x.ma_kieu_dang = v.ma
    );

    INSERT INTO mau_sac
        (ma_mau_sac, ten_mau_sac, ma_mau_hex, trang_thai, ngay_tao, ngay_cap_nhat)
    SELECT v.ma, v.ten, v.hex, 1, SYSDATETIME(), SYSDATETIME()
    FROM (VALUES
        ('MS001', N'Đen',        '#000000'),
        ('MS002', N'Trắng',      '#FFFFFF'),
        ('MS003', N'Đỏ',         '#FF0000'),
        ('MS004', N'Xanh dương', '#0066FF'),
        ('MS005', N'Xám',        '#808080'),
        ('MS006', N'Xanh lá',    '#00A651'),
        ('MS007', N'Be',         '#F5F5DC'),
        ('MS008', N'Nâu',        '#8B4513'),
        ('MS009', N'Vàng',        '#FFD700'),
        ('MS010', N'Cam',         '#FF8C00'),
        ('MS011', N'Hồng',        '#FF69B4'),
        ('MS012', N'Tím',         '#800080'),
        ('MS013', N'Xanh navy',   '#000080'),
        ('MS014', N'Kem',         '#FFFDD0'),
        ('MS015', N'Bạc',         '#C0C0C0')
    ) v(ma, ten, hex)
    WHERE NOT EXISTS (
        SELECT 1 FROM mau_sac x WHERE x.ma_mau_sac = v.ma
    );

    INSERT INTO kich_thuoc
        (gia_tri, ghi_chu, trang_thai, ngay_tao, ngay_cap_nhat)
    SELECT v.gia_tri, CONCAT(N'Size ', v.gia_tri), 1, SYSDATETIME(), SYSDATETIME()
    FROM (VALUES
        ('35'), ('36'), ('36.5'), ('37'), ('37.5'), ('38'), ('38.5'),
        ('39'), ('39.5'), ('40'), ('40.5'), ('41'), ('41.5'), ('42'),
        ('42.5'), ('43'), ('43.5'), ('44'), ('44.5'), ('45'), ('46')
    ) v(gia_tri)
    WHERE NOT EXISTS (
        SELECT 1 FROM kich_thuoc x WHERE x.gia_tri = v.gia_tri
    );

    /* =====================================================
       2. SẢN PHẨM
       Dùng mã sản phẩm làm khóa nghiệp vụ, không phụ thuộc ID.
       ===================================================== */

    INSERT INTO san_pham
        (id_danh_muc, id_thuong_hieu, id_chat_lieu, id_kieu_dang,
         id_co_giay, id_xuat_xu, ma_san_pham, ten_san_pham,
         mo_ta_chi_tiet, ngay_tao, nguoi_tao, nguoi_cap_nhat,
         ngay_cap_nhat, trang_thai)
    SELECT
        dm.id, th.id, cl.id, kd.id, cg.id, xx.id,
        v.ma_san_pham, v.ten_san_pham, v.mo_ta,
        SYSDATETIME(), NULL, NULL, SYSDATETIME(), 1
    FROM (VALUES
        ('SP001', 'DM001', 'TH001', 'CL001', 'KD001', 'CG001', 'XX002',
         N'Nike Air Zoom Pegasus 41',
         N'Giày chạy bộ Nike Pegasus 41'),

        ('SP002', 'DM002', 'TH002', 'CL005', 'KD002', 'CG001', 'XX002',
         N'Adidas Ultraboost Light',
         N'Giày thể thao Adidas Ultraboost Light'),

        ('SP003', 'DM003', 'TH001', 'CL002', 'KD003', 'CG003', 'XX002',
         N'Nike Air Jordan One Take',
         N'Giày bóng rổ Nike Air Jordan'),

        ('SP004', 'DM004', 'TH005', 'CL004', 'KD004', 'CG001', 'XX001',
         N'Converse Chuck 70',
         N'Giày sneaker Converse Chuck 70'),

        ('SP005', 'DM005', 'TH003', 'CL002', 'KD006', 'CG001', 'XX003',
         N'Puma Future Play',
         N'Giày đá bóng Puma Future Play'),

        ('SP006', 'DM006', 'TH007', 'CL001', 'KD007', 'CG001', 'XX005',
         N'ASICS Gel-Resolution',
         N'Giày tennis ASICS Gel-Resolution')
    ) v(ma_san_pham, ma_dm, ma_th, ma_cl, ma_kd, ma_cg, ma_xx,
        ten_san_pham, mo_ta)
    JOIN danh_muc dm ON dm.ma_danh_muc = v.ma_dm
    JOIN thuong_hieu th ON th.ma_thuong_hieu = v.ma_th
    JOIN chat_lieu cl ON cl.ma_chat_lieu = v.ma_cl
    JOIN kieu_dang kd ON kd.ma_kieu_dang = v.ma_kd
    JOIN co_giay cg ON cg.ma_co_giay = v.ma_cg
    JOIN xuat_xu xx ON xx.ma_xuat_xu = v.ma_xx
    WHERE NOT EXISTS (
        SELECT 1 FROM san_pham x WHERE x.ma_san_pham = v.ma_san_pham
    );

    /* =====================================================
       3. BIẾN THỂ SẢN PHẨM
       Mỗi mã chi tiết/SKU là duy nhất.
       ===================================================== */

    INSERT INTO san_pham_chi_tiet
        (id_san_pham, id_mau_sac, id_kich_thuoc,
         ma_chi_tiet_san_pham, so_luong, gia_ban, sku,
         kich_hoat, ngay_tao, ngay_cap_nhat, trang_thai)
    SELECT
        sp.id, ms.id, kt.id,
        v.ma_ct, v.so_luong, v.gia_ban, v.sku,
        1, SYSDATETIME(), SYSDATETIME(), 1
    FROM (VALUES
        ('SP001','MS001','40','SPCT001',20,2200000,'SKU-SP001-B-40'),
        ('SP001','MS001','41','SPCT002',15,2200000,'SKU-SP001-B-41'),
        ('SP001','MS002','42','SPCT003',12,2250000,'SKU-SP001-T-42'),

        ('SP002','MS002','40','SPCT004',18,2370000,'SKU-SP002-T-40'),
        ('SP002','MS005','41','SPCT005',10,2370000,'SKU-SP002-X-41'),
        ('SP002','MS001','42','SPCT006',8,2400000,'SKU-SP002-B-42'),

        ('SP003','MS001','41','SPCT007',10,2070000,'SKU-SP003-B-41'),
        ('SP003','MS003','42','SPCT008',8,2070000,'SKU-SP003-D-42'),

        ('SP004','MS002','39','SPCT009',14,1760000,'SKU-SP004-T-39'),
        ('SP004','MS001','40','SPCT010',12,1760000,'SKU-SP004-B-40'),

        ('SP005','MS003','41','SPCT011',16,1980000,'SKU-SP005-D-41'),
        ('SP005','MS004','42','SPCT012',10,1980000,'SKU-SP005-X-42'),

        ('SP006','MS004','40','SPCT013',9,2620000,'SKU-SP006-X-40'),
        ('SP006','MS005','41','SPCT014',7,2620000,'SKU-SP006-X-41')
    ) v(ma_sp, ma_ms, gia_tri_size, ma_ct, so_luong, gia_ban, sku)
    JOIN san_pham sp ON sp.ma_san_pham = v.ma_sp
    JOIN mau_sac ms ON ms.ma_mau_sac = v.ma_ms
    JOIN kich_thuoc kt ON kt.gia_tri = v.gia_tri_size
    WHERE NOT EXISTS (
        SELECT 1
        FROM san_pham_chi_tiet x
        WHERE x.ma_chi_tiet_san_pham = v.ma_ct
    );

    /* =====================================================
       4. HÌNH ẢNH TEST
       Không bắt buộc cho hóa đơn, nhưng để màn sản phẩm có ảnh.
       ===================================================== */

    INSERT INTO hinh_anh_san_pham (id_san_pham, id_mau_sac, url_anh, is_anh_chinh)
    SELECT sp.id, ms.id, v.url_anh, 1
    FROM (VALUES
        ('SP001','MS001','https://example.com/san-pham/sp001-den.jpg'),
        ('SP002','MS002','https://example.com/san-pham/sp002-trang.jpg'),
        ('SP003','MS001','https://example.com/san-pham/sp003-den.jpg'),
        ('SP004','MS002','https://example.com/san-pham/sp004-trang.jpg'),
        ('SP005','MS003','https://example.com/san-pham/sp005-do.jpg'),
        ('SP006','MS004','https://example.com/san-pham/sp006-xanh.jpg')
    ) v(ma_sp, ma_ms, url_anh)
    JOIN san_pham sp ON sp.ma_san_pham = v.ma_sp
    JOIN mau_sac ms ON ms.ma_mau_sac = v.ma_ms
    WHERE NOT EXISTS (
        SELECT 1
        FROM hinh_anh_san_pham x
        WHERE x.id_san_pham = sp.id
          AND x.id_mau_sac = ms.id
          AND x.url_anh = v.url_anh
    );

    /* =====================================================
       5. CHI TIẾT HÓA ĐƠN
       Không tạo hóa đơn mới.
       Gắn sản phẩm vào 15 hóa đơn HD000067..HD000081 đã có.
       Dùng mã hóa đơn + mã biến thể, không phụ thuộc ID.
       ===================================================== */

    INSERT INTO hoa_don_chi_tiet
        (id_hoa_don, id_san_pham_chi_tiet, so_luong,
         don_gia, thanh_tien, ghi_chu, trang_thai)
    SELECT
        hd.id,
        spct.id,
        v.so_luong,
        spct.gia_ban,
        v.so_luong * spct.gia_ban,
        NULL,
        1
    FROM (VALUES
        ('HD000081','SPCT001',1),
        ('HD000081','SPCT002',1),

        ('HD000080','SPCT004',1),
        ('HD000080','SPCT005',1),

        ('HD000079','SPCT007',1),
        ('HD000079','SPCT008',1),

        ('HD000078','SPCT009',1),
        ('HD000078','SPCT010',1),

        ('HD000077','SPCT011',1),
        ('HD000077','SPCT012',1),

        ('HD000076','SPCT013',1),
        ('HD000076','SPCT014',1),

        ('HD000075','SPCT001',1),
        ('HD000075','SPCT004',1),

        ('HD000074','SPCT009',1),
        ('HD000074','SPCT006',1),

        ('HD000073','SPCT002',1),
        ('HD000073','SPCT007',1),

        ('HD000072','SPCT003',1),
        ('HD000072','SPCT005',1),

        ('HD000071','SPCT001',1),
        ('HD000071','SPCT010',1),

        ('HD000070','SPCT004',1),
        ('HD000070','SPCT012',1),

        ('HD000069','SPCT008',1),
        ('HD000069','SPCT011',1),

        ('HD000068','SPCT003',1),
        ('HD000068','SPCT009',1),

        ('HD000067','SPCT005',1),
        ('HD000067','SPCT013',1)
    ) v(ma_hd, ma_ct, so_luong)
    JOIN hoa_don hd ON hd.ma_hoa_don = v.ma_hd
    JOIN san_pham_chi_tiet spct ON spct.ma_chi_tiet_san_pham = v.ma_ct
    WHERE NOT EXISTS (
        SELECT 1
        FROM hoa_don_chi_tiet x
        WHERE x.id_hoa_don = hd.id
          AND x.id_san_pham_chi_tiet = spct.id
    );

    COMMIT TRANSACTION;

    PRINT N'==============================================';
    PRINT N'SEED SẢN PHẨM + HÓA ĐƠN CHI TIẾT THÀNH CÔNG';
    PRINT N'Không xóa dữ liệu hóa đơn hiện có.';
    PRINT N'==============================================';

END TRY
BEGIN CATCH

    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;

    PRINT N'CÓ LỖI - ĐÃ ROLLBACK TOÀN BỘ THAY ĐỔI CỦA FILE 03';
    THROW;

END CATCH;
GO

/* =========================================================
   6. KIỂM TRA
   ========================================================= */

SELECT COUNT(*) AS DanhMuc FROM danh_muc;
SELECT COUNT(*) AS ThuongHieu FROM thuong_hieu;
SELECT COUNT(*) AS ChatLieu FROM chat_lieu;
SELECT COUNT(*) AS XuatXu FROM xuat_xu;
SELECT COUNT(*) AS CoGiay FROM co_giay;
SELECT COUNT(*) AS KieuDang FROM kieu_dang;
SELECT COUNT(*) AS MauSac FROM mau_sac;
SELECT COUNT(*) AS KichThuoc FROM kich_thuoc;
SELECT COUNT(*) AS SanPham FROM san_pham;
SELECT COUNT(*) AS SanPhamChiTiet FROM san_pham_chi_tiet;
SELECT COUNT(*) AS HinhAnhSanPham FROM hinh_anh_san_pham;
SELECT COUNT(*) AS HoaDon FROM hoa_don;
SELECT COUNT(*) AS HoaDonChiTiet FROM hoa_don_chi_tiet;

SELECT
    hd.ma_hoa_don,
    hd.trang_thai,
    sp.ma_san_pham,
    sp.ten_san_pham,
    spct.ma_chi_tiet_san_pham,
    spct.sku,
    hdct.so_luong,
    hdct.don_gia,
    hdct.thanh_tien
FROM hoa_don_chi_tiet hdct
JOIN hoa_don hd ON hd.id = hdct.id_hoa_don
JOIN san_pham_chi_tiet spct ON spct.id = hdct.id_san_pham_chi_tiet
JOIN san_pham sp ON sp.id = spct.id_san_pham
ORDER BY hd.ngay_tao DESC, hd.id DESC, hdct.id;
GO
