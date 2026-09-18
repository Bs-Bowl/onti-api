package com.bsbowl.onti.domain.space.service;

import com.bsbowl.onti.domain.space.dto.ParticipantCreateRequest;
import com.bsbowl.onti.domain.space.dto.ParticipantResponse;
import com.bsbowl.onti.domain.space.dto.ParticipantUpdateRequest;
import com.bsbowl.onti.domain.space.entity.Participant;
import com.bsbowl.onti.domain.space.entity.RecordSpace;
import com.bsbowl.onti.domain.space.repository.ParticipantRepository;
import com.bsbowl.onti.global.exception.CustomException;
import com.bsbowl.onti.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ParticipantService {

    private final ParticipantRepository participantRepository;
    private final SpaceService spaceService;

    public ParticipantService(ParticipantRepository participantRepository, SpaceService spaceService) {
        this.participantRepository = participantRepository;
        this.spaceService = spaceService;
    }

    @Transactional
    public ParticipantResponse create(String spaceId, String userId, ParticipantCreateRequest request) {
        RecordSpace space = spaceService.getOwnedSpace(spaceId, userId);
        Participant participant = Participant.builder()
                .space(space)
                .displayName(request.displayName())
                .email(request.email())
                .role(request.role())
                .build();
        return ParticipantResponse.from(participantRepository.save(participant));
    }

    public List<ParticipantResponse> list(String spaceId, String userId) {
        spaceService.getOwnedSpace(spaceId, userId);
        return participantRepository.findAllBySpaceId(spaceId).stream()
                .map(ParticipantResponse::from)
                .toList();
    }

    @Transactional
    public ParticipantResponse update(String participantId, String userId, ParticipantUpdateRequest request) {
        Participant participant = getOwnedParticipant(participantId, userId);
        participant.update(request.displayName(), request.role(), request.status(), request.photoUrl());
        return ParticipantResponse.from(participant);
    }

    @Transactional
    public void delete(String participantId, String userId) {
        participantRepository.delete(getOwnedParticipant(participantId, userId));
    }

    private Participant getOwnedParticipant(String participantId, String userId) {
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new CustomException(ErrorCode.PARTICIPANT_NOT_FOUND));
        spaceService.getOwnedSpace(participant.getSpace().getId(), userId);
        return participant;
    }
}
