package com.bsbowl.onti.domain.space.service;

import com.bsbowl.onti.domain.record.entity.RecordType;
import com.bsbowl.onti.domain.space.dto.SpaceRecordCreateRequest;
import com.bsbowl.onti.domain.space.dto.SpaceRecordImageCreateRequest;
import com.bsbowl.onti.domain.space.entity.RecordSpace;
import com.bsbowl.onti.domain.space.entity.SpaceRecord;
import com.bsbowl.onti.domain.space.entity.SpaceRecordImage;
import com.bsbowl.onti.domain.space.repository.ParticipantRepository;
import com.bsbowl.onti.domain.space.repository.SpaceQuestionRepository;
import com.bsbowl.onti.domain.space.repository.SpaceRecordImageRepository;
import com.bsbowl.onti.domain.space.repository.SpaceRecordRepository;
import com.bsbowl.onti.domain.user.entity.User;
import com.bsbowl.onti.global.exception.CustomException;
import com.bsbowl.onti.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpaceRecordServiceTest {

    @Mock
    private SpaceRecordRepository spaceRecordRepository;
    @Mock
    private SpaceRecordImageRepository spaceRecordImageRepository;
    @Mock
    private ParticipantRepository participantRepository;
    @Mock
    private SpaceQuestionRepository spaceQuestionRepository;
    @Mock
    private SpaceService spaceService;
    @InjectMocks
    private SpaceRecordService spaceRecordService;

    @Test
    void create_withoutAuthorOrQuestion_savesRecord() {
        User user = User.builder().email("a@onti.com").password("x").name("a").build();
        RecordSpace space = RecordSpace.builder().owner(user).title("공간").build();
        when(spaceService.getOwnedSpace("space-1", "user-1")).thenReturn(space);
        when(spaceRecordRepository.save(any(SpaceRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(spaceRecordImageRepository.findAllBySpaceRecordIdOrderByOrderAsc(any())).thenReturn(Collections.emptyList());

        var response = spaceRecordService.create("space-1", "user-1",
                new SpaceRecordCreateRequest(RecordType.TEXT, "제목", "내용", null, null, null, "1998년 봄"));

        assertThat(response.title()).isEqualTo("제목");
        assertThat(response.occurredAt()).isEqualTo("1998년 봄");
        assertThat(response.images()).isEmpty();
    }

    @Test
    void addImage_assignsNextOrder() {
        User user = User.builder().email("a@onti.com").password("x").name("a").build();
        RecordSpace space = RecordSpace.builder().owner(user).title("공간").build();
        SpaceRecord record = SpaceRecord.builder().space(space).type(RecordType.PHOTO).build();
        when(spaceRecordRepository.findById("record-1")).thenReturn(Optional.of(record));
        when(spaceService.getOwnedSpace(any(), any())).thenReturn(space);
        when(spaceRecordImageRepository.findAllBySpaceRecordIdOrderByOrderAsc("record-1")).thenReturn(Collections.emptyList());
        when(spaceRecordImageRepository.save(any(SpaceRecordImage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = spaceRecordService.addImage("record-1", "user-1",
                new SpaceRecordImageCreateRequest("https://example.com/a.jpg", "설명"));

        assertThat(response.order()).isEqualTo(0);
        assertThat(response.url()).isEqualTo("https://example.com/a.jpg");
    }

    @Test
    void deleteImage_imageFromDifferentRecord_throwsImageNotFound() {
        User user = User.builder().email("a@onti.com").password("x").name("a").build();
        RecordSpace space = RecordSpace.builder().owner(user).title("공간").build();
        SpaceRecord record = SpaceRecord.builder().space(space).type(RecordType.PHOTO).build();
        SpaceRecord otherRecord = SpaceRecord.builder().space(space).type(RecordType.PHOTO).build();
        org.springframework.test.util.ReflectionTestUtils.setField(record, "id", "record-1");
        org.springframework.test.util.ReflectionTestUtils.setField(otherRecord, "id", "record-2");
        SpaceRecordImage foreignImage = SpaceRecordImage.builder().spaceRecord(otherRecord).url("https://x").order(0).build();
        when(spaceRecordRepository.findById("record-1")).thenReturn(Optional.of(record));
        when(spaceService.getOwnedSpace(any(), any())).thenReturn(space);
        when(spaceRecordImageRepository.findById("image-2")).thenReturn(Optional.of(foreignImage));

        assertThatThrownBy(() -> spaceRecordService.deleteImage("record-1", "image-2", "user-1"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPACE_RECORD_IMAGE_NOT_FOUND);
    }
}
