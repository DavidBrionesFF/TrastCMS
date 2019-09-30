package com.bytecode.tratcms.core.controller.rest;

import com.bytecode.tratcms.core.model.Post;
import com.bytecode.tratcms.core.repository.PostRepository;
import com.bytecode.tratcms.core.model.common.RepBase;
import javafx.geometry.Pos;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/post")
public class PostRestRepository {

    @Autowired
    private PostRepository repository;

    @PutMapping
    public ResponseEntity<RepBase> save(@RequestBody Post post){
        return ResponseEntity.ok(new RepBase(repository.save(post)));
    }

    @PostMapping
    public ResponseEntity<RepBase> update(@RequestBody Post post){
        return ResponseEntity.ok(new RepBase(repository.update(post)));
    }

    @GetMapping
    public ResponseEntity<RepBase<List<Post>>> findAll(SpringDataWebProperties.Pageable pageable){
        return ResponseEntity.ok(RepBase.create(repository.countAll(), repository.findAll(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RepBase<Pos>> findById(@PathVariable int id){
        return ResponseEntity.ok(RepBase.create(1, repository.findById(id)));
    }
}