package com.bytecode.tratcms.controller.rest;

import com.bytecode.tratcms.model.MPostMetadata;
import com.bytecode.tratcms.model.common.RepBase;
import com.bytecode.tratcms.repository.PostMetadataRepository;
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
    public ResponseEntity<RepBase> save(@RequestBody MPostMetadata MPostMetadata){
        return ResponseEntity.ok(new RepBase(repository.save(MPostMetadata)));
    }

    @PostMapping
    public ResponseEntity<RepBase> update(@RequestBody MPostMetadata MPostMetadata){
        return ResponseEntity.ok(new RepBase(repository.update(MPostMetadata)));
    }

    @GetMapping
    public ResponseEntity<List<MPostMetadata>> findAll(SpringDataWebProperties.Pageable pageable){
        return ResponseEntity.ok(repository.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MPostMetadata> findById(@PathVariable int id){
        return ResponseEntity.ok(repository.findById(id));
    }
}
