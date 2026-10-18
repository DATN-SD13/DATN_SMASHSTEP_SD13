package com.smashstep.datn.storefront.checkout;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.smashstep.datn.common.exception.AppException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.math.BigDecimal;
import java.util.*;

/** Server-only fee lookup. Never creates a shipment or exposes the carrier token. */
@Service
public class GhnShippingService {
    private final ObjectMapper mapper;
    private final String token, shop, base;
    private final int unitWeight;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    public GhnShippingService(ObjectMapper mapper,
            @Value("${GHN_TOKEN:}") String token, @Value("${GHN_SHOP_ID:}") String shop,
            @Value("${GHN_SANDBOX:false}") boolean sandbox,
            @Value("${GHN_ITEM_WEIGHT_GRAMS:0}") int unitWeight) {
        this.mapper=mapper; this.token=token; this.shop=shop; this.unitWeight=unitWeight;
        this.base=(sandbox ? "https://dev-online-gateway.ghn.vn" : "https://online-gateway.ghn.vn")+"/shiip/public-api";
    }
    public boolean configured() { return !token.isBlank() && shop.matches("[1-9][0-9]*") && unitWeight>0; }
    public record Area(String id, String name) {}
    public List<Area> areas(String level, Integer parent) {
        if (token.isBlank() || shop.isBlank()) throw AppException.badRequest("Chưa cấu hình GHN. Vui lòng liên hệ cửa hàng để tính phí giao hàng.");
        String suffix; String id; String name;
        if ("provinces".equals(level)) { suffix="/master-data/province"; id="ProvinceID"; name="ProvinceName"; }
        else if ("districts".equals(level) && parent!=null && parent>0) { suffix="/master-data/district?province_id="+parent; id="DistrictID"; name="DistrictName"; }
        else if ("wards".equals(level) && parent!=null && parent>0) { suffix="/master-data/ward?district_id="+parent; id="WardCode"; name="WardName"; }
        else throw AppException.badRequest("Cấp địa chỉ không hợp lệ");
        JsonNode rows=call(suffix,null);
        List<Area> result=new ArrayList<>();
        if(rows.isArray()) for(JsonNode row:rows) if(row.hasNonNull(id) && row.hasNonNull(name)) result.add(new Area(row.get(id).asText(),row.get(name).asText()));
        return result;
    }
    public BigDecimal fee(CommerceDtos.Receiver r, int quantity, BigDecimal goods) {
        if(!configured()) throw AppException.badRequest("Chưa cấu hình đủ GHN (token, mã shop, khối lượng đóng gói). Chưa thể tính phí vận chuyển.");
        if(r==null || r.provinceId()==null || r.districtId()==null || r.wardCode()==null)
            throw AppException.badRequest("Vui lòng chọn lại tỉnh, quận/huyện và phường/xã theo danh sách GHN để tính phí.");
        boolean province=areas("provinces",null).stream().anyMatch(a->a.id().equals(r.provinceId().toString()) && a.name().equals(r.province()));
        boolean district=areas("districts",r.provinceId()).stream().anyMatch(a->a.id().equals(r.districtId().toString()) && a.name().equals(r.district()));
        boolean ward=areas("wards",r.districtId()).stream().anyMatch(a->a.id().equals(r.wardCode()) && a.name().equals(r.ward()));
        if(!province || !district || !ward) throw AppException.badRequest("Địa chỉ giao hàng không khớp danh mục GHN. Vui lòng chọn lại.");
        long weight=(long)unitWeight*quantity;
        if(weight<=0 || weight>=20000) throw AppException.badRequest("Kiện hàng từ 20kg cần cửa hàng xác nhận phương án vận chuyển riêng.");
        Map<String,Object> body=new LinkedHashMap<>();
        body.put("to_district_id",r.districtId()); body.put("to_ward_code",r.wardCode());
        body.put("service_type_id",2); body.put("weight",weight);
        body.put("insurance_value",goods.setScale(0,java.math.RoundingMode.CEILING).min(new BigDecimal("5000000")).intValueExact());
        JsonNode value=call("/v2/shipping-order/fee",body).get("total");
        if(value==null || !value.isNumber() || value.decimalValue().signum()<0) throw AppException.badRequest("GHN chưa trả về phí vận chuyển hợp lệ.");
        return value.decimalValue().setScale(2,java.math.RoundingMode.HALF_UP);
    }
    private JsonNode call(String suffix, Map<String,Object> body) {
        try {
            var builder=HttpRequest.newBuilder(URI.create(base+suffix)).timeout(Duration.ofSeconds(10))
                    .header("Token",token).header("ShopId",shop).header("Content-Type","application/json");
            if(body==null) builder.GET(); else builder.POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
            var response=client.send(builder.build(),HttpResponse.BodyHandlers.ofString());
            JsonNode json=mapper.readTree(response.body());
            if(response.statusCode()!=200 || json.path("code").asInt()!=200) throw AppException.badRequest("GHN chưa tính được phí/địa chỉ. Vui lòng kiểm tra cấu hình hoặc thử lại.");
            return json.path("data");
        } catch(InterruptedException interrupted) { Thread.currentThread().interrupt(); throw AppException.badRequest("Yêu cầu GHN bị gián đoạn. Vui lòng thử lại."); }
        catch(AppException e) { throw e; }
        catch(Exception e) { throw AppException.badRequest("Không kết nối được GHN. Vui lòng thử lại sau."); }
    }
}
