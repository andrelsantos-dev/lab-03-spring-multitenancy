package com.alssant.spring_multitenancy.patient;

import com.alssant.spring_multitenancy.api.dto.PatientResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
