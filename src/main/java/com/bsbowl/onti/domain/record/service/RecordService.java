package com.bsbowl.onti.domain.record.service;

import com.bsbowl.onti.domain.book.entity.Book;
import com.bsbowl.onti.domain.book.service.BookService;
import com.bsbowl.onti.domain.chapter.entity.Chapter;
import com.bsbowl.onti.domain.chapter.repository.ChapterRepository;
import com.bsbowl.onti.domain.record.dto.RecordCreateRequest;
import com.bsbowl.onti.domain.record.dto.RecordResponse;
import com.bsbowl.onti.domain.record.dto.RecordUpdateRequest;
import com.bsbowl.onti.domain.record.entity.Record;
import com.bsbowl.onti.domain.record.repository.RecordRepository;
import com.bsbowl.onti.global.exception.CustomException;
import com.bsbowl.onti.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class RecordService {

    private final RecordRepository recordRepository;
    private final ChapterRepository chapterRepository;
    private final BookService bookService;

    public RecordService(RecordRepository recordRepository, ChapterRepository chapterRepository, BookService bookService) {
        this.recordRepository = recordRepository;
        this.chapterRepository = chapterRepository;
        this.bookService = bookService;
    }

    @Transactional
    public RecordResponse create(String bookId, String userId, RecordCreateRequest request) {
        Book book = bookService.getOwnedBook(bookId, userId);
        Record record = Record.builder()
                .book(book)
                .type(request.type())
                .content(request.content())
                .mediaUrl(request.mediaUrl())
                .memo(request.memo())
                .recordedAt(request.recordedAt())
                .order(0)
                .build();
        return RecordResponse.from(recordRepository.save(record));
    }

    public List<RecordResponse> list(String bookId, String userId) {
        bookService.getOwnedBook(bookId, userId);
        return recordRepository.findAllByBookIdOrderByOrderAsc(bookId).stream()
                .map(RecordResponse::from)
                .toList();
    }

    @Transactional
    public RecordResponse update(String recordId, String userId, RecordUpdateRequest request) {
        Record record = getOwnedRecord(recordId, userId);
        record.update(request.content(), request.mediaUrl(), request.memo(), request.recordedAt(), request.order());
        if (request.unlinkChapter()) {
            record.linkChapter(null);
        } else if (request.chapterId() != null) {
            Chapter chapter = chapterRepository.findById(request.chapterId())
                    .orElseThrow(() -> new CustomException(ErrorCode.CHAPTER_NOT_FOUND));
            if (!chapter.getBook().getId().equals(record.getBook().getId())) {
                throw new CustomException(ErrorCode.CHAPTER_NOT_FOUND);
            }
            record.linkChapter(chapter);
        }
        return RecordResponse.from(record);
    }

    @Transactional
    public void delete(String recordId, String userId) {
        recordRepository.delete(getOwnedRecord(recordId, userId));
    }

    private Record getOwnedRecord(String recordId, String userId) {
        Record record = recordRepository.findById(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.RECORD_NOT_FOUND));
        bookService.getOwnedBook(record.getBook().getId(), userId);
        return record;
    }
}
