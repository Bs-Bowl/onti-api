package com.bsbowl.onti.domain.book.service;

import com.bsbowl.onti.domain.book.dto.BookDesignRequest;
import com.bsbowl.onti.domain.book.entity.Book;
import com.bsbowl.onti.domain.book.entity.BookDesign;
import com.bsbowl.onti.domain.book.repository.BookDesignRepository;
import com.bsbowl.onti.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookDesignServiceTest {

    @Mock
    private BookDesignRepository bookDesignRepository;
    @Mock
    private BookService bookService;
    @InjectMocks
    private BookDesignService bookDesignService;

    @Test
    void upsert_createsDesignWhenAbsent() {
        User user = User.builder().email("a@onti.com").password("x").name("a").build();
        Book book = Book.builder().user(user).title("책").build();
        when(bookService.getOwnedBook("book-1", "user-1")).thenReturn(book);
        when(bookDesignRepository.findByBookId("book-1")).thenReturn(Optional.empty());
        when(bookDesignRepository.save(any(BookDesign.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = bookDesignService.upsert("book-1", "user-1",
                new BookDesignRequest("classic", "#111111", null, "Pretendard", "editorial", "{\"cover\":{}}"));

        assertThat(response.coverTemplate()).isEqualTo("classic");
        assertThat(response.coverColor()).isEqualTo("#111111");
        assertThat(response.designJson()).isEqualTo("{\"cover\":{}}");
    }

    @Test
    void upsert_updatesExistingDesign() {
        User user = User.builder().email("a@onti.com").password("x").name("a").build();
        Book book = Book.builder().user(user).title("책").build();
        BookDesign existing = BookDesign.builder().book(book).build();
        when(bookService.getOwnedBook("book-1", "user-1")).thenReturn(book);
        when(bookDesignRepository.findByBookId("book-1")).thenReturn(Optional.of(existing));

        var response = bookDesignService.upsert("book-1", "user-1",
                new BookDesignRequest(null, "#222222", null, null, null, null));

        assertThat(response.coverColor()).isEqualTo("#222222");
        assertThat(response.coverTemplate()).isEqualTo("default");
    }
}
