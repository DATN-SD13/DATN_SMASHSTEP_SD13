/* SMASHSTEP SD-013: seed for Database/01_sqlSD13.sql.
   No schema changes, database reset or existing-row overwrite.
   New rows only; stock, IDs and existing invoice/payment/history data are preserved.
   Do not run on production without reviewing the sample data.
*/
USE [SmashStep];
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
IF COL_LENGTH(N'dbo.vai_tro',N'id') IS NULL THROW 51001, N'Canonical schema mismatch: vai_tro.id. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.vai_tro',N'ten_vai_tro') IS NULL THROW 51001, N'Canonical schema mismatch: vai_tro.ten_vai_tro. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.vai_tro',N'mo_ta') IS NULL THROW 51001, N'Canonical schema mismatch: vai_tro.mo_ta. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.vai_tro',N'trang_thai') IS NULL THROW 51001, N'Canonical schema mismatch: vai_tro.trang_thai. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.nhan_vien',N'id') IS NULL THROW 51001, N'Canonical schema mismatch: nhan_vien.id. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.nhan_vien',N'id_vai_tro') IS NULL THROW 51001, N'Canonical schema mismatch: nhan_vien.id_vai_tro. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.nhan_vien',N'ma_nhan_vien') IS NULL THROW 51001, N'Canonical schema mismatch: nhan_vien.ma_nhan_vien. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.nhan_vien',N'ten_dang_nhap') IS NULL THROW 51001, N'Canonical schema mismatch: nhan_vien.ten_dang_nhap. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.nhan_vien',N'ten_nhan_vien') IS NULL THROW 51001, N'Canonical schema mismatch: nhan_vien.ten_nhan_vien. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.nhan_vien',N'email') IS NULL THROW 51001, N'Canonical schema mismatch: nhan_vien.email. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.nhan_vien',N'mat_khau') IS NULL THROW 51001, N'Canonical schema mismatch: nhan_vien.mat_khau. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.nhan_vien',N'so_dien_thoai') IS NULL THROW 51001, N'Canonical schema mismatch: nhan_vien.so_dien_thoai. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.nhan_vien',N'gioi_tinh') IS NULL THROW 51001, N'Canonical schema mismatch: nhan_vien.gioi_tinh. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.nhan_vien',N'ngay_sinh') IS NULL THROW 51001, N'Canonical schema mismatch: nhan_vien.ngay_sinh. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.nhan_vien',N'dia_chi') IS NULL THROW 51001, N'Canonical schema mismatch: nhan_vien.dia_chi. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.nhan_vien',N'tinh_thanh') IS NULL THROW 51001, N'Canonical schema mismatch: nhan_vien.tinh_thanh. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.nhan_vien',N'phuong_xa') IS NULL THROW 51001, N'Canonical schema mismatch: nhan_vien.phuong_xa. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.nhan_vien',N'trang_thai') IS NULL THROW 51001, N'Canonical schema mismatch: nhan_vien.trang_thai. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.nhan_vien',N'ngay_tao') IS NULL THROW 51001, N'Canonical schema mismatch: nhan_vien.ngay_tao. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.nhan_vien',N'hinh_anh') IS NULL THROW 51001, N'Canonical schema mismatch: nhan_vien.hinh_anh. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.nhan_vien',N'ngay_cap_nhat') IS NULL THROW 51001, N'Canonical schema mismatch: nhan_vien.ngay_cap_nhat. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.khach_hang',N'id') IS NULL THROW 51001, N'Canonical schema mismatch: khach_hang.id. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.khach_hang',N'ma_khach_hang') IS NULL THROW 51001, N'Canonical schema mismatch: khach_hang.ma_khach_hang. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.khach_hang',N'ten_tai_khoan') IS NULL THROW 51001, N'Canonical schema mismatch: khach_hang.ten_tai_khoan. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.khach_hang',N'ten_khach_hang') IS NULL THROW 51001, N'Canonical schema mismatch: khach_hang.ten_khach_hang. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.khach_hang',N'email') IS NULL THROW 51001, N'Canonical schema mismatch: khach_hang.email. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.khach_hang',N'so_dien_thoai') IS NULL THROW 51001, N'Canonical schema mismatch: khach_hang.so_dien_thoai. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.khach_hang',N'ngay_sinh') IS NULL THROW 51001, N'Canonical schema mismatch: khach_hang.ngay_sinh. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.khach_hang',N'gioi_tinh') IS NULL THROW 51001, N'Canonical schema mismatch: khach_hang.gioi_tinh. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.khach_hang',N'mat_khau') IS NULL THROW 51001, N'Canonical schema mismatch: khach_hang.mat_khau. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.khach_hang',N'hinh_anh') IS NULL THROW 51001, N'Canonical schema mismatch: khach_hang.hinh_anh. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.khach_hang',N'trang_thai') IS NULL THROW 51001, N'Canonical schema mismatch: khach_hang.trang_thai. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.khach_hang',N'ngay_tao') IS NULL THROW 51001, N'Canonical schema mismatch: khach_hang.ngay_tao. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.khach_hang',N'ngay_cap_nhat') IS NULL THROW 51001, N'Canonical schema mismatch: khach_hang.ngay_cap_nhat. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.dia_chi_khach_hang',N'id') IS NULL THROW 51001, N'Canonical schema mismatch: dia_chi_khach_hang.id. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.dia_chi_khach_hang',N'id_khach_hang') IS NULL THROW 51001, N'Canonical schema mismatch: dia_chi_khach_hang.id_khach_hang. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.dia_chi_khach_hang',N'ten_nguoi_nhan') IS NULL THROW 51001, N'Canonical schema mismatch: dia_chi_khach_hang.ten_nguoi_nhan. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.dia_chi_khach_hang',N'sdt_nguoi_nhan') IS NULL THROW 51001, N'Canonical schema mismatch: dia_chi_khach_hang.sdt_nguoi_nhan. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.dia_chi_khach_hang',N'tinh_thanh') IS NULL THROW 51001, N'Canonical schema mismatch: dia_chi_khach_hang.tinh_thanh. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.dia_chi_khach_hang',N'quan_huyen') IS NULL THROW 51001, N'Canonical schema mismatch: dia_chi_khach_hang.quan_huyen. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.dia_chi_khach_hang',N'phuong_xa') IS NULL THROW 51001, N'Canonical schema mismatch: dia_chi_khach_hang.phuong_xa. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.dia_chi_khach_hang',N'dia_chi_cu_the') IS NULL THROW 51001, N'Canonical schema mismatch: dia_chi_khach_hang.dia_chi_cu_the. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.dia_chi_khach_hang',N'loai_dia_chi') IS NULL THROW 51001, N'Canonical schema mismatch: dia_chi_khach_hang.loai_dia_chi. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.dia_chi_khach_hang',N'is_mac_dinh') IS NULL THROW 51001, N'Canonical schema mismatch: dia_chi_khach_hang.is_mac_dinh. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.dia_chi_khach_hang',N'trang_thai') IS NULL THROW 51001, N'Canonical schema mismatch: dia_chi_khach_hang.trang_thai. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.danh_muc',N'id') IS NULL THROW 51001, N'Canonical schema mismatch: danh_muc.id. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.danh_muc',N'ma_danh_muc') IS NULL THROW 51001, N'Canonical schema mismatch: danh_muc.ma_danh_muc. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.danh_muc',N'ten_danh_muc') IS NULL THROW 51001, N'Canonical schema mismatch: danh_muc.ten_danh_muc. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.danh_muc',N'mo_ta') IS NULL THROW 51001, N'Canonical schema mismatch: danh_muc.mo_ta. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.danh_muc',N'trang_thai') IS NULL THROW 51001, N'Canonical schema mismatch: danh_muc.trang_thai. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.thuong_hieu',N'id') IS NULL THROW 51001, N'Canonical schema mismatch: thuong_hieu.id. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.thuong_hieu',N'ma_thuong_hieu') IS NULL THROW 51001, N'Canonical schema mismatch: thuong_hieu.ma_thuong_hieu. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.thuong_hieu',N'ten_thuong_hieu') IS NULL THROW 51001, N'Canonical schema mismatch: thuong_hieu.ten_thuong_hieu. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.thuong_hieu',N'trang_thai') IS NULL THROW 51001, N'Canonical schema mismatch: thuong_hieu.trang_thai. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.chat_lieu',N'id') IS NULL THROW 51001, N'Canonical schema mismatch: chat_lieu.id. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.chat_lieu',N'ma_chat_lieu') IS NULL THROW 51001, N'Canonical schema mismatch: chat_lieu.ma_chat_lieu. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.chat_lieu',N'ten_chat_lieu') IS NULL THROW 51001, N'Canonical schema mismatch: chat_lieu.ten_chat_lieu. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.chat_lieu',N'trang_thai') IS NULL THROW 51001, N'Canonical schema mismatch: chat_lieu.trang_thai. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.xuat_xu',N'id') IS NULL THROW 51001, N'Canonical schema mismatch: xuat_xu.id. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.xuat_xu',N'ma_xuat_xu') IS NULL THROW 51001, N'Canonical schema mismatch: xuat_xu.ma_xuat_xu. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.xuat_xu',N'ten_xuat_xu') IS NULL THROW 51001, N'Canonical schema mismatch: xuat_xu.ten_xuat_xu. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.xuat_xu',N'trang_thai') IS NULL THROW 51001, N'Canonical schema mismatch: xuat_xu.trang_thai. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.co_giay',N'id') IS NULL THROW 51001, N'Canonical schema mismatch: co_giay.id. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.co_giay',N'ma_co_giay') IS NULL THROW 51001, N'Canonical schema mismatch: co_giay.ma_co_giay. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.co_giay',N'ten_co_giay') IS NULL THROW 51001, N'Canonical schema mismatch: co_giay.ten_co_giay. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.co_giay',N'trang_thai') IS NULL THROW 51001, N'Canonical schema mismatch: co_giay.trang_thai. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.kieu_dang',N'id') IS NULL THROW 51001, N'Canonical schema mismatch: kieu_dang.id. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.kieu_dang',N'ma_kieu_dang') IS NULL THROW 51001, N'Canonical schema mismatch: kieu_dang.ma_kieu_dang. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.kieu_dang',N'ten_kieu_dang') IS NULL THROW 51001, N'Canonical schema mismatch: kieu_dang.ten_kieu_dang. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.kieu_dang',N'trang_thai') IS NULL THROW 51001, N'Canonical schema mismatch: kieu_dang.trang_thai. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.mau_sac',N'id') IS NULL THROW 51001, N'Canonical schema mismatch: mau_sac.id. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.mau_sac',N'ma_mau_sac') IS NULL THROW 51001, N'Canonical schema mismatch: mau_sac.ma_mau_sac. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.mau_sac',N'ten_mau_sac') IS NULL THROW 51001, N'Canonical schema mismatch: mau_sac.ten_mau_sac. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.mau_sac',N'ma_mau_hex') IS NULL THROW 51001, N'Canonical schema mismatch: mau_sac.ma_mau_hex. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.mau_sac',N'trang_thai') IS NULL THROW 51001, N'Canonical schema mismatch: mau_sac.trang_thai. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.mau_sac',N'ngay_tao') IS NULL THROW 51001, N'Canonical schema mismatch: mau_sac.ngay_tao. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.mau_sac',N'ngay_cap_nhat') IS NULL THROW 51001, N'Canonical schema mismatch: mau_sac.ngay_cap_nhat. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.kich_thuoc',N'id') IS NULL THROW 51001, N'Canonical schema mismatch: kich_thuoc.id. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.kich_thuoc',N'gia_tri') IS NULL THROW 51001, N'Canonical schema mismatch: kich_thuoc.gia_tri. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.kich_thuoc',N'ghi_chu') IS NULL THROW 51001, N'Canonical schema mismatch: kich_thuoc.ghi_chu. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.kich_thuoc',N'trang_thai') IS NULL THROW 51001, N'Canonical schema mismatch: kich_thuoc.trang_thai. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.kich_thuoc',N'ngay_tao') IS NULL THROW 51001, N'Canonical schema mismatch: kich_thuoc.ngay_tao. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.kich_thuoc',N'ngay_cap_nhat') IS NULL THROW 51001, N'Canonical schema mismatch: kich_thuoc.ngay_cap_nhat. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.san_pham',N'id') IS NULL THROW 51001, N'Canonical schema mismatch: san_pham.id. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.san_pham',N'id_danh_muc') IS NULL THROW 51001, N'Canonical schema mismatch: san_pham.id_danh_muc. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.san_pham',N'id_thuong_hieu') IS NULL THROW 51001, N'Canonical schema mismatch: san_pham.id_thuong_hieu. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.san_pham',N'id_chat_lieu') IS NULL THROW 51001, N'Canonical schema mismatch: san_pham.id_chat_lieu. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.san_pham',N'id_kieu_dang') IS NULL THROW 51001, N'Canonical schema mismatch: san_pham.id_kieu_dang. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.san_pham',N'id_co_giay') IS NULL THROW 51001, N'Canonical schema mismatch: san_pham.id_co_giay. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.san_pham',N'id_xuat_xu') IS NULL THROW 51001, N'Canonical schema mismatch: san_pham.id_xuat_xu. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.san_pham',N'ma_san_pham') IS NULL THROW 51001, N'Canonical schema mismatch: san_pham.ma_san_pham. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.san_pham',N'ten_san_pham') IS NULL THROW 51001, N'Canonical schema mismatch: san_pham.ten_san_pham. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.san_pham',N'mo_ta_chi_tiet') IS NULL THROW 51001, N'Canonical schema mismatch: san_pham.mo_ta_chi_tiet. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.san_pham',N'ngay_tao') IS NULL THROW 51001, N'Canonical schema mismatch: san_pham.ngay_tao. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.san_pham',N'nguoi_tao') IS NULL THROW 51001, N'Canonical schema mismatch: san_pham.nguoi_tao. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.san_pham',N'nguoi_cap_nhat') IS NULL THROW 51001, N'Canonical schema mismatch: san_pham.nguoi_cap_nhat. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.san_pham',N'ngay_cap_nhat') IS NULL THROW 51001, N'Canonical schema mismatch: san_pham.ngay_cap_nhat. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.san_pham',N'trang_thai') IS NULL THROW 51001, N'Canonical schema mismatch: san_pham.trang_thai. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.san_pham_chi_tiet',N'id') IS NULL THROW 51001, N'Canonical schema mismatch: san_pham_chi_tiet.id. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.san_pham_chi_tiet',N'id_san_pham') IS NULL THROW 51001, N'Canonical schema mismatch: san_pham_chi_tiet.id_san_pham. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.san_pham_chi_tiet',N'id_mau_sac') IS NULL THROW 51001, N'Canonical schema mismatch: san_pham_chi_tiet.id_mau_sac. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.san_pham_chi_tiet',N'id_kich_thuoc') IS NULL THROW 51001, N'Canonical schema mismatch: san_pham_chi_tiet.id_kich_thuoc. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.san_pham_chi_tiet',N'ma_chi_tiet_san_pham') IS NULL THROW 51001, N'Canonical schema mismatch: san_pham_chi_tiet.ma_chi_tiet_san_pham. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.san_pham_chi_tiet',N'so_luong') IS NULL THROW 51001, N'Canonical schema mismatch: san_pham_chi_tiet.so_luong. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.san_pham_chi_tiet',N'gia_ban') IS NULL THROW 51001, N'Canonical schema mismatch: san_pham_chi_tiet.gia_ban. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.san_pham_chi_tiet',N'sku') IS NULL THROW 51001, N'Canonical schema mismatch: san_pham_chi_tiet.sku. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.san_pham_chi_tiet',N'kich_hoat') IS NULL THROW 51001, N'Canonical schema mismatch: san_pham_chi_tiet.kich_hoat. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.san_pham_chi_tiet',N'ngay_tao') IS NULL THROW 51001, N'Canonical schema mismatch: san_pham_chi_tiet.ngay_tao. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.san_pham_chi_tiet',N'ngay_cap_nhat') IS NULL THROW 51001, N'Canonical schema mismatch: san_pham_chi_tiet.ngay_cap_nhat. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.san_pham_chi_tiet',N'trang_thai') IS NULL THROW 51001, N'Canonical schema mismatch: san_pham_chi_tiet.trang_thai. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.hinh_anh_san_pham',N'id') IS NULL THROW 51001, N'Canonical schema mismatch: hinh_anh_san_pham.id. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.hinh_anh_san_pham',N'id_san_pham') IS NULL THROW 51001, N'Canonical schema mismatch: hinh_anh_san_pham.id_san_pham. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.hinh_anh_san_pham',N'id_san_pham_chi_tiet') IS NULL THROW 51001, N'Canonical schema mismatch: hinh_anh_san_pham.id_san_pham_chi_tiet. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.hinh_anh_san_pham',N'id_mau_sac') IS NULL THROW 51001, N'Canonical schema mismatch: hinh_anh_san_pham.id_mau_sac. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.hinh_anh_san_pham',N'url_anh') IS NULL THROW 51001, N'Canonical schema mismatch: hinh_anh_san_pham.url_anh. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.hinh_anh_san_pham',N'is_anh_chinh') IS NULL THROW 51001, N'Canonical schema mismatch: hinh_anh_san_pham.is_anh_chinh. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.phieu_giam_gia',N'id') IS NULL THROW 51001, N'Canonical schema mismatch: phieu_giam_gia.id. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.phieu_giam_gia',N'ma_phieu_giam_gia') IS NULL THROW 51001, N'Canonical schema mismatch: phieu_giam_gia.ma_phieu_giam_gia. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.phieu_giam_gia',N'ten_phieu_giam_gia') IS NULL THROW 51001, N'Canonical schema mismatch: phieu_giam_gia.ten_phieu_giam_gia. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.phieu_giam_gia',N'hinh_thuc_phieu') IS NULL THROW 51001, N'Canonical schema mismatch: phieu_giam_gia.hinh_thuc_phieu. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.phieu_giam_gia',N'loai_giam_gia') IS NULL THROW 51001, N'Canonical schema mismatch: phieu_giam_gia.loai_giam_gia. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.phieu_giam_gia',N'gia_tri_giam') IS NULL THROW 51001, N'Canonical schema mismatch: phieu_giam_gia.gia_tri_giam. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.phieu_giam_gia',N'gia_tri_toi_thieu') IS NULL THROW 51001, N'Canonical schema mismatch: phieu_giam_gia.gia_tri_toi_thieu. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.phieu_giam_gia',N'giam_toi_da') IS NULL THROW 51001, N'Canonical schema mismatch: phieu_giam_gia.giam_toi_da. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.phieu_giam_gia',N'ngay_bat_dau') IS NULL THROW 51001, N'Canonical schema mismatch: phieu_giam_gia.ngay_bat_dau. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.phieu_giam_gia',N'ngay_ket_thuc') IS NULL THROW 51001, N'Canonical schema mismatch: phieu_giam_gia.ngay_ket_thuc. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.phieu_giam_gia',N'so_luong') IS NULL THROW 51001, N'Canonical schema mismatch: phieu_giam_gia.so_luong. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.phieu_giam_gia',N'so_luong_da_dung') IS NULL THROW 51001, N'Canonical schema mismatch: phieu_giam_gia.so_luong_da_dung. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.phieu_giam_gia',N'trang_thai') IS NULL THROW 51001, N'Canonical schema mismatch: phieu_giam_gia.trang_thai. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.phieu_giam_gia',N'ngay_tao') IS NULL THROW 51001, N'Canonical schema mismatch: phieu_giam_gia.ngay_tao. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.phieu_giam_gia',N'ngay_cap_nhat') IS NULL THROW 51001, N'Canonical schema mismatch: phieu_giam_gia.ngay_cap_nhat. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.phieu_giam_gia',N'mo_ta') IS NULL THROW 51001, N'Canonical schema mismatch: phieu_giam_gia.mo_ta. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.phieu_giam_gia_khach_hang',N'id') IS NULL THROW 51001, N'Canonical schema mismatch: phieu_giam_gia_khach_hang.id. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.phieu_giam_gia_khach_hang',N'id_khach_hang') IS NULL THROW 51001, N'Canonical schema mismatch: phieu_giam_gia_khach_hang.id_khach_hang. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.phieu_giam_gia_khach_hang',N'id_phieu_giam_gia') IS NULL THROW 51001, N'Canonical schema mismatch: phieu_giam_gia_khach_hang.id_phieu_giam_gia. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.phieu_giam_gia_khach_hang',N'ngay_su_dung') IS NULL THROW 51001, N'Canonical schema mismatch: phieu_giam_gia_khach_hang.ngay_su_dung. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.phieu_giam_gia_khach_hang',N'trang_thai') IS NULL THROW 51001, N'Canonical schema mismatch: phieu_giam_gia_khach_hang.trang_thai. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.dot_giam_gia',N'id') IS NULL THROW 51001, N'Canonical schema mismatch: dot_giam_gia.id. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.dot_giam_gia',N'ma_dot_giam_gia') IS NULL THROW 51001, N'Canonical schema mismatch: dot_giam_gia.ma_dot_giam_gia. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.dot_giam_gia',N'ten_dot_giam_gia') IS NULL THROW 51001, N'Canonical schema mismatch: dot_giam_gia.ten_dot_giam_gia. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.dot_giam_gia',N'phan_tram_giam_dot') IS NULL THROW 51001, N'Canonical schema mismatch: dot_giam_gia.phan_tram_giam_dot. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.dot_giam_gia',N'ngay_bat_dau') IS NULL THROW 51001, N'Canonical schema mismatch: dot_giam_gia.ngay_bat_dau. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.dot_giam_gia',N'ngay_ket_thuc') IS NULL THROW 51001, N'Canonical schema mismatch: dot_giam_gia.ngay_ket_thuc. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.dot_giam_gia',N'kich_hoat') IS NULL THROW 51001, N'Canonical schema mismatch: dot_giam_gia.kich_hoat. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.dot_giam_gia',N'ngay_tao') IS NULL THROW 51001, N'Canonical schema mismatch: dot_giam_gia.ngay_tao. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.dot_giam_gia',N'ngay_cap_nhat') IS NULL THROW 51001, N'Canonical schema mismatch: dot_giam_gia.ngay_cap_nhat. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.dot_giam_gia',N'trang_thai') IS NULL THROW 51001, N'Canonical schema mismatch: dot_giam_gia.trang_thai. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.dot_giam_gia',N'mo_ta') IS NULL THROW 51001, N'Canonical schema mismatch: dot_giam_gia.mo_ta. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.chi_tiet_dot_giam_gia',N'id') IS NULL THROW 51001, N'Canonical schema mismatch: chi_tiet_dot_giam_gia.id. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.chi_tiet_dot_giam_gia',N'id_dot_giam_gia') IS NULL THROW 51001, N'Canonical schema mismatch: chi_tiet_dot_giam_gia.id_dot_giam_gia. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.chi_tiet_dot_giam_gia',N'id_san_pham_chi_tiet') IS NULL THROW 51001, N'Canonical schema mismatch: chi_tiet_dot_giam_gia.id_san_pham_chi_tiet. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.chi_tiet_dot_giam_gia',N'phan_tram_giam_bien_the') IS NULL THROW 51001, N'Canonical schema mismatch: chi_tiet_dot_giam_gia.phan_tram_giam_bien_the. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.chi_tiet_dot_giam_gia',N'trang_thai') IS NULL THROW 51001, N'Canonical schema mismatch: chi_tiet_dot_giam_gia.trang_thai. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.chi_tiet_dot_giam_gia',N'ngay_tao') IS NULL THROW 51001, N'Canonical schema mismatch: chi_tiet_dot_giam_gia.ngay_tao. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.hinh_thuc_thanh_toan',N'id') IS NULL THROW 51001, N'Canonical schema mismatch: hinh_thuc_thanh_toan.id. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.hinh_thuc_thanh_toan',N'ma_hinh_thuc') IS NULL THROW 51001, N'Canonical schema mismatch: hinh_thuc_thanh_toan.ma_hinh_thuc. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.hinh_thuc_thanh_toan',N'ten_hinh_thuc') IS NULL THROW 51001, N'Canonical schema mismatch: hinh_thuc_thanh_toan.ten_hinh_thuc. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.hinh_thuc_thanh_toan',N'trang_thai') IS NULL THROW 51001, N'Canonical schema mismatch: hinh_thuc_thanh_toan.trang_thai. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.phuong_thuc_thanh_toan',N'id') IS NULL THROW 51001, N'Canonical schema mismatch: phuong_thuc_thanh_toan.id. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.phuong_thuc_thanh_toan',N'id_hinh_thuc_thanh_toan') IS NULL THROW 51001, N'Canonical schema mismatch: phuong_thuc_thanh_toan.id_hinh_thuc_thanh_toan. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.phuong_thuc_thanh_toan',N'ma_phuong_thuc') IS NULL THROW 51001, N'Canonical schema mismatch: phuong_thuc_thanh_toan.ma_phuong_thuc. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.phuong_thuc_thanh_toan',N'ten_phuong_thuc') IS NULL THROW 51001, N'Canonical schema mismatch: phuong_thuc_thanh_toan.ten_phuong_thuc. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.phuong_thuc_thanh_toan',N'trang_thai') IS NULL THROW 51001, N'Canonical schema mismatch: phuong_thuc_thanh_toan.trang_thai. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.hoa_don',N'id') IS NULL THROW 51001, N'Canonical schema mismatch: hoa_don.id. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.hoa_don',N'id_khach_hang') IS NULL THROW 51001, N'Canonical schema mismatch: hoa_don.id_khach_hang. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.hoa_don',N'id_nhan_vien') IS NULL THROW 51001, N'Canonical schema mismatch: hoa_don.id_nhan_vien. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.hoa_don',N'id_phieu_giam_gia') IS NULL THROW 51001, N'Canonical schema mismatch: hoa_don.id_phieu_giam_gia. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.hoa_don',N'id_phuong_thuc_thanh_toan') IS NULL THROW 51001, N'Canonical schema mismatch: hoa_don.id_phuong_thuc_thanh_toan. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.hoa_don',N'ma_hoa_don') IS NULL THROW 51001, N'Canonical schema mismatch: hoa_don.ma_hoa_don. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.hoa_don',N'loai_hoa_don') IS NULL THROW 51001, N'Canonical schema mismatch: hoa_don.loai_hoa_don. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.hoa_don',N'tong_tien') IS NULL THROW 51001, N'Canonical schema mismatch: hoa_don.tong_tien. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.hoa_don',N'phi_van_chuyen') IS NULL THROW 51001, N'Canonical schema mismatch: hoa_don.phi_van_chuyen. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.hoa_don',N'tien_giam_gia') IS NULL THROW 51001, N'Canonical schema mismatch: hoa_don.tien_giam_gia. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.hoa_don',N'thanh_tien') IS NULL THROW 51001, N'Canonical schema mismatch: hoa_don.thanh_tien. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.hoa_don',N'don_vi_van_chuyen') IS NULL THROW 51001, N'Canonical schema mismatch: hoa_don.don_vi_van_chuyen. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.hoa_don',N'ho_ten_nguoi_nhan') IS NULL THROW 51001, N'Canonical schema mismatch: hoa_don.ho_ten_nguoi_nhan. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.hoa_don',N'so_dien_thoai_nguoi_nhan') IS NULL THROW 51001, N'Canonical schema mismatch: hoa_don.so_dien_thoai_nguoi_nhan. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.hoa_don',N'dia_chi_giao_hang') IS NULL THROW 51001, N'Canonical schema mismatch: hoa_don.dia_chi_giao_hang. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.hoa_don',N'ghi_chu') IS NULL THROW 51001, N'Canonical schema mismatch: hoa_don.ghi_chu. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.hoa_don',N'ngay_thanh_toan') IS NULL THROW 51001, N'Canonical schema mismatch: hoa_don.ngay_thanh_toan. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.hoa_don',N'ngay_tao') IS NULL THROW 51001, N'Canonical schema mismatch: hoa_don.ngay_tao. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.hoa_don',N'ngay_cap_nhat') IS NULL THROW 51001, N'Canonical schema mismatch: hoa_don.ngay_cap_nhat. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.hoa_don',N'trang_thai') IS NULL THROW 51001, N'Canonical schema mismatch: hoa_don.trang_thai. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.hoa_don_chi_tiet',N'id') IS NULL THROW 51001, N'Canonical schema mismatch: hoa_don_chi_tiet.id. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.hoa_don_chi_tiet',N'id_hoa_don') IS NULL THROW 51001, N'Canonical schema mismatch: hoa_don_chi_tiet.id_hoa_don. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.hoa_don_chi_tiet',N'id_san_pham_chi_tiet') IS NULL THROW 51001, N'Canonical schema mismatch: hoa_don_chi_tiet.id_san_pham_chi_tiet. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.hoa_don_chi_tiet',N'so_luong') IS NULL THROW 51001, N'Canonical schema mismatch: hoa_don_chi_tiet.so_luong. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.hoa_don_chi_tiet',N'don_gia') IS NULL THROW 51001, N'Canonical schema mismatch: hoa_don_chi_tiet.don_gia. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.hoa_don_chi_tiet',N'thanh_tien') IS NULL THROW 51001, N'Canonical schema mismatch: hoa_don_chi_tiet.thanh_tien. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.hoa_don_chi_tiet',N'ghi_chu') IS NULL THROW 51001, N'Canonical schema mismatch: hoa_don_chi_tiet.ghi_chu. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.hoa_don_chi_tiet',N'trang_thai') IS NULL THROW 51001, N'Canonical schema mismatch: hoa_don_chi_tiet.trang_thai. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.lich_su_hoa_don',N'id') IS NULL THROW 51001, N'Canonical schema mismatch: lich_su_hoa_don.id. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.lich_su_hoa_don',N'id_hoa_don') IS NULL THROW 51001, N'Canonical schema mismatch: lich_su_hoa_don.id_hoa_don. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.lich_su_hoa_don',N'nguoi_tao') IS NULL THROW 51001, N'Canonical schema mismatch: lich_su_hoa_don.nguoi_tao. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.lich_su_hoa_don',N'trang_thai') IS NULL THROW 51001, N'Canonical schema mismatch: lich_su_hoa_don.trang_thai. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.lich_su_hoa_don',N'ghi_chu') IS NULL THROW 51001, N'Canonical schema mismatch: lich_su_hoa_don.ghi_chu. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.lich_su_hoa_don',N'ngay_tao') IS NULL THROW 51001, N'Canonical schema mismatch: lich_su_hoa_don.ngay_tao. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.lich_su_thanh_toan',N'id') IS NULL THROW 51001, N'Canonical schema mismatch: lich_su_thanh_toan.id. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.lich_su_thanh_toan',N'id_hoa_don') IS NULL THROW 51001, N'Canonical schema mismatch: lich_su_thanh_toan.id_hoa_don. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.lich_su_thanh_toan',N'id_phuong_thuc_thanh_toan') IS NULL THROW 51001, N'Canonical schema mismatch: lich_su_thanh_toan.id_phuong_thuc_thanh_toan. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.lich_su_thanh_toan',N'so_tien') IS NULL THROW 51001, N'Canonical schema mismatch: lich_su_thanh_toan.so_tien. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.lich_su_thanh_toan',N'ma_giao_dich') IS NULL THROW 51001, N'Canonical schema mismatch: lich_su_thanh_toan.ma_giao_dich. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.lich_su_thanh_toan',N'thoi_gian') IS NULL THROW 51001, N'Canonical schema mismatch: lich_su_thanh_toan.thoi_gian. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.lich_su_thanh_toan',N'trang_thai') IS NULL THROW 51001, N'Canonical schema mismatch: lich_su_thanh_toan.trang_thai. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
IF COL_LENGTH(N'dbo.lich_su_thanh_toan',N'mo_ta') IS NULL THROW 51001, N'Canonical schema mismatch: lich_su_thanh_toan.mo_ta. Run 01_sqlSD13.sql in a fresh database; no automatic migration.', 1;
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

-- 02_seed_phieu_giam_gia.sql; unlimited is represented by a null quantity.

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
INSERT INTO khach_hang (ma_khach_hang, ten_tai_khoan, ten_khach_hang, email, so_dien_thoai, trang_thai, ngay_tao, ngay_cap_nhat)
SELECT v.ma, LOWER(v.ma), v.ten, v.email, v.sdt, 1, SYSDATETIME(), SYSDATETIME()
FROM (VALUES
    ('KH001', N'Trần Minh Bảo Hoàng', 'tranminhbaohoang@smashstep.vn', '0909899999'),
    ('KH002', N'Nguyễn Thị An', 'nguyenthian@smashstep.vn', '0911111111'),
    ('KH003', N'Lê Quốc Hưng', 'lequochung@smashstep.vn', '0911111111'),
    ('KH004', N'Phạm Thanh Tú', 'phamthanhtu@smashstep.vn', '0983214567'),
    ('KH005', N'Võ Gia Hân', 'vogiahan@smashstep.vn', '0905882114'),
    ('KH006', N'Đặng Hoài Nam', 'danghoainam@smashstep.vn', '0934625881'),
    ('KH007', N'Bùi Mỹ Linh', 'buimylinh@smashstep.vn', '0972230456'),
    ('KH008', N'Ngô Đức Anh', 'ngoducanh@smashstep.vn', '0902718663'),
    ('KH009', N'Đỗ Phương Vy', 'dophuongvy@smashstep.vn', '0968440127'),
    ('KH010', N'Mai Tiến Thành', 'maitienthanh@smashstep.vn', '0918305902')
     ) AS v(ma, ten, email, sdt)
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
    loai INT, tong DECIMAL(18,2), giam DECIMAL(18,2), ship DECIMAL(18,2), trang_thai INT,
    nguoi_nhan NVARCHAR(255), sdt VARCHAR(20), dia_chi NVARCHAR(500), ngay DATETIME2
);

INSERT INTO @hd VALUES
    ('HD000081', 'KH001', 'NV002', 'TIEN_MAT', 0, 2200000, 0, 0, 5, N'Trần Minh Bảo Hoàng', '0909899999', N'123 Nguyễn Trãi, Thanh Xuân, Hà Nội', '2026-09-26T09:15:00'),
    ('HD000080', 'KH002', 'NV001', 'CHUYEN_KHOAN', 1, 2370000, 100000, 30000, 5, N'Nguyễn Thị An', '0911111111', N'Hai Bà Trưng, Hà Nội', '2026-09-26T10:40:00'),
    ('HD000079', 'KH003', 'NV001', 'COD', 0, 2070000, 0, 30000, 3, N'Lê Quốc Hưng', '0911111111', N'Cầu Giấy, Hà Nội', '2026-09-25T14:05:00'),
    ('HD000078', 'KH004', 'NV002', 'CHUYEN_KHOAN', 1, 1920000, 100000, 30000, 4, N'Phạm Thanh Tú', '0983214567', N'Nam Từ Liêm, Hà Nội', '2026-09-25T16:20:00'),
    ('HD000077', 'KH005', 'NV003', 'TIEN_MAT', 0, 3420000, 0, 0, 0, N'Võ Gia Hân', '0905882114', N'Hà Nội', '2026-09-24T08:30:00'),
    ('HD000076', 'KH006', 'NV002', 'CHUYEN_KHOAN', 1, 1260000, 0, 30000, 6, N'Đặng Hoài Nam', '0934625881', N'Hà Nội', '2026-09-24T11:10:00'),
    ('HD000075', 'KH007', 'NV003', 'COD', 0, 2620000, 0, 30000, 4, N'Bùi Mỹ Linh', '0972230456', N'Thanh Xuân, Hà Nội', '2026-09-23T15:45:00'),
    ('HD000074', 'KH008', 'NV001', 'THE', 0, 1760000, 0, 0, 5, N'Ngô Đức Anh', '0902718663', N'Hà Nội', '2026-09-23T09:50:00'),
    ('HD000073', 'KH009', 'NV003', 'CHUYEN_KHOAN', 1, 2950000, 0, 30000, 0, N'Đỗ Phương Vy', '0968440127', N'Hà Nội', '2026-09-22T13:25:00'),
    ('HD000072', 'KH010', 'NV002', 'COD', 0, 4120000, 0, 30000, 3, N'Mai Tiến Thành', '0918305902', N'Hoàng Mai, Hà Nội', '2026-09-22T17:00:00'),
    ('HD000071', 'KH001', 'NV001', 'TIEN_MAT', 0, 1590000, 0, 0, 5, N'Trần Minh Bảo Hoàng', '0909899999', N'123 Nguyễn Trãi, Thanh Xuân, Hà Nội', '2026-09-21T10:00:00'),
    ('HD000070', 'KH003', 'NV002', 'CHUYEN_KHOAN', 1, 2890000, 150000, 30000, 5, N'Lê Quốc Hưng', '0911111111', N'Cầu Giấy, Hà Nội', '2026-09-21T15:30:00'),
    ('HD000069', 'KH005', 'NV003', 'COD', 0, 1980000, 0, 30000, 6, N'Võ Gia Hân', '0905882114', N'Hà Nội', '2026-09-20T09:10:00'),
    ('HD000068', 'KH007', 'NV001', 'THE', 0, 3150000, 0, 0, 5, N'Bùi Mỹ Linh', '0972230456', N'Thanh Xuân, Hà Nội', '2026-09-20T12:45:00'),
    ('HD000067', 'KH009', 'NV002', 'CHUYEN_KHOAN', 1, 2240000, 0, 30000, 5, N'Đỗ Phương Vy', '0968440127', N'Hà Nội', '2026-09-19T16:15:00');

DECLARE @NewInvoices TABLE (id BIGINT PRIMARY KEY);
INSERT INTO hoa_don (id_khach_hang, id_nhan_vien, id_phuong_thuc_thanh_toan, ma_hoa_don, loai_hoa_don,
                     tong_tien, phi_van_chuyen, tien_giam_gia, thanh_tien,
                     ho_ten_nguoi_nhan, so_dien_thoai_nguoi_nhan, dia_chi_giao_hang,
                     ngay_thanh_toan, ngay_tao, ngay_cap_nhat, trang_thai)
OUTPUT inserted.id INTO @NewInvoices
SELECT kh.id, nv.id, pt.id, h.ma, h.loai,
       tot.tong, h.ship, h.giam, tot.tong - h.giam + h.ship,
       h.nguoi_nhan, h.sdt, h.dia_chi,
       CASE WHEN h.trang_thai = 5 THEN h.ngay ELSE NULL END, h.ngay, h.ngay, h.trang_thai
FROM @hd h
CROSS APPLY (SELECT SUM(l.so_luong*spct.gia_ban) tong FROM @SeedInvoiceLines l JOIN @SeedVariantMap vm ON vm.ma_ct=l.ma_ct JOIN san_pham_chi_tiet spct ON spct.id=vm.id WHERE l.ma_hd=h.ma) tot
LEFT JOIN khach_hang kh ON kh.ma_khach_hang = h.ma_kh
LEFT JOIN nhan_vien nv ON nv.ma_nhan_vien = h.ma_nv
LEFT JOIN phuong_thuc_thanh_toan pt ON pt.ma_phuong_thuc = h.ma_pt
WHERE NOT EXISTS (SELECT 1 FROM hoa_don x WHERE x.ma_hoa_don = h.ma);

/* ---------- 5. Làm sạch thông tin giao hàng + lịch sử hóa đơn ----------
   Dữ liệu seed phải có thông tin giao hàng để FE chi tiết hóa đơn hiển thị đúng.
   Lịch sử được tạo đủ theo từng trạng thái đã đi qua, không dùng DATEADD(HOUR, 2, ...). */
UPDATE hd
SET hd.don_vi_van_chuyen = CASE hd.ma_hoa_don
        WHEN 'HD000080' THEN N'Giao Hàng Nhanh'
        WHEN 'HD000079' THEN N'GHTK'
        WHEN 'HD000078' THEN N'Viettel Post'
        WHEN 'HD000076' THEN N'Giao Hàng Nhanh'
        WHEN 'HD000075' THEN N'GHTK'
        WHEN 'HD000073' THEN N'Viettel Post'
        WHEN 'HD000072' THEN N'GHTK'
        WHEN 'HD000070' THEN N'Giao Hàng Nhanh'
        WHEN 'HD000069' THEN N'Viettel Post'
        WHEN 'HD000067' THEN N'GHTK'
        ELSE NULL
    END,
    hd.ghi_chu = CASE hd.ma_hoa_don
        WHEN 'HD000080' THEN N'Giao trong giờ hành chính, gọi trước khi giao.'
        WHEN 'HD000079' THEN N'Để hàng tại quầy bảo vệ nếu khách không nghe máy.'
        WHEN 'HD000078' THEN N'Vui lòng giao đúng địa chỉ đã đăng ký.'
        WHEN 'HD000076' THEN N'Đơn đã hủy theo yêu cầu của khách hàng.'
        WHEN 'HD000075' THEN N'Giao trong giờ hành chính.'
        WHEN 'HD000073' THEN N'Liên hệ khách hàng trước khi giao.'
        WHEN 'HD000072' THEN N'Giao giờ hành chính, ưu tiên buổi chiều.'
        WHEN 'HD000070' THEN N'Vui lòng kiểm tra hàng trước khi nhận.'
        WHEN 'HD000069' THEN N'Đơn đã hủy theo yêu cầu của khách hàng.'
        WHEN 'HD000067' THEN N'Giao giờ hành chính.'
        ELSE NULL
    END
FROM dbo.hoa_don hd
JOIN @hd seed ON seed.ma = hd.ma_hoa_don
JOIN @NewInvoices newInvoice ON newInvoice.id = hd.id;

;DECLARE @ExpectedHistory TABLE (
    id_hoa_don BIGINT NOT NULL,
    nguoi_tao BIGINT NULL,
    trang_thai INT NOT NULL,
    ghi_chu NVARCHAR(1000),
    ngay_tao DATETIME2 NOT NULL,
    PRIMARY KEY (id_hoa_don, trang_thai)
);

INSERT INTO @ExpectedHistory (id_hoa_don, nguoi_tao, trang_thai, ghi_chu, ngay_tao)
SELECT
    hd.id AS id_hoa_don,
    hd.id_nhan_vien AS nguoi_tao,
    s.trang_thai,
    CASE s.trang_thai
        WHEN 0 THEN N'Tạo hóa đơn'
        WHEN 1 THEN N'Đã xác nhận đơn hàng'
        WHEN 2 THEN N'Chuyển sang chờ giao hàng'
        WHEN 3 THEN N'Bắt đầu giao hàng'
        WHEN 4 THEN N'Giao hàng thành công'
        WHEN 5 THEN N'Hoàn thành đơn hàng'
        WHEN 6 THEN N'Hủy đơn hàng'
        WHEN 7 THEN N'Hoàn tiền đơn hàng'
        WHEN 8 THEN N'Hóa đơn chờ'
    END AS ghi_chu,
    CASE s.trang_thai
        WHEN 0 THEN seed.ngay
        WHEN 1 THEN DATEADD(MINUTE, 10, seed.ngay)
        WHEN 2 THEN DATEADD(MINUTE, 30, seed.ngay)
        WHEN 3 THEN DATEADD(MINUTE, 60, seed.ngay)
        WHEN 4 THEN DATEADD(MINUTE, 90, seed.ngay)
        WHEN 5 THEN DATEADD(MINUTE, CASE WHEN (CASE WHEN hd.loai_hoa_don IN (1,2) OR (hd.loai_hoa_don=0 AND hd.id_khach_hang IS NOT NULL) THEN 1 ELSE 0 END) = 0 THEN 60 ELSE 120 END, seed.ngay)
        WHEN 6 THEN DATEADD(MINUTE, 150, seed.ngay)
        WHEN 7 THEN DATEADD(MINUTE, 180, seed.ngay)
        WHEN 8 THEN DATEADD(MINUTE, 5, seed.ngay)
    END AS ngay_tao
FROM dbo.hoa_don hd
JOIN @hd seed ON seed.ma = hd.ma_hoa_don
JOIN @NewInvoices newInvoice ON newInvoice.id = hd.id
CROSS APPLY (VALUES (0),(1),(2),(3),(4),(5),(6),(7),(8)) s(trang_thai)
WHERE
    -- Đơn tại quầy: chỉ 0 -> 1 -> 5; trạng thái hủy/hoàn tiền vẫn giữ lịch sử trước đó.
    ((CASE WHEN hd.loai_hoa_don IN (1,2) OR (hd.loai_hoa_don=0 AND hd.id_khach_hang IS NOT NULL) THEN 1 ELSE 0 END) = 0 AND (
        s.trang_thai = 0 OR
        (s.trang_thai = 1 AND seed.trang_thai IN (1,5,6,7)) OR
        (s.trang_thai = 5 AND seed.trang_thai IN (5,7)) OR
        (s.trang_thai IN (6,7) AND seed.trang_thai = s.trang_thai)
    ))
    OR
    -- Đơn giao hàng: đầy đủ 0 -> 1 -> 2 -> 3 -> 4 -> 5 theo trạng thái hiện tại.
    ((CASE WHEN hd.loai_hoa_don IN (1,2) OR (hd.loai_hoa_don=0 AND hd.id_khach_hang IS NOT NULL) THEN 1 ELSE 0 END) = 1 AND (
        (seed.trang_thai BETWEEN 0 AND 5 AND s.trang_thai BETWEEN 0 AND seed.trang_thai) OR
        (seed.trang_thai = 6 AND s.trang_thai IN (0,1,2,6)) OR
        (seed.trang_thai = 7 AND s.trang_thai IN (0,1,2,3,4,5,7))
    ));

-- Chỉ thêm những trạng thái còn thiếu, không tạo duplicate.
INSERT INTO lich_su_hoa_don (id_hoa_don, nguoi_tao, trang_thai, ghi_chu, ngay_tao)
SELECT e.id_hoa_don, e.nguoi_tao, e.trang_thai, e.ghi_chu, e.ngay_tao
FROM @ExpectedHistory e
WHERE NOT EXISTS (
    SELECT 1
    FROM lich_su_hoa_don l
    WHERE l.id_hoa_don = e.id_hoa_don
      AND l.trang_thai = e.trang_thai
);

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
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
SELECT name AS canonical_table FROM sys.tables WHERE is_ms_shipped=0 ORDER BY name;
GO
SELECT loai_hoa_don, COUNT_BIG(*) AS so_luong FROM dbo.hoa_don GROUP BY loai_hoa_don ORDER BY loai_hoa_don;
GO
