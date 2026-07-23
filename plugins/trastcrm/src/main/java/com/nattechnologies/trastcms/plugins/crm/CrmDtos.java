package com.nattechnologies.trastcms.plugins.crm;
import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.*; import java.util.*;
public final class CrmDtos { private CrmDtos(){}
 public record PageResponse<T>(List<T> content,int page,int size,long totalElements,int totalPages){}
 public record Dashboard(long contacts,long companies,long openDeals,long pendingActivities,long forms,long newSubmissions,BigDecimal pipelineValue,List<StageMetric> pipeline,List<SubmissionResponse> recentSubmissions){}
 public record StageMetric(String id,String name,String color,int probability,long deals,BigDecimal value,boolean closed,boolean won){}
 public record CompanyRequest(@NotBlank @Size(max=180) String name,@Size(max=190) String domain,@Email @Size(max=190) String email,@Size(max=60) String phone,@Size(max=500) String address,@Size(max=120) String city,@Size(max=120) String country,CrmCompanyStatus status,@Size(max=10000) String notes){}
 public record CompanyResponse(String id,String name,String domain,String email,String phone,String address,String city,String country,CrmCompanyStatus status,String notes,Instant createdAt,Instant updatedAt){}
 public record ContactRequest(@NotBlank @Size(max=100) String firstName,@Size(max=100) String lastName,@Email @Size(max=190) String email,@Size(max=60) String phone,@Size(max=140) String jobTitle,CrmContactStatus status,@Size(max=120) String source,@Size(max=190) String ownerEmail,@Size(max=2000) String tags,@Size(max=10000) String notes,String companyId){}
 public record ContactResponse(String id,String firstName,String lastName,String email,String phone,String jobTitle,CrmContactStatus status,String source,String ownerEmail,String tags,String notes,CompanyResponse company,Instant createdAt,Instant updatedAt){}
 public record StageRequest(@NotBlank @Size(max=120) String name,@Min(0) @Max(10000) int position,@Min(0) @Max(100) int probability,@Pattern(regexp="#[0-9a-fA-F]{6}") String color,boolean closed,boolean won){}
 public record StageResponse(String id,String name,int position,int probability,String color,boolean closed,boolean won){}
 public record DealRequest(@NotBlank @Size(max=220) String title,@DecimalMin("0") BigDecimal value,@NotBlank @Size(max=10) String currency,LocalDate expectedCloseDate,@Size(max=190) String ownerEmail,@Size(max=10000) String description,String contactId,String companyId,@NotBlank String stageId){}
 public record DealResponse(String id,String title,BigDecimal value,String currency,LocalDate expectedCloseDate,String ownerEmail,String description,ContactResponse contact,CompanyResponse company,StageResponse stage,Instant createdAt,Instant updatedAt){}
 public record ActivityRequest(@NotNull CrmActivityType type,@NotBlank @Size(max=220) String subject,@Size(max=10000) String description,Instant dueAt,boolean completed,@Size(max=190) String assignedTo,String contactId,String companyId,String dealId){}
 public record ActivityResponse(String id,CrmActivityType type,String subject,String description,Instant dueAt,Instant completedAt,String assignedTo,String contactId,String companyId,String dealId,Instant createdAt,Instant updatedAt){}
 public record FormField(@NotBlank @Pattern(regexp="[a-z][a-z0-9_]{1,79}") String name,@NotBlank @Size(max=160) String label,@NotBlank @Pattern(regexp="text|email|phone|textarea|number|select|checkbox|date") String type,boolean required,@Size(max=500) String placeholder,List<String> options){}
 public record FormRequest(@NotBlank @Pattern(regexp="[a-z0-9][a-z0-9-]{2,119}") String formKey,@NotBlank @Size(max=180) String name,@Size(max=1000) String description,@NotBlank @Size(max=500) String successMessage,@Size(max=1000) String notifyEmails,@NotEmpty List<@jakarta.validation.Valid FormField> fields,Map<String,Object> settings,boolean active){}
 public record FormResponse(String id,String formKey,String name,String description,String successMessage,String notifyEmails,List<FormField> fields,Map<String,Object> settings,boolean active,Instant createdAt,Instant updatedAt){}
 public record PublicForm(String formKey,String name,String description,String successMessage,List<FormField> fields,Map<String,Object> settings){}
 public record SubmissionRequest(@NotNull Map<String,Object> values,@Size(max=1000) String sourceUrl){}
 public record SubmissionResponse(String id,String formId,String formName,Map<String,Object> values,CrmSubmissionStatus status,String contactId,String sourceUrl,Instant createdAt){}
 public record SubmissionStatusRequest(@NotNull CrmSubmissionStatus status){}
 public record Message(String message){}
}
