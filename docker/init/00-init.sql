CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE ROLE app_user LOGIN PASSWORD 'app_password';
CREATE ROLE migration_user LOGIN PASSWORD 'migration_password';