package com.bytecode.tratcms.controller.rest;

import com.bytecode.tratcms.model.MUsuario;
import com.bytecode.tratcms.model.common.RepBase;
import com.bytecode.tratcms.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/usuario")
public class UsuarioRestController {
    @Autowired
    private UsuarioRepository repository;

    @PutMapping
    public ResponseEntity<RepBase> save(@RequestBody MUsuario MUsuario){
        return ResponseEntity.ok(new RepBase(repository.save(MUsuario)));
    }

    @PostMapping
    public ResponseEntity<RepBase> update(@RequestBody MUsuario MUsuario){
        return ResponseEntity.ok(new RepBase(repository.update(MUsuario)));
    }

    @GetMapping
    public ResponseEntity<List<MUsuario>> findAll(SpringDataWebProperties.Pageable pageable){
        return ResponseEntity.ok(repository.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MUsuario> findById(@PathVariable int id){
        return ResponseEntity.ok(repository.findById(id));
    }
}
