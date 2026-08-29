package com.nattechnologies.trastcms.plugins.academy;

import com.nattechnologies.trastcms.plugin.api.BundledPlugin;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class TrastAcademyPlugin implements BundledPlugin {
    private final AcademyService service;
    TrastAcademyPlugin(AcademyService service){this.service=service;}
    @Override public String id(){return "trastacademy";}
    @Override public String name(){return "TrastAcademy";}
    @Override public String version(){return "1.0.4";}
    @Override public String description(){return "Cursos, lecciones en video, evaluaciones, progreso, importación y API móvil para academias.";}
    @Override public boolean enabledByDefault(){return true;}
    @Override public List<String> optionalPlugins(){return List.of("trastpay","trastaccount","trastcrm");}
    @Override public void onEnabled(){service.initializeDefaults();}
    @Override public List<String> capabilities(){return List.of(
            "academy.courses","academy.video-lessons","academy.quizzes","academy.enrollments",
            "academy.progress","academy.import","academy.mobile-api","academy.learnpress-compat",
            "commerce.sellables","account.sections","builder.blocks","rest.api","mcp.tools","plugin.events");}
    @Override public List<Map<String,Object>> adminMenuItems(){return List.of(Map.of(
            "id","academy","label","Academia","icon","graduation","description","Cursos, videos, evaluaciones e inscripciones.","routeName","academy"));}
    @Override public List<Map<String,Object>> blocks(){return List.of(
            Map.of("type","plugin:trastacademy:course-grid","label","Catálogo de cursos","description","Cuadrícula responsive de cursos publicados.","category","Academia","icon","grid",
                    "schema",Map.of("limit",Map.of("type","number","label","Cantidad","default",6),"featuredOnly",Map.of("type","boolean","label","Solo destacados","default",false))),
            Map.of("type","plugin:trastacademy:course","label","Detalle de curso","description","Presentación e inscripción de un curso.","category","Academia","icon","document",
                    "schema",Map.of("courseSlug",Map.of("type","text","label","Slug del curso"))),
            Map.of("type","plugin:trastacademy:account-progress","label","Progreso de academia","description","Resumen de cursos del usuario autenticado.","category","Academia","icon","chart",
                    "schema",Map.of("limit",Map.of("type","number","label","Cantidad","default",4))));}
}
