package com.bytecode.tratcms.controller.rest;

import com.bytecode.tratcms.model.Grupo;
import com.bytecode.tratcms.model.Usuario;
import com.bytecode.tratcms.model.common.RepBase;
import com.bytecode.tratcms.repository.GrupoPermisoRepository;
import com.bytecode.tratcms.repository.GrupoRepository;
import com.bytecode.tratcms.repository.UsuarioRepository;
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
    public ResponseEntity<RepBase> save(@RequestBody Grupo grupo){
        return ResponseEntity.ok(new RepBase(repository.save(grupo)));
    }

    @PostMapping
    public ResponseEntity<RepBase> update(@RequestBody Grupo grupo){
        return ResponseEntity.ok(new RepBase(repository.update(grupo)));
    }

    @GetMapping
    public ResponseEntity<List<Grupo>> findAll(SpringDataWebProperties.Pageable pageable){
        return ResponseEntity.ok(repository.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Grupo> findById(@PathVariable int id){
        return ResponseEntity.ok(repository.findById(id));
    }
}
