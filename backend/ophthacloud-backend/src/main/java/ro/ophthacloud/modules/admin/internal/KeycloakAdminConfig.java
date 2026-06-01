package ro.ophthacloud.modules.admin.internal;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakAdminConfig {

    @Bean
    public static Keycloak keycloak(
            @Value("${ophthacloud.keycloak.admin-url}") String serverUrl,
            @Value("${ophthacloud.keycloak.realm}") String realm,
            @Value("${ophthacloud.keycloak.client-id}") String clientId,
            @Value("${ophthacloud.keycloak.client-secret:}") String clientSecret) {
        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(realm)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();
    }
}
