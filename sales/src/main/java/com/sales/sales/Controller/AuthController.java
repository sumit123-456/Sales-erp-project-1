package com.sales.sales.Controller;


import com.sales.sales.Services.AuthService;
import com.sales.sales.dto.LoginRequest;
import com.sales.sales.dto.LoginResponse;
import com.sales.sales.dto.UserRequest;
import com.sales.sales.validation.CommonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest loginRequest) {
        log.info("AuthController : loginUser() : Execution Start");
        LoginResponse loginResponce = authService.login(loginRequest);

        if (ObjectUtils.isEmpty(loginResponce) || !loginResponce.getSuccess()) {
            log.info("Error : {}","Login Unsuccessful - Invalid credentials");
            return CommonUtil.createErrorResponseMessage("Login Failed: Invalid email or password", HttpStatus.UNAUTHORIZED);
        }

        log.info("Login successful for user: {}", loginResponce.getEmail());
        return CommonUtil.createBuildResponse(loginResponce, HttpStatus.OK);
    }

    // Admin-only registration endpoint
    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> registerUser(@RequestBody UserRequest userRequest) {
        log.info("AuthController : registerUser() : Admin registering new user");
        try {
            Boolean register = authService.register(userRequest);
            if (register) {
                log.info("Success : {}","User registered successfully");
                return CommonUtil.createBuildResponse("User registered successfully", HttpStatus.CREATED);
            }
            return CommonUtil.createErrorResponse("Registration Failed", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (RuntimeException e) {
            return CommonUtil.createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/user-role")
    public ResponseEntity<?> getUserRole(@RequestHeader("Authorization") String token) {
        log.info("AuthController : getUserRole() : Execution Start");
        try {
            // Remove "Bearer " prefix if present
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            String userRole = authService.getUserRoleFromToken(token);
            return CommonUtil.createBuildResponse(Map.of("role", userRole), HttpStatus.OK);
        } catch (Exception e) {
            return CommonUtil.createErrorResponse("Invalid token", HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping("/count")
    public Map<String, Object> getUserCount() {
        long count = authService.getTotalUsers();
        return Map.of("totalUsers", count);
    }
}