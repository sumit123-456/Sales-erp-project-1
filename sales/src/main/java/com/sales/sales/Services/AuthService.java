package com.sales.sales.Services;

import com.sales.sales.Entity.Employee;
import com.sales.sales.Entity.Role;
import com.sales.sales.Entity.User;
import com.sales.sales.Repositories.EmployeeRepository;
import com.sales.sales.Repositories.RoleRepository;
import com.sales.sales.Repositories.UserRepository;
import com.sales.sales.dto.LoginRequest;
import com.sales.sales.dto.LoginResponse;
import com.sales.sales.dto.UserRequest;
import com.sales.sales.dto.UserResponse;
import com.sales.sales.security.CustomUserDetails;
import com.sales.sales.security.JwtUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final EmployeeRepository employeeRepository;

    // Fixed admin details
    private final String ADMIN_EMAIL = "admin@saleserp.com";
    private final String ADMIN_PASSWORD = "admin123";
    private final String ADMIN_NAME = "System Administrator";

    @PostConstruct
    public void initAdminUser() {
        // Ensure essential roles exist (ADMIN and EMPLOYEE) for registration and role checks
        roleRepository.findByRoleName("EMPLOYEE").orElseGet(() -> roleRepository.save(new Role(null, "EMPLOYEE")));

        // Get ADMIN role or create if missing
        Role adminRole = roleRepository.findByRoleName("ADMIN")
                .orElseGet(() -> roleRepository.save(new Role(null, "ADMIN")));

        // Always delete existing admin user and recreate with fresh password to ensure correct credentials
        userRepository.findByEmail(ADMIN_EMAIL).ifPresent(userRepository::delete);

        String encodedPassword = passwordEncoder.encode(ADMIN_PASSWORD);
        log.info("Creating admin with encoded password: {}", encodedPassword);
        
        User admin = User.builder()
                .fullName(ADMIN_NAME)
                .email(ADMIN_EMAIL)
                .password(encodedPassword)
                .callTarget(0)
                .monthlyTarget(0)
                .teamAllocation("Administration")
                .role(adminRole)
                .build();

        userRepository.save(admin);
        
        // Verify the password can be matched
        boolean passwordMatches = passwordEncoder.matches(ADMIN_PASSWORD, encodedPassword);
        log.info("Password verification: {} matches {} = {}", ADMIN_PASSWORD, encodedPassword, passwordMatches);
        log.info("Default admin user initialized: {} with fresh password", ADMIN_EMAIL);
    }

    public LoginResponse login(LoginRequest loginRequest) {

        log.info("AuthService: Attempt login for {}", loginRequest.getEmail());

        try {
            // First, find the user by email
            User user = userRepository.findByEmail(loginRequest.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Manually verify password using the PasswordEncoder
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                log.error("Login failed: Password mismatch for user {}", loginRequest.getEmail());
                return LoginResponse.builder()
                        .success(false)
                        .message("Invalid email or password")
                        .build();
            }

            // If password matches, generate token
            String token = jwtUtil.generateToken(
                    user.getEmail(),
                    user.getRole().getRoleName()
            );

            log.info("Login successful for user: {}", user.getEmail());
            return LoginResponse.builder()
                    .userId(user.getUserId())
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .role(user.getRole().getRoleName())
                    .callTarget(user.getCallTarget())
                    .monthlyTarget(user.getMonthlyTarget())
                    .teamAllocation(user.getTeamAllocation())
                    .token(token)
                    .success(true)
                    .message("Login successful")
                    .build();

        } catch (Exception e) {
            log.error("Login failed: {}", e.getMessage());
            log.debug("Exception details:", e);
            return LoginResponse.builder()
                    .success(false)
                    .message("Invalid email or password")
                    .build();
        }
    }

    public Boolean register(UserRequest req) {

        if (userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        Role role = roleRepository.findByRoleName(req.getRole())
                .orElseThrow(() -> new RuntimeException("Invalid role: " + req.getRole()));

        User user = User.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .phoneNumber(req.getPhoneNumber())
                .callTarget(req.getCallTarget() != null ? req.getCallTarget() : 50)
                .monthlyTarget(req.getMonthlyTarget() != null ? req.getMonthlyTarget() : 10000)
                .teamAllocation(req.getTeamAllocation() != null ? req.getTeamAllocation() : "General")
                .role(role)
                .build();

        userRepository.save(user);


        Employee emp = new Employee();
        emp.setEmpId("EMP" + user.getUserId());
        emp.setEmpName(user.getFullName());
        emp.setEmail(user.getEmail());
        emp.setAchieved(0);
        emp.setCallsMade(0);
        emp.setMonthlyTarget(user.getMonthlyTarget());
        emp.setMonthlyCallTarget(user.getCallTarget());
        emp.setTeam(user.getTeamAllocation());
        emp.setJoinDate(LocalDate.now());

        employeeRepository.save(emp);

        return true;
    }

    public String getUserRoleFromToken(String token) {
        try {
            if (token.startsWith("Bearer ")) token = token.substring(7);

            String role = jwtUtil.extractRole(token);
            if (role != null) return role;

            String email = jwtUtil.extractUsername(token);
            return userRepository.findByEmail(email)
                    .map(u -> u.getRole().getRoleName())
                    .orElse("UNKNOWN");

        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    public String getUserEmailFromToken(String token) {
        if (token.startsWith("Bearer ")) token = token.substring(7);
        return jwtUtil.extractUsername(token);
    }

    public boolean isAdmin(String token) {
        return "ADMIN".equalsIgnoreCase(getUserRoleFromToken(token));
    }


    public long getTotalUsers() {
        return userRepository.count();
    }

    public long getEmployeeCount() {
        return userRepository.countByRole_RoleName("EMPLOYEE");
    }

    public long getAdminCount() {
        return userRepository.countByRole_RoleName("ADMIN");
    }

    public List<UserResponse> getAllEmployees() {
        return userRepository.findByRole_RoleName("EMPLOYEE")
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    public UserResponse updateEmployee(Integer id, UserRequest req) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getEmail().equals(req.getEmail())
                && userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        user.setFullName(req.getFullName());
        user.setEmail(req.getEmail());
        user.setPhoneNumber(req.getPhoneNumber());
        user.setCallTarget(req.getCallTarget());
        user.setMonthlyTarget(req.getMonthlyTarget());
        user.setTeamAllocation(req.getTeamAllocation());

        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(req.getPassword()));
        }

        return mapToResponse(userRepository.save(user));
    }

    public void deleteEmployee(Integer id) {

        User u = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if ("ADMIN".equalsIgnoreCase(u.getRole().getRoleName())) {
            throw new RuntimeException("Cannot delete admin user");
        }

        userRepository.delete(u);
    }

    private UserResponse mapToResponse(User u) {
        return UserResponse.builder()
                .userId(u.getUserId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .phoneNumber(u.getPhoneNumber())
                .role(u.getRole().getRoleName())
                .callTarget(u.getCallTarget())
                .monthlyTarget(u.getMonthlyTarget())
                .teamAllocation(u.getTeamAllocation())
                .build();
    }
}