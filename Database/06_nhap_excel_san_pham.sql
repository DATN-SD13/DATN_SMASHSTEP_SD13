-- Chay tren DB cua ung dung, khong tao DB moi.
-- Chi them bang luu ket qua lo nhap de chong tao trung khi gui lai request.
-- Khong cap nhat san_pham, san_pham_chi_tiet hay ton kho hien tai.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF OBJECT_ID(N'dbo.san_pham_import_batch', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.san_pham_import_batch (
        id varchar(36) NOT NULL CONSTRAINT PK_san_pham_import_batch PRIMARY KEY,
        nguoi_tao bigint NOT NULL,
        payload_hash varchar(64) NOT NULL,
        ngay_tao datetime2(7) NOT NULL,
        ket_qua_json nvarchar(max) NOT NULL
    );
END;
COMMIT TRANSACTION;
