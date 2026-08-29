package com.nattechnologies.trastcms.plugins.academy;

import com.nattechnologies.trastcms.plugin.api.SellableProvider;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class AcademySellableProvider implements SellableProvider {
    private final AcademyCourseRepository courses;
    private final AcademyService service;
    AcademySellableProvider(AcademyCourseRepository courses, AcademyService service){this.courses=courses;this.service=service;}
    @Override public String providerKey(){return "academy";}
    @Override public String pluginId(){return "trastacademy";}
    @Override public Optional<SellableItem> resolve(String reference){
        return courses.findById(reference)
                .filter(course->course.status==AcademyCourseStatus.PUBLISHED && course.accessType==AcademyAccessType.PAID)
                .map(course->new SellableItem(providerKey(),course.id,"ACADEMY_COURSE",course.title,course.excerpt,
                        course.price,course.currency,false,null,false,course.featuredImageUrl,true,
                        Map.of("slug",course.slug,"instructor",Optional.ofNullable(course.instructorName).orElse(""),
                                "estimatedMinutes",course.estimatedMinutes,"accessType",course.accessType.name(),
                                "singleQuantity",true)));
    }
    @Override public List<SellableItem> featured(int limit){return courses.findByStatusAndFeaturedTrueOrderByUpdatedAtDesc(AcademyCourseStatus.PUBLISHED).stream()
            .filter(course->course.accessType==AcademyAccessType.PAID).limit(Math.max(0,limit)).map(course->resolve(course.id).orElseThrow()).toList();}
    @Override public void fulfill(FulfillmentRequest request){service.fulfill(request.itemReference(),request.customerId(),request.customerEmail(),request.orderId());}
}
