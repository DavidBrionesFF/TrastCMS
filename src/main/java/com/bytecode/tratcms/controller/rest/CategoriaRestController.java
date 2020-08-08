package com.bytecode.tratcms.controller.rest;

import com.bytecode.tratcms.model.MCategoria;
import com.bytecode.tratcms.model.common.RepBase;
import com.bytecode.tratcms.repository.CategoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/categoria")
public class CategoriaRestController {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @PutMapping//(consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<RepBase> save(@RequestBody @Valid MCategoria MCategoria){
        return ResponseEntity.ok(new RepBase(categoriaRepository.save(MCategoria)));
    }

    @PostMapping
    public ResponseEntity<RepBase> update(@RequestBody @Valid MCategoria MCategoria){
        return ResponseEntity.ok(new RepBase(categoriaRepository.update(MCategoria)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<RepBase> delete(@PathVariable int id){
        return ResponseEntity.ok(new RepBase(categoriaRepository.deleteById(id)));
    }

    @GetMapping
    public ResponseEntity<List<MCategoria>> findAll(SpringDataWebProperties.Pageable pageable){
        return ResponseEntity.ok(categoriaRepository.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MCategoria> findById(@PathVariable int id){
        return ResponseEntity.ok(categoriaRepository.findById(id));
    }
}
