package com.smashstep.datn.sales.service;
import com.smashstep.datn.invoice.entity.*;
import com.smashstep.datn.invoice.rules.PosDraftRules;
import com.smashstep.datn.invoice.service.InvoiceCodeService;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class ExpiredPosDraftService {
    private final EntityManager em;
    private final InvoiceCodeService codes;
    /** Same transaction and application lock as every POS stock writer/checkout. */
    @Transactional
    public int clean() {
        codes.lockInvoiceCreation();
        var rows = em.createQuery("select h from HoaDon h where h.loaiHoaDon = 0 and h.trangThai = 8"
                        + " and h.ngayThanhToan is null and h.ngayTao < :start"
                        + " and (locate(:pickup,h.ghiChu)=1 or locate(:delivery,h.ghiChu)=1)"
                        + " and not exists (select p.id from LichSuThanhToan p where p.idHoaDon=h) order by h.id", HoaDon.class)
                .setParameter("start", PosDayRules.startToday())
                .setParameter("pickup", PosDraftRules.PREFIX).setParameter("delivery", PosDraftRules.DELIVERY_PREFIX)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
        for (var h : rows) {
            // Includes legacy virtual reservations: helper restores only TK1 physical reservations.
            TonKhoHoaDonCho.hoanKhiHuy(em,h);
            em.createQuery("select d from HoaDonChiTiet d where d.idHoaDon=:h",HoaDonChiTiet.class)
                    .setParameter("h",h).getResultList().forEach(em::remove);
            em.createQuery("select l from LichSuHoaDon l where l.idHoaDon=:h",LichSuHoaDon.class)
                    .setParameter("h",h).getResultList().forEach(em::remove);
            em.flush(); // FK children before parent; failure rolls stock and deletion back together.
            em.remove(h);
        }
        em.flush();
        return rows.size();
    }
}
