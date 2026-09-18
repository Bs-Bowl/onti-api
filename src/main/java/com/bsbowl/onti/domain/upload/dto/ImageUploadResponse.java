package com.bsbowl.onti.domain.upload.dto;

/**
 * 업로드한 이미지를 가리키는 주소. 절대 URL이 아니라 `/uploads/{파일명}` 형태의
 * 경로만 돌려준다 — 개발 중에는 로컬 디스크, 운영에서는 S3로 저장 위치가 바뀔
 * 예정이라, 호스트가 바뀌어도 저장된 값을 고칠 필요가 없게 하기 위함이다.
 */
public record ImageUploadResponse(String url) {
}
