-- Additive migration: run once on the existing SmashStep database; safe to rerun.
IF OBJECT_ID(N'dbo.danh_gia_san_pham', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.danh_gia_san_pham (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        id_san_pham BIGINT NOT NULL REFERENCES dbo.san_pham(id),
        id_khach_hang BIGINT NOT NULL REFERENCES dbo.khach_hang(id),
        so_sao INT NOT NULL CHECK (so_sao BETWEEN 1 AND 5),
        binh_luan NVARCHAR(2000) NOT NULL,
        hinh_anh NVARCHAR(MAX) NULL,
        ngay_tao DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        ngay_cap_nhat DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT UQ_danh_gia_khach_san_pham UNIQUE (id_khach_hang, id_san_pham)
    );
    CREATE INDEX IX_danh_gia_san_pham ON dbo.danh_gia_san_pham(id_san_pham, ngay_tao DESC);
END;
ELSE
BEGIN
    IF COL_LENGTH('dbo.danh_gia_san_pham', 'hinh_anh') IS NULL
    BEGIN
        ALTER TABLE dbo.danh_gia_san_pham ADD hinh_anh NVARCHAR(MAX) NULL;
    END;
END;
GO
