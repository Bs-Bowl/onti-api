package com.bsbowl.onti.domain.record.service;

import com.bsbowl.onti.domain.book.entity.Book;
import com.bsbowl.onti.domain.book.service.BookService;
import com.bsbowl.onti.domain.chapter.entity.Chapter;
import com.bsbowl.onti.domain.chapter.repository.ChapterRepository;
import com.bsbowl.onti.domain.record.dto.RecordCreateRequest;
import com.bsbowl.onti.domain.record.dto.RecordUpdateRequest;
import com.bsbowl.onti.domain.record.entity.Record;
import com.bsbowl.onti.domain.record.entity.RecordType;
import com.bsbowl.onti.domain.record.repository.RecordRepository;
import com.bsbowl.onti.domain.user.entity.User;
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
class RecordServiceTest {

    @Mock
    private RecordRepository recordRepository;
    @Mock
    private ChapterRepository chapterRepository;
    @Mock
    private BookService bookService;
    @InjectMocks
    private RecordService recordService;

    @Test
    void create_savesRecordUnderBook() {
        User user = User.builder().email("a@onti.com").password("x").name("a").build();
        Book book = Book.builder().user(user).title("책").build();
        when(bookService.getOwnedBook("book-1", "user-1")).thenReturn(book);
        when(recordRepository.save(any(Record.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = recordService.create("book-1", "user-1",
                new RecordCreateRequest(RecordType.MEMO, "메모 내용", null, null, null));

        assertThat(response.type()).isEqualTo(RecordType.MEMO);
        assertThat(response.content()).isEqualTo("메모 내용");
    }

    @Test
    void update_notFound_throwsRecordNotFound() {
        when(recordRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recordService.update("missing", "user-1",
                new RecordUpdateRequest(null, null, null, null, null, null, false)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RECORD_NOT_FOUND);
    }

    @Test
    void update_unlinkChapter_removesChapterAssociation() {
        User user = User.builder().email("a@onti.com").password("x").name("a").build();
        Book book = Book.builder().user(user).title("책").build();
        Record record = Record.builder().book(book).type(RecordType.MEMO).content("메모").order(0).build();
        when(recordRepository.findById("record-1")).thenReturn(Optional.of(record));
        when(bookService.getOwnedBook(any(), any())).thenReturn(book);

        var response = recordService.update("record-1", "user-1",
                new RecordUpdateRequest(null, null, null, null, null, null, true));

        assertThat(response.chapterId()).isNull();
    }

    @Test
    void update_linkingChapterFromDifferentBook_throwsChapterNotFound() {
        User user = User.builder().email("a@onti.com").password("x").name("a").build();
        Book recordBook = Book.builder().user(user).title("내 책").build();
        ReflectionTestUtils.setField(recordBook, "id", "book-1");
        Book otherBook = Book.builder().user(user).title("다른 책").build();
        ReflectionTestUtils.setField(otherBook, "id", "book-2");
        Record record = Record.builder().book(recordBook).type(RecordType.MEMO).content("메모").order(0).build();
        Chapter foreignChapter = Chapter.builder().book(otherBook).title("남의 챕터").order(0).build();
        when(recordRepository.findById("record-1")).thenReturn(Optional.of(record));
        when(bookService.getOwnedBook(any(), any())).thenReturn(recordBook);
        when(chapterRepository.findById("chapter-2")).thenReturn(Optional.of(foreignChapter));

        assertThatThrownBy(() -> recordService.update("record-1", "user-1",
                new RecordUpdateRequest(null, null, null, null, null, "chapter-2", false)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CHAPTER_NOT_FOUND);
    }
}
