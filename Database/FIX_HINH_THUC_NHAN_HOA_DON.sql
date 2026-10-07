/*
  FIX NGHIỆP VỤ HÓA ĐƠN

  loai_hoa_don:
    0 = Tại quầy
    1 = Trực tuyến

  hinh_thuc_nhan:
    0 = Nhận tại quầy
    1 = Giao hàng

  Dữ liệu cũ:
    loai_hoa_don = 2 (Giao hàng)
  sẽ được chuẩn hóa thành:
    loai_hoa_don = 0 (Tại quầy)
    hinh_thuc_nhan = 1 (Giao hàng)

  Chạy sau khi backup DB.
  SQL Server.
*/

------------------------------------------------------------
-- 1. Thêm cột hinh_thuc_nhan nếu chưa có
------------------------------------------------------------

IF COL_LENGTH('hoa_don', 'hinh_thuc_nhan') IS NULL
BEGIN
    ALTER TABLE hoa_don
    ADD hinh_thuc_nhan TINYINT NULL;
END;
GO


------------------------------------------------------------
-- 2. Chuẩn hóa dữ liệu cũ
------------------------------------------------------------

/*
    Quy tắc:

    loai_hoa_don = 2
        -> Tại quầy + Giao hàng

    loai_hoa_don = 0
        -> Nếu có địa chỉ giao hàng hoặc phí vận chuyển
           => Giao hàng
        -> Ngược lại
           => Nhận tại quầy

    loai_hoa_don = 1
        -> Trực tuyến + Giao hàng
*/

UPDATE hoa_don
SET
    hinh_thuc_nhan =
        CASE
            -- Dữ liệu cũ "Giao hàng"
            WHEN loai_hoa_don = 2 THEN 1

            -- Tại quầy nhưng có thông tin giao hàng
            WHEN loai_hoa_don = 0
                 AND (
                     NULLIF(
                         LTRIM(RTRIM(ISNULL(dia_chi_giao_hang, N''))),
                         N''
                     ) IS NOT NULL
                     OR ISNULL(phi_van_chuyen, 0) > 0
                 )
            THEN 1

            -- Trực tuyến
            WHEN loai_hoa_don = 1 THEN 1

            -- Tại quầy, nhận tại quầy
            ELSE 0
        END,

    -- Chuyển loại đơn cũ = 2 thành Tại quầy
    loai_hoa_don =
        CASE
            WHEN loai_hoa_don = 2 THEN 0
            ELSE loai_hoa_don
        END

WHERE loai_hoa_don IN (0, 1, 2)
   OR hinh_thuc_nhan IS NULL;
GO


------------------------------------------------------------
-- 3. Các bản ghi còn thiếu hình thức nhận
------------------------------------------------------------

UPDATE hoa_don
SET hinh_thuc_nhan =
    CASE
        WHEN loai_hoa_don = 0 THEN 0
        WHEN loai_hoa_don = 1 THEN 1
        ELSE 0
    END
WHERE hinh_thuc_nhan IS NULL;
GO


------------------------------------------------------------
-- 4. Thêm CHECK constraint
------------------------------------------------------------

IF NOT EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = 'CK_hoa_don_hinh_thuc_nhan'
)
BEGIN
    ALTER TABLE hoa_don
    ADD CONSTRAINT CK_hoa_don_hinh_thuc_nhan
    CHECK (hinh_thuc_nhan IN (0, 1));
END;
GO


------------------------------------------------------------
-- 5. Kiểm tra dữ liệu sau khi sửa
------------------------------------------------------------

SELECT
    loai_hoa_don,
    hinh_thuc_nhan,
    COUNT(*) AS so_luong
FROM hoa_don
GROUP BY
    loai_hoa_don,
    hinh_thuc_nhan
ORDER BY
    loai_hoa_don,
    hinh_thuc_nhan;
GO


------------------------------------------------------------
-- 6. Kiểm tra trực tiếp các hóa đơn
------------------------------------------------------------

SELECT
    id,
    ma_hoa_don,
    loai_hoa_don,
    hinh_thuc_nhan,
    dia_chi_giao_hang,
    phi_van_chuyen
FROM hoa_don
ORDER BY id;
GO

------------------------------------------------------------
-- TẠO DỮ LIỆU DEMO CHO LUỒNG:
-- Tại quầy + Nhận tại quầy
------------------------------------------------------------

-- Lấy 1 hóa đơn hiện có làm hóa đơn Tại quầy + Nhận tại quầy
UPDATE hoa_don
SET
    loai_hoa_don = 0,
    hinh_thuc_nhan = 0,
    dia_chi_giao_hang = NULL,
    phi_van_chuyen = 0,
    don_vi_van_chuyen = NULL,
    ngay_cap_nhat = GETDATE()
WHERE id = (
    SELECT MIN(id)
    FROM hoa_don
    WHERE trang_thai IN (0, 1, 5)
);
GO

UPDATE hoa_don
SET
    loai_hoa_don = 0,
    hinh_thuc_nhan = 1,
    ngay_cap_nhat = GETDATE()
WHERE id = (
    SELECT MIN(id)
    FROM hoa_don
    WHERE id <> (
        SELECT MIN(id)
        FROM hoa_don
        WHERE trang_thai IN (0, 1, 5)
    )
);
GO

SELECT
    id,
    ma_hoa_don,
    loai_hoa_don,
    hinh_thuc_nhan,
    trang_thai
FROM hoa_don
ORDER BY id;