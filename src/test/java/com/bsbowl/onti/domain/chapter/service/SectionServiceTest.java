package com.bsbowl.onti.domain.chapter.service;

import com.bsbowl.onti.domain.book.entity.Book;
import com.bsbowl.onti.domain.chapter.dto.SectionCreateRequest;
import com.bsbowl.onti.domain.chapter.dto.SectionUpdateRequest;
import com.bsbowl.onti.domain.chapter.entity.Chapter;
import com.bsbowl.onti.domain.chapter.entity.Section;
import com.bsbowl.onti.domain.chapter.entity.SectionStatus;
import com.bsbowl.onti.domain.chapter.repository.SectionRepository;
import com.bsbowl.onti.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SectionServiceTest {

    @Mock
    private SectionRepository sectionRepository;
    @Mock
    private ChapterService chapterService;
    @InjectMocks
    private SectionService sectionService;

    @Test
    void create_assignsNextOrder() {
        User user = User.builder().email("a@onti.com").password("x").name("a").build();
        Book book = Book.builder().user(user).title("책").build();
        Chapter chapter = Chapter.builder().book(book).title("1장").order(0).build();
        when(chapterService.getOwnedChapter("chapter-1", "user-1")).thenReturn(chapter);
        when(sectionRepository.findAllByChapterIdOrderByOrderAsc("chapter-1")).thenReturn(Collections.emptyList());
        when(sectionRepository.save(any(Section.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = sectionService.create("chapter-1", "user-1", new SectionCreateRequest("소제목"));

        assertThat(response.order()).isEqualTo(0);
        assertThat(response.status()).isEqualTo(SectionStatus.EMPTY);
    }

    @Test
    void update_settingBody_movesStatusFromEmptyToDrafting() {
        User user = User.builder().email("a@onti.com").password("x").name("a").build();
        Book book = Book.builder().user(user).title("책").build();
        Chapter chapter = Chapter.builder().book(book).title("1장").order(0).build();
        Section section = Section.builder().chapter(chapter).title("소제목").order(0).build();
        when(sectionRepository.findById("section-1")).thenReturn(Optional.of(section));
        when(chapterService.getOwnedChapter(any(), any())).thenReturn(chapter);

        var response = sectionService.update("section-1", "user-1",
                new SectionUpdateRequest(null, "본문 내용", null, null));

        assertThat(response.status()).isEqualTo(SectionStatus.DRAFTING);
        assertThat(response.body()).isEqualTo("본문 내용");
    }

    @Test
    void update_explicitStatus_movesThroughReviewStates() {
        User user = User.builder().email("a@onti.com").password("x").name("a").build();
        Book book = Book.builder().user(user).title("책").build();
        Chapter chapter = Chapter.builder().book(book).title("1장").order(0).build();
        Section section = Section.builder().chapter(chapter).title("소제목").order(0).build();
        when(sectionRepository.findById("section-1")).thenReturn(Optional.of(section));
        when(chapterService.getOwnedChapter(any(), any())).thenReturn(chapter);

        var draftComplete = sectionService.update("section-1", "user-1",
                new SectionUpdateRequest(null, null, SectionStatus.DRAFT_COMPLETE, null));
        assertThat(draftComplete.status()).isEqualTo(SectionStatus.DRAFT_COMPLETE);

        var reviewing = sectionService.update("section-1", "user-1",
                new SectionUpdateRequest(null, null, SectionStatus.REVIEWING, null));
        assertThat(reviewing.status()).isEqualTo(SectionStatus.REVIEWING);
    }
}
