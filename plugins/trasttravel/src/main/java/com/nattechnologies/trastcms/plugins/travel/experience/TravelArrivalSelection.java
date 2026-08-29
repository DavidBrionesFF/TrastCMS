package com.nattechnologies.trastcms.plugins.travel.experience;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.time.Instant;

@Entity
@Table(name="travel_arrival_selection", uniqueConstraints=@UniqueConstraint(name="uk_travel_arrival_booking",columnNames={"site_id","booking_reference"}))
public class TravelArrivalSelection {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @TenantId @Column(name="site_id",nullable=false,length=80) private String siteId;
 @Column(name="booking_reference",nullable=false,length=120) private String bookingReference;
 @Column(name="location_id") private Long locationId;
 @Column(name="location_name",nullable=false,length=160) private String locationName;
 @Column(name="location_type",length=60) private String locationType;
 @Column(name="location_instructions",length=800) private String locationInstructions;
 @Column(name="selected_at",nullable=false) private Instant selectedAt=Instant.now();
 public Long getId(){return id;} public String getSiteId(){return siteId;} public void setSiteId(String v){siteId=v;}
 public String getBookingReference(){return bookingReference;} public void setBookingReference(String v){bookingReference=v;}
 public Long getLocationId(){return locationId;} public void setLocationId(Long v){locationId=v;}
 public String getLocationName(){return locationName;} public void setLocationName(String v){locationName=v;}
 public String getLocationType(){return locationType;} public void setLocationType(String v){locationType=v;}
 public String getLocationInstructions(){return locationInstructions;} public void setLocationInstructions(String v){locationInstructions=v;}
 public Instant getSelectedAt(){return selectedAt;} public void setSelectedAt(Instant v){selectedAt=v;}
}
