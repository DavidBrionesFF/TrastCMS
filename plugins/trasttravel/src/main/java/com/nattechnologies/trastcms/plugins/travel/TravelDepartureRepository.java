package com.nattechnologies.trastcms.plugins.travel;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

interface TravelDepartureRepository extends JpaRepository<TravelDeparture, String> {
    @EntityGraph(attributePaths = "tour")
    List<TravelDeparture> findByTourId(String tourId);

    @EntityGraph(attributePaths = "tour")
    List<TravelDeparture> findByTourIdAndStartsAtAfterOrderByStartsAtAsc(String tourId, Instant startsAt);

    @EntityGraph(attributePaths = "tour")
    List<TravelDeparture> findByTourIdAndStartsAtBetweenOrderByStartsAtAsc(
            String tourId, Instant from, Instant to);

    @EntityGraph(attributePaths = "tour")
    List<TravelDeparture> findByStartsAtBetweenOrderByStartsAtAsc(Instant from, Instant to);
    long countByStartsAtAfter(Instant value);
    long countByDemoDataTrue();
    void deleteByDemoDataTrue();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "tour")
    @Query("select departure from TravelDeparture departure where departure.id = :id")
    Optional<TravelDeparture> findLockedById(@Param("id") String id);
}
