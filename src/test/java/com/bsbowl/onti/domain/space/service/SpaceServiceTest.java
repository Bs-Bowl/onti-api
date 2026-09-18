package com.bsbowl.onti.domain.space.service;

import com.bsbowl.onti.domain.space.dto.SpaceCreateRequest;
import com.bsbowl.onti.domain.space.entity.RecordSpace;
import com.bsbowl.onti.domain.space.entity.SpaceKind;
import com.bsbowl.onti.domain.space.repository.RecordSpaceRepository;
import com.bsbowl.onti.domain.user.entity.User;
import com.bsbowl.onti.domain.user.repository.UserRepository;
import com.bsbowl.onti.global.exception.CustomException;
import com.bsbowl.onti.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpaceServiceTest {

    @Mock
    private RecordSpaceRepository recordSpaceRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private SpaceService spaceService;

    @Test
    void create_savesSpaceWithPersonalKind() {
        User user = User.builder().email("a@onti.com").password("x").name("a").build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(recordSpaceRepository.save(any(RecordSpace.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = spaceService.create("user-1", new SpaceCreateRequest("엄마 이야기", "부모님의 삶", null, "엄마", null));

        assertThat(response.kind()).isEqualTo(SpaceKind.PERSONAL);
        assertThat(response.title()).isEqualTo("엄마 이야기");
    }

    @Test
    void getOwnedSpace_notOwner_throwsForbidden() {
        User owner = User.builder().email("owner@onti.com").password("x").name("owner").build();
        ReflectionTestUtils.setField(owner, "id", "owner-id");
        RecordSpace space = RecordSpace.builder().owner(owner).title("공간").build();
        when(recordSpaceRepository.findById("space-1")).thenReturn(Optional.of(space));

        assertThatThrownBy(() -> spaceService.getOwnedSpace("space-1", "other-id"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void getOwnedSpace_notFound_throwsSpaceNotFound() {
        when(recordSpaceRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> spaceService.getOwnedSpace("missing", "user-1"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPACE_NOT_FOUND);
    }
}
