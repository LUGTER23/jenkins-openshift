package com.example.demo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DemoApplicationTests {

    @Value("${test1.example}")
    String base64Secret;

    @Test
    void compare(){
        System.out.println("Env: "+ System.getenv());
        System.out.println("Secreto: " + base64Secret);
        Assertions.assertEquals("aHR0cDovL214bXR5YWxtMDU6NzgvTm9DZW1leC9GV0VCL0JQRC9UcnVuay9Eb2N1bWVudGF0aW9uLzBFc3RpbWF0aW9uIGFuZCBQcm9wb3NhbC9SZXF1aXJlbWVudHMvUHJveWVjdG9zLwpodHRwOi8vMTAuMTUuNDIuMzI6NzgvTm9DZW1leC9GV0VCL0JQRC9UcnVuay9Eb2N1bWVudGF0aW9uLzBFc3RpbWF0aW9uIGFuZCBQcm9wb3NhbC9SZXF1aXJlbWVudHMvUHJveWVjdG9zLwpZQSBMRSBFTlRFTkRJCkpPSk9KRUpFSkVKRUUKR09ORE9SClJPSEFO",
                base64Secret);
    }

}
