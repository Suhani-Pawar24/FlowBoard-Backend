package com.flowboard.user_service.repository;

import com.flowboard.user_service.entity.Broadcast;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BroadcastRepository extends JpaRepository<Broadcast, Long> {
    Page<Broadcast> findAllByOrderBySentAtDesc(Pageable pageable);
}
