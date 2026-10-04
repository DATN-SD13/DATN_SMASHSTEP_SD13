USE SmashStep;
GO

BEGIN TRY
    BEGIN TRANSACTION;

    /* =====================================================
       1. XÓA DỮ LIỆU PHỤ THUỘC SẢN PHẨM
       ===================================================== */

    DELETE FROM chi_tiet_dot_giam_gia;
    DELETE FROM hoa_don_chi_tiet;

    DELETE FROM hinh_anh_san_pham;
    DELETE FROM san_pham_chi_tiet;
    DELETE FROM san_pham;

    /* =====================================================
       2. XÓA TOÀN BỘ THUỘC TÍNH SẢN PHẨM
       ===================================================== */

    DELETE FROM mau_sac;
    DELETE FROM kich_thuoc;

    DELETE FROM danh_muc;
    DELETE FROM thuong_hieu;
    DELETE FROM chat_lieu;
    DELETE FROM xuat_xu;
    DELETE FROM co_giay;
    DELETE FROM kieu_dang;

    /* =====================================================
       3. RESET IDENTITY
       Sau khi bảng rỗng, RESEED 0 -> record tiếp theo ID = 1
       ===================================================== */

    DBCC CHECKIDENT ('chi_tiet_dot_giam_gia', RESEED, 0);
    DBCC CHECKIDENT ('hoa_don_chi_tiet', RESEED, 0);

    DBCC CHECKIDENT ('hinh_anh_san_pham', RESEED, 0);
    DBCC CHECKIDENT ('san_pham_chi_tiet', RESEED, 0);
    DBCC CHECKIDENT ('san_pham', RESEED, 0);

    DBCC CHECKIDENT ('mau_sac', RESEED, 0);
    DBCC CHECKIDENT ('kich_thuoc', RESEED, 0);

    DBCC CHECKIDENT ('danh_muc', RESEED, 0);
    DBCC CHECKIDENT ('thuong_hieu', RESEED, 0);
    DBCC CHECKIDENT ('chat_lieu', RESEED, 0);
    DBCC CHECKIDENT ('xuat_xu', RESEED, 0);
    DBCC CHECKIDENT ('co_giay', RESEED, 0);
    DBCC CHECKIDENT ('kieu_dang', RESEED, 0);


    /* =====================================================
       4. DANH MỤC
       ID 1 -> 8
       ===================================================== */

    INSERT INTO danh_muc
        (ma_danh_muc, ten_danh_muc, mo_ta, trang_thai)
    VALUES
        ('DM001', N'Giày chạy bộ',
         N'Giày dành cho chạy bộ và luyện tập', 1),

        ('DM002', N'Giày thể thao',
         N'Giày thể thao sử dụng hàng ngày', 1),

        ('DM003', N'Giày bóng rổ',
         N'Giày chuyên dụng cho bóng rổ', 1),

        ('DM004', N'Giày thời trang',
         N'Giày sneaker và thời trang đường phố', 1),

        ('DM005', N'Giày đá bóng',
         N'Giày sử dụng khi chơi bóng đá', 1),

        ('DM006', N'Giày tennis',
         N'Giày dành cho tennis và thể thao sân', 1),

        ('DM007', N'Giày đi bộ',
         N'Giày nhẹ dùng để đi bộ hàng ngày', 1),

        ('DM008', N'Giày tập gym',
         N'Giày phục vụ tập luyện và fitness', 1);


    /* =====================================================
       5. THƯƠNG HIỆU
       ID 1 -> 10
       ===================================================== */

    INSERT INTO thuong_hieu
        (ma_thuong_hieu, ten_thuong_hieu, trang_thai)
    VALUES
        ('TH001', N'Nike', 1),
        ('TH002', N'Adidas', 1),
        ('TH003', N'Puma', 1),
        ('TH004', N'New Balance', 1),
        ('TH005', N'Converse', 1),
        ('TH006', N'Vans', 1),
        ('TH007', N'ASICS', 1),
        ('TH008', N'Under Armour', 1),
        ('TH009', N'Reebok', 1),
        ('TH010', N'Skechers', 1);


    /* =====================================================
       6. CHẤT LIỆU
       ID 1 -> 10
       ===================================================== */

    INSERT INTO chat_lieu
        (ma_chat_lieu, ten_chat_lieu, trang_thai)
    VALUES
        ('CL001', N'Vải Mesh', 1),
        ('CL002', N'Da tổng hợp', 1),
        ('CL003', N'Da thật', 1),
        ('CL004', N'Vải Canvas', 1),
        ('CL005', N'Vải Knit', 1),
        ('CL006', N'Da lộn', 1),
        ('CL007', N'Polyester', 1),
        ('CL008', N'Cao su', 1),
        ('CL009', N'Vải dệt', 1),
        ('CL010', N'Sợi tổng hợp', 1);


    /* =====================================================
       7. XUẤT XỨ
       ID 1 -> 8
       ===================================================== */

    INSERT INTO xuat_xu
        (ma_xuat_xu, ten_xuat_xu, trang_thai)
    VALUES
        ('XX001', N'Việt Nam', 1),
        ('XX002', N'Indonesia', 1),
        ('XX003', N'Trung Quốc', 1),
        ('XX004', N'Mỹ', 1),
        ('XX005', N'Nhật Bản', 1),
        ('XX006', N'Hàn Quốc', 1),
        ('XX007', N'Thái Lan', 1),
        ('XX008', N'Đức', 1);


    /* =====================================================
       8. CỔ GIÀY
       ID 1 -> 3
       ===================================================== */

    INSERT INTO co_giay
        (ma_co_giay, ten_co_giay, trang_thai)
    VALUES
        ('CG001', N'Cổ thấp', 1),
        ('CG002', N'Cổ trung', 1),
        ('CG003', N'Cổ cao', 1);


    /* =====================================================
       9. KIỂU DÁNG
       ID 1 -> 10
       ===================================================== */

    INSERT INTO kieu_dang
        (ma_kieu_dang, ten_kieu_dang, trang_thai)
    VALUES
        ('KD001', N'Running', 1),
        ('KD002', N'Sneaker', 1),
        ('KD003', N'Basketball', 1),
        ('KD004', N'Casual', 1),
        ('KD005', N'Training', 1),
        ('KD006', N'Football', 1),
        ('KD007', N'Tennis', 1),
        ('KD008', N'Walking', 1),
        ('KD009', N'Skate', 1),
        ('KD010', N'Lifestyle', 1);


    /* =====================================================
       10. MÀU SẮC
       ID 1 -> 15
       ===================================================== */

    INSERT INTO mau_sac
        (
            ma_mau_sac,
            ten_mau_sac,
            ma_mau_hex,
            trang_thai,
            ngay_tao,
            ngay_cap_nhat
        )
    VALUES
        ('MS001', N'Đen',        '#000000', 1, GETDATE(), GETDATE()),
        ('MS002', N'Trắng',      '#FFFFFF', 1, GETDATE(), GETDATE()),
        ('MS003', N'Đỏ',         '#FF0000', 1, GETDATE(), GETDATE()),
        ('MS004', N'Xanh dương', '#0066FF', 1, GETDATE(), GETDATE()),
        ('MS005', N'Xám',        '#808080', 1, GETDATE(), GETDATE()),
        ('MS006', N'Xanh lá',    '#00A651', 1, GETDATE(), GETDATE()),
        ('MS007', N'Be',         '#F5F5DC', 1, GETDATE(), GETDATE()),
        ('MS008', N'Nâu',        '#8B4513', 1, GETDATE(), GETDATE()),
        ('MS009', N'Vàng',       '#FFD700', 1, GETDATE(), GETDATE()),
        ('MS010', N'Cam',        '#FF8C00', 1, GETDATE(), GETDATE()),
        ('MS011', N'Hồng',       '#FF69B4', 1, GETDATE(), GETDATE()),
        ('MS012', N'Tím',        '#800080', 1, GETDATE(), GETDATE()),
        ('MS013', N'Xanh navy',  '#000080', 1, GETDATE(), GETDATE()),
        ('MS014', N'Kem',        '#FFFDD0', 1, GETDATE(), GETDATE()),
        ('MS015', N'Bạc',        '#C0C0C0', 1, GETDATE(), GETDATE());


    /* =====================================================
       11. KÍCH THƯỚC
       ID cố định theo thứ tự này
       ===================================================== */

    INSERT INTO kich_thuoc
        (
            gia_tri,
            ghi_chu,
            trang_thai,
            ngay_tao,
            ngay_cap_nhat
        )
    VALUES
        ('35',   N'Size 35',   1, GETDATE(), GETDATE()),
        ('36',   N'Size 36',   1, GETDATE(), GETDATE()),
        ('36.5', N'Size 36.5', 1, GETDATE(), GETDATE()),
        ('37',   N'Size 37',   1, GETDATE(), GETDATE()),
        ('37.5', N'Size 37.5', 1, GETDATE(), GETDATE()),
        ('38',   N'Size 38',   1, GETDATE(), GETDATE()),
        ('38.5', N'Size 38.5', 1, GETDATE(), GETDATE()),
        ('39',   N'Size 39',   1, GETDATE(), GETDATE()),
        ('39.5', N'Size 39.5', 1, GETDATE(), GETDATE()),
        ('40',   N'Size 40',   1, GETDATE(), GETDATE()),
        ('40.5', N'Size 40.5', 1, GETDATE(), GETDATE()),
        ('41',   N'Size 41',   1, GETDATE(), GETDATE()),
        ('41.5', N'Size 41.5', 1, GETDATE(), GETDATE()),
        ('42',   N'Size 42',   1, GETDATE(), GETDATE()),
        ('42.5', N'Size 42.5', 1, GETDATE(), GETDATE()),
        ('43',   N'Size 43',   1, GETDATE(), GETDATE()),
        ('43.5', N'Size 43.5', 1, GETDATE(), GETDATE()),
        ('44',   N'Size 44',   1, GETDATE(), GETDATE()),
        ('44.5', N'Size 44.5', 1, GETDATE(), GETDATE()),
        ('45',   N'Size 45',   1, GETDATE(), GETDATE()),
        ('46',   N'Size 46',   1, GETDATE(), GETDATE());


    COMMIT TRANSACTION;

    PRINT N'==============================================';
    PRINT N'CLEAN + SEED DỮ LIỆU SẢN PHẨM THÀNH CÔNG';
    PRINT N'==============================================';

END TRY

BEGIN CATCH

    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;

    PRINT N'CÓ LỖI - ĐÃ ROLLBACK';
    THROW;

END CATCH;
GO


/* =========================================================
   12. KIỂM TRA SAU KHI TẠO
   ===================================================== */

SELECT * FROM danh_muc ORDER BY id;
SELECT * FROM thuong_hieu ORDER BY id;
SELECT * FROM chat_lieu ORDER BY id;
SELECT * FROM xuat_xu ORDER BY id;
SELECT * FROM co_giay ORDER BY id;
SELECT * FROM kieu_dang ORDER BY id;
SELECT * FROM mau_sac ORDER BY id;
SELECT * FROM kich_thuoc ORDER BY id;

SELECT * FROM san_pham;
SELECT * FROM san_pham_chi_tiet;
SELECT * FROM hinh_anh_san_pham;
GO