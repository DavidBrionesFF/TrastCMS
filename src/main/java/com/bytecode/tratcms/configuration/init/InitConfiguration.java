package com.bytecode.tratcms.configuration.init;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
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
