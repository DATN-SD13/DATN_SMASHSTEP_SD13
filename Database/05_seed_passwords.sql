SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
GO

-- ============================================================
-- SEED TÀI KHOẢN SMASHSTEP
--
-- PHÂN QUYỀN:
-- id_vai_tro = 1: Nhân viên
-- id_vai_tro = 2: Quản lý
--
-- TÀI KHOẢN:
-- NV0001: Quản lý
-- NV001, NV002, NV003...: Nhân viên
--
-- MẬT KHẨU MẶC ĐỊNH:
-- Quản lý / Nhân viên: Admin@123
-- Khách hàng: Customer@123
--
-- TRẠNG THÁI:
-- 1: Đang hoạt động
--
-- CHỈ CHẠY TRÊN MÔI TRƯỜNG DEV/TEST
-- ============================================================


-- ============================================================
-- PHẦN 1: CẬP NHẬT VAI TRÒ
-- ============================================================

-- Vai trò 1: Nhân viên
UPDATE vai_tro
SET ten_vai_tro = N'Nhân viên'
WHERE id = 1;

-- Vai trò 2: Quản lý
UPDATE vai_tro
SET ten_vai_tro = N'Quản lý'
WHERE id = 2;

GO


-- ============================================================
-- PHẦN 2: CẬP NHẬT TÀI KHOẢN NHÂN VIÊN
-- ============================================================

DECLARE @hashAdmin VARCHAR(255) =
'$2a$10$su60Otf6rbew7s1i6JgWHu/II/qt5ut9goU2PwkKQjeOE54jYiO9S';

-- Cập nhật mật khẩu và tên đăng nhập
UPDATE nhan_vien
SET
    mat_khau = @hashAdmin,
    ten_dang_nhap = ma_nhan_vien;

-- NV0001 là Quản lý
UPDATE nhan_vien
SET
    id_vai_tro = 2,
    trang_thai = 1
WHERE ma_nhan_vien = 'NV0001';

-- Các tài khoản còn lại là Nhân viên
UPDATE nhan_vien
SET
    id_vai_tro = 1,
    trang_thai = 1
WHERE ma_nhan_vien <> 'NV0001';

GO


-- ============================================================
-- PHẦN 3: CẬP NHẬT MẬT KHẨU KHÁCH HÀNG
-- ============================================================

DECLARE @hashCustomer VARCHAR(255) =
'$2a$10$pDjMAxU5LpJ0tVfg8nY.zuateBVdIPz.PicvOx4Y6BUVnIeNsv8QC';

-- Chỉ cập nhật khách hàng chưa có mật khẩu
UPDATE khach_hang
SET mat_khau = @hashCustomer
WHERE mat_khau IS NULL
   OR mat_khau = '';

GO


-- ============================================================
-- PHẦN 4: KIỂM TRA DANH SÁCH VAI TRÒ
-- ============================================================

SELECT
    id,
    ten_vai_tro
FROM vai_tro
ORDER BY id;

GO


-- ============================================================
-- PHẦN 5: KIỂM TRA TÀI KHOẢN NHÂN VIÊN
-- ============================================================

SELECT
    nv.ma_nhan_vien,
    nv.ten_dang_nhap,
    nv.id_vai_tro,
    vt.ten_vai_tro,
    nv.trang_thai
FROM nhan_vien nv
LEFT JOIN vai_tro vt
    ON nv.id_vai_tro = vt.id
ORDER BY
    CASE
        WHEN nv.ma_nhan_vien = 'NV0001' THEN 1
        ELSE 0
    END,
    nv.ma_nhan_vien;

GO