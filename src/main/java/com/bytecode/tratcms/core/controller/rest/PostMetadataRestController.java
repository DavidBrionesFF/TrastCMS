package com.bytecode.tratcms.core.controller.rest;

import com.bytecode.tratcms.core.model.PostMetadata;
import com.bytecode.tratcms.core.repository.PostMetadataRepository;
import com.bytecode.tratcms.core.model.common.RepBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/postmetadata")
public class PostMetadataRestController {
    @Autowired
    private PostMetadataRepository repository;

    @PutMapping
    public ResponseEntity<RepBase> save(@RequestBody PostMetadata postMetadata){
        return ResponseEntity.ok(new RepBase(repository.save(postMetadata)));
    }

    @PostMapping
    public ResponseEntity<RepBase> update(@RequestBody PostMetadata postMetadata){
        return ResponseEntity.ok(new RepBase(repository.update(postMetadata)));
    }

    @GetMapping
    public ResponseEntity<RepBase<List<PostMetadata>>> findAll(SpringDataWebProperties.Pageable pageable){
        return ResponseEntity.ok(RepBase.create(repository.countAll(), repository.findAll(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RepBase<PostMetadata>> findById(@PathVariable int id){
        return ResponseEntity.ok(RepBase.create(1, repository.findById(id)));
    }
}
