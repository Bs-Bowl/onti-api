package com.bsbowl.onti.domain.space.service;

import com.bsbowl.onti.domain.space.dto.SpaceQuestionCreateRequest;
import com.bsbowl.onti.domain.space.entity.Participant;
import com.bsbowl.onti.domain.space.entity.RecordSpace;
import com.bsbowl.onti.domain.space.entity.SpaceQuestion;
import com.bsbowl.onti.domain.space.repository.ParticipantRepository;
import com.bsbowl.onti.domain.space.repository.SpaceQuestionRepository;
import com.bsbowl.onti.domain.user.entity.User;
import com.bsbowl.onti.global.exception.CustomException;
import com.bsbowl.onti.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpaceQuestionServiceTest {

    @Mock
    private SpaceQuestionRepository spaceQuestionRepository;
    @Mock
    private ParticipantRepository participantRepository;
    @Mock
    private SpaceService spaceService;
    @InjectMocks
    private SpaceQuestionService spaceQuestionService;

    @Test
    void create_savesQuestionWithCustomSource() {
        User user = User.builder().email("a@onti.com").password("x").name("a").build();
        RecordSpace space = RecordSpace.builder().owner(user).title("공간").build();
        ReflectionTestUtils.setField(space, "id", "space-1");
        Participant participant = Participant.builder().space(space).displayName("엄마").email("mom@onti.com").build();
        when(spaceService.getOwnedSpace("space-1", "user-1")).thenReturn(space);
        when(participantRepository.findById("participant-1")).thenReturn(Optional.of(participant));
        when(spaceQuestionRepository.save(any(SpaceQuestion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = spaceQuestionService.create("space-1", "user-1",
                new SpaceQuestionCreateRequest("가장 행복했던 순간은?", "participant-1", List.of()));

        assertThat(response.text()).isEqualTo("가장 행복했던 순간은?");
        assertThat(response.source()).isEqualTo(com.bsbowl.onti.domain.space.entity.QuestionSource.CUSTOM);
    }

    @Test
    void create_participantFromDifferentSpace_throwsParticipantNotFound() {
        User user = User.builder().email("a@onti.com").password("x").name("a").build();
        RecordSpace space = RecordSpace.builder().owner(user).title("공간").build();
        ReflectionTestUtils.setField(space, "id", "space-1");
        RecordSpace otherSpace = RecordSpace.builder().owner(user).title("다른 공간").build();
        ReflectionTestUtils.setField(otherSpace, "id", "space-2");
        Participant foreignParticipant = Participant.builder().space(otherSpace).displayName("남").email("x@onti.com").build();
        when(spaceService.getOwnedSpace("space-1", "user-1")).thenReturn(space);
        when(participantRepository.findById("participant-2")).thenReturn(Optional.of(foreignParticipant));

        assertThatThrownBy(() -> spaceQuestionService.create("space-1", "user-1",
                new SpaceQuestionCreateRequest("질문", "participant-2", List.of())))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PARTICIPANT_NOT_FOUND);
    }
}
