USE SmashStep;
GO

-- =============================================
-- SEED MODULE NHÂN VIÊN
-- =============================================

-- 1. VAI TRÒ QUẢN LÝ
IF NOT EXISTS (
    SELECT 1
    FROM vai_tro
    WHERE ten_vai_tro = N'Quản lý'
)
BEGIN
    INSERT INTO vai_tro (
        ten_vai_tro,
        mo_ta,
        trang_thai
    )
    VALUES (
        N'Quản lý',
        N'Quản lý hệ thống',
        1
    );
END
GO


-- 2. VAI TRÒ NHÂN VIÊN
IF NOT EXISTS (
    SELECT 1
    FROM vai_tro
    WHERE ten_vai_tro = N'Nhân viên'
)
BEGIN
    INSERT INTO vai_tro (
        ten_vai_tro,
        mo_ta,
        trang_thai
    )
    VALUES (
        N'Nhân viên',
        N'Nhân viên cửa hàng',
        1
    );
END
GO


-- 3. NHÂN VIÊN MẪU
DECLARE @idVaiTroNhanVien BIGINT;

SELECT TOP 1
    @idVaiTroNhanVien = id
FROM vai_tro
WHERE ten_vai_tro = N'Nhân viên';


IF NOT EXISTS (
    SELECT 1
    FROM nhan_vien
    WHERE ma_nhan_vien = 'NV0001'
)
BEGIN
    INSERT INTO nhan_vien (
        id_vai_tro,
        ma_nhan_vien,
        ten_dang_nhap,
        ten_nhan_vien,
        email,
        mat_khau,
        so_dien_thoai,
        gioi_tinh,
        ngay_sinh,
        dia_chi,
        tinh_thanh,
        phuong_xa,
        trang_thai,
        ngay_tao,
        hinh_anh,
        ngay_cap_nhat
    )
    VALUES (
        @idVaiTroNhanVien,
        'NV0001',
        'NV0001',
        N'Nguyễn Văn An',
        'an@sportshoe.vn',
        NULL,
        '0901000001',
        1,
        '2000-01-15',
        N'Số 10 Nguyễn Trãi',
        N'Hà Nội',
        N'Phường Thanh Xuân',
        1,
        GETDATE(),
        NULL,
        GETDATE()
    );
END
GO


-- KIỂM TRA
SELECT
    id,
    ma_nhan_vien,
    ten_nhan_vien,
    email,
    so_dien_thoai,
    gioi_tinh,
    trang_thai
FROM nhan_vien;
GO