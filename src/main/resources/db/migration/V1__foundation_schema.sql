CREATE TABLE tenants (
     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
     name VARCHAR(255) NOT NULL
);

CREATE TABLE patients (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      tenant_id UUID NOT NULL REFERENCES tenants(id),
      name VARCHAR(255) NOT NULL
);

CREATE INDEX idx_patients_tenant ON patients (tenant_id);

CREATE TABLE appointments (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      patient_id UUID NOT NULL REFERENCES patients(id),
      tenant_id UUID NOT NULL REFERENCES tenants(id),
      description VARCHAR(255) NOT NULL
);

CREATE INDEX idx_appointments_tenant ON appointments (tenant_id);