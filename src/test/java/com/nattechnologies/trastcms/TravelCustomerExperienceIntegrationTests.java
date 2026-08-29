package com.nattechnologies.trastcms;
import org.junit.jupiter.api.Test;import static org.assertj.core.api.Assertions.assertThat;
class TravelCustomerExperienceIntegrationTests{@Test void reviewDelayCannotBeNegative(){var s=new com.nattechnologies.trastcms.plugins.travel.experience.TravelExperienceSettings();s.setReviewDelayHours(-1);assertThat(s.getReviewDelayHours()).isZero();}}
