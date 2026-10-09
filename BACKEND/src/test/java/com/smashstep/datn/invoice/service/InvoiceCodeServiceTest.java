package com.smashstep.datn.invoice.service;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class InvoiceCodeServiceTest {
    @Test void maximumNumericSuffixIgnoresOtherCodesAndPadsToSix() {
        assertEquals("HD000013", InvoiceCodeService.nextCode(Arrays.asList("HD000001", "HD000012", "other", null, "HDwrong", "hd999999")));
    }
    @Test void supportsEmptyDatabaseAndMoreThanSixDigitsWithoutTruncating() {
        assertEquals("HD000001", InvoiceCodeService.nextCode(List.of()));
        assertEquals("HD1000000", InvoiceCodeService.nextCode(List.of("HD999999")));
        assertEquals("HD100000000000000000000", InvoiceCodeService.nextCode(List.of("HD99999999999999999999")));
    }
}
