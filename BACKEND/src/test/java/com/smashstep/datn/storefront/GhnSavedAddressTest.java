package com.smashstep.datn.storefront;
import com.smashstep.datn.sales.service.GhnPhiVanChuyenService;
import com.smashstep.datn.storefront.checkout.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class GhnSavedAddressTest {
    private GhnPhiVanChuyenService.Location area(String id,String name,String... aliases) {
        return new GhnPhiVanChuyenService.Location(id,name,List.of(aliases));
    }
    @Test void savedAddressGetsCanonicalProvinceAndWardCodesWithoutManualReselection() {
        var carrier=mock(GhnPhiVanChuyenService.class);
        when(carrier.tinhThanh()).thenReturn(List.of(area("1","Hà Nội","Thành phố Hà Nội")));
        when(carrier.phuongXa(1)).thenReturn(List.of(area("101","Phường Đống Đa")));
        var found=new GhnShippingService(carrier).resolve(new CommerceDtos.LocationRequest("Thành phố Hà Nội","","Phường Đống Đa"));
        assertTrue(found.resolved());assertEquals(1,found.provinceId());assertEquals("101",found.wardCode());assertEquals("Hà Nội",found.province());
    }
    @Test void ambiguousProvinceDoesNotGuessOrQueryWards() {
        var carrier=mock(GhnPhiVanChuyenService.class);
        when(carrier.tinhThanh()).thenReturn(List.of(area("1","Hà Nội"),area("2","Thành phố Hà Nội")));
        assertFalse(new GhnShippingService(carrier).resolve(new CommerceDtos.LocationRequest("Hà Nội","","Phường Đống Đa")).resolved());
        verify(carrier,never()).phuongXa(anyInt());
    }
    @Test void duplicateWardNamesAreNotSilentlyPicked() {
        var carrier=mock(GhnPhiVanChuyenService.class);
        when(carrier.tinhThanh()).thenReturn(List.of(area("1","Hà Nội")));
        when(carrier.phuongXa(1)).thenReturn(List.of(area("101","Phường An Bình"),area("102","Phường An Bình")));
        assertFalse(new GhnShippingService(carrier).resolve(new CommerceDtos.LocationRequest("Hà Nội","","Phường An Bình")).resolved());
    }
    @Test void communeDoesNotMatchAnUrbanWardOfTheSameName() {
        var carrier=mock(GhnPhiVanChuyenService.class);
        when(carrier.tinhThanh()).thenReturn(List.of(area("1","Hà Nội")));
        when(carrier.phuongXa(1)).thenReturn(List.of(area("101","Phường An Bình")));
        assertFalse(new GhnShippingService(carrier).resolve(new CommerceDtos.LocationRequest("Hà Nội","","Xã An Bình")).resolved());
    }
}
