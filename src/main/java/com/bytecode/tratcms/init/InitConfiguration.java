package com.bytecode.tratcms.init;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

//@Component
public class InitConfiguration {
//    private Environment env;
//
//    @PostConstruct
//    public void postCOnstruct(){
//        env = new StandardEnvironment();
//    }

    public List<String> getTypes() throws IOException {
        File file = new File("/home/application.json");
        ObjectMapper objectMapper = new ObjectMapper();
        HashMap<String, Object> object = objectMapper.readValue(file, HashMap.class);
        List<HashMap> typesObjects = (List<HashMap>) object.get("post_types");
        return typesObjects.stream()
                .map(hashMap -> (String) hashMap.get("type"))
                .collect(Collectors.toList());
    }
}
