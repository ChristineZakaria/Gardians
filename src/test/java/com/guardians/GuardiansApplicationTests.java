package com.guardians;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class GuardiansApplicationTests {

    @Test
    void contextLoads() {
        // Verifies Spring context starts without errors
    }
}
