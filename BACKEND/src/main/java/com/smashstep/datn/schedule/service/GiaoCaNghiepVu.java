package com.smashstep.datn.schedule.service;
import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.schedule.dto.*;
import com.smashstep.datn.schedule.entity.LichLamViec;
import com.smashstep.datn.schedule.repository.LichLamViecRepository;
import com.smashstep.datn.product.service.KhoaGhiSanPham;
import com.smashstep.datn.auth.security.JwtAuthFilter.AuthDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class GiaoCaNghiepVu {
    private final LichLamViecRepository repo;
    private final TienCaService tien;
    private final SoGiaoCaStore so;
    private final KhoaGhiSanPham lock;
    private Clock clock=Clock.system(ZoneId.of("Asia/Ho_Chi_Minh"));
    private LocalDateTime now(){return LocalDateTime.now(clock);}
    public static Long nhanVienHienTai(){
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth==null||!auth.isAuthenticated()||!(auth.getDetails() instanceof AuthDetails d)
                ||!"NHAN_VIEN".equals(d.userType())||d.userId()==null)throw AppException.forbidden("Cần đăng nhập tài khoản nhân viên");
        return ((AuthDetails)auth.getDetails()).userId();
    }
    private List<LichLamViec> nearby(Long employee){var day=now().toLocalDate();return repo.findForEmployee(employee,day.minusDays(1),day.plusDays(1));}
    private QuyTacLichLamViec.KhoangGio range(LichLamViec row){return QuyTacLichLamViec.khoang(row.getNgayLam(),row.getIdCaLamViec());}
    public static boolean coTheNhan(LichLamViec row,LocalDateTime now){
        return QuyTacTrangThaiCa.coTheNhan(row,now);
    }
    public GiaoCaResponse sapNhan(Long employee){
        var now=now();
        if(nearby(employee).stream().anyMatch(r->Integer.valueOf(1).equals(r.getTrangThai())))return null;
        return nearby(employee).stream().filter(r->coTheNhan(r,now)).min(Comparator.comparing(r->range(r).batDau()))
                .map(this::deXuatNhanCa).orElse(null);
    }
    private GiaoCaResponse deXuatNhanCa(LichLamViec current){
        var builder=GiaoCaResponse.builder().lichLamViecId(current.getId())
                .tenCa(current.getIdCaLamViec().getTenCa()).gioBatDau(fmt(range(current).batDau()))
                .gioKetThuc(fmt(range(current).ketThuc())).tienMatDauCa(BigDecimal.ZERO)
                .chuyenKhoanDauCa(BigDecimal.ZERO).tongTienDauCa(BigDecimal.ZERO);
        var previous=repo.findClosedBeforeShift(current.getNgayLam(),current.getIdCaLamViec().getGioBatDau()).stream()
                .filter(row -> !closedAt(row).isAfter(now()))
                .max(Comparator.comparing(this::closedAt).thenComparing(LichLamViec::getId));
        if(previous.isPresent()){
            var row=previous.get();var summary=tongKet(row);
            boolean counted=summary.getTienMatThucTe()!=null;
            var cash=counted?summary.getTienMatThucTe():summary.getTienMatDuKien();
            builder.lichLamViecCaTruocId(row.getId()).tenCaTruoc(row.getIdCaLamViec().getTenCa())
                    .nhanVienCaTruoc(row.getIdNhanVien().getTenNhanVien()).ngayCaTruoc(row.getNgayLam())
                    .tienMatCaTruoc(cash).chuyenKhoanCaTruoc(summary.getChuyenKhoanCuoiCa())
                    .tienMatCaTruocDaKiemDem(counted)
                    .tienMatDauCa(counted?cash:null)
                    .chuyenKhoanDauCa(summary.getChuyenKhoanCuoiCa())
                    .tongTienDauCa(cash.add(summary.getChuyenKhoanCuoiCa()));
        }
        return builder.build();
    }
    private LocalDateTime closedAt(LichLamViec row) {
        return so.doc(row,true).map(SoGiaoCaStore.Phieu::ketThuc).orElseGet(() -> range(row).ketThuc());
    }
    public KetThucCaResponse dangLam(Long employee){return nearby(employee).stream().filter(r->Integer.valueOf(1).equals(r.getTrangThai()))
            .min(Comparator.comparing(r->range(r).batDau())).map(this::tongKet).orElse(null);}
    private LichLamViec own(Long id,Long employee){
        var row=repo.findById(id).orElseThrow(()->AppException.notFound("Không tìm thấy lịch làm việc"));
        if(!row.getIdNhanVien().getId().equals(employee))throw AppException.forbidden("Chỉ được nhận hoặc kết thúc ca của chính mình");return row;
    }
    private static void validMoney(BigDecimal amount){if(amount==null||amount.signum()<0||amount.scale()>2||amount.precision()-amount.scale()>15)throw AppException.badRequest("Nhập số tiền kiểm đếm hợp lệ, không âm, tối đa 15 chữ số và 2 số thập phân");}
    @Transactional public LichLamViec nhan(Long id,Long employee,NhanCaRequest request){
        lock.khoa("schedule");var row=own(id,employee);var now=now();
        if(request==null)throw AppException.badRequest("Cần nhập tiền mặt đầu ca đã kiểm đếm");validMoney(request.getTienMatDauCa());
        if(!coTheNhan(row,now))throw AppException.conflict("Chỉ nhận ca từ 15 phút trước giờ bắt đầu đến muộn tối đa 15 phút, trước khi ca kết thúc và khi lịch còn chờ nhận");
        if(nearby(employee).stream().anyMatch(r->Integer.valueOf(1).equals(r.getTrangThai())))throw AppException.conflict("Bạn đang có ca chưa kết thúc");
        row.setTrangThai(1);row.setNgayCapNhat(now);
        so.ghi(row,new SoGiaoCaStore.Phieu(now,null,request.getTienMatDauCa(),BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,null,false),false);
        return repo.save(row);
    }
    @Transactional public LichLamViec chuaNhan(Long id,Long employee){
        lock.khoa("schedule");var row=own(id,employee);if(!coTheNhan(row,now()))throw AppException.conflict("Ca không còn ở trạng thái có thể nhận");
        row.setTrangThai(4);row.setNgayCapNhat(now());return repo.save(row);
    }
    public KetThucCaResponse tongKet(LichLamViec row){
        var span=range(row);var saved=so.doc(row,true);var opening=so.doc(row,false);
        var start=opening.map(SoGiaoCaStore.Phieu::batDau).orElse(span.batDau());
        var end=saved.map(SoGiaoCaStore.Phieu::ketThuc).orElseGet(()->Integer.valueOf(3).equals(row.getTrangThai())
                ?span.ketThuc():(now().isBefore(span.ketThuc())?now():span.ketThuc()));
        var flow=saved.map(p->new TienCaService.Tien(p.mat(),p.nganHang(),p.khac())).orElseGet(()->
                Set.of(1,3).contains(Objects.requireNonNullElse(row.getTrangThai(),-1))
                        ? tien.tinh(row.getIdNhanVien().getId(),row.getId(),start,end) : new TienCaService.Tien(BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO));
        var initial=saved.map(SoGiaoCaStore.Phieu::dauCa).orElseGet(()->opening.map(SoGiaoCaStore.Phieu::dauCa).orElse(BigDecimal.ZERO));
        BigDecimal actual=saved.map(SoGiaoCaStore.Phieu::thucTe).orElse(null),expected=initial.add(flow.mat());
        return KetThucCaResponse.builder().lichLamViecId(row.getId()).tenCa(row.getIdCaLamViec().getTenCa()).gioBatDau(fmt(span.batDau())).gioKetThuc(fmt(span.ketThuc()))
                .ngayLam(row.getNgayLam()).thoiGianBatDau(start).thoiGianKetThuc(end).tienMatCuoiCa(flow.mat()).chuyenKhoanCuoiCa(flow.nganHang())
                .tienMatDauCa(initial).tienMatDuKien(expected).tienMatThucTe(actual).chenhLech(actual==null?null:actual.subtract(expected))
                .thanhToanKhac(flow.khac()).tongTienCuoiCa(flow.tong()).doanhThuCa(flow.tong()).ghiChuKetCa(row.getGhiChu()).daChot(saved.isPresent())
                .tuDongKetThuc(saved.map(SoGiaoCaStore.Phieu::tuDong).orElse(false)).build();
    }
    @Transactional public LichLamViec ketThuc(Long id,Long employee,KetThucCaRequest request){
        lock.khoa("schedule");var row=own(id,employee);
        if(request==null)throw AppException.badRequest("Cần số tiền mặt thực tế cuối ca");validMoney(request.getTienMatThucTe());
        return close(row,request.getTienMatThucTe(),request.getGhiChuKetCa(),false);
    }
    private LichLamViec close(LichLamViec row,BigDecimal actual,String note,boolean automatic){
        if(!Integer.valueOf(1).equals(row.getTrangThai()))throw AppException.conflict("Chỉ kết thúc ca đang làm; ca có thể đã được chốt");
        var summary=tongKet(row);String old=Objects.toString(row.getGhiChu(),"");
        String text=note==null||note.isBlank()?old:(old.isBlank()?"Kết ca: "+note.trim():old+" | Kết ca: "+note.trim());
        if(text.length()>500)throw AppException.badRequest("Tổng ghi chú lịch và ghi chú kết ca không được quá 500 ký tự");
        so.ghi(row,new SoGiaoCaStore.Phieu(summary.getThoiGianBatDau(),summary.getThoiGianKetThuc(),summary.getTienMatDauCa(),
                summary.getTienMatCuoiCa(),summary.getChuyenKhoanCuoiCa(),summary.getThanhToanKhac(),actual,automatic),true);
        row.setTrangThai(3);row.setGhiChu(text.isBlank()?null:text);row.setNgayCapNhat(now());return repo.save(row);
    }
    @Transactional public int quaGio(){
        lock.khoa("schedule");var rows=repo.findExpiredLich(now());
        for(var row:rows){if(Integer.valueOf(1).equals(row.getTrangThai()))close(row,null,null,true);
        else if(QuyTacTrangThaiCa.choNhan(row) && QuyTacTrangThaiCa.hetHanNhan(row,now())){row.setTrangThai(2);row.setNgayCapNhat(now());repo.save(row);}}
        return rows.size();
    }
    private static String fmt(LocalDateTime time){return time.toLocalTime().toString().substring(0,5);}
}
