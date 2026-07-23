package com.nattechnologies.trastcms.plugins.crm;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/crm")
public class CrmAdminController {
    private final CrmService service;
    private final CrmPluginGuard guard;
    public CrmAdminController(CrmService service, CrmPluginGuard guard){this.service=service;this.guard=guard;}
    @ModelAttribute void requireEnabled(){guard.requireEnabled();}
    @GetMapping("/dashboard") public CrmDtos.Dashboard dashboard(){return service.dashboard();}

    @GetMapping("/contacts") public CrmDtos.PageResponse<CrmDtos.ContactResponse> contacts(@RequestParam(required=false)String search,@RequestParam(required=false)CrmContactStatus status,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="25")int size){return service.contacts(search,status,page,size);}
    @PostMapping("/contacts") @ResponseStatus(HttpStatus.CREATED) public CrmDtos.ContactResponse createContact(@Valid @RequestBody CrmDtos.ContactRequest request){return service.saveContact(null,request);}
    @PutMapping("/contacts/{id}") public CrmDtos.ContactResponse updateContact(@PathVariable String id,@Valid @RequestBody CrmDtos.ContactRequest request){return service.saveContact(id,request);}
    @DeleteMapping("/contacts/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteContact(@PathVariable String id){service.deleteContact(id);}

    @GetMapping("/companies") public CrmDtos.PageResponse<CrmDtos.CompanyResponse> companies(@RequestParam(required=false)String search,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="25")int size){return service.companies(search,page,size);}
    @PostMapping("/companies") @ResponseStatus(HttpStatus.CREATED) public CrmDtos.CompanyResponse createCompany(@Valid @RequestBody CrmDtos.CompanyRequest request){return service.saveCompany(null,request);}
    @PutMapping("/companies/{id}") public CrmDtos.CompanyResponse updateCompany(@PathVariable String id,@Valid @RequestBody CrmDtos.CompanyRequest request){return service.saveCompany(id,request);}
    @DeleteMapping("/companies/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteCompany(@PathVariable String id){service.deleteCompany(id);}

    @GetMapping("/stages") public List<CrmDtos.StageResponse> stages(){return service.stages();}
    @PostMapping("/stages") @ResponseStatus(HttpStatus.CREATED) public CrmDtos.StageResponse createStage(@Valid @RequestBody CrmDtos.StageRequest request){return service.saveStage(null,request);}
    @PutMapping("/stages/{id}") public CrmDtos.StageResponse updateStage(@PathVariable String id,@Valid @RequestBody CrmDtos.StageRequest request){return service.saveStage(id,request);}

    @GetMapping("/deals") public CrmDtos.PageResponse<CrmDtos.DealResponse> deals(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="50")int size){return service.deals(page,size);}
    @PostMapping("/deals") @ResponseStatus(HttpStatus.CREATED) public CrmDtos.DealResponse createDeal(@Valid @RequestBody CrmDtos.DealRequest request){return service.saveDeal(null,request);}
    @PutMapping("/deals/{id}") public CrmDtos.DealResponse updateDeal(@PathVariable String id,@Valid @RequestBody CrmDtos.DealRequest request){return service.saveDeal(id,request);}
    @DeleteMapping("/deals/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteDeal(@PathVariable String id){service.deleteDeal(id);}

    @GetMapping("/activities") public CrmDtos.PageResponse<CrmDtos.ActivityResponse> activities(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="50")int size){return service.activities(page,size);}
    @PostMapping("/activities") @ResponseStatus(HttpStatus.CREATED) public CrmDtos.ActivityResponse createActivity(@Valid @RequestBody CrmDtos.ActivityRequest request){return service.saveActivity(null,request);}
    @PutMapping("/activities/{id}") public CrmDtos.ActivityResponse updateActivity(@PathVariable String id,@Valid @RequestBody CrmDtos.ActivityRequest request){return service.saveActivity(id,request);}
    @DeleteMapping("/activities/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteActivity(@PathVariable String id){service.deleteActivity(id);}

    @GetMapping("/forms") public List<CrmDtos.FormResponse> forms(){return service.forms();}
    @PostMapping("/forms") @ResponseStatus(HttpStatus.CREATED) public CrmDtos.FormResponse createForm(@Valid @RequestBody CrmDtos.FormRequest request){return service.saveForm(null,request);}
    @PutMapping("/forms/{id}") public CrmDtos.FormResponse updateForm(@PathVariable String id,@Valid @RequestBody CrmDtos.FormRequest request){return service.saveForm(id,request);}
    @DeleteMapping("/forms/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteForm(@PathVariable String id){service.deleteForm(id);}

    @GetMapping("/submissions") public CrmDtos.PageResponse<CrmDtos.SubmissionResponse> submissions(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="50")int size){return service.submissions(page,size);}
    @PutMapping("/submissions/{id}/status") public CrmDtos.SubmissionResponse updateSubmission(@PathVariable String id,@Valid @RequestBody CrmDtos.SubmissionStatusRequest request){return service.updateSubmission(id,request);}
    @DeleteMapping("/submissions/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteSubmission(@PathVariable String id){service.deleteSubmission(id);}
}
