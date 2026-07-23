package com.nattechnologies.trastcms.plugins.crm;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/plugins/trastcrm/forms")
public class CrmPublicController {
    private final CrmService service;
    private final CrmPluginGuard guard;
    public CrmPublicController(CrmService service, CrmPluginGuard guard){this.service=service;this.guard=guard;}
    @ModelAttribute void requireEnabled(){guard.requireEnabled();}
    @GetMapping("/{key}") public CrmDtos.PublicForm form(@PathVariable String key){return service.publicForm(key);}
    @PostMapping("/{key}/submissions") public CrmDtos.Message submit(@PathVariable String key,@Valid @RequestBody CrmDtos.SubmissionRequest request,HttpServletRequest servletRequest){return service.submit(key,request,clientIp(servletRequest),servletRequest.getHeader("User-Agent"));}
    private String clientIp(HttpServletRequest request){String forwarded=request.getHeader("X-Forwarded-For");return forwarded==null||forwarded.isBlank()?request.getRemoteAddr():forwarded.split(",")[0].trim();}
}
