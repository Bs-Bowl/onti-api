package com.bsbowl.onti.domain.space.service;

import com.bsbowl.onti.domain.book.entity.Book;
import com.bsbowl.onti.domain.book.service.BookService;
import com.bsbowl.onti.domain.chapter.entity.Chapter;
import com.bsbowl.onti.domain.chapter.repository.ChapterRepository;
import com.bsbowl.onti.domain.record.entity.RecordType;
import com.bsbowl.onti.domain.space.dto.BookRecordLinkCreateRequest;
import com.bsbowl.onti.domain.space.entity.BookRecordLink;
import com.bsbowl.onti.domain.space.entity.RecordSpace;
import com.bsbowl.onti.domain.space.entity.SpaceRecord;
import com.bsbowl.onti.domain.space.repository.BookRecordLinkRepository;
import com.bsbowl.onti.domain.user.entity.User;
import com.bsbowl.onti.global.exception.CustomException;
import com.bsbowl.onti.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookRecordLinkServiceTest {

    @Mock
    private BookRecordLinkRepository bookRecordLinkRepository;
    @Mock
    private ChapterRepository chapterRepository;
    @Mock
    private BookService bookService;
    @Mock
    private SpaceRecordService spaceRecordService;
    @InjectMocks
    private BookRecordLinkService bookRecordLinkService;

    @Test
    void create_assignsNextOrder() {
        User user = User.builder().email("a@onti.com").password("x").name("a").build();
        Book book = Book.builder().user(user).title("책").build();
        RecordSpace space = RecordSpace.builder().owner(user).title("공간").build();
        SpaceRecord record = SpaceRecord.builder().space(space).type(RecordType.TEXT).build();
        when(bookService.getOwnedBook("book-1", "user-1")).thenReturn(book);
        when(spaceRecordService.getOwnedSpaceRecord("record-1", "user-1")).thenReturn(record);
        when(bookRecordLinkRepository.findAllByBookIdOrderByOrderAsc("book-1")).thenReturn(Collections.emptyList());
        when(bookRecordLinkRepository.save(any(BookRecordLink.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = bookRecordLinkService.create("book-1", "user-1",
                new BookRecordLinkCreateRequest("record-1", null));

        assertThat(response.order()).isEqualTo(0);
        assertThat(response.chapterId()).isNull();
    }

    @Test
    void create_chapterFromDifferentBook_throwsChapterNotFound() {
        User user = User.builder().email("a@onti.com").password("x").name("a").build();
        Book book = Book.builder().user(user).title("내 책").build();
        ReflectionTestUtils.setField(book, "id", "book-1");
        Book otherBook = Book.builder().user(user).title("다른 책").build();
        ReflectionTestUtils.setField(otherBook, "id", "book-2");
        Chapter foreignChapter = Chapter.builder().book(otherBook).title("남의 챕터").order(0).build();
        RecordSpace space = RecordSpace.builder().owner(user).title("공간").build();
        SpaceRecord record = SpaceRecord.builder().space(space).type(RecordType.TEXT).build();
        when(bookService.getOwnedBook("book-1", "user-1")).thenReturn(book);
        when(spaceRecordService.getOwnedSpaceRecord("record-1", "user-1")).thenReturn(record);
        when(chapterRepository.findById("chapter-2")).thenReturn(java.util.Optional.of(foreignChapter));

        assertThatThrownBy(() -> bookRecordLinkService.create("book-1", "user-1",
                new BookRecordLinkCreateRequest("record-1", "chapter-2")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CHAPTER_NOT_FOUND);
    }

    @Test
    void create_duplicateRecordInSameBook_throwsAlreadyExists() {
        User user = User.builder().email("a@onti.com").password("x").name("a").build();
        Book book = Book.builder().user(user).title("책").build();
        RecordSpace space = RecordSpace.builder().owner(user).title("공간").build();
        SpaceRecord record = SpaceRecord.builder().space(space).type(RecordType.TEXT).build();
        ReflectionTestUtils.setField(record, "id", "record-1");
        when(bookService.getOwnedBook("book-1", "user-1")).thenReturn(book);
        when(spaceRecordService.getOwnedSpaceRecord("record-1", "user-1")).thenReturn(record);
        when(bookRecordLinkRepository.existsByBookIdAndSpaceRecordId("book-1", "record-1")).thenReturn(true);

        assertThatThrownBy(() -> bookRecordLinkService.create("book-1", "user-1",
                new BookRecordLinkCreateRequest("record-1", null)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BOOK_RECORD_LINK_ALREADY_EXISTS);
    }
}
