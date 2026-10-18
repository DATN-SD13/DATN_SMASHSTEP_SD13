package com.smashstep.datn.employee.service;
import com.smashstep.datn.employee.util.NhanVienImportExcelReader;
import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.employee.repository.*;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.*;
import static com.smashstep.datn.employee.dto.NhanVienImportDto.*;
@Component @RequiredArgsConstructor
public class NhanVienImportValidator {
    private final Validator validator;
    private final NhanVienService employees;
    private final NhanVienRepository repository;
    private final VaiTroRepository roles;
    public List<CheckedRow> check(List<CheckedRow> input) {
        if(input==null||input.isEmpty()||input.size()>NhanVienImportExcelReader.MAX_ROWS)throw AppException.badRequest("Lô nhập phải có từ 1 đến 500 nhân viên");
        Map<String,List<CheckedRow>> emails=new HashMap<>();Set<Integer> rowNumbers=new HashSet<>();List<CheckedRow> result=new ArrayList<>();
        for(var row:input) {
            List<String> errors=new ArrayList<>(row.errors());var r=row.employee();var checked=new CheckedRow(row.rowNumber(),r,errors);result.add(checked);
            if(row.rowNumber()<2||row.rowNumber()>501||!rowNumbers.add(row.rowNumber()))errors.add("Số dòng Excel không hợp lệ hoặc bị trùng");
            if(r==null){errors.add("Dữ liệu nhân viên không hợp lệ");continue;}
            r.setName(trim(r.getName()));r.setEmail(trim(r.getEmail()));r.setPhone(trim(r.getPhone()));r.setProvince(trim(r.getProvince()));r.setWard(trim(r.getWard()));r.setStreet(trim(r.getStreet()));
            validator.validate(r).stream().map(v->v.getMessage()).sorted().forEach(errors::add);
            try {employees.validateImportRules(r);}catch(AppException e){errors.add(e.getMessage());}
            if(r.getProvince()==null||r.getProvince().isBlank())errors.add("Tỉnh/Thành phố không được để trống");
            if(r.getWard()==null||r.getWard().isBlank())errors.add("Phường/Xã không được để trống");
            if(r.getRoleId()!=null && roles.findById(r.getRoleId()).filter(role->Integer.valueOf(1).equals(role.getTrangThai())).isEmpty())errors.add("Vai trò không tồn tại hoặc ngừng hoạt động");
            if(r.getEmail()!=null&&!r.getEmail().isBlank()) {
                if(!r.getEmail().matches("^\\S+@\\S+\\.\\S+$"))errors.add("Email không đúng định dạng");
                emails.computeIfAbsent(r.getEmail().toLowerCase(Locale.ROOT),k->new ArrayList<>()).add(checked);
                if(repository.existsByEmailIgnoreCase(r.getEmail()))errors.add("Email đã tồn tại trong hệ thống");
            }
        }
        for(var group:emails.values())if(group.size()>1)for(var row:group)row.errors().add("Email trùng trong file tại dòng "+group.stream().map(r->String.valueOf(r.rowNumber())).reduce((a,b)->a+", "+b).orElse(""));
        return result.stream().map(r->new CheckedRow(r.rowNumber(),r.employee(),r.errors().stream().distinct().toList())).toList();
    }
    private String trim(String value){return value==null?null:value.trim();}
    public Preview preview(String id,List<CheckedRow> rows) {long valid=rows.stream().filter(r->r.errors().isEmpty()).count();return new Preview(id,rows.size(),valid,rows.size()-valid,rows);}
}
