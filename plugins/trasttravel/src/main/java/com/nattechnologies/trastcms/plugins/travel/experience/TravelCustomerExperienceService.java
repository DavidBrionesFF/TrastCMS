package com.nattechnologies.trastcms.plugins.travel.experience;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class TravelCustomerExperienceService {
 private final TravelExperienceSettingsRepository settings;
 private final TravelArrivalLocationRepository locations;
 private final TravelArrivalSelectionRepository selections;
 public TravelCustomerExperienceService(TravelExperienceSettingsRepository settings,TravelArrivalLocationRepository locations,TravelArrivalSelectionRepository selections){this.settings=settings;this.locations=locations;this.selections=selections;}
 @Transactional(readOnly=true) public TravelExperienceSettings settings(){return settings.findFirstByOrderByIdAsc().orElseGet(TravelExperienceSettings::new);}
 @Transactional public TravelExperienceSettings saveSettings(TravelExperienceSettings input){
   TravelExperienceSettings target=settings.findFirstByOrderByIdAsc().orElseGet(TravelExperienceSettings::new);
   target.setReviewEnabled(input.isReviewEnabled()); target.setReviewDelayHours(input.getReviewDelayHours()); target.setEligibleStatuses(input.getEligibleStatuses());
   target.setReviewSubject(input.getReviewSubject()); target.setReviewBody(input.getReviewBody()); target.setReviewButtonText(input.getReviewButtonText()); target.setReviewUrl(input.getReviewUrl());
   target.setArrivalEnabled(input.isArrivalEnabled()); target.setArrivalRequired(input.isArrivalRequired()); target.setArrivalLabel(input.getArrivalLabel()); target.setArrivalHelp(input.getArrivalHelp());
   return settings.save(target);
 }
 @Transactional(readOnly=true) public List<TravelArrivalLocation> locations(String tourReference,boolean publicOnly){
   return locations.findAllByOrderBySortOrderAscNameAsc().stream().filter(x->!publicOnly||x.isActive()).filter(x->x.getTourReference()==null||x.getTourReference().isBlank()||Objects.equals(x.getTourReference(),tourReference)).toList();
 }
 @Transactional public TravelArrivalLocation saveLocation(TravelArrivalLocation value){
   if(value.getName()==null||value.getName().isBlank()) throw new IllegalArgumentException("Location name is required");
   if(value.getId()!=null){TravelArrivalLocation current=locations.findById(value.getId()).orElseThrow(); current.setName(value.getName());current.setType(value.getType());current.setInstructions(value.getInstructions());current.setTourReference(value.getTourReference());current.setActive(value.isActive());current.setSortOrder(value.getSortOrder());return locations.save(current);}
   return locations.save(value);
 }
 @Transactional public void deleteLocation(Long id){locations.deleteById(id);}
 @Transactional public TravelArrivalSelection select(String bookingReference,Long locationId){
   TravelExperienceSettings cfg=settings(); if(!cfg.isArrivalEnabled()) throw new IllegalStateException("Arrival selection is disabled");
   if(bookingReference==null||bookingReference.isBlank()) throw new IllegalArgumentException("Booking reference is required");
   TravelArrivalLocation loc=locations.findById(locationId).filter(TravelArrivalLocation::isActive).orElseThrow(()->new IllegalArgumentException("Arrival location is not available"));
   TravelArrivalSelection target=selections.findByBookingReference(bookingReference).orElseGet(TravelArrivalSelection::new);
   target.setBookingReference(bookingReference);target.setLocationId(loc.getId());target.setLocationName(loc.getName());target.setLocationType(loc.getType());target.setLocationInstructions(loc.getInstructions());target.setSelectedAt(java.time.Instant.now());
   return selections.save(target);
 }
}
