package com.nattechnologies.trastcms.plugins.travel;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

interface TravelBookingRepository extends JpaRepository<TravelBooking, String> {
    Optional<TravelBooking> findByBookingCodeIgnoreCase(String code);
    Page<TravelBooking> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<TravelBooking> findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(String email);
    @Query("select booking from TravelBooking booking "
            + "where booking.departure.startsAt between :from and :to "
            + "order by booking.createdAt desc")
    List<TravelBooking> findCalendarBookings(
            @Param("from") Instant from, @Param("to") Instant to);
    List<TravelBooking> findByBookingStatusAndHoldExpiresAtBefore(TravelBookingStatus status, Instant value);
    long countByBookingStatus(TravelBookingStatus status);
    long countByDemoDataTrue();
    void deleteByDemoDataTrue();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select booking from TravelBooking booking where booking.id = :id")
    Optional<TravelBooking> findLockedById(@Param("id") String id);
}
