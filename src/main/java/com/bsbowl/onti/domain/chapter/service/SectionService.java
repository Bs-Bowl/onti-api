package com.bsbowl.onti.domain.chapter.service;

import com.bsbowl.onti.domain.chapter.dto.SectionCreateRequest;
import com.bsbowl.onti.domain.chapter.dto.SectionResponse;
import com.bsbowl.onti.domain.chapter.dto.SectionUpdateRequest;
import com.bsbowl.onti.domain.chapter.entity.Chapter;
import com.bsbowl.onti.domain.chapter.entity.Section;
import com.bsbowl.onti.domain.chapter.repository.SectionRepository;
import com.bsbowl.onti.global.exception.CustomException;
import com.bsbowl.onti.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SectionService {

    private final SectionRepository sectionRepository;
    private final ChapterService chapterService;

    public SectionService(SectionRepository sectionRepository, ChapterService chapterService) {
        this.sectionRepository = sectionRepository;
        this.chapterService = chapterService;
    }

    @Transactional
    public SectionResponse create(String chapterId, String userId, SectionCreateRequest request) {
        Chapter chapter = chapterService.getOwnedChapter(chapterId, userId);
        int nextOrder = sectionRepository.findAllByChapterIdOrderByOrderAsc(chapterId).size();
        Section section = Section.builder().chapter(chapter).title(request.title()).order(nextOrder).build();
        return SectionResponse.from(sectionRepository.save(section));
    }

    public List<SectionResponse> list(String chapterId, String userId) {
        chapterService.getOwnedChapter(chapterId, userId);
        return sectionRepository.findAllByChapterIdOrderByOrderAsc(chapterId).stream()
                .map(SectionResponse::from)
                .toList();
    }

    @Transactional
    public SectionResponse update(String sectionId, String userId, SectionUpdateRequest request) {
        Section section = getOwnedSection(sectionId, userId);
        section.update(request.title(), request.body(), request.status(), request.order());
        return SectionResponse.from(section);
    }

    @Transactional
    public void delete(String sectionId, String userId) {
        sectionRepository.delete(getOwnedSection(sectionId, userId));
    }

    private Section getOwnedSection(String sectionId, String userId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new CustomException(ErrorCode.SECTION_NOT_FOUND));
        chapterService.getOwnedChapter(section.getChapter().getId(), userId);
        return section;
    }
}
