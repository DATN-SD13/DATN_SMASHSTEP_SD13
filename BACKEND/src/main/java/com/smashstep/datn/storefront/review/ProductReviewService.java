package com.smashstep.datn.storefront.review;

import com.smashstep.datn.common.exception.AppException;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductReviewService {
    private final JdbcTemplate jdbc;
    public record Input(@NotNull @Min(1) @Max(5) Integer stars,
                        @NotBlank @Size(max=2000) String comment) {}
    public record Summary(double average, long count) {}
    public record Review(long id, String customerName, int stars, String comment,
                         LocalDateTime createdAt, LocalDateTime updatedAt) {}
    public record Page(Summary summary, Map<Integer,Long> distribution, List<Review> items,
                       int page, long total) {}
    public record Mine(boolean eligible, Review review) {}
    private static final String SELECT = "select r.*, k.ten_khach_hang from danh_gia_san_pham r join khach_hang k on k.id=r.id_khach_hang ";
    private Review row(java.sql.ResultSet r, int index) throws java.sql.SQLException {
        String name = r.getString("ten_khach_hang");
        String display = name == null || name.isBlank() ? "Khách hàng" : name.strip().split("\\s+")[0] + " ***";
        return new Review(r.getLong("id"), display, r.getInt("so_sao"), r.getString("binh_luan"),
                r.getTimestamp("ngay_tao").toLocalDateTime(), r.getTimestamp("ngay_cap_nhat").toLocalDateTime());
    }
    private void product(Long id) {
        if (id == null || id <= 0 || jdbc.queryForObject("select count(*) from san_pham where id=? and trang_thai=1", Long.class, id)==0)
            throw AppException.notFound("Không tìm thấy sản phẩm đang kinh doanh");
    }
    private void customer(Long id) {
        if (jdbc.queryForObject("select count(*) from khach_hang where id=? and trang_thai=1", Long.class, id)==0)
            throw AppException.unauthorized("Tài khoản không còn hoạt động");
    }
    private boolean purchased(Long product, Long customer) {
        return jdbc.queryForObject("select count(*) from hoa_don h join hoa_don_chi_tiet d on d.id_hoa_don=h.id join san_pham_chi_tiet v on v.id=d.id_san_pham_chi_tiet where h.id_khach_hang=? and v.id_san_pham=? and h.trang_thai in (4,5) and d.so_luong>0", Long.class, customer, product)>0;
    }
    public Map<Long,Summary> summaries(List<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        var result = new HashMap<Long,Summary>();
        jdbc.query("select id_san_pham, avg(cast(so_sao as decimal(10,4))) as average, count_big(*) as total from danh_gia_san_pham where id_san_pham in (" + String.join(",", Collections.nCopies(ids.size(), "?")) + ") group by id_san_pham",
                r -> { result.put(r.getLong("id_san_pham"), new Summary(r.getDouble("average"), r.getLong("total"))); }, ids.toArray());
        return result;
    }
    @Transactional(readOnly=true, isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public Page list(Long id, int page, Integer stars) {
        product(id);
        if (page<0 || page>10000 || (stars!=null && (stars<1 || stars>5))) throw AppException.badRequest("Bộ lọc đánh giá không hợp lệ");
        var distribution = new TreeMap<Integer,Long>();
        for (int i=1;i<=5;i++) distribution.put(i,0L);
        jdbc.query("select so_sao,count_big(*) as total from danh_gia_san_pham where id_san_pham=? group by so_sao",
                r -> { distribution.put(r.getInt("so_sao"),r.getLong("total")); }, id);
        long count=distribution.values().stream().mapToLong(Long::longValue).sum();
        double avg=count==0?0:distribution.entrySet().stream().mapToDouble(e->e.getKey()*e.getValue()).sum()/count;
        String filter=stars==null?"":" and r.so_sao=?";
        var args=new ArrayList<Object>(); args.add(id); if(stars!=null)args.add(stars); args.add(page*10);
        var items=jdbc.query(SELECT+" where r.id_san_pham=?"+filter+" order by r.ngay_tao desc,r.id desc offset ? rows fetch next 10 rows only",this::row,args.toArray());
        return new Page(new Summary(avg,count),distribution,items,page,stars==null?count:distribution.get(stars));
    }
    public Mine mine(Long id, Long customer) {
        product(id); customer(customer);
        var own=jdbc.query(SELECT+" where r.id_san_pham=? and r.id_khach_hang=?",this::row,id,customer);
        return new Mine(purchased(id,customer),own.isEmpty()?null:own.get(0));
    }
    @Transactional
    public Mine save(Long id, Long customer, Input input) {
        product(id); customer(customer);
        if (!purchased(id,customer)) throw AppException.forbidden("Bạn chỉ được đánh giá sản phẩm trong đơn đã giao hoặc hoàn thành");
        String comment=input.comment().strip();
        if(comment.isBlank() || comment.codePoints().anyMatch(c -> Character.isISOControl(c) && c!='\n' && c!='\r' && c!='\t'))
            throw AppException.badRequest("Vui lòng nhập bình luận hợp lệ, tối đa 2.000 ký tự");
        // Serialize first submissions too; the unique constraint is a second safeguard.
        jdbc.queryForObject("select id from khach_hang with (updlock,holdlock) where id=?",Long.class,customer);
        int updated=jdbc.update("update danh_gia_san_pham set so_sao=?,binh_luan=?,ngay_cap_nhat=SYSDATETIME() where id_san_pham=? and id_khach_hang=?",input.stars(),comment,id,customer);
        if(updated==0)jdbc.update("insert into danh_gia_san_pham(id_san_pham,id_khach_hang,so_sao,binh_luan) values(?,?,?,?)",id,customer,input.stars(),comment);
        return mine(id,customer);
    }
}
