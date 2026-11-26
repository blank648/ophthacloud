package ro.unibuc.fmi.ophthacloud.securityaudit.controller;

import jakarta.validation.Valid;
import ro.unibuc.fmi.ophthacloud.securityaudit.dto.LoginRequest;
import ro.unibuc.fmi.ophthacloud.securityaudit.dto.LoginResponse;
import ro.unibuc.fmi.ophthacloud.securityaudit.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/api/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request);
    }
}
