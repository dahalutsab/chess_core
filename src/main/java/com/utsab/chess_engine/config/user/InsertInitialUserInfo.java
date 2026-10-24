package com.utsab.chess_engine.config.user;

import com.utsab.chess_engine.user.entity.UserEntity;
import com.utsab.chess_engine.user.repository.UserInfoRepository;
import com.utsab.chess_engine.user.role.entity.UserRole;
import com.utsab.chess_engine.user.role.repository.RolesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(2)
public class InsertInitialUserInfo implements CommandLineRunner {

    private final UserInfoRepository userInfoRepository;
    private final RolesRepository rolesRepository;
    private final PasswordEncoder passwordEncoder;
    // From configuration (ADMIN_EMAIL, ADMIN_NAME, ADMIN_PASSWORD), so no password is committed; only the
    // development profiles have a default
    @Value("${app.admin.email}")
    private String adminEmail;
    @Value("${app.admin.name}")
    private String adminName;
    @Value("${app.admin.password:}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (adminPassword == null || adminPassword.isBlank()) {
            log.info("No admin password configured (ADMIN_PASSWORD); skipping the admin account.");
            return;
        }
        if (userInfoRepository.findByEmailId(adminEmail).isEmpty()) {
            UserEntity admin = new UserEntity();
            admin.setFullName(adminName);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRoles(List.of(
                    rolesRepository.findByName(UserRole.ADMIN.toString())
                            .orElseThrow(() -> new RuntimeException("ADMIN Role not found"))
            ));
            admin.setEmailId(adminEmail);
            admin.setVerified(true);
            userInfoRepository.save(admin);
            log.info("Admin user inserted.");
        } else {
            log.info("Admin user already exists.");
        }
    }
}
