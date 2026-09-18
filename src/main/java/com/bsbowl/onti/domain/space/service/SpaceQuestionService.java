package com.bsbowl.onti.domain.space.service;

import com.bsbowl.onti.domain.space.dto.SpaceQuestionCreateRequest;
import com.bsbowl.onti.domain.space.dto.SpaceQuestionResponse;
import com.bsbowl.onti.domain.space.dto.SpaceQuestionUpdateRequest;
import com.bsbowl.onti.domain.space.entity.Participant;
import com.bsbowl.onti.domain.space.entity.QuestionSource;
import com.bsbowl.onti.domain.space.entity.RecordSpace;
import com.bsbowl.onti.domain.space.entity.SpaceQuestion;
import com.bsbowl.onti.domain.space.repository.ParticipantRepository;
import com.bsbowl.onti.domain.space.repository.SpaceQuestionRepository;
import com.bsbowl.onti.global.exception.CustomException;
import com.bsbowl.onti.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SpaceQuestionService {

    private final SpaceQuestionRepository spaceQuestionRepository;
    private final ParticipantRepository participantRepository;
    private final SpaceService spaceService;

    public SpaceQuestionService(SpaceQuestionRepository spaceQuestionRepository,
                                 ParticipantRepository participantRepository, SpaceService spaceService) {
        this.spaceQuestionRepository = spaceQuestionRepository;
        this.participantRepository = participantRepository;
        this.spaceService = spaceService;
    }

    @Transactional
    public SpaceQuestionResponse create(String spaceId, String userId, SpaceQuestionCreateRequest request) {
        RecordSpace space = spaceService.getOwnedSpace(spaceId, userId);
        Participant createdBy = participantRepository.findById(request.createdByParticipantId())
                .orElseThrow(() -> new CustomException(ErrorCode.PARTICIPANT_NOT_FOUND));
        if (!createdBy.getSpace().getId().equals(spaceId)) {
            throw new CustomException(ErrorCode.PARTICIPANT_NOT_FOUND);
        }
        SpaceQuestion question = SpaceQuestion.builder()
                .space(space)
                .text(request.text())
                .source(QuestionSource.CUSTOM)
                .createdBy(createdBy)
                .sentToParticipantIds(request.sentToParticipantIds())
                .build();
        return SpaceQuestionResponse.from(spaceQuestionRepository.save(question));
    }

    public List<SpaceQuestionResponse> list(String spaceId, String userId) {
        spaceService.getOwnedSpace(spaceId, userId);
        return spaceQuestionRepository.findAllBySpaceId(spaceId).stream()
                .map(SpaceQuestionResponse::from)
                .toList();
    }

    @Transactional
    public SpaceQuestionResponse update(String questionId, String userId, SpaceQuestionUpdateRequest request) {
        SpaceQuestion question = getOwnedQuestion(questionId, userId);
        question.update(request.text(), request.sentToParticipantIds());
        return SpaceQuestionResponse.from(question);
    }

    @Transactional
    public void delete(String questionId, String userId) {
        spaceQuestionRepository.delete(getOwnedQuestion(questionId, userId));
    }

    private SpaceQuestion getOwnedQuestion(String questionId, String userId) {
        SpaceQuestion question = spaceQuestionRepository.findById(questionId)
                .orElseThrow(() -> new CustomException(ErrorCode.SPACE_QUESTION_NOT_FOUND));
        spaceService.getOwnedSpace(question.getSpace().getId(), userId);
        return question;
    }
}
