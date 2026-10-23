package com.minicloud.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minicloud.infrastructure.host.HostMetricsProvider;
import com.minicloud.infrastructure.host.HostSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import(AuthApiIT.TestHostConfig.class)
class AuthApiIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("minicloud")
            .withUsername("minicloud")
            .withPassword("minicloud");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("minicloud.security.jwt-secret", () -> "integration-test-secret-key-32bytes");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loginAndAccessProtectedHostApi() throws Exception {
        mockMvc.perform(get("/api/v1/hosts"))
                .andExpect(status().isUnauthorized());

        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"ChangeMe!Admin"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        String token = json.get("data").get("token").asText();

        mockMvc.perform(get("/api/v1/hosts").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].hostname").value("test-host"));
    }

    @TestConfiguration
    static class TestHostConfig {
        @Bean
        @Primary
        HostMetricsProvider testHostMetricsProvider() {
            return () -> new HostSnapshot(
                    "test-host",
                    "Ubuntu",
                    "Ubuntu 24.04",
                    "6.8.0-test",
                    "amd64",
                    4,
                    "Test CPU",
                    5.0,
                    0.1,
                    8_000_000_000L,
                    4_000_000_000L,
                    100_000_000_000L,
                    80_000_000_000L,
                    "/",
                    new HostSnapshot.KvmDetection(false, false, false, "none", "UNAVAILABLE", "test"),
                    new HostSnapshot.LibvirtDetection(false, null, "not-detected", false, false, "qemu:///system", "NOT_INSTALLED", "test"),
                    Instant.now()
            );
        }
    }
}
