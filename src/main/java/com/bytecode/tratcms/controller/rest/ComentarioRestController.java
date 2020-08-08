package com.bytecode.tratcms.controller.rest;

import com.bytecode.tratcms.model.MComentario;
import com.bytecode.tratcms.model.common.RepBase;
import com.bytecode.tratcms.repository.ComentarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/comentario")
public class ComentarioRestController {
    @Autowired
    private ComentarioRepository repository;

    @PutMapping
    public ResponseEntity<RepBase> save(@RequestBody MComentario MComentario){
        return ResponseEntity.ok(new RepBase(repository.save(MComentario)));
    }

    @PostMapping
    public ResponseEntity<RepBase> update(@RequestBody MComentario MComentario){
        return ResponseEntity.ok(new RepBase(repository.update(MComentario)));
    }

    @GetMapping
    public ResponseEntity<List<MComentario>> findAll(SpringDataWebProperties.Pageable pageable){
        return ResponseEntity.ok(repository.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MComentario> findById(@PathVariable int id){
        return ResponseEntity.ok(repository.findById(id));
    }
}
