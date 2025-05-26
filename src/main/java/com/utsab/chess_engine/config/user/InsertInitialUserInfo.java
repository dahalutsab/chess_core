package com.utsab.chess_engine.config.user;

import com.utsab.chess_engine.user.entity.UserEntity;
import com.utsab.chess_engine.user.repository.UserInfoRepository;
import com.utsab.chess_engine.user.role.entity.UserRole;
import com.utsab.chess_engine.user.role.repository.RolesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private static final String ADMIN_PASSWORD = "Admin@123";

    @Override
    public void run(String... args) {
        if (userInfoRepository.findByEmailId("admin@chess_engine.com").isEmpty()) {
            UserEntity admin = new UserEntity();
            admin.setFullName("Admin User");
            admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
            admin.setRoles(List.of(
                    rolesRepository.findByName(UserRole.ADMIN.toString())
                            .orElseThrow(() -> new RuntimeException("SUPER ADMIN Role not found"))
            ));
            admin.setEmailId("admin@chess_engine.com");
            admin.setVerified(true);
            userInfoRepository.save(admin);
            log.info("Admin user inserted.");
        } else {
            log.info("Admin user already exists.");
        }
    }
}
