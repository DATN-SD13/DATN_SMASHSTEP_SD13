/* SMASHSTEP SD-013 - BAN SAO DAY DU DATABASE HIEN TAI
   Tao ngay 2026-10-07 (Asia/Bangkok). Schema + du lieu lay tu SQL Server local.
   25 bang; 231 dong; 25 khoa ngoai.
   SQL Server 2019 tro len. Mo file UTF-8 trong SSMS, chon toan bo va Execute mot lan.
   KHONG DROP/DELETE database cu. Database dich da co bang thi script dung.
   Giu nguyen schema hien tai; KHONG tu them cot/migration hay suy dien du lieu.
   Duong dan anh duoc giu nguyen; file anh nam trong folder uploads cua project.
*/
USE [master];
GO
IF DB_ID(N'SmashStep') IS NULL
    CREATE DATABASE [SmashStep] COLLATE SQL_Latin1_General_CP1_CI_AS;
GO
USE [SmashStep];
GO
IF EXISTS (SELECT 1 FROM sys.objects WHERE is_ms_shipped=0 AND type IN ('U','V','P','FN','IF','TF','SO'))
BEGIN
    RAISERROR(N'Database SmashStep da co bang/object. Dung script de giu nguyen du lieu. Hay chay tren database moi/trong.',16,1);
    SET NOEXEC ON;
END;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
-- 1. TABLES, PRIMARY/UNIQUE KEYS, DEFAULTS, CHECKS, INDEXES
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
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

SET ANSI_PADDING OFF
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
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

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
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

SET ANSI_PADDING OFF
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
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

SET ANSI_PADDING OFF
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
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

SET ANSI_PADDING OFF
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
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

SET ANSI_PADDING OFF
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
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

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
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

SET ANSI_PADDING OFF
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
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

SET ANSI_PADDING OFF
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
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

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
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

SET ANSI_PADDING OFF
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
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

SET ANSI_PADDING OFF
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
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

SET ANSI_PADDING OFF
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
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

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
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

SET ANSI_PADDING OFF
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
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

SET ANSI_PADDING OFF
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
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

SET ANSI_PADDING OFF
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
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

SET ANSI_PADDING OFF
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
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

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
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

SET ANSI_PADDING OFF
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
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

SET ANSI_PADDING OFF
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
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

SET ANSI_PADDING OFF
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
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

SET ANSI_PADDING OFF
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
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

SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
SET ANSI_PADDING ON
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

SET ANSI_PADDING OFF
ALTER TABLE [dbo].[phieu_giam_gia] ADD  CONSTRAINT [DF_phieu_giam_gia_vo_han]  DEFAULT ((0)) FOR [vo_han]
-- 2. ALL CURRENT DATA, ORIGINAL IDs AND UNICODE VALUES
-- DATA: [dbo].[chat_lieu]
SET IDENTITY_INSERT [dbo].[chat_lieu] ON 

INSERT [dbo].[chat_lieu] ([id], [ma_chat_lieu], [ten_chat_lieu], [trang_thai]) VALUES (1, N'CL001', N'Vải Mesh', 1)
INSERT [dbo].[chat_lieu] ([id], [ma_chat_lieu], [ten_chat_lieu], [trang_thai]) VALUES (2, N'CL002', N'Da tổng hợp', 1)
INSERT [dbo].[chat_lieu] ([id], [ma_chat_lieu], [ten_chat_lieu], [trang_thai]) VALUES (3, N'CL003', N'Da thật', 1)
INSERT [dbo].[chat_lieu] ([id], [ma_chat_lieu], [ten_chat_lieu], [trang_thai]) VALUES (4, N'CL004', N'Vải Canvas', 1)
INSERT [dbo].[chat_lieu] ([id], [ma_chat_lieu], [ten_chat_lieu], [trang_thai]) VALUES (5, N'CL005', N'Vải Knit', 1)
INSERT [dbo].[chat_lieu] ([id], [ma_chat_lieu], [ten_chat_lieu], [trang_thai]) VALUES (6, N'CL006', N'Da lộn', 1)
INSERT [dbo].[chat_lieu] ([id], [ma_chat_lieu], [ten_chat_lieu], [trang_thai]) VALUES (7, N'CL007', N'Polyester', 1)
INSERT [dbo].[chat_lieu] ([id], [ma_chat_lieu], [ten_chat_lieu], [trang_thai]) VALUES (8, N'CL008', N'Cao su', 1)
INSERT [dbo].[chat_lieu] ([id], [ma_chat_lieu], [ten_chat_lieu], [trang_thai]) VALUES (9, N'CL009', N'Vải dệt', 1)
INSERT [dbo].[chat_lieu] ([id], [ma_chat_lieu], [ten_chat_lieu], [trang_thai]) VALUES (10, N'CL010', N'Sợi tổng hợp', 1)
SET IDENTITY_INSERT [dbo].[chat_lieu] OFF
-- DATA: [dbo].[chi_tiet_dot_giam_gia]
SET IDENTITY_INSERT [dbo].[chi_tiet_dot_giam_gia] ON 

INSERT [dbo].[chi_tiet_dot_giam_gia] ([id], [id_dot_giam_gia], [id_san_pham_chi_tiet], [phan_tram_giam_bien_the], [trang_thai], [ngay_tao]) VALUES (0, 1, 15, NULL, 1, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2))
INSERT [dbo].[chi_tiet_dot_giam_gia] ([id], [id_dot_giam_gia], [id_san_pham_chi_tiet], [phan_tram_giam_bien_the], [trang_thai], [ngay_tao]) VALUES (1, 1, 14, NULL, 1, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2))
INSERT [dbo].[chi_tiet_dot_giam_gia] ([id], [id_dot_giam_gia], [id_san_pham_chi_tiet], [phan_tram_giam_bien_the], [trang_thai], [ngay_tao]) VALUES (2, 2, 13, NULL, 1, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2))
INSERT [dbo].[chi_tiet_dot_giam_gia] ([id], [id_dot_giam_gia], [id_san_pham_chi_tiet], [phan_tram_giam_bien_the], [trang_thai], [ngay_tao]) VALUES (3, 2, 6, NULL, 1, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2))
INSERT [dbo].[chi_tiet_dot_giam_gia] ([id], [id_dot_giam_gia], [id_san_pham_chi_tiet], [phan_tram_giam_bien_the], [trang_thai], [ngay_tao]) VALUES (4, 3, 5, NULL, 1, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2))
INSERT [dbo].[chi_tiet_dot_giam_gia] ([id], [id_dot_giam_gia], [id_san_pham_chi_tiet], [phan_tram_giam_bien_the], [trang_thai], [ngay_tao]) VALUES (5, 3, 4, NULL, 1, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2))
INSERT [dbo].[chi_tiet_dot_giam_gia] ([id], [id_dot_giam_gia], [id_san_pham_chi_tiet], [phan_tram_giam_bien_the], [trang_thai], [ngay_tao]) VALUES (12, 5, 81, CAST(3.00 AS Decimal(18, 2)), 1, CAST(N'2026-10-07T02:13:35.8986630' AS DateTime2))
INSERT [dbo].[chi_tiet_dot_giam_gia] ([id], [id_dot_giam_gia], [id_san_pham_chi_tiet], [phan_tram_giam_bien_the], [trang_thai], [ngay_tao]) VALUES (13, 5, 82, CAST(3.00 AS Decimal(18, 2)), 1, CAST(N'2026-10-07T02:13:35.9151896' AS DateTime2))
INSERT [dbo].[chi_tiet_dot_giam_gia] ([id], [id_dot_giam_gia], [id_san_pham_chi_tiet], [phan_tram_giam_bien_the], [trang_thai], [ngay_tao]) VALUES (14, 5, 83, CAST(3.00 AS Decimal(18, 2)), 1, CAST(N'2026-10-07T02:13:35.9151896' AS DateTime2))
INSERT [dbo].[chi_tiet_dot_giam_gia] ([id], [id_dot_giam_gia], [id_san_pham_chi_tiet], [phan_tram_giam_bien_the], [trang_thai], [ngay_tao]) VALUES (15, 5, 84, CAST(3.00 AS Decimal(18, 2)), 1, CAST(N'2026-10-07T02:13:35.9151896' AS DateTime2))
SET IDENTITY_INSERT [dbo].[chi_tiet_dot_giam_gia] OFF
-- DATA: [dbo].[co_giay]
SET IDENTITY_INSERT [dbo].[co_giay] ON 

INSERT [dbo].[co_giay] ([id], [ma_co_giay], [ten_co_giay], [trang_thai]) VALUES (1, N'CG001', N'Cổ thấp', 1)
INSERT [dbo].[co_giay] ([id], [ma_co_giay], [ten_co_giay], [trang_thai]) VALUES (2, N'CG002', N'Cổ trung', 1)
INSERT [dbo].[co_giay] ([id], [ma_co_giay], [ten_co_giay], [trang_thai]) VALUES (3, N'CG003', N'Cổ cao', 1)
SET IDENTITY_INSERT [dbo].[co_giay] OFF
-- DATA: [dbo].[danh_muc]
SET IDENTITY_INSERT [dbo].[danh_muc] ON 

INSERT [dbo].[danh_muc] ([id], [ma_danh_muc], [ten_danh_muc], [mo_ta], [trang_thai]) VALUES (1, N'DM001', N'Giày chạy bộ', N'Giày dành cho chạy bộ và luyện tập', 1)
INSERT [dbo].[danh_muc] ([id], [ma_danh_muc], [ten_danh_muc], [mo_ta], [trang_thai]) VALUES (2, N'DM002', N'Giày thể thao', N'Giày thể thao sử dụng hàng ngày', 1)
INSERT [dbo].[danh_muc] ([id], [ma_danh_muc], [ten_danh_muc], [mo_ta], [trang_thai]) VALUES (3, N'DM003', N'Giày bóng rổ', N'Giày chuyên dụng cho bóng rổ', 1)
INSERT [dbo].[danh_muc] ([id], [ma_danh_muc], [ten_danh_muc], [mo_ta], [trang_thai]) VALUES (4, N'DM004', N'Giày thời trang', N'Giày sneaker và thời trang đường phố', 1)
INSERT [dbo].[danh_muc] ([id], [ma_danh_muc], [ten_danh_muc], [mo_ta], [trang_thai]) VALUES (5, N'DM005', N'Giày đá bóng', N'Giày sử dụng khi chơi bóng đá', 1)
INSERT [dbo].[danh_muc] ([id], [ma_danh_muc], [ten_danh_muc], [mo_ta], [trang_thai]) VALUES (6, N'DM006', N'Giày tennis', N'Giày dành cho tennis và thể thao sân', 1)
INSERT [dbo].[danh_muc] ([id], [ma_danh_muc], [ten_danh_muc], [mo_ta], [trang_thai]) VALUES (7, N'DM007', N'Giày đi bộ', N'Giày nhẹ dùng để đi bộ hàng ngày', 1)
INSERT [dbo].[danh_muc] ([id], [ma_danh_muc], [ten_danh_muc], [mo_ta], [trang_thai]) VALUES (8, N'DM008', N'Giày tập gym', N'Giày phục vụ tập luyện và fitness', 1)
SET IDENTITY_INSERT [dbo].[danh_muc] OFF
-- DATA: [dbo].[dia_chi_khach_hang]
SET IDENTITY_INSERT [dbo].[dia_chi_khach_hang] ON 

INSERT [dbo].[dia_chi_khach_hang] ([id], [id_khach_hang], [ten_nguoi_nhan], [sdt_nguoi_nhan], [tinh_thanh], [quan_huyen], [phuong_xa], [dia_chi_cu_the], [loai_dia_chi], [is_mac_dinh], [trang_thai]) VALUES (4, 1, N'Trần Minh Bảo Hoàng', N'0909899999', N'Hà Nội', NULL, N'Phường Ba Đình', N'20 Đội Cấn - Địa chỉ demo', 1, 1, 1)
INSERT [dbo].[dia_chi_khach_hang] ([id], [id_khach_hang], [ten_nguoi_nhan], [sdt_nguoi_nhan], [tinh_thanh], [quan_huyen], [phuong_xa], [dia_chi_cu_the], [loai_dia_chi], [is_mac_dinh], [trang_thai]) VALUES (5, 2, N'Nguyễn Thị An', N'0911111111', N'Hà Nội', NULL, N'Phường Ba Đình', N'20 Đội Cấn - Địa chỉ demo', 1, 1, 1)
INSERT [dbo].[dia_chi_khach_hang] ([id], [id_khach_hang], [ten_nguoi_nhan], [sdt_nguoi_nhan], [tinh_thanh], [quan_huyen], [phuong_xa], [dia_chi_cu_the], [loai_dia_chi], [is_mac_dinh], [trang_thai]) VALUES (6, 3, N'Lê Quốc Hưng', N'0911111111', N'Hà Nội', NULL, N'Phường Ba Đình', N'20 Đội Cấn - Địa chỉ demo', 1, 1, 1)
INSERT [dbo].[dia_chi_khach_hang] ([id], [id_khach_hang], [ten_nguoi_nhan], [sdt_nguoi_nhan], [tinh_thanh], [quan_huyen], [phuong_xa], [dia_chi_cu_the], [loai_dia_chi], [is_mac_dinh], [trang_thai]) VALUES (7, 4, N'Phạm Thanh Tú', N'0983214567', N'Hà Nội', NULL, N'Phường Ba Đình', N'20 Đội Cấn - Địa chỉ demo', 1, 1, 1)
INSERT [dbo].[dia_chi_khach_hang] ([id], [id_khach_hang], [ten_nguoi_nhan], [sdt_nguoi_nhan], [tinh_thanh], [quan_huyen], [phuong_xa], [dia_chi_cu_the], [loai_dia_chi], [is_mac_dinh], [trang_thai]) VALUES (8, 5, N'Võ Gia Hân', N'0905882114', N'Hà Nội', NULL, N'Phường Ba Đình', N'20 Đội Cấn - Địa chỉ demo', 1, 1, 1)
INSERT [dbo].[dia_chi_khach_hang] ([id], [id_khach_hang], [ten_nguoi_nhan], [sdt_nguoi_nhan], [tinh_thanh], [quan_huyen], [phuong_xa], [dia_chi_cu_the], [loai_dia_chi], [is_mac_dinh], [trang_thai]) VALUES (9, 6, N'Đặng Hoài Nam', N'0934625881', N'Hà Nội', NULL, N'Phường Ba Đình', N'20 Đội Cấn - Địa chỉ demo', 1, 1, 1)
INSERT [dbo].[dia_chi_khach_hang] ([id], [id_khach_hang], [ten_nguoi_nhan], [sdt_nguoi_nhan], [tinh_thanh], [quan_huyen], [phuong_xa], [dia_chi_cu_the], [loai_dia_chi], [is_mac_dinh], [trang_thai]) VALUES (10, 7, N'Bùi Mỹ Linh', N'0972230456', N'Hà Nội', NULL, N'Phường Ba Đình', N'20 Đội Cấn - Địa chỉ demo', 1, 1, 1)
INSERT [dbo].[dia_chi_khach_hang] ([id], [id_khach_hang], [ten_nguoi_nhan], [sdt_nguoi_nhan], [tinh_thanh], [quan_huyen], [phuong_xa], [dia_chi_cu_the], [loai_dia_chi], [is_mac_dinh], [trang_thai]) VALUES (11, 8, N'Ngô Đức Anh', N'0902718663', N'Hà Nội', NULL, N'Phường Ba Đình', N'20 Đội Cấn - Địa chỉ demo', 1, 1, 1)
INSERT [dbo].[dia_chi_khach_hang] ([id], [id_khach_hang], [ten_nguoi_nhan], [sdt_nguoi_nhan], [tinh_thanh], [quan_huyen], [phuong_xa], [dia_chi_cu_the], [loai_dia_chi], [is_mac_dinh], [trang_thai]) VALUES (12, 9, N'Đỗ Phương Vy', N'0968440127', N'Hà Nội', NULL, N'Phường Ba Đình', N'20 Đội Cấn - Địa chỉ demo', 1, 1, 1)
INSERT [dbo].[dia_chi_khach_hang] ([id], [id_khach_hang], [ten_nguoi_nhan], [sdt_nguoi_nhan], [tinh_thanh], [quan_huyen], [phuong_xa], [dia_chi_cu_the], [loai_dia_chi], [is_mac_dinh], [trang_thai]) VALUES (13, 10, N'Mai Tiến Thành', N'0918305902', N'Hà Nội', NULL, N'Phường Ba Đình', N'20 Đội Cấn - Địa chỉ demo', 1, 1, 1)
INSERT [dbo].[dia_chi_khach_hang] ([id], [id_khach_hang], [ten_nguoi_nhan], [sdt_nguoi_nhan], [tinh_thanh], [quan_huyen], [phuong_xa], [dia_chi_cu_the], [loai_dia_chi], [is_mac_dinh], [trang_thai]) VALUES (17, 17, N'DEMO UI Khách hàng 20261007', N'0998800001', N'Hà Nội', NULL, N'Phường Cầu Giấy', N'30 Đội Cấn - địa chỉ demo UI đã kiểm tra', 1, 1, 1)
SET IDENTITY_INSERT [dbo].[dia_chi_khach_hang] OFF
-- DATA: [dbo].[dot_giam_gia]
SET IDENTITY_INSERT [dbo].[dot_giam_gia] ON 

INSERT [dbo].[dot_giam_gia] ([id], [ma_dot_giam_gia], [ten_dot_giam_gia], [phan_tram_giam_dot], [ngay_bat_dau], [ngay_ket_thuc], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (1, N'DGG001', N'DEMO - Đợt giảm giá đang diễn ra', CAST(10.00 AS Decimal(18, 2)), CAST(N'2026-10-06T00:00:00.0000000' AS DateTime2), CAST(N'2026-10-12T23:59:59.0000000' AS DateTime2), 1, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), 1)
INSERT [dbo].[dot_giam_gia] ([id], [ma_dot_giam_gia], [ten_dot_giam_gia], [phan_tram_giam_dot], [ngay_bat_dau], [ngay_ket_thuc], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (2, N'DGG002', N'DEMO - Đợt giảm giá sắp diễn ra', CAST(15.00 AS Decimal(18, 2)), CAST(N'2026-10-14T00:00:00.0000000' AS DateTime2), CAST(N'2026-10-21T23:59:59.0000000' AS DateTime2), 1, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), 1)
INSERT [dbo].[dot_giam_gia] ([id], [ma_dot_giam_gia], [ten_dot_giam_gia], [phan_tram_giam_dot], [ngay_bat_dau], [ngay_ket_thuc], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (3, N'DGG003', N'DEMO - Đợt giảm giá đã kết thúc', CAST(5.00 AS Decimal(18, 2)), CAST(N'2026-09-17T00:00:00.0000000' AS DateTime2), CAST(N'2026-10-05T23:59:59.0000000' AS DateTime2), 1, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), 1)
INSERT [dbo].[dot_giam_gia] ([id], [ma_dot_giam_gia], [ten_dot_giam_gia], [phan_tram_giam_dot], [ngay_bat_dau], [ngay_ket_thuc], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (5, N'DGG004', N'DEMO UI 20261007 - Đợt đã kiểm tra', CAST(3.00 AS Decimal(18, 2)), CAST(N'2026-10-06T00:00:00.0000000' AS DateTime2), CAST(N'2026-10-14T23:59:59.0000000' AS DateTime2), 1, CAST(N'2026-10-07T02:12:06.2739715' AS DateTime2), CAST(N'2026-10-07T02:18:48.4142832' AS DateTime2), 1)
SET IDENTITY_INSERT [dbo].[dot_giam_gia] OFF
-- DATA: [dbo].[hinh_anh_san_pham]
SET IDENTITY_INSERT [dbo].[hinh_anh_san_pham] ON 

INSERT [dbo].[hinh_anh_san_pham] ([id], [id_san_pham], [url_anh], [is_anh_chinh]) VALUES (37, 4, N'/uploads/products/4fd52df5-f621-4f62-ab6a-3a1d978765c8.png', 1)
INSERT [dbo].[hinh_anh_san_pham] ([id], [id_san_pham], [url_anh], [is_anh_chinh]) VALUES (56, 3, N'/uploads/products/d00ce3b7-d173-4762-939f-d3fd13ded48a.png', 1)
INSERT [dbo].[hinh_anh_san_pham] ([id], [id_san_pham], [url_anh], [is_anh_chinh]) VALUES (57, 88, N'/uploads/products/eb29524e-74b6-4e41-b376-a3c18739e0bc.png', 1)
INSERT [dbo].[hinh_anh_san_pham] ([id], [id_san_pham], [url_anh], [is_anh_chinh]) VALUES (58, 88, N'/uploads/products/c2eb9d6f-bc97-4d80-ac77-85f76b16fc5e.png', 0)
SET IDENTITY_INSERT [dbo].[hinh_anh_san_pham] OFF
-- DATA: [dbo].[hinh_thuc_thanh_toan]
SET IDENTITY_INSERT [dbo].[hinh_thuc_thanh_toan] ON 

INSERT [dbo].[hinh_thuc_thanh_toan] ([id], [ma_hinh_thuc], [ten_hinh_thuc], [trang_thai]) VALUES (1, N'TRUC_TIEP', N'Trực tiếp', 1)
INSERT [dbo].[hinh_thuc_thanh_toan] ([id], [ma_hinh_thuc], [ten_hinh_thuc], [trang_thai]) VALUES (2, N'TRUC_TUYEN', N'Trực tuyến', 1)
SET IDENTITY_INSERT [dbo].[hinh_thuc_thanh_toan] OFF
-- DATA: [dbo].[hoa_don]
SET IDENTITY_INSERT [dbo].[hoa_don] ON 

INSERT [dbo].[hoa_don] ([id], [id_khach_hang], [id_nhan_vien], [id_phieu_giam_gia], [id_phuong_thuc_thanh_toan], [ma_hoa_don], [loai_hoa_don], [tong_tien], [phi_van_chuyen], [thanh_tien], [don_vi_van_chuyen], [ho_ten_nguoi_nhan], [so_dien_thoai_nguoi_nhan], [gia_chi_giao_hang], [ghi_chu], [ngay_thanh_toan], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (1, 1, 6, NULL, 1, N'HD000001', 0, CAST(600000.00 AS Decimal(18, 2)), CAST(0.00 AS Decimal(18, 2)), CAST(600000.00 AS Decimal(18, 2)), NULL, N'Trần Minh Bảo Hoàng', N'0909899999', NULL, N'DEMO-STABILIZATION-01', CAST(N'2026-09-27T03:49:07.8364510' AS DateTime2), CAST(N'2026-09-27T01:49:07.8364510' AS DateTime2), CAST(N'2026-09-27T03:49:07.8364510' AS DateTime2), 5)
INSERT [dbo].[hoa_don] ([id], [id_khach_hang], [id_nhan_vien], [id_phieu_giam_gia], [id_phuong_thuc_thanh_toan], [ma_hoa_don], [loai_hoa_don], [tong_tien], [phi_van_chuyen], [thanh_tien], [don_vi_van_chuyen], [ho_ten_nguoi_nhan], [so_dien_thoai_nguoi_nhan], [gia_chi_giao_hang], [ghi_chu], [ngay_thanh_toan], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (2, 2, 7, NULL, 3, N'HD000002', 1, CAST(300123.00 AS Decimal(18, 2)), CAST(30000.00 AS Decimal(18, 2)), CAST(330123.00 AS Decimal(18, 2)), N'Giao hàng demo', N'Nguyễn Thị An', N'0911111111', NULL, N'DEMO-STABILIZATION-02', CAST(N'2026-09-28T03:49:07.8364510' AS DateTime2), CAST(N'2026-09-28T01:49:07.8364510' AS DateTime2), CAST(N'2026-09-28T03:49:07.8364510' AS DateTime2), 5)
INSERT [dbo].[hoa_don] ([id], [id_khach_hang], [id_nhan_vien], [id_phieu_giam_gia], [id_phuong_thuc_thanh_toan], [ma_hoa_don], [loai_hoa_don], [tong_tien], [phi_van_chuyen], [thanh_tien], [don_vi_van_chuyen], [ho_ten_nguoi_nhan], [so_dien_thoai_nguoi_nhan], [gia_chi_giao_hang], [ghi_chu], [ngay_thanh_toan], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (3, 3, 8, NULL, 3, N'HD000003', 2, CAST(246.00 AS Decimal(18, 2)), CAST(30000.00 AS Decimal(18, 2)), CAST(30246.00 AS Decimal(18, 2)), N'Giao hàng demo', N'Lê Quốc Hưng', N'0911111111', NULL, N'DEMO-STABILIZATION-03', CAST(N'2026-09-29T03:49:07.8364510' AS DateTime2), CAST(N'2026-09-29T01:49:07.8364510' AS DateTime2), CAST(N'2026-09-29T03:49:07.8364510' AS DateTime2), 5)
INSERT [dbo].[hoa_don] ([id], [id_khach_hang], [id_nhan_vien], [id_phieu_giam_gia], [id_phuong_thuc_thanh_toan], [ma_hoa_don], [loai_hoa_don], [tong_tien], [phi_van_chuyen], [thanh_tien], [don_vi_van_chuyen], [ho_ten_nguoi_nhan], [so_dien_thoai_nguoi_nhan], [gia_chi_giao_hang], [ghi_chu], [ngay_thanh_toan], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (4, 4, 1, NULL, 1, N'HD000004', 0, CAST(300123.00 AS Decimal(18, 2)), CAST(0.00 AS Decimal(18, 2)), CAST(300123.00 AS Decimal(18, 2)), NULL, N'Phạm Thanh Tú', N'0983214567', NULL, N'DEMO-STABILIZATION-04', CAST(N'2026-09-30T03:49:07.8364510' AS DateTime2), CAST(N'2026-09-30T01:49:07.8364510' AS DateTime2), CAST(N'2026-09-30T03:49:07.8364510' AS DateTime2), 5)
INSERT [dbo].[hoa_don] ([id], [id_khach_hang], [id_nhan_vien], [id_phieu_giam_gia], [id_phuong_thuc_thanh_toan], [ma_hoa_don], [loai_hoa_don], [tong_tien], [phi_van_chuyen], [thanh_tien], [don_vi_van_chuyen], [ho_ten_nguoi_nhan], [so_dien_thoai_nguoi_nhan], [gia_chi_giao_hang], [ghi_chu], [ngay_thanh_toan], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (5, 5, 2, NULL, 3, N'HD000005', 1, CAST(600000.00 AS Decimal(18, 2)), CAST(30000.00 AS Decimal(18, 2)), CAST(630000.00 AS Decimal(18, 2)), N'Giao hàng demo', N'Võ Gia Hân', N'0905882114', NULL, N'DEMO-STABILIZATION-05', CAST(N'2026-10-01T03:49:07.8364510' AS DateTime2), CAST(N'2026-10-01T01:49:07.8364510' AS DateTime2), CAST(N'2026-10-01T03:49:07.8364510' AS DateTime2), 5)
INSERT [dbo].[hoa_don] ([id], [id_khach_hang], [id_nhan_vien], [id_phieu_giam_gia], [id_phuong_thuc_thanh_toan], [ma_hoa_don], [loai_hoa_don], [tong_tien], [phi_van_chuyen], [thanh_tien], [don_vi_van_chuyen], [ho_ten_nguoi_nhan], [so_dien_thoai_nguoi_nhan], [gia_chi_giao_hang], [ghi_chu], [ngay_thanh_toan], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (6, 6, 3, NULL, 4, N'HD000006', 1, CAST(600000.00 AS Decimal(18, 2)), CAST(30000.00 AS Decimal(18, 2)), CAST(630000.00 AS Decimal(18, 2)), N'Giao hàng demo', N'Đặng Hoài Nam', N'0934625881', NULL, N'DEMO-STABILIZATION-06', NULL, CAST(N'2026-10-02T01:49:07.8364510' AS DateTime2), CAST(N'2026-10-07T02:07:18.3124557' AS DateTime2), 1)
INSERT [dbo].[hoa_don] ([id], [id_khach_hang], [id_nhan_vien], [id_phieu_giam_gia], [id_phuong_thuc_thanh_toan], [ma_hoa_don], [loai_hoa_don], [tong_tien], [phi_van_chuyen], [thanh_tien], [don_vi_van_chuyen], [ho_ten_nguoi_nhan], [so_dien_thoai_nguoi_nhan], [gia_chi_giao_hang], [ghi_chu], [ngay_thanh_toan], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (7, 7, 6, NULL, 4, N'HD000007', 1, CAST(400000.00 AS Decimal(18, 2)), CAST(30000.00 AS Decimal(18, 2)), CAST(430000.00 AS Decimal(18, 2)), N'Giao hàng demo', N'Bùi Mỹ Linh', N'0972230456', NULL, N'DEMO-STABILIZATION-07', NULL, CAST(N'2026-10-03T01:49:07.8364510' AS DateTime2), CAST(N'2026-10-03T03:49:07.8364510' AS DateTime2), 1)
INSERT [dbo].[hoa_don] ([id], [id_khach_hang], [id_nhan_vien], [id_phieu_giam_gia], [id_phuong_thuc_thanh_toan], [ma_hoa_don], [loai_hoa_don], [tong_tien], [phi_van_chuyen], [thanh_tien], [don_vi_van_chuyen], [ho_ten_nguoi_nhan], [so_dien_thoai_nguoi_nhan], [gia_chi_giao_hang], [ghi_chu], [ngay_thanh_toan], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (8, 8, 7, NULL, 4, N'HD000008', 2, CAST(200000.00 AS Decimal(18, 2)), CAST(30000.00 AS Decimal(18, 2)), CAST(230000.00 AS Decimal(18, 2)), N'Giao hàng demo', N'Ngô Đức Anh', N'0902718663', NULL, N'DEMO-STABILIZATION-08', NULL, CAST(N'2026-10-04T01:49:07.8364510' AS DateTime2), CAST(N'2026-10-04T03:49:07.8364510' AS DateTime2), 3)
INSERT [dbo].[hoa_don] ([id], [id_khach_hang], [id_nhan_vien], [id_phieu_giam_gia], [id_phuong_thuc_thanh_toan], [ma_hoa_don], [loai_hoa_don], [tong_tien], [phi_van_chuyen], [thanh_tien], [don_vi_van_chuyen], [ho_ten_nguoi_nhan], [so_dien_thoai_nguoi_nhan], [gia_chi_giao_hang], [ghi_chu], [ngay_thanh_toan], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (9, 9, 8, NULL, 4, N'HD000009', 2, CAST(1699000.00 AS Decimal(18, 2)), CAST(30000.00 AS Decimal(18, 2)), CAST(1729000.00 AS Decimal(18, 2)), N'Giao hàng demo', N'Đỗ Phương Vy', N'0968440127', NULL, N'DEMO-STABILIZATION-09', NULL, CAST(N'2026-10-05T01:49:07.8364510' AS DateTime2), CAST(N'2026-10-05T03:49:07.8364510' AS DateTime2), 4)
INSERT [dbo].[hoa_don] ([id], [id_khach_hang], [id_nhan_vien], [id_phieu_giam_gia], [id_phuong_thuc_thanh_toan], [ma_hoa_don], [loai_hoa_don], [tong_tien], [phi_van_chuyen], [thanh_tien], [don_vi_van_chuyen], [ho_ten_nguoi_nhan], [so_dien_thoai_nguoi_nhan], [gia_chi_giao_hang], [ghi_chu], [ngay_thanh_toan], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (10, 10, 1, NULL, 4, N'HD000010', 1, CAST(1799000.00 AS Decimal(18, 2)), CAST(30000.00 AS Decimal(18, 2)), CAST(1829000.00 AS Decimal(18, 2)), N'Giao hàng demo', N'Mai Tiến Thành', N'0918305902', NULL, N'DEMO-STABILIZATION-10', NULL, CAST(N'2026-10-06T01:49:07.8364510' AS DateTime2), CAST(N'2026-10-06T03:49:07.8364510' AS DateTime2), 6)
SET IDENTITY_INSERT [dbo].[hoa_don] OFF
-- DATA: [dbo].[hoa_don_chi_tiet]
SET IDENTITY_INSERT [dbo].[hoa_don_chi_tiet] ON 

INSERT [dbo].[hoa_don_chi_tiet] ([id], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong], [don_gia], [thanh_tien], [ghi_chu], [trang_thai]) VALUES (0, 1, 15, 1, CAST(300000.00 AS Decimal(18, 2)), CAST(300000.00 AS Decimal(18, 2)), N'Chi tiết demo; không điều chỉnh tồn kho', 1)
INSERT [dbo].[hoa_don_chi_tiet] ([id], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong], [don_gia], [thanh_tien], [ghi_chu], [trang_thai]) VALUES (1, 1, 14, 1, CAST(300000.00 AS Decimal(18, 2)), CAST(300000.00 AS Decimal(18, 2)), N'Chi tiết demo; không điều chỉnh tồn kho', 1)
INSERT [dbo].[hoa_don_chi_tiet] ([id], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong], [don_gia], [thanh_tien], [ghi_chu], [trang_thai]) VALUES (2, 2, 13, 1, CAST(300000.00 AS Decimal(18, 2)), CAST(300000.00 AS Decimal(18, 2)), N'Chi tiết demo; không điều chỉnh tồn kho', 1)
INSERT [dbo].[hoa_don_chi_tiet] ([id], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong], [don_gia], [thanh_tien], [ghi_chu], [trang_thai]) VALUES (3, 2, 6, 1, CAST(123.00 AS Decimal(18, 2)), CAST(123.00 AS Decimal(18, 2)), N'Chi tiết demo; không điều chỉnh tồn kho', 1)
INSERT [dbo].[hoa_don_chi_tiet] ([id], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong], [don_gia], [thanh_tien], [ghi_chu], [trang_thai]) VALUES (4, 3, 5, 1, CAST(123.00 AS Decimal(18, 2)), CAST(123.00 AS Decimal(18, 2)), N'Chi tiết demo; không điều chỉnh tồn kho', 1)
INSERT [dbo].[hoa_don_chi_tiet] ([id], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong], [don_gia], [thanh_tien], [ghi_chu], [trang_thai]) VALUES (5, 3, 4, 1, CAST(123.00 AS Decimal(18, 2)), CAST(123.00 AS Decimal(18, 2)), N'Chi tiết demo; không điều chỉnh tồn kho', 1)
INSERT [dbo].[hoa_don_chi_tiet] ([id], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong], [don_gia], [thanh_tien], [ghi_chu], [trang_thai]) VALUES (6, 4, 3, 1, CAST(123.00 AS Decimal(18, 2)), CAST(123.00 AS Decimal(18, 2)), N'Chi tiết demo; không điều chỉnh tồn kho', 1)
INSERT [dbo].[hoa_don_chi_tiet] ([id], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong], [don_gia], [thanh_tien], [ghi_chu], [trang_thai]) VALUES (7, 4, 12, 1, CAST(300000.00 AS Decimal(18, 2)), CAST(300000.00 AS Decimal(18, 2)), N'Chi tiết demo; không điều chỉnh tồn kho', 1)
INSERT [dbo].[hoa_don_chi_tiet] ([id], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong], [don_gia], [thanh_tien], [ghi_chu], [trang_thai]) VALUES (8, 5, 11, 1, CAST(300000.00 AS Decimal(18, 2)), CAST(300000.00 AS Decimal(18, 2)), N'Chi tiết demo; không điều chỉnh tồn kho', 1)
INSERT [dbo].[hoa_don_chi_tiet] ([id], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong], [don_gia], [thanh_tien], [ghi_chu], [trang_thai]) VALUES (9, 5, 10, 1, CAST(300000.00 AS Decimal(18, 2)), CAST(300000.00 AS Decimal(18, 2)), N'Chi tiết demo; không điều chỉnh tồn kho', 1)
INSERT [dbo].[hoa_don_chi_tiet] ([id], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong], [don_gia], [thanh_tien], [ghi_chu], [trang_thai]) VALUES (10, 6, 9, 1, CAST(300000.00 AS Decimal(18, 2)), CAST(300000.00 AS Decimal(18, 2)), N'Chi tiết demo; không điều chỉnh tồn kho', 1)
INSERT [dbo].[hoa_don_chi_tiet] ([id], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong], [don_gia], [thanh_tien], [ghi_chu], [trang_thai]) VALUES (11, 6, 8, 1, CAST(300000.00 AS Decimal(18, 2)), CAST(300000.00 AS Decimal(18, 2)), N'Chi tiết demo; không điều chỉnh tồn kho', 1)
INSERT [dbo].[hoa_don_chi_tiet] ([id], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong], [don_gia], [thanh_tien], [ghi_chu], [trang_thai]) VALUES (12, 7, 7, 1, CAST(300000.00 AS Decimal(18, 2)), CAST(300000.00 AS Decimal(18, 2)), N'Chi tiết demo; không điều chỉnh tồn kho', 1)
INSERT [dbo].[hoa_don_chi_tiet] ([id], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong], [don_gia], [thanh_tien], [ghi_chu], [trang_thai]) VALUES (13, 7, 19, 1, CAST(100000.00 AS Decimal(18, 2)), CAST(100000.00 AS Decimal(18, 2)), N'Chi tiết demo; không điều chỉnh tồn kho', 1)
INSERT [dbo].[hoa_don_chi_tiet] ([id], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong], [don_gia], [thanh_tien], [ghi_chu], [trang_thai]) VALUES (14, 8, 18, 1, CAST(100000.00 AS Decimal(18, 2)), CAST(100000.00 AS Decimal(18, 2)), N'Chi tiết demo; không điều chỉnh tồn kho', 1)
INSERT [dbo].[hoa_don_chi_tiet] ([id], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong], [don_gia], [thanh_tien], [ghi_chu], [trang_thai]) VALUES (15, 8, 17, 1, CAST(100000.00 AS Decimal(18, 2)), CAST(100000.00 AS Decimal(18, 2)), N'Chi tiết demo; không điều chỉnh tồn kho', 1)
INSERT [dbo].[hoa_don_chi_tiet] ([id], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong], [don_gia], [thanh_tien], [ghi_chu], [trang_thai]) VALUES (16, 9, 16, 1, CAST(100000.00 AS Decimal(18, 2)), CAST(100000.00 AS Decimal(18, 2)), N'Chi tiết demo; không điều chỉnh tồn kho', 1)
INSERT [dbo].[hoa_don_chi_tiet] ([id], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong], [don_gia], [thanh_tien], [ghi_chu], [trang_thai]) VALUES (17, 9, 1, 1, CAST(1599000.00 AS Decimal(18, 2)), CAST(1599000.00 AS Decimal(18, 2)), N'Chi tiết demo; không điều chỉnh tồn kho', 1)
INSERT [dbo].[hoa_don_chi_tiet] ([id], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong], [don_gia], [thanh_tien], [ghi_chu], [trang_thai]) VALUES (18, 10, 2, 1, CAST(1699000.00 AS Decimal(18, 2)), CAST(1699000.00 AS Decimal(18, 2)), N'Chi tiết demo; không điều chỉnh tồn kho', 1)
INSERT [dbo].[hoa_don_chi_tiet] ([id], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong], [don_gia], [thanh_tien], [ghi_chu], [trang_thai]) VALUES (19, 10, 84, 1, CAST(100000.00 AS Decimal(18, 2)), CAST(100000.00 AS Decimal(18, 2)), N'Chi tiết demo; không điều chỉnh tồn kho', 1)
SET IDENTITY_INSERT [dbo].[hoa_don_chi_tiet] OFF
-- DATA: [dbo].[khach_hang]
SET IDENTITY_INSERT [dbo].[khach_hang] ON 

INSERT [dbo].[khach_hang] ([id], [ma_khach_hang], [ten_tai_khoan], [ten_khach_hang], [email], [so_dien_thoai], [ngay_sinh], [gioi_tinh], [mat_khau], [hinh_anh], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (1, N'KH001', N'kh001', N'Trần Minh Bảo Hoàng', NULL, N'0909899999', NULL, NULL, NULL, NULL, 1, CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2), CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2))
INSERT [dbo].[khach_hang] ([id], [ma_khach_hang], [ten_tai_khoan], [ten_khach_hang], [email], [so_dien_thoai], [ngay_sinh], [gioi_tinh], [mat_khau], [hinh_anh], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (2, N'KH002', N'kh002', N'Nguyễn Thị An', NULL, N'0911111111', NULL, NULL, NULL, NULL, 1, CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2), CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2))
INSERT [dbo].[khach_hang] ([id], [ma_khach_hang], [ten_tai_khoan], [ten_khach_hang], [email], [so_dien_thoai], [ngay_sinh], [gioi_tinh], [mat_khau], [hinh_anh], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (3, N'KH003', N'kh003', N'Lê Quốc Hưng', NULL, N'0911111111', NULL, NULL, NULL, NULL, 1, CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2), CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2))
INSERT [dbo].[khach_hang] ([id], [ma_khach_hang], [ten_tai_khoan], [ten_khach_hang], [email], [so_dien_thoai], [ngay_sinh], [gioi_tinh], [mat_khau], [hinh_anh], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (4, N'KH004', N'kh004', N'Phạm Thanh Tú', NULL, N'0983214567', NULL, NULL, NULL, NULL, 1, CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2), CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2))
INSERT [dbo].[khach_hang] ([id], [ma_khach_hang], [ten_tai_khoan], [ten_khach_hang], [email], [so_dien_thoai], [ngay_sinh], [gioi_tinh], [mat_khau], [hinh_anh], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (5, N'KH005', N'kh005', N'Võ Gia Hân', NULL, N'0905882114', NULL, NULL, NULL, NULL, 1, CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2), CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2))
INSERT [dbo].[khach_hang] ([id], [ma_khach_hang], [ten_tai_khoan], [ten_khach_hang], [email], [so_dien_thoai], [ngay_sinh], [gioi_tinh], [mat_khau], [hinh_anh], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (6, N'KH006', N'kh006', N'Đặng Hoài Nam', NULL, N'0934625881', NULL, NULL, NULL, NULL, 1, CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2), CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2))
INSERT [dbo].[khach_hang] ([id], [ma_khach_hang], [ten_tai_khoan], [ten_khach_hang], [email], [so_dien_thoai], [ngay_sinh], [gioi_tinh], [mat_khau], [hinh_anh], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (7, N'KH007', N'kh007', N'Bùi Mỹ Linh', NULL, N'0972230456', NULL, NULL, NULL, NULL, 1, CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2), CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2))
INSERT [dbo].[khach_hang] ([id], [ma_khach_hang], [ten_tai_khoan], [ten_khach_hang], [email], [so_dien_thoai], [ngay_sinh], [gioi_tinh], [mat_khau], [hinh_anh], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (8, N'KH008', N'kh008', N'Ngô Đức Anh', NULL, N'0902718663', NULL, NULL, NULL, NULL, 1, CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2), CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2))
INSERT [dbo].[khach_hang] ([id], [ma_khach_hang], [ten_tai_khoan], [ten_khach_hang], [email], [so_dien_thoai], [ngay_sinh], [gioi_tinh], [mat_khau], [hinh_anh], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (9, N'KH009', N'kh009', N'Đỗ Phương Vy', NULL, N'0968440127', NULL, NULL, NULL, NULL, 1, CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2), CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2))
INSERT [dbo].[khach_hang] ([id], [ma_khach_hang], [ten_tai_khoan], [ten_khach_hang], [email], [so_dien_thoai], [ngay_sinh], [gioi_tinh], [mat_khau], [hinh_anh], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (10, N'KH010', N'kh010', N'Mai Tiến Thành', NULL, N'0918305902', NULL, NULL, NULL, NULL, 1, CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2), CAST(N'2026-10-06T14:14:49.3737710' AS DateTime2))
INSERT [dbo].[khach_hang] ([id], [ma_khach_hang], [ten_tai_khoan], [ten_khach_hang], [email], [so_dien_thoai], [ngay_sinh], [gioi_tinh], [mat_khau], [hinh_anh], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (17, N'KH0017', N'KH0017', N'DEMO UI Khách hàng 20261007 - đã kiểm tra', N'stabilization.customer.20261007@smashstep.example', N'0998800001', NULL, 1, NULL, N'/uploads/avatars/56195cda-afe2-4f41-9388-a6ad99864a97.png', 1, CAST(N'2026-10-07T01:58:21.5396646' AS DateTime2), CAST(N'2026-10-07T03:09:03.1153744' AS DateTime2))
SET IDENTITY_INSERT [dbo].[khach_hang] OFF
-- DATA: [dbo].[kich_thuoc]
SET IDENTITY_INSERT [dbo].[kich_thuoc] ON 

INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (1, N'35', N'Size 35', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2))
INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (2, N'36', N'Size 36', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2))
INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (3, N'36.5', N'Size 36.5', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2))
INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (4, N'37', N'Size 37', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2))
INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (5, N'37.5', N'Size 37.5', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2))
INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (6, N'38', N'Size 38', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2))
INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (7, N'38.5', N'Size 38.5', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2))
INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (8, N'39', N'Size 39', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2))
INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (9, N'39.5', N'Size 39.5', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2))
INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (10, N'40', N'Size 40', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2))
INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (11, N'40.5', N'Size 40.5', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2))
INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (12, N'41', N'Size 41', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2))
INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (13, N'41.5', N'Size 41.5', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2))
INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (14, N'42', N'Size 42', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2))
INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (15, N'42.5', N'Size 42.5', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2))
INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (16, N'43', N'Size 43', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2))
INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (17, N'43.5', N'Size 43.5', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2))
INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (18, N'44', N'Size 44', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2))
INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (19, N'44.5', N'Size 44.5', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2))
INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (20, N'45', N'Size 45', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2))
INSERT [dbo].[kich_thuoc] ([id], [gia_tri], [ghi_chu], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (21, N'46', N'Size 46', 1, CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2), CAST(N'2026-10-03T21:42:46.2966667' AS DateTime2))
SET IDENTITY_INSERT [dbo].[kich_thuoc] OFF
-- DATA: [dbo].[kieu_dang]
SET IDENTITY_INSERT [dbo].[kieu_dang] ON 

INSERT [dbo].[kieu_dang] ([id], [ma_kieu_dang], [ten_kieu_dang], [trang_thai]) VALUES (1, N'KD001', N'Running', 1)
INSERT [dbo].[kieu_dang] ([id], [ma_kieu_dang], [ten_kieu_dang], [trang_thai]) VALUES (2, N'KD002', N'Sneaker', 1)
INSERT [dbo].[kieu_dang] ([id], [ma_kieu_dang], [ten_kieu_dang], [trang_thai]) VALUES (3, N'KD003', N'Basketball', 1)
INSERT [dbo].[kieu_dang] ([id], [ma_kieu_dang], [ten_kieu_dang], [trang_thai]) VALUES (4, N'KD004', N'Casual', 1)
INSERT [dbo].[kieu_dang] ([id], [ma_kieu_dang], [ten_kieu_dang], [trang_thai]) VALUES (5, N'KD005', N'Training', 1)
INSERT [dbo].[kieu_dang] ([id], [ma_kieu_dang], [ten_kieu_dang], [trang_thai]) VALUES (6, N'KD006', N'Football', 1)
INSERT [dbo].[kieu_dang] ([id], [ma_kieu_dang], [ten_kieu_dang], [trang_thai]) VALUES (7, N'KD007', N'Tennis', 1)
INSERT [dbo].[kieu_dang] ([id], [ma_kieu_dang], [ten_kieu_dang], [trang_thai]) VALUES (8, N'KD008', N'Walking', 1)
INSERT [dbo].[kieu_dang] ([id], [ma_kieu_dang], [ten_kieu_dang], [trang_thai]) VALUES (9, N'KD009', N'Skate', 1)
INSERT [dbo].[kieu_dang] ([id], [ma_kieu_dang], [ten_kieu_dang], [trang_thai]) VALUES (10, N'KD010', N'Lifestyle', 1)
SET IDENTITY_INSERT [dbo].[kieu_dang] OFF
-- DATA: [dbo].[lich_su_hoa_don]
SET IDENTITY_INSERT [dbo].[lich_su_hoa_don] ON 

INSERT [dbo].[lich_su_hoa_don] ([id], [id_hoa_don], [nguoi_tao], [trang_thai], [ghi_chu], [ngay_tao]) VALUES (1, 1, 6, 5, N'Trạng thái hóa đơn demo tích hợp', CAST(N'2026-09-27T03:49:07.8364510' AS DateTime2))
INSERT [dbo].[lich_su_hoa_don] ([id], [id_hoa_don], [nguoi_tao], [trang_thai], [ghi_chu], [ngay_tao]) VALUES (2, 2, 7, 5, N'Trạng thái hóa đơn demo tích hợp', CAST(N'2026-09-28T03:49:07.8364510' AS DateTime2))
INSERT [dbo].[lich_su_hoa_don] ([id], [id_hoa_don], [nguoi_tao], [trang_thai], [ghi_chu], [ngay_tao]) VALUES (3, 3, 8, 5, N'Trạng thái hóa đơn demo tích hợp', CAST(N'2026-09-29T03:49:07.8364510' AS DateTime2))
INSERT [dbo].[lich_su_hoa_don] ([id], [id_hoa_don], [nguoi_tao], [trang_thai], [ghi_chu], [ngay_tao]) VALUES (4, 4, 1, 5, N'Trạng thái hóa đơn demo tích hợp', CAST(N'2026-09-30T03:49:07.8364510' AS DateTime2))
INSERT [dbo].[lich_su_hoa_don] ([id], [id_hoa_don], [nguoi_tao], [trang_thai], [ghi_chu], [ngay_tao]) VALUES (5, 5, 2, 5, N'Trạng thái hóa đơn demo tích hợp', CAST(N'2026-10-01T03:49:07.8364510' AS DateTime2))
INSERT [dbo].[lich_su_hoa_don] ([id], [id_hoa_don], [nguoi_tao], [trang_thai], [ghi_chu], [ngay_tao]) VALUES (6, 6, 3, 0, N'Trạng thái hóa đơn demo tích hợp', CAST(N'2026-10-02T03:49:07.8364510' AS DateTime2))
INSERT [dbo].[lich_su_hoa_don] ([id], [id_hoa_don], [nguoi_tao], [trang_thai], [ghi_chu], [ngay_tao]) VALUES (7, 7, 6, 1, N'Trạng thái hóa đơn demo tích hợp', CAST(N'2026-10-03T03:49:07.8364510' AS DateTime2))
INSERT [dbo].[lich_su_hoa_don] ([id], [id_hoa_don], [nguoi_tao], [trang_thai], [ghi_chu], [ngay_tao]) VALUES (8, 8, 7, 3, N'Trạng thái hóa đơn demo tích hợp', CAST(N'2026-10-04T03:49:07.8364510' AS DateTime2))
INSERT [dbo].[lich_su_hoa_don] ([id], [id_hoa_don], [nguoi_tao], [trang_thai], [ghi_chu], [ngay_tao]) VALUES (9, 9, 8, 4, N'Trạng thái hóa đơn demo tích hợp', CAST(N'2026-10-05T03:49:07.8364510' AS DateTime2))
INSERT [dbo].[lich_su_hoa_don] ([id], [id_hoa_don], [nguoi_tao], [trang_thai], [ghi_chu], [ngay_tao]) VALUES (10, 10, 1, 6, N'Trạng thái hóa đơn demo tích hợp', CAST(N'2026-10-06T03:49:07.8364510' AS DateTime2))
INSERT [dbo].[lich_su_hoa_don] ([id], [id_hoa_don], [nguoi_tao], [trang_thai], [ghi_chu], [ngay_tao]) VALUES (12, 6, 3, 1, NULL, CAST(N'2026-10-07T02:07:18.3134508' AS DateTime2))
SET IDENTITY_INSERT [dbo].[lich_su_hoa_don] OFF
-- DATA: [dbo].[lich_su_thanh_toan]
SET IDENTITY_INSERT [dbo].[lich_su_thanh_toan] ON 

INSERT [dbo].[lich_su_thanh_toan] ([id], [id_hoa_don], [so_tien], [ma_giao_dich], [thoi_gian], [trang_thai], [mo_ta]) VALUES (1, 1, CAST(600000.00 AS Decimal(18, 2)), N'DEMO-HD000001', CAST(N'2026-09-27T03:49:07.8364510' AS DateTime2), 1, N'Thanh toán hóa đơn demo tích hợp')
INSERT [dbo].[lich_su_thanh_toan] ([id], [id_hoa_don], [so_tien], [ma_giao_dich], [thoi_gian], [trang_thai], [mo_ta]) VALUES (2, 2, CAST(330123.00 AS Decimal(18, 2)), N'DEMO-HD000002', CAST(N'2026-09-28T03:49:07.8364510' AS DateTime2), 1, N'Thanh toán hóa đơn demo tích hợp')
INSERT [dbo].[lich_su_thanh_toan] ([id], [id_hoa_don], [so_tien], [ma_giao_dich], [thoi_gian], [trang_thai], [mo_ta]) VALUES (3, 3, CAST(30246.00 AS Decimal(18, 2)), N'DEMO-HD000003', CAST(N'2026-09-29T03:49:07.8364510' AS DateTime2), 1, N'Thanh toán hóa đơn demo tích hợp')
INSERT [dbo].[lich_su_thanh_toan] ([id], [id_hoa_don], [so_tien], [ma_giao_dich], [thoi_gian], [trang_thai], [mo_ta]) VALUES (4, 4, CAST(300123.00 AS Decimal(18, 2)), N'DEMO-HD000004', CAST(N'2026-09-30T03:49:07.8364510' AS DateTime2), 1, N'Thanh toán hóa đơn demo tích hợp')
INSERT [dbo].[lich_su_thanh_toan] ([id], [id_hoa_don], [so_tien], [ma_giao_dich], [thoi_gian], [trang_thai], [mo_ta]) VALUES (5, 5, CAST(630000.00 AS Decimal(18, 2)), N'DEMO-HD000005', CAST(N'2026-10-01T03:49:07.8364510' AS DateTime2), 1, N'Thanh toán hóa đơn demo tích hợp')
SET IDENTITY_INSERT [dbo].[lich_su_thanh_toan] OFF
-- DATA: [dbo].[mau_sac]
SET IDENTITY_INSERT [dbo].[mau_sac] ON 

INSERT [dbo].[mau_sac] ([id], [ma_mau_sac], [ten_mau_sac], [ma_mau_hex], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (1, N'MS001', N'Đen', N'#000000', 1, CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2))
INSERT [dbo].[mau_sac] ([id], [ma_mau_sac], [ten_mau_sac], [ma_mau_hex], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (2, N'MS002', N'Trắng', N'#FFFFFF', 1, CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2))
INSERT [dbo].[mau_sac] ([id], [ma_mau_sac], [ten_mau_sac], [ma_mau_hex], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (3, N'MS003', N'Đỏ', N'#FF0000', 1, CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2))
INSERT [dbo].[mau_sac] ([id], [ma_mau_sac], [ten_mau_sac], [ma_mau_hex], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (4, N'MS004', N'Xanh dương', N'#0066FF', 1, CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2))
INSERT [dbo].[mau_sac] ([id], [ma_mau_sac], [ten_mau_sac], [ma_mau_hex], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (5, N'MS005', N'Xám', N'#808080', 1, CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2))
INSERT [dbo].[mau_sac] ([id], [ma_mau_sac], [ten_mau_sac], [ma_mau_hex], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (6, N'MS006', N'Xanh lá', N'#00A651', 1, CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2))
INSERT [dbo].[mau_sac] ([id], [ma_mau_sac], [ten_mau_sac], [ma_mau_hex], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (7, N'MS007', N'Be', N'#F5F5DC', 1, CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2))
INSERT [dbo].[mau_sac] ([id], [ma_mau_sac], [ten_mau_sac], [ma_mau_hex], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (8, N'MS008', N'Nâu', N'#8B4513', 1, CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2))
INSERT [dbo].[mau_sac] ([id], [ma_mau_sac], [ten_mau_sac], [ma_mau_hex], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (9, N'MS009', N'Vàng', N'#FFD700', 1, CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2))
INSERT [dbo].[mau_sac] ([id], [ma_mau_sac], [ten_mau_sac], [ma_mau_hex], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (10, N'MS010', N'Cam', N'#FF8C00', 1, CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2))
INSERT [dbo].[mau_sac] ([id], [ma_mau_sac], [ten_mau_sac], [ma_mau_hex], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (11, N'MS011', N'Hồng', N'#FF69B4', 1, CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2))
INSERT [dbo].[mau_sac] ([id], [ma_mau_sac], [ten_mau_sac], [ma_mau_hex], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (12, N'MS012', N'Tím', N'#800080', 1, CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2))
INSERT [dbo].[mau_sac] ([id], [ma_mau_sac], [ten_mau_sac], [ma_mau_hex], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (13, N'MS013', N'Xanh navy', N'#000080', 1, CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2))
INSERT [dbo].[mau_sac] ([id], [ma_mau_sac], [ten_mau_sac], [ma_mau_hex], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (14, N'MS014', N'Kem', N'#FFFDD0', 1, CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2))
INSERT [dbo].[mau_sac] ([id], [ma_mau_sac], [ten_mau_sac], [ma_mau_hex], [trang_thai], [ngay_tao], [ngay_cap_nhat]) VALUES (15, N'MS015', N'Bạc', N'#C0C0C0', 1, CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2), CAST(N'2026-10-03T21:42:46.2933333' AS DateTime2))
SET IDENTITY_INSERT [dbo].[mau_sac] OFF
-- DATA: [dbo].[nhan_vien]
SET IDENTITY_INSERT [dbo].[nhan_vien] ON 

INSERT [dbo].[nhan_vien] ([id], [id_vai_tro], [ma_nhan_vien], [ten_dang_nhap], [ten_nhan_vien], [email], [mat_khau], [so_dien_thoai], [gioi_tinh], [ngay_sinh], [dia_chi], [tinh_thanh], [phuong_xa], [trang_thai], [ngay_tao], [hinh_anh], [ngay_cap_nhat]) VALUES (1, 1, N'NV001', N'nguyenvana', N'Nguyễn Văn A', N'nguyenvana@smashstep.local', NULL, N'0900000001', NULL, NULL, NULL, NULL, NULL, 1, CAST(N'2026-10-06T14:14:49.3578157' AS DateTime2), NULL, CAST(N'2026-10-06T14:14:49.3578157' AS DateTime2))
INSERT [dbo].[nhan_vien] ([id], [id_vai_tro], [ma_nhan_vien], [ten_dang_nhap], [ten_nhan_vien], [email], [mat_khau], [so_dien_thoai], [gioi_tinh], [ngay_sinh], [dia_chi], [tinh_thanh], [phuong_xa], [trang_thai], [ngay_tao], [hinh_anh], [ngay_cap_nhat]) VALUES (2, 1, N'NV002', N'khanhha', N'Khánh Hà', N'khanhha@smashstep.local', NULL, N'0900000002', NULL, NULL, NULL, NULL, NULL, 1, CAST(N'2026-10-06T14:14:49.3578157' AS DateTime2), NULL, CAST(N'2026-10-06T14:14:49.3578157' AS DateTime2))
INSERT [dbo].[nhan_vien] ([id], [id_vai_tro], [ma_nhan_vien], [ten_dang_nhap], [ten_nhan_vien], [email], [mat_khau], [so_dien_thoai], [gioi_tinh], [ngay_sinh], [dia_chi], [tinh_thanh], [phuong_xa], [trang_thai], [ngay_tao], [hinh_anh], [ngay_cap_nhat]) VALUES (3, 1, N'NV003', N'tranhuy', N'Trần Huy', N'tranhuy@smashstep.local', NULL, N'0900000003', NULL, NULL, NULL, NULL, NULL, 1, CAST(N'2026-10-06T14:14:49.3578157' AS DateTime2), NULL, CAST(N'2026-10-06T14:14:49.3578157' AS DateTime2))
INSERT [dbo].[nhan_vien] ([id], [id_vai_tro], [ma_nhan_vien], [ten_dang_nhap], [ten_nhan_vien], [email], [mat_khau], [so_dien_thoai], [gioi_tinh], [ngay_sinh], [dia_chi], [tinh_thanh], [phuong_xa], [trang_thai], [ngay_tao], [hinh_anh], [ngay_cap_nhat]) VALUES (6, 2, N'NV0004', N'demo.nhanvien.1', N'Nhân viên demo 1', N'demo.employee.1@smashstep.example', NULL, N'0979900001', 1, CAST(N'1996-05-15' AS Date), N'10 Nguyễn Trãi', N'Hà Nội', N'Phường Thanh Xuân', 1, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), NULL, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2))
INSERT [dbo].[nhan_vien] ([id], [id_vai_tro], [ma_nhan_vien], [ten_dang_nhap], [ten_nhan_vien], [email], [mat_khau], [so_dien_thoai], [gioi_tinh], [ngay_sinh], [dia_chi], [tinh_thanh], [phuong_xa], [trang_thai], [ngay_tao], [hinh_anh], [ngay_cap_nhat]) VALUES (7, 1, N'NV0005', N'demo.nhanvien.2', N'Nhân viên demo 2', N'demo.employee.2@smashstep.example', NULL, N'0979900002', 2, CAST(N'1997-05-15' AS Date), N'20 Nguyễn Trãi', N'Hà Nội', N'Phường Thanh Xuân', 1, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), NULL, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2))
INSERT [dbo].[nhan_vien] ([id], [id_vai_tro], [ma_nhan_vien], [ten_dang_nhap], [ten_nhan_vien], [email], [mat_khau], [so_dien_thoai], [gioi_tinh], [ngay_sinh], [dia_chi], [tinh_thanh], [phuong_xa], [trang_thai], [ngay_tao], [hinh_anh], [ngay_cap_nhat]) VALUES (8, 1, N'NV0006', N'demo.nhanvien.3', N'Nhân viên demo 3', N'demo.employee.3@smashstep.example', NULL, N'0979900003', 1, CAST(N'1998-05-15' AS Date), N'30 Nguyễn Trãi', N'Hà Nội', N'Phường Thanh Xuân', 1, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), NULL, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2))
INSERT [dbo].[nhan_vien] ([id], [id_vai_tro], [ma_nhan_vien], [ten_dang_nhap], [ten_nhan_vien], [email], [mat_khau], [so_dien_thoai], [gioi_tinh], [ngay_sinh], [dia_chi], [tinh_thanh], [phuong_xa], [trang_thai], [ngay_tao], [hinh_anh], [ngay_cap_nhat]) VALUES (11, 2, N'NV0011', N'NV0011', N'DEMO UI Nhân viên 20261007 - đã kiểm tra', N'stabilization.employee.20261007@smashstep.example', NULL, N'0998800002', 1, CAST(N'1995-05-15' AS Date), N'40 Đội Cấn - địa chỉ demo UI', N'Hà Nội', N'Phường Cầu Giấy', 1, CAST(N'2026-10-07T02:01:08.2531189' AS DateTime2), N'/uploads/avatars/c3505ce5-31cc-44b3-a144-10bdd1a1084c.png', CAST(N'2026-10-07T03:09:56.9314936' AS DateTime2))
SET IDENTITY_INSERT [dbo].[nhan_vien] OFF
-- DATA: [dbo].[phieu_giam_gia]
SET IDENTITY_INSERT [dbo].[phieu_giam_gia] ON 

INSERT [dbo].[phieu_giam_gia] ([id], [ma_phieu_giam_gia], [ten_phieu_giam_gia], [loai_giam_gia], [gia_tri_giam], [gia_tri_toi_thieu], [giam_toi_da], [ngay_bat_dau], [ngay_ket_thuc], [so_luong], [so_luong_da_dung], [trang_thai], [ngay_tao], [ngay_cap_nhat], [mo_ta], [vo_han]) VALUES (1, N'PGG001', N'DEMO - Phiếu công khai 10 phần trăm', 1, CAST(10.00 AS Decimal(18, 2)), CAST(300000.00 AS Decimal(18, 2)), CAST(100000.00 AS Decimal(18, 2)), CAST(N'2026-10-06T00:00:00.0000000' AS DateTime2), CAST(N'2026-11-06T23:59:59.0000000' AS DateTime2), 100, 0, 1, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), N'Dữ liệu demo tích hợp; giữ nguyên dữ liệu cũ', 0)
INSERT [dbo].[phieu_giam_gia] ([id], [ma_phieu_giam_gia], [ten_phieu_giam_gia], [loai_giam_gia], [gia_tri_giam], [gia_tri_toi_thieu], [giam_toi_da], [ngay_bat_dau], [ngay_ket_thuc], [so_luong], [so_luong_da_dung], [trang_thai], [ngay_tao], [ngay_cap_nhat], [mo_ta], [vo_han]) VALUES (2, N'PGG002', N'DEMO - Phiếu công khai 50 nghìn', 2, CAST(50000.00 AS Decimal(18, 2)), CAST(300000.00 AS Decimal(18, 2)), CAST(50000.00 AS Decimal(18, 2)), CAST(N'2026-10-06T00:00:00.0000000' AS DateTime2), CAST(N'2026-11-06T23:59:59.0000000' AS DateTime2), 100, 0, 1, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), N'Dữ liệu demo tích hợp; giữ nguyên dữ liệu cũ', 0)
INSERT [dbo].[phieu_giam_gia] ([id], [ma_phieu_giam_gia], [ten_phieu_giam_gia], [loai_giam_gia], [gia_tri_giam], [gia_tri_toi_thieu], [giam_toi_da], [ngay_bat_dau], [ngay_ket_thuc], [so_luong], [so_luong_da_dung], [trang_thai], [ngay_tao], [ngay_cap_nhat], [mo_ta], [vo_han]) VALUES (3, N'PGG003', N'DEMO - Phiếu cá nhân 20 phần trăm', 1, CAST(20.00 AS Decimal(18, 2)), CAST(300000.00 AS Decimal(18, 2)), CAST(100000.00 AS Decimal(18, 2)), CAST(N'2026-10-06T00:00:00.0000000' AS DateTime2), CAST(N'2026-11-06T23:59:59.0000000' AS DateTime2), 100, 0, 1, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), N'Dữ liệu demo tích hợp; giữ nguyên dữ liệu cũ', 0)
INSERT [dbo].[phieu_giam_gia] ([id], [ma_phieu_giam_gia], [ten_phieu_giam_gia], [loai_giam_gia], [gia_tri_giam], [gia_tri_toi_thieu], [giam_toi_da], [ngay_bat_dau], [ngay_ket_thuc], [so_luong], [so_luong_da_dung], [trang_thai], [ngay_tao], [ngay_cap_nhat], [mo_ta], [vo_han]) VALUES (4, N'PGG004', N'DEMO - Phiếu cá nhân 100 nghìn', 2, CAST(100000.00 AS Decimal(18, 2)), CAST(300000.00 AS Decimal(18, 2)), CAST(100000.00 AS Decimal(18, 2)), CAST(N'2026-10-06T00:00:00.0000000' AS DateTime2), CAST(N'2026-11-06T23:59:59.0000000' AS DateTime2), 100, 0, 1, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), N'Dữ liệu demo tích hợp; giữ nguyên dữ liệu cũ', 0)
INSERT [dbo].[phieu_giam_gia] ([id], [ma_phieu_giam_gia], [ten_phieu_giam_gia], [loai_giam_gia], [gia_tri_giam], [gia_tri_toi_thieu], [giam_toi_da], [ngay_bat_dau], [ngay_ket_thuc], [so_luong], [so_luong_da_dung], [trang_thai], [ngay_tao], [ngay_cap_nhat], [mo_ta], [vo_han]) VALUES (5, N'PGG005', N'DEMO - Phiếu sắp diễn ra 15 phần trăm', 1, CAST(15.00 AS Decimal(18, 2)), CAST(300000.00 AS Decimal(18, 2)), CAST(100000.00 AS Decimal(18, 2)), CAST(N'2026-10-14T00:00:00.0000000' AS DateTime2), CAST(N'2026-10-27T23:59:59.0000000' AS DateTime2), 100, 0, 1, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), N'Dữ liệu demo tích hợp; giữ nguyên dữ liệu cũ', 0)
INSERT [dbo].[phieu_giam_gia] ([id], [ma_phieu_giam_gia], [ten_phieu_giam_gia], [loai_giam_gia], [gia_tri_giam], [gia_tri_toi_thieu], [giam_toi_da], [ngay_bat_dau], [ngay_ket_thuc], [so_luong], [so_luong_da_dung], [trang_thai], [ngay_tao], [ngay_cap_nhat], [mo_ta], [vo_han]) VALUES (6, N'PGG006', N'DEMO - Phiếu đã kết thúc 5 phần trăm', 1, CAST(5.00 AS Decimal(18, 2)), CAST(300000.00 AS Decimal(18, 2)), CAST(100000.00 AS Decimal(18, 2)), CAST(N'2026-09-17T00:00:00.0000000' AS DateTime2), CAST(N'2026-10-05T23:59:59.0000000' AS DateTime2), 100, 0, 1, CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), CAST(N'2026-10-07T01:49:07.8364510' AS DateTime2), N'Dữ liệu demo tích hợp; giữ nguyên dữ liệu cũ', 0)
INSERT [dbo].[phieu_giam_gia] ([id], [ma_phieu_giam_gia], [ten_phieu_giam_gia], [loai_giam_gia], [gia_tri_giam], [gia_tri_toi_thieu], [giam_toi_da], [ngay_bat_dau], [ngay_ket_thuc], [so_luong], [so_luong_da_dung], [trang_thai], [ngay_tao], [ngay_cap_nhat], [mo_ta], [vo_han]) VALUES (10, N'VCHI480J8', N'DEMO UI 20261007 - Phiếu cá nhân đã kiểm tra', 1, CAST(5.00 AS Decimal(18, 2)), CAST(0.00 AS Decimal(18, 2)), CAST(50000.00 AS Decimal(18, 2)), CAST(N'2026-10-06T00:00:00.0000000' AS DateTime2), CAST(N'2026-11-07T23:59:59.0000000' AS DateTime2), 10, 0, 1, CAST(N'2026-10-07T02:10:14.4036308' AS DateTime2), CAST(N'2026-10-07T02:11:17.7549493' AS DateTime2), N'DEMO UI final stabilization 20261007', 0)
SET IDENTITY_INSERT [dbo].[phieu_giam_gia] OFF
-- DATA: [dbo].[phieu_giam_gia_khach_hang]
SET IDENTITY_INSERT [dbo].[phieu_giam_gia_khach_hang] ON 

INSERT [dbo].[phieu_giam_gia_khach_hang] ([id], [id_khach_hang], [id_phieu_giam_gia], [ngay_su_dung], [trang_thai]) VALUES (1, 1, 3, NULL, 1)
INSERT [dbo].[phieu_giam_gia_khach_hang] ([id], [id_khach_hang], [id_phieu_giam_gia], [ngay_su_dung], [trang_thai]) VALUES (2, 2, 3, NULL, 1)
INSERT [dbo].[phieu_giam_gia_khach_hang] ([id], [id_khach_hang], [id_phieu_giam_gia], [ngay_su_dung], [trang_thai]) VALUES (3, 1, 4, NULL, 1)
INSERT [dbo].[phieu_giam_gia_khach_hang] ([id], [id_khach_hang], [id_phieu_giam_gia], [ngay_su_dung], [trang_thai]) VALUES (4, 2, 4, NULL, 1)
INSERT [dbo].[phieu_giam_gia_khach_hang] ([id], [id_khach_hang], [id_phieu_giam_gia], [ngay_su_dung], [trang_thai]) VALUES (6, 17, 10, NULL, 1)
SET IDENTITY_INSERT [dbo].[phieu_giam_gia_khach_hang] OFF
-- DATA: [dbo].[phuong_thuc_thanh_toan]
SET IDENTITY_INSERT [dbo].[phuong_thuc_thanh_toan] ON 

INSERT [dbo].[phuong_thuc_thanh_toan] ([id], [id_hinh_thuc_thanh_toan], [ma_phuong_thuc], [ten_phuong_thuc], [trang_thai]) VALUES (1, 1, N'TIEN_MAT', N'Tiền mặt', 1)
INSERT [dbo].[phuong_thuc_thanh_toan] ([id], [id_hinh_thuc_thanh_toan], [ma_phuong_thuc], [ten_phuong_thuc], [trang_thai]) VALUES (2, 1, N'THE', N'Thẻ', 1)
INSERT [dbo].[phuong_thuc_thanh_toan] ([id], [id_hinh_thuc_thanh_toan], [ma_phuong_thuc], [ten_phuong_thuc], [trang_thai]) VALUES (3, 2, N'CHUYEN_KHOAN', N'Chuyển khoản', 1)
INSERT [dbo].[phuong_thuc_thanh_toan] ([id], [id_hinh_thuc_thanh_toan], [ma_phuong_thuc], [ten_phuong_thuc], [trang_thai]) VALUES (4, 2, N'COD', N'COD', 1)
SET IDENTITY_INSERT [dbo].[phuong_thuc_thanh_toan] OFF
-- DATA: [dbo].[san_pham]
SET IDENTITY_INSERT [dbo].[san_pham] ON 

INSERT [dbo].[san_pham] ([id], [id_danh_muc], [id_thuong_hieu], [id_chat_lieu], [id_kieu_dang], [id_co_giay], [id_xuat_xu], [ma_san_pham], [ten_san_pham], [mo_ta_chi_tiet], [ngay_tao], [nguoi_tao], [nguoi_cap_nhat], [ngay_cap_nhat], [trang_thai]) VALUES (1, 1, 1, 1, 1, 1, 1, N'SP001', N'Nike Air Max Running 2026', N'Giày Nike chạy bộ phiên bản cập nhật 2026', CAST(N'2026-10-03T21:44:00.0954595' AS DateTime2), NULL, NULL, CAST(N'2026-10-03T21:46:11.9647551' AS DateTime2), 1)
INSERT [dbo].[san_pham] ([id], [id_danh_muc], [id_thuong_hieu], [id_chat_lieu], [id_kieu_dang], [id_co_giay], [id_xuat_xu], [ma_san_pham], [ten_san_pham], [mo_ta_chi_tiet], [ngay_tao], [nguoi_tao], [nguoi_cap_nhat], [ngay_cap_nhat], [trang_thai]) VALUES (2, 8, 10, 8, 8, 3, 1, N'SP0002', N'giay 123', N'sp tét', CAST(N'2026-10-03T22:21:21.7679075' AS DateTime2), NULL, NULL, NULL, 1)
INSERT [dbo].[san_pham] ([id], [id_danh_muc], [id_thuong_hieu], [id_chat_lieu], [id_kieu_dang], [id_co_giay], [id_xuat_xu], [ma_san_pham], [ten_san_pham], [mo_ta_chi_tiet], [ngay_tao], [nguoi_tao], [nguoi_cap_nhat], [ngay_cap_nhat], [trang_thai]) VALUES (3, 4, 3, 3, 3, 1, 8, N'SP0004', N'Quang', N'', CAST(N'2026-10-03T23:33:55.0485766' AS DateTime2), NULL, NULL, NULL, 1)
INSERT [dbo].[san_pham] ([id], [id_danh_muc], [id_thuong_hieu], [id_chat_lieu], [id_kieu_dang], [id_co_giay], [id_xuat_xu], [ma_san_pham], [ten_san_pham], [mo_ta_chi_tiet], [ngay_tao], [nguoi_tao], [nguoi_cap_nhat], [ngay_cap_nhat], [trang_thai]) VALUES (4, 8, 5, 9, 10, 2, 7, N'SP005', N'test', N'ok', CAST(N'2026-10-04T00:31:58.8255698' AS DateTime2), NULL, NULL, CAST(N'2026-10-04T18:43:22.2209574' AS DateTime2), 0)
INSERT [dbo].[san_pham] ([id], [id_danh_muc], [id_thuong_hieu], [id_chat_lieu], [id_kieu_dang], [id_co_giay], [id_xuat_xu], [ma_san_pham], [ten_san_pham], [mo_ta_chi_tiet], [ngay_tao], [nguoi_tao], [nguoi_cap_nhat], [ngay_cap_nhat], [trang_thai]) VALUES (88, 8, 10, 10, 1, 3, 1, N'SP006', N'Test2', N'SP test', CAST(N'2026-10-04T19:04:54.0609518' AS DateTime2), NULL, NULL, NULL, 1)
SET IDENTITY_INSERT [dbo].[san_pham] OFF
-- DATA: [dbo].[san_pham_chi_tiet]
SET IDENTITY_INSERT [dbo].[san_pham_chi_tiet] ON 

INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (1, 1, 1, 10, N'SP001-DEN-40', 20, CAST(1599000.00 AS Decimal(18, 2)), N'NIKE-SP001-BLK-40', 1, CAST(N'2026-10-03T21:47:12.3180747' AS DateTime2), NULL, 1)
INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (2, 1, 2, 12, N'SP001-TRANG-41', 15, CAST(1699000.00 AS Decimal(18, 2)), N'NIKE-SP001-WHT-41', 1, CAST(N'2026-10-03T21:47:46.3689431' AS DateTime2), NULL, 1)
INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (3, 2, 15, 21, N'SP0002-15-21', 30000, CAST(123.00 AS Decimal(18, 2)), N'SP0002-15-21', 1, CAST(N'2026-10-03T22:23:59.9446101' AS DateTime2), NULL, 1)
INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (4, 2, 15, 20, N'SP0002-15-20', 30000, CAST(123.00 AS Decimal(18, 2)), N'SP0002-15-20', 1, CAST(N'2026-10-03T22:23:59.9705398' AS DateTime2), NULL, 1)
INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (5, 2, 10, 21, N'SP0002-10-21', 30000, CAST(123.00 AS Decimal(18, 2)), N'SP0002-10-21', 1, CAST(N'2026-10-03T22:23:59.9825092' AS DateTime2), NULL, 1)
INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (6, 2, 10, 20, N'SP0002-10-20', 30000, CAST(123.00 AS Decimal(18, 2)), N'SP0002-10-20', 1, CAST(N'2026-10-03T22:23:59.9894891' AS DateTime2), NULL, 1)
INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (7, 2, 3, 8, N'SP0002-3-8', 4, CAST(300000.00 AS Decimal(18, 2)), N'SP0002-3-8', 1, CAST(N'2026-10-03T22:44:09.5234285' AS DateTime2), NULL, 1)
INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (8, 2, 3, 17, N'SP0002-3-17', 4, CAST(300000.00 AS Decimal(18, 2)), N'SP0002-3-17', 1, CAST(N'2026-10-03T22:44:09.5563400' AS DateTime2), NULL, 1)
INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (9, 2, 3, 16, N'SP0002-3-16', 4, CAST(300000.00 AS Decimal(18, 2)), N'SP0002-3-16', 1, CAST(N'2026-10-03T22:44:09.5633225' AS DateTime2), NULL, 1)
INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (10, 2, 2, 8, N'SP0002-2-8', 4, CAST(300000.00 AS Decimal(18, 2)), N'SP0002-2-8', 1, CAST(N'2026-10-03T22:44:09.5760964' AS DateTime2), NULL, 1)
INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (11, 2, 2, 17, N'SP0002-2-17', 4, CAST(300000.00 AS Decimal(18, 2)), N'SP0002-2-17', 1, CAST(N'2026-10-03T22:44:09.5840759' AS DateTime2), NULL, 1)
INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (12, 2, 2, 16, N'SP0002-2-16', 4, CAST(300000.00 AS Decimal(18, 2)), N'SP0002-2-16', 1, CAST(N'2026-10-03T22:44:09.5950451' AS DateTime2), NULL, 1)
INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (13, 2, 1, 8, N'SP0002-1-8', 4, CAST(300000.00 AS Decimal(18, 2)), N'SP0002-1-8', 1, CAST(N'2026-10-03T22:44:09.6010308' AS DateTime2), NULL, 1)
INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (14, 2, 1, 17, N'SP0002-1-17', 4, CAST(300000.00 AS Decimal(18, 2)), N'SP0002-1-17', 1, CAST(N'2026-10-03T22:44:09.6060166' AS DateTime2), NULL, 1)
INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (15, 2, 1, 16, N'SP0002-1-16', 4, CAST(300000.00 AS Decimal(18, 2)), N'SP0002-1-16', 1, CAST(N'2026-10-03T22:44:09.6110037' AS DateTime2), NULL, 1)
INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (16, 3, 15, 21, N'SP0004-15-21', 4, CAST(100000.00 AS Decimal(18, 2)), N'SP0004-15-21', 1, CAST(N'2026-10-03T23:34:12.6528769' AS DateTime2), NULL, 1)
INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (17, 3, 15, 17, N'SP0004-15-17', 4, CAST(100000.00 AS Decimal(18, 2)), N'SP0004-15-17', 1, CAST(N'2026-10-03T23:34:12.6673231' AS DateTime2), NULL, 1)
INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (18, 3, 12, 21, N'SP0004-12-21', 4, CAST(100000.00 AS Decimal(18, 2)), N'SP0004-12-21', 1, CAST(N'2026-10-03T23:34:12.6713015' AS DateTime2), NULL, 1)
INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (19, 3, 12, 17, N'SP0004-12-17', 4, CAST(100000.00 AS Decimal(18, 2)), N'SP0004-12-17', 1, CAST(N'2026-10-03T23:34:12.6782808' AS DateTime2), NULL, 1)
INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (20, 4, 14, 4, N'SP005-14-4', 123, CAST(123.00 AS Decimal(18, 2)), N'SP005-14-4', 1, CAST(N'2026-10-04T00:32:40.5446268' AS DateTime2), NULL, 1)
INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (21, 4, 14, 5, N'SP005-14-5', 123, CAST(123.00 AS Decimal(18, 2)), N'SP005-14-5', 1, CAST(N'2026-10-04T00:32:40.5707167' AS DateTime2), NULL, 1)
INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (22, 4, 15, 4, N'SP005-15-4', 123, CAST(123.00 AS Decimal(18, 2)), N'SP005-15-4', 1, CAST(N'2026-10-04T00:32:40.5797283' AS DateTime2), NULL, 1)
INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (23, 4, 15, 5, N'SP005-15-5', 123, CAST(123.00 AS Decimal(18, 2)), N'SP005-15-5', 1, CAST(N'2026-10-04T00:32:40.5892536' AS DateTime2), CAST(N'2026-10-04T18:57:06.8382946' AS DateTime2), 1)
INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (81, 88, 3, 5, N'SP006-3-5', 3, CAST(109999.98 AS Decimal(18, 2)), N'SP006-3-5', 1, CAST(N'2026-10-04T19:05:43.6518691' AS DateTime2), NULL, 1)
INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (82, 88, 3, 4, N'SP006-3-4', 5, CAST(100000.00 AS Decimal(18, 2)), N'SP006-3-4', 1, CAST(N'2026-10-04T19:05:43.6650458' AS DateTime2), NULL, 1)
INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (83, 88, 2, 5, N'SP006-2-5', 3, CAST(100000.00 AS Decimal(18, 2)), N'SP006-2-5', 1, CAST(N'2026-10-04T19:05:43.6743000' AS DateTime2), NULL, 1)
INSERT [dbo].[san_pham_chi_tiet] ([id], [id_san_pham], [id_mau_sac], [id_kich_thuoc], [ma_chi_tiet_san_pham], [so_luong], [gia_ban], [sku], [kich_hoat], [ngay_tao], [ngay_cap_nhat], [trang_thai]) VALUES (84, 88, 2, 4, N'SP006-2-4', 5, CAST(100000.00 AS Decimal(18, 2)), N'SP006-2-4', 1, CAST(N'2026-10-04T19:05:43.6782900' AS DateTime2), CAST(N'2026-10-04T19:06:01.2868796' AS DateTime2), 1)
SET IDENTITY_INSERT [dbo].[san_pham_chi_tiet] OFF
-- DATA: [dbo].[thuong_hieu]
SET IDENTITY_INSERT [dbo].[thuong_hieu] ON 

INSERT [dbo].[thuong_hieu] ([id], [ma_thuong_hieu], [ten_thuong_hieu], [trang_thai]) VALUES (1, N'TH001', N'Nike', 1)
INSERT [dbo].[thuong_hieu] ([id], [ma_thuong_hieu], [ten_thuong_hieu], [trang_thai]) VALUES (2, N'TH002', N'Adidas', 1)
INSERT [dbo].[thuong_hieu] ([id], [ma_thuong_hieu], [ten_thuong_hieu], [trang_thai]) VALUES (3, N'TH003', N'Puma', 1)
INSERT [dbo].[thuong_hieu] ([id], [ma_thuong_hieu], [ten_thuong_hieu], [trang_thai]) VALUES (4, N'TH004', N'New Balance', 1)
INSERT [dbo].[thuong_hieu] ([id], [ma_thuong_hieu], [ten_thuong_hieu], [trang_thai]) VALUES (5, N'TH005', N'Converse', 1)
INSERT [dbo].[thuong_hieu] ([id], [ma_thuong_hieu], [ten_thuong_hieu], [trang_thai]) VALUES (6, N'TH006', N'Vans', 1)
INSERT [dbo].[thuong_hieu] ([id], [ma_thuong_hieu], [ten_thuong_hieu], [trang_thai]) VALUES (7, N'TH007', N'ASICS', 1)
INSERT [dbo].[thuong_hieu] ([id], [ma_thuong_hieu], [ten_thuong_hieu], [trang_thai]) VALUES (8, N'TH008', N'Under Armour', 1)
INSERT [dbo].[thuong_hieu] ([id], [ma_thuong_hieu], [ten_thuong_hieu], [trang_thai]) VALUES (9, N'TH009', N'Reebok', 1)
INSERT [dbo].[thuong_hieu] ([id], [ma_thuong_hieu], [ten_thuong_hieu], [trang_thai]) VALUES (10, N'TH010', N'Skechers', 1)
SET IDENTITY_INSERT [dbo].[thuong_hieu] OFF
-- DATA: [dbo].[vai_tro]
SET IDENTITY_INSERT [dbo].[vai_tro] ON 

INSERT [dbo].[vai_tro] ([id], [ten_vai_tro], [mo_ta], [trang_thai]) VALUES (1, N'Nhân viên', N'Nhân viên bán hàng', 1)
INSERT [dbo].[vai_tro] ([id], [ten_vai_tro], [mo_ta], [trang_thai]) VALUES (2, N'Quản lý', N'Vai trò quản lý cửa hàng', 1)
SET IDENTITY_INSERT [dbo].[vai_tro] OFF
-- DATA: [dbo].[xuat_xu]
SET IDENTITY_INSERT [dbo].[xuat_xu] ON 

INSERT [dbo].[xuat_xu] ([id], [ma_xuat_xu], [ten_xuat_xu], [trang_thai]) VALUES (1, N'XX001', N'Việt Nam', 1)
INSERT [dbo].[xuat_xu] ([id], [ma_xuat_xu], [ten_xuat_xu], [trang_thai]) VALUES (2, N'XX002', N'Indonesia', 1)
INSERT [dbo].[xuat_xu] ([id], [ma_xuat_xu], [ten_xuat_xu], [trang_thai]) VALUES (3, N'XX003', N'Trung Quốc', 1)
INSERT [dbo].[xuat_xu] ([id], [ma_xuat_xu], [ten_xuat_xu], [trang_thai]) VALUES (4, N'XX004', N'Mỹ', 1)
INSERT [dbo].[xuat_xu] ([id], [ma_xuat_xu], [ten_xuat_xu], [trang_thai]) VALUES (5, N'XX005', N'Nhật Bản', 1)
INSERT [dbo].[xuat_xu] ([id], [ma_xuat_xu], [ten_xuat_xu], [trang_thai]) VALUES (6, N'XX006', N'Hàn Quốc', 1)
INSERT [dbo].[xuat_xu] ([id], [ma_xuat_xu], [ten_xuat_xu], [trang_thai]) VALUES (7, N'XX007', N'Thái Lan', 1)
INSERT [dbo].[xuat_xu] ([id], [ma_xuat_xu], [ten_xuat_xu], [trang_thai]) VALUES (8, N'XX008', N'Đức', 1)
INSERT [dbo].[xuat_xu] ([id], [ma_xuat_xu], [ten_xuat_xu], [trang_thai]) VALUES (153, N'XX009', N'Hà Lan', 1)
SET IDENTITY_INSERT [dbo].[xuat_xu] OFF
-- 3. FOREIGN KEYS (after all data is loaded)
ALTER TABLE [dbo].[chi_tiet_dot_giam_gia]  WITH CHECK ADD  CONSTRAINT [FK_ctdgg_dot_giam_gia] FOREIGN KEY([id_dot_giam_gia])
REFERENCES [dbo].[dot_giam_gia] ([id])
ALTER TABLE [dbo].[chi_tiet_dot_giam_gia] CHECK CONSTRAINT [FK_ctdgg_dot_giam_gia]
ALTER TABLE [dbo].[chi_tiet_dot_giam_gia]  WITH CHECK ADD  CONSTRAINT [FK_ctdgg_spct] FOREIGN KEY([id_san_pham_chi_tiet])
REFERENCES [dbo].[san_pham_chi_tiet] ([id])
ALTER TABLE [dbo].[chi_tiet_dot_giam_gia] CHECK CONSTRAINT [FK_ctdgg_spct]
ALTER TABLE [dbo].[dia_chi_khach_hang]  WITH CHECK ADD  CONSTRAINT [FK_dia_chi_khach_hang] FOREIGN KEY([id_khach_hang])
REFERENCES [dbo].[khach_hang] ([id])
ALTER TABLE [dbo].[dia_chi_khach_hang] CHECK CONSTRAINT [FK_dia_chi_khach_hang]
ALTER TABLE [dbo].[hoa_don_chi_tiet]  WITH CHECK ADD  CONSTRAINT [FK_hdct_hoa_don] FOREIGN KEY([id_hoa_don])
REFERENCES [dbo].[hoa_don] ([id])
ALTER TABLE [dbo].[hoa_don_chi_tiet] CHECK CONSTRAINT [FK_hdct_hoa_don]
ALTER TABLE [dbo].[hoa_don_chi_tiet]  WITH CHECK ADD  CONSTRAINT [FK_hdct_spct] FOREIGN KEY([id_san_pham_chi_tiet])
REFERENCES [dbo].[san_pham_chi_tiet] ([id])
ALTER TABLE [dbo].[hoa_don_chi_tiet] CHECK CONSTRAINT [FK_hdct_spct]
ALTER TABLE [dbo].[hinh_anh_san_pham]  WITH CHECK ADD  CONSTRAINT [FK_hinh_anh_san_pham] FOREIGN KEY([id_san_pham])
REFERENCES [dbo].[san_pham] ([id])
ALTER TABLE [dbo].[hinh_anh_san_pham] CHECK CONSTRAINT [FK_hinh_anh_san_pham]
ALTER TABLE [dbo].[hoa_don]  WITH CHECK ADD  CONSTRAINT [FK_hoa_don_khach_hang] FOREIGN KEY([id_khach_hang])
REFERENCES [dbo].[khach_hang] ([id])
ALTER TABLE [dbo].[hoa_don] CHECK CONSTRAINT [FK_hoa_don_khach_hang]
ALTER TABLE [dbo].[hoa_don]  WITH CHECK ADD  CONSTRAINT [FK_hoa_don_nhan_vien] FOREIGN KEY([id_nhan_vien])
REFERENCES [dbo].[nhan_vien] ([id])
ALTER TABLE [dbo].[hoa_don] CHECK CONSTRAINT [FK_hoa_don_nhan_vien]
ALTER TABLE [dbo].[hoa_don]  WITH CHECK ADD  CONSTRAINT [FK_hoa_don_phieu_giam_gia] FOREIGN KEY([id_phieu_giam_gia])
REFERENCES [dbo].[phieu_giam_gia] ([id])
ALTER TABLE [dbo].[hoa_don] CHECK CONSTRAINT [FK_hoa_don_phieu_giam_gia]
ALTER TABLE [dbo].[hoa_don]  WITH CHECK ADD  CONSTRAINT [FK_hoa_don_phuong_thuc] FOREIGN KEY([id_phuong_thuc_thanh_toan])
REFERENCES [dbo].[phuong_thuc_thanh_toan] ([id])
ALTER TABLE [dbo].[hoa_don] CHECK CONSTRAINT [FK_hoa_don_phuong_thuc]
ALTER TABLE [dbo].[lich_su_hoa_don]  WITH CHECK ADD  CONSTRAINT [FK_lshd_hoa_don] FOREIGN KEY([id_hoa_don])
REFERENCES [dbo].[hoa_don] ([id])
ALTER TABLE [dbo].[lich_su_hoa_don] CHECK CONSTRAINT [FK_lshd_hoa_don]
ALTER TABLE [dbo].[lich_su_thanh_toan]  WITH CHECK ADD  CONSTRAINT [FK_lstt_hoa_don] FOREIGN KEY([id_hoa_don])
REFERENCES [dbo].[hoa_don] ([id])
ALTER TABLE [dbo].[lich_su_thanh_toan] CHECK CONSTRAINT [FK_lstt_hoa_don]
ALTER TABLE [dbo].[nhan_vien]  WITH CHECK ADD  CONSTRAINT [FK_nhan_vien_vai_tro] FOREIGN KEY([id_vai_tro])
REFERENCES [dbo].[vai_tro] ([id])
ALTER TABLE [dbo].[nhan_vien] CHECK CONSTRAINT [FK_nhan_vien_vai_tro]
ALTER TABLE [dbo].[phieu_giam_gia_khach_hang]  WITH CHECK ADD  CONSTRAINT [FK_pggkh_khach_hang] FOREIGN KEY([id_khach_hang])
REFERENCES [dbo].[khach_hang] ([id])
ALTER TABLE [dbo].[phieu_giam_gia_khach_hang] CHECK CONSTRAINT [FK_pggkh_khach_hang]
ALTER TABLE [dbo].[phieu_giam_gia_khach_hang]  WITH CHECK ADD  CONSTRAINT [FK_pggkh_phieu_giam_gia] FOREIGN KEY([id_phieu_giam_gia])
REFERENCES [dbo].[phieu_giam_gia] ([id])
ALTER TABLE [dbo].[phieu_giam_gia_khach_hang] CHECK CONSTRAINT [FK_pggkh_phieu_giam_gia]
ALTER TABLE [dbo].[phuong_thuc_thanh_toan]  WITH CHECK ADD  CONSTRAINT [FK_pttt_hinh_thuc] FOREIGN KEY([id_hinh_thuc_thanh_toan])
REFERENCES [dbo].[hinh_thuc_thanh_toan] ([id])
ALTER TABLE [dbo].[phuong_thuc_thanh_toan] CHECK CONSTRAINT [FK_pttt_hinh_thuc]
ALTER TABLE [dbo].[san_pham]  WITH CHECK ADD  CONSTRAINT [FK_san_pham_chat_lieu] FOREIGN KEY([id_chat_lieu])
REFERENCES [dbo].[chat_lieu] ([id])
ALTER TABLE [dbo].[san_pham] CHECK CONSTRAINT [FK_san_pham_chat_lieu]
ALTER TABLE [dbo].[san_pham]  WITH CHECK ADD  CONSTRAINT [FK_san_pham_co_giay] FOREIGN KEY([id_co_giay])
REFERENCES [dbo].[co_giay] ([id])
ALTER TABLE [dbo].[san_pham] CHECK CONSTRAINT [FK_san_pham_co_giay]
ALTER TABLE [dbo].[san_pham]  WITH CHECK ADD  CONSTRAINT [FK_san_pham_danh_muc] FOREIGN KEY([id_danh_muc])
REFERENCES [dbo].[danh_muc] ([id])
ALTER TABLE [dbo].[san_pham] CHECK CONSTRAINT [FK_san_pham_danh_muc]
ALTER TABLE [dbo].[san_pham]  WITH CHECK ADD  CONSTRAINT [FK_san_pham_kieu_dang] FOREIGN KEY([id_kieu_dang])
REFERENCES [dbo].[kieu_dang] ([id])
ALTER TABLE [dbo].[san_pham] CHECK CONSTRAINT [FK_san_pham_kieu_dang]
ALTER TABLE [dbo].[san_pham]  WITH CHECK ADD  CONSTRAINT [FK_san_pham_thuong_hieu] FOREIGN KEY([id_thuong_hieu])
REFERENCES [dbo].[thuong_hieu] ([id])
ALTER TABLE [dbo].[san_pham] CHECK CONSTRAINT [FK_san_pham_thuong_hieu]
ALTER TABLE [dbo].[san_pham]  WITH CHECK ADD  CONSTRAINT [FK_san_pham_xuat_xu] FOREIGN KEY([id_xuat_xu])
REFERENCES [dbo].[xuat_xu] ([id])
ALTER TABLE [dbo].[san_pham] CHECK CONSTRAINT [FK_san_pham_xuat_xu]
ALTER TABLE [dbo].[san_pham_chi_tiet]  WITH CHECK ADD  CONSTRAINT [FK_spct_kich_thuoc] FOREIGN KEY([id_kich_thuoc])
REFERENCES [dbo].[kich_thuoc] ([id])
ALTER TABLE [dbo].[san_pham_chi_tiet] CHECK CONSTRAINT [FK_spct_kich_thuoc]
ALTER TABLE [dbo].[san_pham_chi_tiet]  WITH CHECK ADD  CONSTRAINT [FK_spct_mau_sac] FOREIGN KEY([id_mau_sac])
REFERENCES [dbo].[mau_sac] ([id])
ALTER TABLE [dbo].[san_pham_chi_tiet] CHECK CONSTRAINT [FK_spct_mau_sac]
ALTER TABLE [dbo].[san_pham_chi_tiet]  WITH CHECK ADD  CONSTRAINT [FK_spct_san_pham] FOREIGN KEY([id_san_pham])
REFERENCES [dbo].[san_pham] ([id])
ALTER TABLE [dbo].[san_pham_chi_tiet] CHECK CONSTRAINT [FK_spct_san_pham]
-- 4. PRESERVE IDENTITY COUNTERS AND VERIFY EXACT ROW COUNTS
DBCC CHECKIDENT (N'[dbo].[chat_lieu]', RESEED, 528) WITH NO_INFOMSGS;
DBCC CHECKIDENT (N'[dbo].[chi_tiet_dot_giam_gia]', RESEED, 47) WITH NO_INFOMSGS;
DBCC CHECKIDENT (N'[dbo].[co_giay]', RESEED, 521) WITH NO_INFOMSGS;
DBCC CHECKIDENT (N'[dbo].[danh_muc]', RESEED, 526) WITH NO_INFOMSGS;
DBCC CHECKIDENT (N'[dbo].[dia_chi_khach_hang]', RESEED, 65) WITH NO_INFOMSGS;
DBCC CHECKIDENT (N'[dbo].[dot_giam_gia]', RESEED, 21) WITH NO_INFOMSGS;
DBCC CHECKIDENT (N'[dbo].[hinh_anh_san_pham]', RESEED, 190) WITH NO_INFOMSGS;
DBCC CHECKIDENT (N'[dbo].[hinh_thuc_thanh_toan]', RESEED, 2) WITH NO_INFOMSGS;
DBCC CHECKIDENT (N'[dbo].[hoa_don]', RESEED, 27) WITH NO_INFOMSGS;
DBCC CHECKIDENT (N'[dbo].[hoa_don_chi_tiet]', RESEED, 36) WITH NO_INFOMSGS;
DBCC CHECKIDENT (N'[dbo].[khach_hang]', RESEED, 81) WITH NO_INFOMSGS;
DBCC CHECKIDENT (N'[dbo].[kich_thuoc]', RESEED, 598) WITH NO_INFOMSGS;
DBCC CHECKIDENT (N'[dbo].[kieu_dang]', RESEED, 528) WITH NO_INFOMSGS;
DBCC CHECKIDENT (N'[dbo].[lich_su_hoa_don]', RESEED, 28) WITH NO_INFOMSGS;
DBCC CHECKIDENT (N'[dbo].[lich_su_thanh_toan]', RESEED, 22) WITH NO_INFOMSGS;
DBCC CHECKIDENT (N'[dbo].[mau_sac]', RESEED, 564) WITH NO_INFOMSGS;
DBCC CHECKIDENT (N'[dbo].[nhan_vien]', RESEED, 43) WITH NO_INFOMSGS;
DBCC CHECKIDENT (N'[dbo].[phieu_giam_gia]', RESEED, 43) WITH NO_INFOMSGS;
DBCC CHECKIDENT (N'[dbo].[phieu_giam_gia_khach_hang]', RESEED, 7) WITH NO_INFOMSGS;
DBCC CHECKIDENT (N'[dbo].[phuong_thuc_thanh_toan]', RESEED, 4) WITH NO_INFOMSGS;
DBCC CHECKIDENT (N'[dbo].[san_pham]', RESEED, 286) WITH NO_INFOMSGS;
DBCC CHECKIDENT (N'[dbo].[san_pham_chi_tiet]', RESEED, 237) WITH NO_INFOMSGS;
DBCC CHECKIDENT (N'[dbo].[thuong_hieu]', RESEED, 528) WITH NO_INFOMSGS;
DBCC CHECKIDENT (N'[dbo].[vai_tro]', RESEED, 2) WITH NO_INFOMSGS;
DBCC CHECKIDENT (N'[dbo].[xuat_xu]', RESEED, 527) WITH NO_INFOMSGS;
IF (SELECT COUNT_BIG(*) FROM [dbo].[chat_lieu]) <> 10
    THROW 51001, N'Row count mismatch: [dbo].[chat_lieu]', 1;
IF (SELECT COUNT_BIG(*) FROM [dbo].[chi_tiet_dot_giam_gia]) <> 10
    THROW 51001, N'Row count mismatch: [dbo].[chi_tiet_dot_giam_gia]', 1;
IF (SELECT COUNT_BIG(*) FROM [dbo].[co_giay]) <> 3
    THROW 51001, N'Row count mismatch: [dbo].[co_giay]', 1;
IF (SELECT COUNT_BIG(*) FROM [dbo].[danh_muc]) <> 8
    THROW 51001, N'Row count mismatch: [dbo].[danh_muc]', 1;
IF (SELECT COUNT_BIG(*) FROM [dbo].[dia_chi_khach_hang]) <> 11
    THROW 51001, N'Row count mismatch: [dbo].[dia_chi_khach_hang]', 1;
IF (SELECT COUNT_BIG(*) FROM [dbo].[dot_giam_gia]) <> 4
    THROW 51001, N'Row count mismatch: [dbo].[dot_giam_gia]', 1;
IF (SELECT COUNT_BIG(*) FROM [dbo].[hinh_anh_san_pham]) <> 4
    THROW 51001, N'Row count mismatch: [dbo].[hinh_anh_san_pham]', 1;
IF (SELECT COUNT_BIG(*) FROM [dbo].[hinh_thuc_thanh_toan]) <> 2
    THROW 51001, N'Row count mismatch: [dbo].[hinh_thuc_thanh_toan]', 1;
IF (SELECT COUNT_BIG(*) FROM [dbo].[hoa_don]) <> 10
    THROW 51001, N'Row count mismatch: [dbo].[hoa_don]', 1;
IF (SELECT COUNT_BIG(*) FROM [dbo].[hoa_don_chi_tiet]) <> 20
    THROW 51001, N'Row count mismatch: [dbo].[hoa_don_chi_tiet]', 1;
IF (SELECT COUNT_BIG(*) FROM [dbo].[khach_hang]) <> 11
    THROW 51001, N'Row count mismatch: [dbo].[khach_hang]', 1;
IF (SELECT COUNT_BIG(*) FROM [dbo].[kich_thuoc]) <> 21
    THROW 51001, N'Row count mismatch: [dbo].[kich_thuoc]', 1;
IF (SELECT COUNT_BIG(*) FROM [dbo].[kieu_dang]) <> 10
    THROW 51001, N'Row count mismatch: [dbo].[kieu_dang]', 1;
IF (SELECT COUNT_BIG(*) FROM [dbo].[lich_su_hoa_don]) <> 11
    THROW 51001, N'Row count mismatch: [dbo].[lich_su_hoa_don]', 1;
IF (SELECT COUNT_BIG(*) FROM [dbo].[lich_su_thanh_toan]) <> 5
    THROW 51001, N'Row count mismatch: [dbo].[lich_su_thanh_toan]', 1;
IF (SELECT COUNT_BIG(*) FROM [dbo].[mau_sac]) <> 15
    THROW 51001, N'Row count mismatch: [dbo].[mau_sac]', 1;
IF (SELECT COUNT_BIG(*) FROM [dbo].[nhan_vien]) <> 7
    THROW 51001, N'Row count mismatch: [dbo].[nhan_vien]', 1;
IF (SELECT COUNT_BIG(*) FROM [dbo].[phieu_giam_gia]) <> 7
    THROW 51001, N'Row count mismatch: [dbo].[phieu_giam_gia]', 1;
IF (SELECT COUNT_BIG(*) FROM [dbo].[phieu_giam_gia_khach_hang]) <> 5
    THROW 51001, N'Row count mismatch: [dbo].[phieu_giam_gia_khach_hang]', 1;
IF (SELECT COUNT_BIG(*) FROM [dbo].[phuong_thuc_thanh_toan]) <> 4
    THROW 51001, N'Row count mismatch: [dbo].[phuong_thuc_thanh_toan]', 1;
IF (SELECT COUNT_BIG(*) FROM [dbo].[san_pham]) <> 5
    THROW 51001, N'Row count mismatch: [dbo].[san_pham]', 1;
IF (SELECT COUNT_BIG(*) FROM [dbo].[san_pham_chi_tiet]) <> 27
    THROW 51001, N'Row count mismatch: [dbo].[san_pham_chi_tiet]', 1;
IF (SELECT COUNT_BIG(*) FROM [dbo].[thuong_hieu]) <> 10
    THROW 51001, N'Row count mismatch: [dbo].[thuong_hieu]', 1;
IF (SELECT COUNT_BIG(*) FROM [dbo].[vai_tro]) <> 2
    THROW 51001, N'Row count mismatch: [dbo].[vai_tro]', 1;
IF (SELECT COUNT_BIG(*) FROM [dbo].[xuat_xu]) <> 9
    THROW 51001, N'Row count mismatch: [dbo].[xuat_xu]', 1;
    COMMIT TRANSACTION;
    PRINT N'SmashStep: import thanh cong - 25 bang, 231 dong, 25 khoa ngoai.';
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
SET NOEXEC OFF;
GO
