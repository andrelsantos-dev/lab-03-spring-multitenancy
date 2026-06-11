package com.alssant.spring_multitenancy.api;

import com.alssant.spring_multitenancy.tenant.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/database")
public class DatabaseController {
    private final JdbcTemplate jdbcTemplate;

    public DatabaseController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /*
     * Returns the current database user executing the query.
     * This helps verify which role Spring Boot is using to connect to PostgreSQL.
     * @return
     */
    @GetMapping("/user")
    public Map<String, String> currentUser() {

        String currentUser = jdbcTemplate.queryForObject(
                "select current_user",
                String.class
        );

        return Map.of(
                "currentUser",
                currentUser
        );
    }

    @PostMapping("/tenant/default")
    public ResponseEntity<String> setTenantDefault() {

        jdbcTemplate.execute(
                "SET app.current_tenant = 'hospital-a'"
        );

        return ResponseEntity.ok("Tenant set to default : hospital-a");
    }

    @GetMapping("/tenant/current")
    public ResponseEntity<String> getCurrentTenant() {
        String currentTenant = jdbcTemplate.queryForObject(
                "SELECT current_setting('app.current_tenant', true)",
                String.class
        );

        if (currentTenant == null || currentTenant.isEmpty()) {
            return ResponseEntity.ok("No tenant set");
        }

        return ResponseEntity.ok(currentTenant);
    }

    @PostMapping("/tenant/sync")
    public ResponseEntity<Map<String, String>> syncTenant() {

        String tenantId = TenantContext.getTenantId();

        if (tenantId == null) {
            tenantId = "NOT_DEFINED";
        }

        // SELECT set_config('app.current_tenant', ?, false) is a way to protect against sql injection so we change the SET app.current_tenant = ..

        jdbcTemplate.queryForObject(
                "SELECT set_config('app.current_tenant', ?, false)",
                String.class,
                tenantId
        );

        String currentTenant = jdbcTemplate.queryForObject(
                "SELECT current_setting('app.current_tenant', true)",
                String.class
        );

        return ResponseEntity.ok(Map.of(
                "tenantContext", tenantId,
                "sessionTenant", currentTenant != null ? currentTenant : "NOT_SET"
        ));
    }

    @PostMapping("/tenant/reset")
    public ResponseEntity<String> resetTenant() {

        jdbcTemplate.execute(
                "RESET app.current_tenant"
        );

        return ResponseEntity.ok("Tenant reset");
    }
}
