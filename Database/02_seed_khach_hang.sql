USE SmashStep;
GO

-- =============================================
-- SEED MODULE KHÁCH HÀNG
-- =============================================

-- Dùng mã riêng để không đụng KH0001, KH0002...
-- do API tự sinh.

IF NOT EXISTS (
    SELECT 1
    FROM khach_hang
    WHERE ma_khach_hang = 'KHSEED001'
)
BEGIN
    INSERT INTO khach_hang (
        ma_khach_hang,
        ten_tai_khoan,
        ten_khach_hang,
        email,
        so_dien_thoai,
        ngay_sinh,
        gioi_tinh,
        mat_khau,
        hinh_anh,
        trang_thai,
        ngay_tao,
        ngay_cap_nhat
    )
    VALUES (
        'KHSEED001',
        'KHSEED001',
        N'Khách hàng mẫu',
        'khach.seed@smashstep.vn',
        '0902000001',
        '2000-01-15',
        1,
        NULL,
        NULL,
        1,
        SYSDATETIME(),
        SYSDATETIME()
    );
END
GO


-- =============================================
-- ĐỊA CHỈ KHÁCH HÀNG
-- =============================================

IF NOT EXISTS (
    SELECT 1
    FROM dia_chi_khach_hang d
    JOIN khach_hang k
        ON k.id = d.id_khach_hang
    WHERE
        k.ma_khach_hang = 'KHSEED001'
        AND d.trang_thai = 1
)
BEGIN
    INSERT INTO dia_chi_khach_hang (
        id_khach_hang,
        ten_nguoi_nhan,
        sdt_nguoi_nhan,
        tinh_thanh,
        quan_huyen,
        phuong_xa,
        dia_chi_cu_the,
        loai_dia_chi,
        is_mac_dinh,
        trang_thai
    )
    SELECT
        id,
        N'Khách hàng mẫu',
        '0902000001',
        N'Hà Nội',
        N'Cầu Giấy',
        N'Phường Dịch Vọng',
        N'Số 1 Trần Thái Tông',
        1,
        1,
        1
    FROM khach_hang
    WHERE ma_khach_hang = 'KHSEED001';
END
GO


-- KIỂM TRA KHÁCH HÀNG
SELECT
    id,
    ma_khach_hang,
    ten_khach_hang,
    email,
    so_dien_thoai,
    gioi_tinh,
    trang_thai
FROM khach_hang
WHERE ma_khach_hang = 'KHSEED001';
GO


-- KIỂM TRA ĐỊA CHỈ
SELECT
    d.*
FROM dia_chi_khach_hang d
JOIN khach_hang k
    ON k.id = d.id_khach_hang
WHERE k.ma_khach_hang = 'KHSEED001';
GO