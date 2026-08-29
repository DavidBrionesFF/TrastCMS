package com.nattechnologies.trastcms.plugins.academy;

import com.nattechnologies.trastcms.plugin.api.AccountSectionProvider;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class AcademyAccountSectionProvider implements AccountSectionProvider {
    private final AcademyService service;
    AcademyAccountSectionProvider(AcademyService service){this.service=service;}
    @Override public String key(){return "academy";}
    @Override public String pluginId(){return "trastacademy";}
    @Override public String label(){return "Mis cursos";}
    @Override public String icon(){return "graduation";}
    @Override public int order(){return 15;}
    @Override public AccountSection load(AccountContext context){
        List<AcademyDtos.EnrollmentResponse> rows=service.accountEnrollments(context.email());
        List<Map<String,Object>> items=rows.stream().map(row->{
            Map<String,Object> item=new LinkedHashMap<>(); item.put("id",row.id()); item.put("courseId",row.courseId()); item.put("title",row.courseTitle());
            item.put("slug",row.courseSlug()); item.put("imageUrl",Optional.ofNullable(row.courseImageUrl()).orElse("")); item.put("status",row.status().name());
            item.put("progress",row.progressPercentage()); item.put("completedLessons",row.completedLessons()); item.put("totalLessons",row.totalLessons());
            item.put("completedQuizzes",row.completedQuizzes()); item.put("totalQuizzes",row.totalQuizzes());
            item.put("description",row.completedLessons()+"/"+row.totalLessons()+" lecciones · "
                    +row.completedQuizzes()+"/"+row.totalQuizzes()+" evaluaciones");
            item.put("href","/academy/learn/"+row.courseSlug()); return item;
        }).toList();
        long completed=rows.stream().filter(row->row.status()==AcademyEnrollmentStatus.COMPLETED).count();
        return new AccountSection(key(),pluginId(),label(),icon(),order(),"READY","Todavía no tiene cursos inscritos.",items,
                Map.of("total",rows.size(),"active",rows.size()-completed,"completed",completed));
    }
}
