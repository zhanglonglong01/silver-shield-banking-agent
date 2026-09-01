package com.fintechathon.silvershield.sandbox;

import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sandbox")
@Profile({"dev", "test"})
public class SandboxResetController {

    private final SandboxResetService resetService;

    public SandboxResetController(SandboxResetService resetService) {
        this.resetService = resetService;
    }

    @GetMapping("/metadata")
    public SandboxDatasetMetadata metadata() {
        return resetService.metadata().orElseThrow(() -> new IllegalStateException("Sandbox is not initialized"));
    }

    @PostMapping("/reset")
    @PreAuthorize("hasRole('DEMO_ADMIN')")
    public SandboxDatasetMetadata reset() {
        return resetService.reset();
    }
}
