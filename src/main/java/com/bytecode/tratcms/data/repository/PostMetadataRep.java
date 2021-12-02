package com.bytecode.tratcms.data.repository;

import com.bytecode.tratcms.data.model.MPostMetadata;

import java.util.List;

public interface PostMetadataRep extends BaseRep<MPostMetadata> {
    public List<MPostMetadata> findByIdPost(int idPost);
}
