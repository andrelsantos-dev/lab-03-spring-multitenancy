package com.alssant.spring_multitenancy.integration;

import com.alssant.spring_multitenancy.api.dto.PatientResponse;
import com.alssant.spring_multitenancy.support.BaseIntegrationTest;
import com.alssant.spring_multitenancy.support.TenantFixture;
import com.alssant.spring_multitenancy.tenant.TenantContext;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class TenantRlsIntegrationTest extends BaseIntegrationTest {
    @Autowired
    TenantFixture tenantFixture;
    @Autowired
    MockMvc mockMvc;

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void shouldConnectAsAppUser() {
        String currentUser = jdbcTemplate.queryForObject(
                "select current_user",
                String.class
        );

        assertEquals(APP_USER, currentUser);
    }

    @Test
    void shouldExposeTenantAsDatabaseSessionVariable() {
        String tenantId = UUID.randomUUID().toString();
        TenantContext.setTenantId(tenantId);

        String currentUser =
                jdbcTemplate.queryForObject(
                        """
                                SELECT current_setting(
                                    'app.current_tenant',
                                    true
                                )
                                """,
                        String.class
                );

        assertEquals(tenantId, currentUser);

    }

    @Test
    void shouldReturnOnlyPatientsFromHospitalA() {
        String tenantId = tenantFixture.hospitalA();
        TenantContext.setTenantId(tenantId);

        List<PatientResponse> patients = jdbcTemplate.query(
                "SELECT id, tenant_id, name FROM patients",
                new DataClassRowMapper<>(PatientResponse.class)
        );

        assertEquals(1, patients.size());
        assertEquals("Alice", patients.getFirst().name());
    }

    @Test
    void shouldReturnOnlyPatientsFromHospitalB() {
        String tenantId = tenantFixture.hospitalB();
        TenantContext.setTenantId(tenantId);

        List<PatientResponse> patients = jdbcTemplate.query(
                "SELECT id, tenant_id, name FROM patients",
                new DataClassRowMapper<>(PatientResponse.class)
        );

        assertEquals(1, patients.size());
        assertEquals("Bob", patients.getFirst().name());
    }

    @Test
    void shouldReturnEmptyWhenTenantHasNoPatients() {
        String tenantId = UUID.randomUUID().toString();
        TenantContext.setTenantId(tenantId);

        List<PatientResponse> patients = jdbcTemplate.query(
                "SELECT id, tenant_id, name FROM patients",
                new DataClassRowMapper<>(PatientResponse.class)
        );

        assertTrue(patients.isEmpty());
    }

    @Test
    void shouldFilterPatientsByHeader()
            throws Exception {

        mockMvc.perform(
                        get("/database/patients")
                                .header(
                                        "X-Tenant-Id",
                                        tenantFixture.hospitalA()
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.length()"
                        )
                                .value(1)
                );

    }


    @Test
    void shouldNotLeakTenantBetweenRequests() throws Exception {
        String hospitalAId = tenantFixture.hospitalA();
        String hospitalXId = UUID.randomUUID().toString();

        mockMvc.perform(
                        get("/database/patients")
                                .header("X-Tenant-Id", hospitalAId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Alice"))
                .andExpect(jsonPath("$[0].tenantId").value(hospitalAId))
                .andExpect(jsonPath("$[0].id").exists());


        mockMvc.perform(
                        get("/database/patients")
                                .header("X-Tenant-Id", hospitalXId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldClearTenantBetweenRequests()
            throws Exception {

        mockMvc.perform(
                        get("/database/patients")
                                .header(
                                        "X-Tenant-Id",
                                        tenantFixture.hospitalA()
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(
                        get("/database/patients")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

    }

    @Test
    void shouldInsertPatientIntoCurrentTenant() throws Exception {

        String hospitalA = tenantFixture.hospitalA();

        mockMvc.perform(post("/database/patients")
                        .header("X-Tenant-Id", hospitalA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name":
                                  "Charlie"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc
                .perform(get("/database/patients")
                        .header("X-Tenant-Id", hospitalA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name")
                        .value(hasItem("Charlie")))
                .andExpect(jsonPath("$[*].tenantId")
                        .value(
                                everyItem(
                                        equalTo(
                                                hospitalA
                                        )
                                )));

    }

    @Test
    void shouldRejectInsertWithoutTenant() throws Exception {
        ServletException exception = Assertions.assertThrows(ServletException.class, () -> {
            mockMvc.perform(
                    post("/database/patients")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                        "name":"Ghost"
                                    }
                                    """)
            );
        });


        assertInstanceOf(

                IllegalStateException.class,
                exception.getCause()
        );

        assertTrue(
                exception
                        .getCause()
                        .getMessage()
                        .contains(
                                "Tenant not set"
                        )
        );
    }

    @Test
    void shouldNotReturnPatientFromAnotherTenant() throws Exception {
        String hospitalA = tenantFixture.hospitalA();

        mockMvc.perform(post("/database/patients")
                        .header("X-Tenant-Id", hospitalA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name":
                                  "Dave"
                                }
                                """))
                .andExpect(status().isCreated());

        String hospitalB = tenantFixture.hospitalB();
        mockMvc
                .perform(get("/database/patients")
                        .header("X-Tenant-Id", hospitalB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name")
                        .value(not(hasItem("Dave"))))
                .andExpect(jsonPath("$[*].tenantId")
                        .value(
                                everyItem(
                                        equalTo(
                                                hospitalB
                                        )
                                )));

    }
}
