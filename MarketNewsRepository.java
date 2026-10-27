package com.uip.backend.repository;

import com.uip.backend.model.MarketNews;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MarketNewsRepository extends JpaRepository<MarketNews, Long> {
    List<MarketNews> findAllByOrderByPublishedDateDesc();
}
