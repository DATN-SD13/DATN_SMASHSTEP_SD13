/* CANONICAL FULL DATABASE - RUN THIS FILE
   SMASHSTEP SD-013 | SQL Server 2019+ | UTF-8 | SSMS: Execute the entire file.
   Source of truth: current backend (25 entities), then 01_sqlSD13.sql.
   Includes all six seed sources; 03 contains the safe superset of 02 product attributes.
   Creates SmashStep when absent. Existing tables receive missing columns only.
   Never replaces existing rows/IDs, changes stock, or reseeds identities.
   Legacy extra columns are retained on existing databases; absent from new schema.
   vo_han is unused: unlimited voucher = so_luong IS NULL.
   Image color is @Transient and rejected by HinhAnhService: no image id_mau_sac.
   Missing columns on populated tables are nullable; unknown FKs are never guessed.
   Incompatible types, orphan FKs or duplicate unique keys abort the transaction.
   Correct those reported records manually, then run this file again.
   Do not run the old destructive product seed after this file.
   Seed passwords remain NULL: this file contains no account credentials.
*/
USE [master];
GO
IF DB_ID(N'SmashStep') IS NULL
    CREATE DATABASE [SmashStep];
GO
USE [SmashStep];
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
BEGIN TRANSACTION;
GO
BEGIN TRY
    DECLARE @lockResult INT;
    EXEC @lockResult = sys.sp_getapplock @Resource=N'SmashStep.CanonicalFullDatabase',
        @LockMode='Exclusive', @LockOwner='Transaction', @LockTimeout=60000;
    IF @lockResult < 0 THROW 51000, N'Cannot acquire FULL_DB migration lock.', 1;
    -- 1. Schema. Dynamic DDL avoids compiling references before ADD finishes.

    IF OBJECT_ID(N'dbo.vai_tro', N'U') IS NULL
        EXEC sys.sp_executesql N'CREATE TABLE dbo.[vai_tro] (
        [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [PK_vai_tro] PRIMARY KEY,
        [ten_vai_tro] NVARCHAR(255),
        [mo_ta] NVARCHAR(1000),
        [trang_thai] INT
    );';

    IF COL_LENGTH(N'dbo.vai_tro', N'id') IS NULL
        THROW 51001, N'Existing table vai_tro has no id; manual recovery required.', 1;

    IF COL_LENGTH(N'dbo.vai_tro', N'ten_vai_tro') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[vai_tro] ADD [ten_vai_tro] NVARCHAR(255) NULL;';

    IF COL_LENGTH(N'dbo.vai_tro', N'mo_ta') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[vai_tro] ADD [mo_ta] NVARCHAR(1000) NULL;';

    IF COL_LENGTH(N'dbo.vai_tro', N'trang_thai') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[vai_tro] ADD [trang_thai] INT NULL;';

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.vai_tro') AND c.name=N'id' AND (TYPE_NAME(c.user_type_id) <> N'bigint' OR c.is_identity <> 1))
        THROW 51002, N'Incompatible type: vai_tro.id. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.vai_tro') AND c.name=N'ten_vai_tro' AND (TYPE_NAME(c.user_type_id) <> N'nvarchar' OR (c.max_length <> -1 AND c.max_length < 510)))
        THROW 51002, N'Incompatible type: vai_tro.ten_vai_tro. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.vai_tro') AND c.name=N'mo_ta' AND (TYPE_NAME(c.user_type_id) <> N'nvarchar' OR (c.max_length <> -1 AND c.max_length < 2000)))
        THROW 51002, N'Incompatible type: vai_tro.mo_ta. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.vai_tro') AND c.name=N'trang_thai' AND (TYPE_NAME(c.user_type_id) <> N'int'))
        THROW 51002, N'Incompatible type: vai_tro.trang_thai. No automatic data conversion.', 1;

    IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE parent_object_id=OBJECT_ID(N'dbo.vai_tro') AND type='PK')
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[vai_tro] ADD CONSTRAINT [PK_vai_tro] PRIMARY KEY ([id]);';

    IF OBJECT_ID(N'dbo.nhan_vien', N'U') IS NULL
        EXEC sys.sp_executesql N'CREATE TABLE dbo.[nhan_vien] (
        [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [PK_nhan_vien] PRIMARY KEY,
        [id_vai_tro] BIGINT,
        [ma_nhan_vien] VARCHAR(50),
        [ten_dang_nhap] VARCHAR(100),
        [ten_nhan_vien] NVARCHAR(255),
        [email] VARCHAR(255),
        [mat_khau] VARCHAR(255),
        [so_dien_thoai] VARCHAR(20),
        [gioi_tinh] INT,
        [ngay_sinh] DATE,
        [dia_chi] NVARCHAR(500),
        [tinh_thanh] NVARCHAR(100),
        [phuong_xa] NVARCHAR(100),
        [trang_thai] INT,
        [ngay_tao] DATETIME2,
        [hinh_anh] NVARCHAR(1000),
        [ngay_cap_nhat] DATETIME2
    );';

    IF COL_LENGTH(N'dbo.nhan_vien', N'id') IS NULL
        THROW 51001, N'Existing table nhan_vien has no id; manual recovery required.', 1;

    IF COL_LENGTH(N'dbo.nhan_vien', N'id_vai_tro') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[nhan_vien] ADD [id_vai_tro] BIGINT NULL;';

    IF COL_LENGTH(N'dbo.nhan_vien', N'ma_nhan_vien') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[nhan_vien] ADD [ma_nhan_vien] VARCHAR(50) NULL;';

    IF COL_LENGTH(N'dbo.nhan_vien', N'ten_dang_nhap') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[nhan_vien] ADD [ten_dang_nhap] VARCHAR(100) NULL;';

    IF COL_LENGTH(N'dbo.nhan_vien', N'ten_nhan_vien') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[nhan_vien] ADD [ten_nhan_vien] NVARCHAR(255) NULL;';

    IF COL_LENGTH(N'dbo.nhan_vien', N'email') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[nhan_vien] ADD [email] VARCHAR(255) NULL;';

    IF COL_LENGTH(N'dbo.nhan_vien', N'mat_khau') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[nhan_vien] ADD [mat_khau] VARCHAR(255) NULL;';

    IF COL_LENGTH(N'dbo.nhan_vien', N'so_dien_thoai') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[nhan_vien] ADD [so_dien_thoai] VARCHAR(20) NULL;';

    IF COL_LENGTH(N'dbo.nhan_vien', N'gioi_tinh') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[nhan_vien] ADD [gioi_tinh] INT NULL;';

    IF COL_LENGTH(N'dbo.nhan_vien', N'ngay_sinh') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[nhan_vien] ADD [ngay_sinh] DATE NULL;';

    IF COL_LENGTH(N'dbo.nhan_vien', N'dia_chi') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[nhan_vien] ADD [dia_chi] NVARCHAR(500) NULL;';

    IF COL_LENGTH(N'dbo.nhan_vien', N'tinh_thanh') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[nhan_vien] ADD [tinh_thanh] NVARCHAR(100) NULL;';

    IF COL_LENGTH(N'dbo.nhan_vien', N'phuong_xa') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[nhan_vien] ADD [phuong_xa] NVARCHAR(100) NULL;';

    IF COL_LENGTH(N'dbo.nhan_vien', N'trang_thai') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[nhan_vien] ADD [trang_thai] INT NULL;';

    IF COL_LENGTH(N'dbo.nhan_vien', N'ngay_tao') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[nhan_vien] ADD [ngay_tao] DATETIME2 NULL;';

    IF COL_LENGTH(N'dbo.nhan_vien', N'hinh_anh') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[nhan_vien] ADD [hinh_anh] NVARCHAR(1000) NULL;';

    IF COL_LENGTH(N'dbo.nhan_vien', N'ngay_cap_nhat') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[nhan_vien] ADD [ngay_cap_nhat] DATETIME2 NULL;';

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.nhan_vien') AND c.name=N'id' AND (TYPE_NAME(c.user_type_id) <> N'bigint' OR c.is_identity <> 1))
        THROW 51002, N'Incompatible type: nhan_vien.id. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.nhan_vien') AND c.name=N'id_vai_tro' AND (TYPE_NAME(c.user_type_id) <> N'bigint'))
        THROW 51002, N'Incompatible type: nhan_vien.id_vai_tro. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.nhan_vien') AND c.name=N'ma_nhan_vien' AND (TYPE_NAME(c.user_type_id) <> N'varchar' OR (c.max_length <> -1 AND c.max_length < 50)))
        THROW 51002, N'Incompatible type: nhan_vien.ma_nhan_vien. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.nhan_vien') AND c.name=N'ten_dang_nhap' AND (TYPE_NAME(c.user_type_id) <> N'varchar' OR (c.max_length <> -1 AND c.max_length < 100)))
        THROW 51002, N'Incompatible type: nhan_vien.ten_dang_nhap. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.nhan_vien') AND c.name=N'ten_nhan_vien' AND (TYPE_NAME(c.user_type_id) <> N'nvarchar' OR (c.max_length <> -1 AND c.max_length < 510)))
        THROW 51002, N'Incompatible type: nhan_vien.ten_nhan_vien. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.nhan_vien') AND c.name=N'email' AND (TYPE_NAME(c.user_type_id) <> N'varchar' OR (c.max_length <> -1 AND c.max_length < 255)))
        THROW 51002, N'Incompatible type: nhan_vien.email. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.nhan_vien') AND c.name=N'mat_khau' AND (TYPE_NAME(c.user_type_id) <> N'varchar' OR (c.max_length <> -1 AND c.max_length < 255)))
        THROW 51002, N'Incompatible type: nhan_vien.mat_khau. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.nhan_vien') AND c.name=N'so_dien_thoai' AND (TYPE_NAME(c.user_type_id) <> N'varchar' OR (c.max_length <> -1 AND c.max_length < 20)))
        THROW 51002, N'Incompatible type: nhan_vien.so_dien_thoai. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.nhan_vien') AND c.name=N'gioi_tinh' AND (TYPE_NAME(c.user_type_id) <> N'int'))
        THROW 51002, N'Incompatible type: nhan_vien.gioi_tinh. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.nhan_vien') AND c.name=N'ngay_sinh' AND (TYPE_NAME(c.user_type_id) <> N'date'))
        THROW 51002, N'Incompatible type: nhan_vien.ngay_sinh. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.nhan_vien') AND c.name=N'dia_chi' AND (TYPE_NAME(c.user_type_id) <> N'nvarchar' OR (c.max_length <> -1 AND c.max_length < 1000)))
        THROW 51002, N'Incompatible type: nhan_vien.dia_chi. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.nhan_vien') AND c.name=N'tinh_thanh' AND (TYPE_NAME(c.user_type_id) <> N'nvarchar' OR (c.max_length <> -1 AND c.max_length < 200)))
        THROW 51002, N'Incompatible type: nhan_vien.tinh_thanh. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.nhan_vien') AND c.name=N'phuong_xa' AND (TYPE_NAME(c.user_type_id) <> N'nvarchar' OR (c.max_length <> -1 AND c.max_length < 200)))
        THROW 51002, N'Incompatible type: nhan_vien.phuong_xa. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.nhan_vien') AND c.name=N'trang_thai' AND (TYPE_NAME(c.user_type_id) <> N'int'))
        THROW 51002, N'Incompatible type: nhan_vien.trang_thai. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.nhan_vien') AND c.name=N'ngay_tao' AND (TYPE_NAME(c.user_type_id) <> N'datetime2'))
        THROW 51002, N'Incompatible type: nhan_vien.ngay_tao. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.nhan_vien') AND c.name=N'hinh_anh' AND (TYPE_NAME(c.user_type_id) <> N'nvarchar' OR (c.max_length <> -1 AND c.max_length < 2000)))
        THROW 51002, N'Incompatible type: nhan_vien.hinh_anh. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.nhan_vien') AND c.name=N'ngay_cap_nhat' AND (TYPE_NAME(c.user_type_id) <> N'datetime2'))
        THROW 51002, N'Incompatible type: nhan_vien.ngay_cap_nhat. No automatic data conversion.', 1;

    IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE parent_object_id=OBJECT_ID(N'dbo.nhan_vien') AND type='PK')
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[nhan_vien] ADD CONSTRAINT [PK_nhan_vien] PRIMARY KEY ([id]);';

    IF OBJECT_ID(N'dbo.khach_hang', N'U') IS NULL
        EXEC sys.sp_executesql N'CREATE TABLE dbo.[khach_hang] (
        [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [PK_khach_hang] PRIMARY KEY,
        [ma_khach_hang] VARCHAR(50),
        [ten_tai_khoan] VARCHAR(100),
        [ten_khach_hang] NVARCHAR(255),
        [email] VARCHAR(255),
        [so_dien_thoai] VARCHAR(20),
        [ngay_sinh] DATE,
        [gioi_tinh] INT,
        [mat_khau] VARCHAR(255),
        [hinh_anh] NVARCHAR(1000),
        [trang_thai] INT,
        [ngay_tao] DATETIME2,
        [ngay_cap_nhat] DATETIME2
    );';

    IF COL_LENGTH(N'dbo.khach_hang', N'id') IS NULL
        THROW 51001, N'Existing table khach_hang has no id; manual recovery required.', 1;

    IF COL_LENGTH(N'dbo.khach_hang', N'ma_khach_hang') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[khach_hang] ADD [ma_khach_hang] VARCHAR(50) NULL;';

    IF COL_LENGTH(N'dbo.khach_hang', N'ten_tai_khoan') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[khach_hang] ADD [ten_tai_khoan] VARCHAR(100) NULL;';

    IF COL_LENGTH(N'dbo.khach_hang', N'ten_khach_hang') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[khach_hang] ADD [ten_khach_hang] NVARCHAR(255) NULL;';

    IF COL_LENGTH(N'dbo.khach_hang', N'email') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[khach_hang] ADD [email] VARCHAR(255) NULL;';

    IF COL_LENGTH(N'dbo.khach_hang', N'so_dien_thoai') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[khach_hang] ADD [so_dien_thoai] VARCHAR(20) NULL;';

    IF COL_LENGTH(N'dbo.khach_hang', N'ngay_sinh') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[khach_hang] ADD [ngay_sinh] DATE NULL;';

    IF COL_LENGTH(N'dbo.khach_hang', N'gioi_tinh') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[khach_hang] ADD [gioi_tinh] INT NULL;';

    IF COL_LENGTH(N'dbo.khach_hang', N'mat_khau') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[khach_hang] ADD [mat_khau] VARCHAR(255) NULL;';

    IF COL_LENGTH(N'dbo.khach_hang', N'hinh_anh') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[khach_hang] ADD [hinh_anh] NVARCHAR(1000) NULL;';

    IF COL_LENGTH(N'dbo.khach_hang', N'trang_thai') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[khach_hang] ADD [trang_thai] INT NULL;';

    IF COL_LENGTH(N'dbo.khach_hang', N'ngay_tao') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[khach_hang] ADD [ngay_tao] DATETIME2 NULL;';

    IF COL_LENGTH(N'dbo.khach_hang', N'ngay_cap_nhat') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[khach_hang] ADD [ngay_cap_nhat] DATETIME2 NULL;';

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.khach_hang') AND c.name=N'id' AND (TYPE_NAME(c.user_type_id) <> N'bigint' OR c.is_identity <> 1))
        THROW 51002, N'Incompatible type: khach_hang.id. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.khach_hang') AND c.name=N'ma_khach_hang' AND (TYPE_NAME(c.user_type_id) <> N'varchar' OR (c.max_length <> -1 AND c.max_length < 50)))
        THROW 51002, N'Incompatible type: khach_hang.ma_khach_hang. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.khach_hang') AND c.name=N'ten_tai_khoan' AND (TYPE_NAME(c.user_type_id) <> N'varchar' OR (c.max_length <> -1 AND c.max_length < 100)))
        THROW 51002, N'Incompatible type: khach_hang.ten_tai_khoan. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.khach_hang') AND c.name=N'ten_khach_hang' AND (TYPE_NAME(c.user_type_id) <> N'nvarchar' OR (c.max_length <> -1 AND c.max_length < 510)))
        THROW 51002, N'Incompatible type: khach_hang.ten_khach_hang. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.khach_hang') AND c.name=N'email' AND (TYPE_NAME(c.user_type_id) <> N'varchar' OR (c.max_length <> -1 AND c.max_length < 255)))
        THROW 51002, N'Incompatible type: khach_hang.email. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.khach_hang') AND c.name=N'so_dien_thoai' AND (TYPE_NAME(c.user_type_id) <> N'varchar' OR (c.max_length <> -1 AND c.max_length < 20)))
        THROW 51002, N'Incompatible type: khach_hang.so_dien_thoai. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.khach_hang') AND c.name=N'ngay_sinh' AND (TYPE_NAME(c.user_type_id) <> N'date'))
        THROW 51002, N'Incompatible type: khach_hang.ngay_sinh. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.khach_hang') AND c.name=N'gioi_tinh' AND (TYPE_NAME(c.user_type_id) <> N'int'))
        THROW 51002, N'Incompatible type: khach_hang.gioi_tinh. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.khach_hang') AND c.name=N'mat_khau' AND (TYPE_NAME(c.user_type_id) <> N'varchar' OR (c.max_length <> -1 AND c.max_length < 255)))
        THROW 51002, N'Incompatible type: khach_hang.mat_khau. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.khach_hang') AND c.name=N'hinh_anh' AND (TYPE_NAME(c.user_type_id) <> N'nvarchar' OR (c.max_length <> -1 AND c.max_length < 2000)))
        THROW 51002, N'Incompatible type: khach_hang.hinh_anh. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.khach_hang') AND c.name=N'trang_thai' AND (TYPE_NAME(c.user_type_id) <> N'int'))
        THROW 51002, N'Incompatible type: khach_hang.trang_thai. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.khach_hang') AND c.name=N'ngay_tao' AND (TYPE_NAME(c.user_type_id) <> N'datetime2'))
        THROW 51002, N'Incompatible type: khach_hang.ngay_tao. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.khach_hang') AND c.name=N'ngay_cap_nhat' AND (TYPE_NAME(c.user_type_id) <> N'datetime2'))
        THROW 51002, N'Incompatible type: khach_hang.ngay_cap_nhat. No automatic data conversion.', 1;

    IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE parent_object_id=OBJECT_ID(N'dbo.khach_hang') AND type='PK')
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[khach_hang] ADD CONSTRAINT [PK_khach_hang] PRIMARY KEY ([id]);';

    IF OBJECT_ID(N'dbo.dia_chi_khach_hang', N'U') IS NULL
        EXEC sys.sp_executesql N'CREATE TABLE dbo.[dia_chi_khach_hang] (
        [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [PK_dia_chi_khach_hang] PRIMARY KEY,
        [id_khach_hang] BIGINT NOT NULL,
        [ten_nguoi_nhan] NVARCHAR(255),
        [sdt_nguoi_nhan] VARCHAR(20),
        [tinh_thanh] NVARCHAR(100),
        [quan_huyen] NVARCHAR(100),
        [phuong_xa] NVARCHAR(100),
        [dia_chi_cu_the] NVARCHAR(500),
        [loai_dia_chi] INT,
        [is_mac_dinh] BIT,
        [trang_thai] INT
    );';

    IF COL_LENGTH(N'dbo.dia_chi_khach_hang', N'id') IS NULL
        THROW 51001, N'Existing table dia_chi_khach_hang has no id; manual recovery required.', 1;

    IF COL_LENGTH(N'dbo.dia_chi_khach_hang', N'id_khach_hang') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[dia_chi_khach_hang] ADD [id_khach_hang] BIGINT NULL;';

    IF COL_LENGTH(N'dbo.dia_chi_khach_hang', N'ten_nguoi_nhan') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[dia_chi_khach_hang] ADD [ten_nguoi_nhan] NVARCHAR(255) NULL;';

    IF COL_LENGTH(N'dbo.dia_chi_khach_hang', N'sdt_nguoi_nhan') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[dia_chi_khach_hang] ADD [sdt_nguoi_nhan] VARCHAR(20) NULL;';

    IF COL_LENGTH(N'dbo.dia_chi_khach_hang', N'tinh_thanh') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[dia_chi_khach_hang] ADD [tinh_thanh] NVARCHAR(100) NULL;';

    IF COL_LENGTH(N'dbo.dia_chi_khach_hang', N'quan_huyen') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[dia_chi_khach_hang] ADD [quan_huyen] NVARCHAR(100) NULL;';

    IF COL_LENGTH(N'dbo.dia_chi_khach_hang', N'phuong_xa') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[dia_chi_khach_hang] ADD [phuong_xa] NVARCHAR(100) NULL;';

    IF COL_LENGTH(N'dbo.dia_chi_khach_hang', N'dia_chi_cu_the') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[dia_chi_khach_hang] ADD [dia_chi_cu_the] NVARCHAR(500) NULL;';

    IF COL_LENGTH(N'dbo.dia_chi_khach_hang', N'loai_dia_chi') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[dia_chi_khach_hang] ADD [loai_dia_chi] INT NULL;';

    IF COL_LENGTH(N'dbo.dia_chi_khach_hang', N'is_mac_dinh') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[dia_chi_khach_hang] ADD [is_mac_dinh] BIT NULL;';

    IF COL_LENGTH(N'dbo.dia_chi_khach_hang', N'trang_thai') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[dia_chi_khach_hang] ADD [trang_thai] INT NULL;';

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.dia_chi_khach_hang') AND c.name=N'id' AND (TYPE_NAME(c.user_type_id) <> N'bigint' OR c.is_identity <> 1))
        THROW 51002, N'Incompatible type: dia_chi_khach_hang.id. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.dia_chi_khach_hang') AND c.name=N'id_khach_hang' AND (TYPE_NAME(c.user_type_id) <> N'bigint'))
        THROW 51002, N'Incompatible type: dia_chi_khach_hang.id_khach_hang. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.dia_chi_khach_hang') AND c.name=N'ten_nguoi_nhan' AND (TYPE_NAME(c.user_type_id) <> N'nvarchar' OR (c.max_length <> -1 AND c.max_length < 510)))
        THROW 51002, N'Incompatible type: dia_chi_khach_hang.ten_nguoi_nhan. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.dia_chi_khach_hang') AND c.name=N'sdt_nguoi_nhan' AND (TYPE_NAME(c.user_type_id) <> N'varchar' OR (c.max_length <> -1 AND c.max_length < 20)))
        THROW 51002, N'Incompatible type: dia_chi_khach_hang.sdt_nguoi_nhan. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.dia_chi_khach_hang') AND c.name=N'tinh_thanh' AND (TYPE_NAME(c.user_type_id) <> N'nvarchar' OR (c.max_length <> -1 AND c.max_length < 200)))
        THROW 51002, N'Incompatible type: dia_chi_khach_hang.tinh_thanh. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.dia_chi_khach_hang') AND c.name=N'quan_huyen' AND (TYPE_NAME(c.user_type_id) <> N'nvarchar' OR (c.max_length <> -1 AND c.max_length < 200)))
        THROW 51002, N'Incompatible type: dia_chi_khach_hang.quan_huyen. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.dia_chi_khach_hang') AND c.name=N'phuong_xa' AND (TYPE_NAME(c.user_type_id) <> N'nvarchar' OR (c.max_length <> -1 AND c.max_length < 200)))
        THROW 51002, N'Incompatible type: dia_chi_khach_hang.phuong_xa. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.dia_chi_khach_hang') AND c.name=N'dia_chi_cu_the' AND (TYPE_NAME(c.user_type_id) <> N'nvarchar' OR (c.max_length <> -1 AND c.max_length < 1000)))
        THROW 51002, N'Incompatible type: dia_chi_khach_hang.dia_chi_cu_the. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.dia_chi_khach_hang') AND c.name=N'loai_dia_chi' AND (TYPE_NAME(c.user_type_id) <> N'int'))
        THROW 51002, N'Incompatible type: dia_chi_khach_hang.loai_dia_chi. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.dia_chi_khach_hang') AND c.name=N'is_mac_dinh' AND (TYPE_NAME(c.user_type_id) <> N'bit'))
        THROW 51002, N'Incompatible type: dia_chi_khach_hang.is_mac_dinh. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.dia_chi_khach_hang') AND c.name=N'trang_thai' AND (TYPE_NAME(c.user_type_id) <> N'int'))
        THROW 51002, N'Incompatible type: dia_chi_khach_hang.trang_thai. No automatic data conversion.', 1;

    IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE parent_object_id=OBJECT_ID(N'dbo.dia_chi_khach_hang') AND type='PK')
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[dia_chi_khach_hang] ADD CONSTRAINT [PK_dia_chi_khach_hang] PRIMARY KEY ([id]);';

    IF OBJECT_ID(N'dbo.danh_muc', N'U') IS NULL
        EXEC sys.sp_executesql N'CREATE TABLE dbo.[danh_muc] (
        [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [PK_danh_muc] PRIMARY KEY,
        [ma_danh_muc] VARCHAR(50),
        [ten_danh_muc] NVARCHAR(255),
        [mo_ta] NVARCHAR(1000),
        [trang_thai] INT
    );';

    IF COL_LENGTH(N'dbo.danh_muc', N'id') IS NULL
        THROW 51001, N'Existing table danh_muc has no id; manual recovery required.', 1;

    IF COL_LENGTH(N'dbo.danh_muc', N'ma_danh_muc') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[danh_muc] ADD [ma_danh_muc] VARCHAR(50) NULL;';

    IF COL_LENGTH(N'dbo.danh_muc', N'ten_danh_muc') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[danh_muc] ADD [ten_danh_muc] NVARCHAR(255) NULL;';

    IF COL_LENGTH(N'dbo.danh_muc', N'mo_ta') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[danh_muc] ADD [mo_ta] NVARCHAR(1000) NULL;';

    IF COL_LENGTH(N'dbo.danh_muc', N'trang_thai') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[danh_muc] ADD [trang_thai] INT NULL;';

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.danh_muc') AND c.name=N'id' AND (TYPE_NAME(c.user_type_id) <> N'bigint' OR c.is_identity <> 1))
        THROW 51002, N'Incompatible type: danh_muc.id. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.danh_muc') AND c.name=N'ma_danh_muc' AND (TYPE_NAME(c.user_type_id) <> N'varchar' OR (c.max_length <> -1 AND c.max_length < 50)))
        THROW 51002, N'Incompatible type: danh_muc.ma_danh_muc. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.danh_muc') AND c.name=N'ten_danh_muc' AND (TYPE_NAME(c.user_type_id) <> N'nvarchar' OR (c.max_length <> -1 AND c.max_length < 510)))
        THROW 51002, N'Incompatible type: danh_muc.ten_danh_muc. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.danh_muc') AND c.name=N'mo_ta' AND (TYPE_NAME(c.user_type_id) <> N'nvarchar' OR (c.max_length <> -1 AND c.max_length < 2000)))
        THROW 51002, N'Incompatible type: danh_muc.mo_ta. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.danh_muc') AND c.name=N'trang_thai' AND (TYPE_NAME(c.user_type_id) <> N'int'))
        THROW 51002, N'Incompatible type: danh_muc.trang_thai. No automatic data conversion.', 1;

    IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE parent_object_id=OBJECT_ID(N'dbo.danh_muc') AND type='PK')
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[danh_muc] ADD CONSTRAINT [PK_danh_muc] PRIMARY KEY ([id]);';

    IF OBJECT_ID(N'dbo.thuong_hieu', N'U') IS NULL
        EXEC sys.sp_executesql N'CREATE TABLE dbo.[thuong_hieu] (
        [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [PK_thuong_hieu] PRIMARY KEY,
        [ma_thuong_hieu] VARCHAR(50),
        [ten_thuong_hieu] NVARCHAR(255),
        [trang_thai] INT
    );';

    IF COL_LENGTH(N'dbo.thuong_hieu', N'id') IS NULL
        THROW 51001, N'Existing table thuong_hieu has no id; manual recovery required.', 1;

    IF COL_LENGTH(N'dbo.thuong_hieu', N'ma_thuong_hieu') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[thuong_hieu] ADD [ma_thuong_hieu] VARCHAR(50) NULL;';

    IF COL_LENGTH(N'dbo.thuong_hieu', N'ten_thuong_hieu') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[thuong_hieu] ADD [ten_thuong_hieu] NVARCHAR(255) NULL;';

    IF COL_LENGTH(N'dbo.thuong_hieu', N'trang_thai') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[thuong_hieu] ADD [trang_thai] INT NULL;';

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.thuong_hieu') AND c.name=N'id' AND (TYPE_NAME(c.user_type_id) <> N'bigint' OR c.is_identity <> 1))
        THROW 51002, N'Incompatible type: thuong_hieu.id. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.thuong_hieu') AND c.name=N'ma_thuong_hieu' AND (TYPE_NAME(c.user_type_id) <> N'varchar' OR (c.max_length <> -1 AND c.max_length < 50)))
        THROW 51002, N'Incompatible type: thuong_hieu.ma_thuong_hieu. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.thuong_hieu') AND c.name=N'ten_thuong_hieu' AND (TYPE_NAME(c.user_type_id) <> N'nvarchar' OR (c.max_length <> -1 AND c.max_length < 510)))
        THROW 51002, N'Incompatible type: thuong_hieu.ten_thuong_hieu. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.thuong_hieu') AND c.name=N'trang_thai' AND (TYPE_NAME(c.user_type_id) <> N'int'))
        THROW 51002, N'Incompatible type: thuong_hieu.trang_thai. No automatic data conversion.', 1;

    IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE parent_object_id=OBJECT_ID(N'dbo.thuong_hieu') AND type='PK')
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[thuong_hieu] ADD CONSTRAINT [PK_thuong_hieu] PRIMARY KEY ([id]);';

    IF OBJECT_ID(N'dbo.chat_lieu', N'U') IS NULL
        EXEC sys.sp_executesql N'CREATE TABLE dbo.[chat_lieu] (
        [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [PK_chat_lieu] PRIMARY KEY,
        [ma_chat_lieu] VARCHAR(50),
        [ten_chat_lieu] NVARCHAR(255),
        [trang_thai] INT
    );';

    IF COL_LENGTH(N'dbo.chat_lieu', N'id') IS NULL
        THROW 51001, N'Existing table chat_lieu has no id; manual recovery required.', 1;

    IF COL_LENGTH(N'dbo.chat_lieu', N'ma_chat_lieu') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[chat_lieu] ADD [ma_chat_lieu] VARCHAR(50) NULL;';

    IF COL_LENGTH(N'dbo.chat_lieu', N'ten_chat_lieu') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[chat_lieu] ADD [ten_chat_lieu] NVARCHAR(255) NULL;';

    IF COL_LENGTH(N'dbo.chat_lieu', N'trang_thai') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[chat_lieu] ADD [trang_thai] INT NULL;';

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.chat_lieu') AND c.name=N'id' AND (TYPE_NAME(c.user_type_id) <> N'bigint' OR c.is_identity <> 1))
        THROW 51002, N'Incompatible type: chat_lieu.id. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.chat_lieu') AND c.name=N'ma_chat_lieu' AND (TYPE_NAME(c.user_type_id) <> N'varchar' OR (c.max_length <> -1 AND c.max_length < 50)))
        THROW 51002, N'Incompatible type: chat_lieu.ma_chat_lieu. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.chat_lieu') AND c.name=N'ten_chat_lieu' AND (TYPE_NAME(c.user_type_id) <> N'nvarchar' OR (c.max_length <> -1 AND c.max_length < 510)))
        THROW 51002, N'Incompatible type: chat_lieu.ten_chat_lieu. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.chat_lieu') AND c.name=N'trang_thai' AND (TYPE_NAME(c.user_type_id) <> N'int'))
        THROW 51002, N'Incompatible type: chat_lieu.trang_thai. No automatic data conversion.', 1;

    IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE parent_object_id=OBJECT_ID(N'dbo.chat_lieu') AND type='PK')
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[chat_lieu] ADD CONSTRAINT [PK_chat_lieu] PRIMARY KEY ([id]);';

    IF OBJECT_ID(N'dbo.xuat_xu', N'U') IS NULL
        EXEC sys.sp_executesql N'CREATE TABLE dbo.[xuat_xu] (
        [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [PK_xuat_xu] PRIMARY KEY,
        [ma_xuat_xu] VARCHAR(50),
        [ten_xuat_xu] NVARCHAR(255),
        [trang_thai] INT
    );';

    IF COL_LENGTH(N'dbo.xuat_xu', N'id') IS NULL
        THROW 51001, N'Existing table xuat_xu has no id; manual recovery required.', 1;

    IF COL_LENGTH(N'dbo.xuat_xu', N'ma_xuat_xu') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[xuat_xu] ADD [ma_xuat_xu] VARCHAR(50) NULL;';

    IF COL_LENGTH(N'dbo.xuat_xu', N'ten_xuat_xu') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[xuat_xu] ADD [ten_xuat_xu] NVARCHAR(255) NULL;';

    IF COL_LENGTH(N'dbo.xuat_xu', N'trang_thai') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[xuat_xu] ADD [trang_thai] INT NULL;';

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.xuat_xu') AND c.name=N'id' AND (TYPE_NAME(c.user_type_id) <> N'bigint' OR c.is_identity <> 1))
        THROW 51002, N'Incompatible type: xuat_xu.id. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.xuat_xu') AND c.name=N'ma_xuat_xu' AND (TYPE_NAME(c.user_type_id) <> N'varchar' OR (c.max_length <> -1 AND c.max_length < 50)))
        THROW 51002, N'Incompatible type: xuat_xu.ma_xuat_xu. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.xuat_xu') AND c.name=N'ten_xuat_xu' AND (TYPE_NAME(c.user_type_id) <> N'nvarchar' OR (c.max_length <> -1 AND c.max_length < 510)))
        THROW 51002, N'Incompatible type: xuat_xu.ten_xuat_xu. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.xuat_xu') AND c.name=N'trang_thai' AND (TYPE_NAME(c.user_type_id) <> N'int'))
        THROW 51002, N'Incompatible type: xuat_xu.trang_thai. No automatic data conversion.', 1;

    IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE parent_object_id=OBJECT_ID(N'dbo.xuat_xu') AND type='PK')
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[xuat_xu] ADD CONSTRAINT [PK_xuat_xu] PRIMARY KEY ([id]);';

    IF OBJECT_ID(N'dbo.co_giay', N'U') IS NULL
        EXEC sys.sp_executesql N'CREATE TABLE dbo.[co_giay] (
        [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [PK_co_giay] PRIMARY KEY,
        [ma_co_giay] VARCHAR(50),
        [ten_co_giay] NVARCHAR(255),
        [trang_thai] INT
    );';

    IF COL_LENGTH(N'dbo.co_giay', N'id') IS NULL
        THROW 51001, N'Existing table co_giay has no id; manual recovery required.', 1;

    IF COL_LENGTH(N'dbo.co_giay', N'ma_co_giay') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[co_giay] ADD [ma_co_giay] VARCHAR(50) NULL;';

    IF COL_LENGTH(N'dbo.co_giay', N'ten_co_giay') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[co_giay] ADD [ten_co_giay] NVARCHAR(255) NULL;';

    IF COL_LENGTH(N'dbo.co_giay', N'trang_thai') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[co_giay] ADD [trang_thai] INT NULL;';

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.co_giay') AND c.name=N'id' AND (TYPE_NAME(c.user_type_id) <> N'bigint' OR c.is_identity <> 1))
        THROW 51002, N'Incompatible type: co_giay.id. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.co_giay') AND c.name=N'ma_co_giay' AND (TYPE_NAME(c.user_type_id) <> N'varchar' OR (c.max_length <> -1 AND c.max_length < 50)))
        THROW 51002, N'Incompatible type: co_giay.ma_co_giay. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.co_giay') AND c.name=N'ten_co_giay' AND (TYPE_NAME(c.user_type_id) <> N'nvarchar' OR (c.max_length <> -1 AND c.max_length < 510)))
        THROW 51002, N'Incompatible type: co_giay.ten_co_giay. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.co_giay') AND c.name=N'trang_thai' AND (TYPE_NAME(c.user_type_id) <> N'int'))
        THROW 51002, N'Incompatible type: co_giay.trang_thai. No automatic data conversion.', 1;

    IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE parent_object_id=OBJECT_ID(N'dbo.co_giay') AND type='PK')
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[co_giay] ADD CONSTRAINT [PK_co_giay] PRIMARY KEY ([id]);';

    IF OBJECT_ID(N'dbo.kieu_dang', N'U') IS NULL
        EXEC sys.sp_executesql N'CREATE TABLE dbo.[kieu_dang] (
        [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [PK_kieu_dang] PRIMARY KEY,
        [ma_kieu_dang] VARCHAR(50),
        [ten_kieu_dang] NVARCHAR(255),
        [trang_thai] INT
    );';

    IF COL_LENGTH(N'dbo.kieu_dang', N'id') IS NULL
        THROW 51001, N'Existing table kieu_dang has no id; manual recovery required.', 1;

    IF COL_LENGTH(N'dbo.kieu_dang', N'ma_kieu_dang') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[kieu_dang] ADD [ma_kieu_dang] VARCHAR(50) NULL;';

    IF COL_LENGTH(N'dbo.kieu_dang', N'ten_kieu_dang') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[kieu_dang] ADD [ten_kieu_dang] NVARCHAR(255) NULL;';

    IF COL_LENGTH(N'dbo.kieu_dang', N'trang_thai') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[kieu_dang] ADD [trang_thai] INT NULL;';

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.kieu_dang') AND c.name=N'id' AND (TYPE_NAME(c.user_type_id) <> N'bigint' OR c.is_identity <> 1))
        THROW 51002, N'Incompatible type: kieu_dang.id. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.kieu_dang') AND c.name=N'ma_kieu_dang' AND (TYPE_NAME(c.user_type_id) <> N'varchar' OR (c.max_length <> -1 AND c.max_length < 50)))
        THROW 51002, N'Incompatible type: kieu_dang.ma_kieu_dang. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.kieu_dang') AND c.name=N'ten_kieu_dang' AND (TYPE_NAME(c.user_type_id) <> N'nvarchar' OR (c.max_length <> -1 AND c.max_length < 510)))
        THROW 51002, N'Incompatible type: kieu_dang.ten_kieu_dang. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.kieu_dang') AND c.name=N'trang_thai' AND (TYPE_NAME(c.user_type_id) <> N'int'))
        THROW 51002, N'Incompatible type: kieu_dang.trang_thai. No automatic data conversion.', 1;

    IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE parent_object_id=OBJECT_ID(N'dbo.kieu_dang') AND type='PK')
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[kieu_dang] ADD CONSTRAINT [PK_kieu_dang] PRIMARY KEY ([id]);';

    IF OBJECT_ID(N'dbo.mau_sac', N'U') IS NULL
        EXEC sys.sp_executesql N'CREATE TABLE dbo.[mau_sac] (
        [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [PK_mau_sac] PRIMARY KEY,
        [ma_mau_sac] VARCHAR(50),
        [ten_mau_sac] NVARCHAR(255),
        [ma_mau_hex] VARCHAR(20),
        [trang_thai] INT,
        [ngay_tao] DATETIME2,
        [ngay_cap_nhat] DATETIME2
    );';

    IF COL_LENGTH(N'dbo.mau_sac', N'id') IS NULL
        THROW 51001, N'Existing table mau_sac has no id; manual recovery required.', 1;

    IF COL_LENGTH(N'dbo.mau_sac', N'ma_mau_sac') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[mau_sac] ADD [ma_mau_sac] VARCHAR(50) NULL;';

    IF COL_LENGTH(N'dbo.mau_sac', N'ten_mau_sac') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[mau_sac] ADD [ten_mau_sac] NVARCHAR(255) NULL;';

    IF COL_LENGTH(N'dbo.mau_sac', N'ma_mau_hex') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[mau_sac] ADD [ma_mau_hex] VARCHAR(20) NULL;';

    IF COL_LENGTH(N'dbo.mau_sac', N'trang_thai') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[mau_sac] ADD [trang_thai] INT NULL;';

    IF COL_LENGTH(N'dbo.mau_sac', N'ngay_tao') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[mau_sac] ADD [ngay_tao] DATETIME2 NULL;';

    IF COL_LENGTH(N'dbo.mau_sac', N'ngay_cap_nhat') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[mau_sac] ADD [ngay_cap_nhat] DATETIME2 NULL;';

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.mau_sac') AND c.name=N'id' AND (TYPE_NAME(c.user_type_id) <> N'bigint' OR c.is_identity <> 1))
        THROW 51002, N'Incompatible type: mau_sac.id. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.mau_sac') AND c.name=N'ma_mau_sac' AND (TYPE_NAME(c.user_type_id) <> N'varchar' OR (c.max_length <> -1 AND c.max_length < 50)))
        THROW 51002, N'Incompatible type: mau_sac.ma_mau_sac. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.mau_sac') AND c.name=N'ten_mau_sac' AND (TYPE_NAME(c.user_type_id) <> N'nvarchar' OR (c.max_length <> -1 AND c.max_length < 510)))
        THROW 51002, N'Incompatible type: mau_sac.ten_mau_sac. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.mau_sac') AND c.name=N'ma_mau_hex' AND (TYPE_NAME(c.user_type_id) <> N'varchar' OR (c.max_length <> -1 AND c.max_length < 20)))
        THROW 51002, N'Incompatible type: mau_sac.ma_mau_hex. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.mau_sac') AND c.name=N'trang_thai' AND (TYPE_NAME(c.user_type_id) <> N'int'))
        THROW 51002, N'Incompatible type: mau_sac.trang_thai. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.mau_sac') AND c.name=N'ngay_tao' AND (TYPE_NAME(c.user_type_id) <> N'datetime2'))
        THROW 51002, N'Incompatible type: mau_sac.ngay_tao. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.mau_sac') AND c.name=N'ngay_cap_nhat' AND (TYPE_NAME(c.user_type_id) <> N'datetime2'))
        THROW 51002, N'Incompatible type: mau_sac.ngay_cap_nhat. No automatic data conversion.', 1;

    IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE parent_object_id=OBJECT_ID(N'dbo.mau_sac') AND type='PK')
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[mau_sac] ADD CONSTRAINT [PK_mau_sac] PRIMARY KEY ([id]);';

    IF OBJECT_ID(N'dbo.kich_thuoc', N'U') IS NULL
        EXEC sys.sp_executesql N'CREATE TABLE dbo.[kich_thuoc] (
        [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [PK_kich_thuoc] PRIMARY KEY,
        [gia_tri] VARCHAR(50),
        [ghi_chu] NVARCHAR(1000),
        [trang_thai] INT,
        [ngay_tao] DATETIME2,
        [ngay_cap_nhat] DATETIME2
    );';

    IF COL_LENGTH(N'dbo.kich_thuoc', N'id') IS NULL
        THROW 51001, N'Existing table kich_thuoc has no id; manual recovery required.', 1;

    IF COL_LENGTH(N'dbo.kich_thuoc', N'gia_tri') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[kich_thuoc] ADD [gia_tri] VARCHAR(50) NULL;';

    IF COL_LENGTH(N'dbo.kich_thuoc', N'ghi_chu') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[kich_thuoc] ADD [ghi_chu] NVARCHAR(1000) NULL;';

    IF COL_LENGTH(N'dbo.kich_thuoc', N'trang_thai') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[kich_thuoc] ADD [trang_thai] INT NULL;';

    IF COL_LENGTH(N'dbo.kich_thuoc', N'ngay_tao') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[kich_thuoc] ADD [ngay_tao] DATETIME2 NULL;';

    IF COL_LENGTH(N'dbo.kich_thuoc', N'ngay_cap_nhat') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[kich_thuoc] ADD [ngay_cap_nhat] DATETIME2 NULL;';

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.kich_thuoc') AND c.name=N'id' AND (TYPE_NAME(c.user_type_id) <> N'bigint' OR c.is_identity <> 1))
        THROW 51002, N'Incompatible type: kich_thuoc.id. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.kich_thuoc') AND c.name=N'gia_tri' AND (TYPE_NAME(c.user_type_id) <> N'varchar' OR (c.max_length <> -1 AND c.max_length < 50)))
        THROW 51002, N'Incompatible type: kich_thuoc.gia_tri. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.kich_thuoc') AND c.name=N'ghi_chu' AND (TYPE_NAME(c.user_type_id) <> N'nvarchar' OR (c.max_length <> -1 AND c.max_length < 2000)))
        THROW 51002, N'Incompatible type: kich_thuoc.ghi_chu. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.kich_thuoc') AND c.name=N'trang_thai' AND (TYPE_NAME(c.user_type_id) <> N'int'))
        THROW 51002, N'Incompatible type: kich_thuoc.trang_thai. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.kich_thuoc') AND c.name=N'ngay_tao' AND (TYPE_NAME(c.user_type_id) <> N'datetime2'))
        THROW 51002, N'Incompatible type: kich_thuoc.ngay_tao. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.kich_thuoc') AND c.name=N'ngay_cap_nhat' AND (TYPE_NAME(c.user_type_id) <> N'datetime2'))
        THROW 51002, N'Incompatible type: kich_thuoc.ngay_cap_nhat. No automatic data conversion.', 1;

    IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE parent_object_id=OBJECT_ID(N'dbo.kich_thuoc') AND type='PK')
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[kich_thuoc] ADD CONSTRAINT [PK_kich_thuoc] PRIMARY KEY ([id]);';

    IF OBJECT_ID(N'dbo.san_pham', N'U') IS NULL
        EXEC sys.sp_executesql N'CREATE TABLE dbo.[san_pham] (
        [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [PK_san_pham] PRIMARY KEY,
        [id_danh_muc] BIGINT,
        [id_thuong_hieu] BIGINT,
        [id_chat_lieu] BIGINT,
        [id_kieu_dang] BIGINT,
        [id_co_giay] BIGINT,
        [id_xuat_xu] BIGINT,
        [ma_san_pham] VARCHAR(50),
        [ten_san_pham] NVARCHAR(255),
        [mo_ta_chi_tiet] NVARCHAR(MAX),
        [ngay_tao] DATETIME2,
        [nguoi_tao] BIGINT,
        [nguoi_cap_nhat] BIGINT,
        [ngay_cap_nhat] DATETIME2,
        [trang_thai] INT
    );';

    IF COL_LENGTH(N'dbo.san_pham', N'id') IS NULL
        THROW 51001, N'Existing table san_pham has no id; manual recovery required.', 1;

    IF COL_LENGTH(N'dbo.san_pham', N'id_danh_muc') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[san_pham] ADD [id_danh_muc] BIGINT NULL;';

    IF COL_LENGTH(N'dbo.san_pham', N'id_thuong_hieu') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[san_pham] ADD [id_thuong_hieu] BIGINT NULL;';

    IF COL_LENGTH(N'dbo.san_pham', N'id_chat_lieu') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[san_pham] ADD [id_chat_lieu] BIGINT NULL;';

    IF COL_LENGTH(N'dbo.san_pham', N'id_kieu_dang') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[san_pham] ADD [id_kieu_dang] BIGINT NULL;';

    IF COL_LENGTH(N'dbo.san_pham', N'id_co_giay') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[san_pham] ADD [id_co_giay] BIGINT NULL;';

    IF COL_LENGTH(N'dbo.san_pham', N'id_xuat_xu') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[san_pham] ADD [id_xuat_xu] BIGINT NULL;';

    IF COL_LENGTH(N'dbo.san_pham', N'ma_san_pham') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[san_pham] ADD [ma_san_pham] VARCHAR(50) NULL;';

    IF COL_LENGTH(N'dbo.san_pham', N'ten_san_pham') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[san_pham] ADD [ten_san_pham] NVARCHAR(255) NULL;';

    IF COL_LENGTH(N'dbo.san_pham', N'mo_ta_chi_tiet') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[san_pham] ADD [mo_ta_chi_tiet] NVARCHAR(MAX) NULL;';

    IF COL_LENGTH(N'dbo.san_pham', N'ngay_tao') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[san_pham] ADD [ngay_tao] DATETIME2 NULL;';

    IF COL_LENGTH(N'dbo.san_pham', N'nguoi_tao') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[san_pham] ADD [nguoi_tao] BIGINT NULL;';

    IF COL_LENGTH(N'dbo.san_pham', N'nguoi_cap_nhat') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[san_pham] ADD [nguoi_cap_nhat] BIGINT NULL;';

    IF COL_LENGTH(N'dbo.san_pham', N'ngay_cap_nhat') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[san_pham] ADD [ngay_cap_nhat] DATETIME2 NULL;';

    IF COL_LENGTH(N'dbo.san_pham', N'trang_thai') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[san_pham] ADD [trang_thai] INT NULL;';

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.san_pham') AND c.name=N'id' AND (TYPE_NAME(c.user_type_id) <> N'bigint' OR c.is_identity <> 1))
        THROW 51002, N'Incompatible type: san_pham.id. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.san_pham') AND c.name=N'id_danh_muc' AND (TYPE_NAME(c.user_type_id) <> N'bigint'))
        THROW 51002, N'Incompatible type: san_pham.id_danh_muc. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.san_pham') AND c.name=N'id_thuong_hieu' AND (TYPE_NAME(c.user_type_id) <> N'bigint'))
        THROW 51002, N'Incompatible type: san_pham.id_thuong_hieu. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.san_pham') AND c.name=N'id_chat_lieu' AND (TYPE_NAME(c.user_type_id) <> N'bigint'))
        THROW 51002, N'Incompatible type: san_pham.id_chat_lieu. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.san_pham') AND c.name=N'id_kieu_dang' AND (TYPE_NAME(c.user_type_id) <> N'bigint'))
        THROW 51002, N'Incompatible type: san_pham.id_kieu_dang. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.san_pham') AND c.name=N'id_co_giay' AND (TYPE_NAME(c.user_type_id) <> N'bigint'))
        THROW 51002, N'Incompatible type: san_pham.id_co_giay. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.san_pham') AND c.name=N'id_xuat_xu' AND (TYPE_NAME(c.user_type_id) <> N'bigint'))
        THROW 51002, N'Incompatible type: san_pham.id_xuat_xu. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.san_pham') AND c.name=N'ma_san_pham' AND (TYPE_NAME(c.user_type_id) <> N'varchar' OR (c.max_length <> -1 AND c.max_length < 50)))
        THROW 51002, N'Incompatible type: san_pham.ma_san_pham. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.san_pham') AND c.name=N'ten_san_pham' AND (TYPE_NAME(c.user_type_id) <> N'nvarchar' OR (c.max_length <> -1 AND c.max_length < 510)))
        THROW 51002, N'Incompatible type: san_pham.ten_san_pham. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.san_pham') AND c.name=N'mo_ta_chi_tiet' AND (TYPE_NAME(c.user_type_id) <> N'nvarchar' OR c.max_length <> -1))
        THROW 51002, N'Incompatible type: san_pham.mo_ta_chi_tiet. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.san_pham') AND c.name=N'ngay_tao' AND (TYPE_NAME(c.user_type_id) <> N'datetime2'))
        THROW 51002, N'Incompatible type: san_pham.ngay_tao. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.san_pham') AND c.name=N'nguoi_tao' AND (TYPE_NAME(c.user_type_id) <> N'bigint'))
        THROW 51002, N'Incompatible type: san_pham.nguoi_tao. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.san_pham') AND c.name=N'nguoi_cap_nhat' AND (TYPE_NAME(c.user_type_id) <> N'bigint'))
        THROW 51002, N'Incompatible type: san_pham.nguoi_cap_nhat. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.san_pham') AND c.name=N'ngay_cap_nhat' AND (TYPE_NAME(c.user_type_id) <> N'datetime2'))
        THROW 51002, N'Incompatible type: san_pham.ngay_cap_nhat. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.san_pham') AND c.name=N'trang_thai' AND (TYPE_NAME(c.user_type_id) <> N'int'))
        THROW 51002, N'Incompatible type: san_pham.trang_thai. No automatic data conversion.', 1;

    IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE parent_object_id=OBJECT_ID(N'dbo.san_pham') AND type='PK')
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[san_pham] ADD CONSTRAINT [PK_san_pham] PRIMARY KEY ([id]);';

    IF OBJECT_ID(N'dbo.san_pham_chi_tiet', N'U') IS NULL
        EXEC sys.sp_executesql N'CREATE TABLE dbo.[san_pham_chi_tiet] (
        [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [PK_san_pham_chi_tiet] PRIMARY KEY,
        [id_san_pham] BIGINT NOT NULL,
        [id_mau_sac] BIGINT NOT NULL,
        [id_kich_thuoc] BIGINT NOT NULL,
        [ma_chi_tiet_san_pham] VARCHAR(100),
        [so_luong] INT,
        [gia_ban] DECIMAL(18,2),
        [sku] VARCHAR(100),
        [kich_hoat] BIT,
        [ngay_tao] DATETIME2,
        [ngay_cap_nhat] DATETIME2,
        [trang_thai] INT
    );';

    IF COL_LENGTH(N'dbo.san_pham_chi_tiet', N'id') IS NULL
        THROW 51001, N'Existing table san_pham_chi_tiet has no id; manual recovery required.', 1;

    IF COL_LENGTH(N'dbo.san_pham_chi_tiet', N'id_san_pham') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[san_pham_chi_tiet] ADD [id_san_pham] BIGINT NULL;';

    IF COL_LENGTH(N'dbo.san_pham_chi_tiet', N'id_mau_sac') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[san_pham_chi_tiet] ADD [id_mau_sac] BIGINT NULL;';

    IF COL_LENGTH(N'dbo.san_pham_chi_tiet', N'id_kich_thuoc') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[san_pham_chi_tiet] ADD [id_kich_thuoc] BIGINT NULL;';

    IF COL_LENGTH(N'dbo.san_pham_chi_tiet', N'ma_chi_tiet_san_pham') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[san_pham_chi_tiet] ADD [ma_chi_tiet_san_pham] VARCHAR(100) NULL;';

    IF COL_LENGTH(N'dbo.san_pham_chi_tiet', N'so_luong') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[san_pham_chi_tiet] ADD [so_luong] INT NULL;';

    IF COL_LENGTH(N'dbo.san_pham_chi_tiet', N'gia_ban') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[san_pham_chi_tiet] ADD [gia_ban] DECIMAL(18,2) NULL;';

    IF COL_LENGTH(N'dbo.san_pham_chi_tiet', N'sku') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[san_pham_chi_tiet] ADD [sku] VARCHAR(100) NULL;';

    IF COL_LENGTH(N'dbo.san_pham_chi_tiet', N'kich_hoat') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[san_pham_chi_tiet] ADD [kich_hoat] BIT NULL;';

    IF COL_LENGTH(N'dbo.san_pham_chi_tiet', N'ngay_tao') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[san_pham_chi_tiet] ADD [ngay_tao] DATETIME2 NULL;';

    IF COL_LENGTH(N'dbo.san_pham_chi_tiet', N'ngay_cap_nhat') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[san_pham_chi_tiet] ADD [ngay_cap_nhat] DATETIME2 NULL;';

    IF COL_LENGTH(N'dbo.san_pham_chi_tiet', N'trang_thai') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[san_pham_chi_tiet] ADD [trang_thai] INT NULL;';

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.san_pham_chi_tiet') AND c.name=N'id' AND (TYPE_NAME(c.user_type_id) <> N'bigint' OR c.is_identity <> 1))
        THROW 51002, N'Incompatible type: san_pham_chi_tiet.id. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.san_pham_chi_tiet') AND c.name=N'id_san_pham' AND (TYPE_NAME(c.user_type_id) <> N'bigint'))
        THROW 51002, N'Incompatible type: san_pham_chi_tiet.id_san_pham. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.san_pham_chi_tiet') AND c.name=N'id_mau_sac' AND (TYPE_NAME(c.user_type_id) <> N'bigint'))
        THROW 51002, N'Incompatible type: san_pham_chi_tiet.id_mau_sac. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.san_pham_chi_tiet') AND c.name=N'id_kich_thuoc' AND (TYPE_NAME(c.user_type_id) <> N'bigint'))
        THROW 51002, N'Incompatible type: san_pham_chi_tiet.id_kich_thuoc. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.san_pham_chi_tiet') AND c.name=N'ma_chi_tiet_san_pham' AND (TYPE_NAME(c.user_type_id) <> N'varchar' OR (c.max_length <> -1 AND c.max_length < 100)))
        THROW 51002, N'Incompatible type: san_pham_chi_tiet.ma_chi_tiet_san_pham. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.san_pham_chi_tiet') AND c.name=N'so_luong' AND (TYPE_NAME(c.user_type_id) <> N'int'))
        THROW 51002, N'Incompatible type: san_pham_chi_tiet.so_luong. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.san_pham_chi_tiet') AND c.name=N'gia_ban' AND ((TYPE_NAME(c.user_type_id) NOT IN (N'decimal',N'numeric') OR c.precision < 18 OR c.scale <> 2)))
        THROW 51002, N'Incompatible type: san_pham_chi_tiet.gia_ban. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.san_pham_chi_tiet') AND c.name=N'sku' AND (TYPE_NAME(c.user_type_id) <> N'varchar' OR (c.max_length <> -1 AND c.max_length < 100)))
        THROW 51002, N'Incompatible type: san_pham_chi_tiet.sku. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.san_pham_chi_tiet') AND c.name=N'kich_hoat' AND (TYPE_NAME(c.user_type_id) <> N'bit'))
        THROW 51002, N'Incompatible type: san_pham_chi_tiet.kich_hoat. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.san_pham_chi_tiet') AND c.name=N'ngay_tao' AND (TYPE_NAME(c.user_type_id) <> N'datetime2'))
        THROW 51002, N'Incompatible type: san_pham_chi_tiet.ngay_tao. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.san_pham_chi_tiet') AND c.name=N'ngay_cap_nhat' AND (TYPE_NAME(c.user_type_id) <> N'datetime2'))
        THROW 51002, N'Incompatible type: san_pham_chi_tiet.ngay_cap_nhat. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.san_pham_chi_tiet') AND c.name=N'trang_thai' AND (TYPE_NAME(c.user_type_id) <> N'int'))
        THROW 51002, N'Incompatible type: san_pham_chi_tiet.trang_thai. No automatic data conversion.', 1;

    IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE parent_object_id=OBJECT_ID(N'dbo.san_pham_chi_tiet') AND type='PK')
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[san_pham_chi_tiet] ADD CONSTRAINT [PK_san_pham_chi_tiet] PRIMARY KEY ([id]);';

    IF OBJECT_ID(N'dbo.hinh_anh_san_pham', N'U') IS NULL
        EXEC sys.sp_executesql N'CREATE TABLE dbo.[hinh_anh_san_pham] (
        [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [PK_hinh_anh_san_pham] PRIMARY KEY,
        [id_san_pham] BIGINT NOT NULL,
        [url_anh] NVARCHAR(1000),
        [is_anh_chinh] BIT
    );';

    IF COL_LENGTH(N'dbo.hinh_anh_san_pham', N'id') IS NULL
        THROW 51001, N'Existing table hinh_anh_san_pham has no id; manual recovery required.', 1;

    IF COL_LENGTH(N'dbo.hinh_anh_san_pham', N'id_san_pham') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[hinh_anh_san_pham] ADD [id_san_pham] BIGINT NULL;';

    IF COL_LENGTH(N'dbo.hinh_anh_san_pham', N'url_anh') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[hinh_anh_san_pham] ADD [url_anh] NVARCHAR(1000) NULL;';

    IF COL_LENGTH(N'dbo.hinh_anh_san_pham', N'is_anh_chinh') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[hinh_anh_san_pham] ADD [is_anh_chinh] BIT NULL;';

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.hinh_anh_san_pham') AND c.name=N'id' AND (TYPE_NAME(c.user_type_id) <> N'bigint' OR c.is_identity <> 1))
        THROW 51002, N'Incompatible type: hinh_anh_san_pham.id. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.hinh_anh_san_pham') AND c.name=N'id_san_pham' AND (TYPE_NAME(c.user_type_id) <> N'bigint'))
        THROW 51002, N'Incompatible type: hinh_anh_san_pham.id_san_pham. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.hinh_anh_san_pham') AND c.name=N'url_anh' AND (TYPE_NAME(c.user_type_id) <> N'nvarchar' OR (c.max_length <> -1 AND c.max_length < 2000)))
        THROW 51002, N'Incompatible type: hinh_anh_san_pham.url_anh. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.hinh_anh_san_pham') AND c.name=N'is_anh_chinh' AND (TYPE_NAME(c.user_type_id) <> N'bit'))
        THROW 51002, N'Incompatible type: hinh_anh_san_pham.is_anh_chinh. No automatic data conversion.', 1;

    IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE parent_object_id=OBJECT_ID(N'dbo.hinh_anh_san_pham') AND type='PK')
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[hinh_anh_san_pham] ADD CONSTRAINT [PK_hinh_anh_san_pham] PRIMARY KEY ([id]);';

    IF OBJECT_ID(N'dbo.phieu_giam_gia', N'U') IS NULL
        EXEC sys.sp_executesql N'CREATE TABLE dbo.[phieu_giam_gia] (
        [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [PK_phieu_giam_gia] PRIMARY KEY,
        [ma_phieu_giam_gia] VARCHAR(50),
        [ten_phieu_giam_gia] NVARCHAR(255),
        [hinh_thuc_phieu] INT,
        [loai_giam_gia] INT,
        [gia_tri_giam] DECIMAL(18,2),
        [gia_tri_toi_thieu] DECIMAL(18,2),
        [giam_toi_da] DECIMAL(18,2),
        [ngay_bat_dau] DATETIME2,
        [ngay_ket_thuc] DATETIME2,
        [so_luong] INT,
        [so_luong_da_dung] INT,
        [trang_thai] INT,
        [ngay_tao] DATETIME2,
        [ngay_cap_nhat] DATETIME2,
        [mo_ta] NVARCHAR(1000)
    );';

    IF COL_LENGTH(N'dbo.phieu_giam_gia', N'id') IS NULL
        THROW 51001, N'Existing table phieu_giam_gia has no id; manual recovery required.', 1;

    IF COL_LENGTH(N'dbo.phieu_giam_gia', N'ma_phieu_giam_gia') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[phieu_giam_gia] ADD [ma_phieu_giam_gia] VARCHAR(50) NULL;';

    IF COL_LENGTH(N'dbo.phieu_giam_gia', N'ten_phieu_giam_gia') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[phieu_giam_gia] ADD [ten_phieu_giam_gia] NVARCHAR(255) NULL;';

    IF COL_LENGTH(N'dbo.phieu_giam_gia', N'hinh_thuc_phieu') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[phieu_giam_gia] ADD [hinh_thuc_phieu] INT NULL;';

    IF COL_LENGTH(N'dbo.phieu_giam_gia', N'loai_giam_gia') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[phieu_giam_gia] ADD [loai_giam_gia] INT NULL;';

    IF COL_LENGTH(N'dbo.phieu_giam_gia', N'gia_tri_giam') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[phieu_giam_gia] ADD [gia_tri_giam] DECIMAL(18,2) NULL;';

    IF COL_LENGTH(N'dbo.phieu_giam_gia', N'gia_tri_toi_thieu') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[phieu_giam_gia] ADD [gia_tri_toi_thieu] DECIMAL(18,2) NULL;';

    IF COL_LENGTH(N'dbo.phieu_giam_gia', N'giam_toi_da') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[phieu_giam_gia] ADD [giam_toi_da] DECIMAL(18,2) NULL;';

    IF COL_LENGTH(N'dbo.phieu_giam_gia', N'ngay_bat_dau') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[phieu_giam_gia] ADD [ngay_bat_dau] DATETIME2 NULL;';

    IF COL_LENGTH(N'dbo.phieu_giam_gia', N'ngay_ket_thuc') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[phieu_giam_gia] ADD [ngay_ket_thuc] DATETIME2 NULL;';

    IF COL_LENGTH(N'dbo.phieu_giam_gia', N'so_luong') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[phieu_giam_gia] ADD [so_luong] INT NULL;';

    IF COL_LENGTH(N'dbo.phieu_giam_gia', N'so_luong_da_dung') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[phieu_giam_gia] ADD [so_luong_da_dung] INT NULL;';

    IF COL_LENGTH(N'dbo.phieu_giam_gia', N'trang_thai') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[phieu_giam_gia] ADD [trang_thai] INT NULL;';

    IF COL_LENGTH(N'dbo.phieu_giam_gia', N'ngay_tao') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[phieu_giam_gia] ADD [ngay_tao] DATETIME2 NULL;';

    IF COL_LENGTH(N'dbo.phieu_giam_gia', N'ngay_cap_nhat') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[phieu_giam_gia] ADD [ngay_cap_nhat] DATETIME2 NULL;';

    IF COL_LENGTH(N'dbo.phieu_giam_gia', N'mo_ta') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[phieu_giam_gia] ADD [mo_ta] NVARCHAR(1000) NULL;';

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.phieu_giam_gia') AND c.name=N'id' AND (TYPE_NAME(c.user_type_id) <> N'bigint' OR c.is_identity <> 1))
        THROW 51002, N'Incompatible type: phieu_giam_gia.id. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.phieu_giam_gia') AND c.name=N'ma_phieu_giam_gia' AND (TYPE_NAME(c.user_type_id) <> N'varchar' OR (c.max_length <> -1 AND c.max_length < 50)))
        THROW 51002, N'Incompatible type: phieu_giam_gia.ma_phieu_giam_gia. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.phieu_giam_gia') AND c.name=N'ten_phieu_giam_gia' AND (TYPE_NAME(c.user_type_id) <> N'nvarchar' OR (c.max_length <> -1 AND c.max_length < 510)))
        THROW 51002, N'Incompatible type: phieu_giam_gia.ten_phieu_giam_gia. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.phieu_giam_gia') AND c.name=N'hinh_thuc_phieu' AND (TYPE_NAME(c.user_type_id) <> N'int'))
        THROW 51002, N'Incompatible type: phieu_giam_gia.hinh_thuc_phieu. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.phieu_giam_gia') AND c.name=N'loai_giam_gia' AND (TYPE_NAME(c.user_type_id) <> N'int'))
        THROW 51002, N'Incompatible type: phieu_giam_gia.loai_giam_gia. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.phieu_giam_gia') AND c.name=N'gia_tri_giam' AND ((TYPE_NAME(c.user_type_id) NOT IN (N'decimal',N'numeric') OR c.precision < 18 OR c.scale <> 2)))
        THROW 51002, N'Incompatible type: phieu_giam_gia.gia_tri_giam. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.phieu_giam_gia') AND c.name=N'gia_tri_toi_thieu' AND ((TYPE_NAME(c.user_type_id) NOT IN (N'decimal',N'numeric') OR c.precision < 18 OR c.scale <> 2)))
        THROW 51002, N'Incompatible type: phieu_giam_gia.gia_tri_toi_thieu. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.phieu_giam_gia') AND c.name=N'giam_toi_da' AND ((TYPE_NAME(c.user_type_id) NOT IN (N'decimal',N'numeric') OR c.precision < 18 OR c.scale <> 2)))
        THROW 51002, N'Incompatible type: phieu_giam_gia.giam_toi_da. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.phieu_giam_gia') AND c.name=N'ngay_bat_dau' AND (TYPE_NAME(c.user_type_id) <> N'datetime2'))
        THROW 51002, N'Incompatible type: phieu_giam_gia.ngay_bat_dau. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.phieu_giam_gia') AND c.name=N'ngay_ket_thuc' AND (TYPE_NAME(c.user_type_id) <> N'datetime2'))
        THROW 51002, N'Incompatible type: phieu_giam_gia.ngay_ket_thuc. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.phieu_giam_gia') AND c.name=N'so_luong' AND (TYPE_NAME(c.user_type_id) <> N'int'))
        THROW 51002, N'Incompatible type: phieu_giam_gia.so_luong. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.phieu_giam_gia') AND c.name=N'so_luong_da_dung' AND (TYPE_NAME(c.user_type_id) <> N'int'))
        THROW 51002, N'Incompatible type: phieu_giam_gia.so_luong_da_dung. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.phieu_giam_gia') AND c.name=N'trang_thai' AND (TYPE_NAME(c.user_type_id) <> N'int'))
        THROW 51002, N'Incompatible type: phieu_giam_gia.trang_thai. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.phieu_giam_gia') AND c.name=N'ngay_tao' AND (TYPE_NAME(c.user_type_id) <> N'datetime2'))
        THROW 51002, N'Incompatible type: phieu_giam_gia.ngay_tao. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.phieu_giam_gia') AND c.name=N'ngay_cap_nhat' AND (TYPE_NAME(c.user_type_id) <> N'datetime2'))
        THROW 51002, N'Incompatible type: phieu_giam_gia.ngay_cap_nhat. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.phieu_giam_gia') AND c.name=N'mo_ta' AND (TYPE_NAME(c.user_type_id) <> N'nvarchar' OR (c.max_length <> -1 AND c.max_length < 2000)))
        THROW 51002, N'Incompatible type: phieu_giam_gia.mo_ta. No automatic data conversion.', 1;

    IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE parent_object_id=OBJECT_ID(N'dbo.phieu_giam_gia') AND type='PK')
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[phieu_giam_gia] ADD CONSTRAINT [PK_phieu_giam_gia] PRIMARY KEY ([id]);';

    IF OBJECT_ID(N'dbo.phieu_giam_gia_khach_hang', N'U') IS NULL
        EXEC sys.sp_executesql N'CREATE TABLE dbo.[phieu_giam_gia_khach_hang] (
        [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [PK_phieu_giam_gia_khach_hang] PRIMARY KEY,
        [id_khach_hang] BIGINT NOT NULL,
        [id_phieu_giam_gia] BIGINT NOT NULL,
        [ngay_su_dung] DATETIME2,
        [trang_thai] INT
    );';

    IF COL_LENGTH(N'dbo.phieu_giam_gia_khach_hang', N'id') IS NULL
        THROW 51001, N'Existing table phieu_giam_gia_khach_hang has no id; manual recovery required.', 1;

    IF COL_LENGTH(N'dbo.phieu_giam_gia_khach_hang', N'id_khach_hang') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[phieu_giam_gia_khach_hang] ADD [id_khach_hang] BIGINT NULL;';

    IF COL_LENGTH(N'dbo.phieu_giam_gia_khach_hang', N'id_phieu_giam_gia') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[phieu_giam_gia_khach_hang] ADD [id_phieu_giam_gia] BIGINT NULL;';

    IF COL_LENGTH(N'dbo.phieu_giam_gia_khach_hang', N'ngay_su_dung') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[phieu_giam_gia_khach_hang] ADD [ngay_su_dung] DATETIME2 NULL;';

    IF COL_LENGTH(N'dbo.phieu_giam_gia_khach_hang', N'trang_thai') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[phieu_giam_gia_khach_hang] ADD [trang_thai] INT NULL;';

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.phieu_giam_gia_khach_hang') AND c.name=N'id' AND (TYPE_NAME(c.user_type_id) <> N'bigint' OR c.is_identity <> 1))
        THROW 51002, N'Incompatible type: phieu_giam_gia_khach_hang.id. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.phieu_giam_gia_khach_hang') AND c.name=N'id_khach_hang' AND (TYPE_NAME(c.user_type_id) <> N'bigint'))
        THROW 51002, N'Incompatible type: phieu_giam_gia_khach_hang.id_khach_hang. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.phieu_giam_gia_khach_hang') AND c.name=N'id_phieu_giam_gia' AND (TYPE_NAME(c.user_type_id) <> N'bigint'))
        THROW 51002, N'Incompatible type: phieu_giam_gia_khach_hang.id_phieu_giam_gia. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.phieu_giam_gia_khach_hang') AND c.name=N'ngay_su_dung' AND (TYPE_NAME(c.user_type_id) <> N'datetime2'))
        THROW 51002, N'Incompatible type: phieu_giam_gia_khach_hang.ngay_su_dung. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.phieu_giam_gia_khach_hang') AND c.name=N'trang_thai' AND (TYPE_NAME(c.user_type_id) <> N'int'))
        THROW 51002, N'Incompatible type: phieu_giam_gia_khach_hang.trang_thai. No automatic data conversion.', 1;

    IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE parent_object_id=OBJECT_ID(N'dbo.phieu_giam_gia_khach_hang') AND type='PK')
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[phieu_giam_gia_khach_hang] ADD CONSTRAINT [PK_phieu_giam_gia_khach_hang] PRIMARY KEY ([id]);';

    IF OBJECT_ID(N'dbo.dot_giam_gia', N'U') IS NULL
        EXEC sys.sp_executesql N'CREATE TABLE dbo.[dot_giam_gia] (
        [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [PK_dot_giam_gia] PRIMARY KEY,
        [ma_dot_giam_gia] VARCHAR(50),
        [ten_dot_giam_gia] NVARCHAR(255),
        [phan_tram_giam_dot] DECIMAL(18,2),
        [ngay_bat_dau] DATETIME2,
        [ngay_ket_thuc] DATETIME2,
        [kich_hoat] BIT,
        [ngay_tao] DATETIME2,
        [ngay_cap_nhat] DATETIME2,
        [trang_thai] INT,
        [mo_ta] NVARCHAR(1000)
    );';

    IF COL_LENGTH(N'dbo.dot_giam_gia', N'id') IS NULL
        THROW 51001, N'Existing table dot_giam_gia has no id; manual recovery required.', 1;

    IF COL_LENGTH(N'dbo.dot_giam_gia', N'ma_dot_giam_gia') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[dot_giam_gia] ADD [ma_dot_giam_gia] VARCHAR(50) NULL;';

    IF COL_LENGTH(N'dbo.dot_giam_gia', N'ten_dot_giam_gia') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[dot_giam_gia] ADD [ten_dot_giam_gia] NVARCHAR(255) NULL;';

    IF COL_LENGTH(N'dbo.dot_giam_gia', N'phan_tram_giam_dot') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[dot_giam_gia] ADD [phan_tram_giam_dot] DECIMAL(18,2) NULL;';

    IF COL_LENGTH(N'dbo.dot_giam_gia', N'ngay_bat_dau') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[dot_giam_gia] ADD [ngay_bat_dau] DATETIME2 NULL;';

    IF COL_LENGTH(N'dbo.dot_giam_gia', N'ngay_ket_thuc') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[dot_giam_gia] ADD [ngay_ket_thuc] DATETIME2 NULL;';

    IF COL_LENGTH(N'dbo.dot_giam_gia', N'kich_hoat') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[dot_giam_gia] ADD [kich_hoat] BIT NULL;';

    IF COL_LENGTH(N'dbo.dot_giam_gia', N'ngay_tao') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[dot_giam_gia] ADD [ngay_tao] DATETIME2 NULL;';

    IF COL_LENGTH(N'dbo.dot_giam_gia', N'ngay_cap_nhat') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[dot_giam_gia] ADD [ngay_cap_nhat] DATETIME2 NULL;';

    IF COL_LENGTH(N'dbo.dot_giam_gia', N'trang_thai') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[dot_giam_gia] ADD [trang_thai] INT NULL;';

    IF COL_LENGTH(N'dbo.dot_giam_gia', N'mo_ta') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[dot_giam_gia] ADD [mo_ta] NVARCHAR(1000) NULL;';

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.dot_giam_gia') AND c.name=N'id' AND (TYPE_NAME(c.user_type_id) <> N'bigint' OR c.is_identity <> 1))
        THROW 51002, N'Incompatible type: dot_giam_gia.id. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.dot_giam_gia') AND c.name=N'ma_dot_giam_gia' AND (TYPE_NAME(c.user_type_id) <> N'varchar' OR (c.max_length <> -1 AND c.max_length < 50)))
        THROW 51002, N'Incompatible type: dot_giam_gia.ma_dot_giam_gia. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.dot_giam_gia') AND c.name=N'ten_dot_giam_gia' AND (TYPE_NAME(c.user_type_id) <> N'nvarchar' OR (c.max_length <> -1 AND c.max_length < 510)))
        THROW 51002, N'Incompatible type: dot_giam_gia.ten_dot_giam_gia. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.dot_giam_gia') AND c.name=N'phan_tram_giam_dot' AND ((TYPE_NAME(c.user_type_id) NOT IN (N'decimal',N'numeric') OR c.precision < 18 OR c.scale <> 2)))
        THROW 51002, N'Incompatible type: dot_giam_gia.phan_tram_giam_dot. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.dot_giam_gia') AND c.name=N'ngay_bat_dau' AND (TYPE_NAME(c.user_type_id) <> N'datetime2'))
        THROW 51002, N'Incompatible type: dot_giam_gia.ngay_bat_dau. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.dot_giam_gia') AND c.name=N'ngay_ket_thuc' AND (TYPE_NAME(c.user_type_id) <> N'datetime2'))
        THROW 51002, N'Incompatible type: dot_giam_gia.ngay_ket_thuc. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.dot_giam_gia') AND c.name=N'kich_hoat' AND (TYPE_NAME(c.user_type_id) <> N'bit'))
        THROW 51002, N'Incompatible type: dot_giam_gia.kich_hoat. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.dot_giam_gia') AND c.name=N'ngay_tao' AND (TYPE_NAME(c.user_type_id) <> N'datetime2'))
        THROW 51002, N'Incompatible type: dot_giam_gia.ngay_tao. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.dot_giam_gia') AND c.name=N'ngay_cap_nhat' AND (TYPE_NAME(c.user_type_id) <> N'datetime2'))
        THROW 51002, N'Incompatible type: dot_giam_gia.ngay_cap_nhat. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.dot_giam_gia') AND c.name=N'trang_thai' AND (TYPE_NAME(c.user_type_id) <> N'int'))
        THROW 51002, N'Incompatible type: dot_giam_gia.trang_thai. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.dot_giam_gia') AND c.name=N'mo_ta' AND (TYPE_NAME(c.user_type_id) <> N'nvarchar' OR (c.max_length <> -1 AND c.max_length < 2000)))
        THROW 51002, N'Incompatible type: dot_giam_gia.mo_ta. No automatic data conversion.', 1;

    IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE parent_object_id=OBJECT_ID(N'dbo.dot_giam_gia') AND type='PK')
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[dot_giam_gia] ADD CONSTRAINT [PK_dot_giam_gia] PRIMARY KEY ([id]);';

    IF OBJECT_ID(N'dbo.chi_tiet_dot_giam_gia', N'U') IS NULL
        EXEC sys.sp_executesql N'CREATE TABLE dbo.[chi_tiet_dot_giam_gia] (
        [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [PK_chi_tiet_dot_giam_gia] PRIMARY KEY,
        [id_dot_giam_gia] BIGINT NOT NULL,
        [id_san_pham_chi_tiet] BIGINT NOT NULL,
        [phan_tram_giam_bien_the] DECIMAL(18,2),
        [trang_thai] INT,
        [ngay_tao] DATETIME2
    );';

    IF COL_LENGTH(N'dbo.chi_tiet_dot_giam_gia', N'id') IS NULL
        THROW 51001, N'Existing table chi_tiet_dot_giam_gia has no id; manual recovery required.', 1;

    IF COL_LENGTH(N'dbo.chi_tiet_dot_giam_gia', N'id_dot_giam_gia') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[chi_tiet_dot_giam_gia] ADD [id_dot_giam_gia] BIGINT NULL;';

    IF COL_LENGTH(N'dbo.chi_tiet_dot_giam_gia', N'id_san_pham_chi_tiet') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[chi_tiet_dot_giam_gia] ADD [id_san_pham_chi_tiet] BIGINT NULL;';

    IF COL_LENGTH(N'dbo.chi_tiet_dot_giam_gia', N'phan_tram_giam_bien_the') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[chi_tiet_dot_giam_gia] ADD [phan_tram_giam_bien_the] DECIMAL(18,2) NULL;';

    IF COL_LENGTH(N'dbo.chi_tiet_dot_giam_gia', N'trang_thai') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[chi_tiet_dot_giam_gia] ADD [trang_thai] INT NULL;';

    IF COL_LENGTH(N'dbo.chi_tiet_dot_giam_gia', N'ngay_tao') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[chi_tiet_dot_giam_gia] ADD [ngay_tao] DATETIME2 NULL;';

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.chi_tiet_dot_giam_gia') AND c.name=N'id' AND (TYPE_NAME(c.user_type_id) <> N'bigint' OR c.is_identity <> 1))
        THROW 51002, N'Incompatible type: chi_tiet_dot_giam_gia.id. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.chi_tiet_dot_giam_gia') AND c.name=N'id_dot_giam_gia' AND (TYPE_NAME(c.user_type_id) <> N'bigint'))
        THROW 51002, N'Incompatible type: chi_tiet_dot_giam_gia.id_dot_giam_gia. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.chi_tiet_dot_giam_gia') AND c.name=N'id_san_pham_chi_tiet' AND (TYPE_NAME(c.user_type_id) <> N'bigint'))
        THROW 51002, N'Incompatible type: chi_tiet_dot_giam_gia.id_san_pham_chi_tiet. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.chi_tiet_dot_giam_gia') AND c.name=N'phan_tram_giam_bien_the' AND ((TYPE_NAME(c.user_type_id) NOT IN (N'decimal',N'numeric') OR c.precision < 18 OR c.scale <> 2)))
        THROW 51002, N'Incompatible type: chi_tiet_dot_giam_gia.phan_tram_giam_bien_the. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.chi_tiet_dot_giam_gia') AND c.name=N'trang_thai' AND (TYPE_NAME(c.user_type_id) <> N'int'))
        THROW 51002, N'Incompatible type: chi_tiet_dot_giam_gia.trang_thai. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.chi_tiet_dot_giam_gia') AND c.name=N'ngay_tao' AND (TYPE_NAME(c.user_type_id) <> N'datetime2'))
        THROW 51002, N'Incompatible type: chi_tiet_dot_giam_gia.ngay_tao. No automatic data conversion.', 1;

    IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE parent_object_id=OBJECT_ID(N'dbo.chi_tiet_dot_giam_gia') AND type='PK')
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[chi_tiet_dot_giam_gia] ADD CONSTRAINT [PK_chi_tiet_dot_giam_gia] PRIMARY KEY ([id]);';

    IF OBJECT_ID(N'dbo.hinh_thuc_thanh_toan', N'U') IS NULL
        EXEC sys.sp_executesql N'CREATE TABLE dbo.[hinh_thuc_thanh_toan] (
        [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [PK_hinh_thuc_thanh_toan] PRIMARY KEY,
        [ma_hinh_thuc] VARCHAR(50),
        [ten_hinh_thuc] NVARCHAR(255),
        [trang_thai] INT
    );';

    IF COL_LENGTH(N'dbo.hinh_thuc_thanh_toan', N'id') IS NULL
        THROW 51001, N'Existing table hinh_thuc_thanh_toan has no id; manual recovery required.', 1;

    IF COL_LENGTH(N'dbo.hinh_thuc_thanh_toan', N'ma_hinh_thuc') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[hinh_thuc_thanh_toan] ADD [ma_hinh_thuc] VARCHAR(50) NULL;';

    IF COL_LENGTH(N'dbo.hinh_thuc_thanh_toan', N'ten_hinh_thuc') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[hinh_thuc_thanh_toan] ADD [ten_hinh_thuc] NVARCHAR(255) NULL;';

    IF COL_LENGTH(N'dbo.hinh_thuc_thanh_toan', N'trang_thai') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[hinh_thuc_thanh_toan] ADD [trang_thai] INT NULL;';

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.hinh_thuc_thanh_toan') AND c.name=N'id' AND (TYPE_NAME(c.user_type_id) <> N'bigint' OR c.is_identity <> 1))
        THROW 51002, N'Incompatible type: hinh_thuc_thanh_toan.id. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.hinh_thuc_thanh_toan') AND c.name=N'ma_hinh_thuc' AND (TYPE_NAME(c.user_type_id) <> N'varchar' OR (c.max_length <> -1 AND c.max_length < 50)))
        THROW 51002, N'Incompatible type: hinh_thuc_thanh_toan.ma_hinh_thuc. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.hinh_thuc_thanh_toan') AND c.name=N'ten_hinh_thuc' AND (TYPE_NAME(c.user_type_id) <> N'nvarchar' OR (c.max_length <> -1 AND c.max_length < 510)))
        THROW 51002, N'Incompatible type: hinh_thuc_thanh_toan.ten_hinh_thuc. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.hinh_thuc_thanh_toan') AND c.name=N'trang_thai' AND (TYPE_NAME(c.user_type_id) <> N'int'))
        THROW 51002, N'Incompatible type: hinh_thuc_thanh_toan.trang_thai. No automatic data conversion.', 1;

    IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE parent_object_id=OBJECT_ID(N'dbo.hinh_thuc_thanh_toan') AND type='PK')
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[hinh_thuc_thanh_toan] ADD CONSTRAINT [PK_hinh_thuc_thanh_toan] PRIMARY KEY ([id]);';

    IF OBJECT_ID(N'dbo.phuong_thuc_thanh_toan', N'U') IS NULL
        EXEC sys.sp_executesql N'CREATE TABLE dbo.[phuong_thuc_thanh_toan] (
        [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [PK_phuong_thuc_thanh_toan] PRIMARY KEY,
        [id_hinh_thuc_thanh_toan] BIGINT NOT NULL,
        [ma_phuong_thuc] VARCHAR(50),
        [ten_phuong_thuc] NVARCHAR(255),
        [trang_thai] INT
    );';

    IF COL_LENGTH(N'dbo.phuong_thuc_thanh_toan', N'id') IS NULL
        THROW 51001, N'Existing table phuong_thuc_thanh_toan has no id; manual recovery required.', 1;

    IF COL_LENGTH(N'dbo.phuong_thuc_thanh_toan', N'id_hinh_thuc_thanh_toan') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[phuong_thuc_thanh_toan] ADD [id_hinh_thuc_thanh_toan] BIGINT NULL;';

    IF COL_LENGTH(N'dbo.phuong_thuc_thanh_toan', N'ma_phuong_thuc') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[phuong_thuc_thanh_toan] ADD [ma_phuong_thuc] VARCHAR(50) NULL;';

    IF COL_LENGTH(N'dbo.phuong_thuc_thanh_toan', N'ten_phuong_thuc') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[phuong_thuc_thanh_toan] ADD [ten_phuong_thuc] NVARCHAR(255) NULL;';

    IF COL_LENGTH(N'dbo.phuong_thuc_thanh_toan', N'trang_thai') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[phuong_thuc_thanh_toan] ADD [trang_thai] INT NULL;';

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.phuong_thuc_thanh_toan') AND c.name=N'id' AND (TYPE_NAME(c.user_type_id) <> N'bigint' OR c.is_identity <> 1))
        THROW 51002, N'Incompatible type: phuong_thuc_thanh_toan.id. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.phuong_thuc_thanh_toan') AND c.name=N'id_hinh_thuc_thanh_toan' AND (TYPE_NAME(c.user_type_id) <> N'bigint'))
        THROW 51002, N'Incompatible type: phuong_thuc_thanh_toan.id_hinh_thuc_thanh_toan. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.phuong_thuc_thanh_toan') AND c.name=N'ma_phuong_thuc' AND (TYPE_NAME(c.user_type_id) <> N'varchar' OR (c.max_length <> -1 AND c.max_length < 50)))
        THROW 51002, N'Incompatible type: phuong_thuc_thanh_toan.ma_phuong_thuc. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.phuong_thuc_thanh_toan') AND c.name=N'ten_phuong_thuc' AND (TYPE_NAME(c.user_type_id) <> N'nvarchar' OR (c.max_length <> -1 AND c.max_length < 510)))
        THROW 51002, N'Incompatible type: phuong_thuc_thanh_toan.ten_phuong_thuc. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.phuong_thuc_thanh_toan') AND c.name=N'trang_thai' AND (TYPE_NAME(c.user_type_id) <> N'int'))
        THROW 51002, N'Incompatible type: phuong_thuc_thanh_toan.trang_thai. No automatic data conversion.', 1;

    IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE parent_object_id=OBJECT_ID(N'dbo.phuong_thuc_thanh_toan') AND type='PK')
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[phuong_thuc_thanh_toan] ADD CONSTRAINT [PK_phuong_thuc_thanh_toan] PRIMARY KEY ([id]);';

    IF OBJECT_ID(N'dbo.hoa_don', N'U') IS NULL
        EXEC sys.sp_executesql N'CREATE TABLE dbo.[hoa_don] (
        [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [PK_hoa_don] PRIMARY KEY,
        [id_khach_hang] BIGINT,
        [id_nhan_vien] BIGINT,
        [id_phieu_giam_gia] BIGINT,
        [id_phuong_thuc_thanh_toan] BIGINT,
        [ma_hoa_don] VARCHAR(50),
        [loai_hoa_don] INT,
        [tong_tien] DECIMAL(18,2),
        [phi_van_chuyen] DECIMAL(18,2),
        [tien_giam_gia] DECIMAL(18,2),
        [thanh_tien] DECIMAL(18,2),
        [don_vi_van_chuyen] NVARCHAR(255),
        [ho_ten_nguoi_nhan] NVARCHAR(255),
        [so_dien_thoai_nguoi_nhan] VARCHAR(20),
        [dia_chi_giao_hang] NVARCHAR(500),
        [ghi_chu] NVARCHAR(1000),
        [ngay_thanh_toan] DATETIME2,
        [ngay_tao] DATETIME2,
        [ngay_cap_nhat] DATETIME2,
        [trang_thai] INT
    );';

    IF COL_LENGTH(N'dbo.hoa_don', N'id') IS NULL
        THROW 51001, N'Existing table hoa_don has no id; manual recovery required.', 1;

    IF COL_LENGTH(N'dbo.hoa_don', N'id_khach_hang') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[hoa_don] ADD [id_khach_hang] BIGINT NULL;';

    IF COL_LENGTH(N'dbo.hoa_don', N'id_nhan_vien') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[hoa_don] ADD [id_nhan_vien] BIGINT NULL;';

    IF COL_LENGTH(N'dbo.hoa_don', N'id_phieu_giam_gia') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[hoa_don] ADD [id_phieu_giam_gia] BIGINT NULL;';

    IF COL_LENGTH(N'dbo.hoa_don', N'id_phuong_thuc_thanh_toan') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[hoa_don] ADD [id_phuong_thuc_thanh_toan] BIGINT NULL;';

    IF COL_LENGTH(N'dbo.hoa_don', N'ma_hoa_don') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[hoa_don] ADD [ma_hoa_don] VARCHAR(50) NULL;';

    IF COL_LENGTH(N'dbo.hoa_don', N'loai_hoa_don') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[hoa_don] ADD [loai_hoa_don] INT NULL;';

    IF COL_LENGTH(N'dbo.hoa_don', N'tong_tien') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[hoa_don] ADD [tong_tien] DECIMAL(18,2) NULL;';

    IF COL_LENGTH(N'dbo.hoa_don', N'phi_van_chuyen') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[hoa_don] ADD [phi_van_chuyen] DECIMAL(18,2) NULL;';

    IF COL_LENGTH(N'dbo.hoa_don', N'tien_giam_gia') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[hoa_don] ADD [tien_giam_gia] DECIMAL(18,2) NULL;';

    IF COL_LENGTH(N'dbo.hoa_don', N'thanh_tien') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[hoa_don] ADD [thanh_tien] DECIMAL(18,2) NULL;';

    IF COL_LENGTH(N'dbo.hoa_don', N'don_vi_van_chuyen') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[hoa_don] ADD [don_vi_van_chuyen] NVARCHAR(255) NULL;';

    IF COL_LENGTH(N'dbo.hoa_don', N'ho_ten_nguoi_nhan') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[hoa_don] ADD [ho_ten_nguoi_nhan] NVARCHAR(255) NULL;';

    IF COL_LENGTH(N'dbo.hoa_don', N'so_dien_thoai_nguoi_nhan') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[hoa_don] ADD [so_dien_thoai_nguoi_nhan] VARCHAR(20) NULL;';

    IF COL_LENGTH(N'dbo.hoa_don', N'dia_chi_giao_hang') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[hoa_don] ADD [dia_chi_giao_hang] NVARCHAR(500) NULL;';

    IF COL_LENGTH(N'dbo.hoa_don', N'ghi_chu') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[hoa_don] ADD [ghi_chu] NVARCHAR(1000) NULL;';

    IF COL_LENGTH(N'dbo.hoa_don', N'ngay_thanh_toan') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[hoa_don] ADD [ngay_thanh_toan] DATETIME2 NULL;';

    IF COL_LENGTH(N'dbo.hoa_don', N'ngay_tao') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[hoa_don] ADD [ngay_tao] DATETIME2 NULL;';

    IF COL_LENGTH(N'dbo.hoa_don', N'ngay_cap_nhat') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[hoa_don] ADD [ngay_cap_nhat] DATETIME2 NULL;';

    IF COL_LENGTH(N'dbo.hoa_don', N'trang_thai') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[hoa_don] ADD [trang_thai] INT NULL;';
    IF COL_LENGTH(N'dbo.hoa_don', N'hinh_thuc_nhan') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[hoa_don] ADD [hinh_thuc_nhan] TINYINT NULL;';

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.hoa_don') AND c.name=N'id' AND (TYPE_NAME(c.user_type_id) <> N'bigint' OR c.is_identity <> 1))
        THROW 51002, N'Incompatible type: hoa_don.id. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.hoa_don') AND c.name=N'id_khach_hang' AND (TYPE_NAME(c.user_type_id) <> N'bigint'))
        THROW 51002, N'Incompatible type: hoa_don.id_khach_hang. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.hoa_don') AND c.name=N'id_nhan_vien' AND (TYPE_NAME(c.user_type_id) <> N'bigint'))
        THROW 51002, N'Incompatible type: hoa_don.id_nhan_vien. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.hoa_don') AND c.name=N'id_phieu_giam_gia' AND (TYPE_NAME(c.user_type_id) <> N'bigint'))
        THROW 51002, N'Incompatible type: hoa_don.id_phieu_giam_gia. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.hoa_don') AND c.name=N'id_phuong_thuc_thanh_toan' AND (TYPE_NAME(c.user_type_id) <> N'bigint'))
        THROW 51002, N'Incompatible type: hoa_don.id_phuong_thuc_thanh_toan. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.hoa_don') AND c.name=N'ma_hoa_don' AND (TYPE_NAME(c.user_type_id) <> N'varchar' OR (c.max_length <> -1 AND c.max_length < 50)))
        THROW 51002, N'Incompatible type: hoa_don.ma_hoa_don. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.hoa_don') AND c.name=N'loai_hoa_don' AND (TYPE_NAME(c.user_type_id) <> N'int'))
        THROW 51002, N'Incompatible type: hoa_don.loai_hoa_don. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.hoa_don') AND c.name=N'tong_tien' AND ((TYPE_NAME(c.user_type_id) NOT IN (N'decimal',N'numeric') OR c.precision < 18 OR c.scale <> 2)))
        THROW 51002, N'Incompatible type: hoa_don.tong_tien. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.hoa_don') AND c.name=N'phi_van_chuyen' AND ((TYPE_NAME(c.user_type_id) NOT IN (N'decimal',N'numeric') OR c.precision < 18 OR c.scale <> 2)))
        THROW 51002, N'Incompatible type: hoa_don.phi_van_chuyen. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.hoa_don') AND c.name=N'tien_giam_gia' AND ((TYPE_NAME(c.user_type_id) NOT IN (N'decimal',N'numeric') OR c.precision < 18 OR c.scale <> 2)))
        THROW 51002, N'Incompatible type: hoa_don.tien_giam_gia. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.hoa_don') AND c.name=N'thanh_tien' AND ((TYPE_NAME(c.user_type_id) NOT IN (N'decimal',N'numeric') OR c.precision < 18 OR c.scale <> 2)))
        THROW 51002, N'Incompatible type: hoa_don.thanh_tien. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.hoa_don') AND c.name=N'don_vi_van_chuyen' AND (TYPE_NAME(c.user_type_id) <> N'nvarchar' OR (c.max_length <> -1 AND c.max_length < 510)))
        THROW 51002, N'Incompatible type: hoa_don.don_vi_van_chuyen. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.hoa_don') AND c.name=N'ho_ten_nguoi_nhan' AND (TYPE_NAME(c.user_type_id) <> N'nvarchar' OR (c.max_length <> -1 AND c.max_length < 510)))
        THROW 51002, N'Incompatible type: hoa_don.ho_ten_nguoi_nhan. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.hoa_don') AND c.name=N'so_dien_thoai_nguoi_nhan' AND (TYPE_NAME(c.user_type_id) <> N'varchar' OR (c.max_length <> -1 AND c.max_length < 20)))
        THROW 51002, N'Incompatible type: hoa_don.so_dien_thoai_nguoi_nhan. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.hoa_don') AND c.name=N'dia_chi_giao_hang' AND (TYPE_NAME(c.user_type_id) <> N'nvarchar' OR (c.max_length <> -1 AND c.max_length < 1000)))
        THROW 51002, N'Incompatible type: hoa_don.dia_chi_giao_hang. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.hoa_don') AND c.name=N'ghi_chu' AND (TYPE_NAME(c.user_type_id) <> N'nvarchar' OR (c.max_length <> -1 AND c.max_length < 2000)))
        THROW 51002, N'Incompatible type: hoa_don.ghi_chu. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.hoa_don') AND c.name=N'ngay_thanh_toan' AND (TYPE_NAME(c.user_type_id) <> N'datetime2'))
        THROW 51002, N'Incompatible type: hoa_don.ngay_thanh_toan. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.hoa_don') AND c.name=N'ngay_tao' AND (TYPE_NAME(c.user_type_id) <> N'datetime2'))
        THROW 51002, N'Incompatible type: hoa_don.ngay_tao. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.hoa_don') AND c.name=N'ngay_cap_nhat' AND (TYPE_NAME(c.user_type_id) <> N'datetime2'))
        THROW 51002, N'Incompatible type: hoa_don.ngay_cap_nhat. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.hoa_don') AND c.name=N'trang_thai' AND (TYPE_NAME(c.user_type_id) <> N'int'))
        THROW 51002, N'Incompatible type: hoa_don.trang_thai. No automatic data conversion.', 1;
    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.hoa_don') AND c.name=N'hinh_thuc_nhan' AND (TYPE_NAME(c.user_type_id) <> N'tinyint'))
        THROW 51002, N'Incompatible type: hoa_don.hinh_thuc_nhan. No automatic data conversion.', 1;

    IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE parent_object_id=OBJECT_ID(N'dbo.hoa_don') AND type='PK')
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[hoa_don] ADD CONSTRAINT [PK_hoa_don] PRIMARY KEY ([id]);';

    IF OBJECT_ID(N'dbo.hoa_don_chi_tiet', N'U') IS NULL
        EXEC sys.sp_executesql N'CREATE TABLE dbo.[hoa_don_chi_tiet] (
        [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [PK_hoa_don_chi_tiet] PRIMARY KEY,
        [id_hoa_don] BIGINT NOT NULL,
        [id_san_pham_chi_tiet] BIGINT NOT NULL,
        [so_luong] INT,
        [don_gia] DECIMAL(18,2),
        [thanh_tien] DECIMAL(18,2),
        [ghi_chu] NVARCHAR(1000),
        [trang_thai] INT
    );';

    IF COL_LENGTH(N'dbo.hoa_don_chi_tiet', N'id') IS NULL
        THROW 51001, N'Existing table hoa_don_chi_tiet has no id; manual recovery required.', 1;

    IF COL_LENGTH(N'dbo.hoa_don_chi_tiet', N'id_hoa_don') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[hoa_don_chi_tiet] ADD [id_hoa_don] BIGINT NULL;';

    IF COL_LENGTH(N'dbo.hoa_don_chi_tiet', N'id_san_pham_chi_tiet') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[hoa_don_chi_tiet] ADD [id_san_pham_chi_tiet] BIGINT NULL;';

    IF COL_LENGTH(N'dbo.hoa_don_chi_tiet', N'so_luong') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[hoa_don_chi_tiet] ADD [so_luong] INT NULL;';

    IF COL_LENGTH(N'dbo.hoa_don_chi_tiet', N'don_gia') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[hoa_don_chi_tiet] ADD [don_gia] DECIMAL(18,2) NULL;';

    IF COL_LENGTH(N'dbo.hoa_don_chi_tiet', N'thanh_tien') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[hoa_don_chi_tiet] ADD [thanh_tien] DECIMAL(18,2) NULL;';

    IF COL_LENGTH(N'dbo.hoa_don_chi_tiet', N'ghi_chu') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[hoa_don_chi_tiet] ADD [ghi_chu] NVARCHAR(1000) NULL;';

    IF COL_LENGTH(N'dbo.hoa_don_chi_tiet', N'trang_thai') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[hoa_don_chi_tiet] ADD [trang_thai] INT NULL;';

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.hoa_don_chi_tiet') AND c.name=N'id' AND (TYPE_NAME(c.user_type_id) <> N'bigint' OR c.is_identity <> 1))
        THROW 51002, N'Incompatible type: hoa_don_chi_tiet.id. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.hoa_don_chi_tiet') AND c.name=N'id_hoa_don' AND (TYPE_NAME(c.user_type_id) <> N'bigint'))
        THROW 51002, N'Incompatible type: hoa_don_chi_tiet.id_hoa_don. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.hoa_don_chi_tiet') AND c.name=N'id_san_pham_chi_tiet' AND (TYPE_NAME(c.user_type_id) <> N'bigint'))
        THROW 51002, N'Incompatible type: hoa_don_chi_tiet.id_san_pham_chi_tiet. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.hoa_don_chi_tiet') AND c.name=N'so_luong' AND (TYPE_NAME(c.user_type_id) <> N'int'))
        THROW 51002, N'Incompatible type: hoa_don_chi_tiet.so_luong. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.hoa_don_chi_tiet') AND c.name=N'don_gia' AND ((TYPE_NAME(c.user_type_id) NOT IN (N'decimal',N'numeric') OR c.precision < 18 OR c.scale <> 2)))
        THROW 51002, N'Incompatible type: hoa_don_chi_tiet.don_gia. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.hoa_don_chi_tiet') AND c.name=N'thanh_tien' AND ((TYPE_NAME(c.user_type_id) NOT IN (N'decimal',N'numeric') OR c.precision < 18 OR c.scale <> 2)))
        THROW 51002, N'Incompatible type: hoa_don_chi_tiet.thanh_tien. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.hoa_don_chi_tiet') AND c.name=N'ghi_chu' AND (TYPE_NAME(c.user_type_id) <> N'nvarchar' OR (c.max_length <> -1 AND c.max_length < 2000)))
        THROW 51002, N'Incompatible type: hoa_don_chi_tiet.ghi_chu. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.hoa_don_chi_tiet') AND c.name=N'trang_thai' AND (TYPE_NAME(c.user_type_id) <> N'int'))
        THROW 51002, N'Incompatible type: hoa_don_chi_tiet.trang_thai. No automatic data conversion.', 1;

    IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE parent_object_id=OBJECT_ID(N'dbo.hoa_don_chi_tiet') AND type='PK')
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[hoa_don_chi_tiet] ADD CONSTRAINT [PK_hoa_don_chi_tiet] PRIMARY KEY ([id]);';

    IF OBJECT_ID(N'dbo.lich_su_hoa_don', N'U') IS NULL
        EXEC sys.sp_executesql N'CREATE TABLE dbo.[lich_su_hoa_don] (
        [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [PK_lich_su_hoa_don] PRIMARY KEY,
        [id_hoa_don] BIGINT NOT NULL,
        [nguoi_tao] BIGINT,
        [trang_thai] INT,
        [ghi_chu] NVARCHAR(1000),
        [ngay_tao] DATETIME2
    );';

    IF COL_LENGTH(N'dbo.lich_su_hoa_don', N'id') IS NULL
        THROW 51001, N'Existing table lich_su_hoa_don has no id; manual recovery required.', 1;

    IF COL_LENGTH(N'dbo.lich_su_hoa_don', N'id_hoa_don') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[lich_su_hoa_don] ADD [id_hoa_don] BIGINT NULL;';

    IF COL_LENGTH(N'dbo.lich_su_hoa_don', N'nguoi_tao') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[lich_su_hoa_don] ADD [nguoi_tao] BIGINT NULL;';

    IF COL_LENGTH(N'dbo.lich_su_hoa_don', N'trang_thai') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[lich_su_hoa_don] ADD [trang_thai] INT NULL;';

    IF COL_LENGTH(N'dbo.lich_su_hoa_don', N'ghi_chu') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[lich_su_hoa_don] ADD [ghi_chu] NVARCHAR(1000) NULL;';

    IF COL_LENGTH(N'dbo.lich_su_hoa_don', N'ngay_tao') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[lich_su_hoa_don] ADD [ngay_tao] DATETIME2 NULL;';

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.lich_su_hoa_don') AND c.name=N'id' AND (TYPE_NAME(c.user_type_id) <> N'bigint' OR c.is_identity <> 1))
        THROW 51002, N'Incompatible type: lich_su_hoa_don.id. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.lich_su_hoa_don') AND c.name=N'id_hoa_don' AND (TYPE_NAME(c.user_type_id) <> N'bigint'))
        THROW 51002, N'Incompatible type: lich_su_hoa_don.id_hoa_don. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.lich_su_hoa_don') AND c.name=N'nguoi_tao' AND (TYPE_NAME(c.user_type_id) <> N'bigint'))
        THROW 51002, N'Incompatible type: lich_su_hoa_don.nguoi_tao. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.lich_su_hoa_don') AND c.name=N'trang_thai' AND (TYPE_NAME(c.user_type_id) <> N'int'))
        THROW 51002, N'Incompatible type: lich_su_hoa_don.trang_thai. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.lich_su_hoa_don') AND c.name=N'ghi_chu' AND (TYPE_NAME(c.user_type_id) <> N'nvarchar' OR (c.max_length <> -1 AND c.max_length < 2000)))
        THROW 51002, N'Incompatible type: lich_su_hoa_don.ghi_chu. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.lich_su_hoa_don') AND c.name=N'ngay_tao' AND (TYPE_NAME(c.user_type_id) <> N'datetime2'))
        THROW 51002, N'Incompatible type: lich_su_hoa_don.ngay_tao. No automatic data conversion.', 1;

    IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE parent_object_id=OBJECT_ID(N'dbo.lich_su_hoa_don') AND type='PK')
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[lich_su_hoa_don] ADD CONSTRAINT [PK_lich_su_hoa_don] PRIMARY KEY ([id]);';

    IF OBJECT_ID(N'dbo.lich_su_thanh_toan', N'U') IS NULL
        EXEC sys.sp_executesql N'CREATE TABLE dbo.[lich_su_thanh_toan] (
        [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [PK_lich_su_thanh_toan] PRIMARY KEY,
        [id_hoa_don] BIGINT NOT NULL,
        [id_phuong_thuc_thanh_toan] BIGINT,
        [so_tien] DECIMAL(18,2),
        [ma_giao_dich] VARCHAR(255),
        [thoi_gian] DATETIME2,
        [trang_thai] INT,
        [mo_ta] NVARCHAR(1000)
    );';

    IF COL_LENGTH(N'dbo.lich_su_thanh_toan', N'id') IS NULL
        THROW 51001, N'Existing table lich_su_thanh_toan has no id; manual recovery required.', 1;

    IF COL_LENGTH(N'dbo.lich_su_thanh_toan', N'id_hoa_don') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[lich_su_thanh_toan] ADD [id_hoa_don] BIGINT NULL;';

    IF COL_LENGTH(N'dbo.lich_su_thanh_toan', N'id_phuong_thuc_thanh_toan') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[lich_su_thanh_toan] ADD [id_phuong_thuc_thanh_toan] BIGINT NULL;';

    IF COL_LENGTH(N'dbo.lich_su_thanh_toan', N'so_tien') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[lich_su_thanh_toan] ADD [so_tien] DECIMAL(18,2) NULL;';

    IF COL_LENGTH(N'dbo.lich_su_thanh_toan', N'ma_giao_dich') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[lich_su_thanh_toan] ADD [ma_giao_dich] VARCHAR(255) NULL;';

    IF COL_LENGTH(N'dbo.lich_su_thanh_toan', N'thoi_gian') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[lich_su_thanh_toan] ADD [thoi_gian] DATETIME2 NULL;';

    IF COL_LENGTH(N'dbo.lich_su_thanh_toan', N'trang_thai') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[lich_su_thanh_toan] ADD [trang_thai] INT NULL;';

    IF COL_LENGTH(N'dbo.lich_su_thanh_toan', N'mo_ta') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[lich_su_thanh_toan] ADD [mo_ta] NVARCHAR(1000) NULL;';

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.lich_su_thanh_toan') AND c.name=N'id' AND (TYPE_NAME(c.user_type_id) <> N'bigint' OR c.is_identity <> 1))
        THROW 51002, N'Incompatible type: lich_su_thanh_toan.id. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.lich_su_thanh_toan') AND c.name=N'id_hoa_don' AND (TYPE_NAME(c.user_type_id) <> N'bigint'))
        THROW 51002, N'Incompatible type: lich_su_thanh_toan.id_hoa_don. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.lich_su_thanh_toan') AND c.name=N'id_phuong_thuc_thanh_toan' AND (TYPE_NAME(c.user_type_id) <> N'bigint'))
        THROW 51002, N'Incompatible type: lich_su_thanh_toan.id_phuong_thuc_thanh_toan. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.lich_su_thanh_toan') AND c.name=N'so_tien' AND ((TYPE_NAME(c.user_type_id) NOT IN (N'decimal',N'numeric') OR c.precision < 18 OR c.scale <> 2)))
        THROW 51002, N'Incompatible type: lich_su_thanh_toan.so_tien. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.lich_su_thanh_toan') AND c.name=N'ma_giao_dich' AND (TYPE_NAME(c.user_type_id) <> N'varchar' OR (c.max_length <> -1 AND c.max_length < 255)))
        THROW 51002, N'Incompatible type: lich_su_thanh_toan.ma_giao_dich. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.lich_su_thanh_toan') AND c.name=N'thoi_gian' AND (TYPE_NAME(c.user_type_id) <> N'datetime2'))
        THROW 51002, N'Incompatible type: lich_su_thanh_toan.thoi_gian. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.lich_su_thanh_toan') AND c.name=N'trang_thai' AND (TYPE_NAME(c.user_type_id) <> N'int'))
        THROW 51002, N'Incompatible type: lich_su_thanh_toan.trang_thai. No automatic data conversion.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns c WHERE c.object_id=OBJECT_ID(N'dbo.lich_su_thanh_toan') AND c.name=N'mo_ta' AND (TYPE_NAME(c.user_type_id) <> N'nvarchar' OR (c.max_length <> -1 AND c.max_length < 2000)))
        THROW 51002, N'Incompatible type: lich_su_thanh_toan.mo_ta. No automatic data conversion.', 1;

    IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE parent_object_id=OBJECT_ID(N'dbo.lich_su_thanh_toan') AND type='PK')
        EXEC sys.sp_executesql N'ALTER TABLE dbo.[lich_su_thanh_toan] ADD CONSTRAINT [PK_lich_su_thanh_toan] PRIMARY KEY ([id]);';

    -- Keep an existing unused NOT NULL legacy column insertable; never create it.
    IF COL_LENGTH(N'dbo.phieu_giam_gia', N'vo_han') IS NOT NULL
       AND EXISTS (SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID(N'dbo.phieu_giam_gia') AND name=N'vo_han' AND is_nullable=0)
       AND NOT EXISTS (SELECT 1 FROM sys.default_constraints d JOIN sys.columns c ON c.object_id=d.parent_object_id AND c.column_id=d.parent_column_id WHERE d.parent_object_id=OBJECT_ID(N'dbo.phieu_giam_gia') AND c.name=N'vo_han')
        EXEC sys.sp_executesql N'ALTER TABLE dbo.phieu_giam_gia ADD CONSTRAINT DF_phieu_giam_gia_legacy_vo_han DEFAULT (0) FOR vo_han;';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
IF XACT_STATE() <> 1 THROW 51003, N'Schema phase failed. Stop and fix the reported error.', 1;
BEGIN TRY
    -- 2. All parents/children now exist. Detect FKs by relationship, not name.

    IF NOT EXISTS (
        SELECT 1 FROM sys.foreign_key_columns f
        WHERE f.parent_object_id=OBJECT_ID(N'dbo.nhan_vien') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.nhan_vien'),N'id_vai_tro','ColumnId')
          AND f.referenced_object_id=OBJECT_ID(N'dbo.vai_tro') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.vai_tro'),N'id','ColumnId'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[nhan_vien] ch WHERE ch.[id_vai_tro] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[vai_tro] p WHERE p.[id]=ch.[id_vai_tro]))
            THROW 51004, N'Orphan FK: nhan_vien.id_vai_tro -> vai_tro.id. Existing data left unchanged.', 1;
        ALTER TABLE dbo.[nhan_vien] WITH CHECK ADD CONSTRAINT [FK_nhan_vien_vai_tro] FOREIGN KEY ([id_vai_tro]) REFERENCES dbo.[vai_tro] ([id]);
    END;

    -- Also validate/re-enable an equivalent legacy FK if it was disabled/untrusted.
    DECLARE @legacy_FK_nhan_vien_vai_tro SYSNAME;
    SELECT TOP (1) @legacy_FK_nhan_vien_vai_tro=fk.name
    FROM sys.foreign_keys fk JOIN sys.foreign_key_columns f ON f.constraint_object_id=fk.object_id
    WHERE f.parent_object_id=OBJECT_ID(N'dbo.nhan_vien') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.nhan_vien'),N'id_vai_tro','ColumnId')
      AND f.referenced_object_id=OBJECT_ID(N'dbo.vai_tro') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.vai_tro'),N'id','ColumnId')
      AND (fk.is_disabled=1 OR fk.is_not_trusted=1);
    IF @legacy_FK_nhan_vien_vai_tro IS NOT NULL
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[nhan_vien] ch WHERE ch.[id_vai_tro] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[vai_tro] p WHERE p.[id]=ch.[id_vai_tro]))
            THROW 51004, N'Orphan legacy FK: nhan_vien.id_vai_tro. Existing data left unchanged.', 1;
        DECLARE @check_FK_nhan_vien_vai_tro NVARCHAR(MAX)=N'ALTER TABLE dbo.[nhan_vien] WITH CHECK CHECK CONSTRAINT '+QUOTENAME(@legacy_FK_nhan_vien_vai_tro)+N';';
        EXEC sys.sp_executesql @check_FK_nhan_vien_vai_tro;
    END;

    IF NOT EXISTS (
        SELECT 1 FROM sys.foreign_key_columns f
        WHERE f.parent_object_id=OBJECT_ID(N'dbo.dia_chi_khach_hang') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.dia_chi_khach_hang'),N'id_khach_hang','ColumnId')
          AND f.referenced_object_id=OBJECT_ID(N'dbo.khach_hang') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.khach_hang'),N'id','ColumnId'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[dia_chi_khach_hang] ch WHERE ch.[id_khach_hang] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[khach_hang] p WHERE p.[id]=ch.[id_khach_hang]))
            THROW 51004, N'Orphan FK: dia_chi_khach_hang.id_khach_hang -> khach_hang.id. Existing data left unchanged.', 1;
        ALTER TABLE dbo.[dia_chi_khach_hang] WITH CHECK ADD CONSTRAINT [FK_dia_chi_khach_hang] FOREIGN KEY ([id_khach_hang]) REFERENCES dbo.[khach_hang] ([id]);
    END;

    -- Also validate/re-enable an equivalent legacy FK if it was disabled/untrusted.
    DECLARE @legacy_FK_dia_chi_khach_hang SYSNAME;
    SELECT TOP (1) @legacy_FK_dia_chi_khach_hang=fk.name
    FROM sys.foreign_keys fk JOIN sys.foreign_key_columns f ON f.constraint_object_id=fk.object_id
    WHERE f.parent_object_id=OBJECT_ID(N'dbo.dia_chi_khach_hang') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.dia_chi_khach_hang'),N'id_khach_hang','ColumnId')
      AND f.referenced_object_id=OBJECT_ID(N'dbo.khach_hang') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.khach_hang'),N'id','ColumnId')
      AND (fk.is_disabled=1 OR fk.is_not_trusted=1);
    IF @legacy_FK_dia_chi_khach_hang IS NOT NULL
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[dia_chi_khach_hang] ch WHERE ch.[id_khach_hang] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[khach_hang] p WHERE p.[id]=ch.[id_khach_hang]))
            THROW 51004, N'Orphan legacy FK: dia_chi_khach_hang.id_khach_hang. Existing data left unchanged.', 1;
        DECLARE @check_FK_dia_chi_khach_hang NVARCHAR(MAX)=N'ALTER TABLE dbo.[dia_chi_khach_hang] WITH CHECK CHECK CONSTRAINT '+QUOTENAME(@legacy_FK_dia_chi_khach_hang)+N';';
        EXEC sys.sp_executesql @check_FK_dia_chi_khach_hang;
    END;

    IF NOT EXISTS (
        SELECT 1 FROM sys.foreign_key_columns f
        WHERE f.parent_object_id=OBJECT_ID(N'dbo.san_pham') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.san_pham'),N'id_danh_muc','ColumnId')
          AND f.referenced_object_id=OBJECT_ID(N'dbo.danh_muc') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.danh_muc'),N'id','ColumnId'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[san_pham] ch WHERE ch.[id_danh_muc] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[danh_muc] p WHERE p.[id]=ch.[id_danh_muc]))
            THROW 51004, N'Orphan FK: san_pham.id_danh_muc -> danh_muc.id. Existing data left unchanged.', 1;
        ALTER TABLE dbo.[san_pham] WITH CHECK ADD CONSTRAINT [FK_san_pham_danh_muc] FOREIGN KEY ([id_danh_muc]) REFERENCES dbo.[danh_muc] ([id]);
    END;

    -- Also validate/re-enable an equivalent legacy FK if it was disabled/untrusted.
    DECLARE @legacy_FK_san_pham_danh_muc SYSNAME;
    SELECT TOP (1) @legacy_FK_san_pham_danh_muc=fk.name
    FROM sys.foreign_keys fk JOIN sys.foreign_key_columns f ON f.constraint_object_id=fk.object_id
    WHERE f.parent_object_id=OBJECT_ID(N'dbo.san_pham') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.san_pham'),N'id_danh_muc','ColumnId')
      AND f.referenced_object_id=OBJECT_ID(N'dbo.danh_muc') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.danh_muc'),N'id','ColumnId')
      AND (fk.is_disabled=1 OR fk.is_not_trusted=1);
    IF @legacy_FK_san_pham_danh_muc IS NOT NULL
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[san_pham] ch WHERE ch.[id_danh_muc] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[danh_muc] p WHERE p.[id]=ch.[id_danh_muc]))
            THROW 51004, N'Orphan legacy FK: san_pham.id_danh_muc. Existing data left unchanged.', 1;
        DECLARE @check_FK_san_pham_danh_muc NVARCHAR(MAX)=N'ALTER TABLE dbo.[san_pham] WITH CHECK CHECK CONSTRAINT '+QUOTENAME(@legacy_FK_san_pham_danh_muc)+N';';
        EXEC sys.sp_executesql @check_FK_san_pham_danh_muc;
    END;

    IF NOT EXISTS (
        SELECT 1 FROM sys.foreign_key_columns f
        WHERE f.parent_object_id=OBJECT_ID(N'dbo.san_pham') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.san_pham'),N'id_thuong_hieu','ColumnId')
          AND f.referenced_object_id=OBJECT_ID(N'dbo.thuong_hieu') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.thuong_hieu'),N'id','ColumnId'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[san_pham] ch WHERE ch.[id_thuong_hieu] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[thuong_hieu] p WHERE p.[id]=ch.[id_thuong_hieu]))
            THROW 51004, N'Orphan FK: san_pham.id_thuong_hieu -> thuong_hieu.id. Existing data left unchanged.', 1;
        ALTER TABLE dbo.[san_pham] WITH CHECK ADD CONSTRAINT [FK_san_pham_thuong_hieu] FOREIGN KEY ([id_thuong_hieu]) REFERENCES dbo.[thuong_hieu] ([id]);
    END;

    -- Also validate/re-enable an equivalent legacy FK if it was disabled/untrusted.
    DECLARE @legacy_FK_san_pham_thuong_hieu SYSNAME;
    SELECT TOP (1) @legacy_FK_san_pham_thuong_hieu=fk.name
    FROM sys.foreign_keys fk JOIN sys.foreign_key_columns f ON f.constraint_object_id=fk.object_id
    WHERE f.parent_object_id=OBJECT_ID(N'dbo.san_pham') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.san_pham'),N'id_thuong_hieu','ColumnId')
      AND f.referenced_object_id=OBJECT_ID(N'dbo.thuong_hieu') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.thuong_hieu'),N'id','ColumnId')
      AND (fk.is_disabled=1 OR fk.is_not_trusted=1);
    IF @legacy_FK_san_pham_thuong_hieu IS NOT NULL
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[san_pham] ch WHERE ch.[id_thuong_hieu] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[thuong_hieu] p WHERE p.[id]=ch.[id_thuong_hieu]))
            THROW 51004, N'Orphan legacy FK: san_pham.id_thuong_hieu. Existing data left unchanged.', 1;
        DECLARE @check_FK_san_pham_thuong_hieu NVARCHAR(MAX)=N'ALTER TABLE dbo.[san_pham] WITH CHECK CHECK CONSTRAINT '+QUOTENAME(@legacy_FK_san_pham_thuong_hieu)+N';';
        EXEC sys.sp_executesql @check_FK_san_pham_thuong_hieu;
    END;

    IF NOT EXISTS (
        SELECT 1 FROM sys.foreign_key_columns f
        WHERE f.parent_object_id=OBJECT_ID(N'dbo.san_pham') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.san_pham'),N'id_chat_lieu','ColumnId')
          AND f.referenced_object_id=OBJECT_ID(N'dbo.chat_lieu') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.chat_lieu'),N'id','ColumnId'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[san_pham] ch WHERE ch.[id_chat_lieu] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[chat_lieu] p WHERE p.[id]=ch.[id_chat_lieu]))
            THROW 51004, N'Orphan FK: san_pham.id_chat_lieu -> chat_lieu.id. Existing data left unchanged.', 1;
        ALTER TABLE dbo.[san_pham] WITH CHECK ADD CONSTRAINT [FK_san_pham_chat_lieu] FOREIGN KEY ([id_chat_lieu]) REFERENCES dbo.[chat_lieu] ([id]);
    END;

    -- Also validate/re-enable an equivalent legacy FK if it was disabled/untrusted.
    DECLARE @legacy_FK_san_pham_chat_lieu SYSNAME;
    SELECT TOP (1) @legacy_FK_san_pham_chat_lieu=fk.name
    FROM sys.foreign_keys fk JOIN sys.foreign_key_columns f ON f.constraint_object_id=fk.object_id
    WHERE f.parent_object_id=OBJECT_ID(N'dbo.san_pham') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.san_pham'),N'id_chat_lieu','ColumnId')
      AND f.referenced_object_id=OBJECT_ID(N'dbo.chat_lieu') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.chat_lieu'),N'id','ColumnId')
      AND (fk.is_disabled=1 OR fk.is_not_trusted=1);
    IF @legacy_FK_san_pham_chat_lieu IS NOT NULL
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[san_pham] ch WHERE ch.[id_chat_lieu] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[chat_lieu] p WHERE p.[id]=ch.[id_chat_lieu]))
            THROW 51004, N'Orphan legacy FK: san_pham.id_chat_lieu. Existing data left unchanged.', 1;
        DECLARE @check_FK_san_pham_chat_lieu NVARCHAR(MAX)=N'ALTER TABLE dbo.[san_pham] WITH CHECK CHECK CONSTRAINT '+QUOTENAME(@legacy_FK_san_pham_chat_lieu)+N';';
        EXEC sys.sp_executesql @check_FK_san_pham_chat_lieu;
    END;

    IF NOT EXISTS (
        SELECT 1 FROM sys.foreign_key_columns f
        WHERE f.parent_object_id=OBJECT_ID(N'dbo.san_pham') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.san_pham'),N'id_kieu_dang','ColumnId')
          AND f.referenced_object_id=OBJECT_ID(N'dbo.kieu_dang') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.kieu_dang'),N'id','ColumnId'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[san_pham] ch WHERE ch.[id_kieu_dang] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[kieu_dang] p WHERE p.[id]=ch.[id_kieu_dang]))
            THROW 51004, N'Orphan FK: san_pham.id_kieu_dang -> kieu_dang.id. Existing data left unchanged.', 1;
        ALTER TABLE dbo.[san_pham] WITH CHECK ADD CONSTRAINT [FK_san_pham_kieu_dang] FOREIGN KEY ([id_kieu_dang]) REFERENCES dbo.[kieu_dang] ([id]);
    END;

    -- Also validate/re-enable an equivalent legacy FK if it was disabled/untrusted.
    DECLARE @legacy_FK_san_pham_kieu_dang SYSNAME;
    SELECT TOP (1) @legacy_FK_san_pham_kieu_dang=fk.name
    FROM sys.foreign_keys fk JOIN sys.foreign_key_columns f ON f.constraint_object_id=fk.object_id
    WHERE f.parent_object_id=OBJECT_ID(N'dbo.san_pham') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.san_pham'),N'id_kieu_dang','ColumnId')
      AND f.referenced_object_id=OBJECT_ID(N'dbo.kieu_dang') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.kieu_dang'),N'id','ColumnId')
      AND (fk.is_disabled=1 OR fk.is_not_trusted=1);
    IF @legacy_FK_san_pham_kieu_dang IS NOT NULL
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[san_pham] ch WHERE ch.[id_kieu_dang] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[kieu_dang] p WHERE p.[id]=ch.[id_kieu_dang]))
            THROW 51004, N'Orphan legacy FK: san_pham.id_kieu_dang. Existing data left unchanged.', 1;
        DECLARE @check_FK_san_pham_kieu_dang NVARCHAR(MAX)=N'ALTER TABLE dbo.[san_pham] WITH CHECK CHECK CONSTRAINT '+QUOTENAME(@legacy_FK_san_pham_kieu_dang)+N';';
        EXEC sys.sp_executesql @check_FK_san_pham_kieu_dang;
    END;

    IF NOT EXISTS (
        SELECT 1 FROM sys.foreign_key_columns f
        WHERE f.parent_object_id=OBJECT_ID(N'dbo.san_pham') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.san_pham'),N'id_co_giay','ColumnId')
          AND f.referenced_object_id=OBJECT_ID(N'dbo.co_giay') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.co_giay'),N'id','ColumnId'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[san_pham] ch WHERE ch.[id_co_giay] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[co_giay] p WHERE p.[id]=ch.[id_co_giay]))
            THROW 51004, N'Orphan FK: san_pham.id_co_giay -> co_giay.id. Existing data left unchanged.', 1;
        ALTER TABLE dbo.[san_pham] WITH CHECK ADD CONSTRAINT [FK_san_pham_co_giay] FOREIGN KEY ([id_co_giay]) REFERENCES dbo.[co_giay] ([id]);
    END;

    -- Also validate/re-enable an equivalent legacy FK if it was disabled/untrusted.
    DECLARE @legacy_FK_san_pham_co_giay SYSNAME;
    SELECT TOP (1) @legacy_FK_san_pham_co_giay=fk.name
    FROM sys.foreign_keys fk JOIN sys.foreign_key_columns f ON f.constraint_object_id=fk.object_id
    WHERE f.parent_object_id=OBJECT_ID(N'dbo.san_pham') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.san_pham'),N'id_co_giay','ColumnId')
      AND f.referenced_object_id=OBJECT_ID(N'dbo.co_giay') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.co_giay'),N'id','ColumnId')
      AND (fk.is_disabled=1 OR fk.is_not_trusted=1);
    IF @legacy_FK_san_pham_co_giay IS NOT NULL
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[san_pham] ch WHERE ch.[id_co_giay] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[co_giay] p WHERE p.[id]=ch.[id_co_giay]))
            THROW 51004, N'Orphan legacy FK: san_pham.id_co_giay. Existing data left unchanged.', 1;
        DECLARE @check_FK_san_pham_co_giay NVARCHAR(MAX)=N'ALTER TABLE dbo.[san_pham] WITH CHECK CHECK CONSTRAINT '+QUOTENAME(@legacy_FK_san_pham_co_giay)+N';';
        EXEC sys.sp_executesql @check_FK_san_pham_co_giay;
    END;

    IF NOT EXISTS (
        SELECT 1 FROM sys.foreign_key_columns f
        WHERE f.parent_object_id=OBJECT_ID(N'dbo.san_pham') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.san_pham'),N'id_xuat_xu','ColumnId')
          AND f.referenced_object_id=OBJECT_ID(N'dbo.xuat_xu') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.xuat_xu'),N'id','ColumnId'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[san_pham] ch WHERE ch.[id_xuat_xu] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[xuat_xu] p WHERE p.[id]=ch.[id_xuat_xu]))
            THROW 51004, N'Orphan FK: san_pham.id_xuat_xu -> xuat_xu.id. Existing data left unchanged.', 1;
        ALTER TABLE dbo.[san_pham] WITH CHECK ADD CONSTRAINT [FK_san_pham_xuat_xu] FOREIGN KEY ([id_xuat_xu]) REFERENCES dbo.[xuat_xu] ([id]);
    END;

    -- Also validate/re-enable an equivalent legacy FK if it was disabled/untrusted.
    DECLARE @legacy_FK_san_pham_xuat_xu SYSNAME;
    SELECT TOP (1) @legacy_FK_san_pham_xuat_xu=fk.name
    FROM sys.foreign_keys fk JOIN sys.foreign_key_columns f ON f.constraint_object_id=fk.object_id
    WHERE f.parent_object_id=OBJECT_ID(N'dbo.san_pham') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.san_pham'),N'id_xuat_xu','ColumnId')
      AND f.referenced_object_id=OBJECT_ID(N'dbo.xuat_xu') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.xuat_xu'),N'id','ColumnId')
      AND (fk.is_disabled=1 OR fk.is_not_trusted=1);
    IF @legacy_FK_san_pham_xuat_xu IS NOT NULL
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[san_pham] ch WHERE ch.[id_xuat_xu] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[xuat_xu] p WHERE p.[id]=ch.[id_xuat_xu]))
            THROW 51004, N'Orphan legacy FK: san_pham.id_xuat_xu. Existing data left unchanged.', 1;
        DECLARE @check_FK_san_pham_xuat_xu NVARCHAR(MAX)=N'ALTER TABLE dbo.[san_pham] WITH CHECK CHECK CONSTRAINT '+QUOTENAME(@legacy_FK_san_pham_xuat_xu)+N';';
        EXEC sys.sp_executesql @check_FK_san_pham_xuat_xu;
    END;

    IF NOT EXISTS (
        SELECT 1 FROM sys.foreign_key_columns f
        WHERE f.parent_object_id=OBJECT_ID(N'dbo.san_pham_chi_tiet') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.san_pham_chi_tiet'),N'id_san_pham','ColumnId')
          AND f.referenced_object_id=OBJECT_ID(N'dbo.san_pham') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.san_pham'),N'id','ColumnId'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[san_pham_chi_tiet] ch WHERE ch.[id_san_pham] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[san_pham] p WHERE p.[id]=ch.[id_san_pham]))
            THROW 51004, N'Orphan FK: san_pham_chi_tiet.id_san_pham -> san_pham.id. Existing data left unchanged.', 1;
        ALTER TABLE dbo.[san_pham_chi_tiet] WITH CHECK ADD CONSTRAINT [FK_spct_san_pham] FOREIGN KEY ([id_san_pham]) REFERENCES dbo.[san_pham] ([id]);
    END;

    -- Also validate/re-enable an equivalent legacy FK if it was disabled/untrusted.
    DECLARE @legacy_FK_spct_san_pham SYSNAME;
    SELECT TOP (1) @legacy_FK_spct_san_pham=fk.name
    FROM sys.foreign_keys fk JOIN sys.foreign_key_columns f ON f.constraint_object_id=fk.object_id
    WHERE f.parent_object_id=OBJECT_ID(N'dbo.san_pham_chi_tiet') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.san_pham_chi_tiet'),N'id_san_pham','ColumnId')
      AND f.referenced_object_id=OBJECT_ID(N'dbo.san_pham') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.san_pham'),N'id','ColumnId')
      AND (fk.is_disabled=1 OR fk.is_not_trusted=1);
    IF @legacy_FK_spct_san_pham IS NOT NULL
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[san_pham_chi_tiet] ch WHERE ch.[id_san_pham] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[san_pham] p WHERE p.[id]=ch.[id_san_pham]))
            THROW 51004, N'Orphan legacy FK: san_pham_chi_tiet.id_san_pham. Existing data left unchanged.', 1;
        DECLARE @check_FK_spct_san_pham NVARCHAR(MAX)=N'ALTER TABLE dbo.[san_pham_chi_tiet] WITH CHECK CHECK CONSTRAINT '+QUOTENAME(@legacy_FK_spct_san_pham)+N';';
        EXEC sys.sp_executesql @check_FK_spct_san_pham;
    END;

    IF NOT EXISTS (
        SELECT 1 FROM sys.foreign_key_columns f
        WHERE f.parent_object_id=OBJECT_ID(N'dbo.san_pham_chi_tiet') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.san_pham_chi_tiet'),N'id_mau_sac','ColumnId')
          AND f.referenced_object_id=OBJECT_ID(N'dbo.mau_sac') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.mau_sac'),N'id','ColumnId'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[san_pham_chi_tiet] ch WHERE ch.[id_mau_sac] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[mau_sac] p WHERE p.[id]=ch.[id_mau_sac]))
            THROW 51004, N'Orphan FK: san_pham_chi_tiet.id_mau_sac -> mau_sac.id. Existing data left unchanged.', 1;
        ALTER TABLE dbo.[san_pham_chi_tiet] WITH CHECK ADD CONSTRAINT [FK_spct_mau_sac] FOREIGN KEY ([id_mau_sac]) REFERENCES dbo.[mau_sac] ([id]);
    END;

    -- Also validate/re-enable an equivalent legacy FK if it was disabled/untrusted.
    DECLARE @legacy_FK_spct_mau_sac SYSNAME;
    SELECT TOP (1) @legacy_FK_spct_mau_sac=fk.name
    FROM sys.foreign_keys fk JOIN sys.foreign_key_columns f ON f.constraint_object_id=fk.object_id
    WHERE f.parent_object_id=OBJECT_ID(N'dbo.san_pham_chi_tiet') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.san_pham_chi_tiet'),N'id_mau_sac','ColumnId')
      AND f.referenced_object_id=OBJECT_ID(N'dbo.mau_sac') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.mau_sac'),N'id','ColumnId')
      AND (fk.is_disabled=1 OR fk.is_not_trusted=1);
    IF @legacy_FK_spct_mau_sac IS NOT NULL
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[san_pham_chi_tiet] ch WHERE ch.[id_mau_sac] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[mau_sac] p WHERE p.[id]=ch.[id_mau_sac]))
            THROW 51004, N'Orphan legacy FK: san_pham_chi_tiet.id_mau_sac. Existing data left unchanged.', 1;
        DECLARE @check_FK_spct_mau_sac NVARCHAR(MAX)=N'ALTER TABLE dbo.[san_pham_chi_tiet] WITH CHECK CHECK CONSTRAINT '+QUOTENAME(@legacy_FK_spct_mau_sac)+N';';
        EXEC sys.sp_executesql @check_FK_spct_mau_sac;
    END;

    IF NOT EXISTS (
        SELECT 1 FROM sys.foreign_key_columns f
        WHERE f.parent_object_id=OBJECT_ID(N'dbo.san_pham_chi_tiet') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.san_pham_chi_tiet'),N'id_kich_thuoc','ColumnId')
          AND f.referenced_object_id=OBJECT_ID(N'dbo.kich_thuoc') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.kich_thuoc'),N'id','ColumnId'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[san_pham_chi_tiet] ch WHERE ch.[id_kich_thuoc] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[kich_thuoc] p WHERE p.[id]=ch.[id_kich_thuoc]))
            THROW 51004, N'Orphan FK: san_pham_chi_tiet.id_kich_thuoc -> kich_thuoc.id. Existing data left unchanged.', 1;
        ALTER TABLE dbo.[san_pham_chi_tiet] WITH CHECK ADD CONSTRAINT [FK_spct_kich_thuoc] FOREIGN KEY ([id_kich_thuoc]) REFERENCES dbo.[kich_thuoc] ([id]);
    END;

    -- Also validate/re-enable an equivalent legacy FK if it was disabled/untrusted.
    DECLARE @legacy_FK_spct_kich_thuoc SYSNAME;
    SELECT TOP (1) @legacy_FK_spct_kich_thuoc=fk.name
    FROM sys.foreign_keys fk JOIN sys.foreign_key_columns f ON f.constraint_object_id=fk.object_id
    WHERE f.parent_object_id=OBJECT_ID(N'dbo.san_pham_chi_tiet') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.san_pham_chi_tiet'),N'id_kich_thuoc','ColumnId')
      AND f.referenced_object_id=OBJECT_ID(N'dbo.kich_thuoc') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.kich_thuoc'),N'id','ColumnId')
      AND (fk.is_disabled=1 OR fk.is_not_trusted=1);
    IF @legacy_FK_spct_kich_thuoc IS NOT NULL
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[san_pham_chi_tiet] ch WHERE ch.[id_kich_thuoc] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[kich_thuoc] p WHERE p.[id]=ch.[id_kich_thuoc]))
            THROW 51004, N'Orphan legacy FK: san_pham_chi_tiet.id_kich_thuoc. Existing data left unchanged.', 1;
        DECLARE @check_FK_spct_kich_thuoc NVARCHAR(MAX)=N'ALTER TABLE dbo.[san_pham_chi_tiet] WITH CHECK CHECK CONSTRAINT '+QUOTENAME(@legacy_FK_spct_kich_thuoc)+N';';
        EXEC sys.sp_executesql @check_FK_spct_kich_thuoc;
    END;

    IF NOT EXISTS (
        SELECT 1 FROM sys.foreign_key_columns f
        WHERE f.parent_object_id=OBJECT_ID(N'dbo.hinh_anh_san_pham') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.hinh_anh_san_pham'),N'id_san_pham','ColumnId')
          AND f.referenced_object_id=OBJECT_ID(N'dbo.san_pham') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.san_pham'),N'id','ColumnId'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[hinh_anh_san_pham] ch WHERE ch.[id_san_pham] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[san_pham] p WHERE p.[id]=ch.[id_san_pham]))
            THROW 51004, N'Orphan FK: hinh_anh_san_pham.id_san_pham -> san_pham.id. Existing data left unchanged.', 1;
        ALTER TABLE dbo.[hinh_anh_san_pham] WITH CHECK ADD CONSTRAINT [FK_hinh_anh_san_pham] FOREIGN KEY ([id_san_pham]) REFERENCES dbo.[san_pham] ([id]);
    END;

    -- Also validate/re-enable an equivalent legacy FK if it was disabled/untrusted.
    DECLARE @legacy_FK_hinh_anh_san_pham SYSNAME;
    SELECT TOP (1) @legacy_FK_hinh_anh_san_pham=fk.name
    FROM sys.foreign_keys fk JOIN sys.foreign_key_columns f ON f.constraint_object_id=fk.object_id
    WHERE f.parent_object_id=OBJECT_ID(N'dbo.hinh_anh_san_pham') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.hinh_anh_san_pham'),N'id_san_pham','ColumnId')
      AND f.referenced_object_id=OBJECT_ID(N'dbo.san_pham') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.san_pham'),N'id','ColumnId')
      AND (fk.is_disabled=1 OR fk.is_not_trusted=1);
    IF @legacy_FK_hinh_anh_san_pham IS NOT NULL
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[hinh_anh_san_pham] ch WHERE ch.[id_san_pham] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[san_pham] p WHERE p.[id]=ch.[id_san_pham]))
            THROW 51004, N'Orphan legacy FK: hinh_anh_san_pham.id_san_pham. Existing data left unchanged.', 1;
        DECLARE @check_FK_hinh_anh_san_pham NVARCHAR(MAX)=N'ALTER TABLE dbo.[hinh_anh_san_pham] WITH CHECK CHECK CONSTRAINT '+QUOTENAME(@legacy_FK_hinh_anh_san_pham)+N';';
        EXEC sys.sp_executesql @check_FK_hinh_anh_san_pham;
    END;

    IF NOT EXISTS (
        SELECT 1 FROM sys.foreign_key_columns f
        WHERE f.parent_object_id=OBJECT_ID(N'dbo.phieu_giam_gia_khach_hang') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.phieu_giam_gia_khach_hang'),N'id_khach_hang','ColumnId')
          AND f.referenced_object_id=OBJECT_ID(N'dbo.khach_hang') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.khach_hang'),N'id','ColumnId'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[phieu_giam_gia_khach_hang] ch WHERE ch.[id_khach_hang] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[khach_hang] p WHERE p.[id]=ch.[id_khach_hang]))
            THROW 51004, N'Orphan FK: phieu_giam_gia_khach_hang.id_khach_hang -> khach_hang.id. Existing data left unchanged.', 1;
        ALTER TABLE dbo.[phieu_giam_gia_khach_hang] WITH CHECK ADD CONSTRAINT [FK_pggkh_khach_hang] FOREIGN KEY ([id_khach_hang]) REFERENCES dbo.[khach_hang] ([id]);
    END;

    -- Also validate/re-enable an equivalent legacy FK if it was disabled/untrusted.
    DECLARE @legacy_FK_pggkh_khach_hang SYSNAME;
    SELECT TOP (1) @legacy_FK_pggkh_khach_hang=fk.name
    FROM sys.foreign_keys fk JOIN sys.foreign_key_columns f ON f.constraint_object_id=fk.object_id
    WHERE f.parent_object_id=OBJECT_ID(N'dbo.phieu_giam_gia_khach_hang') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.phieu_giam_gia_khach_hang'),N'id_khach_hang','ColumnId')
      AND f.referenced_object_id=OBJECT_ID(N'dbo.khach_hang') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.khach_hang'),N'id','ColumnId')
      AND (fk.is_disabled=1 OR fk.is_not_trusted=1);
    IF @legacy_FK_pggkh_khach_hang IS NOT NULL
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[phieu_giam_gia_khach_hang] ch WHERE ch.[id_khach_hang] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[khach_hang] p WHERE p.[id]=ch.[id_khach_hang]))
            THROW 51004, N'Orphan legacy FK: phieu_giam_gia_khach_hang.id_khach_hang. Existing data left unchanged.', 1;
        DECLARE @check_FK_pggkh_khach_hang NVARCHAR(MAX)=N'ALTER TABLE dbo.[phieu_giam_gia_khach_hang] WITH CHECK CHECK CONSTRAINT '+QUOTENAME(@legacy_FK_pggkh_khach_hang)+N';';
        EXEC sys.sp_executesql @check_FK_pggkh_khach_hang;
    END;

    IF NOT EXISTS (
        SELECT 1 FROM sys.foreign_key_columns f
        WHERE f.parent_object_id=OBJECT_ID(N'dbo.phieu_giam_gia_khach_hang') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.phieu_giam_gia_khach_hang'),N'id_phieu_giam_gia','ColumnId')
          AND f.referenced_object_id=OBJECT_ID(N'dbo.phieu_giam_gia') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.phieu_giam_gia'),N'id','ColumnId'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[phieu_giam_gia_khach_hang] ch WHERE ch.[id_phieu_giam_gia] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[phieu_giam_gia] p WHERE p.[id]=ch.[id_phieu_giam_gia]))
            THROW 51004, N'Orphan FK: phieu_giam_gia_khach_hang.id_phieu_giam_gia -> phieu_giam_gia.id. Existing data left unchanged.', 1;
        ALTER TABLE dbo.[phieu_giam_gia_khach_hang] WITH CHECK ADD CONSTRAINT [FK_pggkh_phieu_giam_gia] FOREIGN KEY ([id_phieu_giam_gia]) REFERENCES dbo.[phieu_giam_gia] ([id]);
    END;

    -- Also validate/re-enable an equivalent legacy FK if it was disabled/untrusted.
    DECLARE @legacy_FK_pggkh_phieu_giam_gia SYSNAME;
    SELECT TOP (1) @legacy_FK_pggkh_phieu_giam_gia=fk.name
    FROM sys.foreign_keys fk JOIN sys.foreign_key_columns f ON f.constraint_object_id=fk.object_id
    WHERE f.parent_object_id=OBJECT_ID(N'dbo.phieu_giam_gia_khach_hang') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.phieu_giam_gia_khach_hang'),N'id_phieu_giam_gia','ColumnId')
      AND f.referenced_object_id=OBJECT_ID(N'dbo.phieu_giam_gia') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.phieu_giam_gia'),N'id','ColumnId')
      AND (fk.is_disabled=1 OR fk.is_not_trusted=1);
    IF @legacy_FK_pggkh_phieu_giam_gia IS NOT NULL
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[phieu_giam_gia_khach_hang] ch WHERE ch.[id_phieu_giam_gia] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[phieu_giam_gia] p WHERE p.[id]=ch.[id_phieu_giam_gia]))
            THROW 51004, N'Orphan legacy FK: phieu_giam_gia_khach_hang.id_phieu_giam_gia. Existing data left unchanged.', 1;
        DECLARE @check_FK_pggkh_phieu_giam_gia NVARCHAR(MAX)=N'ALTER TABLE dbo.[phieu_giam_gia_khach_hang] WITH CHECK CHECK CONSTRAINT '+QUOTENAME(@legacy_FK_pggkh_phieu_giam_gia)+N';';
        EXEC sys.sp_executesql @check_FK_pggkh_phieu_giam_gia;
    END;

    IF NOT EXISTS (
        SELECT 1 FROM sys.foreign_key_columns f
        WHERE f.parent_object_id=OBJECT_ID(N'dbo.chi_tiet_dot_giam_gia') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.chi_tiet_dot_giam_gia'),N'id_dot_giam_gia','ColumnId')
          AND f.referenced_object_id=OBJECT_ID(N'dbo.dot_giam_gia') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.dot_giam_gia'),N'id','ColumnId'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[chi_tiet_dot_giam_gia] ch WHERE ch.[id_dot_giam_gia] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[dot_giam_gia] p WHERE p.[id]=ch.[id_dot_giam_gia]))
            THROW 51004, N'Orphan FK: chi_tiet_dot_giam_gia.id_dot_giam_gia -> dot_giam_gia.id. Existing data left unchanged.', 1;
        ALTER TABLE dbo.[chi_tiet_dot_giam_gia] WITH CHECK ADD CONSTRAINT [FK_ctdgg_dot_giam_gia] FOREIGN KEY ([id_dot_giam_gia]) REFERENCES dbo.[dot_giam_gia] ([id]);
    END;

    -- Also validate/re-enable an equivalent legacy FK if it was disabled/untrusted.
    DECLARE @legacy_FK_ctdgg_dot_giam_gia SYSNAME;
    SELECT TOP (1) @legacy_FK_ctdgg_dot_giam_gia=fk.name
    FROM sys.foreign_keys fk JOIN sys.foreign_key_columns f ON f.constraint_object_id=fk.object_id
    WHERE f.parent_object_id=OBJECT_ID(N'dbo.chi_tiet_dot_giam_gia') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.chi_tiet_dot_giam_gia'),N'id_dot_giam_gia','ColumnId')
      AND f.referenced_object_id=OBJECT_ID(N'dbo.dot_giam_gia') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.dot_giam_gia'),N'id','ColumnId')
      AND (fk.is_disabled=1 OR fk.is_not_trusted=1);
    IF @legacy_FK_ctdgg_dot_giam_gia IS NOT NULL
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[chi_tiet_dot_giam_gia] ch WHERE ch.[id_dot_giam_gia] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[dot_giam_gia] p WHERE p.[id]=ch.[id_dot_giam_gia]))
            THROW 51004, N'Orphan legacy FK: chi_tiet_dot_giam_gia.id_dot_giam_gia. Existing data left unchanged.', 1;
        DECLARE @check_FK_ctdgg_dot_giam_gia NVARCHAR(MAX)=N'ALTER TABLE dbo.[chi_tiet_dot_giam_gia] WITH CHECK CHECK CONSTRAINT '+QUOTENAME(@legacy_FK_ctdgg_dot_giam_gia)+N';';
        EXEC sys.sp_executesql @check_FK_ctdgg_dot_giam_gia;
    END;

    IF NOT EXISTS (
        SELECT 1 FROM sys.foreign_key_columns f
        WHERE f.parent_object_id=OBJECT_ID(N'dbo.chi_tiet_dot_giam_gia') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.chi_tiet_dot_giam_gia'),N'id_san_pham_chi_tiet','ColumnId')
          AND f.referenced_object_id=OBJECT_ID(N'dbo.san_pham_chi_tiet') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.san_pham_chi_tiet'),N'id','ColumnId'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[chi_tiet_dot_giam_gia] ch WHERE ch.[id_san_pham_chi_tiet] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[san_pham_chi_tiet] p WHERE p.[id]=ch.[id_san_pham_chi_tiet]))
            THROW 51004, N'Orphan FK: chi_tiet_dot_giam_gia.id_san_pham_chi_tiet -> san_pham_chi_tiet.id. Existing data left unchanged.', 1;
        ALTER TABLE dbo.[chi_tiet_dot_giam_gia] WITH CHECK ADD CONSTRAINT [FK_ctdgg_spct] FOREIGN KEY ([id_san_pham_chi_tiet]) REFERENCES dbo.[san_pham_chi_tiet] ([id]);
    END;

    -- Also validate/re-enable an equivalent legacy FK if it was disabled/untrusted.
    DECLARE @legacy_FK_ctdgg_spct SYSNAME;
    SELECT TOP (1) @legacy_FK_ctdgg_spct=fk.name
    FROM sys.foreign_keys fk JOIN sys.foreign_key_columns f ON f.constraint_object_id=fk.object_id
    WHERE f.parent_object_id=OBJECT_ID(N'dbo.chi_tiet_dot_giam_gia') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.chi_tiet_dot_giam_gia'),N'id_san_pham_chi_tiet','ColumnId')
      AND f.referenced_object_id=OBJECT_ID(N'dbo.san_pham_chi_tiet') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.san_pham_chi_tiet'),N'id','ColumnId')
      AND (fk.is_disabled=1 OR fk.is_not_trusted=1);
    IF @legacy_FK_ctdgg_spct IS NOT NULL
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[chi_tiet_dot_giam_gia] ch WHERE ch.[id_san_pham_chi_tiet] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[san_pham_chi_tiet] p WHERE p.[id]=ch.[id_san_pham_chi_tiet]))
            THROW 51004, N'Orphan legacy FK: chi_tiet_dot_giam_gia.id_san_pham_chi_tiet. Existing data left unchanged.', 1;
        DECLARE @check_FK_ctdgg_spct NVARCHAR(MAX)=N'ALTER TABLE dbo.[chi_tiet_dot_giam_gia] WITH CHECK CHECK CONSTRAINT '+QUOTENAME(@legacy_FK_ctdgg_spct)+N';';
        EXEC sys.sp_executesql @check_FK_ctdgg_spct;
    END;

    IF NOT EXISTS (
        SELECT 1 FROM sys.foreign_key_columns f
        WHERE f.parent_object_id=OBJECT_ID(N'dbo.phuong_thuc_thanh_toan') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.phuong_thuc_thanh_toan'),N'id_hinh_thuc_thanh_toan','ColumnId')
          AND f.referenced_object_id=OBJECT_ID(N'dbo.hinh_thuc_thanh_toan') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.hinh_thuc_thanh_toan'),N'id','ColumnId'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[phuong_thuc_thanh_toan] ch WHERE ch.[id_hinh_thuc_thanh_toan] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[hinh_thuc_thanh_toan] p WHERE p.[id]=ch.[id_hinh_thuc_thanh_toan]))
            THROW 51004, N'Orphan FK: phuong_thuc_thanh_toan.id_hinh_thuc_thanh_toan -> hinh_thuc_thanh_toan.id. Existing data left unchanged.', 1;
        ALTER TABLE dbo.[phuong_thuc_thanh_toan] WITH CHECK ADD CONSTRAINT [FK_pttt_hinh_thuc] FOREIGN KEY ([id_hinh_thuc_thanh_toan]) REFERENCES dbo.[hinh_thuc_thanh_toan] ([id]);
    END;

    -- Also validate/re-enable an equivalent legacy FK if it was disabled/untrusted.
    DECLARE @legacy_FK_pttt_hinh_thuc SYSNAME;
    SELECT TOP (1) @legacy_FK_pttt_hinh_thuc=fk.name
    FROM sys.foreign_keys fk JOIN sys.foreign_key_columns f ON f.constraint_object_id=fk.object_id
    WHERE f.parent_object_id=OBJECT_ID(N'dbo.phuong_thuc_thanh_toan') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.phuong_thuc_thanh_toan'),N'id_hinh_thuc_thanh_toan','ColumnId')
      AND f.referenced_object_id=OBJECT_ID(N'dbo.hinh_thuc_thanh_toan') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.hinh_thuc_thanh_toan'),N'id','ColumnId')
      AND (fk.is_disabled=1 OR fk.is_not_trusted=1);
    IF @legacy_FK_pttt_hinh_thuc IS NOT NULL
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[phuong_thuc_thanh_toan] ch WHERE ch.[id_hinh_thuc_thanh_toan] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[hinh_thuc_thanh_toan] p WHERE p.[id]=ch.[id_hinh_thuc_thanh_toan]))
            THROW 51004, N'Orphan legacy FK: phuong_thuc_thanh_toan.id_hinh_thuc_thanh_toan. Existing data left unchanged.', 1;
        DECLARE @check_FK_pttt_hinh_thuc NVARCHAR(MAX)=N'ALTER TABLE dbo.[phuong_thuc_thanh_toan] WITH CHECK CHECK CONSTRAINT '+QUOTENAME(@legacy_FK_pttt_hinh_thuc)+N';';
        EXEC sys.sp_executesql @check_FK_pttt_hinh_thuc;
    END;

    IF NOT EXISTS (
        SELECT 1 FROM sys.foreign_key_columns f
        WHERE f.parent_object_id=OBJECT_ID(N'dbo.hoa_don') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.hoa_don'),N'id_khach_hang','ColumnId')
          AND f.referenced_object_id=OBJECT_ID(N'dbo.khach_hang') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.khach_hang'),N'id','ColumnId'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[hoa_don] ch WHERE ch.[id_khach_hang] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[khach_hang] p WHERE p.[id]=ch.[id_khach_hang]))
            THROW 51004, N'Orphan FK: hoa_don.id_khach_hang -> khach_hang.id. Existing data left unchanged.', 1;
        ALTER TABLE dbo.[hoa_don] WITH CHECK ADD CONSTRAINT [FK_hoa_don_khach_hang] FOREIGN KEY ([id_khach_hang]) REFERENCES dbo.[khach_hang] ([id]);
    END;

    -- Also validate/re-enable an equivalent legacy FK if it was disabled/untrusted.
    DECLARE @legacy_FK_hoa_don_khach_hang SYSNAME;
    SELECT TOP (1) @legacy_FK_hoa_don_khach_hang=fk.name
    FROM sys.foreign_keys fk JOIN sys.foreign_key_columns f ON f.constraint_object_id=fk.object_id
    WHERE f.parent_object_id=OBJECT_ID(N'dbo.hoa_don') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.hoa_don'),N'id_khach_hang','ColumnId')
      AND f.referenced_object_id=OBJECT_ID(N'dbo.khach_hang') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.khach_hang'),N'id','ColumnId')
      AND (fk.is_disabled=1 OR fk.is_not_trusted=1);
    IF @legacy_FK_hoa_don_khach_hang IS NOT NULL
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[hoa_don] ch WHERE ch.[id_khach_hang] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[khach_hang] p WHERE p.[id]=ch.[id_khach_hang]))
            THROW 51004, N'Orphan legacy FK: hoa_don.id_khach_hang. Existing data left unchanged.', 1;
        DECLARE @check_FK_hoa_don_khach_hang NVARCHAR(MAX)=N'ALTER TABLE dbo.[hoa_don] WITH CHECK CHECK CONSTRAINT '+QUOTENAME(@legacy_FK_hoa_don_khach_hang)+N';';
        EXEC sys.sp_executesql @check_FK_hoa_don_khach_hang;
    END;

    IF NOT EXISTS (
        SELECT 1 FROM sys.foreign_key_columns f
        WHERE f.parent_object_id=OBJECT_ID(N'dbo.hoa_don') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.hoa_don'),N'id_nhan_vien','ColumnId')
          AND f.referenced_object_id=OBJECT_ID(N'dbo.nhan_vien') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.nhan_vien'),N'id','ColumnId'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[hoa_don] ch WHERE ch.[id_nhan_vien] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[nhan_vien] p WHERE p.[id]=ch.[id_nhan_vien]))
            THROW 51004, N'Orphan FK: hoa_don.id_nhan_vien -> nhan_vien.id. Existing data left unchanged.', 1;
        ALTER TABLE dbo.[hoa_don] WITH CHECK ADD CONSTRAINT [FK_hoa_don_nhan_vien] FOREIGN KEY ([id_nhan_vien]) REFERENCES dbo.[nhan_vien] ([id]);
    END;

    -- Also validate/re-enable an equivalent legacy FK if it was disabled/untrusted.
    DECLARE @legacy_FK_hoa_don_nhan_vien SYSNAME;
    SELECT TOP (1) @legacy_FK_hoa_don_nhan_vien=fk.name
    FROM sys.foreign_keys fk JOIN sys.foreign_key_columns f ON f.constraint_object_id=fk.object_id
    WHERE f.parent_object_id=OBJECT_ID(N'dbo.hoa_don') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.hoa_don'),N'id_nhan_vien','ColumnId')
      AND f.referenced_object_id=OBJECT_ID(N'dbo.nhan_vien') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.nhan_vien'),N'id','ColumnId')
      AND (fk.is_disabled=1 OR fk.is_not_trusted=1);
    IF @legacy_FK_hoa_don_nhan_vien IS NOT NULL
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[hoa_don] ch WHERE ch.[id_nhan_vien] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[nhan_vien] p WHERE p.[id]=ch.[id_nhan_vien]))
            THROW 51004, N'Orphan legacy FK: hoa_don.id_nhan_vien. Existing data left unchanged.', 1;
        DECLARE @check_FK_hoa_don_nhan_vien NVARCHAR(MAX)=N'ALTER TABLE dbo.[hoa_don] WITH CHECK CHECK CONSTRAINT '+QUOTENAME(@legacy_FK_hoa_don_nhan_vien)+N';';
        EXEC sys.sp_executesql @check_FK_hoa_don_nhan_vien;
    END;

    IF NOT EXISTS (
        SELECT 1 FROM sys.foreign_key_columns f
        WHERE f.parent_object_id=OBJECT_ID(N'dbo.hoa_don') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.hoa_don'),N'id_phieu_giam_gia','ColumnId')
          AND f.referenced_object_id=OBJECT_ID(N'dbo.phieu_giam_gia') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.phieu_giam_gia'),N'id','ColumnId'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[hoa_don] ch WHERE ch.[id_phieu_giam_gia] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[phieu_giam_gia] p WHERE p.[id]=ch.[id_phieu_giam_gia]))
            THROW 51004, N'Orphan FK: hoa_don.id_phieu_giam_gia -> phieu_giam_gia.id. Existing data left unchanged.', 1;
        ALTER TABLE dbo.[hoa_don] WITH CHECK ADD CONSTRAINT [FK_hoa_don_phieu_giam_gia] FOREIGN KEY ([id_phieu_giam_gia]) REFERENCES dbo.[phieu_giam_gia] ([id]);
    END;

    -- Also validate/re-enable an equivalent legacy FK if it was disabled/untrusted.
    DECLARE @legacy_FK_hoa_don_phieu_giam_gia SYSNAME;
    SELECT TOP (1) @legacy_FK_hoa_don_phieu_giam_gia=fk.name
    FROM sys.foreign_keys fk JOIN sys.foreign_key_columns f ON f.constraint_object_id=fk.object_id
    WHERE f.parent_object_id=OBJECT_ID(N'dbo.hoa_don') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.hoa_don'),N'id_phieu_giam_gia','ColumnId')
      AND f.referenced_object_id=OBJECT_ID(N'dbo.phieu_giam_gia') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.phieu_giam_gia'),N'id','ColumnId')
      AND (fk.is_disabled=1 OR fk.is_not_trusted=1);
    IF @legacy_FK_hoa_don_phieu_giam_gia IS NOT NULL
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[hoa_don] ch WHERE ch.[id_phieu_giam_gia] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[phieu_giam_gia] p WHERE p.[id]=ch.[id_phieu_giam_gia]))
            THROW 51004, N'Orphan legacy FK: hoa_don.id_phieu_giam_gia. Existing data left unchanged.', 1;
        DECLARE @check_FK_hoa_don_phieu_giam_gia NVARCHAR(MAX)=N'ALTER TABLE dbo.[hoa_don] WITH CHECK CHECK CONSTRAINT '+QUOTENAME(@legacy_FK_hoa_don_phieu_giam_gia)+N';';
        EXEC sys.sp_executesql @check_FK_hoa_don_phieu_giam_gia;
    END;

    IF NOT EXISTS (
        SELECT 1 FROM sys.foreign_key_columns f
        WHERE f.parent_object_id=OBJECT_ID(N'dbo.hoa_don') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.hoa_don'),N'id_phuong_thuc_thanh_toan','ColumnId')
          AND f.referenced_object_id=OBJECT_ID(N'dbo.phuong_thuc_thanh_toan') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.phuong_thuc_thanh_toan'),N'id','ColumnId'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[hoa_don] ch WHERE ch.[id_phuong_thuc_thanh_toan] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[phuong_thuc_thanh_toan] p WHERE p.[id]=ch.[id_phuong_thuc_thanh_toan]))
            THROW 51004, N'Orphan FK: hoa_don.id_phuong_thuc_thanh_toan -> phuong_thuc_thanh_toan.id. Existing data left unchanged.', 1;
        ALTER TABLE dbo.[hoa_don] WITH CHECK ADD CONSTRAINT [FK_hoa_don_phuong_thuc] FOREIGN KEY ([id_phuong_thuc_thanh_toan]) REFERENCES dbo.[phuong_thuc_thanh_toan] ([id]);
    END;

    -- Also validate/re-enable an equivalent legacy FK if it was disabled/untrusted.
    DECLARE @legacy_FK_hoa_don_phuong_thuc SYSNAME;
    SELECT TOP (1) @legacy_FK_hoa_don_phuong_thuc=fk.name
    FROM sys.foreign_keys fk JOIN sys.foreign_key_columns f ON f.constraint_object_id=fk.object_id
    WHERE f.parent_object_id=OBJECT_ID(N'dbo.hoa_don') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.hoa_don'),N'id_phuong_thuc_thanh_toan','ColumnId')
      AND f.referenced_object_id=OBJECT_ID(N'dbo.phuong_thuc_thanh_toan') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.phuong_thuc_thanh_toan'),N'id','ColumnId')
      AND (fk.is_disabled=1 OR fk.is_not_trusted=1);
    IF @legacy_FK_hoa_don_phuong_thuc IS NOT NULL
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[hoa_don] ch WHERE ch.[id_phuong_thuc_thanh_toan] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[phuong_thuc_thanh_toan] p WHERE p.[id]=ch.[id_phuong_thuc_thanh_toan]))
            THROW 51004, N'Orphan legacy FK: hoa_don.id_phuong_thuc_thanh_toan. Existing data left unchanged.', 1;
        DECLARE @check_FK_hoa_don_phuong_thuc NVARCHAR(MAX)=N'ALTER TABLE dbo.[hoa_don] WITH CHECK CHECK CONSTRAINT '+QUOTENAME(@legacy_FK_hoa_don_phuong_thuc)+N';';
        EXEC sys.sp_executesql @check_FK_hoa_don_phuong_thuc;
    END;

    IF NOT EXISTS (
        SELECT 1 FROM sys.foreign_key_columns f
        WHERE f.parent_object_id=OBJECT_ID(N'dbo.hoa_don_chi_tiet') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.hoa_don_chi_tiet'),N'id_hoa_don','ColumnId')
          AND f.referenced_object_id=OBJECT_ID(N'dbo.hoa_don') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.hoa_don'),N'id','ColumnId'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[hoa_don_chi_tiet] ch WHERE ch.[id_hoa_don] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[hoa_don] p WHERE p.[id]=ch.[id_hoa_don]))
            THROW 51004, N'Orphan FK: hoa_don_chi_tiet.id_hoa_don -> hoa_don.id. Existing data left unchanged.', 1;
        ALTER TABLE dbo.[hoa_don_chi_tiet] WITH CHECK ADD CONSTRAINT [FK_hdct_hoa_don] FOREIGN KEY ([id_hoa_don]) REFERENCES dbo.[hoa_don] ([id]);
    END;

    -- Also validate/re-enable an equivalent legacy FK if it was disabled/untrusted.
    DECLARE @legacy_FK_hdct_hoa_don SYSNAME;
    SELECT TOP (1) @legacy_FK_hdct_hoa_don=fk.name
    FROM sys.foreign_keys fk JOIN sys.foreign_key_columns f ON f.constraint_object_id=fk.object_id
    WHERE f.parent_object_id=OBJECT_ID(N'dbo.hoa_don_chi_tiet') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.hoa_don_chi_tiet'),N'id_hoa_don','ColumnId')
      AND f.referenced_object_id=OBJECT_ID(N'dbo.hoa_don') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.hoa_don'),N'id','ColumnId')
      AND (fk.is_disabled=1 OR fk.is_not_trusted=1);
    IF @legacy_FK_hdct_hoa_don IS NOT NULL
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[hoa_don_chi_tiet] ch WHERE ch.[id_hoa_don] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[hoa_don] p WHERE p.[id]=ch.[id_hoa_don]))
            THROW 51004, N'Orphan legacy FK: hoa_don_chi_tiet.id_hoa_don. Existing data left unchanged.', 1;
        DECLARE @check_FK_hdct_hoa_don NVARCHAR(MAX)=N'ALTER TABLE dbo.[hoa_don_chi_tiet] WITH CHECK CHECK CONSTRAINT '+QUOTENAME(@legacy_FK_hdct_hoa_don)+N';';
        EXEC sys.sp_executesql @check_FK_hdct_hoa_don;
    END;

    IF NOT EXISTS (
        SELECT 1 FROM sys.foreign_key_columns f
        WHERE f.parent_object_id=OBJECT_ID(N'dbo.hoa_don_chi_tiet') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.hoa_don_chi_tiet'),N'id_san_pham_chi_tiet','ColumnId')
          AND f.referenced_object_id=OBJECT_ID(N'dbo.san_pham_chi_tiet') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.san_pham_chi_tiet'),N'id','ColumnId'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[hoa_don_chi_tiet] ch WHERE ch.[id_san_pham_chi_tiet] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[san_pham_chi_tiet] p WHERE p.[id]=ch.[id_san_pham_chi_tiet]))
            THROW 51004, N'Orphan FK: hoa_don_chi_tiet.id_san_pham_chi_tiet -> san_pham_chi_tiet.id. Existing data left unchanged.', 1;
        ALTER TABLE dbo.[hoa_don_chi_tiet] WITH CHECK ADD CONSTRAINT [FK_hdct_spct] FOREIGN KEY ([id_san_pham_chi_tiet]) REFERENCES dbo.[san_pham_chi_tiet] ([id]);
    END;

    -- Also validate/re-enable an equivalent legacy FK if it was disabled/untrusted.
    DECLARE @legacy_FK_hdct_spct SYSNAME;
    SELECT TOP (1) @legacy_FK_hdct_spct=fk.name
    FROM sys.foreign_keys fk JOIN sys.foreign_key_columns f ON f.constraint_object_id=fk.object_id
    WHERE f.parent_object_id=OBJECT_ID(N'dbo.hoa_don_chi_tiet') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.hoa_don_chi_tiet'),N'id_san_pham_chi_tiet','ColumnId')
      AND f.referenced_object_id=OBJECT_ID(N'dbo.san_pham_chi_tiet') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.san_pham_chi_tiet'),N'id','ColumnId')
      AND (fk.is_disabled=1 OR fk.is_not_trusted=1);
    IF @legacy_FK_hdct_spct IS NOT NULL
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[hoa_don_chi_tiet] ch WHERE ch.[id_san_pham_chi_tiet] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[san_pham_chi_tiet] p WHERE p.[id]=ch.[id_san_pham_chi_tiet]))
            THROW 51004, N'Orphan legacy FK: hoa_don_chi_tiet.id_san_pham_chi_tiet. Existing data left unchanged.', 1;
        DECLARE @check_FK_hdct_spct NVARCHAR(MAX)=N'ALTER TABLE dbo.[hoa_don_chi_tiet] WITH CHECK CHECK CONSTRAINT '+QUOTENAME(@legacy_FK_hdct_spct)+N';';
        EXEC sys.sp_executesql @check_FK_hdct_spct;
    END;

    IF NOT EXISTS (
        SELECT 1 FROM sys.foreign_key_columns f
        WHERE f.parent_object_id=OBJECT_ID(N'dbo.lich_su_hoa_don') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.lich_su_hoa_don'),N'id_hoa_don','ColumnId')
          AND f.referenced_object_id=OBJECT_ID(N'dbo.hoa_don') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.hoa_don'),N'id','ColumnId'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[lich_su_hoa_don] ch WHERE ch.[id_hoa_don] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[hoa_don] p WHERE p.[id]=ch.[id_hoa_don]))
            THROW 51004, N'Orphan FK: lich_su_hoa_don.id_hoa_don -> hoa_don.id. Existing data left unchanged.', 1;
        ALTER TABLE dbo.[lich_su_hoa_don] WITH CHECK ADD CONSTRAINT [FK_lshd_hoa_don] FOREIGN KEY ([id_hoa_don]) REFERENCES dbo.[hoa_don] ([id]);
    END;

    -- Also validate/re-enable an equivalent legacy FK if it was disabled/untrusted.
    DECLARE @legacy_FK_lshd_hoa_don SYSNAME;
    SELECT TOP (1) @legacy_FK_lshd_hoa_don=fk.name
    FROM sys.foreign_keys fk JOIN sys.foreign_key_columns f ON f.constraint_object_id=fk.object_id
    WHERE f.parent_object_id=OBJECT_ID(N'dbo.lich_su_hoa_don') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.lich_su_hoa_don'),N'id_hoa_don','ColumnId')
      AND f.referenced_object_id=OBJECT_ID(N'dbo.hoa_don') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.hoa_don'),N'id','ColumnId')
      AND (fk.is_disabled=1 OR fk.is_not_trusted=1);
    IF @legacy_FK_lshd_hoa_don IS NOT NULL
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[lich_su_hoa_don] ch WHERE ch.[id_hoa_don] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[hoa_don] p WHERE p.[id]=ch.[id_hoa_don]))
            THROW 51004, N'Orphan legacy FK: lich_su_hoa_don.id_hoa_don. Existing data left unchanged.', 1;
        DECLARE @check_FK_lshd_hoa_don NVARCHAR(MAX)=N'ALTER TABLE dbo.[lich_su_hoa_don] WITH CHECK CHECK CONSTRAINT '+QUOTENAME(@legacy_FK_lshd_hoa_don)+N';';
        EXEC sys.sp_executesql @check_FK_lshd_hoa_don;
    END;

    IF NOT EXISTS (
        SELECT 1 FROM sys.foreign_key_columns f
        WHERE f.parent_object_id=OBJECT_ID(N'dbo.lich_su_thanh_toan') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.lich_su_thanh_toan'),N'id_hoa_don','ColumnId')
          AND f.referenced_object_id=OBJECT_ID(N'dbo.hoa_don') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.hoa_don'),N'id','ColumnId'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[lich_su_thanh_toan] ch WHERE ch.[id_hoa_don] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[hoa_don] p WHERE p.[id]=ch.[id_hoa_don]))
            THROW 51004, N'Orphan FK: lich_su_thanh_toan.id_hoa_don -> hoa_don.id. Existing data left unchanged.', 1;
        ALTER TABLE dbo.[lich_su_thanh_toan] WITH CHECK ADD CONSTRAINT [FK_lstt_hoa_don] FOREIGN KEY ([id_hoa_don]) REFERENCES dbo.[hoa_don] ([id]);
    END;

    -- Also validate/re-enable an equivalent legacy FK if it was disabled/untrusted.
    DECLARE @legacy_FK_lstt_hoa_don SYSNAME;
    SELECT TOP (1) @legacy_FK_lstt_hoa_don=fk.name
    FROM sys.foreign_keys fk JOIN sys.foreign_key_columns f ON f.constraint_object_id=fk.object_id
    WHERE f.parent_object_id=OBJECT_ID(N'dbo.lich_su_thanh_toan') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.lich_su_thanh_toan'),N'id_hoa_don','ColumnId')
      AND f.referenced_object_id=OBJECT_ID(N'dbo.hoa_don') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.hoa_don'),N'id','ColumnId')
      AND (fk.is_disabled=1 OR fk.is_not_trusted=1);
    IF @legacy_FK_lstt_hoa_don IS NOT NULL
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[lich_su_thanh_toan] ch WHERE ch.[id_hoa_don] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[hoa_don] p WHERE p.[id]=ch.[id_hoa_don]))
            THROW 51004, N'Orphan legacy FK: lich_su_thanh_toan.id_hoa_don. Existing data left unchanged.', 1;
        DECLARE @check_FK_lstt_hoa_don NVARCHAR(MAX)=N'ALTER TABLE dbo.[lich_su_thanh_toan] WITH CHECK CHECK CONSTRAINT '+QUOTENAME(@legacy_FK_lstt_hoa_don)+N';';
        EXEC sys.sp_executesql @check_FK_lstt_hoa_don;
    END;

    IF NOT EXISTS (
        SELECT 1 FROM sys.foreign_key_columns f
        WHERE f.parent_object_id=OBJECT_ID(N'dbo.lich_su_thanh_toan') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.lich_su_thanh_toan'),N'id_phuong_thuc_thanh_toan','ColumnId')
          AND f.referenced_object_id=OBJECT_ID(N'dbo.phuong_thuc_thanh_toan') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.phuong_thuc_thanh_toan'),N'id','ColumnId'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[lich_su_thanh_toan] ch WHERE ch.[id_phuong_thuc_thanh_toan] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[phuong_thuc_thanh_toan] p WHERE p.[id]=ch.[id_phuong_thuc_thanh_toan]))
            THROW 51004, N'Orphan FK: lich_su_thanh_toan.id_phuong_thuc_thanh_toan -> phuong_thuc_thanh_toan.id. Existing data left unchanged.', 1;
        ALTER TABLE dbo.[lich_su_thanh_toan] WITH CHECK ADD CONSTRAINT [FK_lstt_phuong_thuc] FOREIGN KEY ([id_phuong_thuc_thanh_toan]) REFERENCES dbo.[phuong_thuc_thanh_toan] ([id]);
    END;

    -- Also validate/re-enable an equivalent legacy FK if it was disabled/untrusted.
    DECLARE @legacy_FK_lstt_phuong_thuc SYSNAME;
    SELECT TOP (1) @legacy_FK_lstt_phuong_thuc=fk.name
    FROM sys.foreign_keys fk JOIN sys.foreign_key_columns f ON f.constraint_object_id=fk.object_id
    WHERE f.parent_object_id=OBJECT_ID(N'dbo.lich_su_thanh_toan') AND f.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.lich_su_thanh_toan'),N'id_phuong_thuc_thanh_toan','ColumnId')
      AND f.referenced_object_id=OBJECT_ID(N'dbo.phuong_thuc_thanh_toan') AND f.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.phuong_thuc_thanh_toan'),N'id','ColumnId')
      AND (fk.is_disabled=1 OR fk.is_not_trusted=1);
    IF @legacy_FK_lstt_phuong_thuc IS NOT NULL
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[lich_su_thanh_toan] ch WHERE ch.[id_phuong_thuc_thanh_toan] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.[phuong_thuc_thanh_toan] p WHERE p.[id]=ch.[id_phuong_thuc_thanh_toan]))
            THROW 51004, N'Orphan legacy FK: lich_su_thanh_toan.id_phuong_thuc_thanh_toan. Existing data left unchanged.', 1;
        DECLARE @check_FK_lstt_phuong_thuc NVARCHAR(MAX)=N'ALTER TABLE dbo.[lich_su_thanh_toan] WITH CHECK CHECK CONSTRAINT '+QUOTENAME(@legacy_FK_lstt_phuong_thuc)+N';';
        EXEC sys.sp_executesql @check_FK_lstt_phuong_thuc;
    END;

    -- 3. Unique keys. Stop on existing duplicates; never discard user rows.

    IF NOT EXISTS (SELECT 1 FROM sys.indexes i WHERE i.object_id=OBJECT_ID(N'dbo.nhan_vien') AND i.is_unique=1 AND i.is_disabled=0 AND (i.has_filter=0 OR REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(i.filter_definition),'[',''),']',''),'(',''),')',''),' ','')=N'MA_NHAN_VIENISNOTNULL')
        AND (SELECT COUNT(*) FROM sys.index_columns ic WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal>0)=1 AND EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal=1 AND c.name=N'ma_nhan_vien'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[nhan_vien]  WHERE ma_nhan_vien IS NOT NULL GROUP BY ma_nhan_vien HAVING COUNT_BIG(*)>1)
            THROW 51005, N'Duplicate unique key: nhan_vien(ma_nhan_vien). Existing data left unchanged.', 1;
        CREATE UNIQUE INDEX [UX_nhan_vien_ma_nhan_vien] ON dbo.[nhan_vien] (ma_nhan_vien)  WHERE ma_nhan_vien IS NOT NULL;
    END;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes i WHERE i.object_id=OBJECT_ID(N'dbo.nhan_vien') AND i.is_unique=1 AND i.is_disabled=0 AND (i.has_filter=0 OR REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(i.filter_definition),'[',''),']',''),'(',''),')',''),' ','')=N'TEN_DANG_NHAPISNOTNULL')
        AND (SELECT COUNT(*) FROM sys.index_columns ic WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal>0)=1 AND EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal=1 AND c.name=N'ten_dang_nhap'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[nhan_vien]  WHERE ten_dang_nhap IS NOT NULL GROUP BY ten_dang_nhap HAVING COUNT_BIG(*)>1)
            THROW 51005, N'Duplicate unique key: nhan_vien(ten_dang_nhap). Existing data left unchanged.', 1;
        CREATE UNIQUE INDEX [UX_nhan_vien_ten_dang_nhap] ON dbo.[nhan_vien] (ten_dang_nhap)  WHERE ten_dang_nhap IS NOT NULL;
    END;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes i WHERE i.object_id=OBJECT_ID(N'dbo.nhan_vien') AND i.is_unique=1 AND i.is_disabled=0 AND (i.has_filter=0 OR REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(i.filter_definition),'[',''),']',''),'(',''),')',''),' ','')=N'EMAILISNOTNULL')
        AND (SELECT COUNT(*) FROM sys.index_columns ic WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal>0)=1 AND EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal=1 AND c.name=N'email'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[nhan_vien]  WHERE email IS NOT NULL GROUP BY email HAVING COUNT_BIG(*)>1)
            THROW 51005, N'Duplicate unique key: nhan_vien(email). Existing data left unchanged.', 1;
        CREATE UNIQUE INDEX [UX_nhan_vien_email] ON dbo.[nhan_vien] (email)  WHERE email IS NOT NULL;
    END;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes i WHERE i.object_id=OBJECT_ID(N'dbo.khach_hang') AND i.is_unique=1 AND i.is_disabled=0 AND (i.has_filter=0 OR REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(i.filter_definition),'[',''),']',''),'(',''),')',''),' ','')=N'MA_KHACH_HANGISNOTNULL')
        AND (SELECT COUNT(*) FROM sys.index_columns ic WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal>0)=1 AND EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal=1 AND c.name=N'ma_khach_hang'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[khach_hang]  WHERE ma_khach_hang IS NOT NULL GROUP BY ma_khach_hang HAVING COUNT_BIG(*)>1)
            THROW 51005, N'Duplicate unique key: khach_hang(ma_khach_hang). Existing data left unchanged.', 1;
        CREATE UNIQUE INDEX [UX_khach_hang_ma_khach_hang] ON dbo.[khach_hang] (ma_khach_hang)  WHERE ma_khach_hang IS NOT NULL;
    END;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes i WHERE i.object_id=OBJECT_ID(N'dbo.khach_hang') AND i.is_unique=1 AND i.is_disabled=0 AND (i.has_filter=0 OR REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(i.filter_definition),'[',''),']',''),'(',''),')',''),' ','')=N'TEN_TAI_KHOANISNOTNULL')
        AND (SELECT COUNT(*) FROM sys.index_columns ic WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal>0)=1 AND EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal=1 AND c.name=N'ten_tai_khoan'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[khach_hang]  WHERE ten_tai_khoan IS NOT NULL GROUP BY ten_tai_khoan HAVING COUNT_BIG(*)>1)
            THROW 51005, N'Duplicate unique key: khach_hang(ten_tai_khoan). Existing data left unchanged.', 1;
        CREATE UNIQUE INDEX [UX_khach_hang_ten_tai_khoan] ON dbo.[khach_hang] (ten_tai_khoan)  WHERE ten_tai_khoan IS NOT NULL;
    END;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes i WHERE i.object_id=OBJECT_ID(N'dbo.khach_hang') AND i.is_unique=1 AND i.is_disabled=0 AND (i.has_filter=0 OR REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(i.filter_definition),'[',''),']',''),'(',''),')',''),' ','')=N'EMAILISNOTNULL')
        AND (SELECT COUNT(*) FROM sys.index_columns ic WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal>0)=1 AND EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal=1 AND c.name=N'email'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[khach_hang]  WHERE email IS NOT NULL GROUP BY email HAVING COUNT_BIG(*)>1)
            THROW 51005, N'Duplicate unique key: khach_hang(email). Existing data left unchanged.', 1;
        CREATE UNIQUE INDEX [UX_khach_hang_email] ON dbo.[khach_hang] (email)  WHERE email IS NOT NULL;
    END;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes i WHERE i.object_id=OBJECT_ID(N'dbo.danh_muc') AND i.is_unique=1 AND i.is_disabled=0 AND (i.has_filter=0 OR REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(i.filter_definition),'[',''),']',''),'(',''),')',''),' ','')=N'MA_DANH_MUCISNOTNULL')
        AND (SELECT COUNT(*) FROM sys.index_columns ic WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal>0)=1 AND EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal=1 AND c.name=N'ma_danh_muc'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[danh_muc]  WHERE ma_danh_muc IS NOT NULL GROUP BY ma_danh_muc HAVING COUNT_BIG(*)>1)
            THROW 51005, N'Duplicate unique key: danh_muc(ma_danh_muc). Existing data left unchanged.', 1;
        CREATE UNIQUE INDEX [UX_danh_muc_ma_danh_muc] ON dbo.[danh_muc] (ma_danh_muc)  WHERE ma_danh_muc IS NOT NULL;
    END;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes i WHERE i.object_id=OBJECT_ID(N'dbo.thuong_hieu') AND i.is_unique=1 AND i.is_disabled=0 AND (i.has_filter=0 OR REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(i.filter_definition),'[',''),']',''),'(',''),')',''),' ','')=N'MA_THUONG_HIEUISNOTNULL')
        AND (SELECT COUNT(*) FROM sys.index_columns ic WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal>0)=1 AND EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal=1 AND c.name=N'ma_thuong_hieu'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[thuong_hieu]  WHERE ma_thuong_hieu IS NOT NULL GROUP BY ma_thuong_hieu HAVING COUNT_BIG(*)>1)
            THROW 51005, N'Duplicate unique key: thuong_hieu(ma_thuong_hieu). Existing data left unchanged.', 1;
        CREATE UNIQUE INDEX [UX_thuong_hieu_ma_thuong_hieu] ON dbo.[thuong_hieu] (ma_thuong_hieu)  WHERE ma_thuong_hieu IS NOT NULL;
    END;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes i WHERE i.object_id=OBJECT_ID(N'dbo.chat_lieu') AND i.is_unique=1 AND i.is_disabled=0 AND (i.has_filter=0 OR REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(i.filter_definition),'[',''),']',''),'(',''),')',''),' ','')=N'MA_CHAT_LIEUISNOTNULL')
        AND (SELECT COUNT(*) FROM sys.index_columns ic WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal>0)=1 AND EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal=1 AND c.name=N'ma_chat_lieu'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[chat_lieu]  WHERE ma_chat_lieu IS NOT NULL GROUP BY ma_chat_lieu HAVING COUNT_BIG(*)>1)
            THROW 51005, N'Duplicate unique key: chat_lieu(ma_chat_lieu). Existing data left unchanged.', 1;
        CREATE UNIQUE INDEX [UX_chat_lieu_ma_chat_lieu] ON dbo.[chat_lieu] (ma_chat_lieu)  WHERE ma_chat_lieu IS NOT NULL;
    END;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes i WHERE i.object_id=OBJECT_ID(N'dbo.xuat_xu') AND i.is_unique=1 AND i.is_disabled=0 AND (i.has_filter=0 OR REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(i.filter_definition),'[',''),']',''),'(',''),')',''),' ','')=N'MA_XUAT_XUISNOTNULL')
        AND (SELECT COUNT(*) FROM sys.index_columns ic WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal>0)=1 AND EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal=1 AND c.name=N'ma_xuat_xu'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[xuat_xu]  WHERE ma_xuat_xu IS NOT NULL GROUP BY ma_xuat_xu HAVING COUNT_BIG(*)>1)
            THROW 51005, N'Duplicate unique key: xuat_xu(ma_xuat_xu). Existing data left unchanged.', 1;
        CREATE UNIQUE INDEX [UX_xuat_xu_ma_xuat_xu] ON dbo.[xuat_xu] (ma_xuat_xu)  WHERE ma_xuat_xu IS NOT NULL;
    END;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes i WHERE i.object_id=OBJECT_ID(N'dbo.co_giay') AND i.is_unique=1 AND i.is_disabled=0 AND (i.has_filter=0 OR REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(i.filter_definition),'[',''),']',''),'(',''),')',''),' ','')=N'MA_CO_GIAYISNOTNULL')
        AND (SELECT COUNT(*) FROM sys.index_columns ic WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal>0)=1 AND EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal=1 AND c.name=N'ma_co_giay'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[co_giay]  WHERE ma_co_giay IS NOT NULL GROUP BY ma_co_giay HAVING COUNT_BIG(*)>1)
            THROW 51005, N'Duplicate unique key: co_giay(ma_co_giay). Existing data left unchanged.', 1;
        CREATE UNIQUE INDEX [UX_co_giay_ma_co_giay] ON dbo.[co_giay] (ma_co_giay)  WHERE ma_co_giay IS NOT NULL;
    END;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes i WHERE i.object_id=OBJECT_ID(N'dbo.kieu_dang') AND i.is_unique=1 AND i.is_disabled=0 AND (i.has_filter=0 OR REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(i.filter_definition),'[',''),']',''),'(',''),')',''),' ','')=N'MA_KIEU_DANGISNOTNULL')
        AND (SELECT COUNT(*) FROM sys.index_columns ic WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal>0)=1 AND EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal=1 AND c.name=N'ma_kieu_dang'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[kieu_dang]  WHERE ma_kieu_dang IS NOT NULL GROUP BY ma_kieu_dang HAVING COUNT_BIG(*)>1)
            THROW 51005, N'Duplicate unique key: kieu_dang(ma_kieu_dang). Existing data left unchanged.', 1;
        CREATE UNIQUE INDEX [UX_kieu_dang_ma_kieu_dang] ON dbo.[kieu_dang] (ma_kieu_dang)  WHERE ma_kieu_dang IS NOT NULL;
    END;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes i WHERE i.object_id=OBJECT_ID(N'dbo.mau_sac') AND i.is_unique=1 AND i.is_disabled=0 AND (i.has_filter=0 OR REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(i.filter_definition),'[',''),']',''),'(',''),')',''),' ','')=N'MA_MAU_SACISNOTNULL')
        AND (SELECT COUNT(*) FROM sys.index_columns ic WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal>0)=1 AND EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal=1 AND c.name=N'ma_mau_sac'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[mau_sac]  WHERE ma_mau_sac IS NOT NULL GROUP BY ma_mau_sac HAVING COUNT_BIG(*)>1)
            THROW 51005, N'Duplicate unique key: mau_sac(ma_mau_sac). Existing data left unchanged.', 1;
        CREATE UNIQUE INDEX [UX_mau_sac_ma_mau_sac] ON dbo.[mau_sac] (ma_mau_sac)  WHERE ma_mau_sac IS NOT NULL;
    END;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes i WHERE i.object_id=OBJECT_ID(N'dbo.kich_thuoc') AND i.is_unique=1 AND i.is_disabled=0 AND (i.has_filter=0 OR REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(i.filter_definition),'[',''),']',''),'(',''),')',''),' ','')=N'GIA_TRIISNOTNULL')
        AND (SELECT COUNT(*) FROM sys.index_columns ic WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal>0)=1 AND EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal=1 AND c.name=N'gia_tri'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[kich_thuoc]  WHERE gia_tri IS NOT NULL GROUP BY gia_tri HAVING COUNT_BIG(*)>1)
            THROW 51005, N'Duplicate unique key: kich_thuoc(gia_tri). Existing data left unchanged.', 1;
        CREATE UNIQUE INDEX [UX_kich_thuoc_gia_tri] ON dbo.[kich_thuoc] (gia_tri)  WHERE gia_tri IS NOT NULL;
    END;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes i WHERE i.object_id=OBJECT_ID(N'dbo.san_pham') AND i.is_unique=1 AND i.is_disabled=0 AND (i.has_filter=0 OR REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(i.filter_definition),'[',''),']',''),'(',''),')',''),' ','')=N'MA_SAN_PHAMISNOTNULL')
        AND (SELECT COUNT(*) FROM sys.index_columns ic WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal>0)=1 AND EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal=1 AND c.name=N'ma_san_pham'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[san_pham]  WHERE ma_san_pham IS NOT NULL GROUP BY ma_san_pham HAVING COUNT_BIG(*)>1)
            THROW 51005, N'Duplicate unique key: san_pham(ma_san_pham). Existing data left unchanged.', 1;
        CREATE UNIQUE INDEX [UX_san_pham_ma_san_pham] ON dbo.[san_pham] (ma_san_pham)  WHERE ma_san_pham IS NOT NULL;
    END;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes i WHERE i.object_id=OBJECT_ID(N'dbo.san_pham_chi_tiet') AND i.is_unique=1 AND i.is_disabled=0 AND (i.has_filter=0 OR REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(i.filter_definition),'[',''),']',''),'(',''),')',''),' ','')=N'MA_CHI_TIET_SAN_PHAMISNOTNULL')
        AND (SELECT COUNT(*) FROM sys.index_columns ic WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal>0)=1 AND EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal=1 AND c.name=N'ma_chi_tiet_san_pham'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[san_pham_chi_tiet]  WHERE ma_chi_tiet_san_pham IS NOT NULL GROUP BY ma_chi_tiet_san_pham HAVING COUNT_BIG(*)>1)
            THROW 51005, N'Duplicate unique key: san_pham_chi_tiet(ma_chi_tiet_san_pham). Existing data left unchanged.', 1;
        CREATE UNIQUE INDEX [UX_san_pham_chi_tiet_ma_chi_tiet_san_pham] ON dbo.[san_pham_chi_tiet] (ma_chi_tiet_san_pham)  WHERE ma_chi_tiet_san_pham IS NOT NULL;
    END;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes i WHERE i.object_id=OBJECT_ID(N'dbo.san_pham_chi_tiet') AND i.is_unique=1 AND i.is_disabled=0 AND (i.has_filter=0 OR REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(i.filter_definition),'[',''),']',''),'(',''),')',''),' ','')=N'SKUISNOTNULL')
        AND (SELECT COUNT(*) FROM sys.index_columns ic WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal>0)=1 AND EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal=1 AND c.name=N'sku'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[san_pham_chi_tiet]  WHERE sku IS NOT NULL GROUP BY sku HAVING COUNT_BIG(*)>1)
            THROW 51005, N'Duplicate unique key: san_pham_chi_tiet(sku). Existing data left unchanged.', 1;
        CREATE UNIQUE INDEX [UX_san_pham_chi_tiet_sku] ON dbo.[san_pham_chi_tiet] (sku)  WHERE sku IS NOT NULL;
    END;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes i WHERE i.object_id=OBJECT_ID(N'dbo.phieu_giam_gia') AND i.is_unique=1 AND i.is_disabled=0 AND (i.has_filter=0 OR REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(i.filter_definition),'[',''),']',''),'(',''),')',''),' ','')=N'MA_PHIEU_GIAM_GIAISNOTNULL')
        AND (SELECT COUNT(*) FROM sys.index_columns ic WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal>0)=1 AND EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal=1 AND c.name=N'ma_phieu_giam_gia'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[phieu_giam_gia]  WHERE ma_phieu_giam_gia IS NOT NULL GROUP BY ma_phieu_giam_gia HAVING COUNT_BIG(*)>1)
            THROW 51005, N'Duplicate unique key: phieu_giam_gia(ma_phieu_giam_gia). Existing data left unchanged.', 1;
        CREATE UNIQUE INDEX [UX_phieu_giam_gia_ma_phieu_giam_gia] ON dbo.[phieu_giam_gia] (ma_phieu_giam_gia)  WHERE ma_phieu_giam_gia IS NOT NULL;
    END;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes i WHERE i.object_id=OBJECT_ID(N'dbo.dot_giam_gia') AND i.is_unique=1 AND i.is_disabled=0 AND (i.has_filter=0 OR REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(i.filter_definition),'[',''),']',''),'(',''),')',''),' ','')=N'MA_DOT_GIAM_GIAISNOTNULL')
        AND (SELECT COUNT(*) FROM sys.index_columns ic WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal>0)=1 AND EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal=1 AND c.name=N'ma_dot_giam_gia'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[dot_giam_gia]  WHERE ma_dot_giam_gia IS NOT NULL GROUP BY ma_dot_giam_gia HAVING COUNT_BIG(*)>1)
            THROW 51005, N'Duplicate unique key: dot_giam_gia(ma_dot_giam_gia). Existing data left unchanged.', 1;
        CREATE UNIQUE INDEX [UX_dot_giam_gia_ma_dot_giam_gia] ON dbo.[dot_giam_gia] (ma_dot_giam_gia)  WHERE ma_dot_giam_gia IS NOT NULL;
    END;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes i WHERE i.object_id=OBJECT_ID(N'dbo.hinh_thuc_thanh_toan') AND i.is_unique=1 AND i.is_disabled=0 AND (i.has_filter=0 OR REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(i.filter_definition),'[',''),']',''),'(',''),')',''),' ','')=N'MA_HINH_THUCISNOTNULL')
        AND (SELECT COUNT(*) FROM sys.index_columns ic WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal>0)=1 AND EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal=1 AND c.name=N'ma_hinh_thuc'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[hinh_thuc_thanh_toan]  WHERE ma_hinh_thuc IS NOT NULL GROUP BY ma_hinh_thuc HAVING COUNT_BIG(*)>1)
            THROW 51005, N'Duplicate unique key: hinh_thuc_thanh_toan(ma_hinh_thuc). Existing data left unchanged.', 1;
        CREATE UNIQUE INDEX [UX_hinh_thuc_thanh_toan_ma_hinh_thuc] ON dbo.[hinh_thuc_thanh_toan] (ma_hinh_thuc)  WHERE ma_hinh_thuc IS NOT NULL;
    END;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes i WHERE i.object_id=OBJECT_ID(N'dbo.phuong_thuc_thanh_toan') AND i.is_unique=1 AND i.is_disabled=0 AND (i.has_filter=0 OR REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(i.filter_definition),'[',''),']',''),'(',''),')',''),' ','')=N'MA_PHUONG_THUCISNOTNULL')
        AND (SELECT COUNT(*) FROM sys.index_columns ic WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal>0)=1 AND EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal=1 AND c.name=N'ma_phuong_thuc'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[phuong_thuc_thanh_toan]  WHERE ma_phuong_thuc IS NOT NULL GROUP BY ma_phuong_thuc HAVING COUNT_BIG(*)>1)
            THROW 51005, N'Duplicate unique key: phuong_thuc_thanh_toan(ma_phuong_thuc). Existing data left unchanged.', 1;
        CREATE UNIQUE INDEX [UX_phuong_thuc_thanh_toan_ma_phuong_thuc] ON dbo.[phuong_thuc_thanh_toan] (ma_phuong_thuc)  WHERE ma_phuong_thuc IS NOT NULL;
    END;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes i WHERE i.object_id=OBJECT_ID(N'dbo.hoa_don') AND i.is_unique=1 AND i.is_disabled=0 AND (i.has_filter=0 OR REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(i.filter_definition),'[',''),']',''),'(',''),')',''),' ','')=N'MA_HOA_DONISNOTNULL')
        AND (SELECT COUNT(*) FROM sys.index_columns ic WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal>0)=1 AND EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal=1 AND c.name=N'ma_hoa_don'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[hoa_don]  WHERE ma_hoa_don IS NOT NULL GROUP BY ma_hoa_don HAVING COUNT_BIG(*)>1)
            THROW 51005, N'Duplicate unique key: hoa_don(ma_hoa_don). Existing data left unchanged.', 1;
        CREATE UNIQUE INDEX [UX_hoa_don_ma_hoa_don] ON dbo.[hoa_don] (ma_hoa_don)  WHERE ma_hoa_don IS NOT NULL;
    END;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes i WHERE i.object_id=OBJECT_ID(N'dbo.lich_su_thanh_toan') AND i.is_unique=1 AND i.is_disabled=0 AND (i.has_filter=0 OR REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(i.filter_definition),'[',''),']',''),'(',''),')',''),' ','')=N'MA_GIAO_DICHISNOTNULL')
        AND (SELECT COUNT(*) FROM sys.index_columns ic WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal>0)=1 AND EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal=1 AND c.name=N'ma_giao_dich'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[lich_su_thanh_toan]  WHERE ma_giao_dich IS NOT NULL GROUP BY ma_giao_dich HAVING COUNT_BIG(*)>1)
            THROW 51005, N'Duplicate unique key: lich_su_thanh_toan(ma_giao_dich). Existing data left unchanged.', 1;
        CREATE UNIQUE INDEX [UX_lich_su_thanh_toan_ma_giao_dich] ON dbo.[lich_su_thanh_toan] (ma_giao_dich)  WHERE ma_giao_dich IS NOT NULL;
    END;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes i WHERE i.object_id=OBJECT_ID(N'dbo.san_pham_chi_tiet') AND i.is_unique=1 AND i.is_disabled=0 AND i.has_filter=0
        AND (SELECT COUNT(*) FROM sys.index_columns ic WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal>0)=3 AND EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal=1 AND c.name=N'id_san_pham') AND EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal=2 AND c.name=N'id_mau_sac') AND EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal=3 AND c.name=N'id_kich_thuoc'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[san_pham_chi_tiet]  GROUP BY id_san_pham, id_mau_sac, id_kich_thuoc HAVING COUNT_BIG(*)>1)
            THROW 51005, N'Duplicate unique key: san_pham_chi_tiet(id_san_pham, id_mau_sac, id_kich_thuoc). Existing data left unchanged.', 1;
        CREATE UNIQUE INDEX [UX_spct_bien_the] ON dbo.[san_pham_chi_tiet] (id_san_pham, id_mau_sac, id_kich_thuoc) ;
    END;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes i WHERE i.object_id=OBJECT_ID(N'dbo.phieu_giam_gia_khach_hang') AND i.is_unique=1 AND i.is_disabled=0 AND i.has_filter=0
        AND (SELECT COUNT(*) FROM sys.index_columns ic WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal>0)=2 AND EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal=1 AND c.name=N'id_khach_hang') AND EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal=2 AND c.name=N'id_phieu_giam_gia'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[phieu_giam_gia_khach_hang]  GROUP BY id_khach_hang, id_phieu_giam_gia HAVING COUNT_BIG(*)>1)
            THROW 51005, N'Duplicate unique key: phieu_giam_gia_khach_hang(id_khach_hang, id_phieu_giam_gia). Existing data left unchanged.', 1;
        CREATE UNIQUE INDEX [UX_pggkh_kh_phieu] ON dbo.[phieu_giam_gia_khach_hang] (id_khach_hang, id_phieu_giam_gia) ;
    END;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes i WHERE i.object_id=OBJECT_ID(N'dbo.chi_tiet_dot_giam_gia') AND i.is_unique=1 AND i.is_disabled=0 AND i.has_filter=0
        AND (SELECT COUNT(*) FROM sys.index_columns ic WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal>0)=2 AND EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal=1 AND c.name=N'id_dot_giam_gia') AND EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal=2 AND c.name=N'id_san_pham_chi_tiet'))
    BEGIN
        IF EXISTS (SELECT 1 FROM dbo.[chi_tiet_dot_giam_gia]  GROUP BY id_dot_giam_gia, id_san_pham_chi_tiet HAVING COUNT_BIG(*)>1)
            THROW 51005, N'Duplicate unique key: chi_tiet_dot_giam_gia(id_dot_giam_gia, id_san_pham_chi_tiet). Existing data left unchanged.', 1;
        CREATE UNIQUE INDEX [UX_ctdgg_dot_spct] ON dbo.[chi_tiet_dot_giam_gia] (id_dot_giam_gia, id_san_pham_chi_tiet) ;
    END;

    -- 4. Non-destructive seeds. Existing business codes always win.
    -- Attributes shared by 02_seed_san_pham and 03_seed_test_hoa_don+san_pham.

    INSERT INTO danh_muc (ma_danh_muc, ten_danh_muc, mo_ta, trang_thai)
    SELECT v.ma, v.ten, v.mo_ta, 1
    FROM (VALUES
        ('DM001', N'Giày chạy bộ', N'Giày dành cho chạy bộ và luyện tập'),
        ('DM002', N'Giày thể thao', N'Giày thể thao sử dụng hàng ngày'),
        ('DM003', N'Giày bóng rổ', N'Giày chuyên dụng cho bóng rổ'),
        ('DM004', N'Giày thời trang', N'Giày sneaker và thời trang đường phố'),
        ('DM005', N'Giày đá bóng', N'Giày sử dụng khi chơi bóng đá'),
        ('DM006', N'Giày tennis', N'Giày dành cho tennis và thể thao sân'),
        ('DM007', N'Giày đi bộ', N'Giày nhẹ dùng để đi bộ hàng ngày'),
        ('DM008', N'Giày tập gym', N'Giày phục vụ tập luyện và fitness')
    ) v(ma, ten, mo_ta)
    WHERE NOT EXISTS (
        SELECT 1 FROM danh_muc x WHERE x.ma_danh_muc = v.ma
    );

    INSERT INTO thuong_hieu (ma_thuong_hieu, ten_thuong_hieu, trang_thai)
    SELECT v.ma, v.ten, 1
    FROM (VALUES
        ('TH001', N'Nike'),
        ('TH002', N'Adidas'),
        ('TH003', N'Puma'),
        ('TH004', N'New Balance'),
        ('TH005', N'Converse'),
        ('TH006', N'Vans'),
        ('TH007', N'ASICS'),
        ('TH008', N'Under Armour'),
        ('TH009', N'Reebok'),
        ('TH010', N'Skechers')
    ) v(ma, ten)
    WHERE NOT EXISTS (
        SELECT 1 FROM thuong_hieu x WHERE x.ma_thuong_hieu = v.ma
    );

    INSERT INTO chat_lieu (ma_chat_lieu, ten_chat_lieu, trang_thai)
    SELECT v.ma, v.ten, 1
    FROM (VALUES
        ('CL001', N'Vải Mesh'),
        ('CL002', N'Da tổng hợp'),
        ('CL003', N'Da thật'),
        ('CL004', N'Vải Canvas'),
        ('CL005', N'Vải Knit'),
        ('CL006', N'Da lộn'),
        ('CL007', N'Polyester'),
        ('CL008', N'Cao su'),
        ('CL009', N'Vải dệt'),
        ('CL010', N'Sợi tổng hợp')
    ) v(ma, ten)
    WHERE NOT EXISTS (
        SELECT 1 FROM chat_lieu x WHERE x.ma_chat_lieu = v.ma
    );

    INSERT INTO xuat_xu (ma_xuat_xu, ten_xuat_xu, trang_thai)
    SELECT v.ma, v.ten, 1
    FROM (VALUES
        ('XX001', N'Việt Nam'),
        ('XX002', N'Indonesia'),
        ('XX003', N'Trung Quốc'),
        ('XX004', N'Mỹ'),
        ('XX005', N'Nhật Bản'),
        ('XX006', N'Hàn Quốc'),
        ('XX007', N'Thái Lan'),
        ('XX008', N'Đức')
    ) v(ma, ten)
    WHERE NOT EXISTS (
        SELECT 1 FROM xuat_xu x WHERE x.ma_xuat_xu = v.ma
    );

    INSERT INTO co_giay (ma_co_giay, ten_co_giay, trang_thai)
    SELECT v.ma, v.ten, 1
    FROM (VALUES
        ('CG001', N'Cổ thấp'),
        ('CG002', N'Cổ trung'),
        ('CG003', N'Cổ cao')
    ) v(ma, ten)
    WHERE NOT EXISTS (
        SELECT 1 FROM co_giay x WHERE x.ma_co_giay = v.ma
    );

    INSERT INTO kieu_dang (ma_kieu_dang, ten_kieu_dang, trang_thai)
    SELECT v.ma, v.ten, 1
    FROM (VALUES
        ('KD001', N'Running'),
        ('KD002', N'Sneaker'),
        ('KD003', N'Basketball'),
        ('KD004', N'Casual'),
        ('KD005', N'Training'),
        ('KD006', N'Football'),
        ('KD007', N'Tennis'),
        ('KD008', N'Walking'),
        ('KD009', N'Skate'),
        ('KD010', N'Lifestyle')
    ) v(ma, ten)
    WHERE NOT EXISTS (
        SELECT 1 FROM kieu_dang x WHERE x.ma_kieu_dang = v.ma
    );

    INSERT INTO mau_sac
        (ma_mau_sac, ten_mau_sac, ma_mau_hex, trang_thai, ngay_tao, ngay_cap_nhat)
    SELECT v.ma, v.ten, v.hex, 1, SYSDATETIME(), SYSDATETIME()
    FROM (VALUES
        ('MS001', N'Đen',        '#000000'),
        ('MS002', N'Trắng',      '#FFFFFF'),
        ('MS003', N'Đỏ',         '#FF0000'),
        ('MS004', N'Xanh dương', '#0066FF'),
        ('MS005', N'Xám',        '#808080'),
        ('MS006', N'Xanh lá',    '#00A651'),
        ('MS007', N'Be',         '#F5F5DC'),
        ('MS008', N'Nâu',        '#8B4513'),
        ('MS009', N'Vàng',        '#FFD700'),
        ('MS010', N'Cam',         '#FF8C00'),
        ('MS011', N'Hồng',        '#FF69B4'),
        ('MS012', N'Tím',         '#800080'),
        ('MS013', N'Xanh navy',   '#000080'),
        ('MS014', N'Kem',         '#FFFDD0'),
        ('MS015', N'Bạc',         '#C0C0C0')
    ) v(ma, ten, hex)
    WHERE NOT EXISTS (
        SELECT 1 FROM mau_sac x WHERE x.ma_mau_sac = v.ma
    );

    INSERT INTO kich_thuoc
        (gia_tri, ghi_chu, trang_thai, ngay_tao, ngay_cap_nhat)
    SELECT v.gia_tri, CONCAT(N'Size ', v.gia_tri), 1, SYSDATETIME(), SYSDATETIME()
    FROM (VALUES
        ('35'), ('36'), ('36.5'), ('37'), ('37.5'), ('38'), ('38.5'),
        ('39'), ('39.5'), ('40'), ('40.5'), ('41'), ('41.5'), ('42'),
        ('42.5'), ('43'), ('43.5'), ('44'), ('44.5'), ('45'), ('46')
    ) v(gia_tri)
    WHERE NOT EXISTS (
        SELECT 1 FROM kich_thuoc x WHERE x.gia_tri = v.gia_tri
    );

    /* =====================================================
       2. SẢN PHẨM
       Dùng mã sản phẩm làm khóa nghiệp vụ, không phụ thuộc ID.
       ===================================================== */

    INSERT INTO san_pham
        (id_danh_muc, id_thuong_hieu, id_chat_lieu, id_kieu_dang,
         id_co_giay, id_xuat_xu, ma_san_pham, ten_san_pham,
         mo_ta_chi_tiet, ngay_tao, nguoi_tao, nguoi_cap_nhat,
         ngay_cap_nhat, trang_thai)
    SELECT
        dm.id, th.id, cl.id, kd.id, cg.id, xx.id,
        v.ma_san_pham, v.ten_san_pham, v.mo_ta,
        SYSDATETIME(), NULL, NULL, SYSDATETIME(), 1
    FROM (VALUES
        ('SP001', 'DM001', 'TH001', 'CL001', 'KD001', 'CG001', 'XX002',
         N'Nike Air Zoom Pegasus 41',
         N'Giày chạy bộ Nike Pegasus 41'),

        ('SP002', 'DM002', 'TH002', 'CL005', 'KD002', 'CG001', 'XX002',
         N'Adidas Ultraboost Light',
         N'Giày thể thao Adidas Ultraboost Light'),

        ('SP003', 'DM003', 'TH001', 'CL002', 'KD003', 'CG003', 'XX002',
         N'Nike Air Jordan One Take',
         N'Giày bóng rổ Nike Air Jordan'),

        ('SP004', 'DM004', 'TH005', 'CL004', 'KD004', 'CG001', 'XX001',
         N'Converse Chuck 70',
         N'Giày sneaker Converse Chuck 70'),

        ('SP005', 'DM005', 'TH003', 'CL002', 'KD006', 'CG001', 'XX003',
         N'Puma Future Play',
         N'Giày đá bóng Puma Future Play'),

        ('SP006', 'DM006', 'TH007', 'CL001', 'KD007', 'CG001', 'XX005',
         N'ASICS Gel-Resolution',
         N'Giày tennis ASICS Gel-Resolution')
    ) v(ma_san_pham, ma_dm, ma_th, ma_cl, ma_kd, ma_cg, ma_xx,
        ten_san_pham, mo_ta)
    JOIN danh_muc dm ON dm.ma_danh_muc = v.ma_dm
    JOIN thuong_hieu th ON th.ma_thuong_hieu = v.ma_th
    JOIN chat_lieu cl ON cl.ma_chat_lieu = v.ma_cl
    JOIN kieu_dang kd ON kd.ma_kieu_dang = v.ma_kd
    JOIN co_giay cg ON cg.ma_co_giay = v.ma_cg
    JOIN xuat_xu xx ON xx.ma_xuat_xu = v.ma_xx
    WHERE NOT EXISTS (
        SELECT 1 FROM san_pham x WHERE x.ma_san_pham = v.ma_san_pham
    );

    /* =====================================================
       3. BIẾN THỂ SẢN PHẨM
       Mỗi mã chi tiết/SKU là duy nhất.
       ===================================================== */


    INSERT INTO san_pham_chi_tiet
        (id_san_pham, id_mau_sac, id_kich_thuoc,
         ma_chi_tiet_san_pham, so_luong, gia_ban, sku,
         kich_hoat, ngay_tao, ngay_cap_nhat, trang_thai)
    SELECT
        sp.id, ms.id, kt.id,
        v.ma_ct, v.so_luong, v.gia_ban, v.sku,
        1, SYSDATETIME(), SYSDATETIME(), 1
    FROM (VALUES
        ('SP001','MS001','40','SPCT001',20,2200000,'SKU-SP001-B-40'),
        ('SP001','MS001','41','SPCT002',15,2200000,'SKU-SP001-B-41'),
        ('SP001','MS002','42','SPCT003',12,2250000,'SKU-SP001-T-42'),

        ('SP002','MS002','40','SPCT004',18,2370000,'SKU-SP002-T-40'),
        ('SP002','MS005','41','SPCT005',10,2370000,'SKU-SP002-X-41'),
        ('SP002','MS001','42','SPCT006',8,2400000,'SKU-SP002-B-42'),

        ('SP003','MS001','41','SPCT007',10,2070000,'SKU-SP003-B-41'),
        ('SP003','MS003','42','SPCT008',8,2070000,'SKU-SP003-D-42'),

        ('SP004','MS002','39','SPCT009',14,1760000,'SKU-SP004-T-39'),
        ('SP004','MS001','40','SPCT010',12,1760000,'SKU-SP004-B-40'),

        ('SP005','MS003','41','SPCT011',16,1980000,'SKU-SP005-D-41'),
        ('SP005','MS004','42','SPCT012',10,1980000,'SKU-SP005-X-42'),

        ('SP006','MS004','40','SPCT013',9,2620000,'SKU-SP006-X-40'),
        ('SP006','MS005','41','SPCT014',7,2620000,'SKU-SP006-X-41')
    ) v(ma_sp, ma_ms, gia_tri_size, ma_ct, so_luong, gia_ban, sku)
    JOIN san_pham sp ON sp.ma_san_pham = v.ma_sp
    JOIN mau_sac ms ON ms.ma_mau_sac = v.ma_ms
    JOIN kich_thuoc kt ON kt.gia_tri = v.gia_tri_size
    WHERE NOT EXISTS (
        SELECT 1
        FROM san_pham_chi_tiet x
        WHERE x.ma_chi_tiet_san_pham = v.ma_ct OR x.sku = v.sku
           OR (x.id_san_pham=sp.id AND x.id_mau_sac=ms.id AND x.id_kich_thuoc=kt.id)
    );

    /* =====================================================
       4. HÌNH ẢNH TEST
       Không bắt buộc cho hóa đơn, nhưng để màn sản phẩm có ảnh.
       ===================================================== */



    -- Preserve pre-existing codes/SKUs/stock. Resolve aliases through the real tuple.
    DECLARE @SeedVariantMap TABLE (ma_ct VARCHAR(100) PRIMARY KEY, id BIGINT NOT NULL);
    INSERT INTO @SeedVariantMap (ma_ct,id)
    SELECT v.ma_ct, x.id FROM (VALUES
        ('SP001','MS001','40','SPCT001',20,2200000,'SKU-SP001-B-40'),
        ('SP001','MS001','41','SPCT002',15,2200000,'SKU-SP001-B-41'),
        ('SP001','MS002','42','SPCT003',12,2250000,'SKU-SP001-T-42'),

        ('SP002','MS002','40','SPCT004',18,2370000,'SKU-SP002-T-40'),
        ('SP002','MS005','41','SPCT005',10,2370000,'SKU-SP002-X-41'),
        ('SP002','MS001','42','SPCT006',8,2400000,'SKU-SP002-B-42'),

        ('SP003','MS001','41','SPCT007',10,2070000,'SKU-SP003-B-41'),
        ('SP003','MS003','42','SPCT008',8,2070000,'SKU-SP003-D-42'),

        ('SP004','MS002','39','SPCT009',14,1760000,'SKU-SP004-T-39'),
        ('SP004','MS001','40','SPCT010',12,1760000,'SKU-SP004-B-40'),

        ('SP005','MS003','41','SPCT011',16,1980000,'SKU-SP005-D-41'),
        ('SP005','MS004','42','SPCT012',10,1980000,'SKU-SP005-X-42'),

        ('SP006','MS004','40','SPCT013',9,2620000,'SKU-SP006-X-40'),
        ('SP006','MS005','41','SPCT014',7,2620000,'SKU-SP006-X-41')
    ) v(ma_sp,ma_ms,gia_tri_size,ma_ct,so_luong,gia_ban,sku)
    JOIN san_pham sp ON sp.ma_san_pham=v.ma_sp
    JOIN mau_sac ms ON ms.ma_mau_sac=v.ma_ms
    JOIN kich_thuoc kt ON kt.gia_tri=v.gia_tri_size
    JOIN san_pham_chi_tiet x ON x.id_san_pham=sp.id AND x.id_mau_sac=ms.id AND x.id_kich_thuoc=kt.id;
    IF (SELECT COUNT(*) FROM @SeedVariantMap) <> 14
        THROW 51006, N'Seed variant code/SKU conflicts with another product/color/size. No existing record overwritten.', 1;

    INSERT INTO hinh_anh_san_pham (id_san_pham, url_anh, is_anh_chinh)
    SELECT sp.id, v.url_anh, CASE WHEN EXISTS (SELECT 1 FROM hinh_anh_san_pham old WHERE old.id_san_pham=sp.id AND old.is_anh_chinh=1) THEN 0 ELSE 1 END
    FROM (VALUES
        ('SP001','MS001','https://example.com/san-pham/sp001-den.jpg'),
        ('SP002','MS002','https://example.com/san-pham/sp002-trang.jpg'),
        ('SP003','MS001','https://example.com/san-pham/sp003-den.jpg'),
        ('SP004','MS002','https://example.com/san-pham/sp004-trang.jpg'),
        ('SP005','MS003','https://example.com/san-pham/sp005-do.jpg'),
        ('SP006','MS004','https://example.com/san-pham/sp006-xanh.jpg')
    ) v(ma_sp, ma_ms, url_anh)
    JOIN san_pham sp ON sp.ma_san_pham = v.ma_sp
    WHERE NOT EXISTS (
        SELECT 1
        FROM hinh_anh_san_pham x
        WHERE x.id_san_pham = sp.id
          AND x.url_anh = v.url_anh
    );

    /* =====================================================
       5. CHI TIẾT HÓA ĐƠN
       Không tạo hóa đơn mới.
       Gắn sản phẩm vào 15 hóa đơn HD000067..HD000081 đã có.
       Dùng mã hóa đơn + mã biến thể, không phụ thuộc ID.
       ===================================================== */


-- 02_seed_nhan_vien.sql (code/username/email guarded).


-- =============================================
-- SEED MODULE NHÂN VIÊN
-- =============================================

-- 1. VAI TRÒ QUẢN LÝ
IF NOT EXISTS (
    SELECT 1
    FROM vai_tro
    WHERE ten_vai_tro = N'Quản lý'
)
BEGIN
    INSERT INTO vai_tro (
        ten_vai_tro,
        mo_ta,
        trang_thai
    )
    VALUES (
        N'Quản lý',
        N'Quản lý hệ thống',
        1
    );
END

-- 2. VAI TRÒ NHÂN VIÊN
IF NOT EXISTS (
    SELECT 1
    FROM vai_tro
    WHERE ten_vai_tro = N'Nhân viên'
)
BEGIN
    INSERT INTO vai_tro (
        ten_vai_tro,
        mo_ta,
        trang_thai
    )
    VALUES (
        N'Nhân viên',
        N'Nhân viên cửa hàng',
        1
    );
END

-- 3. NHÂN VIÊN MẪU
DECLARE @idVaiTroNhanVien BIGINT;

SELECT TOP 1
    @idVaiTroNhanVien = id
FROM vai_tro
WHERE ten_vai_tro = N'Nhân viên';


IF NOT EXISTS (
    SELECT 1
    FROM nhan_vien
    WHERE ma_nhan_vien = 'NV0001' OR ten_dang_nhap='NV0001' OR email='an@sportshoe.vn'
)
BEGIN
    INSERT INTO nhan_vien (
        id_vai_tro,
        ma_nhan_vien,
        ten_dang_nhap,
        ten_nhan_vien,
        email,
        mat_khau,
        so_dien_thoai,
        gioi_tinh,
        ngay_sinh,
        dia_chi,
        tinh_thanh,
        phuong_xa,
        trang_thai,
        ngay_tao,
        hinh_anh,
        ngay_cap_nhat
    )
    VALUES (
        @idVaiTroNhanVien,
        'NV0001',
        'NV0001',
        N'Nguyễn Văn An',
        'an@sportshoe.vn',
        NULL,
        '0901000001',
        1,
        '2000-01-15',
        N'Số 10 Nguyễn Trãi',
        N'Hà Nội',
        N'Phường Thanh Xuân',
        1,
        GETDATE(),
        NULL,
        GETDATE()
    );
END

-- 02_seed_khach_hang.sql.


-- =============================================
-- SEED MODULE KHÁCH HÀNG
-- =============================================

-- Dùng mã riêng để không đụng KH0001, KH0002...
-- do API tự sinh.

IF NOT EXISTS (
    SELECT 1
    FROM khach_hang
    WHERE ma_khach_hang = 'KHSEED001' OR ten_tai_khoan='KHSEED001' OR email='khach.seed@smashstep.vn'
)
BEGIN
    INSERT INTO khach_hang (
        ma_khach_hang,
        ten_tai_khoan,
        ten_khach_hang,
        email,
        so_dien_thoai,
        ngay_sinh,
        gioi_tinh,
        mat_khau,
        hinh_anh,
        trang_thai,
        ngay_tao,
        ngay_cap_nhat
    )
    VALUES (
        'KHSEED001',
        'KHSEED001',
        N'Khách hàng mẫu',
        'khach.seed@smashstep.vn',
        '0902000001',
        '2000-01-15',
        1,
        NULL,
        NULL,
        1,
        SYSDATETIME(),
        SYSDATETIME()
    );
END

-- =============================================
-- ĐỊA CHỈ KHÁCH HÀNG
-- =============================================

IF NOT EXISTS (
    SELECT 1
    FROM dia_chi_khach_hang d
    JOIN khach_hang k
        ON k.id = d.id_khach_hang
    WHERE
        k.ma_khach_hang = 'KHSEED001'
        AND d.trang_thai = 1
)
BEGIN
    INSERT INTO dia_chi_khach_hang (
        id_khach_hang,
        ten_nguoi_nhan,
        sdt_nguoi_nhan,
        tinh_thanh,
        quan_huyen,
        phuong_xa,
        dia_chi_cu_the,
        loai_dia_chi,
        is_mac_dinh,
        trang_thai
    )
    SELECT
        id,
        N'Khách hàng mẫu',
        '0902000001',
        N'Hà Nội',
        N'Cầu Giấy',
        N'Phường Dịch Vọng',
        N'Số 1 Trần Thái Tông',
        1,
        1,
        1
    FROM khach_hang
    WHERE ma_khach_hang = 'KHSEED001';
END

-- 02_seed_phieu_giam_gia.sql, without obsolete vo_han DDL.

INSERT INTO phieu_giam_gia (ma_phieu_giam_gia, ten_phieu_giam_gia, hinh_thuc_phieu, loai_giam_gia, gia_tri_giam, gia_tri_toi_thieu, giam_toi_da, ngay_bat_dau, ngay_ket_thuc, so_luong, so_luong_da_dung, trang_thai, ngay_tao, ngay_cap_nhat, mo_ta)
SELECT v.ma_phieu_giam_gia, v.ten_phieu_giam_gia, v.hinh_thuc_phieu, v.loai_giam_gia, v.gia_tri_giam, v.gia_tri_toi_thieu, v.giam_toi_da, v.ngay_bat_dau, v.ngay_ket_thuc, v.so_luong, v.so_luong_da_dung, v.trang_thai, v.ngay_tao, v.ngay_cap_nhat, v.mo_ta
FROM (VALUES (
    'PGG006',
    N'Giảm 10% cho đơn từ 200K',
    1,
    1,
    10.00,
    500000.00,
    100000.00,
    '2026-10-01 00:00:00',
    '2026-12-31 23:59:59',
    100,
    0,
    1,
    GETDATE(),
    GETDATE(),
    N'Giảm 10% tối đa 100.000đ cho đơn hàng từ 200.000đ'
),
(
    'PGG001',
    N'Giảm 10% cho đơn từ 500K',
    1,
    1,
    10.00,
    500000.00,
    100000.00,
    '2026-10-01 00:00:00',
    '2026-12-31 23:59:59',
    100,
    0,
    1,
    GETDATE(),
    GETDATE(),
    N'Giảm 10% tối đa 100.000đ cho đơn hàng từ 500.000đ'
),
(
    'PGG002',
    N'Giảm 50K cho đơn từ 300K',
    1,
    2,
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
    2,
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
)) v(ma_phieu_giam_gia, ten_phieu_giam_gia, hinh_thuc_phieu, loai_giam_gia, gia_tri_giam, gia_tri_toi_thieu, giam_toi_da, ngay_bat_dau, ngay_ket_thuc, so_luong, so_luong_da_dung, trang_thai, ngay_tao, ngay_cap_nhat, mo_ta)
WHERE NOT EXISTS (SELECT 1 FROM phieu_giam_gia p WHERE p.ma_phieu_giam_gia=v.ma_phieu_giam_gia);

    -- Extra runnable public unlimited/private samples; no legacy form guessing.
    INSERT INTO phieu_giam_gia (ma_phieu_giam_gia,ten_phieu_giam_gia,hinh_thuc_phieu,loai_giam_gia,gia_tri_giam,gia_tri_toi_thieu,giam_toi_da,ngay_bat_dau,ngay_ket_thuc,so_luong,so_luong_da_dung,trang_thai,ngay_tao,ngay_cap_nhat,mo_ta)
    SELECT v.ma,v.ten,v.hinh_thuc,2,50000,300000,50000,CONVERT(date,SYSDATETIME()),DATEADD(year,1,CONVERT(date,SYSDATETIME())),v.so_luong,0,1,SYSDATETIME(),SYSDATETIME(),N'Dữ liệu mẫu canonical'
    FROM (VALUES ('PGGSEED_UNLIMITED',N'Voucher công khai không giới hạn',1,CAST(NULL AS INT)),('PGGSEED_PRIVATE',N'Voucher cá nhân mẫu',2,50)) v(ma,ten,hinh_thuc,so_luong)
    WHERE NOT EXISTS (SELECT 1 FROM phieu_giam_gia p WHERE p.ma_phieu_giam_gia=v.ma);
    INSERT INTO phieu_giam_gia_khach_hang (id_khach_hang,id_phieu_giam_gia,ngay_su_dung,trang_thai)
    SELECT k.id,p.id,NULL,1 FROM khach_hang k CROSS JOIN phieu_giam_gia p
    WHERE k.ma_khach_hang='KHSEED001' AND p.ma_phieu_giam_gia IN ('PGG004','PGGSEED_PRIVATE') AND p.hinh_thuc_phieu=2
      AND NOT EXISTS (SELECT 1 FROM phieu_giam_gia_khach_hang x WHERE x.id_khach_hang=k.id AND x.id_phieu_giam_gia=p.id);
    -- Planned, disabled campaign: demonstrates both promotion tables without changing current sale prices.
    INSERT INTO dot_giam_gia (ma_dot_giam_gia,ten_dot_giam_gia,phan_tram_giam_dot,ngay_bat_dau,ngay_ket_thuc,kich_hoat,ngay_tao,ngay_cap_nhat,trang_thai,mo_ta)
    SELECT 'DGGSEED001',N'Đợt giảm giá mẫu',15,DATEADD(month,1,CONVERT(date,SYSDATETIME())),DATEADD(month,2,CONVERT(date,SYSDATETIME())),0,SYSDATETIME(),SYSDATETIME(),1,N'Đợt mẫu chưa kích hoạt'
    WHERE NOT EXISTS (SELECT 1 FROM dot_giam_gia WHERE ma_dot_giam_gia='DGGSEED001');
    INSERT INTO chi_tiet_dot_giam_gia (id_dot_giam_gia,id_san_pham_chi_tiet,phan_tram_giam_bien_the,trang_thai,ngay_tao)
    SELECT d.id,v.id,15,1,SYSDATETIME() FROM dot_giam_gia d CROSS JOIN @SeedVariantMap v
    WHERE d.ma_dot_giam_gia='DGGSEED001' AND v.ma_ct='SPCT014'
      AND NOT EXISTS (SELECT 1 FROM chi_tiet_dot_giam_gia x WHERE x.id_dot_giam_gia=d.id AND x.id_san_pham_chi_tiet=v.id);
    -- 02_seed_hoa_don + 03 detail plan. Totals for NEW invoices use their actual lines.
    -- Existing invoices and their lines/history/payments remain untouched.

IF NOT EXISTS (SELECT 1 FROM hinh_thuc_thanh_toan WHERE ma_hinh_thuc = 'TRUC_TIEP')
    INSERT INTO hinh_thuc_thanh_toan (ma_hinh_thuc, ten_hinh_thuc, trang_thai) VALUES ('TRUC_TIEP', N'Trực tiếp', 1);
IF NOT EXISTS (SELECT 1 FROM hinh_thuc_thanh_toan WHERE ma_hinh_thuc = 'TRUC_TUYEN')
    INSERT INTO hinh_thuc_thanh_toan (ma_hinh_thuc, ten_hinh_thuc, trang_thai) VALUES ('TRUC_TUYEN', N'Trực tuyến', 1);

INSERT INTO phuong_thuc_thanh_toan (id_hinh_thuc_thanh_toan, ma_phuong_thuc, ten_phuong_thuc, trang_thai)
SELECT ht.id, v.ma, v.ten, 1
FROM (VALUES ('TIEN_MAT',     N'Tiền mặt',     'TRUC_TIEP'),
             ('THE',          N'Thẻ',          'TRUC_TIEP'),
             ('CHUYEN_KHOAN', N'Chuyển khoản', 'TRUC_TUYEN'),
             ('COD',          N'COD',          'TRUC_TUYEN')) AS v(ma, ten, ma_hinh_thuc)
JOIN hinh_thuc_thanh_toan ht ON ht.ma_hinh_thuc = v.ma_hinh_thuc
WHERE NOT EXISTS (SELECT 1 FROM phuong_thuc_thanh_toan p WHERE p.ma_phuong_thuc = v.ma);

/* ---------- 2. Nhân viên tạm (mật khẩu để trống) ---------- */
IF NOT EXISTS (SELECT 1 FROM vai_tro WHERE ten_vai_tro = N'Nhân viên')
    INSERT INTO vai_tro (ten_vai_tro, mo_ta, trang_thai) VALUES (N'Nhân viên', N'Nhân viên bán hàng', 1);

INSERT INTO nhan_vien (id_vai_tro, ma_nhan_vien, ten_dang_nhap, ten_nhan_vien, email, so_dien_thoai, trang_thai, ngay_tao, ngay_cap_nhat)
SELECT (SELECT TOP 1 id FROM vai_tro WHERE ten_vai_tro = N'Nhân viên'), v.ma, v.tdn, v.ten, v.email, v.sdt, 1, SYSDATETIME(), SYSDATETIME()
FROM (VALUES
    ('NV001', 'nguyenvana', N'Nguyễn Văn A', 'nguyenvana@smashstep.local', '0900000001'),
    ('NV002', 'khanhha', N'Khánh Hà', 'khanhha@smashstep.local', '0900000002'),
    ('NV003', 'tranhuy', N'Trần Huy', 'tranhuy@smashstep.local', '0900000003')
     ) AS v(ma, tdn, ten, email, sdt)
WHERE NOT EXISTS (SELECT 1 FROM nhan_vien n WHERE n.ma_nhan_vien = v.ma
                                          OR n.ten_dang_nhap = v.tdn
                                          OR n.email = v.email);

/* ---------- 3. Khách hàng tạm ---------- */
INSERT INTO khach_hang (ma_khach_hang, ten_tai_khoan, ten_khach_hang, so_dien_thoai, trang_thai, ngay_tao, ngay_cap_nhat)
SELECT v.ma, LOWER(v.ma), v.ten, v.sdt, 1, SYSDATETIME(), SYSDATETIME()
FROM (VALUES
    ('KH001', N'Trần Minh Bảo Hoàng', '0909899999'),
    ('KH002', N'Nguyễn Thị An', '0911111111'),
    ('KH003', N'Lê Quốc Hưng', '0911111111'),
    ('KH004', N'Phạm Thanh Tú', '0983214567'),
    ('KH005', N'Võ Gia Hân', '0905882114'),
    ('KH006', N'Đặng Hoài Nam', '0934625881'),
    ('KH007', N'Bùi Mỹ Linh', '0972230456'),
    ('KH008', N'Ngô Đức Anh', '0902718663'),
    ('KH009', N'Đỗ Phương Vy', '0968440127'),
    ('KH010', N'Mai Tiến Thành', '0918305902')
     ) AS v(ma, ten, sdt)
WHERE NOT EXISTS (SELECT 1 FROM khach_hang k WHERE k.ma_khach_hang = v.ma OR k.ten_tai_khoan = LOWER(v.ma));

/* ---------- 4. Hóa đơn ----------
   tong_tien  = tiền hàng
   thanh_tien = tong_tien - tien_giam_gia + phi_van_chuyen   (khớp cách FE tính)  */

    DECLARE @SeedInvoiceLines TABLE (ma_hd VARCHAR(50),ma_ct VARCHAR(100),so_luong INT);
    INSERT INTO @SeedInvoiceLines VALUES
        ('HD000081','SPCT001',1),
        ('HD000081','SPCT002',1),

        ('HD000080','SPCT004',1),
        ('HD000080','SPCT005',1),

        ('HD000079','SPCT007',1),
        ('HD000079','SPCT008',1),

        ('HD000078','SPCT009',1),
        ('HD000078','SPCT010',1),

        ('HD000077','SPCT011',1),
        ('HD000077','SPCT012',1),

        ('HD000076','SPCT013',1),
        ('HD000076','SPCT014',1),

        ('HD000075','SPCT001',1),
        ('HD000075','SPCT004',1),

        ('HD000074','SPCT009',1),
        ('HD000074','SPCT006',1),

        ('HD000073','SPCT002',1),
        ('HD000073','SPCT007',1),

        ('HD000072','SPCT003',1),
        ('HD000072','SPCT005',1),

        ('HD000071','SPCT001',1),
        ('HD000071','SPCT010',1),

        ('HD000070','SPCT004',1),
        ('HD000070','SPCT012',1),

        ('HD000069','SPCT008',1),
        ('HD000069','SPCT011',1),

        ('HD000068','SPCT003',1),
        ('HD000068','SPCT009',1),

        ('HD000067','SPCT005',1),
        ('HD000067','SPCT013',1)
    ;

DECLARE @hd TABLE (
    ma VARCHAR(50), ma_kh VARCHAR(50), ma_nv VARCHAR(50), ma_pt VARCHAR(50),
    loai INT, hinh_nhan TINYINT, tong DECIMAL(18,2), giam DECIMAL(18,2), ship DECIMAL(18,2), trang_thai INT,
    nguoi_nhan NVARCHAR(255), sdt VARCHAR(20), dia_chi NVARCHAR(500), ngay DATETIME2
);

INSERT INTO @hd VALUES
    ('HD000081', 'KH001', 'NV002', 'TIEN_MAT', 0, 0, 2200000, 0, 0, 5, N'Trần Minh Bảo Hoàng', '0909899999', N'123 Nguyễn Trãi, Thanh Xuân, Hà Nội', '2026-09-26T09:15:00'),
    ('HD000080', 'KH002', 'NV001', 'CHUYEN_KHOAN', 1, 1, 2370000, 100000, 30000, 5, N'Nguyễn Thị An', '0911111111', N'Hai Bà Trưng, Hà Nội', '2026-09-26T10:40:00'),
    ('HD000079', 'KH003', 'NV001', 'COD', 0, 1, 2070000, 0, 30000, 3, N'Lê Quốc Hưng', '0911111111', N'Cầu Giấy, Hà Nội', '2026-09-25T14:05:00'),
    ('HD000078', 'KH004', 'NV002', 'CHUYEN_KHOAN', 1, 1, 1920000, 100000, 30000, 4, N'Phạm Thanh Tú', '0983214567', N'Nam Từ Liêm, Hà Nội', '2026-09-25T16:20:00'),
    ('HD000077', 'KH005', 'NV003', 'TIEN_MAT', 0, 0, 3420000, 0, 0, 0, N'Võ Gia Hân', '0905882114', N'Hà Nội', '2026-09-24T08:30:00'),
    ('HD000076', 'KH006', 'NV002', 'CHUYEN_KHOAN', 1, 1, 1260000, 0, 30000, 6, N'Đặng Hoài Nam', '0934625881', N'Hà Nội', '2026-09-24T11:10:00'),
    ('HD000075', 'KH007', 'NV003', 'COD', 0, 1, 2620000, 0, 30000, 4, N'Bùi Mỹ Linh', '0972230456', N'Thanh Xuân, Hà Nội', '2026-09-23T15:45:00'),
    ('HD000074', 'KH008', 'NV001', 'THE', 0, 0, 1760000, 0, 0, 5, N'Ngô Đức Anh', '0902718663', N'Hà Nội', '2026-09-23T09:50:00'),
    ('HD000073', 'KH009', 'NV003', 'CHUYEN_KHOAN', 1, 1, 2950000, 0, 30000, 0, N'Đỗ Phương Vy', '0968440127', N'Hà Nội', '2026-09-22T13:25:00'),
    ('HD000072', 'KH010', 'NV002', 'COD', 0, 1, 4120000, 0, 30000, 3, N'Mai Tiến Thành', '0918305902', N'Hoàng Mai, Hà Nội', '2026-09-22T17:00:00'),
    ('HD000071', 'KH001', 'NV001', 'TIEN_MAT', 0, 0, 1590000, 0, 0, 5, N'Trần Minh Bảo Hoàng', '0909899999', N'123 Nguyễn Trãi, Thanh Xuân, Hà Nội', '2026-09-21T10:00:00'),
    ('HD000070', 'KH003', 'NV002', 'CHUYEN_KHOAN', 1, 1, 2890000, 150000, 30000, 5, N'Lê Quốc Hưng', '0911111111', N'Cầu Giấy, Hà Nội', '2026-09-21T15:30:00'),
    ('HD000069', 'KH005', 'NV003', 'COD', 0, 1, 1980000, 0, 30000, 6, N'Võ Gia Hân', '0905882114', N'Hà Nội', '2026-09-20T09:10:00'),
    ('HD000068', 'KH007', 'NV001', 'THE', 0, 0, 3150000, 0, 0, 5, N'Bùi Mỹ Linh', '0972230456', N'Thanh Xuân, Hà Nội', '2026-09-20T12:45:00'),
    ('HD000067', 'KH009', 'NV002', 'CHUYEN_KHOAN', 1, 1, 2240000, 0, 30000, 5, N'Đỗ Phương Vy', '0968440127', N'Hà Nội', '2026-09-19T16:15:00');

-- Chuẩn hóa dữ liệu hóa đơn cũ: chỉ còn 0=Tại quầy, 1=Trực tuyến.
-- Mọi giá trị loại đơn cũ ngoài 0/1 được đưa về Tại quầy + Giao hàng.
UPDATE dbo.hoa_don
SET loai_hoa_don = 0, hinh_thuc_nhan = 1, ngay_cap_nhat = COALESCE(ngay_cap_nhat, SYSDATETIME())
WHERE loai_hoa_don IS NOT NULL AND loai_hoa_don NOT IN (0, 1);

UPDATE dbo.hoa_don
SET hinh_thuc_nhan = CASE WHEN loai_hoa_don = 0 THEN 0 WHEN loai_hoa_don = 1 THEN 1 ELSE 0 END
WHERE hinh_thuc_nhan IS NULL OR hinh_thuc_nhan NOT IN (0, 1);

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_hoa_don_hinh_thuc_nhan')
    ALTER TABLE dbo.hoa_don WITH CHECK ADD CONSTRAINT CK_hoa_don_hinh_thuc_nhan CHECK (hinh_thuc_nhan IN (0, 1));

DECLARE @NewInvoices TABLE (id BIGINT PRIMARY KEY);
INSERT INTO hoa_don (id_khach_hang, id_nhan_vien, id_phuong_thuc_thanh_toan, ma_hoa_don, loai_hoa_don, hinh_thuc_nhan,
                     tong_tien, phi_van_chuyen, tien_giam_gia, thanh_tien,
                     ho_ten_nguoi_nhan, so_dien_thoai_nguoi_nhan, dia_chi_giao_hang,
                     ngay_thanh_toan, ngay_tao, ngay_cap_nhat, trang_thai)
OUTPUT inserted.id INTO @NewInvoices
SELECT kh.id, nv.id, pt.id, h.ma, h.loai, h.hinh_nhan,
       tot.tong, h.ship, h.giam, tot.tong - h.giam + h.ship,
       h.nguoi_nhan, h.sdt, h.dia_chi,
       CASE WHEN h.trang_thai = 5 THEN h.ngay ELSE NULL END, h.ngay, h.ngay, h.trang_thai
FROM @hd h
CROSS APPLY (SELECT SUM(l.so_luong*spct.gia_ban) tong FROM @SeedInvoiceLines l JOIN @SeedVariantMap vm ON vm.ma_ct=l.ma_ct JOIN san_pham_chi_tiet spct ON spct.id=vm.id WHERE l.ma_hd=h.ma) tot
LEFT JOIN khach_hang kh ON kh.ma_khach_hang = h.ma_kh
LEFT JOIN nhan_vien nv ON nv.ma_nhan_vien = h.ma_nv
LEFT JOIN phuong_thuc_thanh_toan pt ON pt.ma_phuong_thuc = h.ma_pt
WHERE NOT EXISTS (SELECT 1 FROM hoa_don x WHERE x.ma_hoa_don = h.ma);

/* ---------- 5. Lịch sử hóa đơn (dùng cho timeline ở ngày 3) ----------
   Mỗi hóa đơn có 1 dòng "Tạo hóa đơn" (trạng thái 0) và, nếu đã đổi trạng thái, 1 dòng trạng thái hiện tại. */
INSERT INTO lich_su_hoa_don (id_hoa_don, nguoi_tao, trang_thai, ghi_chu, ngay_tao)
SELECT hd.id, hd.id_nhan_vien, 0, N'Tạo hóa đơn', hd.ngay_tao
FROM hoa_don hd
WHERE EXISTS (SELECT 1 FROM @NewInvoices ni WHERE ni.id=hd.id)
  AND NOT EXISTS (SELECT 1 FROM lich_su_hoa_don l WHERE l.id_hoa_don = hd.id AND l.trang_thai = 0);

INSERT INTO lich_su_hoa_don (id_hoa_don, nguoi_tao, trang_thai, ghi_chu, ngay_tao)
SELECT hd.id, hd.id_nhan_vien, hd.trang_thai, N'Cập nhật trạng thái', DATEADD(HOUR, 2, hd.ngay_tao)
FROM hoa_don hd
WHERE EXISTS (SELECT 1 FROM @NewInvoices ni WHERE ni.id=hd.id) AND hd.trang_thai <> 0
  AND NOT EXISTS (SELECT 1 FROM lich_su_hoa_don l WHERE l.id_hoa_don = hd.id AND l.trang_thai = hd.trang_thai);

    INSERT INTO hoa_don_chi_tiet
        (id_hoa_don, id_san_pham_chi_tiet, so_luong,
         don_gia, thanh_tien, ghi_chu, trang_thai)
    SELECT
        hd.id,
        spct.id,
        v.so_luong,
        spct.gia_ban,
        v.so_luong * spct.gia_ban,
        NULL,
        1
    FROM @SeedInvoiceLines v
    JOIN hoa_don hd ON hd.ma_hoa_don = v.ma_hd
    JOIN @SeedVariantMap vm ON vm.ma_ct=v.ma_ct
    JOIN san_pham_chi_tiet spct ON spct.id=vm.id
    JOIN @NewInvoices ni ON ni.id=hd.id
    WHERE NOT EXISTS (
        SELECT 1
        FROM hoa_don_chi_tiet x
        WHERE x.id_hoa_don = hd.id
          AND x.id_san_pham_chi_tiet = spct.id
    );


    INSERT INTO lich_su_thanh_toan (id_hoa_don,id_phuong_thuc_thanh_toan,so_tien,ma_giao_dich,thoi_gian,trang_thai,mo_ta)
    SELECT hd.id,hd.id_phuong_thuc_thanh_toan,hd.thanh_tien,CONCAT('SEED-PAY-',hd.ma_hoa_don),hd.ngay_thanh_toan,1,N'Thanh toán hóa đơn mẫu'
    FROM hoa_don hd JOIN @NewInvoices ni ON ni.id=hd.id
    WHERE hd.trang_thai=5 AND hd.id_phuong_thuc_thanh_toan IS NOT NULL
      AND NOT EXISTS (SELECT 1 FROM lich_su_thanh_toan l WHERE l.ma_giao_dich=CONCAT('SEED-PAY-',hd.ma_hoa_don));
    COMMIT TRANSACTION;
    PRINT N'CANONICAL FULL DATABASE: schema and seeds applied successfully. Existing records preserved.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
SELECT name AS canonical_table FROM sys.tables WHERE is_ms_shipped=0 ORDER BY name;
GO

/* ============================================================
   KIỂM TRA NGHIỆP VỤ HÓA ĐƠN
   loai_hoa_don: 0=Tại quầy, 1=Trực tuyến
   hinh_thuc_nhan: 0=Nhận tại quầy, 1=Giao hàng
   Không còn loại đơn thứ 3.
   ============================================================ */
SELECT loai_hoa_don, hinh_thuc_nhan, COUNT(*) AS so_luong
FROM dbo.hoa_don
GROUP BY loai_hoa_don, hinh_thuc_nhan
ORDER BY loai_hoa_don, hinh_thuc_nhan;
GO
