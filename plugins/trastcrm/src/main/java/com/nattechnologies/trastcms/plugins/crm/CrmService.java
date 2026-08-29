package com.nattechnologies.trastcms.plugins.crm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nattechnologies.trastcms.plugin.api.PluginEvent;
import com.nattechnologies.trastcms.service.BadRequestException;
import com.nattechnologies.trastcms.service.NotFoundException;
import com.nattechnologies.trastcms.service.PluginEventDispatcher;
import com.nattechnologies.trastcms.service.SiteSettingService;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class CrmService {
    private static final String DEMO_DISMISSED_KEY = "plugins.trastcrm.demo.dismissed";
    private final CrmCompanyRepository companies;
    private final CrmContactRepository contacts;
    private final CrmPipelineStageRepository stages;
    private final CrmDealRepository deals;
    private final CrmActivityRepository activities;
    private final CrmFormRepository forms;
    private final CrmSubmissionRepository submissions;
    private final ObjectMapper mapper;
    private final PluginEventDispatcher events;
    private final CrmPluginGuard guard;
    private final SiteSettingService siteSettings;

    public CrmService(CrmCompanyRepository companies, CrmContactRepository contacts,
                      CrmPipelineStageRepository stages, CrmDealRepository deals,
                      CrmActivityRepository activities, CrmFormRepository forms,
                      CrmSubmissionRepository submissions, ObjectMapper mapper,
                      PluginEventDispatcher events, CrmPluginGuard guard,
                      SiteSettingService siteSettings) {
        this.companies = companies;
        this.contacts = contacts;
        this.stages = stages;
        this.deals = deals;
        this.activities = activities;
        this.forms = forms;
        this.submissions = submissions;
        this.mapper = mapper;
        this.events = events;
        this.guard = guard;
        this.siteSettings = siteSettings;
    }

    @Transactional
    public void initializeDefaults() {
        if (stages.count() == 0) {
            saveStage(new CrmDtos.StageRequest("Nuevo", 10, 10, "#64748b", false, false), false);
            saveStage(new CrmDtos.StageRequest("Calificado", 20, 30, "#0ea5e9", false, false), false);
            saveStage(new CrmDtos.StageRequest("Propuesta", 30, 60, "#8b5cf6", false, false), false);
            saveStage(new CrmDtos.StageRequest("Ganado", 40, 100, "#10b981", true, true), false);
            saveStage(new CrmDtos.StageRequest("Perdido", 50, 0, "#ef4444", true, false), false);
        }
        if (forms.findByFormKey("contacto").isEmpty()) {
            CrmDtos.FormRequest request = new CrmDtos.FormRequest(
                    "contacto", "Formulario de contacto",
                    "Recolecta consultas generales desde la página Contáctanos.",
                    "Gracias. Hemos recibido su mensaje y nos pondremos en contacto.",
                    "", List.of(
                    new CrmDtos.FormField("nombre", "Nombre", "text", true, "Su nombre", List.of()),
                    new CrmDtos.FormField("email", "Correo electrónico", "email", true, "correo@empresa.com", List.of()),
                    new CrmDtos.FormField("telefono", "Teléfono", "phone", false, "+504", List.of()),
                    new CrmDtos.FormField("empresa", "Empresa", "text", false, "Nombre de la empresa", List.of()),
                    new CrmDtos.FormField("mensaje", "Mensaje", "textarea", true, "¿Cómo podemos ayudarle?", List.of())
            ), Map.of(
                    "submitLabel", "Enviar mensaje",
                    "createLead", true,
                    "createDeal", false,
                    "createFollowUp", true,
                    "leadSource", "Formulario: Contacto"), true);
            saveForm(null, request, false);
        }
        if (forms.findByFormKey("proyecto-web").isEmpty()) {
            CrmDtos.FormRequest request = new CrmDtos.FormRequest(
                    "proyecto-web", "Solicitud de proyecto web",
                    "Califica prospectos interesados en una web, tienda, portal o solución personalizada.",
                    "Gracias. Revisaremos su proyecto y le contactaremos para coordinar una conversación.",
                    "", List.of(
                    new CrmDtos.FormField("nombre", "Nombre", "text", true, "Su nombre completo", List.of()),
                    new CrmDtos.FormField("email", "Correo electrónico", "email", true, "correo@empresa.com", List.of()),
                    new CrmDtos.FormField("telefono", "WhatsApp o teléfono", "phone", true, "+504", List.of()),
                    new CrmDtos.FormField("empresa", "Empresa o proyecto", "text", false, "Nombre del negocio", List.of()),
                    new CrmDtos.FormField("tipo_proyecto", "¿Qué desea construir?", "select", true, "", List.of(
                            "Sitio corporativo", "Tienda en línea", "Landing page", "Portal o plataforma",
                            "Rediseño de sitio", "Integración o desarrollo especial", "Aún no estoy seguro")),
                    new CrmDtos.FormField("presupuesto", "Presupuesto estimado", "select", false, "", List.of(
                            "Menos de USD 500", "USD 500 - 1,500", "USD 1,500 - 5,000",
                            "Más de USD 5,000", "Necesito orientación")),
                    new CrmDtos.FormField("plazo", "¿Cuándo desea iniciar?", "select", false, "", List.of(
                            "Lo antes posible", "En 1 mes", "En 2 a 3 meses", "Más adelante")),
                    new CrmDtos.FormField("objetivo", "Objetivo principal", "textarea", true,
                            "Cuéntenos qué necesita lograr con el proyecto", List.of())
            ), Map.of(
                    "submitLabel", "Solicitar evaluación",
                    "createLead", true,
                    "createDeal", true,
                    "createFollowUp", true,
                    "leadSource", "Formulario: Proyecto web"), true);
            saveForm(null, request, false);
        }
        if (companies.count() == 0
                && contacts.count() == 0
                && deals.count() == 0
                && !siteSettings.getBoolean(DEMO_DISMISSED_KEY, false)) {
            generateDemoData();
        }
    }

    @Transactional
    public synchronized CrmDtos.DemoDataResponse generateDemoData() {
        guard.requireEnabled();
        siteSettings.put(DEMO_DISMISSED_KEY, "false");
        if (!companies.findByDemoDataTrue().isEmpty()) {
            return demoResponse("Los datos demo de TrastCRM ya están disponibles.");
        }

        CrmCompany coffee = demoCompany(
                "Café Montaña Azul", "montana-azul.demo", "ventas@montana-azul.demo",
                "+504 9990-1101", "Santa Rosa de Copán", "Honduras",
                "Productor interesado en tienda online y seguimiento comercial.");
        CrmCompany tours = demoCompany(
                "Roatán Experience", "roatan-experience.demo", "reservas@roatan-experience.demo",
                "+504 9990-2202", "Roatán", "Honduras",
                "Operador turístico evaluando portal de reservas y CRM.");
        CrmCompany accounting = demoCompany(
                "Contabilidad Ágil", "contabilidad-agil.demo", "direccion@contabilidad-agil.demo",
                "+504 9990-3303", "San Pedro Sula", "Honduras",
                "Firma interesada en SaaS de gestión para sus clientes.");

        CrmContact ana = demoContact(coffee, "Ana", "López", "ana@montana-azul.demo",
                "+504 9901-1001", "Gerente general", CrmContactStatus.PROSPECT,
                "Formulario: Proyecto web");
        CrmContact kevin = demoContact(tours, "Kevin", "Bodden", "kevin@roatan-experience.demo",
                "+504 9902-2002", "Director comercial", CrmContactStatus.LEAD,
                "Formulario: Contacto");
        CrmContact sofia = demoContact(accounting, "Sofía", "Martínez", "sofia@contabilidad-agil.demo",
                "+504 9903-3003", "Socia directora", CrmContactStatus.CUSTOMER,
                "Referencia");
        demoContact(null, "Miguel", "Castillo", "miguel@startup-demo.com",
                "+504 9904-4004", "Fundador", CrmContactStatus.LEAD,
                "Página de precios SaaS");

        List<CrmPipelineStage> pipeline = stages.findAllByOrderByPositionAsc();
        CrmDeal web = demoDeal(
                "Tienda online para Café Montaña Azul", new BigDecimal("3200.00"), "USD",
                ana, coffee, stageByName(pipeline, "Propuesta"), 21,
                "Diseño, catálogo, pagos, TrastCRM y capacitación.");
        CrmDeal portal = demoDeal(
                "Portal de reservas Roatán Experience", new BigDecimal("6800.00"), "USD",
                kevin, tours, stageByName(pipeline, "Calificado"), 45,
                "Portal turístico con disponibilidad, pagos y automatización.");
        CrmDeal saas = demoDeal(
                "Licenciamiento SaaS para Contabilidad Ágil", new BigDecimal("12000.00"), "USD",
                sofia, accounting, stageByName(pipeline, "Ganado"), 0,
                "Implementación anual de TrastSaaS para múltiples clientes.");

        demoActivity(CrmActivityType.MEETING, "Presentación de propuesta comercial",
                "Revisar alcance, cronograma y condiciones de pago.", Instant.now().plus(Duration.ofDays(2)),
                ana, coffee, web, false);
        demoActivity(CrmActivityType.CALL, "Llamada de descubrimiento",
                "Confirmar número de tours, temporadas y métodos de pago.", Instant.now().plus(Duration.ofDays(1)),
                kevin, tours, portal, false);
        demoActivity(CrmActivityType.TASK, "Preparar contrato anual",
                "Enviar contrato y credenciales del portal SaaS.", Instant.now().minus(Duration.ofDays(1)),
                sofia, accounting, saas, true);

        if (forms.findByFormKey("diagnostico-digital-demo").isEmpty()) {
            CrmDtos.FormRequest request = new CrmDtos.FormRequest(
                    "diagnostico-digital-demo",
                    "Diagnóstico digital",
                    "Formulario demo para calificar negocios interesados en una web, tienda o SaaS.",
                    "Gracias. Prepararemos un diagnóstico inicial y le contactaremos.",
                    "",
                    List.of(
                            new CrmDtos.FormField("nombre", "Nombre", "text", true, "Su nombre", List.of()),
                            new CrmDtos.FormField("email", "Correo", "email", true, "correo@empresa.com", List.of()),
                            new CrmDtos.FormField("solucion", "Solución de interés", "select", true, "", List.of(
                                    "Sitio web", "Tienda online", "Software SaaS", "CRM", "Aún no estoy seguro")),
                            new CrmDtos.FormField("objetivo", "Objetivo", "textarea", true,
                                    "¿Qué necesita mejorar?", List.of())),
                    Map.of(
                            "submitLabel", "Solicitar diagnóstico",
                            "createLead", true,
                            "createDeal", true,
                            "createFollowUp", true,
                            "leadSource", "Formulario demo: Diagnóstico digital"),
                    true);
            CrmForm form = new CrmForm();
            form.formKey = request.formKey();
            form.name = request.name();
            form.description = request.description();
            form.successMessage = request.successMessage();
            form.notifyEmails = request.notifyEmails();
            form.fieldsJson = write(request.fields());
            form.settingsJson = write(request.settings());
            form.active = true;
            form.demoData = true;
            forms.save(form);
        }

        events.publish(PluginEvent.of("crm.demo.generated", Map.of(
                "companies", companies.findByDemoDataTrue().size(),
                "contacts", contacts.findByDemoDataTrue().size(),
                "deals", deals.findByDemoDataTrue().size())));
        return demoResponse("Datos demo de TrastCRM generados correctamente.");
    }

    @Transactional
    public CrmDtos.DemoDataResponse deleteDemoData() {
        guard.requireEnabled();
        siteSettings.put(DEMO_DISMISSED_KEY, "true");
        List<CrmActivity> demoActivities = activities.findByDemoDataTrue();
        List<CrmDeal> demoDeals = deals.findByDemoDataTrue();
        List<CrmContact> demoContacts = contacts.findByDemoDataTrue();
        List<CrmCompany> demoCompanies = companies.findByDemoDataTrue();
        List<CrmForm> demoForms = forms.findByDemoDataTrue();

        activities.deleteAll(demoActivities);
        deals.deleteAll(demoDeals);
        contacts.deleteAll(demoContacts);
        companies.deleteAll(demoCompanies);
        demoForms.stream()
                .filter(form -> submissions.findAll().stream()
                        .noneMatch(submission -> submission.form.id.equals(form.id)))
                .forEach(forms::delete);

        events.publish(PluginEvent.of("crm.demo.deleted", Map.of(
                "companies", demoCompanies.size(),
                "contacts", demoContacts.size(),
                "deals", demoDeals.size(),
                "activities", demoActivities.size())));
        return new CrmDtos.DemoDataResponse(
                "Datos demo de TrastCRM eliminados.", 0, 0, 0, 0);
    }

    private CrmDtos.DemoDataResponse demoResponse(String message) {
        return new CrmDtos.DemoDataResponse(
                message,
                companies.findByDemoDataTrue().size(),
                contacts.findByDemoDataTrue().size(),
                deals.findByDemoDataTrue().size(),
                activities.findByDemoDataTrue().size());
    }

    private CrmCompany demoCompany(
            String name, String domain, String email, String phone,
            String city, String country, String notes) {
        CrmCompany company = new CrmCompany();
        company.name = name;
        company.domain = domain;
        company.email = email;
        company.phone = phone;
        company.city = city;
        company.country = country;
        company.status = CrmCompanyStatus.PROSPECT;
        company.notes = notes;
        company.demoData = true;
        return companies.save(company);
    }

    private CrmContact demoContact(
            CrmCompany company, String firstName, String lastName, String email,
            String phone, String jobTitle, CrmContactStatus status, String source) {
        CrmContact contact = new CrmContact();
        contact.company = company;
        contact.firstName = firstName;
        contact.lastName = lastName;
        contact.email = email;
        contact.phone = phone;
        contact.jobTitle = jobTitle;
        contact.status = status;
        contact.source = source;
        contact.tags = "demo, prospecto";
        contact.demoData = true;
        return contacts.save(contact);
    }

    private CrmDeal demoDeal(
            String title, BigDecimal value, String currency, CrmContact contact,
            CrmCompany company, CrmPipelineStage stage, int closeInDays,
            String description) {
        CrmDeal deal = new CrmDeal();
        deal.title = title;
        deal.value = value;
        deal.currency = currency;
        deal.contact = contact;
        deal.company = company;
        deal.stage = stage;
        deal.expectedCloseDate = closeInDays == 0 ? LocalDate.now() : LocalDate.now().plusDays(closeInDays);
        deal.description = description;
        deal.demoData = true;
        return deals.save(deal);
    }

    private void demoActivity(
            CrmActivityType type, String subject, String description, Instant dueAt,
            CrmContact contact, CrmCompany company, CrmDeal deal, boolean completed) {
        CrmActivity activity = new CrmActivity();
        activity.type = type;
        activity.subject = subject;
        activity.description = description;
        activity.dueAt = dueAt;
        activity.contact = contact;
        activity.company = company;
        activity.deal = deal;
        activity.completedAt = completed ? Instant.now() : null;
        activity.demoData = true;
        activities.save(activity);
    }

    private CrmPipelineStage stageByName(List<CrmPipelineStage> pipeline, String name) {
        return pipeline.stream()
                .filter(stage -> stage.name.equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> pipeline.get(0));
    }

    @Transactional(readOnly = true)
    public CrmDtos.Dashboard dashboard() {
        guard.requireEnabled();
        List<CrmDeal> allDeals = deals.findAll();
        Map<String, List<CrmDeal>> byStage = new HashMap<>();
        allDeals.forEach(deal -> byStage.computeIfAbsent(deal.stage.id, ignored -> new ArrayList<>()).add(deal));
        List<CrmDtos.StageMetric> pipeline = stages.findAllByOrderByPositionAsc().stream().map(stage -> {
            List<CrmDeal> stageDeals = byStage.getOrDefault(stage.id, List.of());
            BigDecimal value = stageDeals.stream().map(deal -> deal.value).reduce(BigDecimal.ZERO, BigDecimal::add);
            return new CrmDtos.StageMetric(stage.id, stage.name, stage.color, stage.probability,
                    stageDeals.size(), value, stage.closed, stage.won);
        }).toList();
        BigDecimal pipelineValue = allDeals.stream().filter(deal -> !deal.stage.closed)
                .map(deal -> deal.value).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<CrmDtos.SubmissionResponse> recent = submissions.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 5))
                .stream().map(this::submissionResponse).toList();
        return new CrmDtos.Dashboard(contacts.count(), companies.count(), deals.countByStageClosedFalse(),
                activities.countByCompletedAtIsNull(), forms.count(), submissions.countByStatus(CrmSubmissionStatus.NEW),
                pipelineValue, pipeline, recent);
    }

    @Transactional(readOnly = true)
    public CrmDtos.PageResponse<CrmDtos.ContactResponse> contacts(String search, CrmContactStatus status, int page, int size) {
        guard.requireEnabled();
        return page(contacts.search(cleanSearch(search), status, pageable(page, size)).map(this::contactResponse));
    }

    @Transactional
    public CrmDtos.ContactResponse saveContact(String id, CrmDtos.ContactRequest request) {
        guard.requireEnabled();
        CrmContact entity = id == null ? new CrmContact() : requireContact(id);
        entity.firstName = request.firstName().trim(); entity.lastName = blank(request.lastName());
        entity.email = lower(request.email()); entity.phone = blank(request.phone()); entity.jobTitle = blank(request.jobTitle());
        entity.status = request.status() == null ? CrmContactStatus.LEAD : request.status(); entity.source = blank(request.source());
        entity.ownerEmail = lower(request.ownerEmail()); entity.tags = blank(request.tags()); entity.notes = blank(request.notes());
        entity.company = requireOptionalCompany(request.companyId());
        entity = contacts.save(entity);
        events.publish(PluginEvent.of(id == null ? "crm.contact.created" : "crm.contact.updated",
                Map.of("id", entity.id, "email", entity.email == null ? "" : entity.email, "status", entity.status.name())));
        return contactResponse(entity);
    }

    @Transactional
    public void deleteContact(String id) { guard.requireEnabled(); contacts.delete(requireContact(id)); }

    @Transactional(readOnly = true)
    public CrmDtos.PageResponse<CrmDtos.CompanyResponse> companies(String search, int page, int size) {
        guard.requireEnabled(); return page(companies.search(cleanSearch(search), pageable(page, size)).map(this::companyResponse));
    }

    @Transactional
    public CrmDtos.CompanyResponse saveCompany(String id, CrmDtos.CompanyRequest request) {
        guard.requireEnabled(); CrmCompany entity = id == null ? new CrmCompany() : requireCompany(id);
        entity.name=request.name().trim();entity.domain=blank(request.domain());entity.email=lower(request.email());entity.phone=blank(request.phone());
        entity.address=blank(request.address());entity.city=blank(request.city());entity.country=blank(request.country());
        entity.status=request.status()==null?CrmCompanyStatus.PROSPECT:request.status();entity.notes=blank(request.notes());
        return companyResponse(companies.save(entity));
    }
    @Transactional public void deleteCompany(String id){guard.requireEnabled();companies.delete(requireCompany(id));}

    @Transactional(readOnly = true)
    public List<CrmDtos.StageResponse> stages(){guard.requireEnabled();return stages.findAllByOrderByPositionAsc().stream().map(this::stageResponse).toList();}
    @Transactional public CrmDtos.StageResponse saveStage(String id, CrmDtos.StageRequest request){guard.requireEnabled();return saveStageInternal(id,request);}
    private CrmDtos.StageResponse saveStage(CrmDtos.StageRequest request, boolean check){return saveStageInternal(null,request);}
    private CrmDtos.StageResponse saveStageInternal(String id, CrmDtos.StageRequest request){CrmPipelineStage entity=id==null?new CrmPipelineStage():requireStage(id);entity.name=request.name().trim();entity.position=request.position();entity.probability=request.probability();entity.color=request.color();entity.closed=request.closed();entity.won=request.won();return stageResponse(stages.save(entity));}

    @Transactional(readOnly = true)
    public CrmDtos.PageResponse<CrmDtos.DealResponse> deals(int page,int size){guard.requireEnabled();return page(deals.findAllByOrderByUpdatedAtDesc(pageable(page,size)).map(this::dealResponse));}
    @Transactional
    public CrmDtos.DealResponse saveDeal(String id,CrmDtos.DealRequest request){guard.requireEnabled();CrmDeal entity=id==null?new CrmDeal():requireDeal(id);entity.title=request.title().trim();entity.value=request.value()==null?BigDecimal.ZERO:request.value();entity.currency=request.currency().trim().toUpperCase(Locale.ROOT);entity.expectedCloseDate=request.expectedCloseDate();entity.ownerEmail=lower(request.ownerEmail());entity.description=blank(request.description());entity.contact=requireOptionalContact(request.contactId());entity.company=requireOptionalCompany(request.companyId());entity.stage=requireStage(request.stageId());entity=deals.save(entity);events.publish(PluginEvent.of(id==null?"crm.deal.created":"crm.deal.updated",Map.of("id",entity.id,"stage",entity.stage.name,"value",entity.value)));return dealResponse(entity);}
    @Transactional public void deleteDeal(String id){guard.requireEnabled();deals.delete(requireDeal(id));}

    @Transactional(readOnly = true)
    public CrmDtos.PageResponse<CrmDtos.ActivityResponse> activities(int page,int size){guard.requireEnabled();return page(activities.findAllByOrderByCreatedAtDesc(pageable(page,size)).map(this::activityResponse));}
    @Transactional
    public CrmDtos.ActivityResponse saveActivity(String id,CrmDtos.ActivityRequest request){guard.requireEnabled();CrmActivity entity=id==null?new CrmActivity():requireActivity(id);entity.type=request.type();entity.subject=request.subject().trim();entity.description=blank(request.description());entity.dueAt=request.dueAt();entity.completedAt=request.completed()?(entity.completedAt==null?Instant.now():entity.completedAt):null;entity.assignedTo=lower(request.assignedTo());entity.contact=requireOptionalContact(request.contactId());entity.company=requireOptionalCompany(request.companyId());entity.deal=requireOptionalDeal(request.dealId());return activityResponse(activities.save(entity));}
    @Transactional public void deleteActivity(String id){guard.requireEnabled();activities.delete(requireActivity(id));}

    @Transactional(readOnly = true)
    public List<CrmDtos.FormResponse> forms(){guard.requireEnabled();return forms.findAllByOrderByUpdatedAtDesc().stream().map(this::formResponse).toList();}
    @Transactional public CrmDtos.FormResponse saveForm(String id,CrmDtos.FormRequest request){guard.requireEnabled();return saveForm(id,request,true);}
    private CrmDtos.FormResponse saveForm(String id,CrmDtos.FormRequest request,boolean publish){
        CrmForm entity=id==null?new CrmForm():requireForm(id);
        forms.findByFormKey(request.formKey()).filter(found->!Objects.equals(found.id,id)).ifPresent(found->{throw new BadRequestException("Ya existe un formulario con ese identificador");});
        validateFields(request.fields());entity.formKey=request.formKey();entity.name=request.name().trim();entity.description=blank(request.description());entity.successMessage=request.successMessage().trim();entity.notifyEmails=blank(request.notifyEmails());entity.fieldsJson=write(request.fields());entity.settingsJson=write(request.settings()==null?Map.of():request.settings());entity.active=request.active();entity=forms.save(entity);
        if(publish)events.publish(PluginEvent.of(id==null?"crm.form.created":"crm.form.updated",Map.of("id",entity.id,"formKey",entity.formKey)));
        return formResponse(entity);
    }
    @Transactional public void deleteForm(String id){guard.requireEnabled();if(submissions.findAll().stream().anyMatch(item->item.form.id.equals(id)))throw new BadRequestException("El formulario tiene envíos y no puede eliminarse; desactívelo");forms.delete(requireForm(id));}

    @Transactional(readOnly = true)
    public CrmDtos.PublicForm publicForm(String key){guard.requireEnabled();return publicFormResponse(forms.findByFormKeyAndActiveTrue(key).orElseThrow(()->new NotFoundException("Formulario no encontrado")));}

    @Transactional
    public CrmDtos.Message submit(String key,CrmDtos.SubmissionRequest request,String ip,String userAgent){
        guard.requireEnabled();CrmForm form=forms.findByFormKeyAndActiveTrue(key).orElseThrow(()->new NotFoundException("Formulario no encontrado"));
        List<CrmDtos.FormField> fields = readFields(form.fieldsJson);
        Map<String, Object> normalized = validateSubmission(
                fields,
                request.values());
        Map<String, Object> formSettings = readMap(form.settingsJson);
        CrmCompany company = createCompanyFromSubmission(
                normalized,
                formSettings);
        CrmContact contact = createLead(
                normalized,
                form,
                formSettings,
                company);
        CrmDeal deal = createDealFromSubmission(
                normalized,
                form,
                formSettings,
                contact,
                company);
        createFollowUpActivity(
                normalized,
                form,
                formSettings,
                contact,
                company,
                deal);

        CrmSubmission submission = new CrmSubmission();
        submission.form = form;
        submission.contact = contact;
        submission.payloadJson = write(normalized);
        submission.status = CrmSubmissionStatus.NEW;
        submission.sourceUrl = blank(request.sourceUrl());
        submission.ipHash = hash(ip);
        submission.userAgent = truncate(userAgent, 1000);
        submissions.save(submission);

        events.publish(PluginEvent.of(
                "crm.form.submitted",
                Map.of(
                        "submissionId", submission.id,
                        "formKey", form.formKey,
                        "contactId", contact == null ? "" : contact.id,
                        "dealId", deal == null ? "" : deal.id)));
        return new CrmDtos.Message(form.successMessage);
    }

    @Transactional(readOnly = true)
    public CrmDtos.PageResponse<CrmDtos.SubmissionResponse> submissions(int page,int size){guard.requireEnabled();return page(submissions.findAllByOrderByCreatedAtDesc(pageable(page,size)).map(this::submissionResponse));}
    @Transactional public CrmDtos.SubmissionResponse updateSubmission(String id,CrmDtos.SubmissionStatusRequest request){guard.requireEnabled();CrmSubmission entity=requireSubmission(id);entity.status=request.status();return submissionResponse(submissions.save(entity));}
    @Transactional public void deleteSubmission(String id){guard.requireEnabled();submissions.delete(requireSubmission(id));}

    private CrmCompany createCompanyFromSubmission(
            Map<String, Object> values,
            Map<String, Object> settings) {
        if (!flag(settings, "createLead", true)) {
            return null;
        }
        String name = blank(String.valueOf(
                values.getOrDefault("empresa", "")));
        if (name == null) {
            return null;
        }
        return companies.findByNameIgnoreCase(name)
                .orElseGet(() -> {
                    CrmCompany company = new CrmCompany();
                    company.name = truncate(name, 180);
                    company.status = CrmCompanyStatus.PROSPECT;
                    company.country = "Honduras";
                    company.notes = "Empresa creada automáticamente desde un formulario web.";
                    return companies.save(company);
                });
    }

    private CrmContact createLead(
            Map<String, Object> values,
            CrmForm form,
            Map<String, Object> settings,
            CrmCompany company) {
        if (!flag(settings, "createLead", true)) {
            return null;
        }
        String email = lower(String.valueOf(
                values.getOrDefault("email", "")));
        if (email == null) {
            return null;
        }
        CrmContact contact = contacts.findByEmailIgnoreCase(email)
                .orElseGet(CrmContact::new);
        String full = String.valueOf(values.getOrDefault(
                "nombre",
                "Contacto web")).trim();
        String[] parts = full.split("\\s+", 2);
        contact.firstName = truncate(parts[0], 100);
        contact.lastName = parts.length > 1
                ? truncate(parts[1], 100)
                : contact.lastName;
        contact.email = truncate(email, 190);
        contact.phone = truncate(blank(String.valueOf(
                values.getOrDefault("telefono", ""))), 60);
        contact.source = truncate(String.valueOf(settings.getOrDefault(
                "leadSource",
                "Formulario: " + form.name)), 120);
        contact.status = CrmContactStatus.LEAD;
        contact.company = company;
        contact.tags = mergeTags(contact.tags, form.formKey, "web");
        contact.notes = mergeNotes(
                contact.notes,
                "Nueva solicitud recibida desde " + form.name + ".\n"
                        + submissionSummary(values));
        return contacts.save(contact);
    }

    private CrmDeal createDealFromSubmission(
            Map<String, Object> values,
            CrmForm form,
            Map<String, Object> settings,
            CrmContact contact,
            CrmCompany company) {
        if (!flag(settings, "createDeal", false)
                || contact == null) {
            return null;
        }
        CrmPipelineStage stage = stages.findAllByOrderByPositionAsc()
                .stream()
                .filter(candidate -> !candidate.closed)
                .findFirst()
                .orElse(null);
        if (stage == null) {
            return null;
        }
        CrmDeal deal = new CrmDeal();
        String subject = company != null
                ? company.name
                : contact.firstName + (contact.lastName == null
                        ? ""
                        : " " + contact.lastName);
        deal.title = truncate(form.name + " · " + subject, 220);
        deal.value = BigDecimal.ZERO;
        deal.currency = "HNL";
        deal.description = submissionSummary(values);
        deal.contact = contact;
        deal.company = company;
        deal.stage = stage;
        return deals.save(deal);
    }

    private void createFollowUpActivity(
            Map<String, Object> values,
            CrmForm form,
            Map<String, Object> settings,
            CrmContact contact,
            CrmCompany company,
            CrmDeal deal) {
        if (!flag(settings, "createFollowUp", true)
                || contact == null) {
            return;
        }
        CrmActivity activity = new CrmActivity();
        activity.type = CrmActivityType.TASK;
        activity.subject = truncate(
                "Dar seguimiento a " + contact.firstName
                        + " · " + form.name,
                220);
        activity.description = "Revise la solicitud, califique el lead y defina el próximo paso.\n\n"
                + submissionSummary(values);
        activity.dueAt = Instant.now().plus(1, java.time.temporal.ChronoUnit.DAYS);
        activity.contact = contact;
        activity.company = company;
        activity.deal = deal;
        activities.save(activity);
    }

    private boolean flag(
            Map<String, Object> settings,
            String key,
            boolean fallback) {
        return Boolean.parseBoolean(String.valueOf(
                settings.getOrDefault(key, fallback)));
    }

    private String submissionSummary(Map<String, Object> values) {
        return values.entrySet().stream()
                .filter(entry -> entry.getValue() != null
                        && !String.valueOf(entry.getValue()).isBlank())
                .map(entry -> entry.getKey().replace('_', ' ')
                        + ": " + String.valueOf(entry.getValue()))
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private String mergeTags(String current, String... values) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        if (current != null) {
            Arrays.stream(current.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .forEach(tags::add);
        }
        Arrays.stream(values)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .forEach(tags::add);
        return truncate(String.join(", ", tags), 500);
    }

    private String mergeNotes(String current, String addition) {
        String normalized = blank(current);
        String value = normalized == null
                ? addition
                : normalized + "\n\n---\n" + addition;
        return truncate(value, 10000);
    }

    @Transactional
    public void captureTravelEvent(PluginEvent event) {
        if (event == null || event.type() == null || !event.type().startsWith("travel.booking.")) return;
        Map<String, Object> payload = event.payload() == null ? Map.of() : event.payload();
        String email = lower(payloadText(payload, "customerEmail"));
        if (email == null) return;

        String fullName = payloadText(payload, "customerName");
        String[] parts = (fullName == null || fullName.isBlank() ? "Viajero" : fullName.trim()).split("\\s+", 2);
        CrmContact contact = contacts.findByEmailIgnoreCase(email).orElseGet(CrmContact::new);
        contact.firstName = truncate(parts[0], 100);
        if (parts.length > 1) contact.lastName = truncate(parts[1], 100);
        contact.email = truncate(email, 190);
        contact.source = "TrastTravel";
        contact.tags = mergeTags(contact.tags, "travel", "cliente-web");
        String bookingCode = payloadText(payload, "bookingCode");
        String tourName = payloadText(payload, "tourName");
        contact.notes = mergeNotes(contact.notes,
                "Evento de TrastTravel: " + event.type()
                        + (bookingCode == null ? "" : "\nReservación: " + bookingCode)
                        + (tourName == null ? "" : "\nTour: " + tourName));
        if ("travel.booking.confirmed".equals(event.type())) {
            contact.status = CrmContactStatus.CUSTOMER;
        } else if (contact.status == null) {
            contact.status = CrmContactStatus.LEAD;
        }
        contact = contacts.save(contact);

        String dealTitle = bookingCode == null
                ? "Reservación turística · " + contact.firstName
                : "Viaje " + bookingCode + (tourName == null ? "" : " · " + tourName);
        CrmDeal deal = deals.findByTitleIgnoreCase(dealTitle).orElse(null);
        if ("travel.booking.created".equals(event.type())) {
            CrmPipelineStage firstOpen = stages.findAllByOrderByPositionAsc().stream()
                    .filter(stage -> !stage.closed)
                    .findFirst().orElse(null);
            if (firstOpen != null) {
                if (deal == null) deal = new CrmDeal();
                deal.title = truncate(dealTitle, 220);
                deal.value = payloadDecimal(payload, "total");
                deal.currency = Optional.ofNullable(payloadText(payload, "currency")).orElse("USD");
                deal.description = "Reservación creada automáticamente por TrastTravel."
                        + (payloadText(payload, "startsAt") == null ? "" : "\nSalida: " + payloadText(payload, "startsAt"));
                deal.contact = contact;
                deal.company = contact.company;
                deal.stage = firstOpen;
                try {
                    String startsAt = payloadText(payload, "startsAt");
                    if (startsAt != null) deal.expectedCloseDate = Instant.parse(startsAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                } catch (RuntimeException ignored) { }
                deal = deals.save(deal);
            }
        } else if (deal != null && "travel.booking.confirmed".equals(event.type())) {
            CrmPipelineStage won = stages.findAllByOrderByPositionAsc().stream()
                    .filter(stage -> stage.closed && stage.won)
                    .findFirst().orElse(null);
            if (won != null) {
                deal.stage = won;
                deals.save(deal);
            }
        }

        CrmActivity activity = new CrmActivity();
        activity.contact = contact;
        activity.company = contact.company;
        activity.deal = deal;
        activity.type = switch (event.type()) {
            case "travel.booking.confirmed", "travel.booking.paid" -> CrmActivityType.NOTE;
            case "travel.booking.cancelled" -> CrmActivityType.CALL;
            default -> CrmActivityType.TASK;
        };
        activity.subject = truncate(switch (event.type()) {
            case "travel.booking.confirmed" -> "Viaje confirmado · " + Optional.ofNullable(bookingCode).orElse(email);
            case "travel.booking.paid" -> "Pago completado · " + Optional.ofNullable(bookingCode).orElse(email);
            case "travel.booking.cancelled" -> "Dar seguimiento a cancelación · " + Optional.ofNullable(bookingCode).orElse(email);
            default -> "Dar seguimiento a reservación · " + Optional.ofNullable(bookingCode).orElse(email);
        }, 220);
        activity.description = "Evento recibido desde TrastTravel: " + event.type();
        if (activity.type != CrmActivityType.NOTE) {
            activity.dueAt = Instant.now().plus(1, java.time.temporal.ChronoUnit.DAYS);
        } else {
            activity.completedAt = Instant.now();
        }
        activities.save(activity);
    }

    private String payloadText(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value == null ? null : blank(String.valueOf(value));
    }

    private BigDecimal payloadDecimal(Map<String, Object> payload, String key) {
        try { return new BigDecimal(String.valueOf(payload.getOrDefault(key, "0"))); }
        catch (RuntimeException exception) { return BigDecimal.ZERO; }
    }

    private Map<String,Object> validateSubmission(List<CrmDtos.FormField> fields,Map<String,Object> input){if(input==null)throw new BadRequestException("No se recibieron datos");Map<String,Object> result=new LinkedHashMap<>();for(CrmDtos.FormField field:fields){Object raw=input.get(field.name());String value=raw==null?"":String.valueOf(raw).trim();if(field.required()&&value.isBlank())throw new BadRequestException("El campo "+field.label()+" es obligatorio");if(value.length()>4000)throw new BadRequestException("El campo "+field.label()+" es demasiado extenso");if(field.type().equals("email")&&!value.isBlank()&&!value.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))throw new BadRequestException("Correo electrónico inválido");if(field.type().equals("select")&&!value.isBlank()&&!field.options().contains(value))throw new BadRequestException("Opción inválida para "+field.label());result.put(field.name(),field.type().equals("checkbox")?Boolean.parseBoolean(value):value);}return result;}
    private void validateFields(List<CrmDtos.FormField> fields){Set<String> names=new HashSet<>();for(CrmDtos.FormField field:fields){if(!names.add(field.name()))throw new BadRequestException("Campo duplicado: "+field.name());if(field.type().equals("select")&&(field.options()==null||field.options().isEmpty()))throw new BadRequestException("El campo "+field.label()+" necesita opciones");}}

    private CrmCompany requireCompany(String id){return companies.findById(id).orElseThrow(()->new NotFoundException("Empresa no encontrada"));}
    private CrmContact requireContact(String id){return contacts.findById(id).orElseThrow(()->new NotFoundException("Contacto no encontrado"));}
    private CrmPipelineStage requireStage(String id){return stages.findById(id).orElseThrow(()->new NotFoundException("Etapa no encontrada"));}
    private CrmDeal requireDeal(String id){return deals.findById(id).orElseThrow(()->new NotFoundException("Negocio no encontrado"));}
    private CrmActivity requireActivity(String id){return activities.findById(id).orElseThrow(()->new NotFoundException("Actividad no encontrada"));}
    private CrmForm requireForm(String id){return forms.findById(id).orElseThrow(()->new NotFoundException("Formulario no encontrado"));}
    private CrmSubmission requireSubmission(String id){return submissions.findById(id).orElseThrow(()->new NotFoundException("Envío no encontrado"));}
    private CrmCompany requireOptionalCompany(String id){return id==null||id.isBlank()?null:requireCompany(id);}
    private CrmContact requireOptionalContact(String id){return id==null||id.isBlank()?null:requireContact(id);}
    private CrmDeal requireOptionalDeal(String id){return id==null||id.isBlank()?null:requireDeal(id);}

    private CrmDtos.CompanyResponse companyResponse(CrmCompany c){return new CrmDtos.CompanyResponse(c.id,c.name,c.domain,c.email,c.phone,c.address,c.city,c.country,c.status,c.notes,c.createdAt,c.updatedAt);}
    private CrmDtos.ContactResponse contactResponse(CrmContact c){return new CrmDtos.ContactResponse(c.id,c.firstName,c.lastName,c.email,c.phone,c.jobTitle,c.status,c.source,c.ownerEmail,c.tags,c.notes,c.company==null?null:companyResponse(c.company),c.createdAt,c.updatedAt);}
    private CrmDtos.StageResponse stageResponse(CrmPipelineStage s){return new CrmDtos.StageResponse(s.id,s.name,s.position,s.probability,s.color,s.closed,s.won);}
    private CrmDtos.DealResponse dealResponse(CrmDeal d){return new CrmDtos.DealResponse(d.id,d.title,d.value,d.currency,d.expectedCloseDate,d.ownerEmail,d.description,d.contact==null?null:contactResponse(d.contact),d.company==null?null:companyResponse(d.company),stageResponse(d.stage),d.createdAt,d.updatedAt);}
    private CrmDtos.ActivityResponse activityResponse(CrmActivity a){return new CrmDtos.ActivityResponse(a.id,a.type,a.subject,a.description,a.dueAt,a.completedAt,a.assignedTo,a.contact==null?null:a.contact.id,a.company==null?null:a.company.id,a.deal==null?null:a.deal.id,a.createdAt,a.updatedAt);}
    private CrmDtos.FormResponse formResponse(CrmForm f){return new CrmDtos.FormResponse(f.id,f.formKey,f.name,f.description,f.successMessage,f.notifyEmails,readFields(f.fieldsJson),readMap(f.settingsJson),f.active,f.createdAt,f.updatedAt);}
    private CrmDtos.PublicForm publicFormResponse(CrmForm f){return new CrmDtos.PublicForm(f.formKey,f.name,f.description,f.successMessage,readFields(f.fieldsJson),readMap(f.settingsJson));}
    private CrmDtos.SubmissionResponse submissionResponse(CrmSubmission s){return new CrmDtos.SubmissionResponse(s.id,s.form.id,s.form.name,readMap(s.payloadJson),s.status,s.contact==null?null:s.contact.id,s.sourceUrl,s.createdAt);}

    private Pageable pageable(int page,int size){return PageRequest.of(Math.max(0,page),Math.min(Math.max(1,size),100));}
    private <T> CrmDtos.PageResponse<T> page(Page<T> p){return new CrmDtos.PageResponse<>(p.getContent(),p.getNumber(),p.getSize(),p.getTotalElements(),p.getTotalPages());}
    private String cleanSearch(String value){return value==null?"":value.trim();} private String blank(String value){return value==null||value.isBlank()?null:value.trim();} private String lower(String value){String v=blank(value);return v==null?null:v.toLowerCase(Locale.ROOT);} private String truncate(String value,int max){if(value==null)return null;return value.length()<=max?value:value.substring(0,max);}
    private String write(Object value){try{return mapper.writeValueAsString(value);}catch(Exception e){throw new IllegalStateException("No se pudo serializar información del CRM",e);}}
    private List<CrmDtos.FormField> readFields(String json){try{return mapper.readValue(json,new TypeReference<>(){});}catch(Exception e){return List.of();}}
    private Map<String,Object> readMap(String json){if(json==null||json.isBlank())return Map.of();try{return mapper.readValue(json,new TypeReference<>(){});}catch(Exception e){return Map.of();}}
    private String hash(String value){if(value==null||value.isBlank())return null;try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){return null;}}
}
