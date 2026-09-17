package com.bsbowl.onti.domain.book.dto;

import jakarta.validation.constraints.NotBlank;

public record BookCreateRequest(@NotBlank String title, String subtitle, String description) {
}
