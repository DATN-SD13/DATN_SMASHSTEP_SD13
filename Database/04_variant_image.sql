-- Additive migration. Existing product/color images remain unchanged.
USE SmashStep;
GO
SET XACT_ABORT ON;
GO
IF COL_LENGTH(N'dbo.hinh_anh_san_pham', N'id_san_pham_chi_tiet') IS NULL
    ALTER TABLE dbo.hinh_anh_san_pham ADD id_san_pham_chi_tiet BIGINT NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_key_columns
    WHERE parent_object_id = OBJECT_ID(N'dbo.hinh_anh_san_pham')
      AND parent_column_id = COLUMNPROPERTY(OBJECT_ID(N'dbo.hinh_anh_san_pham'), N'id_san_pham_chi_tiet', 'ColumnId')
      AND referenced_object_id = OBJECT_ID(N'dbo.san_pham_chi_tiet'))
    ALTER TABLE dbo.hinh_anh_san_pham WITH CHECK ADD CONSTRAINT FK_hinh_anh_bien_the
        FOREIGN KEY (id_san_pham_chi_tiet) REFERENCES dbo.san_pham_chi_tiet(id);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.hinh_anh_san_pham') AND name = N'IX_hinh_anh_bien_the')
    CREATE INDEX IX_hinh_anh_bien_the ON dbo.hinh_anh_san_pham(id_san_pham_chi_tiet);
GO
