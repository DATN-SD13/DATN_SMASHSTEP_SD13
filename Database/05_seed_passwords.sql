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

-- Đảm bảo ten_dang_nhap = ma_nhan_vien, set hash mật khẩu
UPDATE nhan_vien
SET    mat_khau      = @hashAdmin,
       ten_dang_nhap = ma_nhan_vien
WHERE  mat_khau IS NULL OR mat_khau = '';
GO

-- Khách hàng
DECLARE @hashCustomer VARCHAR(255) = '$2a$10$pDjMAxU5LpJ0tVfg8nY.zuateBVdIPz.PicvOx4Y6BUVnIeNsv8QC'
UPDATE khach_hang
SET    mat_khau = @hashCustomer
WHERE  mat_khau IS NULL OR mat_khau = '';
GO

-- Kiểm tra
SELECT ma_nhan_vien, ten_dang_nhap, LEFT(mat_khau, 20) AS hash_prefix, trang_thai
FROM   nhan_vien
ORDER  BY id;
GO
