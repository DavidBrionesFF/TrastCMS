package com.nattechnologies.trastcms.plugins.travel;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
interface TravelTourRepository extends JpaRepository<TravelTour,String> {
    Optional<TravelTour> findBySlugIgnoreCase(String slug);
    Optional<TravelTour> findByCodeIgnoreCase(String code);
    Page<TravelTour> findByStatusOrderByFeaturedDescUpdatedAtDesc(TravelTourStatus status, Pageable pageable);
    List<TravelTour> findByStatusOrderByFeaturedDescUpdatedAtDesc(TravelTourStatus status);
    long countByStatus(TravelTourStatus status);
    long countByDemoDataTrue();
    void deleteByDemoDataTrue();
}
