package com.bytecode.tratcms.controller.rest;

import com.bytecode.tratcms.model.Usuario;
import com.bytecode.tratcms.model.common.RepBase;
import com.bytecode.tratcms.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/usuario")
public class UsuarioRestController {
    @Autowired
    private UsuarioRepository repository;

    @PutMapping
    public ResponseEntity<RepBase> save(@RequestBody Usuario usuario){
        return ResponseEntity.ok(new RepBase(repository.save(usuario)));
    }

    @PostMapping
    public ResponseEntity<RepBase> update(@RequestBody Usuario usuario){
        return ResponseEntity.ok(new RepBase(repository.update(usuario)));
    }

    @GetMapping
    public ResponseEntity<RepBase<List<Usuario>>> findAll(SpringDataWebProperties.Pageable pageable){
        return ResponseEntity.ok(RepBase.create(repository.countAll(),
                repository.findAll(pageable)
                        .stream()
                        .map(usuario -> {
                            usuario.setContrasena(null);
                            return usuario;
                        }).collect(Collectors.toList())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RepBase<Usuario>> findById(@PathVariable int id){
        Usuario usuario = repository.findById(id);
        usuario.setContrasena(null);
        return ResponseEntity.ok(RepBase.create(1, usuario));
    }
}
