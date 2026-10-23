package com.minicloud.config;

import com.minicloud.auth.entity.Role;
import com.minicloud.auth.entity.User;
import com.minicloud.auth.repository.UserRepository;
import com.minicloud.host.service.HostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MiniCloudProperties properties;
    private final HostService hostService;

    public DataInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            MiniCloudProperties properties,
            HostService hostService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.hostService = hostService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedUser(properties.getBootstrap().getAdmin(), Role.ADMIN);
        seedUser(properties.getBootstrap().getOperator(), Role.OPERATOR);
        seedUser(properties.getBootstrap().getViewer(), Role.VIEWER);
        hostService.discoverLocalHost();
        log.info("Phase 1 bootstrap complete. Host discovery finished.");
    }

    private void seedUser(MiniCloudProperties.BootstrapUser bootstrap, Role role) {
        if (bootstrap.getUsername() == null || bootstrap.getUsername().isBlank()) {
            return;
        }
        if (userRepository.existsByUsername(bootstrap.getUsername())) {
            return;
        }
        User user = new User();
        user.setUsername(bootstrap.getUsername());
        user.setPasswordHash(passwordEncoder.encode(bootstrap.getPassword()));
        user.setDisplayName(bootstrap.getDisplayName() == null ? bootstrap.getUsername() : bootstrap.getDisplayName());
        user.setRole(role);
        user.setEnabled(true);
        userRepository.save(user);
        log.info("Seeded {} user '{}'", role, bootstrap.getUsername());
    }
}
