package com.nattechnologies.trastcms.plugins.travel.experience;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TravelReviewDispatchRepository extends JpaRepository<TravelReviewDispatch,Long> { java.util.Optional<TravelReviewDispatch> findByToken(String token); java.util.Optional<TravelReviewDispatch> findByBookingReference(String bookingReference); }
