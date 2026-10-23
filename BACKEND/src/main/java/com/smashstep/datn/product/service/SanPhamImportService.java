package com.smashstep.datn.product.service;

import com.smashstep.datn.auth.security.JwtAuthFilter.AuthDetails;
import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.product.dto.SanPhamImportDto.*;
import com.smashstep.datn.product.entity.SanPhamImportBatch;
import com.smashstep.datn.product.repository.SanPhamImportBatchRepository;
import com.smashstep.datn.product.exception.SanPhamImportException;
import com.smashstep.datn.product.util.SanPhamImportExcelReader;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class SanPhamImportService {
    private final SanPhamImportExcelReader reader;
    private final SanPhamImportValidator validation;
    private final ThuocTinhSanPhamService attributes;
    private final SanPhamService products;
    private final SanPhamChiTietService variants;
    private final SanPhamImportBatchRepository batches;
    private final KhoaGhiSanPham lock;
    private final EntityManager em;
    private final ObjectMapper mapper;
    private Long actor(){var a=SecurityContextHolder.getContext().getAuthentication();if(a==null||a.getAuthorities().stream().noneMatch(v->List.of("ROLE_ADMIN","ROLE_STAFF").contains(v.getAuthority()))||!(a.getDetails() instanceof AuthDetails d)||!"NHAN_VIEN".equals(d.userType())||d.userId()==null)throw AppException.forbidden("Chỉ nhân viên hoặc quản lý được nhập sản phẩm");return ((AuthDetails)a.getDetails()).userId();}
    public byte[] mau(){actor();return reader.mau(attributes.layLuaChon());}
    public XemTruoc xemTruoc(MultipartFile file){actor();return validation.kiemTra(UUID.randomUUID().toString(),reader.doc(file));}
    public KetQua ketQua(String id){Long user=actor();var batch=batches.findById(id(id)).orElseThrow(()->AppException.notFound("Không tìm thấy lô nhập"));return owned(batch,user);}
    private KetQua owned(SanPhamImportBatch batch,Long user){if(!user.equals(batch.getNguoiTao()))throw AppException.forbidden("Không có quyền xem lô nhập này");return mapper.readValue(batch.getKetQuaJson(),KetQua.class);}
    @Transactional
    public KetQua xacNhan(XacNhan request){
        Long user=actor();if(request==null||request.dong()==null||request.dong().isEmpty()||request.dong().size()>500||request.dong().stream().anyMatch(Objects::isNull))throw AppException.badRequest("Nhập từ 1 đến 500 dòng");
        String batchId=id(request.maLo());String hash=hash(request.dong());
        // Same lock as manual product creation: reserves next SP code and serializes batch retries.
        lock.khoa("products");
        var previous=batches.findById(batchId);if(previous.isPresent()){KetQua result=owned(previous.get(),user);if(!hash.equals(previous.get().getPayloadHash()))throw AppException.conflict("Mã lô đã được sử dụng cho dữ liệu khác");return result;}
        for(String type:List.of("brands","categories","collars","colors","materials","origins","sizes","styles"))lock.khoa("attributes:"+type);
        lock.khoa("variants");
        var raw=request.dong().stream().map(d->new KiemTraDong(d.soDong(),d.giaTri(),List.of())).toList();
        XemTruoc checked=validation.kiemTra(batchId,raw);if(checked.coLoi()>0)throw new SanPhamImportException(checked);
        Map<String,List<SanPhamImportValidator.DuLieu>> groups=new LinkedHashMap<>();for(var row:validation.docTatCa(request.dong()))groups.computeIfAbsent(SanPhamImportValidator.nhom(row.dong().giaTri().get(0)),k->new ArrayList<>()).add(row);
        BigInteger next=BigInteger.ZERO;
        for(String code:em.createQuery("select s.maSanPham from SanPham s",String.class).getResultList()){if(code!=null&&code.matches("(?i)SP[0-9]+")){BigInteger number=new BigInteger(code.substring(2));if(number.compareTo(next)>0)next=number;}}
        List<SanPhamDaTao> created=new ArrayList<>();
        for(var group:groups.values()){
            next=next.add(BigInteger.ONE);String digits=next.toString();String code="SP"+"0".repeat(Math.max(0,3-digits.length()))+digits;if(code.length()>50)throw AppException.badRequest("Không thể sinh mã sản phẩm tiếp theo");
            try{
                var data=group.get(0).sanPham();data.setMaSanPham(code);
                var product=products.themSanPham(data);
                var variantData=group.stream().map(row->{var b=row.bienThe();String vcode=code+"-"+b.getMauSacId()+"-"+b.getKichThuocId();b.setMaChiTietSanPham(vcode);b.setSku(vcode);return b;}).toList();
                variants.themDanhSachBienThe(product.getId(),variantData);
                created.add(new SanPhamDaTao(group.get(0).dong().giaTri().get(0).trim(),product.getId(),code,group.size()));
            }catch(AppException e){Set<Integer> lines=new HashSet<>();group.forEach(r->lines.add(r.dong().soDong()));throw new SanPhamImportException(validation.tongHop(batchId,raw.stream().map(r->lines.contains(r.soDong())?new KiemTraDong(r.soDong(),r.giaTri(),List.of("Không thể lưu: "+e.getMessage())):r).toList()));}
        }
        em.flush();
        KetQua result=new KetQua(batchId,LocalDateTime.now(),created.size(),request.dong().size(),created);
        var receipt=new SanPhamImportBatch();receipt.setId(batchId);receipt.setNguoiTao(user);receipt.setPayloadHash(hash);receipt.setNgayTao(result.ngayTao());receipt.setKetQuaJson(mapper.writeValueAsString(result));batches.saveAndFlush(receipt);
        return result;
    }
    private static String id(String value){try{String canonical=UUID.fromString(value).toString();if(!canonical.equalsIgnoreCase(value))throw new IllegalArgumentException();return canonical;}catch(IllegalArgumentException|NullPointerException e){throw AppException.badRequest("Mã lô không hợp lệ");}}
    private static String hash(List<Dong> rows){StringBuilder s=new StringBuilder();for(var row:rows){s.append(row.soDong()).append(';');if(row.giaTri()==null||row.giaTri().size()!=16||row.giaTri().stream().anyMatch(Objects::isNull))throw AppException.badRequest("Dòng phải có đúng 16 ô");for(var cell:row.giaTri()){if(cell.length()>10000)throw AppException.badRequest("Mỗi ô tối đa 10.000 ký tự");s.append(cell.length()).append(':').append(cell);}}try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.toString().getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
