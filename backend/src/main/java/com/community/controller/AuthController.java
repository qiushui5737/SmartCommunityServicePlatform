package com.community.controller;
import com.community.common.Result;
import com.community.service.AuthService;
import com.community.util.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final TokenBlacklistService tokenBlacklistService;
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> req) {
        return authService.login(req.get("username"), req.get("password"), req.get("role"));
    }
    @PostMapping("/register")
    public Result<Void> register(@RequestBody Map<String, String> req) {
        return authService.register(req.get("username"), req.get("password"), req.get("realName"), req.get("phone"));
    }
    @GetMapping("/info")
    public Result<Map<String, Object>> getInfo(@RequestAttribute("userId") Long userId) {
        // 实际项目中应查询数据库，此处简化
        return Result.ok(Map.of("id", userId, "role", "OWNER"));
    }

    /**
     * 用户登出 —— 将 Token 加入 Redis 黑名单，主动失效
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            tokenBlacklistService.addToBlacklist(token);
            log.info("用户登出，Token 已加入黑名单");
        }
        return Result.ok(null);
    }
}
