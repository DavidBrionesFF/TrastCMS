package com.bytecode.tratcms.repository;

import com.bytecode.tratcms.model.PostMetadata;

import java.util.List;

public interface PostMetadataRep extends BaseRep<PostMetadata> {
    public List<PostMetadata> findByIdPost(int idPost);
}
