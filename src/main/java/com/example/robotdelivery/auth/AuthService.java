package com.example.robotdelivery.auth;

import com.example.robotdelivery.config.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private record Session(CurrentUser user, Instant expiresAt) {
    }

    private static final long TOKEN_TTL_HOURS = 12;

    private final UserAccountRepository userRepository;
    private final Map<String, Session> tokens = new ConcurrentHashMap<>();

    public AuthService(UserAccountRepository userRepository) {
        this.userRepository = userRepository;
    }

    public LoginResult login(String username, String password) {
        UserAccount account = userRepository.findByUsername(username)
                .filter(a -> a.getPassword().equals(password))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "用户名或密码错误"));
        String token = UUID.randomUUID().toString().replace("-", "");
        CurrentUser user = new CurrentUser(account.getId(), account.getUsername(),
                account.getDisplayName(), account.getRole());
        tokens.put(token, new Session(user, Instant.now().plus(TOKEN_TTL_HOURS, ChronoUnit.HOURS)));
        return new LoginResult(token, user);
    }

    public CurrentUser resolve(String token) {
        Session session = tokens.get(token);
        if (session == null) {
            return null;
        }
        if (session.expiresAt().isBefore(Instant.now())) {
            tokens.remove(token);
            return null;
        }
        return session.user();
    }

    public record LoginResult(String token, CurrentUser user) {
    }
}
