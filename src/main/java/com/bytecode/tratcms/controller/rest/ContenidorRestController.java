package com.bytecode.tratcms.controller.rest;

import com.bytecode.tratcms.model.MContenido;
import com.bytecode.tratcms.model.common.RepBase;
import com.bytecode.tratcms.repository.ContenidoRepository;
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
    public ResponseEntity<RepBase> save(@RequestBody MContenido MContenido){
        return ResponseEntity.ok(new RepBase(repository.save(MContenido)));
    }

    @PostMapping
    public ResponseEntity<RepBase> update(@RequestBody MContenido MContenido){
        return ResponseEntity.ok(new RepBase(repository.update(MContenido)));
    }

    @GetMapping
    public ResponseEntity<List<MContenido>> findAll(SpringDataWebProperties.Pageable pageable){
        return ResponseEntity.ok(repository.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MContenido> findById(@PathVariable int id){
        return ResponseEntity.ok(repository.findById(id));
    }
}
