package com.alssant.spring_multitenancy.api.dto;

import java.util.UUID;

public record TenantResponse(
        UUID id,
        String name
) {
}
