package com.smashstep.datn.schedule.service;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.employee.entity.NhanVien;
import com.smashstep.datn.employee.repository.NhanVienRepository;
import com.smashstep.datn.product.service.KhoaGhiSanPham;
import com.smashstep.datn.schedule.dto.LichLamViecImportDto.*;
import com.smashstep.datn.schedule.entity.*;
import com.smashstep.datn.schedule.exception.LichLamViecImportException;
import com.smashstep.datn.schedule.repository.*;
import com.smashstep.datn.schedule.util.LichLamViecImportExcelReader;
import com.smashstep.datn.schedule.util.LichLamViecImportExcelReader.DocDong;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.time.*;
import java.time.format.*;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class LichLamViecImportService {
    private final LichLamViecImportExcelReader reader;
    private final NhanVienRepository employees;
    private final CaLamViecRepository shifts;
    private final LichLamViecRepository schedules;
    private final KhoaGhiSanPham lock;
    private record Prepared(KiemTraDong row,NhanVien employee,CaLamViec shift,LocalDate date) {}
    public XemTruoc xemTruoc(MultipartFile file){return summary(prepare(reader.doc(file)));}
    @Transactional
    public KetQua xacNhan(XacNhan request){
        if(request==null||request.dong()==null||request.dong().isEmpty()||request.dong().size()>500)
            throw AppException.badRequest("Nhập từ 1 đến 500 dòng");
        lock.khoa("schedule");
        var plan=prepare(request.dong().stream().map(d->new DocDong(d,List.of())).toList());
        var checked=summary(plan);if(checked.coLoi()>0)throw new LichLamViecImportException(checked);
        var now=LocalDateTime.now();var created=new ArrayList<LichLamViec>();
        for(var p:plan)if(!p.row().daTonTai()){
            var row=new LichLamViec();row.setIdNhanVien(p.employee());row.setIdCaLamViec(p.shift());row.setNgayLam(p.date());
            row.setGhiChu(p.row().duLieu().ghiChu().isBlank()?null:p.row().duLieu().ghiChu().trim());
            row.setTrangThai(0);row.setNgayTao(now);row.setNgayCapNhat(now);created.add(row);
        }
        schedules.saveAllAndFlush(created);
        return new KetQua(created.size(),(int)checked.daTonTai(),0,"Đã tạo "+created.size()+" lịch; bỏ qua "+checked.daTonTai()+" lịch đã tồn tại.");
    }
    private List<Prepared> prepare(List<DocDong> input){
        if(input==null||input.isEmpty()||input.size()>500)throw AppException.badRequest("Nhập từ 1 đến 500 dòng");
        var result=new ArrayList<Prepared>();var numbers=new HashSet<Integer>();
        for(var source:input){
            if(source==null||source.dong()==null)throw AppException.badRequest("Dòng nhập không hợp lệ");
            var d=source.dong();
            if(d.maNhanVien()==null||d.maCa()==null||d.ngayLam()==null||d.ghiChu()==null)
                throw AppException.badRequest("Các ô trong dòng không được là null");
            var errors=new ArrayList<>(source.loi());
            if(d.soDong()<2||d.soDong()>5001||!numbers.add(d.soDong()))errors.add("Số dòng không hợp lệ hoặc bị lặp");
            if(d.maNhanVien().isBlank()||d.maNhanVien().length()>50)errors.add("Mã nhân viên bắt buộc, tối đa 50 ký tự");
            if(d.maCa().isBlank()||d.maCa().length()>50)errors.add("Mã ca bắt buộc, tối đa 50 ký tự");
            if(d.ghiChu().length()>500)errors.add("Ghi chú tối đa 500 ký tự");
            LocalDate date=parseDate(d.ngayLam(),errors);
            var nv=d.maNhanVien().isBlank()||d.maNhanVien().length()>50?null:employees.findByMaNhanVien(d.maNhanVien().trim()).orElse(null);
            var ca=d.maCa().isBlank()||d.maCa().length()>50?null:shifts.findByMaCa(d.maCa().trim()).orElse(null);
            if(nv==null)errors.add("Không tìm thấy nhân viên");else if(!Integer.valueOf(1).equals(nv.getTrangThai()))errors.add("Nhân viên đang bị khóa");
            if(ca==null)errors.add("Không tìm thấy ca làm việc");else if(!Integer.valueOf(1).equals(ca.getTrangThai()))errors.add("Ca làm việc đã ngừng hoạt động");
            boolean exists=false;
            if(nv!=null&&ca!=null&&date!=null&&errors.isEmpty()){
                try{
                    QuyTacLichLamViec.khoang(date,ca);
                    var prior=schedules.findForEmployee(nv.getId(),date.minusDays(1),date.plusDays(1));
                    exists=prior.stream().anyMatch(p->p.getNgayLam().equals(date)&&p.getIdCaLamViec().getId().equals(ca.getId()));
                    if(!exists&&prior.stream().anyMatch(p->QuyTacLichLamViec.chong(date,ca,p)))errors.add("Trùng giờ với lịch nhân viên đã có (kể cả ca qua đêm)");
                }catch(AppException|DateTimeException e){errors.add("Ngày/giờ ca không hợp lệ: "+e.getMessage());}
            }
            var normalized=new Dong(d.soDong(),d.maNhanVien().trim(),d.maCa().trim(),date==null?d.ngayLam().trim():date.toString(),d.ghiChu());
            result.add(new Prepared(new KiemTraDong(normalized,nv==null?null:nv.getTenNhanVien(),ca==null?null:ca.getTenCa(),exists,errors),nv,ca,date));
        }
        for(int i=0;i<result.size();i++)for(int j=i+1;j<result.size();j++){
            var a=result.get(i);var b=result.get(j);
            if(a.employee()==null||b.employee()==null||a.shift()==null||b.shift()==null||a.date()==null||b.date()==null
                    ||!a.employee().getId().equals(b.employee().getId()))continue;
            boolean same=a.date().equals(b.date())&&a.shift().getId().equals(b.shift().getId());
            boolean overlap=false;
            try{overlap=QuyTacLichLamViec.khoang(a.date(),a.shift()).chong(QuyTacLichLamViec.khoang(b.date(),b.shift()));}catch(AppException ignored){}
            if(same||overlap){
                String message=same?"Trùng nhân viên + ca + ngày trong file":"Chồng giờ với dòng khác trong file (kể cả ca qua đêm)";
                a.row().loi().add(message+" — dòng "+b.row().duLieu().soDong());b.row().loi().add(message+" — dòng "+a.row().duLieu().soDong());
            }
        }
        return result;
    }
    private LocalDate parseDate(String raw,List<String> errors){
        String value=raw.trim();
        for(var format:List.of(DateTimeFormatter.ISO_LOCAL_DATE,DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT))){
            try{var date=LocalDate.parse(value,format);if(date.getYear()<1||date.getYear()>9999)break;return date;}catch(DateTimeParseException ignored){}
        }
        errors.add("Ngày làm bắt buộc, đúng yyyy-MM-dd hoặc dd/MM/yyyy và phải là ngày có thật");return null;
    }
    private XemTruoc summary(List<Prepared> plan){
        var rows=plan.stream().map(p->new KiemTraDong(p.row().duLieu(),p.row().tenNhanVien(),p.row().tenCa(),p.row().daTonTai(),p.row().loi().stream().distinct().toList())).toList();
        long valid=rows.stream().filter(r->r.loi().isEmpty()&&!r.daTonTai()).count();
        long prior=rows.stream().filter(r->r.loi().isEmpty()&&r.daTonTai()).count();
        return new XemTruoc(rows.size(),valid,prior,rows.size()-valid-prior,rows);
    }
}
