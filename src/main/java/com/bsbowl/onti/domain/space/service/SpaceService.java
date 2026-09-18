package com.bsbowl.onti.domain.space.service;

import com.bsbowl.onti.domain.space.dto.SpaceCreateRequest;
import com.bsbowl.onti.domain.space.dto.SpaceResponse;
import com.bsbowl.onti.domain.space.dto.SpaceUpdateRequest;
import com.bsbowl.onti.domain.space.entity.RecordSpace;
import com.bsbowl.onti.domain.space.repository.RecordSpaceRepository;
import com.bsbowl.onti.domain.user.entity.User;
import com.bsbowl.onti.domain.user.repository.UserRepository;
import com.bsbowl.onti.global.exception.CustomException;
import com.bsbowl.onti.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SpaceService {

    private final RecordSpaceRepository recordSpaceRepository;
    private final UserRepository userRepository;

    public SpaceService(RecordSpaceRepository recordSpaceRepository, UserRepository userRepository) {
        this.recordSpaceRepository = recordSpaceRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public SpaceResponse create(String userId, SpaceCreateRequest request) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        RecordSpace space = RecordSpace.builder()
                .owner(owner)
                .title(request.title())
                .topic(request.topic())
                .description(request.description())
                .subjectName(request.subjectName())
                .visibility(request.visibility())
                .build();
        return SpaceResponse.from(recordSpaceRepository.save(space));
    }

    public List<SpaceResponse> list(String userId) {
        return recordSpaceRepository.findAllByOwnerId(userId).stream()
                .map(SpaceResponse::from)
                .toList();
    }

    public SpaceResponse get(String spaceId, String userId) {
        return SpaceResponse.from(getOwnedSpace(spaceId, userId));
    }

    @Transactional
    public SpaceResponse update(String spaceId, String userId, SpaceUpdateRequest request) {
        RecordSpace space = getOwnedSpace(spaceId, userId);
        space.update(request.title(), request.topic(), request.description(), request.subjectName(),
                request.kind(), request.visibility());
        return SpaceResponse.from(space);
    }

    @Transactional
    public void delete(String spaceId, String userId) {
        recordSpaceRepository.delete(getOwnedSpace(spaceId, userId));
    }

    public RecordSpace getOwnedSpace(String spaceId, String userId) {
        RecordSpace space = recordSpaceRepository.findById(spaceId)
                .orElseThrow(() -> new CustomException(ErrorCode.SPACE_NOT_FOUND));
        if (!space.isOwnedBy(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return space;
    }
}
