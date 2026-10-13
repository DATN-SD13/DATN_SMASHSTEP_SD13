SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
GO

-- ============================================================
-- Seed mật khẩu mặc định cho tài khoản mẫu
-- Nhân viên / admin : Admin@123
-- Khách hàng        : Customer@123
-- CHỈ CHẠY MỘT LẦN trên môi trường dev/test.
-- ============================================================

DECLARE @hashAdmin    VARCHAR(255) = '$2a$10$su60Otf6rbew7s1i6JgWHu/II/qt5ut9goU2PwkKQjeOE54jYiO9S'
DECLARE @hashCustomer VARCHAR(255) = '$2a$10$pDjMAxU5LpJ0tVfg8nY.zuateBVdIPz.PicvOx4Y6BUVnIeNsv8QC'

-- Set mật khẩu + đảm bảo ten_dang_nhap = ma_nhan_vien
UPDATE nhan_vien
SET    mat_khau      = @hashAdmin,
       ten_dang_nhap = ma_nhan_vien;
GO

-- Khách hàng
DECLARE @hashCustomer VARCHAR(255) = '$2a$10$pDjMAxU5LpJ0tVfg8nY.zuateBVdIPz.PicvOx4Y6BUVnIeNsv8QC'
UPDATE khach_hang
SET    mat_khau = @hashCustomer
WHERE  mat_khau IS NULL OR mat_khau = '';
GO

-- Đảm bảo NV0001 có vai trò Quản lý (tìm theo tên, không hardcode id)
UPDATE nhan_vien
SET    id_vai_tro = (SELECT TOP 1 id FROM vai_tro WHERE ten_vai_tro LIKE N'%Qu%n l%')
WHERE  ma_nhan_vien = 'NV0001';
GO

-- Kiểm tra
SELECT nv.ma_nhan_vien, nv.ten_dang_nhap, vt.ten_vai_tro, nv.trang_thai
FROM   nhan_vien nv
LEFT JOIN vai_tro vt ON nv.id_vai_tro = vt.id
ORDER  BY nv.id;
GO


SELECT
    nv.ma_nhan_vien,
    nv.ten_dang_nhap,
    vt.id        AS id_vai_tro,
    vt.ten_vai_tro,
    nv.trang_thai
FROM nhan_vien nv
LEFT JOIN vai_tro vt ON nv.id_vai_tro = vt.id
ORDER BY nv.id;
