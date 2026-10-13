package com.smashstep.datn.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Kiểu response chung cho MỌI API: { success, message, data }.
 * Controller luôn trả ApiResponse, không trả thẳng entity hay Map.
 */
@Getter
@AllArgsConstructor
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "Thành công", data);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static ApiResponse<Void> ok(String message) {
        return new ApiResponse<>(true, message, null);
    }

    /** Alias của ok(data) */
    public static <T> ApiResponse<T> success(T data) {
        return ok(data);
    }

    /** Generic error — caller tự suy kiểu T */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }

    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>(false, message, data);
    }
}
