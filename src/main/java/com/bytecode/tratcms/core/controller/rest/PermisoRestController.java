package com.bytecode.tratcms.core.controller.rest;

import com.bytecode.tratcms.core.model.Permiso;
import com.bytecode.tratcms.core.repository.PermisoRepository;
import com.bytecode.tratcms.core.model.common.RepBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/permiso")
public class PermisoRestController {

    @Autowired
    private PermisoRepository repository;

    @PutMapping
    public ResponseEntity<RepBase> save(@RequestBody Permiso permiso){
        return ResponseEntity.ok(new RepBase(repository.save(permiso)));
    }

    @PostMapping
    public ResponseEntity<RepBase> update(@RequestBody Permiso permiso){
        return ResponseEntity.ok(new RepBase(repository.update(permiso)));
    }

    @GetMapping
    public ResponseEntity<RepBase<List<Permiso>>> findAll(SpringDataWebProperties.Pageable pageable){
        return ResponseEntity.ok(RepBase.create(repository.countAll(), repository.findAll(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RepBase<Permiso>> findById(@PathVariable int id){
        return ResponseEntity.ok(RepBase.create(1, repository.findById(id)));
    }
}
