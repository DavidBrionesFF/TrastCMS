package com.nattechnologies.trastcms.plugins.travel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
interface TravelExtraRepository extends JpaRepository<TravelExtra,String> {
    Optional<TravelExtra> findBySlugIgnoreCase(String slug);
    List<TravelExtra> findByActiveTrueOrderByNameAsc();
    long countByDemoDataTrue();
    void deleteByDemoDataTrue();
}
