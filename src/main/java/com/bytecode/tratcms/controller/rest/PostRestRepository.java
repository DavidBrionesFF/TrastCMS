package com.bytecode.tratcms.controller.rest;

import com.bytecode.tratcms.model.MPost;
import com.bytecode.tratcms.model.common.RepBase;
import com.bytecode.tratcms.repository.PostRepository;
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
    public ResponseEntity<RepBase> save(@RequestBody MPost MPost){
        return ResponseEntity.ok(new RepBase(repository.save(MPost)));
    }

    @PostMapping
    public ResponseEntity<RepBase> update(@RequestBody MPost MPost){
        return ResponseEntity.ok(new RepBase(repository.update(MPost)));
    }

    @GetMapping
    public ResponseEntity<List<MPost>> findAll(SpringDataWebProperties.Pageable pageable){
        return ResponseEntity.ok(repository.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MPost> findById(@PathVariable int id){
        return ResponseEntity.ok(repository.findById(id));
    }
}
