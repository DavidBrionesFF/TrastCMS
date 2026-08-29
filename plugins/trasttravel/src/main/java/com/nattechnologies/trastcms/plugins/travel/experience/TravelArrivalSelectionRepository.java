package com.nattechnologies.trastcms.plugins.travel.experience;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TravelArrivalSelectionRepository extends JpaRepository<TravelArrivalSelection,Long> { java.util.Optional<TravelArrivalSelection> findByBookingReference(String bookingReference); }
