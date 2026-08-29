package com.nattechnologies.trastcms.plugins.travel.experience;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TravelExperienceSettingsRepository extends JpaRepository<TravelExperienceSettings,Long> { java.util.Optional<TravelExperienceSettings> findFirstByOrderByIdAsc(); }
