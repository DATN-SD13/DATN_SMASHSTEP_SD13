USE SmashStep;
GO

-- Thêm cột reset_token và reset_token_expiry vào bảng khach_hang
-- Chạy một lần duy nhất trên môi trường dev/test/prod

IF COL_LENGTH('dbo.khach_hang', 'reset_token') IS NULL
BEGIN
    ALTER TABLE khach_hang
    ADD reset_token VARCHAR(255) NULL,
        reset_token_expiry DATETIME2 NULL;

    PRINT N'Đã thêm cột reset_token và reset_token_expiry vào bảng khach_hang.';
END
ELSE
BEGIN
    PRINT N'Cột reset_token đã tồn tại, bỏ qua.';
END

GO
