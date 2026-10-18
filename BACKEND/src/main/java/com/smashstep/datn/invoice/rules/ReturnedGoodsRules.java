package com.smashstep.datn.invoice.rules;
import com.smashstep.datn.invoice.entity.LichSuHoaDon;
import java.util.List;
/** Persist completion in immutable audit records, independent of delivery attempt status. */
public final class ReturnedGoodsRules {
    private ReturnedGoodsRules() {}
    public static final String PREFIX="[RETURN-GOODS:1]|";
    public static boolean handled(List<LichSuHoaDon> rows) {
        return rows.stream().anyMatch(r -> r.getTrangThai()==null && reserved(r.getGhiChu()));
    }
    public static boolean reserved(String note) { return note!=null && note.startsWith(PREFIX); }
    public static String note(String note) {
        if(!reserved(note)) return note;
        String value=note.substring(PREFIX.length());
        int separator=value.indexOf('|');
        return separator<0 ? value : value.substring(separator+1);
    }
}
