USE SmashStep;
GO

INSERT INTO phieu_giam_gia (
    ma_phieu_giam_gia,
    ten_phieu_giam_gia,
    hinh_thuc_phieu,
    loai_giam_gia,
    gia_tri_giam,
    gia_tri_toi_thieu,
    giam_toi_da,
    ngay_bat_dau,
    ngay_ket_thuc,
    so_luong,
    so_luong_da_dung,
    trang_thai,
    ngay_tao,
    ngay_cap_nhat,
    mo_ta
)
VALUES
(
    'PGG006',
    N'Giảm 10% cho đơn từ 200K',
    1,                  -- Công khai
    1,                  -- Phần trăm
    10.00,              -- 10%
    500000.00,
    100000.00,
    '2026-10-01 00:00:00',
    '2026-12-31 23:59:59',
    100,
    0,
    1,                  -- Hoạt động
    GETDATE(),
    GETDATE(),
    N'Giảm 10% tối đa 100.000đ cho đơn hàng từ 200.000đ'
),
(
    'PGG001',
    N'Giảm 10% cho đơn từ 500K',
    1,                  -- Công khai
    1,                  -- Phần trăm
    10.00,              -- 10%
    500000.00,
    100000.00,
    '2026-10-01 00:00:00',
    '2026-12-31 23:59:59',
    100,
    0,
    1,                  -- Hoạt động
    GETDATE(),
    GETDATE(),
    N'Giảm 10% tối đa 100.000đ cho đơn hàng từ 500.000đ'
),
(
    'PGG002',
    N'Giảm 50K cho đơn từ 300K',
    1,
    2,                  -- Tiền mặt
    50000.00,
    300000.00,
    50000.00,
    '2026-10-01 00:00:00',
    '2026-11-30 23:59:59',
    200,
    0,
    1,
    GETDATE(),
    GETDATE(),
    N'Giảm trực tiếp 50.000đ cho đơn hàng từ 300.000đ'
),
(
    'PGG003',
    N'Giảm 20% cuối năm',
    1,
    1,
    20.00,
    1000000.00,
    200000.00,
    '2026-11-01 00:00:00',
    '2026-12-31 23:59:59',
    100,
    0,
    1,
    GETDATE(),
    GETDATE(),
    N'Ưu đãi giảm 20% tối đa 200.000đ cho đơn từ 1.000.000đ'
),
(
    'PGG004',
    N'Voucher khách hàng thân thiết',
    2,                  -- Cá nhân
    2,
    100000.00,
    500000.00,
    100000.00,
    '2026-10-01 00:00:00',
    '2026-12-31 23:59:59',
    50,
    0,
    1,
    GETDATE(),
    GETDATE(),
    N'Voucher dành riêng cho khách hàng thân thiết'
),
(
    'PGG005',
    N'Giảm 15% tháng 10',
    1,
    1,
    15.00,
    700000.00,
    150000.00,
    '2026-10-01 00:00:00',
    '2026-10-31 23:59:59',
    300,
    0,
    1,
    GETDATE(),
    GETDATE(),
    N'Giảm 15% tối đa 150.000đ trong tháng 10'
);
GO
SELECT *
FROM phieu_giam_gia
ORDER BY id DESC;

ALTER TABLE phieu_giam_gia
ADD vo_han BIT NOT NULL DEFAULT 0;