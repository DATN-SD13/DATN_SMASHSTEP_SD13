package com.smashstep.datn.adminai.service;

import com.smashstep.datn.adminai.dto.AdminAiDto.EmployeeInsight;
import com.smashstep.datn.adminai.dto.AdminAiDto.MetricCard;
import com.smashstep.datn.adminai.dto.AdminAiDto.OrderStatusInsight;
import com.smashstep.datn.adminai.dto.AdminAiDto.ProductInsight;
import com.smashstep.datn.adminai.dto.AdminAiDto.Reply;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAiService {

    private final EntityManager entityManager;

    public Reply ask(String rawMessage) {
        String message = normalize(rawMessage);

        if (message.isBlank() || containsAny(message, "xin chao", "chao", "hello", "hi", "alo")) {
            return welcome();
        }

        if (containsAny(message, "doanh thu", "hom nay ban duoc", "tong quan hom nay", "tinh hinh hom nay", "bao cao hom nay")) {
            return todayOverview();
        }

        if (containsAny(message, "sap het", "ton kho thap", "het hang", "can nhap", "sap het hang")) {
            return lowStock();
        }

        if (containsAny(message, "don can xu ly", "don cho", "don dang giao", "trang thai don", "hoa don cho", "don hang")) {
            return pendingOrders();
        }

        if (containsAny(message, "nhan vien", "hieu qua", "ban tot", "top nhan vien", "doanh thu nhan vien")) {
            return topEmployees();
        }

        if (containsAny(message, "danh gia", "review", "rating", "phan hoi tot", "danh gia tot")) {
            return reviewsUnavailable();
        }

        if (containsAny(message, "hot", "xu huong", "tang nhanh", "dang hot")) {
            return hotProducts();
        }

        if (containsAny(message, "ban chay", "top san pham", "san pham tot", "mua nhieu", "best seller")) {
            return bestSellers();
        }

        return new Reply(
                "Mình chưa xác định chính xác báo cáo bạn muốn xem. Bạn có thể hỏi theo các nhóm dữ liệu bên dưới; kết quả được đọc trực tiếp từ dữ liệu vận hành của SmashStep.",
                "CLARIFY",
                LocalDateTime.now(),
                List.of(), List.of(), List.of(), List.of(),
                defaultSuggestions(),
                false
        );
    }

    private Reply welcome() {
        return new Reply(
                "Xin chào quản trị viên. Mình là trợ lý quản trị SmashStep. Mình có thể tổng hợp nhanh tình hình bán hàng, sản phẩm, tồn kho, đơn cần xử lý và hiệu suất nhân viên từ dữ liệu hiện tại của hệ thống.",
                "WELCOME",
                LocalDateTime.now(),
                List.of(), List.of(), List.of(), List.of(),
                defaultSuggestions(),
                false
        );
    }

    private Reply todayOverview() {
        LocalDate today = LocalDate.now();
        Overview current = overview(today, today.plusDays(1));
        Overview previous = overview(today.minusDays(1), today);

        BigDecimal growth = percentChange(current.revenue(), previous.revenue());

        List<MetricCard> metrics = List.of(
                new MetricCard("revenue", "Doanh thu hôm nay", money(current.revenue()), growthText(growth, "hôm qua"), growth == null ? "neutral" : growth.signum() >= 0 ? "positive" : "negative"),
                new MetricCard("orders", "Đơn hoàn thành", number(current.orders()), compareCount(current.orders(), previous.orders(), "hôm qua"), "neutral"),
                new MetricCard("products", "Sản phẩm đã bán", number(current.productsSold()), "Từ các hóa đơn hoàn thành hôm nay", "neutral"),
                new MetricCard("aov", "Giá trị đơn trung bình", money(current.averageOrderValue()), "Trung bình trên mỗi đơn hoàn thành", "neutral"),
                new MetricCard("customers", "Khách hàng mới", number(current.newCustomers()), "Đăng ký trong hôm nay", "neutral")
        );

        String content = current.orders() == 0
                ? "Hôm nay chưa có hóa đơn hoàn thành. Mình vẫn đang theo dõi đơn đang xử lý, tồn kho và các chỉ số khác để bạn kiểm tra khi cần."
                : "Hôm nay SmashStep đã ghi nhận " + number(current.orders()) + " đơn hoàn thành với doanh thu " + money(current.revenue()) + ". " + revenueSentence(growth);

        return new Reply(
                content,
                "TODAY_OVERVIEW",
                LocalDateTime.now(),
                metrics,
                List.of(), List.of(), List.of(),
                List.of("Sản phẩm bán chạy", "Sản phẩm đang hot", "Đơn cần xử lý", "Sản phẩm sắp hết hàng"),
                false
        );
    }

    private Reply bestSellers() {
        LocalDate end = LocalDate.now().plusDays(1);
        LocalDate start = end.minusDays(30);
        List<ProductInsight> products = productSales(start, end);

        return new Reply(
                products.isEmpty()
                        ? "30 ngày gần đây chưa có dữ liệu bán hàng hoàn thành để xếp hạng sản phẩm."
                        : "Đây là các sản phẩm bán chạy nhất trong 30 ngày gần đây, xếp theo số lượng đã bán từ hóa đơn hoàn thành.",
                "BEST_SELLERS",
                LocalDateTime.now(),
                List.of(), products, List.of(), List.of(),
                List.of("Doanh thu hôm nay", "Sản phẩm đang hot", "Sản phẩm sắp hết hàng"),
                false
        );
    }

    private Reply hotProducts() {
        LocalDate today = LocalDate.now();
        LocalDate currentStart = today.minusDays(6);
        LocalDate currentEnd = today.plusDays(1);
        LocalDate previousStart = currentStart.minusDays(7);

        List<?> rows = entityManager.createNativeQuery("""
                SELECT TOP 6
                    sp.id,
                    sp.ma_san_pham,
                    sp.ten_san_pham,
                    COALESCE(cur.so_luong, 0) AS so_luong_hien_tai,
                    COALESCE(cur.doanh_thu, 0) AS doanh_thu_hien_tai,
                    COALESCE(prev.so_luong, 0) AS so_luong_truoc,
                    COALESCE(kho.ton_kho, 0) AS ton_kho,
                    img.url_anh
                FROM san_pham sp
                LEFT JOIN (
                    SELECT spct.id_san_pham,
                           SUM(CAST(ct.so_luong AS BIGINT)) AS so_luong,
                           SUM(COALESCE(ct.thanh_tien, 0)) AS doanh_thu
                    FROM hoa_don_chi_tiet ct
                    JOIN hoa_don hd ON hd.id = ct.id_hoa_don
                    JOIN san_pham_chi_tiet spct ON spct.id = ct.id_san_pham_chi_tiet
                    WHERE hd.trang_thai = 5
                      AND hd.ngay_thanh_toan >= :currentStart
                      AND hd.ngay_thanh_toan < :currentEnd
                    GROUP BY spct.id_san_pham
                ) cur ON cur.id_san_pham = sp.id
                LEFT JOIN (
                    SELECT spct.id_san_pham,
                           SUM(CAST(ct.so_luong AS BIGINT)) AS so_luong
                    FROM hoa_don_chi_tiet ct
                    JOIN hoa_don hd ON hd.id = ct.id_hoa_don
                    JOIN san_pham_chi_tiet spct ON spct.id = ct.id_san_pham_chi_tiet
                    WHERE hd.trang_thai = 5
                      AND hd.ngay_thanh_toan >= :previousStart
                      AND hd.ngay_thanh_toan < :currentStart
                    GROUP BY spct.id_san_pham
                ) prev ON prev.id_san_pham = sp.id
                LEFT JOIN (
                    SELECT id_san_pham, SUM(CAST(COALESCE(so_luong, 0) AS BIGINT)) AS ton_kho
                    FROM san_pham_chi_tiet
                    WHERE trang_thai = 1 AND kich_hoat = 1
                    GROUP BY id_san_pham
                ) kho ON kho.id_san_pham = sp.id
                OUTER APPLY (
                    SELECT TOP 1 ha.url_anh
                    FROM hinh_anh_san_pham ha
                    WHERE ha.id_san_pham = sp.id
                    ORDER BY CASE WHEN ha.is_anh_chinh = 1 THEN 0 ELSE 1 END, ha.id
                ) img
                WHERE sp.trang_thai = 1
                  AND COALESCE(cur.so_luong, 0) > 0
                ORDER BY
                    CASE WHEN COALESCE(prev.so_luong, 0) = 0 THEN COALESCE(cur.so_luong, 0) * 1000
                         ELSE (COALESCE(cur.so_luong, 0) - COALESCE(prev.so_luong, 0)) * 100.0 / prev.so_luong END DESC,
                    COALESCE(cur.so_luong, 0) DESC
                """)
                .setParameter("currentStart", currentStart.atStartOfDay())
                .setParameter("currentEnd", currentEnd.atStartOfDay())
                .setParameter("previousStart", previousStart.atStartOfDay())
                .getResultList();

        List<ProductInsight> products = new ArrayList<>();
        for (Object rowObj : rows) {
            Object[] row = (Object[]) rowObj;
            long sold = longValue(row[3]);
            long previousSold = longValue(row[5]);
            BigDecimal trend = previousSold == 0
                    ? (sold > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO)
                    : BigDecimal.valueOf(sold - previousSold)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(previousSold), 1, RoundingMode.HALF_UP);
            products.add(new ProductInsight(
                    longValue(row[0]), stringValue(row[1]), stringValue(row[2]), stringValue(row[7]),
                    sold, decimal(row[4]), longValue(row[6]), trend,
                    trend.signum() >= 0 ? "Tăng " + percent(trend) + " so với 7 ngày trước" : "Giảm " + percent(trend.abs()) + " so với 7 ngày trước"
            ));
        }

        return new Reply(
                products.isEmpty()
                        ? "7 ngày gần đây chưa có đủ dữ liệu bán hàng để xác định xu hướng sản phẩm."
                        : "Mình xếp hạng sản phẩm đang hot theo tốc độ bán 7 ngày gần nhất so với 7 ngày trước, sau đó ưu tiên sản phẩm có lượng bán cao.",
                "HOT_PRODUCTS",
                LocalDateTime.now(),
                List.of(), products, List.of(), List.of(),
                List.of("Sản phẩm bán chạy", "Doanh thu hôm nay", "Sản phẩm sắp hết hàng"),
                false
        );
    }

    private Reply lowStock() {
        List<?> rows = entityManager.createNativeQuery("""
                SELECT TOP 8
                    sp.id,
                    sp.ma_san_pham,
                    sp.ten_san_pham,
                    COALESCE(SUM(CASE WHEN spct.trang_thai = 1 AND spct.kich_hoat = 1 THEN CAST(COALESCE(spct.so_luong, 0) AS BIGINT) ELSE 0 END), 0) AS ton_kho,
                    img.url_anh
                FROM san_pham sp
                LEFT JOIN san_pham_chi_tiet spct ON spct.id_san_pham = sp.id
                OUTER APPLY (
                    SELECT TOP 1 ha.url_anh
                    FROM hinh_anh_san_pham ha
                    WHERE ha.id_san_pham = sp.id
                    ORDER BY CASE WHEN ha.is_anh_chinh = 1 THEN 0 ELSE 1 END, ha.id
                ) img
                WHERE sp.trang_thai = 1
                GROUP BY sp.id, sp.ma_san_pham, sp.ten_san_pham, img.url_anh
                HAVING COALESCE(SUM(CASE WHEN spct.trang_thai = 1 AND spct.kich_hoat = 1 THEN CAST(COALESCE(spct.so_luong, 0) AS BIGINT) ELSE 0 END), 0) <= 10
                ORDER BY ton_kho ASC, sp.ten_san_pham ASC
                """).getResultList();

        List<ProductInsight> products = new ArrayList<>();
        for (Object rowObj : rows) {
            Object[] row = (Object[]) rowObj;
            long stock = longValue(row[3]);
            products.add(new ProductInsight(
                    longValue(row[0]), stringValue(row[1]), stringValue(row[2]), stringValue(row[4]),
                    0, BigDecimal.ZERO, stock, null,
                    stock == 0 ? "Đã hết hàng" : "Tồn kho thấp: còn " + stock
            ));
        }

        return new Reply(
                products.isEmpty()
                        ? "Hiện không có sản phẩm đang bán nào có tổng tồn kho từ 10 trở xuống."
                        : "Các sản phẩm dưới đây có tổng tồn kho từ 10 trở xuống. Nên kiểm tra biến thể và kế hoạch nhập hàng trước khi lượng tồn giảm thêm.",
                "LOW_STOCK",
                LocalDateTime.now(),
                List.of(), products, List.of(), List.of(),
                List.of("Sản phẩm bán chạy", "Sản phẩm đang hot", "Đơn cần xử lý"),
                false
        );
    }

    private Reply pendingOrders() {
        List<?> rows = entityManager.createNativeQuery("""
                SELECT trang_thai, COUNT_BIG(*)
                FROM hoa_don
                WHERE trang_thai IN (0,1,2,3,8,9)
                GROUP BY trang_thai
                ORDER BY trang_thai
                """).getResultList();

        List<OrderStatusInsight> statuses = new ArrayList<>();
        long total = 0;
        for (Object rowObj : rows) {
            Object[] row = (Object[]) rowObj;
            int status = ((Number) row[0]).intValue();
            long count = longValue(row[1]);
            total += count;
            statuses.add(new OrderStatusInsight(status, statusLabel(status), count));
        }

        List<MetricCard> metrics = List.of(
                new MetricCard("pendingOrders", "Đơn cần theo dõi", number(total), total == 0 ? "Không có đơn ở trạng thái xử lý" : "Tổng các trạng thái chưa kết thúc", total > 0 ? "warning" : "positive")
        );

        return new Reply(
                total == 0
                        ? "Hiện không có hóa đơn ở các trạng thái đang xử lý hoặc giao thất bại."
                        : "Hiện có " + number(total) + " đơn cần theo dõi. Bạn nên ưu tiên đơn chờ xác nhận và các đơn giao hàng thất bại trước.",
                "PENDING_ORDERS",
                LocalDateTime.now(),
                metrics, List.of(), List.of(), statuses,
                List.of("Doanh thu hôm nay", "Sản phẩm sắp hết hàng", "Nhân viên hiệu quả nhất"),
                false
        );
    }

    private Reply topEmployees() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate end = today.plusDays(1);

        List<?> rows = entityManager.createNativeQuery("""
                SELECT TOP 5
                    nv.id,
                    nv.ma_nhan_vien,
                    nv.ten_nhan_vien,
                    COUNT_BIG(hd.id) AS tong_don,
                    COALESCE(SUM(hd.thanh_tien), 0) AS doanh_thu,
                    COALESCE(SUM(items.so_luong), 0) AS san_pham_ban
                FROM hoa_don hd
                JOIN nhan_vien nv ON nv.id = hd.id_nhan_vien
                LEFT JOIN (
                    SELECT id_hoa_don, SUM(CAST(so_luong AS BIGINT)) AS so_luong
                    FROM hoa_don_chi_tiet
                    GROUP BY id_hoa_don
                ) items ON items.id_hoa_don = hd.id
                WHERE hd.trang_thai = 5
                  AND hd.ngay_thanh_toan >= :startTime
                  AND hd.ngay_thanh_toan < :endTime
                GROUP BY nv.id, nv.ma_nhan_vien, nv.ten_nhan_vien
                ORDER BY doanh_thu DESC, tong_don DESC
                """)
                .setParameter("startTime", start.atStartOfDay())
                .setParameter("endTime", end.atStartOfDay())
                .getResultList();

        List<EmployeeInsight> employees = new ArrayList<>();
        for (Object rowObj : rows) {
            Object[] row = (Object[]) rowObj;
            employees.add(new EmployeeInsight(
                    longValue(row[0]), stringValue(row[1]), stringValue(row[2]),
                    longValue(row[3]), decimal(row[4]), longValue(row[5])
            ));
        }

        return new Reply(
                employees.isEmpty()
                        ? "Tháng này chưa có hóa đơn hoàn thành gắn với nhân viên để xếp hạng hiệu suất."
                        : "Đây là nhóm nhân viên có doanh thu cao nhất từ đầu tháng đến hôm nay. Xếp hạng dùng doanh thu hoàn thành, sau đó ưu tiên số đơn.",
                "TOP_EMPLOYEES",
                LocalDateTime.now(),
                List.of(), List.of(), employees, List.of(),
                List.of("Doanh thu hôm nay", "Sản phẩm bán chạy", "Đơn cần xử lý"),
                false
        );
    }

    private Reply reviewsUnavailable() {
        return new Reply(
                "Source BACKEND và Database hiện tại chưa có bảng/entity/module đánh giá sản phẩm, nên mình chưa thể đưa ra 'đánh giá tốt nhất' mà vẫn đảm bảo dữ liệu chính xác. Khi module đánh giá được merge, chỉ cần nối adapter đánh giá vào AdminAiService là widget có thể hiển thị điểm trung bình, số lượt đánh giá và phản hồi nổi bật.",
                "REVIEWS_UNAVAILABLE",
                LocalDateTime.now(),
                List.of(), List.of(), List.of(), List.of(),
                List.of("Doanh thu hôm nay", "Sản phẩm bán chạy", "Sản phẩm đang hot"),
                false
        );
    }

    private List<ProductInsight> productSales(LocalDate start, LocalDate end) {
        List<?> rows = entityManager.createNativeQuery("""
                SELECT TOP 6
                    sp.id,
                    sp.ma_san_pham,
                    sp.ten_san_pham,
                    SUM(CAST(ct.so_luong AS BIGINT)) AS so_luong_ban,
                    SUM(COALESCE(ct.thanh_tien, 0)) AS doanh_thu,
                    COALESCE(kho.ton_kho, 0) AS ton_kho,
                    img.url_anh
                FROM hoa_don_chi_tiet ct
                JOIN hoa_don hd ON hd.id = ct.id_hoa_don
                JOIN san_pham_chi_tiet spct ON spct.id = ct.id_san_pham_chi_tiet
                JOIN san_pham sp ON sp.id = spct.id_san_pham
                LEFT JOIN (
                    SELECT id_san_pham, SUM(CAST(COALESCE(so_luong, 0) AS BIGINT)) AS ton_kho
                    FROM san_pham_chi_tiet
                    WHERE trang_thai = 1 AND kich_hoat = 1
                    GROUP BY id_san_pham
                ) kho ON kho.id_san_pham = sp.id
                OUTER APPLY (
                    SELECT TOP 1 ha.url_anh
                    FROM hinh_anh_san_pham ha
                    WHERE ha.id_san_pham = sp.id
                    ORDER BY CASE WHEN ha.is_anh_chinh = 1 THEN 0 ELSE 1 END, ha.id
                ) img
                WHERE hd.trang_thai = 5
                  AND hd.ngay_thanh_toan >= :startTime
                  AND hd.ngay_thanh_toan < :endTime
                  AND sp.trang_thai = 1
                GROUP BY sp.id, sp.ma_san_pham, sp.ten_san_pham, kho.ton_kho, img.url_anh
                ORDER BY so_luong_ban DESC, doanh_thu DESC
                """)
                .setParameter("startTime", start.atStartOfDay())
                .setParameter("endTime", end.atStartOfDay())
                .getResultList();

        List<ProductInsight> products = new ArrayList<>();
        for (Object rowObj : rows) {
            Object[] row = (Object[]) rowObj;
            products.add(new ProductInsight(
                    longValue(row[0]), stringValue(row[1]), stringValue(row[2]), stringValue(row[6]),
                    longValue(row[3]), decimal(row[4]), longValue(row[5]), null,
                    "Bán " + number(longValue(row[3])) + " sản phẩm trong 30 ngày gần đây"
            ));
        }
        return products;
    }

    private Overview overview(LocalDate start, LocalDate endExclusive) {
        Object[] revenueRow = (Object[]) entityManager.createNativeQuery("""
                SELECT COUNT_BIG(hd.id), COALESCE(SUM(hd.thanh_tien), 0)
                FROM hoa_don hd
                WHERE hd.trang_thai = 5
                  AND hd.ngay_thanh_toan IS NOT NULL
                  AND hd.ngay_thanh_toan >= :startTime
                  AND hd.ngay_thanh_toan < :endTime
                """)
                .setParameter("startTime", start.atStartOfDay())
                .setParameter("endTime", endExclusive.atStartOfDay())
                .getSingleResult();

        Number productsSold = (Number) entityManager.createNativeQuery("""
                SELECT COALESCE(SUM(CAST(ct.so_luong AS BIGINT)), 0)
                FROM hoa_don_chi_tiet ct
                JOIN hoa_don hd ON hd.id = ct.id_hoa_don
                WHERE hd.trang_thai = 5
                  AND hd.ngay_thanh_toan >= :startTime
                  AND hd.ngay_thanh_toan < :endTime
                """)
                .setParameter("startTime", start.atStartOfDay())
                .setParameter("endTime", endExclusive.atStartOfDay())
                .getSingleResult();

        Number customers = (Number) entityManager.createNativeQuery("""
                SELECT COUNT_BIG(*)
                FROM khach_hang
                WHERE ngay_tao >= :startTime AND ngay_tao < :endTime
                """)
                .setParameter("startTime", start.atStartOfDay())
                .setParameter("endTime", endExclusive.atStartOfDay())
                .getSingleResult();

        long orders = longValue(revenueRow[0]);
        BigDecimal revenue = decimal(revenueRow[1]);
        BigDecimal aov = orders == 0 ? BigDecimal.ZERO : revenue.divide(BigDecimal.valueOf(orders), 0, RoundingMode.HALF_UP);
        return new Overview(revenue, orders, longValue(productsSold), longValue(customers), aov);
    }

    private List<String> defaultSuggestions() {
        return List.of(
                "Doanh thu hôm nay",
                "Sản phẩm bán chạy",
                "Sản phẩm đang hot",
                "Sản phẩm sắp hết hàng",
                "Đơn cần xử lý",
                "Nhân viên hiệu quả nhất"
        );
    }

    private String statusLabel(int status) {
        return switch (status) {
            case 0 -> "Chờ xác nhận";
            case 1 -> "Đã xác nhận";
            case 2 -> "Chờ lấy hàng";
            case 3 -> "Đang giao hàng";
            case 8 -> "Hóa đơn chờ";
            case 9 -> "Giao hàng thất bại";
            default -> "Trạng thái " + status;
        };
    }

    private BigDecimal percentChange(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) return null;
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 1, RoundingMode.HALF_UP);
    }

    private String revenueSentence(BigDecimal growth) {
        if (growth == null) return "Hôm qua chưa có doanh thu hoàn thành nên chưa thể tính tỷ lệ tăng trưởng.";
        if (growth.signum() == 0) return "Doanh thu đang bằng hôm qua.";
        return "Doanh thu " + (growth.signum() > 0 ? "tăng " : "giảm ") + percent(growth.abs()) + " so với hôm qua.";
    }

    private String growthText(BigDecimal growth, String period) {
        if (growth == null) return "Chưa đủ dữ liệu để so với " + period;
        if (growth.signum() == 0) return "Không đổi so với " + period;
        return (growth.signum() > 0 ? "Tăng " : "Giảm ") + percent(growth.abs()) + " so với " + period;
    }

    private String compareCount(long current, long previous, String period) {
        long delta = current - previous;
        if (delta == 0) return "Không đổi so với " + period;
        return (delta > 0 ? "Nhiều hơn " : "Ít hơn ") + number(Math.abs(delta)) + " đơn so với " + period;
    }

    private String money(BigDecimal value) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
        format.setMaximumFractionDigits(0);
        return format.format(value == null ? BigDecimal.ZERO : value) + " ₫";
    }

    private String number(long value) {
        return NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(value);
    }

    private String percent(BigDecimal value) {
        return value.setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "%";
    }

    private long longValue(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    private BigDecimal decimal(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(normalize(needle))) return true;
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');
        return normalized.toLowerCase(Locale.ROOT).trim();
    }

    private record Overview(
            BigDecimal revenue,
            long orders,
            long productsSold,
            long newCustomers,
            BigDecimal averageOrderValue
    ) {}
}
