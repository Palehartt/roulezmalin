package com.roulezmalin.auth.repository;

import com.roulezmalin.auth.model.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {
    List<SearchHistory> findByUserIdOrderBySearchedAtDesc(Long userId);
}
