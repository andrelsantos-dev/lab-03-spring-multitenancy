package com.alssant.spring_multitenancy.support;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TenantFixture {
    private final JdbcTemplate admin;
    private final Map<String, String> tenants = new ConcurrentHashMap<>();

    public TenantFixture(@Value("${spring.flyway.url}") String url) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(url);
        ds.setUsername("migration_user");
        ds.setPassword("migration_password");

        admin = new JdbcTemplate(ds);
    }

    @PostConstruct
    private void fillTenants() {
        String sql = "SELECT id, name FROM tenants";

        admin.query(sql, (rs) -> {
            tenants.put(rs.getString("name"), rs.getString("id"));
        });

    }

    public String hospitalA() {
        return getTenant("Hospital A");
    }

    public String hospitalB() {
        return getTenant("Hospital B");
    }

    public String getTenant(String name) {

        String tenantId = tenants.get(name);

        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant not found: " + name);
        }

        return tenantId;

    }
}
