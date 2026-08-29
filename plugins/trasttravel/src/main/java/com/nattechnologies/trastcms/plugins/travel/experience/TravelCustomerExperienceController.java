package com.nattechnologies.trastcms.plugins.travel.experience;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.*;

@RestController
public class TravelCustomerExperienceController {
 private final TravelCustomerExperienceService service; private final TravelReviewDispatchRepository reviews;
 public TravelCustomerExperienceController(TravelCustomerExperienceService service,TravelReviewDispatchRepository reviews){this.service=service;this.reviews=reviews;}
 @GetMapping("/api/admin/travel/customer-experience/settings") public TravelExperienceSettings adminSettings(){return service.settings();}
 @PutMapping("/api/admin/travel/customer-experience/settings") public TravelExperienceSettings saveSettings(@RequestBody TravelExperienceSettings value){return service.saveSettings(value);}
 @GetMapping("/api/admin/travel/customer-experience/arrival-locations") public List<TravelArrivalLocation> adminLocations(){return service.locations(null,false);}
 @PostMapping("/api/admin/travel/customer-experience/arrival-locations") public ResponseEntity<TravelArrivalLocation> createLocation(@RequestBody TravelArrivalLocation value){value.setId(null);return ResponseEntity.status(HttpStatus.CREATED).body(service.saveLocation(value));}
 @PutMapping("/api/admin/travel/customer-experience/arrival-locations/{id}") public TravelArrivalLocation updateLocation(@PathVariable Long id,@RequestBody TravelArrivalLocation value){value.setId(id);return service.saveLocation(value);}
 @DeleteMapping("/api/admin/travel/customer-experience/arrival-locations/{id}") public ResponseEntity<Void> deleteLocation(@PathVariable Long id){service.deleteLocation(id);return ResponseEntity.noContent().build();}
 @GetMapping("/api/public/travel/customer-experience") public Map<String,Object> publicConfig(@RequestParam(required=false) String tourId){TravelExperienceSettings cfg=service.settings();return Map.of("enabled",cfg.isArrivalEnabled(),"required",cfg.isArrivalRequired(),"label",cfg.getArrivalLabel(),"help",cfg.getArrivalHelp()==null?"":cfg.getArrivalHelp(),"locations",service.locations(tourId,true));}
 @PostMapping("/api/public/travel/customer-experience/arrival") public ResponseEntity<TravelArrivalSelection> select(@RequestBody ArrivalSelectionRequest request){return ResponseEntity.status(HttpStatus.CREATED).body(service.select(request.bookingReference(),request.locationId()));}
 @GetMapping("/api/public/travel/customer-experience/review/{token}") public ResponseEntity<Void> review(@PathVariable String token){
   TravelReviewDispatch dispatch=reviews.findByToken(token).orElseThrow(); dispatch.setClickedAt(java.time.Instant.now()); reviews.save(dispatch);
   String url=service.settings().getReviewUrl(); if(url==null||url.isBlank()) return ResponseEntity.notFound().build(); return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
 }
 public record ArrivalSelectionRequest(String bookingReference,Long locationId){}
}
