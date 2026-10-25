package com.smashstep.datn.product.service;

import com.smashstep.datn.product.dto.*;
import com.smashstep.datn.product.dto.SanPhamImportDto.*;
import com.smashstep.datn.product.util.SanPhamImportExcelReader;
import com.smashstep.datn.common.exception.AppException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;

@Component @RequiredArgsConstructor
public class SanPhamImportValidator {
    private final Validator validator;
    private final ThuocTinhSanPhamService thuocTinh;
    private final SanPhamService sanPham;
    public record DuLieu(Dong dong, SanPhamThemRequest sanPham, SanPhamChiTietThemRequest bienThe, List<String> loi) {}
    public static String nhom(String value){return value.trim().toLowerCase(Locale.ROOT);}
    private static final String[] KEYS = {"categories", "brands", "materials", "styles", "collars", "origins", "colors", "sizes"};
    private static final int[] COLUMNS = {2, 3, 4, 5, 6, 7, 10, 11};

    private static String tenChuan(String value) {
        return QuyTacSanPham.normalizeBusinessName(Normalizer.normalize(value, Normalizer.Form.NFC));
    }

    // Keep all active matches: choosing the first match would silently link the wrong attribute.
    private Map<Integer, Map<String, List<Long>>> danhMucTen() {
        var options = thuocTinh.layLuaChon();
        Map<Integer, Map<String, List<Long>>> result = new HashMap<>();
        for (int i = 0; i < KEYS.length; i++) {
            Map<String, List<Long>> names = new HashMap<>();
            for (var item : options.getOrDefault(KEYS[i], List.of())) {
                if (Integer.valueOf(1).equals(item.getTrangThai()) && item.getTen() != null) {
                    names.computeIfAbsent(tenChuan(item.getTen()), key -> new ArrayList<>()).add(item.getId());
                }
            }
            result.put(COLUMNS[i], names);
        }
        return result;
    }

    private Long timId(String value, int column, Map<Integer, Map<String, List<Long>>> names, List<String> errors) {
        String name = column == 11 && QuyTacSanPham.kichCoHopLe(value)
                ? QuyTacSanPham.chuanHoaKichCo(value) : tenChuan(value);
        String label = SanPhamImportExcelReader.COT.get(column);
        if (name.isEmpty()) {
            errors.add(label + ": không được để trống");
            return null;
        }
        if (column == 11 && !QuyTacSanPham.kichCoHopLe(value)) {
            errors.add(label + ": chỉ nhận size 35–46, bước 0.5 (ví dụ 40 hoặc 40.5).");
            return null;
        }
        List<Long> matches = names.get(column).getOrDefault(name, List.of());
        if (matches.isEmpty()) {
            errors.add(label + ": '" + value.trim() + "' không tồn tại hoặc đã ngừng hoạt động. Nhập tên/giá trị theo sheet thuộc tính, không nhập ID.");
            return null;
        }
        if (matches.size() > 1) {
            errors.add(label + ": '" + value.trim() + "' trùng tên nhiều thuộc tính đang hoạt động. Sửa tên trùng trong Quản lý thuộc tính trước khi nhập.");
            return null;
        }
        return matches.get(0);
    }

    public DuLieu doc(Dong dong) {
        return doc(dong, danhMucTen());
    }

    public List<DuLieu> docTatCa(List<Dong> rows) {
        var names = danhMucTen();
        return rows.stream().map(row -> doc(row, names)).toList();
    }

    private DuLieu doc(Dong dong, Map<Integer, Map<String, List<Long>>> names) {
        var errors=new ArrayList<String>();var v=dong.giaTri();
        if(v==null||v.size()!=16||v.stream().anyMatch(Objects::isNull))throw AppException.badRequest("Dòng phải có đúng 16 cột, không có ô null");
        if(v.stream().anyMatch(s->s.length()>10000))throw AppException.badRequest("Mỗi ô tối đa 10.000 ký tự");
        String group=v.get(0).trim();if(group.isEmpty()||group.length()>50)errors.add("Nhóm sản phẩm bắt buộc, tối đa 50 ký tự");
        var p=new SanPhamThemRequest("IMPORT",v.get(1).trim(),timId(v.get(2),2,names,errors),timId(v.get(3),3,names,errors),timId(v.get(4),4,names,errors),timId(v.get(5),5,names,errors),timId(v.get(6),6,names,errors),timId(v.get(7),7,names,errors),v.get(8).trim(),flag(v.get(9),9,errors));
        Long color=timId(v.get(10),10,names,errors),size=timId(v.get(11),11,names,errors);
        Integer quantity=null;try{if(!v.get(12).trim().matches("[0-9]+"))throw new NumberFormatException();quantity=Integer.valueOf(v.get(12).trim());}catch(NumberFormatException e){errors.add("Số lượng phải là số nguyên từ 0 đến 2147483647");}
        BigDecimal price=null;try{String value=v.get(13).trim();if(!value.matches("[0-9]+(?:\\.[0-9]{1,2})?"))throw new NumberFormatException();price=new BigDecimal(value);}catch(NumberFormatException e){errors.add("Giá bán phải là số, không có dấu phân cách hàng nghìn, tối đa 2 số thập phân");}
        Integer enabled=flag(v.get(14),14,errors),status=flag(v.get(15),15,errors);
        var b=new SanPhamChiTietThemRequest(null,"IMPORT","IMPORT",color,size,quantity,price,enabled==null?null:enabled==1,status);
        validator.validate(p).stream().map(x->x.getMessage()).sorted().forEach(errors::add);
        validator.validate(b).stream().map(x->x.getMessage()).sorted().forEach(errors::add);
        return new DuLieu(dong,p,b,errors);
    }
    private Integer flag(String value, int column, List<String> errors) {
        String s = tenChuan(value);
        if (s.isEmpty() || s.equals("1")) return 1;
        if (s.equals("0")) return 0;
        boolean activation = column == 14;
        if (s.equals(activation ? "có" : "hoạt động")) return 1;
        if (s.equals(activation ? "không" : "ngừng hoạt động")) return 0;
        errors.add(SanPhamImportExcelReader.COT.get(column) + (activation
                ? ": nhập Có / Không (hoặc 1 / 0), để trống mặc định Có"
                : ": nhập Hoạt động / Ngừng hoạt động (hoặc 1 / 0), để trống mặc định Hoạt động"));
        return null;
    }
    private String common(DuLieu d){var p=d.sanPham();return String.join("|",QuyTacSanPham.normalizeBusinessName(p.getTenSanPham()),String.valueOf(p.getDanhMucId()),String.valueOf(p.getThuongHieuId()),String.valueOf(p.getChatLieuId()),String.valueOf(p.getKieuDangId()),String.valueOf(p.getCoGiayId()),String.valueOf(p.getXuatXuId()));}
    public XemTruoc kiemTra(String maLo,List<KiemTraDong> input){
        if(input==null||input.isEmpty()||input.size()>500)throw AppException.badRequest("Nhập từ 1 đến 500 dòng");
        var names=danhMucTen();
        var parsed=new ArrayList<DuLieu>();Map<String,List<DuLieu>> groups=new LinkedHashMap<>();Set<Integer> numbers=new HashSet<>();
        for(var row:input){if(row==null)throw AppException.badRequest("Dòng không hợp lệ");var d=doc(new Dong(row.soDong(),row.giaTri()),names);d.loi().addAll(row.loi());parsed.add(d);
            if(row.soDong()<2||row.soDong()>501||!numbers.add(row.soDong()))d.loi().add("Số dòng không hợp lệ hoặc bị trùng");
            groups.computeIfAbsent(nhom(row.giaTri().get(0)),k->new ArrayList<>()).add(d);
        }
        Map<String,List<DuLieu>> identities=new HashMap<>();
        for(var group:groups.values()){
            var first=group.get(0);String signature=common(first)+"|"+first.sanPham().getMoTaChiTiet()+"|"+first.sanPham().getTrangThai();
            if(group.stream().anyMatch(d->!signature.equals(common(d)+"|"+d.sanPham().getMoTaChiTiet()+"|"+d.sanPham().getTrangThai())))group.forEach(d->d.loi().add("Các dòng cùng nhóm phải giống tên, 6 thuộc tính, mô tả và trạng thái sản phẩm"));
            if(group.size()>200)group.forEach(d->d.loi().add("Mỗi sản phẩm tối đa 200 biến thể trong một lần nhập"));
            Map<String,List<DuLieu>> combinations=new HashMap<>();for(var d:group)if(d.bienThe().getMauSacId()!=null&&d.bienThe().getKichThuocId()!=null)combinations.computeIfAbsent(d.bienThe().getMauSacId()+":"+d.bienThe().getKichThuocId(),k->new ArrayList<>()).add(d);
            combinations.values().stream().filter(v->v.size()>1).forEach(v->v.forEach(d->d.loi().add("Trùng Màu sắc + Kích thước trong cùng nhóm")));
            if(group.stream().allMatch(d->d.loi().isEmpty())){
                identities.computeIfAbsent(common(first),k->new ArrayList<>()).addAll(group);
                if(sanPham.kiemTraTrung(SanPhamTrungRequest.from(first.sanPham(),null)).isDuplicate())group.forEach(d->d.loi().add("Sản phẩm cùng tên và bộ thuộc tính đã có. Không nhập đè hoặc cộng tồn qua chức năng này."));
            }
        }
        for(var same:identities.values())if(same.stream().map(d->nhom(d.dong().giaTri().get(0))).distinct().count()>1)same.forEach(d->d.loi().add("Hai nhóm trong file có cùng tên và bộ thuộc tính; hãy gộp về một nhóm"));
        var rows=parsed.stream().map(d->new KiemTraDong(d.dong().soDong(),d.dong().giaTri(),d.loi().stream().distinct().toList())).toList();
        return tongHop(maLo,rows);
    }
    public XemTruoc tongHop(String id,List<KiemTraDong> rows){long valid=rows.stream().filter(r->r.loi().isEmpty()).count();return new XemTruoc(id,SanPhamImportExcelReader.COT,rows.size(),(int)rows.stream().map(r->nhom(r.giaTri().get(0))).distinct().count(),valid,rows.size()-valid,rows);}
}
