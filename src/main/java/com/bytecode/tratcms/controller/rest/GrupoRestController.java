package com.bytecode.tratcms.controller.rest;

import com.bytecode.tratcms.model.MGrupo;
import com.bytecode.tratcms.model.common.RepBase;
import com.bytecode.tratcms.repository.GrupoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/grupo")
public class GrupoRestController {
    @Autowired
    private GrupoRepository repository;

    @PutMapping
    public ResponseEntity<RepBase> save(@RequestBody MGrupo MGrupo){
        return ResponseEntity.ok(new RepBase(repository.save(MGrupo)));
    }

    @PostMapping
    public ResponseEntity<RepBase> update(@RequestBody MGrupo MGrupo){
        return ResponseEntity.ok(new RepBase(repository.update(MGrupo)));
    }

    @GetMapping
    public ResponseEntity<List<MGrupo>> findAll(SpringDataWebProperties.Pageable pageable){
        return ResponseEntity.ok(repository.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MGrupo> findById(@PathVariable int id){
        return ResponseEntity.ok(repository.findById(id));
    }
}
