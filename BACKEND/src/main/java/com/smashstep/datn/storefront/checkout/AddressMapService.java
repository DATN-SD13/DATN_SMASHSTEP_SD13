package com.smashstep.datn.storefront.checkout;

import com.smashstep.datn.common.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

/** Photon geocoding for explicit searches/clicks, cached and globally rate limited. Never logs locations. */
@Service
public class AddressMapService {
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).followRedirects(HttpClient.Redirect.NEVER).build();
    private final JsonMapper json=JsonMapper.builder().build();
    private final Map<String,Cached> cache=new LinkedHashMap<>();private Instant next=Instant.EPOCH;
    public record Place(String label,String street,String ward,String province,double latitude,double longitude) {}
    private record Cached(Instant until,List<Place> places) {}
    public List<Place> search(String value){
        String query=Objects.toString(value,"").trim();
        if(query.length()<3 || query.length()>200 || query.codePoints().anyMatch(Character::isISOControl))throw AppException.badRequest("Nhập địa chỉ từ 3 đến 200 ký tự.");
        return fetch("search:"+query.toLowerCase(Locale.ROOT),"api/?limit=5&countrycode=VN&q="+URLEncoder.encode(query,StandardCharsets.UTF_8));
    }
    public List<Place> reverse(double lat,double lon){
        if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180)throw AppException.badRequest("Vị trí không hợp lệ");
        String point=String.format(Locale.ROOT,"%.7f:%.7f",lat,lon);
        return fetch("reverse:"+point,String.format(Locale.ROOT,"reverse/?limit=1&lat=%.7f&lon=%.7f",lat,lon));
    }
    private List<Place> fetch(String key,String path){
        synchronized(this){
            Instant now=Instant.now();cache.entrySet().removeIf(e->!e.getValue().until().isAfter(now));
            var saved=cache.get(key);if(saved!=null)return saved.places();
            if(next.isAfter(now))throw new AppException(HttpStatus.TOO_MANY_REQUESTS,"Vui lòng chờ một giây rồi tìm lại địa chỉ.");
            next=now.plusMillis(1100);
        }
        try{
            var request=HttpRequest.newBuilder(URI.create("https://photon.komoot.io/"+path)).timeout(Duration.ofSeconds(8))
                    .header("User-Agent","SmashStepLocal/1.0 (address selection)").header("Accept","application/json").GET().build();
            var response=http.send(request,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if(response.statusCode()!=200 || response.body().length()>1_000_000)throw new IllegalStateException();
            List<Place> results=new ArrayList<>();
            for(var feature:json.readTree(response.body()).path("features")){
                var p=feature.path("properties");var coords=feature.path("geometry").path("coordinates");
                if(coords.size()<2 || !"VN".equalsIgnoreCase(text(p,"countrycode")))continue;
                double lon=coords.get(0).asDouble(),lat=coords.get(1).asDouble();
                String street=String.join(" ",text(p,"housenumber"),text(p,"street")).trim();
                if(street.isBlank())street=text(p,"name");
                String province=first(p,"state","city"),ward=first(p,"locality","district","county");
                String label=java.util.stream.Stream.of(text(p,"name"),street,ward,text(p,"city"),province).filter(s->!s.isBlank()).distinct().reduce((a,b)->a+", "+b).orElse("Vị trí đã chọn");
                results.add(new Place(label,street,ward,province,lat,lon));
            }
            var immutable=List.copyOf(results);
            synchronized(this){if(cache.size()>=500)cache.remove(cache.keySet().iterator().next());cache.put(key,new Cached(Instant.now().plusSeconds(600),immutable));}
            return immutable;
        }catch(InterruptedException e){Thread.currentThread().interrupt();throw unavailable();}catch(Exception e){throw unavailable();}
    }
    private String text(JsonNode node,String key){var value=node.path(key);return value.isMissingNode()||value.isNull()?"":value.asText();}
    private String first(JsonNode node,String... keys){for(var key:keys){String value=text(node,key);if(!value.isBlank())return value;}return "";}
    private AppException unavailable(){return new AppException(HttpStatus.SERVICE_UNAVAILABLE,"Chưa tra được địa chỉ trên bản đồ. Bạn vẫn có thể nhập địa chỉ bằng tay.");}
}
