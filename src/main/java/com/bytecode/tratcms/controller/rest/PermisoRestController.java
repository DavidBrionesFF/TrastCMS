package com.bytecode.tratcms.controller.rest;

import com.bytecode.tratcms.model.Permiso;
import com.bytecode.tratcms.model.common.RepBase;
import com.bytecode.tratcms.repository.PermisoRepository;
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
    public ResponseEntity<List<Permiso>> findAll(SpringDataWebProperties.Pageable pageable){
        return ResponseEntity.ok(repository.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Permiso> findById(@PathVariable int id){
        return ResponseEntity.ok(repository.findById(id));
    }
}
