package com.roulezmalin.auth.repository;

import com.roulezmalin.auth.model.NegotiationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NegotiationHistoryRepository extends JpaRepository<NegotiationHistory, Long> {
    List<NegotiationHistory> findByUserIdOrderByNegotiatedAtDesc(Long userId);
}
