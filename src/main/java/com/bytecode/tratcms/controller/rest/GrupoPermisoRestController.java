package com.bytecode.tratcms.controller.rest;

import com.bytecode.tratcms.model.MGrupoPermiso;
import com.bytecode.tratcms.model.common.RepBase;
import com.bytecode.tratcms.repository.GrupoPermisoRepository;
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
    public ResponseEntity<RepBase> save(@RequestBody MGrupoPermiso MGrupoPermiso){
        return ResponseEntity.ok(new RepBase(repository.save(MGrupoPermiso)));
    }

    @PostMapping
    public ResponseEntity<RepBase> update(@RequestBody MGrupoPermiso MGrupoPermiso){
        return ResponseEntity.ok(new RepBase(repository.update(MGrupoPermiso)));
    }

    @GetMapping
    public ResponseEntity<List<MGrupoPermiso>> findAll(SpringDataWebProperties.Pageable pageable){
        return ResponseEntity.ok(repository.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MGrupoPermiso> findById(@PathVariable int id){
        return ResponseEntity.ok(repository.findById(id));
    }
}
