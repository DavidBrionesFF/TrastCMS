package com.bytecode.tratcms.controller.rest;

import com.bytecode.tratcms.model.MPermiso;
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
    public ResponseEntity<RepBase> save(@RequestBody MPermiso MPermiso){
        return ResponseEntity.ok(new RepBase(repository.save(MPermiso)));
    }

    @PostMapping
    public ResponseEntity<RepBase> update(@RequestBody MPermiso MPermiso){
        return ResponseEntity.ok(new RepBase(repository.update(MPermiso)));
    }

    @GetMapping
    public ResponseEntity<List<MPermiso>> findAll(SpringDataWebProperties.Pageable pageable){
        return ResponseEntity.ok(repository.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MPermiso> findById(@PathVariable int id){
        return ResponseEntity.ok(repository.findById(id));
    }
}
