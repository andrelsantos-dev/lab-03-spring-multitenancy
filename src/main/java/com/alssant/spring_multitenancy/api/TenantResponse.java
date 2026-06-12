package com.alssant.spring_multitenancy.api;

import java.util.UUID;

public record TenantResponse(
        UUID id,
        String name
) {
}
