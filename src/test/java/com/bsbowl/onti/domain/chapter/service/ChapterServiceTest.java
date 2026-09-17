package com.bsbowl.onti.domain.chapter.service;

import com.bsbowl.onti.domain.book.entity.Book;
import com.bsbowl.onti.domain.book.service.BookService;
import com.bsbowl.onti.domain.chapter.dto.ChapterCreateRequest;
import com.bsbowl.onti.domain.chapter.entity.Chapter;
import com.bsbowl.onti.domain.chapter.repository.ChapterRepository;
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
class ChapterServiceTest {

    @Mock
    private ChapterRepository chapterRepository;
    @Mock
    private BookService bookService;
    @InjectMocks
    private ChapterService chapterService;

    @Test
    void create_assignsNextOrder() {
        User user = User.builder().email("a@onti.com").password("x").name("a").build();
        Book book = Book.builder().user(user).title("책").build();
        when(bookService.getOwnedBook("book-1", "user-1")).thenReturn(book);
        when(chapterRepository.findAllByBookIdOrderByOrderAsc("book-1")).thenReturn(Collections.emptyList());
        when(chapterRepository.save(any(Chapter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = chapterService.create("book-1", "user-1", new ChapterCreateRequest("1장"));

        assertThat(response.order()).isEqualTo(0);
        assertThat(response.title()).isEqualTo("1장");
    }

    @Test
    void getOwnedChapter_notFound_throwsChapterNotFound() {
        when(chapterRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chapterService.getOwnedChapter("missing", "user-1"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CHAPTER_NOT_FOUND);
    }
}
