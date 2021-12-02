package com.bytecode.tratcms.data.repository;

import com.bytecode.tratcms.data.model.MPost;

public interface PostRep extends BaseRep<MPost> {
    public MPost findOnSave(MPost MPost);
    public MPost findLast();
}
