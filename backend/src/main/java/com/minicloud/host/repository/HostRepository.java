package com.minicloud.host.repository;

import com.minicloud.host.entity.Host;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface HostRepository extends JpaRepository<Host, UUID> {

    Optional<Host> findByHostname(String hostname);

    Optional<Host> findFirstByLocalTrue();
}
