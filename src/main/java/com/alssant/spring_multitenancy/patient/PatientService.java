package com.alssant.spring_multitenancy.patient;

import com.alssant.spring_multitenancy.api.dto.CreatePatientRequest;
import com.alssant.spring_multitenancy.api.dto.PatientResponse;
import com.alssant.spring_multitenancy.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class PatientService {
    private final PatientRepository repository;

    public PatientService(PatientRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<PatientResponse> findAll() {
        return repository.findAll();
    }

    @Transactional
    public void create(CreatePatientRequest request) {
        if (!StringUtils.hasText(TenantContext.getTenantId())){
            throw new IllegalStateException("Tenant not set");
        }
        repository.create(request.name());
    }
}
