# Phase 07 — Integration Tests with Spring Boot, Testcontainers and PostgreSQL RLS

## Objective

Validate application behavior in an isolated environment using Spring Boot, Testcontainers and PostgreSQL RLS, ensuring that:

* Spring Boot starts correctly
* Flyway migrations execute automatically
* PostgreSQL roles are initialized correctly
* Runtime executes with restricted permissions
* Tenant context propagates correctly
* PostgreSQL RLS enforces data isolation
* Tests do not depend on local infrastructure

---

## Problem Statement

The application previously depended on a manually started PostgreSQL instance:

```text
localhost:5432
```

This approach had limitations:

* tests depended on external infrastructure
* environment configuration could differ between machines
* Flyway migrations could interfere with local databases
* runtime and migration credentials were not isolated

To make tests reproducible, PostgreSQL was moved into Testcontainers.

---

## Dependencies

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
```

Note:

Testcontainers transitive vulnerabilities were intentionally not overridden because these dependencies exist only in test scope and are not packaged in runtime artifacts.

---

## Test Infrastructure

A reusable integration base was introduced.

```mermaid
graph TD
    %% Node Declarations
    Node_Base["BaseIntegrationTest"]
    Node_Tenant["TenantRlsIntegrationTest"]

    %% Relationships
    Node_Base -->|Inherits Infrastructure / Extends| Node_Tenant

    %% Styling
    style Node_Base fill:#bbf,stroke:#333,stroke-width:2px
    style Node_Tenant fill:#99ff99,stroke:#333,stroke-width:2px
```

Responsibilities:

### BaseIntegrationTest

Provides:

* PostgreSQL Testcontainer
* Spring Boot integration
* datasource override
* Flyway configuration

### TenantRlsIntegrationTest

Validates:

* runtime permissions
* session variables
* RLS filtering
* HTTP integration

---

## Initializing PostgreSQL for Tests

Container bootstrap:

```java
@SpringBootTest
@Testcontainers
abstract class BaseIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17")
                    .withDatabaseName("dbRLSTest")
                    .withInitScript(
                            "db/init/00-test-init.sql"
                    );

}
```

The initialization script executes before Spring starts.

Responsibilities:

* create database roles
* enable extensions
* configure migration access

---

## Dynamic Property Injection

Application configuration points to localhost.

Integration tests override those values dynamically:

```java
@DynamicPropertySource
static void configureProperties(DynamicPropertyRegistry registry) {
    String jdbcUrl = postgres.getJdbcUrl();

    registry.add("spring.flyway.url", () -> jdbcUrl);
    registry.add("spring.flyway.user", () -> MIGRATION_USER);
    registry.add("spring.flyway.password", () -> "migration_password");

    registry.add("spring.datasource.url", () -> jdbcUrl);
    registry.add("spring.datasource.username", () -> APP_USER);
    registry.add("spring.datasource.password", () -> "app_password");
}
```

This guarantees that tests never connect to localhost.

---

## Database Role Separation

Three users participate in execution.

```mermaid
graph TD
    %% Node Declarations
    Node_Postgres[postgres/test]
    Node_Bootstrap[Container Bootstrap]
    Node_Init[Creates Roles & Executes init.sql]
    
    Node_Migration[migration_user]
    Node_Flyway[Used by Flyway]
    Node_DDL[Executes DDL + BYPASSRLS]
    
    Node_App[app_user]
    Node_Runtime[Application Runtime]
    Node_Access[Restricted Access]

    %% Relationships
    Node_Postgres --> Node_Bootstrap
    Node_Bootstrap --> Node_Init
    
    Node_Migration --> Node_Flyway
    Node_Flyway --> Node_DDL
    
    Node_App --> Node_Runtime
    Node_Runtime --> Node_Access

    %% Styling
    style Node_Postgres fill:#f9f,stroke:#333,stroke-width:2px
    style Node_Migration fill:#f96,stroke:#333,stroke-width:2px
    style Node_App fill:#bbf,stroke:#333,stroke-width:2px
```

This separation simulates production responsibilities.

---

## Why @ServiceConnection Was Not Used

Spring Boot provides:

```java
@ServiceConnection
```

This feature simplifies container integration.

However, this lab intentionally separates database responsibilities.

Using `@ServiceConnection` caused datasource credentials to be overridden.

Result:

```text
runtime → container user
```

instead of:

```text
runtime → app_user
```

For this architecture, `@DynamicPropertySource` provided explicit control.

---

## Runtime Validation

To ensure runtime permissions were applied correctly:

```java
@Test
void readCurrentUser() {

    String currentUser =
            jdbcTemplate.queryForObject(
                    "select current_user",
                    String.class
            );

    System.out.println(
            "Current User: "
                    + currentUser
    );

}
```

Output:

```text
Current User: app_user
```

This confirms that:

* migrations executed with migration_user
* application executed with app_user
* runtime did not inherit elevated privileges

---

## Tenant Context Propagation

The application must propagate tenant information from request execution to PostgreSQL.

Flow:

```mermaid
graph TD
    %% Node Declarations
    Node_Req["Request"]
    Node_Filter["Tenant Filter"]
    Node_Ctx["TenantContext (ThreadLocal)"]
    Node_DS["TenantAwareDataSource"]
    Node_Session["Database Session"]
    Node_RLS["PostgreSQL RLS"]

    %% Relationships
    Node_Req -->|1. Incoming HTTP| Node_Filter
    Node_Filter -->|2. Extract & Set| Node_Ctx
    Node_Ctx -->|3. Read Context| Node_DS
    Node_DS -->|4. SET LOCAL app.current_tenant| Node_Session
    Node_Session -->|5. Evaluate Policy| Node_RLS

    %% Styling
    style Node_Req fill:#f9f,stroke:#333,stroke-width:2px
    style Node_Filter fill:#f96,stroke:#333,stroke-width:2px
    style Node_Ctx fill:#bbf,stroke:#333,stroke-width:2px
    style Node_DS fill:#ff9999,stroke:#333,stroke-width:2px
    style Node_Session fill:#ffff99,stroke:#333,stroke-width:2px
    style Node_RLS fill:#99ff99,stroke:#333,stroke-width:2px
```

The datasource applies:

```sql
SET app.current_tenant = '<tenant-id>'
```

before executing queries.

---

## Why Tenant Is Applied Per Connection

Initial idea:

```text
Before every SELECT
Before every UPDATE
Before every DELETE
```

This was rejected.

Transactions may execute multiple operations using the same connection.

Final approach:

```mermaid
graph TD
    %% Node Declarations
    Node_Acquire["Acquire Connection"]
    Node_Apply["Apply Tenant Once"]
    Node_Execute["Execute Operations"]
    Node_Return["Return Connection"]

    %% Relationships
    Node_Acquire -->|1. Pull from Pool| Node_Apply
    Node_Apply -->|2. SET LOCAL session_var| Node_Execute
    Node_Execute -->|3. Run Queries / RLS| Node_Return

    %% Styling
    style Node_Acquire fill:#bbf,stroke:#333,stroke-width:2px
    style Node_Apply fill:#f96,stroke:#333,stroke-width:2px
    style Node_Execute fill:#99ff99,stroke:#333,stroke-width:2px
    style Node_Return fill:#ff9999,stroke:#333,stroke-width:2px
```

---

## ThreadLocal Lifecycle

Tenant values must not leak.

Important rule:

```text
ThreadLocal.remove()
```

clears only the current thread.

Connection reuse remains the real risk.

Cleanup is executed after tests.

Example:

```java
@AfterEach
void clearTenant() {
    TenantContext.clear();
}
```

---

## Tenant Fixture

Tenant IDs are generated dynamically.

To avoid hardcoded values, `TenantFixture` was introduced.

Example:

```java
tenantFixture.hospitalA();
tenantFixture.hospitalB();
```

Responsibilities:

* discover tenant IDs
* cache values
* simplify test readability

---

## Integration Coverage

Validated scenarios:

### Database Connectivity

```text
Application connects as app_user
```

### Session Variables

```sql
-- TenantContext propagates to:
SELECT current_setting('app.current_tenant');
```

### RLS Filtering

```text
Hospital A → Alice

Hospital B → Bob

Unknown Tenant → Empty Result
```

### HTTP Integration

Using MockMvc:

```mermaid
graph TD
    %% Node Declarations
    Node_Header["HTTP Header"]
    Node_Filter["Spring Filter"]
    Node_Ctx["TenantContext"]
    Node_DS["Datasource"]
    Node_PG["PostgreSQL"]
    Node_JSON["JSON Response"]

    %% Relationships
    Node_Header -->|1. Transmits Tenant ID| Node_Filter
    Node_Filter -->|2. Intercepts & Extracts| Node_Ctx
    Node_Ctx -->|3. Propagates Context| Node_DS
    Node_DS -->|4. Configures Session & Queries| Node_PG
    Node_PG -->|5. Returns Isolated Data| Node_JSON

    %% Styling
    style Node_Header fill:#f9f,stroke:#333,stroke-width:2px
    style Node_Filter fill:#f96,stroke:#333,stroke-width:2px
    style Node_Ctx fill:#bbf,stroke:#333,stroke-width:2px
    style Node_DS fill:#ff9999,stroke:#333,stroke-width:2px
    style Node_PG fill:#99ff99,stroke:#333,stroke-width:2px
    style Node_JSON fill:#99bbf,stroke:#333,stroke-width:2px
```

Example:

```text
Hospital A
→ returns Alice

Unknown Tenant
→ returns []
```

---

## Unexpected Issues During Implementation

Main discoveries:

* duplicate `@SpringBootTest`
* datasource override conflicts
* `@ServiceConnection` limitations
* role initialization order
* ResultSet cursor misuse
* localhost configuration leakage

Root cause analysis was performed layer by layer.

### RLS Policies Must Handle Missing Tenant Context

During integration testing, unexpected behavior was discovered when validating tenant isolation across multiple sequential HTTP requests.

#### The Scenario
* **Request 1:** `X-Tenant-Id: hospital-a-uuid` → Successfully returns `[Alice]`.
* **Request 2:** No tenant header provided → Expected: `[]` (Empty list).

**Actual Result:**
```text
org.postgresql.util.PSQLException: ERROR: invalid input syntax for type uuid: ""

```

#### Root Cause Analysis

The issue was **not** caused by a `ThreadLocal` memory leak. The application correctly executed `TenantContext.clear()`, and the database pool executed:

```sql
-- or RESET app.current_tenant
SET app.current_tenant = '';
```

However, the original PostgreSQL RLS policy performed a direct string-to-UUID conversion:

```sql
current_setting('app.current_tenant', true)::uuid
```

When the session variable was cleared (becoming an empty string `''`), PostgreSQL attempted to execute `''::uuid`, which is syntactically invalid for the `UUID` data type, throwing a `500 Internal Server Error`.

#### Solution

All Row-Level Security (RLS) policies were updated to safely tolerate empty or missing tenant contexts using `NULLIF`.

**Before:**

```sql
USING 
    (tenant_id = current_setting('app.current_tenant', true)::uuid)
WITH CHECK 
    (tenant_id = current_setting('app.current_tenant', true)::uuid);

```

**After:**

```sql
USING 
    (tenant_id = 
            NULLIF(current_setting('app.current_tenant', true), '')::uuid)
WITH CHECK 
    (tenant_id = 
            NULLIF(current_setting('app.current_tenant', true), '')::uuid);

```

### Why This Works (Execution Flow)

* **Old Broken Flow:** `''` (Empty) → `::uuid` (Cast) → **`PSQLException (Invalid Syntax)`**
* **New Resilient Flow:** `''` (Empty) → `NULLIF('', '')` → `NULL` → `NULL::uuid` (Valid) → `tenant_id = NULL` → **`Safe Empty Result []`**

### Architectural Lesson

Database-level security policies must be defensive and should never assume the application state is always present or perfectly populated.

A missing or cleared tenant context must fail closed (**no access / empty result**), never open, and should not crash the database engine with an unhandled exception. This improvement makes our RLS implementation resilient against:

* Missing or malformed HTTP headers.
* Cleared `ThreadLocal` contexts in async threads.
* Recycled or dirty pooled database connections.

---

## Key Learnings

* Integration tests should not depend on localhost
* Testcontainers improves reproducibility
* Flyway and runtime users may differ
* Container bootstrap is independent of runtime access
* `@ServiceConnection` is useful but not universal
* `@DynamicPropertySource` provides explicit configuration control
* `SELECT current_user` is a simple way to validate active credentials
* RLS policies should tolerate missing tenant context and fail closed

---

## Final Result

Integration tests now validate:

```mermaid
graph TD
%% Node Declarations
    Node_SB["Spring Boot"]
    Node_TC["Testcontainers"]
    Node_Fly["Flyway Migrations"]
    Node_DS["Spring Datasource"]
    Node_Ctx["Tenant Context"]
    Node_Session["PostgreSQL Session"]
    Node_RLS["PostgreSQL RLS"]
    Node_HTTP["HTTP Response"]

%% Relationships
    Node_SB -->|1. Starts Suite & Context| Node_TC
    Node_TC -->|2. Provisions Postgres Container| Node_Fly
    Node_Fly -->|3. Executes DDL as migration_user| Node_DS
    Node_DS -->|4. Obtains Connection as app_user| Node_Ctx
    Node_Ctx -->|5. Binds Tenant ID to ThreadLocal| Node_Session
    Node_Session -->|6. Sets SET LOCAL session_var| Node_RLS
    Node_RLS -->|7. Enforces Row Isolation| Node_HTTP

%% Styling
    style Node_SB fill:#ff9999,stroke:#333,stroke-width:2px
    style Node_TC fill:#f9f,stroke:#333,stroke-width:2px
    style Node_Fly fill:#f96,stroke:#333,stroke-width:2px
    style Node_DS fill:#bbf,stroke:#333,stroke-width:2px
    style Node_Ctx fill:#ffff99,stroke:#333,stroke-width:2px
    style Node_Session fill:#99bbf,stroke:#333,stroke-width:2px
    style Node_RLS fill:#99ff99,stroke:#333,stroke-width:2px
    style Node_HTTP fill:#e1bbf7,stroke:#333,stroke-width:2px
```

Validated:

* isolated infrastructure
* runtime permissions
* tenant propagation
* database filtering
* HTTP integration
* end-to-end tenant isolation

Phase completed successfully.
