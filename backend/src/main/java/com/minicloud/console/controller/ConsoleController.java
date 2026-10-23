package com.minicloud.console.controller;

import com.minicloud.common.response.ApiResponse;
import com.minicloud.console.dto.ConsoleListResponse;
import com.minicloud.console.dto.ConsoleSessionResponse;
import com.minicloud.console.service.ConsoleService;
import com.minicloud.infrastructure.console.ConsoleConnectionInfo;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ConsoleController {

    private final ConsoleService consoleService;

    public ConsoleController(ConsoleService consoleService) {
        this.consoleService = consoleService;
    }

    @GetMapping("/consoles")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ApiResponse<ConsoleListResponse> list() {
        ConsoleListResponse data = consoleService.list();
        return ApiResponse.ok(data, data.note());
    }

    @GetMapping("/vms/{name}/console")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ApiResponse<ConsoleConnectionInfo> get(@PathVariable String name) {
        return ApiResponse.ok(consoleService.get(name), "Console endpoint read from libvirt");
    }

    @PostMapping("/vms/{name}/console/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ApiResponse<ConsoleSessionResponse> open(@PathVariable String name, Authentication authentication) {
        return ApiResponse.ok(
                consoleService.open(name, authentication.getName()),
                "Console ticket issued. The browser will use noVNC through MiniCloud; MiniCloud does not speak VNC itself.");
    }
}
