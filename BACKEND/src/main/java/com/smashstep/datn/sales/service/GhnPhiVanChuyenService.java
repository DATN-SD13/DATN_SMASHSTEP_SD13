package com.smashstep.datn.sales.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.sales.dto.SalesDelivery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.*;
import java.text.Normalizer;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** GHN fee-only integration. Never creates a consignment or sends credentials to the browser. */
@Service
public class GhnPhiVanChuyenService {
    private final ObjectMapper mapper;
    private volatile HttpClient client;
    @Value("${app.ghn.token:${GHN_TOKEN:}}") private String token;
    @Value("${app.ghn.shop-id:${GHN_SHOP_ID:0}}") private int shopId;
    @Value("${app.ghn.base-url:${GHN_BASE_URL:https://online-gateway.ghn.vn}}") private String baseUrl;
    @Value("${app.ghn.grams-per-pair:${GHN_GRAMS_PER_PAIR:1000}}") private int gramsPerPair;
    private final Map<String, CachedFee> fees = new ConcurrentHashMap<>();
    private final Map<String, CachedData> locations = new ConcurrentHashMap<>();
    public GhnPhiVanChuyenService(ObjectMapper mapper) { this.mapper = mapper; }
    public record Location(String id, String ten, List<String> tenKhac) {}
    public record KhuVuc(String idTinhThanh, String tenTinhThanh, String idQuanHuyen,
                         String tenQuanHuyen, String maPhuongXa, String tenPhuongXa) {}
    public record Fee(BigDecimal phiVanChuyen, Instant hetHan, int soLuong, int khoiLuongGram, KhuVuc khuVuc) {
        public Fee(BigDecimal phi, Instant hetHan, int soLuong, int gram) {
            this(phi, hetHan, soLuong, gram, null);
        }
    }
    private record Destination(Location province, Location ward) {}
    private record CachedFee(Fee fee, long employee, String address) {}
    private record CachedData(JsonNode data, Instant expires) {}

    public void kiemTraCauHinh() {
        if (token == null || token.isBlank() || shopId <= 0 || gramsPerPair <= 0)
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Chưa cấu hình GHN: cần token và Shop ID có địa chỉ kho. Không thể lấy phí giao hàng.");
    }
    public List<Location> tinhThanh() {
        return list("/shiip/public-api/v3/master-data/province/all", null);
    }
    public List<Location> phuongXa(int province) {
        find(tinhThanh(), String.valueOf(province));
        return list("/shiip/public-api/v3/master-data/ward/all-by-province-id?province_id=" + province, province);
    }
    private List<Location> list(String path, Integer parent) {
        kiemTraCauHinh();
        List<Location> result = new ArrayList<>();
        // Fetch every page; never infer a unique match from an incomplete catalogue.
        for (int offset = 0; offset < 10000; offset += 200) {
            String page = path + (path.contains("?") ? "&" : "?") + "offset=" + offset + "&limit=200";
            CachedData cached = locations.get(page);
            JsonNode data;
            if (cached != null && cached.expires().isAfter(Instant.now())) data = cached.data();
            else {
                data = call(page, null);
                if (!data.isArray()) throw AppException.badRequest("GHN không có dữ liệu địa chỉ phù hợp");
                locations.put(page, new CachedData(data, Instant.now().plus(Duration.ofHours(6))));
            }
            for (var row : data) {
                if (row.path("status").asInt() != 1 || !row.hasNonNull("_id") || !row.hasNonNull("name")) continue;
                if (parent != null && row.path("parent_id").asInt() != parent) continue;
                List<String> aliases = new ArrayList<>();
                if (row.path("extension_names").isArray()) row.get("extension_names").forEach(v -> { if (v.isTextual() && !v.asText().isBlank()) aliases.add(v.asText()); });
                result.add(new Location(row.get("_id").asText(), row.get("name").asText(), List.copyOf(aliases)));
            }
            if (data.size() < 200) return List.copyOf(result);
        }
        throw new AppException(HttpStatus.BAD_GATEWAY, "Danh mục địa chỉ GHN vượt giới hạn. Vui lòng thử lại.");
    }
    public Fee tinhPhi(SalesDelivery delivery, int quantity, Integer provinceId, String wardCode) {
        kiemTraCauHinh();
        if (delivery == null || !delivery.apDung() || quantity < 1 || quantity > 500000)
            throw AppException.badRequest("Cần địa chỉ giao hàng và sản phẩm để tính phí GHN");
        String address = delivery.diaChi();
        if (address == null || address.isBlank()) throw AppException.badRequest("Chưa có địa chỉ giao hàng");
        var destination = resolve(address, provinceId, wardCode);
        var province = destination.province();
        var ward = destination.ward();
        int weight;
        try { weight = Math.multiplyExact(quantity, gramsPerPair); }
        catch (ArithmeticException ex) { throw AppException.badRequest("Khối lượng kiện hàng vượt giới hạn"); }
        if (weight > 50000) throw AppException.badRequest("GHN chỉ hỗ trợ kiện tối đa 50 kg. Vui lòng tách đơn giao hàng.");
        int type = weight < 20000 ? 2 : 5;
        Map<String, Object> body = new LinkedHashMap<>();
        // Preview is a dry-run only: registered ShopId supplies pickup details; no consignment is created.
        // GHN's new model accepts canonical ward/province names, with no district tier.
        body.put("is_new_to_address", true);
        body.put("to_name", delivery.tenNguoiNhan()); body.put("to_phone", delivery.soDienThoaiNguoiNhan());
        body.put("to_address", address); body.put("to_ward_name", ward.ten()); body.put("to_province_name", province.ten());
        body.put("payment_type_id", 1); body.put("required_note", "KHONGCHOXEMHANG");
        body.put("content", "Giày thể thao"); body.put("cod_amount", 0); body.put("insurance_value", 0);
        body.put("service_type_id", type); body.put("weight", weight);
        body.put("length", 35); body.put("width", 25); body.put("height", 12);
        if (type == 5) body.put("items", List.of(Map.of("name", "Giày thể thao", "quantity", quantity,
                "weight", gramsPerPair, "length", 35, "width", 25, "height", 12)));
        JsonNode preview = call("/shiip/public-api/v2/shipping-order/preview", body);
        JsonNode total = preview.get("total_fee");
        if (total == null || !total.isIntegralNumber() || total.decimalValue().signum() < 0
                || !preview.path("order_code").isTextual() || !preview.path("order_code").asText().isBlank())
            throw new AppException(HttpStatus.BAD_GATEWAY, "GHN trả phí vận chuyển không hợp lệ");
        // Customer shipping charge: nearest 1,000 VND, consistently used by quote validation and checkout.
        BigDecimal customerFee = total.decimalValue().setScale(-3, RoundingMode.HALF_UP).setScale(0);
        Fee fee = new Fee(customerFee, Instant.now().plus(Duration.ofMinutes(15)), quantity, weight,
                new KhuVuc(province.id(), province.ten(), null, null, ward.id(), ward.ten()));
        long employee = PosActor.idNhanVien();
        fees.entrySet().removeIf(e -> !e.getValue().fee().hetHan().isAfter(Instant.now()));
        if (fees.size() >= 5000) throw new AppException(HttpStatus.SERVICE_UNAVAILABLE, "Hệ thống tính phí đang bận, vui lòng thử lại");
        fees.put(key(employee, address, quantity), new CachedFee(fee, employee, address.trim()));
        return fee;
    }
    public void xacThuc(SalesDelivery delivery, int quantity) {
        if (delivery == null || !delivery.apDung()) return;
        kiemTraCauHinh();
        var cached = fees.get(key(PosActor.idNhanVien(), delivery.diaChi(), quantity));
        if (cached == null || !cached.fee().hetHan().isAfter(Instant.now()) || delivery.phiVanChuyen() == null
                || cached.fee().phiVanChuyen().compareTo(delivery.phiVanChuyen()) != 0)
            throw AppException.conflict("Phí GHN chưa được tính, đã hết hạn hoặc giỏ hàng thay đổi. Bấm cập nhật phí vận chuyển.");
    }
    private String key(long employee, String address, int quantity) { return employee + "|" + quantity + "|" + Objects.toString(address, "").trim(); }
    private Location find(List<Location> locations, String id) {
        return locations.stream().filter(l -> l.id().equals(id)).findFirst().orElseThrow(() -> AppException.badRequest("Khu vực GHN không hợp lệ"));
    }
    private Destination resolve(String address, Integer provinceId, String wardCode) {
        var province = provinceId == null ? match(tinhThanh(), address) : find(tinhThanh(), String.valueOf(provinceId));
        if (!hasPart(address, province)) throw AppException.badRequest("Tỉnh/thành không khớp địa chỉ giao hàng");
        var wards = phuongXa(Integer.parseInt(province.id()));
        var ward = wardCode == null || wardCode.isBlank() ? match(wards, address) : find(wards, wardCode);
        if (!hasPart(address, ward)) throw AppException.badRequest("Phường/xã không khớp địa chỉ giao hàng");
        return new Destination(province, ward);
    }
    private Location match(List<Location> locations, String address) {
        var found = locations.stream().filter(l -> hasPart(address, l)).toList();
        if (found.size() != 1) throw AppException.badRequest("Chưa xác định duy nhất địa chỉ 2 cấp. Bấm Chỉnh sửa và chọn Tỉnh/Thành phố, Phường/Xã.");
        return found.get(0);
    }
    private boolean hasPart(String address, Location location) {
        return Arrays.stream(address.split(",")).anyMatch(part -> normalize(part).equals(normalize(location.ten()))
                || location.tenKhac().stream().anyMatch(alias -> normalize(part).equals(normalize(alias))));
    }
    private String normalize(String s) {
        // Keep ward type: a commune must not silently match a same-named urban ward.
        return Normalizer.normalize(s.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace('đ', 'd').replaceAll("\\s+", " ");
    }
    private JsonNode call(String path, Object body) {
        try {
            if (client == null) synchronized (this) {
                if (client == null) client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            }
            var request = HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(12))
                    .header("Token", token).header("ShopId", String.valueOf(shopId)).header("Content-Type", "application/json");
            if (body == null) request.GET(); else request.POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
            var response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401 || response.statusCode() == 403)
                throw new AppException(HttpStatus.SERVICE_UNAVAILABLE, "GHN từ chối xác thực. Kiểm tra token và Shop ID trên BE.");
            JsonNode json = mapper.readTree(response.body());
            if (response.statusCode() != 200 || json == null || json.path("code").asInt() != 200 || !json.hasNonNull("data"))
                throw new AppException(HttpStatus.BAD_GATEWAY, "GHN không trả được dữ liệu. Kiểm tra cấu hình kho/khu vực và thử lại.");
            return json.get("data");
        } catch (AppException e) { throw e; }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AppException(HttpStatus.SERVICE_UNAVAILABLE, "Yêu cầu GHN bị gián đoạn"); }
        catch (Exception e) { throw new AppException(HttpStatus.BAD_GATEWAY, "Không kết nối được GHN hoặc GHN trả dữ liệu lỗi. Vui lòng thử lại."); }
    }
}
