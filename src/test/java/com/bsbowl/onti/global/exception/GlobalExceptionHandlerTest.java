package com.bsbowl.onti.global.exception;

import com.bsbowl.onti.global.common.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleCustomException_mapsToErrorCodeStatusAndBody() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleCustomException(new CustomException(ErrorCode.FORBIDDEN));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().error().code()).isEqualTo("FORBIDDEN");
    }

    @Test
    void handleException_returns500WithGenericError() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleException(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().error().code()).isEqualTo("INTERNAL_SERVER_ERROR");
    }
}
