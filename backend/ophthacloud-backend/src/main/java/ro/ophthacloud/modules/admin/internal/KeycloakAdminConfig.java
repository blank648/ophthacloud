package ro.ophthacloud.modules.admin.internal;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakAdminConfig {

    @Value("${ophthacloud.keycloak.admin-url}")
    private String serverUrl;

    @Value("${ophthacloud.keycloak.realm}")
    private String realm;

    @Value("${ophthacloud.keycloak.client-id}")
    private String clientId;

    @Value("${ophthacloud.keycloak.client-secret:}")
    private String clientSecret;

    @Bean
    public Keycloak keycloak() {
        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(realm)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();
    }
}
