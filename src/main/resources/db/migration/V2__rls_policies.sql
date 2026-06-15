ALTER TABLE patients ENABLE ROW LEVEL SECURITY;
ALTER TABLE patients FORCE ROW LEVEL SECURITY;

ALTER TABLE appointments ENABLE ROW LEVEL SECURITY;
ALTER TABLE appointments FORCE ROW LEVEL SECURITY;

CREATE POLICY patients_tenant_isolation ON patients
USING (
    tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid
)
WITH CHECK (
    tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid
);

CREATE POLICY appointments_tenant_isolation ON appointments
USING (
    tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid
)
WITH CHECK (
    tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid
);

GRANT SELECT, INSERT, UPDATE, DELETE ON patients TO app_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON appointments TO app_user;