package com.dcg.gateway.config;

import com.dcg.gateway.model.config.DcgConfig;
import com.dcg.gateway.model.config.auth.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class ConfigLoadTest {

    @Autowired
    private DcgConfig dcgConfig;

    @Test
    void testConfigLoad() {
        var malaAuth = dcgConfig.getVas().getSchemes().get("mala").getAuth();
        var otherAuth = dcgConfig.getVas().getSchemes().get("other_system").getAuth();
        var legacyAuth = dcgConfig.getVas().getSchemes().get("legacy_system").getAuth();

        // Test Bearer Token Auth
        assertThat(malaAuth).isInstanceOf(BearerTokenAuthConfig.class);
        var bearerConfig = (BearerTokenAuthConfig) malaAuth;
        assertThat(bearerConfig.getTokenRefresh().getUrl()).isEqualTo("/api/token");
        assertThat(bearerConfig.getHeaders()).containsKey("X-Client-ID");

        // Test OAuth2 Auth
        assertThat(otherAuth).isInstanceOf(OAuth2AuthConfig.class);
        var oauthConfig = (OAuth2AuthConfig) otherAuth;
        assertThat(oauthConfig.getTokenUrl()).isEqualTo("/oauth/token");
        assertThat(oauthConfig.getGrantType()).isEqualTo("client_credentials");

        // Test Basic Auth
        assertThat(legacyAuth).isInstanceOf(BasicAuthConfig.class);
        var basicConfig = (BasicAuthConfig) legacyAuth;
        assertThat(basicConfig.getHeaders()).containsKey("X-Source");
    }
} 