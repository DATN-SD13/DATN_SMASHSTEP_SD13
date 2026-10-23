package com.smashstep.datn.storefront.checkout;

import com.smashstep.datn.invoice.entity.*;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.Locale;

public final class OrderLocation {
    public static final String PREFIX="[WEB-LOCATION:1]|";
    private OrderLocation(){}
    public static void clear(EntityManager em,HoaDon order){
        var row=new LichSuHoaDon();row.setIdHoaDon(order);row.setTrangThai(order.getTrangThai());row.setNgayTao(LocalDateTime.now());row.setGhiChu(PREFIX+"CLEAR");em.persist(row);
    }
    public static void save(EntityManager em,HoaDon order,CommerceDtos.MapPoint point){
        if(point==null)return;
        var row=new LichSuHoaDon();row.setIdHoaDon(order);row.setTrangThai(order.getTrangThai());row.setNgayTao(LocalDateTime.now());
        row.setGhiChu(PREFIX+String.format(Locale.ROOT,"%.7f|%.7f|%s",point.latitude(),point.longitude(),point.accuracy()==null?"":point.accuracy()));em.persist(row);
    }
    public static CommerceDtos.MapPoint get(EntityManager em,HoaDon order){
        var rows=em.createQuery("select l.ghiChu from LichSuHoaDon l where l.idHoaDon.id=:id and locate(:prefix,l.ghiChu)=1 order by l.id desc",String.class)
                .setParameter("id",order.getId()).setParameter("prefix",PREFIX).setMaxResults(1).getResultList();
        if(rows.isEmpty())return null;
        try{var parts=rows.get(0).substring(PREFIX.length()).split("\\|",-1);if(parts.length!=3)return null;
            double lat=Double.parseDouble(parts[0]),lon=Double.parseDouble(parts[1]);
            if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180)return null;
            return new CommerceDtos.MapPoint(lat,lon,parts[2].isBlank()?null:Double.parseDouble(parts[2]));
        }catch(RuntimeException e){return null;}
    }
}
