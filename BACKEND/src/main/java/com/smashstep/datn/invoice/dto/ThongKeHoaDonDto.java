package com.smashstep.datn.invoice.dto;

import java.math.BigDecimal;
import java.util.List;

public final class ThongKeHoaDonDto {

    private final long tongHoaDon;
    private final long hoaDonHoanThanh;
    private final long hoaDonDaHuy;
    private final long hoaDonDangXuLy;
    private final long hoaDonDaThanhToan;
    private final BigDecimal doanhThuDaThanhToan;
    private final List<TrangThai> theoTrangThai;
    private final List<LoaiDon> theoLoaiDon;

    public ThongKeHoaDonDto(
            long tongHoaDon,
            long hoaDonHoanThanh,
            long hoaDonDaHuy,
            long hoaDonDangXuLy,
            long hoaDonDaThanhToan,
            BigDecimal doanhThuDaThanhToan,
            List<TrangThai> theoTrangThai,
            List<LoaiDon> theoLoaiDon
    ) {
        this.tongHoaDon = tongHoaDon;
        this.hoaDonHoanThanh = hoaDonHoanThanh;
        this.hoaDonDaHuy = hoaDonDaHuy;
        this.hoaDonDangXuLy = hoaDonDangXuLy;
        this.hoaDonDaThanhToan = hoaDonDaThanhToan;
        this.doanhThuDaThanhToan = doanhThuDaThanhToan;
        this.theoTrangThai = theoTrangThai;
        this.theoLoaiDon = theoLoaiDon;
    }

    // Giữ cách gọi giống record để code/service/test hiện tại không phải sửa
    public long tongHoaDon() {
        return tongHoaDon;
    }

    public long hoaDonHoanThanh() {
        return hoaDonHoanThanh;
    }

    public long hoaDonDaHuy() {
        return hoaDonDaHuy;
    }

    public long hoaDonDangXuLy() {
        return hoaDonDangXuLy;
    }

    public long hoaDonDaThanhToan() {
        return hoaDonDaThanhToan;
    }

    public BigDecimal doanhThuDaThanhToan() {
        return doanhThuDaThanhToan;
    }

    public List<TrangThai> theoTrangThai() {
        return theoTrangThai;
    }

    public List<LoaiDon> theoLoaiDon() {
        return theoLoaiDon;
    }

    // Getter chuẩn để Jackson trả JSON cho frontend
    public long getTongHoaDon() {
        return tongHoaDon;
    }

    public long getHoaDonHoanThanh() {
        return hoaDonHoanThanh;
    }

    public long getHoaDonDaHuy() {
        return hoaDonDaHuy;
    }

    public long getHoaDonDangXuLy() {
        return hoaDonDangXuLy;
    }

    public long getHoaDonDaThanhToan() {
        return hoaDonDaThanhToan;
    }

    public BigDecimal getDoanhThuDaThanhToan() {
        return doanhThuDaThanhToan;
    }

    public List<TrangThai> getTheoTrangThai() {
        return theoTrangThai;
    }

    public List<LoaiDon> getTheoLoaiDon() {
        return theoLoaiDon;
    }

    public static final class TrangThai {

        private final Integer ma;
        private final String ten;
        private final String khoa;
        private final long soLuong;

        public TrangThai(Integer ma, String ten, String khoa, long soLuong) {
            this.ma = ma;
            this.ten = ten;
            this.khoa = khoa;
            this.soLuong = soLuong;
        }

        // Giữ tương thích code/test đang dùng record accessor
        public Integer ma() {
            return ma;
        }

        public String ten() {
            return ten;
        }

        public String khoa() {
            return khoa;
        }

        public long soLuong() {
            return soLuong;
        }

        public Integer getMa() {
            return ma;
        }

        public String getTen() {
            return ten;
        }

        public String getKhoa() {
            return khoa;
        }

        public long getSoLuong() {
            return soLuong;
        }
    }

    public static final class LoaiDon {

        private final Integer ma;
        private final String ten;
        private final String khoa;
        private final long soLuong;

        public LoaiDon(Integer ma, String ten, String khoa, long soLuong) {
            this.ma = ma;
            this.ten = ten;
            this.khoa = khoa;
            this.soLuong = soLuong;
        }

        public Integer ma() {
            return ma;
        }

        public String ten() {
            return ten;
        }

        public String khoa() {
            return khoa;
        }

        public long soLuong() {
            return soLuong;
        }

        public Integer getMa() {
            return ma;
        }

        public String getTen() {
            return ten;
        }

        public String getKhoa() {
            return khoa;
        }

        public long getSoLuong() {
            return soLuong;
        }
    }
}