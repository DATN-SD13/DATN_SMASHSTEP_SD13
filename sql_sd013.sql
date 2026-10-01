/* =========================================================
   SMASHSTEP SD-013 
   ========================================================= */

IF DB_ID(N'SmashStep') IS NULL
BEGIN
    CREATE DATABASE SmashStep;
END
GO

USE SmashStep;
GO

/* =========================================================
   1. VAI TRÒ
   ========================================================= */
CREATE TABLE vai_tro (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ten_vai_tro NVARCHAR(255),
    mo_ta NVARCHAR(1000),
    trang_thai INT
);
GO

/* =========================================================
   2. DANH MỤC / THUỘC TÍNH SẢN PHẨM
   ========================================================= */
CREATE TABLE danh_muc (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_danh_muc VARCHAR(50),
    ten_danh_muc NVARCHAR(255),
    mo_ta NVARCHAR(1000),
    trang_thai INT
);
GO

CREATE TABLE thuong_hieu (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_thuong_hieu VARCHAR(50),
    ten_thuong_hieu NVARCHAR(255),
    trang_thai INT
);
GO

CREATE TABLE chat_lieu (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_chat_lieu VARCHAR(50),
    ten_chat_lieu NVARCHAR(255),
    trang_thai INT
);
GO

CREATE TABLE xuat_xu (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_xuat_xu VARCHAR(50),
    ten_xuat_xu NVARCHAR(255),
    trang_thai INT
);
GO

CREATE TABLE co_giay (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_co_giay VARCHAR(50),
    ten_co_giay NVARCHAR(255),
    trang_thai INT
);
GO

CREATE TABLE kieu_dang (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_kieu_dang VARCHAR(50),
    ten_kieu_dang NVARCHAR(255),
    trang_thai INT
);
GO

CREATE TABLE mau_sac (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_mau_sac VARCHAR(50),
    ten_mau_sac NVARCHAR(255),
    ma_mau_hex VARCHAR(20),
    trang_thai INT,
    ngay_tao DATETIME2,
    ngay_cap_nhat DATETIME2
);
GO

CREATE TABLE kich_thuoc (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    gia_tri VARCHAR(50),
    ghi_chu NVARCHAR(1000),
    trang_thai INT,
    ngay_tao DATETIME2,
    ngay_cap_nhat DATETIME2
);
GO

/* =========================================================
   3. NHÂN VIÊN / KHÁCH HÀNG
   ========================================================= */
CREATE TABLE nhan_vien (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_vai_tro BIGINT,
    ma_nhan_vien VARCHAR(50),
    ten_dang_nhap VARCHAR(100),
    ten_nhan_vien NVARCHAR(255),
    email VARCHAR(255),
    mat_khau VARCHAR(255),
    so_dien_thoai VARCHAR(20),
    gioi_tinh INT,
    ngay_sinh DATE,
    dia_chi NVARCHAR(500),
    tinh_thanh NVARCHAR(100),
    phuong_xa NVARCHAR(100),
    trang_thai INT,
    ngay_tao DATETIME2,
    hinh_anh NVARCHAR(1000),
    ngay_cap_nhat DATETIME2,
    CONSTRAINT FK_nhan_vien_vai_tro FOREIGN KEY (id_vai_tro) REFERENCES vai_tro(id)
);
GO

CREATE TABLE khach_hang (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_khach_hang VARCHAR(50),
    ten_tai_khoan VARCHAR(100),
    ten_khach_hang NVARCHAR(255),
    email VARCHAR(255),
    so_dien_thoai VARCHAR(20),
    ngay_sinh DATE,
    gioi_tinh INT,
    mat_khau VARCHAR(255),
    hinh_anh NVARCHAR(1000),
    trang_thai INT,
    ngay_tao DATETIME2,
    ngay_cap_nhat DATETIME2
);
GO

CREATE TABLE dia_chi_khach_hang (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_khach_hang BIGINT NOT NULL,
    ten_nguoi_nhan NVARCHAR(255),
    sdt_nguoi_nhan VARCHAR(20),
    tinh_thanh NVARCHAR(100),
    quan_huyen NVARCHAR(100),
    phuong_xa NVARCHAR(100),
    dia_chi_cu_the NVARCHAR(500),
    loai_dia_chi INT,
    is_mac_dinh BIT,
    trang_thai INT,
    CONSTRAINT FK_dia_chi_khach_hang FOREIGN KEY (id_khach_hang) REFERENCES khach_hang(id)
);
GO

/* =========================================================
   4. SẢN PHẨM
   ========================================================= */
CREATE TABLE san_pham (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_danh_muc BIGINT,
    id_thuong_hieu BIGINT,
    id_chat_lieu BIGINT,
    id_kieu_dang BIGINT,
    id_co_giay BIGINT,
    id_xuat_xu BIGINT,
    ma_san_pham VARCHAR(50),
    ten_san_pham NVARCHAR(255),
    mo_ta_chi_tiet NVARCHAR(MAX),
    ngay_tao DATETIME2,
    nguoi_tao BIGINT,
    nguoi_cap_nhat BIGINT,
    ngay_cap_nhat DATETIME2,
    trang_thai INT,
    CONSTRAINT FK_san_pham_danh_muc FOREIGN KEY (id_danh_muc) REFERENCES danh_muc(id),
    CONSTRAINT FK_san_pham_thuong_hieu FOREIGN KEY (id_thuong_hieu) REFERENCES thuong_hieu(id),
    CONSTRAINT FK_san_pham_chat_lieu FOREIGN KEY (id_chat_lieu) REFERENCES chat_lieu(id),
    CONSTRAINT FK_san_pham_kieu_dang FOREIGN KEY (id_kieu_dang) REFERENCES kieu_dang(id),
    CONSTRAINT FK_san_pham_co_giay FOREIGN KEY (id_co_giay) REFERENCES co_giay(id),
    CONSTRAINT FK_san_pham_xuat_xu FOREIGN KEY (id_xuat_xu) REFERENCES xuat_xu(id)
);
GO

CREATE TABLE san_pham_chi_tiet (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_san_pham BIGINT NOT NULL,
    id_mau_sac BIGINT NOT NULL,
    id_kich_thuoc BIGINT NOT NULL,
    ma_chi_tiet_san_pham VARCHAR(100),
    so_luong INT,
    gia_ban DECIMAL(18,2),
    sku VARCHAR(100),
    kich_hoat BIT,
    ngay_tao DATETIME2,
    ngay_cap_nhat DATETIME2,
    trang_thai INT,
    CONSTRAINT FK_spct_san_pham FOREIGN KEY (id_san_pham) REFERENCES san_pham(id),
    CONSTRAINT FK_spct_mau_sac FOREIGN KEY (id_mau_sac) REFERENCES mau_sac(id),
    CONSTRAINT FK_spct_kich_thuoc FOREIGN KEY (id_kich_thuoc) REFERENCES kich_thuoc(id)
);
GO

CREATE TABLE hinh_anh_san_pham (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_san_pham BIGINT NOT NULL,
    url_anh NVARCHAR(1000),
    is_anh_chinh BIT,
    CONSTRAINT FK_hinh_anh_san_pham FOREIGN KEY (id_san_pham) REFERENCES san_pham(id)
);
GO

/* =========================================================
   5. PHIẾU / ĐỢT GIẢM GIÁ
   ========================================================= */
CREATE TABLE phieu_giam_gia (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_phieu_giam_gia VARCHAR(50),
    ten_phieu_giam_gia NVARCHAR(255),
    loai_giam_gia INT,
    gia_tri_giam DECIMAL(18,2),
    gia_tri_toi_thieu DECIMAL(18,2),
    giam_toi_da DECIMAL(18,2),
    ngay_bat_dau DATETIME2,
    ngay_ket_thuc DATETIME2,
    so_luong INT,
    so_luong_da_dung INT,
    trang_thai INT,
    ngay_tao DATETIME2,
    ngay_cap_nhat DATETIME2,
    mo_ta NVARCHAR(1000)
);
GO

CREATE TABLE phieu_giam_gia_khach_hang (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_khach_hang BIGINT NOT NULL,
    id_phieu_giam_gia BIGINT NOT NULL,
    ngay_su_dung DATETIME2,
    trang_thai INT,
    CONSTRAINT FK_pggkh_khach_hang FOREIGN KEY (id_khach_hang) REFERENCES khach_hang(id),
    CONSTRAINT FK_pggkh_phieu_giam_gia FOREIGN KEY (id_phieu_giam_gia) REFERENCES phieu_giam_gia(id)
);
GO

CREATE TABLE dot_giam_gia (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_dot_giam_gia VARCHAR(50),
    ten_dot_giam_gia NVARCHAR(255),
    phan_tram_giam_dot DECIMAL(18,2),
    ngay_bat_dau DATETIME2,
    ngay_ket_thuc DATETIME2,
    kich_hoat BIT,
    ngay_tao DATETIME2,
    ngay_cap_nhat DATETIME2,
    trang_thai INT
);
GO

CREATE TABLE chi_tiet_dot_giam_gia (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_dot_giam_gia BIGINT NOT NULL,
    id_san_pham_chi_tiet BIGINT NOT NULL,
    phan_tram_giam_bien_the DECIMAL(18,2),
    trang_thai INT,
    ngay_tao DATETIME2,
    CONSTRAINT FK_ctdgg_dot_giam_gia FOREIGN KEY (id_dot_giam_gia) REFERENCES dot_giam_gia(id),
    CONSTRAINT FK_ctdgg_spct FOREIGN KEY (id_san_pham_chi_tiet) REFERENCES san_pham_chi_tiet(id)
);
GO

/* =========================================================
   6. THANH TOÁN
   phuong_thuc_thanh_toan có FK tới hinh_thuc_thanh_toan.
   ========================================================= */
CREATE TABLE hinh_thuc_thanh_toan (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_hinh_thuc VARCHAR(50),
    ten_hinh_thuc NVARCHAR(255),
    trang_thai INT
);
GO

CREATE TABLE phuong_thuc_thanh_toan (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_hinh_thuc_thanh_toan BIGINT NOT NULL,
    ma_phuong_thuc VARCHAR(50),
    ten_phuong_thuc NVARCHAR(255),
    trang_thai INT,
    CONSTRAINT FK_pttt_hinh_thuc FOREIGN KEY (id_hinh_thuc_thanh_toan) REFERENCES hinh_thuc_thanh_toan(id)
);
GO

/* =========================================================
   7. HÓA ĐƠN
   ========================================================= */
CREATE TABLE hoa_don (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_khach_hang BIGINT,
    id_nhan_vien BIGINT,
    id_phieu_giam_gia BIGINT,
    id_phuong_thuc_thanh_toan BIGINT,
    ma_hoa_don VARCHAR(50),
    loai_hoa_don INT,
    tong_tien DECIMAL(18,2),
    phi_van_chuyen DECIMAL(18,2),
    thanh_tien DECIMAL(18,2),
    don_vi_van_chuyen NVARCHAR(255),
    ho_ten_nguoi_nhan NVARCHAR(255),
    so_dien_thoai_nguoi_nhan VARCHAR(20),
    gia_chi_giao_hang DECIMAL(18,2),
    ghi_chu NVARCHAR(1000),
    ngay_thanh_toan DATETIME2,
    ngay_tao DATETIME2,
    ngay_cap_nhat DATETIME2,
    trang_thai INT,
    CONSTRAINT FK_hoa_don_khach_hang FOREIGN KEY (id_khach_hang) REFERENCES khach_hang(id),
    CONSTRAINT FK_hoa_don_nhan_vien FOREIGN KEY (id_nhan_vien) REFERENCES nhan_vien(id),
    CONSTRAINT FK_hoa_don_phieu_giam_gia FOREIGN KEY (id_phieu_giam_gia) REFERENCES phieu_giam_gia(id),
    CONSTRAINT FK_hoa_don_phuong_thuc FOREIGN KEY (id_phuong_thuc_thanh_toan) REFERENCES phuong_thuc_thanh_toan(id)
);
GO

CREATE TABLE hoa_don_chi_tiet (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_hoa_don BIGINT NOT NULL,
    id_san_pham_chi_tiet BIGINT NOT NULL,
    so_luong INT,
    don_gia DECIMAL(18,2),
    thanh_tien DECIMAL(18,2),
    ghi_chu NVARCHAR(1000),
    trang_thai INT,
    CONSTRAINT FK_hdct_hoa_don FOREIGN KEY (id_hoa_don) REFERENCES hoa_don(id),
    CONSTRAINT FK_hdct_spct FOREIGN KEY (id_san_pham_chi_tiet) REFERENCES san_pham_chi_tiet(id)
);
GO

CREATE TABLE lich_su_hoa_don (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_hoa_don BIGINT NOT NULL,
    nguoi_tao BIGINT,
    trang_thai INT,
    ghi_chu NVARCHAR(1000),
    ngay_tao DATETIME2,
    CONSTRAINT FK_lshd_hoa_don FOREIGN KEY (id_hoa_don) REFERENCES hoa_don(id)
);
GO

CREATE TABLE lich_su_thanh_toan (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_hoa_don BIGINT NOT NULL,
    so_tien DECIMAL(18,2),
    ma_giao_dich VARCHAR(255),
    thoi_gian DATETIME2,
    trang_thai INT,
    mo_ta NVARCHAR(1000),
    CONSTRAINT FK_lstt_hoa_don FOREIGN KEY (id_hoa_don) REFERENCES hoa_don(id)
);
GO

/* =========================================================
   8. KIỂM TRA NHANH 25 BẢNG MỚI
   ========================================================= */
SELECT name AS ten_bang
FROM sys.tables
WHERE is_ms_shipped = 0
ORDER BY name;
GO

PRINT N'Đã xóa schema DB cũ và tạo schema SmashStep DB mới.';
GO