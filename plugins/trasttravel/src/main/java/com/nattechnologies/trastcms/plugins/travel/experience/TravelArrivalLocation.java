package com.nattechnologies.trastcms.plugins.travel.experience;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "travel_arrival_location", indexes = {
 @Index(name="idx_travel_arrival_site_active", columnList="site_id,active,sort_order"),
 @Index(name="idx_travel_arrival_tour", columnList="site_id,tour_reference")
})
public class TravelArrivalLocation {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @TenantId @Column(name="site_id",nullable=false,length=80) private String siteId;
 @Column(nullable=false,length=160) private String name;
 @Column(nullable=false,length=60) private String type="OTHER";
 @Column(length=800) private String instructions;
 @Column(name="tour_reference",length=100) private String tourReference;
 @Column(nullable=false) private boolean active=true;
 @Column(name="sort_order",nullable=false) private int sortOrder;
 public Long getId(){return id;} public void setId(Long v){id=v;}
 public String getSiteId(){return siteId;} public void setSiteId(String v){siteId=v;}
 public String getName(){return name;} public void setName(String v){name=v;}
 public String getType(){return type;} public void setType(String v){type=v;}
 public String getInstructions(){return instructions;} public void setInstructions(String v){instructions=v;}
 public String getTourReference(){return tourReference;} public void setTourReference(String v){tourReference=v;}
 public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
 public int getSortOrder(){return sortOrder;} public void setSortOrder(int v){sortOrder=v;}
}
