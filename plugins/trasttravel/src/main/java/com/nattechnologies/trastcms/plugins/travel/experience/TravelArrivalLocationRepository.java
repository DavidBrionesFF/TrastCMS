package com.nattechnologies.trastcms.plugins.travel.experience;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TravelArrivalLocationRepository extends JpaRepository<TravelArrivalLocation,Long> { java.util.List<TravelArrivalLocation> findAllByOrderBySortOrderAscNameAsc(); }
