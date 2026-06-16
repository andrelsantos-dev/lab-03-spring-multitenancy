package com.alssant.spring_multitenancy.patient;

import com.alssant.spring_multitenancy.api.dto.PatientResponse;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PatientRepository {
    public static final String FIND_ALL_QUERY = """
                SELECT
                id,
                tenant_id,
                name
            FROM patients
            """;
    public static final String INSERT =
            """
            INSERT INTO patients (
                tenant_id,
                name
            )
            VALUES (
                current_setting(
                    'app.current_tenant'
                )::uuid,
                ?
            )
            """;

    private final JdbcTemplate jdbcTemplate;

    public PatientRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PatientResponse> findAll() {
        return jdbcTemplate.query(FIND_ALL_QUERY, new DataClassRowMapper<>(PatientResponse.class));
    }

    public void create(String name) {
        jdbcTemplate.update(INSERT, name);
    }
}
