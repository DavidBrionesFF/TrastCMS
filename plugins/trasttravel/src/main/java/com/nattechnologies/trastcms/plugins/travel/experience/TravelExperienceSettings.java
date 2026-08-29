package com.nattechnologies.trastcms.plugins.travel.experience;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "travel_experience_settings", uniqueConstraints = @UniqueConstraint(name = "uk_travel_experience_site", columnNames = "site_id"))
public class TravelExperienceSettings {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @TenantId @Column(name = "site_id", nullable = false, length = 80) private String siteId;
    @Column(name = "review_enabled", nullable = false) private boolean reviewEnabled;
    @Column(name = "review_delay_hours", nullable = false) private int reviewDelayHours = 24;
    @Column(name = "eligible_statuses", nullable = false, length = 255) private String eligibleStatuses = "CONFIRMED,PAID,COMPLETED";
    @Column(name = "review_subject", nullable = false, length = 255) private String reviewSubject = "How was your tour?";
    @Lob @Column(name = "review_body", nullable = false) private String reviewBody = "Thank you for visiting us. We would appreciate your review.";
    @Column(name = "review_button_text", nullable = false, length = 120) private String reviewButtonText = "Leave a review";
    @Column(name = "review_url", length = 1000) private String reviewUrl;
    @Column(name = "arrival_enabled", nullable = false) private boolean arrivalEnabled = true;
    @Column(name = "arrival_required", nullable = false) private boolean arrivalRequired = true;
    @Column(name = "arrival_label", nullable = false, length = 160) private String arrivalLabel = "Arrival or pickup location";
    @Column(name = "arrival_help", length = 500) private String arrivalHelp = "Select where our team should meet you.";
    public Long getId() { return id; } public void setId(Long id) { this.id=id; }
    public String getSiteId() { return siteId; } public void setSiteId(String siteId) { this.siteId=siteId; }
    public boolean isReviewEnabled() { return reviewEnabled; } public void setReviewEnabled(boolean v) { reviewEnabled=v; }
    public int getReviewDelayHours() { return reviewDelayHours; } public void setReviewDelayHours(int v) { reviewDelayHours=Math.max(0,v); }
    public String getEligibleStatuses() { return eligibleStatuses; } public void setEligibleStatuses(String v) { eligibleStatuses=v; }
    public String getReviewSubject() { return reviewSubject; } public void setReviewSubject(String v) { reviewSubject=v; }
    public String getReviewBody() { return reviewBody; } public void setReviewBody(String v) { reviewBody=v; }
    public String getReviewButtonText() { return reviewButtonText; } public void setReviewButtonText(String v) { reviewButtonText=v; }
    public String getReviewUrl() { return reviewUrl; } public void setReviewUrl(String v) { reviewUrl=v; }
    public boolean isArrivalEnabled() { return arrivalEnabled; } public void setArrivalEnabled(boolean v) { arrivalEnabled=v; }
    public boolean isArrivalRequired() { return arrivalRequired; } public void setArrivalRequired(boolean v) { arrivalRequired=v; }
    public String getArrivalLabel() { return arrivalLabel; } public void setArrivalLabel(String v) { arrivalLabel=v; }
    public String getArrivalHelp() { return arrivalHelp; } public void setArrivalHelp(String v) { arrivalHelp=v; }
}
