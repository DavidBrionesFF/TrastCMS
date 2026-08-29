package com.nattechnologies.trastcms.plugins.travel.experience;

import org.slf4j.*;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.lang.reflect.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.regex.*;

@Component
public class TravelReviewScheduler {
 private static final Logger log=LoggerFactory.getLogger(TravelReviewScheduler.class);
 private static final Pattern EMAIL=Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}",Pattern.CASE_INSENSITIVE);
 private final JdbcTemplate jdbc; private final ApplicationContext context;
 public TravelReviewScheduler(JdbcTemplate jdbc,ApplicationContext context){this.jdbc=jdbc;this.context=context;}
 @Scheduled(cron="${trastcms.travel.review.cron:0 */15 * * * *}")
 public void run(){
  List<Map<String,Object>> configs=safeQuery("select * from travel_experience_settings where review_enabled=true");
  for(Map<String,Object> cfg:configs) try{scanSite(cfg);}catch(Exception ex){log.warn("Travel review scan failed for site {}: {}",cfg.get("site_id"),ex.getMessage());}
 }
 private void scanSite(Map<String,Object> cfg)throws Exception{
  String site=Objects.toString(cfg.get("site_id"),""); String table=findBookingTable(); if(table==null)return;
  for(Map<String,Object> row:safeQuery("select * from "+table+" where site_id = ?",site)){
   String ref=first(row,"id","booking_id","uuid","reference","booking_reference"); if(ref==null)continue;
   if(!eligible(first(row,"status","booking_status","payment_status"),Objects.toString(cfg.get("eligible_statuses"),"CONFIRMED,PAID,COMPLETED")))continue;
   Instant visit=firstInstant(row,"tour_date","travel_date","departure_date","start_date","service_date","scheduled_at","booking_date"); if(visit==null)continue;
   int delay=((Number)cfg.getOrDefault("review_delay_hours",24)).intValue(); Instant due=visit.plus(Duration.ofHours(delay)); if(due.isAfter(Instant.now()))continue;
   if(!safeQuery("select id from travel_review_dispatch where site_id=? and booking_reference=?",site,ref).isEmpty())continue;
   String email=findEmail(row); if(email==null)continue;
   String name=first(row,"customer_name","full_name","name","contact_name"); String tour=first(row,"tour_name","trip_name","title");
   String token=UUID.randomUUID().toString();
   jdbc.update("insert into travel_review_dispatch(site_id,booking_reference,email,customer_name,tour_name,due_at,status,token,attempt_count) values(?,?,?,?,?,?,?, ?,0)",site,ref,email,name,tour,Timestamp.from(due),"PENDING",token);
   send(site,ref,email,name,tour,cfg,token);
  }
 }
 @Transactional protected void send(String site,String ref,String email,String name,String tour,Map<String,Object> cfg,String token){
  try{String subject=render(Objects.toString(cfg.get("review_subject"),"How was your tour?"),name,tour);String body=render(Objects.toString(cfg.get("review_body"),"Thank you for visiting us."),name,tour);
   String button=Objects.toString(cfg.get("review_button_text"),"Leave a review"); String base=Objects.toString(cfg.get("review_url"),"");
   String link=base; if(!base.isBlank()) link="/api/public/travel/customer-experience/review/"+token;
   String html="<div style=\"font-family:Arial,sans-serif;max-width:600px\"><p>"+escape(body)+"</p>"+(link.isBlank()?"":"<p><a style=\"display:inline-block;padding:12px 18px;background:#2563eb;color:white;text-decoration:none;border-radius:6px\" href=\""+escape(link)+"\">"+escape(button)+"</a></p>")+"</div>";
   invokeMail(site,email,subject,html); jdbc.update("update travel_review_dispatch set status='SENT',sent_at=?,attempt_count=attempt_count+1,last_error=null where site_id=? and booking_reference=?",Timestamp.from(Instant.now()),site,ref);
  }catch(Exception ex){String msg=sanitize(ex.getMessage());jdbc.update("update travel_review_dispatch set status='FAILED',attempt_count=attempt_count+1,last_error=? where site_id=? and booking_reference=?",msg,site,ref);log.warn("Review invitation failed for booking {}: {}",ref,msg);}
 }
 private void invokeMail(String site,String to,String subject,String html)throws Exception{
  Object bean=null; for(String n:context.getBeanDefinitionNames()){Object b;try{b=context.getBean(n);}catch(Exception ignored){continue;}String cn=b.getClass().getName().toLowerCase();if(cn.contains("platformmailservice")||cn.endsWith("mailservice")){bean=b;if(cn.contains("platformmailservice"))break;}}
  if(bean==null)throw new IllegalStateException("Central mail service is unavailable");
  for(String methodName:List.of("sendHtmlNow","sendHtml"))for(Method m:bean.getClass().getMethods())if(m.getName().equals(methodName)){Class<?>[] t=m.getParameterTypes();if(t.length==3&&Arrays.stream(t).allMatch(x->x==String.class)){m.invoke(bean,to,subject,html);return;}if(t.length==4&&Arrays.stream(t).allMatch(x->x==String.class)){m.invoke(bean,site,to,subject,html);return;}}
  throw new IllegalStateException("Central mail service does not expose sendHtml");
 }
 private String findBookingTable()throws SQLException{try(Connection c=Objects.requireNonNull(jdbc.getDataSource()).getConnection()){DatabaseMetaData md=c.getMetaData();try(ResultSet rs=md.getTables(c.getCatalog(),null,"%",new String[]{"TABLE"})){String fallback=null;while(rs.next()){String n=rs.getString("TABLE_NAME");String l=n.toLowerCase();if(l.contains("travel")&&l.contains("booking"))return n;if(fallback==null&&l.contains("booking"))fallback=n;}return fallback;}}}
 private List<Map<String,Object>> safeQuery(String sql,Object...args){try{return jdbc.queryForList(sql,args);}catch(Exception e){return List.of();}}
 private boolean eligible(String status,String csv){if(status==null)return false;return Arrays.stream(csv.split(",")).map(String::trim).anyMatch(x->x.equalsIgnoreCase(status));}
 private String findEmail(Map<String,Object> row){String direct=first(row,"customer_email","email","contact_email","billing_email");if(direct!=null)return direct;for(Object v:row.values())if(v instanceof String s){Matcher m=EMAIL.matcher(s);if(m.find())return m.group();}return null;}
 private String first(Map<String,Object> row,String...names){for(String n:names)for(var e:row.entrySet())if(e.getKey().equalsIgnoreCase(n)&&e.getValue()!=null)return Objects.toString(e.getValue(),null);return null;}
 private Instant firstInstant(Map<String,Object> row,String...names){for(String n:names)for(var e:row.entrySet())if(e.getKey().equalsIgnoreCase(n)&&e.getValue()!=null){Object v=e.getValue();if(v instanceof Timestamp t)return t.toInstant();if(v instanceof java.sql.Date d)return d.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant();if(v instanceof LocalDate d)return d.atStartOfDay(ZoneId.systemDefault()).toInstant();if(v instanceof LocalDateTime d)return d.atZone(ZoneId.systemDefault()).toInstant();if(v instanceof Instant i)return i;}return null;}
 private String render(String s,String name,String tour){return s.replace("{customerName}",Objects.toString(name,"")).replace("{tourName}",Objects.toString(tour,"your tour"));}
 private String escape(String s){return s==null?"":s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
 private String sanitize(String s){if(s==null)return "Unknown error";return s.replaceAll("(?i)(authorization|token|cvv|card)[^,; ]*","[redacted]").substring(0,Math.min(900,s.length()));}
}
