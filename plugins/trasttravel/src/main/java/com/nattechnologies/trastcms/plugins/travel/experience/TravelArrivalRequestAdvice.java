package com.nattechnologies.trastcms.plugins.travel.experience;

import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.beans.BeanWrapperImpl;
import java.lang.reflect.Type;

@ControllerAdvice
public class TravelArrivalRequestAdvice extends RequestBodyAdviceAdapter implements ResponseBodyAdvice<Object> {
 private static final ThreadLocal<Long> LOCATION=new ThreadLocal<>(); private final TravelCustomerExperienceService service;
 public TravelArrivalRequestAdvice(TravelCustomerExperienceService service){this.service=service;}
 @Override public boolean supports(MethodParameter p,Type t,Class<? extends HttpMessageConverter<?>> c){return p.getContainingClass().getName().toLowerCase().contains("travel")&&p.getMethod()!=null&&p.getMethod().getName().toLowerCase().contains("book");}
 @Override public Object afterBodyRead(Object body,HttpInputMessage input,MethodParameter p,Type t,Class<? extends HttpMessageConverter<?>> c){try{BeanWrapperImpl w=new BeanWrapperImpl(body);if(w.isReadableProperty("arrivalLocationId")){Object v=w.getPropertyValue("arrivalLocationId");if(v instanceof Number n)LOCATION.set(n.longValue());}}catch(Exception ignored){}return body;}
 @Override public boolean supports(MethodParameter p,Class<? extends HttpMessageConverter<?>> c){return p.getContainingClass().getName().toLowerCase().contains("travel")&&p.getMethod()!=null&&p.getMethod().getName().toLowerCase().contains("book");}
 @Override public Object beforeBodyWrite(Object body,MethodParameter p,MediaType m,Class<? extends HttpMessageConverter<?>> c,org.springframework.http.server.ServerHttpRequest req,org.springframework.http.server.ServerHttpResponse res){Long location=LOCATION.get();LOCATION.remove();if(location!=null&&body!=null)try{Object value=body;if(body instanceof ResponseEntity<?> r)value=r.getBody();if(value!=null){BeanWrapperImpl w=new BeanWrapperImpl(value);String ref=null;for(String n:new String[]{"id","bookingId","reference","bookingReference"})if(w.isReadableProperty(n)&&w.getPropertyValue(n)!=null){ref=String.valueOf(w.getPropertyValue(n));break;}if(ref!=null)service.select(ref,location);}}catch(Exception ignored){}return body;}
}
