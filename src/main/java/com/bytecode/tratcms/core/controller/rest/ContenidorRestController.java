package com.bytecode.tratcms.core.controller.rest;

import com.bytecode.tratcms.core.model.Contenido;
import com.bytecode.tratcms.core.repository.ContenidoRepository;
import com.bytecode.tratcms.core.model.common.RepBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contenido")
public class ContenidorRestController {
    @Autowired
    private ContenidoRepository repository;

    @PutMapping
    public ResponseEntity<RepBase> save(@RequestBody Contenido contenido){
        return ResponseEntity.ok(new RepBase(repository.save(contenido)));
    }

    @PostMapping
    public ResponseEntity<RepBase> update(@RequestBody Contenido contenido){
        return ResponseEntity.ok(new RepBase(repository.update(contenido)));
    }

    @GetMapping
    public ResponseEntity<RepBase<List<Contenido>>> findAll(SpringDataWebProperties.Pageable pageable){
        return ResponseEntity.ok(RepBase.create(repository.countAll(), repository.findAll(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RepBase<Contenido>> findById(@PathVariable int id){
        return ResponseEntity.ok(RepBase.create(1, repository.findById(id)));
    }
}