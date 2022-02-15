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
        Assertions.assertEquals("aHR0cDovL214bXR5YWxtMDU6NzgvTm9DZW1leC9GV0VCL0JQRC9UcnVuay9Eb2N1bWVudGF0aW9uLzBFc3RpbWF0aW9uIGFuZCBQcm9wb3NhbC9SZXF1aXJlbWVudHMvUHJveWVjdG9zLw0KaHR0cDovLzEwLjE1LjQyLjMyOjc4L05vQ2VtZXgvRldFQi9CUEQvVHJ1bmsvRG9jdW1lbnRhdGlvbi8wRXN0aW1hdGlvbiBhbmQgUHJvcG9zYWwvUmVxdWlyZW1lbnRzL1Byb3llY3Rvcy8NCllBIExFIEVOVEVOREkNCkpPSk9KRUpFSkVKRUUNCkdPTkRPUg==",
                base64Secret);
    }

}
