/* =========================================================
   SMASHSTEP SD-013 - DATABASE SCHEMA (SQL Server)
   25 bảng. File duy nhất cho cả nhóm - mỗi người chỉ sửa khối
   của module mình (A/B/C/D bên dưới), rồi tạo Pull Request.

   Thứ tự khối đã xếp theo khóa ngoại: A -> B -> C -> D.
   File KHÔNG xóa dữ liệu. Nếu DB đã có bảng, script tự dừng và báo lỗi.
   Muốn tạo lại từ đầu (chỉ DB dev trên máy): DROP DATABASE SmashStep rồi chạy lại.
   ========================================================= */

IF DB_ID(N'SmashStep') IS NULL
BEGIN
    CREATE DATABASE SmashStep;
END
GO

USE SmashStep;
GO

SET NOCOUNT ON;
GO

/* =========================================================
   0. KIỂM TRA AN TOÀN: đã có bảng thì dừng, không đụng dữ liệu
   ========================================================= */
IF OBJECT_ID(N'dbo.vai_tro', N'U') IS NOT NULL
BEGIN
    RAISERROR(N'DB SmashStep đã có bảng. Script dừng để không mất dữ liệu.', 16, 1);
    SET NOEXEC ON;   -- bỏ qua toàn bộ phần còn lại
END
GO

/* =========================================================
   A. NHÂN SỰ & KHÁCH HÀNG   (module: Nhân viên, Khách hàng)
   ========================================================= */

CREATE TABLE vai_tro (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ten_vai_tro NVARCHAR(255),
    mo_ta NVARCHAR(1000),
    trang_thai INT
);
GO

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
   B. DANH MỤC & SẢN PHẨM    (module: Sản phẩm, Biến thể)
   TODO (làm sau): FE form thêm sản phẩm có Giới tính, Đế giày, Công nghệ đệm,
   Trọng lượng nhưng DB chưa có chỗ lưu; DB có kieu_dang, xuat_xu mà FE chưa dùng.
   Chưa sửa - người phụ trách module Sản phẩm sẽ chốt khi nối API.
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
    id_san_pham_chi_tiet BIGINT NULL, -- ảnh của đúng biến thể; NULL = ảnh chung/legacy
    id_mau_sac BIGINT,   -- ảnh theo màu (FE: "Ảnh sản phẩm chi tiết" theo từng màu); NULL = ảnh chung
    url_anh NVARCHAR(1000),
    is_anh_chinh BIT,
    CONSTRAINT FK_hinh_anh_san_pham FOREIGN KEY (id_san_pham) REFERENCES san_pham(id),
    CONSTRAINT FK_hinh_anh_mau_sac FOREIGN KEY (id_mau_sac) REFERENCES mau_sac(id),
    CONSTRAINT FK_hinh_anh_bien_the FOREIGN KEY (id_san_pham_chi_tiet) REFERENCES san_pham_chi_tiet(id)
);
GO

/* =========================================================
   C. GIẢM GIÁ               (module: Phiếu giảm giá, Đợt giảm giá)
   ========================================================= */

CREATE TABLE phieu_giam_gia (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_phieu_giam_gia VARCHAR(50),
    ten_phieu_giam_gia NVARCHAR(255),
    hinh_thuc_phieu INT,   -- 1 = Công khai, 2 = Cá nhân
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
    trang_thai INT,
    mo_ta NVARCHAR(1000)
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
   D. THANH TOÁN & HÓA ĐƠN   (module: Hóa đơn, Bán hàng tại quầy)
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
    tien_giam_gia DECIMAL(18,2),   -- số tiền đã giảm từ phiếu (lưu cứng, phiếu đổi sau này không ảnh hưởng)
    thanh_tien DECIMAL(18,2),
    don_vi_van_chuyen NVARCHAR(255),
    ho_ten_nguoi_nhan NVARCHAR(255),
    so_dien_thoai_nguoi_nhan VARCHAR(20),
    dia_chi_giao_hang NVARCHAR(500),
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
    id_phuong_thuc_thanh_toan BIGINT,   -- mỗi lần thanh toán 1 phương thức (hỗ trợ "Kết hợp")
    so_tien DECIMAL(18,2),
    ma_giao_dich VARCHAR(255),
    thoi_gian DATETIME2,
    trang_thai INT,
    mo_ta NVARCHAR(1000),
    CONSTRAINT FK_lstt_hoa_don FOREIGN KEY (id_hoa_don) REFERENCES hoa_don(id),
    CONSTRAINT FK_lstt_phuong_thuc FOREIGN KEY (id_phuong_thuc_thanh_toan) REFERENCES phuong_thuc_thanh_toan(id)
);
GO

/* =========================================================
   E. RÀNG BUỘC DUY NHẤT
   Dùng filtered index (WHERE ... IS NOT NULL) vì SQL Server chỉ cho
   1 giá trị NULL trong UNIQUE thường -> cột để trống vẫn thêm được nhiều dòng.
   ========================================================= */
CREATE UNIQUE INDEX UX_nhan_vien_ma_nhan_vien ON nhan_vien(ma_nhan_vien) WHERE ma_nhan_vien IS NOT NULL;
CREATE UNIQUE INDEX UX_nhan_vien_ten_dang_nhap ON nhan_vien(ten_dang_nhap) WHERE ten_dang_nhap IS NOT NULL;
CREATE UNIQUE INDEX UX_nhan_vien_email ON nhan_vien(email) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX UX_khach_hang_ma_khach_hang ON khach_hang(ma_khach_hang) WHERE ma_khach_hang IS NOT NULL;
CREATE UNIQUE INDEX UX_khach_hang_ten_tai_khoan ON khach_hang(ten_tai_khoan) WHERE ten_tai_khoan IS NOT NULL;
CREATE UNIQUE INDEX UX_khach_hang_email ON khach_hang(email) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX UX_danh_muc_ma_danh_muc ON danh_muc(ma_danh_muc) WHERE ma_danh_muc IS NOT NULL;
CREATE UNIQUE INDEX UX_thuong_hieu_ma_thuong_hieu ON thuong_hieu(ma_thuong_hieu) WHERE ma_thuong_hieu IS NOT NULL;
CREATE UNIQUE INDEX UX_chat_lieu_ma_chat_lieu ON chat_lieu(ma_chat_lieu) WHERE ma_chat_lieu IS NOT NULL;
CREATE UNIQUE INDEX UX_xuat_xu_ma_xuat_xu ON xuat_xu(ma_xuat_xu) WHERE ma_xuat_xu IS NOT NULL;
CREATE UNIQUE INDEX UX_co_giay_ma_co_giay ON co_giay(ma_co_giay) WHERE ma_co_giay IS NOT NULL;
CREATE UNIQUE INDEX UX_kieu_dang_ma_kieu_dang ON kieu_dang(ma_kieu_dang) WHERE ma_kieu_dang IS NOT NULL;
CREATE UNIQUE INDEX UX_mau_sac_ma_mau_sac ON mau_sac(ma_mau_sac) WHERE ma_mau_sac IS NOT NULL;
CREATE UNIQUE INDEX UX_kich_thuoc_gia_tri ON kich_thuoc(gia_tri) WHERE gia_tri IS NOT NULL;
CREATE UNIQUE INDEX UX_san_pham_ma_san_pham ON san_pham(ma_san_pham) WHERE ma_san_pham IS NOT NULL;
CREATE UNIQUE INDEX UX_san_pham_chi_tiet_ma_chi_tiet_san_pham ON san_pham_chi_tiet(ma_chi_tiet_san_pham) WHERE ma_chi_tiet_san_pham IS NOT NULL;
CREATE UNIQUE INDEX UX_san_pham_chi_tiet_sku ON san_pham_chi_tiet(sku) WHERE sku IS NOT NULL;
CREATE UNIQUE INDEX UX_phieu_giam_gia_ma_phieu_giam_gia ON phieu_giam_gia(ma_phieu_giam_gia) WHERE ma_phieu_giam_gia IS NOT NULL;
CREATE UNIQUE INDEX UX_dot_giam_gia_ma_dot_giam_gia ON dot_giam_gia(ma_dot_giam_gia) WHERE ma_dot_giam_gia IS NOT NULL;
CREATE UNIQUE INDEX UX_hinh_thuc_thanh_toan_ma_hinh_thuc ON hinh_thuc_thanh_toan(ma_hinh_thuc) WHERE ma_hinh_thuc IS NOT NULL;
CREATE UNIQUE INDEX UX_phuong_thuc_thanh_toan_ma_phuong_thuc ON phuong_thuc_thanh_toan(ma_phuong_thuc) WHERE ma_phuong_thuc IS NOT NULL;
CREATE UNIQUE INDEX UX_hoa_don_ma_hoa_don ON hoa_don(ma_hoa_don) WHERE ma_hoa_don IS NOT NULL;
CREATE UNIQUE INDEX UX_lich_su_thanh_toan_ma_giao_dich ON lich_su_thanh_toan(ma_giao_dich) WHERE ma_giao_dich IS NOT NULL;
CREATE UNIQUE INDEX UX_spct_bien_the ON san_pham_chi_tiet(id_san_pham, id_mau_sac, id_kich_thuoc);
CREATE UNIQUE INDEX UX_pggkh_kh_phieu ON phieu_giam_gia_khach_hang(id_khach_hang, id_phieu_giam_gia);
CREATE UNIQUE INDEX UX_ctdgg_dot_spct ON chi_tiet_dot_giam_gia(id_dot_giam_gia, id_san_pham_chi_tiet);
GO

/* =========================================================
   F. QUY ƯỚC GIÁ TRỊ (cột INT) - thống nhất giữa FE và BE
   ---------------------------------------------------------
   Giá trị gợi ý, nhóm họp xác nhận lại 1 lần rồi giữ nguyên.

   hoa_don.loai_hoa_don : 0 = Tại quầy | 1 = Trực tuyến | 2 = Giao hàng
   hoa_don.trang_thai   : 0 = Chờ xác nhận | 1 = Đã xác nhận | 2 = Chờ giao hàng
                          3 = Đang giao hàng | 4 = Đã giao hàng | 5 = Đã hoàn thành
                          6 = Đã hủy | 7 = Hoàn tiền | 8 = Hóa đơn chờ (POS, chưa thanh toán)
   phieu_giam_gia.hinh_thuc_phieu : 1 = Công khai | 2 = Cá nhân
   phieu_giam_gia.loai_giam_gia   : 1 = Phần trăm (%) | 2 = Tiền mặt (VNĐ)
   *.trang_thai (các bảng còn lại): 1 = Hoạt động | 0 = Ngừng hoạt động
   *.gioi_tinh : 1 = Nam | 2 = Nữ | 0 = Khác
   ========================================================= */

SELECT name AS ten_bang FROM sys.tables WHERE is_ms_shipped = 0 ORDER BY name;
GO

SET NOEXEC OFF;
GO