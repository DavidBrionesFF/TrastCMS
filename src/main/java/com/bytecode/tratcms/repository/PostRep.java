package com.bytecode.tratcms.repository;

import com.bytecode.tratcms.model.Post;

public interface PostRep extends BaseRep<Post> {
    public Post findOnSave(Post post);
    public Post findLast();
}
