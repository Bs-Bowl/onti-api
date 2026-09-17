package com.bsbowl.onti.domain.book.service;

import com.bsbowl.onti.domain.book.dto.BookCreateRequest;
import com.bsbowl.onti.domain.book.entity.Book;
import com.bsbowl.onti.domain.book.entity.BookStatus;
import com.bsbowl.onti.domain.book.repository.BookRepository;
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
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private BookService bookService;

    @Test
    void create_savesBookWithDraftStatus() {
        User user = User.builder().email("a@onti.com").password("x").name("a").build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = bookService.create("user-1", new BookCreateRequest("내 책", null, null));

        assertThat(response.status()).isEqualTo(BookStatus.DRAFT);
        assertThat(response.title()).isEqualTo("내 책");
    }

    @Test
    void getOwnedBook_notOwner_throwsForbidden() {
        User owner = User.builder().email("owner@onti.com").password("x").name("owner").build();
        ReflectionTestUtils.setField(owner, "id", "owner-id");
        Book book = Book.builder().user(owner).title("책").build();
        when(bookRepository.findById("book-1")).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> bookService.getOwnedBook("book-1", "other-id"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void getOwnedBook_notFound_throwsBookNotFound() {
        when(bookRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.getOwnedBook("missing", "user-1"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BOOK_NOT_FOUND);
    }
}
