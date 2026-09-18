package com.bsbowl.onti.domain.space.repository;

import com.bsbowl.onti.domain.space.entity.SpaceQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpaceQuestionRepository extends JpaRepository<SpaceQuestion, String> {
    List<SpaceQuestion> findAllBySpaceId(String spaceId);
}
