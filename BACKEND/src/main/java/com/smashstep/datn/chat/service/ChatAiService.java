package com.smashstep.datn.chat.service;

import com.smashstep.datn.chat.dto.ChatDto.ProductSuggestion;
import com.smashstep.datn.storefront.dto.StorefrontProductDetailResponse;
import com.smashstep.datn.storefront.dto.StorefrontProductResponse;
import com.smashstep.datn.storefront.dto.StorefrontQuery;
import com.smashstep.datn.storefront.service.StorefrontProductService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatAiService {

    public record AiReply(
            String content,
            String responseType,
            List<ProductSuggestion> products,
            List<String> suggestions
    ) {}

    private static final Set<String> STOP_WORDS = Set.of(
            "shop", "smashstep", "toi", "minh", "ban", "cho", "xem", "giup", "voi", "nhe",
            "co", "khong", "mau", "giay", "san", "pham", "gia", "bao", "nhieu", "nao", "dang",
            "can", "muon", "tim", "mua", "ho", "tro", "tu", "van", "ve", "la", "cua", "mot",
            "doi", "do", "nay", "kia", "gi"
    );

    private final StorefrontProductService storefrontProductService;
    private final EntityManager em;

    public AiReply welcome() {
        return new AiReply(
                "Xin chào. Mình là trợ lý mua sắm của SmashStep. Mình có thể tra cứu sản phẩm đang bán, giá hiện tại, khuyến mãi, các mẫu bán chạy và hỗ trợ chọn size theo nhu cầu của bạn.",
                "WELCOME",
                List.of(),
                List.of("Sản phẩm bán chạy", "Sản phẩm đang giảm giá", "Tư vấn size")
        );
    }

    public AiReply reply(String rawMessage, List<Long> recentProductIds) {
        String raw = rawMessage == null ? "" : rawMessage.trim();
        String message = normalize(raw);
        List<Long> contextIds = recentProductIds == null ? List.of() : recentProductIds;

        if (message.isBlank()) return welcome();

        if (containsAny(message, "gap nhan vien", "nhan vien", "tu van vien", "nguoi that", "ho tro truc tiep")) {
            return new AiReply(
                    "Mình có thể chuyển cuộc trò chuyện sang nhân viên tư vấn. Bạn chọn “Gặp nhân viên” bên dưới, toàn bộ nội dung trao đổi hiện tại sẽ được giữ lại để nhân viên tiếp tục hỗ trợ.",
                    "STAFF_HANDOFF",
                    List.of(),
                    List.of("Gặp nhân viên")
            );
        }

        Integer requestedSize = extractRequestedSize(raw);
        Double footLength = extractFootLength(raw);
        if (requestedSize != null && !contextIds.isEmpty()) {
            return availabilityBySize(requestedSize, contextIds);
        }
        if (footLength != null || containsAny(message, "tu van size", "chon size", "size", "kich thuoc", "chan dai")) {
            return sizeAdvice(footLength, contextIds);
        }

        if (containsAny(message, "ban chay", "hot", "noi bat", "mua nhieu", "pho bien", "best seller")) {
            return bestSellers();
        }

        if (containsAny(message, "giam gia", "khuyen mai", "sale", "uu dai", "dang giam", "dot giam gia")) {
            return discountedProducts();
        }

        BigDecimal budget = extractMaxBudget(message);
        if (budget != null) {
            return productsWithinBudget(budget);
        }

        List<ProductSuggestion> search = searchProducts(raw, 4);
        if (!search.isEmpty()) {
            return new AiReply(
                    "Mình tìm thấy các sản phẩm phù hợp với nội dung bạn đang hỏi. Giá và tồn kho bên dưới là dữ liệu hiện tại của hệ thống.",
                    "PRODUCT_SEARCH",
                    search,
                    List.of("Sản phẩm bán chạy", "Sản phẩm đang giảm giá", "Tư vấn size")
            );
        }

        if (containsAny(message, "chay bo", "running", "tap gym", "gym", "di hoc", "di lam", "di choi", "hang ngay")) {
            return needsConsultation(message);
        }

        if (containsAny(message, "doi tra", "doi size", "bao hanh", "tra hang")) {
            return new AiReply(
                    "Với yêu cầu đổi size hoặc đổi/trả, bạn nên giữ nguyên hộp, phụ kiện và thông tin đơn hàng. Điều kiện cụ thể còn phụ thuộc tình trạng sản phẩm và đơn hàng. Nếu bạn muốn kiểm tra một đơn cụ thể, mình khuyên chuyển sang nhân viên để được tra cứu chính xác.",
                    "POLICY",
                    List.of(),
                    List.of("Gặp nhân viên", "Tư vấn size")
            );
        }

        if (containsAny(message, "xin chao", "hello", "chao", "alo", "hi")) return welcome();

        return new AiReply(
                "Mình chưa xác định được chính xác sản phẩm hoặc nhu cầu từ câu hỏi này. Bạn có thể nói rõ hơn về mẫu giày, ngân sách, chiều dài bàn chân hoặc mục đích sử dụng. Mình sẽ dựa trên dữ liệu hiện có của SmashStep để tư vấn tiếp.",
                "CLARIFY",
                List.of(),
                List.of("Sản phẩm bán chạy", "Sản phẩm đang giảm giá", "Tư vấn size", "Gặp nhân viên")
        );
    }

    private AiReply bestSellers() {
        List<Object[]> rows = em.createQuery("""
                select sp.id, sum(coalesce(ct.soLuong, 0))
                from HoaDonChiTiet ct
                join ct.idHoaDon hd
                join ct.idSanPhamChiTiet v
                join v.idSanPham sp
                where hd.trangThai = 5
                  and sp.trangThai = 1
                  and v.trangThai = 1
                  and v.kichHoat = true
                group by sp.id
                order by sum(coalesce(ct.soLuong, 0)) desc
                """, Object[].class)
                .setMaxResults(6)
                .getResultList();

        List<ProductSuggestion> products = new ArrayList<>();
        for (Object[] row : rows) {
            Long productId = ((Number) row[0]).longValue();
            Long sold = ((Number) row[1]).longValue();
            ProductSuggestion card = productCard(productId, sold);
            if (card != null) products.add(card);
        }

        if (products.isEmpty()) products = featuredProducts(6);

        return new AiReply(
                products.isEmpty()
                        ? "Hiện tại mình chưa lấy được dữ liệu sản phẩm bán chạy. Bạn có thể xem danh sách sản phẩm hoặc chuyển sang nhân viên tư vấn."
                        : "Dựa trên các hóa đơn đã hoàn thành, đây là những mẫu có lượng bán tốt tại SmashStep. Bạn có thể chọn trực tiếp một sản phẩm để xem chi tiết.",
                "BEST_SELLERS",
                products,
                List.of("Sản phẩm đang giảm giá", "Tư vấn size", "Gặp nhân viên")
        );
    }

    private AiReply discountedProducts() {
        List<StorefrontProductResponse> candidates = new ArrayList<>();
        for (int page = 0; page < 4; page++) {
            StorefrontQuery query = new StorefrontQuery();
            query.setPage(page);
            query.setSize(48);
            query.setSort("featured");
            var result = storefrontProductService.list(query);
            if (result.getContent() == null || result.getContent().isEmpty()) break;
            candidates.addAll(result.getContent().stream().filter(StorefrontProductResponse::dangGiamGia).toList());
            if (page + 1 >= result.getTotalPages()) break;
        }

        candidates.sort(Comparator
                .comparing(StorefrontProductResponse::phanTramGiamCaoNhat,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(StorefrontProductResponse::giaSauGiam,
                        Comparator.nullsLast(Comparator.naturalOrder())));

        List<ProductSuggestion> products = candidates.stream()
                .limit(6)
                .map(p -> productCard(p.id(), null))
                .filter(Objects::nonNull)
                .toList();

        return new AiReply(
                products.isEmpty()
                        ? "Hiện tại hệ thống chưa ghi nhận sản phẩm nào đang được áp dụng giá khuyến mãi."
                        : "Đây là các sản phẩm đang có giá ưu đãi nổi bật. Giá gốc, giá hiện tại và mức giảm đều được lấy từ chương trình đang có hiệu lực.",
                "DISCOUNTS",
                products,
                List.of("Sản phẩm bán chạy", "Tư vấn size", "Gặp nhân viên")
        );
    }

    private AiReply productsWithinBudget(BigDecimal maxBudget) {
        StorefrontQuery query = new StorefrontQuery();
        query.setPage(0);
        query.setSize(6);
        query.setSort("priceDesc");
        query.setMaxPrice(maxBudget);
        List<ProductSuggestion> products = storefrontProductService.list(query).getContent().stream()
                .map(p -> productCard(p.id(), null))
                .filter(Objects::nonNull)
                .toList();

        String budgetText = String.format(Locale.US, "%,.0f", maxBudget).replace(',', '.');
        return new AiReply(
                products.isEmpty()
                        ? "Mình chưa thấy sản phẩm đang bán phù hợp mức ngân sách tối đa " + budgetText + "đ. Bạn có thể tăng ngân sách một chút hoặc cho mình biết thương hiệu bạn ưu tiên."
                        : "Mình đã lọc các mẫu đang bán có giá hiện tại không vượt quá " + budgetText + "đ. Mình ưu tiên những lựa chọn sát ngân sách để bạn dễ so sánh.",
                "BUDGET",
                products,
                List.of("Sản phẩm bán chạy", "Sản phẩm đang giảm giá", "Tư vấn size")
        );
    }

    private AiReply sizeAdvice(Double footLength, List<Long> contextIds) {
        if (footLength == null) {
            return new AiReply(
                    "Để tư vấn size chính xác hơn, bạn hãy đo chiều dài bàn chân từ gót đến ngón dài nhất và gửi số đo theo cm. Ví dụ: “Chân mình dài 25.5 cm”. Nếu bạn đang quan tâm một mẫu cụ thể, hãy gửi thêm tên sản phẩm.",
                    "SIZE_GUIDE",
                    recentCards(contextIds, 3),
                    List.of("Sản phẩm bán chạy", "Gặp nhân viên")
            );
        }

        int estimated = estimateEuSize(footLength);
        List<ProductSuggestion> context = recentCards(contextIds, 4);
        StringBuilder text = new StringBuilder()
                .append("Với chiều dài bàn chân khoảng ")
                .append(String.format(Locale.US, "%.1f", footLength))
                .append(" cm, size EU tham khảo là ")
                .append(estimated)
                .append(". ");

        if (!context.isEmpty()) {
            boolean found = context.stream().anyMatch(p -> p.availableSizes() != null && p.availableSizes().contains(String.valueOf(estimated)));
            text.append(found
                    ? "Trong các sản phẩm vừa gợi ý có biến thể size này đang còn hàng."
                    : "Trong các sản phẩm vừa gợi ý, mình chưa thấy biến thể size này còn hàng ở thời điểm hiện tại.");
        } else {
            text.append("Đây là mức tham khảo chung; form thực tế có thể chênh theo từng thương hiệu và mẫu giày.");
        }

        text.append(" Nếu dùng để chạy bộ hoặc thích đi thoải mái, bạn có thể cân nhắc tăng khoảng nửa đến một size tùy form.");

        return new AiReply(
                text.toString(),
                "SIZE_RESULT",
                context,
                List.of("Sản phẩm bán chạy", "Sản phẩm đang giảm giá", "Gặp nhân viên")
        );
    }

    private AiReply availabilityBySize(Integer size, List<Long> contextIds) {
        List<ProductSuggestion> recent = recentCards(contextIds, 6);
        List<ProductSuggestion> available = recent.stream()
                .filter(p -> p.availableSizes() != null && p.availableSizes().contains(String.valueOf(size)))
                .toList();

        return new AiReply(
                available.isEmpty()
                        ? "Trong các sản phẩm vừa trao đổi, mình chưa thấy size " + size + " còn hàng. Bạn có thể chọn một mẫu khác hoặc cho mình chiều dài bàn chân để mình gợi ý size phù hợp hơn."
                        : "Các sản phẩm dưới đây hiện có biến thể size " + size + " còn hàng. Bạn có thể mở sản phẩm để xem màu và biến thể chi tiết.",
                "SIZE_AVAILABILITY",
                available,
                List.of("Tư vấn size", "Sản phẩm bán chạy", "Gặp nhân viên")
        );
    }

    private AiReply needsConsultation(String normalizedMessage) {
        String need;
        if (containsAny(normalizedMessage, "chay bo", "running")) need = "chạy bộ";
        else if (containsAny(normalizedMessage, "gym", "tap gym")) need = "tập gym";
        else need = "đi hằng ngày";

        List<ProductSuggestion> products = bestSellerCards(4);
        return new AiReply(
                "Với nhu cầu " + need + ", mình cần thêm ngân sách, chiều dài bàn chân và độ rộng bàn chân để tư vấn sát hơn. Trong lúc đó, dưới đây là một số mẫu đang có lượng bán tốt để bạn tham khảo.",
                "NEED_CONSULTATION",
                products,
                List.of("Tư vấn size", "Sản phẩm đang giảm giá", "Gặp nhân viên")
        );
    }

    private List<ProductSuggestion> searchProducts(String raw, int limit) {
        String normalized = normalize(raw);
        Set<String> queryTokens = meaningfulTokens(normalized);
        if (queryTokens.isEmpty()) return List.of();

        List<Object[]> rows = em.createQuery("""
                select p.id, p.tenSanPham, th.tenThuongHieu
                from SanPham p
                left join p.idThuongHieu th
                where p.trangThai = 1
                order by p.id desc
                """, Object[].class)
                .setMaxResults(300)
                .getResultList();

        record Scored(Long id, int score) {}
        List<Scored> scored = new ArrayList<>();
        for (Object[] row : rows) {
            Long id = ((Number) row[0]).longValue();
            String name = row[1] == null ? "" : normalize(String.valueOf(row[1]));
            String brand = row[2] == null ? "" : normalize(String.valueOf(row[2]));
            int score = 0;
            if (!name.isBlank() && normalized.contains(name)) score += 20;
            if (!brand.isBlank() && normalized.contains(brand)) score += 6;
            Set<String> nameTokens = meaningfulTokens(name);
            Set<String> brandTokens = meaningfulTokens(brand);
            for (String token : queryTokens) {
                if (nameTokens.contains(token)) score += 4;
                if (brandTokens.contains(token)) score += 3;
            }
            if (score >= 4) scored.add(new Scored(id, score));
        }

        scored.sort(Comparator.comparingInt(Scored::score).reversed());
        return scored.stream()
                .limit(limit)
                .map(s -> productCard(s.id(), null))
                .filter(Objects::nonNull)
                .toList();
    }

    private List<ProductSuggestion> bestSellerCards(int limit) {
        return bestSellerRows(limit).stream()
                .map(row -> productCard(((Number) row[0]).longValue(), ((Number) row[1]).longValue()))
                .filter(Objects::nonNull)
                .toList();
    }

    private List<Object[]> bestSellerRows(int limit) {
        return em.createQuery("""
                select sp.id, sum(coalesce(ct.soLuong, 0))
                from HoaDonChiTiet ct
                join ct.idHoaDon hd
                join ct.idSanPhamChiTiet v
                join v.idSanPham sp
                where hd.trangThai = 5 and sp.trangThai = 1 and v.trangThai = 1 and v.kichHoat = true
                group by sp.id
                order by sum(coalesce(ct.soLuong, 0)) desc
                """, Object[].class)
                .setMaxResults(limit)
                .getResultList();
    }

    private List<ProductSuggestion> featuredProducts(int limit) {
        StorefrontQuery query = new StorefrontQuery();
        query.setPage(0);
        query.setSize(Math.min(limit, 48));
        query.setSort("featured");
        return storefrontProductService.list(query).getContent().stream()
                .map(p -> productCard(p.id(), null))
                .filter(Objects::nonNull)
                .toList();
    }

    private ProductSuggestion productCard(Long productId, Long soldCount) {
        try {
            StorefrontProductDetailResponse detail = storefrontProductService.detail(productId);
            StorefrontProductResponse p = detail.product();
            List<String> sizes = detail.variants().stream()
                    .filter(v -> v.soLuong() > 0)
                    .map(v -> v.tenKichThuoc() == null ? "" : v.tenKichThuoc().trim())
                    .filter(s -> !s.isBlank())
                    .distinct()
                    .sorted(this::compareSize)
                    .toList();
            return new ProductSuggestion(
                    p.id(), p.maSanPham(), p.tenSanPham(), p.tenThuongHieu(), p.tenDanhMuc(), p.anhChinh(),
                    p.giaGoc(), p.giaSauGiam(), p.phanTramGiam(), p.dangGiamGia(), p.soLuong(), soldCount, sizes
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<ProductSuggestion> recentCards(List<Long> ids, int limit) {
        if (ids == null || ids.isEmpty()) return List.of();
        return ids.stream().filter(Objects::nonNull).distinct().limit(limit)
                .map(id -> productCard(id, null)).filter(Objects::nonNull).toList();
    }

    private Integer extractRequestedSize(String raw) {
        if (raw == null) return null;
        Matcher matcher = Pattern.compile("(?i)(?:size|sz|co)\\s*(3[5-9]|4[0-6])\\b").matcher(normalize(raw));
        if (matcher.find()) return Integer.parseInt(matcher.group(1));
        return null;
    }

    private Double extractFootLength(String raw) {
        if (raw == null) return null;
        Matcher matcher = Pattern.compile("(\\d{2}(?:[\\.,]\\d{1,2})?)\\s*cm", Pattern.CASE_INSENSITIVE).matcher(raw);
        if (!matcher.find()) return null;
        try {
            double value = Double.parseDouble(matcher.group(1).replace(',', '.'));
            return value >= 20 && value <= 32 ? value : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private BigDecimal extractMaxBudget(String normalized) {
        if (!containsAny(normalized, "duoi", "khong qua", "toi da", "tam", "khoang")) return null;
        Matcher million = Pattern.compile("(\\d+(?:[\\.,]\\d+)?)\\s*(?:tr|trieu)").matcher(normalized);
        if (million.find()) {
            return new BigDecimal(million.group(1).replace(',', '.')).multiply(BigDecimal.valueOf(1_000_000));
        }
        Matcher thousand = Pattern.compile("(\\d+)\\s*k\\b").matcher(normalized);
        if (thousand.find()) return new BigDecimal(thousand.group(1)).multiply(BigDecimal.valueOf(1_000));
        Matcher plain = Pattern.compile("\\b(\\d{6,9})\\b").matcher(normalized.replace(".", ""));
        if (plain.find()) return new BigDecimal(plain.group(1));
        return null;
    }

    private int estimateEuSize(double cm) {
        if (cm < 22.5) return 35;
        if (cm < 23.0) return 36;
        if (cm < 23.5) return 37;
        if (cm < 24.2) return 38;
        if (cm < 24.8) return 39;
        if (cm < 25.4) return 40;
        if (cm < 26.0) return 41;
        if (cm < 26.7) return 42;
        if (cm < 27.4) return 43;
        if (cm < 28.0) return 44;
        if (cm < 28.7) return 45;
        return 46;
    }

    private int compareSize(String a, String b) {
        try { return Double.compare(Double.parseDouble(a), Double.parseDouble(b)); }
        catch (Exception ignored) { return a.compareToIgnoreCase(b); }
    }

    private Set<String> meaningfulTokens(String normalized) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String token : normalized.split("[^a-z0-9]+")) {
            if (token.length() >= 2 && !STOP_WORDS.contains(token)) result.add(token);
        }
        return result;
    }

    private boolean containsAny(String value, String... keys) {
        for (String key : keys) if (value.contains(normalize(key))) return true;
        return false;
    }

    private String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}
