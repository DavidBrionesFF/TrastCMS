package com.bytecode.tratcms.controller.rest;

import com.bytecode.tratcms.model.Categoria;
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

    @PutMapping
    public ResponseEntity<RepBase> save(@RequestBody @Valid Categoria categoria){
        return ResponseEntity.ok(new RepBase(categoriaRepository.save(categoria)));
    }

    @PostMapping
    public ResponseEntity<RepBase> update(@RequestBody @Valid Categoria categoria){
        return ResponseEntity.ok(new RepBase(categoriaRepository.update(categoria)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<RepBase> delete(@PathVariable int id){
        return ResponseEntity.ok(new RepBase(categoriaRepository.deleteById(id)));
    }

    @GetMapping
    public ResponseEntity<RepBase<List<Categoria>>> findAll(SpringDataWebProperties.Pageable pageable){
        return ResponseEntity.ok(RepBase.create(categoriaRepository.countAll(), categoriaRepository.findAll(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RepBase> findById(@PathVariable int id){
        return ResponseEntity.ok(RepBase.create(1, categoriaRepository.findById(id)));
    }
}
