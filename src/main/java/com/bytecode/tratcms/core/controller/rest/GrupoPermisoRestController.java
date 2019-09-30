package com.bytecode.tratcms.core.controller.rest;

import com.bytecode.tratcms.core.model.GrupoPermiso;
import com.bytecode.tratcms.core.repository.GrupoPermisoRepository;
import com.bytecode.tratcms.core.model.common.RepBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/grupopermiso")
public class GrupoPermisoRestController {
    @Autowired
    private GrupoPermisoRepository repository;

    @PutMapping
    public ResponseEntity<RepBase> save(@RequestBody GrupoPermiso grupoPermiso){
        return ResponseEntity.ok(new RepBase(repository.save(grupoPermiso)));
    }

    @PostMapping
    public ResponseEntity<RepBase> update(@RequestBody GrupoPermiso grupoPermiso){
        return ResponseEntity.ok(new RepBase(repository.update(grupoPermiso)));
    }

    @GetMapping
    public ResponseEntity<RepBase<List<GrupoPermiso>>> findAll(SpringDataWebProperties.Pageable pageable){
        return ResponseEntity.ok(RepBase.create(repository.countAll(), repository.findAll(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RepBase<GrupoPermiso>> findById(@PathVariable int id){
        return ResponseEntity.ok(RepBase.create(1, repository.findById(id)));
    }
}
