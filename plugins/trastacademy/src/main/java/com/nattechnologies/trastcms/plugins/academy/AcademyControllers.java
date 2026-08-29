package com.nattechnologies.trastcms.plugins.academy;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/public/academy")
class AcademyPublicController {
    private final AcademyService service;
    AcademyPublicController(AcademyService service){this.service=service;}

    @GetMapping("/settings") AcademyDtos.SettingsResponse settings(){return service.settingsResponse();}
    @GetMapping("/courses") AcademyDtos.PageResponse<AcademyDtos.CourseSummary> courses(
            @RequestParam(required=false) String search,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="24") int size){
        return service.courses(search,page,size,true);
    }
    @GetMapping("/courses/{slug}") AcademyDtos.CourseDetail course(@PathVariable String slug, Authentication authentication){
        return service.publicCourse(slug,email(authentication));
    }
    private String email(Authentication authentication){return authentication==null||"anonymousUser".equals(authentication.getName())?null:authentication.getName();}
}

@RestController
@RequestMapping("/api/account/academy")
class AcademyAccountController {
    private final AcademyService service;
    AcademyAccountController(AcademyService service){this.service=service;}

    @GetMapping("/courses") List<AcademyDtos.EnrollmentResponse> courses(Authentication auth){return service.accountEnrollments(auth.getName());}
    @GetMapping("/courses/{courseId}") AcademyDtos.CourseDetail course(@PathVariable String courseId,Authentication auth){return service.accountCourse(courseId,auth.getName());}
    @PostMapping("/courses/{courseId}/enroll") @ResponseStatus(HttpStatus.CREATED)
    AcademyDtos.EnrollmentResponse enroll(@PathVariable String courseId,Authentication auth){return service.enrollFree(courseId,auth.getName());}
    @GetMapping("/lessons/{lessonId}") AcademyDtos.LessonResponse lesson(@PathVariable String lessonId,Authentication auth){return service.accountLesson(lessonId,auth.getName());}
    @PutMapping("/lessons/{lessonId}/progress") AcademyDtos.EnrollmentResponse progress(@PathVariable String lessonId,@Valid @RequestBody AcademyDtos.LessonProgressRequest request,Authentication auth){return service.updateLesson(lessonId,auth.getName(),request);}
    @GetMapping("/quizzes/{quizId}") AcademyDtos.QuizResponse quiz(@PathVariable String quizId,Authentication auth){return service.accountQuiz(quizId,auth.getName());}
    @PostMapping("/quizzes/{quizId}/attempts") @ResponseStatus(HttpStatus.CREATED)
    AcademyDtos.QuizAttemptResponse start(@PathVariable String quizId,Authentication auth){return service.startQuiz(quizId,auth.getName());}
    @PostMapping("/attempts/{attemptId}/finish") AcademyDtos.QuizAttemptResponse finish(@PathVariable String attemptId,@Valid @RequestBody AcademyDtos.FinishQuizRequest request,Authentication auth){return service.finishQuiz(attemptId,auth.getName(),request);}
}

@RestController
@RequestMapping("/api/mobile/academy")
class AcademyMobileController {
    private final AcademyService service;
    AcademyMobileController(AcademyService service){this.service=service;}
    @GetMapping("/me/courses") List<AcademyDtos.EnrollmentResponse> courses(Authentication auth){return service.accountEnrollments(auth.getName());}
    @GetMapping("/me/courses/{courseId}") AcademyDtos.CourseDetail course(@PathVariable String courseId,Authentication auth){return service.accountCourse(courseId,auth.getName());}
    @PostMapping("/courses/{courseId}/enroll") AcademyDtos.EnrollmentResponse enroll(@PathVariable String courseId,Authentication auth){return service.enrollFree(courseId,auth.getName());}
    @GetMapping("/lessons/{lessonId}") AcademyDtos.LessonResponse lesson(@PathVariable String lessonId,Authentication auth){return service.accountLesson(lessonId,auth.getName());}
    @PostMapping("/lessons/{lessonId}/complete") AcademyDtos.EnrollmentResponse complete(@PathVariable String lessonId,@RequestBody(required=false) AcademyDtos.LessonProgressRequest request,Authentication auth){
        return service.updateLesson(lessonId,auth.getName(),request==null?new AcademyDtos.LessonProgressRequest(0,true):new AcademyDtos.LessonProgressRequest(request.watchedSeconds(),true));
    }
    @GetMapping("/quizzes/{quizId}") AcademyDtos.QuizResponse quiz(@PathVariable String quizId,Authentication auth){return service.accountQuiz(quizId,auth.getName());}
    @PostMapping("/quizzes/{quizId}/attempts") AcademyDtos.QuizAttemptResponse start(@PathVariable String quizId,Authentication auth){return service.startQuiz(quizId,auth.getName());}
    @PostMapping("/attempts/{attemptId}/finish") AcademyDtos.QuizAttemptResponse finish(@PathVariable String attemptId,@Valid @RequestBody AcademyDtos.FinishQuizRequest request,Authentication auth){return service.finishQuiz(attemptId,auth.getName(),request);}
}

@RestController
@RequestMapping("/api/admin/academy")
class AcademyAdminController {
    private final AcademyService service;
    private final AcademyImportService importer;
    private final AcademyDemoDataService demoData;
    AcademyAdminController(AcademyService service,AcademyImportService importer,AcademyDemoDataService demoData){this.service=service;this.importer=importer;this.demoData=demoData;}
    @GetMapping("/dashboard") AcademyDtos.Dashboard dashboard(){return service.dashboard();}
    @GetMapping("/diagnostics") Map<String,Object> diagnostics(){return service.diagnostics();}
    @GetMapping("/courses") AcademyDtos.PageResponse<AcademyDtos.CourseSummary> courses(@RequestParam(required=false) String search,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size){return service.courses(search,page,size,false);}
    @GetMapping("/courses/{id}") AcademyDtos.CourseDetail course(@PathVariable String id){return service.adminCourse(id);}
    @PostMapping("/courses") @ResponseStatus(HttpStatus.CREATED) AcademyDtos.CourseDetail create(@Valid @RequestBody AcademyDtos.CourseRequest request){return service.saveCourse(null,request);}
    @PutMapping("/courses/{id}") AcademyDtos.CourseDetail update(@PathVariable String id,@Valid @RequestBody AcademyDtos.CourseRequest request){return service.saveCourse(id,request);}
    @DeleteMapping("/courses/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void delete(@PathVariable String id){service.deleteCourse(id);}
    @GetMapping("/enrollments") List<AcademyDtos.EnrollmentResponse> enrollments(){return service.adminEnrollments();}
    @PostMapping("/courses/{courseId}/enrollments") @ResponseStatus(HttpStatus.CREATED) AcademyDtos.EnrollmentResponse enroll(@PathVariable String courseId,@RequestBody Map<String,Object> body){return service.enrollAdmin(courseId,required(body,"email"));}
    @GetMapping("/reviews") List<AcademyDtos.PendingReview> reviews(){return service.pendingReviews();}
    @PostMapping("/attempts/{attemptId}/review") AcademyDtos.QuizAttemptResponse review(@PathVariable String attemptId,@Valid @RequestBody AcademyDtos.ManualReviewRequest request){return service.reviewAttempt(attemptId,request);}
    @GetMapping("/settings") AcademyDtos.SettingsResponse settings(){return service.settingsResponse();}
    @PutMapping("/settings") AcademyDtos.SettingsResponse settings(@Valid @RequestBody AcademyDtos.SettingsRequest request){return service.saveSettings(request);}
    @PostMapping("/imports/preview") AcademyDtos.ImportPreview preview(@Valid @RequestBody AcademyDtos.ImportRequest request){return importer.preview(request);}
    @PostMapping("/imports") @ResponseStatus(HttpStatus.CREATED) AcademyDtos.ImportJobResponse execute(@Valid @RequestBody AcademyDtos.ImportRequest request,Authentication auth){return importer.execute(request,auth.getName());}
    @GetMapping("/imports") List<AcademyDtos.ImportJobResponse> imports(){return importer.history();}
    @PostMapping("/demo-data") AcademyDtos.DemoDataResponse generateDemoData(Authentication auth){return demoData.generate(auth.getName());}
    @DeleteMapping("/demo-data") AcademyDtos.DemoDataResponse deleteDemoData(){return demoData.delete();}
    private String required(Map<String,Object>body,String key){Object value=body.get(key);if(value==null||String.valueOf(value).isBlank())throw new com.nattechnologies.trastcms.service.BadRequestException(key+" es obligatorio");return String.valueOf(value).trim();}
}

@RestController
@RequestMapping("/api/auth")
class AcademyRegistrationController {
    private final AcademyTokenService tokens;
    AcademyRegistrationController(AcademyTokenService tokens){this.tokens=tokens;}
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    AcademyDtos.TokenResponse register(@Valid @RequestBody AcademyDtos.RegisterRequest request){return tokens.register(request);}
}

@RestController
@RequestMapping("/api/auth/token")
class AcademyTokenController {
    private final AcademyTokenService tokens;
    AcademyTokenController(AcademyTokenService tokens){this.tokens=tokens;}
    @PostMapping AcademyDtos.TokenResponse create(@Valid @RequestBody AcademyDtos.TokenRequest request){return tokens.issue(request);}
    @PostMapping("/refresh") AcademyDtos.TokenResponse refresh(@Valid @RequestBody AcademyDtos.RefreshTokenRequest request){return tokens.refresh(request.refreshToken());}
    @PostMapping("/validate") AcademyDtos.TokenResponse validate(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization){return tokens.validate(bearer(authorization));}
    @PostMapping("/revoke") @ResponseStatus(HttpStatus.NO_CONTENT) void revoke(@RequestHeader(value=HttpHeaders.AUTHORIZATION,required=false) String authorization,@RequestBody(required=false) AcademyDtos.RevokeTokenRequest request){
        String value=authorization!=null?bearer(authorization):request==null?null:request.token(); tokens.revoke(value);
    }
    private String bearer(String header){if(header==null||!header.regionMatches(true,0,"Bearer ",0,7))throw new com.nattechnologies.trastcms.service.BadRequestException("Use Authorization: Bearer <token>");return header.substring(7).trim();}
}

@RestController
@RequestMapping("/api/compat/learnpress/v1")
class LearnPressCompatibilityController {
    private final AcademyService service;
    private final AcademyTokenService tokens;
    LearnPressCompatibilityController(AcademyService service,AcademyTokenService tokens){this.service=service;this.tokens=tokens;}

    @PostMapping("/token") Map<String,Object> token(@RequestBody Map<String,Object> body){
        String login=requiredEither(body,"username","email");
        return legacyToken(tokens.issue(new AcademyDtos.TokenRequest(login,required(body,"password"),text(body,"device_name","deviceName"))));
    }
    @PostMapping("/token/refresh") Map<String,Object> refresh(@Valid @RequestBody AcademyDtos.RefreshTokenRequest request){return legacyToken(tokens.refresh(request.refreshToken()));}
    @PostMapping("/token/validate") Map<String,Object> validate(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization){
        AcademyDtos.TokenResponse response=tokens.validate(bearer(authorization));
        return Map.of("code","lp_jwt_auth_valid_token","data",Map.of("status",200),"user_id",response.userId(),"user_email",response.email());
    }
    @PostMapping("/token/register") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> register(@RequestBody Map<String,Object> body){
        AcademyDtos.RegisterRequest request=new AcademyDtos.RegisterRequest(
                requiredEither(body,"email","user_email"),text(body,"display_name","user_display_name","displayName"),
                required(body,"password"),text(body,"phone"),text(body,"device_name","deviceName"));
        return legacyToken(tokens.register(request));
    }
    @GetMapping("/courses") Map<String,Object> courses(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int per_page,@RequestParam(required=false) String search){
        AcademyDtos.PageResponse<AcademyDtos.CourseSummary> result=service.courses(search,Math.max(0,page-1),per_page,true);return success(Map.of("courses",result.content(),"total",result.totalElements(),"pages",result.totalPages()));
    }
    @GetMapping("/courses/{reference}") Map<String,Object> course(@PathVariable String reference,Authentication auth){return success(loadCourse(reference,email(auth)));}
    @PostMapping("/courses/enroll") Map<String,Object> enroll(@RequestBody Map<String,Object> body,Authentication auth){return success(service.enrollFree(requiredEither(body,"course_id","id"),auth.getName()));}
    @PostMapping("/courses/finish") Map<String,Object> finishCourse(@RequestBody Map<String,Object> body,Authentication auth){return success(service.finishCourse(requiredEither(body,"course_id","id"),auth.getName()));}
    @PostMapping("/courses/retake") Map<String,Object> retakeCourse(@RequestBody Map<String,Object> body,Authentication auth){return success(service.retakeCourse(requiredEither(body,"course_id","id"),auth.getName()));}
    @GetMapping("/lessons") Map<String,Object> lessons(@RequestParam(name="course_id") String courseId,Authentication auth){return success(service.accountLessons(courseId,auth.getName()));}
    @GetMapping("/lessons/{id}") Map<String,Object> lesson(@PathVariable String id,Authentication auth){return success(service.accountLesson(id,auth.getName()));}
    @PostMapping("/lessons/finish") Map<String,Object> finishLesson(@RequestBody Map<String,Object> body,Authentication auth){
        return success(service.updateLesson(requiredEither(body,"lesson_id","id"),auth.getName(),new AcademyDtos.LessonProgressRequest(number(body.get("watched_seconds"),0),true)));
    }
    @GetMapping("/quiz") Map<String,Object> quizzes(@RequestParam(name="course_id") String courseId,Authentication auth){return success(service.accountQuizzes(courseId,auth.getName()));}
    @GetMapping("/quiz/{id}") Map<String,Object> quiz(@PathVariable String id,Authentication auth){return success(service.accountQuiz(id,auth.getName()));}
    @PostMapping("/quiz/start") Map<String,Object> startQuiz(@RequestBody Map<String,Object> body,Authentication auth){return success(service.startQuiz(requiredEither(body,"quiz_id","id"),auth.getName()));}
    @PostMapping("/quiz/check_answer") Map<String,Object> check(){return success(Map.of("accepted",true,"message","La respuesta será evaluada al finalizar el intento."));}
    @PostMapping("/quiz/finish") Map<String,Object> finishQuiz(@RequestBody CompatFinishRequest request,Authentication auth){
        List<AcademyDtos.AnswerRequest> answers=request.answers()==null?List.of():request.answers().stream().map(value->new AcademyDtos.AnswerRequest(value.question_id(),value.selected_option_ids(),value.text_answer())).toList();
        return success(service.finishQuiz(request.attempt_id(),auth.getName(),new AcademyDtos.FinishQuizRequest(answers)));
    }
    @GetMapping("/questions") Map<String,Object> questions(@RequestParam(name="quiz_id") String quizId,Authentication auth){return success(service.accountQuestions(quizId,auth.getName()));}
    @GetMapping("/questions/{id}") Map<String,Object> question(@PathVariable String id,Authentication auth){return success(service.accountQuestion(id,auth.getName()));}
    @PostMapping("/users/start-quiz") Map<String,Object> legacyStartQuiz(@RequestBody Map<String,Object> body,Authentication auth){
        AcademyDtos.QuizAttemptResponse attempt=service.startQuiz(requiredEither(body,"item_id","quiz_id"),auth.getName());
        return legacyResult(attempt);
    }
    @PostMapping("/users/submit-quiz") Map<String,Object> legacySubmitQuiz(@RequestBody Map<String,Object> body,Authentication auth){
        String quizId=requiredEither(body,"item_id","quiz_id");
        AcademyDtos.QuizResponse quiz=service.accountQuiz(quizId,auth.getName());
        Map<String,Object> answered=map(body.get("answered"));
        List<AcademyDtos.AnswerRequest> answers=quiz.questions().stream().map(question->{
            Object raw=answered.get(question.id());
            if(question.type()==AcademyQuestionType.SHORT_TEXT||question.type()==AcademyQuestionType.LONG_TEXT){
                return new AcademyDtos.AnswerRequest(question.id(),List.of(),raw==null?null:String.valueOf(raw));
            }
            List<String> selected=raw instanceof Collection<?> values
                    ? values.stream().filter(Objects::nonNull).map(String::valueOf).toList()
                    : raw==null||String.valueOf(raw).isBlank()?List.of():List.of(String.valueOf(raw));
            return new AcademyDtos.AnswerRequest(question.id(),selected,null);
        }).toList();
        return legacyResult(service.finishLatestQuiz(quizId,auth.getName(),new AcademyDtos.FinishQuizRequest(answers)));
    }
    @PostMapping("/users/check-answer") Map<String,Object> legacyCheckAnswer(){return Map.of("status","success","results",Map.of("accepted",true));}
    @GetMapping("/sections/sections-by-course-id/{courseId}") Map<String,Object> sections(@PathVariable String courseId,Authentication auth){return success(service.accountCourse(courseId,auth.getName()).sections());}
    @GetMapping("/section-items/items/{sectionId}") Map<String,Object> items(@PathVariable String sectionId,Authentication auth){return success(service.accountSection(sectionId,auth.getName()));}

    record CompatFinishRequest(String attempt_id,List<CompatAnswer> answers){}
    record CompatAnswer(String question_id,List<String> selected_option_ids,String text_answer){}
    private AcademyDtos.CourseDetail loadCourse(String ref,String email){try{return service.publicCourseById(ref,email);}catch(com.nattechnologies.trastcms.service.NotFoundException ex){return service.publicCourse(ref,email);}}
    private Map<String,Object> success(Object data){return Map.of("status","success","data",data);}
    private Map<String,Object> legacyResult(Object result){return Map.of("status","success","results",result);}
    @SuppressWarnings("unchecked")
    private Map<String,Object> map(Object value){return value instanceof Map<?,?> source?(Map<String,Object>)source:Map.of();}
    private Map<String,Object> legacyToken(AcademyDtos.TokenResponse value){
        Map<String,Object> response=new LinkedHashMap<>();
        response.put("token",value.accessToken()); response.put("refresh_token",value.refreshToken());
        response.put("token_type",value.tokenType()); response.put("expires_in",value.expiresIn());
        response.put("user_id",value.userId()); response.put("user_login",value.email());
        response.put("user_email",value.email()); response.put("user_display_name",value.displayName());
        response.put("role",value.role()); return response;
    }
    private String email(Authentication auth){return auth==null||"anonymousUser".equals(auth.getName())?null:auth.getName();}
    private String required(Map<String,Object>b,String k){Object v=b.get(k);if(v==null||String.valueOf(v).isBlank())throw new com.nattechnologies.trastcms.service.BadRequestException(k+" es obligatorio");return String.valueOf(v).trim();}
    private String requiredEither(Map<String,Object>b,String a,String c){return b.containsKey(a)?required(b,a):required(b,c);}
    private String text(Map<String,Object>b,String...keys){for(String key:keys){Object value=b.get(key);if(value!=null&&!String.valueOf(value).isBlank())return String.valueOf(value).trim();}return null;}
    private int number(Object v,int f){try{return v==null?f:Integer.parseInt(String.valueOf(v));}catch(Exception e){return f;}}
    private String bearer(String header){if(header==null||!header.regionMatches(true,0,"Bearer ",0,7))throw new com.nattechnologies.trastcms.service.BadRequestException("Use Authorization Bearer");return header.substring(7).trim();}
}
