package com.bsbowl.onti.domain.chapter.service;

import com.bsbowl.onti.domain.book.entity.Book;
import com.bsbowl.onti.domain.book.service.BookService;
import com.bsbowl.onti.domain.chapter.dto.ChapterCreateRequest;
import com.bsbowl.onti.domain.chapter.dto.ChapterResponse;
import com.bsbowl.onti.domain.chapter.dto.ChapterUpdateRequest;
import com.bsbowl.onti.domain.chapter.entity.Chapter;
import com.bsbowl.onti.domain.chapter.repository.ChapterRepository;
import com.bsbowl.onti.global.exception.CustomException;
import com.bsbowl.onti.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ChapterService {

    private final ChapterRepository chapterRepository;
    private final BookService bookService;

    public ChapterService(ChapterRepository chapterRepository, BookService bookService) {
        this.chapterRepository = chapterRepository;
        this.bookService = bookService;
    }

    @Transactional
    public ChapterResponse create(String bookId, String userId, ChapterCreateRequest request) {
        Book book = bookService.getOwnedBook(bookId, userId);
        int nextOrder = chapterRepository.findAllByBookIdOrderByOrderAsc(bookId).size();
        Chapter chapter = Chapter.builder().book(book).title(request.title()).order(nextOrder).build();
        return ChapterResponse.from(chapterRepository.save(chapter));
    }

    public List<ChapterResponse> list(String bookId, String userId) {
        bookService.getOwnedBook(bookId, userId);
        return chapterRepository.findAllByBookIdOrderByOrderAsc(bookId).stream()
                .map(ChapterResponse::from)
                .toList();
    }

    @Transactional
    public ChapterResponse update(String chapterId, String userId, ChapterUpdateRequest request) {
        Chapter chapter = getOwnedChapter(chapterId, userId);
        chapter.update(request.title(), request.note(), request.order());
        return ChapterResponse.from(chapter);
    }

    @Transactional
    public void delete(String chapterId, String userId) {
        chapterRepository.delete(getOwnedChapter(chapterId, userId));
    }

    public Chapter getOwnedChapter(String chapterId, String userId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAPTER_NOT_FOUND));
        bookService.getOwnedBook(chapter.getBook().getId(), userId);
        return chapter;
    }
}
