package com.smashstep.datn.common.validation;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

/** Shared format rules for registration, login and customer/employee profiles. */
public final class AccountRules {
    private AccountRules() {}
    public enum Kind { EMAIL, NAME, PHONE, LOGIN, PASSWORD, ADDRESS }
    private static final Map<String,String> EMAIL_TYPOS = Map.ofEntries(
            Map.entry("gmai.com", "gmail.com"), Map.entry("gmial.com", "gmail.com"),
            Map.entry("gamil.com", "gmail.com"), Map.entry("gmail.con", "gmail.com"),
            Map.entry("gmail.comm", "gmail.com"), Map.entry("yahoo.con", "yahoo.com"),
            Map.entry("hotmial.com", "hotmail.com"), Map.entry("outlok.com", "outlook.com"));

    public static String error(Kind kind, String raw) {
        if (raw == null || raw.isEmpty()) return null; // required fields also use @NotBlank
        String value = raw.trim();
        if (kind == Kind.PASSWORD) return passwordError(raw);
        if (raw.codePoints().anyMatch(Character::isISOControl)) return "Không được chứa ký tự điều khiển hoặc xuống dòng";
        return switch (kind) {
            case EMAIL -> emailError(value);
            case LOGIN -> value.contains("@") ? emailError(value)
                    : value.matches("[A-Za-z0-9_][A-Za-z0-9_.-]{0,63}") ? null
                    : "Tên đăng nhập tối đa 64 ký tự, chỉ gồm chữ không dấu, số, dấu chấm, gạch dưới hoặc gạch ngang";
            case NAME -> value.length() <= 255 && value.codePoints().filter(Character::isLetter).count() >= 2
                    && value.matches("[\\p{L}\\p{M}]+(?:[ '.-][\\p{L}\\p{M}]+)*\\.?") ? null
                    : "Họ tên cần ít nhất 2 chữ cái; không chứa số, biểu tượng hoặc ký tự đặc biệt";
            case PHONE -> value.isEmpty() || value.matches("0[35789][0-9]{8}") ? null
                    : "Số điện thoại phải gồm 10 chữ số, bắt đầu bằng 03, 05, 07, 08 hoặc 09";
            case ADDRESS -> !value.isEmpty() && !value.matches(".*[<>].*") ? null : "Địa chỉ không được để trống hoặc chứa ký tự < >";
            default -> null;
        };
    }

    private static String emailError(String value) {
        if (value.length() > 254 || !value.matches("[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)+"))
            return "Email phải đúng định dạng, ví dụ ten@gmail.com; không có khoảng trắng, hai dấu @ hoặc dấu chấm liên tiếp";
        String[] parts = value.split("@", -1);
        if (parts[0].length() > 64) return "Phần trước @ của email tối đa 64 ký tự";
        String domain = parts[1].toLowerCase(Locale.ROOT);
        for (String label : domain.split("\\.")) if (label.length() > 63) return "Tên miền email không hợp lệ";
        String suffix = domain.substring(domain.lastIndexOf('.') + 1);
        if (!suffix.matches("[a-z]{2,63}|xn--[a-z0-9-]{2,59}")) return "Đuôi tên miền email không hợp lệ";
        return EMAIL_TYPOS.containsKey(domain) ? "Tên miền email có thể bị gõ nhầm. Bạn có muốn dùng " + EMAIL_TYPOS.get(domain) + "? Vui lòng nhập lại." : null;
    }

    private static String passwordError(String value) {
        if (value.length() < 6 || value.getBytes(StandardCharsets.UTF_8).length > 72)
            return "Mật khẩu cần ít nhất 6 ký tự và tối đa 72 byte; hãy rút ngắn nếu dùng ký tự có dấu";
        if (!value.equals(value.strip()) || value.codePoints().anyMatch(Character::isISOControl))
            return "Mật khẩu không được có khoảng trắng ở đầu/cuối hoặc ký tự xuống dòng";
        if (!value.codePoints().anyMatch(Character::isLetter) || !value.codePoints().anyMatch(Character::isDigit))
            return "Mật khẩu phải có ít nhất một chữ cái và một chữ số";
        return null;
    }
}
