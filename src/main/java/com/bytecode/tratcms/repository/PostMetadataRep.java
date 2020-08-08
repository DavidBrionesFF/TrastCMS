package com.bytecode.tratcms.repository;

import com.bytecode.tratcms.model.MPostMetadata;

import java.util.List;

public interface PostMetadataRep extends BaseRep<MPostMetadata> {
    public List<MPostMetadata> findByIdPost(int idPost);
}
