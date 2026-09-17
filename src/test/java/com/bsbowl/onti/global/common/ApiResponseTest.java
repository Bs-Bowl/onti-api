package com.bsbowl.onti.global.common;

import com.bsbowl.onti.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void success_wrapsDataWithSuccessTrue() {
        ApiResponse<String> response = ApiResponse.success("hello");

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isEqualTo("hello");
        assertThat(response.error()).isNull();
    }

    @Test
    void error_wrapsErrorCodeWithSuccessFalse() {
        ApiResponse<Void> response = ApiResponse.error(ErrorCode.BOOK_NOT_FOUND);

        assertThat(response.success()).isFalse();
        assertThat(response.error().code()).isEqualTo("BOOK_NOT_FOUND");
        assertThat(response.error().message()).isEqualTo("책을 찾을 수 없습니다.");
    }
}
