package com.bsbowl.onti.domain.space.service;

import com.bsbowl.onti.domain.space.dto.SpaceRecordCreateRequest;
import com.bsbowl.onti.domain.space.dto.SpaceRecordImageCreateRequest;
import com.bsbowl.onti.domain.space.dto.SpaceRecordImageResponse;
import com.bsbowl.onti.domain.space.dto.SpaceRecordResponse;
import com.bsbowl.onti.domain.space.dto.SpaceRecordUpdateRequest;
import com.bsbowl.onti.domain.space.entity.Participant;
import com.bsbowl.onti.domain.space.entity.RecordSpace;
import com.bsbowl.onti.domain.space.entity.SpaceQuestion;
import com.bsbowl.onti.domain.space.entity.SpaceRecord;
import com.bsbowl.onti.domain.space.entity.SpaceRecordImage;
import com.bsbowl.onti.domain.space.repository.ParticipantRepository;
import com.bsbowl.onti.domain.space.repository.SpaceQuestionRepository;
import com.bsbowl.onti.domain.space.repository.SpaceRecordImageRepository;
import com.bsbowl.onti.domain.space.repository.SpaceRecordRepository;
import com.bsbowl.onti.global.exception.CustomException;
import com.bsbowl.onti.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SpaceRecordService {

    private final SpaceRecordRepository spaceRecordRepository;
    private final SpaceRecordImageRepository spaceRecordImageRepository;
    private final ParticipantRepository participantRepository;
    private final SpaceQuestionRepository spaceQuestionRepository;
    private final SpaceService spaceService;

    public SpaceRecordService(SpaceRecordRepository spaceRecordRepository,
                               SpaceRecordImageRepository spaceRecordImageRepository,
                               ParticipantRepository participantRepository,
                               SpaceQuestionRepository spaceQuestionRepository,
                               SpaceService spaceService) {
        this.spaceRecordRepository = spaceRecordRepository;
        this.spaceRecordImageRepository = spaceRecordImageRepository;
        this.participantRepository = participantRepository;
        this.spaceQuestionRepository = spaceQuestionRepository;
        this.spaceService = spaceService;
    }

    @Transactional
    public SpaceRecordResponse create(String spaceId, String userId, SpaceRecordCreateRequest request) {
        RecordSpace space = spaceService.getOwnedSpace(spaceId, userId);
        Participant author = resolveParticipant(request.authorParticipantId(), spaceId);
        SpaceQuestion answeredQuestion = resolveQuestion(request.answeredQuestionId(), spaceId);
        SpaceRecord record = SpaceRecord.builder()
                .space(space)
                .type(request.type())
                .title(request.title())
                .content(request.content())
                .author(author)
                .answeredQuestion(answeredQuestion)
                .visibility(request.visibility())
                .occurredAt(request.occurredAt())
                .build();
        return toResponse(spaceRecordRepository.save(record));
    }

    public List<SpaceRecordResponse> list(String spaceId, String userId) {
        spaceService.getOwnedSpace(spaceId, userId);
        return spaceRecordRepository.findAllBySpaceId(spaceId).stream()
                .map(this::toResponse)
                .toList();
    }

    public SpaceRecordResponse get(String recordId, String userId) {
        return toResponse(getOwnedSpaceRecord(recordId, userId));
    }

    @Transactional
    public SpaceRecordResponse update(String recordId, String userId, SpaceRecordUpdateRequest request) {
        SpaceRecord record = getOwnedSpaceRecord(recordId, userId);
        record.update(request.title(), request.content(), request.visibility(), request.occurredAt());
        return toResponse(record);
    }

    @Transactional
    public void delete(String recordId, String userId) {
        spaceRecordRepository.delete(getOwnedSpaceRecord(recordId, userId));
    }

    @Transactional
    public SpaceRecordImageResponse addImage(String recordId, String userId, SpaceRecordImageCreateRequest request) {
        SpaceRecord record = getOwnedSpaceRecord(recordId, userId);
        int nextOrder = spaceRecordImageRepository.findAllBySpaceRecordIdOrderByOrderAsc(recordId).size();
        SpaceRecordImage image = SpaceRecordImage.builder()
                .spaceRecord(record)
                .url(request.url())
                .caption(request.caption())
                .order(nextOrder)
                .build();
        return SpaceRecordImageResponse.from(spaceRecordImageRepository.save(image));
    }

    @Transactional
    public void deleteImage(String recordId, String imageId, String userId) {
        getOwnedSpaceRecord(recordId, userId);
        SpaceRecordImage image = spaceRecordImageRepository.findById(imageId)
                .orElseThrow(() -> new CustomException(ErrorCode.SPACE_RECORD_IMAGE_NOT_FOUND));
        if (!image.getSpaceRecord().getId().equals(recordId)) {
            throw new CustomException(ErrorCode.SPACE_RECORD_IMAGE_NOT_FOUND);
        }
        spaceRecordImageRepository.delete(image);
    }

    public SpaceRecord getOwnedSpaceRecord(String recordId, String userId) {
        SpaceRecord record = spaceRecordRepository.findById(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.SPACE_RECORD_NOT_FOUND));
        spaceService.getOwnedSpace(record.getSpace().getId(), userId);
        return record;
    }

    private Participant resolveParticipant(String participantId, String spaceId) {
        if (participantId == null) return null;
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new CustomException(ErrorCode.PARTICIPANT_NOT_FOUND));
        if (!participant.getSpace().getId().equals(spaceId)) {
            throw new CustomException(ErrorCode.PARTICIPANT_NOT_FOUND);
        }
        return participant;
    }

    private SpaceQuestion resolveQuestion(String questionId, String spaceId) {
        if (questionId == null) return null;
        SpaceQuestion question = spaceQuestionRepository.findById(questionId)
                .orElseThrow(() -> new CustomException(ErrorCode.SPACE_QUESTION_NOT_FOUND));
        if (!question.getSpace().getId().equals(spaceId)) {
            throw new CustomException(ErrorCode.SPACE_QUESTION_NOT_FOUND);
        }
        return question;
    }

    private SpaceRecordResponse toResponse(SpaceRecord record) {
        List<SpaceRecordImageResponse> images = spaceRecordImageRepository
                .findAllBySpaceRecordIdOrderByOrderAsc(record.getId()).stream()
                .map(SpaceRecordImageResponse::from)
                .toList();
        return SpaceRecordResponse.from(record, images);
    }
}
