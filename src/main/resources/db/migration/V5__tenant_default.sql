ALTER TABLE patients
    ALTER COLUMN tenant_id
        SET DEFAULT (
        current_setting(
                'app.current_tenant'
        )::uuid
        );