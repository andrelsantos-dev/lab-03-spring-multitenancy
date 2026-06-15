package com.alssant.spring_multitenancy.api.dto;

import java.util.UUID;

public record PatientResponse(
        UUID id,
        UUID tenantId,
        String name
) {
}
