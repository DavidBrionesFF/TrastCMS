package com.nattechnologies.trastcms.plugins.travel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
interface TravelActivityRepository extends JpaRepository<TravelActivity,String> {
    Optional<TravelActivity> findBySlugIgnoreCase(String slug);
    List<TravelActivity> findByActiveTrueOrderByNameAsc();
    long countByDemoDataTrue();
    void deleteByDemoDataTrue();
}
