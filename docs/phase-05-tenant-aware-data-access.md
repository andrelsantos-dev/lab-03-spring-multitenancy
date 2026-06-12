# Phase 05 - Tenant Aware Data Access

## Goal

Validate database access from Spring Boot using JdbcTemplate before applying tenant isolation rules.

This phase introduces basic data access patterns and prepares the application for RLS-based tenant isolation in the next phases.

---

## Database Access with JdbcTemplate

The application accesses PostgreSQL using JdbcTemplate.

The objective is to validate:

```mermaid
graph TD
    %% Style Definitions
    classDef frameworkStyle fill:#cfc,stroke:#333,stroke-width:2px;
    classDef libraryStyle fill:#bdf,stroke:#333,stroke-width:2px;
    classDef dbStyle fill:#ffc,stroke:#333,stroke-width:2px;

    %% Flow Nodes
    A["Spring Boot<br><b>(Application Layer)</b>"]:::frameworkStyle -->|1. Invokes| B["JdbcTemplate<br><b>(Abstraction Layer)</b>"]:::libraryStyle
    B -->|2. Delegates to Driver| C["PostgreSQL Driver<br><b>(Network Connection)</b>"]:::dbStyle
    C -->|3. Executes| D["Database Queries<br><b>(SQL Engine)</b>"]:::dbStyle

    %% Scope Groups (Subgraphs)
    subgraph Java_Application ["Java / JVM Scope"]
        A
        B
    end

    subgraph Database_Server ["PostgreSQL Server Scope"]
        C
        D
    end
```

---

## Query Multiple Records

For queries returning multiple records, JdbcTemplate query() is used.

Example:

```java
List<PatientResponse> patients = jdbcTemplate.query(
    "SELECT id, tenant_id, name FROM patients",
    new DataClassRowMapper<>(PatientResponse.class)
);
```

The result is mapped into a List of DTO objects using DataClassRowMapper.

Example endpoint:

```java
@GetMapping("/patients")
public ResponseEntity<List<PatientResponse>> getPatients() {
    List<PatientResponse> tenants = jdbcTemplate.query(
            "SELECT id, tenant_id, name FROM patients",
            new DataClassRowMapper<>(PatientResponse.class)
    );

    return ResponseEntity.ok(tenants);
}
```

---

## Query Single Record

For queries returning a single object, JdbcTemplate queryForObject() is used.

Example:

```java
TenantResponse tenant = jdbcTemplate.queryForObject(
    "SELECT id, name FROM tenants where id = ?",
    new DataClassRowMapper<>(TenantResponse.class),
    id
);
```

When no record is found, EmptyResultDataAccessException is handled and converted into HTTP 404.

---

## Tenant Query Example

Endpoint:

```bash
curl http://localhost:8080/database/tenants
```

Result:

```json
[
  {
    "id":"671ae306-baf0-4a9c-8374-136322f033c3",
    "name":"Hospital A"
  },
  {
    "id":"80d34bcd-e11f-4051-afe7-6ea607793c6b",
    "name":"Hospital B"
  }
]
```

---

## Patient Query Example

Endpoint:

```bash
curl http://localhost:8080/database/patients
```

Result:

```json
[
  {
    "id":"55eac6fc-ea63-4756-ba98-cc2baa6e8399",
    "tenantId":"ed3b3b4a-cbad-4565-b77a-53b34b79756f",
    "name":"Alice"
  },
  {
    "id":"825ff9da-d0cc-4439-8ef5-93b0f58a3d87",
    "tenantId":"f920f4f5-7790-4ba5-9b14-6d7393ed4512",
    "name":"Bob"
  }
]
```

---

## RLS Behavior During This Phase

The application datasource is currently configured using:

```text
migration_user
```

This role has:

```sql
BYPASSRLS
```

permission.

Because of this, queries executed by the application are not restricted by Row Level Security policies.

The following query returns records from multiple tenants:

```sql
SELECT id, tenant_id, name
FROM patients;
```

This behavior is expected because the goal of this phase is to validate database access patterns.

---

## Key Learnings

JdbcTemplate provides different methods depending on the expected result:

```mermaid
graph TD
    %% Style Definitions
    classDef methodStyle fill:#bdf,stroke:#333,stroke-width:2px;
    classDef resultStyle fill:#fff,stroke:#333,stroke-dasharray: 5 5;

    %% Core Concept Node
    Start["JdbcTemplate Method Selection"] --> Methods

    %% Branching to Methods
    subgraph Methods ["Methods"]
        M1["query(...)"]:::methodStyle
        M2["queryForObject(...)"]:::methodStyle
        M3["update(...)"]:::methodStyle
    end

    %% Method mapping to Results
    subgraph Expected_Results ["Expected Results"]
        R1["Multiple Records<br><b>(Returns List&lt;T&gt;)</b>"]:::resultStyle
        R2["Single Record / Value<br><b>(Returns T)</b>"]:::resultStyle
        R3["Affected Rows<br><b>(Returns int)</b>"]:::resultStyle
    end

    %% Flow Connections
    M1 -->|Maps to| R1
    M2 -->|Maps to| R2
    M3 -->|Maps to| R3
```

DataClassRowMapper allows mapping query results directly into Java records.

```mermaid
graph TD
    %% Style Definitions
    classDef dbStyle fill:#ffc,stroke:#333,stroke-width:2px;
    classDef mapperStyle fill:#bdf,stroke:#333,stroke-width:2px;
    classDef javaStyle fill:#cfc,stroke:#333,stroke-width:2px;

    %% Nodes
    subgraph DB_Result ["1. Database ResultSet (Snake Case)"]
        A["id: '671ae3bc...'<br>name: 'John Doe'<br>tenant_id: '892bf3...'"]:::dbStyle
    end

    subgraph Mapper_Engine ["2. DataClassRowMapper Interception"]
        B["Matches column names to constructor parameters<br><i>(snake_case ➔ camelCase)</i>"]:::mapperStyle
    end

    subgraph Java_Record ["3. Java Record Instance (Immutable)"]
        C["new PatientResponse(<br>&nbsp;&nbsp;id,<br>&nbsp;&nbsp;name,<br>&nbsp;&nbsp;tenantId<br>)"]:::javaStyle
    end

    %% Connections
    A --> B
    B -->|Invokes Constructor via Reflection| C
```

---

## Next Step

The next phase will replace the migration role with the application role and validate tenant isolation using PostgreSQL Row Level Security.