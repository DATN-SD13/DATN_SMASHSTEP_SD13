package com.smashstep.datn.employee.util;

import java.security.SecureRandom;

public final class PasswordGenerator {

    private static final String UPPER =
            "ABCDEFGHJKLMNPQRSTUVWXYZ";

    private static final String LOWER =
            "abcdefghijkmnopqrstuvwxyz";

    private static final String NUMBER =
            "23456789";

    private static final String SPECIAL =
            "@#$%";

    private static final String ALL =
            UPPER + LOWER + NUMBER + SPECIAL;

    private static final SecureRandom RANDOM =
            new SecureRandom();

    private PasswordGenerator() {
    }

    public static String generate() {

        StringBuilder password =
                new StringBuilder();

        /*
         * Bắt buộc ít nhất:
         * - 1 chữ hoa
         * - 1 chữ thường
         * - 1 số
         * - 1 ký tự đặc biệt
         */
        password.append(
                randomChar(UPPER)
        );

        password.append(
                randomChar(LOWER)
        );

        password.append(
                randomChar(NUMBER)
        );

        password.append(
                randomChar(SPECIAL)
        );

        /*
         * Tổng cộng 10 ký tự.
         */
        while (password.length() < 10) {

            password.append(
                    randomChar(ALL)
            );
        }

        /*
         * Trộn thứ tự ký tự.
         */
        char[] chars =
                password
                        .toString()
                        .toCharArray();

        for (
                int i = chars.length - 1;
                i > 0;
                i--
        ) {

            int j =
                    RANDOM.nextInt(
                            i + 1
                    );

            char temp =
                    chars[i];

            chars[i] =
                    chars[j];

            chars[j] =
                    temp;
        }

        return new String(
                chars
        );
    }

    private static char randomChar(
            String source
    ) {

        return source.charAt(
                RANDOM.nextInt(
                        source.length()
                )
        );
    }
}