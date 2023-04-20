package com.bytecode.tratcms.data.listener;


import com.bytecode.tratcms.data.model.entity.Post;

import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import java.util.Date;

public class PostListener {

    @PrePersist
    public void prePersist(Post post){
        post.setSlug(post.getTitulo().replace(" ", "-").toLowerCase());
    }

    @PreUpdate
    public void preUpdate(Post post){
        post.setSlug(post.getTitulo().replace(" ", "-").toLowerCase());
        post.setFechaActualizacion(new Date());
    }

    @PostLoad
    public void postLoad(Post post){
        //System.out.println("LEYENDO EL POST=" + post.getIdPost());
    }


}
