package com.alssant.spring_multitenancy.tenant;

import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class TenantAwareDataSource extends DelegatingDataSource {

    public TenantAwareDataSource(DataSource delegate){
        super(delegate);
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = super.getConnection();
        this.applyTenant(connection);

        return connection;
    }

    // Apply tenant context to current DB session for PostgreSQL RLS
    private void applyTenant(Connection connection) {
        String tenantId = TenantContext.getTenantId();

        if (tenantId != null) {
            try(Statement stmt = connection.createStatement()){
                stmt.execute(
                        "SET app.current_tenant = '%s'"
                                .formatted(tenantId)
                );
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
