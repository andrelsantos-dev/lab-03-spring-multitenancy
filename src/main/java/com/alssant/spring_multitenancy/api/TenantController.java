package com.alssant.spring_multitenancy.api;

import com.alssant.spring_multitenancy.tenant.TenantContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/tenant")
public class TenantController {
    @GetMapping
    public Map<String, String> currentTenant() {
        return Map.of(
                "tenantId",
                String.valueOf(TenantContext.getTenantId())
        );
    }
}
