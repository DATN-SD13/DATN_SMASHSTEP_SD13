-- Source: origin/hung 69f9afd. Add missing schedule storage and default shifts only.
USE SmashStep;
GO

SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
GO

/* =========================================================
   Module: Quản lý lịch làm việc
   Gồm 2 bảng:
     - ca_lam_viec : template ca (Ca sáng, Ca chiều, Ca tối...)
     - lich_lam_viec: phân ca nhân viên theo ngày
   ========================================================= */

IF OBJECT_ID(N'dbo.ca_lam_viec', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ca_lam_viec (
        id            BIGINT IDENTITY(1,1) PRIMARY KEY,
        ma_ca         VARCHAR(50)       NOT NULL,
        ten_ca        NVARCHAR(255)     NOT NULL,
        gio_bat_dau   TIME(0)           NOT NULL,
        gio_ket_thuc  TIME(0)           NOT NULL,
        mo_ta         NVARCHAR(1000),
        trang_thai    INT               DEFAULT 1,
        ngay_tao      DATETIME2,
        ngay_cap_nhat DATETIME2
    );
    PRINT 'Tao bang ca_lam_viec thanh cong';
END
ELSE
    PRINT 'Bang ca_lam_viec da ton tai, bo qua';
GO

IF OBJECT_ID(N'dbo.lich_lam_viec', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.lich_lam_viec (
        id               BIGINT IDENTITY(1,1) PRIMARY KEY,
        id_nhan_vien     BIGINT        NOT NULL,
        id_ca_lam_viec   BIGINT        NOT NULL,
        ngay_lam         DATE          NOT NULL,
        trang_thai       INT           DEFAULT 0,  -- 0=Chờ, 1=Xác nhận, 2=Vắng, 3=Hoàn thành
        ghi_chu          NVARCHAR(500),
        ngay_tao         DATETIME2,
        ngay_cap_nhat    DATETIME2,
        CONSTRAINT FK_llv_nhan_vien   FOREIGN KEY (id_nhan_vien)   REFERENCES dbo.nhan_vien(id),
        CONSTRAINT FK_llv_ca_lam_viec FOREIGN KEY (id_ca_lam_viec) REFERENCES dbo.ca_lam_viec(id)
    );
    PRINT 'Tao bang lich_lam_viec thanh cong';
END
ELSE
    PRINT 'Bang lich_lam_viec da ton tai, bo qua';
GO

/* Seed 3 ca làm mặc định */
IF NOT EXISTS (SELECT 1 FROM dbo.ca_lam_viec)
BEGIN
    INSERT INTO dbo.ca_lam_viec (ma_ca, ten_ca, gio_bat_dau, gio_ket_thuc, mo_ta, trang_thai, ngay_tao, ngay_cap_nhat)
    VALUES
        ('CA_SANG',  N'Ca sáng',  '08:00', '12:00', N'Ca sáng 08:00 - 12:00', 1, GETDATE(), GETDATE()),
        ('CA_CHIEU', N'Ca chiều', '13:00', '17:00', N'Ca chiều 13:00 - 17:00', 1, GETDATE(), GETDATE()),
        ('CA_TOI',   N'Ca tối',   '17:30', '21:30', N'Ca tối 17:30 - 21:30',  1, GETDATE(), GETDATE());
    PRINT 'Seed 3 ca mac dinh thanh cong';
END
GO
