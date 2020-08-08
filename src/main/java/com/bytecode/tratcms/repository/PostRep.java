package com.bytecode.tratcms.repository;

import com.bytecode.tratcms.model.MPost;

public interface PostRep extends BaseRep<MPost> {
    public MPost findOnSave(MPost MPost);
    public MPost findLast();
}
