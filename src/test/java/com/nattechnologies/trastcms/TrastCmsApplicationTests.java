package com.nattechnologies.trastcms;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:trastcms;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "trastcms.data-dir=target/test-data",
    "trastcms.admin.password=Testing-Password-2026!"
})
class TrastCmsApplicationTests {
    @Test
    void contextLoads() {}
}
