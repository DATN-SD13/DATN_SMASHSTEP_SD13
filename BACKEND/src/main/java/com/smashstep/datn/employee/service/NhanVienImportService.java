package com.smashstep.datn.employee.service;
import com.smashstep.datn.employee.entity.NhanVienImportBatch;
import com.smashstep.datn.employee.entity.NhanVienImportMail;
import com.smashstep.datn.employee.repository.NhanVienImportBatchRepository;
import com.smashstep.datn.employee.repository.NhanVienImportMailRepository;
import com.smashstep.datn.employee.util.NhanVienImportExcelReader;
import com.smashstep.datn.employee.util.NhanVienImportMailCipher;
import com.smashstep.datn.employee.exception.NhanVienImportException;
import com.smashstep.datn.auth.security.JwtAuthFilter.AuthDetails;
import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.employee.mail.EmployeeMailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.web.multipart.MultipartFile;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDateTime;
import java.util.*;
import static com.smashstep.datn.employee.dto.NhanVienImportDto.*;
@Service @RequiredArgsConstructor
public class NhanVienImportService {
    private final NhanVienImportExcelReader excel;
    private final NhanVienImportValidator validation;
    private final NhanVienImportBatchRepository batches;
    private final NhanVienImportMailRepository mails;
    private final NhanVienService employees;
    private final NhanVienWriteLock writeLock;
    private final NhanVienImportMailCipher cipher;
    private final EmployeeMailService sender;
    private final PlatformTransactionManager transactionManager;
    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.beans.factory.annotation.Qualifier("nhanVienImportMailExecutor")
    private org.springframework.core.task.TaskExecutor mailExecutor;
    private TransactionTemplate tx(){var t=new TransactionTemplate(transactionManager);t.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);return t;}
    private Long actor() {
        var a=SecurityContextHolder.getContext().getAuthentication();
        if(a==null||a.getAuthorities().stream().noneMatch(r->r.getAuthority().equals("ROLE_ADMIN"))||!(a.getDetails() instanceof AuthDetails d)||!"NHAN_VIEN".equals(d.userType())||d.userId()==null)throw AppException.forbidden("Chỉ quản lý được nhập Excel nhân viên");
        return ((AuthDetails)a.getDetails()).userId();
    }
    public byte[] template(){actor();return excel.template(employees.layDanhSachVaiTro());}
    public Preview preview(MultipartFile file) {actor();return validation.preview(UUID.randomUUID().toString(),validation.check(excel.read(file)));}
    public Result confirm(Confirm request) {
        Long actor=actor();
        if(request==null||request.rows()==null||request.rows().isEmpty()||request.rows().size()>500||request.rows().stream().anyMatch(r->r==null||r.employee()==null))throw AppException.badRequest("Dữ liệu lô nhập không hợp lệ");
        String id=canonicalId(request.requestId()),hash=hash(request.rows());
        tx().executeWithoutResult(status->{
            writeLock.lock();
            var previous=batches.findById(id);
            if(previous.isPresent()) {owned(previous.get(),actor);if(!previous.get().getPayloadHash().equals(hash))throw AppException.conflict("Mã lô nhập đã được sử dụng cho dữ liệu khác");return;}
            var rows=validation.check(request.rows().stream().map(r->new CheckedRow(r.rowNumber(),r.employee(),List.of())).toList());
            if(rows.stream().anyMatch(r->!r.errors().isEmpty()))throw new NhanVienImportException(validation.preview(id,rows));
            NhanVienImportBatch batch=new NhanVienImportBatch();batch.setId(id);batch.setPayloadHash(hash);batch.setCreatedBy(actor);batch.setCreatedAt(LocalDateTime.now());batches.saveAndFlush(batch);
            for(var row:rows)employees.themTuExcel(row.employee(),(employee,password)->{
                NhanVienImportMail mail=new NhanVienImportMail();mail.setBatch(batch);mail.setEmployee(employee);mail.setRowNumber(row.rowNumber());mail.setState("PENDING");mail.setAttempts(0);mail.setSecretCipher(cipher.encrypt(password,employee.getMaNhanVien()));mails.save(mail);
            });
            mails.flush();
        }); // all employees, BCrypt hashes and mail jobs committed before any SMTP request
        Result result=result(id);
        dispatch(id,actor);
        return result;
    }
    public Result retry(String batchId){
        Long actor=actor();String id=canonicalId(batchId);
        tx().executeWithoutResult(s->{var b=batches.findById(id).orElseThrow(()->AppException.notFound("Không tìm thấy lô nhập"));owned(b,actor);
            for(var candidate:mails.findBatch(id)) {var m=mails.lockById(candidate.getId()).orElseThrow();if("FAILED".equals(m.getState())){m.setState("PENDING");m.setLastError(null);mails.save(m);}}
        });
        Result result=result(id);dispatch(id,actor);return result;
    }
    private void dispatch(String id,Long actor) {
        try {mailExecutor.execute(()->sendPending(id,actor));}
        catch(org.springframework.core.task.TaskRejectedException ex) { /* Durable PENDING jobs remain available for explicit retry. */ }
    }
    public Result result(String batchId) {Long actor=actor();String id=canonicalId(batchId);return tx().execute(s->{var b=batches.findById(id).orElseThrow(()->AppException.notFound("Không tìm thấy lô nhập"));owned(b,actor);return map(b);});}
    public List<Result> recent(){Long actor=actor();return tx().execute(s->batches.findTop10ByCreatedByOrderByCreatedAtDesc(actor).stream().map(this::map).toList());}
    private void owned(NhanVienImportBatch b,Long actor){if(!Objects.equals(b.getCreatedBy(),actor))throw AppException.forbidden("Bạn không có quyền xem lô nhập này");}
    private Result map(NhanVienImportBatch b) {
        List<MailResult> rows=mails.findBatch(b.getId()).stream().map(m->new MailResult(m.getId(),m.getRowNumber(),m.getEmployee().getMaNhanVien(),m.getEmployee().getTenNhanVien(),m.getEmployee().getEmail(),m.getState(),m.getAttempts(),m.getLastError())).toList();
        return new Result(b.getId(),b.getCreatedAt(),rows.size(),rows.stream().filter(r->"SENT".equals(r.state())).count(),rows.stream().filter(r->"FAILED".equals(r.state())).count(),rows.stream().filter(r->"PENDING".equals(r.state())).count(),rows);
    }
    private void sendPending(String id,Long actor) {
        List<Long> ids=tx().execute(s->{var b=batches.findById(id).orElseThrow(()->AppException.notFound("Không tìm thấy lô nhập"));owned(b,actor);return mails.findBatch(id).stream().filter(m->!"SENT".equals(m.getState())).map(NhanVienImportMail::getId).toList();});
        for(Long mailId:ids)try {
            tx().executeWithoutResult(status->{
                // A database row lock serializes concurrent confirm/retry calls across app instances.
                var mail=mails.lockById(mailId).orElseThrow(()->AppException.notFound("Không tìm thấy email"));
                if("SENT".equals(mail.getState()))return;
                mail.setAttempts(mail.getAttempts()+1);
                try {
                    var e=mail.getEmployee();if(!Integer.valueOf(1).equals(e.getTrangThai()))throw new IllegalStateException("Account disabled");
                    String password=cipher.decrypt(mail.getSecretCipher(),e.getMaNhanVien());
                    sender.sendEmployeeAccountEmail(e.getEmail(),e.getTenNhanVien(),e.getIdVaiTro()==null?"Nhân viên":e.getIdVaiTro().getTenVaiTro(),e.getTenDangNhap(),password);
                    mail.setState("SENT");mail.setSecretCipher(null);mail.setLastError(null);mail.setSentAt(LocalDateTime.now());
                } catch(RuntimeException ex) {
                    // Do not expose or log SMTP exception payloads, credentials, or temporary passwords.
                    mail.setState("FAILED");mail.setLastError("Chưa gửi được email. Kiểm tra cấu hình mail, tài khoản và khóa mã hóa rồi thử lại.");
                }
                mails.saveAndFlush(mail);
            });
        } catch(RuntimeException ex) {
            // Commit/delivery may be uncertain; keep the persisted PENDING/FAILED state for explicit retry.
            // Already-committed employees must never be created again because email delivery failed.
        }
    }
    static String canonicalId(String value) {
        try {String id=UUID.fromString(value).toString();if(!id.equalsIgnoreCase(value))throw new IllegalArgumentException();return id;}
        catch(IllegalArgumentException|NullPointerException e){throw AppException.badRequest("Mã lô nhập không hợp lệ");}
    }
    static String hash(List<Row> rows) {
        StringBuilder text=new StringBuilder();
        for(var row:rows){var r=row.employee();for(Object v:Arrays.asList(row.rowNumber(),r.getName(),r.getEmail(),r.getPhone(),r.getGender(),r.getDob(),r.getRoleId(),r.getProvince(),r.getWard(),r.getStreet(),r.getImage())){if(v==null){text.append("N;");}else{String value=v.toString();text.append("V").append(value.length()).append(':').append(value);}}}
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.toString().getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException("Missing SHA-256");}
    }
}
