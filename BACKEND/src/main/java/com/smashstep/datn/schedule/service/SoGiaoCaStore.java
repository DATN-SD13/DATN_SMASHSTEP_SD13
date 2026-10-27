package com.smashstep.datn.schedule.service;
import com.smashstep.datn.schedule.entity.LichLamViec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.*;
import java.io.*;

/** Local receipts are valid only for the matching allocation, employee and database state.
 * Write before DB commit: failed I/O rolls back the DB; a rolled-back receipt is ignored.
 * Admin cannot reopen completed allocations. One persistent backend directory is required.
 */
@Service
public class SoGiaoCaStore {
    @Value("${smashstep.shift-ledger.directory:private/shift-ledger}")
    private String directory="private/shift-ledger";
    public record Phieu(LocalDateTime batDau,LocalDateTime ketThuc,BigDecimal dauCa,
                        BigDecimal mat,BigDecimal nganHang,BigDecimal khac,BigDecimal thucTe,boolean tuDong) {
        public BigDecimal duKien(){return dauCa.add(mat);}
    }
    private Path file(LichLamViec row,boolean close)throws IOException {
        Path dir=Path.of(directory).toAbsolutePath().normalize();Files.createDirectories(dir);
        return dir.resolve(row.getId()+(close?"-close":"-open")+".properties");
    }
    public Optional<Phieu> doc(LichLamViec row,boolean close) {
        if(close&&!Integer.valueOf(3).equals(row.getTrangThai()))return Optional.empty();
        if(!close&&!Set.of(1,3).contains(Objects.requireNonNullElse(row.getTrangThai(),-1)))return Optional.empty();
        try {
            var file=file(row,close);if(!Files.exists(file))return Optional.empty();
            var p=new Properties();try(var in=Files.newInputStream(file)){p.load(in);}
            if(!Objects.toString(row.getNgayTao(),"").equals(p.getProperty("allocation"))
                    || !row.getIdNhanVien().getId().toString().equals(p.getProperty("employee")))return Optional.empty();
            return Optional.of(new Phieu(LocalDateTime.parse(p.getProperty("start")),
                    p.getProperty("end","").isBlank()?null:LocalDateTime.parse(p.getProperty("end")),
                    money(p,"opening"),money(p,"cash"),money(p,"bank"),money(p,"other"),
                    p.getProperty("actual","").isBlank()?null:money(p,"actual"),Boolean.parseBoolean(p.getProperty("automatic"))));
        }catch(IOException|RuntimeException e){throw new IllegalStateException("Không đọc được sổ giao ca. Kiểm tra thư mục lưu biên bản.",e);}
    }
    private static BigDecimal money(Properties p,String key){return new BigDecimal(p.getProperty(key,"0"));}
    public void ghi(LichLamViec row,Phieu receipt,boolean close) {
        try {
            var p=new Properties();p.setProperty("allocation",Objects.toString(row.getNgayTao(),""));p.setProperty("employee",row.getIdNhanVien().getId().toString());
            p.setProperty("start",receipt.batDau().toString());p.setProperty("end",Objects.toString(receipt.ketThuc(),""));
            p.setProperty("opening",receipt.dauCa().toPlainString());p.setProperty("cash",receipt.mat().toPlainString());
            p.setProperty("bank",receipt.nganHang().toPlainString());p.setProperty("other",receipt.khac().toPlainString());
            p.setProperty("actual",receipt.thucTe()==null?"":receipt.thucTe().toPlainString());p.setProperty("automatic",Boolean.toString(receipt.tuDong()));
            var target=file(row,close);var temp=Files.createTempFile(target.getParent(),"shift-",".tmp");
            try {try(var out=Files.newOutputStream(temp)){p.store(out,"SmashStep shift receipt");}
                try{Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
                catch(AtomicMoveNotSupportedException e){Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING);}
            } finally {Files.deleteIfExists(temp);}
        }catch(IOException e){throw new IllegalStateException("Không lưu được biên bản giao ca; thao tác chưa hoàn tất.",e);}
    }
}
