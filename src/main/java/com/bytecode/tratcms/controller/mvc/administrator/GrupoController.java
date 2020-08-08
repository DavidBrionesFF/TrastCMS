package com.bytecode.tratcms.controller.mvc.administrator;

import com.bytecode.tratcms.model.MGrupo;
import com.bytecode.tratcms.model.MGrupoPermiso;
import com.bytecode.tratcms.repository.GrupoPermisoRepository;
import com.bytecode.tratcms.repository.GrupoRepository;
import com.bytecode.tratcms.repository.PermisoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/admin/grupo")
public class GrupoController {

    @Autowired
    private GrupoRepository grupoRepository;

    @Autowired
    private PermisoRepository permisoRepository;

    @Autowired
    private GrupoPermisoRepository grupoPermisoRepository;

    @GetMapping
    public ModelAndView getHome(
            @RequestParam(defaultValue = "all", required = false) String view_name,
            @RequestParam(defaultValue = "0", required = false) int id,
            SpringDataWebProperties.Pageable pageable
    ){
        ModelAndView modelAndView = new ModelAndView("administrator/grupo");
        switch (view_name){
            case "all":
                modelAndView.addObject("grupos", grupoRepository.findAll(pageable));
                break;
            case "new":
                modelAndView.addObject("grupo", new MGrupo());
                break;
            case "update":
                modelAndView.addObject("grupo", grupoRepository.findById(id));
                modelAndView.addObject("update", true);
                modelAndView.addObject("permisos", permisoRepository.findByIdGrupo(id));
                modelAndView.addObject("permisos_disponibles", permisoRepository.findByNotIdGrupo(id));
                break;
        }
        return modelAndView;
    }

    @PostMapping
    public String newAndUpdate(
            @ModelAttribute MGrupo MGrupo
    ){
        if (MGrupo.getIdgrupo() > 0){
            grupoRepository.update(MGrupo);
        } else {
            grupoRepository.save(MGrupo);
        }
        return "redirect:/admin/grupo";
    }

    @PostMapping("/addPermiso")
    public String addPermiso(@RequestParam int idGrupo,
                             @RequestParam int idPermiso){
        MGrupoPermiso MGrupoPermiso = new MGrupoPermiso();
        MGrupoPermiso.setIdPermiso(idPermiso);
        MGrupoPermiso.setIdGrupo(idGrupo);
        grupoPermisoRepository.save(MGrupoPermiso);
        return "redirect:/admin/grupo?view_name=update&id=" + idGrupo;
    }
}
