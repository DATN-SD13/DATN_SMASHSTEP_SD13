package com.smashstep.datn.auth.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Tiện ích dòng lệnh — chạy main() để in ra BCrypt hash cho việc seed DB.
 *
 * Cách dùng trong IntelliJ: chuột phải → Run 'PasswordHashGenerator.main()'
 *
 * Output ví dụ:
 *   Admin@123  →  $2a$10$xxxxx
 */
public class PasswordHashGenerator {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

        String[] passwords = { "Admin@123", "Staff@123", "Customer@123" };

        System.out.println("=== BCrypt Hashes (copy vào 05_seed_passwords.sql) ===");
        for (String pwd : passwords) {
            System.out.printf("%-20s →  %s%n", pwd, encoder.encode(pwd));
        }
    }
}
