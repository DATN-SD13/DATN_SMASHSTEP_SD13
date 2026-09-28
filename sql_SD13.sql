IF DB_ID(N'SmashStep') IS NULL
BEGIN
    CREATE DATABASE SmashStep;
END
GO

USE SmashStep;
GO

-- =========================================================
-- 1. DANH MỤC
-- =========================================================

CREATE TABLE danh_muc (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_danh_muc VARCHAR(50) NOT NULL UNIQUE,
    ten_danh_muc NVARCHAR(255) NOT NULL,
    mo_ta NVARCHAR(1000),
    trang_thai INT,
    xoa_mem BIT DEFAULT 0
);
GO

CREATE TABLE thuong_hieu (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_thuong_hieu VARCHAR(50) NOT NULL UNIQUE,
    ten_thuong_hieu NVARCHAR(255) NOT NULL,
    trang_thai INT,
    xoa_mem BIT DEFAULT 0
);
GO

CREATE TABLE chat_lieu (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_chat_lieu VARCHAR(50) NOT NULL UNIQUE,
    ten_chat_lieu NVARCHAR(255) NOT NULL,
    trang_thai INT,
    xoa_mem BIT DEFAULT 0
);
GO

CREATE TABLE kieu_dang (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_kieu_dang VARCHAR(50) NOT NULL UNIQUE,
    ten_kieu_dang NVARCHAR(255) NOT NULL,
    trang_thai INT,
    xoa_mem BIT DEFAULT 0
);
GO

CREATE TABLE co_giay (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_co_giay VARCHAR(50) NOT NULL UNIQUE,
    ten_co_giay NVARCHAR(255) NOT NULL,
    trang_thai INT,
    xoa_mem BIT DEFAULT 0
);
GO

CREATE TABLE xuat_xu (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_xuat_xu VARCHAR(50) NOT NULL UNIQUE,
    ten_xuat_xu NVARCHAR(255) NOT NULL,
    trang_thai INT,
    xoa_mem BIT DEFAULT 0
);
GO

CREATE TABLE mau_sac (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_mau VARCHAR(50) NOT NULL UNIQUE,
    ten_mau NVARCHAR(255) NOT NULL,
    trang_thai INT,
    xoa_mem BIT DEFAULT 0
);
GO

CREATE TABLE kich_thuoc (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_kich_thuoc VARCHAR(50) NOT NULL UNIQUE,
    ten_kich_thuoc NVARCHAR(255),
    size VARCHAR(20),
    trang_thai INT,
    xoa_mem BIT DEFAULT 0
);
GO


-- =========================================================
-- 2. SẢN PHẨM
-- =========================================================

CREATE TABLE san_pham (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_danh_muc BIGINT,
    id_thuong_hieu BIGINT,
    id_chat_lieu BIGINT,
    id_kieu_dang BIGINT,
    id_co_giay BIGINT,
    id_xuat_xu BIGINT,
    ma_san_pham VARCHAR(50) NOT NULL UNIQUE,
    ten_san_pham NVARCHAR(255) NOT NULL,
    mo_ta_chi_tiet NVARCHAR(MAX),
    ngay_tao DATETIME2 DEFAULT GETDATE(),
    xoa_mem BIT DEFAULT 0,
    trang_thai INT,
    nguoi_tao BIGINT,
    nguoi_cap_nhat BIGINT,

    CONSTRAINT FK_san_pham_danh_muc
        FOREIGN KEY (id_danh_muc)
        REFERENCES danh_muc(id),

    CONSTRAINT FK_san_pham_thuong_hieu
        FOREIGN KEY (id_thuong_hieu)
        REFERENCES thuong_hieu(id),

    CONSTRAINT FK_san_pham_chat_lieu
        FOREIGN KEY (id_chat_lieu)
        REFERENCES chat_lieu(id),

    CONSTRAINT FK_san_pham_kieu_dang
        FOREIGN KEY (id_kieu_dang)
        REFERENCES kieu_dang(id),

    CONSTRAINT FK_san_pham_co_giay
        FOREIGN KEY (id_co_giay)
        REFERENCES co_giay(id),

    CONSTRAINT FK_san_pham_xuat_xu
        FOREIGN KEY (id_xuat_xu)
        REFERENCES xuat_xu(id)
);
GO


CREATE TABLE san_pham_chi_tiet (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_san_pham BIGINT NOT NULL,
    id_mau_sac BIGINT NOT NULL,
    id_kich_thuoc BIGINT NOT NULL,
    ma_chi_tiet_sp VARCHAR(50) NOT NULL UNIQUE,
    so_luong INT DEFAULT 0,
    gia_nem_yet DECIMAL(18,2),
    gia_ban DECIMAL(18,2),
    ghi_chu NVARCHAR(1000),
    trang_thai INT,
    xoa_mem BIT DEFAULT 0,
    ngay_tao DATETIME2 DEFAULT GETDATE(),
    nguoi_tao BIGINT,
    ngay_cap_nhat DATETIME2,
    nguoi_cap_nhat BIGINT,

    CONSTRAINT FK_spct_san_pham
        FOREIGN KEY (id_san_pham)
        REFERENCES san_pham(id),

    CONSTRAINT FK_spct_mau_sac
        FOREIGN KEY (id_mau_sac)
        REFERENCES mau_sac(id),

    CONSTRAINT FK_spct_kich_thuoc
        FOREIGN KEY (id_kich_thuoc)
        REFERENCES kich_thuoc(id)
);
GO


CREATE TABLE hinh_anh_san_pham (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_san_pham BIGINT NOT NULL,
    url_anh NVARCHAR(1000),
    is_anh_chinh BIT DEFAULT 0,
    xoa_mem BIT DEFAULT 0,

    CONSTRAINT FK_hinh_anh_san_pham
        FOREIGN KEY (id_san_pham)
        REFERENCES san_pham(id)
);
GO


CREATE TABLE anh_chi_tiet_sp (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_chi_tiet_san_pham BIGINT NOT NULL,
    duong_san_sp NVARCHAR(1000),
    anh_dai_dien NVARCHAR(1000),
    mo_ta NVARCHAR(1000),
    xoa_mem BIT DEFAULT 0,

    CONSTRAINT FK_anh_ctsp
        FOREIGN KEY (id_chi_tiet_san_pham)
        REFERENCES san_pham_chi_tiet(id)
);
GO


-- =========================================================
-- 3. QUYỀN HẠN - CHỨC NĂNG
-- =========================================================

CREATE TABLE quyen_han (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_quyen_han VARCHAR(50) NOT NULL UNIQUE,
    ten_quyen_han NVARCHAR(255) NOT NULL,
    trang_thai INT,
    xoa_mem BIT DEFAULT 0
);
GO


CREATE TABLE chuc_nang (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_chuc_nang VARCHAR(50) NOT NULL UNIQUE,
    ten_chuc_nang NVARCHAR(255) NOT NULL,
    mo_ta NVARCHAR(1000),
    trang_thai INT
);
GO


CREATE TABLE quyen_han_chuc_nang (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_quyen_han BIGINT NOT NULL,
    id_chuc_nang BIGINT NOT NULL,
    trang_thai INT,
    xoa_mem BIT DEFAULT 0,

    CONSTRAINT FK_qhcn_quyen_han
        FOREIGN KEY (id_quyen_han)
        REFERENCES quyen_han(id),

    CONSTRAINT FK_qhcn_chuc_nang
        FOREIGN KEY (id_chuc_nang)
        REFERENCES chuc_nang(id),

    CONSTRAINT UQ_qhcn
        UNIQUE (id_quyen_han, id_chuc_nang)
);
GO


-- =========================================================
-- 4. NHÂN VIÊN
-- =========================================================

CREATE TABLE nhan_vien (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_nhan_vien VARCHAR(50) NOT NULL UNIQUE,
    ho_ten NVARCHAR(255) NOT NULL,
    email VARCHAR(255),
    so_dien_thoai VARCHAR(20),
    mat_khau VARCHAR(255),
    ngay_sinh DATE,
    gioi_tinh INT,
    dia_chi NVARCHAR(500),
    chuc_vu NVARCHAR(100),
    anh_dai_dien NVARCHAR(1000),
    trang_thai INT,
    ngay_tao DATETIME2 DEFAULT GETDATE(),
    nguoi_tao BIGINT,
    ngay_cap_nhat DATETIME2,
    nguoi_cap_nhat BIGINT,
    id_quyen_han BIGINT,

    CONSTRAINT FK_nhan_vien_quyen_han
        FOREIGN KEY (id_quyen_han)
        REFERENCES quyen_han(id)
);
GO


-- =========================================================
-- 5. KHÁCH HÀNG
-- =========================================================

CREATE TABLE khach_hang (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_khach_hang VARCHAR(50) NOT NULL UNIQUE,
    ten_khach_hang NVARCHAR(255) NOT NULL,
    ten_tai_khoan VARCHAR(100),
    mat_khau VARCHAR(255),
    email VARCHAR(255),
    so_dien_thoai VARCHAR(20),
    gioi_tinh INT,
    ngay_sinh DATE,
    anh_dai_dien NVARCHAR(1000),
    trang_thai INT,
    ngay_tao DATETIME2 DEFAULT GETDATE(),
    nguoi_tao BIGINT,
    ngay_cap_nhat DATETIME2,
    nguoi_cap_nhat BIGINT
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
    is_mac_dinh BIT DEFAULT 0,

    CONSTRAINT FK_dia_chi_khach_hang
        FOREIGN KEY (id_khach_hang)
        REFERENCES khach_hang(id)
);
GO


-- =========================================================
-- 6. PHIẾU GIẢM GIÁ
-- =========================================================

CREATE TABLE phieu_giam_gia (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_voucher VARCHAR(50) NOT NULL UNIQUE,
    ten_voucher NVARCHAR(255) NOT NULL,
    loai_giam_gia INT,
    gia_tri_giam DECIMAL(18,2),
    giam_toi_da DECIMAL(18,2),
    don_toi_thieu DECIMAL(18,2),
    so_luong INT,
    so_luong_da_dung INT DEFAULT 0,
    loai_phieu INT,
    ngay_bat_dau DATETIME2,
    ngay_ket_thuc DATETIME2,
    ngay_tao DATETIME2 DEFAULT GETDATE(),
    trang_thai INT
);
GO


CREATE TABLE phieu_giam_gia_ca_nhan (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_khach_hang BIGINT NOT NULL,
    id_phieu_giam_gia BIGINT NOT NULL,
    trang_thai INT,
    da_su_dung BIT DEFAULT 0,
    ma_phieu_giam_gia_ca_nhan VARCHAR(50),
    ngay_nhan DATETIME2,
    ngay_su_dung DATETIME2,

    CONSTRAINT FK_pggcn_khach_hang
        FOREIGN KEY (id_khach_hang)
        REFERENCES khach_hang(id),

    CONSTRAINT FK_pggcn_phieu_giam_gia
        FOREIGN KEY (id_phieu_giam_gia)
        REFERENCES phieu_giam_gia(id)
);
GO


CREATE TABLE phieu_giam_gia_chi_tiet (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_phieu_giam_gia BIGINT NOT NULL,
    id_khach_hang BIGINT NOT NULL,
    trang_thai INT,

    CONSTRAINT FK_pggct_phieu
        FOREIGN KEY (id_phieu_giam_gia)
        REFERENCES phieu_giam_gia(id),

    CONSTRAINT FK_pggct_khach
        FOREIGN KEY (id_khach_hang)
        REFERENCES khach_hang(id)
);
GO


-- =========================================================
-- 7. ĐỢT GIẢM GIÁ
-- =========================================================

CREATE TABLE dot_giam_gia (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_dot_giam_gia VARCHAR(50) NOT NULL UNIQUE,
    ten_dot_giam_gia NVARCHAR(255) NOT NULL,
    loai_giam_gia INT,
    gia_tri_giam_gia DECIMAL(18,2),
    ngay_bat_dau DATETIME2,
    ngay_ket_thuc DATETIME2,
    muc_uu_tien INT,
    trang_thai INT,
    xoa_mem BIT DEFAULT 0
);
GO


CREATE TABLE chi_tiet_dot_giam_gia (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_dot_giam_gia BIGINT NOT NULL,
    id_chi_tiet_san_pham BIGINT NOT NULL,
    so_luong_ap_dung INT,
    gia_tri_giam DECIMAL(18,2),
    so_tien_da_giam DECIMAL(18,2),
    trang_thai INT,
    ghi_chu NVARCHAR(1000),
    xoa_mem BIT DEFAULT 0,
    ngay_tao DATETIME2 DEFAULT GETDATE(),
    nguoi_tao BIGINT,
    ngay_cap_nhat DATETIME2,
    nguoi_cap_nhat BIGINT,

    CONSTRAINT FK_ctdgg_dot_giam_gia
        FOREIGN KEY (id_dot_giam_gia)
        REFERENCES dot_giam_gia(id),

    CONSTRAINT FK_ctdgg_spct
        FOREIGN KEY (id_chi_tiet_san_pham)
        REFERENCES san_pham_chi_tiet(id)
);
GO


-- =========================================================
-- 8. HÌNH THỨC THANH TOÁN
-- =========================================================

CREATE TABLE hinh_thuc_thanh_toan (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_hinh_thuc_thanh_toan VARCHAR(50) NOT NULL UNIQUE,
    ten_hinh_thuc_thanh_toan NVARCHAR(255) NOT NULL,
    nha_cung_cap NVARCHAR(255),
    trang_thai INT
);
GO


-- =========================================================
-- 9. CA LÀM - LỊCH LÀM VIỆC
-- =========================================================

CREATE TABLE ca_lam (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_ca VARCHAR(50) NOT NULL UNIQUE,
    ten_ca NVARCHAR(255) NOT NULL,
    gio_bat_dau TIME,
    gio_ket_thuc TIME,
    mo_ta NVARCHAR(1000),
    trang_thai INT
);
GO


CREATE TABLE lich_lam_viec (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_ca_lam BIGINT NOT NULL,
    ngay_lam DATE NOT NULL,
    ghi_chu NVARCHAR(1000),
    ngay_tao DATETIME2 DEFAULT GETDATE(),
    nguoi_tao BIGINT,
    ngay_cap_nhat DATETIME2,
    nguoi_cap_nhat BIGINT,

    CONSTRAINT FK_lich_lam_viec_ca_lam
        FOREIGN KEY (id_ca_lam)
        REFERENCES ca_lam(id)
);
GO


CREATE TABLE lich_lam_viec_nhan_vien (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_lich_lam_viec BIGINT NOT NULL,
    id_nhan_vien BIGINT NOT NULL,
    ngay_tao DATETIME2 DEFAULT GETDATE(),
    nguoi_tao BIGINT,
    ngay_cap_nhat DATETIME2,
    nguoi_cap_nhat BIGINT,

    CONSTRAINT FK_llvnv_lich
        FOREIGN KEY (id_lich_lam_viec)
        REFERENCES lich_lam_viec(id),

    CONSTRAINT FK_llvnv_nhan_vien
        FOREIGN KEY (id_nhan_vien)
        REFERENCES nhan_vien(id)
);
GO


CREATE TABLE giao_ca (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_giao_ca VARCHAR(50) NOT NULL UNIQUE,
    id_lich_lam_viec BIGINT,
    id_nhan_vien BIGINT,
    id_giao_ca_truoc BIGINT,
    thoi_gian_nhan_ca DATETIME2,
    thoi_gian_ket_thuc DATETIME2,
    tien_ban_giao DECIMAL(18,2),
    xac_nhan BIT DEFAULT 0,
    trang_thai INT,
    ghi_chu NVARCHAR(1000),
    ngay_tao DATETIME2 DEFAULT GETDATE(),
    nguoi_tao BIGINT,
    ngay_cap_nhat DATETIME2,
    nguoi_cap_nhat BIGINT,

    CONSTRAINT FK_giao_ca_lich
        FOREIGN KEY (id_lich_lam_viec)
        REFERENCES lich_lam_viec(id),

    CONSTRAINT FK_giao_ca_nhan_vien
        FOREIGN KEY (id_nhan_vien)
        REFERENCES nhan_vien(id),

    CONSTRAINT FK_giao_ca_truoc
        FOREIGN KEY (id_giao_ca_truoc)
        REFERENCES giao_ca(id)
);
GO


-- =========================================================
-- 10. HÓA ĐƠN
-- =========================================================

CREATE TABLE hoa_don (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_khach_hang BIGINT,
    id_nhan_vien BIGINT,
    id_phieu_giam_gia BIGINT,
    id_phieu_giam_gia_ca_nhan BIGINT,
    id_giao_ca BIGINT,
    ma_hoa_don VARCHAR(50) NOT NULL UNIQUE,
    loai_don INT,
    phi_van_chuyen DECIMAL(18,2),
    tong_tien DECIMAL(18,2),
    tong_tien_giam DECIMAL(18,2),
    tong_tien_sau_giam DECIMAL(18,2),
    ten_khach_hang NVARCHAR(255),
    dia_chi_khach_hang NVARCHAR(500),
    so_dien_thoai_khach_hang VARCHAR(20),
    email_khach_hang VARCHAR(255),
    trang_thai_hien_tai INT,
    ngay_tao DATETIME2 DEFAULT GETDATE(),
    nguoi_tao BIGINT,
    ngay_cap_nhat DATETIME2,
    nguoi_cap_nhat BIGINT,
    ngay_thanh_toan DATETIME2,
    ghi_chu NVARCHAR(1000),

    CONSTRAINT FK_hoa_don_khach_hang
        FOREIGN KEY (id_khach_hang)
        REFERENCES khach_hang(id),

    CONSTRAINT FK_hoa_don_nhan_vien
        FOREIGN KEY (id_nhan_vien)
        REFERENCES nhan_vien(id),

    CONSTRAINT FK_hoa_don_phieu_giam_gia
        FOREIGN KEY (id_phieu_giam_gia)
        REFERENCES phieu_giam_gia(id),

    CONSTRAINT FK_hoa_don_pgg_ca_nhan
        FOREIGN KEY (id_phieu_giam_gia_ca_nhan)
        REFERENCES phieu_giam_gia_ca_nhan(id),

    CONSTRAINT FK_hoa_don_giao_ca
        FOREIGN KEY (id_giao_ca)
        REFERENCES giao_ca(id)
);
GO


-- =========================================================
-- 11. CHI TIẾT HÓA ĐƠN
-- =========================================================

CREATE TABLE chi_tiet_hoa_don (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_hoa_don BIGINT NOT NULL,
    id_san_pham_chi_tiet BIGINT NOT NULL,
    ma_hoa_don_ct VARCHAR(50),
    don_gia DECIMAL(18,2),
    thanh_tien DECIMAL(18,2),
    so_luong INT,
    trang_thai INT,

    CONSTRAINT FK_cthd_hoa_don
        FOREIGN KEY (id_hoa_don)
        REFERENCES hoa_don(id),

    CONSTRAINT FK_cthd_spct
        FOREIGN KEY (id_san_pham_chi_tiet)
        REFERENCES san_pham_chi_tiet(id)
);
GO


-- =========================================================
-- 12. THANH TOÁN HÓA ĐƠN
-- =========================================================

CREATE TABLE thanh_toan_hoa_don (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_hoa_don BIGINT NOT NULL,
    id_hinh_thuc_thanh_toan BIGINT NOT NULL,
    ma_giao_dich_thanh_toan VARCHAR(255),
    so_tien DECIMAL(18,2),
    trang_thai INT,
    ma_yeu_cau VARCHAR(255),
    ma_giao_dich_ngoai VARCHAR(255),
    ma_tham_chieu VARCHAR(255),
    duong_dan_thanh_toan NVARCHAR(1000),
    ma_qr NVARCHAR(1000),
    thoi_gian_het_han DATETIME2,
    du_lieu_phan_hoi NVARCHAR(MAX),
    thoi_gian_tao DATETIME2 DEFAULT GETDATE(),
    thoi_gian_cap_nhat DATETIME2,
    ghi_chu NVARCHAR(1000),

    CONSTRAINT FK_tthd_hoa_don
        FOREIGN KEY (id_hoa_don)
        REFERENCES hoa_don(id),

    CONSTRAINT FK_tthd_hinh_thuc
        FOREIGN KEY (id_hinh_thuc_thanh_toan)
        REFERENCES hinh_thuc_thanh_toan(id)
);
GO


-- =========================================================
-- 13. LỊCH SỬ HÓA ĐƠN
-- =========================================================

CREATE TABLE lich_su_hoa_don (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_hoa_don BIGINT NOT NULL,
    trang_thai INT,
    thoi_gian DATETIME2 DEFAULT GETDATE(),
    ghi_chu NVARCHAR(1000),
    nguoi_cap_nhat BIGINT,
    nguoi_thuc_hien BIGINT,

    CONSTRAINT FK_lshd_hoa_don
        FOREIGN KEY (id_hoa_don)
        REFERENCES hoa_don(id),

    CONSTRAINT FK_lshd_nguoi_cap_nhat
        FOREIGN KEY (nguoi_cap_nhat)
        REFERENCES nhan_vien(id),

    CONSTRAINT FK_lshd_nguoi_thuc_hien
        FOREIGN KEY (nguoi_thuc_hien)
        REFERENCES nhan_vien(id)
);
GO


-- =========================================================
-- 14. LỊCH SỬ THANH TOÁN
-- =========================================================

CREATE TABLE lich_su_thanh_toan (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_hoa_don BIGINT NOT NULL,
    so_tien DECIMAL(18,2),
    phuong_thuc_thanh_toan NVARCHAR(255),
    trang_thai_thanh_toan INT,
    ngay_thanh_toan DATETIME2,
    ghi_chu NVARCHAR(1000),

    CONSTRAINT FK_lstt_hoa_don
        FOREIGN KEY (id_hoa_don)
        REFERENCES hoa_don(id)
);
GO
