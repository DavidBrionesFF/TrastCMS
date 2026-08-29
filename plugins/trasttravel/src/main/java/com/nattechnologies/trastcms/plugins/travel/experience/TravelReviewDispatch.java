package com.nattechnologies.trastcms.plugins.travel.experience;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="travel_review_dispatch", uniqueConstraints=@UniqueConstraint(name="uk_travel_review_booking",columnNames={"site_id","booking_reference"}), indexes=@Index(name="idx_travel_review_due",columnList="site_id,status,due_at"))
public class TravelReviewDispatch {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @TenantId @Column(name="site_id",nullable=false,length=80) private String siteId;
 @Column(name="booking_reference",nullable=false,length=120) private String bookingReference;
 @Column(nullable=false,length=320) private String email;
 @Column(name="customer_name",length=200) private String customerName;
 @Column(name="tour_name",length=250) private String tourName;
 @Column(name="due_at",nullable=false) private Instant dueAt;
 @Column(name="sent_at") private Instant sentAt;
 @Column(name="clicked_at") private Instant clickedAt;
 @Column(nullable=false,length=30) private String status="PENDING";
 @Column(nullable=false,length=80,unique=true) private String token=UUID.randomUUID().toString();
 @Column(name="attempt_count",nullable=false) private int attemptCount;
 @Column(name="last_error",length=1000) private String lastError;
 public Long getId(){return id;} public String getSiteId(){return siteId;} public void setSiteId(String v){siteId=v;}
 public String getBookingReference(){return bookingReference;} public void setBookingReference(String v){bookingReference=v;}
 public String getEmail(){return email;} public void setEmail(String v){email=v;}
 public String getCustomerName(){return customerName;} public void setCustomerName(String v){customerName=v;}
 public String getTourName(){return tourName;} public void setTourName(String v){tourName=v;}
 public Instant getDueAt(){return dueAt;} public void setDueAt(Instant v){dueAt=v;}
 public Instant getSentAt(){return sentAt;} public void setSentAt(Instant v){sentAt=v;}
 public Instant getClickedAt(){return clickedAt;} public void setClickedAt(Instant v){clickedAt=v;}
 public String getStatus(){return status;} public void setStatus(String v){status=v;}
 public String getToken(){return token;} public void setToken(String v){token=v;}
 public int getAttemptCount(){return attemptCount;} public void setAttemptCount(int v){attemptCount=v;}
 public String getLastError(){return lastError;} public void setLastError(String v){lastError=v;}
}
