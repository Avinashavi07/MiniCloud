package com.minicloud.vm.repository;

import com.minicloud.vm.entity.VirtualMachine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VirtualMachineRepository extends JpaRepository<VirtualMachine, UUID> {

    Optional<VirtualMachine> findByName(String name);

    void deleteByName(String name);
}
