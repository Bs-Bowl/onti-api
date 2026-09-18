package com.bsbowl.onti.domain.space.service;

import com.bsbowl.onti.domain.book.entity.Book;
import com.bsbowl.onti.domain.book.service.BookService;
import com.bsbowl.onti.domain.chapter.entity.Chapter;
import com.bsbowl.onti.domain.chapter.repository.ChapterRepository;
import com.bsbowl.onti.domain.space.dto.BookRecordLinkCreateRequest;
import com.bsbowl.onti.domain.space.dto.BookRecordLinkResponse;
import com.bsbowl.onti.domain.space.entity.BookRecordLink;
import com.bsbowl.onti.domain.space.entity.SpaceRecord;
import com.bsbowl.onti.domain.space.repository.BookRecordLinkRepository;
import com.bsbowl.onti.global.exception.CustomException;
import com.bsbowl.onti.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class BookRecordLinkService {

    private final BookRecordLinkRepository bookRecordLinkRepository;
    private final ChapterRepository chapterRepository;
    private final BookService bookService;
    private final SpaceRecordService spaceRecordService;

    public BookRecordLinkService(BookRecordLinkRepository bookRecordLinkRepository, ChapterRepository chapterRepository,
                                  BookService bookService, SpaceRecordService spaceRecordService) {
        this.bookRecordLinkRepository = bookRecordLinkRepository;
        this.chapterRepository = chapterRepository;
        this.bookService = bookService;
        this.spaceRecordService = spaceRecordService;
    }

    @Transactional
    public BookRecordLinkResponse create(String bookId, String userId, BookRecordLinkCreateRequest request) {
        Book book = bookService.getOwnedBook(bookId, userId);
        SpaceRecord spaceRecord = spaceRecordService.getOwnedSpaceRecord(request.spaceRecordId(), userId);
        Chapter chapter = null;
        if (request.chapterId() != null) {
            chapter = chapterRepository.findById(request.chapterId())
                    .orElseThrow(() -> new CustomException(ErrorCode.CHAPTER_NOT_FOUND));
            if (!chapter.getBook().getId().equals(bookId)) {
                throw new CustomException(ErrorCode.CHAPTER_NOT_FOUND);
            }
        }
        int nextOrder = bookRecordLinkRepository.findAllByBookIdOrderByOrderAsc(bookId).size();
        BookRecordLink link = BookRecordLink.builder()
                .book(book)
                .spaceRecord(spaceRecord)
                .chapter(chapter)
                .order(nextOrder)
                .build();
        return BookRecordLinkResponse.from(bookRecordLinkRepository.save(link));
    }

    public List<BookRecordLinkResponse> list(String bookId, String userId) {
        bookService.getOwnedBook(bookId, userId);
        return bookRecordLinkRepository.findAllByBookIdOrderByOrderAsc(bookId).stream()
                .map(BookRecordLinkResponse::from)
                .toList();
    }

    @Transactional
    public void delete(String linkId, String userId) {
        BookRecordLink link = bookRecordLinkRepository.findById(linkId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOOK_RECORD_LINK_NOT_FOUND));
        bookService.getOwnedBook(link.getBook().getId(), userId);
        bookRecordLinkRepository.delete(link);
    }
}
