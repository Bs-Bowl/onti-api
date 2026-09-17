package com.bsbowl.onti.domain.ai.service;

import com.bsbowl.onti.domain.ai.dto.ReviewSuggestionResponse;
import com.bsbowl.onti.domain.ai.dto.StructureSuggestionResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiService {

    // TODO: 실제 AI 모델 연동 전까지는 빈 제안 목록을 반환하는 스텁
    public StructureSuggestionResponse suggestStructure(String bookId) {
        return new StructureSuggestionResponse(List.of());
    }

    // TODO: 실제 맞춤법/중복/맥락 점검 로직 연동 필요
    public ReviewSuggestionResponse reviewSection(String sectionId) {
        return new ReviewSuggestionResponse(List.of());
    }
}
