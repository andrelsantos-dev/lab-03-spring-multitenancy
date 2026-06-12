INSERT INTO patients (tenant_id, name)
VALUES
    ((SELECT id FROM tenants WHERE name = 'Hospital A' LIMIT 1), 'Alice'),

    ((SELECT id FROM tenants WHERE name = 'Hospital B' LIMIT 1), 'Bob');