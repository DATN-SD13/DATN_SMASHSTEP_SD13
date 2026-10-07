/* ============================================================================
   SMASHSTEP SD-013 - OVERLAY / MERGE LEN DATABASE CU
   Sinh tu ban snapshot 2026-10-07.

   MUC TIEU:
   - Chay duoc khi database [SmashStep] da ton tai va da co bang.
   - KHONG DROP DATABASE, KHONG DROP TABLE, KHONG DELETE du lieu cu.
   - Bang chua co -> tao moi theo snapshot.
   - Bang da co -> bo sung cot con thieu.
   - Dong co cung [id] -> UPDATE theo snapshot; chua co [id] -> INSERT.
   - Constraint/default/FK chi them khi chua co.
   - KHONG ep row-count = 231 va KHONG reseed identity ve so co dinh.

   LUU Y: Day la script merge schema + du lieu theo ID, khong xoa cac dong cu
   nam ngoai snapshot. Nen backup DB truoc khi chay tren du lieu quan trong.
============================================================================ */

USE [master];
GO
IF DB_ID(N'SmashStep') IS NULL
    CREATE DATABASE [SmashStep] COLLATE SQL_Latin1_General_CP1_CI_AS;
GO
USE [SmashStep];
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;
    PRINT N'1/4 - Dong bo schema...';

    -- TABLE: [dbo].[chat_lieu]
    IF OBJECT_ID(N'[dbo].[chat_lieu]', N'U') IS NULL
    BEGIN
        CREATE TABLE [dbo].[chat_lieu](
        	[id] [bigint] IDENTITY(1,1) NOT NULL,
        	[ma_chat_lieu] [varchar](50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[ten_chat_lieu] [nvarchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[trang_thai] [int] NULL,
         CONSTRAINT [PK__chat_lie__3213E83F8AC70909] PRIMARY KEY CLUSTERED 
        (
        	[id] ASC
        )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        )
    END
    ELSE
    BEGIN
        IF COL_LENGTH(N'dbo.chat_lieu', N'id') IS NULL
            ALTER TABLE [dbo].[chat_lieu] ADD [id] [bigint] IDENTITY(1,1) NOT NULL;
        IF COL_LENGTH(N'dbo.chat_lieu', N'ma_chat_lieu') IS NULL
            ALTER TABLE [dbo].[chat_lieu] ADD [ma_chat_lieu] [varchar](50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.chat_lieu', N'ten_chat_lieu') IS NULL
            ALTER TABLE [dbo].[chat_lieu] ADD [ten_chat_lieu] [nvarchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.chat_lieu', N'trang_thai') IS NULL
            ALTER TABLE [dbo].[chat_lieu] ADD [trang_thai] [int] NULL;
    END;

    -- TABLE: [dbo].[chi_tiet_dot_giam_gia]
    IF OBJECT_ID(N'[dbo].[chi_tiet_dot_giam_gia]', N'U') IS NULL
    BEGIN
        CREATE TABLE [dbo].[chi_tiet_dot_giam_gia](
        	[id] [bigint] IDENTITY(1,1) NOT NULL,
        	[id_dot_giam_gia] [bigint] NOT NULL,
        	[id_san_pham_chi_tiet] [bigint] NOT NULL,
        	[phan_tram_giam_bien_the] [decimal](18, 2) NULL,
        	[trang_thai] [int] NULL,
        	[ngay_tao] [datetime2](7) NULL,
         CONSTRAINT [PK__chi_tiet__3213E83FF4B45D5D] PRIMARY KEY CLUSTERED 
        (
        	[id] ASC
        )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        )
    END
    ELSE
    BEGIN
        IF COL_LENGTH(N'dbo.chi_tiet_dot_giam_gia', N'id') IS NULL
            ALTER TABLE [dbo].[chi_tiet_dot_giam_gia] ADD [id] [bigint] IDENTITY(1,1) NOT NULL;
        IF COL_LENGTH(N'dbo.chi_tiet_dot_giam_gia', N'id_dot_giam_gia') IS NULL
        BEGIN
            IF NOT EXISTS (SELECT 1 FROM [dbo].[chi_tiet_dot_giam_gia])
                ALTER TABLE [dbo].[chi_tiet_dot_giam_gia] ADD [id_dot_giam_gia] [bigint] NOT NULL;
            ELSE
            BEGIN
                ALTER TABLE [dbo].[chi_tiet_dot_giam_gia] ADD [id_dot_giam_gia] [bigint] NULL;
                PRINT N'CANH BAO: dbo.chi_tiet_dot_giam_gia.id_dot_giam_gia duoc them NULL de giu du lieu cu; snapshot goc la NOT NULL.';
            END
        END
        IF COL_LENGTH(N'dbo.chi_tiet_dot_giam_gia', N'id_san_pham_chi_tiet') IS NULL
        BEGIN
            IF NOT EXISTS (SELECT 1 FROM [dbo].[chi_tiet_dot_giam_gia])
                ALTER TABLE [dbo].[chi_tiet_dot_giam_gia] ADD [id_san_pham_chi_tiet] [bigint] NOT NULL;
            ELSE
            BEGIN
                ALTER TABLE [dbo].[chi_tiet_dot_giam_gia] ADD [id_san_pham_chi_tiet] [bigint] NULL;
                PRINT N'CANH BAO: dbo.chi_tiet_dot_giam_gia.id_san_pham_chi_tiet duoc them NULL de giu du lieu cu; snapshot goc la NOT NULL.';
            END
        END
        IF COL_LENGTH(N'dbo.chi_tiet_dot_giam_gia', N'phan_tram_giam_bien_the') IS NULL
            ALTER TABLE [dbo].[chi_tiet_dot_giam_gia] ADD [phan_tram_giam_bien_the] [decimal](18, 2) NULL;
        IF COL_LENGTH(N'dbo.chi_tiet_dot_giam_gia', N'trang_thai') IS NULL
            ALTER TABLE [dbo].[chi_tiet_dot_giam_gia] ADD [trang_thai] [int] NULL;
        IF COL_LENGTH(N'dbo.chi_tiet_dot_giam_gia', N'ngay_tao') IS NULL
            ALTER TABLE [dbo].[chi_tiet_dot_giam_gia] ADD [ngay_tao] [datetime2](7) NULL;
    END;

    -- TABLE: [dbo].[co_giay]
    IF OBJECT_ID(N'[dbo].[co_giay]', N'U') IS NULL
    BEGIN
        CREATE TABLE [dbo].[co_giay](
        	[id] [bigint] IDENTITY(1,1) NOT NULL,
        	[ma_co_giay] [varchar](50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[ten_co_giay] [nvarchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[trang_thai] [int] NULL,
         CONSTRAINT [PK__co_giay__3213E83F4F7CD933] PRIMARY KEY CLUSTERED 
        (
        	[id] ASC
        )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        )
    END
    ELSE
    BEGIN
        IF COL_LENGTH(N'dbo.co_giay', N'id') IS NULL
            ALTER TABLE [dbo].[co_giay] ADD [id] [bigint] IDENTITY(1,1) NOT NULL;
        IF COL_LENGTH(N'dbo.co_giay', N'ma_co_giay') IS NULL
            ALTER TABLE [dbo].[co_giay] ADD [ma_co_giay] [varchar](50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.co_giay', N'ten_co_giay') IS NULL
            ALTER TABLE [dbo].[co_giay] ADD [ten_co_giay] [nvarchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.co_giay', N'trang_thai') IS NULL
            ALTER TABLE [dbo].[co_giay] ADD [trang_thai] [int] NULL;
    END;

    -- TABLE: [dbo].[danh_muc]
    IF OBJECT_ID(N'[dbo].[danh_muc]', N'U') IS NULL
    BEGIN
        CREATE TABLE [dbo].[danh_muc](
        	[id] [bigint] IDENTITY(1,1) NOT NULL,
        	[ma_danh_muc] [varchar](50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[ten_danh_muc] [nvarchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[mo_ta] [nvarchar](1000) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[trang_thai] [int] NULL,
         CONSTRAINT [PK__danh_muc__3213E83FAEBFDA6C] PRIMARY KEY CLUSTERED 
        (
        	[id] ASC
        )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        )
    END
    ELSE
    BEGIN
        IF COL_LENGTH(N'dbo.danh_muc', N'id') IS NULL
            ALTER TABLE [dbo].[danh_muc] ADD [id] [bigint] IDENTITY(1,1) NOT NULL;
        IF COL_LENGTH(N'dbo.danh_muc', N'ma_danh_muc') IS NULL
            ALTER TABLE [dbo].[danh_muc] ADD [ma_danh_muc] [varchar](50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.danh_muc', N'ten_danh_muc') IS NULL
            ALTER TABLE [dbo].[danh_muc] ADD [ten_danh_muc] [nvarchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.danh_muc', N'mo_ta') IS NULL
            ALTER TABLE [dbo].[danh_muc] ADD [mo_ta] [nvarchar](1000) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.danh_muc', N'trang_thai') IS NULL
            ALTER TABLE [dbo].[danh_muc] ADD [trang_thai] [int] NULL;
    END;

    -- TABLE: [dbo].[dia_chi_khach_hang]
    IF OBJECT_ID(N'[dbo].[dia_chi_khach_hang]', N'U') IS NULL
    BEGIN
        CREATE TABLE [dbo].[dia_chi_khach_hang](
        	[id] [bigint] IDENTITY(1,1) NOT NULL,
        	[id_khach_hang] [bigint] NOT NULL,
        	[ten_nguoi_nhan] [nvarchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[sdt_nguoi_nhan] [varchar](20) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[tinh_thanh] [nvarchar](100) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[quan_huyen] [nvarchar](100) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[phuong_xa] [nvarchar](100) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[dia_chi_cu_the] [nvarchar](500) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[loai_dia_chi] [int] NULL,
        	[is_mac_dinh] [bit] NULL,
        	[trang_thai] [int] NULL,
         CONSTRAINT [PK__dia_chi___3213E83F4B0F61C9] PRIMARY KEY CLUSTERED 
        (
        	[id] ASC
        )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        )
    END
    ELSE
    BEGIN
        IF COL_LENGTH(N'dbo.dia_chi_khach_hang', N'id') IS NULL
            ALTER TABLE [dbo].[dia_chi_khach_hang] ADD [id] [bigint] IDENTITY(1,1) NOT NULL;
        IF COL_LENGTH(N'dbo.dia_chi_khach_hang', N'id_khach_hang') IS NULL
        BEGIN
            IF NOT EXISTS (SELECT 1 FROM [dbo].[dia_chi_khach_hang])
                ALTER TABLE [dbo].[dia_chi_khach_hang] ADD [id_khach_hang] [bigint] NOT NULL;
            ELSE
            BEGIN
                ALTER TABLE [dbo].[dia_chi_khach_hang] ADD [id_khach_hang] [bigint] NULL;
                PRINT N'CANH BAO: dbo.dia_chi_khach_hang.id_khach_hang duoc them NULL de giu du lieu cu; snapshot goc la NOT NULL.';
            END
        END
        IF COL_LENGTH(N'dbo.dia_chi_khach_hang', N'ten_nguoi_nhan') IS NULL
            ALTER TABLE [dbo].[dia_chi_khach_hang] ADD [ten_nguoi_nhan] [nvarchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.dia_chi_khach_hang', N'sdt_nguoi_nhan') IS NULL
            ALTER TABLE [dbo].[dia_chi_khach_hang] ADD [sdt_nguoi_nhan] [varchar](20) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.dia_chi_khach_hang', N'tinh_thanh') IS NULL
            ALTER TABLE [dbo].[dia_chi_khach_hang] ADD [tinh_thanh] [nvarchar](100) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.dia_chi_khach_hang', N'quan_huyen') IS NULL
            ALTER TABLE [dbo].[dia_chi_khach_hang] ADD [quan_huyen] [nvarchar](100) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.dia_chi_khach_hang', N'phuong_xa') IS NULL
            ALTER TABLE [dbo].[dia_chi_khach_hang] ADD [phuong_xa] [nvarchar](100) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.dia_chi_khach_hang', N'dia_chi_cu_the') IS NULL
            ALTER TABLE [dbo].[dia_chi_khach_hang] ADD [dia_chi_cu_the] [nvarchar](500) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.dia_chi_khach_hang', N'loai_dia_chi') IS NULL
            ALTER TABLE [dbo].[dia_chi_khach_hang] ADD [loai_dia_chi] [int] NULL;
        IF COL_LENGTH(N'dbo.dia_chi_khach_hang', N'is_mac_dinh') IS NULL
            ALTER TABLE [dbo].[dia_chi_khach_hang] ADD [is_mac_dinh] [bit] NULL;
        IF COL_LENGTH(N'dbo.dia_chi_khach_hang', N'trang_thai') IS NULL
            ALTER TABLE [dbo].[dia_chi_khach_hang] ADD [trang_thai] [int] NULL;
    END;

    -- TABLE: [dbo].[dot_giam_gia]
    IF OBJECT_ID(N'[dbo].[dot_giam_gia]', N'U') IS NULL
    BEGIN
        CREATE TABLE [dbo].[dot_giam_gia](
        	[id] [bigint] IDENTITY(1,1) NOT NULL,
        	[ma_dot_giam_gia] [varchar](50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[ten_dot_giam_gia] [nvarchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[phan_tram_giam_dot] [decimal](18, 2) NULL,
        	[ngay_bat_dau] [datetime2](7) NULL,
        	[ngay_ket_thuc] [datetime2](7) NULL,
        	[kich_hoat] [bit] NULL,
        	[ngay_tao] [datetime2](7) NULL,
        	[ngay_cap_nhat] [datetime2](7) NULL,
        	[trang_thai] [int] NULL,
         CONSTRAINT [PK__dot_giam__3213E83F43B6845D] PRIMARY KEY CLUSTERED 
        (
        	[id] ASC
        )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        )
    END
    ELSE
    BEGIN
        IF COL_LENGTH(N'dbo.dot_giam_gia', N'id') IS NULL
            ALTER TABLE [dbo].[dot_giam_gia] ADD [id] [bigint] IDENTITY(1,1) NOT NULL;
        IF COL_LENGTH(N'dbo.dot_giam_gia', N'ma_dot_giam_gia') IS NULL
            ALTER TABLE [dbo].[dot_giam_gia] ADD [ma_dot_giam_gia] [varchar](50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.dot_giam_gia', N'ten_dot_giam_gia') IS NULL
            ALTER TABLE [dbo].[dot_giam_gia] ADD [ten_dot_giam_gia] [nvarchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.dot_giam_gia', N'phan_tram_giam_dot') IS NULL
            ALTER TABLE [dbo].[dot_giam_gia] ADD [phan_tram_giam_dot] [decimal](18, 2) NULL;
        IF COL_LENGTH(N'dbo.dot_giam_gia', N'ngay_bat_dau') IS NULL
            ALTER TABLE [dbo].[dot_giam_gia] ADD [ngay_bat_dau] [datetime2](7) NULL;
        IF COL_LENGTH(N'dbo.dot_giam_gia', N'ngay_ket_thuc') IS NULL
            ALTER TABLE [dbo].[dot_giam_gia] ADD [ngay_ket_thuc] [datetime2](7) NULL;
        IF COL_LENGTH(N'dbo.dot_giam_gia', N'kich_hoat') IS NULL
            ALTER TABLE [dbo].[dot_giam_gia] ADD [kich_hoat] [bit] NULL;
        IF COL_LENGTH(N'dbo.dot_giam_gia', N'ngay_tao') IS NULL
            ALTER TABLE [dbo].[dot_giam_gia] ADD [ngay_tao] [datetime2](7) NULL;
        IF COL_LENGTH(N'dbo.dot_giam_gia', N'ngay_cap_nhat') IS NULL
            ALTER TABLE [dbo].[dot_giam_gia] ADD [ngay_cap_nhat] [datetime2](7) NULL;
        IF COL_LENGTH(N'dbo.dot_giam_gia', N'trang_thai') IS NULL
            ALTER TABLE [dbo].[dot_giam_gia] ADD [trang_thai] [int] NULL;
    END;

    -- TABLE: [dbo].[hinh_anh_san_pham]
    IF OBJECT_ID(N'[dbo].[hinh_anh_san_pham]', N'U') IS NULL
    BEGIN
        CREATE TABLE [dbo].[hinh_anh_san_pham](
        	[id] [bigint] IDENTITY(1,1) NOT NULL,
        	[id_san_pham] [bigint] NOT NULL,
        	[url_anh] [nvarchar](1000) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[is_anh_chinh] [bit] NULL,
         CONSTRAINT [PK__hinh_anh__3213E83F9E24F932] PRIMARY KEY CLUSTERED 
        (
        	[id] ASC
        )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        )
    END
    ELSE
    BEGIN
        IF COL_LENGTH(N'dbo.hinh_anh_san_pham', N'id') IS NULL
            ALTER TABLE [dbo].[hinh_anh_san_pham] ADD [id] [bigint] IDENTITY(1,1) NOT NULL;
        IF COL_LENGTH(N'dbo.hinh_anh_san_pham', N'id_san_pham') IS NULL
        BEGIN
            IF NOT EXISTS (SELECT 1 FROM [dbo].[hinh_anh_san_pham])
                ALTER TABLE [dbo].[hinh_anh_san_pham] ADD [id_san_pham] [bigint] NOT NULL;
            ELSE
            BEGIN
                ALTER TABLE [dbo].[hinh_anh_san_pham] ADD [id_san_pham] [bigint] NULL;
                PRINT N'CANH BAO: dbo.hinh_anh_san_pham.id_san_pham duoc them NULL de giu du lieu cu; snapshot goc la NOT NULL.';
            END
        END
        IF COL_LENGTH(N'dbo.hinh_anh_san_pham', N'url_anh') IS NULL
            ALTER TABLE [dbo].[hinh_anh_san_pham] ADD [url_anh] [nvarchar](1000) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.hinh_anh_san_pham', N'is_anh_chinh') IS NULL
            ALTER TABLE [dbo].[hinh_anh_san_pham] ADD [is_anh_chinh] [bit] NULL;
    END;

    -- TABLE: [dbo].[hinh_thuc_thanh_toan]
    IF OBJECT_ID(N'[dbo].[hinh_thuc_thanh_toan]', N'U') IS NULL
    BEGIN
        CREATE TABLE [dbo].[hinh_thuc_thanh_toan](
        	[id] [bigint] IDENTITY(1,1) NOT NULL,
        	[ma_hinh_thuc] [varchar](50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[ten_hinh_thuc] [nvarchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[trang_thai] [int] NULL,
         CONSTRAINT [PK__hinh_thu__3213E83F39CF73E3] PRIMARY KEY CLUSTERED 
        (
        	[id] ASC
        )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        )
    END
    ELSE
    BEGIN
        IF COL_LENGTH(N'dbo.hinh_thuc_thanh_toan', N'id') IS NULL
            ALTER TABLE [dbo].[hinh_thuc_thanh_toan] ADD [id] [bigint] IDENTITY(1,1) NOT NULL;
        IF COL_LENGTH(N'dbo.hinh_thuc_thanh_toan', N'ma_hinh_thuc') IS NULL
            ALTER TABLE [dbo].[hinh_thuc_thanh_toan] ADD [ma_hinh_thuc] [varchar](50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.hinh_thuc_thanh_toan', N'ten_hinh_thuc') IS NULL
            ALTER TABLE [dbo].[hinh_thuc_thanh_toan] ADD [ten_hinh_thuc] [nvarchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.hinh_thuc_thanh_toan', N'trang_thai') IS NULL
            ALTER TABLE [dbo].[hinh_thuc_thanh_toan] ADD [trang_thai] [int] NULL;
    END;

    -- TABLE: [dbo].[hoa_don]
    IF OBJECT_ID(N'[dbo].[hoa_don]', N'U') IS NULL
    BEGIN
        CREATE TABLE [dbo].[hoa_don](
        	[id] [bigint] IDENTITY(1,1) NOT NULL,
        	[id_khach_hang] [bigint] NULL,
        	[id_nhan_vien] [bigint] NULL,
        	[id_phieu_giam_gia] [bigint] NULL,
        	[id_phuong_thuc_thanh_toan] [bigint] NULL,
        	[ma_hoa_don] [varchar](50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[loai_hoa_don] [int] NULL,
        	[tong_tien] [decimal](18, 2) NULL,
        	[phi_van_chuyen] [decimal](18, 2) NULL,
        	[thanh_tien] [decimal](18, 2) NULL,
        	[don_vi_van_chuyen] [nvarchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[ho_ten_nguoi_nhan] [nvarchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[so_dien_thoai_nguoi_nhan] [varchar](20) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[gia_chi_giao_hang] [decimal](18, 2) NULL,
        	[ghi_chu] [nvarchar](1000) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[ngay_thanh_toan] [datetime2](7) NULL,
        	[ngay_tao] [datetime2](7) NULL,
        	[ngay_cap_nhat] [datetime2](7) NULL,
        	[trang_thai] [int] NULL,
         CONSTRAINT [PK__hoa_don__3213E83F2B8A9779] PRIMARY KEY CLUSTERED 
        (
        	[id] ASC
        )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        )
    END
    ELSE
    BEGIN
        IF COL_LENGTH(N'dbo.hoa_don', N'id') IS NULL
            ALTER TABLE [dbo].[hoa_don] ADD [id] [bigint] IDENTITY(1,1) NOT NULL;
        IF COL_LENGTH(N'dbo.hoa_don', N'id_khach_hang') IS NULL
            ALTER TABLE [dbo].[hoa_don] ADD [id_khach_hang] [bigint] NULL;
        IF COL_LENGTH(N'dbo.hoa_don', N'id_nhan_vien') IS NULL
            ALTER TABLE [dbo].[hoa_don] ADD [id_nhan_vien] [bigint] NULL;
        IF COL_LENGTH(N'dbo.hoa_don', N'id_phieu_giam_gia') IS NULL
            ALTER TABLE [dbo].[hoa_don] ADD [id_phieu_giam_gia] [bigint] NULL;
        IF COL_LENGTH(N'dbo.hoa_don', N'id_phuong_thuc_thanh_toan') IS NULL
            ALTER TABLE [dbo].[hoa_don] ADD [id_phuong_thuc_thanh_toan] [bigint] NULL;
        IF COL_LENGTH(N'dbo.hoa_don', N'ma_hoa_don') IS NULL
            ALTER TABLE [dbo].[hoa_don] ADD [ma_hoa_don] [varchar](50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.hoa_don', N'loai_hoa_don') IS NULL
            ALTER TABLE [dbo].[hoa_don] ADD [loai_hoa_don] [int] NULL;
        IF COL_LENGTH(N'dbo.hoa_don', N'tong_tien') IS NULL
            ALTER TABLE [dbo].[hoa_don] ADD [tong_tien] [decimal](18, 2) NULL;
        IF COL_LENGTH(N'dbo.hoa_don', N'phi_van_chuyen') IS NULL
            ALTER TABLE [dbo].[hoa_don] ADD [phi_van_chuyen] [decimal](18, 2) NULL;
        IF COL_LENGTH(N'dbo.hoa_don', N'thanh_tien') IS NULL
            ALTER TABLE [dbo].[hoa_don] ADD [thanh_tien] [decimal](18, 2) NULL;
        IF COL_LENGTH(N'dbo.hoa_don', N'don_vi_van_chuyen') IS NULL
            ALTER TABLE [dbo].[hoa_don] ADD [don_vi_van_chuyen] [nvarchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.hoa_don', N'ho_ten_nguoi_nhan') IS NULL
            ALTER TABLE [dbo].[hoa_don] ADD [ho_ten_nguoi_nhan] [nvarchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.hoa_don', N'so_dien_thoai_nguoi_nhan') IS NULL
            ALTER TABLE [dbo].[hoa_don] ADD [so_dien_thoai_nguoi_nhan] [varchar](20) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.hoa_don', N'gia_chi_giao_hang') IS NULL
            ALTER TABLE [dbo].[hoa_don] ADD [gia_chi_giao_hang] [decimal](18, 2) NULL;
        IF COL_LENGTH(N'dbo.hoa_don', N'ghi_chu') IS NULL
            ALTER TABLE [dbo].[hoa_don] ADD [ghi_chu] [nvarchar](1000) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.hoa_don', N'ngay_thanh_toan') IS NULL
            ALTER TABLE [dbo].[hoa_don] ADD [ngay_thanh_toan] [datetime2](7) NULL;
        IF COL_LENGTH(N'dbo.hoa_don', N'ngay_tao') IS NULL
            ALTER TABLE [dbo].[hoa_don] ADD [ngay_tao] [datetime2](7) NULL;
        IF COL_LENGTH(N'dbo.hoa_don', N'ngay_cap_nhat') IS NULL
            ALTER TABLE [dbo].[hoa_don] ADD [ngay_cap_nhat] [datetime2](7) NULL;
        IF COL_LENGTH(N'dbo.hoa_don', N'trang_thai') IS NULL
            ALTER TABLE [dbo].[hoa_don] ADD [trang_thai] [int] NULL;
    END;

    -- TABLE: [dbo].[hoa_don_chi_tiet]
    IF OBJECT_ID(N'[dbo].[hoa_don_chi_tiet]', N'U') IS NULL
    BEGIN
        CREATE TABLE [dbo].[hoa_don_chi_tiet](
        	[id] [bigint] IDENTITY(1,1) NOT NULL,
        	[id_hoa_don] [bigint] NOT NULL,
        	[id_san_pham_chi_tiet] [bigint] NOT NULL,
        	[so_luong] [int] NULL,
        	[don_gia] [decimal](18, 2) NULL,
        	[thanh_tien] [decimal](18, 2) NULL,
        	[ghi_chu] [nvarchar](1000) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[trang_thai] [int] NULL,
         CONSTRAINT [PK__hoa_don___3213E83F8CC208E6] PRIMARY KEY CLUSTERED 
        (
        	[id] ASC
        )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        )
    END
    ELSE
    BEGIN
        IF COL_LENGTH(N'dbo.hoa_don_chi_tiet', N'id') IS NULL
            ALTER TABLE [dbo].[hoa_don_chi_tiet] ADD [id] [bigint] IDENTITY(1,1) NOT NULL;
        IF COL_LENGTH(N'dbo.hoa_don_chi_tiet', N'id_hoa_don') IS NULL
        BEGIN
            IF NOT EXISTS (SELECT 1 FROM [dbo].[hoa_don_chi_tiet])
                ALTER TABLE [dbo].[hoa_don_chi_tiet] ADD [id_hoa_don] [bigint] NOT NULL;
            ELSE
            BEGIN
                ALTER TABLE [dbo].[hoa_don_chi_tiet] ADD [id_hoa_don] [bigint] NULL;
                PRINT N'CANH BAO: dbo.hoa_don_chi_tiet.id_hoa_don duoc them NULL de giu du lieu cu; snapshot goc la NOT NULL.';
            END
        END
        IF COL_LENGTH(N'dbo.hoa_don_chi_tiet', N'id_san_pham_chi_tiet') IS NULL
        BEGIN
            IF NOT EXISTS (SELECT 1 FROM [dbo].[hoa_don_chi_tiet])
                ALTER TABLE [dbo].[hoa_don_chi_tiet] ADD [id_san_pham_chi_tiet] [bigint] NOT NULL;
            ELSE
            BEGIN
                ALTER TABLE [dbo].[hoa_don_chi_tiet] ADD [id_san_pham_chi_tiet] [bigint] NULL;
                PRINT N'CANH BAO: dbo.hoa_don_chi_tiet.id_san_pham_chi_tiet duoc them NULL de giu du lieu cu; snapshot goc la NOT NULL.';
            END
        END
        IF COL_LENGTH(N'dbo.hoa_don_chi_tiet', N'so_luong') IS NULL
            ALTER TABLE [dbo].[hoa_don_chi_tiet] ADD [so_luong] [int] NULL;
        IF COL_LENGTH(N'dbo.hoa_don_chi_tiet', N'don_gia') IS NULL
            ALTER TABLE [dbo].[hoa_don_chi_tiet] ADD [don_gia] [decimal](18, 2) NULL;
        IF COL_LENGTH(N'dbo.hoa_don_chi_tiet', N'thanh_tien') IS NULL
            ALTER TABLE [dbo].[hoa_don_chi_tiet] ADD [thanh_tien] [decimal](18, 2) NULL;
        IF COL_LENGTH(N'dbo.hoa_don_chi_tiet', N'ghi_chu') IS NULL
            ALTER TABLE [dbo].[hoa_don_chi_tiet] ADD [ghi_chu] [nvarchar](1000) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.hoa_don_chi_tiet', N'trang_thai') IS NULL
            ALTER TABLE [dbo].[hoa_don_chi_tiet] ADD [trang_thai] [int] NULL;
    END;

    -- TABLE: [dbo].[khach_hang]
    IF OBJECT_ID(N'[dbo].[khach_hang]', N'U') IS NULL
    BEGIN
        CREATE TABLE [dbo].[khach_hang](
        	[id] [bigint] IDENTITY(1,1) NOT NULL,
        	[ma_khach_hang] [varchar](50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[ten_tai_khoan] [varchar](100) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[ten_khach_hang] [nvarchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[email] [varchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[so_dien_thoai] [varchar](20) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[ngay_sinh] [date] NULL,
        	[gioi_tinh] [int] NULL,
        	[mat_khau] [varchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[hinh_anh] [nvarchar](1000) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[trang_thai] [int] NULL,
        	[ngay_tao] [datetime2](7) NULL,
        	[ngay_cap_nhat] [datetime2](7) NULL,
         CONSTRAINT [PK__khach_ha__3213E83F00FA8BDF] PRIMARY KEY CLUSTERED 
        (
        	[id] ASC
        )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        )
    END
    ELSE
    BEGIN
        IF COL_LENGTH(N'dbo.khach_hang', N'id') IS NULL
            ALTER TABLE [dbo].[khach_hang] ADD [id] [bigint] IDENTITY(1,1) NOT NULL;
        IF COL_LENGTH(N'dbo.khach_hang', N'ma_khach_hang') IS NULL
            ALTER TABLE [dbo].[khach_hang] ADD [ma_khach_hang] [varchar](50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.khach_hang', N'ten_tai_khoan') IS NULL
            ALTER TABLE [dbo].[khach_hang] ADD [ten_tai_khoan] [varchar](100) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.khach_hang', N'ten_khach_hang') IS NULL
            ALTER TABLE [dbo].[khach_hang] ADD [ten_khach_hang] [nvarchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.khach_hang', N'email') IS NULL
            ALTER TABLE [dbo].[khach_hang] ADD [email] [varchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.khach_hang', N'so_dien_thoai') IS NULL
            ALTER TABLE [dbo].[khach_hang] ADD [so_dien_thoai] [varchar](20) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.khach_hang', N'ngay_sinh') IS NULL
            ALTER TABLE [dbo].[khach_hang] ADD [ngay_sinh] [date] NULL;
        IF COL_LENGTH(N'dbo.khach_hang', N'gioi_tinh') IS NULL
            ALTER TABLE [dbo].[khach_hang] ADD [gioi_tinh] [int] NULL;
        IF COL_LENGTH(N'dbo.khach_hang', N'mat_khau') IS NULL
            ALTER TABLE [dbo].[khach_hang] ADD [mat_khau] [varchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.khach_hang', N'hinh_anh') IS NULL
            ALTER TABLE [dbo].[khach_hang] ADD [hinh_anh] [nvarchar](1000) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.khach_hang', N'trang_thai') IS NULL
            ALTER TABLE [dbo].[khach_hang] ADD [trang_thai] [int] NULL;
        IF COL_LENGTH(N'dbo.khach_hang', N'ngay_tao') IS NULL
            ALTER TABLE [dbo].[khach_hang] ADD [ngay_tao] [datetime2](7) NULL;
        IF COL_LENGTH(N'dbo.khach_hang', N'ngay_cap_nhat') IS NULL
            ALTER TABLE [dbo].[khach_hang] ADD [ngay_cap_nhat] [datetime2](7) NULL;
    END;

    -- TABLE: [dbo].[kich_thuoc]
    IF OBJECT_ID(N'[dbo].[kich_thuoc]', N'U') IS NULL
    BEGIN
        CREATE TABLE [dbo].[kich_thuoc](
        	[id] [bigint] IDENTITY(1,1) NOT NULL,
        	[gia_tri] [varchar](50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[ghi_chu] [nvarchar](1000) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[trang_thai] [int] NULL,
        	[ngay_tao] [datetime2](7) NULL,
        	[ngay_cap_nhat] [datetime2](7) NULL,
         CONSTRAINT [PK__kich_thu__3213E83FBFA0B1A2] PRIMARY KEY CLUSTERED 
        (
        	[id] ASC
        )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        )
    END
    ELSE
    BEGIN
        IF COL_LENGTH(N'dbo.kich_thuoc', N'id') IS NULL
            ALTER TABLE [dbo].[kich_thuoc] ADD [id] [bigint] IDENTITY(1,1) NOT NULL;
        IF COL_LENGTH(N'dbo.kich_thuoc', N'gia_tri') IS NULL
            ALTER TABLE [dbo].[kich_thuoc] ADD [gia_tri] [varchar](50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.kich_thuoc', N'ghi_chu') IS NULL
            ALTER TABLE [dbo].[kich_thuoc] ADD [ghi_chu] [nvarchar](1000) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.kich_thuoc', N'trang_thai') IS NULL
            ALTER TABLE [dbo].[kich_thuoc] ADD [trang_thai] [int] NULL;
        IF COL_LENGTH(N'dbo.kich_thuoc', N'ngay_tao') IS NULL
            ALTER TABLE [dbo].[kich_thuoc] ADD [ngay_tao] [datetime2](7) NULL;
        IF COL_LENGTH(N'dbo.kich_thuoc', N'ngay_cap_nhat') IS NULL
            ALTER TABLE [dbo].[kich_thuoc] ADD [ngay_cap_nhat] [datetime2](7) NULL;
    END;

    -- TABLE: [dbo].[kieu_dang]
    IF OBJECT_ID(N'[dbo].[kieu_dang]', N'U') IS NULL
    BEGIN
        CREATE TABLE [dbo].[kieu_dang](
        	[id] [bigint] IDENTITY(1,1) NOT NULL,
        	[ma_kieu_dang] [varchar](50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[ten_kieu_dang] [nvarchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[trang_thai] [int] NULL,
         CONSTRAINT [PK__kieu_dan__3213E83F55E71C72] PRIMARY KEY CLUSTERED 
        (
        	[id] ASC
        )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        )
    END
    ELSE
    BEGIN
        IF COL_LENGTH(N'dbo.kieu_dang', N'id') IS NULL
            ALTER TABLE [dbo].[kieu_dang] ADD [id] [bigint] IDENTITY(1,1) NOT NULL;
        IF COL_LENGTH(N'dbo.kieu_dang', N'ma_kieu_dang') IS NULL
            ALTER TABLE [dbo].[kieu_dang] ADD [ma_kieu_dang] [varchar](50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.kieu_dang', N'ten_kieu_dang') IS NULL
            ALTER TABLE [dbo].[kieu_dang] ADD [ten_kieu_dang] [nvarchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.kieu_dang', N'trang_thai') IS NULL
            ALTER TABLE [dbo].[kieu_dang] ADD [trang_thai] [int] NULL;
    END;

    -- TABLE: [dbo].[lich_su_hoa_don]
    IF OBJECT_ID(N'[dbo].[lich_su_hoa_don]', N'U') IS NULL
    BEGIN
        CREATE TABLE [dbo].[lich_su_hoa_don](
        	[id] [bigint] IDENTITY(1,1) NOT NULL,
        	[id_hoa_don] [bigint] NOT NULL,
        	[nguoi_tao] [bigint] NULL,
        	[trang_thai] [int] NULL,
        	[ghi_chu] [nvarchar](1000) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[ngay_tao] [datetime2](7) NULL,
         CONSTRAINT [PK__lich_su___3213E83FCE35FBA1] PRIMARY KEY CLUSTERED 
        (
        	[id] ASC
        )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        )
    END
    ELSE
    BEGIN
        IF COL_LENGTH(N'dbo.lich_su_hoa_don', N'id') IS NULL
            ALTER TABLE [dbo].[lich_su_hoa_don] ADD [id] [bigint] IDENTITY(1,1) NOT NULL;
        IF COL_LENGTH(N'dbo.lich_su_hoa_don', N'id_hoa_don') IS NULL
        BEGIN
            IF NOT EXISTS (SELECT 1 FROM [dbo].[lich_su_hoa_don])
                ALTER TABLE [dbo].[lich_su_hoa_don] ADD [id_hoa_don] [bigint] NOT NULL;
            ELSE
            BEGIN
                ALTER TABLE [dbo].[lich_su_hoa_don] ADD [id_hoa_don] [bigint] NULL;
                PRINT N'CANH BAO: dbo.lich_su_hoa_don.id_hoa_don duoc them NULL de giu du lieu cu; snapshot goc la NOT NULL.';
            END
        END
        IF COL_LENGTH(N'dbo.lich_su_hoa_don', N'nguoi_tao') IS NULL
            ALTER TABLE [dbo].[lich_su_hoa_don] ADD [nguoi_tao] [bigint] NULL;
        IF COL_LENGTH(N'dbo.lich_su_hoa_don', N'trang_thai') IS NULL
            ALTER TABLE [dbo].[lich_su_hoa_don] ADD [trang_thai] [int] NULL;
        IF COL_LENGTH(N'dbo.lich_su_hoa_don', N'ghi_chu') IS NULL
            ALTER TABLE [dbo].[lich_su_hoa_don] ADD [ghi_chu] [nvarchar](1000) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.lich_su_hoa_don', N'ngay_tao') IS NULL
            ALTER TABLE [dbo].[lich_su_hoa_don] ADD [ngay_tao] [datetime2](7) NULL;
    END;

    -- TABLE: [dbo].[lich_su_thanh_toan]
    IF OBJECT_ID(N'[dbo].[lich_su_thanh_toan]', N'U') IS NULL
    BEGIN
        CREATE TABLE [dbo].[lich_su_thanh_toan](
        	[id] [bigint] IDENTITY(1,1) NOT NULL,
        	[id_hoa_don] [bigint] NOT NULL,
        	[so_tien] [decimal](18, 2) NULL,
        	[ma_giao_dich] [varchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[thoi_gian] [datetime2](7) NULL,
        	[trang_thai] [int] NULL,
        	[mo_ta] [nvarchar](1000) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
         CONSTRAINT [PK__lich_su___3213E83FCB23DB98] PRIMARY KEY CLUSTERED 
        (
        	[id] ASC
        )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        )
    END
    ELSE
    BEGIN
        IF COL_LENGTH(N'dbo.lich_su_thanh_toan', N'id') IS NULL
            ALTER TABLE [dbo].[lich_su_thanh_toan] ADD [id] [bigint] IDENTITY(1,1) NOT NULL;
        IF COL_LENGTH(N'dbo.lich_su_thanh_toan', N'id_hoa_don') IS NULL
        BEGIN
            IF NOT EXISTS (SELECT 1 FROM [dbo].[lich_su_thanh_toan])
                ALTER TABLE [dbo].[lich_su_thanh_toan] ADD [id_hoa_don] [bigint] NOT NULL;
            ELSE
            BEGIN
                ALTER TABLE [dbo].[lich_su_thanh_toan] ADD [id_hoa_don] [bigint] NULL;
                PRINT N'CANH BAO: dbo.lich_su_thanh_toan.id_hoa_don duoc them NULL de giu du lieu cu; snapshot goc la NOT NULL.';
            END
        END
        IF COL_LENGTH(N'dbo.lich_su_thanh_toan', N'so_tien') IS NULL
            ALTER TABLE [dbo].[lich_su_thanh_toan] ADD [so_tien] [decimal](18, 2) NULL;
        IF COL_LENGTH(N'dbo.lich_su_thanh_toan', N'ma_giao_dich') IS NULL
            ALTER TABLE [dbo].[lich_su_thanh_toan] ADD [ma_giao_dich] [varchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.lich_su_thanh_toan', N'thoi_gian') IS NULL
            ALTER TABLE [dbo].[lich_su_thanh_toan] ADD [thoi_gian] [datetime2](7) NULL;
        IF COL_LENGTH(N'dbo.lich_su_thanh_toan', N'trang_thai') IS NULL
            ALTER TABLE [dbo].[lich_su_thanh_toan] ADD [trang_thai] [int] NULL;
        IF COL_LENGTH(N'dbo.lich_su_thanh_toan', N'mo_ta') IS NULL
            ALTER TABLE [dbo].[lich_su_thanh_toan] ADD [mo_ta] [nvarchar](1000) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
    END;

    -- TABLE: [dbo].[mau_sac]
    IF OBJECT_ID(N'[dbo].[mau_sac]', N'U') IS NULL
    BEGIN
        CREATE TABLE [dbo].[mau_sac](
        	[id] [bigint] IDENTITY(1,1) NOT NULL,
        	[ma_mau_sac] [varchar](50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[ten_mau_sac] [nvarchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[ma_mau_hex] [varchar](20) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[trang_thai] [int] NULL,
        	[ngay_tao] [datetime2](7) NULL,
        	[ngay_cap_nhat] [datetime2](7) NULL,
         CONSTRAINT [PK__mau_sac__3213E83F49DE3C2B] PRIMARY KEY CLUSTERED 
        (
        	[id] ASC
        )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        )
    END
    ELSE
    BEGIN
        IF COL_LENGTH(N'dbo.mau_sac', N'id') IS NULL
            ALTER TABLE [dbo].[mau_sac] ADD [id] [bigint] IDENTITY(1,1) NOT NULL;
        IF COL_LENGTH(N'dbo.mau_sac', N'ma_mau_sac') IS NULL
            ALTER TABLE [dbo].[mau_sac] ADD [ma_mau_sac] [varchar](50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.mau_sac', N'ten_mau_sac') IS NULL
            ALTER TABLE [dbo].[mau_sac] ADD [ten_mau_sac] [nvarchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.mau_sac', N'ma_mau_hex') IS NULL
            ALTER TABLE [dbo].[mau_sac] ADD [ma_mau_hex] [varchar](20) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.mau_sac', N'trang_thai') IS NULL
            ALTER TABLE [dbo].[mau_sac] ADD [trang_thai] [int] NULL;
        IF COL_LENGTH(N'dbo.mau_sac', N'ngay_tao') IS NULL
            ALTER TABLE [dbo].[mau_sac] ADD [ngay_tao] [datetime2](7) NULL;
        IF COL_LENGTH(N'dbo.mau_sac', N'ngay_cap_nhat') IS NULL
            ALTER TABLE [dbo].[mau_sac] ADD [ngay_cap_nhat] [datetime2](7) NULL;
    END;

    -- TABLE: [dbo].[nhan_vien]
    IF OBJECT_ID(N'[dbo].[nhan_vien]', N'U') IS NULL
    BEGIN
        CREATE TABLE [dbo].[nhan_vien](
        	[id] [bigint] IDENTITY(1,1) NOT NULL,
        	[id_vai_tro] [bigint] NULL,
        	[ma_nhan_vien] [varchar](50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[ten_dang_nhap] [varchar](100) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[ten_nhan_vien] [nvarchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[email] [varchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[mat_khau] [varchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[so_dien_thoai] [varchar](20) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[gioi_tinh] [int] NULL,
        	[ngay_sinh] [date] NULL,
        	[dia_chi] [nvarchar](500) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[tinh_thanh] [nvarchar](100) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[phuong_xa] [nvarchar](100) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[trang_thai] [int] NULL,
        	[ngay_tao] [datetime2](7) NULL,
        	[hinh_anh] [nvarchar](1000) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[ngay_cap_nhat] [datetime2](7) NULL,
         CONSTRAINT [PK__nhan_vie__3213E83F06D78DAA] PRIMARY KEY CLUSTERED 
        (
        	[id] ASC
        )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        )
    END
    ELSE
    BEGIN
        IF COL_LENGTH(N'dbo.nhan_vien', N'id') IS NULL
            ALTER TABLE [dbo].[nhan_vien] ADD [id] [bigint] IDENTITY(1,1) NOT NULL;
        IF COL_LENGTH(N'dbo.nhan_vien', N'id_vai_tro') IS NULL
            ALTER TABLE [dbo].[nhan_vien] ADD [id_vai_tro] [bigint] NULL;
        IF COL_LENGTH(N'dbo.nhan_vien', N'ma_nhan_vien') IS NULL
            ALTER TABLE [dbo].[nhan_vien] ADD [ma_nhan_vien] [varchar](50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.nhan_vien', N'ten_dang_nhap') IS NULL
            ALTER TABLE [dbo].[nhan_vien] ADD [ten_dang_nhap] [varchar](100) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.nhan_vien', N'ten_nhan_vien') IS NULL
            ALTER TABLE [dbo].[nhan_vien] ADD [ten_nhan_vien] [nvarchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.nhan_vien', N'email') IS NULL
            ALTER TABLE [dbo].[nhan_vien] ADD [email] [varchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.nhan_vien', N'mat_khau') IS NULL
            ALTER TABLE [dbo].[nhan_vien] ADD [mat_khau] [varchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.nhan_vien', N'so_dien_thoai') IS NULL
            ALTER TABLE [dbo].[nhan_vien] ADD [so_dien_thoai] [varchar](20) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.nhan_vien', N'gioi_tinh') IS NULL
            ALTER TABLE [dbo].[nhan_vien] ADD [gioi_tinh] [int] NULL;
        IF COL_LENGTH(N'dbo.nhan_vien', N'ngay_sinh') IS NULL
            ALTER TABLE [dbo].[nhan_vien] ADD [ngay_sinh] [date] NULL;
        IF COL_LENGTH(N'dbo.nhan_vien', N'dia_chi') IS NULL
            ALTER TABLE [dbo].[nhan_vien] ADD [dia_chi] [nvarchar](500) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.nhan_vien', N'tinh_thanh') IS NULL
            ALTER TABLE [dbo].[nhan_vien] ADD [tinh_thanh] [nvarchar](100) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.nhan_vien', N'phuong_xa') IS NULL
            ALTER TABLE [dbo].[nhan_vien] ADD [phuong_xa] [nvarchar](100) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.nhan_vien', N'trang_thai') IS NULL
            ALTER TABLE [dbo].[nhan_vien] ADD [trang_thai] [int] NULL;
        IF COL_LENGTH(N'dbo.nhan_vien', N'ngay_tao') IS NULL
            ALTER TABLE [dbo].[nhan_vien] ADD [ngay_tao] [datetime2](7) NULL;
        IF COL_LENGTH(N'dbo.nhan_vien', N'hinh_anh') IS NULL
            ALTER TABLE [dbo].[nhan_vien] ADD [hinh_anh] [nvarchar](1000) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.nhan_vien', N'ngay_cap_nhat') IS NULL
            ALTER TABLE [dbo].[nhan_vien] ADD [ngay_cap_nhat] [datetime2](7) NULL;
    END;

    -- TABLE: [dbo].[phieu_giam_gia]
    IF OBJECT_ID(N'[dbo].[phieu_giam_gia]', N'U') IS NULL
    BEGIN
        CREATE TABLE [dbo].[phieu_giam_gia](
        	[id] [bigint] IDENTITY(1,1) NOT NULL,
        	[ma_phieu_giam_gia] [varchar](50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[ten_phieu_giam_gia] [nvarchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[loai_giam_gia] [int] NULL,
        	[gia_tri_giam] [decimal](18, 2) NULL,
        	[gia_tri_toi_thieu] [decimal](18, 2) NULL,
        	[giam_toi_da] [decimal](18, 2) NULL,
        	[ngay_bat_dau] [datetime2](7) NULL,
        	[ngay_ket_thuc] [datetime2](7) NULL,
        	[so_luong] [int] NULL,
        	[so_luong_da_dung] [int] NULL,
        	[trang_thai] [int] NULL,
        	[ngay_tao] [datetime2](7) NULL,
        	[ngay_cap_nhat] [datetime2](7) NULL,
        	[mo_ta] [nvarchar](1000) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[vo_han] [bit] NOT NULL,
         CONSTRAINT [PK__phieu_gi__3213E83F996CC37D] PRIMARY KEY CLUSTERED 
        (
        	[id] ASC
        )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        )
    END
    ELSE
    BEGIN
        IF COL_LENGTH(N'dbo.phieu_giam_gia', N'id') IS NULL
            ALTER TABLE [dbo].[phieu_giam_gia] ADD [id] [bigint] IDENTITY(1,1) NOT NULL;
        IF COL_LENGTH(N'dbo.phieu_giam_gia', N'ma_phieu_giam_gia') IS NULL
            ALTER TABLE [dbo].[phieu_giam_gia] ADD [ma_phieu_giam_gia] [varchar](50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.phieu_giam_gia', N'ten_phieu_giam_gia') IS NULL
            ALTER TABLE [dbo].[phieu_giam_gia] ADD [ten_phieu_giam_gia] [nvarchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.phieu_giam_gia', N'loai_giam_gia') IS NULL
            ALTER TABLE [dbo].[phieu_giam_gia] ADD [loai_giam_gia] [int] NULL;
        IF COL_LENGTH(N'dbo.phieu_giam_gia', N'gia_tri_giam') IS NULL
            ALTER TABLE [dbo].[phieu_giam_gia] ADD [gia_tri_giam] [decimal](18, 2) NULL;
        IF COL_LENGTH(N'dbo.phieu_giam_gia', N'gia_tri_toi_thieu') IS NULL
            ALTER TABLE [dbo].[phieu_giam_gia] ADD [gia_tri_toi_thieu] [decimal](18, 2) NULL;
        IF COL_LENGTH(N'dbo.phieu_giam_gia', N'giam_toi_da') IS NULL
            ALTER TABLE [dbo].[phieu_giam_gia] ADD [giam_toi_da] [decimal](18, 2) NULL;
        IF COL_LENGTH(N'dbo.phieu_giam_gia', N'ngay_bat_dau') IS NULL
            ALTER TABLE [dbo].[phieu_giam_gia] ADD [ngay_bat_dau] [datetime2](7) NULL;
        IF COL_LENGTH(N'dbo.phieu_giam_gia', N'ngay_ket_thuc') IS NULL
            ALTER TABLE [dbo].[phieu_giam_gia] ADD [ngay_ket_thuc] [datetime2](7) NULL;
        IF COL_LENGTH(N'dbo.phieu_giam_gia', N'so_luong') IS NULL
            ALTER TABLE [dbo].[phieu_giam_gia] ADD [so_luong] [int] NULL;
        IF COL_LENGTH(N'dbo.phieu_giam_gia', N'so_luong_da_dung') IS NULL
            ALTER TABLE [dbo].[phieu_giam_gia] ADD [so_luong_da_dung] [int] NULL;
        IF COL_LENGTH(N'dbo.phieu_giam_gia', N'trang_thai') IS NULL
            ALTER TABLE [dbo].[phieu_giam_gia] ADD [trang_thai] [int] NULL;
        IF COL_LENGTH(N'dbo.phieu_giam_gia', N'ngay_tao') IS NULL
            ALTER TABLE [dbo].[phieu_giam_gia] ADD [ngay_tao] [datetime2](7) NULL;
        IF COL_LENGTH(N'dbo.phieu_giam_gia', N'ngay_cap_nhat') IS NULL
            ALTER TABLE [dbo].[phieu_giam_gia] ADD [ngay_cap_nhat] [datetime2](7) NULL;
        IF COL_LENGTH(N'dbo.phieu_giam_gia', N'mo_ta') IS NULL
            ALTER TABLE [dbo].[phieu_giam_gia] ADD [mo_ta] [nvarchar](1000) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.phieu_giam_gia', N'vo_han') IS NULL
            ALTER TABLE [dbo].[phieu_giam_gia] ADD [vo_han] [bit] NOT NULL CONSTRAINT [DF_phieu_giam_gia_vo_han_overlay] DEFAULT ((0)) WITH VALUES;
    END;

    -- TABLE: [dbo].[phieu_giam_gia_khach_hang]
    IF OBJECT_ID(N'[dbo].[phieu_giam_gia_khach_hang]', N'U') IS NULL
    BEGIN
        CREATE TABLE [dbo].[phieu_giam_gia_khach_hang](
        	[id] [bigint] IDENTITY(1,1) NOT NULL,
        	[id_khach_hang] [bigint] NOT NULL,
        	[id_phieu_giam_gia] [bigint] NOT NULL,
        	[ngay_su_dung] [datetime2](7) NULL,
        	[trang_thai] [int] NULL,
         CONSTRAINT [PK__phieu_gi__3213E83FC16B3D3D] PRIMARY KEY CLUSTERED 
        (
        	[id] ASC
        )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        )
    END
    ELSE
    BEGIN
        IF COL_LENGTH(N'dbo.phieu_giam_gia_khach_hang', N'id') IS NULL
            ALTER TABLE [dbo].[phieu_giam_gia_khach_hang] ADD [id] [bigint] IDENTITY(1,1) NOT NULL;
        IF COL_LENGTH(N'dbo.phieu_giam_gia_khach_hang', N'id_khach_hang') IS NULL
        BEGIN
            IF NOT EXISTS (SELECT 1 FROM [dbo].[phieu_giam_gia_khach_hang])
                ALTER TABLE [dbo].[phieu_giam_gia_khach_hang] ADD [id_khach_hang] [bigint] NOT NULL;
            ELSE
            BEGIN
                ALTER TABLE [dbo].[phieu_giam_gia_khach_hang] ADD [id_khach_hang] [bigint] NULL;
                PRINT N'CANH BAO: dbo.phieu_giam_gia_khach_hang.id_khach_hang duoc them NULL de giu du lieu cu; snapshot goc la NOT NULL.';
            END
        END
        IF COL_LENGTH(N'dbo.phieu_giam_gia_khach_hang', N'id_phieu_giam_gia') IS NULL
        BEGIN
            IF NOT EXISTS (SELECT 1 FROM [dbo].[phieu_giam_gia_khach_hang])
                ALTER TABLE [dbo].[phieu_giam_gia_khach_hang] ADD [id_phieu_giam_gia] [bigint] NOT NULL;
            ELSE
            BEGIN
                ALTER TABLE [dbo].[phieu_giam_gia_khach_hang] ADD [id_phieu_giam_gia] [bigint] NULL;
                PRINT N'CANH BAO: dbo.phieu_giam_gia_khach_hang.id_phieu_giam_gia duoc them NULL de giu du lieu cu; snapshot goc la NOT NULL.';
            END
        END
        IF COL_LENGTH(N'dbo.phieu_giam_gia_khach_hang', N'ngay_su_dung') IS NULL
            ALTER TABLE [dbo].[phieu_giam_gia_khach_hang] ADD [ngay_su_dung] [datetime2](7) NULL;
        IF COL_LENGTH(N'dbo.phieu_giam_gia_khach_hang', N'trang_thai') IS NULL
            ALTER TABLE [dbo].[phieu_giam_gia_khach_hang] ADD [trang_thai] [int] NULL;
    END;

    -- TABLE: [dbo].[phuong_thuc_thanh_toan]
    IF OBJECT_ID(N'[dbo].[phuong_thuc_thanh_toan]', N'U') IS NULL
    BEGIN
        CREATE TABLE [dbo].[phuong_thuc_thanh_toan](
        	[id] [bigint] IDENTITY(1,1) NOT NULL,
        	[id_hinh_thuc_thanh_toan] [bigint] NOT NULL,
        	[ma_phuong_thuc] [varchar](50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[ten_phuong_thuc] [nvarchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[trang_thai] [int] NULL,
         CONSTRAINT [PK__phuong_t__3213E83F01DAC923] PRIMARY KEY CLUSTERED 
        (
        	[id] ASC
        )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        )
    END
    ELSE
    BEGIN
        IF COL_LENGTH(N'dbo.phuong_thuc_thanh_toan', N'id') IS NULL
            ALTER TABLE [dbo].[phuong_thuc_thanh_toan] ADD [id] [bigint] IDENTITY(1,1) NOT NULL;
        IF COL_LENGTH(N'dbo.phuong_thuc_thanh_toan', N'id_hinh_thuc_thanh_toan') IS NULL
        BEGIN
            IF NOT EXISTS (SELECT 1 FROM [dbo].[phuong_thuc_thanh_toan])
                ALTER TABLE [dbo].[phuong_thuc_thanh_toan] ADD [id_hinh_thuc_thanh_toan] [bigint] NOT NULL;
            ELSE
            BEGIN
                ALTER TABLE [dbo].[phuong_thuc_thanh_toan] ADD [id_hinh_thuc_thanh_toan] [bigint] NULL;
                PRINT N'CANH BAO: dbo.phuong_thuc_thanh_toan.id_hinh_thuc_thanh_toan duoc them NULL de giu du lieu cu; snapshot goc la NOT NULL.';
            END
        END
        IF COL_LENGTH(N'dbo.phuong_thuc_thanh_toan', N'ma_phuong_thuc') IS NULL
            ALTER TABLE [dbo].[phuong_thuc_thanh_toan] ADD [ma_phuong_thuc] [varchar](50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.phuong_thuc_thanh_toan', N'ten_phuong_thuc') IS NULL
            ALTER TABLE [dbo].[phuong_thuc_thanh_toan] ADD [ten_phuong_thuc] [nvarchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.phuong_thuc_thanh_toan', N'trang_thai') IS NULL
            ALTER TABLE [dbo].[phuong_thuc_thanh_toan] ADD [trang_thai] [int] NULL;
    END;

    -- TABLE: [dbo].[san_pham]
    IF OBJECT_ID(N'[dbo].[san_pham]', N'U') IS NULL
    BEGIN
        CREATE TABLE [dbo].[san_pham](
        	[id] [bigint] IDENTITY(1,1) NOT NULL,
        	[id_danh_muc] [bigint] NULL,
        	[id_thuong_hieu] [bigint] NULL,
        	[id_chat_lieu] [bigint] NULL,
        	[id_kieu_dang] [bigint] NULL,
        	[id_co_giay] [bigint] NULL,
        	[id_xuat_xu] [bigint] NULL,
        	[ma_san_pham] [varchar](50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[ten_san_pham] [nvarchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[mo_ta_chi_tiet] [nvarchar](max) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[ngay_tao] [datetime2](7) NULL,
        	[nguoi_tao] [bigint] NULL,
        	[nguoi_cap_nhat] [bigint] NULL,
        	[ngay_cap_nhat] [datetime2](7) NULL,
        	[trang_thai] [int] NULL,
         CONSTRAINT [PK__san_pham__3213E83F13EB8DE2] PRIMARY KEY CLUSTERED 
        (
        	[id] ASC
        )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        )
    END
    ELSE
    BEGIN
        IF COL_LENGTH(N'dbo.san_pham', N'id') IS NULL
            ALTER TABLE [dbo].[san_pham] ADD [id] [bigint] IDENTITY(1,1) NOT NULL;
        IF COL_LENGTH(N'dbo.san_pham', N'id_danh_muc') IS NULL
            ALTER TABLE [dbo].[san_pham] ADD [id_danh_muc] [bigint] NULL;
        IF COL_LENGTH(N'dbo.san_pham', N'id_thuong_hieu') IS NULL
            ALTER TABLE [dbo].[san_pham] ADD [id_thuong_hieu] [bigint] NULL;
        IF COL_LENGTH(N'dbo.san_pham', N'id_chat_lieu') IS NULL
            ALTER TABLE [dbo].[san_pham] ADD [id_chat_lieu] [bigint] NULL;
        IF COL_LENGTH(N'dbo.san_pham', N'id_kieu_dang') IS NULL
            ALTER TABLE [dbo].[san_pham] ADD [id_kieu_dang] [bigint] NULL;
        IF COL_LENGTH(N'dbo.san_pham', N'id_co_giay') IS NULL
            ALTER TABLE [dbo].[san_pham] ADD [id_co_giay] [bigint] NULL;
        IF COL_LENGTH(N'dbo.san_pham', N'id_xuat_xu') IS NULL
            ALTER TABLE [dbo].[san_pham] ADD [id_xuat_xu] [bigint] NULL;
        IF COL_LENGTH(N'dbo.san_pham', N'ma_san_pham') IS NULL
            ALTER TABLE [dbo].[san_pham] ADD [ma_san_pham] [varchar](50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.san_pham', N'ten_san_pham') IS NULL
            ALTER TABLE [dbo].[san_pham] ADD [ten_san_pham] [nvarchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.san_pham', N'mo_ta_chi_tiet') IS NULL
            ALTER TABLE [dbo].[san_pham] ADD [mo_ta_chi_tiet] [nvarchar](max) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.san_pham', N'ngay_tao') IS NULL
            ALTER TABLE [dbo].[san_pham] ADD [ngay_tao] [datetime2](7) NULL;
        IF COL_LENGTH(N'dbo.san_pham', N'nguoi_tao') IS NULL
            ALTER TABLE [dbo].[san_pham] ADD [nguoi_tao] [bigint] NULL;
        IF COL_LENGTH(N'dbo.san_pham', N'nguoi_cap_nhat') IS NULL
            ALTER TABLE [dbo].[san_pham] ADD [nguoi_cap_nhat] [bigint] NULL;
        IF COL_LENGTH(N'dbo.san_pham', N'ngay_cap_nhat') IS NULL
            ALTER TABLE [dbo].[san_pham] ADD [ngay_cap_nhat] [datetime2](7) NULL;
        IF COL_LENGTH(N'dbo.san_pham', N'trang_thai') IS NULL
            ALTER TABLE [dbo].[san_pham] ADD [trang_thai] [int] NULL;
    END;

    -- TABLE: [dbo].[san_pham_chi_tiet]
    IF OBJECT_ID(N'[dbo].[san_pham_chi_tiet]', N'U') IS NULL
    BEGIN
        CREATE TABLE [dbo].[san_pham_chi_tiet](
        	[id] [bigint] IDENTITY(1,1) NOT NULL,
        	[id_san_pham] [bigint] NOT NULL,
        	[id_mau_sac] [bigint] NOT NULL,
        	[id_kich_thuoc] [bigint] NOT NULL,
        	[ma_chi_tiet_san_pham] [varchar](100) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[so_luong] [int] NULL,
        	[gia_ban] [decimal](18, 2) NULL,
        	[sku] [varchar](100) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[kich_hoat] [bit] NULL,
        	[ngay_tao] [datetime2](7) NULL,
        	[ngay_cap_nhat] [datetime2](7) NULL,
        	[trang_thai] [int] NULL,
         CONSTRAINT [PK__san_pham__3213E83F2C4A75C2] PRIMARY KEY CLUSTERED 
        (
        	[id] ASC
        )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        )
    END
    ELSE
    BEGIN
        IF COL_LENGTH(N'dbo.san_pham_chi_tiet', N'id') IS NULL
            ALTER TABLE [dbo].[san_pham_chi_tiet] ADD [id] [bigint] IDENTITY(1,1) NOT NULL;
        IF COL_LENGTH(N'dbo.san_pham_chi_tiet', N'id_san_pham') IS NULL
        BEGIN
            IF NOT EXISTS (SELECT 1 FROM [dbo].[san_pham_chi_tiet])
                ALTER TABLE [dbo].[san_pham_chi_tiet] ADD [id_san_pham] [bigint] NOT NULL;
            ELSE
            BEGIN
                ALTER TABLE [dbo].[san_pham_chi_tiet] ADD [id_san_pham] [bigint] NULL;
                PRINT N'CANH BAO: dbo.san_pham_chi_tiet.id_san_pham duoc them NULL de giu du lieu cu; snapshot goc la NOT NULL.';
            END
        END
        IF COL_LENGTH(N'dbo.san_pham_chi_tiet', N'id_mau_sac') IS NULL
        BEGIN
            IF NOT EXISTS (SELECT 1 FROM [dbo].[san_pham_chi_tiet])
                ALTER TABLE [dbo].[san_pham_chi_tiet] ADD [id_mau_sac] [bigint] NOT NULL;
            ELSE
            BEGIN
                ALTER TABLE [dbo].[san_pham_chi_tiet] ADD [id_mau_sac] [bigint] NULL;
                PRINT N'CANH BAO: dbo.san_pham_chi_tiet.id_mau_sac duoc them NULL de giu du lieu cu; snapshot goc la NOT NULL.';
            END
        END
        IF COL_LENGTH(N'dbo.san_pham_chi_tiet', N'id_kich_thuoc') IS NULL
        BEGIN
            IF NOT EXISTS (SELECT 1 FROM [dbo].[san_pham_chi_tiet])
                ALTER TABLE [dbo].[san_pham_chi_tiet] ADD [id_kich_thuoc] [bigint] NOT NULL;
            ELSE
            BEGIN
                ALTER TABLE [dbo].[san_pham_chi_tiet] ADD [id_kich_thuoc] [bigint] NULL;
                PRINT N'CANH BAO: dbo.san_pham_chi_tiet.id_kich_thuoc duoc them NULL de giu du lieu cu; snapshot goc la NOT NULL.';
            END
        END
        IF COL_LENGTH(N'dbo.san_pham_chi_tiet', N'ma_chi_tiet_san_pham') IS NULL
            ALTER TABLE [dbo].[san_pham_chi_tiet] ADD [ma_chi_tiet_san_pham] [varchar](100) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.san_pham_chi_tiet', N'so_luong') IS NULL
            ALTER TABLE [dbo].[san_pham_chi_tiet] ADD [so_luong] [int] NULL;
        IF COL_LENGTH(N'dbo.san_pham_chi_tiet', N'gia_ban') IS NULL
            ALTER TABLE [dbo].[san_pham_chi_tiet] ADD [gia_ban] [decimal](18, 2) NULL;
        IF COL_LENGTH(N'dbo.san_pham_chi_tiet', N'sku') IS NULL
            ALTER TABLE [dbo].[san_pham_chi_tiet] ADD [sku] [varchar](100) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.san_pham_chi_tiet', N'kich_hoat') IS NULL
            ALTER TABLE [dbo].[san_pham_chi_tiet] ADD [kich_hoat] [bit] NULL;
        IF COL_LENGTH(N'dbo.san_pham_chi_tiet', N'ngay_tao') IS NULL
            ALTER TABLE [dbo].[san_pham_chi_tiet] ADD [ngay_tao] [datetime2](7) NULL;
        IF COL_LENGTH(N'dbo.san_pham_chi_tiet', N'ngay_cap_nhat') IS NULL
            ALTER TABLE [dbo].[san_pham_chi_tiet] ADD [ngay_cap_nhat] [datetime2](7) NULL;
        IF COL_LENGTH(N'dbo.san_pham_chi_tiet', N'trang_thai') IS NULL
            ALTER TABLE [dbo].[san_pham_chi_tiet] ADD [trang_thai] [int] NULL;
    END;

    -- TABLE: [dbo].[thuong_hieu]
    IF OBJECT_ID(N'[dbo].[thuong_hieu]', N'U') IS NULL
    BEGIN
        CREATE TABLE [dbo].[thuong_hieu](
        	[id] [bigint] IDENTITY(1,1) NOT NULL,
        	[ma_thuong_hieu] [varchar](50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[ten_thuong_hieu] [nvarchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[trang_thai] [int] NULL,
         CONSTRAINT [PK__thuong_h__3213E83F36A620CA] PRIMARY KEY CLUSTERED 
        (
        	[id] ASC
        )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        )
    END
    ELSE
    BEGIN
        IF COL_LENGTH(N'dbo.thuong_hieu', N'id') IS NULL
            ALTER TABLE [dbo].[thuong_hieu] ADD [id] [bigint] IDENTITY(1,1) NOT NULL;
        IF COL_LENGTH(N'dbo.thuong_hieu', N'ma_thuong_hieu') IS NULL
            ALTER TABLE [dbo].[thuong_hieu] ADD [ma_thuong_hieu] [varchar](50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.thuong_hieu', N'ten_thuong_hieu') IS NULL
            ALTER TABLE [dbo].[thuong_hieu] ADD [ten_thuong_hieu] [nvarchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.thuong_hieu', N'trang_thai') IS NULL
            ALTER TABLE [dbo].[thuong_hieu] ADD [trang_thai] [int] NULL;
    END;

    -- TABLE: [dbo].[vai_tro]
    IF OBJECT_ID(N'[dbo].[vai_tro]', N'U') IS NULL
    BEGIN
        CREATE TABLE [dbo].[vai_tro](
        	[id] [bigint] IDENTITY(1,1) NOT NULL,
        	[ten_vai_tro] [nvarchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[mo_ta] [nvarchar](1000) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[trang_thai] [int] NULL,
         CONSTRAINT [PK__vai_tro__3213E83F43C01B45] PRIMARY KEY CLUSTERED 
        (
        	[id] ASC
        )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        )
    END
    ELSE
    BEGIN
        IF COL_LENGTH(N'dbo.vai_tro', N'id') IS NULL
            ALTER TABLE [dbo].[vai_tro] ADD [id] [bigint] IDENTITY(1,1) NOT NULL;
        IF COL_LENGTH(N'dbo.vai_tro', N'ten_vai_tro') IS NULL
            ALTER TABLE [dbo].[vai_tro] ADD [ten_vai_tro] [nvarchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.vai_tro', N'mo_ta') IS NULL
            ALTER TABLE [dbo].[vai_tro] ADD [mo_ta] [nvarchar](1000) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.vai_tro', N'trang_thai') IS NULL
            ALTER TABLE [dbo].[vai_tro] ADD [trang_thai] [int] NULL;
    END;

    -- TABLE: [dbo].[xuat_xu]
    IF OBJECT_ID(N'[dbo].[xuat_xu]', N'U') IS NULL
    BEGIN
        CREATE TABLE [dbo].[xuat_xu](
        	[id] [bigint] IDENTITY(1,1) NOT NULL,
        	[ma_xuat_xu] [varchar](50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[ten_xuat_xu] [nvarchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
        	[trang_thai] [int] NULL,
         CONSTRAINT [PK__xuat_xu__3213E83FA9749B84] PRIMARY KEY CLUSTERED 
        (
        	[id] ASC
        )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        )
    END
    ELSE
    BEGIN
        IF COL_LENGTH(N'dbo.xuat_xu', N'id') IS NULL
            ALTER TABLE [dbo].[xuat_xu] ADD [id] [bigint] IDENTITY(1,1) NOT NULL;
        IF COL_LENGTH(N'dbo.xuat_xu', N'ma_xuat_xu') IS NULL
            ALTER TABLE [dbo].[xuat_xu] ADD [ma_xuat_xu] [varchar](50) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.xuat_xu', N'ten_xuat_xu') IS NULL
            ALTER TABLE [dbo].[xuat_xu] ADD [ten_xuat_xu] [nvarchar](255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL;
        IF COL_LENGTH(N'dbo.xuat_xu', N'trang_thai') IS NULL
            ALTER TABLE [dbo].[xuat_xu] ADD [trang_thai] [int] NULL;
    END;

    -- DEFAULT cua phieu_giam_gia.vo_han
    IF COL_LENGTH(N'dbo.phieu_giam_gia', N'vo_han') IS NOT NULL
       AND NOT EXISTS (
           SELECT 1
           FROM sys.default_constraints dc
           JOIN sys.columns c ON c.object_id = dc.parent_object_id AND c.column_id = dc.parent_column_id
           WHERE dc.parent_object_id = OBJECT_ID(N'dbo.phieu_giam_gia') AND c.name = N'vo_han'
       )
    BEGIN
        ALTER TABLE [dbo].[phieu_giam_gia] ADD CONSTRAINT [DF_phieu_giam_gia_vo_han] DEFAULT ((0)) FOR [vo_han];
    END;

    PRINT N'2/4 - Tam tat CHECK/FK tren 25 bang de merge du lieu an toan...';
    IF OBJECT_ID(N'[dbo].[chat_lieu]', N'U') IS NOT NULL ALTER TABLE [dbo].[chat_lieu] NOCHECK CONSTRAINT ALL;
    IF OBJECT_ID(N'[dbo].[chi_tiet_dot_giam_gia]', N'U') IS NOT NULL ALTER TABLE [dbo].[chi_tiet_dot_giam_gia] NOCHECK CONSTRAINT ALL;
    IF OBJECT_ID(N'[dbo].[co_giay]', N'U') IS NOT NULL ALTER TABLE [dbo].[co_giay] NOCHECK CONSTRAINT ALL;
    IF OBJECT_ID(N'[dbo].[danh_muc]', N'U') IS NOT NULL ALTER TABLE [dbo].[danh_muc] NOCHECK CONSTRAINT ALL;
    IF OBJECT_ID(N'[dbo].[dia_chi_khach_hang]', N'U') IS NOT NULL ALTER TABLE [dbo].[dia_chi_khach_hang] NOCHECK CONSTRAINT ALL;
    IF OBJECT_ID(N'[dbo].[dot_giam_gia]', N'U') IS NOT NULL ALTER TABLE [dbo].[dot_giam_gia] NOCHECK CONSTRAINT ALL;
    IF OBJECT_ID(N'[dbo].[hinh_anh_san_pham]', N'U') IS NOT NULL ALTER TABLE [dbo].[hinh_anh_san_pham] NOCHECK CONSTRAINT ALL;
    IF OBJECT_ID(N'[dbo].[hinh_thuc_thanh_toan]', N'U') IS NOT NULL ALTER TABLE [dbo].[hinh_thuc_thanh_toan] NOCHECK CONSTRAINT ALL;
    IF OBJECT_ID(N'[dbo].[hoa_don]', N'U') IS NOT NULL ALTER TABLE [dbo].[hoa_don] NOCHECK CONSTRAINT ALL;
    IF OBJECT_ID(N'[dbo].[hoa_don_chi_tiet]', N'U') IS NOT NULL ALTER TABLE [dbo].[hoa_don_chi_tiet] NOCHECK CONSTRAINT ALL;
    IF OBJECT_ID(N'[dbo].[khach_hang]', N'U') IS NOT NULL ALTER TABLE [dbo].[khach_hang] NOCHECK CONSTRAINT ALL;
    IF OBJECT_ID(N'[dbo].[kich_thuoc]', N'U') IS NOT NULL ALTER TABLE [dbo].[kich_thuoc] NOCHECK CONSTRAINT ALL;
    IF OBJECT_ID(N'[dbo].[kieu_dang]', N'U') IS NOT NULL ALTER TABLE [dbo].[kieu_dang] NOCHECK CONSTRAINT ALL;
    IF OBJECT_ID(N'[dbo].[lich_su_hoa_don]', N'U') IS NOT NULL ALTER TABLE [dbo].[lich_su_hoa_don] NOCHECK CONSTRAINT ALL;
    IF OBJECT_ID(N'[dbo].[lich_su_thanh_toan]', N'U') IS NOT NULL ALTER TABLE [dbo].[lich_su_thanh_toan] NOCHECK CONSTRAINT ALL;
    IF OBJECT_ID(N'[dbo].[mau_sac]', N'U') IS NOT NULL ALTER TABLE [dbo].[mau_sac] NOCHECK CONSTRAINT ALL;
    IF OBJECT_ID(N'[dbo].[nhan_vien]', N'U') IS NOT NULL ALTER TABLE [dbo].[nhan_vien] NOCHECK CONSTRAINT ALL;
    IF OBJECT_ID(N'[dbo].[phieu_giam_gia]', N'U') IS NOT NULL ALTER TABLE [dbo].[phieu_giam_gia] NOCHECK CONSTRAINT ALL;
    IF OBJECT_ID(N'[dbo].[phieu_giam_gia_khach_hang]', N'U') IS NOT NULL ALTER TABLE [dbo].[phieu_giam_gia_khach_hang] NOCHECK CONSTRAINT ALL;
    IF OBJECT_ID(N'[dbo].[phuong_thuc_thanh_toan]', N'U') IS NOT NULL ALTER TABLE [dbo].[phuong_thuc_thanh_toan] NOCHECK CONSTRAINT ALL;
    IF OBJECT_ID(N'[dbo].[san_pham]', N'U') IS NOT NULL ALTER TABLE [dbo].[san_pham] NOCHECK CONSTRAINT ALL;
    IF OBJECT_ID(N'[dbo].[san_pham_chi_tiet]', N'U') IS NOT NULL ALTER TABLE [dbo].[san_pham_chi_tiet] NOCHECK CONSTRAINT ALL;
    IF OBJECT_ID(N'[dbo].[thuong_hieu]', N'U') IS NOT NULL ALTER TABLE [dbo].[thuong_hieu] NOCHECK CONSTRAINT ALL;
    IF OBJECT_ID(N'[dbo].[vai_tro]', N'U') IS NOT NULL ALTER TABLE [dbo].[vai_tro] NOCHECK CONSTRAINT ALL;
    IF OBJECT_ID(N'[dbo].[xuat_xu]', N'U') IS NOT NULL ALTER TABLE [dbo].[xuat_xu] NOCHECK CONSTRAINT ALL;

    PRINT N'3/4 - Merge du lieu snapshot theo ID...';
    -- DATA MERGE: [dbo].[chat_lieu] (10 dong snapshot)
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[chat_lieu]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[chat_lieu] ON;
    IF EXISTS (SELECT 1 FROM [dbo].[chat_lieu] WHERE [id] = 1)
        UPDATE [dbo].[chat_lieu] SET [ma_chat_lieu] = N'CL001', [ten_chat_lieu] = N'Vải Mesh', [trang_thai] = 1 WHERE [id] = 1;
    ELSE
        INSERT [dbo].[chat_lieu] ([id], [ma_chat_lieu], [ten_chat_lieu], [trang_thai]) VALUES (1, N'CL001', N'Vải Mesh', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[chat_lieu] WHERE [id] = 2)
        UPDATE [dbo].[chat_lieu] SET [ma_chat_lieu] = N'CL002', [ten_chat_lieu] = N'Da tổng hợp', [trang_thai] = 1 WHERE [id] = 2;
    ELSE
        INSERT [dbo].[chat_lieu] ([id], [ma_chat_lieu], [ten_chat_lieu], [trang_thai]) VALUES (2, N'CL002', N'Da tổng hợp', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[chat_lieu] WHERE [id] = 3)
        UPDATE [dbo].[chat_lieu] SET [ma_chat_lieu] = N'CL003', [ten_chat_lieu] = N'Da thật', [trang_thai] = 1 WHERE [id] = 3;
    ELSE
        INSERT [dbo].[chat_lieu] ([id], [ma_chat_lieu], [ten_chat_lieu], [trang_thai]) VALUES (3, N'CL003', N'Da thật', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[chat_lieu] WHERE [id] = 4)
        UPDATE [dbo].[chat_lieu] SET [ma_chat_lieu] = N'CL004', [ten_chat_lieu] = N'Vải Canvas', [trang_thai] = 1 WHERE [id] = 4;
    ELSE
        INSERT [dbo].[chat_lieu] ([id], [ma_chat_lieu], [ten_chat_lieu], [trang_thai]) VALUES (4, N'CL004', N'Vải Canvas', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[chat_lieu] WHERE [id] = 5)
        UPDATE [dbo].[chat_lieu] SET [ma_chat_lieu] = N'CL005', [ten_chat_lieu] = N'Vải Knit', [trang_thai] = 1 WHERE [id] = 5;
    ELSE
        INSERT [dbo].[chat_lieu] ([id], [ma_chat_lieu], [ten_chat_lieu], [trang_thai]) VALUES (5, N'CL005', N'Vải Knit', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[chat_lieu] WHERE [id] = 6)
        UPDATE [dbo].[chat_lieu] SET [ma_chat_lieu] = N'CL006', [ten_chat_lieu] = N'Da lộn', [trang_thai] = 1 WHERE [id] = 6;
    ELSE
        INSERT [dbo].[chat_lieu] ([id], [ma_chat_lieu], [ten_chat_lieu], [trang_thai]) VALUES (6, N'CL006', N'Da lộn', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[chat_lieu] WHERE [id] = 7)
        UPDATE [dbo].[chat_lieu] SET [ma_chat_lieu] = N'CL007', [ten_chat_lieu] = N'Polyester', [trang_thai] = 1 WHERE [id] = 7;
    ELSE
        INSERT [dbo].[chat_lieu] ([id], [ma_chat_lieu], [ten_chat_lieu], [trang_thai]) VALUES (7, N'CL007', N'Polyester', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[chat_lieu] WHERE [id] = 8)
        UPDATE [dbo].[chat_lieu] SET [ma_chat_lieu] = N'CL008', [ten_chat_lieu] = N'Cao su', [trang_thai] = 1 WHERE [id] = 8;
    ELSE
        INSERT [dbo].[chat_lieu] ([id], [ma_chat_lieu], [ten_chat_lieu], [trang_thai]) VALUES (8, N'CL008', N'Cao su', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[chat_lieu] WHERE [id] = 9)
        UPDATE [dbo].[chat_lieu] SET [ma_chat_lieu] = N'CL009', [ten_chat_lieu] = N'Vải dệt', [trang_thai] = 1 WHERE [id] = 9;
    ELSE
        INSERT [dbo].[chat_lieu] ([id], [ma_chat_lieu], [ten_chat_lieu], [trang_thai]) VALUES (9, N'CL009', N'Vải dệt', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[chat_lieu] WHERE [id] = 10)
        UPDATE [dbo].[chat_lieu] SET [ma_chat_lieu] = N'CL010', [ten_chat_lieu] = N'Sợi tổng hợp', [trang_thai] = 1 WHERE [id] = 10;
    ELSE
        INSERT [dbo].[chat_lieu] ([id], [ma_chat_lieu], [ten_chat_lieu], [trang_thai]) VALUES (10, N'CL010', N'Sợi tổng hợp', 1);
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[chat_lieu]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[chat_lieu] OFF;

    -- DATA MERGE: [dbo].[chi_tiet_dot_giam_gia] (10 dong snapshot)
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[chi_tiet_dot_giam_gia]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[chi_tiet_dot_giam_gia] ON;
    IF EXISTS (SELECT 1 FROM [dbo].[chi_tiet_dot_giam_gia] WHERE [id] = 0)
        UPDATE [dbo].[chi_tiet_dot_giam_gia] SET [id_dot_giam_gia] = 1, [id_san_pham_chi_tiet] = 15, [phan_tram_giam_bien_the] = NULL, [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2) WHERE [id] = 0;
    ELSE
        INSERT [dbo].[chi_tiet_dot_giam_gia] ([id], [id_dot_giam_gia], [id_san_pham_chi_tiet], [phan_tram_giam_bien_the], [trang_thai], [ngay_tao]) VALUES (0, 1, 15, NULL, 1, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[chi_tiet_dot_giam_gia] WHERE [id] = 1)
        UPDATE [dbo].[chi_tiet_dot_giam_gia] SET [id_dot_giam_gia] = 1, [id_san_pham_chi_tiet] = 14, [phan_tram_giam_bien_the] = NULL, [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2) WHERE [id] = 1;
    ELSE
        INSERT [dbo].[chi_tiet_dot_giam_gia] ([id], [id_dot_giam_gia], [id_san_pham_chi_tiet], [phan_tram_giam_bien_the], [trang_thai], [ngay_tao]) VALUES (1, 1, 14, NULL, 1, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[chi_tiet_dot_giam_gia] WHERE [id] = 2)
        UPDATE [dbo].[chi_tiet_dot_giam_gia] SET [id_dot_giam_gia] = 2, [id_san_pham_chi_tiet] = 13, [phan_tram_giam_bien_the] = NULL, [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2) WHERE [id] = 2;
    ELSE
        INSERT [dbo].[chi_tiet_dot_giam_gia] ([id], [id_dot_giam_gia], [id_san_pham_chi_tiet], [phan_tram_giam_bien_the], [trang_thai], [ngay_tao]) VALUES (2, 2, 13, NULL, 1, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[chi_tiet_dot_giam_gia] WHERE [id] = 3)
        UPDATE [dbo].[chi_tiet_dot_giam_gia] SET [id_dot_giam_gia] = 2, [id_san_pham_chi_tiet] = 6, [phan_tram_giam_bien_the] = NULL, [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2) WHERE [id] = 3;
    ELSE
        INSERT [dbo].[chi_tiet_dot_giam_gia] ([id], [id_dot_giam_gia], [id_san_pham_chi_tiet], [phan_tram_giam_bien_the], [trang_thai], [ngay_tao]) VALUES (3, 2, 6, NULL, 1, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[chi_tiet_dot_giam_gia] WHERE [id] = 4)
        UPDATE [dbo].[chi_tiet_dot_giam_gia] SET [id_dot_giam_gia] = 3, [id_san_pham_chi_tiet] = 5, [phan_tram_giam_bien_the] = NULL, [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2) WHERE [id] = 4;
    ELSE
        INSERT [dbo].[chi_tiet_dot_giam_gia] ([id], [id_dot_giam_gia], [id_san_pham_chi_tiet], [phan_tram_giam_bien_the], [trang_thai], [ngay_tao]) VALUES (4, 3, 5, NULL, 1, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[chi_tiet_dot_giam_gia] WHERE [id] = 5)
        UPDATE [dbo].[chi_tiet_dot_giam_gia] SET [id_dot_giam_gia] = 3, [id_san_pham_chi_tiet] = 4, [phan_tram_giam_bien_the] = NULL, [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2) WHERE [id] = 5;
    ELSE
        INSERT [dbo].[chi_tiet_dot_giam_gia] ([id], [id_dot_giam_gia], [id_san_pham_chi_tiet], [phan_tram_giam_bien_the], [trang_thai], [ngay_tao]) VALUES (5, 3, 4, NULL, 1, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[chi_tiet_dot_giam_gia] WHERE [id] = 12)
        UPDATE [dbo].[chi_tiet_dot_giam_gia] SET [id_dot_giam_gia] = 5, [id_san_pham_chi_tiet] = 81, [phan_tram_giam_bien_the] = CAST(3.00 AS Decimal(18, 2)), [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-07T02:13:35.8986630' AS DateTime2) WHERE [id] = 12;
    ELSE
        INSERT [dbo].[chi_tiet_dot_giam_gia] ([id], [id_dot_giam_gia], [id_san_pham_chi_tiet], [phan_tram_giam_bien_the], [trang_thai], [ngay_tao]) VALUES (12, 5, 81, CAST(3.00 AS Decimal(18, 2)), 1, CAST(N'2026-10-07T02:13:35.8986630' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[chi_tiet_dot_giam_gia] WHERE [id] = 13)
        UPDATE [dbo].[chi_tiet_dot_giam_gia] SET [id_dot_giam_gia] = 5, [id_san_pham_chi_tiet] = 82, [phan_tram_giam_bien_the] = CAST(3.00 AS Decimal(18, 2)), [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-07T02:13:35.9151896' AS DateTime2) WHERE [id] = 13;
    ELSE
        INSERT [dbo].[chi_tiet_dot_giam_gia] ([id], [id_dot_giam_gia], [id_san_pham_chi_tiet], [phan_tram_giam_bien_the], [trang_thai], [ngay_tao]) VALUES (13, 5, 82, CAST(3.00 AS Decimal(18, 2)), 1, CAST(N'2026-10-07T02:13:35.9151896' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[chi_tiet_dot_giam_gia] WHERE [id] = 14)
        UPDATE [dbo].[chi_tiet_dot_giam_gia] SET [id_dot_giam_gia] = 5, [id_san_pham_chi_tiet] = 83, [phan_tram_giam_bien_the] = CAST(3.00 AS Decimal(18, 2)), [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-07T02:13:35.9151896' AS DateTime2) WHERE [id] = 14;
    ELSE
        INSERT [dbo].[chi_tiet_dot_giam_gia] ([id], [id_dot_giam_gia], [id_san_pham_chi_tiet], [phan_tram_giam_bien_the], [trang_thai], [ngay_tao]) VALUES (14, 5, 83, CAST(3.00 AS Decimal(18, 2)), 1, CAST(N'2026-10-07T02:13:35.9151896' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[chi_tiet_dot_giam_gia] WHERE [id] = 15)
        UPDATE [dbo].[chi_tiet_dot_giam_gia] SET [id_dot_giam_gia] = 5, [id_san_pham_chi_tiet] = 84, [phan_tram_giam_bien_the] = CAST(3.00 AS Decimal(18, 2)), [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-07T02:13:35.9151896' AS DateTime2) WHERE [id] = 15;
    ELSE
        INSERT [dbo].[chi_tiet_dot_giam_gia] ([id], [id_dot_giam_gia], [id_san_pham_chi_tiet], [phan_tram_giam_bien_the], [trang_thai], [ngay_tao]) VALUES (15, 5, 84, CAST(3.00 AS Decimal(18, 2)), 1, CAST(N'2026-10-07T02:13:35.9151896' AS DateTime2));
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[chi_tiet_dot_giam_gia]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[chi_tiet_dot_giam_gia] OFF;

    -- DATA MERGE: [dbo].[co_giay] (3 dong snapshot)
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[co_giay]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[co_giay] ON;
    IF EXISTS (SELECT 1 FROM [dbo].[co_giay] WHERE [id] = 1)
        UPDATE [dbo].[co_giay] SET [ma_co_giay] = N'CG001', [ten_co_giay] = N'Cổ thấp', [trang_thai] = 1 WHERE [id] = 1;
    ELSE
        INSERT [dbo].[co_giay] ([id], [ma_co_giay], [ten_co_giay], [trang_thai]) VALUES (1, N'CG001', N'Cổ thấp', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[co_giay] WHERE [id] = 2)
        UPDATE [dbo].[co_giay] SET [ma_co_giay] = N'CG002', [ten_co_giay] = N'Cổ trung', [trang_thai] = 1 WHERE [id] = 2;
    ELSE
        INSERT [dbo].[co_giay] ([id], [ma_co_giay], [ten_co_giay], [trang_thai]) VALUES (2, N'CG002', N'Cổ trung', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[co_giay] WHERE [id] = 3)
        UPDATE [dbo].[co_giay] SET [ma_co_giay] = N'CG003', [ten_co_giay] = N'Cổ cao', [trang_thai] = 1 WHERE [id] = 3;
    ELSE
        INSERT [dbo].[co_giay] ([id], [ma_co_giay], [ten_co_giay], [trang_thai]) VALUES (3, N'CG003', N'Cổ cao', 1);
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[co_giay]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[co_giay] OFF;

    -- DATA MERGE: [dbo].[danh_muc] (8 dong snapshot)
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[danh_muc]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[danh_muc] ON;
    IF EXISTS (SELECT 1 FROM [dbo].[danh_muc] WHERE [id] = 1)
        UPDATE [dbo].[danh_muc] SET [ma_danh_muc] = N'DM001', [ten_danh_muc] = N'Giày chạy bộ', [mo_ta] = N'Giày dành cho chạy bộ và luyện tập', [trang_thai] = 1 WHERE [id] = 1;
    ELSE
        INSERT [dbo].[danh_muc] ([id], [ma_danh_muc], [ten_danh_muc], [mo_ta], [trang_thai]) VALUES (1, N'DM001', N'Giày chạy bộ', N'Giày dành cho chạy bộ và luyện tập', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[danh_muc] WHERE [id] = 2)
        UPDATE [dbo].[danh_muc] SET [ma_danh_muc] = N'DM002', [ten_danh_muc] = N'Giày thể thao', [mo_ta] = N'Giày thể thao sử dụng hàng ngày', [trang_thai] = 1 WHERE [id] = 2;
    ELSE
        INSERT [dbo].[danh_muc] ([id], [ma_danh_muc], [ten_danh_muc], [mo_ta], [trang_thai]) VALUES (2, N'DM002', N'Giày thể thao', N'Giày thể thao sử dụng hàng ngày', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[danh_muc] WHERE [id] = 3)
        UPDATE [dbo].[danh_muc] SET [ma_danh_muc] = N'DM003', [ten_danh_muc] = N'Giày bóng rổ', [mo_ta] = N'Giày chuyên dụng cho bóng rổ', [trang_thai] = 1 WHERE [id] = 3;
    ELSE
        INSERT [dbo].[danh_muc] ([id], [ma_danh_muc], [ten_danh_muc], [mo_ta], [trang_thai]) VALUES (3, N'DM003', N'Giày bóng rổ', N'Giày chuyên dụng cho bóng rổ', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[danh_muc] WHERE [id] = 4)
        UPDATE [dbo].[danh_muc] SET [ma_danh_muc] = N'DM004', [ten_danh_muc] = N'Giày thời trang', [mo_ta] = N'Giày sneaker và thời trang đường phố', [trang_thai] = 1 WHERE [id] = 4;
    ELSE
        INSERT [dbo].[danh_muc] ([id], [ma_danh_muc], [ten_danh_muc], [mo_ta], [trang_thai]) VALUES (4, N'DM004', N'Giày thời trang', N'Giày sneaker và thời trang đường phố', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[danh_muc] WHERE [id] = 5)
        UPDATE [dbo].[danh_muc] SET [ma_danh_muc] = N'DM005', [ten_danh_muc] = N'Giày đá bóng', [mo_ta] = N'Giày sử dụng khi chơi bóng đá', [trang_thai] = 1 WHERE [id] = 5;
    ELSE
        INSERT [dbo].[danh_muc] ([id], [ma_danh_muc], [ten_danh_muc], [mo_ta], [trang_thai]) VALUES (5, N'DM005', N'Giày đá bóng', N'Giày sử dụng khi chơi bóng đá', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[danh_muc] WHERE [id] = 6)
        UPDATE [dbo].[danh_muc] SET [ma_danh_muc] = N'DM006', [ten_danh_muc] = N'Giày tennis', [mo_ta] = N'Giày dành cho tennis và thể thao sân', [trang_thai] = 1 WHERE [id] = 6;
    ELSE
        INSERT [dbo].[danh_muc] ([id], [ma_danh_muc], [ten_danh_muc], [mo_ta], [trang_thai]) VALUES (6, N'DM006', N'Giày tennis', N'Giày dành cho tennis và thể thao sân', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[danh_muc] WHERE [id] = 7)
        UPDATE [dbo].[danh_muc] SET [ma_danh_muc] = N'DM007', [ten_danh_muc] = N'Giày đi bộ', [mo_ta] = N'Giày nhẹ dùng để đi bộ hàng ngày', [trang_thai] = 1 WHERE [id] = 7;
    ELSE
        INSERT [dbo].[danh_muc] ([id], [ma_danh_muc], [ten_danh_muc], [mo_ta], [trang_thai]) VALUES (7, N'DM007', N'Giày đi bộ', N'Giày nhẹ dùng để đi bộ hàng ngày', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[danh_muc] WHERE [id] = 8)
        UPDATE [dbo].[danh_muc] SET [ma_danh_muc] = N'DM008', [ten_danh_muc] = N'Giày tập gym', [mo_ta] = N'Giày phục vụ tập luyện và fitness', [trang_thai] = 1 WHERE [id] = 8;
    ELSE
        INSERT [dbo].[danh_muc] ([id], [ma_danh_muc], [ten_danh_muc], [mo_ta], [trang_thai]) VALUES (8, N'DM008', N'Giày tập gym', N'Giày phục vụ tập luyện và fitness', 1);
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[danh_muc]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[danh_muc] OFF;

    -- DATA MERGE: [dbo].[dia_chi_khach_hang] (11 dong snapshot)
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[dia_chi_khach_hang]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[dia_chi_khach_hang] ON;
    IF EXISTS (SELECT 1 FROM [dbo].[dia_chi_khach_hang] WHERE [id] = 4)
        UPDATE [dbo].[dia_chi_khach_hang] SET [id_khach_hang] = 1, [ten_nguoi_nhan] = N'Trần Minh Bảo Hoàng', [sdt_nguoi_nhan] = N'0909899999', [tinh_thanh] = N'Hà Nội', [quan_huyen] = NULL, [phuong_xa] = N'Phường Ba Đình', [dia_chi_cu_the] = N'20 Đội Cấn - Địa chỉ demo', [loai_dia_chi] = 1, [is_mac_dinh] = 1, [trang_thai] = 1 WHERE [id] = 4;
    ELSE
        INSERT [dbo].[dia_chi_khach_hang] ([id], [id_khach_hang], [ten_nguoi_nhan], [sdt_nguoi_nhan], [tinh_thanh], [quan_huyen], [phuong_xa], [dia_chi_cu_the], [loai_dia_chi], [is_mac_dinh], [trang_thai]) VALUES (4, 1, N'Trần Minh Bảo Hoàng', N'0909899999', N'Hà Nội', NULL, N'Phường Ba Đình', N'20 Đội Cấn - Địa chỉ demo', 1, 1, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[dia_chi_khach_hang] WHERE [id] = 5)
        UPDATE [dbo].[dia_chi_khach_hang] SET [id_khach_hang] = 2, [ten_nguoi_nhan] = N'Nguyễn Thị An', [sdt_nguoi_nhan] = N'0911111111', [tinh_thanh] = N'Hà Nội', [quan_huyen] = NULL, [phuong_xa] = N'Phường Ba Đình', [dia_chi_cu_the] = N'20 Đội Cấn - Địa chỉ demo', [loai_dia_chi] = 1, [is_mac_dinh] = 1, [trang_thai] = 1 WHERE [id] = 5;
    ELSE
        INSERT [dbo].[dia_chi_khach_hang] ([id], [id_khach_hang], [ten_nguoi_nhan], [sdt_nguoi_nhan], [tinh_thanh], [quan_huyen], [phuong_xa], [dia_chi_cu_the], [loai_dia_chi], [is_mac_dinh], [trang_thai]) VALUES (5, 2, N'Nguyễn Thị An', N'0911111111', N'Hà Nội', NULL, N'Phường Ba Đình', N'20 Đội Cấn - Địa chỉ demo', 1, 1, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[dia_chi_khach_hang] WHERE [id] = 6)
        UPDATE [dbo].[dia_chi_khach_hang] SET [id_khach_hang] = 3, [ten_nguoi_nhan] = N'Lê Quốc Hưng', [sdt_nguoi_nhan] = N'0911111111', [tinh_thanh] = N'Hà Nội', [quan_huyen] = NULL, [phuong_xa] = N'Phường Ba Đình', [dia_chi_cu_the] = N'20 Đội Cấn - Địa chỉ demo', [loai_dia_chi] = 1, [is_mac_dinh] = 1, [trang_thai] = 1 WHERE [id] = 6;
    ELSE
        INSERT [dbo].[dia_chi_khach_hang] ([id], [id_khach_hang], [ten_nguoi_nhan], [sdt_nguoi_nhan], [tinh_thanh], [quan_huyen], [phuong_xa], [dia_chi_cu_the], [loai_dia_chi], [is_mac_dinh], [trang_thai]) VALUES (6, 3, N'Lê Quốc Hưng', N'0911111111', N'Hà Nội', NULL, N'Phường Ba Đình', N'20 Đội Cấn - Địa chỉ demo', 1, 1, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[dia_chi_khach_hang] WHERE [id] = 7)
        UPDATE [dbo].[dia_chi_khach_hang] SET [id_khach_hang] = 4, [ten_nguoi_nhan] = N'Phạm Thanh Tú', [sdt_nguoi_nhan] = N'0983214567', [tinh_thanh] = N'Hà Nội', [quan_huyen] = NULL, [phuong_xa] = N'Phường Ba Đình', [dia_chi_cu_the] = N'20 Đội Cấn - Địa chỉ demo', [loai_dia_chi] = 1, [is_mac_dinh] = 1, [trang_thai] = 1 WHERE [id] = 7;
    ELSE
        INSERT [dbo].[dia_chi_khach_hang] ([id], [id_khach_hang], [ten_nguoi_nhan], [sdt_nguoi_nhan], [tinh_thanh], [quan_huyen], [phuong_xa], [dia_chi_cu_the], [loai_dia_chi], [is_mac_dinh], [trang_thai]) VALUES (7, 4, N'Phạm Thanh Tú', N'0983214567', N'Hà Nội', NULL, N'Phường Ba Đình', N'20 Đội Cấn - Địa chỉ demo', 1, 1, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[dia_chi_khach_hang] WHERE [id] = 8)
        UPDATE [dbo].[dia_chi_khach_hang] SET [id_khach_hang] = 5, [ten_nguoi_nhan] = N'Võ Gia Hân', [sdt_nguoi_nhan] = N'0905882114', [tinh_thanh] = N'Hà Nội', [quan_huyen] = NULL, [phuong_xa] = N'Phường Ba Đình', [dia_chi_cu_the] = N'20 Đội Cấn - Địa chỉ demo', [loai_dia_chi] = 1, [is_mac_dinh] = 1, [trang_thai] = 1 WHERE [id] = 8;
    ELSE
        INSERT [dbo].[dia_chi_khach_hang] ([id], [id_khach_hang], [ten_nguoi_nhan], [sdt_nguoi_nhan], [tinh_thanh], [quan_huyen], [phuong_xa], [dia_chi_cu_the], [loai_dia_chi], [is_mac_dinh], [trang_thai]) VALUES (8, 5, N'Võ Gia Hân', N'0905882114', N'Hà Nội', NULL, N'Phường Ba Đình', N'20 Đội Cấn - Địa chỉ demo', 1, 1, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[dia_chi_khach_hang] WHERE [id] = 9)
        UPDATE [dbo].[dia_chi_khach_hang] SET [id_khach_hang] = 6, [ten_nguoi_nhan] = N'Đặng Hoài Nam', [sdt_nguoi_nhan] = N'0934625881', [tinh_thanh] = N'Hà Nội', [quan_huyen] = NULL, [phuong_xa] = N'Phường Ba Đình', [dia_chi_cu_the] = N'20 Đội Cấn - Địa chỉ demo', [loai_dia_chi] = 1, [is_mac_dinh] = 1, [trang_thai] = 1 WHERE [id] = 9;
    ELSE
        INSERT [dbo].[dia_chi_khach_hang] ([id], [id_khach_hang], [ten_nguoi_nhan], [sdt_nguoi_nhan], [tinh_thanh], [quan_huyen], [phuong_xa], [dia_chi_cu_the], [loai_dia_chi], [is_mac_dinh], [trang_thai]) VALUES (9, 6, N'Đặng Hoài Nam', N'0934625881', N'Hà Nội', NULL, N'Phường Ba Đình', N'20 Đội Cấn - Địa chỉ demo', 1, 1, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[dia_chi_khach_hang] WHERE [id] = 10)
        UPDATE [dbo].[dia_chi_khach_hang] SET [id_khach_hang] = 7, [ten_nguoi_nhan] = N'Bùi Mỹ Linh', [sdt_nguoi_nhan] = N'0972230456', [tinh_thanh] = N'Hà Nội', [quan_huyen] = NULL, [phuong_xa] = N'Phường Ba Đình', [dia_chi_cu_the] = N'20 Đội Cấn - Địa chỉ demo', [loai_dia_chi] = 1, [is_mac_dinh] = 1, [trang_thai] = 1 WHERE [id] = 10;
    ELSE
        INSERT [dbo].[dia_chi_khach_hang] ([id], [id_khach_hang], [ten_nguoi_nhan], [sdt_nguoi_nhan], [tinh_thanh], [quan_huyen], [phuong_xa], [dia_chi_cu_the], [loai_dia_chi], [is_mac_dinh], [trang_thai]) VALUES (10, 7, N'Bùi Mỹ Linh', N'0972230456', N'Hà Nội', NULL, N'Phường Ba Đình', N'20 Đội Cấn - Địa chỉ demo', 1, 1, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[dia_chi_khach_hang] WHERE [id] = 11)
        UPDATE [dbo].[dia_chi_khach_hang] SET [id_khach_hang] = 8, [ten_nguoi_nhan] = N'Ngô Đức Anh', [sdt_nguoi_nhan] = N'0902718663', [tinh_thanh] = N'Hà Nội', [quan_huyen] = NULL, [phuong_xa] = N'Phường Ba Đình', [dia_chi_cu_the] = N'20 Đội Cấn - Địa chỉ demo', [loai_dia_chi] = 1, [is_mac_dinh] = 1, [trang_thai] = 1 WHERE [id] = 11;
    ELSE
        INSERT [dbo].[dia_chi_khach_hang] ([id], [id_khach_hang], [ten_nguoi_nhan], [sdt_nguoi_nhan], [tinh_thanh], [quan_huyen], [phuong_xa], [dia_chi_cu_the], [loai_dia_chi], [is_mac_dinh], [trang_thai]) VALUES (11, 8, N'Ngô Đức Anh', N'0902718663', N'Hà Nội', NULL, N'Phường Ba Đình', N'20 Đội Cấn - Địa chỉ demo', 1, 1, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[dia_chi_khach_hang] WHERE [id] = 12)
        UPDATE [dbo].[dia_chi_khach_hang] SET [id_khach_hang] = 9, [ten_nguoi_nhan] = N'Đỗ Phương Vy', [sdt_nguoi_nhan] = N'0968440127', [tinh_thanh] = N'Hà Nội', [quan_huyen] = NULL, [phuong_xa] = N'Phường Ba Đình', [dia_chi_cu_the] = N'20 Đội Cấn - Địa chỉ demo', [loai_dia_chi] = 1, [is_mac_dinh] = 1, [trang_thai] = 1 WHERE [id] = 12;
    ELSE
        INSERT [dbo].[dia_chi_khach_hang] ([id], [id_khach_hang], [ten_nguoi_nhan], [sdt_nguoi_nhan], [tinh_thanh], [quan_huyen], [phuong_xa], [dia_chi_cu_the], [loai_dia_chi], [is_mac_dinh], [trang_thai]) VALUES (12, 9, N'Đỗ Phương Vy', N'0968440127', N'Hà Nội', NULL, N'Phường Ba Đình', N'20 Đội Cấn - Địa chỉ demo', 1, 1, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[dia_chi_khach_hang] WHERE [id] = 13)
        UPDATE [dbo].[dia_chi_khach_hang] SET [id_khach_hang] = 10, [ten_nguoi_nhan] = N'Mai Tiến Thành', [sdt_nguoi_nhan] = N'0918305902', [tinh_thanh] = N'Hà Nội', [quan_huyen] = NULL, [phuong_xa] = N'Phường Ba Đình', [dia_chi_cu_the] = N'20 Đội Cấn - Địa chỉ demo', [loai_dia_chi] = 1, [is_mac_dinh] = 1, [trang_thai] = 1 WHERE [id] = 13;
    ELSE
        INSERT [dbo].[dia_chi_khach_hang] ([id], [id_khach_hang], [ten_nguoi_nhan], [sdt_nguoi_nhan], [tinh_thanh], [quan_huyen], [phuong_xa], [dia_chi_cu_the], [loai_dia_chi], [is_mac_dinh], [trang_thai]) VALUES (13, 10, N'Mai Tiến Thành', N'0918305902', N'Hà Nội', NULL, N'Phường Ba Đình', N'20 Đội Cấn - Địa chỉ demo', 1, 1, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[dia_chi_khach_hang] WHERE [id] = 17)
        UPDATE [dbo].[dia_chi_khach_hang] SET [id_khach_hang] = 17, [ten_nguoi_nhan] = N'DEMO UI Khách hàng 20261007', [sdt_nguoi_nhan] = N'0998800001', [tinh_thanh] = N'Hà Nội', [quan_huyen] = NULL, [phuong_xa] = N'Phường Cầu Giấy', [dia_chi_cu_the] = N'30 Đội Cấn - địa chỉ demo UI đã kiểm tra', [loai_dia_chi] = 1, [is_mac_dinh] = 1, [trang_thai] = 1 WHERE [id] = 17;
    ELSE
        INSERT [dbo].[dia_chi_khach_hang] ([id], [id_khach_hang], [ten_nguoi_nhan], [sdt_nguoi_nhan], [tinh_thanh], [quan_huyen], [phuong_xa], [dia_chi_cu_the], [loai_dia_chi], [is_mac_dinh], [trang_thai]) VALUES (17, 17, N'DEMO UI Khách hàng 20261007', N'0998800001', N'Hà Nội', NULL, N'Phường Cầu Giấy', N'30 Đội Cấn - địa chỉ demo UI đã kiểm tra', 1, 1, 1);
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[dia_chi_khach_hang]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[dia_chi_khach_hang] OFF;

    -- DATA MERGE: [dbo].[dot_giam_gia] (4 dong snapshot)
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[dot_giam_gia]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[dot_giam_gia] ON;
    IF EXISTS (SELECT 1 FROM [dbo].[dot_giam_gia] WHERE [id] = 1)
        UPDATE [dbo].[dot_giam_gia] SET [ma_dot_giam_gia] = N'DGG001', [ten_dot_giam_gia] = N'DEMO - Đợt giảm giá đang diễn ra', [phan_tram_giam_dot] = CAST(10.00 AS Decimal(18, 2)), [ngay_bat_dau] = CAST(N'2026-10-06T00:00:00.0000000' AS DateTime2), [ngay_ket_thuc] = CAST(N'2026-10-12T23:59:59.0000000' AS DateTime2), [kich_hoat] = 1, [ngay_tao] = CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), [trang_thai] = 1 WHERE [id] = 1;
    ELSE
        INSERT [dbo].[dot_giam_gia] ([id], [ma_dot_giam_gia], [ten_dot_giam_gia], [phan_tram_giam_dot], [ngay_bat_dau], [ngay_ket_thuc], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (1, N'DGG001', N'DEMO - Đợt giảm giá đang diễn ra', CAST(10.00 AS Decimal(18, 2)), CAST(N'2026-10-06T00:00:00.0000000' AS DateTime2), CAST(N'2026-10-12T23:59:59.0000000' AS DateTime2), 1, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), 1);
    IF EXISTS (SELECT 1 FROM [dbo].[dot_giam_gia] WHERE [id] = 2)
        UPDATE [dbo].[dot_giam_gia] SET [ma_dot_giam_gia] = N'DGG002', [ten_dot_giam_gia] = N'DEMO - Đợt giảm giá sắp diễn ra', [phan_tram_giam_dot] = CAST(15.00 AS Decimal(18, 2)), [ngay_bat_dau] = CAST(N'2026-10-14T00:00:00.0000000' AS DateTime2), [ngay_ket_thuc] = CAST(N'2026-10-21T23:59:59.0000000' AS DateTime2), [kich_hoat] = 1, [ngay_tao] = CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), [trang_thai] = 1 WHERE [id] = 2;
    ELSE
        INSERT [dbo].[dot_giam_gia] ([id], [ma_dot_giam_gia], [ten_dot_giam_gia], [phan_tram_giam_dot], [ngay_bat_dau], [ngay_ket_thuc], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (2, N'DGG002', N'DEMO - Đợt giảm giá sắp diễn ra', CAST(15.00 AS Decimal(18, 2)), CAST(N'2026-10-14T00:00:00.0000000' AS DateTime2), CAST(N'2026-10-21T23:59:59.0000000' AS DateTime2), 1, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), 1);
    IF EXISTS (SELECT 1 FROM [dbo].[dot_giam_gia] WHERE [id] = 3)
        UPDATE [dbo].[dot_giam_gia] SET [ma_dot_giam_gia] = N'DGG003', [ten_dot_giam_gia] = N'DEMO - Đợt giảm giá đã kết thúc', [phan_tram_giam_dot] = CAST(5.00 AS Decimal(18, 2)), [ngay_bat_dau] = CAST(N'2026-09-17T00:00:00.0000000' AS DateTime2), [ngay_ket_thuc] = CAST(N'2026-10-05T23:59:59.0000000' AS DateTime2), [kich_hoat] = 1, [ngay_tao] = CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), [trang_thai] = 1 WHERE [id] = 3;
    ELSE
        INSERT [dbo].[dot_giam_gia] ([id], [ma_dot_giam_gia], [ten_dot_giam_gia], [phan_tram_giam_dot], [ngay_bat_dau], [ngay_ket_thuc], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (3, N'DGG003', N'DEMO - Đợt giảm giá đã kết thúc', CAST(5.00 AS Decimal(18, 2)), CAST(N'2026-09-17T00:00:00.0000000' AS DateTime2), CAST(N'2026-10-05T23:59:59.0000000' AS DateTime2), 1, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), 1);
    IF EXISTS (SELECT 1 FROM [dbo].[dot_giam_gia] WHERE [id] = 5)
        UPDATE [dbo].[dot_giam_gia] SET [ma_dot_giam_gia] = N'DGG004', [ten_dot_giam_gia] = N'DEMO UI 20261007 - Đợt đã kiểm tra', [phan_tram_giam_dot] = CAST(3.00 AS Decimal(18, 2)), [ngay_bat_dau] = CAST(N'2026-10-06T00:00:00.0000000' AS DateTime2), [ngay_ket_thuc] = CAST(N'2026-10-14T23:59:59.0000000' AS DateTime2), [kich_hoat] = 1, [ngay_tao] = CAST(N'2026-10-07T02:12:06.2739715' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-07T02:18:48.4142832' AS DateTime2), [trang_thai] = 1 WHERE [id] = 5;
    ELSE
        INSERT [dbo].[dot_giam_gia] ([id], [ma_dot_giam_gia], [ten_dot_giam_gia], [phan_tram_giam_dot], [ngay_bat_dau], [ngay_ket_thuc], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (5, N'DGG004', N'DEMO UI 20261007 - Đợt đã kiểm tra', CAST(3.00 AS Decimal(18, 2)), CAST(N'2026-10-06T00:00:00.0000000' AS DateTime2), CAST(N'2026-10-14T23:59:59.0000000' AS DateTime2), 1, CAST(N'2026-10-07T02:12:06.2739715' AS DateTime2), CAST(N'2026-10-07T02:18:48.4142832' AS DateTime2), 1);
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[dot_giam_gia]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[dot_giam_gia] OFF;

    -- DATA MERGE: [dbo].[hinh_anh_san_pham] (4 dong snapshot)
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[hinh_anh_san_pham]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[hinh_anh_san_pham] ON;
    IF EXISTS (SELECT 1 FROM [dbo].[hinh_anh_san_pham] WHERE [id] = 37)
        UPDATE [dbo].[hinh_anh_san_pham] SET [id_san_pham] = 4, [url_anh] = N'/uploads/products/4fd52df5-f621-4f62-ab6a-3a1d978765c8.png', [is_anh_chinh] = 1 WHERE [id] = 37;
    ELSE
        INSERT [dbo].[hinh_anh_san_pham] ([id], [id_san_pham], [url_anh], [is_anh_chinh]) VALUES (37, 4, N'/uploads/products/4fd52df5-f621-4f62-ab6a-3a1d978765c8.png', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[hinh_anh_san_pham] WHERE [id] = 56)
        UPDATE [dbo].[hinh_anh_san_pham] SET [id_san_pham] = 3, [url_anh] = N'/uploads/products/d00ce3b7-d173-4762-939f-d3fd13ded48a.png', [is_anh_chinh] = 1 WHERE [id] = 56;
    ELSE
        INSERT [dbo].[hinh_anh_san_pham] ([id], [id_san_pham], [url_anh], [is_anh_chinh]) VALUES (56, 3, N'/uploads/products/d00ce3b7-d173-4762-939f-d3fd13ded48a.png', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[hinh_anh_san_pham] WHERE [id] = 57)
        UPDATE [dbo].[hinh_anh_san_pham] SET [id_san_pham] = 88, [url_anh] = N'/uploads/products/eb29524e-74b6-4e41-b376-a3c18739e0bc.png', [is_anh_chinh] = 1 WHERE [id] = 57;
    ELSE
        INSERT [dbo].[hinh_anh_san_pham] ([id], [id_san_pham], [url_anh], [is_anh_chinh]) VALUES (57, 88, N'/uploads/products/eb29524e-74b6-4e41-b376-a3c18739e0bc.png', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[hinh_anh_san_pham] WHERE [id] = 58)
        UPDATE [dbo].[hinh_anh_san_pham] SET [id_san_pham] = 88, [url_anh] = N'/uploads/products/c2eb9d6f-bc97-4d80-ac77-85f76b16fc5e.png', [is_anh_chinh] = 0 WHERE [id] = 58;
    ELSE
        INSERT [dbo].[hinh_anh_san_pham] ([id], [id_san_pham], [url_anh], [is_anh_chinh]) VALUES (58, 88, N'/uploads/products/c2eb9d6f-bc97-4d80-ac77-85f76b16fc5e.png', 0);
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[hinh_anh_san_pham]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[hinh_anh_san_pham] OFF;

    -- DATA MERGE: [dbo].[hinh_thuc_thanh_toan] (2 dong snapshot)
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[hinh_thuc_thanh_toan]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[hinh_thuc_thanh_toan] ON;
    IF EXISTS (SELECT 1 FROM [dbo].[hinh_thuc_thanh_toan] WHERE [id] = 1)
        UPDATE [dbo].[hinh_thuc_thanh_toan] SET [ma_hinh_thuc] = N'TRUC_TIEP', [ten_hinh_thuc] = N'Trực tiếp', [trang_thai] = 1 WHERE [id] = 1;
    ELSE
        INSERT [dbo].[hinh_thuc_thanh_toan] ([id], [ma_hinh_thuc], [ten_hinh_thuc], [trang_thai]) VALUES (1, N'TRUC_TIEP', N'Trực tiếp', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[hinh_thuc_thanh_toan] WHERE [id] = 2)
        UPDATE [dbo].[hinh_thuc_thanh_toan] SET [ma_hinh_thuc] = N'TRUC_TUYEN', [ten_hinh_thuc] = N'Trực tuyến', [trang_thai] = 1 WHERE [id] = 2;
    ELSE
        INSERT [dbo].[hinh_thuc_thanh_toan] ([id], [ma_hinh_thuc], [ten_hinh_thuc], [trang_thai]) VALUES (2, N'TRUC_TUYEN', N'Trực tuyến', 1);
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[hinh_thuc_thanh_toan]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[hinh_thuc_thanh_toan] OFF;

    -- DATA MERGE: [dbo].[hoa_don] (10 dong snapshot)
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[hoa_don]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[hoa_don] ON;
    IF EXISTS (SELECT 1 FROM [dbo].[hoa_don] WHERE [id] = 1)
        UPDATE [dbo].[hoa_don] SET [id_khach_hang] = 1, [id_nhan_vien] = 6, [id_phieu_giam_gia] = NULL, [id_phuong_thuc_thanh_toan] = 1, [ma_hoa_don] = N'HD000001', [loai_hoa_don] = 0, [tong_tien] = CAST(600000.00 AS Decimal(18, 2)), [phi_van_chuyen] = CAST(0.00 AS Decimal(18, 2)), [thanh_tien] = CAST(600000.00 AS Decimal(18, 2)), [don_vi_van_chuyen] = NULL, [ho_ten_nguoi_nhan] = N'Trần Minh Bảo Hoàng', [so_dien_thoai_nguoi_nhan] = N'0909899999', [gia_chi_giao_hang] = NULL, [ghi_chu] = N'DEMO-STABILIZATION-01', [ngay_thanh_toan] = CAST(N'2026-09-27T03:49:07.8364510' AS DateTime2), [ngay_tao] = CAST(N'2026-09-27T01:49:07.8364510' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-09-27T03:49:07.8364510' AS DateTime2), [trang_thai] = 5 WHERE [id] = 1;
    ELSE
        INSERT [dbo].[hoa_don] ([id], [id_khach_hang], [id_nhan_vien], [id_phieu_giam_gia], [id_phuong_thuc_thanh_toan], [ma_hoa_don], [loai_hoa_don], [tong_tien], [phi_van_chuyen], [thanh_tien], [don_vi_van_chuyen], [ho_ten_nguoi_nhan], [so_dien_thoai_nguoi_nhan], [gia_chi_giao_hang], [ghi_chu], [ngay_thanh_toan], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (1, 1, 6, NULL, 1, N'HD000001', 0, CAST(600000.00 AS Decimal(18, 2)), CAST(0.00 AS Decimal(18, 2)), CAST(600000.00 AS Decimal(18, 2)), NULL, N'Trần Minh Bảo Hoàng', N'0909899999', NULL, N'DEMO-STABILIZATION-01', CAST(N'2026-09-27T03:49:07.8364510' AS DateTime2), CAST(N'2026-09-27T01:49:07.8364510' AS DateTime2), CAST(N'2026-09-27T03:49:07.8364510' AS DateTime2), 5);
    IF EXISTS (SELECT 1 FROM [dbo].[hoa_don] WHERE [id] = 2)
        UPDATE [dbo].[hoa_don] SET [id_khach_hang] = 2, [id_nhan_vien] = 7, [id_phieu_giam_gia] = NULL, [id_phuong_thuc_thanh_toan] = 3, [ma_hoa_don] = N'HD000002', [loai_hoa_don] = 1, [tong_tien] = CAST(300123.00 AS Decimal(18, 2)), [phi_van_chuyen] = CAST(30000.00 AS Decimal(18, 2)), [thanh_tien] = CAST(330123.00 AS Decimal(18, 2)), [don_vi_van_chuyen] = N'Giao hàng demo', [ho_ten_nguoi_nhan] = N'Nguyễn Thị An', [so_dien_thoai_nguoi_nhan] = N'0911111111', [gia_chi_giao_hang] = NULL, [ghi_chu] = N'DEMO-STABILIZATION-02', [ngay_thanh_toan] = CAST(N'2026-09-28T03:49:07.8364510' AS DateTime2), [ngay_tao] = CAST(N'2026-09-28T01:49:07.8364510' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-09-28T03:49:07.8364510' AS DateTime2), [trang_thai] = 5 WHERE [id] = 2;
    ELSE
        INSERT [dbo].[hoa_don] ([id], [id_khach_hang], [id_nhan_vien], [id_phieu_giam_gia], [id_phuong_thuc_thanh_toan], [ma_hoa_don], [loai_hoa_don], [tong_tien], [phi_van_chuyen], [thanh_tien], [don_vi_van_chuyen], [ho_ten_nguoi_nhan], [so_dien_thoai_nguoi_nhan], [gia_chi_giao_hang], [ghi_chu], [ngay_thanh_toan], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (2, 2, 7, NULL, 3, N'HD000002', 1, CAST(300123.00 AS Decimal(18, 2)), CAST(30000.00 AS Decimal(18, 2)), CAST(330123.00 AS Decimal(18, 2)), N'Giao hàng demo', N'Nguyễn Thị An', N'0911111111', NULL, N'DEMO-STABILIZATION-02', CAST(N'2026-09-28T03:49:07.8364510' AS DateTime2), CAST(N'2026-09-28T01:49:07.8364510' AS DateTime2), CAST(N'2026-09-28T03:49:07.8364510' AS DateTime2), 5);
    IF EXISTS (SELECT 1 FROM [dbo].[hoa_don] WHERE [id] = 3)
        UPDATE [dbo].[hoa_don] SET [id_khach_hang] = 3, [id_nhan_vien] = 8, [id_phieu_giam_gia] = NULL, [id_phuong_thuc_thanh_toan] = 3, [ma_hoa_don] = N'HD000003', [loai_hoa_don] = 2, [tong_tien] = CAST(246.00 AS Decimal(18, 2)), [phi_van_chuyen] = CAST(30000.00 AS Decimal(18, 2)), [thanh_tien] = CAST(30246.00 AS Decimal(18, 2)), [don_vi_van_chuyen] = N'Giao hàng demo', [ho_ten_nguoi_nhan] = N'Lê Quốc Hưng', [so_dien_thoai_nguoi_nhan] = N'0911111111', [gia_chi_giao_hang] = NULL, [ghi_chu] = N'DEMO-STABILIZATION-03', [ngay_thanh_toan] = CAST(N'2026-09-29T03:49:07.8364510' AS DateTime2), [ngay_tao] = CAST(N'2026-09-29T01:49:07.8364510' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-09-29T03:49:07.8364510' AS DateTime2), [trang_thai] = 5 WHERE [id] = 3;
    ELSE
        INSERT [dbo].[hoa_don] ([id], [id_khach_hang], [id_nhan_vien], [id_phieu_giam_gia], [id_phuong_thuc_thanh_toan], [ma_hoa_don], [loai_hoa_don], [tong_tien], [phi_van_chuyen], [thanh_tien], [don_vi_van_chuyen], [ho_ten_nguoi_nhan], [so_dien_thoai_nguoi_nhan], [gia_chi_giao_hang], [ghi_chu], [ngay_thanh_toan], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (3, 3, 8, NULL, 3, N'HD000003', 2, CAST(246.00 AS Decimal(18, 2)), CAST(30000.00 AS Decimal(18, 2)), CAST(30246.00 AS Decimal(18, 2)), N'Giao hàng demo', N'Lê Quốc Hưng', N'0911111111', NULL, N'DEMO-STABILIZATION-03', CAST(N'2026-09-29T03:49:07.8364510' AS DateTime2), CAST(N'2026-09-29T01:49:07.8364510' AS DateTime2), CAST(N'2026-09-29T03:49:07.8364510' AS DateTime2), 5);
    IF EXISTS (SELECT 1 FROM [dbo].[hoa_don] WHERE [id] = 4)
        UPDATE [dbo].[hoa_don] SET [id_khach_hang] = 4, [id_nhan_vien] = 1, [id_phieu_giam_gia] = NULL, [id_phuong_thuc_thanh_toan] = 1, [ma_hoa_don] = N'HD000004', [loai_hoa_don] = 0, [tong_tien] = CAST(300123.00 AS Decimal(18, 2)), [phi_van_chuyen] = CAST(0.00 AS Decimal(18, 2)), [thanh_tien] = CAST(300123.00 AS Decimal(18, 2)), [don_vi_van_chuyen] = NULL, [ho_ten_nguoi_nhan] = N'Phạm Thanh Tú', [so_dien_thoai_nguoi_nhan] = N'0983214567', [gia_chi_giao_hang] = NULL, [ghi_chu] = N'DEMO-STABILIZATION-04', [ngay_thanh_toan] = CAST(N'2026-09-30T03:49:07.8364510' AS DateTime2), [ngay_tao] = CAST(N'2026-09-30T01:49:07.8364510' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-09-30T03:49:07.8364510' AS DateTime2), [trang_thai] = 5 WHERE [id] = 4;
    ELSE
        INSERT [dbo].[hoa_don] ([id], [id_khach_hang], [id_nhan_vien], [id_phieu_giam_gia], [id_phuong_thuc_thanh_toan], [ma_hoa_don], [loai_hoa_don], [tong_tien], [phi_van_chuyen], [thanh_tien], [don_vi_van_chuyen], [ho_ten_nguoi_nhan], [so_dien_thoai_nguoi_nhan], [gia_chi_giao_hang], [ghi_chu], [ngay_thanh_toan], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (4, 4, 1, NULL, 1, N'HD000004', 0, CAST(300123.00 AS Decimal(18, 2)), CAST(0.00 AS Decimal(18, 2)), CAST(300123.00 AS Decimal(18, 2)), NULL, N'Phạm Thanh Tú', N'0983214567', NULL, N'DEMO-STABILIZATION-04', CAST(N'2026-09-30T03:49:07.8364510' AS DateTime2), CAST(N'2026-09-30T01:49:07.8364510' AS DateTime2), CAST(N'2026-09-30T03:49:07.8364510' AS DateTime2), 5);
    IF EXISTS (SELECT 1 FROM [dbo].[hoa_don] WHERE [id] = 5)
        UPDATE [dbo].[hoa_don] SET [id_khach_hang] = 5, [id_nhan_vien] = 2, [id_phieu_giam_gia] = NULL, [id_phuong_thuc_thanh_toan] = 3, [ma_hoa_don] = N'HD000005', [loai_hoa_don] = 1, [tong_tien] = CAST(600000.00 AS Decimal(18, 2)), [phi_van_chuyen] = CAST(30000.00 AS Decimal(18, 2)), [thanh_tien] = CAST(630000.00 AS Decimal(18, 2)), [don_vi_van_chuyen] = N'Giao hàng demo', [ho_ten_nguoi_nhan] = N'Võ Gia Hân', [so_dien_thoai_nguoi_nhan] = N'0905882114', [gia_chi_giao_hang] = NULL, [ghi_chu] = N'DEMO-STABILIZATION-05', [ngay_thanh_toan] = CAST(N'2026-10-01T03:49:07.8364510' AS DateTime2), [ngay_tao] = CAST(N'2026-10-01T01:49:07.8364510' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-01T03:49:07.8364510' AS DateTime2), [trang_thai] = 5 WHERE [id] = 5;
    ELSE
        INSERT [dbo].[hoa_don] ([id], [id_khach_hang], [id_nhan_vien], [id_phieu_giam_gia], [id_phuong_thuc_thanh_toan], [ma_hoa_don], [loai_hoa_don], [tong_tien], [phi_van_chuyen], [thanh_tien], [don_vi_van_chuyen], [ho_ten_nguoi_nhan], [so_dien_thoai_nguoi_nhan], [gia_chi_giao_hang], [ghi_chu], [ngay_thanh_toan], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (5, 5, 2, NULL, 3, N'HD000005', 1, CAST(600000.00 AS Decimal(18, 2)), CAST(30000.00 AS Decimal(18, 2)), CAST(630000.00 AS Decimal(18, 2)), N'Giao hàng demo', N'Võ Gia Hân', N'0905882114', NULL, N'DEMO-STABILIZATION-05', CAST(N'2026-10-01T03:49:07.8364510' AS DateTime2), CAST(N'2026-10-01T01:49:07.8364510' AS DateTime2), CAST(N'2026-10-01T03:49:07.8364510' AS DateTime2), 5);
    IF EXISTS (SELECT 1 FROM [dbo].[hoa_don] WHERE [id] = 6)
        UPDATE [dbo].[hoa_don] SET [id_khach_hang] = 6, [id_nhan_vien] = 3, [id_phieu_giam_gia] = NULL, [id_phuong_thuc_thanh_toan] = 4, [ma_hoa_don] = N'HD000006', [loai_hoa_don] = 1, [tong_tien] = CAST(600000.00 AS Decimal(18, 2)), [phi_van_chuyen] = CAST(30000.00 AS Decimal(18, 2)), [thanh_tien] = CAST(630000.00 AS Decimal(18, 2)), [don_vi_van_chuyen] = N'Giao hàng demo', [ho_ten_nguoi_nhan] = N'Đặng Hoài Nam', [so_dien_thoai_nguoi_nhan] = N'0934625881', [gia_chi_giao_hang] = NULL, [ghi_chu] = N'DEMO-STABILIZATION-06', [ngay_thanh_toan] = NULL, [ngay_tao] = CAST(N'2026-10-02T01:49:07.8364510' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-07T02:07:18.3124557' AS DateTime2), [trang_thai] = 1 WHERE [id] = 6;
    ELSE
        INSERT [dbo].[hoa_don] ([id], [id_khach_hang], [id_nhan_vien], [id_phieu_giam_gia], [id_phuong_thuc_thanh_toan], [ma_hoa_don], [loai_hoa_don], [tong_tien], [phi_van_chuyen], [thanh_tien], [don_vi_van_chuyen], [ho_ten_nguoi_nhan], [so_dien_thoai_nguoi_nhan], [gia_chi_giao_hang], [ghi_chu], [ngay_thanh_toan], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (6, 6, 3, NULL, 4, N'HD000006', 1, CAST(600000.00 AS Decimal(18, 2)), CAST(30000.00 AS Decimal(18, 2)), CAST(630000.00 AS Decimal(18, 2)), N'Giao hàng demo', N'Đặng Hoài Nam', N'0934625881', NULL, N'DEMO-STABILIZATION-06', NULL, CAST(N'2026-10-02T01:49:07.8364510' AS DateTime2), CAST(N'2026-10-07T02:07:18.3124557' AS DateTime2), 1);
    IF EXISTS (SELECT 1 FROM [dbo].[hoa_don] WHERE [id] = 7)
        UPDATE [dbo].[hoa_don] SET [id_khach_hang] = 7, [id_nhan_vien] = 6, [id_phieu_giam_gia] = NULL, [id_phuong_thuc_thanh_toan] = 4, [ma_hoa_don] = N'HD000007', [loai_hoa_don] = 1, [tong_tien] = CAST(400000.00 AS Decimal(18, 2)), [phi_van_chuyen] = CAST(30000.00 AS Decimal(18, 2)), [thanh_tien] = CAST(430000.00 AS Decimal(18, 2)), [don_vi_van_chuyen] = N'Giao hàng demo', [ho_ten_nguoi_nhan] = N'Bùi Mỹ Linh', [so_dien_thoai_nguoi_nhan] = N'0972230456', [gia_chi_giao_hang] = NULL, [ghi_chu] = N'DEMO-STABILIZATION-07', [ngay_thanh_toan] = NULL, [ngay_tao] = CAST(N'2026-10-03T01:49:07.8364510' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-03T03:49:07.8364510' AS DateTime2), [trang_thai] = 1 WHERE [id] = 7;
    ELSE
        INSERT [dbo].[hoa_don] ([id], [id_khach_hang], [id_nhan_vien], [id_phieu_giam_gia], [id_phuong_thuc_thanh_toan], [ma_hoa_don], [loai_hoa_don], [tong_tien], [phi_van_chuyen], [thanh_tien], [don_vi_van_chuyen], [ho_ten_nguoi_nhan], [so_dien_thoai_nguoi_nhan], [gia_chi_giao_hang], [ghi_chu], [ngay_thanh_toan], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (7, 7, 6, NULL, 4, N'HD000007', 1, CAST(400000.00 AS Decimal(18, 2)), CAST(30000.00 AS Decimal(18, 2)), CAST(430000.00 AS Decimal(18, 2)), N'Giao hàng demo', N'Bùi Mỹ Linh', N'0972230456', NULL, N'DEMO-STABILIZATION-07', NULL, CAST(N'2026-10-03T01:49:07.8364510' AS DateTime2), CAST(N'2026-10-03T03:49:07.8364510' AS DateTime2), 1);
    IF EXISTS (SELECT 1 FROM [dbo].[hoa_don] WHERE [id] = 8)
        UPDATE [dbo].[hoa_don] SET [id_khach_hang] = 8, [id_nhan_vien] = 7, [id_phieu_giam_gia] = NULL, [id_phuong_thuc_thanh_toan] = 4, [ma_hoa_don] = N'HD000008', [loai_hoa_don] = 2, [tong_tien] = CAST(200000.00 AS Decimal(18, 2)), [phi_van_chuyen] = CAST(30000.00 AS Decimal(18, 2)), [thanh_tien] = CAST(230000.00 AS Decimal(18, 2)), [don_vi_van_chuyen] = N'Giao hàng demo', [ho_ten_nguoi_nhan] = N'Ngô Đức Anh', [so_dien_thoai_nguoi_nhan] = N'0902718663', [gia_chi_giao_hang] = NULL, [ghi_chu] = N'DEMO-STABILIZATION-08', [ngay_thanh_toan] = NULL, [ngay_tao] = CAST(N'2026-10-04T01:49:07.8364510' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-04T03:49:07.8364510' AS DateTime2), [trang_thai] = 3 WHERE [id] = 8;
    ELSE
        INSERT [dbo].[hoa_don] ([id], [id_khach_hang], [id_nhan_vien], [id_phieu_giam_gia], [id_phuong_thuc_thanh_toan], [ma_hoa_don], [loai_hoa_don], [tong_tien], [phi_van_chuyen], [thanh_tien], [don_vi_van_chuyen], [ho_ten_nguoi_nhan], [so_dien_thoai_nguoi_nhan], [gia_chi_giao_hang], [ghi_chu], [ngay_thanh_toan], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (8, 8, 7, NULL, 4, N'HD000008', 2, CAST(200000.00 AS Decimal(18, 2)), CAST(30000.00 AS Decimal(18, 2)), CAST(230000.00 AS Decimal(18, 2)), N'Giao hàng demo', N'Ngô Đức Anh', N'0902718663', NULL, N'DEMO-STABILIZATION-08', NULL, CAST(N'2026-10-04T01:49:07.8364510' AS DateTime2), CAST(N'2026-10-04T03:49:07.8364510' AS DateTime2), 3);
    IF EXISTS (SELECT 1 FROM [dbo].[hoa_don] WHERE [id] = 9)
        UPDATE [dbo].[hoa_don] SET [id_khach_hang] = 9, [id_nhan_vien] = 8, [id_phieu_giam_gia] = NULL, [id_phuong_thuc_thanh_toan] = 4, [ma_hoa_don] = N'HD000009', [loai_hoa_don] = 2, [tong_tien] = CAST(1699000.00 AS Decimal(18, 2)), [phi_van_chuyen] = CAST(30000.00 AS Decimal(18, 2)), [thanh_tien] = CAST(1729000.00 AS Decimal(18, 2)), [don_vi_van_chuyen] = N'Giao hàng demo', [ho_ten_nguoi_nhan] = N'Đỗ Phương Vy', [so_dien_thoai_nguoi_nhan] = N'0968440127', [gia_chi_giao_hang] = NULL, [ghi_chu] = N'DEMO-STABILIZATION-09', [ngay_thanh_toan] = NULL, [ngay_tao] = CAST(N'2026-10-05T01:49:07.8364510' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-05T03:49:07.8364510' AS DateTime2), [trang_thai] = 4 WHERE [id] = 9;
    ELSE
        INSERT [dbo].[hoa_don] ([id], [id_khach_hang], [id_nhan_vien], [id_phieu_giam_gia], [id_phuong_thuc_thanh_toan], [ma_hoa_don], [loai_hoa_don], [tong_tien], [phi_van_chuyen], [thanh_tien], [don_vi_van_chuyen], [ho_ten_nguoi_nhan], [so_dien_thoai_nguoi_nhan], [gia_chi_giao_hang], [ghi_chu], [ngay_thanh_toan], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (9, 9, 8, NULL, 4, N'HD000009', 2, CAST(1699000.00 AS Decimal(18, 2)), CAST(30000.00 AS Decimal(18, 2)), CAST(1729000.00 AS Decimal(18, 2)), N'Giao hàng demo', N'Đỗ Phương Vy', N'0968440127', NULL, N'DEMO-STABILIZATION-09', NULL, CAST(N'2026-10-05T01:49:07.8364510' AS DateTime2), CAST(N'2026-10-05T03:49:07.8364510' AS DateTime2), 4);
    IF EXISTS (SELECT 1 FROM [dbo].[hoa_don] WHERE [id] = 10)
        UPDATE [dbo].[hoa_don] SET [id_khach_hang] = 10, [id_nhan_vien] = 1, [id_phieu_giam_gia] = NULL, [id_phuong_thuc_thanh_toan] = 4, [ma_hoa_don] = N'HD000010', [loai_hoa_don] = 1, [tong_tien] = CAST(1799000.00 AS Decimal(18, 2)), [phi_van_chuyen] = CAST(30000.00 AS Decimal(18, 2)), [thanh_tien] = CAST(1829000.00 AS Decimal(18, 2)), [don_vi_van_chuyen] = N'Giao hàng demo', [ho_ten_nguoi_nhan] = N'Mai Tiến Thành', [so_dien_thoai_nguoi_nhan] = N'0918305902', [gia_chi_giao_hang] = NULL, [ghi_chu] = N'DEMO-STABILIZATION-10', [ngay_thanh_toan] = NULL, [ngay_tao] = CAST(N'2026-10-06T01:49:07.8364510' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-06T03:49:07.8364510' AS DateTime2), [trang_thai] = 6 WHERE [id] = 10;
    ELSE
        INSERT [dbo].[hoa_don] ([id], [id_khach_hang], [id_nhan_vien], [id_phieu_giam_gia], [id_phuong_thuc_thanh_toan], [ma_hoa_don], [loai_hoa_don], [tong_tien], [phi_van_chuyen], [thanh_tien], [don_vi_van_chuyen], [ho_ten_nguoi_nhan], [so_dien_thoai_nguoi_nhan], [gia_chi_giao_hang], [ghi_chu], [ngay_thanh_toan], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (10, 10, 1, NULL, 4, N'HD000010', 1, CAST(1799000.00 AS Decimal(18, 2)), CAST(30000.00 AS Decimal(18, 2)), CAST(1829000.00 AS Decimal(18, 2)), N'Giao hàng demo', N'Mai Tiến Thành', N'0918305902', NULL, N'DEMO-STABILIZATION-10', NULL, CAST(N'2026-10-06T01:49:07.8364510' AS DateTime2), CAST(N'2026-10-06T03:49:07.8364510' AS DateTime2), 6);
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[hoa_don]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[hoa_don] OFF;

    -- DATA MERGE: [dbo].[hoa_don_chi_tiet] (20 dong snapshot)
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[hoa_don_chi_tiet]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[hoa_don_chi_tiet] ON;
    IF EXISTS (SELECT 1 FROM [dbo].[hoa_don_chi_tiet] WHERE [id] = 0)
        UPDATE [dbo].[hoa_don_chi_tiet] SET [id_hoa_don] = 1, [id_san_pham_chi_tiet] = 15, [so_luong] = 1, [don_gia] = CAST(300000.00 AS Decimal(18, 2)), [thanh_tien] = CAST(300000.00 AS Decimal(18, 2)), [ghi_chu] = N'Chi tiết demo; không điều chỉnh tồn kho', [trang_thai] = 1 WHERE [id] = 0;
    ELSE
        INSERT [dbo].[hoa_don_chi_tiet] ([id], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong], [don_gia], [thanh_tien], [ghi_chu], [trang_thai]) VALUES (0, 1, 15, 1, CAST(300000.00 AS Decimal(18, 2)), CAST(300000.00 AS Decimal(18, 2)), N'Chi tiết demo; không điều chỉnh tồn kho', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[hoa_don_chi_tiet] WHERE [id] = 1)
        UPDATE [dbo].[hoa_don_chi_tiet] SET [id_hoa_don] = 1, [id_san_pham_chi_tiet] = 14, [so_luong] = 1, [don_gia] = CAST(300000.00 AS Decimal(18, 2)), [thanh_tien] = CAST(300000.00 AS Decimal(18, 2)), [ghi_chu] = N'Chi tiết demo; không điều chỉnh tồn kho', [trang_thai] = 1 WHERE [id] = 1;
    ELSE
        INSERT [dbo].[hoa_don_chi_tiet] ([id], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong], [don_gia], [thanh_tien], [ghi_chu], [trang_thai]) VALUES (1, 1, 14, 1, CAST(300000.00 AS Decimal(18, 2)), CAST(300000.00 AS Decimal(18, 2)), N'Chi tiết demo; không điều chỉnh tồn kho', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[hoa_don_chi_tiet] WHERE [id] = 2)
        UPDATE [dbo].[hoa_don_chi_tiet] SET [id_hoa_don] = 2, [id_san_pham_chi_tiet] = 13, [so_luong] = 1, [don_gia] = CAST(300000.00 AS Decimal(18, 2)), [thanh_tien] = CAST(300000.00 AS Decimal(18, 2)), [ghi_chu] = N'Chi tiết demo; không điều chỉnh tồn kho', [trang_thai] = 1 WHERE [id] = 2;
    ELSE
        INSERT [dbo].[hoa_don_chi_tiet] ([id], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong], [don_gia], [thanh_tien], [ghi_chu], [trang_thai]) VALUES (2, 2, 13, 1, CAST(300000.00 AS Decimal(18, 2)), CAST(300000.00 AS Decimal(18, 2)), N'Chi tiết demo; không điều chỉnh tồn kho', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[hoa_don_chi_tiet] WHERE [id] = 3)
        UPDATE [dbo].[hoa_don_chi_tiet] SET [id_hoa_don] = 2, [id_san_pham_chi_tiet] = 6, [so_luong] = 1, [don_gia] = CAST(123.00 AS Decimal(18, 2)), [thanh_tien] = CAST(123.00 AS Decimal(18, 2)), [ghi_chu] = N'Chi tiết demo; không điều chỉnh tồn kho', [trang_thai] = 1 WHERE [id] = 3;
    ELSE
        INSERT [dbo].[hoa_don_chi_tiet] ([id], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong], [don_gia], [thanh_tien], [ghi_chu], [trang_thai]) VALUES (3, 2, 6, 1, CAST(123.00 AS Decimal(18, 2)), CAST(123.00 AS Decimal(18, 2)), N'Chi tiết demo; không điều chỉnh tồn kho', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[hoa_don_chi_tiet] WHERE [id] = 4)
        UPDATE [dbo].[hoa_don_chi_tiet] SET [id_hoa_don] = 3, [id_san_pham_chi_tiet] = 5, [so_luong] = 1, [don_gia] = CAST(123.00 AS Decimal(18, 2)), [thanh_tien] = CAST(123.00 AS Decimal(18, 2)), [ghi_chu] = N'Chi tiết demo; không điều chỉnh tồn kho', [trang_thai] = 1 WHERE [id] = 4;
    ELSE
        INSERT [dbo].[hoa_don_chi_tiet] ([id], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong], [don_gia], [thanh_tien], [ghi_chu], [trang_thai]) VALUES (4, 3, 5, 1, CAST(123.00 AS Decimal(18, 2)), CAST(123.00 AS Decimal(18, 2)), N'Chi tiết demo; không điều chỉnh tồn kho', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[hoa_don_chi_tiet] WHERE [id] = 5)
        UPDATE [dbo].[hoa_don_chi_tiet] SET [id_hoa_don] = 3, [id_san_pham_chi_tiet] = 4, [so_luong] = 1, [don_gia] = CAST(123.00 AS Decimal(18, 2)), [thanh_tien] = CAST(123.00 AS Decimal(18, 2)), [ghi_chu] = N'Chi tiết demo; không điều chỉnh tồn kho', [trang_thai] = 1 WHERE [id] = 5;
    ELSE
        INSERT [dbo].[hoa_don_chi_tiet] ([id], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong], [don_gia], [thanh_tien], [ghi_chu], [trang_thai]) VALUES (5, 3, 4, 1, CAST(123.00 AS Decimal(18, 2)), CAST(123.00 AS Decimal(18, 2)), N'Chi tiết demo; không điều chỉnh tồn kho', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[hoa_don_chi_tiet] WHERE [id] = 6)
        UPDATE [dbo].[hoa_don_chi_tiet] SET [id_hoa_don] = 4, [id_san_pham_chi_tiet] = 3, [so_luong] = 1, [don_gia] = CAST(123.00 AS Decimal(18, 2)), [thanh_tien] = CAST(123.00 AS Decimal(18, 2)), [ghi_chu] = N'Chi tiết demo; không điều chỉnh tồn kho', [trang_thai] = 1 WHERE [id] = 6;
    ELSE
        INSERT [dbo].[hoa_don_chi_tiet] ([id], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong], [don_gia], [thanh_tien], [ghi_chu], [trang_thai]) VALUES (6, 4, 3, 1, CAST(123.00 AS Decimal(18, 2)), CAST(123.00 AS Decimal(18, 2)), N'Chi tiết demo; không điều chỉnh tồn kho', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[hoa_don_chi_tiet] WHERE [id] = 7)
        UPDATE [dbo].[hoa_don_chi_tiet] SET [id_hoa_don] = 4, [id_san_pham_chi_tiet] = 12, [so_luong] = 1, [don_gia] = CAST(300000.00 AS Decimal(18, 2)), [thanh_tien] = CAST(300000.00 AS Decimal(18, 2)), [ghi_chu] = N'Chi tiết demo; không điều chỉnh tồn kho', [trang_thai] = 1 WHERE [id] = 7;
    ELSE
        INSERT [dbo].[hoa_don_chi_tiet] ([id], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong], [don_gia], [thanh_tien], [ghi_chu], [trang_thai]) VALUES (7, 4, 12, 1, CAST(300000.00 AS Decimal(18, 2)), CAST(300000.00 AS Decimal(18, 2)), N'Chi tiết demo; không điều chỉnh tồn kho', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[hoa_don_chi_tiet] WHERE [id] = 8)
        UPDATE [dbo].[hoa_don_chi_tiet] SET [id_hoa_don] = 5, [id_san_pham_chi_tiet] = 11, [so_luong] = 1, [don_gia] = CAST(300000.00 AS Decimal(18, 2)), [thanh_tien] = CAST(300000.00 AS Decimal(18, 2)), [ghi_chu] = N'Chi tiết demo; không điều chỉnh tồn kho', [trang_thai] = 1 WHERE [id] = 8;
    ELSE
        INSERT [dbo].[hoa_don_chi_tiet] ([id], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong], [don_gia], [thanh_tien], [ghi_chu], [trang_thai]) VALUES (8, 5, 11, 1, CAST(300000.00 AS Decimal(18, 2)), CAST(300000.00 AS Decimal(18, 2)), N'Chi tiết demo; không điều chỉnh tồn kho', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[hoa_don_chi_tiet] WHERE [id] = 9)
        UPDATE [dbo].[hoa_don_chi_tiet] SET [id_hoa_don] = 5, [id_san_pham_chi_tiet] = 10, [so_luong] = 1, [don_gia] = CAST(300000.00 AS Decimal(18, 2)), [thanh_tien] = CAST(300000.00 AS Decimal(18, 2)), [ghi_chu] = N'Chi tiết demo; không điều chỉnh tồn kho', [trang_thai] = 1 WHERE [id] = 9;
    ELSE
        INSERT [dbo].[hoa_don_chi_tiet] ([id], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong], [don_gia], [thanh_tien], [ghi_chu], [trang_thai]) VALUES (9, 5, 10, 1, CAST(300000.00 AS Decimal(18, 2)), CAST(300000.00 AS Decimal(18, 2)), N'Chi tiết demo; không điều chỉnh tồn kho', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[hoa_don_chi_tiet] WHERE [id] = 10)
        UPDATE [dbo].[hoa_don_chi_tiet] SET [id_hoa_don] = 6, [id_san_pham_chi_tiet] = 9, [so_luong] = 1, [don_gia] = CAST(300000.00 AS Decimal(18, 2)), [thanh_tien] = CAST(300000.00 AS Decimal(18, 2)), [ghi_chu] = N'Chi tiết demo; không điều chỉnh tồn kho', [trang_thai] = 1 WHERE [id] = 10;
    ELSE
        INSERT [dbo].[hoa_don_chi_tiet] ([id], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong], [don_gia], [thanh_tien], [ghi_chu], [trang_thai]) VALUES (10, 6, 9, 1, CAST(300000.00 AS Decimal(18, 2)), CAST(300000.00 AS Decimal(18, 2)), N'Chi tiết demo; không điều chỉnh tồn kho', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[hoa_don_chi_tiet] WHERE [id] = 11)
        UPDATE [dbo].[hoa_don_chi_tiet] SET [id_hoa_don] = 6, [id_san_pham_chi_tiet] = 8, [so_luong] = 1, [don_gia] = CAST(300000.00 AS Decimal(18, 2)), [thanh_tien] = CAST(300000.00 AS Decimal(18, 2)), [ghi_chu] = N'Chi tiết demo; không điều chỉnh tồn kho', [trang_thai] = 1 WHERE [id] = 11;
    ELSE
        INSERT [dbo].[hoa_don_chi_tiet] ([id], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong], [don_gia], [thanh_tien], [ghi_chu], [trang_thai]) VALUES (11, 6, 8, 1, CAST(300000.00 AS Decimal(18, 2)), CAST(300000.00 AS Decimal(18, 2)), N'Chi tiết demo; không điều chỉnh tồn kho', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[hoa_don_chi_tiet] WHERE [id] = 12)
        UPDATE [dbo].[hoa_don_chi_tiet] SET [id_hoa_don] = 7, [id_san_pham_chi_tiet] = 7, [so_luong] = 1, [don_gia] = CAST(300000.00 AS Decimal(18, 2)), [thanh_tien] = CAST(300000.00 AS Decimal(18, 2)), [ghi_chu] = N'Chi tiết demo; không điều chỉnh tồn kho', [trang_thai] = 1 WHERE [id] = 12;
    ELSE
        INSERT [dbo].[hoa_don_chi_tiet] ([id], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong], [don_gia], [thanh_tien], [ghi_chu], [trang_thai]) VALUES (12, 7, 7, 1, CAST(300000.00 AS Decimal(18, 2)), CAST(300000.00 AS Decimal(18, 2)), N'Chi tiết demo; không điều chỉnh tồn kho', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[hoa_don_chi_tiet] WHERE [id] = 13)
        UPDATE [dbo].[hoa_don_chi_tiet] SET [id_hoa_don] = 7, [id_san_pham_chi_tiet] = 19, [so_luong] = 1, [don_gia] = CAST(100000.00 AS Decimal(18, 2)), [thanh_tien] = CAST(100000.00 AS Decimal(18, 2)), [ghi_chu] = N'Chi tiết demo; không điều chỉnh tồn kho', [trang_thai] = 1 WHERE [id] = 13;
    ELSE
        INSERT [dbo].[hoa_don_chi_tiet] ([id], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong], [don_gia], [thanh_tien], [ghi_chu], [trang_thai]) VALUES (13, 7, 19, 1, CAST(100000.00 AS Decimal(18, 2)), CAST(100000.00 AS Decimal(18, 2)), N'Chi tiết demo; không điều chỉnh tồn kho', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[hoa_don_chi_tiet] WHERE [id] = 14)
        UPDATE [dbo].[hoa_don_chi_tiet] SET [id_hoa_don] = 8, [id_san_pham_chi_tiet] = 18, [so_luong] = 1, [don_gia] = CAST(100000.00 AS Decimal(18, 2)), [thanh_tien] = CAST(100000.00 AS Decimal(18, 2)), [ghi_chu] = N'Chi tiết demo; không điều chỉnh tồn kho', [trang_thai] = 1 WHERE [id] = 14;
    ELSE
        INSERT [dbo].[hoa_don_chi_tiet] ([id], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong], [don_gia], [thanh_tien], [ghi_chu], [trang_thai]) VALUES (14, 8, 18, 1, CAST(100000.00 AS Decimal(18, 2)), CAST(100000.00 AS Decimal(18, 2)), N'Chi tiết demo; không điều chỉnh tồn kho', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[hoa_don_chi_tiet] WHERE [id] = 15)
        UPDATE [dbo].[hoa_don_chi_tiet] SET [id_hoa_don] = 8, [id_san_pham_chi_tiet] = 17, [so_luong] = 1, [don_gia] = CAST(100000.00 AS Decimal(18, 2)), [thanh_tien] = CAST(100000.00 AS Decimal(18, 2)), [ghi_chu] = N'Chi tiết demo; không điều chỉnh tồn kho', [trang_thai] = 1 WHERE [id] = 15;
    ELSE
        INSERT [dbo].[hoa_don_chi_tiet] ([id], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong], [don_gia], [thanh_tien], [ghi_chu], [trang_thai]) VALUES (15, 8, 17, 1, CAST(100000.00 AS Decimal(18, 2)), CAST(100000.00 AS Decimal(18, 2)), N'Chi tiết demo; không điều chỉnh tồn kho', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[hoa_don_chi_tiet] WHERE [id] = 16)
        UPDATE [dbo].[hoa_don_chi_tiet] SET [id_hoa_don] = 9, [id_san_pham_chi_tiet] = 16, [so_luong] = 1, [don_gia] = CAST(100000.00 AS Decimal(18, 2)), [thanh_tien] = CAST(100000.00 AS Decimal(18, 2)), [ghi_chu] = N'Chi tiết demo; không điều chỉnh tồn kho', [trang_thai] = 1 WHERE [id] = 16;
    ELSE
        INSERT [dbo].[hoa_don_chi_tiet] ([id], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong], [don_gia], [thanh_tien], [ghi_chu], [trang_thai]) VALUES (16, 9, 16, 1, CAST(100000.00 AS Decimal(18, 2)), CAST(100000.00 AS Decimal(18, 2)), N'Chi tiết demo; không điều chỉnh tồn kho', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[hoa_don_chi_tiet] WHERE [id] = 17)
        UPDATE [dbo].[hoa_don_chi_tiet] SET [id_hoa_don] = 9, [id_san_pham_chi_tiet] = 1, [so_luong] = 1, [don_gia] = CAST(1599000.00 AS Decimal(18, 2)), [thanh_tien] = CAST(1599000.00 AS Decimal(18, 2)), [ghi_chu] = N'Chi tiết demo; không điều chỉnh tồn kho', [trang_thai] = 1 WHERE [id] = 17;
    ELSE
        INSERT [dbo].[hoa_don_chi_tiet] ([id], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong], [don_gia], [thanh_tien], [ghi_chu], [trang_thai]) VALUES (17, 9, 1, 1, CAST(1599000.00 AS Decimal(18, 2)), CAST(1599000.00 AS Decimal(18, 2)), N'Chi tiết demo; không điều chỉnh tồn kho', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[hoa_don_chi_tiet] WHERE [id] = 18)
        UPDATE [dbo].[hoa_don_chi_tiet] SET [id_hoa_don] = 10, [id_san_pham_chi_tiet] = 2, [so_luong] = 1, [don_gia] = CAST(1699000.00 AS Decimal(18, 2)), [thanh_tien] = CAST(1699000.00 AS Decimal(18, 2)), [ghi_chu] = N'Chi tiết demo; không điều chỉnh tồn kho', [trang_thai] = 1 WHERE [id] = 18;
    ELSE
        INSERT [dbo].[hoa_don_chi_tiet] ([id], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong], [don_gia], [thanh_tien], [ghi_chu], [trang_thai]) VALUES (18, 10, 2, 1, CAST(1699000.00 AS Decimal(18, 2)), CAST(1699000.00 AS Decimal(18, 2)), N'Chi tiết demo; không điều chỉnh tồn kho', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[hoa_don_chi_tiet] WHERE [id] = 19)
        UPDATE [dbo].[hoa_don_chi_tiet] SET [id_hoa_don] = 10, [id_san_pham_chi_tiet] = 84, [so_luong] = 1, [don_gia] = CAST(100000.00 AS Decimal(18, 2)), [thanh_tien] = CAST(100000.00 AS Decimal(18, 2)), [ghi_chu] = N'Chi tiết demo; không điều chỉnh tồn kho', [trang_thai] = 1 WHERE [id] = 19;
    ELSE
        INSERT [dbo].[hoa_don_chi_tiet] ([id], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong], [don_gia], [thanh_tien], [ghi_chu], [trang_thai]) VALUES (19, 10, 84, 1, CAST(100000.00 AS Decimal(18, 2)), CAST(100000.00 AS Decimal(18, 2)), N'Chi tiết demo; không điều chỉnh tồn kho', 1);
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[hoa_don_chi_tiet]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[hoa_don_chi_tiet] OFF;

    -- DATA MERGE: [dbo].[khach_hang] (11 dong snapshot)
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[khach_hang]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[khach_hang] ON;
    IF EXISTS (SELECT 1 FROM [dbo].[khach_hang] WHERE [id] = 1)
        UPDATE [dbo].[khach_hang] SET [ma_khach_hang] = N'KH001', [ten_tai_khoan] = N'kh001', [ten_khach_hang] = N'Trần Minh Bảo Hoàng', [email] = NULL, [so_dien_thoai] = N'0909899999', [ngay_sinh] = NULL, [gioi_tinh] = NULL, [mat_khau] = NULL, [hinh_anh] = NULL, [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2) WHERE [id] = 1;
    ELSE
        INSERT [dbo].[khach_hang] ([id], [ma_khach_hang], [ten_tai_khoan], [ten_khach_hang], [email], [so_dien_thoai], [ngay_sinh], [gioi_tinh], [mat_khau], [hinh_anh], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (1, N'KH001', N'kh001', N'Trần Minh Bảo Hoàng', NULL, N'0909899999', NULL, NULL, NULL, NULL, 1, CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2), CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[khach_hang] WHERE [id] = 2)
        UPDATE [dbo].[khach_hang] SET [ma_khach_hang] = N'KH002', [ten_tai_khoan] = N'kh002', [ten_khach_hang] = N'Nguyễn Thị An', [email] = NULL, [so_dien_thoai] = N'0911111111', [ngay_sinh] = NULL, [gioi_tinh] = NULL, [mat_khau] = NULL, [hinh_anh] = NULL, [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2) WHERE [id] = 2;
    ELSE
        INSERT [dbo].[khach_hang] ([id], [ma_khach_hang], [ten_tai_khoan], [ten_khach_hang], [email], [so_dien_thoai], [ngay_sinh], [gioi_tinh], [mat_khau], [hinh_anh], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (2, N'KH002', N'kh002', N'Nguyễn Thị An', NULL, N'0911111111', NULL, NULL, NULL, NULL, 1, CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2), CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[khach_hang] WHERE [id] = 3)
        UPDATE [dbo].[khach_hang] SET [ma_khach_hang] = N'KH003', [ten_tai_khoan] = N'kh003', [ten_khach_hang] = N'Lê Quốc Hưng', [email] = NULL, [so_dien_thoai] = N'0911111111', [ngay_sinh] = NULL, [gioi_tinh] = NULL, [mat_khau] = NULL, [hinh_anh] = NULL, [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2) WHERE [id] = 3;
    ELSE
        INSERT [dbo].[khach_hang] ([id], [ma_khach_hang], [ten_tai_khoan], [ten_khach_hang], [email], [so_dien_thoai], [ngay_sinh], [gioi_tinh], [mat_khau], [hinh_anh], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (3, N'KH003', N'kh003', N'Lê Quốc Hưng', NULL, N'0911111111', NULL, NULL, NULL, NULL, 1, CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2), CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[khach_hang] WHERE [id] = 4)
        UPDATE [dbo].[khach_hang] SET [ma_khach_hang] = N'KH004', [ten_tai_khoan] = N'kh004', [ten_khach_hang] = N'Phạm Thanh Tú', [email] = NULL, [so_dien_thoai] = N'0983214567', [ngay_sinh] = NULL, [gioi_tinh] = NULL, [mat_khau] = NULL, [hinh_anh] = NULL, [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2) WHERE [id] = 4;
    ELSE
        INSERT [dbo].[khach_hang] ([id], [ma_khach_hang], [ten_tai_khoan], [ten_khach_hang], [email], [so_dien_thoai], [ngay_sinh], [gioi_tinh], [mat_khau], [hinh_anh], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (4, N'KH004', N'kh004', N'Phạm Thanh Tú', NULL, N'0983214567', NULL, NULL, NULL, NULL, 1, CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2), CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[khach_hang] WHERE [id] = 5)
        UPDATE [dbo].[khach_hang] SET [ma_khach_hang] = N'KH005', [ten_tai_khoan] = N'kh005', [ten_khach_hang] = N'Võ Gia Hân', [email] = NULL, [so_dien_thoai] = N'0905882114', [ngay_sinh] = NULL, [gioi_tinh] = NULL, [mat_khau] = NULL, [hinh_anh] = NULL, [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2) WHERE [id] = 5;
    ELSE
        INSERT [dbo].[khach_hang] ([id], [ma_khach_hang], [ten_tai_khoan], [ten_khach_hang], [email], [so_dien_thoai], [ngay_sinh], [gioi_tinh], [mat_khau], [hinh_anh], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (5, N'KH005', N'kh005', N'Võ Gia Hân', NULL, N'0905882114', NULL, NULL, NULL, NULL, 1, CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2), CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[khach_hang] WHERE [id] = 6)
        UPDATE [dbo].[khach_hang] SET [ma_khach_hang] = N'KH006', [ten_tai_khoan] = N'kh006', [ten_khach_hang] = N'Đặng Hoài Nam', [email] = NULL, [so_dien_thoai] = N'0934625881', [ngay_sinh] = NULL, [gioi_tinh] = NULL, [mat_khau] = NULL, [hinh_anh] = NULL, [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2) WHERE [id] = 6;
    ELSE
        INSERT [dbo].[khach_hang] ([id], [ma_khach_hang], [ten_tai_khoan], [ten_khach_hang], [email], [so_dien_thoai], [ngay_sinh], [gioi_tinh], [mat_khau], [hinh_anh], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (6, N'KH006', N'kh006', N'Đặng Hoài Nam', NULL, N'0934625881', NULL, NULL, NULL, NULL, 1, CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2), CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[khach_hang] WHERE [id] = 7)
        UPDATE [dbo].[khach_hang] SET [ma_khach_hang] = N'KH007', [ten_tai_khoan] = N'kh007', [ten_khach_hang] = N'Bùi Mỹ Linh', [email] = NULL, [so_dien_thoai] = N'0972230456', [ngay_sinh] = NULL, [gioi_tinh] = NULL, [mat_khau] = NULL, [hinh_anh] = NULL, [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2) WHERE [id] = 7;
    ELSE
        INSERT [dbo].[khach_hang] ([id], [ma_khach_hang], [ten_tai_khoan], [ten_khach_hang], [email], [so_dien_thoai], [ngay_sinh], [gioi_tinh], [mat_khau], [hinh_anh], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (7, N'KH007', N'kh007', N'Bùi Mỹ Linh', NULL, N'0972230456', NULL, NULL, NULL, NULL, 1, CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2), CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[khach_hang] WHERE [id] = 8)
        UPDATE [dbo].[khach_hang] SET [ma_khach_hang] = N'KH008', [ten_tai_khoan] = N'kh008', [ten_khach_hang] = N'Ngô Đức Anh', [email] = NULL, [so_dien_thoai] = N'0902718663', [ngay_sinh] = NULL, [gioi_tinh] = NULL, [mat_khau] = NULL, [hinh_anh] = NULL, [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2) WHERE [id] = 8;
    ELSE
        INSERT [dbo].[khach_hang] ([id], [ma_khach_hang], [ten_tai_khoan], [ten_khach_hang], [email], [so_dien_thoai], [ngay_sinh], [gioi_tinh], [mat_khau], [hinh_anh], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (8, N'KH008', N'kh008', N'Ngô Đức Anh', NULL, N'0902718663', NULL, NULL, NULL, NULL, 1, CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2), CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[khach_hang] WHERE [id] = 9)
        UPDATE [dbo].[khach_hang] SET [ma_khach_hang] = N'KH009', [ten_tai_khoan] = N'kh009', [ten_khach_hang] = N'Đỗ Phương Vy', [email] = NULL, [so_dien_thoai] = N'0968440127', [ngay_sinh] = NULL, [gioi_tinh] = NULL, [mat_khau] = NULL, [hinh_anh] = NULL, [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2) WHERE [id] = 9;
    ELSE
        INSERT [dbo].[khach_hang] ([id], [ma_khach_hang], [ten_tai_khoan], [ten_khach_hang], [email], [so_dien_thoai], [ngay_sinh], [gioi_tinh], [mat_khau], [hinh_anh], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (9, N'KH009', N'kh009', N'Đỗ Phương Vy', NULL, N'0968440127', NULL, NULL, NULL, NULL, 1, CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2), CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[khach_hang] WHERE [id] = 10)
        UPDATE [dbo].[khach_hang] SET [ma_khach_hang] = N'KH010', [ten_tai_khoan] = N'kh010', [ten_khach_hang] = N'Mai Tiến Thành', [email] = NULL, [so_dien_thoai] = N'0918305902', [ngay_sinh] = NULL, [gioi_tinh] = NULL, [mat_khau] = NULL, [hinh_anh] = NULL, [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2) WHERE [id] = 10;
    ELSE
        INSERT [dbo].[khach_hang] ([id], [ma_khach_hang], [ten_tai_khoan], [ten_khach_hang], [email], [so_dien_thoai], [ngay_sinh], [gioi_tinh], [mat_khau], [hinh_anh], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (10, N'KH010', N'kh010', N'Mai Tiến Thành', NULL, N'0918305902', NULL, NULL, NULL, NULL, 1, CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2), CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[khach_hang] WHERE [id] = 17)
        UPDATE [dbo].[khach_hang] SET [ma_khach_hang] = N'KH0017', [ten_tai_khoan] = N'KH0017', [ten_khach_hang] = N'DEMO UI Khách hàng 20261007 - đã kiểm tra', [email] = N'stabilization.customer.20261007@smashstep.example', [so_dien_thoai] = N'0998800001', [ngay_sinh] = NULL, [gioi_tinh] = 1, [mat_khau] = NULL, [hinh_anh] = N'/uploads/avatars/56195cda-afe2-4f41-9388-a6ad99864a97.png', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-07T01:58:21.5396646' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-07T03:09:03.1153744' AS DateTime2) WHERE [id] = 17;
    ELSE
        INSERT [dbo].[khach_hang] ([id], [ma_khach_hang], [ten_tai_khoan], [ten_khach_hang], [email], [so_dien_thoai], [ngay_sinh], [gioi_tinh], [mat_khau], [hinh_anh], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (17, N'KH0017', N'KH0017', N'DEMO UI Khách hàng 20261007 - đã kiểm tra', N'stabilization.customer.20261007@smashstep.example', N'0998800001', NULL, 1, NULL, N'/uploads/avatars/56195cda-afe2-4f41-9388-a6ad99864a97.png', 1, CAST(N'2026-10-07T01:58:21.5396646' AS DateTime2), CAST(N'2026-10-07T03:09:03.1153744' AS DateTime2));
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[khach_hang]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[khach_hang] OFF;

    -- DATA MERGE: [dbo].[kich_thuoc] (21 dong snapshot)
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[kich_thuoc]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[kich_thuoc] ON;
    IF EXISTS (SELECT 1 FROM [dbo].[kich_thuoc] WHERE [id] = 1)
        UPDATE [dbo].[kich_thuoc] SET [gia_tri] = N'35', [ghi_chu] = N'Size 35', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2) WHERE [id] = 1;
    ELSE
        INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (1, N'35', N'Size 35', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[kich_thuoc] WHERE [id] = 2)
        UPDATE [dbo].[kich_thuoc] SET [gia_tri] = N'36', [ghi_chu] = N'Size 36', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2) WHERE [id] = 2;
    ELSE
        INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (2, N'36', N'Size 36', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[kich_thuoc] WHERE [id] = 3)
        UPDATE [dbo].[kich_thuoc] SET [gia_tri] = N'36.5', [ghi_chu] = N'Size 36.5', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2) WHERE [id] = 3;
    ELSE
        INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (3, N'36.5', N'Size 36.5', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[kich_thuoc] WHERE [id] = 4)
        UPDATE [dbo].[kich_thuoc] SET [gia_tri] = N'37', [ghi_chu] = N'Size 37', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2) WHERE [id] = 4;
    ELSE
        INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (4, N'37', N'Size 37', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[kich_thuoc] WHERE [id] = 5)
        UPDATE [dbo].[kich_thuoc] SET [gia_tri] = N'37.5', [ghi_chu] = N'Size 37.5', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2) WHERE [id] = 5;
    ELSE
        INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (5, N'37.5', N'Size 37.5', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[kich_thuoc] WHERE [id] = 6)
        UPDATE [dbo].[kich_thuoc] SET [gia_tri] = N'38', [ghi_chu] = N'Size 38', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2) WHERE [id] = 6;
    ELSE
        INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (6, N'38', N'Size 38', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[kich_thuoc] WHERE [id] = 7)
        UPDATE [dbo].[kich_thuoc] SET [gia_tri] = N'38.5', [ghi_chu] = N'Size 38.5', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2) WHERE [id] = 7;
    ELSE
        INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (7, N'38.5', N'Size 38.5', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[kich_thuoc] WHERE [id] = 8)
        UPDATE [dbo].[kich_thuoc] SET [gia_tri] = N'39', [ghi_chu] = N'Size 39', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2) WHERE [id] = 8;
    ELSE
        INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (8, N'39', N'Size 39', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[kich_thuoc] WHERE [id] = 9)
        UPDATE [dbo].[kich_thuoc] SET [gia_tri] = N'39.5', [ghi_chu] = N'Size 39.5', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2) WHERE [id] = 9;
    ELSE
        INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (9, N'39.5', N'Size 39.5', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[kich_thuoc] WHERE [id] = 10)
        UPDATE [dbo].[kich_thuoc] SET [gia_tri] = N'40', [ghi_chu] = N'Size 40', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2) WHERE [id] = 10;
    ELSE
        INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (10, N'40', N'Size 40', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[kich_thuoc] WHERE [id] = 11)
        UPDATE [dbo].[kich_thuoc] SET [gia_tri] = N'40.5', [ghi_chu] = N'Size 40.5', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2) WHERE [id] = 11;
    ELSE
        INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (11, N'40.5', N'Size 40.5', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[kich_thuoc] WHERE [id] = 12)
        UPDATE [dbo].[kich_thuoc] SET [gia_tri] = N'41', [ghi_chu] = N'Size 41', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2) WHERE [id] = 12;
    ELSE
        INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (12, N'41', N'Size 41', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[kich_thuoc] WHERE [id] = 13)
        UPDATE [dbo].[kich_thuoc] SET [gia_tri] = N'41.5', [ghi_chu] = N'Size 41.5', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2) WHERE [id] = 13;
    ELSE
        INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (13, N'41.5', N'Size 41.5', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[kich_thuoc] WHERE [id] = 14)
        UPDATE [dbo].[kich_thuoc] SET [gia_tri] = N'42', [ghi_chu] = N'Size 42', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2) WHERE [id] = 14;
    ELSE
        INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (14, N'42', N'Size 42', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[kich_thuoc] WHERE [id] = 15)
        UPDATE [dbo].[kich_thuoc] SET [gia_tri] = N'42.5', [ghi_chu] = N'Size 42.5', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2) WHERE [id] = 15;
    ELSE
        INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (15, N'42.5', N'Size 42.5', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[kich_thuoc] WHERE [id] = 16)
        UPDATE [dbo].[kich_thuoc] SET [gia_tri] = N'43', [ghi_chu] = N'Size 43', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2) WHERE [id] = 16;
    ELSE
        INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (16, N'43', N'Size 43', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[kich_thuoc] WHERE [id] = 17)
        UPDATE [dbo].[kich_thuoc] SET [gia_tri] = N'43.5', [ghi_chu] = N'Size 43.5', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2) WHERE [id] = 17;
    ELSE
        INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (17, N'43.5', N'Size 43.5', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[kich_thuoc] WHERE [id] = 18)
        UPDATE [dbo].[kich_thuoc] SET [gia_tri] = N'44', [ghi_chu] = N'Size 44', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2) WHERE [id] = 18;
    ELSE
        INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (18, N'44', N'Size 44', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[kich_thuoc] WHERE [id] = 19)
        UPDATE [dbo].[kich_thuoc] SET [gia_tri] = N'44.5', [ghi_chu] = N'Size 44.5', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2) WHERE [id] = 19;
    ELSE
        INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (19, N'44.5', N'Size 44.5', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[kich_thuoc] WHERE [id] = 20)
        UPDATE [dbo].[kich_thuoc] SET [gia_tri] = N'45', [ghi_chu] = N'Size 45', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2) WHERE [id] = 20;
    ELSE
        INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (20, N'45', N'Size 45', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[kich_thuoc] WHERE [id] = 21)
        UPDATE [dbo].[kich_thuoc] SET [gia_tri] = N'46', [ghi_chu] = N'Size 46', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2) WHERE [id] = 21;
    ELSE
        INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (21, N'46', N'Size 46', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2));
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[kich_thuoc]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[kich_thuoc] OFF;

    -- DATA MERGE: [dbo].[kieu_dang] (10 dong snapshot)
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[kieu_dang]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[kieu_dang] ON;
    IF EXISTS (SELECT 1 FROM [dbo].[kieu_dang] WHERE [id] = 1)
        UPDATE [dbo].[kieu_dang] SET [ma_kieu_dang] = N'KD001', [ten_kieu_dang] = N'Running', [trang_thai] = 1 WHERE [id] = 1;
    ELSE
        INSERT [dbo].[kieu_dang] ([id], [ma_kieu_dang], [ten_kieu_dang], [trang_thai]) VALUES (1, N'KD001', N'Running', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[kieu_dang] WHERE [id] = 2)
        UPDATE [dbo].[kieu_dang] SET [ma_kieu_dang] = N'KD002', [ten_kieu_dang] = N'Sneaker', [trang_thai] = 1 WHERE [id] = 2;
    ELSE
        INSERT [dbo].[kieu_dang] ([id], [ma_kieu_dang], [ten_kieu_dang], [trang_thai]) VALUES (2, N'KD002', N'Sneaker', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[kieu_dang] WHERE [id] = 3)
        UPDATE [dbo].[kieu_dang] SET [ma_kieu_dang] = N'KD003', [ten_kieu_dang] = N'Basketball', [trang_thai] = 1 WHERE [id] = 3;
    ELSE
        INSERT [dbo].[kieu_dang] ([id], [ma_kieu_dang], [ten_kieu_dang], [trang_thai]) VALUES (3, N'KD003', N'Basketball', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[kieu_dang] WHERE [id] = 4)
        UPDATE [dbo].[kieu_dang] SET [ma_kieu_dang] = N'KD004', [ten_kieu_dang] = N'Casual', [trang_thai] = 1 WHERE [id] = 4;
    ELSE
        INSERT [dbo].[kieu_dang] ([id], [ma_kieu_dang], [ten_kieu_dang], [trang_thai]) VALUES (4, N'KD004', N'Casual', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[kieu_dang] WHERE [id] = 5)
        UPDATE [dbo].[kieu_dang] SET [ma_kieu_dang] = N'KD005', [ten_kieu_dang] = N'Training', [trang_thai] = 1 WHERE [id] = 5;
    ELSE
        INSERT [dbo].[kieu_dang] ([id], [ma_kieu_dang], [ten_kieu_dang], [trang_thai]) VALUES (5, N'KD005', N'Training', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[kieu_dang] WHERE [id] = 6)
        UPDATE [dbo].[kieu_dang] SET [ma_kieu_dang] = N'KD006', [ten_kieu_dang] = N'Football', [trang_thai] = 1 WHERE [id] = 6;
    ELSE
        INSERT [dbo].[kieu_dang] ([id], [ma_kieu_dang], [ten_kieu_dang], [trang_thai]) VALUES (6, N'KD006', N'Football', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[kieu_dang] WHERE [id] = 7)
        UPDATE [dbo].[kieu_dang] SET [ma_kieu_dang] = N'KD007', [ten_kieu_dang] = N'Tennis', [trang_thai] = 1 WHERE [id] = 7;
    ELSE
        INSERT [dbo].[kieu_dang] ([id], [ma_kieu_dang], [ten_kieu_dang], [trang_thai]) VALUES (7, N'KD007', N'Tennis', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[kieu_dang] WHERE [id] = 8)
        UPDATE [dbo].[kieu_dang] SET [ma_kieu_dang] = N'KD008', [ten_kieu_dang] = N'Walking', [trang_thai] = 1 WHERE [id] = 8;
    ELSE
        INSERT [dbo].[kieu_dang] ([id], [ma_kieu_dang], [ten_kieu_dang], [trang_thai]) VALUES (8, N'KD008', N'Walking', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[kieu_dang] WHERE [id] = 9)
        UPDATE [dbo].[kieu_dang] SET [ma_kieu_dang] = N'KD009', [ten_kieu_dang] = N'Skate', [trang_thai] = 1 WHERE [id] = 9;
    ELSE
        INSERT [dbo].[kieu_dang] ([id], [ma_kieu_dang], [ten_kieu_dang], [trang_thai]) VALUES (9, N'KD009', N'Skate', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[kieu_dang] WHERE [id] = 10)
        UPDATE [dbo].[kieu_dang] SET [ma_kieu_dang] = N'KD010', [ten_kieu_dang] = N'Lifestyle', [trang_thai] = 1 WHERE [id] = 10;
    ELSE
        INSERT [dbo].[kieu_dang] ([id], [ma_kieu_dang], [ten_kieu_dang], [trang_thai]) VALUES (10, N'KD010', N'Lifestyle', 1);
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[kieu_dang]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[kieu_dang] OFF;

    -- DATA MERGE: [dbo].[lich_su_hoa_don] (11 dong snapshot)
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[lich_su_hoa_don]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[lich_su_hoa_don] ON;
    IF EXISTS (SELECT 1 FROM [dbo].[lich_su_hoa_don] WHERE [id] = 1)
        UPDATE [dbo].[lich_su_hoa_don] SET [id_hoa_don] = 1, [nguoi_tao] = 6, [trang_thai] = 5, [ghi_chu] = N'Trạng thái hóa đơn demo tích hợp', [ngay_tao] = CAST(N'2026-09-27T03:49:07.8364510' AS DateTime2) WHERE [id] = 1;
    ELSE
        INSERT [dbo].[lich_su_hoa_don] ([id], [id_hoa_don], [nguoi_tao], [trang_thai], [ghi_chu], [ngay_tao]) VALUES (1, 1, 6, 5, N'Trạng thái hóa đơn demo tích hợp', CAST(N'2026-09-27T03:49:07.8364510' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[lich_su_hoa_don] WHERE [id] = 2)
        UPDATE [dbo].[lich_su_hoa_don] SET [id_hoa_don] = 2, [nguoi_tao] = 7, [trang_thai] = 5, [ghi_chu] = N'Trạng thái hóa đơn demo tích hợp', [ngay_tao] = CAST(N'2026-09-28T03:49:07.8364510' AS DateTime2) WHERE [id] = 2;
    ELSE
        INSERT [dbo].[lich_su_hoa_don] ([id], [id_hoa_don], [nguoi_tao], [trang_thai], [ghi_chu], [ngay_tao]) VALUES (2, 2, 7, 5, N'Trạng thái hóa đơn demo tích hợp', CAST(N'2026-09-28T03:49:07.8364510' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[lich_su_hoa_don] WHERE [id] = 3)
        UPDATE [dbo].[lich_su_hoa_don] SET [id_hoa_don] = 3, [nguoi_tao] = 8, [trang_thai] = 5, [ghi_chu] = N'Trạng thái hóa đơn demo tích hợp', [ngay_tao] = CAST(N'2026-09-29T03:49:07.8364510' AS DateTime2) WHERE [id] = 3;
    ELSE
        INSERT [dbo].[lich_su_hoa_don] ([id], [id_hoa_don], [nguoi_tao], [trang_thai], [ghi_chu], [ngay_tao]) VALUES (3, 3, 8, 5, N'Trạng thái hóa đơn demo tích hợp', CAST(N'2026-09-29T03:49:07.8364510' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[lich_su_hoa_don] WHERE [id] = 4)
        UPDATE [dbo].[lich_su_hoa_don] SET [id_hoa_don] = 4, [nguoi_tao] = 1, [trang_thai] = 5, [ghi_chu] = N'Trạng thái hóa đơn demo tích hợp', [ngay_tao] = CAST(N'2026-09-30T03:49:07.8364510' AS DateTime2) WHERE [id] = 4;
    ELSE
        INSERT [dbo].[lich_su_hoa_don] ([id], [id_hoa_don], [nguoi_tao], [trang_thai], [ghi_chu], [ngay_tao]) VALUES (4, 4, 1, 5, N'Trạng thái hóa đơn demo tích hợp', CAST(N'2026-09-30T03:49:07.8364510' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[lich_su_hoa_don] WHERE [id] = 5)
        UPDATE [dbo].[lich_su_hoa_don] SET [id_hoa_don] = 5, [nguoi_tao] = 2, [trang_thai] = 5, [ghi_chu] = N'Trạng thái hóa đơn demo tích hợp', [ngay_tao] = CAST(N'2026-10-01T03:49:07.8364510' AS DateTime2) WHERE [id] = 5;
    ELSE
        INSERT [dbo].[lich_su_hoa_don] ([id], [id_hoa_don], [nguoi_tao], [trang_thai], [ghi_chu], [ngay_tao]) VALUES (5, 5, 2, 5, N'Trạng thái hóa đơn demo tích hợp', CAST(N'2026-10-01T03:49:07.8364510' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[lich_su_hoa_don] WHERE [id] = 6)
        UPDATE [dbo].[lich_su_hoa_don] SET [id_hoa_don] = 6, [nguoi_tao] = 3, [trang_thai] = 0, [ghi_chu] = N'Trạng thái hóa đơn demo tích hợp', [ngay_tao] = CAST(N'2026-10-02T03:49:07.8364510' AS DateTime2) WHERE [id] = 6;
    ELSE
        INSERT [dbo].[lich_su_hoa_don] ([id], [id_hoa_don], [nguoi_tao], [trang_thai], [ghi_chu], [ngay_tao]) VALUES (6, 6, 3, 0, N'Trạng thái hóa đơn demo tích hợp', CAST(N'2026-10-02T03:49:07.8364510' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[lich_su_hoa_don] WHERE [id] = 7)
        UPDATE [dbo].[lich_su_hoa_don] SET [id_hoa_don] = 7, [nguoi_tao] = 6, [trang_thai] = 1, [ghi_chu] = N'Trạng thái hóa đơn demo tích hợp', [ngay_tao] = CAST(N'2026-10-03T03:49:07.8364510' AS DateTime2) WHERE [id] = 7;
    ELSE
        INSERT [dbo].[lich_su_hoa_don] ([id], [id_hoa_don], [nguoi_tao], [trang_thai], [ghi_chu], [ngay_tao]) VALUES (7, 7, 6, 1, N'Trạng thái hóa đơn demo tích hợp', CAST(N'2026-10-03T03:49:07.8364510' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[lich_su_hoa_don] WHERE [id] = 8)
        UPDATE [dbo].[lich_su_hoa_don] SET [id_hoa_don] = 8, [nguoi_tao] = 7, [trang_thai] = 3, [ghi_chu] = N'Trạng thái hóa đơn demo tích hợp', [ngay_tao] = CAST(N'2026-10-04T03:49:07.8364510' AS DateTime2) WHERE [id] = 8;
    ELSE
        INSERT [dbo].[lich_su_hoa_don] ([id], [id_hoa_don], [nguoi_tao], [trang_thai], [ghi_chu], [ngay_tao]) VALUES (8, 8, 7, 3, N'Trạng thái hóa đơn demo tích hợp', CAST(N'2026-10-04T03:49:07.8364510' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[lich_su_hoa_don] WHERE [id] = 9)
        UPDATE [dbo].[lich_su_hoa_don] SET [id_hoa_don] = 9, [nguoi_tao] = 8, [trang_thai] = 4, [ghi_chu] = N'Trạng thái hóa đơn demo tích hợp', [ngay_tao] = CAST(N'2026-10-05T03:49:07.8364510' AS DateTime2) WHERE [id] = 9;
    ELSE
        INSERT [dbo].[lich_su_hoa_don] ([id], [id_hoa_don], [nguoi_tao], [trang_thai], [ghi_chu], [ngay_tao]) VALUES (9, 9, 8, 4, N'Trạng thái hóa đơn demo tích hợp', CAST(N'2026-10-05T03:49:07.8364510' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[lich_su_hoa_don] WHERE [id] = 10)
        UPDATE [dbo].[lich_su_hoa_don] SET [id_hoa_don] = 10, [nguoi_tao] = 1, [trang_thai] = 6, [ghi_chu] = N'Trạng thái hóa đơn demo tích hợp', [ngay_tao] = CAST(N'2026-10-06T03:49:07.8364510' AS DateTime2) WHERE [id] = 10;
    ELSE
        INSERT [dbo].[lich_su_hoa_don] ([id], [id_hoa_don], [nguoi_tao], [trang_thai], [ghi_chu], [ngay_tao]) VALUES (10, 10, 1, 6, N'Trạng thái hóa đơn demo tích hợp', CAST(N'2026-10-06T03:49:07.8364510' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[lich_su_hoa_don] WHERE [id] = 12)
        UPDATE [dbo].[lich_su_hoa_don] SET [id_hoa_don] = 6, [nguoi_tao] = 3, [trang_thai] = 1, [ghi_chu] = NULL, [ngay_tao] = CAST(N'2026-10-07T02:07:18.3134508' AS DateTime2) WHERE [id] = 12;
    ELSE
        INSERT [dbo].[lich_su_hoa_don] ([id], [id_hoa_don], [nguoi_tao], [trang_thai], [ghi_chu], [ngay_tao]) VALUES (12, 6, 3, 1, NULL, CAST(N'2026-10-07T02:07:18.3134508' AS DateTime2));
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[lich_su_hoa_don]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[lich_su_hoa_don] OFF;

    -- DATA MERGE: [dbo].[lich_su_thanh_toan] (5 dong snapshot)
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[lich_su_thanh_toan]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[lich_su_thanh_toan] ON;
    IF EXISTS (SELECT 1 FROM [dbo].[lich_su_thanh_toan] WHERE [id] = 1)
        UPDATE [dbo].[lich_su_thanh_toan] SET [id_hoa_don] = 1, [so_tien] = CAST(600000.00 AS Decimal(18, 2)), [ma_giao_dich] = N'DEMO-HD000001', [thoi_gian] = CAST(N'2026-09-27T03:49:07.8364510' AS DateTime2), [trang_thai] = 1, [mo_ta] = N'Thanh toán hóa đơn demo tích hợp' WHERE [id] = 1;
    ELSE
        INSERT [dbo].[lich_su_thanh_toan] ([id], [id_hoa_don], [so_tien], [ma_giao_dich], [thoi_gian], [trang_thai], [mo_ta]) VALUES (1, 1, CAST(600000.00 AS Decimal(18, 2)), N'DEMO-HD000001', CAST(N'2026-09-27T03:49:07.8364510' AS DateTime2), 1, N'Thanh toán hóa đơn demo tích hợp');
    IF EXISTS (SELECT 1 FROM [dbo].[lich_su_thanh_toan] WHERE [id] = 2)
        UPDATE [dbo].[lich_su_thanh_toan] SET [id_hoa_don] = 2, [so_tien] = CAST(330123.00 AS Decimal(18, 2)), [ma_giao_dich] = N'DEMO-HD000002', [thoi_gian] = CAST(N'2026-09-28T03:49:07.8364510' AS DateTime2), [trang_thai] = 1, [mo_ta] = N'Thanh toán hóa đơn demo tích hợp' WHERE [id] = 2;
    ELSE
        INSERT [dbo].[lich_su_thanh_toan] ([id], [id_hoa_don], [so_tien], [ma_giao_dich], [thoi_gian], [trang_thai], [mo_ta]) VALUES (2, 2, CAST(330123.00 AS Decimal(18, 2)), N'DEMO-HD000002', CAST(N'2026-09-28T03:49:07.8364510' AS DateTime2), 1, N'Thanh toán hóa đơn demo tích hợp');
    IF EXISTS (SELECT 1 FROM [dbo].[lich_su_thanh_toan] WHERE [id] = 3)
        UPDATE [dbo].[lich_su_thanh_toan] SET [id_hoa_don] = 3, [so_tien] = CAST(30246.00 AS Decimal(18, 2)), [ma_giao_dich] = N'DEMO-HD000003', [thoi_gian] = CAST(N'2026-09-29T03:49:07.8364510' AS DateTime2), [trang_thai] = 1, [mo_ta] = N'Thanh toán hóa đơn demo tích hợp' WHERE [id] = 3;
    ELSE
        INSERT [dbo].[lich_su_thanh_toan] ([id], [id_hoa_don], [so_tien], [ma_giao_dich], [thoi_gian], [trang_thai], [mo_ta]) VALUES (3, 3, CAST(30246.00 AS Decimal(18, 2)), N'DEMO-HD000003', CAST(N'2026-09-29T03:49:07.8364510' AS DateTime2), 1, N'Thanh toán hóa đơn demo tích hợp');
    IF EXISTS (SELECT 1 FROM [dbo].[lich_su_thanh_toan] WHERE [id] = 4)
        UPDATE [dbo].[lich_su_thanh_toan] SET [id_hoa_don] = 4, [so_tien] = CAST(300123.00 AS Decimal(18, 2)), [ma_giao_dich] = N'DEMO-HD000004', [thoi_gian] = CAST(N'2026-09-30T03:49:07.8364510' AS DateTime2), [trang_thai] = 1, [mo_ta] = N'Thanh toán hóa đơn demo tích hợp' WHERE [id] = 4;
    ELSE
        INSERT [dbo].[lich_su_thanh_toan] ([id], [id_hoa_don], [so_tien], [ma_giao_dich], [thoi_gian], [trang_thai], [mo_ta]) VALUES (4, 4, CAST(300123.00 AS Decimal(18, 2)), N'DEMO-HD000004', CAST(N'2026-09-30T03:49:07.8364510' AS DateTime2), 1, N'Thanh toán hóa đơn demo tích hợp');
    IF EXISTS (SELECT 1 FROM [dbo].[lich_su_thanh_toan] WHERE [id] = 5)
        UPDATE [dbo].[lich_su_thanh_toan] SET [id_hoa_don] = 5, [so_tien] = CAST(630000.00 AS Decimal(18, 2)), [ma_giao_dich] = N'DEMO-HD000005', [thoi_gian] = CAST(N'2026-10-01T03:49:07.8364510' AS DateTime2), [trang_thai] = 1, [mo_ta] = N'Thanh toán hóa đơn demo tích hợp' WHERE [id] = 5;
    ELSE
        INSERT [dbo].[lich_su_thanh_toan] ([id], [id_hoa_don], [so_tien], [ma_giao_dich], [thoi_gian], [trang_thai], [mo_ta]) VALUES (5, 5, CAST(630000.00 AS Decimal(18, 2)), N'DEMO-HD000005', CAST(N'2026-10-01T03:49:07.8364510' AS DateTime2), 1, N'Thanh toán hóa đơn demo tích hợp');
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[lich_su_thanh_toan]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[lich_su_thanh_toan] OFF;

    -- DATA MERGE: [dbo].[mau_sac] (15 dong snapshot)
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[mau_sac]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[mau_sac] ON;
    IF EXISTS (SELECT 1 FROM [dbo].[mau_sac] WHERE [id] = 1)
        UPDATE [dbo].[mau_sac] SET [ma_mau_sac] = N'MS001', [ten_mau_sac] = N'Đen', [ma_mau_hex] = N'#000000', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2) WHERE [id] = 1;
    ELSE
        INSERT [dbo].[mau_sac] ([id], [ma_mau_sac], [ten_mau_sac], [ma_mau_hex], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (1, N'MS001', N'Đen', N'#000000', 1, CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[mau_sac] WHERE [id] = 2)
        UPDATE [dbo].[mau_sac] SET [ma_mau_sac] = N'MS002', [ten_mau_sac] = N'Trắng', [ma_mau_hex] = N'#FFFFFF', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2) WHERE [id] = 2;
    ELSE
        INSERT [dbo].[mau_sac] ([id], [ma_mau_sac], [ten_mau_sac], [ma_mau_hex], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (2, N'MS002', N'Trắng', N'#FFFFFF', 1, CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[mau_sac] WHERE [id] = 3)
        UPDATE [dbo].[mau_sac] SET [ma_mau_sac] = N'MS003', [ten_mau_sac] = N'Đỏ', [ma_mau_hex] = N'#FF0000', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2) WHERE [id] = 3;
    ELSE
        INSERT [dbo].[mau_sac] ([id], [ma_mau_sac], [ten_mau_sac], [ma_mau_hex], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (3, N'MS003', N'Đỏ', N'#FF0000', 1, CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[mau_sac] WHERE [id] = 4)
        UPDATE [dbo].[mau_sac] SET [ma_mau_sac] = N'MS004', [ten_mau_sac] = N'Xanh dương', [ma_mau_hex] = N'#0066FF', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2) WHERE [id] = 4;
    ELSE
        INSERT [dbo].[mau_sac] ([id], [ma_mau_sac], [ten_mau_sac], [ma_mau_hex], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (4, N'MS004', N'Xanh dương', N'#0066FF', 1, CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[mau_sac] WHERE [id] = 5)
        UPDATE [dbo].[mau_sac] SET [ma_mau_sac] = N'MS005', [ten_mau_sac] = N'Xám', [ma_mau_hex] = N'#808080', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2) WHERE [id] = 5;
    ELSE
        INSERT [dbo].[mau_sac] ([id], [ma_mau_sac], [ten_mau_sac], [ma_mau_hex], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (5, N'MS005', N'Xám', N'#808080', 1, CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[mau_sac] WHERE [id] = 6)
        UPDATE [dbo].[mau_sac] SET [ma_mau_sac] = N'MS006', [ten_mau_sac] = N'Xanh lá', [ma_mau_hex] = N'#00A651', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2) WHERE [id] = 6;
    ELSE
        INSERT [dbo].[mau_sac] ([id], [ma_mau_sac], [ten_mau_sac], [ma_mau_hex], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (6, N'MS006', N'Xanh lá', N'#00A651', 1, CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[mau_sac] WHERE [id] = 7)
        UPDATE [dbo].[mau_sac] SET [ma_mau_sac] = N'MS007', [ten_mau_sac] = N'Be', [ma_mau_hex] = N'#F5F5DC', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2) WHERE [id] = 7;
    ELSE
        INSERT [dbo].[mau_sac] ([id], [ma_mau_sac], [ten_mau_sac], [ma_mau_hex], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (7, N'MS007', N'Be', N'#F5F5DC', 1, CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[mau_sac] WHERE [id] = 8)
        UPDATE [dbo].[mau_sac] SET [ma_mau_sac] = N'MS008', [ten_mau_sac] = N'Nâu', [ma_mau_hex] = N'#8B4513', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2) WHERE [id] = 8;
    ELSE
        INSERT [dbo].[mau_sac] ([id], [ma_mau_sac], [ten_mau_sac], [ma_mau_hex], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (8, N'MS008', N'Nâu', N'#8B4513', 1, CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[mau_sac] WHERE [id] = 9)
        UPDATE [dbo].[mau_sac] SET [ma_mau_sac] = N'MS009', [ten_mau_sac] = N'Vàng', [ma_mau_hex] = N'#FFD700', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2) WHERE [id] = 9;
    ELSE
        INSERT [dbo].[mau_sac] ([id], [ma_mau_sac], [ten_mau_sac], [ma_mau_hex], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (9, N'MS009', N'Vàng', N'#FFD700', 1, CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[mau_sac] WHERE [id] = 10)
        UPDATE [dbo].[mau_sac] SET [ma_mau_sac] = N'MS010', [ten_mau_sac] = N'Cam', [ma_mau_hex] = N'#FF8C00', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2) WHERE [id] = 10;
    ELSE
        INSERT [dbo].[mau_sac] ([id], [ma_mau_sac], [ten_mau_sac], [ma_mau_hex], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (10, N'MS010', N'Cam', N'#FF8C00', 1, CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[mau_sac] WHERE [id] = 11)
        UPDATE [dbo].[mau_sac] SET [ma_mau_sac] = N'MS011', [ten_mau_sac] = N'Hồng', [ma_mau_hex] = N'#FF69B4', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2) WHERE [id] = 11;
    ELSE
        INSERT [dbo].[mau_sac] ([id], [ma_mau_sac], [ten_mau_sac], [ma_mau_hex], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (11, N'MS011', N'Hồng', N'#FF69B4', 1, CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[mau_sac] WHERE [id] = 12)
        UPDATE [dbo].[mau_sac] SET [ma_mau_sac] = N'MS012', [ten_mau_sac] = N'Tím', [ma_mau_hex] = N'#800080', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2) WHERE [id] = 12;
    ELSE
        INSERT [dbo].[mau_sac] ([id], [ma_mau_sac], [ten_mau_sac], [ma_mau_hex], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (12, N'MS012', N'Tím', N'#800080', 1, CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[mau_sac] WHERE [id] = 13)
        UPDATE [dbo].[mau_sac] SET [ma_mau_sac] = N'MS013', [ten_mau_sac] = N'Xanh navy', [ma_mau_hex] = N'#000080', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2) WHERE [id] = 13;
    ELSE
        INSERT [dbo].[mau_sac] ([id], [ma_mau_sac], [ten_mau_sac], [ma_mau_hex], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (13, N'MS013', N'Xanh navy', N'#000080', 1, CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[mau_sac] WHERE [id] = 14)
        UPDATE [dbo].[mau_sac] SET [ma_mau_sac] = N'MS014', [ten_mau_sac] = N'Kem', [ma_mau_hex] = N'#FFFDD0', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2) WHERE [id] = 14;
    ELSE
        INSERT [dbo].[mau_sac] ([id], [ma_mau_sac], [ten_mau_sac], [ma_mau_hex], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (14, N'MS014', N'Kem', N'#FFFDD0', 1, CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[mau_sac] WHERE [id] = 15)
        UPDATE [dbo].[mau_sac] SET [ma_mau_sac] = N'MS015', [ten_mau_sac] = N'Bạc', [ma_mau_hex] = N'#C0C0C0', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2) WHERE [id] = 15;
    ELSE
        INSERT [dbo].[mau_sac] ([id], [ma_mau_sac], [ten_mau_sac], [ma_mau_hex], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (15, N'MS015', N'Bạc', N'#C0C0C0', 1, CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2));
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[mau_sac]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[mau_sac] OFF;

    -- DATA MERGE: [dbo].[nhan_vien] (7 dong snapshot)
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[nhan_vien]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[nhan_vien] ON;
    IF EXISTS (SELECT 1 FROM [dbo].[nhan_vien] WHERE [id] = 1)
        UPDATE [dbo].[nhan_vien] SET [id_vai_tro] = 1, [ma_nhan_vien] = N'NV001', [ten_dang_nhap] = N'nguyenvana', [ten_nhan_vien] = N'Nguyễn Văn A', [email] = N'nguyenvana@smashstep.local', [mat_khau] = NULL, [so_dien_thoai] = N'0900000001', [gioi_tinh] = NULL, [ngay_sinh] = NULL, [dia_chi] = NULL, [tinh_thanh] = NULL, [phuong_xa] = NULL, [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-06T14:14:49.3578157' AS DateTime2), [hinh_anh] = NULL, [ngay_cap_nhat] = CAST(N'2026-10-06T14:14:49.3578157' AS DateTime2) WHERE [id] = 1;
    ELSE
        INSERT [dbo].[nhan_vien] ([id], [id_vai_tro], [ma_nhan_vien], [ten_dang_nhap], [ten_nhan_vien], [email], [mat_khau], [so_dien_thoai], [gioi_tinh], [ngay_sinh], [dia_chi], [tinh_thanh], [phuong_xa], [trang_thai], [ngay_tao], [hinh_anh], [ngay_cap_nhat]) VALUES (1, 1, N'NV001', N'nguyenvana', N'Nguyễn Văn A', N'nguyenvana@smashstep.local', NULL, N'0900000001', NULL, NULL, NULL, NULL, NULL, 1, CAST(N'2026-10-06T14:14:49.3578157' AS DateTime2), NULL, CAST(N'2026-10-06T14:14:49.3578157' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[nhan_vien] WHERE [id] = 2)
        UPDATE [dbo].[nhan_vien] SET [id_vai_tro] = 1, [ma_nhan_vien] = N'NV002', [ten_dang_nhap] = N'khanhha', [ten_nhan_vien] = N'Khánh Hà', [email] = N'khanhha@smashstep.local', [mat_khau] = NULL, [so_dien_thoai] = N'0900000002', [gioi_tinh] = NULL, [ngay_sinh] = NULL, [dia_chi] = NULL, [tinh_thanh] = NULL, [phuong_xa] = NULL, [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-06T14:14:49.3578157' AS DateTime2), [hinh_anh] = NULL, [ngay_cap_nhat] = CAST(N'2026-10-06T14:14:49.3578157' AS DateTime2) WHERE [id] = 2;
    ELSE
        INSERT [dbo].[nhan_vien] ([id], [id_vai_tro], [ma_nhan_vien], [ten_dang_nhap], [ten_nhan_vien], [email], [mat_khau], [so_dien_thoai], [gioi_tinh], [ngay_sinh], [dia_chi], [tinh_thanh], [phuong_xa], [trang_thai], [ngay_tao], [hinh_anh], [ngay_cap_nhat]) VALUES (2, 1, N'NV002', N'khanhha', N'Khánh Hà', N'khanhha@smashstep.local', NULL, N'0900000002', NULL, NULL, NULL, NULL, NULL, 1, CAST(N'2026-10-06T14:14:49.3578157' AS DateTime2), NULL, CAST(N'2026-10-06T14:14:49.3578157' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[nhan_vien] WHERE [id] = 3)
        UPDATE [dbo].[nhan_vien] SET [id_vai_tro] = 1, [ma_nhan_vien] = N'NV003', [ten_dang_nhap] = N'tranhuy', [ten_nhan_vien] = N'Trần Huy', [email] = N'tranhuy@smashstep.local', [mat_khau] = NULL, [so_dien_thoai] = N'0900000003', [gioi_tinh] = NULL, [ngay_sinh] = NULL, [dia_chi] = NULL, [tinh_thanh] = NULL, [phuong_xa] = NULL, [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-06T14:14:49.3578157' AS DateTime2), [hinh_anh] = NULL, [ngay_cap_nhat] = CAST(N'2026-10-06T14:14:49.3578157' AS DateTime2) WHERE [id] = 3;
    ELSE
        INSERT [dbo].[nhan_vien] ([id], [id_vai_tro], [ma_nhan_vien], [ten_dang_nhap], [ten_nhan_vien], [email], [mat_khau], [so_dien_thoai], [gioi_tinh], [ngay_sinh], [dia_chi], [tinh_thanh], [phuong_xa], [trang_thai], [ngay_tao], [hinh_anh], [ngay_cap_nhat]) VALUES (3, 1, N'NV003', N'tranhuy', N'Trần Huy', N'tranhuy@smashstep.local', NULL, N'0900000003', NULL, NULL, NULL, NULL, NULL, 1, CAST(N'2026-10-06T14:14:49.3578157' AS DateTime2), NULL, CAST(N'2026-10-06T14:14:49.3578157' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[nhan_vien] WHERE [id] = 6)
        UPDATE [dbo].[nhan_vien] SET [id_vai_tro] = 2, [ma_nhan_vien] = N'NV0004', [ten_dang_nhap] = N'demo.nhanvien.1', [ten_nhan_vien] = N'Nhân viên demo 1', [email] = N'demo.employee.1@smashstep.example', [mat_khau] = NULL, [so_dien_thoai] = N'0979900001', [gioi_tinh] = 1, [ngay_sinh] = CAST(N'1996-05-15' AS Date), [dia_chi] = N'10 Nguyễn Trãi', [tinh_thanh] = N'Hà Nội', [phuong_xa] = N'Phường Thanh Xuân', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), [hinh_anh] = NULL, [ngay_cap_nhat] = CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2) WHERE [id] = 6;
    ELSE
        INSERT [dbo].[nhan_vien] ([id], [id_vai_tro], [ma_nhan_vien], [ten_dang_nhap], [ten_nhan_vien], [email], [mat_khau], [so_dien_thoai], [gioi_tinh], [ngay_sinh], [dia_chi], [tinh_thanh], [phuong_xa], [trang_thai], [ngay_tao], [hinh_anh], [ngay_cap_nhat]) VALUES (6, 2, N'NV0004', N'demo.nhanvien.1', N'Nhân viên demo 1', N'demo.employee.1@smashstep.example', NULL, N'0979900001', 1, CAST(N'1996-05-15' AS Date), N'10 Nguyễn Trãi', N'Hà Nội', N'Phường Thanh Xuân', 1, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), NULL, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[nhan_vien] WHERE [id] = 7)
        UPDATE [dbo].[nhan_vien] SET [id_vai_tro] = 1, [ma_nhan_vien] = N'NV0005', [ten_dang_nhap] = N'demo.nhanvien.2', [ten_nhan_vien] = N'Nhân viên demo 2', [email] = N'demo.employee.2@smashstep.example', [mat_khau] = NULL, [so_dien_thoai] = N'0979900002', [gioi_tinh] = 2, [ngay_sinh] = CAST(N'1997-05-15' AS Date), [dia_chi] = N'20 Nguyễn Trãi', [tinh_thanh] = N'Hà Nội', [phuong_xa] = N'Phường Thanh Xuân', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), [hinh_anh] = NULL, [ngay_cap_nhat] = CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2) WHERE [id] = 7;
    ELSE
        INSERT [dbo].[nhan_vien] ([id], [id_vai_tro], [ma_nhan_vien], [ten_dang_nhap], [ten_nhan_vien], [email], [mat_khau], [so_dien_thoai], [gioi_tinh], [ngay_sinh], [dia_chi], [tinh_thanh], [phuong_xa], [trang_thai], [ngay_tao], [hinh_anh], [ngay_cap_nhat]) VALUES (7, 1, N'NV0005', N'demo.nhanvien.2', N'Nhân viên demo 2', N'demo.employee.2@smashstep.example', NULL, N'0979900002', 2, CAST(N'1997-05-15' AS Date), N'20 Nguyễn Trãi', N'Hà Nội', N'Phường Thanh Xuân', 1, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), NULL, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[nhan_vien] WHERE [id] = 8)
        UPDATE [dbo].[nhan_vien] SET [id_vai_tro] = 1, [ma_nhan_vien] = N'NV0006', [ten_dang_nhap] = N'demo.nhanvien.3', [ten_nhan_vien] = N'Nhân viên demo 3', [email] = N'demo.employee.3@smashstep.example', [mat_khau] = NULL, [so_dien_thoai] = N'0979900003', [gioi_tinh] = 1, [ngay_sinh] = CAST(N'1998-05-15' AS Date), [dia_chi] = N'30 Nguyễn Trãi', [tinh_thanh] = N'Hà Nội', [phuong_xa] = N'Phường Thanh Xuân', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), [hinh_anh] = NULL, [ngay_cap_nhat] = CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2) WHERE [id] = 8;
    ELSE
        INSERT [dbo].[nhan_vien] ([id], [id_vai_tro], [ma_nhan_vien], [ten_dang_nhap], [ten_nhan_vien], [email], [mat_khau], [so_dien_thoai], [gioi_tinh], [ngay_sinh], [dia_chi], [tinh_thanh], [phuong_xa], [trang_thai], [ngay_tao], [hinh_anh], [ngay_cap_nhat]) VALUES (8, 1, N'NV0006', N'demo.nhanvien.3', N'Nhân viên demo 3', N'demo.employee.3@smashstep.example', NULL, N'0979900003', 1, CAST(N'1998-05-15' AS Date), N'30 Nguyễn Trãi', N'Hà Nội', N'Phường Thanh Xuân', 1, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), NULL, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2));
    IF EXISTS (SELECT 1 FROM [dbo].[nhan_vien] WHERE [id] = 11)
        UPDATE [dbo].[nhan_vien] SET [id_vai_tro] = 2, [ma_nhan_vien] = N'NV0011', [ten_dang_nhap] = N'NV0011', [ten_nhan_vien] = N'DEMO UI Nhân viên 20261007 - đã kiểm tra', [email] = N'stabilization.employee.20261007@smashstep.example', [mat_khau] = NULL, [so_dien_thoai] = N'0998800002', [gioi_tinh] = 1, [ngay_sinh] = CAST(N'1995-05-15' AS Date), [dia_chi] = N'40 Đội Cấn - địa chỉ demo UI', [tinh_thanh] = N'Hà Nội', [phuong_xa] = N'Phường Cầu Giấy', [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-07T02:01:08.2531189' AS DateTime2), [hinh_anh] = N'/uploads/avatars/c3505ce5-31cc-44b3-a144-10bdd1a1084c.png', [ngay_cap_nhat] = CAST(N'2026-10-07T03:09:56.9314936' AS DateTime2) WHERE [id] = 11;
    ELSE
        INSERT [dbo].[nhan_vien] ([id], [id_vai_tro], [ma_nhan_vien], [ten_dang_nhap], [ten_nhan_vien], [email], [mat_khau], [so_dien_thoai], [gioi_tinh], [ngay_sinh], [dia_chi], [tinh_thanh], [phuong_xa], [trang_thai], [ngay_tao], [hinh_anh], [ngay_cap_nhat]) VALUES (11, 2, N'NV0011', N'NV0011', N'DEMO UI Nhân viên 20261007 - đã kiểm tra', N'stabilization.employee.20261007@smashstep.example', NULL, N'0998800002', 1, CAST(N'1995-05-15' AS Date), N'40 Đội Cấn - địa chỉ demo UI', N'Hà Nội', N'Phường Cầu Giấy', 1, CAST(N'2026-10-07T02:01:08.2531189' AS DateTime2), N'/uploads/avatars/c3505ce5-31cc-44b3-a144-10bdd1a1084c.png', CAST(N'2026-10-07T03:09:56.9314936' AS DateTime2));
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[nhan_vien]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[nhan_vien] OFF;

    -- DATA MERGE: [dbo].[phieu_giam_gia] (7 dong snapshot)
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[phieu_giam_gia]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[phieu_giam_gia] ON;
    IF EXISTS (SELECT 1 FROM [dbo].[phieu_giam_gia] WHERE [id] = 1)
        UPDATE [dbo].[phieu_giam_gia] SET [ma_phieu_giam_gia] = N'PGG001', [ten_phieu_giam_gia] = N'DEMO - Phiếu công khai 10 phần trăm', [loai_giam_gia] = 1, [gia_tri_giam] = CAST(10.00 AS Decimal(18, 2)), [gia_tri_toi_thieu] = CAST(300000.00 AS Decimal(18, 2)), [giam_toi_da] = CAST(100000.00 AS Decimal(18, 2)), [ngay_bat_dau] = CAST(N'2026-10-06T00:00:00.0000000' AS DateTime2), [ngay_ket_thuc] = CAST(N'2026-11-06T23:59:59.0000000' AS DateTime2), [so_luong] = 100, [so_luong_da_dung] = 0, [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), [mo_ta] = N'Dữ liệu demo tích hợp; giữ nguyên dữ liệu cũ', [vo_han] = 0 WHERE [id] = 1;
    ELSE
        INSERT [dbo].[phieu_giam_gia] ([id], [ma_phieu_giam_gia], [ten_phieu_giam_gia], [loai_giam_gia], [gia_tri_giam], [gia_tri_toi_thieu], [giam_toi_da], [ngay_bat_dau], [ngay_ket_thuc], [so_luong], [so_luong_da_dung], [trang_thai], [ngay_tao], [ngay_cap_nhat], [mo_ta], [vo_han]) VALUES (1, N'PGG001', N'DEMO - Phiếu công khai 10 phần trăm', 1, CAST(10.00 AS Decimal(18, 2)), CAST(300000.00 AS Decimal(18, 2)), CAST(100000.00 AS Decimal(18, 2)), CAST(N'2026-10-06T00:00:00.0000000' AS DateTime2), CAST(N'2026-11-06T23:59:59.0000000' AS DateTime2), 100, 0, 1, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), N'Dữ liệu demo tích hợp; giữ nguyên dữ liệu cũ', 0);
    IF EXISTS (SELECT 1 FROM [dbo].[phieu_giam_gia] WHERE [id] = 2)
        UPDATE [dbo].[phieu_giam_gia] SET [ma_phieu_giam_gia] = N'PGG002', [ten_phieu_giam_gia] = N'DEMO - Phiếu công khai 50 nghìn', [loai_giam_gia] = 2, [gia_tri_giam] = CAST(50000.00 AS Decimal(18, 2)), [gia_tri_toi_thieu] = CAST(300000.00 AS Decimal(18, 2)), [giam_toi_da] = CAST(50000.00 AS Decimal(18, 2)), [ngay_bat_dau] = CAST(N'2026-10-06T00:00:00.0000000' AS DateTime2), [ngay_ket_thuc] = CAST(N'2026-11-06T23:59:59.0000000' AS DateTime2), [so_luong] = 100, [so_luong_da_dung] = 0, [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), [mo_ta] = N'Dữ liệu demo tích hợp; giữ nguyên dữ liệu cũ', [vo_han] = 0 WHERE [id] = 2;
    ELSE
        INSERT [dbo].[phieu_giam_gia] ([id], [ma_phieu_giam_gia], [ten_phieu_giam_gia], [loai_giam_gia], [gia_tri_giam], [gia_tri_toi_thieu], [giam_toi_da], [ngay_bat_dau], [ngay_ket_thuc], [so_luong], [so_luong_da_dung], [trang_thai], [ngay_tao], [ngay_cap_nhat], [mo_ta], [vo_han]) VALUES (2, N'PGG002', N'DEMO - Phiếu công khai 50 nghìn', 2, CAST(50000.00 AS Decimal(18, 2)), CAST(300000.00 AS Decimal(18, 2)), CAST(50000.00 AS Decimal(18, 2)), CAST(N'2026-10-06T00:00:00.0000000' AS DateTime2), CAST(N'2026-11-06T23:59:59.0000000' AS DateTime2), 100, 0, 1, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), N'Dữ liệu demo tích hợp; giữ nguyên dữ liệu cũ', 0);
    IF EXISTS (SELECT 1 FROM [dbo].[phieu_giam_gia] WHERE [id] = 3)
        UPDATE [dbo].[phieu_giam_gia] SET [ma_phieu_giam_gia] = N'PGG003', [ten_phieu_giam_gia] = N'DEMO - Phiếu cá nhân 20 phần trăm', [loai_giam_gia] = 1, [gia_tri_giam] = CAST(20.00 AS Decimal(18, 2)), [gia_tri_toi_thieu] = CAST(300000.00 AS Decimal(18, 2)), [giam_toi_da] = CAST(100000.00 AS Decimal(18, 2)), [ngay_bat_dau] = CAST(N'2026-10-06T00:00:00.0000000' AS DateTime2), [ngay_ket_thuc] = CAST(N'2026-11-06T23:59:59.0000000' AS DateTime2), [so_luong] = 100, [so_luong_da_dung] = 0, [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), [mo_ta] = N'Dữ liệu demo tích hợp; giữ nguyên dữ liệu cũ', [vo_han] = 0 WHERE [id] = 3;
    ELSE
        INSERT [dbo].[phieu_giam_gia] ([id], [ma_phieu_giam_gia], [ten_phieu_giam_gia], [loai_giam_gia], [gia_tri_giam], [gia_tri_toi_thieu], [giam_toi_da], [ngay_bat_dau], [ngay_ket_thuc], [so_luong], [so_luong_da_dung], [trang_thai], [ngay_tao], [ngay_cap_nhat], [mo_ta], [vo_han]) VALUES (3, N'PGG003', N'DEMO - Phiếu cá nhân 20 phần trăm', 1, CAST(20.00 AS Decimal(18, 2)), CAST(300000.00 AS Decimal(18, 2)), CAST(100000.00 AS Decimal(18, 2)), CAST(N'2026-10-06T00:00:00.0000000' AS DateTime2), CAST(N'2026-11-06T23:59:59.0000000' AS DateTime2), 100, 0, 1, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), N'Dữ liệu demo tích hợp; giữ nguyên dữ liệu cũ', 0);
    IF EXISTS (SELECT 1 FROM [dbo].[phieu_giam_gia] WHERE [id] = 4)
        UPDATE [dbo].[phieu_giam_gia] SET [ma_phieu_giam_gia] = N'PGG004', [ten_phieu_giam_gia] = N'DEMO - Phiếu cá nhân 100 nghìn', [loai_giam_gia] = 2, [gia_tri_giam] = CAST(100000.00 AS Decimal(18, 2)), [gia_tri_toi_thieu] = CAST(300000.00 AS Decimal(18, 2)), [giam_toi_da] = CAST(100000.00 AS Decimal(18, 2)), [ngay_bat_dau] = CAST(N'2026-10-06T00:00:00.0000000' AS DateTime2), [ngay_ket_thuc] = CAST(N'2026-11-06T23:59:59.0000000' AS DateTime2), [so_luong] = 100, [so_luong_da_dung] = 0, [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), [mo_ta] = N'Dữ liệu demo tích hợp; giữ nguyên dữ liệu cũ', [vo_han] = 0 WHERE [id] = 4;
    ELSE
        INSERT [dbo].[phieu_giam_gia] ([id], [ma_phieu_giam_gia], [ten_phieu_giam_gia], [loai_giam_gia], [gia_tri_giam], [gia_tri_toi_thieu], [giam_toi_da], [ngay_bat_dau], [ngay_ket_thuc], [so_luong], [so_luong_da_dung], [trang_thai], [ngay_tao], [ngay_cap_nhat], [mo_ta], [vo_han]) VALUES (4, N'PGG004', N'DEMO - Phiếu cá nhân 100 nghìn', 2, CAST(100000.00 AS Decimal(18, 2)), CAST(300000.00 AS Decimal(18, 2)), CAST(100000.00 AS Decimal(18, 2)), CAST(N'2026-10-06T00:00:00.0000000' AS DateTime2), CAST(N'2026-11-06T23:59:59.0000000' AS DateTime2), 100, 0, 1, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), N'Dữ liệu demo tích hợp; giữ nguyên dữ liệu cũ', 0);
    IF EXISTS (SELECT 1 FROM [dbo].[phieu_giam_gia] WHERE [id] = 5)
        UPDATE [dbo].[phieu_giam_gia] SET [ma_phieu_giam_gia] = N'PGG005', [ten_phieu_giam_gia] = N'DEMO - Phiếu sắp diễn ra 15 phần trăm', [loai_giam_gia] = 1, [gia_tri_giam] = CAST(15.00 AS Decimal(18, 2)), [gia_tri_toi_thieu] = CAST(300000.00 AS Decimal(18, 2)), [giam_toi_da] = CAST(100000.00 AS Decimal(18, 2)), [ngay_bat_dau] = CAST(N'2026-10-14T00:00:00.0000000' AS DateTime2), [ngay_ket_thuc] = CAST(N'2026-10-27T23:59:59.0000000' AS DateTime2), [so_luong] = 100, [so_luong_da_dung] = 0, [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), [mo_ta] = N'Dữ liệu demo tích hợp; giữ nguyên dữ liệu cũ', [vo_han] = 0 WHERE [id] = 5;
    ELSE
        INSERT [dbo].[phieu_giam_gia] ([id], [ma_phieu_giam_gia], [ten_phieu_giam_gia], [loai_giam_gia], [gia_tri_giam], [gia_tri_toi_thieu], [giam_toi_da], [ngay_bat_dau], [ngay_ket_thuc], [so_luong], [so_luong_da_dung], [trang_thai], [ngay_tao], [ngay_cap_nhat], [mo_ta], [vo_han]) VALUES (5, N'PGG005', N'DEMO - Phiếu sắp diễn ra 15 phần trăm', 1, CAST(15.00 AS Decimal(18, 2)), CAST(300000.00 AS Decimal(18, 2)), CAST(100000.00 AS Decimal(18, 2)), CAST(N'2026-10-14T00:00:00.0000000' AS DateTime2), CAST(N'2026-10-27T23:59:59.0000000' AS DateTime2), 100, 0, 1, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), N'Dữ liệu demo tích hợp; giữ nguyên dữ liệu cũ', 0);
    IF EXISTS (SELECT 1 FROM [dbo].[phieu_giam_gia] WHERE [id] = 6)
        UPDATE [dbo].[phieu_giam_gia] SET [ma_phieu_giam_gia] = N'PGG006', [ten_phieu_giam_gia] = N'DEMO - Phiếu đã kết thúc 5 phần trăm', [loai_giam_gia] = 1, [gia_tri_giam] = CAST(5.00 AS Decimal(18, 2)), [gia_tri_toi_thieu] = CAST(300000.00 AS Decimal(18, 2)), [giam_toi_da] = CAST(100000.00 AS Decimal(18, 2)), [ngay_bat_dau] = CAST(N'2026-09-17T00:00:00.0000000' AS DateTime2), [ngay_ket_thuc] = CAST(N'2026-10-05T23:59:59.0000000' AS DateTime2), [so_luong] = 100, [so_luong_da_dung] = 0, [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), [mo_ta] = N'Dữ liệu demo tích hợp; giữ nguyên dữ liệu cũ', [vo_han] = 0 WHERE [id] = 6;
    ELSE
        INSERT [dbo].[phieu_giam_gia] ([id], [ma_phieu_giam_gia], [ten_phieu_giam_gia], [loai_giam_gia], [gia_tri_giam], [gia_tri_toi_thieu], [giam_toi_da], [ngay_bat_dau], [ngay_ket_thuc], [so_luong], [so_luong_da_dung], [trang_thai], [ngay_tao], [ngay_cap_nhat], [mo_ta], [vo_han]) VALUES (6, N'PGG006', N'DEMO - Phiếu đã kết thúc 5 phần trăm', 1, CAST(5.00 AS Decimal(18, 2)), CAST(300000.00 AS Decimal(18, 2)), CAST(100000.00 AS Decimal(18, 2)), CAST(N'2026-09-17T00:00:00.0000000' AS DateTime2), CAST(N'2026-10-05T23:59:59.0000000' AS DateTime2), 100, 0, 1, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), N'Dữ liệu demo tích hợp; giữ nguyên dữ liệu cũ', 0);
    IF EXISTS (SELECT 1 FROM [dbo].[phieu_giam_gia] WHERE [id] = 10)
        UPDATE [dbo].[phieu_giam_gia] SET [ma_phieu_giam_gia] = N'VCHI480J8', [ten_phieu_giam_gia] = N'DEMO UI 20261007 - Phiếu cá nhân đã kiểm tra', [loai_giam_gia] = 1, [gia_tri_giam] = CAST(5.00 AS Decimal(18, 2)), [gia_tri_toi_thieu] = CAST(0.00 AS Decimal(18, 2)), [giam_toi_da] = CAST(50000.00 AS Decimal(18, 2)), [ngay_bat_dau] = CAST(N'2026-10-06T00:00:00.0000000' AS DateTime2), [ngay_ket_thuc] = CAST(N'2026-11-07T23:59:59.0000000' AS DateTime2), [so_luong] = 10, [so_luong_da_dung] = 0, [trang_thai] = 1, [ngay_tao] = CAST(N'2026-10-07T02:10:14.4036308' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-07T02:11:17.7549493' AS DateTime2), [mo_ta] = N'DEMO UI final stabilization 20261007', [vo_han] = 0 WHERE [id] = 10;
    ELSE
        INSERT [dbo].[phieu_giam_gia] ([id], [ma_phieu_giam_gia], [ten_phieu_giam_gia], [loai_giam_gia], [gia_tri_giam], [gia_tri_toi_thieu], [giam_toi_da], [ngay_bat_dau], [ngay_ket_thuc], [so_luong], [so_luong_da_dung], [trang_thai], [ngay_tao], [ngay_cap_nhat], [mo_ta], [vo_han]) VALUES (10, N'VCHI480J8', N'DEMO UI 20261007 - Phiếu cá nhân đã kiểm tra', 1, CAST(5.00 AS Decimal(18, 2)), CAST(0.00 AS Decimal(18, 2)), CAST(50000.00 AS Decimal(18, 2)), CAST(N'2026-10-06T00:00:00.0000000' AS DateTime2), CAST(N'2026-11-07T23:59:59.0000000' AS DateTime2), 10, 0, 1, CAST(N'2026-10-07T02:10:14.4036308' AS DateTime2), CAST(N'2026-10-07T02:11:17.7549493' AS DateTime2), N'DEMO UI final stabilization 20261007', 0);
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[phieu_giam_gia]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[phieu_giam_gia] OFF;

    -- DATA MERGE: [dbo].[phieu_giam_gia_khach_hang] (5 dong snapshot)
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[phieu_giam_gia_khach_hang]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[phieu_giam_gia_khach_hang] ON;
    IF EXISTS (SELECT 1 FROM [dbo].[phieu_giam_gia_khach_hang] WHERE [id] = 1)
        UPDATE [dbo].[phieu_giam_gia_khach_hang] SET [id_khach_hang] = 1, [id_phieu_giam_gia] = 3, [ngay_su_dung] = NULL, [trang_thai] = 1 WHERE [id] = 1;
    ELSE
        INSERT [dbo].[phieu_giam_gia_khach_hang] ([id], [id_khach_hang], [id_phieu_giam_gia], [ngay_su_dung], [trang_thai]) VALUES (1, 1, 3, NULL, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[phieu_giam_gia_khach_hang] WHERE [id] = 2)
        UPDATE [dbo].[phieu_giam_gia_khach_hang] SET [id_khach_hang] = 2, [id_phieu_giam_gia] = 3, [ngay_su_dung] = NULL, [trang_thai] = 1 WHERE [id] = 2;
    ELSE
        INSERT [dbo].[phieu_giam_gia_khach_hang] ([id], [id_khach_hang], [id_phieu_giam_gia], [ngay_su_dung], [trang_thai]) VALUES (2, 2, 3, NULL, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[phieu_giam_gia_khach_hang] WHERE [id] = 3)
        UPDATE [dbo].[phieu_giam_gia_khach_hang] SET [id_khach_hang] = 1, [id_phieu_giam_gia] = 4, [ngay_su_dung] = NULL, [trang_thai] = 1 WHERE [id] = 3;
    ELSE
        INSERT [dbo].[phieu_giam_gia_khach_hang] ([id], [id_khach_hang], [id_phieu_giam_gia], [ngay_su_dung], [trang_thai]) VALUES (3, 1, 4, NULL, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[phieu_giam_gia_khach_hang] WHERE [id] = 4)
        UPDATE [dbo].[phieu_giam_gia_khach_hang] SET [id_khach_hang] = 2, [id_phieu_giam_gia] = 4, [ngay_su_dung] = NULL, [trang_thai] = 1 WHERE [id] = 4;
    ELSE
        INSERT [dbo].[phieu_giam_gia_khach_hang] ([id], [id_khach_hang], [id_phieu_giam_gia], [ngay_su_dung], [trang_thai]) VALUES (4, 2, 4, NULL, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[phieu_giam_gia_khach_hang] WHERE [id] = 6)
        UPDATE [dbo].[phieu_giam_gia_khach_hang] SET [id_khach_hang] = 17, [id_phieu_giam_gia] = 10, [ngay_su_dung] = NULL, [trang_thai] = 1 WHERE [id] = 6;
    ELSE
        INSERT [dbo].[phieu_giam_gia_khach_hang] ([id], [id_khach_hang], [id_phieu_giam_gia], [ngay_su_dung], [trang_thai]) VALUES (6, 17, 10, NULL, 1);
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[phieu_giam_gia_khach_hang]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[phieu_giam_gia_khach_hang] OFF;

    -- DATA MERGE: [dbo].[phuong_thuc_thanh_toan] (4 dong snapshot)
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[phuong_thuc_thanh_toan]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[phuong_thuc_thanh_toan] ON;
    IF EXISTS (SELECT 1 FROM [dbo].[phuong_thuc_thanh_toan] WHERE [id] = 1)
        UPDATE [dbo].[phuong_thuc_thanh_toan] SET [id_hinh_thuc_thanh_toan] = 1, [ma_phuong_thuc] = N'TIEN_MAT', [ten_phuong_thuc] = N'Tiền mặt', [trang_thai] = 1 WHERE [id] = 1;
    ELSE
        INSERT [dbo].[phuong_thuc_thanh_toan] ([id], [id_hinh_thuc_thanh_toan], [ma_phuong_thuc], [ten_phuong_thuc], [trang_thai]) VALUES (1, 1, N'TIEN_MAT', N'Tiền mặt', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[phuong_thuc_thanh_toan] WHERE [id] = 2)
        UPDATE [dbo].[phuong_thuc_thanh_toan] SET [id_hinh_thuc_thanh_toan] = 1, [ma_phuong_thuc] = N'THE', [ten_phuong_thuc] = N'Thẻ', [trang_thai] = 1 WHERE [id] = 2;
    ELSE
        INSERT [dbo].[phuong_thuc_thanh_toan] ([id], [id_hinh_thuc_thanh_toan], [ma_phuong_thuc], [ten_phuong_thuc], [trang_thai]) VALUES (2, 1, N'THE', N'Thẻ', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[phuong_thuc_thanh_toan] WHERE [id] = 3)
        UPDATE [dbo].[phuong_thuc_thanh_toan] SET [id_hinh_thuc_thanh_toan] = 2, [ma_phuong_thuc] = N'CHUYEN_KHOAN', [ten_phuong_thuc] = N'Chuyển khoản', [trang_thai] = 1 WHERE [id] = 3;
    ELSE
        INSERT [dbo].[phuong_thuc_thanh_toan] ([id], [id_hinh_thuc_thanh_toan], [ma_phuong_thuc], [ten_phuong_thuc], [trang_thai]) VALUES (3, 2, N'CHUYEN_KHOAN', N'Chuyển khoản', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[phuong_thuc_thanh_toan] WHERE [id] = 4)
        UPDATE [dbo].[phuong_thuc_thanh_toan] SET [id_hinh_thuc_thanh_toan] = 2, [ma_phuong_thuc] = N'COD', [ten_phuong_thuc] = N'COD', [trang_thai] = 1 WHERE [id] = 4;
    ELSE
        INSERT [dbo].[phuong_thuc_thanh_toan] ([id], [id_hinh_thuc_thanh_toan], [ma_phuong_thuc], [ten_phuong_thuc], [trang_thai]) VALUES (4, 2, N'COD', N'COD', 1);
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[phuong_thuc_thanh_toan]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[phuong_thuc_thanh_toan] OFF;

    -- DATA MERGE: [dbo].[san_pham] (5 dong snapshot)
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[san_pham]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[san_pham] ON;
    IF EXISTS (SELECT 1 FROM [dbo].[san_pham] WHERE [id] = 1)
        UPDATE [dbo].[san_pham] SET [id_danh_muc] = 1, [id_thuong_hieu] = 1, [id_chat_lieu] = 1, [id_kieu_dang] = 1, [id_co_giay] = 1, [id_xuat_xu] = 1, [ma_san_pham] = N'SP001', [ten_san_pham] = N'Nike Air Max Running 2026', [mo_ta_chi_tiet] = N'Giày Nike chạy bộ phiên bản cập nhật 2026', [ngay_tao] = CAST(N'2026-10-03T21:44:00.0954595' AS DateTime2), [nguoi_tao] = NULL, [nguoi_cap_nhat] = NULL, [ngay_cap_nhat] = CAST(N'2026-10-03T21:46:11.9647551' AS DateTime2), [trang_thai] = 1 WHERE [id] = 1;
    ELSE
        INSERT [dbo].[san_pham] ([id], [id_danh_muc], [id_thuong_hieu], [id_chat_lieu], [id_kieu_dang], [id_co_giay], [id_xuat_xu], [ma_san_pham], [ten_san_pham], [mo_ta_chi_tiet], [ngay_tao], [nguoi_tao], [nguoi_cap_nhat], [ngay_cap_nhat], [trang_thai]) VALUES (1, 1, 1, 1, 1, 1, 1, N'SP001', N'Nike Air Max Running 2026', N'Giày Nike chạy bộ phiên bản cập nhật 2026', CAST(N'2026-10-03T21:44:00.0954595' AS DateTime2), NULL, NULL, CAST(N'2026-10-03T21:46:11.9647551' AS DateTime2), 1);
    IF EXISTS (SELECT 1 FROM [dbo].[san_pham] WHERE [id] = 2)
        UPDATE [dbo].[san_pham] SET [id_danh_muc] = 8, [id_thuong_hieu] = 10, [id_chat_lieu] = 8, [id_kieu_dang] = 8, [id_co_giay] = 3, [id_xuat_xu] = 1, [ma_san_pham] = N'SP0002', [ten_san_pham] = N'giay 123', [mo_ta_chi_tiet] = N'sp tét', [ngay_tao] = CAST(N'2026-10-03T22:21:21.7679075' AS DateTime2), [nguoi_tao] = NULL, [nguoi_cap_nhat] = NULL, [ngay_cap_nhat] = NULL, [trang_thai] = 1 WHERE [id] = 2;
    ELSE
        INSERT [dbo].[san_pham] ([id], [id_danh_muc], [id_thuong_hieu], [id_chat_lieu], [id_kieu_dang], [id_co_giay], [id_xuat_xu], [ma_san_pham], [ten_san_pham], [mo_ta_chi_tiet], [ngay_tao], [nguoi_tao], [nguoi_cap_nhat], [ngay_cap_nhat], [trang_thai]) VALUES (2, 8, 10, 8, 8, 3, 1, N'SP0002', N'giay 123', N'sp tét', CAST(N'2026-10-03T22:21:21.7679075' AS DateTime2), NULL, NULL, NULL, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[san_pham] WHERE [id] = 3)
        UPDATE [dbo].[san_pham] SET [id_danh_muc] = 4, [id_thuong_hieu] = 3, [id_chat_lieu] = 3, [id_kieu_dang] = 3, [id_co_giay] = 1, [id_xuat_xu] = 8, [ma_san_pham] = N'SP0004', [ten_san_pham] = N'Quang', [mo_ta_chi_tiet] = N'', [ngay_tao] = CAST(N'2026-10-03T23:33:55.0485766' AS DateTime2), [nguoi_tao] = NULL, [nguoi_cap_nhat] = NULL, [ngay_cap_nhat] = NULL, [trang_thai] = 1 WHERE [id] = 3;
    ELSE
        INSERT [dbo].[san_pham] ([id], [id_danh_muc], [id_thuong_hieu], [id_chat_lieu], [id_kieu_dang], [id_co_giay], [id_xuat_xu], [ma_san_pham], [ten_san_pham], [mo_ta_chi_tiet], [ngay_tao], [nguoi_tao], [nguoi_cap_nhat], [ngay_cap_nhat], [trang_thai]) VALUES (3, 4, 3, 3, 3, 1, 8, N'SP0004', N'Quang', N'', CAST(N'2026-10-03T23:33:55.0485766' AS DateTime2), NULL, NULL, NULL, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[san_pham] WHERE [id] = 4)
        UPDATE [dbo].[san_pham] SET [id_danh_muc] = 8, [id_thuong_hieu] = 5, [id_chat_lieu] = 9, [id_kieu_dang] = 10, [id_co_giay] = 2, [id_xuat_xu] = 7, [ma_san_pham] = N'SP005', [ten_san_pham] = N'test', [mo_ta_chi_tiet] = N'ok', [ngay_tao] = CAST(N'2026-10-04T00:31:58.8255698' AS DateTime2), [nguoi_tao] = NULL, [nguoi_cap_nhat] = NULL, [ngay_cap_nhat] = CAST(N'2026-10-04T18:43:22.2209574' AS DateTime2), [trang_thai] = 0 WHERE [id] = 4;
    ELSE
        INSERT [dbo].[san_pham] ([id], [id_danh_muc], [id_thuong_hieu], [id_chat_lieu], [id_kieu_dang], [id_co_giay], [id_xuat_xu], [ma_san_pham], [ten_san_pham], [mo_ta_chi_tiet], [ngay_tao], [nguoi_tao], [nguoi_cap_nhat], [ngay_cap_nhat], [trang_thai]) VALUES (4, 8, 5, 9, 10, 2, 7, N'SP005', N'test', N'ok', CAST(N'2026-10-04T00:31:58.8255698' AS DateTime2), NULL, NULL, CAST(N'2026-10-04T18:43:22.2209574' AS DateTime2), 0);
    IF EXISTS (SELECT 1 FROM [dbo].[san_pham] WHERE [id] = 88)
        UPDATE [dbo].[san_pham] SET [id_danh_muc] = 8, [id_thuong_hieu] = 10, [id_chat_lieu] = 10, [id_kieu_dang] = 1, [id_co_giay] = 3, [id_xuat_xu] = 1, [ma_san_pham] = N'SP006', [ten_san_pham] = N'Test2', [mo_ta_chi_tiet] = N'SP test', [ngay_tao] = CAST(N'2026-10-04T19:04:54.0609518' AS DateTime2), [nguoi_tao] = NULL, [nguoi_cap_nhat] = NULL, [ngay_cap_nhat] = NULL, [trang_thai] = 1 WHERE [id] = 88;
    ELSE
        INSERT [dbo].[san_pham] ([id], [id_danh_muc], [id_thuong_hieu], [id_chat_lieu], [id_kieu_dang], [id_co_giay], [id_xuat_xu], [ma_san_pham], [ten_san_pham], [mo_ta_chi_tiet], [ngay_tao], [nguoi_tao], [nguoi_cap_nhat], [ngay_cap_nhat], [trang_thai]) VALUES (88, 8, 10, 10, 1, 3, 1, N'SP006', N'Test2', N'SP test', CAST(N'2026-10-04T19:04:54.0609518' AS DateTime2), NULL, NULL, NULL, 1);
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[san_pham]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[san_pham] OFF;

    -- DATA MERGE: [dbo].[san_pham_chi_tiet] (27 dong snapshot)
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[san_pham_chi_tiet]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[san_pham_chi_tiet] ON;
    IF EXISTS (SELECT 1 FROM [dbo].[san_pham_chi_tiet] WHERE [id] = 1)
        UPDATE [dbo].[san_pham_chi_tiet] SET [id_san_pham] = 1, [id_mau_sac] = 1, [id_kich_thuoc] = 10, [ma_chi_tiet_san_pham] = N'SP001-DEN-40', [so_luong] = 20, [gia_ban] = CAST(1599000.00 AS Decimal(18, 2)), [sku] = N'NIKE-SP001-BLK-40', [kich_hoat] = 1, [ngay_tao] = CAST(N'2026-10-03T21:47:12.3180747' AS DateTime2), [ngay_cap_nhat] = NULL, [trang_thai] = 1 WHERE [id] = 1;
    ELSE
        INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (1, 1, 1, 10, N'SP001-DEN-40', 20, CAST(1599000.00 AS Decimal(18, 2)), N'NIKE-SP001-BLK-40', 1, CAST(N'2026-10-03T21:47:12.3180747' AS DateTime2), NULL, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[san_pham_chi_tiet] WHERE [id] = 2)
        UPDATE [dbo].[san_pham_chi_tiet] SET [id_san_pham] = 1, [id_mau_sac] = 2, [id_kich_thuoc] = 12, [ma_chi_tiet_san_pham] = N'SP001-TRANG-41', [so_luong] = 15, [gia_ban] = CAST(1699000.00 AS Decimal(18, 2)), [sku] = N'NIKE-SP001-WHT-41', [kich_hoat] = 1, [ngay_tao] = CAST(N'2026-10-03T21:47:46.3689431' AS DateTime2), [ngay_cap_nhat] = NULL, [trang_thai] = 1 WHERE [id] = 2;
    ELSE
        INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (2, 1, 2, 12, N'SP001-TRANG-41', 15, CAST(1699000.00 AS Decimal(18, 2)), N'NIKE-SP001-WHT-41', 1, CAST(N'2026-10-03T21:47:46.3689431' AS DateTime2), NULL, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[san_pham_chi_tiet] WHERE [id] = 3)
        UPDATE [dbo].[san_pham_chi_tiet] SET [id_san_pham] = 2, [id_mau_sac] = 15, [id_kich_thuoc] = 21, [ma_chi_tiet_san_pham] = N'SP0002-15-21', [so_luong] = 30000, [gia_ban] = CAST(123.00 AS Decimal(18, 2)), [sku] = N'SP0002-15-21', [kich_hoat] = 1, [ngay_tao] = CAST(N'2026-10-03T22:23:59.9446101' AS DateTime2), [ngay_cap_nhat] = NULL, [trang_thai] = 1 WHERE [id] = 3;
    ELSE
        INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (3, 2, 15, 21, N'SP0002-15-21', 30000, CAST(123.00 AS Decimal(18, 2)), N'SP0002-15-21', 1, CAST(N'2026-10-03T22:23:59.9446101' AS DateTime2), NULL, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[san_pham_chi_tiet] WHERE [id] = 4)
        UPDATE [dbo].[san_pham_chi_tiet] SET [id_san_pham] = 2, [id_mau_sac] = 15, [id_kich_thuoc] = 20, [ma_chi_tiet_san_pham] = N'SP0002-15-20', [so_luong] = 30000, [gia_ban] = CAST(123.00 AS Decimal(18, 2)), [sku] = N'SP0002-15-20', [kich_hoat] = 1, [ngay_tao] = CAST(N'2026-10-03T22:23:59.9705398' AS DateTime2), [ngay_cap_nhat] = NULL, [trang_thai] = 1 WHERE [id] = 4;
    ELSE
        INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (4, 2, 15, 20, N'SP0002-15-20', 30000, CAST(123.00 AS Decimal(18, 2)), N'SP0002-15-20', 1, CAST(N'2026-10-03T22:23:59.9705398' AS DateTime2), NULL, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[san_pham_chi_tiet] WHERE [id] = 5)
        UPDATE [dbo].[san_pham_chi_tiet] SET [id_san_pham] = 2, [id_mau_sac] = 10, [id_kich_thuoc] = 21, [ma_chi_tiet_san_pham] = N'SP0002-10-21', [so_luong] = 30000, [gia_ban] = CAST(123.00 AS Decimal(18, 2)), [sku] = N'SP0002-10-21', [kich_hoat] = 1, [ngay_tao] = CAST(N'2026-10-03T22:23:59.9825092' AS DateTime2), [ngay_cap_nhat] = NULL, [trang_thai] = 1 WHERE [id] = 5;
    ELSE
        INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (5, 2, 10, 21, N'SP0002-10-21', 30000, CAST(123.00 AS Decimal(18, 2)), N'SP0002-10-21', 1, CAST(N'2026-10-03T22:23:59.9825092' AS DateTime2), NULL, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[san_pham_chi_tiet] WHERE [id] = 6)
        UPDATE [dbo].[san_pham_chi_tiet] SET [id_san_pham] = 2, [id_mau_sac] = 10, [id_kich_thuoc] = 20, [ma_chi_tiet_san_pham] = N'SP0002-10-20', [so_luong] = 30000, [gia_ban] = CAST(123.00 AS Decimal(18, 2)), [sku] = N'SP0002-10-20', [kich_hoat] = 1, [ngay_tao] = CAST(N'2026-10-03T22:23:59.9894891' AS DateTime2), [ngay_cap_nhat] = NULL, [trang_thai] = 1 WHERE [id] = 6;
    ELSE
        INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (6, 2, 10, 20, N'SP0002-10-20', 30000, CAST(123.00 AS Decimal(18, 2)), N'SP0002-10-20', 1, CAST(N'2026-10-03T22:23:59.9894891' AS DateTime2), NULL, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[san_pham_chi_tiet] WHERE [id] = 7)
        UPDATE [dbo].[san_pham_chi_tiet] SET [id_san_pham] = 2, [id_mau_sac] = 3, [id_kich_thuoc] = 8, [ma_chi_tiet_san_pham] = N'SP0002-3-8', [so_luong] = 4, [gia_ban] = CAST(300000.00 AS Decimal(18, 2)), [sku] = N'SP0002-3-8', [kich_hoat] = 1, [ngay_tao] = CAST(N'2026-10-03T22:44:09.5234285' AS DateTime2), [ngay_cap_nhat] = NULL, [trang_thai] = 1 WHERE [id] = 7;
    ELSE
        INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (7, 2, 3, 8, N'SP0002-3-8', 4, CAST(300000.00 AS Decimal(18, 2)), N'SP0002-3-8', 1, CAST(N'2026-10-03T22:44:09.5234285' AS DateTime2), NULL, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[san_pham_chi_tiet] WHERE [id] = 8)
        UPDATE [dbo].[san_pham_chi_tiet] SET [id_san_pham] = 2, [id_mau_sac] = 3, [id_kich_thuoc] = 17, [ma_chi_tiet_san_pham] = N'SP0002-3-17', [so_luong] = 4, [gia_ban] = CAST(300000.00 AS Decimal(18, 2)), [sku] = N'SP0002-3-17', [kich_hoat] = 1, [ngay_tao] = CAST(N'2026-10-03T22:44:09.5563400' AS DateTime2), [ngay_cap_nhat] = NULL, [trang_thai] = 1 WHERE [id] = 8;
    ELSE
        INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (8, 2, 3, 17, N'SP0002-3-17', 4, CAST(300000.00 AS Decimal(18, 2)), N'SP0002-3-17', 1, CAST(N'2026-10-03T22:44:09.5563400' AS DateTime2), NULL, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[san_pham_chi_tiet] WHERE [id] = 9)
        UPDATE [dbo].[san_pham_chi_tiet] SET [id_san_pham] = 2, [id_mau_sac] = 3, [id_kich_thuoc] = 16, [ma_chi_tiet_san_pham] = N'SP0002-3-16', [so_luong] = 4, [gia_ban] = CAST(300000.00 AS Decimal(18, 2)), [sku] = N'SP0002-3-16', [kich_hoat] = 1, [ngay_tao] = CAST(N'2026-10-03T22:44:09.5633225' AS DateTime2), [ngay_cap_nhat] = NULL, [trang_thai] = 1 WHERE [id] = 9;
    ELSE
        INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (9, 2, 3, 16, N'SP0002-3-16', 4, CAST(300000.00 AS Decimal(18, 2)), N'SP0002-3-16', 1, CAST(N'2026-10-03T22:44:09.5633225' AS DateTime2), NULL, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[san_pham_chi_tiet] WHERE [id] = 10)
        UPDATE [dbo].[san_pham_chi_tiet] SET [id_san_pham] = 2, [id_mau_sac] = 2, [id_kich_thuoc] = 8, [ma_chi_tiet_san_pham] = N'SP0002-2-8', [so_luong] = 4, [gia_ban] = CAST(300000.00 AS Decimal(18, 2)), [sku] = N'SP0002-2-8', [kich_hoat] = 1, [ngay_tao] = CAST(N'2026-10-03T22:44:09.5760964' AS DateTime2), [ngay_cap_nhat] = NULL, [trang_thai] = 1 WHERE [id] = 10;
    ELSE
        INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (10, 2, 2, 8, N'SP0002-2-8', 4, CAST(300000.00 AS Decimal(18, 2)), N'SP0002-2-8', 1, CAST(N'2026-10-03T22:44:09.5760964' AS DateTime2), NULL, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[san_pham_chi_tiet] WHERE [id] = 11)
        UPDATE [dbo].[san_pham_chi_tiet] SET [id_san_pham] = 2, [id_mau_sac] = 2, [id_kich_thuoc] = 17, [ma_chi_tiet_san_pham] = N'SP0002-2-17', [so_luong] = 4, [gia_ban] = CAST(300000.00 AS Decimal(18, 2)), [sku] = N'SP0002-2-17', [kich_hoat] = 1, [ngay_tao] = CAST(N'2026-10-03T22:44:09.5840759' AS DateTime2), [ngay_cap_nhat] = NULL, [trang_thai] = 1 WHERE [id] = 11;
    ELSE
        INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (11, 2, 2, 17, N'SP0002-2-17', 4, CAST(300000.00 AS Decimal(18, 2)), N'SP0002-2-17', 1, CAST(N'2026-10-03T22:44:09.5840759' AS DateTime2), NULL, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[san_pham_chi_tiet] WHERE [id] = 12)
        UPDATE [dbo].[san_pham_chi_tiet] SET [id_san_pham] = 2, [id_mau_sac] = 2, [id_kich_thuoc] = 16, [ma_chi_tiet_san_pham] = N'SP0002-2-16', [so_luong] = 4, [gia_ban] = CAST(300000.00 AS Decimal(18, 2)), [sku] = N'SP0002-2-16', [kich_hoat] = 1, [ngay_tao] = CAST(N'2026-10-03T22:44:09.5950451' AS DateTime2), [ngay_cap_nhat] = NULL, [trang_thai] = 1 WHERE [id] = 12;
    ELSE
        INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (12, 2, 2, 16, N'SP0002-2-16', 4, CAST(300000.00 AS Decimal(18, 2)), N'SP0002-2-16', 1, CAST(N'2026-10-03T22:44:09.5950451' AS DateTime2), NULL, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[san_pham_chi_tiet] WHERE [id] = 13)
        UPDATE [dbo].[san_pham_chi_tiet] SET [id_san_pham] = 2, [id_mau_sac] = 1, [id_kich_thuoc] = 8, [ma_chi_tiet_san_pham] = N'SP0002-1-8', [so_luong] = 4, [gia_ban] = CAST(300000.00 AS Decimal(18, 2)), [sku] = N'SP0002-1-8', [kich_hoat] = 1, [ngay_tao] = CAST(N'2026-10-03T22:44:09.6010308' AS DateTime2), [ngay_cap_nhat] = NULL, [trang_thai] = 1 WHERE [id] = 13;
    ELSE
        INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (13, 2, 1, 8, N'SP0002-1-8', 4, CAST(300000.00 AS Decimal(18, 2)), N'SP0002-1-8', 1, CAST(N'2026-10-03T22:44:09.6010308' AS DateTime2), NULL, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[san_pham_chi_tiet] WHERE [id] = 14)
        UPDATE [dbo].[san_pham_chi_tiet] SET [id_san_pham] = 2, [id_mau_sac] = 1, [id_kich_thuoc] = 17, [ma_chi_tiet_san_pham] = N'SP0002-1-17', [so_luong] = 4, [gia_ban] = CAST(300000.00 AS Decimal(18, 2)), [sku] = N'SP0002-1-17', [kich_hoat] = 1, [ngay_tao] = CAST(N'2026-10-03T22:44:09.6060166' AS DateTime2), [ngay_cap_nhat] = NULL, [trang_thai] = 1 WHERE [id] = 14;
    ELSE
        INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (14, 2, 1, 17, N'SP0002-1-17', 4, CAST(300000.00 AS Decimal(18, 2)), N'SP0002-1-17', 1, CAST(N'2026-10-03T22:44:09.6060166' AS DateTime2), NULL, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[san_pham_chi_tiet] WHERE [id] = 15)
        UPDATE [dbo].[san_pham_chi_tiet] SET [id_san_pham] = 2, [id_mau_sac] = 1, [id_kich_thuoc] = 16, [ma_chi_tiet_san_pham] = N'SP0002-1-16', [so_luong] = 4, [gia_ban] = CAST(300000.00 AS Decimal(18, 2)), [sku] = N'SP0002-1-16', [kich_hoat] = 1, [ngay_tao] = CAST(N'2026-10-03T22:44:09.6110037' AS DateTime2), [ngay_cap_nhat] = NULL, [trang_thai] = 1 WHERE [id] = 15;
    ELSE
        INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (15, 2, 1, 16, N'SP0002-1-16', 4, CAST(300000.00 AS Decimal(18, 2)), N'SP0002-1-16', 1, CAST(N'2026-10-03T22:44:09.6110037' AS DateTime2), NULL, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[san_pham_chi_tiet] WHERE [id] = 16)
        UPDATE [dbo].[san_pham_chi_tiet] SET [id_san_pham] = 3, [id_mau_sac] = 15, [id_kich_thuoc] = 21, [ma_chi_tiet_san_pham] = N'SP0004-15-21', [so_luong] = 4, [gia_ban] = CAST(100000.00 AS Decimal(18, 2)), [sku] = N'SP0004-15-21', [kich_hoat] = 1, [ngay_tao] = CAST(N'2026-10-03T23:34:12.6528769' AS DateTime2), [ngay_cap_nhat] = NULL, [trang_thai] = 1 WHERE [id] = 16;
    ELSE
        INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (16, 3, 15, 21, N'SP0004-15-21', 4, CAST(100000.00 AS Decimal(18, 2)), N'SP0004-15-21', 1, CAST(N'2026-10-03T23:34:12.6528769' AS DateTime2), NULL, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[san_pham_chi_tiet] WHERE [id] = 17)
        UPDATE [dbo].[san_pham_chi_tiet] SET [id_san_pham] = 3, [id_mau_sac] = 15, [id_kich_thuoc] = 17, [ma_chi_tiet_san_pham] = N'SP0004-15-17', [so_luong] = 4, [gia_ban] = CAST(100000.00 AS Decimal(18, 2)), [sku] = N'SP0004-15-17', [kich_hoat] = 1, [ngay_tao] = CAST(N'2026-10-03T23:34:12.6673231' AS DateTime2), [ngay_cap_nhat] = NULL, [trang_thai] = 1 WHERE [id] = 17;
    ELSE
        INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (17, 3, 15, 17, N'SP0004-15-17', 4, CAST(100000.00 AS Decimal(18, 2)), N'SP0004-15-17', 1, CAST(N'2026-10-03T23:34:12.6673231' AS DateTime2), NULL, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[san_pham_chi_tiet] WHERE [id] = 18)
        UPDATE [dbo].[san_pham_chi_tiet] SET [id_san_pham] = 3, [id_mau_sac] = 12, [id_kich_thuoc] = 21, [ma_chi_tiet_san_pham] = N'SP0004-12-21', [so_luong] = 4, [gia_ban] = CAST(100000.00 AS Decimal(18, 2)), [sku] = N'SP0004-12-21', [kich_hoat] = 1, [ngay_tao] = CAST(N'2026-10-03T23:34:12.6713015' AS DateTime2), [ngay_cap_nhat] = NULL, [trang_thai] = 1 WHERE [id] = 18;
    ELSE
        INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (18, 3, 12, 21, N'SP0004-12-21', 4, CAST(100000.00 AS Decimal(18, 2)), N'SP0004-12-21', 1, CAST(N'2026-10-03T23:34:12.6713015' AS DateTime2), NULL, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[san_pham_chi_tiet] WHERE [id] = 19)
        UPDATE [dbo].[san_pham_chi_tiet] SET [id_san_pham] = 3, [id_mau_sac] = 12, [id_kich_thuoc] = 17, [ma_chi_tiet_san_pham] = N'SP0004-12-17', [so_luong] = 4, [gia_ban] = CAST(100000.00 AS Decimal(18, 2)), [sku] = N'SP0004-12-17', [kich_hoat] = 1, [ngay_tao] = CAST(N'2026-10-03T23:34:12.6782808' AS DateTime2), [ngay_cap_nhat] = NULL, [trang_thai] = 1 WHERE [id] = 19;
    ELSE
        INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (19, 3, 12, 17, N'SP0004-12-17', 4, CAST(100000.00 AS Decimal(18, 2)), N'SP0004-12-17', 1, CAST(N'2026-10-03T23:34:12.6782808' AS DateTime2), NULL, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[san_pham_chi_tiet] WHERE [id] = 20)
        UPDATE [dbo].[san_pham_chi_tiet] SET [id_san_pham] = 4, [id_mau_sac] = 14, [id_kich_thuoc] = 4, [ma_chi_tiet_san_pham] = N'SP005-14-4', [so_luong] = 123, [gia_ban] = CAST(123.00 AS Decimal(18, 2)), [sku] = N'SP005-14-4', [kich_hoat] = 1, [ngay_tao] = CAST(N'2026-10-04T00:32:40.5446268' AS DateTime2), [ngay_cap_nhat] = NULL, [trang_thai] = 1 WHERE [id] = 20;
    ELSE
        INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (20, 4, 14, 4, N'SP005-14-4', 123, CAST(123.00 AS Decimal(18, 2)), N'SP005-14-4', 1, CAST(N'2026-10-04T00:32:40.5446268' AS DateTime2), NULL, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[san_pham_chi_tiet] WHERE [id] = 21)
        UPDATE [dbo].[san_pham_chi_tiet] SET [id_san_pham] = 4, [id_mau_sac] = 14, [id_kich_thuoc] = 5, [ma_chi_tiet_san_pham] = N'SP005-14-5', [so_luong] = 123, [gia_ban] = CAST(123.00 AS Decimal(18, 2)), [sku] = N'SP005-14-5', [kich_hoat] = 1, [ngay_tao] = CAST(N'2026-10-04T00:32:40.5707167' AS DateTime2), [ngay_cap_nhat] = NULL, [trang_thai] = 1 WHERE [id] = 21;
    ELSE
        INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (21, 4, 14, 5, N'SP005-14-5', 123, CAST(123.00 AS Decimal(18, 2)), N'SP005-14-5', 1, CAST(N'2026-10-04T00:32:40.5707167' AS DateTime2), NULL, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[san_pham_chi_tiet] WHERE [id] = 22)
        UPDATE [dbo].[san_pham_chi_tiet] SET [id_san_pham] = 4, [id_mau_sac] = 15, [id_kich_thuoc] = 4, [ma_chi_tiet_san_pham] = N'SP005-15-4', [so_luong] = 123, [gia_ban] = CAST(123.00 AS Decimal(18, 2)), [sku] = N'SP005-15-4', [kich_hoat] = 1, [ngay_tao] = CAST(N'2026-10-04T00:32:40.5797283' AS DateTime2), [ngay_cap_nhat] = NULL, [trang_thai] = 1 WHERE [id] = 22;
    ELSE
        INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (22, 4, 15, 4, N'SP005-15-4', 123, CAST(123.00 AS Decimal(18, 2)), N'SP005-15-4', 1, CAST(N'2026-10-04T00:32:40.5797283' AS DateTime2), NULL, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[san_pham_chi_tiet] WHERE [id] = 23)
        UPDATE [dbo].[san_pham_chi_tiet] SET [id_san_pham] = 4, [id_mau_sac] = 15, [id_kich_thuoc] = 5, [ma_chi_tiet_san_pham] = N'SP005-15-5', [so_luong] = 123, [gia_ban] = CAST(123.00 AS Decimal(18, 2)), [sku] = N'SP005-15-5', [kich_hoat] = 1, [ngay_tao] = CAST(N'2026-10-04T00:32:40.5892536' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-04T18:57:06.8382946' AS DateTime2), [trang_thai] = 1 WHERE [id] = 23;
    ELSE
        INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (23, 4, 15, 5, N'SP005-15-5', 123, CAST(123.00 AS Decimal(18, 2)), N'SP005-15-5', 1, CAST(N'2026-10-04T00:32:40.5892536' AS DateTime2), CAST(N'2026-10-04T18:57:06.8382946' AS DateTime2), 1);
    IF EXISTS (SELECT 1 FROM [dbo].[san_pham_chi_tiet] WHERE [id] = 81)
        UPDATE [dbo].[san_pham_chi_tiet] SET [id_san_pham] = 88, [id_mau_sac] = 3, [id_kich_thuoc] = 5, [ma_chi_tiet_san_pham] = N'SP006-3-5', [so_luong] = 3, [gia_ban] = CAST(109999.98 AS Decimal(18, 2)), [sku] = N'SP006-3-5', [kich_hoat] = 1, [ngay_tao] = CAST(N'2026-10-04T19:05:43.6518691' AS DateTime2), [ngay_cap_nhat] = NULL, [trang_thai] = 1 WHERE [id] = 81;
    ELSE
        INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (81, 88, 3, 5, N'SP006-3-5', 3, CAST(109999.98 AS Decimal(18, 2)), N'SP006-3-5', 1, CAST(N'2026-10-04T19:05:43.6518691' AS DateTime2), NULL, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[san_pham_chi_tiet] WHERE [id] = 82)
        UPDATE [dbo].[san_pham_chi_tiet] SET [id_san_pham] = 88, [id_mau_sac] = 3, [id_kich_thuoc] = 4, [ma_chi_tiet_san_pham] = N'SP006-3-4', [so_luong] = 5, [gia_ban] = CAST(100000.00 AS Decimal(18, 2)), [sku] = N'SP006-3-4', [kich_hoat] = 1, [ngay_tao] = CAST(N'2026-10-04T19:05:43.6650458' AS DateTime2), [ngay_cap_nhat] = NULL, [trang_thai] = 1 WHERE [id] = 82;
    ELSE
        INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (82, 88, 3, 4, N'SP006-3-4', 5, CAST(100000.00 AS Decimal(18, 2)), N'SP006-3-4', 1, CAST(N'2026-10-04T19:05:43.6650458' AS DateTime2), NULL, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[san_pham_chi_tiet] WHERE [id] = 83)
        UPDATE [dbo].[san_pham_chi_tiet] SET [id_san_pham] = 88, [id_mau_sac] = 2, [id_kich_thuoc] = 5, [ma_chi_tiet_san_pham] = N'SP006-2-5', [so_luong] = 3, [gia_ban] = CAST(100000.00 AS Decimal(18, 2)), [sku] = N'SP006-2-5', [kich_hoat] = 1, [ngay_tao] = CAST(N'2026-10-04T19:05:43.6743000' AS DateTime2), [ngay_cap_nhat] = NULL, [trang_thai] = 1 WHERE [id] = 83;
    ELSE
        INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (83, 88, 2, 5, N'SP006-2-5', 3, CAST(100000.00 AS Decimal(18, 2)), N'SP006-2-5', 1, CAST(N'2026-10-04T19:05:43.6743000' AS DateTime2), NULL, 1);
    IF EXISTS (SELECT 1 FROM [dbo].[san_pham_chi_tiet] WHERE [id] = 84)
        UPDATE [dbo].[san_pham_chi_tiet] SET [id_san_pham] = 88, [id_mau_sac] = 2, [id_kich_thuoc] = 4, [ma_chi_tiet_san_pham] = N'SP006-2-4', [so_luong] = 5, [gia_ban] = CAST(100000.00 AS Decimal(18, 2)), [sku] = N'SP006-2-4', [kich_hoat] = 1, [ngay_tao] = CAST(N'2026-10-04T19:05:43.6782900' AS DateTime2), [ngay_cap_nhat] = CAST(N'2026-10-04T19:06:01.2868796' AS DateTime2), [trang_thai] = 1 WHERE [id] = 84;
    ELSE
        INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (84, 88, 2, 4, N'SP006-2-4', 5, CAST(100000.00 AS Decimal(18, 2)), N'SP006-2-4', 1, CAST(N'2026-10-04T19:05:43.6782900' AS DateTime2), CAST(N'2026-10-04T19:06:01.2868796' AS DateTime2), 1);
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[san_pham_chi_tiet]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[san_pham_chi_tiet] OFF;

    -- DATA MERGE: [dbo].[thuong_hieu] (10 dong snapshot)
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[thuong_hieu]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[thuong_hieu] ON;
    IF EXISTS (SELECT 1 FROM [dbo].[thuong_hieu] WHERE [id] = 1)
        UPDATE [dbo].[thuong_hieu] SET [ma_thuong_hieu] = N'TH001', [ten_thuong_hieu] = N'Nike', [trang_thai] = 1 WHERE [id] = 1;
    ELSE
        INSERT [dbo].[thuong_hieu] ([id], [ma_thuong_hieu], [ten_thuong_hieu], [trang_thai]) VALUES (1, N'TH001', N'Nike', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[thuong_hieu] WHERE [id] = 2)
        UPDATE [dbo].[thuong_hieu] SET [ma_thuong_hieu] = N'TH002', [ten_thuong_hieu] = N'Adidas', [trang_thai] = 1 WHERE [id] = 2;
    ELSE
        INSERT [dbo].[thuong_hieu] ([id], [ma_thuong_hieu], [ten_thuong_hieu], [trang_thai]) VALUES (2, N'TH002', N'Adidas', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[thuong_hieu] WHERE [id] = 3)
        UPDATE [dbo].[thuong_hieu] SET [ma_thuong_hieu] = N'TH003', [ten_thuong_hieu] = N'Puma', [trang_thai] = 1 WHERE [id] = 3;
    ELSE
        INSERT [dbo].[thuong_hieu] ([id], [ma_thuong_hieu], [ten_thuong_hieu], [trang_thai]) VALUES (3, N'TH003', N'Puma', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[thuong_hieu] WHERE [id] = 4)
        UPDATE [dbo].[thuong_hieu] SET [ma_thuong_hieu] = N'TH004', [ten_thuong_hieu] = N'New Balance', [trang_thai] = 1 WHERE [id] = 4;
    ELSE
        INSERT [dbo].[thuong_hieu] ([id], [ma_thuong_hieu], [ten_thuong_hieu], [trang_thai]) VALUES (4, N'TH004', N'New Balance', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[thuong_hieu] WHERE [id] = 5)
        UPDATE [dbo].[thuong_hieu] SET [ma_thuong_hieu] = N'TH005', [ten_thuong_hieu] = N'Converse', [trang_thai] = 1 WHERE [id] = 5;
    ELSE
        INSERT [dbo].[thuong_hieu] ([id], [ma_thuong_hieu], [ten_thuong_hieu], [trang_thai]) VALUES (5, N'TH005', N'Converse', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[thuong_hieu] WHERE [id] = 6)
        UPDATE [dbo].[thuong_hieu] SET [ma_thuong_hieu] = N'TH006', [ten_thuong_hieu] = N'Vans', [trang_thai] = 1 WHERE [id] = 6;
    ELSE
        INSERT [dbo].[thuong_hieu] ([id], [ma_thuong_hieu], [ten_thuong_hieu], [trang_thai]) VALUES (6, N'TH006', N'Vans', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[thuong_hieu] WHERE [id] = 7)
        UPDATE [dbo].[thuong_hieu] SET [ma_thuong_hieu] = N'TH007', [ten_thuong_hieu] = N'ASICS', [trang_thai] = 1 WHERE [id] = 7;
    ELSE
        INSERT [dbo].[thuong_hieu] ([id], [ma_thuong_hieu], [ten_thuong_hieu], [trang_thai]) VALUES (7, N'TH007', N'ASICS', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[thuong_hieu] WHERE [id] = 8)
        UPDATE [dbo].[thuong_hieu] SET [ma_thuong_hieu] = N'TH008', [ten_thuong_hieu] = N'Under Armour', [trang_thai] = 1 WHERE [id] = 8;
    ELSE
        INSERT [dbo].[thuong_hieu] ([id], [ma_thuong_hieu], [ten_thuong_hieu], [trang_thai]) VALUES (8, N'TH008', N'Under Armour', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[thuong_hieu] WHERE [id] = 9)
        UPDATE [dbo].[thuong_hieu] SET [ma_thuong_hieu] = N'TH009', [ten_thuong_hieu] = N'Reebok', [trang_thai] = 1 WHERE [id] = 9;
    ELSE
        INSERT [dbo].[thuong_hieu] ([id], [ma_thuong_hieu], [ten_thuong_hieu], [trang_thai]) VALUES (9, N'TH009', N'Reebok', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[thuong_hieu] WHERE [id] = 10)
        UPDATE [dbo].[thuong_hieu] SET [ma_thuong_hieu] = N'TH010', [ten_thuong_hieu] = N'Skechers', [trang_thai] = 1 WHERE [id] = 10;
    ELSE
        INSERT [dbo].[thuong_hieu] ([id], [ma_thuong_hieu], [ten_thuong_hieu], [trang_thai]) VALUES (10, N'TH010', N'Skechers', 1);
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[thuong_hieu]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[thuong_hieu] OFF;

    -- DATA MERGE: [dbo].[vai_tro] (2 dong snapshot)
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[vai_tro]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[vai_tro] ON;
    IF EXISTS (SELECT 1 FROM [dbo].[vai_tro] WHERE [id] = 1)
        UPDATE [dbo].[vai_tro] SET [ten_vai_tro] = N'Nhân viên', [mo_ta] = N'Nhân viên bán hàng', [trang_thai] = 1 WHERE [id] = 1;
    ELSE
        INSERT [dbo].[vai_tro] ([id], [ten_vai_tro], [mo_ta], [trang_thai]) VALUES (1, N'Nhân viên', N'Nhân viên bán hàng', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[vai_tro] WHERE [id] = 2)
        UPDATE [dbo].[vai_tro] SET [ten_vai_tro] = N'Quản lý', [mo_ta] = N'Vai trò quản lý cửa hàng', [trang_thai] = 1 WHERE [id] = 2;
    ELSE
        INSERT [dbo].[vai_tro] ([id], [ten_vai_tro], [mo_ta], [trang_thai]) VALUES (2, N'Quản lý', N'Vai trò quản lý cửa hàng', 1);
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[vai_tro]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[vai_tro] OFF;

    -- DATA MERGE: [dbo].[xuat_xu] (9 dong snapshot)
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[xuat_xu]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[xuat_xu] ON;
    IF EXISTS (SELECT 1 FROM [dbo].[xuat_xu] WHERE [id] = 1)
        UPDATE [dbo].[xuat_xu] SET [ma_xuat_xu] = N'XX001', [ten_xuat_xu] = N'Việt Nam', [trang_thai] = 1 WHERE [id] = 1;
    ELSE
        INSERT [dbo].[xuat_xu] ([id], [ma_xuat_xu], [ten_xuat_xu], [trang_thai]) VALUES (1, N'XX001', N'Việt Nam', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[xuat_xu] WHERE [id] = 2)
        UPDATE [dbo].[xuat_xu] SET [ma_xuat_xu] = N'XX002', [ten_xuat_xu] = N'Indonesia', [trang_thai] = 1 WHERE [id] = 2;
    ELSE
        INSERT [dbo].[xuat_xu] ([id], [ma_xuat_xu], [ten_xuat_xu], [trang_thai]) VALUES (2, N'XX002', N'Indonesia', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[xuat_xu] WHERE [id] = 3)
        UPDATE [dbo].[xuat_xu] SET [ma_xuat_xu] = N'XX003', [ten_xuat_xu] = N'Trung Quốc', [trang_thai] = 1 WHERE [id] = 3;
    ELSE
        INSERT [dbo].[xuat_xu] ([id], [ma_xuat_xu], [ten_xuat_xu], [trang_thai]) VALUES (3, N'XX003', N'Trung Quốc', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[xuat_xu] WHERE [id] = 4)
        UPDATE [dbo].[xuat_xu] SET [ma_xuat_xu] = N'XX004', [ten_xuat_xu] = N'Mỹ', [trang_thai] = 1 WHERE [id] = 4;
    ELSE
        INSERT [dbo].[xuat_xu] ([id], [ma_xuat_xu], [ten_xuat_xu], [trang_thai]) VALUES (4, N'XX004', N'Mỹ', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[xuat_xu] WHERE [id] = 5)
        UPDATE [dbo].[xuat_xu] SET [ma_xuat_xu] = N'XX005', [ten_xuat_xu] = N'Nhật Bản', [trang_thai] = 1 WHERE [id] = 5;
    ELSE
        INSERT [dbo].[xuat_xu] ([id], [ma_xuat_xu], [ten_xuat_xu], [trang_thai]) VALUES (5, N'XX005', N'Nhật Bản', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[xuat_xu] WHERE [id] = 6)
        UPDATE [dbo].[xuat_xu] SET [ma_xuat_xu] = N'XX006', [ten_xuat_xu] = N'Hàn Quốc', [trang_thai] = 1 WHERE [id] = 6;
    ELSE
        INSERT [dbo].[xuat_xu] ([id], [ma_xuat_xu], [ten_xuat_xu], [trang_thai]) VALUES (6, N'XX006', N'Hàn Quốc', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[xuat_xu] WHERE [id] = 7)
        UPDATE [dbo].[xuat_xu] SET [ma_xuat_xu] = N'XX007', [ten_xuat_xu] = N'Thái Lan', [trang_thai] = 1 WHERE [id] = 7;
    ELSE
        INSERT [dbo].[xuat_xu] ([id], [ma_xuat_xu], [ten_xuat_xu], [trang_thai]) VALUES (7, N'XX007', N'Thái Lan', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[xuat_xu] WHERE [id] = 8)
        UPDATE [dbo].[xuat_xu] SET [ma_xuat_xu] = N'XX008', [ten_xuat_xu] = N'Đức', [trang_thai] = 1 WHERE [id] = 8;
    ELSE
        INSERT [dbo].[xuat_xu] ([id], [ma_xuat_xu], [ten_xuat_xu], [trang_thai]) VALUES (8, N'XX008', N'Đức', 1);
    IF EXISTS (SELECT 1 FROM [dbo].[xuat_xu] WHERE [id] = 153)
        UPDATE [dbo].[xuat_xu] SET [ma_xuat_xu] = N'XX009', [ten_xuat_xu] = N'Hà Lan', [trang_thai] = 1 WHERE [id] = 153;
    ELSE
        INSERT [dbo].[xuat_xu] ([id], [ma_xuat_xu], [ten_xuat_xu], [trang_thai]) VALUES (153, N'XX009', N'Hà Lan', 1);
    IF COLUMNPROPERTY(OBJECT_ID(N'[dbo].[xuat_xu]'), 'id', 'IsIdentity') = 1 SET IDENTITY_INSERT [dbo].[xuat_xu] OFF;

    PRINT N'4/4 - Dam bao 25 khoa ngoai va bat lai constraint...';
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID(N'dbo.chi_tiet_dot_giam_gia') AND name = N'FK_ctdgg_dot_giam_gia')
    BEGIN
        ALTER TABLE [dbo].[chi_tiet_dot_giam_gia] WITH NOCHECK ADD CONSTRAINT [FK_ctdgg_dot_giam_gia] FOREIGN KEY([id_dot_giam_gia])
            REFERENCES [dbo].[dot_giam_gia] ([id]);
    END;
    ALTER TABLE [dbo].[chi_tiet_dot_giam_gia] CHECK CONSTRAINT [FK_ctdgg_dot_giam_gia];

    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID(N'dbo.chi_tiet_dot_giam_gia') AND name = N'FK_ctdgg_spct')
    BEGIN
        ALTER TABLE [dbo].[chi_tiet_dot_giam_gia] WITH NOCHECK ADD CONSTRAINT [FK_ctdgg_spct] FOREIGN KEY([id_san_pham_chi_tiet])
            REFERENCES [dbo].[san_pham_chi_tiet] ([id]);
    END;
    ALTER TABLE [dbo].[chi_tiet_dot_giam_gia] CHECK CONSTRAINT [FK_ctdgg_spct];

    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID(N'dbo.dia_chi_khach_hang') AND name = N'FK_dia_chi_khach_hang')
    BEGIN
        ALTER TABLE [dbo].[dia_chi_khach_hang] WITH NOCHECK ADD CONSTRAINT [FK_dia_chi_khach_hang] FOREIGN KEY([id_khach_hang])
            REFERENCES [dbo].[khach_hang] ([id]);
    END;
    ALTER TABLE [dbo].[dia_chi_khach_hang] CHECK CONSTRAINT [FK_dia_chi_khach_hang];

    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID(N'dbo.hoa_don_chi_tiet') AND name = N'FK_hdct_hoa_don')
    BEGIN
        ALTER TABLE [dbo].[hoa_don_chi_tiet] WITH NOCHECK ADD CONSTRAINT [FK_hdct_hoa_don] FOREIGN KEY([id_hoa_don])
            REFERENCES [dbo].[hoa_don] ([id]);
    END;
    ALTER TABLE [dbo].[hoa_don_chi_tiet] CHECK CONSTRAINT [FK_hdct_hoa_don];

    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID(N'dbo.hoa_don_chi_tiet') AND name = N'FK_hdct_spct')
    BEGIN
        ALTER TABLE [dbo].[hoa_don_chi_tiet] WITH NOCHECK ADD CONSTRAINT [FK_hdct_spct] FOREIGN KEY([id_san_pham_chi_tiet])
            REFERENCES [dbo].[san_pham_chi_tiet] ([id]);
    END;
    ALTER TABLE [dbo].[hoa_don_chi_tiet] CHECK CONSTRAINT [FK_hdct_spct];

    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID(N'dbo.hinh_anh_san_pham') AND name = N'FK_hinh_anh_san_pham')
    BEGIN
        ALTER TABLE [dbo].[hinh_anh_san_pham] WITH NOCHECK ADD CONSTRAINT [FK_hinh_anh_san_pham] FOREIGN KEY([id_san_pham])
            REFERENCES [dbo].[san_pham] ([id]);
    END;
    ALTER TABLE [dbo].[hinh_anh_san_pham] CHECK CONSTRAINT [FK_hinh_anh_san_pham];

    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID(N'dbo.hoa_don') AND name = N'FK_hoa_don_khach_hang')
    BEGIN
        ALTER TABLE [dbo].[hoa_don] WITH NOCHECK ADD CONSTRAINT [FK_hoa_don_khach_hang] FOREIGN KEY([id_khach_hang])
            REFERENCES [dbo].[khach_hang] ([id]);
    END;
    ALTER TABLE [dbo].[hoa_don] CHECK CONSTRAINT [FK_hoa_don_khach_hang];

    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID(N'dbo.hoa_don') AND name = N'FK_hoa_don_nhan_vien')
    BEGIN
        ALTER TABLE [dbo].[hoa_don] WITH NOCHECK ADD CONSTRAINT [FK_hoa_don_nhan_vien] FOREIGN KEY([id_nhan_vien])
            REFERENCES [dbo].[nhan_vien] ([id]);
    END;
    ALTER TABLE [dbo].[hoa_don] CHECK CONSTRAINT [FK_hoa_don_nhan_vien];

    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID(N'dbo.hoa_don') AND name = N'FK_hoa_don_phieu_giam_gia')
    BEGIN
        ALTER TABLE [dbo].[hoa_don] WITH NOCHECK ADD CONSTRAINT [FK_hoa_don_phieu_giam_gia] FOREIGN KEY([id_phieu_giam_gia])
            REFERENCES [dbo].[phieu_giam_gia] ([id]);
    END;
    ALTER TABLE [dbo].[hoa_don] CHECK CONSTRAINT [FK_hoa_don_phieu_giam_gia];

    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID(N'dbo.hoa_don') AND name = N'FK_hoa_don_phuong_thuc')
    BEGIN
        ALTER TABLE [dbo].[hoa_don] WITH NOCHECK ADD CONSTRAINT [FK_hoa_don_phuong_thuc] FOREIGN KEY([id_phuong_thuc_thanh_toan])
            REFERENCES [dbo].[phuong_thuc_thanh_toan] ([id]);
    END;
    ALTER TABLE [dbo].[hoa_don] CHECK CONSTRAINT [FK_hoa_don_phuong_thuc];

    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID(N'dbo.lich_su_hoa_don') AND name = N'FK_lshd_hoa_don')
    BEGIN
        ALTER TABLE [dbo].[lich_su_hoa_don] WITH NOCHECK ADD CONSTRAINT [FK_lshd_hoa_don] FOREIGN KEY([id_hoa_don])
            REFERENCES [dbo].[hoa_don] ([id]);
    END;
    ALTER TABLE [dbo].[lich_su_hoa_don] CHECK CONSTRAINT [FK_lshd_hoa_don];

    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID(N'dbo.lich_su_thanh_toan') AND name = N'FK_lstt_hoa_don')
    BEGIN
        ALTER TABLE [dbo].[lich_su_thanh_toan] WITH NOCHECK ADD CONSTRAINT [FK_lstt_hoa_don] FOREIGN KEY([id_hoa_don])
            REFERENCES [dbo].[hoa_don] ([id]);
    END;
    ALTER TABLE [dbo].[lich_su_thanh_toan] CHECK CONSTRAINT [FK_lstt_hoa_don];

    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID(N'dbo.nhan_vien') AND name = N'FK_nhan_vien_vai_tro')
    BEGIN
        ALTER TABLE [dbo].[nhan_vien] WITH NOCHECK ADD CONSTRAINT [FK_nhan_vien_vai_tro] FOREIGN KEY([id_vai_tro])
            REFERENCES [dbo].[vai_tro] ([id]);
    END;
    ALTER TABLE [dbo].[nhan_vien] CHECK CONSTRAINT [FK_nhan_vien_vai_tro];

    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID(N'dbo.phieu_giam_gia_khach_hang') AND name = N'FK_pggkh_khach_hang')
    BEGIN
        ALTER TABLE [dbo].[phieu_giam_gia_khach_hang] WITH NOCHECK ADD CONSTRAINT [FK_pggkh_khach_hang] FOREIGN KEY([id_khach_hang])
            REFERENCES [dbo].[khach_hang] ([id]);
    END;
    ALTER TABLE [dbo].[phieu_giam_gia_khach_hang] CHECK CONSTRAINT [FK_pggkh_khach_hang];

    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID(N'dbo.phieu_giam_gia_khach_hang') AND name = N'FK_pggkh_phieu_giam_gia')
    BEGIN
        ALTER TABLE [dbo].[phieu_giam_gia_khach_hang] WITH NOCHECK ADD CONSTRAINT [FK_pggkh_phieu_giam_gia] FOREIGN KEY([id_phieu_giam_gia])
            REFERENCES [dbo].[phieu_giam_gia] ([id]);
    END;
    ALTER TABLE [dbo].[phieu_giam_gia_khach_hang] CHECK CONSTRAINT [FK_pggkh_phieu_giam_gia];

    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID(N'dbo.phuong_thuc_thanh_toan') AND name = N'FK_pttt_hinh_thuc')
    BEGIN
        ALTER TABLE [dbo].[phuong_thuc_thanh_toan] WITH NOCHECK ADD CONSTRAINT [FK_pttt_hinh_thuc] FOREIGN KEY([id_hinh_thuc_thanh_toan])
            REFERENCES [dbo].[hinh_thuc_thanh_toan] ([id]);
    END;
    ALTER TABLE [dbo].[phuong_thuc_thanh_toan] CHECK CONSTRAINT [FK_pttt_hinh_thuc];

    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID(N'dbo.san_pham') AND name = N'FK_san_pham_chat_lieu')
    BEGIN
        ALTER TABLE [dbo].[san_pham] WITH NOCHECK ADD CONSTRAINT [FK_san_pham_chat_lieu] FOREIGN KEY([id_chat_lieu])
            REFERENCES [dbo].[chat_lieu] ([id]);
    END;
    ALTER TABLE [dbo].[san_pham] CHECK CONSTRAINT [FK_san_pham_chat_lieu];

    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID(N'dbo.san_pham') AND name = N'FK_san_pham_co_giay')
    BEGIN
        ALTER TABLE [dbo].[san_pham] WITH NOCHECK ADD CONSTRAINT [FK_san_pham_co_giay] FOREIGN KEY([id_co_giay])
            REFERENCES [dbo].[co_giay] ([id]);
    END;
    ALTER TABLE [dbo].[san_pham] CHECK CONSTRAINT [FK_san_pham_co_giay];

    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID(N'dbo.san_pham') AND name = N'FK_san_pham_danh_muc')
    BEGIN
        ALTER TABLE [dbo].[san_pham] WITH NOCHECK ADD CONSTRAINT [FK_san_pham_danh_muc] FOREIGN KEY([id_danh_muc])
            REFERENCES [dbo].[danh_muc] ([id]);
    END;
    ALTER TABLE [dbo].[san_pham] CHECK CONSTRAINT [FK_san_pham_danh_muc];

    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID(N'dbo.san_pham') AND name = N'FK_san_pham_kieu_dang')
    BEGIN
        ALTER TABLE [dbo].[san_pham] WITH NOCHECK ADD CONSTRAINT [FK_san_pham_kieu_dang] FOREIGN KEY([id_kieu_dang])
            REFERENCES [dbo].[kieu_dang] ([id]);
    END;
    ALTER TABLE [dbo].[san_pham] CHECK CONSTRAINT [FK_san_pham_kieu_dang];

    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID(N'dbo.san_pham') AND name = N'FK_san_pham_thuong_hieu')
    BEGIN
        ALTER TABLE [dbo].[san_pham] WITH NOCHECK ADD CONSTRAINT [FK_san_pham_thuong_hieu] FOREIGN KEY([id_thuong_hieu])
            REFERENCES [dbo].[thuong_hieu] ([id]);
    END;
    ALTER TABLE [dbo].[san_pham] CHECK CONSTRAINT [FK_san_pham_thuong_hieu];

    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID(N'dbo.san_pham') AND name = N'FK_san_pham_xuat_xu')
    BEGIN
        ALTER TABLE [dbo].[san_pham] WITH NOCHECK ADD CONSTRAINT [FK_san_pham_xuat_xu] FOREIGN KEY([id_xuat_xu])
            REFERENCES [dbo].[xuat_xu] ([id]);
    END;
    ALTER TABLE [dbo].[san_pham] CHECK CONSTRAINT [FK_san_pham_xuat_xu];

    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID(N'dbo.san_pham_chi_tiet') AND name = N'FK_spct_kich_thuoc')
    BEGIN
        ALTER TABLE [dbo].[san_pham_chi_tiet] WITH NOCHECK ADD CONSTRAINT [FK_spct_kich_thuoc] FOREIGN KEY([id_kich_thuoc])
            REFERENCES [dbo].[kich_thuoc] ([id]);
    END;
    ALTER TABLE [dbo].[san_pham_chi_tiet] CHECK CONSTRAINT [FK_spct_kich_thuoc];

    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID(N'dbo.san_pham_chi_tiet') AND name = N'FK_spct_mau_sac')
    BEGIN
        ALTER TABLE [dbo].[san_pham_chi_tiet] WITH NOCHECK ADD CONSTRAINT [FK_spct_mau_sac] FOREIGN KEY([id_mau_sac])
            REFERENCES [dbo].[mau_sac] ([id]);
    END;
    ALTER TABLE [dbo].[san_pham_chi_tiet] CHECK CONSTRAINT [FK_spct_mau_sac];

    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID(N'dbo.san_pham_chi_tiet') AND name = N'FK_spct_san_pham')
    BEGIN
        ALTER TABLE [dbo].[san_pham_chi_tiet] WITH NOCHECK ADD CONSTRAINT [FK_spct_san_pham] FOREIGN KEY([id_san_pham])
            REFERENCES [dbo].[san_pham] ([id]);
    END;
    ALTER TABLE [dbo].[san_pham_chi_tiet] CHECK CONSTRAINT [FK_spct_san_pham];

    ALTER TABLE [dbo].[chat_lieu] CHECK CONSTRAINT ALL;
    ALTER TABLE [dbo].[chi_tiet_dot_giam_gia] CHECK CONSTRAINT ALL;
    ALTER TABLE [dbo].[co_giay] CHECK CONSTRAINT ALL;
    ALTER TABLE [dbo].[danh_muc] CHECK CONSTRAINT ALL;
    ALTER TABLE [dbo].[dia_chi_khach_hang] CHECK CONSTRAINT ALL;
    ALTER TABLE [dbo].[dot_giam_gia] CHECK CONSTRAINT ALL;
    ALTER TABLE [dbo].[hinh_anh_san_pham] CHECK CONSTRAINT ALL;
    ALTER TABLE [dbo].[hinh_thuc_thanh_toan] CHECK CONSTRAINT ALL;
    ALTER TABLE [dbo].[hoa_don] CHECK CONSTRAINT ALL;
    ALTER TABLE [dbo].[hoa_don_chi_tiet] CHECK CONSTRAINT ALL;
    ALTER TABLE [dbo].[khach_hang] CHECK CONSTRAINT ALL;
    ALTER TABLE [dbo].[kich_thuoc] CHECK CONSTRAINT ALL;
    ALTER TABLE [dbo].[kieu_dang] CHECK CONSTRAINT ALL;
    ALTER TABLE [dbo].[lich_su_hoa_don] CHECK CONSTRAINT ALL;
    ALTER TABLE [dbo].[lich_su_thanh_toan] CHECK CONSTRAINT ALL;
    ALTER TABLE [dbo].[mau_sac] CHECK CONSTRAINT ALL;
    ALTER TABLE [dbo].[nhan_vien] CHECK CONSTRAINT ALL;
    ALTER TABLE [dbo].[phieu_giam_gia] CHECK CONSTRAINT ALL;
    ALTER TABLE [dbo].[phieu_giam_gia_khach_hang] CHECK CONSTRAINT ALL;
    ALTER TABLE [dbo].[phuong_thuc_thanh_toan] CHECK CONSTRAINT ALL;
    ALTER TABLE [dbo].[san_pham] CHECK CONSTRAINT ALL;
    ALTER TABLE [dbo].[san_pham_chi_tiet] CHECK CONSTRAINT ALL;
    ALTER TABLE [dbo].[thuong_hieu] CHECK CONSTRAINT ALL;
    ALTER TABLE [dbo].[vai_tro] CHECK CONSTRAINT ALL;
    ALTER TABLE [dbo].[xuat_xu] CHECK CONSTRAINT ALL;

    COMMIT TRANSACTION;
    PRINT N'SmashStep overlay: THANH CONG. Da giu du lieu cu, cap nhat/chen snapshot theo ID, khong DROP/DELETE.';
END TRY
BEGIN CATCH
    DECLARE @Err nvarchar(4000) = ERROR_MESSAGE();
    DECLARE @ErrLine int = ERROR_LINE();
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    PRINT N'SmashStep overlay THAT BAI tai dong ' + CONVERT(nvarchar(20), @ErrLine) + N': ' + @Err;
    THROW;
END CATCH;
GO

-- KIEM TRA NHANH SAU KHI CHAY
SELECT N'Bang' AS [Loai], COUNT(*) AS [So_luong] FROM sys.tables WHERE schema_id = SCHEMA_ID(N'dbo');
SELECT N'Foreign key' AS [Loai], COUNT(*) AS [So_luong] FROM sys.foreign_keys WHERE schema_id = SCHEMA_ID(N'dbo');
GO
