package com.bsbowl.onti.domain.space.service;

import com.bsbowl.onti.domain.space.dto.ParticipantCreateRequest;
import com.bsbowl.onti.domain.space.entity.Participant;
import com.bsbowl.onti.domain.space.entity.ParticipantRole;
import com.bsbowl.onti.domain.space.entity.RecordSpace;
import com.bsbowl.onti.domain.space.repository.ParticipantRepository;
import com.bsbowl.onti.domain.user.entity.User;
import com.bsbowl.onti.global.exception.CustomException;
import com.bsbowl.onti.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParticipantServiceTest {

    @Mock
    private ParticipantRepository participantRepository;
    @Mock
    private SpaceService spaceService;
    @InjectMocks
    private ParticipantService participantService;

    @Test
    void create_savesParticipantWithPendingStatus() {
        User user = User.builder().email("a@onti.com").password("x").name("a").build();
        RecordSpace space = RecordSpace.builder().owner(user).title("공간").build();
        when(spaceService.getOwnedSpace("space-1", "user-1")).thenReturn(space);
        when(participantRepository.save(any(Participant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = participantService.create("space-1", "user-1",
                new ParticipantCreateRequest("엄마", "mom@onti.com", ParticipantRole.PARTICIPANT));

        assertThat(response.displayName()).isEqualTo("엄마");
        assertThat(response.status()).isEqualTo(com.bsbowl.onti.domain.space.entity.ParticipantStatus.PENDING);
    }

    @Test
    void update_notFound_throwsParticipantNotFound() {
        when(participantRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> participantService.update("missing", "user-1",
                new com.bsbowl.onti.domain.space.dto.ParticipantUpdateRequest(null, null, null, null)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PARTICIPANT_NOT_FOUND);
    }
}
