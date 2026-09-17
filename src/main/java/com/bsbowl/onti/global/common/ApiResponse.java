package com.bsbowl.onti.global.common;

import com.bsbowl.onti.global.exception.ErrorCode;

public record ApiResponse<T>(boolean success, T data, ErrorResponse error) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return new ApiResponse<>(false, null, new ErrorResponse(errorCode.name(), errorCode.getMessage()));
    }
}
