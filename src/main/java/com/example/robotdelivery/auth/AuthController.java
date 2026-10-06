package com.example.robotdelivery.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "认证", description = "登录获取访问令牌")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    @Operation(summary = "登录", description = "测试账号：admin/admin123, property/property123, ops/ops123, cs/cs123")
    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResult result = authService.login(request.username(), request.password());
        return Map.of(
                "token", result.token(),
                "user", result.user());
    }

    @Operation(summary = "当前登录用户")
    @GetMapping("/me")
    public CurrentUser me() {
        return CurrentUserHolder.get();
    }
}
