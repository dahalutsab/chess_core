package com.utsab.chess_engine.user.role.config;

import com.utsab.chess_engine.user.role.entity.Roles;
import com.utsab.chess_engine.user.role.entity.UserRole;
import com.utsab.chess_engine.user.role.repository.RolesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)
public class InitialRolesConfig implements CommandLineRunner {

    private final RolesRepository rolesRepository;

    @Override
    public void run(String... args) throws Exception {
        if (rolesRepository.count() < 3) {
            log.info("Roles creation request received");
            createRoleIfNotExists(UserRole.ADMIN.name(), "The superadmin of the application");
            createRoleIfNotExists(UserRole.USER.name(), "User of Chess Engine");
            log.info("Roles created");
        } else {
            log.info("Roles already exist");
        }
    }

    private void createRoleIfNotExists(String name, String description) {
        if (rolesRepository.findByName(name).isEmpty()) {
            Roles role = Roles.builder()
                    .name(name)
                    .description(description)
                    .build();
            rolesRepository.save(role);
            log.info("Role {} inserted", name);
        }
    }
}