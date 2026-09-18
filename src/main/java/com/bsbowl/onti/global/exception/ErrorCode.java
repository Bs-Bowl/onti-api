package com.bsbowl.onti.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 주소를 찾을 수 없습니다."),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "책을 찾을 수 없습니다."),
    RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "기록을 찾을 수 없습니다."),
    RECORD_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "이미지를 찾을 수 없습니다."),
    CHAPTER_NOT_FOUND(HttpStatus.NOT_FOUND, "챕터를 찾을 수 없습니다."),
    SECTION_NOT_FOUND(HttpStatus.NOT_FOUND, "섹션을 찾을 수 없습니다."),

    SPACE_NOT_FOUND(HttpStatus.NOT_FOUND, "기록 공간을 찾을 수 없습니다."),
    PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, "참여자를 찾을 수 없습니다."),
    SPACE_QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "질문을 찾을 수 없습니다."),
    SPACE_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "기록을 찾을 수 없습니다."),
    SPACE_RECORD_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "이미지를 찾을 수 없습니다."),
    BOOK_RECORD_LINK_NOT_FOUND(HttpStatus.NOT_FOUND, "연결된 기록을 찾을 수 없습니다."),
    BOOK_RECORD_LINK_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 이 책에 연결된 기록입니다."),
    INVALID_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "현재 비밀번호가 올바르지 않습니다."),

    INVALID_IMAGE_FILE(HttpStatus.BAD_REQUEST, "JPG, PNG, GIF, WEBP 이미지만 업로드할 수 있습니다."),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "이미지 한 장은 10MB까지 올릴 수 있습니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 저장에 실패했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
