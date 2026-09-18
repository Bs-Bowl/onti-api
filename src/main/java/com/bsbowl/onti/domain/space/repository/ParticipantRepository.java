package com.bsbowl.onti.domain.space.repository;

import com.bsbowl.onti.domain.space.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParticipantRepository extends JpaRepository<Participant, String> {
    List<Participant> findAllBySpaceId(String spaceId);
}
