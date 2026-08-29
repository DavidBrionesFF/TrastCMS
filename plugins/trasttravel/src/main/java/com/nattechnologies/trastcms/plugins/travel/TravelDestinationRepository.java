package com.nattechnologies.trastcms.plugins.travel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
interface TravelDestinationRepository extends JpaRepository<TravelDestination,String> {
    Optional<TravelDestination> findBySlugIgnoreCase(String slug);
    List<TravelDestination> findByActiveTrueOrderByFeaturedDescNameAsc();
    long countByDemoDataTrue();
    void deleteByDemoDataTrue();
}
